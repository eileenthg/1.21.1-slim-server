package eteil.csextras;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Rain and sun protection for Origins powers (Enderian/Blazeborn water, Phantom/Vampire sun). */
public final class OriginsCompat {
    static final ResourceKey<DamageType> HURT_BY_WATER =
            ResourceKey.create(Registries.DAMAGE_TYPE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("origins", "hurt_by_water"));

    /** Set the first time each injected lambda runs; reported by /csextras selftest. */
    public static volatile boolean rainHookSeen, sunHookSeen;

    public static void markRainHook() {
        rainHookSeen = true;
    }

    public static void markSunHook() {
        sunHookSeen = true;
    }

    /** Normal rain only: in a thunderstorm the condition stays true and damage is reduced instead. */
    public static boolean blocksRain(Entity entity) {
        return Config.ORIGINS_RAIN.get() && entity instanceof LivingEntity living
                && !entity.level().isThundering() && Gear.originsRainCover(living);
    }

    public static boolean blocksSun(Entity entity) {
        return Config.UMBRELLA_ORIGINS_SUN.get() && entity instanceof LivingEntity living && Gear.holdingOpenUmbrella(living);
    }

    /** Multiplier for a hurt_by_water hit: reduced only for rain in a thunderstorm, never for standing in water. */
    public static float thunderMultiplier(LivingEntity entity, DamageSource source) {
        if (!Config.ORIGINS_RAIN.get() || !source.is(HURT_BY_WATER)) return 1f;
        if (!entity.level().isThundering() || entity.isInWaterOrBubble()) return 1f;
        if (!entity.level().isRainingAt(entity.blockPosition()) && !entity.level().isRainingAt(entity.blockPosition().above())) return 1f;
        return Gear.originsRainCover(entity) ? Config.ORIGINS_THUNDER_MULT.get().floatValue() : 1f;
    }

    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        float mult = thunderMultiplier(event.getEntity(), event.getSource());
        if (mult != 1f) event.setAmount(event.getAmount() * mult);
    }

    private OriginsCompat() {
    }
}
