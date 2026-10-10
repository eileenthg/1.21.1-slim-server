package com.craisinlord.antos.content.client.game.mini;

import com.craisinlord.antos.content.minigame.CheckersLogic;
import com.craisinlord.antos.content.minigame.Difficulty;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/** Checkers against the computer. You are the pale pieces at the bottom and move first. Captures are mandatory. */
final class CheckersSession extends MiniGameSession {
    private static final int CELL = 16;
    private static final int BOARD_X = 4;
    private static final int BOARD_Y = 14;
    private static final int PLAYER_COLOR = 0xFFE4FFE4;
    private static final int AI_COLOR = 0xFFD9822B;
    private static final int LIGHT_SQUARE = 0xFF2E5E2E;
    private static final int DARK_SQUARE = 0xFF0E200E;
    private static final int MIN_COMPUTER_TICKS = 10;

    private static Difficulty savedDifficulty = Difficulty.MEDIUM;
    private static int wins;
    private static int losses;

    private final Random random = new Random();
    private CheckersLogic game = new CheckersLogic();
    private int selected = -1;
    private int lastFrom = -1;
    private int lastTo = -1;
    private CompletableFuture<CheckersLogic.Move> computerMove;
    private int computerTicks;
    private boolean scored;
    private String message = "";

    @Override
    public void tick() {
        if (computerMove != null) {
            computerTicks++;
            if (computerTicks >= MIN_COMPUTER_TICKS && computerMove.isDone()) {
                CheckersLogic.Move move = computerMove.getNow(null);
                computerMove = null;
                if (move != null) apply(move);
            }
        }
        if (!scored) {
            CheckersLogic.Result result = game.result();
            if (result != CheckersLogic.Result.ONGOING) {
                scored = true;
                if (result == CheckersLogic.Result.PLAYER_WINS) {
                    wins++;
                    play(SoundEvents.PLAYER_LEVELUP, 1.0F);
                } else if (result == CheckersLogic.Result.AI_WINS) {
                    losses++;
                }
            }
        }
    }

    private void newGame() {
        game = new CheckersLogic();
        selected = lastFrom = lastTo = -1;
        computerMove = null;
        scored = false;
        message = "";
    }

