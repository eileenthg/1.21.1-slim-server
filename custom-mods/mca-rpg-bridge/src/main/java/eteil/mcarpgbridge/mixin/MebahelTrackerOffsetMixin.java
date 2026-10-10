package eteil.mcarpgbridge.mixin;

import eteil.mcarpgbridge.BridgeClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nudges Mebahel's quest tracker away from whatever sits in its corner (Xaero's minimap here).
 *
 * <p>Translate the whole render call rather than one draw inside it: 2.0.6 drew a backdrop through a
 * private drawSolidBackdrop, which 3.2.0's rewritten HUD no longer has, but both versions keep
 * render(GuiGraphics, DeltaTracker). The translate is in screen pixels, applied before Mebahel's own
 * corner placement and scale, so the offset means the same thing in both.
 *
 * <p>3.2.0 can also place the tracker in any corner and scale it (questTrackerPosition/questTrackerScale in
 * Mebahel's own config); this offset is for fine tuning on top of that. RETURN, not TAIL, so an early
 * return still pops, and require = 0 leaves the tracker where Mebahel put it if this ever stops matching.
 */
@Pseudo
@Mixin(targets = "net.mebahelrpgrquest.client.gui.QuestTrackerHud", remap = false)
public abstract class MebahelTrackerOffsetMixin {
    @Unique
    private static boolean mcarpgbridge$pushed;

    @Inject(method = "render", remap = false, require = 0, at = @At("HEAD"))
    private static void mcarpgbridge$pushOffset(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        int x = BridgeClientConfig.trackerOffsetX();
        int y = BridgeClientConfig.trackerOffsetY();
        if (x == 0 && y == 0) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        mcarpgbridge$pushed = true;
    }

    @Inject(method = "render", remap = false, require = 0, at = @At("RETURN"))
    private static void mcarpgbridge$popOffset(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        // only pop what we pushed, so a missed push injection cannot underflow the pose stack
        if (mcarpgbridge$pushed) {
            mcarpgbridge$pushed = false;
            graphics.pose().popPose();
        }
    }
}
