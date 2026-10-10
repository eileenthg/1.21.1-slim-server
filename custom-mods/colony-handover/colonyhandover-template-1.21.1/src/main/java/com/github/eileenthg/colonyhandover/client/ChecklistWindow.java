package com.github.eileenthg.colonyhandover.client;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import com.github.eileenthg.colonyhandover.scroll.ColonyIssues;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonHandler;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.blockui.views.ScrollingList;
import com.ldtteam.blockui.views.View;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The conversion checklist, on MineColonies' own parchment.
 *
 * <h2>Two pages, not two panels</h2>
 *
 * The resource scroll this imitates is a single 190-pixel column of paper. A list
 * beside a breakdown does not fit on it without shrinking both past legibility, so
 * the window is a list page and a detail page, swapped by visibility. It is the
 * same shape as the builder's list: pick an entry, read it, go back.
 *
 * <h2>Always a fresh reading</h2>
 *
 * Using the scroll surveys first and opens this window only once that survey has
 * arrived on the stack, so what is shown is current as of the moment it opened.
 * Re-survey does the same: it closes, and the new reading reopens it.
 */
public final class ChecklistWindow extends BOWindow implements ButtonHandler {

    private static final ResourceLocation LAYOUT =
            ResourceLocation.fromNamespaceAndPath(ColonyHandover.MODID, "gui/checklist.xml");

    private final Runnable onRefresh;

    private ColonyIssues issues;
    private List<ColonyIssues.Entry> outstanding;
    private List<Component> detail = List.of();
    /** The building open on the detail page, or -1 when the list is showing. */
    private int open = -1;
    /** Where Close and Escape return to, or null to close outright. */
    private Screen returnTo;

    public ChecklistWindow(ColonyIssues issues, Runnable onRefresh) {
        super(LAYOUT);
        this.issues = issues;
        this.outstanding = issues.listed();
        this.onRefresh = onRefresh;

        findPaneOfTypeByID("heading", Text.class)
                .setText(Component.literal("Conversion Checklist"));
        wireBuildingList();
        wireDetailList();
        refreshHeader();
        showList();
    }

    /**
     * Makes Close and Escape go back to {@code screen} instead of the game.
     *
     * <p>The Tweaks book does this for MineColonies' own windows with a mixin on their
     * close method, which this window is not part of - so it does the same by hand.
     */
    public ChecklistWindow returningTo(Screen screen) {
        this.returnTo = screen;
        return this;
    }

    /** Escape lands here too: BlockUI's unhandled-key path calls close(). */
    @Override
    public void close() {
        if (returnTo != null) {
            Minecraft.getInstance().setScreen(returnTo);
            return;
        }
        super.close();
    }

    private void wireBuildingList() {
        findPaneOfTypeByID("buildings", ScrollingList.class).setDataProvider(
                new ScrollingList.DataProvider() {
                    @Override
                    public int getElementCount() {
                        return outstanding.size();
                    }

                    @Override
                    public void updateElement(int index, Pane row) {
                        final ColonyIssues.Entry e = outstanding.get(index);
                        // On parchment, dark red for the prerequisite problem and
                        // plain ink for ordinary furnishing work.
                        final Component name = switch (e.status()) {
                            case NOT_SEALED -> Component.literal(e.hutId())
                                    .withStyle(ChatFormatting.DARK_RED);
                            // Greyed and last: visible, so nothing silently vanishes,
                            // but plainly not the work to do next.
                            case INACCESSIBLE -> Component.literal(e.hutId() + " - inaccessible or too small")
                                    .withStyle(ChatFormatting.GRAY);
                            default -> Component.literal(e.hutId());
                        };
                        row.findPaneOfTypeByID("name", Text.class).setText(name);
                    }
                });
    }

    private void wireDetailList() {
        findPaneOfTypeByID("detail", ScrollingList.class).setDataProvider(
                new ScrollingList.DataProvider() {
                    @Override
                    public int getElementCount() {
                        return detail.size();
                    }

                    @Override
                    public void updateElement(int index, Pane row) {
                        row.findPaneOfTypeByID("line", Text.class).setText(detail.get(index));
                    }
                });
    }

    private void refreshHeader() {
        findPaneOfTypeByID("ribbon", Text.class).setText(Component.literal(issues.colonyName()));
        final Text summary = findPaneOfTypeByID("summary", Text.class);
        if (open >= 0 && open < outstanding.size()) {
            final ColonyIssues.Entry e = outstanding.get(open);
            summary.setText(Component.literal(e.hutId()
                    + (e.mcaType().isEmpty() ? "" : " > " + e.mcaType())));
            return;
        }
        // Count only work that can be done; inaccessible rooms are reported, not owed.
        final int work = issues.outstanding().size();
        final long blocked = issues.inaccessibleCount();
        summary.setText(work == 0
                ? Component.literal("Nothing outstanding"
                        + (blocked > 0 ? " (" + blocked + " inaccessible or too small)" : ""))
                        .withStyle(ChatFormatting.DARK_GREEN)
                : Component.literal(work + " need work"
                        + (blocked > 0 ? ", " + blocked + " inaccessible or too small" : "")));
    }

