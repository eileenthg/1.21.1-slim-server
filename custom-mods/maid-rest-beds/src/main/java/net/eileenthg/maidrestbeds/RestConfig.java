package net.eileenthg.maidrestbeds;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * A common config, so it is never synced to clients and a client without the mod can still join.
 * A maid bed gives full health and the "Sleep" favorability (2 in plain TLM, 18 with Vanilla Companion), once per day.
 */
public final class RestConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ALLOW_BEDS;
    public static final ModConfigSpec.BooleanValue ALLOW_HAMMOCKS;
    public static final ModConfigSpec.DoubleValue BED_FAVORABILITY;
    public static final ModConfigSpec.DoubleValue HAMMOCK_FAVORABILITY;
    public static final ModConfigSpec.DoubleValue BED_HEAL;
    public static final ModConfigSpec.DoubleValue HAMMOCK_HEAL;
    public static final ModConfigSpec.BooleanValue SKIP_CLAIMED;
    public static final ModConfigSpec.BooleanValue WAKE_ON_CLICK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ALLOW_BEDS = b.comment("Maids may sleep in ordinary beds when no maid bed is free.")
                .define("allowBeds", true);
        ALLOW_HAMMOCKS = b.comment("Maids may sleep in Comforts hammocks when no maid bed is free.")
                .define("allowHammocks", true);
        BED_FAVORABILITY = b.comment("Share of a maid bed's sleep favorability that an ordinary bed gives, rounded, at least 1.",
                        "It uses the same once-a-day cooldown as the maid bed, so a maid cannot collect both.",
                        "0 gives nothing and leaves the cooldown untouched.")
                .defineInRange("bedFavorabilityShare", 0.5, 0.0, 1.0);
        HAMMOCK_FAVORABILITY = b.comment("Share of a maid bed's sleep favorability that a hammock gives. Same rules as bedFavorabilityShare.")
                .defineInRange("hammockFavorabilityShare", 0.5, 0.0, 1.0);
        BED_HEAL = b.comment("Share of max health an ordinary bed restores (a maid bed restores all of it).")
                .defineInRange("bedHealFraction", 0.5, 0.0, 1.0);
        HAMMOCK_HEAL = b.comment("Share of max health a hammock restores.")
                .defineInRange("hammockHealFraction", 0.5, 0.0, 1.0);
        SKIP_CLAIMED = b.comment("Leave alone beds that a villager has claimed as its home.")
                .define("skipVillagerClaimedBeds", true);
        WAKE_ON_CLICK = b.comment("A player who clicks a bed or hammock with a sleeping maid in it wakes her and gets in,",
                        "the same way villagers are woken. Maid beds are never affected.")
                .define("wakeMaidsOnBedClick", true);
        SPEC = b.build();
    }

    private RestConfig() {}
}
