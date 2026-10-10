package com.github.eileenthg.tamablearcadianfairy.mixin;

import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets an owner put a lead on their fairy. */
@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "canBeLeashed", at = @At("HEAD"), cancellable = true)
    private void tamablearcadianfairy$allowLeash(CallbackInfoReturnable<Boolean> cir) {
        if (FairyTaming.isTamed((Mob) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
