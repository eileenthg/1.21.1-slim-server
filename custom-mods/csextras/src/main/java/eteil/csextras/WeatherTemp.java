package eteil.csextras;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.ShadeTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.WaterTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Hooks Cold Sweat's own rain-soaking and shade modifiers, so rain gear and umbrellas work through the
 * existing mechanics instead of adding new temperature sources.
 */
public final class WeatherTemp {
    /** Water modifier state before Cold Sweat's calculate() ran (Pre and Post fire back to back on one thread). */
    private static final ThreadLocal<Double> WET_BEFORE = new ThreadLocal<>();

    public static void onPre(TempModifierEvent.Calculate.Pre event) {
        if (event.getModifier() instanceof WaterTempModifier water) {
            WET_BEFORE.set(water.getTemperature());
        }
    }

    public static void onPost(TempModifierEvent.Calculate.Post event) {
        if (event.getModifier() instanceof WaterTempModifier water) {
            Double before = WET_BEFORE.get();
            WET_BEFORE.remove();
            if (before != null) shieldRain(event, water, before);
        } else if (event.getModifier() instanceof ShadeTempModifier) {
            carryShade(event);
        }
    }

    /**
     * In rain (and not in water) Cold Sweat pushes the wetness value toward -maxRainSoak and dries it at the same
     * time. Blend its result toward "dry only" by the protection fraction, and lower the soak cap to match.
     */
    private static void shieldRain(TempModifierEvent.Calculate.Post event, WaterTempModifier water, double before) {
        LivingEntity entity = event.getEntity();
        if (entity == null || WorldHelper.isInWater(entity)) return;
        if (!WorldHelper.isRainingAt(entity.level(), entity.blockPosition())) return;
        double protection = Gear.rainProtection(entity);
        if (protection <= 0) return;

        double after = water.getTemperature();
        double dryOnly = CSMath.shrink(before, dryAmount(entity, water));
        double adjusted = after + (dryOnly - after) * protection;
        double floor = -ConfigSettings.MAX_RAIN_SOAK.get() * (1 - protection);
        adjusted = Math.max(adjusted, Math.min(dryOnly, floor));

        water.setTemperature(adjusted);
        final double wet = adjusted;
        event.setFunction(temp -> temp + wet);
    }

    /** Same drying formula WaterTempModifier uses outside water. */
    static double dryAmount(LivingEntity entity, WaterTempModifier water) {
        double worldTemp = Temperature.get(entity, Temperature.Trait.WORLD);
        double speed = ConfigSettings.DRYOFF_SPEED.get();
        double dry = CSMath.blendExp(speed / 1.5, speed * 5.0, worldTemp,
                ConfigSettings.MIN_TEMP.get(), ConfigSettings.MAX_TEMP.get(), 20.0);
        return dry * water.getTickRate() / 5.0;
    }

    /** Umbrellas and sun hats count as standing in (partial) shade during the day. */
    private static void carryShade(TempModifierEvent.Calculate.Post event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;
        double carried = Gear.carriedShade(entity);
        if (carried <= 0) return;
        Level level = entity.level();
        if (level.dimensionType().hasCeiling() || !level.dimensionType().hasSkyLight()) return;

        double time = Math.max(0.0, WorldHelper.getTimeMultiplier(level));
        double darkness = (1.0 - level.getBrightness(LightLayer.SKY, BlockPos.containing(entity.getEyePosition())) / 15.0) * time;
        double shade = Math.max(darkness, level.getRainLevel(1.0F));
        double withGear = Math.max(shade, carried * time);
        if (withGear <= shade) return;

        double offset = ConfigSettings.SHADE_TEMP_OFFSET.get() * withGear;
        event.setFunction(temp -> temp + offset);
    }

    private WeatherTemp() {
    }
}
