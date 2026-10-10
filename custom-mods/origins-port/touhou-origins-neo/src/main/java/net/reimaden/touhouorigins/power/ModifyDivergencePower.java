package net.reimaden.touhouorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.reimaden.touhouorigins.TouhouOrigins;

/**
 * Changes how inaccurate the holder's projectiles are.
 *
 * <p>Used by {@code wolf_tengu/clumsy_archer} — a tengu is formidable in melee and hopeless with a
 * bow. "Divergence" is vanilla's {@code inaccuracy} parameter: the spread cone applied when a
 * projectile is fired.
 *
 * <p>Voile's semantics preserved: a single {@code set} power wins outright, otherwise every
 * applicable power's value is summed onto vanilla's.
 */
public final class ModifyDivergencePower extends PowerType<ModifyDivergencePower.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("modify_divergence");

    public enum Operation implements StringRepresentable {
        SET("set"), ADD("add");

        private final String name;

        Operation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);
    }

    public record Cfg(float divergence, Operation operation) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("divergence").forGetter(Cfg::divergence),
            Operation.CODEC.optionalFieldOf("operation", Operation.SET).forGetter(Cfg::operation)
        ).apply(i, Cfg::new));
    }

    /** @return the inaccuracy vanilla should use for a projectile fired by {@code shooter}. */
    public static float modify(Entity shooter, float original) {
        if (shooter == null) return original;

        float[] set = {Float.NaN};
        float[] added = {0.0F};
        PowerLookup.forEach(shooter, CANONICAL, Cfg.class, cfg -> {
            if (cfg.operation() == Operation.SET) {
                if (Float.isNaN(set[0])) set[0] = cfg.divergence();
            } else {
                added[0] += cfg.divergence();
            }
        });

        if (!Float.isNaN(set[0])) return set[0];
        return original + added[0];
    }
}
