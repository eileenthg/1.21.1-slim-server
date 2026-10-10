package com.github.eileenthg.tamablearcadianfairy.mixin;

import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.reimaden.arcadiandream.entity.custom.danmaku.BaseBulletEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets a tamed fairy's danmaku actually hurt other fairies.
 *
 * <p>Arcadian Dream deliberately makes a fairy's bullets pass harmlessly through any other
 * fairy, which is right for wild ones so a field of them does not wipe itself out. Once a
 * fairy is a pet that rule stops making sense: it will happily target a wild fairy and shoot
 * at it forever without ever landing a hit.
 *
 * <p>So the exemption is lifted for exactly one case, a fight where at least one side is
 * tamed and the two are not owned by the same player. Wild against wild is untouched, and
 * two of your own fairies still cannot hurt each other.
 */
@Mixin(BaseBulletEntity.class)
public abstract class BaseBulletEntityMixin extends Projectile {

    private BaseBulletEntityMixin() {
        super(null, null);
    }

    @Shadow
    protected abstract void applyDamage(Entity target, Entity owner);

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void tamablearcadianfairy$hurtRivalFairies(EntityHitResult result, CallbackInfo ci) {
        if (level().isClientSide()) return;

        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (!FairyTaming.isFairy(target) || !FairyTaming.isFairy(owner)) return;
        if (!FairyTaming.isTamed(target) && !FairyTaming.isTamed(owner)) return;
        if (FairyTaming.shareOwner(owner, target)) return;

        // Take the hit over: the original would return without dealing any damage.
        // Skipping it loses nothing, because Projectile#onHitEntity is empty.
        applyDamage(target, owner);
        ci.cancel();
    }
}
