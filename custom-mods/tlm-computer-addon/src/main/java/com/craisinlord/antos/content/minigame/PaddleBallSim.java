package com.craisinlord.antos.content.minigame;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Single-player Paddle Ball simulation, stepped at 20 ticks per second.
 * <p>
 * Board size, paddle size and speed, ball speed, scoring rules and the AI difficulty table are ported from
 * MrCrayfish's Furniture Mod: Refurbished (MIT License, Copyright (c) 2024 MrCrayfish). The collision code is
 * a rewrite that sweeps the ball against each paddle face instead of intersecting line segments.
 */
public final class PaddleBallSim {
    public static final int BOARD_WIDTH = 200;
    public static final int BOARD_HEIGHT = 100;
    public static final int PADDLE_WIDTH = 4;
    public static final int PADDLE_HEIGHT = 28;
    public static final int BALL_SIZE = 4;
    public static final float PADDLE_SPEED = 4.0F;
    public static final float BALL_SPEED_X = 8.0F;
    public static final int START_COOLDOWN = 20;
    public static final int RESET_COOLDOWN = 40;
    public static final int POINTS_TO_WIN = 7;
    public static final int PLAYER_X = PADDLE_WIDTH;
    public static final int AI_X = BOARD_WIDTH - PADDLE_WIDTH * 2;
    private static final float PADDLE_MARGIN = 3.0F;
    private static final float HALF_BALL = BALL_SIZE / 2.0F;

    public enum Level {
        EASY(3.5F, 4.0F, 5.0F),
        MEDIUM(4.0F, 8.0F, 6.0F),
        HARD(5.0F, 15.0F, 8.0F);

        private final float aiSpeed;
        private final float aiTolerance;
        private final float ballYSpeed;

        Level(float aiSpeed, float aiTolerance, float ballYSpeed) {
            this.aiSpeed = aiSpeed;
            this.aiTolerance = aiTolerance;
            this.ballYSpeed = ballYSpeed;
        }
    }

    public enum Event { HIT, PLAYER_POINT, AI_POINT, WIN, LOSE }

    public enum State { RUNNING, WON, LOST }

    private final Random random;
    private final Level level;
    private final Deque<Event> events = new ArrayDeque<>();
    private float playerY, aiY, ballX, ballY, velX, velY;
    private float lastPlayerY, lastAiY, lastBallX, lastBallY;
    private int playerScore, aiScore;
    private int cooldown = START_COOLDOWN;
    private boolean frozen = true;
    private State state = State.RUNNING;

    public PaddleBallSim(Level level, Random random) {
        this.level = level;
        this.random = random;
        resetBoard(false);
        snapshot();
    }

    public State state() { return state; }
    public int playerScore() { return playerScore; }
    public int aiScore() { return aiScore; }
    public float playerY() { return playerY; }
    public float aiY() { return aiY; }
    public float ballX() { return ballX; }
    public float ballY() { return ballY; }
    public float velocityX() { return velX; }
    public float velocityY() { return velY; }
    public float lastPlayerY() { return lastPlayerY; }
    public float lastAiY() { return lastAiY; }
    public float lastBallX() { return lastBallX; }
    public float lastBallY() { return lastBallY; }
    public boolean isFrozen() { return frozen; }

    /** Next pending sound/score event, or null. */
    public Event pollEvent() {
        return events.poll();
    }

    public void tick(boolean up, boolean down) {
        snapshot();
        if (state != State.RUNNING) return;
        if (cooldown > 0) {
            if (--cooldown == 0) resetBoard(true);
            return;
        }
        moveBall();
        movePlayer(up, down);
        moveAi();
        testForScore();
    }

    private void snapshot() {
        lastPlayerY = playerY;
        lastAiY = aiY;
        lastBallX = ballX;
        lastBallY = ballY;
    }

    private void resetBoard(boolean launch) {
        playerY = aiY = (BOARD_HEIGHT - PADDLE_HEIGHT) / 2.0F;
        ballX = BOARD_WIDTH / 2.0F;
        ballY = BOARD_HEIGHT / 2.0F;
        velX = velY = 0;
        frozen = !launch;
        if (launch) {
            velX = serveDirection * BALL_SPEED_X;
            velY = level.ballYSpeed * (random.nextFloat() * 2.0F - 1.0F);
        }
    }

