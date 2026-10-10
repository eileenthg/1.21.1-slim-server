package net.reimaden.touhouorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.reimaden.touhouorigins.power.PreventTamingPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@code touhouorigins:prevent_taming} for ocelots.
 *
 * <p>Ocelots are the exception to {@link TamableAnimalMixin}: since 1.14 they extend {@code Animal}
 * rather than {@code TamableAnimal} and gain <i>trust</i> instead of being tamed, so there is no
 * {@code tame(Player)} to cancel. The trust roll lives inline in {@code mobInteract}, so this wraps
 * it and forces a non-zero (failing) result.
 */
@Mixin(Ocelot.class)
public abstract class OcelotTamingMixin {

    @ModifyExpressionValue(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int touhouorigins$preventTrust(int original, Player player, InteractionHand hand) {
        return PreventTamingPower.preventTaming(original, player, (Ocelot) (Object) this);
    }
}
