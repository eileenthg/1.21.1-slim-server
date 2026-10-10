package com.github.eileenthg.colonyhandover.command;

import com.github.eileenthg.colonyhandover.handover.ColonySnapshot;
import com.github.eileenthg.colonyhandover.handover.ParentReport;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.permissions.Rank;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /colonyhandover parents} - who has recorded parents, and what becomes of them.
 *
 * <h2>Why name them rather than count them</h2>
 *
 * The dry run reports totals: so many parents recovered, so many from the name file,
 * so many same-gender pairs. That is the right shape for deciding whether to go
 * ahead, and the wrong shape for checking whether the answer is <em>correct</em>. To
 * do that you have to look at a specific colonist you know the history of and see
 * whether the mod agrees with you. This prints that.
 *
 * <p>It also shows which slot each parent will land in, because that is the part
 * most likely to look wrong later: a second mother is filed under "father" by
 * design, and seeing it here beats discovering it in MCA's family screen after the
 * colony is gone.
 */
public final class ParentsCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("parents").executes(ctx -> run(ctx.getSource()));
    }

    private static int run(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game, standing in the colony."));
            return 0;
        }
        final IColony colony = IMinecoloniesAPI.getInstance().getColonyManager()
                .getIColony(player.serverLevel(), player.blockPosition());
        if (colony == null) {
            source.sendFailure(Component.literal("You are not standing in a colony."));
            return 0;
        }
        final Rank rank = colony.getPermissions().getRank(player);
        if (rank == null || !rank.isColonyManager()) {
            source.sendFailure(Component.literal("You need colony manager rank here."));
            return 0;
        }

        final ColonySnapshot snap = ColonySnapshot.of(colony);
        say(source, ChatFormatting.GOLD, "=== Recorded parents: " + snap.colonyName() + " ===");

        // No cap here: this command exists precisely to list them, so truncating
        // would defeat it. The preview passes a smaller limit instead.
        final int total = ParentReport.render(snap, Integer.MAX_VALUE,
                (colour, text) -> say(source, colour, text));
        if (total > 0) {
            say(source, ChatFormatting.GRAY, total + " colonist(s) with recorded parents.");
        }
        return 1;
    }

    private static void say(CommandSourceStack source, ChatFormatting colour, String text) {
        final Component line = Component.literal(text).withStyle(colour);
        source.sendSuccess(() -> line, false);
    }

    private ParentsCommand() {}
}
