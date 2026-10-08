package net.eileenthg.gentleswings.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.eileenthg.gentleswings.GentleSwings;
import net.eileenthg.gentleswings.SwingConfig;
import net.eileenthg.gentleswings.TogglePayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod(value = GentleSwings.MOD_ID, dist = Dist.CLIENT)
public final class GentleSwingsClient {

    public static final KeyMapping TOGGLE = new KeyMapping("key.gentleswings.toggle",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.gentleswings");

    private static final ModConfigSpec CLIENT_SPEC;
    private static final ModConfigSpec.BooleanValue SHOW_HINT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SHOW_HINT = b.comment("Show a small \"passing through\" label under the crosshair while a gentle swing",
                        "would pass through the entity you are looking at.")
                .define("showCrosshairHint", true);
        CLIENT_SPEC = b.build();
    }

    public GentleSwingsClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(TOGGLE));
        modBus.addListener((RegisterGuiLayersEvent e) ->
                e.registerAbove(VanillaGuiLayers.CROSSHAIR, GentleSwings.id("hint"), GentleSwingsClient::renderHint));

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (TOGGLE.consumeClick()) {
                if (Minecraft.getInstance().player != null) {
                    PacketDistributor.sendToServer(TogglePayload.INSTANCE);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientState.gentle = false);
    }

    private static void renderHint(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientState.gentle || !SHOW_HINT.get() || mc.options.hideGui || mc.screen != null
                || !mc.options.getCameraType().isFirstPerson()
                || mc.crosshairPickEntity == null || !SwingConfig.spares(mc.crosshairPickEntity)) {
            return;
        }
        Component text = Component.translatable("gentleswings.hud.passing");
        int x = (graphics.guiWidth() - mc.font.width(text)) / 2;
        int y = graphics.guiHeight() / 2 + 10;
        graphics.drawString(mc.font, text, x, y, 0xB0E0E0E0, true);
    }
}
