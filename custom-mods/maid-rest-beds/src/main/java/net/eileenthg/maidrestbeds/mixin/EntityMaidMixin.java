package net.eileenthg.maidrestbeds.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.favorability.FavorabilityManager;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.eileenthg.maidrestbeds.RestBeds;
import net.eileenthg.maidrestbeds.RestConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A maid bed heals fully and gives the full "Sleep" favorability. An ordinary bed or hammock gives
 * the configured share of both. The favorability shares the maid bed's "Sleep" cooldown, so there is no double dipping.
 */
@Mixin(value = EntityMaid.class, remap = false)
abstract class EntityMaidMixin {

    @WrapOperation(method = "startSleeping",
            at = @At(value = "INVOKE", target = "Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;setHealth(F)V"))
    private void maidrestbeds$partialHeal(EntityMaid self, float fullHealth, Operation<Void> original) {
        double share = switch (RestBeds.kindAtSleepPos(self)) {
            case BED -> RestConfig.BED_HEAL.get();
            case HAMMOCK -> RestConfig.HAMMOCK_HEAL.get();
            default -> 1.0;
        };
        if (share >= 1.0) {
            original.call(self, fullHealth);
        } else if (share > 0.0) {
            original.call(self, Math.min(fullHealth, self.getHealth() + (float) (self.getMaxHealth() * share)));
        }
    }

    @WrapOperation(method = "startSleeping",
            at = @At(value = "INVOKE", target = "Lcom/github/tartaricacid/touhoulittlemaid/entity/favorability/FavorabilityManager;apply(Lcom/github/tartaricacid/touhoulittlemaid/entity/favorability/Type;)V"))
    private void maidrestbeds$smallerFavor(FavorabilityManager manager, Type type, Operation<Void> original) {
        double share = switch (RestBeds.kindAtSleepPos((EntityMaid) (Object) this)) {
            case BED -> RestConfig.BED_FAVORABILITY.get();
            case HAMMOCK -> RestConfig.HAMMOCK_FAVORABILITY.get();
            default -> 1.0;
        };
        if (share >= 1.0) {
            original.call(manager, type);
        } else if (share > 0.0) {
            manager.apply(type, sleepPoints(type.getPoint(), share));
        }
    }

    /** Scaled from the real value, since other mods (Vanilla Companion) change the Sleep points. */
    private static int sleepPoints(int maidBedPoints, double share) {
        return Math.max(1, (int) Math.round(maidBedPoints * share));
    }
}
