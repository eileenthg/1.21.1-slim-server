package com.github.eileen.llvoices.logic;

/**
 * The emotional "cliffs" Love & Loathe has: when trust and fear cross these lines a maid's behaviour changes for good
 * (she betrays, forgets you, gets possessive, or turns fanatically loyal), or she panics around you.
 * <p>
 * Each zone has three rings around L&L's own trigger: {@link DriftLevel#NEAR} (first signs), {@link DriftLevel#BRINK}
 * (about to tip) and {@link DriftLevel#CORE} (L&L's actual condition). The CORE numbers mirror L&L 2.0.9.
 */
public enum DriftZone {
    /** L&L: trust &lt; 10 and fear &gt; 90 for 10 seconds, then she turns on you. */
    BETRAYAL {
        @Override
        public int distance(int trust, int fear) {
            // How far (in points) she still is from trust 9 / fear 91.
            return Math.max(0, trust - 9) + Math.max(0, 91 - fear);
        }
    },
    /** L&L: trust &lt;= 10 and fear &lt;= 10 for 10 seconds, then she leaves for good. */
    FORGETTING {
        @Override
        public int distance(int trust, int fear) {
            return Math.max(0, trust - 10) + Math.max(0, fear - 10);
        }
    },
    /** L&L doting: trust &gt;= 90 and fear &lt;= 10. Possessive, steals, chases other maids off. */
    DOTING {
        @Override
        public int distance(int trust, int fear) {
            return Math.max(0, 90 - trust) + Math.max(0, fear - 10);
        }
    },
    /** L&L devoted: trust &gt;= 80 and fear &gt;= 80. Fanatically loyal, would die for you. */
    DEVOTED {
        @Override
        public int distance(int trust, int fear) {
            return Math.max(0, 80 - trust) + Math.max(0, 80 - fear);
        }
    },
    /** L&L fear panic: fear exceeds trust by 30 or more, so she bolts when you come close. */
    PANIC {
        @Override
        public int distance(int trust, int fear) {
            return Math.max(0, 30 - (fear - trust));
        }
    };

    /** Points away from the CORE condition at or under which the maid is on the brink. */
    public static final int BRINK_DISTANCE = 10;
    /** Points away from the CORE condition at or under which the first signs show. */
    public static final int NEAR_DISTANCE = 22;
    /** Extra points a maid has to recover before we call a ring "left", so values hovering on a line don't flap. */
    public static final int HYSTERESIS = 3;

    /**
     * Sum of the points trust and fear still have to move to meet L&L's condition; 0 means the condition holds.
     */
    public abstract int distance(int trust, int fear);

    public DriftLevel level(int trust, int fear) {
        return DriftLevel.fromDistance(this.distance(trust, fear), 0);
    }

    /**
     * Like {@link #level(int, int)}, but a maid only drops out of a ring once she is {@link #HYSTERESIS} points past
     * its edge.
     */
    public DriftLevel level(int trust, int fear, DriftLevel previous) {
        int distance = this.distance(trust, fear);
        DriftLevel raw = DriftLevel.fromDistance(distance, 0);
        if (raw.ordinal() >= previous.ordinal()) {
            return raw;
        }
        // Moving outward: stay in the previous ring until clearly out of it.
        DriftLevel sticky = DriftLevel.fromDistance(distance, HYSTERESIS);
        return sticky.ordinal() >= previous.ordinal() ? previous : sticky;
    }

    public String key() {
        return this.name().toLowerCase(java.util.Locale.ROOT);
    }
}
