package eteil.mcarpgbridge;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

@Mod(McaRpgBridge.MOD_ID)
public final class McaRpgBridge {
    public static final String MOD_ID = "mcarpgbridge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public McaRpgBridge(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, BridgeConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, BridgeClientConfig.SPEC);
        modBus.addListener(BridgeNetwork::register);
        NeoForge.EVENT_BUS.addListener(McaRpgBridge::onServerAboutToStart);
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (ModList.get().isLoaded("mebahelrpgrquest")) {
            MebahelInteraction.verifyClickGuard();
        }
    }
}
