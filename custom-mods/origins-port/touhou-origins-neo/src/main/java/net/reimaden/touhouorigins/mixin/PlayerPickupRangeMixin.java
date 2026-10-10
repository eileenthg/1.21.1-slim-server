package net.reimaden.touhouorigins.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.reimaden.touhouorigins.power.ModifyPickupRangePower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Widens the pickup scan box for {@code touhouorigins:modify_pickup_range}.
 *
 * <p>{@code Player.aiStep} builds one AABB (two branches: riding and on foot) and passes it to a
 * single {@code level().getEntities(this, aabb)} call whose results feed {@code touch()} — the
 * item-magnet path. Modifying that argument catches both branches with one injection, where
 * Voile's {@code @ModifyVariable} by LVT index only hit the first store.
 */
@Mixin(Player.class)
public abstract class PlayerPickupRangeMixin {

    @ModifyArg(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
        ),
        index = 1
    )
    private AABB touhouorigins$widenPickupBox(AABB box) {
        float extra = ModifyPickupRangePower.extraRange((Player) (Object) this);
        return extra != 0.0F ? box.inflate(extra) : box;
    }
}
