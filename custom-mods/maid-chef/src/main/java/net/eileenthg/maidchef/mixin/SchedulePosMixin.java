package net.eileenthg.maidchef.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.SchedulePos;
import net.eileenthg.maidchef.ChefConfig;
import net.eileenthg.maidchef.TaskChef;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TLM keeps a home-mode maid within the global MaidWorkRange of her work point and walks/teleports her back
 * when she leaves it. For a Chef, her own kitchen radius (plus a little room to reach chests at the edge)
 * replaces that range while she works, so each Chef can have her own kitchen size.
 */
@Mixin(value = SchedulePos.class, remap = false)
public abstract class SchedulePosMixin {
    @Inject(method = "restrictTo(Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;)V", at = @At("TAIL"))
    private void maidchef$chefRange(EntityMaid maid, CallbackInfo ci) {
        if (maid.getTask() != TaskChef.INSTANCE || !maid.isHomeModeEnable()) return;
        if (maid.getScheduleDetail() != Activity.WORK) return;
        int radius = Math.min(TaskChef.data(maid).radius(), ChefConfig.MAX_RADIUS.get()) + 3;
        if (radius > maid.getRestrictRadius()) {
            maid.restrictTo(maid.getRestrictCenter(), radius);
        }
    }
}
