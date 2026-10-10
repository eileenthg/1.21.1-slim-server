package com.github.eileenthg.colonyhandover.handover;

import com.github.eileenthg.colonyhandover.handover.ColonySnapshot.Parent;

/**
 * Fits MineColonies' two unordered parents into MCA's father and mother slots.
 *
 * <h2>The mismatch</h2>
 *
 * MineColonies records a child's parents as two names in no particular order and
 * with no gender ({@link ParentGender} recovers the gender). It applies
 * <em>no gender filter</em> when choosing them: {@code ReproductionManager:128}
 * excludes candidates who are partnered, same-named, related or children, and
 * nothing else, and partners themselves are formed by that same gender-blind
 * selection at {@code :159-160}. Same-gender couples are therefore ordinary, not
 * an edge case - roughly half of unpartnered pairings by chance.
 *
 * <p>MCA's {@code FamilyTreeNode} has exactly two slots, {@code UUID father} and
 * {@code UUID mother}, with {@code Util.NIL_UUID} for absent. There is no generic
 * parent list. So two mothers cannot both be recorded as mothers.
 *
 * <h2>The two rules</h2>
 *
 * <ol>
 *   <li><b>Never invent a gender.</b> A parent whose gender we could not recover
 *       is left out entirely rather than dropped into whichever slot is free.
 *       Their child keeps the other parent and an empty slot.</li>
 *   <li><b>Never drop a real parent.</b> When both parents share a gender, the
 *       second goes in the vacant slot. That slot label is then wrong - MCA's
 *       family screen will call a second mother "Father" - but
 *       {@code FamilyTreeNode} stores {@code gender} as its own field, so she is
 *       still recorded as female, and the relationship walk reads structure
 *       rather than labels. A mislabelled slot is a cosmetic lie; dropping her
 *       would delete a true ancestry link.</li>
 * </ol>
 *
 * These pull in opposite directions on purpose. Rule 1 refuses to state something
 * unknown; rule 2 refuses to discard something known.
 */
public final class ParentSlots {

    /**
     * @param father    the parent occupying the father slot, or null
     * @param mother    the parent occupying the mother slot, or null
     * @param misfiled  true when a parent sits in a slot contradicting their
     *                  gender, i.e. this was a same-gender pair
     * @param dropped   parents we knew of but could not place, for reporting
     */
    public record Slots(Parent father, Parent mother, boolean misfiled, int dropped) {}

    public static Slots assign(Parent a, Parent b) {
        final boolean aOk = a != null && a.placeable();
        final boolean bOk = b != null && b.placeable();

        // Anyone named but genderless is a known loss, counted rather than guessed.
        int dropped = 0;
        if (a != null && a.known() && !aOk) {
            dropped++;
        }
        if (b != null && b.known() && !bOk) {
            dropped++;
        }

        if (!aOk && !bOk) {
            return new Slots(null, null, false, dropped);
        }
        if (aOk && !bOk) {
            return single(a, dropped);
        }
        if (!aOk) {
            return single(b, dropped);
        }

        // Both placeable. Different genders fit naturally.
        if (!a.female().equals(b.female())) {
            return a.female()
                    ? new Slots(b, a, false, dropped)
                    : new Slots(a, b, false, dropped);
        }

        // Same gender: first to its own slot, second to whatever is left.
        return a.female()
                ? new Slots(b, a, true, dropped)
                : new Slots(a, b, true, dropped);
    }

    private static Slots single(Parent p, int dropped) {
        return p.female()
                ? new Slots(null, p, false, dropped)
                : new Slots(p, null, false, dropped);
    }

    private ParentSlots() {}
}
