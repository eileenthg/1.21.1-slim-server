package net.reimaden.touhouorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.reimaden.touhouorigins.power.PreventTamingPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@code touhouorigins:prevent_taming} for parrots.
 *
 * <p>Parrots have no {@code tryToTame}: the roll and the heart/smoke broadcast are inline in
 * {@code mobInteract}. Same treatment as wolves and cats — fail the roll so vanilla takes its own
 * smoke branch, rather than cancelling {@code tame} and leaving hearts with no collar.
 */
@Mixin(Parrot.class)
public abstract class ParrotTamingMixin {

    @ModifyExpressionValue(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int touhouorigins$failTamingRoll(int original, Player player, InteractionHand hand) {
        return PreventTamingPower.preventTaming(original, player, (Parrot) (Object) this);
    }
}
