package net.eileenthg.taskcurator;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A folder header row in the maid task menu, drawn on the same strip as TLM's task buttons. */
public final class FolderButton extends Button {
    private static final ResourceLocation TASK = ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "textures/gui/maid_gui_task.png");

    private final CuratorLayout.FolderRow row;

    public FolderButton(int x, int y, CuratorLayout.FolderRow row, OnPress onPress) {
        super(x, y, 83, 19, Component.literal(row.folder().name), onPress, DEFAULT_NARRATION);
        this.row = row;
        setTooltip(Tooltip.create(Component.translatable(
                row.folder().open ? "taskcurator.folder.close" : "taskcurator.folder.open", row.count())));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableDepthTest();
        // Same strip and hover offset TLM's TaskButton uses.
        g.blit(TASK, getX(), getY(), 93, isHoveredOrFocused() ? 48 : 28, width, height, 256, 256);
        g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x40C08A3E);

        Font font = Minecraft.getInstance().font;
        String arrow = row.folder().open ? "▼" : "▶";
        g.drawString(font, arrow, getX() + 4, getY() + 6, 0x5A3A12, false);

        String count = " (" + row.count() + ")";
        int room = width - 14 - 4 - font.width(count) - (row.holdsCurrent() ? 6 : 0);
        String name = font.plainSubstrByWidth(row.folder().name, room);
        int x = g.drawString(font, name, getX() + 14, getY() + 6, 0x333333, false);
        x = g.drawString(font, count, x, getY() + 6, 0x6B6B6B, false);
        if (row.holdsCurrent() && !row.folder().open) {
            // The maid's current task is inside this closed folder.
            g.drawString(font, "●", x + 1, getY() + 6, 0xD08A00, false);
        }
    }
}
