package com.craisinlord.antos.content.client.game.mini;

import com.craisinlord.antos.api.client.game.ComputerGame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for the built-in games. Subclasses draw in a local 214x155 coordinate space, and may register
 * clickable buttons while drawing; the button list is rebuilt every frame.
 */
abstract class MiniGameSession implements ComputerGame.Session {
    static final int WIDTH = 214;
    static final int HEIGHT = 155;
    static final int GREEN = 0xFF65FF65;
    static final int PALE_GREEN = 0xFFB8FFB8;
    static final int DIM_GREEN = 0xFF3F9A3F;
    static final int PANEL = 0xFF102010;
    static final int PANEL_LIGHT = 0xFF1D3A1D;
    static final int RED = 0xFFFF7070;
    static final int AMBER = 0xFFFFD36B;

    private record Button(int x, int y, int width, int height, Runnable action) {
        boolean contains(int px, int py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }

    private final List<Button> buttons = new ArrayList<>();
    protected Font font;
    protected int mouseX = -1;
    protected int mouseY = -1;

    protected abstract void draw(GuiGraphics g);

    protected boolean onMouseClicked(int x, int y, int button) {
        return false;
    }

    @Override
    public final void hover(int x, int y) {
        mouseX = x;
        mouseY = y;
    }

    @Override
    public final void render(GuiGraphics graphics, Font font, int x, int y, int width, int height) {
        this.font = font;
        buttons.clear();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        try {
            draw(graphics);
        } finally {
            graphics.pose().popPose();
        }
    }

    @Override
    public final boolean mouseClicked(int x, int y, int button) {
        if (button == 0) {
            for (int i = buttons.size() - 1; i >= 0; i--) {
                Button candidate = buttons.get(i);
                if (candidate.contains(x, y)) {
                    click();
                    candidate.action().run();
                    return true;
                }
            }
        }
        return onMouseClicked(x, y, button);
    }

    protected final boolean hovering(int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    protected final void button(GuiGraphics g, String label, int x, int y, int width, int height, Runnable action) {
        button(g, label, x, y, width, height, true, action);
    }

    protected final void button(GuiGraphics g, String label, int x, int y, int width, int height, boolean enabled, Runnable action) {
        boolean hot = enabled && hovering(x, y, width, height);
        g.fill(x, y, x + width, y + height, hot ? PANEL_LIGHT : PANEL);
        outline(g, x, y, width, height, enabled ? (hot ? GREEN : DIM_GREEN) : 0xFF2A4A2A);
        g.drawString(font, label, x + (width - font.width(label)) / 2, y + (height - 8) / 2 + 1, enabled ? PALE_GREEN : 0xFF4A6A4A, false);
        if (enabled) hitArea(x, y, width, height, action);
    }

    /** Registers an invisible click target; later targets win where they overlap. */
    protected final void hitArea(int x, int y, int width, int height, Runnable action) {
        buttons.add(new Button(x, y, width, height, action));
    }

    protected final void text(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    protected final void centered(GuiGraphics g, String text, int centerX, int y, int color) {
        g.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    protected static void outline(GuiGraphics g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y, x + 1, y + height, color);
        g.fill(x + width - 1, y, x + width, y + height, color);
    }

    /** Filled circle drawn as horizontal spans. */
    protected static void disc(GuiGraphics g, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int span = (int) Math.round(Math.sqrt(radius * radius + radius * 0.8D - dy * dy));
            g.fill(centerX - span, centerY + dy, centerX + span + 1, centerY + dy + 1, color);
        }
    }

    protected static void click() {
        play(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F);
    }

    protected static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 0.6F));
    }
}
