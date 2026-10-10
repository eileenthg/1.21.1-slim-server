package com.github.eileenthg.colonyhandover.command;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import com.github.eileenthg.colonyhandover.mapping.BuildingMapping;
import com.github.eileenthg.colonyhandover.place.Enclosure;
import com.github.eileenthg.colonyhandover.place.PlacementEngine;
import com.github.eileenthg.colonyhandover.place.Placer;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Rank;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /colonyhandover readiness} - what the colonists think of their homes.
 *
 * <h2>Why this reports rather than repairs</h2>
 *
 * The obvious alternative is to have the handover wall the gaps itself. That
 * would be wrong. An open-sided market stall, a colonnade, a shed with no fourth
 * wall - these are usually deliberate, and a mod that silently bricks them up has
 * damaged the thing the player built. The player knows which openings are
 * architecture and which are oversights; we do not.
 *
 * <h2>Drafty is a problem; off-map is not</h2>
 *
 * Two different things get confused easily. A hut we meant to turn into an MCA
 * building, which leaks to the outdoors, is worth fixing. A field, a mine head or
 * a guard tower was never going to be a building at all - it still takes part,
 * it simply never appears on the village map. Listing the second as a problem
 * sends the player out to wall up a quarry.
 *
 * <p>Guard towers are the clearest case for not over-explaining: their knights
 * and rangers become MCA {@code GUARD} and {@code ARCHER}, which are in
 * {@code IS_IMPORTANT} and need no workstation at all. Nothing is missing there,
 * so the report says only what is true - it will not be on the map.
 */
public final class ReadinessCommand {

    /** How far from the hut block to hunt for somewhere to stand and look around. */
    private static final int SEED_RADIUS = 8;
    private static final int SEED_VERTICAL = 6;

    /** Beyond this many entries, chat gets a count and the log gets the list. */
    private static final int CHAT_LIMIT = 15;

    private enum State {
        /** Enclosed, and will register as an MCA building. */
        READY,
        /** Meant to be a building, but the survey walks out of it. */
        DRAFTY,
        /**
         * Takes part, but never becomes an MCA building.
         *
         * <p>Covers job-site huts, marker swaps and the ones we place nothing
         * for. Deliberately not split further: from the player's side the only
         * thing that differs is whether it appears on the map, and calling a
         * guard tower a "job site" would be wrong - its guards are MCA
         * professions that need no workstation at all.
         */
        OFF_MAP,
        /** Registers as a village anchor - town centre, graveyard. */
        ANCHOR,
        /**
         * Sealed, but still short of what its MCA type asks for.
         *
         * <p>The handover places job sites only; everything else a building type
         * wants is the player's to arrange. This is the state that tells them what,
         * and it is deliberately not a failure - a colony can be handed over with
         * every building like this. It just converts fewer of them.
         */
        NEEDS_BLOCKS
    }

    private record Verdict(String hutId, BlockPos at, State state, Enclosure.Leak leak,
                           boolean noInterior, String mcaType,
                           Map<ResourceLocation, Integer> missing, int roomFor) {

        static Verdict of(String hutId, BlockPos at, State state) {
            return new Verdict(hutId, at, state, null, false, null, Map.of(), 0);
        }

        static Verdict drafty(String hutId, BlockPos at, Enclosure.Leak leak, boolean none) {
            return new Verdict(hutId, at, State.DRAFTY, leak, none, null, Map.of(), 0);
        }
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("readiness").executes(ctx -> run(ctx.getSource()));
    }

