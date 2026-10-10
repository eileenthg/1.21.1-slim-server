package com.craisinlord.antos.content.client.game.mini;

import com.craisinlord.antos.content.minigame.PaddleBallSim;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/** Single-player Paddle Ball against the computer. Move with the up/down arrow keys or W/S. */
final class PaddleBallSession extends MiniGameSession {
    private static final int BOARD_X = 7;
    private static final int BOARD_Y = 36;
    private static final int PAPER = 0xFF0A140A;

    private final Random random = new Random();
    private PaddleBallSim.Level level = PaddleBallSim.Level.MEDIUM;
    private PaddleBallSim sim;
    private int flashTicks;
    private boolean flashOnPlayerSide;

    @Override
    public void tick() {
        if (sim == null) return;
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean up = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_UP) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_W);
        boolean down = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DOWN) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_S);
        sim.tick(up, down);
        if (flashTicks > 0) flashTicks--;
        PaddleBallSim.Event event;
        while ((event = sim.pollEvent()) != null) {
            switch (event) {
                case HIT -> play(SoundEvents.NOTE_BLOCK_BIT.value(), 1.0F);
                case PLAYER_POINT -> {
                    play(SoundEvents.NOTE_BLOCK_PLING.value(), 1.5F);
                    flash(false);
                }
                case AI_POINT -> {
                    play(SoundEvents.NOTE_BLOCK_PLING.value(), 0.6F);
                    flash(true);
                }
                case WIN -> play(SoundEvents.PLAYER_LEVELUP, 1.0F);
                case LOSE -> play(SoundEvents.NOTE_BLOCK_BASS.value(), 0.5F);
            }
        }
    }

    private void flash(boolean playerSide) {
        flashTicks = 20;
        flashOnPlayerSide = playerSide;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (sim != null && sim.state() != PaddleBallSim.State.RUNNING
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE)) {
            start();
            return true;
        }
        return false;
    }

    private void start() {
        sim = new PaddleBallSim(level, random);
        flashTicks = 0;
    }

    @Override
    protected void draw(GuiGraphics g) {
        if (sim == null) {
            drawMenu(g);
            return;
        }
        drawBoard(g);
    }

    private void drawMenu(GuiGraphics g) {
        centered(g, "PADDLE BALL", WIDTH / 2, 8, GREEN);
        centered(g, "FIRST TO " + PaddleBallSim.POINTS_TO_WIN + " POINTS WINS", WIDTH / 2, 24, PALE_GREEN);
        centered(g, "MOVE WITH UP / DOWN OR W / S", WIDTH / 2, 36, DIM_GREEN);
        centered(g, "CHOOSE YOUR OPPONENT", WIDTH / 2, 62, PALE_GREEN);
        PaddleBallSim.Level[] levels = PaddleBallSim.Level.values();
        for (int i = 0; i < levels.length; i++) {
            PaddleBallSim.Level choice = levels[i];
            button(g, choice.name(), 12 + i * 66, 78, 62, 20, () -> {
                level = choice;
                start();
            });
        }
    }

    private void drawBoard(GuiGraphics g) {
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        g.fill(BOARD_X - 1, BOARD_Y - 1, BOARD_X + PaddleBallSim.BOARD_WIDTH + 1, BOARD_Y + PaddleBallSim.BOARD_HEIGHT + 1, DIM_GREEN);
        g.fill(BOARD_X, BOARD_Y, BOARD_X + PaddleBallSim.BOARD_WIDTH, BOARD_Y + PaddleBallSim.BOARD_HEIGHT, PAPER);
        for (int y = 2; y < PaddleBallSim.BOARD_HEIGHT; y += 8) {
            g.fill(BOARD_X + PaddleBallSim.BOARD_WIDTH / 2 - 1, BOARD_Y + y, BOARD_X + PaddleBallSim.BOARD_WIDTH / 2 + 1, BOARD_Y + y + 4, 0xFF1D3A1D);
        }
        if (flashTicks > 0 && (flashTicks / 5) % 2 == 0) {
            int x = flashOnPlayerSide ? BOARD_X : BOARD_X + PaddleBallSim.BOARD_WIDTH - PaddleBallSim.PADDLE_WIDTH;
            g.fill(x, BOARD_Y, x + PaddleBallSim.PADDLE_WIDTH, BOARD_Y + PaddleBallSim.BOARD_HEIGHT, 0xFF653938);
        }

        text(g, "YOU", BOARD_X, 6, PALE_GREEN);
        String opponent = level.name() + " AI";
        text(g, opponent, BOARD_X + PaddleBallSim.BOARD_WIDTH - font.width(opponent), 6, PALE_GREEN);
        String score = sim.playerScore() + " - " + sim.aiScore();
        g.pose().pushPose();
        g.pose().translate(WIDTH / 2.0F, 14, 0);
        g.pose().scale(2, 2, 1);
        g.drawString(font, score, -font.width(score) / 2, 0, GREEN, false);
        g.pose().popPose();

        int playerY = Math.round(lerp(sim.lastPlayerY(), sim.playerY(), partial));
        int aiY = Math.round(lerp(sim.lastAiY(), sim.aiY(), partial));
        g.fill(BOARD_X + PaddleBallSim.PLAYER_X, BOARD_Y + playerY, BOARD_X + PaddleBallSim.PLAYER_X + PaddleBallSim.PADDLE_WIDTH, BOARD_Y + playerY + PaddleBallSim.PADDLE_HEIGHT, PALE_GREEN);
        g.fill(BOARD_X + PaddleBallSim.AI_X, BOARD_Y + aiY, BOARD_X + PaddleBallSim.AI_X + PaddleBallSim.PADDLE_WIDTH, BOARD_Y + aiY + PaddleBallSim.PADDLE_HEIGHT, AMBER);
        if (sim.state() == PaddleBallSim.State.RUNNING) {
            int ballX = Math.round(lerp(sim.lastBallX(), sim.ballX(), partial));
            int ballY = Math.round(lerp(sim.lastBallY(), sim.ballY(), partial));
            int half = PaddleBallSim.BALL_SIZE / 2;
            g.fill(BOARD_X + ballX - half, BOARD_Y + ballY - half, BOARD_X + ballX + half, BOARD_Y + ballY + half, GREEN);
        }

        if (sim.state() != PaddleBallSim.State.RUNNING) {
            boolean won = sim.state() == PaddleBallSim.State.WON;
            centered(g, won ? "YOU WIN!" : "YOU LOSE", WIDTH / 2, BOARD_Y + 20, won ? GREEN : RED);
            centered(g, "PRESS ENTER TO PLAY AGAIN", WIDTH / 2, BOARD_Y + 34, PALE_GREEN);
        }
        int bottom = BOARD_Y + PaddleBallSim.BOARD_HEIGHT + 6;
        button(g, "MENU", BOARD_X, bottom, 44, 12, () -> sim = null);
        button(g, "RESTART", BOARD_X + 50, bottom, 56, 12, this::start);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
