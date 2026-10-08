package net.eileenthg.thinairthrottle;

import net.neoforged.neoforge.common.ModConfigSpec;

/** A common config, so it lives in the instance's config/ folder as thinairthrottle-common.toml. */
public final class ThrottleConfig {

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue CHECK_INTERVAL;
    private static final ModConfigSpec.DoubleValue RECHECK_DISTANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        CHECK_INTERVAL = b
                .comment("How often, in ticks, Thin Air re-checks the air around each entity (players included).",
                        "Between checks the last result is reused, but air still drains or refills every tick.",
                        "The block at eye level (water, lava, bubble column, soul fire...) is still checked every tick.",
                        "20 = once a second. 1 = every tick, which is Thin Air's original behaviour.")
                .defineInRange("checkIntervalTicks", 20, 1, 1200);
        RECHECK_DISTANCE = b
                .comment("Re-check early if the entity has moved more than this many blocks since the last check",
                        "(teleports, ender pearls, falling, minecarts). 0 turns the early re-check off.")
                .defineInRange("recheckDistance", 4.0, 0.0, 64.0);
        SPEC = b.build();
    }

    private ThrottleConfig() {}

    public static int checkInterval() {
        return SPEC.isLoaded() ? CHECK_INTERVAL.get() : 20;
    }

    public static double recheckDistance() {
        return SPEC.isLoaded() ? RECHECK_DISTANCE.get() : 4.0;
    }
}
