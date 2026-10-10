package com.github.eileenthg.colonyhandover.command;

import com.github.eileenthg.colonyhandover.place.Enclosure;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;


/**
 * {@code /colonyhandover diagnose} - why does MCA refuse this room?
 *
 * <p>{@code TOO_SMALL} is a catch-all. It is returned when the seed cannot be
 * resolved, when no floor regions are found, and when the envelope really is
 * small - and the first of those has nothing to do with size. This command
 * mirrors MCA's own seed rules and reports which one failed, so the answer is a
 * position you can walk to rather than a guess.
 *
 * <p>The rules replicated here come from {@code StructureScanner}: a seed must be
 * a walkable anchor (open at foot and head, solid ground beneath, roof overhead)
 * that does <em>not</em> reach the exterior. Reaching the exterior is the part
 * that catches open-plan builds: the walk steps through any opening you could
 * physically walk through, and the moment it stands somewhere with open sky the
 * whole building is judged to be outdoors.
 */
public final class DiagnoseCommand {

    /** MCA's ROOF_SEARCH, quoted in the report. */
    private static final int ROOF_SEARCH = 16;

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("diagnose").executes(ctx -> run(ctx.getSource()));
    }

    private static int run(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game, standing in the room."));
            return 0;
        }
        final ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        // Carpet, slabs and dirt paths are all shorter than a full block, so the
        // player's feet sink below the top face and blockPosition() lands on the
        // block itself rather than the air above it. The cell above is where the
        // player physically is, and the one MCA would seed from.
        //
        // This is deliberately NOT the full walkable-anchor test. Stepping up is a
        // coordinate correction, not a verdict - gating it on hasRoof meant an
        // unroofed path reported "you are standing inside a block", sending the
        // player after the wrong problem. Correct the position first, then let the
        // four checks below say what is actually wrong with it.
        if (!Enclosure.isOpen(level, pos)
                && Enclosure.isOpen(level, pos.above())
                && Enclosure.isSupported(level, pos.above())) {
            pos = pos.above();
        }

        say(source, ChatFormatting.GOLD, "=== Room diagnosis at " + pos.toShortString() + " ===");
        if (!pos.equals(player.blockPosition())) {
            say(source, ChatFormatting.GRAY,
                    "(you are on a carpet, slab or path - your feet sit below its top face,"
                            + " so surveying from the space above it)");
        }

        // --- the four walkable-anchor checks, in MCA's order -----------------
        final boolean footOpen = Enclosure.isOpen(level, pos);
        final boolean headOpen = Enclosure.isOpen(level, pos.above());
        final boolean supported = Enclosure.isSupported(level, pos);
        final boolean roofed = Enclosure.hasRoof(level, pos);

        say(source, footOpen ? ChatFormatting.GREEN : ChatFormatting.RED,
                (footOpen ? "PASS" : "FAIL") + "  foot space is open"
                        + (footOpen ? "" : " - you are standing inside a block"));
        say(source, headOpen ? ChatFormatting.GREEN : ChatFormatting.RED,
                (headOpen ? "PASS" : "FAIL") + "  head space is open");
        say(source, supported ? ChatFormatting.GREEN : ChatFormatting.RED,
                (supported ? "PASS" : "FAIL") + "  solid floor beneath"
                        + (supported ? "" : " - needs a quarter of a full block face"));
        say(source, roofed ? ChatFormatting.GREEN : ChatFormatting.RED,
                (roofed ? "PASS" : "FAIL") + "  roof overhead within " + ROOF_SEARCH + " blocks"
                        + (roofed ? "" : " - open sky, or only leaves above"));

        if (!footOpen || !headOpen || !supported || !roofed) {
            say(source, ChatFormatting.YELLOW,
                    "Stand somewhere inside the room, on its floor, and try again.");
            return 0;
        }

        // --- the escape walk -------------------------------------------------
        final var leak = Enclosure.findLeak(level, pos).orElse(null);
        if (leak == null) {
            say(source, ChatFormatting.GREEN, "PASS  the room is enclosed - no walkable route to open sky.");
            say(source, ChatFormatting.GRAY,
                    "MCA should accept this as a seed. If it still refuses, the reason is the"
                            + " floor regions or an overlap with an existing building.");
            return 1;
        }

        say(source, ChatFormatting.RED, leak.ceiling()
                ? "FAIL  open sky above " + leak.escape().toShortString()
                : "FAIL  the survey walks out at " + leak.crossing().toShortString()
                        + " and reaches open sky at " + leak.escape().toShortString());
        say(source, ChatFormatting.YELLOW,
                "That is why this reads as TOO_SMALL. MCA treats a room you can walk out of as"
                        + " outdoors, however big it is.");
        say(source, ChatFormatting.GRAY,
                "Only a door, fence gate or trapdoor stops the walk. Banners, torches and open"
                        + " archways do not - they have no collision, so the scan steps straight"
                        + " through them.");
        return 0;
    }

    private static void say(CommandSourceStack source, ChatFormatting colour, String text) {
        final Component line = Component.literal(text).withStyle(colour);
        source.sendSuccess(() -> line, false);
    }

    private DiagnoseCommand() {}
}
