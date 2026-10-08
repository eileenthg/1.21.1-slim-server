package eteil.seasonclimate;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** Tunables for how strongly each kind of biome feels the seasons. 1.0 = Cold Sweat's own swing, 0 = none. */
final class Config {
    static final ModConfigSpec SPEC;

    static final ModConfigSpec.BooleanValue ENABLED;
    static final ModConfigSpec.DoubleValue TROPICAL;
    static final ModConfigSpec.DoubleValue WARM;
    static final ModConfigSpec.DoubleValue TEMPERATE;
    static final ModConfigSpec.DoubleValue COOL;
    static final ModConfigSpec.DoubleValue COLD;
    static final ModConfigSpec.DoubleValue COASTAL;
    static final ModConfigSpec.ConfigValue<List<? extends String>> OVERRIDES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Season Climate: scales Cold Sweat's seasonal temperature offset by biome.",
                "Cold Sweat's own seasonal swing (config/coldsweat/world.toml, [Compatibility.Seasons]) is applied",
                "everywhere in the dimension. These factors multiply that offset based on where the player is,",
                "so equatorial biomes stay stable and cold ones swing harder. Nothing else about temperature changes.",
                "A factor of 1.0 keeps Cold Sweat's value; 0.0 removes seasons for that biome.").push("general");
        ENABLED = b.comment("Master switch. False makes seasons global again, exactly as Cold Sweat does by default.")
                .define("enabled", true);
        b.pop();

        b.comment("Factors by biome class. The first matching rule wins: overrides, then tropical, then base temperature.").push("factors");
        TROPICAL = b.comment("Biomes tagged #sereneseasons:tropical_biomes (jungles, savanna, badlands, desert, mangrove swamp,",
                        "mushroom fields, warm ocean). Serene Seasons already exempts these from its own seasonal effects.")
                .defineInRange("tropical", 0.15, 0.0, 4.0);
        WARM = b.comment("Non-tropical biomes with a base temperature of 0.9 or above.")
                .defineInRange("warm", 0.3, 0.0, 4.0);
        TEMPERATE = b.comment("Base temperature 0.5 to 0.89: plains, forest, birch, swamp, most starting biomes.")
                .defineInRange("temperate", 0.5, 0.0, 4.0);
        COOL = b.comment("Base temperature 0.15 to 0.49: taiga, meadow, old growth spruce.")
                .defineInRange("cool", 1.0, 0.0, 4.0);
        COLD = b.comment("Base temperature below 0.15: snowy biomes, frozen peaks, ice spikes.")
                .defineInRange("cold", 1.25, 0.0, 4.0);
        COASTAL = b.comment("Extra multiplier for ocean and beach biomes, for the way large water moderates a climate.")
                .defineInRange("coastalMultiplier", 0.7, 0.0, 4.0);
        OVERRIDES = b.comment("Per-biome overrides, highest priority. One entry per line, \"id=factor\" or \"#tag=factor\".",
                        "Examples: \"minecraft:jungle=0.1\", \"#minecraft:is_badlands=0.2\", \"terralith:yellowstone=1.4\"")
                .defineListAllowEmpty("overrides", List.of(), () -> "", o -> o instanceof String s && s.contains("="));
        b.pop();

        SPEC = b.build();
    }

    private Config() {}
}
