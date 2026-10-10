package com.github.eileenthg.colonyhandover.command;

import com.github.eileenthg.colonyhandover.mapping.BuildingMapping;
import com.github.eileenthg.colonyhandover.place.PlacementJournal;
import com.github.eileenthg.colonyhandover.place.PlacementEngine;
import com.github.eileenthg.colonyhandover.place.Placer;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Rank;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.conczin.mca.server.world.data.Building;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /colonyhandover place [dry]} and {@code /colonyhandover undo}.
 *
 * <p>Stage one of the placement engine: one building at a time, with a working
 * undo. The loop is deliberately measure-rather-than-predict:
 *
 * <ol>
 *   <li>scan the building as it stands</li>
 *   <li>ask the target type what is still missing</li>
 *   <li>place only that, journalling every write</li>
 *   <li>scan again</li>
 *   <li>keep it if the result improved, roll back if it did not</li>
 * </ol>
 *
 * <p>Step five is the point. We never have to be sure a placement was correct,
 * only able to tell afterwards and undo when it was not.
 */
public final class PlaceCommand {

    /** How far from a job-site hut we will look for standable ground. */
    private static final int SITE_RADIUS = 6;
    private static final int SITE_VERTICAL = 3;

    /** How far from the hut block to hunt for a spot MCA will scan from. */
    private static final int SEED_RADIUS = 8;
    private static final int SEED_VERTICAL = 6;

    public static LiteralArgumentBuilder<CommandSourceStack> place() {
        return Commands.literal("place")
                .executes(ctx -> run(ctx.getSource(), false, false))
                .then(Commands.literal("dry").executes(ctx -> run(ctx.getSource(), true, false)))
                .then(Commands.literal("force").executes(ctx -> run(ctx.getSource(), false, true)));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> undo() {
        return Commands.literal("undo").executes(ctx -> undo(ctx.getSource()));
    }

    private static int undo(CommandSourceStack source) {
        final ServerLevel level = source.getLevel();
        final PlacementJournal journal = PlacementJournal.get(level);
        final Optional<PlacementJournal.Transaction> undone = journal.undoLast(level);
        if (undone.isEmpty()) {
            source.sendFailure(Component.literal("Nothing to undo in this dimension."));
            return 0;
        }
        ok(source, ChatFormatting.GREEN, "Undid '" + undone.get().label() + "'.");
        ok(source, ChatFormatting.GRAY,
                journal.historySize() + " transaction(s) still undoable.");
        return 1;
    }

    private static int run(CommandSourceStack source, boolean dryRun, boolean force) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game, standing in the colony."));
            return 0;
        }

        final ServerLevel level = player.serverLevel();
        final IColony colony = IMinecoloniesAPI.getInstance().getColonyManager()
                .getIColony(level, player.blockPosition());
        if (colony == null) {
            source.sendFailure(Component.literal("You are not standing in a colony."));
            return 0;
        }
        final Rank rank = colony.getPermissions().getRank(player);
        if (rank == null || !rank.isColonyManager()) {
            source.sendFailure(Component.literal("You need colony manager rank here."));
            return 0;
        }

        // Nearest hut to the player, so you aim by standing somewhere.
        final Map<BlockPos, IBuilding> buildings = colony.getServerBuildingManager().getBuildings();
        BlockPos at = null;
        double best = Double.MAX_VALUE;
        IBuilding target = null;
        for (final Map.Entry<BlockPos, IBuilding> e : buildings.entrySet()) {
            final double d = e.getKey().distSqr(player.blockPosition());
            if (d < best) {
                best = d;
                at = e.getKey();
                target = e.getValue();
            }
        }
        if (target == null) {
            source.sendFailure(Component.literal("This colony has no buildings."));
            return 0;
        }

        final ResourceLocation typeId = target.getBuildingType().getRegistryName();
        final String hutId = typeId.getPath();
        final BuildingMapping.Conversion plan = BuildingMapping.forHut(hutId);

        ok(source, ChatFormatting.GOLD, "=== " + (dryRun ? "Dry run" : "Placing") + ": "
                + hutId + " at " + at.toShortString() + " ===");

        if (plan.anchor() == BuildingMapping.Anchor.SKIP) {
            ok(source, ChatFormatting.DARK_GRAY, "This hut is skipped by design - nothing to place.");
            return 1;
        }
        if (plan.anchor() == BuildingMapping.Anchor.POI_ONLY) {
            return placeJobSites(source, level, at, hutId, plan, dryRun);
        }
        if (plan.anchor() == BuildingMapping.Anchor.MARKER_SWAP) {
            return swapMarkers(source, level, colony, at, target, hutId, plan, dryRun, force);
        }
        if (plan.anchor() != BuildingMapping.Anchor.ROOM) {
            ok(source, ChatFormatting.YELLOW, hutId + " uses " + plan.anchor()
                    + ", which is handled by the full pipeline rather than this command.");
            return 1;
        }

