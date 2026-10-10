package dev.eteil.butcheryam2;

import dev.eteil.butcheryam2.gen.GeneratedMobs;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Part 1's option set, plus one toggle per Part 2 creature. */
public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SPECIFIC_TOOLS;
    public static final ModConfigSpec.BooleanValue ORGANS;
    public static final ModConfigSpec.BooleanValue LOOTING_ENCHANT;
    public static final ModConfigSpec.BooleanValue INSTANT_BLEED;
    public static final ModConfigSpec.BooleanValue BLOOD_PUDDLE;
    public static final ModConfigSpec.BooleanValue BLOOD_SPREAD;
    public static final ModConfigSpec.BooleanValue SHOW_HINTS;

    /** Config key per mob name, as used by {@code MobDef#name()}. */
    private static final Map<String, String> MOB_LABELS = new LinkedHashMap<>();
    private static final Map<String, ModConfigSpec.BooleanValue> MOBS = new LinkedHashMap<>();

    public static final ModConfigSpec SPEC;

    static {
        for (GeneratedMobs.Entry e : GeneratedMobs.ALL) {
            MOB_LABELS.put(e.name(), e.label());
        }

        BUILDER.push("General");
        SPECIFIC_TOOLS = BUILDER.comment("Only cleavers drop carcasses")
                .define("Only Cleavers Drop Carcasses", true);
        ORGANS = BUILDER.comment("Enable organ drops")
                .define("Organs", true);
        LOOTING_ENCHANT = BUILDER.comment("Enable looting when butchering carcasses")
                .define("Looting Enchant", true);
        INSTANT_BLEED = BUILDER.comment("Instantly bleed carcasses.")
                .define("Instant Bleed", false);
        BLOOD_PUDDLE = BUILDER.comment("Enable blood puddles dropping when no blood grate is below a carcass.")
                .define("Blood Puddle", true);
        BLOOD_SPREAD = BUILDER.comment("Enable blood spread")
                .define("Blood Spread", true);
        SHOW_HINTS = BUILDER.comment("Show overlay hints")
                .define("Show Overlay Hints", true);
        BUILDER.pop();

        BUILDER.push("Mobs");
        MOB_LABELS.forEach((name, label) ->
                MOBS.put(name, BUILDER.comment("Enable " + label + " Carcass").define(label, true)));
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static boolean enabled(String mob) {
        ModConfigSpec.BooleanValue value = MOBS.get(mob);
        return value == null || value.get();
    }

    private Config() {}
}
