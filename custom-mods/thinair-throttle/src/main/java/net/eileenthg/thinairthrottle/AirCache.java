package net.eileenthg.thinairthrottle;

import dev.maire.thinair.api.AirQualityHelper;
import dev.maire.thinair.api.AirQualityLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A per-entity cache of Thin Air's air-quality lookup. Server thread only. */
public final class AirCache {

    /** Stored on each LivingEntity through {@link Holder}. */
    public static final class Entry {
        AirQualityLevel quality;
        ResourceKey<Level> dimension;
        Vec3 pos;
        long checkedAt;
    }

    public interface Holder {
        Entry thinairthrottle$entry();

        void thinairthrottle$setEntry(Entry entry);
    }

    private AirCache() {}

    public static AirQualityLevel lookup(AirQualityHelper helper, LivingEntity entity) {
        int interval = ThrottleConfig.checkInterval();
        if (interval <= 1) {
            return helper.getAirQualityAtLocation(entity);
        }

        Level level = entity.level();
        Vec3 eyes = entity.getEyePosition();

        // This is Thin Air's own first step, and the cheap one. The block at eye level decides on its own:
        // fluid is red, a bubble column is green, and an air-provider block gives its own level.
        // Checking it every tick keeps drowning and surfacing instant.
        AirQualityLevel atEyes = AirQualityLevel.getAirQualityAtEyes(level.getBlockState(BlockPos.containing(eyes)));
        if (atEyes != null) {
            return atEyes;
        }

        Holder holder = (Holder) entity;
        Entry entry = holder.thinairthrottle$entry();
        long now = level.getGameTime();
        if (entry != null
                && entry.dimension == level.dimension()
                && now >= entry.checkedAt
                && now - entry.checkedAt < interval
                && !movedTooFar(entry.pos, eyes)) {
            return entry.quality;
        }

        AirQualityLevel quality = helper.getAirQualityAtLocation(entity);
        if (entry == null) {
            entry = new Entry();
            holder.thinairthrottle$setEntry(entry);
            // Spread first checks over the interval, so a chunk full of villagers doesn't re-check on the same tick.
            entry.checkedAt = now - Math.floorMod(entity.getId(), interval);
        } else {
            entry.checkedAt = now;
        }
        entry.quality = quality;
        entry.dimension = level.dimension();
        entry.pos = eyes;
        return quality;
    }

    private static boolean movedTooFar(Vec3 then, Vec3 now) {
        double d = ThrottleConfig.recheckDistance();
        return d > 0 && then.distanceToSqr(now) > d * d;
    }
}
