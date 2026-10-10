package com.github.eileenthg.colonyhandover.command;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import com.github.eileenthg.colonyhandover.handover.ColonySnapshot;
import com.github.eileenthg.colonyhandover.handover.ParentReport;
import com.github.eileenthg.colonyhandover.mapping.BuildingMapping;
import com.github.eileenthg.colonyhandover.mapping.JobMapping;
import com.github.eileenthg.colonyhandover.mapping.OptionalMods;
import com.github.eileenthg.colonyhandover.mapping.ProfessionPoi;
import com.github.eileenthg.colonyhandover.place.Placer;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Rank;
import net.conczin.mca.resources.BuildingTypes;
import net.conczin.mca.resources.data.BuildingType;
import net.conczin.mca.server.world.data.Building;
import net.conczin.mca.server.world.data.BuildingScanResult;
import net.conczin.mca.server.world.data.VillageManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /colonyhandover preview} - a dry run of the handover.
 *
 * <p>This command is strictly read-only. It never writes a block, never touches
 * colony data, and never spawns or removes an entity. Its whole job is to answer
 * "what would happen if I signed the charter here?" against a real colony, so
 * that the mapping tables get tested on actual buildings rather than on
 * assumptions, while everything is still reversible.
 *
 * <p>Detail goes to chat up to {@link #CHAT_BUILDING_LIMIT} buildings; the full
 * listing always goes to the server log, since a mature colony can easily have
 * more buildings than fit comfortably in the chat window.
 */
public final class PreviewCommand {

    /** Beyond this many buildings, chat gets a summary and the log gets the rest. */
    private static final int CHAT_BUILDING_LIMIT = 25;

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("colonyhandover")
                .then(Commands.literal("preview")
                        // Permission level 0: the real gate is the colony rank
                        // check below, which is what MineColonies' own commands
                        // use. A player with no rank in this colony gets told so.
                        .executes(ctx -> run(ctx.getSource())));
    }

    private static int run(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                    "This command needs a player position - run it in game, standing in the colony."));
            return 0;
        }

        final BlockPos pos = player.blockPosition();
        final IColony colony = IMinecoloniesAPI.getInstance().getColonyManager()
                .getIColony(player.level(), pos);

        if (colony == null) {
            source.sendFailure(Component.literal("You are not standing in a colony."));
            return 0;
        }

        // Same gate CommandSetAbandoned uses: you must be able to manage the
        // colony to see (and later, to sign away) its future.
        final Rank rank = colony.getPermissions().getRank(player);
        if (rank == null || !rank.isColonyManager()) {
            source.sendFailure(Component.literal(
                    "You need colony manager rank in " + colony.getName() + " to preview a handover."));
            return 0;
        }

        final List<Component> chat = new ArrayList<>();
        final List<String> log = new ArrayList<>();

        header(chat, log, colony);
        final Counts counts = previewBuildings(chat, log, colony, player.serverLevel());
        final Map<VillagerProfession, Integer> headcount = previewCitizens(chat, log, colony);
        previewWorkstations(chat, log, headcount, counts.provided());
        previewParents(chat, log, colony);
        footer(chat, log, counts);

        // sendSuccess takes a Supplier so the component is only built if it is
        // actually going to be shown. false = do not broadcast to other ops.
        chat.forEach(line -> source.sendSuccess(() -> line, false));
        log.forEach(ColonyHandover.LOGGER::info);
        return 1;
    }

    private record Counts(int converted, int jobSites, int skipped, int atRisk,
                          Map<Block, Integer> provided) {}

    private static void header(List<Component> chat, List<String> log, IColony colony) {
        add(chat, log, ChatFormatting.GOLD, "=== Handover preview: " + colony.getName()
                + " (colony #" + colony.getID() + ") ===");
        add(chat, log, ChatFormatting.GRAY, "Nothing below has happened. This is a dry run.");
        previewOptionalMods(chat, log);
    }

    /**
     * Reports which optional integrations are active.
     *
     * <p>These change the outcome of a handover, so they belong in the preview
     * rather than being discovered afterwards. Presence is reported from the
     * <em>registry</em>, not the mod list, because a mod can be installed with
     * the relevant feature switched off - Friends&amp;Foes gates its beekeeper
     * profession behind a config toggle.
     */
    private static void previewOptionalMods(List<Component> chat, List<String> log) {
        add(chat, log, ChatFormatting.WHITE, "");
        add(chat, log, ChatFormatting.AQUA, "Optional integrations:");

        final boolean chef = OptionalMods.chef().isPresent();
        final boolean cook = OptionalMods.cook().isPresent();
        if (chef || cook) {
            // The professions exist; check their job sites do too.
            final boolean pots = OptionalMods.cookingPot().isPresent()
                    && OptionalMods.skillet().isPresent();
            add(chat, log, pots ? ChatFormatting.GREEN : ChatFormatting.YELLOW,
                    "  - Chef's Delight: chef/cook professions available"
                            + (pots
                            ? " (cooking pot + skillet job sites present)"
                            : "  <- Farmer's Delight missing, so they have no job site"));
        } else {
            add(chat, log, ChatFormatting.DARK_GRAY,
                    "  - Chef's Delight: absent - chef, cook and cook assistant all become butchers");
        }

        if (OptionalMods.beekeeper().isPresent()) {
            add(chat, log, ChatFormatting.GREEN,
                    "  - Friends&Foes: beekeeper profession available (beehive job site)");
        } else if (OptionalMods.isLoaded(OptionalMods.FRIENDS_AND_FOES)) {
            add(chat, log, ChatFormatting.YELLOW,
                    "  - Friends&Foes: installed but the beekeeper profession is not registered"
                            + "  <- check its config toggle; beekeepers become farmers");
        } else {
            add(chat, log, ChatFormatting.DARK_GRAY,
                    "  - Friends&Foes: absent - beekeepers become farmers");
        }

        if (OptionalMods.isLoaded(OptionalMods.BEEKEEPER_HUT)) {
            add(chat, log, ChatFormatting.GRAY,
                    "  - Beekeeper Hut: present, but it only adds worldgen -"
                            + " no profession or job site of its own");
        }
    }

    private static Counts previewBuildings(List<Component> chat, List<String> log,
                                           IColony colony, ServerLevel level) {
        final Map<BlockPos, IBuilding> buildings = colony.getServerBuildingManager().getBuildings();

        int converted = 0;
        int jobSites = 0;
        int skipped = 0;
        int atRisk = 0;
        int shown = 0;
        // Every workstation block this plan would place, so we can check it
        // against how many villagers actually need one.
        final Map<Block, Integer> provided = new LinkedHashMap<>();

        add(chat, log, ChatFormatting.WHITE, "");
        add(chat, log, ChatFormatting.AQUA, "Buildings (" + buildings.size() + "):");

        for (final Map.Entry<BlockPos, IBuilding> entry : buildings.entrySet()) {
            final IBuilding building = entry.getValue();
            final ResourceLocation typeId = building.getBuildingType().getRegistryName();
            final String hutId = typeId.getPath();
            final BuildingMapping.Conversion plan = BuildingMapping.forHut(hutId);
            final BlockPos at = entry.getKey();

            final String where = at.getX() + "," + at.getY() + "," + at.getZ();
            final String lvl = "lvl " + building.getBuildingLevel();

            final String line;
            final ChatFormatting colour;

            // Both lists end up as real blocks in the world, so both count
            // toward job-site supply. Null means the block belongs to a mod
            // that is not installed, and nothing gets placed.
            for (final BuildingMapping.Need need : plan.needs()) {
                final Block b = need.block();
                if (b != null) {
                    provided.merge(b, need.count(), Integer::sum);
                }
            }
            for (final BuildingMapping.Need need : plan.extraPoi()) {
                final Block b = need.block();
                if (b != null) {
                    provided.merge(b, need.count(), Integer::sum);
                }
            }

            if (plan.anchor() == BuildingMapping.Anchor.SKIP) {
                skipped++;
                colour = ChatFormatting.DARK_GRAY;
                line = "  - " + hutId + " (" + lvl + ") at " + where
                        + " -> skipped, stays a plain building";
            } else if (plan.anchor() == BuildingMapping.Anchor.POI_ONLY) {
                jobSites++;
                final String what = describeNeeds(plan);
                colour = what.isEmpty() ? ChatFormatting.DARK_GRAY : ChatFormatting.BLUE;
                line = "  - " + hutId + " (" + lvl + ") at " + where
                        + (what.isEmpty()
                        ? " -> nothing to place (needs an optional mod)"
                        : " -> job site only: " + what);
            } else if (plan.anchor() == BuildingMapping.Anchor.MARKER_SWAP) {
                jobSites++;
                // Not a building conversion - counted separately so the summary
                // does not claim we typed something we did not.
                colour = ChatFormatting.BLUE;
                line = "  - " + hutId + " (" + lvl + ") at " + where
                        + " -> swap marker blocks for " + describeNeeds(plan)
                        + " (job site only, no MCA building)";
            } else {
                converted++;
                // Ask MCA what is already there rather than assuming the hut is
                // empty; most schematics have a head start on their target type.
                final String needs = plan.needs().isEmpty()
                        ? "no blocks needed"
                        : describeShortfall(shortfall(level, at, plan.mcaType()));
                final boolean risky = BuildingMapping.losesToHouse(plan.mcaType());
                if (risky) {
                    atRisk++;
                }
                final String extra = plan.extraPoi().isEmpty() ? "" : describeExtra(plan);
                colour = risky ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
                line = "  - " + hutId + " (" + lvl + ") at " + where
                        + " -> " + plan.mcaType() + extra
                        + (plan.forceable() ? " [forceable]" : "")
                        + "; place " + needs
                        + (risky ? "  <- a bed here would make this a house instead" : "");
            }

            // Chat gets the first N; the log always gets everything.
            if (shown < CHAT_BUILDING_LIMIT) {
                chat.add(Component.literal(line).withStyle(colour));
                shown++;
            } else if (shown == CHAT_BUILDING_LIMIT) {
                chat.add(Component.literal("  ... and " + (buildings.size() - CHAT_BUILDING_LIMIT)
                        + " more - full list in the server log.").withStyle(ChatFormatting.GRAY));
                shown++;
            }
            log.add(line);
        }

        return new Counts(converted, jobSites, skipped, atRisk, provided);
    }

    /**
     * Asks MCA what a building already satisfies, and returns only the shortfall.
     *
     * <p>Most MineColonies schematics already contain much of what the target
     * type wants - a Library ships with bookshelves - so placing the full
     * requirement blindly would be both wasteful and destructive. This runs
     * MCA's own scanner and subtracts what it finds.
     *
     * <p>Using the real scan matters for a second, subtler reason: only blocks
     * the flood fill actually visits are counted, which is the shell touching
     * the interior plus the interior itself. A bookshelf buried two layers deep
     * in a wall exists but does not count. Rather than trying to replicate that
     * reachability rule, we let MCA answer.
     *
     * @return remaining requirement per group, or null if the building cannot be
     *         scanned at all (in which case the shortfall question is moot)
     */
    private static Map<ResourceLocation, Integer> shortfall(ServerLevel level, BlockPos pos,
                                                            String targetType) {
        // Seed from inside the building, never from the hut block itself. MCA
        // requires a walkable anchor and a hut block is solid, so scanning from
        // it always fails TOO_SMALL regardless of how big the building is.
        final BlockPos seed = Placer.findInteriorSeed(level, pos, 8, 6).orElse(null);
        if (seed == null) {
            return null;
        }
        // analyzeRoomAddition tries an already-registered room first and falls
        // back to scanning a brand-new structure. Both paths are read-only -
        // they build a throwaway scan result and commit nothing.
        final BuildingScanResult scan = VillageManager.get(level).analyzeRoomAddition(seed);
        if (scan.result() != Building.validationResult.SUCCESS) {
            return null;
        }

        final BuildingType type = BuildingTypes.getInstance().getBuildingType(targetType);
        if (type == null) {
            return null;
        }

        // getGroups(blocks) folds the scanned blocks into the same groups the
        // requirements are keyed by, resolving tags on the way.
        final Map<ResourceLocation, List<BlockPos>> have = type.getGroups(scan.building().getBlocks());
        final Map<ResourceLocation, Integer> missing = new LinkedHashMap<>();

        type.getGroups().forEach((group, required) -> {
            final int present = have.containsKey(group) ? have.get(group).size() : 0;
            if (present < required) {
                missing.put(group, required - present);
            }
        });
        return missing;
    }

    /** Renders a shortfall map, or a note explaining why there is nothing to render. */
    private static String describeShortfall(Map<ResourceLocation, Integer> missing) {
        if (missing == null) {
            return "no scannable room found from the hut block";
        }
        if (missing.isEmpty()) {
            return "already satisfied - nothing to place";
        }
        final StringBuilder sb = new StringBuilder();
        missing.forEach((group, count) -> {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(count).append("x ").append(group.getPath());
        });
        return "still needs " + sb;
    }

    /** Extra job sites, omitted entirely when their mod is not installed. */
    private static String describeExtra(BuildingMapping.Conversion plan) {
        final StringBuilder sb = new StringBuilder();
        for (final BuildingMapping.Need need : plan.extraPoi()) {
            final Block b = need.block();
            if (b == null) {
                continue;
            }
            sb.append(sb.length() == 0 ? " + job site: " : ", ")
              .append(b.getName().getString());
        }
        return sb.toString();
    }

    private static String describeNeeds(BuildingMapping.Conversion plan) {
        final StringBuilder sb = new StringBuilder();
        for (final BuildingMapping.Need need : plan.needs()) {
            final Block b = need.block();
            if (b == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(b.getName().getString());
            if (need.count() > 1) {
                sb.append(" x").append(need.count());
            }
        }
        return sb.toString();
    }

    private static Map<VillagerProfession, Integer> previewCitizens(
            List<Component> chat, List<String> log, IColony colony) {
        final List<ICitizenData> citizens = colony.getCitizenManager().getCitizens();
        final Map<VillagerProfession, Integer> headcount = new LinkedHashMap<>();

        add(chat, log, ChatFormatting.WHITE, "");
        add(chat, log, ChatFormatting.AQUA, "Citizens (" + citizens.size() + "):");

        // Group by resulting profession rather than listing every villager - a
        // big colony has dozens, and the useful question is "does the workforce
        // come out looking sane", not "what happens to citizen #37".
        final Map<String, Integer> byProfession = new LinkedHashMap<>();
        int unmapped = 0;

        for (final ICitizenData citizen : citizens) {
            final ResourceLocation jobId = citizen.getJob() == null
                    ? null
                    : citizen.getJob().getJobRegistryEntry().getKey();

            final VillagerProfession profession = jobId == null
                    ? VillagerProfession.NONE
                    : JobMapping.forJob(jobId);

            if (jobId != null && !JobMapping.isKnown(jobId)) {
                unmapped++;
                log.add("  ! unmapped job " + jobId + " on " + citizen.getName()
                        + " - will become an unemployed villager");
            }

            final String name = professionName(profession);
            byProfession.merge(name, 1, Integer::sum);
            headcount.merge(profession, 1, Integer::sum);

            log.add("  - " + citizen.getName()
                    + " (" + (citizen.isFemale() ? "female" : "male") + ", id " + citizen.getId() + ")"
                    + " job " + (jobId == null ? "unemployed" : jobId.getPath())
                    + " -> " + name);
        }

        byProfession.forEach((prof, count) ->
                chat.add(Component.literal("  - " + count + "x " + prof).withStyle(ChatFormatting.GREEN)));

        if (unmapped > 0) {
            add(chat, log, ChatFormatting.YELLOW,
                    "  " + unmapped + " citizen(s) have jobs with no mapping - they become unemployed"
                            + " villagers and will claim a workstation on their own.");
        }

        return headcount;
    }

    /**
     * Compares workstations needed against workstations the plan would place.
     *
     * <p>This is the section that answers "will everyone keep their job?".
     * Each POI has {@code maxTickets = 1}, so it is one block per villager, and
     * a villager with a vanilla profession and no workstation gets reset to
     * unemployed by {@code LoseUnimportantJobTask}.
     */
    private static void previewWorkstations(List<Component> chat, List<String> log,
                                            Map<VillagerProfession, Integer> headcount,
                                            Map<Block, Integer> provided) {
        final Map<Block, Integer> needed = ProfessionPoi.required(headcount);
        if (needed.isEmpty()) {
            return;
        }

        add(chat, log, ChatFormatting.WHITE, "");
        add(chat, log, ChatFormatting.AQUA, "Workstations (1 villager per block):");

        int shortfalls = 0;
        for (final Map.Entry<Block, Integer> entry : needed.entrySet()) {
            final int have = provided.getOrDefault(entry.getKey(), 0);
            final int want = entry.getValue();
            final String name = entry.getKey().getName().getString();
            if (have >= want) {
                add(chat, log, ChatFormatting.GREEN,
                        "  - " + name + ": need " + want + ", plan places " + have);
            } else {
                shortfalls += want - have;
                add(chat, log, ChatFormatting.YELLOW,
                        "  - " + name + ": need " + want + ", plan places " + have
                                + "  <- " + (want - have) + " villager(s) would go unemployed");
            }
        }

        if (shortfalls > 0) {
            add(chat, log, ChatFormatting.YELLOW,
                    "  " + shortfalls + " villager(s) short. Either place extra workstations or"
                            + " seed them with villager XP so the job sticks.");
        }
    }

    /**
     * Which colonists have parents on record, and where each will land.
     *
     * <p>Capped for chat, because a long-running colony can have dozens and the
     * preview has other things to say. {@code /colonyhandover parents} prints the
     * lot when you want to check a specific family.
     */
    private static void previewParents(List<Component> chat, List<String> log, IColony colony) {
        add(chat, log, ChatFormatting.WHITE, "");
        add(chat, log, ChatFormatting.WHITE, "Recorded parents");
        ParentReport.render(ColonySnapshot.of(colony), ParentReport.DEFAULT_LIMIT,
                (colour, text) -> add(chat, log, colour, text));
    }

    private static String professionName(VillagerProfession profession) {
        // VillagerProfession has no stable getName() in 1.21.1; the translation
        // key is the reliable identifier and reads fine in a diagnostic.
        return profession.name();
    }

    private static void footer(List<Component> chat, List<String> log, Counts counts) {
        add(chat, log, ChatFormatting.WHITE, "");
        add(chat, log, ChatFormatting.GOLD, "Summary: " + counts.converted()
                + " building(s) would be typed, " + counts.jobSites()
                + " given a job site only, " + counts.skipped() + " left as-is.");
        if (counts.atRisk() > 0) {
            add(chat, log, ChatFormatting.YELLOW, counts.atRisk()
                    + " of those lose to 'house' if the hut contains a bed - check those in game.");
        }
        add(chat, log, ChatFormatting.GRAY,
                "Reminder: buildings still have to pass MCA's room scan (roof, door, >=32 interior).");
    }

    private static void add(List<Component> chat, List<String> log, ChatFormatting colour, String text) {
        chat.add(Component.literal(text).withStyle(colour));
        if (!text.isEmpty()) {
            log.add(text);
        }
    }

    private PreviewCommand() {}
}
