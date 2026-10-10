package eteil.seatrest.mixin;

import dev.ghen.thirst.content.thirst.PlayerThirst;
import eteil.seatrest.SeatRestConfig;
import eteil.seatrest.SitState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Thirst Was Taken does not accrue thirst on its own: {@code updateExhaustion} reads the player's
 * vanilla food exhaustion each tick and feeds the frame-to-frame delta into its own exhaustion
 * pool. That means {@link PlayerExhaustionMixin} has already slowed thirst by the hunger
 * multiplier by the time this delta is handed over, so what is left to apply here is only the
 * difference between the two configured rates.
 */
@Mixin(value = PlayerThirst.class, remap = false)
public abstract class ThirstExhaustionMixin {
    @Redirect(
            method = "updateExhaustion(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ghen/thirst/content/thirst/PlayerThirst;addExhaustion(Lnet/minecraft/world/entity/player/Player;F)V"
            )
    )
    private void seatrest$slowThirstWhileSeated(PlayerThirst instance, Player player, float amount) {
        if (!player.level().isClientSide && SitState.isSitting(player)) {
            amount *= SeatRestConfig.residualThirstFactor();
        }
        instance.addExhaustion(player, amount);
    }
}
