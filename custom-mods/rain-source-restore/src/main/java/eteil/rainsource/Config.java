package eteil.rainsource;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master switch. When on, exposed flowing water can convert back into a source block while it is raining on it.",
                     "Vanilla's own rules still apply: the block needs at least two adjacent water SOURCE blocks and solid support below,",
                     "so this only fills gaps in water that already exists - it never creates new water.")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue INCLUDE_SNOWY_BIOMES = BUILDER
            .comment("Also heal while it is snowing. Level.isRainingAt() is false in biomes that get snow instead of rain,",
                     "so by default snowy biomes never heal. Turn on to treat snowfall the same as rain.",
                     "Biomes with no precipitation at all (deserts, badlands) are never affected either way.")
            .define("includeSnowyBiomes", false);

    public static final ModConfigSpec.IntValue MIN_Y = BUILDER
            .comment("Only heal at or above this Y level. A safety floor: even if a deep area somehow has sky access,",
                     "nothing below this height will convert. Set to the world minimum to disable the check.")
            .defineInRange("minY", 0, -64, 320);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
