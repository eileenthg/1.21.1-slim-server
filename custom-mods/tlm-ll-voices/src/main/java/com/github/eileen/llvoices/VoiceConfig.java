package com.github.eileen.llvoices;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config ({@code config/llvoices-common.toml}). Common rather than server so the LLM site routing, which TLM
 * also reads on the client, sees the same values on both sides.
 */
public final class VoiceConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue HEARING_RADIUS;
    public static final ModConfigSpec.IntValue MIN_SECONDS_BETWEEN_LINES;
    public static final ModConfigSpec.BooleanValue DEBUG;

    public static final ModConfigSpec.BooleanValue USE_LLM;
    public static final ModConfigSpec.ConfigValue<String> LLM_SITE;
    public static final ModConfigSpec.BooleanValue ROUTE_ALL_MAID_CHAT;
    public static final ModConfigSpec.BooleanValue FALLBACK_LINES;

    public static final ModConfigSpec.BooleanValue NEEDS_ENABLED;
    public static final ModConfigSpec.IntValue STATUS_INTERVAL_MINUTES;

    public static final ModConfigSpec.BooleanValue MOOD_ENABLED;
    public static final ModConfigSpec.BooleanValue DRIFT_ENABLED;
    public static final ModConfigSpec.IntValue DRIFT_LINGER_MINUTES;

    public static final ModConfigSpec.BooleanValue PASSIVE_ENABLED;
    public static final ModConfigSpec.DoubleValue PASSIVE_CHANCE;
    public static final ModConfigSpec.DoubleValue ROUTINE_PASSIVE_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder.comment("Master switch. When off, maids never speak up on their own because of this mod.")
                .define("enabled", true);
        HEARING_RADIUS = builder.comment("A maid only speaks up while her owner is within this many blocks.")
                .defineInRange("hearingRadius", 16, 4, 64);
        MIN_SECONDS_BETWEEN_LINES = builder.comment("Minimum gap between two lines from the same maid, in seconds.",
                        "She also waits while any chat bubble (including Love & Loathe's own) is still showing.")
                .defineInRange("minSecondsBetweenLines", 25, 5, 600);
        DEBUG = builder.comment("Tell the owner in chat what each maid is about to say and why.")
                .define("debug", false);
        builder.pop();

        builder.push("llm");
        USE_LLM = builder.comment("Generate every line with the maid's LLM (her TLM character setting), so it",
                        "sounds like her. When off, or when no LLM is available, the canned fallback lines are used.")
                .define("useLlm", true);
        LLM_SITE = builder.comment("TLM LLM site to send the lines to, e.g. \"player2\" for the Player2 app.",
                        "Must be enabled in TLM's AI settings; if it is not, the maid's own site is used.",
                        "Leave empty to always use each maid's own site.")
                .define("llmSite", "player2");
        ROUTE_ALL_MAID_CHAT = builder.comment("Also send every other maid LLM chat (normal TLM chat and Love & Loathe's",
                        "own dialogue) to llmSite, not just this mod's lines.")
                .define("routeAllMaidChat", true);
        FALLBACK_LINES = builder.comment("Show a short canned line when the LLM cannot be used",
                        "(LLM disabled, site off, no character setting, or the owner's token limit).",
                        "Lines over Love & Loathe's apiCallsPerMinute cap are skipped, not replaced.")
                .define("fallbackLines", true);
        builder.pop();

        builder.push("needs");
        NEEDS_ENABLED = builder.comment("Voice hunger (early signs, no food to eat, fed again, stuffed) and injuries.")
                .define("enabled", true);
        STATUS_INTERVAL_MINUTES = builder.comment("Roughly every this many minutes a maid near her owner checks in about",
                        "how she is doing (needs, wants, mood). 0 turns check-ins off.")
                .defineInRange("statusIntervalMinutes", 12, 0, 120);
        builder.pop();

        builder.push("emotion");
        MOOD_ENABLED = builder.comment("Speak up when Love & Loathe's mood tendency changes (friendly, fearful, bonded...).")
                .define("moodShifts", true);
        DRIFT_ENABLED = builder.comment("Show signs of drifting toward betrayal, forgetting, doting, devotion or panic",
                        "before Love & Loathe's thresholds are reached, and relief when backing away from them.")
                .define("driftWarnings", true);
        DRIFT_LINGER_MINUTES = builder.comment("While a maid stays near a threshold, remind about it this often. 0 = never.")
                .defineInRange("driftLingerMinutes", 8, 0, 120);
        builder.pop();

        builder.push("passive");
        PASSIVE_ENABLED = builder.comment("Comment on Love & Loathe's passive trust/fear changes (being hit, fed, ignored,",
                        "working, sleeping...). Changes Love & Loathe already voices itself are skipped.")
                .define("enabled", true);
        PASSIVE_CHANCE = builder.comment("Chance to comment on a notable change (hurt, fed, a maid dying, armour...).")
                .defineInRange("reactionChance", 0.6, 0.0, 1.0);
        ROUTINE_PASSIVE_CHANCE = builder.comment("Chance to comment on a routine change (working, resting, sleeping,",
                        "time passing, being ignored). These happen every few minutes per maid.")
                .defineInRange("routineReactionChance", 0.2, 0.0, 1.0);
        builder.pop();

        SPEC = builder.build();
    }

    private VoiceConfig() {
    }

    public static boolean loaded() {
        return SPEC.isLoaded();
    }
}
