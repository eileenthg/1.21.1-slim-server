package eteil.packprobe.mixin;

import eteil.packprobe.PackProbe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobDoHurtTargetMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"))
    private void packprobe$head(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (!(target instanceof Player) || !PackProbe.isMaid(self) || self.level().isClientSide) return;
        PackProbe.LOG.info("[PackProbe] {} doHurtTarget HEAD target={} attackDamage={} mainHand={}",
                self.getClass().getSimpleName(), target.getName().getString(), self.getAttributeValue(Attributes.ATTACK_DAMAGE), self.getMainHandItem());
    }

    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void packprobe$ret(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (!(target instanceof Player) || !PackProbe.isMaid(self) || self.level().isClientSide) return;
        PackProbe.LOG.info("[PackProbe] {} doHurtTarget RETURN result={}", self.getClass().getSimpleName(), cir.getReturnValue());
    }
}
