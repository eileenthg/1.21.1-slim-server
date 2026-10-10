package eteil.settledcaves;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---------------------------------------------------------------- fluid seam
    static {
        BUILDER.comment("Water/lava contact.",
                        "Any lava block touching water is replaced. The LAVA side is converted, so water lakes keep their volume.",
                        "It becomes OBSIDIAN only when a lava source touches a water source; every other contact becomes stone.")
               .push("fluidSeam");
    }

    public static final ModConfigSpec.BooleanValue FLUID_SEAM_ENABLED = BUILDER
            .define("enabled", true);

    public static final ModConfigSpec.IntValue FLUID_SEAM_MAX_Y = BUILDER
            .comment("Only process lava at or below this Y.")
            .defineInRange("maxY", 64, -64, 320);

    static { BUILDER.pop(); }

    // ---------------------------------------------------------------- aperture seal
    static {
        BUILDER.comment("Water breaches.",
                        "An 'opening' is a water block with air beside it or directly below it - somewhere it can pour out.",
                        "Connected openings are grouped. Groups LARGER than maxOpening are walled off with stone;",
                        "groups at or under it are left alone, so small wall pockets still trickle down cave walls.")
               .push("apertureSeal");
    }

    public static final ModConfigSpec.BooleanValue APERTURE_ENABLED = BUILDER
            .define("enabled", true);

    public static final ModConfigSpec.IntValue APERTURE_MAX_OPENING = BUILDER
            .comment("Largest opening (in connected water blocks) left unsealed. Trickles and narrow waterfalls live under this.")
            .defineInRange("maxOpening", 5, 1, 4096);

    public static final ModConfigSpec.IntValue APERTURE_MAX_Y = BUILDER
            .comment("Only process water at or below this Y. Kept under sea level by default so surface",
                     "rivers, cliff waterfalls and lake shorelines are never touched.")
            .defineInRange("maxY", 50, -64, 320);

    static { BUILDER.pop(); }

    // ---------------------------------------------------------------- gravel cap
    static {
        BUILDER.comment("Falling-block footprint.",
                        "Any block extending FallingBlock (gravel, sand, red sand, concrete powder, modded gravels) with air",
                        "directly below it is 'exposed'. Horizontally connected exposed blocks on the same layer form a footprint.",
                        "Footprints larger than maxFootprint are split by turning blocks on a grid into stone, so a cave-in",
                        "still happens but stays small instead of unzipping a whole ceiling.",
                        "Pointed dripstone is NOT affected - it implements Fallable but not FallingBlock.")
               .push("gravelCap");
    }

    public static final ModConfigSpec.BooleanValue GRAVEL_CAP_ENABLED = BUILDER
            .define("enabled", true);

    public static final ModConfigSpec.IntValue GRAVEL_MAX_FOOTPRINT = BUILDER
            .comment("Target maximum size of a single cave-in, in blocks.",
                     "The grid quantises this: the real cap is floor(sqrt(value))^2.",
                     "So 4-8 give 2x2 (4), 9-15 give 3x3 (9), 16-24 give 4x4 (16).")
            .defineInRange("maxFootprint", 9, 1, 4096);

    public static final ModConfigSpec.IntValue GRAVEL_MAX_Y = BUILDER
            .comment("Only process falling blocks at or below this Y.")
            .defineInRange("maxY", 320, -64, 320);

    static { BUILDER.pop(); }

    // ---------------------------------------------------------------- shared
    public static final ModConfigSpec.BooleanValue USE_DEEPSLATE_BELOW_ZERO = BUILDER
            .comment("Place deepslate instead of stone for seams, walls and supports below Y 0, so they blend into the",
                     "deepslate layer rather than leaving grey streaks. Obsidian is unaffected.")
            .define("useDeepslateBelowZero", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
