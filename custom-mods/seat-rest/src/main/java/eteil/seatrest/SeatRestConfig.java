package eteil.seatrest;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Multipliers applied to exhaustion gain while the player is sitting. 1.0 is vanilla speed,
 * 0.2 means the bar drains five times more slowly, 0.0 freezes it.
 */
public final class SeatRestConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue HUNGER_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue THIRST_MULTIPLIER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Slower hunger and thirst while sitting (Take a Seat).").push("sitting");

        ENABLED = b
                .comment("Master switch. False leaves both drains at their normal rate.")
                .define("enabled", true);

        HUNGER_MULTIPLIER = b
                .comment("Hunger exhaustion gained while sitting, as a fraction of normal.",
                        "Covers everything vanilla charges exhaustion for: movement, mining, healing, taking damage.")
                .defineInRange("hungerMultiplier", 0.2D, 0.0D, 1.0D);

        THIRST_MULTIPLIER = b
                .comment("Thirst exhaustion gained while sitting, as a fraction of normal.",
                        "Thirst Was Taken derives its drain from the hunger exhaustion the player accrues, so",
                        "hungerMultiplier = 0.0 freezes thirst too no matter what this is set to.")
                .defineInRange("thirstMultiplier", 0.2D, 0.0D, 1.0D);

        b.pop();
        SPEC = b.build();
    }

    private SeatRestConfig() {
    }

    public static float hungerFactor() {
        return ENABLED.get() ? HUNGER_MULTIPLIER.get().floatValue() : 1.0F;
    }

    /**
     * What the thirst drain still has to be multiplied by once the hunger multiplier has already
     * been folded into the exhaustion Thirst Was Taken reads back off the food data.
     */
    public static float residualThirstFactor() {
        if (!ENABLED.get()) return 1.0F;
        float hunger = HUNGER_MULTIPLIER.get().floatValue();
        float thirst = THIRST_MULTIPLIER.get().floatValue();
        // Below this the food exhaustion barely moves, so there is nothing left to scale.
        if (hunger < 1.0E-4F) return 1.0F;
        return thirst / hunger;
    }
}
