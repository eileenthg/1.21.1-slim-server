package net.reimaden.touhouorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.reimaden.touhouorigins.power.PreventTamingPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@code touhouorigins:prevent_taming} for horses, donkeys and mules.
 *
 * <p>Horses do not use the zero-roll pattern: this goal (Yarn's {@code HorseBondWithPlayerGoal})
 * bucks the rider off and succeeds only once a roll comes in under the horse's current temper.
 * Returning temper guarantees that comparison fails, so the horse keeps throwing the rider.
 *
 * <p>The rider is the horse's first passenger rather than an interaction argument, hence the
 * different shape from the animal mixins.
 */
@Mixin(RunAroundLikeCrazyGoal.class)
public abstract class RunAroundLikeCrazyGoalMixin {

    @Shadow @Final private AbstractHorse horse;

    @ModifyExpressionValue(method = "tick()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 1))
    private int touhouorigins$preventHorseTaming(int original) {
        if (!(this.horse.getFirstPassenger() instanceof Player player)) return original;
        return PreventTamingPower.preventTamingHorse(original, player, this.horse, this.horse.getTemper());
    }
}
