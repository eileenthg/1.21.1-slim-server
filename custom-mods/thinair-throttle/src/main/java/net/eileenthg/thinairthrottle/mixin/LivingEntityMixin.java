package net.eileenthg.thinairthrottle.mixin;

import net.eileenthg.thinairthrottle.AirCache;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements AirCache.Holder {

    @Unique
    private AirCache.Entry thinairthrottle$entry;

    @Override
    public AirCache.Entry thinairthrottle$entry() {
        return thinairthrottle$entry;
    }

    @Override
    public void thinairthrottle$setEntry(AirCache.Entry entry) {
        thinairthrottle$entry = entry;
    }
}
