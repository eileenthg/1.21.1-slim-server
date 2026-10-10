package com.craisinlord.antos.content.minigame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 3x3 noughts and crosses. The player is X, the computer is O. */
public final class TicTacToeLogic {
    public static final int EMPTY = 0;
    public static final int X = 1;
    public static final int O = 2;

    public enum Result { ONGOING, X_WINS, O_WINS, DRAW }

    private static final int[][] LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}
    };

    private final int[] cells = new int[9];

    public int cell(int index) {
        return cells[index];
    }

    public boolean place(int index, int who) {
        if (index < 0 || index > 8 || cells[index] != EMPTY || result() != Result.ONGOING) return false;
        cells[index] = who;
        return true;
    }

    public Result result() {
        return result(cells);
    }

    /** The three cells of the winning line, or null. */
    public int[] winningLine() {
        for (int[] line : LINES) {
            if (cells[line[0]] != EMPTY && cells[line[0]] == cells[line[1]] && cells[line[1]] == cells[line[2]]) return line;
        }
        return null;
    }

    /** Chooses and plays a move for O. Returns the cell played, or -1 if the game is over. */
    public int playComputer(Difficulty difficulty, Random random) {
        if (result() != Result.ONGOING) return -1;
        int move = chooseMove(difficulty, random);
        cells[move] = O;
        return move;
    }

    private int chooseMove(Difficulty difficulty, Random random) {
        List<Integer> free = freeCells(cells);
        if (difficulty == Difficulty.EASY) return free.get(random.nextInt(free.size()));
        if (difficulty == Difficulty.MEDIUM) {
            int win = findCompletingCell(O);
            if (win >= 0) return win;
            int block = findCompletingCell(X);
            if (block >= 0) return block;
            if (cells[4] == EMPTY) return 4;
            return free.get(random.nextInt(free.size()));
        }
        int best = Integer.MIN_VALUE;
        List<Integer> bestMoves = new ArrayList<>();
        for (int move : free) {
            cells[move] = O;
            int score = minimax(cells, false, 1);
            cells[move] = EMPTY;
            if (score > best) {
                best = score;
                bestMoves.clear();
            }
            if (score == best) bestMoves.add(move);
        }
        return bestMoves.get(random.nextInt(bestMoves.size()));
    }

    private int findCompletingCell(int who) {
        for (int move : freeCells(cells)) {
            cells[move] = who;
            boolean wins = result(cells) == (who == X ? Result.X_WINS : Result.O_WINS);
            cells[move] = EMPTY;
            if (wins) return move;
        }
        return -1;
    }

    /** Score from the computer's side: positive when O wins, preferring faster wins and slower losses. */
    private static int minimax(int[] board, boolean oToMove, int depth) {
        Result result = result(board);
        if (result == Result.O_WINS) return 10 - depth;
        if (result == Result.X_WINS) return depth - 10;
        if (result == Result.DRAW) return 0;
        int best = oToMove ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int move : freeCells(board)) {
            board[move] = oToMove ? O : X;
            int score = minimax(board, !oToMove, depth + 1);
            board[move] = EMPTY;
            best = oToMove ? Math.max(best, score) : Math.min(best, score);
        }
        return best;
    }

    private static List<Integer> freeCells(int[] board) {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i] == EMPTY) free.add(i);
        }
        return free;
    }

    private static Result result(int[] board) {
        for (int[] line : LINES) {
            int a = board[line[0]];
            if (a != EMPTY && a == board[line[1]] && a == board[line[2]]) return a == X ? Result.X_WINS : Result.O_WINS;
        }
        for (int cell : board) {
            if (cell == EMPTY) return Result.ONGOING;
        }
        return Result.DRAW;
    }
}
