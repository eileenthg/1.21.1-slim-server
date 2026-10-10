package net.reimaden.touhouorigins.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.reimaden.touhouorigins.TouhouOrigins;

/**
 * Widget drawing for the grid origin picker.
 *
 * <p>Ported from UltrusBot's AltOriginGui (MIT), which does this for apace100's Origins on 1.20.1.
 * The idea and the sprite sheet are theirs; the code is rewritten against Overgrown's API, which
 * exposes records ({@code origin.name()}, {@code origin.impact()}) where apace100 had getters.
 *
 * <p>Worth having because Overgrown's chooser is a one-at-a-time browser with &lt; and &gt; arrows.
 * That is fine for the ten built-in origins and miserable for nineteen Touhou ones — and this pack
 * also stacks a nine-entry Blessings layer and a seven-entry size layer on top.
 */
public interface OriginGrid {

    ResourceLocation SPRITES = TouhouOrigins.id("textures/gui/origin_choices.png");

    /** Sprite geometry, all from AltOriginGui's sheet. */
    int ICON = 26;
    int PANEL_WIDTH = 219;
    int PANEL_HEIGHT = 182;
    int COLUMNS = 7;
    int PER_PAGE = 35;

    Font gridFont();

    Component currentLayerName();

    boolean iconHighlighted(int mouseX, int mouseY, int x, int y);

    boolean originSelected(int index);

    boolean randomOriginSelected();

    float gridTime();

    default boolean mouseOverIcon(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseY >= y && mouseX < x + ICON && mouseY < y + ICON;
    }

    /**
     * Impact pips, one per level. Overgrown's {@code Impact} has exactly NONE/LOW/MEDIUM/HIGH as
     * levels 0-3, which lines up with the sheet's four pips, so this indexes rather than switching
     * on names — an addon impact beyond the four would otherwise fall through to a wrong sprite.
     */
    static void renderImpact(GuiGraphics graphics, int x, int y, int impactLevel, boolean highlighted) {
        int v = highlighted ? 112 : 104;
        int level = Math.max(0, Math.min(3, impactLevel));
        graphics.blit(SPRITES, x, y, 224 + (level * 8), v, 8, 8);
    }

    default void renderSlot(GuiGraphics graphics, int x, int y, boolean selected, boolean highlighted) {
        int v = selected ? ICON : 0;
        if (highlighted) v += 52;
        graphics.blit(SPRITES, x, y, 230, v, ICON, ICON);
    }

    default void renderOriginIcon(GuiGraphics graphics, int mouseX, int mouseY, int x, int y,
                                  boolean selected, int impactLevel, Component originName) {
        boolean highlighted = iconHighlighted(mouseX, mouseY, x, y);
        renderSlot(graphics, x, y, selected, highlighted);
        renderImpact(graphics, x, y, impactLevel, highlighted);
        if (mouseOverIcon(mouseX, mouseY, x, y)) {
            graphics.renderTooltip(gridFont(),
                Component.empty().append(currentLayerName()).append(": ").append(originName),
                mouseX, mouseY);
        }
    }

    /** The random slot has no fixed impact, so its pip cycles to read as "unknown". */
    default void renderRandomIcon(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean selected) {
        boolean highlighted = iconHighlighted(mouseX, mouseY, x, y);
        renderSlot(graphics, x, y, selected, highlighted);
        graphics.blit(SPRITES, x + 6, y + 5, 243, 120, 13, 16);
        renderImpact(graphics, x, y, (int) (gridTime() / 15.0F) % 4, highlighted);
    }
}
