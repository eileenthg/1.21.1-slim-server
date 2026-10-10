package eteil.mcaanimcompat.mixin;

import eteil.mcaanimcompat.Carry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * The carried player's hitbox surrounds the carrier's eyes, and a hitbox the ray starts inside always wins the
 * crosshair at distance 0. So the carrier could only ever target the person in their arms: no blocks, doors,
 * chests or waystones. Neither half of a carry pair can target the other.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererPickMixin {
    @ModifyArg(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"),
            index = 4)
    private Predicate<Entity> mcaanimcompat$skipCarryPartner(Predicate<Entity> filter) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        UUID partner = camera == null ? null : Carry.partnerOf(camera.getUUID());
        if (partner == null) return filter;
        return entity -> !entity.getUUID().equals(partner) && filter.test(entity);
    }
}
