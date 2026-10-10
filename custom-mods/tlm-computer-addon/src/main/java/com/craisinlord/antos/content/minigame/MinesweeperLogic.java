package com.craisinlord.antos.content.minigame;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/** Minesweeper board. Mines are placed on the first reveal so the first click (and its neighbours) are always safe. */
public final class MinesweeperLogic {
    public enum State { READY, PLAYING, WON, LOST }

    private final int width;
    private final int height;
    private final int mineCount;
    private final Random random;
    private final boolean[] mines;
    private final boolean[] revealed;
    private final boolean[] flagged;
    private State state = State.READY;
    private int revealedCount;
    private int explodedAt = -1;

    public MinesweeperLogic(int width, int height, int mineCount, Random random) {
        if (width < 4 || height < 4 || mineCount < 1 || mineCount > width * height - 9) {
            throw new IllegalArgumentException("Invalid board: " + width + "x" + height + " with " + mineCount + " mines");
        }
        this.width = width;
        this.height = height;
        this.mineCount = mineCount;
        this.random = random;
        this.mines = new boolean[width * height];
        this.revealed = new boolean[width * height];
        this.flagged = new boolean[width * height];
    }

    public int width() { return width; }
    public int height() { return height; }
    public int mineCount() { return mineCount; }
    public State state() { return state; }
    public boolean isRevealed(int x, int y) { return revealed[y * width + x]; }
    public boolean isFlagged(int x, int y) { return flagged[y * width + x]; }
    public boolean isMine(int x, int y) { return mines[y * width + x]; }
    public boolean isExploded(int x, int y) { return explodedAt == y * width + x; }
    public boolean isOver() { return state == State.WON || state == State.LOST; }

    public int flagsLeft() {
        int flags = 0;
        for (boolean flag : flagged) {
            if (flag) flags++;
        }
        return mineCount - flags;
    }

    public int adjacentMines(int x, int y) {
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if ((dx != 0 || dy != 0) && inBounds(nx, ny) && mines[ny * width + nx]) count++;
            }
        }
        return count;
    }

    public void toggleFlag(int x, int y) {
        if (isOver() || !inBounds(x, y) || revealed[y * width + x]) return;
        flagged[y * width + x] = !flagged[y * width + x];
    }

    public void reveal(int x, int y) {
        if (isOver() || !inBounds(x, y) || flagged[y * width + x] || revealed[y * width + x]) return;
        if (state == State.READY) {
            placeMines(x, y);
            state = State.PLAYING;
        }
        if (mines[y * width + x]) {
            explodedAt = y * width + x;
            state = State.LOST;
            return;
        }
        floodReveal(x, y);
        checkWin();
    }

    /** Clicking a number whose flags match it reveals every other neighbour. */
    public void chord(int x, int y) {
        if (isOver() || !inBounds(x, y) || !revealed[y * width + x]) return;
        int flags = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (inBounds(nx, ny) && flagged[ny * width + nx]) flags++;
            }
        }
        if (flags != adjacentMines(x, y)) return;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                reveal(x + dx, y + dy);
            }
        }
    }

    private void placeMines(int safeX, int safeY) {
        int placed = 0;
        while (placed < mineCount) {
            int x = random.nextInt(width), y = random.nextInt(height);
            boolean nearStart = Math.abs(x - safeX) <= 1 && Math.abs(y - safeY) <= 1;
            if (nearStart || mines[y * width + x]) continue;
            mines[y * width + x] = true;
            placed++;
        }
    }

    private void floodReveal(int startX, int startY) {
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int x = cell[0], y = cell[1];
            if (!inBounds(x, y) || revealed[y * width + x] || flagged[y * width + x] || mines[y * width + x]) continue;
            revealed[y * width + x] = true;
            revealedCount++;
            if (adjacentMines(x, y) == 0) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        queue.add(new int[]{x + dx, y + dy});
                    }
                }
            }
        }
    }

    private void checkWin() {
        if (revealedCount == width * height - mineCount) {
            state = State.WON;
            for (int i = 0; i < mines.length; i++) {
                if (mines[i]) flagged[i] = true;
            }
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
