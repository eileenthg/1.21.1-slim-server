package com.github.eileen.llvoices.voice;

import java.util.Locale;

/**
 * Everything a maid can bring up on her own. Priority decides which line wins when several are waiting; the cooldown
 * stops the same topic from repeating (it is tracked per kind and detail, e.g. per drift zone).
 */
public enum VoiceKind {
    HURT_CRITICAL(100, 45, true),
    DRIFT_BRINK(90, 90, false),
    HUNGER_NO_FOOD(80, 300, false),
    DRIFT_NEAR(70, 180, false),
    MOOD_SHIFT(60, 60, false),
    HURT_LOW(55, 120, true),
    DRIFT_EASING(50, 180, false),
    PASSIVE(40, 90, true),
    HUNGER_PECKISH(35, 300, false),
    HUNGER_SATED(35, 120, false),
    HUNGER_FULL(30, 300, false),
    HEALED(30, 120, false),
    DRIFT_LINGER(20, 0, false),
    STATUS(10, 0, false);

    private final int priority;
    private final int cooldownSeconds;
    private final boolean allowedWhileSleeping;

    VoiceKind(int priority, int cooldownSeconds, boolean allowedWhileSleeping) {
        this.priority = priority;
        this.cooldownSeconds = cooldownSeconds;
        this.allowedWhileSleeping = allowedWhileSleeping;
    }

    public int priority() {
        return this.priority;
    }

    public int cooldownSeconds() {
        return this.cooldownSeconds;
    }

    public boolean allowedWhileSleeping() {
        return this.allowedWhileSleeping;
    }

    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
