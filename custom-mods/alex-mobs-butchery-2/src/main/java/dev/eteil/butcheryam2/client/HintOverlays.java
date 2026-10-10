package dev.eteil.butcheryam2.client;

import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import dev.eteil.butcheryam2.Config;
import dev.eteil.butcheryam2.butcher.CutStage;
import dev.eteil.butcheryam2.butcher.CutUp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

/**
 * Vignette hints shown while looking at a carcass, telling the player which tool comes next.
 * Dimensions match Butchery's own overlays, whose textures these are.
 */
@EventBusSubscriber(modid = ButcheryAlexsMobs2.MODID, value = Dist.CLIENT)
public final class HintOverlays {

    private static final ResourceLocation CLEAVER_HINT =
            ResourceLocation.parse("butchery_alexs_mobs_2:textures/screens/butchers_knife_overlay.png");
    private static final ResourceLocation KNIFE_HINT =
            ResourceLocation.parse("butchery_alexs_mobs_2:textures/screens/skinning_knife_overlay.png");

    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(ButcheryAlexsMobs2.MODID, "butchering_hint"),
                HintOverlays::render);
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker delta) {
        if (!Config.SHOW_HINTS.get()) {
            return;
        }
        CutStage.Tool tool = lookedAtTool();
        if (tool == null) {
            return;
        }

        // Like Butchery's own hints: shown whatever the player holds, so it says which tool to fetch.
        ResourceLocation texture = tool == CutStage.Tool.KNIFE ? KNIFE_HINT : CLEAVER_HINT;

        int x = graphics.guiWidth() / 2 - 480;
        int y = graphics.guiHeight() / 2 - 269;
        graphics.blit(texture, x, y, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }

    /** The tool the block under the crosshair needs next, or null. */
    @Nullable
    private static CutStage.Tool lookedAtTool() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.hitResult == null
                || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return CutUp.nextTool(mc.level, ((BlockHitResult) mc.hitResult).getBlockPos());
    }

    private HintOverlays() {}
}
