package com.craisinlord.antos.content.client.game.mini;

import com.craisinlord.antos.content.minigame.CoinMinerLogic;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/** Click the coin to mine, then spend coins on upgrades that mine for you. Progress lasts until the client is closed. */
final class CoinMinerSession extends MiniGameSession {
    /** Kept between sessions so leaving the game does not wipe progress while the client is running. */
    private static CoinMinerLogic saved = new CoinMinerLogic();
    private static final int COIN_X = 48;
    private static final int COIN_Y = 80;
    private static final int COIN_RADIUS = 30;
    private static final int LIST_X = 100;
    private static final int ROW_HEIGHT = 26;

    private CoinMinerLogic game = saved;
    private int pressTicks;
    private int resetConfirmTicks;

    @Override
    public void tick() {
        game.tick();
        if (pressTicks > 0) pressTicks--;
        if (resetConfirmTicks > 0) resetConfirmTicks--;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            mine();
            return true;
        }
        return false;
    }

    private void mine() {
        game.click();
        pressTicks = 3;
        play(SoundEvents.NOTE_BLOCK_HAT.value(), 1.4F);
    }

    @Override
    protected void draw(GuiGraphics g) {
        text(g, "COIN MINER", 0, 2, GREEN);
        text(g, CoinMinerLogic.format(game.coins()) + " COINS", 0, 14, AMBER);
        text(g, CoinMinerLogic.format(game.perSecond()) + " PER SEC", 0, 26, PALE_GREEN);
        text(g, "+" + CoinMinerLogic.format(game.clickValue()) + " PER CLICK", 0, 38, PALE_GREEN);

        boolean hot = Math.hypot(mouseX - COIN_X, mouseY - COIN_Y) <= COIN_RADIUS;
        int radius = COIN_RADIUS - (pressTicks > 0 ? 3 : 0);
        disc(g, COIN_X, COIN_Y, radius + 1, 0xFF6A4A10);
        disc(g, COIN_X, COIN_Y, radius, hot ? 0xFFFFE08A : AMBER);
        disc(g, COIN_X, COIN_Y, radius - 7, hot ? 0xFFE0B040 : 0xFFE8B84A);
        centered(g, "A", COIN_X, COIN_Y - 4, 0xFF6A4A10);
        centered(g, "CLICK TO MINE", COIN_X, COIN_Y + COIN_RADIUS + 6, DIM_GREEN);
        hitArea(COIN_X - COIN_RADIUS, COIN_Y - COIN_RADIUS, COIN_RADIUS * 2, COIN_RADIUS * 2, () -> {
            if (Math.hypot(mouseX - COIN_X, mouseY - COIN_Y) <= COIN_RADIUS) mine();
        });

        for (int i = 0; i < CoinMinerLogic.UPGRADES.length; i++) {
            CoinMinerLogic.Upgrade upgrade = CoinMinerLogic.UPGRADES[i];
            int top = 2 + i * ROW_HEIGHT;
            int index = i;
            boolean affordable = game.canAfford(i);
            boolean rowHot = affordable && hovering(LIST_X, top, WIDTH - LIST_X, ROW_HEIGHT - 2);
            g.fill(LIST_X, top, WIDTH, top + ROW_HEIGHT - 2, rowHot ? PANEL_LIGHT : PANEL);
            outline(g, LIST_X, top, WIDTH - LIST_X, ROW_HEIGHT - 2, affordable ? (rowHot ? GREEN : DIM_GREEN) : 0xFF2A4A2A);
            text(g, upgrade.name() + "  x" + game.level(i), LIST_X + 4, top + 3, affordable ? PALE_GREEN : 0xFF6A8A6A);
            text(g, upgrade.description(), LIST_X + 4, top + 13, DIM_GREEN);
            String cost = CoinMinerLogic.format(game.cost(i));
            text(g, cost, WIDTH - 4 - font.width(cost), top + 3, affordable ? AMBER : 0xFF8A7A4A);
            hitArea(LIST_X, top, WIDTH - LIST_X, ROW_HEIGHT - 2, () -> {
                if (game.buy(index)) play(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8F + index * 0.15F);
            });
        }

        text(g, "TOTAL MINED " + CoinMinerLogic.format(game.totalMined()), 0, HEIGHT - 11, DIM_GREEN);
        button(g, resetConfirmTicks > 0 ? "SURE?" : "RESET", LIST_X + 52, HEIGHT - 16, 62, 14, () -> {
            if (resetConfirmTicks > 0) {
                saved = new CoinMinerLogic();
                game = saved;
                resetConfirmTicks = 0;
            } else {
                resetConfirmTicks = 60;
            }
        });
    }
}
