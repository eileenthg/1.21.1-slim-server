package com.craisinlord.antos.content.minigame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * American checkers on an 8x8 board. The player moves first from the bottom and moves up the board; the computer moves down.
 * Captures are mandatory, a jump can continue into further jumps, and a man that reaches the far row becomes a king
 * and ends its move.
 */
public final class CheckersLogic {
    public static final int SIZE = 8;
    public static final int EMPTY = 0;
    public static final int PLAYER_MAN = 1;
    public static final int PLAYER_KING = 2;
    public static final int AI_MAN = -1;
    public static final int AI_KING = -2;
    private static final int DRAW_AFTER_QUIET_MOVES = 80;

    public enum Result { ONGOING, PLAYER_WINS, AI_WINS, DRAW }

    /** A full move. {@code path} lists every square landed on, starting with the origin. */
    public record Move(int[] path, int[] captured) {
        public int from() { return path[0]; }
        public int to() { return path[path.length - 1]; }
        public boolean isCapture() { return captured.length > 0; }
    }

    private final byte[] board = new byte[SIZE * SIZE];
    private boolean playerToMove = true;
    private int quietMoves;

    public CheckersLogic() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (!isDark(row, col)) continue;
                if (row < 3) board[row * SIZE + col] = AI_MAN;
                else if (row > 4) board[row * SIZE + col] = PLAYER_MAN;
            }
        }
    }

    private CheckersLogic(byte[] source, boolean playerToMove) {
        System.arraycopy(source, 0, board, 0, board.length);
        this.playerToMove = playerToMove;
    }

    /** Builds a position from 64 characters (row-major, rows top to bottom): . empty, p/P player man/king, a/A AI man/king. */
    public static CheckersLogic fromString(String layout, boolean playerToMove) {
        String compact = layout.replaceAll("\\s", "");
        if (compact.length() != 64) throw new IllegalArgumentException("Need 64 squares, got " + compact.length());
        byte[] cells = new byte[64];
        for (int i = 0; i < 64; i++) {
            cells[i] = switch (compact.charAt(i)) {
                case 'p' -> PLAYER_MAN;
                case 'P' -> PLAYER_KING;
                case 'a' -> AI_MAN;
                case 'A' -> AI_KING;
                default -> EMPTY;
            };
        }
        return new CheckersLogic(cells, playerToMove);
    }

    public static boolean isDark(int row, int col) {
        return (row + col) % 2 == 1;
    }

    public int piece(int square) { return board[square]; }
    public boolean isPlayerToMove() { return playerToMove; }

    public List<Move> legalMoves() {
        return legalMoves(board, playerToMove);
    }

    public Result result() {
        if (legalMoves().isEmpty()) return playerToMove ? Result.AI_WINS : Result.PLAYER_WINS;
        return quietMoves >= DRAW_AFTER_QUIET_MOVES ? Result.DRAW : Result.ONGOING;
    }

    /** Plays a move that came from {@link #legalMoves()}. */
    public void play(Move move) {
        boolean moverIsMan = Math.abs(board[move.from()]) == 1;
        boolean capture = move.isCapture();
        apply(board, move);
        quietMoves = capture || moverIsMan ? 0 : quietMoves + 1;
        playerToMove = !playerToMove;
    }

    public Move chooseComputerMove(Difficulty difficulty, Random random) {
        List<Move> moves = legalMoves(board, false);
        if (moves.isEmpty()) return null;
        if (difficulty == Difficulty.EASY && random.nextInt(100) < 35) return moves.get(random.nextInt(moves.size()));
        int depth = switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 4;
            case HARD -> 8;
        };
        int bestScore = Integer.MIN_VALUE;
        List<Move> best = new ArrayList<>();
        for (Move move : moves) {
            byte[] next = board.clone();
            apply(next, move);
            int score = -negamax(next, true, depth - 1, -Integer.MAX_VALUE, Integer.MAX_VALUE);
            if (score > bestScore) {
                bestScore = score;
                best.clear();
            }
            if (score == bestScore) best.add(move);
        }
        return best.get(random.nextInt(best.size()));
    }

    // ---------------------------------------------------------------- move generation

    private static List<Move> legalMoves(byte[] cells, boolean forPlayer) {
        List<Move> captures = new ArrayList<>();
        List<Move> simple = new ArrayList<>();
        for (int square = 0; square < cells.length; square++) {
            int piece = cells[square];
            if (piece == EMPTY || (piece > 0) != forPlayer) continue;
            byte[] vacated = cells.clone();
            vacated[square] = EMPTY;
            collectCaptures(vacated, piece, square, new int[]{square}, new int[0], captures);
            if (captures.isEmpty()) collectSimpleMoves(cells, piece, square, simple);
        }
        return captures.isEmpty() ? simple : captures;
    }

    private static int[] directionRows(int piece) {
        if (Math.abs(piece) == 2) return new int[]{-1, 1};
        return piece > 0 ? new int[]{-1} : new int[]{1};
    }

    private static void collectSimpleMoves(byte[] cells, int piece, int square, List<Move> out) {
        int row = square / SIZE, col = square % SIZE;
        for (int dr : directionRows(piece)) {
            for (int dc : new int[]{-1, 1}) {
                int nr = row + dr, nc = col + dc;
                if (inBounds(nr, nc) && cells[nr * SIZE + nc] == EMPTY) {
                    out.add(new Move(new int[]{square, nr * SIZE + nc}, new int[0]));
                }
            }
        }
    }

    /** Depth-first search for jump sequences. Jumped pieces are lifted off a scratch copy so they cannot be jumped twice. */
    private static void collectCaptures(byte[] cells, int piece, int square, int[] path, int[] captured, List<Move> out) {
        int row = square / SIZE, col = square % SIZE;
        boolean extended = false;
        for (int dr : directionRows(piece)) {
            for (int dc : new int[]{-1, 1}) {
                int midR = row + dr, midC = col + dc, endR = row + 2 * dr, endC = col + 2 * dc;
                if (!inBounds(endR, endC)) continue;
                int middle = cells[midR * SIZE + midC];
                boolean enemy = middle != EMPTY && (middle > 0) != (piece > 0);
                if (!enemy || cells[endR * SIZE + endC] != EMPTY) continue;
                extended = true;
                byte[] scratch = cells.clone();
                scratch[midR * SIZE + midC] = EMPTY;
                int end = endR * SIZE + endC;
                int[] nextPath = Arrays.copyOf(path, path.length + 1);
                nextPath[path.length] = end;
                int[] nextCaptured = Arrays.copyOf(captured, captured.length + 1);
                nextCaptured[captured.length] = midR * SIZE + midC;
                boolean promotes = Math.abs(piece) == 1 && reachesKingRow(piece, endR);
                if (promotes) out.add(new Move(nextPath, nextCaptured));
                else collectCaptures(scratch, piece, end, nextPath, nextCaptured, out);
            }
        }
        if (!extended && captured.length > 0) out.add(new Move(path, captured));
    }

    private static boolean reachesKingRow(int piece, int row) {
        return piece > 0 ? row == 0 : row == SIZE - 1;
    }

    private static boolean inBounds(int row, int col) {
        return row >= 0 && col >= 0 && row < SIZE && col < SIZE;
    }

    private static void apply(byte[] cells, Move move) {
        byte piece = cells[move.from()];
        cells[move.from()] = EMPTY;
        for (int square : move.captured()) cells[square] = EMPTY;
        int to = move.to();
        if (Math.abs(piece) == 1 && reachesKingRow(piece, to / SIZE)) piece = (byte) (piece * 2);
        cells[to] = piece;
    }

    // ---------------------------------------------------------------- search

    /** Score from the point of view of whoever is about to move; the player is "positive" in the board encoding. */
    private static int negamax(byte[] cells, boolean playerToMove, int depth, int alpha, int beta) {
        List<Move> moves = legalMoves(cells, playerToMove);
        if (moves.isEmpty()) return -10_000 - depth;
        if (depth <= 0) return evaluate(cells, playerToMove);
        int best = Integer.MIN_VALUE + 1;
        for (Move move : moves) {
            byte[] next = cells.clone();
            apply(next, move);
            best = Math.max(best, -negamax(next, !playerToMove, depth - 1, -beta, -alpha));
            alpha = Math.max(alpha, best);
            if (alpha >= beta) break;
        }
        return best;
    }

    private static int evaluate(byte[] cells, boolean playerToMove) {
        int score = 0;
        for (int square = 0; square < cells.length; square++) {
            int piece = cells[square];
            if (piece == EMPTY) continue;
            int row = square / SIZE, col = square % SIZE;
            int value;
            if (Math.abs(piece) == 2) {
                value = 170;
            } else {
                int advance = piece > 0 ? SIZE - 1 - row : row;
                value = 100 + advance * 4;
            }
            if (col >= 2 && col <= 5 && row >= 2 && row <= 5) value += 3;
            score += piece > 0 ? value : -value;
        }
        return playerToMove ? score : -score;
    }
}
