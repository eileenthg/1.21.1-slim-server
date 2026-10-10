package com.github.eileen.llvoices.logic;

/**
 * Love & Loathe's hunger bands (0-100), mirrored here so the edge detection is testable without the game.
 */
public enum HungerStage {
    /** 0-9: takes starvation damage. */
    STARVING,
    /** 10-25: L&L itself complains about this every minute. */
    VERY_HUNGRY,
    /** 26-40: auto-eats below 35. The first, quiet sign we voice. */
    HUNGRY,
    /** 41-74: heals, best speed. */
    SATISFIED,
    /** 75-90: slower. */
    FULL,
    /** 91-100: takes damage from overeating. */
    OVERFED;

    public static HungerStage of(float hunger) {
        if (hunger <= 9.0F) {
            return STARVING;
        }
        if (hunger <= 25.0F) {
            return VERY_HUNGRY;
        }
        if (hunger <= 40.0F) {
            return HUNGRY;
        }
        if (hunger <= 74.0F) {
            return SATISFIED;
        }
        return hunger <= 90.0F ? FULL : OVERFED;
    }

    public boolean isHungry() {
        return this.ordinal() <= HUNGRY.ordinal();
    }
}
