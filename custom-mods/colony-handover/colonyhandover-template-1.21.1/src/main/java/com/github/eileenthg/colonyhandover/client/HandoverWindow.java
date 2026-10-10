package com.github.eileenthg.colonyhandover.client;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import com.github.eileenthg.colonyhandover.network.HandoverPayloads;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonHandler;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.blockui.views.ScrollingList;
import com.ldtteam.blockui.views.View;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * "Grant Independence", opened from the town hall's Actions tab.
 *
 * <h2>What it is for</h2>
 *
 * Everything the handover needs a player to look at before committing, in the place a
 * MineColonies player already manages their colony from: how many buildings still
 * need work, what will survive of the people and their families, a way to find the
 * gaps and a checklist to carry around - and then the one irreversible button.
 *
 * <h2>Why confirmation is a separate page</h2>
 *
 * The handover deletes the colony and cannot be undone. The command guards that with a
 * typed token; this window guards it with a second page that says plainly what happens
 * and asks for a deliberately different action. "Grant Independence" never runs
 * anything by itself.
 *
 * <h2>Why the numbers arrive a moment after it opens</h2>
 *
 * A building survey needs the server's world, so the window asks for a summary when it
 * opens and shows "Surveying..." until the reply lands in {@link #receive}.
 */
public final class HandoverWindow extends BOWindow implements ButtonHandler {

    private static final ResourceLocation LAYOUT =
            ResourceLocation.fromNamespaceAndPath(ColonyHandover.MODID, "gui/handover.xml");

    /** The open window, so a reply from the server knows where to go. */
    private static HandoverWindow current;

    private final int colonyId;
    private List<Component> summaryLines = List.of(Component.literal("Surveying the colony..."));
    private List<Component> charterLines = List.of();

    public HandoverWindow(int colonyId, String colonyName) {
        super(LAYOUT);
        this.colonyId = colonyId;

        findPaneOfTypeByID("heading", Text.class).setText(Component.literal("Grant Independence"));
        findPaneOfTypeByID("ribbon", Text.class).setText(Component.literal(colonyName));
        wire("summary", () -> summaryLines);
        wire("charter", () -> charterLines);
        showMain();
    }

    @Override
    public void onOpened() {
        super.onOpened();
        current = this;
        PacketDistributor.sendToServer(
                new HandoverPayloads.Request(colonyId, HandoverPayloads.Action.SUMMARY));
    }

    @Override
    public void onClosed() {
        super.onClosed();
        if (current == this) {
            current = null;
        }
    }

    /** Called on the client thread when the server's summary arrives. */
    public static void receive(HandoverPayloads.Summary summary) {
        final HandoverWindow window = current;
        if (window == null || window.colonyId != summary.colonyId()) {
            return;
        }
        window.summaryLines = describe(summary);
        window.charterLines = charter(summary);
        window.findPaneOfTypeByID("summary", ScrollingList.class).refreshElementPanes();
        window.findPaneOfTypeByID("charter", ScrollingList.class).refreshElementPanes();
    }

    private interface Lines {
        List<Component> get();
    }

    private void wire(String listId, Lines lines) {
        findPaneOfTypeByID(listId, ScrollingList.class).setDataProvider(new ScrollingList.DataProvider() {
            @Override
            public int getElementCount() {
                return lines.get().size();
            }

            @Override
            public void updateElement(int index, Pane row) {
                row.findPaneOfTypeByID("line", Text.class).setText(lines.get().get(index));
            }
        });
    }

    private static List<Component> describe(HandoverPayloads.Summary s) {
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Buildings").withStyle(ChatFormatting.DARK_BLUE));
        lines.add(Component.literal("  " + s.buildings() + " in the colony"));
        lines.add(s.needWork() == 0
                ? Component.literal("  none need work").withStyle(ChatFormatting.DARK_GREEN)
                : Component.literal("  " + s.needWork() + " need work").withStyle(ChatFormatting.DARK_RED));
        if (s.inaccessible() > 0) {
            lines.add(Component.literal("  " + s.inaccessible() + " inaccessible or too small")
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.empty());
        lines.add(Component.literal("People").withStyle(ChatFormatting.DARK_BLUE));
        lines.add(Component.literal("  " + s.citizens() + " colonists, " + s.children() + " children"));
        lines.add(Component.literal("  " + s.couples() + " couple(s)"));
        if (s.parentsKnown() > 0) {
            lines.add(Component.literal("  " + s.parentsPlaced() + " of " + s.parentsKnown()
                    + " parents kept"));
        }
        if (s.sameGenderPairs() > 0) {
            lines.add(Component.literal("  " + s.sameGenderPairs() + " same-gender parent pair(s)")
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private static List<Component> charter(HandoverPayloads.Summary s) {
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("The Charter of Independence").withStyle(ChatFormatting.DARK_BLUE));
        lines.add(Component.empty());
        lines.add(Component.literal("By signing, " + s.colonyName()));
        lines.add(Component.literal("ceases to be your colony."));
        lines.add(Component.empty());
        lines.add(Component.literal("Its " + s.citizens() + " colonists become"));
        lines.add(Component.literal("villagers who manage their"));
        lines.add(Component.literal("own lives. Their buildings"));
        lines.add(Component.literal("and families go with them."));
        lines.add(Component.empty());
        lines.add(Component.literal("This cannot be undone.").withStyle(ChatFormatting.DARK_RED));
        if (s.needWork() > 0) {
            lines.add(Component.literal(s.needWork() + " building(s) still need work.")
                    .withStyle(ChatFormatting.DARK_RED));
        }
        return lines;
    }

    private void showMain() {
        findPaneOfTypeByID("mainPage", View.class).setVisible(true);
        findPaneOfTypeByID("charterPage", View.class).setVisible(false);
    }

    private void showCharter() {
        findPaneOfTypeByID("mainPage", View.class).setVisible(false);
        findPaneOfTypeByID("charterPage", View.class).setVisible(true);
        findPaneOfTypeByID("charter", ScrollingList.class).refreshElementPanes();
    }

    private void send(HandoverPayloads.Action action) {
        PacketDistributor.sendToServer(new HandoverPayloads.Request(colonyId, action));
    }

    @Override
    public void onButtonClicked(Button button) {
        switch (button.getID()) {
            case "marks" -> {
                final var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.connection.sendCommand("colonyhandover marks");
                }
            }
            case "checklist" -> send(HandoverPayloads.Action.CHECKLIST);
            case "grant" -> {
                // Only offer the charter once the numbers it quotes have arrived.
                if (!charterLines.isEmpty()) {
                    showCharter();
                }
            }
            case "sign" -> {
                send(HandoverPayloads.Action.GRANT);
                close();
            }
            case "cancel" -> showMain();
            case "close" -> close();
            default -> { }
        }
    }
}
