package com.github.eileen.workingmaidcomputers;

import com.github.eileen.workingmaidcomputers.client.DesktopLauncher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Turns a second right-click on a Touhou Little Maid computer, made while sitting at it, into the AntOS desktop.
 * <ul>
 *     <li>Empty hand: the client opens the desktop. The first click is TLM's own, and sits the player down.</li>
 *     <li>Floppy disk in hand: the server installs the disk into the player's Anternet account.</li>
 * </ul>
 */
@EventBusSubscriber(modid = WorkingMaidComputers.MOD_ID)
public final class ComputerInteraction {
    private ComputerInteraction() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        var player = event.getEntity();
        var level = event.getLevel();
        if (!MaidComputerSeat.isSeatedAt(player, level, event.getPos())) {
            return;
        }

        ItemStack held = event.getItemStack();
        if (held.isEmpty()) {
            if (level.isClientSide()) {
                DesktopLauncher.open();
            }
            consume(event);
        } else if (held.is(WorkingMaidComputers.FLOPPY_DISK.get())) {
            // 客户端不拦截，让交互包照常发往服务端，由服务端装入软盘
            if (player instanceof ServerPlayer serverPlayer) {
                FloppyInstaller.install(serverPlayer, held);
                consume(event);
            }
        }
    }

    private static void consume(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
