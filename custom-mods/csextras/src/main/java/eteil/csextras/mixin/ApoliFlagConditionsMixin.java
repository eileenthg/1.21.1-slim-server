package eteil.csextras.mixin;

import eteil.csextras.OriginsCompat;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overgrown Apoli 1.56.0 implements apoli:in_rain and apoli:exposed_to_sun as lambdas in SimpleFlagConditions.
 * in_rain reads Level.isRainingAt directly, so Artifacts' Entity.isInRain hook never reaches it; and Convenient Decor's
 * and Artifacts' own Origins mixins target Fabric Apoli class names that don't exist here.
 * Lambda names are tied to that Apoli build; injections are optional so a changed build just loses the feature.
 */
@Pseudo
@Mixin(targets = "dev.overgrown.apoli.condition.builtin.entity.SimpleFlagConditions", remap = false)
public abstract class ApoliFlagConditionsMixin {
    @Inject(method = "lambda$inRain$8(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private static void csextras$rainCover(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        OriginsCompat.markRainHook();
        if (cir.getReturnValueZ() && OriginsCompat.blocksRain(entity)) cir.setReturnValue(false);
    }

    @Inject(method = "lambda$exposedToSun$6(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private static void csextras$sunCover(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        OriginsCompat.markSunHook();
        if (cir.getReturnValueZ() && OriginsCompat.blocksSun(entity)) cir.setReturnValue(false);
    }
}
