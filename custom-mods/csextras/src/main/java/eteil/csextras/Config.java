package eteil.csextras;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class Config {
    public static final ModConfigSpec SPEC;

    // Rainwear: 0 = no protection from rain soaking, 1 = stays dry.
    public static final ModConfigSpec.DoubleValue RAINCOAT;
    public static final ModConfigSpec.DoubleValue RAIN_HAT;
    public static final ModConfigSpec.DoubleValue FULL_COVER;
    public static final ModConfigSpec.DoubleValue UMBRELLA_RAIN;
    public static final ModConfigSpec.BooleanValue ORIGINS_RAIN;
    public static final ModConfigSpec.BooleanValue UMBRELLA_ORIGINS_RAIN;
    public static final ModConfigSpec.DoubleValue ORIGINS_THUNDER_MULT;

    // Shade
    public static final ModConfigSpec.DoubleValue UMBRELLA_SHADE;
    public static final ModConfigSpec.DoubleValue SUN_HAT_SHADE;
    public static final ModConfigSpec.BooleanValue UMBRELLA_ORIGINS_SUN;

    // Shivering
    public static final ModConfigSpec.BooleanValue SHIVER;
    public static final ModConfigSpec.DoubleValue SHIVER_START;
    public static final ModConfigSpec.DoubleValue SHIVER_MIN_WARMTH;
    public static final ModConfigSpec.DoubleValue SHIVER_MAX_WARMTH;
    public static final ModConfigSpec.IntValue SHIVER_MIN_FOOD;
    public static final ModConfigSpec.DoubleValue SHIVER_EXHAUSTION;

    // Campfire
    public static final ModConfigSpec.BooleanValue CAMPFIRE;
    public static final ModConfigSpec.DoubleValue CAMPFIRE_RADIUS;
    public static final ModConfigSpec.IntValue CAMPFIRE_DURATION;

    // Warming foods
    public static final ModConfigSpec.BooleanValue WARMING_FOODS;
    public static final ModConfigSpec.IntValue WARMING_FOOD_DURATION;
    public static final ModConfigSpec.IntValue WARMING_FOOD_AMPLIFIER;

    // Sweat
    public static final ModConfigSpec.BooleanValue SWEAT;
    public static final ModConfigSpec.DoubleValue SWEAT_START;
    public static final ModConfigSpec.DoubleValue SWEAT_MAX_COOLING;
    public static final ModConfigSpec.IntValue SWEAT_MIN_THIRST;
    public static final ModConfigSpec.IntValue SWEAT_FULL_THIRST;
    public static final ModConfigSpec.DoubleValue SWEAT_EXHAUSTION;

    // Heartbeat (client only, but kept here so one file holds everything)
    public static final ModConfigSpec.BooleanValue HEARTBEAT;
    public static final ModConfigSpec.DoubleValue HEARTBEAT_START;
    public static final ModConfigSpec.DoubleValue HEARTBEAT_VOLUME;
    public static final ModConfigSpec.ConfigValue<String> HEARTBEAT_SOUND;

    // Pitchfork
    public static final ModConfigSpec.BooleanValue PITCHFORK;
    public static final ModConfigSpec.DoubleValue PITCHFORK_SPEED;
    public static final ModConfigSpec.IntValue PITCHFORK_RADIUS;

    // Sandstorms
    public static final ModConfigSpec.BooleanValue SANDSTORMS;
    public static final ModConfigSpec.DoubleValue SANDSTORM_SLOW;
    public static final ModConfigSpec.DoubleValue SANDSTORM_FOLLOW_RANGE;
    public static final ModConfigSpec.BooleanValue SANDSTORM_SLOWS_PLAYERS;
    public static final ModConfigSpec.BooleanValue BREEZES;
    public static final ModConfigSpec.IntValue BREEZE_WEIGHT;
    public static final ModConfigSpec.IntValue BREEZE_MAX_NEARBY;
    public static final ModConfigSpec.DoubleValue PILE_CHANCE;
    public static final ModConfigSpec.IntValue PILE_MAX_LAYERS;
    public static final ModConfigSpec.DoubleValue CAULDRON_FILL_CHANCE;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HIDDEN_ITEMS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Rain gear. Values are how much of Cold Sweat's rain soaking is kept off (0-1).",
                "Tags: csextras:raincoats, csextras:rain_hats, csextras:umbrellas").push("rainwear");
        RAINCOAT = b.comment("Raincoat with the hood down").defineInRange("raincoat", 0.4, 0, 1);
        RAIN_HAT = b.comment("Rain hat without a raincoat").defineInRange("rainHat", 0.4, 0, 1);
        FULL_COVER = b.comment("Raincoat with the hood up, or raincoat + rain hat").defineInRange("fullCover", 0.8, 0, 1);
        UMBRELLA_RAIN = b.comment("Open umbrella (stacks with clothing)").defineInRange("umbrella", 0.4, 0, 1);
        ORIGINS_RAIN = b.comment("Full rain cover stops Origins' in_rain check (Enderian, Blazeborn) in normal rain")
                .define("originsRainProtection", true);
        UMBRELLA_ORIGINS_RAIN = b.comment("An open umbrella also counts as full rain cover for Origins")
                .define("umbrellaCountsForOriginsRain", false);
        ORIGINS_THUNDER_MULT = b.comment("During thunderstorms full cover no longer blocks rain; it multiplies origins:hurt_by_water damage by this")
                .defineInRange("originsThunderDamageMultiplier", 0.5, 0, 1);
        b.pop();

        b.comment("Portable shade. 1 = same as standing in full shade. Tag: csextras:sun_hats").push("shade");
        UMBRELLA_SHADE = b.defineInRange("umbrella", 1.0, 0, 1);
        SUN_HAT_SHADE = b.defineInRange("sunHat", 0.6, 0, 1);
        UMBRELLA_ORIGINS_SUN = b.comment("An open umbrella stops Origins' exposed_to_sun check (Phantom, Vampire)")
                .define("umbrellaBlocksOriginsSun", true);
        b.pop();

        b.comment("Shivering: when cold, the body warms itself at the cost of hunger. Body temperature: -100 = freezing damage.").push("shivering");
        SHIVER = b.define("enabled", true);
        SHIVER_START = b.defineInRange("startBodyTemp", -40.0, -150, 0);
        SHIVER_MIN_WARMTH = b.comment("Body warmth per second when shivering starts").defineInRange("minWarmthPerSecond", 0.2, 0, 10);
        SHIVER_MAX_WARMTH = b.comment("Body warmth per second at -100").defineInRange("maxWarmthPerSecond", 0.5, 0, 10);
        SHIVER_MIN_FOOD = b.comment("No shivering at or below this food level").defineInRange("minFood", 6, 0, 20);
        SHIVER_EXHAUSTION = b.comment("Food exhaustion per second at full intensity (4 = one food point)").defineInRange("exhaustionPerSecond", 0.15, 0, 10);
        b.pop();

        b.comment("Right-click a lit campfire with a log to give everything nearby Cold Sweat's Warmth effect.",
                "Tags: csextras:campfire_fuel (items), csextras:stokable_campfires (blocks)").push("campfire");
        CAMPFIRE = b.define("enabled", true);
        CAMPFIRE_RADIUS = b.defineInRange("radius", 6.0, 1, 32);
        CAMPFIRE_DURATION = b.comment("Warmth duration in ticks").defineInRange("duration", 3600, 20, 72000);
        b.pop();

        b.comment("Eating a hot, hearty food gives Cold Sweat's Warmth effect. Tag: csextras:warming_foods").push("warmingFoods");
        WARMING_FOODS = b.define("enabled", true);
        WARMING_FOOD_DURATION = b.comment("Warmth duration in ticks").defineInRange("duration", 3600, 20, 72000);
        WARMING_FOOD_AMPLIFIER = b.comment("Warmth level minus one (0 = Warmth I)").defineInRange("amplifier", 0, 0, 9);
        b.pop();

        b.comment("Sweating: when hot and hydrated, the body cools itself and uses a little extra thirst.").push("sweat");
        SWEAT = b.define("enabled", true);
        SWEAT_START = b.defineInRange("startBodyTemp", 30.0, 0, 150);
        SWEAT_MAX_COOLING = b.comment("Cooling at 100 body temp, in Cold Sweat units (0.2 = 9F)").defineInRange("maxCooling", 0.2, 0, 2);
        SWEAT_MIN_THIRST = b.comment("No sweating at or below this thirst").defineInRange("minThirst", 6, 0, 20);
        SWEAT_FULL_THIRST = b.comment("Full sweating at or above this thirst").defineInRange("fullThirst", 12, 0, 20);
        SWEAT_EXHAUSTION = b.comment("Thirst exhaustion per second at full sweat (Thirst Was Taken)").defineInRange("thirstExhaustionPerSecond", 0.05, 0, 10);
        b.pop();

        b.comment("Heartbeat sound as you approach overheating (client side)").push("heartbeat");
        HEARTBEAT = b.define("enabled", true);
        HEARTBEAT_START = b.defineInRange("startBodyTemp", 60.0, 0, 150);
        HEARTBEAT_VOLUME = b.defineInRange("volume", 0.8, 0, 2);
        HEARTBEAT_SOUND = b.comment("Falls back to a low note block drum if this sound is missing")
                .define("sound", "subtle_effects:entity.player.heartbeat");
        b.pop();

        b.comment("Pitchfork (csextras:pitchforks) clears csextras:pitchfork_mineable fast and sweeps csextras:pitchfork_sweepable around the target. Sneak to break one block.").push("pitchfork");
        PITCHFORK = b.define("enabled", true);
        PITCHFORK_SPEED = b.defineInRange("speedMultiplier", 6.0, 1, 100);
        PITCHFORK_RADIUS = b.comment("Sweep radius (2 = 5x5, one block up and down)").defineInRange("sweepRadius", 2, 0, 4);
        b.pop();

        b.comment("Sandstorms: rain weather in a biome with no precipitation that is in csextras:has_sandstorms (or has_red_sandstorms).").push("sandstorms");
        SANDSTORMS = b.define("enabled", true);
        SANDSTORM_SLOW = b.comment("Movement speed change for exposed mobs (-0.2 = 20% slower). Tag csextras:ignores_sandstorms is exempt")
                .defineInRange("slowness", -0.2, -1, 0);
        SANDSTORM_FOLLOW_RANGE = b.comment("Follow range change for exposed mobs (they can't see as far)").defineInRange("followRange", -0.5, -1, 0);
        SANDSTORM_SLOWS_PLAYERS = b.define("slowPlayers", true);
        BREEZES = b.comment("Breezes spawn naturally (day or night) on the surface during sandstorms").define("breezes", true);
        BREEZE_WEIGHT = b.defineInRange("breezeWeight", 20, 1, 1000);
        BREEZE_MAX_NEARBY = b.comment("No new breeze spawns if this many are already within 48 blocks").defineInRange("breezeMaxNearby", 3, 1, 64);
        PILE_CHANCE = b.comment("Chance per weather tick on a column that a sand pile layer is added (vanilla snow uses the same tick)")
                .defineInRange("pileChance", 0.5, 0, 1);
        PILE_MAX_LAYERS = b.comment("Sand piles stop growing at this many layers (0 = no accumulation)").defineInRange("pileMaxLayers", 2, 0, 8);
        CAULDRON_FILL_CHANCE = b.comment("Chance per random tick that an exposed sand cauldron gains a level during a sandstorm")
                .defineInRange("cauldronFillChance", 0.3, 0, 1);
        b.pop();

        HIDDEN_ITEMS = b.comment("Items removed from creative tabs (recipes are disabled by data, JEI hides c:hidden_from_recipe_viewers)")
                .defineListAllowEmpty("hiddenItems", plasticShovels(), () -> "", o -> o instanceof String);

        SPEC = b.build();
    }

    private static List<String> plasticShovels() {
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        List<String> out = new java.util.ArrayList<>();
        for (String c : colors) out.add("convenientdecor:" + c + "_plastic_shovel");
        return out;
    }

    private Config() {
    }
}
