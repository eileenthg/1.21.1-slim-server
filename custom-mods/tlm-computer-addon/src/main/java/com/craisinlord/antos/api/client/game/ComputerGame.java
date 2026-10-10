package com.craisinlord.antos.api.client.game;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Public client API for games displayed in the AntOS Games app. */
public interface ComputerGame {
    /** Stable game ID, namespaced by the registering mod. */
    ResourceLocation id();

    ResourceLocation diskId();

    default boolean includedByDefault() { return false; }

    /** Display name shown in the Games app. */
    String title();

    /** Creates per-open-game state for this computer. */
    Session create();

    interface Session {
        default void tick() {}
        void render(GuiGraphics graphics, Font font, int x, int y, int width, int height);
        default boolean keyPressed(int keyCode) { return false; }
        default boolean charTyped(char codePoint) { return false; }

        /** Mouse position relative to the top-left corner of the game area, sent every frame before {@link #render}. */
        default void hover(int mouseX, int mouseY) {}

        /** Mouse press relative to the top-left corner of the game area. Return true if handled. */
        default boolean mouseClicked(int mouseX, int mouseY, int button) { return false; }
        default void close() {}
    }
}
