package com.craisinlord.antos.content.client.game.mini;

import com.craisinlord.antos.content.minigame.MinesweeperLogic;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

/** Minesweeper. Left click reveals (or chords on a number), right click or FLAG mode plants a flag. */
final class MinesweeperSession extends MiniGameSession {
    private record Size(String label, int width, int height, int mines) {}

    private static final Size[] SIZES = {
            new Size("EASY", 9, 9, 10),
            new Size("MEDIUM", 12, 9, 18),
            new Size("HARD", 16, 9, 30)
    };
    private static final int BOARD_TOP = 18;
    private static final int[] NUMBER_COLORS = {
            0, 0xFF7FB0FF, 0xFF65FF65, 0xFFFF7070, 0xFFB08CFF, 0xFFFFA050, 0xFF50E0E0, 0xFFE0E0E0, 0xFF909090
    };

    private final Random random = new Random();
    private MinesweeperLogic game;
    private Size size = SIZES[0];
    private boolean flagMode;
    private int ticks;
    private int cell;
    private int boardX;
    private boolean soundedEnd;

    @Override
    public void tick() {
        if (game != null && game.state() == MinesweeperLogic.State.PLAYING) ticks++;
        if (game != null && game.isOver() && !soundedEnd) {
            soundedEnd = true;
            if (game.state() == MinesweeperLogic.State.WON) play(SoundEvents.PLAYER_LEVELUP, 1.0F);
            else play(SoundEvents.GENERIC_EXPLODE.value(), 1.4F);
        }
    }

    private void start(Size chosen) {
        size = chosen;
        game = new MinesweeperLogic(chosen.width(), chosen.height(), chosen.mines(), random);
        ticks = 0;
        soundedEnd = false;
        flagMode = false;
        cell = Math.min(WIDTH / chosen.width(), (HEIGHT - BOARD_TOP) / chosen.height());
        boardX = (WIDTH - cell * chosen.width()) / 2;
    }

    @Override
    protected void draw(GuiGraphics g) {
        if (game == null) {
            centered(g, "MINESWEEPER", WIDTH / 2, 8, GREEN);
            centered(g, "LEFT CLICK REVEALS, RIGHT CLICK FLAGS", WIDTH / 2, 24, PALE_GREEN);
            centered(g, "CLICK A NUMBER TO CLEAR AROUND IT", WIDTH / 2, 36, DIM_GREEN);
            for (int i = 0; i < SIZES.length; i++) {
                Size choice = SIZES[i];
                button(g, choice.label(), 12 + i * 66, 70, 62, 20, () -> start(choice));
                centered(g, choice.width() + "X" + choice.height(), 43 + i * 66, 94, DIM_GREEN);
                centered(g, choice.mines() + " MINES", 43 + i * 66, 105, DIM_GREEN);
            }
            return;
        }

        text(g, "MINES " + game.flagsLeft(), 0, 5, AMBER);
        String time = Math.min(999, ticks / 20) + "S";
        centered(g, time, WIDTH / 2, 5, PALE_GREEN);
        button(g, "FLAG", WIDTH - 98, 1, 30, 13, true, () -> flagMode = !flagMode);
        if (flagMode) outline(g, WIDTH - 98, 1, 30, 13, AMBER);
        button(g, "NEW", WIDTH - 64, 1, 28, 13, () -> start(size));
        button(g, "MENU", WIDTH - 32, 1, 32, 13, () -> game = null);

        for (int y = 0; y < game.height(); y++) {
            for (int x = 0; x < game.width(); x++) {
                drawCell(g, x, y);
            }
        }
        if (game.isOver()) {
            boolean won = game.state() == MinesweeperLogic.State.WON;
            String banner = won ? "CLEARED!" : "BOOM!";
            int width = font.width(banner) + 12;
            int bx = (WIDTH - width) / 2, by = BOARD_TOP + cell * game.height() / 2 - 7;
            g.fill(bx, by, bx + width, by + 14, PANEL);
            outline(g, bx, by, width, 14, won ? GREEN : RED);
            centered(g, banner, WIDTH / 2, by + 3, won ? GREEN : RED);
        }
    }

    private void drawCell(GuiGraphics g, int x, int y) {
        int left = boardX + x * cell, top = BOARD_TOP + y * cell;
        boolean revealed = game.isRevealed(x, y);
        boolean showMine = game.state() == MinesweeperLogic.State.LOST && game.isMine(x, y) && !game.isFlagged(x, y);
        int base = revealed ? 0xFF0A140A : (hovering(left, top, cell, cell) && !game.isOver() ? 0xFF3E7A3E : 0xFF2E5E2E);
        if (game.isExploded(x, y)) base = 0xFF7A2020;
        g.fill(left, top, left + cell - 1, top + cell - 1, base);
        int centreX = left + (cell - 1) / 2, centreY = top + (cell - 1) / 2;
        if (revealed) {
            int count = game.adjacentMines(x, y);
            if (count > 0) g.drawString(font, Integer.toString(count), centreX - 2, centreY - 3, NUMBER_COLORS[count], false);
        } else if (game.isFlagged(x, y)) {
            drawFlag(g, centreX, centreY);
            if (game.state() == MinesweeperLogic.State.LOST && !game.isMine(x, y)) {
                g.fill(left + 1, top + 1, left + cell - 2, top + cell - 2, 0x66FF0000);
            }
        } else if (showMine) {
            disc(g, centreX, centreY, Math.max(2, cell / 4), PALE_GREEN);
        }
    }

    private static void drawFlag(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 1, cy - 4, cx + 1, cy + 4, PALE_GREEN);
        g.fill(cx - 4, cy - 4, cx, cy, RED);
        g.fill(cx - 3, cy - 3, cx, cy - 1, RED);
        g.fill(cx - 3, cy + 3, cx + 3, cy + 5, PALE_GREEN);
    }

    @Override
    protected boolean onMouseClicked(int mx, int my, int button) {
        if (game == null) return false;
        int x = Math.floorDiv(mx - boardX, cell), y = Math.floorDiv(my - BOARD_TOP, cell);
        if (x < 0 || y < 0 || x >= game.width() || y >= game.height() || game.isOver()) return false;
        if (button == 1 || (button == 0 && flagMode)) {
            game.toggleFlag(x, y);
            play(SoundEvents.NOTE_BLOCK_HAT.value(), 0.7F);
        } else if (button == 0) {
            if (game.isRevealed(x, y)) game.chord(x, y);
            else game.reveal(x, y);
            play(SoundEvents.NOTE_BLOCK_HAT.value(), 1.3F);
        }
        return true;
    }
}