        // The decision-making lives in PlacementEngine so the handover pipeline can
        // do the same work for every building without a player or a chat window.
        // This command's job is now to aim it and to narrate the result.
        final PlacementEngine.Report report = PlacementEngine.run(
                level, hutId, at, player.blockPosition(), dryRun);

        if (report.seed() != null) {
            ok(source, ChatFormatting.GRAY, "Scanning from " + report.seed().toShortString()
                    + (report.seedFromHint() ? " (your position)" : " (found near the hut)"));
        }
        if (report.beforeResult() != null) {
            ok(source, ChatFormatting.GRAY, "Scan before: " + report.beforeResult());
        }

        switch (report.outcome()) {
            case NO_SEED -> {
                ok(source, ChatFormatting.YELLOW,
                        "No enclosed, roofed spot found to scan from. Stand inside the building"
                                + " (under its roof, on its floor) and run this again.");
                return 0;
            }
            case SCAN_REFUSED -> {
                ok(source, ChatFormatting.YELLOW, "MCA will not accept this building: "
                        + explain(report.beforeResult()));
                ok(source, ChatFormatting.GRAY,
                        "Nothing was written. Placing blocks cannot fix this - the room itself"
                                + " has to pass the scan first.");
                return 0;
            }
            case NO_MCA_TYPE -> {
                source.sendFailure(Component.literal(
                        "MCA has no building type '" + report.mcaType() + "'."));
                return 0;
            }
            case ALREADY_MATCHES -> {
                ok(source, ChatFormatting.GREEN, "Already satisfies " + report.mcaType()
                        + " - nothing to place.");
                return 1;
            }
            default -> { }
        }

        report.missing().forEach((group, count) ->
                ok(source, ChatFormatting.AQUA, "  needs " + count + "x " + group.getPath()));

        if (report.outcome() == PlacementEngine.Outcome.UNSATISFIABLE) {
            final StringBuilder sb = new StringBuilder();
            report.unsatisfiable().forEach(g ->
                    sb.append(sb.length() == 0 ? "" : ", ").append(g.getPath()));
            ok(source, ChatFormatting.YELLOW, "Nothing placed: no block in the plan satisfies "
                    + sb + ". This hut's mapping needs extending.");
            return 0;
        }

        ok(source, ChatFormatting.GRAY, report.floorCells() + " floor cell(s), "
                + report.candidates() + " usable after filtering.");

        if (report.outcome() == PlacementEngine.Outcome.WOULD_PLACE) {
            report.planned().forEach(pl -> ok(source, ChatFormatting.BLUE,
                    "  would place " + pl.block().getName().getString()
                            + " at " + pl.pos().toShortString()));
            ok(source, ChatFormatting.GRAY, "Dry run - nothing was written.");
            return 1;
        }

