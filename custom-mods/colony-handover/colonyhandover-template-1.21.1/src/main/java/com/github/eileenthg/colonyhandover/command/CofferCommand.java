package com.github.eileenthg.colonyhandover.command;

import com.github.eileenthg.colonyhandover.coffer.CofferData;
import com.github.eileenthg.colonyhandover.coffer.CofferMenu;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;

/**
 * {@code /colonyhandover coffer} - collect what the colonists were carrying.
 *
 * <h2>Why a command rather than a block</h2>
 *
 * A block would need placing somewhere, and there is no good somewhere: inside the
 * town hall it can be walled in or broken, and outside it is litter. It would also
 * be a permanent fixture left over from a one-off event. A command has no location
 * to get wrong and disappears when the coffer is empty.
 */
public final class CofferCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("coffer").executes(ctx -> open(ctx.getSource()));
    }

    private static int open(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game."));
            return 0;
        }

        final CofferData data = CofferData.get(player.serverLevel());
        if (data.isEmpty()) {
            source.sendSuccess(() -> Component
                            .literal("The coffer is empty - nothing left to collect.")
                            .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        final MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, p) -> new CofferMenu(containerId, inventory, data),
                Component.literal("Colony Coffer"));
        player.openMenu(provider);
        return 1;
    }

    private CofferCommand() {}
}
