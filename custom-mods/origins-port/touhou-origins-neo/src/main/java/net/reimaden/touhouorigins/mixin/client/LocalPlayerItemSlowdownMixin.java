package net.reimaden.touhouorigins.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.reimaden.touhouorigins.power.PreventItemSlowdownPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@code touhouorigins:prevent_item_slowdown} — keep normal movement speed while using an item.
 *
 * <p>Used by {@code wolf_tengu/steadfast_defense} for shields: raising a shield normally slows you to
 * a crawl, and a tengu is meant to keep moving.
 *
 * <p><b>Client-side by nature.</b> The movement penalty is applied by the client's own player tick,
 * which asks {@code isUsingItem()} twice — once to decide the slowdown, once to decide whether
 * sprinting may start. Modifying those two answers is enough; the server accepts the resulting
 * movement. Voile did the same thing in its client source set, and the power is registered commonly
 * so the server still knows it exists for syncing and conditions.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerItemSlowdownMixin {

    @ModifyExpressionValue(method = "aiStep()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean touhouorigins$noSlowdownWhileUsingItem(boolean original) {
        return original && !PreventItemSlowdownPower.applies((LocalPlayer) (Object) this);
    }

    @ModifyExpressionValue(method = "canStartSprinting()Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean touhouorigins$canSprintWhileUsingItem(boolean original) {
        return original && !PreventItemSlowdownPower.canStartSprinting((LocalPlayer) (Object) this);
    }
}
