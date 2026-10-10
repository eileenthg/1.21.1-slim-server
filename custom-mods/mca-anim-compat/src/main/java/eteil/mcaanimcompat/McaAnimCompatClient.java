package eteil.mcaanimcompat;

import net.minecraft.world.InteractionResult;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

final class McaAnimCompatClient {
    private McaAnimCompatClient() {
    }

    static void init(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        if (ModList.get().isLoaded("takeaseat") && ModList.get().isLoaded("player_animation_library")) LyingCamera.register();
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> DebugCommand.register(event.getDispatcher()));
        if (Carry.PRESENT) NeoForge.EVENT_BUS.addListener(McaAnimCompatClient::onRightClickBlock);
    }

    /**
     * Sneak + right-click while carrying is the put-down gesture (CarryServer). Cancelled here too so this client
     * does not also predict a block placement or open the block's menu; the click still reaches the server.
     */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() && event.getEntity().isShiftKeyDown() && Carry.isCarrier(event.getEntity().getUUID())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