    private static int run(CommandSourceStack source) {
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

        final List<Component> chat = new ArrayList<>();
        final List<String> log = new ArrayList<>();
        final List<Verdict> verdicts = new ArrayList<>();
        for (final Map.Entry<BlockPos, IBuilding> e
                : colony.getServerBuildingManager().getBuildings().entrySet()) {
            verdicts.add(inspect(level, e.getKey(), e.getValue()));
        }

        int ready = 0;
        int drafty = 0;
        int offMapCount = 0;
        int anchors = 0;
        int needsBlocks = 0;
        for (final Verdict v : verdicts) {
            switch (v.state()) {
                case READY -> ready++;
                case DRAFTY -> drafty++;
                case OFF_MAP -> offMapCount++;
                case ANCHOR -> anchors++;
                case NEEDS_BLOCKS -> needsBlocks++;
            }
        }
        // A building short of blocks still takes part - its workstation is placed
        // and its people convert. It simply will not register as its MCA type until
        // somebody furnishes it, so it is counted in, but not on the map.
        final int takingPart = ready + offMapCount + anchors + needsBlocks;
        final int total = verdicts.size();

        add(chat, log, ChatFormatting.GOLD,
                "=== " + colony.getName() + " looks over its homes ===");

        if (drafty == 0) {
            add(chat, log, ChatFormatting.GREEN,
                    "\"Every roof is sound and every door shuts. We are ready to stand on our own.\"");
        } else {
            add(chat, log, ChatFormatting.YELLOW,
                    "\"Some of our buildings feel awfully drafty. Open walls, open ceilings — the"
                            + " sort of gap a hostile could wander straight through.\"");
            add(chat, log, ChatFormatting.YELLOW,
                    "\"We would feel safer managing on our own if you fortified them first.\"");
        }

        // --- the actionable list ---------------------------------------------
        // Grouped by the gap, not by the hut. A MineColonies Warehouse holds
        // half a dozen Courier huts, each its own IBuilding, all sharing one
        // doorway - listing them separately turns six identical lines into what
        // looks like six problems. It is one hole, and sealing it fixes all six.
        if (drafty > 0) {
            final Map<String, List<Verdict>> byGap = new LinkedHashMap<>();
            for (final Verdict v : verdicts) {
                if (v.state() == State.DRAFTY) {
                    byGap.computeIfAbsent(gapKey(v), k -> new ArrayList<>()).add(v);
                }
            }

            add(chat, log, ChatFormatting.WHITE, "");
            add(chat, log, ChatFormatting.AQUA, "Open to the outside — " + byGap.size()
                    + " gap(s) across " + drafty + " building(s):");

            int shown = 0;
            for (final Map.Entry<String, List<Verdict>> e : byGap.entrySet()) {
                final List<Verdict> sharing = e.getValue();
                final Verdict first = sharing.get(0);
                final StringBuilder who = new StringBuilder();
                sharing.stream().map(Verdict::hutId).distinct()
                        .forEach(h -> who.append(who.length() == 0 ? "" : ", ").append(h));
                final String line = "  - " + describe(first)
                        + "  (" + who + (sharing.size() > 1 ? ", " + sharing.size() + " huts" : "")
                        + ")";
                if (shown < CHAT_LIMIT) {
                    chat.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
                    shown++;
                } else if (shown == CHAT_LIMIT) {
                    chat.add(Component.literal("  ... and " + (byGap.size() - CHAT_LIMIT)
                            + " more - full list in the server log.")
                            .withStyle(ChatFormatting.GRAY));
                    shown++;
                }
                log.add(line);
            }
            footnote(chat, log,
                    "Only a door, fence gate or trapdoor seals an opening. Banners, archways and"
                            + " windows two blocks tall do not — they have no collision, so the"
                            + " survey walks straight through them.");
        }

        // --- what the player still has to build ------------------------------
        // The handover places job sites and nothing else, so this is the list of
        // everything MCA wants that we deliberately leave alone. Sealed rooms only:
        // a drafty building has a more basic problem and saying "also needs a
        // lectern" would bury it.
        if (needsBlocks > 0) {
            add(chat, log, ChatFormatting.WHITE, "");
            add(chat, log, ChatFormatting.AQUA, "Sealed, but not yet furnished — "
                    + needsBlocks + " building(s):");
            add(chat, log, ChatFormatting.GRAY,
                    "The handover places workstations only. Add these yourself and the building"
                            + " registers as its MCA type; leave them and it stays a plain house.");

            int shown = 0;
            for (final Verdict v : verdicts) {
                if (v.state() != State.NEEDS_BLOCKS) {
                    continue;
                }
                final StringBuilder sb = new StringBuilder();
                v.missing().forEach((group, count) -> sb
                        .append(sb.length() == 0 ? "" : ", ")
                        .append(count).append("x ").append(group.getPath()));
                final String line = "  - " + v.hutId() + " at " + v.at().toShortString()
                        + " -> " + v.mcaType() + ": " + sb;
                if (shown < CHAT_LIMIT) {
                    chat.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
                    shown++;
                } else if (shown == CHAT_LIMIT) {
                    chat.add(Component.literal("  ... and " + (needsBlocks - CHAT_LIMIT)
                            + " more - full list in the server log.")
                            .withStyle(ChatFormatting.GRAY));
                    shown++;
                }
                log.add(line);

                // Room is the other half of the answer. Knowing a building wants
                // four chests is no use if there is space for one - that is a
                // "clear the floor" problem, not a "fetch more chests" problem.
                final int wanted = v.missing().values().stream().mapToInt(Integer::intValue).sum();
                if (v.roomFor() < wanted) {
                    final String tight = "      only room for " + v.roomFor() + " of " + wanted
                            + " - clear floor space or make the room bigger.";
                    if (shown <= CHAT_LIMIT) {
                        chat.add(Component.literal(tight).withStyle(ChatFormatting.RED));
                    }
                    log.add(tight);
                }
            }
            footnote(chat, log,
                    "Placement needs a clear floor cell that is not a doorway, not a corridor and"
                            + " has headroom. A bed needs two side by side.");
        }

        // --- the map caveat ---------------------------------------------------
        final Map<String, Integer> offMap = new LinkedHashMap<>();
        for (final Verdict v : verdicts) {
            if (v.state() == State.OFF_MAP) {
                offMap.merge(v.hutId(), 1, Integer::sum);
            }
        }
        if (!offMap.isEmpty()) {
            add(chat, log, ChatFormatting.WHITE, "");
            add(chat, log, ChatFormatting.GRAY,
                    "Note: the following buildings take part in the handover but will not appear"
                            + " on the village map afterwards, because MCA only registers enclosed"
                            + " buildings.");
            final StringBuilder sb = new StringBuilder();
            offMap.forEach((hut, n) -> {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(hut).append(n > 1 ? " x" + n : "");
            });
            add(chat, log, ChatFormatting.DARK_GRAY, "  " + sb);
        }

        // --- the bottom line --------------------------------------------------
        add(chat, log, ChatFormatting.WHITE, "");
        add(chat, log, ChatFormatting.GOLD, takingPart + " of " + total
                + " building(s) will take part in the handover, "
                + (ready + anchors) + " of them shown on the village map.");

        chat.forEach(line -> source.sendSuccess(() -> line, false));
        log.forEach(ColonyHandover.LOGGER::info);
        return 1;
    }

