package net.eileenthg.guidelibrary.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The small book button added to the inventory. Draws an item icon on a vanilla button. */
final class LibraryButton extends Button {

    private final ItemStack icon;

    LibraryButton(int x, int y, ItemStack icon, Component tooltip, OnPress onPress) {
        super(x, y, 20, 18, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        super.renderWidget(g, mx, my, pt);
        g.renderItem(icon, getX() + 2, getY() + 1);
    }
}
