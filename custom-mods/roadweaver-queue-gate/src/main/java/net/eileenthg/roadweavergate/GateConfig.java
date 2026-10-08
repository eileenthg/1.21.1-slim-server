package net.eileenthg.roadweavergate;

import net.neoforged.neoforge.common.ModConfigSpec;

/** A common config: config/roadweavergate-common.toml. */
public final class GateConfig {

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.BooleanValue REFRESH_ON_PLAYER_MOVE;
    private static final ModConfigSpec.IntValue FALLBACK_TICKS;
    private static final ModConfigSpec.BooleanValue LOG_REFRESHES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENABLED = b
                .comment("Gate RoadWeaver's road-queue refresh. Without this mod RoadWeaver re-reads and parses",
                        "data/roadweaver/persistence/structures/<dim>/state.json from disk every server tick.",
                        "The queue is always refreshed right after RoadWeaver saves new or changed road connections,",
                        "which is what happens when a player reaches an area that hasn't been planned yet.",
                        "false = RoadWeaver's original every-tick behaviour.")
                .define("enabled", true);
        REFRESH_ON_PLAYER_MOVE = b
                .comment("Also refresh when a player teleports, changes dimension, respawns or logs in.")
                .define("refreshOnTeleport", true);
        FALLBACK_TICKS = b
                .comment("Safety net: refresh at least this often, in ticks, even if nothing changed. 0 = never.",
                        "600 = every 30 seconds.")
                .defineInRange("fallbackRefreshTicks", 600, 0, 72000);
        LOG_REFRESHES = b
                .comment("Log each refresh and its reason to the game log, for checking the gate works.")
                .define("logRefreshes", false);
        SPEC = b.build();
    }

    private GateConfig() {}

    public static boolean enabled() {
        return !SPEC.isLoaded() || ENABLED.get();
    }

    public static boolean refreshOnPlayerMove() {
        return !SPEC.isLoaded() || REFRESH_ON_PLAYER_MOVE.get();
    }

    public static int fallbackTicks() {
        return SPEC.isLoaded() ? FALLBACK_TICKS.get() : 600;
    }

    public static boolean logRefreshes() {
        return SPEC.isLoaded() && LOG_REFRESHES.get();
    }
}
