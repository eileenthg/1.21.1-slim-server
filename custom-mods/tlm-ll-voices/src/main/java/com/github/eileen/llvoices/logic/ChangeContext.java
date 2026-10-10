package com.github.eileen.llvoices.logic;

/**
 * A snapshot of what was going on around a maid when her trust/fear moved, plus how much they moved.
 * Built on the server from the live entity; kept as plain data so {@link #classify()} can be unit-tested.
 */
public record ChangeContext(
        int deltaTrust,
        int deltaFear,
        boolean hurtRecently,
        boolean hurtByOwner,
        boolean maidDiedNearby,
        boolean ateRecently,
        boolean ridingOrThrown,
        boolean leashed,
        boolean headItemChanged,
        boolean ownerStaringWithItem,
        boolean justSat,
        boolean sleeping,
        boolean armorJustCompleted,
        boolean inDarkness,
        boolean inWater,
        boolean fireworksNearby,
        boolean working,
        boolean companionsNearby,
        boolean hasPositiveEffect
) {
    /** Any single passive rule in L&L moves a value by at most 8 (witnessing a maid die). Bigger jumps are edits. */
    public static final int EDIT_THRESHOLD = 10;

    public ChangeCause classify() {
        if (Math.abs(deltaTrust) >= EDIT_THRESHOLD || Math.abs(deltaFear) >= EDIT_THRESHOLD) {
            return ChangeCause.EDITED;
        }
        if (hurtRecently) {
            return hurtByOwner ? ChangeCause.HURT_BY_OWNER : ChangeCause.HURT;
        }
        if (maidDiedNearby && deltaFear > 0) {
            return ChangeCause.MAID_DIED_NEARBY;
        }
        if (ridingOrThrown) {
            return ChangeCause.CARRIED_OR_THROWN;
        }
        if (leashed) {
            return ChangeCause.LEASHED;
        }
        if (headItemChanged) {
            return ChangeCause.GIFT_ON_HEAD;
        }
        if (ownerStaringWithItem) {
            return ChangeCause.OWNER_STARING_WITH_ITEM;
        }
        if (ateRecently && deltaTrust > 0) {
            return ChangeCause.FED;
        }
        if (justSat) {
            return ChangeCause.SAT_DOWN;
        }
        if (sleeping && deltaTrust > 0) {
            return ChangeCause.SLEPT;
        }
        if (armorJustCompleted && deltaTrust >= 3) {
            return ChangeCause.ARMOR_COMPLETE;
        }
        if (inDarkness && deltaFear > 0 && deltaTrust == 0) {
            return ChangeCause.DARKNESS;
        }
        if (inWater && deltaTrust < 0 && deltaFear == 0) {
            return ChangeCause.SOAKED;
        }
        if (fireworksNearby && deltaTrust > 0 && deltaFear == 0) {
            return ChangeCause.FIREWORKS;
        }
        if (deltaTrust < 0 && deltaFear < 0) {
            return ChangeCause.IGNORED;
        }
        if (deltaTrust > 0 && deltaFear < 0) {
            return ChangeCause.TIME_TOGETHER;
        }
        if (deltaTrust == 0 && deltaFear < 0) {
            if (working) {
                return ChangeCause.WORKING;
            }
            if (companionsNearby) {
                return ChangeCause.COMPANIONS;
            }
        }
        if (deltaTrust > 0 && deltaFear == 0) {
            return hasPositiveEffect ? ChangeCause.BUFFED : ChangeCause.RESTING;
        }
        return ChangeCause.UNKNOWN;
    }

    /** The same surroundings with different totals, for changes that trickle in over a couple of seconds. */
    public ChangeContext withDeltas(int trust, int fear) {
        return new ChangeContext(trust, fear, hurtRecently, hurtByOwner, maidDiedNearby, ateRecently, ridingOrThrown,
                leashed, headItemChanged, ownerStaringWithItem, justSat, sleeping, armorJustCompleted, inDarkness,
                inWater, fireworksNearby, working, companionsNearby, hasPositiveEffect);
    }

    public static Builder builder(int deltaTrust, int deltaFear) {
        return new Builder(deltaTrust, deltaFear);
    }

    public static final class Builder {
        private final int deltaTrust;
        private final int deltaFear;
        private boolean hurtRecently;
        private boolean hurtByOwner;
        private boolean maidDiedNearby;
        private boolean ateRecently;
        private boolean ridingOrThrown;
        private boolean leashed;
        private boolean headItemChanged;
        private boolean ownerStaringWithItem;
        private boolean justSat;
        private boolean sleeping;
        private boolean armorJustCompleted;
        private boolean inDarkness;
        private boolean inWater;
        private boolean fireworksNearby;
        private boolean working;
        private boolean companionsNearby;
        private boolean hasPositiveEffect;

        private Builder(int deltaTrust, int deltaFear) {
            this.deltaTrust = deltaTrust;
            this.deltaFear = deltaFear;
        }

        public Builder hurt(boolean recently, boolean byOwner) {
            this.hurtRecently = recently;
            this.hurtByOwner = byOwner;
            return this;
        }

        public Builder maidDiedNearby(boolean value) {
            this.maidDiedNearby = value;
            return this;
        }

        public Builder ateRecently(boolean value) {
            this.ateRecently = value;
            return this;
        }

        public Builder ridingOrThrown(boolean value) {
            this.ridingOrThrown = value;
            return this;
        }

        public Builder leashed(boolean value) {
            this.leashed = value;
            return this;
        }

        public Builder headItemChanged(boolean value) {
            this.headItemChanged = value;
            return this;
        }

        public Builder ownerStaringWithItem(boolean value) {
            this.ownerStaringWithItem = value;
            return this;
        }

        public Builder justSat(boolean value) {
            this.justSat = value;
            return this;
        }

        public Builder sleeping(boolean value) {
            this.sleeping = value;
            return this;
        }

        public Builder armorJustCompleted(boolean value) {
            this.armorJustCompleted = value;
            return this;
        }

        public Builder inDarkness(boolean value) {
            this.inDarkness = value;
            return this;
        }

        public Builder inWater(boolean value) {
            this.inWater = value;
            return this;
        }

        public Builder fireworksNearby(boolean value) {
            this.fireworksNearby = value;
            return this;
        }

        public Builder working(boolean value) {
            this.working = value;
            return this;
        }

        public Builder companionsNearby(boolean value) {
            this.companionsNearby = value;
            return this;
        }

        public Builder hasPositiveEffect(boolean value) {
            this.hasPositiveEffect = value;
            return this;
        }

        public ChangeContext build() {
            return new ChangeContext(deltaTrust, deltaFear, hurtRecently, hurtByOwner, maidDiedNearby, ateRecently,
                    ridingOrThrown, leashed, headItemChanged, ownerStaringWithItem, justSat, sleeping,
                    armorJustCompleted, inDarkness, inWater, fireworksNearby, working, companionsNearby,
                    hasPositiveEffect);
        }
    }
}
