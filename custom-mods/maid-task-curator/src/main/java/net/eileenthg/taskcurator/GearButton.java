package net.eileenthg.taskcurator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Small button on the task list header that opens the curator editor. */
public final class GearButton extends Button {
    public GearButton(int x, int y, OnPress onPress) {
        super(x, y, 11, 11, Component.translatable("taskcurator.gear"), onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.translatable("taskcurator.gear")));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        g.fill(x, y, x + width, y + height, 0xFF555555);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, isHoveredOrFocused() ? 0xFFE0C08A : 0xFFC6C6C6);
        var font = Minecraft.getInstance().font;
        String glyph = "⚙";
        g.drawString(font, glyph, x + (width - font.width(glyph)) / 2 + 1, y + 2, 0x333333, false);
    }
}
