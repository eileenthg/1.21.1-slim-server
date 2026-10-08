package eteil.gensokyocompat.mixin;

import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.fastprojectileapi.entity.GrazingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Arcadian Dream bullets become grazeable like the danmaku-library ones: a bullet passing within grazeRange of a
 * player (other than its shooter) without hitting them counts as one graze for that player, routed through
 * GrazeHelper so Youkai's Homecoming's graze handling applies.
 */
@Mixin(targets = "net.reimaden.arcadiandream.entity.custom.danmaku.BaseBulletEntity", remap = false)
public abstract class ArcadianGrazeMixin extends ThrowableItemProjectile implements GrazingEntity {
    @Unique private final Set<UUID> gensokyocompat$grazed = new HashSet<>();

    protected ArcadianGrazeMixin(net.minecraft.world.entity.EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void gensokyocompat$graze(CallbackInfo ci) {
        Level level = this.level();
        if (level.isClientSide || this.isRemoved()) return;
        AABB box = this.getBoundingBox();
        Entity owner = this.getOwner();
        for (Player p : level.getEntitiesOfClass(Player.class, box.inflate(this.grazeRange()))) {
            if (p == owner || p.isSpectator() || p.getBoundingBox().intersects(box)) continue;
            if (gensokyocompat$grazed.add(p.getUUID())) GrazeHelper.graze(p, this);
        }
    }
}
