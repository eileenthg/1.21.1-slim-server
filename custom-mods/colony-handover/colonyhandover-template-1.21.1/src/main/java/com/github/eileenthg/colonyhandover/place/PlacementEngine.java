package com.github.eileenthg.colonyhandover.place;

import com.github.eileenthg.colonyhandover.mapping.BuildingMapping;
import net.conczin.mca.resources.BuildingTypes;
import net.conczin.mca.resources.data.BuildingType;
import net.conczin.mca.server.world.data.Building;
import net.conczin.mca.server.world.data.BuildingScanResult;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Making one building satisfy one MCA building type, with no idea who asked.
 *
 * <h2>Why this exists separately from the command</h2>
 *
 * This logic was inside {@code PlaceCommand.run}, tangled with the chat output, the
 * permission check, and the assumption that the target is whichever hut the player
 * is standing nearest. The handover pipeline needs the same work for every building
 * in a snapshot, with no player and no chat - and a second copy of it would drift
 * from the first. So the decision-making lives here and returns a {@link Report};
 * callers decide what to say about it.
 *
 * <h2>The sequence, and why each step is where it is</h2>
 *
 * <ol>
 *   <li><b>Find a seed.</b> Never the hut block - MCA needs a walkable anchor and a
 *       hut block is a solid marker, so seeding from it fails as {@code TOO_SMALL}
 *       before anything is measured (finding #6).</li>
 *   <li><b>Scan first.</b> If the room does not already pass, no amount of block
 *       placement will help, and writing blocks we then roll back wastes time and
 *       reads as a failure.</li>
 *   <li><b>Refuse up front.</b> If any missing group has no block in our table,
 *       stop before writing rather than placing a partial shortfall.</li>
 *   <li><b>Exclude the seed cell.</b> Filling it turns our own vantage point solid,
 *       so the verifying rescan reports {@code TOO_SMALL} - which looks like the
 *       placement broke the building when it only broke the view (finding #11).</li>
 *   <li><b>Verify, then keep or roll back.</b> Every write is journalled, so a
 *       building that does not end up matching leaves the world untouched.</li>
 * </ol>
 */
public final class PlacementEngine {

    private static final int SEED_RADIUS = 8;
    private static final int SEED_VERTICAL = 6;

    public enum Outcome {
        /** Mapped as SKIP - nothing was ever meant to happen here. */
        SKIPPED,
        /** Not a ROOM anchor; job sites and marker swaps are a different job. */
        NOT_ROOM,
        /** No enclosed, roofed cell to scan from. */
        NO_SEED,
        /** MCA rejected the room before we touched it. */
        SCAN_REFUSED,
        /** Our table names an MCA type that does not exist. */
        NO_MCA_TYPE,
        /** Already satisfies the type. */
        ALREADY_MATCHES,
        /** A required group has no block in our mapping - refused before writing. */
        UNSATISFIABLE,
        /** Would place these (dry run only). */
        WOULD_PLACE,
        /** Placed and verified. */
        PLACED,
        /** Placement broke the scan; rolled back. */
        ROLLED_BACK_SCAN,
        /** Placed everything and still did not match; rolled back. */
        ROLLED_BACK_TYPE
    }

    public record Placement(Block block, BlockPos pos) {}

    public record Report(
            Outcome outcome,
            String hutId,
            String mcaType,
            BlockPos seed,
            boolean seedFromHint,
            Building.validationResult beforeResult,
            Building.validationResult afterResult,
            Map<ResourceLocation, Integer> missing,
            List<ResourceLocation> unsatisfiable,
            List<Placement> planned,
            int placed,
            int floorCells,
            int candidates) {

        public boolean success() {
            return outcome == Outcome.PLACED || outcome == Outcome.ALREADY_MATCHES;
        }
    }

    /**
     * @param seedHint where to prefer scanning from - the player's feet when a
     *                 person is driving, null in the pipeline. Falls back to a
     *                 search around the hut either way.
     */
    public static Report run(ServerLevel level, String hutId, BlockPos hutPos,
                             BlockPos seedHint, boolean dryRun) {
        final BuildingMapping.Conversion plan = BuildingMapping.forHut(hutId);

        if (plan.anchor() == BuildingMapping.Anchor.SKIP) {
            return bare(Outcome.SKIPPED, hutId, plan.mcaType());
        }
        if (plan.anchor() != BuildingMapping.Anchor.ROOM) {
            return bare(Outcome.NOT_ROOM, hutId, plan.mcaType());
        }

        BlockPos seed = seedHint == null
                ? null
                : Placer.findSeedNear(level, seedHint).orElse(null);
        final boolean fromHint = seed != null;
        if (seed == null) {
            seed = Placer.findInteriorSeed(level, hutPos, SEED_RADIUS, SEED_VERTICAL).orElse(null);
        }
        if (seed == null) {
            return bare(Outcome.NO_SEED, hutId, plan.mcaType());
        }

        final VillageManager villages = VillageManager.get(level);
        final BuildingScanResult before = villages.analyzeRoomAddition(seed);
        if (before.result() != Building.validationResult.SUCCESS) {
            return new Report(Outcome.SCAN_REFUSED, hutId, plan.mcaType(), seed, fromHint,
                    before.result(), null, Map.of(), List.of(), List.of(), 0, 0, 0);
        }

        final BuildingType type = BuildingTypes.getInstance().getBuildingType(plan.mcaType());
        if (type == null) {
            return bare(Outcome.NO_MCA_TYPE, hutId, plan.mcaType());
        }

        final Map<ResourceLocation, List<BlockPos>> have =
                type.getGroups(before.building().getBlocks());
        final Map<ResourceLocation, Integer> missing = new LinkedHashMap<>();
        type.getGroups().forEach((group, required) -> {
            final int present = have.containsKey(group) ? have.get(group).size() : 0;
            if (present < required) {
                missing.put(group, required - present);
            }
        });
        if (missing.isEmpty()) {
            return new Report(Outcome.ALREADY_MATCHES, hutId, plan.mcaType(), seed, fromHint,
                    before.result(), before.result(), Map.of(), List.of(), List.of(), 0, 0, 0);
        }

        // Map each missing group back to a concrete block from our table. A group
        // key is either a block id (minecraft:lectern) or a TAG id with the '#'
        // stripped (mca:chests), so matching on id alone silently missed every
        // tag requirement - which is most of them: chests, beds, cauldrons.
        final List<Block> toPlace = new ArrayList<>();
        final List<ResourceLocation> unsatisfiable = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, Integer> m : missing.entrySet()) {
            Block chosen = null;
            for (final BuildingMapping.Need need : plan.needs()) {
                final Block block = need.block();
                if (block != null && matchesGroup(block, m.getKey())) {
                    chosen = block;
                    break;
                }
            }
            if (chosen == null) {
                unsatisfiable.add(m.getKey());
                continue;
            }
            for (int i = 0; i < m.getValue(); i++) {
                toPlace.add(chosen);
            }
        }
        if (!unsatisfiable.isEmpty()) {
            return new Report(Outcome.UNSATISFIABLE, hutId, plan.mcaType(), seed, fromHint,
                    before.result(), null, missing, unsatisfiable, List.of(), 0, 0, 0);
        }

        final List<BlockPos> floorCells = new ArrayList<>();
        before.building().getFloorRegions().forEach(region -> floorCells.addAll(region.cells()));
        final List<BlockPos> candidates = new ArrayList<>(
                Placer.rankedCandidates(level, floorCells));
        final BlockPos scanSeed = seed;
        candidates.removeIf(c -> c.equals(scanSeed));

        if (dryRun) {
            return new Report(Outcome.WOULD_PLACE, hutId, plan.mcaType(), seed, fromHint,
                    before.result(), null, missing, List.of(),
                    planPlacements(level, toPlace, candidates), 0,
                    floorCells.size(), candidates.size());
        }

        final PlacementJournal.Transaction tx = PlacementJournal.begin(
                hutId + " -> " + plan.mcaType() + " @ " + hutPos.toShortString());
        final List<Placement> done = new ArrayList<>();
        for (final Placement step : planPlacements(level, toPlace, candidates)) {
            if (step.block() instanceof BedBlock) {
                // A bed is two cells and MCA counts only its HEAD, so it cannot go
                // through the single-block path (finding #11). The planner already
                // proved a pair exists here; this just writes it.
                if (!Placer.placeBed(level, tx, step.pos(), step.block(),
                        List.of(step.pos(), step.pos().north(), step.pos().south(),
                                step.pos().east(), step.pos().west())).isEmpty()) {
                    done.add(step);
                }
            } else if (Placer.place(level, tx, step.pos(), step.block())) {
                done.add(step);
            }
        }

        final BuildingScanResult after = villages.analyzeRoomAddition(seed);
        if (after.result() != Building.validationResult.SUCCESS) {
            tx.restore(level);
            return new Report(Outcome.ROLLED_BACK_SCAN, hutId, plan.mcaType(), seed, fromHint,
                    before.result(), after.result(), missing, List.of(), done, 0,
                    floorCells.size(), candidates.size());
        }
        if (!after.building().matchesType(type)) {
            tx.restore(level);
            return new Report(Outcome.ROLLED_BACK_TYPE, hutId, plan.mcaType(), seed, fromHint,
                    before.result(), after.result(), missing, List.of(), done, 0,
                    floorCells.size(), candidates.size());
        }

        PlacementJournal.get(level).commit(tx);
        return new Report(Outcome.PLACED, hutId, plan.mcaType(), seed, fromHint,
                before.result(), after.result(), missing, List.of(), done, done.size(),
                floorCells.size(), candidates.size());
    }

    /**
     * Decides exactly which block goes in which cell, writing nothing.
     *
     * <p>Both paths run this: the dry run reports it, the real run executes it. That
     * is the whole point - a readiness check is only worth having if it predicts what
     * placement will actually do, and the two drift the moment they reason
     * separately. It also means a bed is modelled as what it is, two adjacent cells,
     * rather than as one cell that happens to hold a bed. Counting free cells alone
     * would call a room ready that a bed cannot physically fit in.
     *
     * @return the placements that fit, shortest-first; fewer entries than
     *         {@code toPlace} means the room is too cramped
     */
    private static List<Placement> planPlacements(ServerLevel level, List<Block> toPlace,
                                                  List<BlockPos> candidates) {
        final List<Placement> planned = new ArrayList<>();
        final List<BlockPos> free = new ArrayList<>(candidates);

        for (final Block block : toPlace) {
            if (free.isEmpty()) {
                break;
            }
            if (block instanceof BedBlock) {
                BlockPos chosen = null;
                for (final BlockPos foot : free) {
                    final var dir = Placer.bedDirection(level, foot, block, free);
                    if (dir.isPresent()) {
                        chosen = foot;
                        free.remove(foot.relative(dir.get()));
                        break;
                    }
                }
                if (chosen != null) {
                    free.remove(chosen);
                    planned.add(new Placement(block, chosen));
                }
            } else {
                planned.add(new Placement(block, free.remove(0)));
            }
        }
        return planned;
    }

    /**
     * Whether this block satisfies a requirement group.
     *
     * <p>Group keys are block ids for plain requirements and tag ids with the
     * {@code #} stripped for tag requirements, so both have to be tried.
     */
    public static boolean matchesGroup(Block block, ResourceLocation group) {
        if (group.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            return true;
        }
        return BuiltInRegistries.BLOCK.getHolder(BuiltInRegistries.BLOCK.getKey(block))
                .map(holder -> holder.is(TagKey.create(Registries.BLOCK, group)))
                .orElse(false);
    }

    private static Report bare(Outcome outcome, String hutId, String mcaType) {
        return new Report(outcome, hutId, mcaType, null, false, null, null,
                Map.of(), List.of(), List.of(), 0, 0, 0);
    }

    private PlacementEngine() {}
}
