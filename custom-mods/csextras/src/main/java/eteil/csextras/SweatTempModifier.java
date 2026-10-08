package eteil.csextras;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;

/**
 * Evaporative cooling on the world temperature while the body is hot, scaled by hydration.
 * Worked out on the server and synced to the client through the modifier's NBT.
 */
public class SweatTempModifier extends TempModifier {
    private static final String KEY = "Sweat";

    public double getCooling() {
        return this.getNBT().getDouble(KEY);
    }

    @Override
    protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait) {
        double cooling;
        if (entity.level().isClientSide()) {
            cooling = getCooling();
        } else {
            cooling = computeCooling(entity);
            if (Math.abs(cooling - getCooling()) > 1e-3) {
                this.getNBT().putDouble(KEY, cooling);
                this.markDirty();
            }
            if (cooling > 0 && entity instanceof Player player && ThirstCompat.LOADED) {
                double intensity = cooling / Math.max(1e-6, Config.SWEAT_MAX_COOLING.get());
                float cost = (float) (Config.SWEAT_EXHAUSTION.get() * intensity * this.getTickRate() / 20.0);
                ThirstCompat.exhaust(player, cost);
            }
        }
        final double c = cooling;
        return temp -> temp - c;
    }

    static double computeCooling(LivingEntity entity) {
        if (!Config.SWEAT.get()) return 0;
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) return 0;
        double body = Temperature.get(entity, Temperature.Trait.BODY);
        double start = Config.SWEAT_START.get();
        if (body <= start) return 0;
        double heat = Mth.clamp((body - start) / Math.max(1e-6, 100.0 - start), 0, 1);
        double hydration = 1;
        if (entity instanceof Player player && ThirstCompat.LOADED) {
            int min = Config.SWEAT_MIN_THIRST.get(), full = Config.SWEAT_FULL_THIRST.get();
            hydration = Mth.clamp((ThirstCompat.thirst(player) - min) / (double) Math.max(1, full - min), 0, 1);
        }
        return Config.SWEAT_MAX_COOLING.get() * heat * hydration;
    }

    @Override
    public void tick(LivingEntity entity) {
        // A few beads of sweat, much sparser than Cold Sweat's own dripping when soaked.
        double cooling = getCooling();
        if (entity.level().isClientSide() && cooling > 0 && !entity.isInWater()
                && Math.random() < cooling * 0.6) {
            double x = entity.getBbWidth() * (Math.random() - 0.5);
            double y = entity.getBbHeight() * (0.5 + Math.random() * 0.4);
            double z = entity.getBbWidth() * (Math.random() - 0.5);
            entity.level().addParticle(ParticleTypes.FALLING_WATER, entity.getX() + x, entity.getY() + y, entity.getZ() + z, 0, 0, 0);
        }
    }
}
