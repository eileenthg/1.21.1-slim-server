package eteil.packprobe.mixin;

import eteil.packprobe.PackProbe;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerHurtMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void packprobe$head(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!PackProbe.isMaid(source)) return;
        Player self = (Player) (Object) this;
        if (self.level().isClientSide) return;
        PackProbe.LOG.info("[PackProbe] Player.hurt HEAD amount={} source={} invulnerableTo={} abilitiesInvulnerable={} invulnerableTime={} dead={} difficulty={}",
                amount, PackProbe.describe(source), self.isInvulnerableTo(source), self.getAbilities().invulnerable,
                self.invulnerableTime, self.isDeadOrDying(), self.level().getDifficulty());
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void packprobe$ret(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!PackProbe.isMaid(source)) return;
        Player self = (Player) (Object) this;
        if (self.level().isClientSide) return;
        PackProbe.LOG.info("[PackProbe] Player.hurt RETURN result={} health={}", cir.getReturnValue(), self.getHealth());
    }
}
