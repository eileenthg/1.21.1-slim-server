package net.reimaden.touhouorigins.mixin;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.reimaden.touhouorigins.power.PreventTamingPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code touhouorigins:prevent_taming} for every tameable animal.
 *
 * <p>Voile hooked the taming dice roll separately in wolf, cat and parrot. In 1.21 all three call
 * {@code TamableAnimal.tame(Player)} once their roll succeeds, so cancelling that single method
 * covers all of them — and any modded tameable — instead of one mixin per species.
 *
 * <p>The attempt still fails the way vanilla fails it: the food is already consumed and the roll
 * already happened, so the player sees a normal unsuccessful taming rather than a no-op.
 *
 * <p>Ocelots are deliberately absent: since 1.14 they extend {@code Animal}, not
 * {@code TamableAnimal}, and gain <i>trust</i> rather than being tamed — a separate mechanic Voile
 * hooked separately too.
 */
@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin {

    @Inject(method = "tame(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), cancellable = true)
    private void touhouorigins$preventTaming(Player player, CallbackInfo ci) {
        if (PreventTamingPower.applies(player, (TamableAnimal) (Object) this)) {
            ci.cancel();
        }
    }
}
