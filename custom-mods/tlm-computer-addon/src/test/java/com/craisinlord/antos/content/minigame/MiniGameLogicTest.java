package com.craisinlord.antos.content.minigame;

import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MiniGameLogicTest {
    // ------------------------------------------------------------------ coin miner

    @Test
    public void coinMinerClickAndBuy() {
        CoinMinerLogic miner = new CoinMinerLogic();
        assertFalse(miner.buy(0));
        for (int i = 0; i < 10; i++) miner.click();
        assertEquals(10, miner.coins(), 1e-9);
        assertTrue(miner.buy(0));
        assertEquals(0, miner.coins(), 1e-9);
        assertEquals(2, miner.clickValue(), 1e-9);
        assertTrue("cost grows after buying", miner.cost(0) > 10);
    }

    @Test
    public void coinMinerPassiveIncome() {
        CoinMinerLogic miner = new CoinMinerLogic();
        for (int i = 0; i < 50; i++) miner.click();
        assertTrue(miner.buy(1));
        double before = miner.coins();
        for (int i = 0; i < CoinMinerLogic.TICKS_PER_SECOND; i++) miner.tick();
        assertEquals(before + 1.0, miner.coins(), 1e-9);
    }

    @Test
    public void coinMinerFormat() {
        assertEquals("999", CoinMinerLogic.format(999.9));
        assertEquals("1.5K", CoinMinerLogic.format(1500));
        assertEquals("2.0M", CoinMinerLogic.format(2_000_000));
    }

    // ------------------------------------------------------------------ tic-tac-toe

    @Test
    public void hardComputerNeverLosesTicTacToe() {
        explore(new int[0]);
    }

    /** Replays each human move sequence from scratch, so every possible human line is tried against the hard computer. */
    private static void explore(int[] humanMoves) {
        TicTacToeLogic game = new TicTacToeLogic();
        for (int move : humanMoves) {
            assertTrue(game.place(move, TicTacToeLogic.X));
            if (game.result() == TicTacToeLogic.Result.ONGOING) game.playComputer(Difficulty.HARD, new Random(7));
        }
        assertTrue("human must never win: " + java.util.Arrays.toString(humanMoves), game.result() != TicTacToeLogic.Result.X_WINS);
        if (game.result() != TicTacToeLogic.Result.ONGOING) return;
        for (int cell = 0; cell < 9; cell++) {
            if (game.cell(cell) != TicTacToeLogic.EMPTY) continue;
            int[] next = java.util.Arrays.copyOf(humanMoves, humanMoves.length + 1);
            next[humanMoves.length] = cell;
            explore(next);
        }
    }

    @Test
    public void mediumComputerTakesWinThenBlocks() {
        TicTacToeLogic game = new TicTacToeLogic();
        game.place(0, TicTacToeLogic.X);
        game.place(1, TicTacToeLogic.X);
        assertEquals(2, game.playComputer(Difficulty.MEDIUM, new Random(3)));
        game = new TicTacToeLogic();
        game.place(0, TicTacToeLogic.O);
        game.place(3, TicTacToeLogic.X);
        game.place(4, TicTacToeLogic.O);
        game.place(6, TicTacToeLogic.X);
        // O to move with 0 and 4 owned: wins at 8 rather than blocking column 0-3-6 which is already full
        assertEquals(8, game.playComputer(Difficulty.MEDIUM, new Random(3)));
        assertEquals(TicTacToeLogic.Result.O_WINS, game.result());
        assertFalse(game.place(2, TicTacToeLogic.X));
    }

    // ------------------------------------------------------------------ minesweeper

    @Test
    public void minesweeperFirstClickIsAlwaysSafe() {
        for (int seed = 0; seed < 200; seed++) {
            MinesweeperLogic game = new MinesweeperLogic(9, 9, 10, new Random(seed));
            game.reveal(4, 4);
            assertTrue(game.state() != MinesweeperLogic.State.LOST);
            assertTrue(game.isRevealed(4, 4));
            assertEquals(0, game.adjacentMines(4, 4));
        }
    }

    @Test
    public void minesweeperCanBeWonAndLost() {
        MinesweeperLogic game = new MinesweeperLogic(9, 9, 10, new Random(5));
        game.reveal(0, 0);
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (!game.isMine(x, y)) game.reveal(x, y);
            }
        }
        assertEquals(MinesweeperLogic.State.WON, game.state());
        assertEquals(0, game.flagsLeft());

        MinesweeperLogic lost = new MinesweeperLogic(9, 9, 10, new Random(5));
        lost.reveal(0, 0);
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (lost.isMine(x, y)) {
                    lost.reveal(x, y);
                    assertEquals(MinesweeperLogic.State.LOST, lost.state());
                    assertTrue(lost.isExploded(x, y));
                    return;
                }
            }
        }
    }

    @Test
    public void minesweeperFlagsBlockRevealAndChordWorks() {
        MinesweeperLogic game = new MinesweeperLogic(9, 9, 10, new Random(11));
        game.reveal(4, 4);
        int mineX = -1, mineY = -1;
        for (int y = 0; y < 9 && mineX < 0; y++) {
            for (int x = 0; x < 9; x++) {
                if (game.isMine(x, y)) {
                    mineX = x;
                    mineY = y;
                    break;
                }
            }
        }
        game.toggleFlag(mineX, mineY);
        assertEquals(9, game.flagsLeft());
        game.reveal(mineX, mineY);
        assertTrue("flagged cells cannot be revealed", game.state() != MinesweeperLogic.State.LOST);
        game.toggleFlag(mineX, mineY);
        assertEquals(10, game.flagsLeft());

        // Flag every mine, then chord every revealed number: nothing should explode.
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (game.isMine(x, y)) game.toggleFlag(x, y);
            }
        }
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                game.chord(x, y);
            }
        }
        assertTrue(game.state() != MinesweeperLogic.State.LOST);
    }

    // ------------------------------------------------------------------ checkers

    @Test
    public void checkersOpeningHasSevenMoves() {
        CheckersLogic game = new CheckersLogic();
        assertEquals(7, game.legalMoves().size());
        assertEquals(CheckersLogic.Result.ONGOING, game.result());
    }

    @Test
    public void checkersCapturesAreMandatoryAndChain() {
        // Player man at row 7 col 0 can double jump over (6,1) and (4,3)... build explicit layout.
        String layout = """
                ........
                ........
                ........
                ..a.....
                ........
                ..a.....
                .p......
                ........
                """;
        // (6,1) player man; AI at (5,2) can be jumped to (4,3); then AI at (3,2) can be jumped to (2,1).
        CheckersLogic game = CheckersLogic.fromString(layout, true);
        List<CheckersLogic.Move> moves = game.legalMoves();
        assertEquals(1, moves.size());
        CheckersLogic.Move move = moves.get(0);
        assertTrue(move.isCapture());
        assertEquals(2, move.captured().length);
        assertEquals(6 * 8 + 1, move.from());
        assertEquals(2 * 8 + 1, move.to());
        game.play(move);
        assertEquals(CheckersLogic.Result.PLAYER_WINS, game.result());
    }

    @Test
    public void checkersManPromotesAndStops() {
        // A man that jumps into the far row becomes a king and its move ends there.
        String promote = """
                ........
                .a......
                ..p.....
                ........
                ........
                ........
                ........
                ........
                """;
        CheckersLogic second = CheckersLogic.fromString(promote, true);
        List<CheckersLogic.Move> moves = second.legalMoves();
        assertEquals(1, moves.size());
        assertTrue(moves.get(0).isCapture());
        second.play(moves.get(0));
        assertEquals(CheckersLogic.PLAYER_KING, second.piece(0));
    }

    @Test
    public void checkersComputerPlaysLegalMovesToTheEnd() {
        Random random = new Random(42);
        CheckersLogic game = new CheckersLogic();
        int plies = 0;
        while (game.result() == CheckersLogic.Result.ONGOING && plies < 400) {
            List<CheckersLogic.Move> moves = game.legalMoves();
            assertFalse(moves.isEmpty());
            if (game.isPlayerToMove()) {
                game.play(moves.get(random.nextInt(moves.size())));
            } else {
                CheckersLogic.Move move = game.chooseComputerMove(Difficulty.MEDIUM, random);
                assertNotNull(move);
                assertTrue("computer move must be legal", moves.stream().anyMatch(m -> m.from() == move.from() && m.to() == move.to()));
                game.play(move);
            }
            plies++;
        }
        assertTrue("game should finish", game.result() != CheckersLogic.Result.ONGOING);
    }

    @Test
    public void checkersHardComputerBeatsRandomPlayer() {
        int computerWins = 0;
        for (int seed = 0; seed < 3; seed++) {
            Random random = new Random(seed);
            CheckersLogic game = new CheckersLogic();
            int plies = 0;
            while (game.result() == CheckersLogic.Result.ONGOING && plies < 400) {
                List<CheckersLogic.Move> moves = game.legalMoves();
                game.play(game.isPlayerToMove() ? moves.get(random.nextInt(moves.size())) : game.chooseComputerMove(Difficulty.HARD, random));
                plies++;
            }
            if (game.result() == CheckersLogic.Result.AI_WINS) computerWins++;
        }
        assertEquals(3, computerWins);
    }

    @Test
    public void checkersNoMovesForSideToMoveLoses() {
        CheckersLogic game = CheckersLogic.fromString("........".repeat(8), true);
        assertEquals(CheckersLogic.Result.AI_WINS, game.result());
        assertNull(game.chooseComputerMove(Difficulty.EASY, new Random(1)));
    }

    // ------------------------------------------------------------------ paddle ball

    @Test
    public void paddleBallServesAfterCountdown() {
        PaddleBallSim sim = new PaddleBallSim(PaddleBallSim.Level.MEDIUM, new Random(1));
        assertTrue(sim.isFrozen());
        for (int i = 0; i < PaddleBallSim.START_COOLDOWN; i++) sim.tick(false, false);
        assertFalse(sim.isFrozen());
        assertEquals(PaddleBallSim.BALL_SPEED_X, sim.velocityX(), 1e-6);
    }

    @Test
    public void paddleBallIdlePlayerIsEventuallyScoredOn() {
        PaddleBallSim sim = new PaddleBallSim(PaddleBallSim.Level.MEDIUM, new Random(1));
        boolean bounced = false;
        for (int i = 0; i < 2000 && sim.state() == PaddleBallSim.State.RUNNING && !bounced; i++) {
            sim.tick(false, false);
            PaddleBallSim.Event event;
            while ((event = sim.pollEvent()) != null) {
                if (event == PaddleBallSim.Event.AI_POINT) {
                    bounced = true;
                }
            }
        }
        assertTrue("an idle player is eventually scored on", bounced);
    }

    @Test
    public void paddleBallWallsKeepBallInside() {
        PaddleBallSim sim = new PaddleBallSim(PaddleBallSim.Level.HARD, new Random(9));
        for (int i = 0; i < 5000 && sim.state() == PaddleBallSim.State.RUNNING; i++) {
            sim.tick(false, false);
            assertTrue(sim.ballY() >= 0 && sim.ballY() <= PaddleBallSim.BOARD_HEIGHT);
        }
    }

    @Test
    public void paddleBallAiDefendsAgainstAnIdlePlayerButFirstToSevenEndsTheGame() {
        PaddleBallSim sim = new PaddleBallSim(PaddleBallSim.Level.HARD, new Random(2));
        for (int i = 0; i < 20_000 && sim.state() == PaddleBallSim.State.RUNNING; i++) sim.tick(false, false);
        assertEquals(PaddleBallSim.State.LOST, sim.state());
        assertEquals(PaddleBallSim.POINTS_TO_WIN, sim.aiScore());
    }

    @Test
    public void paddleBallTrackingPlayerReturnsBallsAndScores() {
        PaddleBallSim sim = new PaddleBallSim(PaddleBallSim.Level.EASY, new Random(4));
        int hits = 0;
        for (int i = 0; i < 6000 && sim.state() == PaddleBallSim.State.RUNNING; i++) {
            // Aim off-centre so returns are angled; a dead-centre hit goes straight back and the AI never misses a flat ball.
            float centre = sim.playerY() + PaddleBallSim.PADDLE_HEIGHT / 2.0F;
            float target = sim.velocityX() < 0 ? sim.ballY() + 11 : PaddleBallSim.BOARD_HEIGHT / 2.0F;
            boolean up = target < centre - 2;
            boolean down = target > centre + 2;
            sim.tick(up, down);
            PaddleBallSim.Event event;
            while ((event = sim.pollEvent()) != null) {
                if (event == PaddleBallSim.Event.HIT) hits++;
            }
        }
        assertTrue("a tracking player returns many balls (" + hits + ")", hits > 50);
        assertTrue("and scores some points", sim.playerScore() >= 1);
        assertTrue("the match is decided", sim.state() != PaddleBallSim.State.RUNNING);
    }
}
