package com.github.eileenthg.colonyhandover.client;

import com.ldtteam.blockui.controls.ButtonImage;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Adds "Grant Independence" to the town hall's Actions tab.
 *
 * <h2>Why the work is here and not in the mixin</h2>
 *
 * The mixin is the fragile part: it names a MineColonies class and constructor, both
 * of which can change in any snapshot. Keeping it to one call means an upstream change
 * breaks that single line, and everything about what the button looks like and does
 * lives in ordinary code that compiles against public BlockUI API (finding #8).
 *
 * <h2>Why a button added in code works where an XML override would not</h2>
 *
 * MineColonies wires button handlers in Java during construction. A resource-pack edit
 * to {@code layoutactions.xml} could draw a button, but nothing would ever handle its
 * click. Adding the pane and registering its handler together, after the constructor
 * has run, avoids both problems.
 */
public final class TownHallButton {

    public static final String ID = "colonyhandover_grant";

    /**
     * Directly under Build, Map and Mercenaries (y 97, 116, 136), in the same texture and
     * spacing, so it reads as one of the tab's own actions.
     */
    private static final int X = 110;
    private static final int Y = 156;

    public static void add(AbstractWindowSkeleton window, int colonyId, String colonyName) {
        final ButtonImage button = new ButtonImage();
        button.setID(ID);
        button.setSize(129, 17);
        // Must follow setSize. A button built in code starts with a 0x0 text box and
        // setSize only rescales it proportionally, so without this the label is
        // clipped to nothing. XML buttons get the box from their size attribute.
        button.setTextRenderBox(129, 17);
        button.setPosition(X, Y);
        button.setImage(ResourceLocation.fromNamespaceAndPath("minecolonies",
                "textures/gui/builderhut/builder_button_medium_large.png"));
        button.setText(Component.literal("Grant Independence"));
        button.setColors(0x000000);
        window.addChild(button);
        window.registerButton(ID, () -> new HandoverWindow(colonyId, colonyName).open());
    }

    private TownHallButton() {}
}
