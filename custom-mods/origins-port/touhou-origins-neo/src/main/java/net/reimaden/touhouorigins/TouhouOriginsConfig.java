package net.reimaden.touhouorigins;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The one tunable in this port: how hard {@code this_side_down} and {@code turnabout} reverse
 * gravity, extracted from JSON into a config at Eileen's request.
 *
 * <p><b>The actual math, verified from source</b> ({@code AttributeModifierOperation.java}):
 * {@code multiply_total_multiplicative} computes {@code total * (1.0 + value)}, not
 * {@code total * value}. So a configured value of {@code -1.01} gives an effective per-tick
 * multiplier of {@code 1.0 + (-1.01) = -0.01} — the velocity flips sign and loses 99% of its
 * magnitude <i>every tick</i>. That is heavy damping toward zero, not the amplifying bounce it
 * looks like at a glance. This was explained backwards earlier in the port; this file is the
 * correction, not just the config.
 *
 * <p>Both origin powers still author their {@code condition} field in JSON as before ({@code
 * resource == 1} for the self-flip, {@code gamemode != creative} for both) — {@code
 * PowerLookup.forEach}/{@code hasActive} evaluate {@code Power.condition()} generically regardless
 * of which {@link net.reimaden.touhouorigins.power.FlipGravityPower} instance is behind it, so
 * moving the multiplier here does not touch how the effect is gated.
 */
public final class TouhouOriginsConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue THIS_SIDE_DOWN_MULTIPLIER = BUILDER
        .comment(
            "Reverse-gravity strength for amanojaku's self-flip (this_side_down).",
            "Applied each tick as velocity.y *= (1.0 + this value) while flipped — see the class",
            "comment on TouhouOriginsConfig for why that is not the same as a plain multiplier.",
            "Upstream Touhou Origins default: -1.10  (effective multiplier -0.10, heavily damped).",
            "Unchanged from upstream here; Eileen's feedback was specifically about turnabout below.")
        .defineInRange("this_side_down_gravity_multiplier", -1.10, -1.999, 100.0);

    private static final ModConfigSpec.DoubleValue TURNABOUT_MULTIPLIER = BUILDER
        .comment(
            "Reverse-gravity strength for the target of amanojaku's turnabout.",
            "Applied each tick as velocity.y *= (1.0 + this value) while the effect is active.",
            "Upstream Touhou Origins default: -1.01  (effective multiplier -0.01 — almost total",
            "damping, which is why the launch felt weak and the timing felt inconsistent).",
            "",
            "Guidance for retuning: effective multiplier = 1.0 + value.",
            "  value in (-2, 0): a damped, sign-flipping bounce. Closer to -1.0 = less damping,",
            "                    a punchier launch, still naturally bounded.",
            "  value == -2.0:    velocity reflects with no energy loss — a strong, sustained",
            "                    bounce that never dies down on its own.",
            "  value <  -2.0:    the bounce GROWS every tick with no upper bound. Avoid — this",
            "                    is how you send a mob into the stratosphere or out of the",
            "                    loaded world.",
            "Set to -1.70 in this build (effective multiplier -0.70) for a noticeably faster,",
            "more satisfying launch than upstream's -1.01, while staying well clear of runaway",
            "growth.")
        .defineInRange("turnabout_gravity_multiplier", -1.70, -1.999, 100.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private TouhouOriginsConfig() {}

    public static double thisSideDownMultiplier() {
        if (!SPEC.isLoaded()) return -1.10;
        return THIS_SIDE_DOWN_MULTIPLIER.get();
    }

    public static double turnaboutMultiplier() {
        if (!SPEC.isLoaded()) return -1.70;
        return TURNABOUT_MULTIPLIER.get();
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}
