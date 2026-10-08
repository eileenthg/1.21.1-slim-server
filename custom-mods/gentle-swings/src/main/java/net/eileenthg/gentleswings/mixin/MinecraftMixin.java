package net.eileenthg.gentleswings.mixin;

import net.eileenthg.gentleswings.SwingConfig;
import net.eileenthg.gentleswings.client.ClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While gentle swings are on and the crosshair rests on a spared entity, the attack code sees a
 * block-only pick instead, so left-click mines the block behind the entity (or swings at air).
 *
 * <p>The swap covers only {@code startAttack} and {@code continueAttack}; right-click, pick-block
 * and the rest of the frame keep the real target, so you can still talk to and trade with NPCs.
 * Priority 500 puts these hooks ahead of Better Combat's, which read the same field to decide
 * between mining and swinging.
 */
@Mixin(value = Minecraft.class, priority = 500)
abstract class MinecraftMixin {

    @Shadow public HitResult hitResult;

    @Unique private HitResult gentleswings$realHit;
    @Unique private HitResult gentleswings$swappedHit;

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void gentleswings$beforeStartAttack(CallbackInfoReturnable<Boolean> cir) {
        gentleswings$swapIn();
    }

    @Inject(method = "startAttack", at = @At("RETURN"))
    private void gentleswings$afterStartAttack(CallbackInfoReturnable<Boolean> cir) {
        gentleswings$restore();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"))
    private void gentleswings$beforeContinueAttack(boolean leftClick, CallbackInfo ci) {
        gentleswings$swapIn();
    }

    @Inject(method = "continueAttack", at = @At("RETURN"))
    private void gentleswings$afterContinueAttack(boolean leftClick, CallbackInfo ci) {
        gentleswings$restore();
    }

    // Another mod can cancel startAttack/continueAttack at HEAD, which skips the RETURN hooks
    // above. These put the real target back before anything else in the tick reads it.
    @Inject(method = {"startUseItem", "pickBlock"}, at = @At("HEAD"))
    private void gentleswings$beforeUse(CallbackInfo ci) {
        gentleswings$restore();
    }

    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void gentleswings$afterKeybinds(CallbackInfo ci) {
        gentleswings$restore();
    }

    @Unique
    private void gentleswings$swapIn() {
        if (!ClientState.gentle || gentleswings$realHit != null
                || !(hitResult instanceof EntityHitResult entityHit)
                || !SwingConfig.spares(entityHit.getEntity())) {
            return;
        }
        Minecraft mc = (Minecraft) (Object) this;
        Entity camera = mc.getCameraEntity();
        if (camera == null || mc.player == null) return;

        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
        gentleswings$realHit = hitResult;
        gentleswings$swappedHit = camera.pick(mc.player.blockInteractionRange(), partialTick, false);
        hitResult = gentleswings$swappedHit;
    }

    @Unique
    private void gentleswings$restore() {
        if (gentleswings$realHit == null) return;
        // Only undo our own swap; if something re-picked in the meantime, its result is newer.
        if (hitResult == gentleswings$swappedHit) hitResult = gentleswings$realHit;
        gentleswings$realHit = null;
        gentleswings$swappedHit = null;
    }
}