    private void showList() {
        open = -1;
        findPaneOfTypeByID("listPage", View.class).setVisible(true);
        findPaneOfTypeByID("detailPage", View.class).setVisible(false);
        refreshHeader();
        findPaneOfTypeByID("buildings", ScrollingList.class).refreshElementPanes();
    }

    private void showDetail(int index) {
        if (index < 0 || index >= outstanding.size()) {
            showList();
            return;
        }
        open = index;
        final ColonyIssues.Entry e = outstanding.get(index);
        detail = describe(e);
        findPaneOfTypeByID("detailPos", Text.class)
                .setText(Component.literal("at " + e.pos().toShortString()));
        findPaneOfTypeByID("listPage", View.class).setVisible(false);
        findPaneOfTypeByID("detailPage", View.class).setVisible(true);
        refreshHeader();
        findPaneOfTypeByID("detail", ScrollingList.class).refreshElementPanes();
    }

    /** The breakdown, as lines short enough for the paper - both kinds of problem. */
    private static List<Component> describe(ColonyIssues.Entry e) {
        final List<Component> lines = new ArrayList<>();
        if (e.status() == ColonyIssues.Status.INACCESSIBLE) {
            lines.add(Component.literal("Inaccessible or too small")
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }
        if (e.status() == ColonyIssues.Status.NOT_SEALED) {
            lines.add(Component.literal("Not sealed").withStyle(ChatFormatting.DARK_RED));
            lines.add(Component.literal(e.gap()
                    .map(p -> "Walks out at " + p.toShortString())
                    .orElse("No roofed room found near it")));
            lines.add(Component.empty());
            lines.add(Component.literal("Only a door, fence gate or"));
            lines.add(Component.literal("trapdoor seals a gap. Banners"));
            lines.add(Component.literal("and open arches do not."));
            return lines;
        }
        lines.add(Component.literal("Still needed").withStyle(ChatFormatting.DARK_BLUE));
        for (final Map.Entry<String, Integer> need : e.missing().entrySet()) {
            lines.add(Component.literal("  " + need.getValue() + " x " + need.getKey()));
        }
        if (e.cramped()) {
            lines.add(Component.empty());
            lines.add(Component.literal("Room for only " + e.roomFor() + " of " + e.wanted())
                    .withStyle(ChatFormatting.DARK_RED));
            lines.add(Component.literal("Clear some floor space."));
        }
        return lines;
    }

    /** Re-reads the scroll each tick, redrawing only when the survey changed. */
    @Override
    public void onUpdate() {
        super.onUpdate();
        final ColonyIssues current = heldIssues();
        if (current == null || current.equals(issues)) {
            return;
        }
        // Keep the reader on the building they were looking at, if it is still listed.
        final String openHut = open >= 0 && open < outstanding.size()
                ? outstanding.get(open).hutId() + outstanding.get(open).pos() : null;
        issues = current;
        outstanding = current.listed();
        if (openHut != null) {
            for (int i = 0; i < outstanding.size(); i++) {
                if ((outstanding.get(i).hutId() + outstanding.get(i).pos()).equals(openHut)) {
                    showDetail(i);
                    return;
                }
            }
        }
        showList();
    }

    private static ColonyIssues heldIssues() {
        final var player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ColonyHandover.CHECKLIST_SCROLL.get())) {
                return stack.get(ColonyHandover.COLONY_ISSUES.get());
            }
        }
        return null;
    }

    @Override
    public void onButtonClicked(Button button) {
        switch (button.getID()) {
            // Close, and let the fresh survey reopen the window when it arrives.
            // Closes outright even when opened from the book: the fresh survey reopens
            // the window over the game, and a book left underneath would be stale.
            case "refresh" -> {
                onRefresh.run();
                super.close();
            }
            // Same toggle as the command, sent as the command, so the button cannot
            // behave differently from typing it - including its colony and rank checks.
            case "marks" -> {
                final var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.connection.sendCommand("colonyhandover marks");
                }
            }
            case "close" -> close();
            case "back" -> showList();
            case "pick" -> showDetail(findPaneOfTypeByID("buildings", ScrollingList.class)
                    .getListElementIndexByPane(button));
            default -> { }
        }
    }
}
