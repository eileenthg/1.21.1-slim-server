package eteil.mcarpgbridge.client;

import eteil.mcarpgbridge.McaRpgBridge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Mod list → MCA RPG Bridge → Config opens NeoForge's editor for the client and common configs. */
@Mod(value = McaRpgBridge.MOD_ID, dist = Dist.CLIENT)
public final class McaRpgBridgeClient {
    public McaRpgBridgeClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
