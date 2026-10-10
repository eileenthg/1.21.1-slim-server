package com.github.eileenthg.colonyhandover.command;

import com.github.eileenthg.colonyhandover.place.MarkTracker;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.permissions.Rank;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /colonyhandover marks} - toggle beams showing every gap.
 *
 * <h2>Why particles rather than a marker block</h2>
 *
 * A readiness report gives coordinates, but coordinates are a poor way to find a
 * hole in a wall you are standing next to. Marking the spot in the world is much
 * more direct: look around, see where the light is, walk there.
 *
 * <p>Particles were chosen over placing temporary marker blocks because they
 * write nothing - no journal entry, no risk of leaving debris behind if the
 * player logs out mid-survey - and over a client-side overlay because that would
 * need a client mod, and this works in vanilla multiplayer as-is.
 *
 * <h2>Why a toggle</h2>
 *
 * A one-shot burst is the wrong shape for the job. Sealing a gap is work you do
 * with the beams in view, walking back and forth, and re-running a command every
 * few seconds to keep the light on is friction in the middle of that. So this
 * latches: on until you turn it off, or until the last gap is sealed.
 * {@link MarkTracker} holds the state and does the re-emitting.
 *
 * <p>This command is the stand-in for what will eventually be a button on the
 * town hall handover screen, so the on/off semantics are deliberately the ones a
 * button would have.
 */
public final class MarkCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("marks").executes(ctx -> run(ctx.getSource()));
    }

    private static int run(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game, standing in the colony."));
            return 0;
        }

        // Turning them off needs no colony and no permission - you are only
        // stopping your own particles, and refusing here would strand anyone who
        // switched them on and then walked out of the colony.
        if (MarkTracker.isWatching(player)) {
            MarkTracker.stop(player);
            say(source, ChatFormatting.GRAY, "Marks off.");
            return 1;
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

        final MarkTracker.Gaps gaps = MarkTracker.start(player, colony);
        if (gaps.isEmpty()) {
            say(source, ChatFormatting.GREEN, "Nothing to mark - every building is sealed.");
            return 1;
        }

        say(source, ChatFormatting.GOLD, "Marking " + gaps.walls().size()
                + " wall gap(s) in white and " + gaps.ceilings().size()
                + " open ceiling(s) in blue.");
        say(source, ChatFormatting.GRAY,
                "Beams stay lit while you work. Run /colonyhandover marks again to turn"
                        + " them off - or seal every gap and they go out by themselves.");
        return 1;
    }

    private static void say(CommandSourceStack source, ChatFormatting colour, String text) {
        final Component line = Component.literal(text).withStyle(colour);
        source.sendSuccess(() -> line, false);
    }

    private MarkCommand() {}
}
