package com.github.eileenthg.tamablearcadianfairy.mixin;

import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Arcadian Dream fairies are monsters, which normally stops players sleeping and makes
 * them vanish on peaceful. Neither should be true once a fairy is a pet.
 */
@Mixin(Monster.class)
public abstract class MonsterMixin {

    @Inject(method = "isPreventingPlayerRest", at = @At("HEAD"), cancellable = true)
    private void tamablearcadianfairy$allowRest(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (FairyTaming.isTamed((Monster) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldDespawnInPeaceful", at = @At("HEAD"), cancellable = true)
    private void tamablearcadianfairy$survivePeaceful(CallbackInfoReturnable<Boolean> cir) {
        if (FairyTaming.isTamed((Monster) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
