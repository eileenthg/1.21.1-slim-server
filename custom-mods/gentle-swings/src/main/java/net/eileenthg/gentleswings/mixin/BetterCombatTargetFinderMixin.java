package net.eileenthg.gentleswings.mixin;

import java.util.List;
import java.util.stream.Collectors;
import net.eileenthg.gentleswings.SwingConfig;
import net.eileenthg.gentleswings.client.ClientState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Better Combat finds its own targets with a weapon hitbox instead of the crosshair, so a wide
 * swing at a zombie would still catch the villager beside it. Dropping spared entities here keeps
 * them out of the swing, the hit sounds and the "enemies nearby, attack instead of mine" check.
 * The server cancels those hits anyway; this just stops the client acting them out.
 * Skipped entirely when Better Combat is not installed.
 */
@Pseudo
@Mixin(targets = "net.bettercombat.client.collision.TargetFinder", remap = false)
abstract class BetterCombatTargetFinderMixin {

    @Inject(method = "findAttackTargets", at = @At("RETURN"), cancellable = true, require = 0)
    private static void gentleswings$dropSpared(CallbackInfoReturnable<List<Entity>> cir) {
        if (!ClientState.gentle) return;
        List<Entity> targets = cir.getReturnValue();
        if (targets != null && targets.stream().anyMatch(SwingConfig::spares)) {
            cir.setReturnValue(targets.stream().filter(e -> !SwingConfig.spares(e)).collect(Collectors.toList()));
        }
    }
}
