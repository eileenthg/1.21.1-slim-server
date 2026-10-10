package net.reimaden.touhouorigins.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.reimaden.touhouorigins.power.FlipGravityPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Drives {@code touhouorigins:flip_gravity_self} / {@code _turnabout} — see
 * {@link FlipGravityPower}.
 *
 * <p>Deliberately mirrors Apoli's own {@code EntityModifyVelocityMixin} — same hook
 * ({@code Entity.move}, {@code HEAD}, {@code MoverType.SELF} only), same reasoning: {@code move}
 * receives the entity's already-gravity-adjusted velocity for this tick, right as collision
 * resolution begins, so multiplying it here reproduces the original {@code apoli:modify_velocity}
 * timing exactly rather than approximating it from a different hook.
 */
@Mixin(Entity.class)
public abstract class EntityFlipGravityMixin {

    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("HEAD"), argsOnly = true)
    private Vec3 touhouorigins$reverseGravity(Vec3 movement, MoverType moverType) {
        if (moverType != MoverType.SELF) return movement;
        return FlipGravityPower.apply((Entity) (Object) this, movement);
    }
}
