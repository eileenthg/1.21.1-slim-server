package com.craisinlord.antos.content.client.game.mini;

import com.craisinlord.antos.content.minigame.Difficulty;
import com.craisinlord.antos.content.minigame.TicTacToeLogic;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

/** Noughts and crosses against the computer. You are X and move first. */
final class TicTacToeSession extends MiniGameSession {
    private static final int CELL = 40;
    private static final int BOARD_X = 8;
    private static final int BOARD_Y = 26;
    private static final int COMPUTER_DELAY_TICKS = 8;

    private static int wins;
    private static int losses;
    private static int draws;
    private static Difficulty savedDifficulty = Difficulty.MEDIUM;

    private final Random random = new Random();
    private TicTacToeLogic game = new TicTacToeLogic();
    private int computerCountdown;
    private boolean scored;

    @Override
    public void tick() {
        if (computerCountdown > 0 && --computerCountdown == 0) {
            int move = game.playComputer(savedDifficulty, random);
            if (move >= 0) play(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.2F);
        }
        if (!scored && game.result() != TicTacToeLogic.Result.ONGOING) {
            scored = true;
            switch (game.result()) {
                case X_WINS -> {
                    wins++;
                    play(SoundEvents.PLAYER_LEVELUP, 1.0F);
                }
                case O_WINS -> losses++;
                default -> draws++;
            }
        }
    }

    private void newGame() {
        game = new TicTacToeLogic();
        computerCountdown = 0;
        scored = false;
    }

    @Override
    protected void draw(GuiGraphics g) {
        text(g, "TIC-TAC-TOE", 0, 2, GREEN);
        int[] winLine = game.winningLine();
        for (int i = 0; i < 9; i++) {
            int x = BOARD_X + (i % 3) * CELL, y = BOARD_Y + (i / 3) * CELL;
            boolean playable = game.cell(i) == TicTacToeLogic.EMPTY && computerCountdown == 0 && game.result() == TicTacToeLogic.Result.ONGOING;
            boolean inWin = winLine != null && (i == winLine[0] || i == winLine[1] || i == winLine[2]);
            g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, inWin ? 0xFF2A5A2A : (playable && hovering(x, y, CELL, CELL) ? PANEL_LIGHT : PANEL));
            outline(g, x, y, CELL, CELL, DIM_GREEN);
            drawMark(g, game.cell(i), x + CELL / 2, y + CELL / 2, inWin);
            final int cell = i;
            if (playable) {
                hitArea(x, y, CELL, CELL, () -> {
                    if (game.place(cell, TicTacToeLogic.X)) {
                        play(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.8F);
                        computerCountdown = COMPUTER_DELAY_TICKS;
                    }
                });
            }
        }

        int panelX = BOARD_X + CELL * 3 + 14;
        TicTacToeLogic.Result result = game.result();
        String status = switch (result) {
            case ONGOING -> computerCountdown > 0 ? "THINKING..." : "YOUR TURN (X)";
            case X_WINS -> "YOU WIN!";
            case O_WINS -> "YOU LOSE";
            case DRAW -> "DRAW";
        };
        int statusColor = result == TicTacToeLogic.Result.X_WINS ? GREEN : result == TicTacToeLogic.Result.O_WINS ? RED : PALE_GREEN;
        text(g, status, panelX, BOARD_Y, statusColor);
        text(g, "WINS   " + wins, panelX, BOARD_Y + 22, PALE_GREEN);
        text(g, "LOSSES " + losses, panelX, BOARD_Y + 34, PALE_GREEN);
        text(g, "DRAWS  " + draws, panelX, BOARD_Y + 46, PALE_GREEN);
        button(g, "AI: " + savedDifficulty.label(), panelX, BOARD_Y + 66, 68, 14, () -> {
            savedDifficulty = Difficulty.values()[(savedDifficulty.ordinal() + 1) % Difficulty.values().length];
        });
        button(g, "NEW GAME", panelX, BOARD_Y + 86, 68, 14, this::newGame);
    }

    private void drawMark(GuiGraphics g, int mark, int cx, int cy, boolean highlight) {
        int color = highlight ? GREEN : PALE_GREEN;
        if (mark == TicTacToeLogic.X) {
            for (int d = -12; d <= 12; d++) {
                g.fill(cx + d - 1, cy + d - 1, cx + d + 1, cy + d + 1, color);
                g.fill(cx + d - 1, cy - d - 1, cx + d + 1, cy - d + 1, color);
            }
        } else if (mark == TicTacToeLogic.O) {
            disc(g, cx, cy, 13, highlight ? AMBER : 0xFFD9822B);
            disc(g, cx, cy, 9, highlight ? 0xFF2A5A2A : PANEL);
        }
    }
}