    private int serveDirection = 1;

    private void moveBall() {
        float previousX = ballX, previousY = ballY;
        float nextX = previousX + velX, nextY = previousY + velY;

        if (velX < 0) {
            float face = PLAYER_X + PADDLE_WIDTH;
            float previousEdge = previousX - HALF_BALL, nextEdge = nextX - HALF_BALL;
            if (previousEdge >= face && nextEdge < face) {
                float t = (previousEdge - face) / (previousEdge - nextEdge);
                float hitY = previousY + (nextY - previousY) * t;
                if (overlapsPaddle(hitY, playerY)) {
                    float overshoot = face - nextEdge;
                    velX = -velX;
                    velY = reflectedYSpeed(hitY, playerY);
                    nextX = face + overshoot + HALF_BALL;
                    nextY = hitY + velY * (overshoot / Math.abs(velX));
                    events.add(Event.HIT);
                }
            }
        } else if (velX > 0) {
            float face = AI_X;
            float previousEdge = previousX + HALF_BALL, nextEdge = nextX + HALF_BALL;
            if (previousEdge <= face && nextEdge > face) {
                float t = (face - previousEdge) / (nextEdge - previousEdge);
                float hitY = previousY + (nextY - previousY) * t;
                if (overlapsPaddle(hitY, aiY)) {
                    float overshoot = nextEdge - face;
                    velX = -velX;
                    velY = reflectedYSpeed(hitY, aiY);
                    nextX = face - overshoot - HALF_BALL;
                    nextY = hitY + velY * (overshoot / Math.abs(velX));
                    events.add(Event.HIT);
                }
            }
        }

        if (nextY - HALF_BALL < 0) {
            nextY = 2 * HALF_BALL - nextY;
            velY = -velY;
            events.add(Event.HIT);
        } else if (nextY + HALF_BALL > BOARD_HEIGHT) {
            nextY = 2 * (BOARD_HEIGHT - HALF_BALL) - nextY;
            velY = -velY;
            events.add(Event.HIT);
        }
        ballX = nextX;
        ballY = nextY;
    }

    private static boolean overlapsPaddle(float ballCentreY, float paddleY) {
        return ballCentreY + HALF_BALL >= paddleY && ballCentreY - HALF_BALL <= paddleY + PADDLE_HEIGHT;
    }

    /** Hitting the top of a paddle sends the ball up, the bottom sends it down. */
    private float reflectedYSpeed(float ballCentreY, float paddleY) {
        float hit = Math.max(0.0F, Math.min(1.0F, (ballCentreY - paddleY) / PADDLE_HEIGHT));
        return level.ballYSpeed * (hit - 0.5F) * 2.0F;
    }

    private void movePlayer(boolean up, boolean down) {
        float delta = up && !down ? -PADDLE_SPEED : !up && down ? PADDLE_SPEED : 0;
        playerY = clampPaddle(playerY + delta);
    }

    private void moveAi() {
        if (velX <= 0) return;
        float centre = aiY + PADDLE_HEIGHT / 2.0F;
        if (ballY < centre - level.aiTolerance) {
            aiY -= Math.min(level.aiSpeed, centre - ballY);
        } else if (ballY > centre + level.aiTolerance) {
            aiY += Math.min(level.aiSpeed, ballY - centre);
        }
        aiY = clampPaddle(aiY);
    }

    private static float clampPaddle(float y) {
        return Math.max(PADDLE_MARGIN, Math.min(BOARD_HEIGHT - PADDLE_HEIGHT - PADDLE_MARGIN, y));
    }

    private void testForScore() {
        if (ballX - HALF_BALL <= 0) {
            aiScore++;
            events.add(Event.AI_POINT);
            serveDirection = -1;
            finishRound(aiScore, State.LOST, Event.LOSE);
        } else if (ballX + HALF_BALL >= BOARD_WIDTH) {
            playerScore++;
            events.add(Event.PLAYER_POINT);
            serveDirection = 1;
            finishRound(playerScore, State.WON, Event.WIN);
        }
    }

    private void finishRound(int scorerPoints, State endState, Event endEvent) {
        if (scorerPoints >= POINTS_TO_WIN) {
            state = endState;
            events.add(endEvent);
        }
        frozen = true;
        velX = velY = 0;
        cooldown = RESET_COOLDOWN;
    }
}
