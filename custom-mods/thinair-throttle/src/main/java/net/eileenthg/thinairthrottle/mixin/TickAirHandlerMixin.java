package net.eileenthg.thinairthrottle.mixin;

import dev.maire.thinair.api.AirQualityHelper;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.handler.TickAirHandler;
import net.eileenthg.thinairthrottle.AirCache;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Thin Air runs its air-quality search on every LivingBreatheEvent, which means every tick for every air-sensitive
 * entity: players, villagers (MCA's too, through its villager-tag hack), traders and illagers. The search walks every
 * recorded air-provider block, lava and fire included, in the surrounding 5x5 chunks. This routes it through a
 * per-entity cache. Everything else in the handler (air drain and refill, advancements, HUD sync) still runs each tick.
 */
@Mixin(value = TickAirHandler.class, remap = false)
public abstract class TickAirHandlerMixin {

    @Redirect(
            method = "onLivingBreathe",
            at = @At(value = "INVOKE",
                    target = "Ldev/maire/thinair/api/AirQualityHelper;getAirQualityAtLocation(Lnet/minecraft/world/entity/LivingEntity;)Ldev/maire/thinair/api/AirQualityLevel;"))
    private static AirQualityLevel thinairthrottle$cachedLookup(AirQualityHelper helper, LivingEntity entity) {
        return AirCache.lookup(helper, entity);
    }
}
