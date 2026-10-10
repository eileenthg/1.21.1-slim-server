package com.github.eileenthg.colonyhandover.handover;

import com.minecolonies.api.colony.CitizenNameFile;

import java.util.Set;

/**
 * Recovers a parent's gender from their name.
 *
 * <h2>The problem this solves</h2>
 *
 * MineColonies stores parents as two <em>names</em> in a
 * {@code Tuple<String, String>} with no father/mother ordering - the
 * {@code ReproductionManager} simply passes whichever two citizens bred. MCA's
 * {@code FamilyTreeNode} wants an explicit {@code father()} and {@code mother()}.
 * Matching the name back to a living citizen works, but only while that citizen
 * is still alive, which for a grandparent generation is usually false.
 *
 * <h2>Why the name itself is good evidence</h2>
 *
 * {@code CitizenNameFile} keeps {@code maleFirstNames} and
 * {@code femalefirstNames} as separate lists, and {@code CitizenData.generateName}
 * draws the first name from exactly one of them according to gender. So the name
 * is not decoration - it was generated <em>from</em> the gender, and reading it
 * back is inverting a function rather than guessing.
 *
 * <p>Measured across all 22 shipped name styles: 4117 male and 3429 female first
 * names, of which only 133 appear in both - 1.76%. Thirteen of the styles have no
 * overlap at all; the largest, {@code default}, has 76 of 2108 (3.6%). Names in
 * both lists are reported as unknown rather than guessed.
 *
 * <h2>What it cannot do</h2>
 *
 * It recovers <em>gender</em>, not identity - which is all that was missing. It
 * fails on player-renamed citizens, on names from a style the colony no longer
 * uses, and on the unisex overlap. Those stay unknown and are reported as such.
 */
public final class ParentGender {

    /** How a parent's gender was established, in descending order of trust. */
    public enum Source {
        /** Matched a living citizen; the gender is read, not inferred. */
        LIVING,
        /** Inferred from the colony's name file. Reliable, but an inference. */
        NAME_FILE,
        /** Renamed, unisex, or from another style. We do not know. */
        UNKNOWN
    }

    /**
     * The first name, per the ordering the colony's style uses.
     *
     * <p>{@code CitizenData.generateName} builds EASTERN names as
     * {@code "Last First"} and WESTERN as {@code "First M. Last"} or
     * {@code "First Last"}, so the first name is the last token in one and the
     * first token in the other.
     */
    public static String firstNameOf(String fullName, CitizenNameFile file) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        final String[] parts = fullName.trim().split("\\s+");
        return file.order == CitizenNameFile.NameOrder.EASTERN
                ? parts[parts.length - 1]
                : parts[0];
    }

    /**
     * Gender from the name file, or null when the name is not decisive.
     *
     * <p>A name in both lists and a name in neither are both returned as null:
     * one is genuinely unisex, the other is not from this style, and inventing a
     * parent's gender is worse than admitting we cannot tell.
     */
    public static Boolean fromNameFile(String fullName, CitizenNameFile file,
                                       Set<String> male, Set<String> female) {
        final String first = firstNameOf(fullName, file);
        if (first.isEmpty()) {
            return null;
        }
        final boolean isMale = male.contains(first);
        final boolean isFemale = female.contains(first);
        if (isMale == isFemale) {
            return null;
        }
        return isFemale;
    }

    private ParentGender() {}
}