    /** Identity of the hole, so huts sharing one are reported together. */
    private static String gapKey(Verdict v) {
        if (v.noInterior()) {
            return "none:" + v.at().toShortString();
        }
        return v.leak().escape().toShortString() + "|" + v.leak().crossing().toShortString();
    }

    /** The specific complaint, in terms of a block the player can walk to. */
    private static String describe(Verdict v) {
        if (v.noInterior()) {
            return "inaccessible or too small - no gap to point at";
        }
        final Enclosure.Leak leak = v.leak();
        return leak.ceiling()
                ? "open sky above " + leak.escape().toShortString()
                : "a gap at " + leak.crossing().toShortString()
                        + " leads out to " + leak.escape().toShortString();
    }

    private static Verdict inspect(ServerLevel level, BlockPos at, IBuilding building) {
        final String hutId = building.getBuildingType().getRegistryName().getPath();
        final BuildingMapping.Conversion plan = BuildingMapping.forHut(hutId);

        switch (plan.anchor()) {
            case GROUPED -> {
                return Verdict.of(hutId, at, State.ANCHOR);
            }
            case POI_ONLY, MARKER_SWAP, SKIP -> {
                return Verdict.of(hutId, at, State.OFF_MAP);
            }
            default -> { /* ROOM - inspect it properly */ }
        }

        final Optional<BlockPos> seed =
                Placer.findInteriorSeed(level, at, SEED_RADIUS, SEED_VERTICAL);
        if (seed.isEmpty()) {
            // No roofed standable cell anywhere near the hut. This is the most
            // extreme drafty case, not a separate category - the building we
            // meant to convert has no indoors at all.
            return Verdict.drafty(hutId, at, null, true);
        }
        final Optional<Enclosure.Leak> leak = Enclosure.findLeak(level, seed.get());
        if (leak.isPresent()) {
            return Verdict.drafty(hutId, at, leak.get(), false);
        }

        // Sealed. Now ask the same question placement would ask, without writing:
        // does this room already satisfy its MCA type, and if not, what is short?
        // A dry run answers both, and because it shares its planner with the real
        // placement its "would fit" is the same "would fit" (beds included).
        final PlacementEngine.Report fit =
                PlacementEngine.run(level, hutId, at, null, true);
        return switch (fit.outcome()) {
            case ALREADY_MATCHES -> Verdict.of(hutId, at, State.READY);
            case WOULD_PLACE, UNSATISFIABLE -> new Verdict(hutId, at, State.NEEDS_BLOCKS,
                    null, false, fit.mcaType(), fit.missing(), fit.planned().size());
            // Anything else means the scan disagreed with our own enclosure walk.
            // Trust the scan - it is the one MCA will use.
            // MCA refused a room our own walk found sealed, so there is no leak to
            // report. It must be built as "no interior" - gapKey and describe both
            // assume a drafty verdict has either a leak or that flag, and building it
            // with neither is what crashed this command.
            default -> Verdict.drafty(hutId, at, null, true);
        };
    }

    /**
     * An aside about the mechanics rather than about this colony.
     *
     * <p>Dimmed and italic so it reads as a footnote: the player scanning the
     * list wants the building names, and only wants the rule underneath them
     * once.
     */
    private static void footnote(List<Component> chat, List<String> log, String text) {
        chat.add(Component.literal(text)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        log.add(text);
    }

    private static void add(List<Component> chat, List<String> log,
                            ChatFormatting colour, String text) {
        chat.add(Component.literal(text).withStyle(colour));
        if (!text.isEmpty()) {
            log.add(text);
        }
    }

    private ReadinessCommand() {}
}
