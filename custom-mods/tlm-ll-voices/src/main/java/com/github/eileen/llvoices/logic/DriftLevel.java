package com.github.eileen.llvoices.logic;

/**
 * How close a maid is to one {@link DriftZone}. Ordered from furthest to deepest.
 */
public enum DriftLevel {
    NONE,
    NEAR,
    BRINK,
    CORE;

    static DriftLevel fromDistance(int distance, int slack) {
        if (distance <= 0) {
            return CORE;
        }
        if (distance <= DriftZone.BRINK_DISTANCE + slack) {
            return BRINK;
        }
        if (distance <= DriftZone.NEAR_DISTANCE + slack) {
            return NEAR;
        }
        return NONE;
    }
}