        switch (report.outcome()) {
            case ROLLED_BACK_SCAN -> {
                source.sendFailure(Component.literal("Placement broke the scan ("
                        + report.afterResult() + ") - rolled back, world unchanged."));
                return 0;
            }
            case ROLLED_BACK_TYPE -> {
                ok(source, ChatFormatting.YELLOW, "Still does not match " + report.mcaType()
                        + " - rolled back. The remaining shortfall is probably tag-based.");
                return 0;
            }
            case PLACED -> {
                ok(source, ChatFormatting.GRAY, "Placed " + report.placed() + " block(s).");
                ok(source, ChatFormatting.GREEN, "Now matches " + report.mcaType()
                        + ". Use /colonyhandover undo to revert.");
                return 1;
            }
            default -> {
                ok(source, ChatFormatting.YELLOW, "Nothing to do (" + report.outcome() + ").");
                return 1;
            }
        }
    }

    /**
     * Stage 2a - job-site-only huts.
     *
     * <p>No MCA building is expected here, so there is nothing to verify against:
     * the whole point is a claimable POI where the work happened. The workstation
     * goes beside the hut rather than on it, because the hut block is still
     * standing at this point in development and, in the finished pipeline, the
     * hut position is not guaranteed to be standable anyway.
     */
    private static int placeJobSites(CommandSourceStack source, ServerLevel level, BlockPos at,
                                     String hutId, BuildingMapping.Conversion plan, boolean dryRun) {
        final List<Block> wanted = new ArrayList<>();
        for (final BuildingMapping.Need need : plan.needs()) {
            final Block block = need.block();
            if (block == null) {
                continue;
            }
            for (int i = 0; i < need.count(); i++) {
                wanted.add(block);
            }
        }
        if (wanted.isEmpty()) {
            ok(source, ChatFormatting.DARK_GRAY,
                    "Nothing to place - this hut's job site needs an optional mod that is not installed.");
            return 1;
        }

        // Drop anything already present nearby, so running the command twice
        // does not stack a second workstation beside the first. The POI only
        // supports one villager anyway, so a duplicate is pure litter.
        final List<Block> needed = new ArrayList<>();
        for (final Block block : wanted) {
            if (Placer.countNearby(level, at, SITE_RADIUS, SITE_VERTICAL, block) > countOf(needed, block)) {
                continue;
            }
            needed.add(block);
        }
        if (needed.isEmpty()) {
            ok(source, ChatFormatting.GREEN,
                    "Every job site block this hut needs is already here - nothing to place.");
            return 1;
        }
        wanted.clear();
        wanted.addAll(needed);

        final List<BlockPos> sites = Placer.nearbySites(level, at, SITE_RADIUS, SITE_VERTICAL);
        ok(source, ChatFormatting.GRAY, sites.size() + " standable site(s) within "
                + SITE_RADIUS + " blocks.");
        if (sites.size() < wanted.size()) {
            ok(source, ChatFormatting.YELLOW, "Only " + sites.size() + " site(s) for "
                    + wanted.size() + " block(s) - the rest will be skipped.");
        }

        if (dryRun) {
            for (int i = 0; i < Math.min(wanted.size(), sites.size()); i++) {
                ok(source, ChatFormatting.BLUE, "  would place " + wanted.get(i).getName().getString()
                        + " at " + sites.get(i).toShortString());
            }
            ok(source, ChatFormatting.GRAY, "Dry run - nothing was written.");
            return 1;
        }

        final PlacementJournal.Transaction tx =
                PlacementJournal.begin("job site: " + hutId + " @ " + at.toShortString());
        int placed = 0;
        for (int i = 0; i < wanted.size() && i < sites.size(); i++) {
            if (Placer.place(level, tx, sites.get(i), wanted.get(i))) {
                placed++;
            }
        }
        if (placed == 0) {
            tx.restore(level);
            source.sendFailure(Component.literal("Could not place anything here - world unchanged."));
            return 0;
        }
        PlacementJournal.get(level).commit(tx);
        ok(source, ChatFormatting.GREEN, "Placed " + placed + " job site block(s). "
                + "Use /colonyhandover undo to revert.");
        return 1;
    }

    /**
     * Stage 2b - marker swaps.
     *
     * <p>Replaces leftover MineColonies markers with a workstation in place.
     * Scarecrows and plantation fields are two blocks tall, so the upper half is
     * cleared first: setting the lower half alone would let the block break
     * itself and drop an item we never recorded.
     *
     * <p>The search is a bounded box around the hut, which is a simplification -
     * a farm's fields can sit further out than this. It reports what it found so
     * the limit is visible rather than silent.
     */
    private static int swapMarkers(CommandSourceStack source, ServerLevel level, IColony colony,
                                   BlockPos at, IBuilding target, String hutId,
                                   BuildingMapping.Conversion plan, boolean dryRun, boolean force) {
        final Block marker = plan.marker() == null ? null : plan.marker().get();
        final Block replacement = plan.needs().isEmpty() ? null : plan.needs().get(0).block();
        if (marker == null || replacement == null) {
            source.sendFailure(Component.literal("Marker or replacement block is not registered."));
            return 0;
        }

        // Ask the colony where its fields are rather than searching a box around
        // the hut. A field's scarecrow can sit anywhere in colony range and its
        // radius reaches up to FarmField.MAX_RANGE (20) per direction, so no
        // fixed search box is correct. This also catches fields that are not
        // currently assigned to a worker, which a per-building lookup would miss.
        final List<BlockPos> lower = new ArrayList<>();
        for (final IBuildingExtension extension
                : colony.getServerBuildingManager().getBuildingExtensions(e -> true)) {
            final BlockPos pos = extension.getPosition();
            // Filtering on the block itself keeps farm fields and plantation
            // fields apart without naming either registry, and guarantees we
            // only ever swap a position that really holds the marker.
            if (level.getBlockState(pos).is(marker)
                    && !level.getBlockState(pos.below()).is(marker)) {
                lower.add(pos.immutable());
            }
        }

        // Max fields is the hut level (FarmerFieldsModule.getMaxExtensionCount),
        // so this is a real cross-check rather than a guess.
        ok(source, ChatFormatting.GRAY, "Colony reports " + lower.size() + " "
                + marker.getName().getString() + " (hut is level " + target.getBuildingLevel()
                + ", so at most " + target.getBuildingLevel() + " field(s) can be assigned).");
        if (lower.isEmpty()) {
            ok(source, ChatFormatting.YELLOW,
                    "The colony has no fields registered with a " + marker.getName().getString()
                            + " still standing.");
            return 1;
        }

        if (dryRun) {
            for (final BlockPos pos : lower) {
                ok(source, ChatFormatting.BLUE, "  would swap " + pos.toShortString()
                        + " -> " + replacement.getName().getString());
            }
            ok(source, ChatFormatting.GRAY, "Dry run - nothing was written.");
            return 1;
        }

        // Every position here came from the colony's own extension registry, so
        // every one of them is a LIVE field. Replacing the marker makes
        // FarmField.isValidPlacement false, and RegisteredStructureManager drops
        // the field permanently on its next sanity sweep. Undo restores the
        // block but cannot re-register the field, its radii or its assignment -
        // a block journal cannot reach inside another mod's data model.
        //
        // In the real pipeline this runs AFTER deleteColony, when there is no
        // registry left to damage. Doing it now is simply out of order.
        if (!force) {
            source.sendFailure(Component.literal(
                    "Refusing: all " + lower.size() + " marker(s) are live colony fields."));
            ok(source, ChatFormatting.YELLOW,
                    "Swapping now would make MineColonies deregister them, and undo cannot put a"
                            + " field registration back - only the block.");
            ok(source, ChatFormatting.GRAY,
                    "This step belongs after colony deletion. Use /colonyhandover place force"
                            + " only on a colony you are willing to lose the fields from.");
            return 0;
        }
        ok(source, ChatFormatting.YELLOW,
                "Forced. These fields will be deregistered and undo will not bring them back.");

        final PlacementJournal.Transaction tx =
                PlacementJournal.begin("marker swap: " + hutId + " @ " + at.toShortString());
        int swapped = 0;
        for (final BlockPos pos : lower) {
            // Clear the upper half first so the lower one cannot break it for us.
            final BlockPos above = pos.above();
            if (level.getBlockState(above).is(marker)) {
                tx.capture(level, above);
                level.setBlock(above, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
            if (Placer.place(level, tx, pos, replacement)) {
                swapped++;
            }
        }
        if (swapped == 0) {
            tx.restore(level);
            source.sendFailure(Component.literal("Could not swap anything - world unchanged."));
            return 0;
        }
        PlacementJournal.get(level).commit(tx);
        ok(source, ChatFormatting.GREEN, "Swapped " + swapped + " marker(s) for "
                + replacement.getName().getString() + ". Use /colonyhandover undo to revert.");
        return 1;
    }

    private static int countOf(List<Block> list, Block block) {
        int n = 0;
        for (final Block b : list) {
            if (b == block) {
                n++;
            }
        }
        return n;
    }

    /** Plain-English reading of a failed scan, so the next step is obvious. */
    private static String explain(Building.validationResult result) {
        return switch (result) {
            case TOO_SMALL -> "the scanned envelope is 32 or smaller. MCA needs a 2x2 floor with"
                    + " 3 blocks of headroom at minimum, and counts the interior plus its shell.";
            case NO_DOOR -> "no door was reached. MCA enters a building through a door block;"
                    + " a doorway with no actual door does not count.";
            case OVERLAP -> "another registered building already claims this space.";
            case BLOCK_LIMIT -> "the scan hit maxBuildingSize before finishing - the interior"
                    + " connects to somewhere much larger.";
            case SIZE_LIMIT -> "the scan reached maxBuildingRadius from the source block.";
            default -> "reason: " + result + ".";
        };
    }

    /**
     * Does this block satisfy that requirement group?
     *
     * <p>{@code BuildingType} keys a group by the block id for a plain
     * requirement, and by the <em>tag</em> id - with the leading '#' removed -
     * for a tag requirement. So both have to be tried.
     */

    private static void ok(CommandSourceStack source, ChatFormatting colour, String text) {
        final Component line = Component.literal(text).withStyle(colour);
        source.sendSuccess(() -> line, false);
    }

    private PlaceCommand() {}
}
