package net.reimaden.touhouorigins.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.reimaden.touhouorigins.power.FlipModelPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code touhouorigins:flip_model} — renders the holder upside-down.
 *
 * <p>Rides on vanilla's Dinnerbone/Grumm easter egg: {@code isEntityUpsideDown} is the single
 * question the renderer already asks, so answering it differently inverts the model with no transform
 * work of our own. Voile's trick, and it survives the version jump untouched because the method is
 * still there.
 *
 * <p>Entities genuinely named Dinnerbone or Grumm are excluded — vanilla already flips them, and
 * flipping twice would leave them upright.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererFlipMixin {

    @Inject(method = "isEntityUpsideDown(Lnet/minecraft/world/entity/LivingEntity;)Z",
        at = @At("HEAD"), cancellable = true)
    private static void touhouorigins$flipModel(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!FlipModelPower.shouldFlip(entity)) return;
        cir.setReturnValue(!FlipModelPower.isVanillaFlipped(entity));
    }
}
