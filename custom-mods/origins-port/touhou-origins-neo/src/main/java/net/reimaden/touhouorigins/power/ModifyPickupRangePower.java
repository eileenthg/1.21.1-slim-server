package net.reimaden.touhouorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.reimaden.touhouorigins.TouhouOrigins;

/**
 * Widens the box a player scans for items and XP orbs to pick up.
 *
 * <p>Used by Touhou Origins Blessings' {@code scarlet/spacetime_manipulation}: sneaking pulls in
 * loot from one block further away. 1.21.1 has no pickup-range attribute (that arrived in a later
 * version), so this stays a power plus a small mixin, as it was in Voile.
 *
 * <p>Voile's semantics preserved: the {@code range} of every active power is summed and the scan
 * box is inflated by the total on all axes. Voile hooked the box's local variable by LVT index;
 * {@link net.reimaden.touhouorigins.mixin.PlayerPickupRangeMixin} modifies the single
 * {@code getEntities} argument instead, which catches both the riding and on-foot branches.
 */
public final class ModifyPickupRangePower extends PowerType<ModifyPickupRangePower.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("modify_pickup_range");

    public record Cfg(float range) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("range").forGetter(Cfg::range)
        ).apply(i, Cfg::new));
    }

    /** @return the summed extra range of every active power on {@code entity}; 0 when none. */
    public static float extraRange(Entity entity) {
        float[] total = {0.0F};
        PowerLookup.forEach(entity, CANONICAL, Cfg.class, cfg -> total[0] += cfg.range());
        return total[0];
    }
}
