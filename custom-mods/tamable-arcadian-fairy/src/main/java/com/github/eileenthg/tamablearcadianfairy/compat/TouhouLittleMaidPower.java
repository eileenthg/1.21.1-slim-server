package com.github.eileenthg.tamablearcadianfairy.compat;

import com.github.eileenthg.tamablearcadianfairy.TamableArcadianFairy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional Touhou Little Maid support: a mob killed by a tamed fairy drops power points,
 * mirroring the way Tamable Fairy rewards kills made by a tamed maid fairy.
 *
 * <p>Reached entirely through reflection so Touhou Little Maid stays an optional dependency.
 */
public final class TouhouLittleMaidPower {

    private static final String MOD_ID = "touhou_little_maid";
    private static final int MAX_ORBS = 20;

    private static Constructor<?> powerPointConstructor;
    private static Method getPowerValue;
    private static Field powerPointConfigField;
    private static boolean available;

    private TouhouLittleMaidPower() {}

    public static void init() {
        if (!ModList.get().isLoaded(MOD_ID)) return;

        try {
            ClassLoader loader = TouhouLittleMaidPower.class.getClassLoader();
            // Loaded without initialising: the class builds an EntityType in its static block,
            // which must not run until Touhou Little Maid has registered its own entities.
            Class<?> powerPoint = Class.forName(
                    "com.github.tartaricacid.touhoulittlemaid.entity.item.EntityPowerPoint", false, loader);
            powerPointConstructor = powerPoint.getConstructor(
                    Level.class, double.class, double.class, double.class, int.class);
            getPowerValue = powerPoint.getMethod("getPowerValue", int.class);

            Class<?> miscConfig = Class.forName(
                    "com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig", false, loader);
            powerPointConfigField = miscConfig.getField("MAID_FAIRY_POWER_POINT");
            available = true;
            TamableArcadianFairy.LOGGER.info(
                    "Touhou Little Maid detected: tamed fairies will farm power points");
        } catch (ReflectiveOperationException e) {
            TamableArcadianFairy.LOGGER.warn(
                    "Touhou Little Maid is present but its API does not match, power point drops are off", e);
        }
    }

    /** Spawns power points on the victim of a kill made by a tamed fairy. */
    public static void dropFor(LivingEntity victim) {
        if (!available || victim.level().isClientSide()) return;
        if (!victim.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;

        try {
            Object config = powerPointConfigField.get(null);
            if (config == null) return;

            double perFairy = ((Number) config.getClass().getMethod("get").invoke(config)).doubleValue();
            int total = (int) (perFairy * 100.0);
            int spawned = 0;
            while (total > 0 && spawned++ < MAX_ORBS) {
                int value = (int) getPowerValue.invoke(null, total);
                if (value <= 0) break;
                total -= value;
                Entity orb = (Entity) powerPointConstructor.newInstance(
                        victim.level(), victim.getX(), victim.getY(), victim.getZ(), value);
                victim.level().addFreshEntity(orb);
            }
        } catch (ReflectiveOperationException e) {
            TamableArcadianFairy.LOGGER.error("Failed to drop Touhou Little Maid power points", e);
            available = false;
        }
    }
}
