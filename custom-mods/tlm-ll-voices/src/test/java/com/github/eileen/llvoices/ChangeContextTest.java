package com.github.eileen.llvoices;

import com.github.eileen.llvoices.logic.ChangeCause;
import com.github.eileen.llvoices.logic.ChangeContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChangeContextTest {
    @Test
    public void ownerHitIsRecognised() {
        ChangeContext context = ChangeContext.builder(-1, 2).hurt(true, true).build();
        assertEquals(ChangeCause.HURT_BY_OWNER, context.classify());
    }

    @Test
    public void bigJumpsAreTreatedAsEdits() {
        assertEquals(ChangeCause.EDITED, ChangeContext.builder(30, 0).build().classify());
        assertTrue(ChangeCause.EDITED.voicedElsewhere());
    }

    @Test
    public void witnessingAMaidDieBeatsOtherHints() {
        ChangeContext context = ChangeContext.builder(-4, 8).maidDiedNearby(true).working(true).build();
        assertEquals(ChangeCause.MAID_DIED_NEARBY, context.classify());
    }

    @Test
    public void loveLoatheVoicedCausesAreSkipped() {
        assertEquals(ChangeCause.LEASHED, ChangeContext.builder(-1, 0).leashed(true).build().classify());
        assertEquals(ChangeCause.DARKNESS, ChangeContext.builder(0, 1).inDarkness(true).build().classify());
        assertEquals(ChangeCause.SOAKED, ChangeContext.builder(-1, 0).inWater(true).build().classify());
        assertTrue(ChangeCause.LEASHED.voicedElsewhere());
        assertTrue(ChangeCause.DARKNESS.voicedElsewhere());
    }

    @Test
    public void routineChangesFromThePassiveTable() {
        assertEquals(ChangeCause.IGNORED, ChangeContext.builder(-1, -1).build().classify());
        assertEquals(ChangeCause.TIME_TOGETHER, ChangeContext.builder(1, -1).build().classify());
        assertEquals(ChangeCause.WORKING, ChangeContext.builder(0, -1).working(true).build().classify());
        assertEquals(ChangeCause.COMPANIONS, ChangeContext.builder(0, -1).companionsNearby(true).build().classify());
        assertEquals(ChangeCause.RESTING, ChangeContext.builder(1, 0).build().classify());
        assertEquals(ChangeCause.SLEPT, ChangeContext.builder(1, 0).sleeping(true).build().classify());
        assertEquals(ChangeCause.ARMOR_COMPLETE, ChangeContext.builder(3, -1).armorJustCompleted(true).build().classify());
        assertTrue(ChangeCause.WORKING.routine());
        assertFalse(ChangeCause.HURT_BY_OWNER.routine());
    }

    @Test
    public void eatingExplainsATrustBump() {
        assertEquals(ChangeCause.FED, ChangeContext.builder(1, -1).ateRecently(true).build().classify());
    }

    @Test
    public void deltasCanBeReplacedLater() {
        ChangeContext context = ChangeContext.builder(0, 0).working(true).build().withDeltas(0, -1);
        assertEquals(ChangeCause.WORKING, context.classify());
    }
}
