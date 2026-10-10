package com.github.eileenthg.colonyhandover.scroll;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import com.github.eileenthg.colonyhandover.ColonyHandoverClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * A scroll listing every building in a colony with work outstanding.
 *
 * <h2>Every use is a fresh survey</h2>
 *
 * Using the scroll surveys, then shows the result. An earlier version tried to keep
 * the list current by re-surveying rooms as blocks changed around the player; it was
 * clever, cost a flood fill per edit, and in practice did not update reliably. Reading
 * the scroll is already the moment someone wants the list to be true, so that is the
 * moment it is made true.
 *
 * <h2>Why the window waits</h2>
 *
 * A right-click happens on the client, but the survey can only run on the server,
 * where the blocks are. The result travels back as a data component on this stack,
 * a tick or two later. Opening the window immediately would show the previous
 * survey, so the client instead remembers the survey it had and opens once the
 * stack carries a different one ({@link ColonyHandoverClient#awaitSurvey}). Each
 * survey is timestamped, so even an unchanged colony counts as a new reading.
 *
 * <h2>It outlives the colony</h2>
 *
 * Inside a colony the survey enumerates it. Anywhere else it re-walks the roster the
 * scroll already carries - which is how it keeps working after the handover, when
 * there is no colony to ask and no hut blocks to find (findings #1 and #2).
 */
public final class ChecklistScroll extends Item {

    public ChecklistScroll(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        survey(context.getLevel(), player, context.getItemInHand());
        return context.getLevel().isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        survey(level, player, stack);
        return InteractionResultHolder.success(stack);
    }

    /** Both sides run this; each does only its own half. */
    private static void survey(Level level, Player player, ItemStack stack) {
        final ColonyIssues previous = stack.get(ColonyHandover.COLONY_ISSUES.get());

        if (level.isClientSide) {
            awaitIfClient(previous);
            return;
        }

        final ColonyIssues fresh =
                IssueSurvey.surveyAt((ServerLevel) level, player.blockPosition(), previous);
        if (fresh == null) {
            say(player, ChatFormatting.YELLOW,
                    "This scroll is blank. Use it inside a colony to survey it.");
            return;
        }
        stack.set(ColonyHandover.COLONY_ISSUES.get(), fresh);
    }

    /**
     * Client half, behind a dist check.
     *
     * <p>{@link ColonyHandoverClient} touches {@code Minecraft}, which does not exist on
     * a dedicated server. {@code isClientSide} is also true inside the integrated
     * server's client, so it alone would not keep the class from loading there.
     */
    private static void awaitIfClient(ColonyIssues previous) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ColonyHandoverClient.awaitSurvey(previous == null ? Long.MIN_VALUE : previous.surveyedAt());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        final ColonyIssues issues = stack.get(ColonyHandover.COLONY_ISSUES.get());
        if (issues == null) {
            tooltip.add(Component.literal("Use inside a colony to survey it")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.literal(issues.colonyName()).withStyle(ChatFormatting.GOLD));
        final int outstanding = issues.outstanding().size();
        tooltip.add(Component.literal(outstanding == 0
                        ? "Nothing outstanding"
                        : outstanding + " building(s) need attention")
                .withStyle(outstanding == 0 ? ChatFormatting.GREEN : ChatFormatting.AQUA));
    }

    private static void say(Player player, ChatFormatting colour, String text) {
        player.displayClientMessage(Component.literal(text).withStyle(colour), false);
    }
}
