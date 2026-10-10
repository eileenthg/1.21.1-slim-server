package eteil.mcaanimcompat.mixin;

import eteil.mcaanimcompat.Carry;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Romantic Expansion ends a carry the moment either player presses sneak. The carrier now keeps carrying while
 * sneaking and puts the other player down with sneak + right-click on a block instead (CarryServer). The carried
 * player's sneak still gets them down, and the carry key still toggles for both.
 */
@Mixin(targets = "com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansionClient")
public abstract class RomanticShiftMixin {
    @Inject(method = "handleShiftPressed(Lnet/minecraft/client/Minecraft;)V", at = @At("HEAD"), cancellable = true)
    private static void mcaanimcompat$keepCarrying(Minecraft mc, CallbackInfo ci) {
        if (mc.player != null && Carry.isCarrier(mc.player.getUUID())) ci.cancel();
    }
}
