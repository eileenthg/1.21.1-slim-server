package eteil.seatrest.mixin;

import eteil.seatrest.SeatRestConfig;
import eteil.seatrest.SitState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Every vanilla source of hunger drain - walking, sprinting, jumping, breaking blocks, healing,
 * taking damage - funnels through this one method before it reaches the food data, so scaling the
 * argument here covers all of them at once.
 */
@Mixin(Player.class)
public abstract class PlayerExhaustionMixin {
    @ModifyVariable(method = "causeFoodExhaustion(F)V", at = @At("HEAD"), argsOnly = true)
    private float seatrest$slowHungerWhileSeated(float amount) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide) return amount;
        if (!SitState.isSitting(self)) return amount;
        return amount * SeatRestConfig.hungerFactor();
    }
}
