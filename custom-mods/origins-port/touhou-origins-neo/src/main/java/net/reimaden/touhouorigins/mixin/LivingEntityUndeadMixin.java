package net.reimaden.touhouorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.LivingEntity;
import net.reimaden.touhouorigins.power.SimplePowers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@code touhouorigins:reverse_instant_effects} — healing hurts and harming heals, as for the undead.
 *
 * <p>Used by {@code jiang_shi/pestilence}, which is thematically a hopping corpse.
 *
 * <p>1.21 removed {@code MobType}; "counts as undead" for potion purposes is now exactly
 * {@code LivingEntity.isInvertedHealAndHarm()}, so inverting that one return is the whole feature.
 * Vanilla calls it from the instant-health and instant-damage effects, so both directions follow.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityUndeadMixin {

    @ModifyReturnValue(method = "isInvertedHealAndHarm()Z", at = @At("TAIL"))
    private boolean touhouorigins$reverseInstantEffects(boolean original) {
        if (PowerLookup.hasActive((LivingEntity) (Object) this, SimplePowers.REVERSE_INSTANT_EFFECTS)) {
            return !original;
        }
        return original;
    }
}
