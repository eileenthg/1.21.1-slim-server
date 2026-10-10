package com.github.eileenthg.colonyhandover.command;

import com.github.eileenthg.colonyhandover.handover.ColonySnapshot;
import com.github.eileenthg.colonyhandover.handover.Handover;
import com.github.eileenthg.colonyhandover.handover.HandoverSession;
import com.github.eileenthg.colonyhandover.handover.HandoverToken;
import com.github.eileenthg.colonyhandover.handover.ParentSlots;
import com.github.eileenthg.colonyhandover.mapping.JobMapping;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.permissions.Rank;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code /colonyhandover handover dry} - what the handover would do, changing nothing.
 *
 * <h2>Why a dry run matters more here than elsewhere</h2>
 *
 * Every other command in this mod is reversible: placement is journalled, marks
 * write nothing. The handover is not. {@code deleteColony} kills the citizens and
 * drops the colony's save data, and no block journal can put that back
 * (finding #10 makes the general version of this point). The dry run is therefore
 * the only safety mechanism there is, and it has to be honest about what it
 * cannot know as well as what it can.
 */
public final class HandoverCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("handover")
                .then(Commands.literal("dry").executes(ctx -> run(ctx.getSource(), false)))
                .then(Commands.literal("debug")
                        .executes(ctx -> debugStart(ctx.getSource()))
                        .then(Commands.literal("step").executes(ctx -> debugStep(ctx.getSource())))
                        .then(Commands.literal("abort").executes(ctx -> debugAbort(ctx.getSource()))))
                .then(Commands.literal("confirm")
                        .then(Commands.argument("token", StringArgumentType.word())
                                .executes(ctx -> confirm(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "token")))))
                .executes(ctx -> run(ctx.getSource(), true));
    }

    /**
     * Resolves the colony the player is standing in, or explains why not.
     *
     * <p>Shared by both entry points so that {@code confirm} re-checks permission
     * and location rather than trusting that they held when the token was issued.
     */
    private static IColony colonyAt(CommandSourceStack source, ServerPlayer player) {
        final IColony colony = IMinecoloniesAPI.getInstance().getColonyManager()
                .getIColony(player.serverLevel(), player.blockPosition());
        if (colony == null) {
            source.sendFailure(Component.literal("You are not standing in a colony."));
            return null;
        }
        final Rank rank = colony.getPermissions().getRank(player);
        if (rank == null || !rank.isColonyManager()) {
            source.sendFailure(Component.literal("You need colony manager rank here."));
            return null;
        }
        return colony;
    }

    /**
     * Validates a token, then would run the pipeline.
     *
     * <p>The gate is deliberately shipped before the thing it guards. An
     * irreversible operation should not be the first build in which its own safety
     * check runs - the same reason placement waited for a working undo. Until the
     * pipeline lands, a correct token gets you a refusal rather than a colony.
     */
    private static int confirm(CommandSourceStack source, String token) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game, standing in the colony."));
            return 0;
        }
        final IColony colony = colonyAt(source, player);
        if (colony == null) {
            return 0;
        }

        final HandoverToken.Result result = HandoverToken.check(player.getUUID(), token,
                colony.getID(), player.serverLevel().dimension());
        switch (result) {
            case NONE_ISSUED -> {
                source.sendFailure(Component.literal(
                        "No handover is pending. Run /colonyhandover handover first."));
                return 0;
            }
            case EXPIRED -> {
                source.sendFailure(Component.literal(
                        "That token has expired. Run /colonyhandover handover again."));
                return 0;
            }
            case WRONG_TOKEN -> {
                source.sendFailure(Component.literal("That token does not match."));
                return 0;
            }
            case WRONG_COLONY -> {
                source.sendFailure(Component.literal(
                        "That token was issued for a different colony."));
                return 0;
            }
            default -> { }
        }

        // Snapshot BEFORE deletion - there is no "after" in which to read any of
        // this (finding #1).
        final ColonySnapshot snap = ColonySnapshot.of(colony);
        say(source, ChatFormatting.GREEN, "Token accepted. Handing over "
                + snap.colonyName() + "...");

        final Handover.Result outcome = Handover.run(player.serverLevel(), snap);

        say(source, ChatFormatting.GOLD, "=== Handover complete ===");
        report(source, outcome);
        return 1;
    }

    private static int run(CommandSourceStack source, boolean offerToken) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game, standing in the colony."));
            return 0;
        }
        final IColony colony = colonyAt(source, player);
        if (colony == null) {
            return 0;
        }

        final ColonySnapshot snap = ColonySnapshot.of(colony);

        say(source, ChatFormatting.GOLD, "=== Handover dry run: " + snap.colonyName() + " ===");
        say(source, ChatFormatting.GRAY, snap.huts().size() + " building(s), "
                + snap.citizens().size() + " citizen(s).");

        reportPeople(source, snap);
        reportProfessions(source, snap);
        reportFamily(source, snap);

        if (!offerToken) {
            say(source, ChatFormatting.DARK_GRAY, "Nothing was changed. This only reads.");
            return 1;
        }

        final String token = HandoverToken.issue(player.getUUID(), colony.getID(),
                player.serverLevel().dimension());
        say(source, ChatFormatting.RED,
                "This cannot be undone. The colony and its citizens are destroyed;"
                        + " no undo restores them. Back up the world first.");
        say(source, ChatFormatting.GOLD, "To go ahead, within "
                + HandoverToken.lifetimeSeconds() + " seconds run:");
        say(source, ChatFormatting.WHITE, "  /colonyhandover handover confirm " + token);
        return 1;
    }

    /** Adults, children, and anyone the spawner would have to guess a spot for. */
    private static void reportPeople(CommandSourceStack source, ColonySnapshot snap) {
        long children = snap.citizens().stream().filter(ColonySnapshot.Citizen::child).count();
        long jobless = snap.citizens().stream().filter(c -> !c.hasJob()).count();

        say(source, ChatFormatting.WHITE, "People");
        say(source, ChatFormatting.GRAY, "  " + (snap.citizens().size() - children)
                + " adult(s), " + children + " child(ren).");
        if (jobless > 0) {
            say(source, ChatFormatting.GRAY, "  " + jobless
                    + " with no job - they become unemployed villagers.");
        }
    }

    /**
     * What each job becomes, and - the part that actually bites - which of those
     * professions will survive.
     *
     * <p>Finding #9: {@code LoseUnimportantJobTask} strips any vanilla profession
     * from a villager with no claimed job site, no XP and level <= 1. So a
     * profession we assign is only real if a matching workstation exists for it,
     * or we seed XP. Counting professions without counting workstations would
     * report a success that quietly unwinds a few minutes later.
     */
    private static void reportProfessions(CommandSourceStack source, ColonySnapshot snap) {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        int unmapped = 0;
        for (final ColonySnapshot.Citizen c : snap.citizens()) {
            if (!c.hasJob()) {
                continue;
            }
            final VillagerProfession profession = JobMapping.forJob(c.jobId());
            if (profession == VillagerProfession.NONE) {
                unmapped++;
                continue;
            }
            counts.merge(professionName(profession), 1, Integer::sum);
        }

        say(source, ChatFormatting.WHITE, "Professions");
        if (counts.isEmpty()) {
            say(source, ChatFormatting.GRAY, "  none - every citizen becomes unemployed.");
        }
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> say(source, ChatFormatting.GRAY,
                        "  " + e.getValue() + " x " + e.getKey()));
        if (unmapped > 0) {
            say(source, ChatFormatting.YELLOW, "  " + unmapped
                    + " job(s) have no vanilla equivalent - those become unemployed.");
        }
        say(source, ChatFormatting.DARK_GRAY,
                "  Professions only stick if a matching workstation exists, or we seed XP.");
    }

    /**
     * How much of the family graph will survive the crossing.
     *
     * <p>Partners, children and siblings are stored as citizen ids and map cleanly
     * once every villager has a UUID. Parents do not: MineColonies stores two
     * <em>names</em> with no father/mother ordering and no gender
     * ({@code ReproductionManager} just passes whichever two citizens bred), so a
     * parent is only placeable in MCA's tree if that name still matches exactly one
     * living citizen. This counts how many actually do.
     */
    private static void reportFamily(CommandSourceStack source, ColonySnapshot snap) {
        final Set<Integer> ids = new HashSet<>();
        snap.citizens().forEach(c -> ids.add(c.id()));

        int partners = 0;
        int danglingPartners = 0;
        int childLinks = 0;
        int danglingChildren = 0;
        int siblingLinks = 0;
        int danglingSiblings = 0;
        int parentsKnown = 0;
        int fromLiving = 0;
        int fromNames = 0;
        int sameGenderPairs = 0;
        int slotted = 0;

        for (final ColonySnapshot.Citizen c : snap.citizens()) {
            if (c.partnerId() >= 0) {
                partners++;
                if (!ids.contains(c.partnerId())) {
                    danglingPartners++;
                }
            }
            for (final int kid : c.children()) {
                childLinks++;
                if (!ids.contains(kid)) {
                    danglingChildren++;
                }
            }
            for (final int sib : c.siblings()) {
                siblingLinks++;
                if (!ids.contains(sib)) {
                    danglingSiblings++;
                }
            }
            for (final ColonySnapshot.Parent p : List.of(c.parentA(), c.parentB())) {
                if (!p.known()) {
                    continue;
                }
                parentsKnown++;
                switch (p.source()) {
                    case LIVING -> fromLiving++;
                    case NAME_FILE -> fromNames++;
                    default -> { }
                }
            }

            final ParentSlots.Slots slots = ParentSlots.assign(c.parentA(), c.parentB());
            if (slots.father() != null) {
                slotted++;
            }
            if (slots.mother() != null) {
                slotted++;
            }
            if (slots.misfiled()) {
                sameGenderPairs++;
            }
        }

        say(source, ChatFormatting.WHITE, "Family");
        say(source, ChatFormatting.GRAY, "  " + (partners / 2) + " couple(s), "
                + childLinks + " parent-child link(s), " + siblingLinks + " sibling link(s).");

        final List<String> dangling = new ArrayList<>();
        if (danglingPartners > 0) {
            dangling.add(danglingPartners + " partner");
        }
        if (danglingChildren > 0) {
            dangling.add(danglingChildren + " child");
        }
        if (danglingSiblings > 0) {
            dangling.add(danglingSiblings + " sibling");
        }
        if (!dangling.isEmpty()) {
            say(source, ChatFormatting.YELLOW, "  " + String.join(", ", dangling)
                    + " link(s) point at citizens who are gone - those get dropped.");
        }

        if (parentsKnown == 0) {
            say(source, ChatFormatting.GRAY, "  no recorded parents.");
            return;
        }

        final int lost = parentsKnown - slotted;
        say(source, ChatFormatting.GRAY, "  " + slotted + " of " + parentsKnown
                + " named parent(s) can be placed in the family tree.");
        say(source, ChatFormatting.DARK_GRAY, "    " + fromLiving
                + " from a living citizen, " + fromNames + " from the name file.");
        if (lost > 0) {
            say(source, ChatFormatting.YELLOW, "  " + lost
                    + " parent name(s) stay unknown and are left out - renamed, unisex,"
                    + " or from a name style this colony no longer uses.");
        }
        if (sameGenderPairs > 0) {
            say(source, ChatFormatting.YELLOW, "  " + sameGenderPairs
                    + " same-gender parent pair(s). MineColonies pairs citizens without"
                    + " checking gender; MCA only has a father slot and a mother slot.");
            say(source, ChatFormatting.DARK_GRAY,
                    "    Both are kept - the second takes the vacant slot. Their recorded"
                            + " gender stays correct, but MCA's family screen mislabels it.");
        }
    }

    private static String professionName(VillagerProfession profession) {
        return profession.name().toString();
    }

    /** Shared reporting, so debug and live describe the same run identically. */
    public static void report(CommandSourceStack source, Handover.Result r) {
        say(source, ChatFormatting.GRAY, "  workstations: " + r.placed().stations()
                + " placed, " + r.placed().anchors() + " village anchor(s), "
                + r.placed().swapped() + " field marker(s) swapped.");
        if (r.placed().shortOfRoom() > 0) {
            say(source, ChatFormatting.YELLOW, "    " + r.placed().shortOfRoom()
                    + " hut(s) had nowhere to stand a block - run readiness to find them.");
        }
        say(source, ChatFormatting.GRAY, "  people: " + r.spawned().spawned()
                + " spawned" + (r.spawned().failed() > 0
                        ? ", " + r.spawned().failed() + " failed" : "")
                + ", " + r.spawned().professions() + " with a profession.");
        say(source, ChatFormatting.GRAY, "  family: " + r.wired().couples() + " couple(s), "
                + r.wired().parentLinks() + " parent link(s), "
                + r.wired().deceased() + " deceased ancestor(s) recreated.");
        say(source, ChatFormatting.GRAY, "  village: " + r.scanned().adopted()
                + " building(s) handed to MCA, " + r.scanned().refused() + " refused.");
    }

    /**
     * Arms a stepped run. Nothing happens until {@code step}.
     *
     * <p>No token here. The token exists so that one mistyped command cannot
     * destroy a colony, and stepping already gives that: the destructive stage is
     * the second step, it is announced in red before you take it, and the run is
     * abortable until then.
     */
    private static int debugStart(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game, standing in the colony."));
            return 0;
        }
        final IColony colony = colonyAt(source, player);
        if (colony == null) {
            return 0;
        }

        HandoverSession.start(player.getUUID(), colony.getID());
        say(source, ChatFormatting.GOLD,
                "=== Stepped handover armed: " + colony.getName() + " ===");
        for (final HandoverSession.Stage stage : HandoverSession.Stage.values()) {
            if (stage != HandoverSession.Stage.DONE) {
                say(source, ChatFormatting.DARK_GRAY,
                        "  " + (stage.ordinal() + 1) + ". " + stage.description());
            }
        }
        say(source, ChatFormatting.WHITE,
                "Next: " + HandoverSession.Stage.SNAPSHOT.description());
        say(source, ChatFormatting.GRAY,
                "/colonyhandover handover debug step to run it,"
                        + " /colonyhandover handover debug abort to stop.");
        return 1;
    }

    private static int debugStep(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game."));
            return 0;
        }
        final HandoverSession session = HandoverSession.of(player.getUUID());
        if (session == null) {
            source.sendFailure(Component.literal(
                    "No stepped handover armed. Run /colonyhandover handover debug first."));
            return 0;
        }
        if (session.finished()) {
            say(source, ChatFormatting.GREEN, "Already finished.");
            return 1;
        }

        // The snapshot stage is the only one that needs a live colony, so it is the
        // only one that looks for one. Every later stage works from the snapshot,
        // by which time the colony is deliberately gone.
        ColonySnapshot fresh = null;
        if (session.stage() == HandoverSession.Stage.SNAPSHOT) {
            final IColony colony = colonyAt(source, player);
            if (colony == null) {
                return 0;
            }
            if (colony.getID() != session.colonyId()) {
                source.sendFailure(Component.literal(
                        "You are standing in a different colony than the one armed."));
                return 0;
            }
            fresh = ColonySnapshot.of(colony);
        }

        final HandoverSession.Stage ran = session.stage();
        final String summary = session.advance(player.serverLevel(), fresh);

        say(source, ChatFormatting.GOLD,
                "[" + (ran.ordinal() + 1) + "/6] " + ran.description());
        say(source, ChatFormatting.GRAY, "  " + summary);

        if (session.finished()) {
            say(source, ChatFormatting.GREEN, "Handover complete.");
            HandoverSession.end(player.getUUID());
            return 1;
        }

        final HandoverSession.Stage next = session.stage();
        if (next == HandoverSession.Stage.DELETE) {
            say(source, ChatFormatting.RED,
                    "The next step DELETES the colony and its citizens. This cannot be"
                            + " undone. Back up the world first, or abort now.");
        }
        say(source, ChatFormatting.WHITE, "Next: " + next.description());
        return 1;
    }

    private static int debugAbort(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game."));
            return 0;
        }
        final HandoverSession session = HandoverSession.of(player.getUUID());
        if (session == null) {
            source.sendFailure(Component.literal("Nothing to abort."));
            return 0;
        }
        HandoverSession.end(player.getUUID());
        say(source, ChatFormatting.YELLOW, "Stepped handover abandoned before: "
                + session.stage().description() + ".");
        say(source, ChatFormatting.GRAY,
                "Stages already run are NOT reverted - this only stops further ones.");
        return 1;
    }

    private static void say(CommandSourceStack source, ChatFormatting colour, String text) {
        final Component line = Component.literal(text).withStyle(colour);
        source.sendSuccess(() -> line, false);
    }

    private HandoverCommand() {}
}
