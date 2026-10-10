package net.reimaden.touhouorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.damagesource.DamageSource;
import net.reimaden.touhouorigins.power.ActionOnBlockPower;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.LivingEntity;
import net.reimaden.touhouorigins.power.SimplePowers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code touhouorigins:disable_shields} — the holder's attacks break shields, as an axe does.
 *
 * <p>Vanilla already asks the attacker {@code canDisableShield()} when resolving a blocked hit, so
 * this only has to answer differently; the disable itself, cooldown included, stays vanilla.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityShieldMixin {

    @ModifyReturnValue(method = "canDisableShield()Z", at = @At("TAIL"))
    private boolean touhouorigins$disableShields(boolean original) {
        if (PowerLookup.hasActive((LivingEntity) (Object) this, SimplePowers.DISABLE_SHIELDS)) {
            return true;
        }
        return original;
    }

    /**
     * {@code touhouorigins:action_on_block} — fires when a shield absorbs a hit. Vanilla calls
     * {@code hurtCurrentlyUsedShield} exactly at that decision point inside {@code hurt}.
     */
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"))
    private void touhouorigins$actionOnBlock(DamageSource source, float amount,
                                             CallbackInfoReturnable<Boolean> cir) {
        ActionOnBlockPower.onBlock((LivingEntity) (Object) this, source, amount);
    }
}
