package com.github.eileenthg.colonyhandover.command;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import com.github.eileenthg.colonyhandover.scroll.Checklist;
import com.github.eileenthg.colonyhandover.scroll.ColonyIssues;
import com.github.eileenthg.colonyhandover.scroll.IssueSurvey;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /colonyhandover checklist <pos> <hut>} - survey one building by coordinate.
 *
 * <h2>Why this exists next to the scroll</h2>
 *
 * The Tweaks addon's book is a client screen. Its rows can show what a scroll knows
 * about itself, but surveying a room means reading blocks, and only the server has
 * those. So the book's Open button runs this, and the answer comes back through the
 * same path as using the scroll in hand - one survey, one wording, no chance of the
 * two disagreeing.
 *
 * <h2>No colony required</h2>
 *
 * Every other command here demands colony manager rank. This one cannot: it is most
 * useful <em>after</em> the handover, when there is no colony left to hold rank in.
 * It only reads blocks and reports, so there is nothing to gate.
 */
public final class ChecklistCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("checklist")
                .then(Commands.literal("refresh").executes(ctx -> refresh(ctx.getSource())))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("hut", StringArgumentType.word())
                                .executes(ctx -> run(ctx.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                                        StringArgumentType.getString(ctx, "hut")))));
    }

    /**
     * Re-surveys the scroll in hand, for the screen's Re-survey button.
     *
     * <p>The survey has to run server-side, and the result has to end up back on the
     * stack the client is reading. Both happen here, and the component sync carries
     * the answer home - which is why this needs no reply packet of its own.
     */
    private static int refresh(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game."));
            return 0;
        }

        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ColonyHandover.CHECKLIST_SCROLL.get())) {
                continue;
            }
            final ColonyIssues fresh = IssueSurvey.surveyAt(player.serverLevel(),
                    player.blockPosition(), stack.get(ColonyHandover.COLONY_ISSUES.get()));
            if (fresh == null) {
                say(source, ChatFormatting.YELLOW,
                        "This scroll is blank. Use it inside a colony first.");
                return 0;
            }
            stack.set(ColonyHandover.COLONY_ISSUES.get(), fresh);
            say(source, ChatFormatting.GREEN, "Re-surveyed " + fresh.colonyName() + ": "
                    + fresh.outstanding().size() + " of " + fresh.buildings().size()
                    + " building(s) need attention.");
            return 1;
        }

        say(source, ChatFormatting.YELLOW, "Hold a Conversion Checklist to re-survey it.");
        return 0;
    }

    private static int run(CommandSourceStack source, BlockPos pos, String hutId) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game."));
            return 0;
        }

        final Checklist.Result result = Checklist.of(player.serverLevel(), pos, hutId);
        say(source, ChatFormatting.GOLD, "=== " + hutId + " -> " + result.mcaType()
                + " @ " + pos.toShortString() + " ===");

        switch (result.status()) {
            case COMPLETE -> say(source, ChatFormatting.GREEN,
                    "Nothing needed. This room already registers as " + result.mcaType() + ".");
            case NOT_ENCLOSED -> {
                say(source, ChatFormatting.YELLOW,
                        "Not an enclosed room yet - the survey walks straight out of it.");
                say(source, ChatFormatting.GRAY,
                        "Seal the gaps first; furniture cannot help until it does.");
            }
            case NOT_A_ROOM -> say(source, ChatFormatting.GRAY,
                    "This building never becomes an MCA building - its workstation is all it"
                            + " needs, and the handover places that.");
            case UNMAPPABLE -> say(source, ChatFormatting.RED,
                    "This hut wants something our mapping has no block for. That is a gap in"
                            + " the mod, not in your building.");
            default -> {
                say(source, ChatFormatting.WHITE, "Still needed:");
                result.groupNames().forEach(n -> say(source, ChatFormatting.AQUA, "  " + n));
                if (result.cramped()) {
                    say(source, ChatFormatting.RED, "  Only room for " + result.roomFor()
                            + " of " + result.wanted() + " - clear floor space first.");
                }
            }
        }
        return 1;
    }

    private static void say(CommandSourceStack source, ChatFormatting colour, String text) {
        final Component line = Component.literal(text).withStyle(colour);
        source.sendSuccess(() -> line, false);
    }

    private ChecklistCommand() {}
}