    private void apply(CheckersLogic.Move move) {
        game.play(move);
        lastFrom = move.from();
        lastTo = move.to();
        selected = -1;
        play(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), move.isCapture() ? 1.4F : 1.0F);
    }

    private void startComputerTurn() {
        Difficulty difficulty = savedDifficulty;
        CheckersLogic snapshot = game;
        computerTicks = 0;
        computerMove = CompletableFuture.supplyAsync(() -> snapshot.chooseComputerMove(difficulty, random));
    }

    private boolean playerCanAct() {
        return game.isPlayerToMove() && computerMove == null && game.result() == CheckersLogic.Result.ONGOING;
    }

    private List<CheckersLogic.Move> movesFrom(int square) {
        List<CheckersLogic.Move> moves = new ArrayList<>();
        for (CheckersLogic.Move move : game.legalMoves()) {
            if (move.from() == square) moves.add(move);
        }
        return moves;
    }

    @Override
    protected void draw(GuiGraphics g) {
        text(g, "CHECKERS", 0, 2, GREEN);
        List<CheckersLogic.Move> selectedMoves = selected >= 0 ? movesFrom(selected) : List.of();
        for (int row = 0; row < CheckersLogic.SIZE; row++) {
            for (int col = 0; col < CheckersLogic.SIZE; col++) {
                int x = BOARD_X + col * CELL, y = BOARD_Y + row * CELL, square = row * CheckersLogic.SIZE + col;
                g.fill(x, y, x + CELL, y + CELL, CheckersLogic.isDark(row, col) ? DARK_SQUARE : LIGHT_SQUARE);
                if (square == lastFrom || square == lastTo) g.fill(x, y, x + CELL, y + CELL, 0x44FFD36B);
                if (square == selected) outline(g, x, y, CELL, CELL, AMBER);
                else if (playerCanAct() && hovering(x, y, CELL, CELL) && CheckersLogic.isDark(row, col)) outline(g, x, y, CELL, CELL, DIM_GREEN);
                drawPiece(g, game.piece(square), x + CELL / 2, y + CELL / 2);
            }
        }
        for (CheckersLogic.Move move : selectedMoves) {
            int x = BOARD_X + (move.to() % 8) * CELL + CELL / 2, y = BOARD_Y + (move.to() / 8) * CELL + CELL / 2;
            disc(g, x, y, 3, move.isCapture() ? RED : GREEN);
        }

        int panelX = BOARD_X + CELL * 8 + 8;
        CheckersLogic.Result result = game.result();
        String status = switch (result) {
            case ONGOING -> computerMove != null ? "THINKING..." : "YOUR TURN";
            case PLAYER_WINS -> "YOU WIN!";
            case AI_WINS -> "YOU LOSE";
            case DRAW -> "DRAW";
        };
        int statusColor = result == CheckersLogic.Result.PLAYER_WINS ? GREEN : result == CheckersLogic.Result.AI_WINS ? RED : PALE_GREEN;
        text(g, status, panelX, BOARD_Y, statusColor);
        if (!message.isEmpty() && playerCanAct()) text(g, message, panelX, BOARD_Y + 12, AMBER);
        text(g, "WINS   " + wins, panelX, BOARD_Y + 34, PALE_GREEN);
        text(g, "LOSSES " + losses, panelX, BOARD_Y + 46, PALE_GREEN);
        button(g, "AI: " + savedDifficulty.label(), panelX, BOARD_Y + 70, 68, 14, () -> {
            savedDifficulty = Difficulty.values()[(savedDifficulty.ordinal() + 1) % Difficulty.values().length];
        });
        button(g, "NEW GAME", panelX, BOARD_Y + 90, 68, 14, this::newGame);
    }

    private void drawPiece(GuiGraphics g, int piece, int cx, int cy) {
        if (piece == CheckersLogic.EMPTY) return;
        int color = piece > 0 ? PLAYER_COLOR : AI_COLOR;
        disc(g, cx, cy, 6, 0xFF050A05);
        disc(g, cx, cy, 5, color);
        disc(g, cx, cy, 3, piece > 0 ? 0xFFB8E0B8 : 0xFFB8601A);
        if (Math.abs(piece) == 2) {
            g.fill(cx - 2, cy - 1, cx + 3, cy + 1, AMBER);
            g.fill(cx - 2, cy - 2, cx - 1, cy - 1, AMBER);
            g.fill(cx, cy - 3, cx + 1, cy - 1, AMBER);
            g.fill(cx + 2, cy - 2, cx + 3, cy - 1, AMBER);
        }
    }

    @Override
    protected boolean onMouseClicked(int mx, int my, int button) {
        if (button != 0 || !playerCanAct()) return false;
        int col = Math.floorDiv(mx - BOARD_X, CELL), row = Math.floorDiv(my - BOARD_Y, CELL);
        if (col < 0 || row < 0 || col >= 8 || row >= 8) return false;
        int square = row * 8 + col;
        message = "";

        if (selected >= 0) {
            for (CheckersLogic.Move move : movesFrom(selected)) {
                if (move.to() == square) {
                    apply(move);
                    if (game.result() == CheckersLogic.Result.ONGOING) startComputerTurn();
                    return true;
                }
            }
        }
        int piece = game.piece(square);
        if (piece > 0) {
            if (movesFrom(square).isEmpty()) {
                boolean mustCapture = game.legalMoves().stream().anyMatch(CheckersLogic.Move::isCapture);
                message = mustCapture ? "MUST CAPTURE" : "NO MOVES";
                selected = -1;
            } else {
                selected = square;
            }
        } else {
            selected = -1;
        }
        return true;
    }
}
