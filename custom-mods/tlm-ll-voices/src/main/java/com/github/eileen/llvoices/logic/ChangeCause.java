package com.github.eileen.llvoices.logic;

import java.util.Locale;

/**
 * The likely reason a maid's trust/fear just moved. Love & Loathe changes the values silently in many places; we
 * reconstruct the reason from what is going on around her when the change shows up.
 */
public enum ChangeCause {
    HURT_BY_OWNER(false, false),
    HURT(false, false),
    MAID_DIED_NEARBY(false, false),
    FED(false, false),
    SAT_DOWN(false, true),
    SLEPT(false, true),
    ARMOR_COMPLETE(false, false),
    IGNORED(false, true),
    TIME_TOGETHER(false, true),
    WORKING(false, true),
    RESTING(false, true),
    COMPANIONS(false, true),
    BUFFED(false, false),
    /** Movement nobody explained; still worth a remark if it is a real shift. */
    UNKNOWN(false, false),

    // L&L already makes the maid speak for these, so we stay quiet to avoid a second bubble.
    LEASHED(true, false),
    CARRIED_OR_THROWN(true, false),
    GIFT_ON_HEAD(true, false),
    OWNER_STARING_WITH_ITEM(true, false),
    DARKNESS(true, false),
    SOAKED(true, false),
    FIREWORKS(true, false),
    /** A change too big to be passive, most likely the Emotion Book or a command. Not commented on. */
    EDITED(true, false);

    private final boolean voicedElsewhere;
    private final boolean routine;

    ChangeCause(boolean voicedElsewhere, boolean routine) {
        this.voicedElsewhere = voicedElsewhere;
        this.routine = routine;
    }

    /** True when L&L (or an edit) already covers this, so we should not add a bubble of our own. */
    public boolean voicedElsewhere() {
        return this.voicedElsewhere;
    }

    /** Routine ticks (sleeping, working, time passing) fire often, so they get a lower comment chance. */
    public boolean routine() {
        return this.routine;
    }

    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
