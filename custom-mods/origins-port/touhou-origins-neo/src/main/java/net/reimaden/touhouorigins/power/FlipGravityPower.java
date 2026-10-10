package net.reimaden.touhouorigins.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.reimaden.touhouorigins.TouhouOrigins;
import net.reimaden.touhouorigins.TouhouOriginsConfig;

/**
 * Replaces the {@code apoli:modify_velocity} "reverse_y" sub-power in {@code this_side_down} and
 * {@code common/flip_entity} — same effect, but the multiplier is read live from
 * {@link TouhouOriginsConfig} instead of being fixed in JSON. See that class for the actual formula
 * and why the numbers look the way they do.
 *
 * <p>No JSON fields at all — this is registered under two ids, {@link #SELF_ID} and
 * {@link #TURNABOUT_ID}, and {@link #apply} checks for each independently so both can be active on
 * the same entity at once (not that anything currently grants both). The surrounding Apoli power's
 * {@code condition} field still works exactly as before: {@link PowerLookup} evaluates
 * {@code Power.condition()} generically, this class never has to look at it.
 */
public final class FlipGravityPower extends PowerType<FlipGravityPower.Cfg> {

    public static final ResourceLocation SELF_ID = TouhouOrigins.id("flip_gravity_self");
    public static final ResourceLocation TURNABOUT_ID = TouhouOrigins.id("flip_gravity_turnabout");

    public record Cfg() {
        public static final Cfg INSTANCE = new Cfg();
    }

    @Override
    public MapCodec<Cfg> configCodec() {
        return MapCodec.unit(Cfg.INSTANCE);
    }

    /**
     * Mirrors {@code apoli:modify_velocity}'s own Y-axis application exactly
     * ({@code total = original * (1.0 + value)}) so behaviour is bit-for-bit the same as before this
     * moved out of JSON, only with the value now configurable. Called from
     * {@code EntityFlipGravityMixin} for every entity, every tick of {@code Entity.move}.
     */
    public static Vec3 apply(Entity entity, Vec3 movement) {
        double y = movement.y;
        boolean changed = false;

        if (PowerLookup.hasActive(entity, SELF_ID)) {
            y *= 1.0 + TouhouOriginsConfig.thisSideDownMultiplier();
            changed = true;
        }
        if (PowerLookup.hasActive(entity, TURNABOUT_ID)) {
            y *= 1.0 + TouhouOriginsConfig.turnaboutMultiplier();
            changed = true;
        }

        return changed ? new Vec3(movement.x, y, movement.z) : movement;
    }
}
