package dev.eteil.butcheryam;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Mirrors the option set of the original 1.20.1 addon. */
public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SPECIFIC_TOOLS;
    public static final ModConfigSpec.BooleanValue ORGANS;
    public static final ModConfigSpec.BooleanValue LOOTING_ENCHANT;
    public static final ModConfigSpec.BooleanValue INSTANT_BLEED;
    public static final ModConfigSpec.BooleanValue BLOOD_PUDDLE;
    public static final ModConfigSpec.BooleanValue BLOOD_SPREAD;
    public static final ModConfigSpec.BooleanValue SHOW_HINTS;

    public static final ModConfigSpec.BooleanValue BALD_EAGLE;
    public static final ModConfigSpec.BooleanValue BLOBFISH;
    public static final ModConfigSpec.BooleanValue CAIMAN;
    public static final ModConfigSpec.BooleanValue CAPUCHIN;
    public static final ModConfigSpec.BooleanValue CROW;
    public static final ModConfigSpec.BooleanValue EMU;
    public static final ModConfigSpec.BooleanValue FLYING_FISH;

    public static final ModConfigSpec SPEC;

    static {
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
        BALD_EAGLE = BUILDER.comment("Enable Bald Eagle Carcass").define("Bald Eagle", true);
        BLOBFISH = BUILDER.comment("Enable Blobfish Carcass").define("Blobfish", true);
        CAIMAN = BUILDER.comment("Enable Caiman Carcass").define("Caiman", true);
        CAPUCHIN = BUILDER.comment("Enable Capuchin Carcass").define("Capuchin", true);
        CROW = BUILDER.comment("Enable Crow Carcass").define("Crow", true);
        EMU = BUILDER.comment("Enable Emu Carcass").define("Emu", true);
        FLYING_FISH = BUILDER.comment("Enable Flying Fish Carcass").define("Flying Fish", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private Config() {}
}
