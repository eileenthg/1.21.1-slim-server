package net.reimaden.touhouorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.reimaden.touhouorigins.power.PreventTamingPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@code touhouorigins:prevent_taming} for Wolfs — fails the taming roll itself.
 *
 * <p>Cancelling {@code TamableAnimal.tame} alone was not enough: vanilla's {@code tryToTame}
 * broadcasts the heart particles <i>after</i> calling {@code tame}, so the animal appeared to accept
 * the player while never gaining a collar. Forcing the roll to fail instead takes vanilla's own
 * else-branch, which emits smoke — the honest "it refused you" animation.
 */
@Mixin(Wolf.class)
public abstract class WolfTamingMixin {

    @ModifyExpressionValue(method = "tryToTame(Lnet/minecraft/world/entity/player/Player;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int touhouorigins$failTamingRoll(int original, Player player) {
        return PreventTamingPower.preventTaming(original, player, (Wolf) (Object) this);
    }
}
