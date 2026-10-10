package com.github.eileenthg.colonyhandover.handover;

import net.minecraft.ChatFormatting;

import java.util.function.BiConsumer;

/**
 * Names every colonist with recorded parents, and says what becomes of each.
 *
 * <h2>Why this is separate from both commands that print it</h2>
 *
 * {@code preview} renders into a chat list and a log list; {@code parents} writes
 * straight to the command source. Neither shape is the other's, so the listing
 * itself is written once here against a {@link BiConsumer} and each command supplies
 * its own way of emitting a line. A second copy would drift, and a drifted copy of a
 * report whose whole job is to be checked against your own memory of the colony is
 * worse than no report.
 *
 * <h2>Why name them rather than count them</h2>
 *
 * The dry run gives totals - so many parents recovered, so many from the name file.
 * That is the right shape for deciding whether to go ahead and the wrong shape for
 * checking whether the answer is <em>correct</em>. For that you need a specific
 * colonist whose history you remember, to see whether the mod agrees with you.
 *
 * <p>The slot each parent lands in is shown too, because that is the part most
 * likely to look wrong later: a second mother is filed under "father" by design, and
 * seeing it here beats discovering it in MCA's family screen once the colony is gone.
 */
public final class ParentReport {

    /** Enough to be useful in chat without burying the rest of a preview. */
    public static final int DEFAULT_LIMIT = 12;

    /**
     * @param limit how many colonists to name before summarising the remainder
     * @return how many colonists had recorded parents in total
     */
    public static int render(ColonySnapshot snap, int limit,
                             BiConsumer<ChatFormatting, String> out) {
        int listed = 0;
        int total = 0;

        for (final ColonySnapshot.Citizen c : snap.citizens()) {
            final boolean any = (c.parentA() != null && c.parentA().known())
                    || (c.parentB() != null && c.parentB().known());
            if (!any) {
                continue;
            }
            total++;
            if (listed >= limit) {
                continue;
            }
            listed++;

            out.accept(ChatFormatting.WHITE, "  #" + c.id() + " " + c.name()
                    + (c.child() ? " (child)" : "") + " - " + (c.female() ? "female" : "male"));

            final ParentSlots.Slots slots = ParentSlots.assign(c.parentA(), c.parentB());
            line(out, "father", slots.father());
            line(out, "mother", slots.mother());

            if (slots.misfiled()) {
                out.accept(ChatFormatting.YELLOW,
                        "      same-gender pair - the second takes the vacant slot,"
                                + " so MCA will mislabel it.");
            }
            if (slots.dropped() > 0) {
                out.accept(ChatFormatting.YELLOW, "      " + slots.dropped()
                        + " parent name(s) left out - gender unrecoverable.");
            }
        }

        if (total == 0) {
            out.accept(ChatFormatting.GRAY,
                    "  None. Only colonists born in the colony have recorded parents -"
                            + " anyone recruited or spawned with it has none.");
        } else if (total > listed) {
            out.accept(ChatFormatting.GRAY,
                    "  ... and " + (total - listed) + " more with recorded parents.");
        }
        return total;
    }

    private static void line(BiConsumer<ChatFormatting, String> out, String slot,
                             ColonySnapshot.Parent parent) {
        if (parent == null) {
            out.accept(ChatFormatting.DARK_GRAY, "      " + slot + ": (empty)");
            return;
        }
        final String origin = switch (parent.source()) {
            case LIVING -> "living citizen #" + parent.id();
            case NAME_FILE -> "gender read from the name file";
            default -> "unknown";
        };
        // Coloured by confidence: a living match is fact, a name-file match is a
        // sound inference, and those are worth telling apart at a glance.
        final ChatFormatting colour = parent.source() == ParentGender.Source.LIVING
                ? ChatFormatting.GREEN
                : ChatFormatting.AQUA;
        out.accept(colour, "      " + slot + ": " + parent.name() + " (" + origin + ")");
    }

    private ParentReport() {}
}
