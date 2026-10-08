package net.eileenthg.maidchef;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ChefConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue DEFAULT_RADIUS;
    public static final ModConfigSpec.IntValue MAX_RADIUS;
    public static final ModConfigSpec.IntValue DEFAULT_TARGET;
    public static final ModConfigSpec.IntValue RECENT_MEMORY;
    public static final ModConfigSpec.IntValue CHAIN_DEPTH;
    public static final ModConfigSpec.DoubleValue NUTRITION_WEIGHT;
    public static final ModConfigSpec.DoubleValue SATURATION_WEIGHT;
    public static final ModConfigSpec.DoubleValue EFFECT_WEIGHT;
    public static final ModConfigSpec.DoubleValue FEAST_WEIGHT;
    public static final ModConfigSpec.IntValue IDLE_REPLAN_TICKS;
    public static final ModConfigSpec.BooleanValue USE_CRAFTING_TABLE;
    public static final ModConfigSpec.BooleanValue USE_CUTTING_BOARD;
    public static final ModConfigSpec.BooleanValue ANNOUNCE;
    public static final ModConfigSpec.IntValue RETURN_WAIT_SECONDS;
    public static final ModConfigSpec.IntValue LEAVE_AFTER_TICKS;
    public static final ModConfigSpec.BooleanValue DEBUG_LOG;
    public static final ModConfigSpec.IntValue REQUEST_MAX_MISSING;
    public static final ModConfigSpec.IntValue REQUEST_MIN_GAIN_PERCENT;
    public static final ModConfigSpec.IntValue REQUEST_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue REQUEST_REPEAT_TICKS;
    public static final ModConfigSpec.BooleanValue REQUEST_CHAT;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> NEVER_REQUEST;
    public static final ModConfigSpec.BooleanValue ONLY_KNOWN;
    public static final ModConfigSpec.IntValue TARGET_MAX_MISSING;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_RESULTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_RECIPES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FUELS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("kitchen");
        DEFAULT_RADIUS = b.comment("Kitchen radius a new Chef maid starts with (blocks around her home/work point). Each maid can change hers in the Chef config screen.")
                .defineInRange("defaultRadius", 8, 3, 64);
        MAX_RADIUS = b.comment("Largest kitchen radius the config screen allows.")
                .defineInRange("maxRadius", 32, 3, 64);
        DEFAULT_TARGET = b.comment("How many of a dish a new Chef maid makes per round. Each maid can change hers in the Chef config screen.")
                .defineInRange("defaultBatch", 8, 1, 64);
        RECENT_MEMORY = b.comment("How many recent dishes a new Chef maid remembers and skips, so she rotates through different food. Each maid can change hers in the Chef config screen.",
                        "When only remembered dishes are possible she makes the oldest one she can; ones she can no longer make are forgotten.")
                .defineInRange("recentMemory", 3, 0, 32);
        CHAIN_DEPTH = b.comment("How many steps deep she plans: 0 = only dishes made straight from what's in the chest; 3 = she may make an",
                        "ingredient of an ingredient of an ingredient first (e.g. cook rice, then make the feast, then serve it).")
                .defineInRange("chainDepth", 3, 0, 6);
        IDLE_REPLAN_TICKS = b.comment("When she can't cook anything, how many ticks she waits before checking the ingredients and stations again.")
                .defineInRange("idleReplanTicks", 200, 20, 6000);
        RETURN_WAIT_SECONDS = b.comment("If a player picks up something that popped out of her station, she pauses and asks for it back.",
                        "After this many seconds without getting it back she gives up on that dish (it counts as recently made) and plans a different one.")
                .defineInRange("returnWaitSeconds", 60, 5, 600);
        LEAVE_AFTER_TICKS = b.comment("Slow stations (kegs, fermentation tanks, titration barrels, long drying): when a load will take longer than this many",
                        "ticks she starts it, goes on with other cooking, and comes back for it once it's done. 0 = always wait at the station.")
                .defineInRange("leaveAfterTicks", 2400, 0, 1000000);
        DEBUG_LOG = b.comment("Log every fetch/load/collect/delivery step to the server log (for troubleshooting).")
                .define("debugLog", false);
        ANNOUNCE = b.comment("Show a chat bubble when she starts a dish or can't cook anything.")
                .define("announce", true);
        b.pop();

        b.push("requests");
        REQUEST_MAX_MISSING = b.comment("She asks her owner for ingredients that would let her make a much better dish, if no more than this many",
                        "kinds of items are missing. 0 turns requests off.")
                .defineInRange("maxMissingKinds", 1, 0, 3);
        REQUEST_MIN_GAIN_PERCENT = b.comment("The dish has to be worth at least this much more than what she can make now (percent).")
                .defineInRange("minGainPercent", 50, 0, 1000);
        REQUEST_INTERVAL_TICKS = b.comment("How often she thinks about what to ask for (ticks; 6000 = 5 minutes).")
                .defineInRange("intervalTicks", 6000, 200, 240000);
        REQUEST_REPEAT_TICKS = b.comment("She won't repeat the same request sooner than this (ticks; 24000 = one Minecraft day).")
                .defineInRange("repeatTicks", 24000, 200, 2400000);
        NEVER_REQUEST = b.comment("Items no maid ever asks for (boss loot and the like). Item ids or #tags. Rare/epic items and prepared food",
                        "(anything cooked from other food) are never asked for anyway; she asks for its ingredients instead.")
                .defineListAllowEmpty("neverRequest", List.of("ends_delight:liquid_dragon_egg", "ends_delight:raw_dragon_meat",
                        "ends_delight:dragon_leg", "minecraft:dragon_egg", "minecraft:dragon_breath", "minecraft:nether_star"),
                        () -> "", o -> o instanceof String);
        ONLY_KNOWN = b.comment("Only ask for items her owner has picked up or crafted at least once (from the owner's statistics),",
                        "so she doesn't ask for meat from mobs you've never met. Requests then only happen while the owner is online.")
                .define("onlyAskForKnownItems", true);
        TARGET_MAX_MISSING = b.comment("In \"Prepare X\" mode she asks for whatever X still needs, up to this many kinds of items.")
                .defineInRange("targetMaxMissingKinds", 3, 0, 6);
        REQUEST_CHAT = b.comment("Also send the request to her owner's chat, with a [Never ask for ...] button per item.")
                .define("chatMessage", true);
        b.pop();

        b.push("value");
        b.comment("Dish value = nutrition * nutritionWeight + saturation * saturationWeight + effect bonus.",
                "Effect bonus = effectWeight * (amplifier + 1) * minutes (capped at 5) * chance, for each beneficial effect.",
                "Farmer's Delight feast blocks are worth servings * (value of one serving) * feastWeight.",
                "A recipe is only considered if its output is worth more than the food it consumes.");
        NUTRITION_WEIGHT = b.defineInRange("nutritionWeight", 1.0, 0.0, 100.0);
        SATURATION_WEIGHT = b.defineInRange("saturationWeight", 1.0, 0.0, 100.0);
        EFFECT_WEIGHT = b.defineInRange("effectWeight", 1.0, 0.0, 100.0);
        FEAST_WEIGHT = b.defineInRange("feastWeight", 1.0, 0.0, 100.0);
        b.pop();

        b.push("stations");
        USE_CRAFTING_TABLE = b.comment("Allow food recipes made at a crafting table in the kitchen (pies, feasts, sandwiches...). Single-ingredient recipes (unpacking crates etc.) are always ignored.")
                .define("useCraftingTable", true);
        USE_CUTTING_BOARD = b.comment("Allow Farmer's Delight cutting board recipes as dishes. Needs a matching tool (knife etc.) in her inventory or the ingredient chest.")
                .define("useCuttingBoard", true);
        FUELS = b.comment("Items she may use as furnace/smoker fuel. Item ids, or tags as #namespace:path.")
                .defineListAllowEmpty("fuels", List.of("#minecraft:coals", "minecraft:coal_block", "minecraft:blaze_rod"), () -> "", o -> o instanceof String);
        b.pop();

        b.push("filters");
        EXCLUDED_RESULTS = b.comment("Items she never makes as a dish. Item ids, or tags as #namespace:path.")
                .defineListAllowEmpty("excludedResults", List.of("minecraft:rotten_flesh", "minecraft:spider_eye", "minecraft:poisonous_potato", "minecraft:suspicious_stew"), () -> "", o -> o instanceof String);
        EXCLUDED_RECIPES = b.comment("Recipe ids she never uses.")
                .defineListAllowEmpty("excludedRecipes", List.of(), () -> "", o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    private ChefConfig() {
    }
}
