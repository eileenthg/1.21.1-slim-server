package net.reimaden.touhouorigins.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.reimaden.touhouorigins.power.ModifyDivergencePower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * {@code touhouorigins:modify_divergence} — alters the spread applied to the holder's projectiles.
 *
 * <p>{@code shootFromRotation} (Yarn's {@code setVelocity}) takes five floats; the last, ordinal 4,
 * is the inaccuracy cone. Vanilla routes bows, crossbows and thrown items through it, so one hook
 * covers them all.
 *
 * <p>Voile also wrapped ordinal 3 (velocity) for its {@code modify_projectile_speed} power. Touhou
 * does not use that type, so it is deliberately not ported.
 */
@Mixin(Projectile.class)
public abstract class ProjectileDivergenceMixin {

    @ModifyVariable(
        method = "shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V",
        at = @At("HEAD"), ordinal = 4, argsOnly = true)
    private float touhouorigins$modifyDivergence(float original, Entity shooter) {
        return ModifyDivergencePower.modify(shooter, original);
    }
}
