package com.github.eileen.llvoices;

import com.github.eileen.llvoices.logic.DriftLevel;
import com.github.eileen.llvoices.logic.DriftZone;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DriftZoneTest {
    @Test
    public void defaultValuesAreNotNearAnything() {
        // Love & Loathe's starting point: trust 40, fear 10.
        for (DriftZone zone : DriftZone.values()) {
            assertEquals(zone.name(), DriftLevel.NONE, zone.level(40, 10));
        }
    }

    @Test
    public void coreMatchesLoveLoatheConditions() {
        assertEquals(DriftLevel.CORE, DriftZone.BETRAYAL.level(9, 91));
        assertEquals(DriftLevel.BRINK, DriftZone.BETRAYAL.level(10, 91));
        assertEquals(DriftLevel.CORE, DriftZone.FORGETTING.level(10, 10));
        assertEquals(DriftLevel.CORE, DriftZone.DOTING.level(90, 10));
        assertEquals(DriftLevel.CORE, DriftZone.DEVOTED.level(80, 80));
        assertEquals(DriftLevel.CORE, DriftZone.PANIC.level(20, 50));
        assertEquals(DriftLevel.BRINK, DriftZone.PANIC.level(20, 49));
    }

    @Test
    public void ringsGetDeeperAsValuesApproach() {
        assertEquals(DriftLevel.NONE, DriftZone.BETRAYAL.level(30, 70));
        // 11 + 11 points from trust 9 / fear 91.
        assertEquals(DriftLevel.NEAR, DriftZone.BETRAYAL.level(20, 80));
        assertEquals(DriftLevel.NONE, DriftZone.BETRAYAL.level(25, 80));
        assertEquals(DriftLevel.BRINK, DriftZone.BETRAYAL.level(14, 86));
        assertEquals(DriftLevel.NEAR, DriftZone.DOTING.level(75, 15));
        assertEquals(DriftLevel.BRINK, DriftZone.DOTING.level(85, 12));
    }

    @Test
    public void leavingARingNeedsHysteresis() {
        // DOTING distance at (78, 10) is 12: just outside BRINK (10).
        assertEquals(DriftLevel.NEAR, DriftZone.DOTING.level(78, 10));
        assertEquals(DriftLevel.BRINK, DriftZone.DOTING.level(78, 10, DriftLevel.BRINK));
        // Distance 14 is past the 3-point slack, so she has left BRINK.
        assertEquals(DriftLevel.NEAR, DriftZone.DOTING.level(76, 10, DriftLevel.BRINK));
        // Going deeper never waits.
        assertEquals(DriftLevel.BRINK, DriftZone.DOTING.level(85, 10, DriftLevel.NONE));
    }
}
