package com.github.eileenthg.tamablearcadianfairy.client;

import com.github.eileenthg.tamablearcadianfairy.TamableArcadianFairy;
import com.github.eileenthg.tamablearcadianfairy.data.ActState;
import com.github.eileenthg.tamablearcadianfairy.network.UpdateFairySettingsPayload;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The settings panel an owner sees when they right-click their fairy empty handed.
 *
 * <p>Laid out like a vanilla container screen: a panel texture, a live model of the
 * fairy on the left so its type is obvious at a glance, and the controls on the right.
 */
public class FairyScreen extends Screen {

    private static final ResourceLocation PANEL =
            ResourceLocation.fromNamespaceAndPath(TamableArcadianFairy.MOD_ID, "textures/gui/fairy_screen.png");

    /** The panel occupies the top-left of a 256x256 texture. */
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 180;
    private static final int TEXTURE_SIZE = 256;

    private static final int PREVIEW_LEFT = 11;
    private static final int PREVIEW_TOP = 34;
    private static final int PREVIEW_RIGHT = 55;
    private static final int PREVIEW_BOTTOM = 148;
    private static final int PREVIEW_SCALE = 45;

    private static final int COLUMN_X = 63;
    private static final int COLUMN_WIDTH = 128;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_STEP = 24;
    private static final int FIRST_ROW = 34;

    private static final int TITLE_COLOUR = 0x404040;
    private static final int OWNER_COLOUR = 0x707070;

    private final LivingEntity fairy;
    private ActState state;

    private int leftPos;
    private int topPos;

    public FairyScreen(LivingEntity fairy) {
        super(fairy.getDisplayName());
        this.fairy = fairy;
        this.state = FairyTaming.actState(fairy);
    }

    @Override
    protected void init() {
        leftPos = (width - PANEL_WIDTH) / 2;
        topPos = (height - PANEL_HEIGHT) / 2;

        addRenderableWidget(Button.builder(attackModeLabel(), button -> {
            state = new ActState(state.attackMode().next(), state.followOwner(), state.wander(),
                    state.followRange(), state.wanderRange(), state.wanderCenter());
            button.setMessage(attackModeLabel());
            send();
        }).bounds(leftPos + COLUMN_X, topPos + FIRST_ROW, COLUMN_WIDTH, ROW_HEIGHT).build());

        addRenderableWidget(Button.builder(followLabel(), button -> {
            state = new ActState(state.attackMode(), !state.followOwner(), state.wander(),
                    state.followRange(), state.wanderRange(), state.wanderCenter());
            button.setMessage(followLabel());
            send();
        }).bounds(leftPos + COLUMN_X, topPos + FIRST_ROW + ROW_STEP, COLUMN_WIDTH, ROW_HEIGHT).build());

        addRenderableWidget(Button.builder(wanderLabel(), button -> {
            // Re-anchor on the fairy's current spot whenever wandering is switched back on.
            state = new ActState(state.attackMode(), state.followOwner(), !state.wander(),
                    state.followRange(), state.wanderRange(), fairy.position());
            button.setMessage(wanderLabel());
            send();
        }).bounds(leftPos + COLUMN_X, topPos + FIRST_ROW + 2 * ROW_STEP, COLUMN_WIDTH, ROW_HEIGHT).build());

        addRenderableWidget(new RangeSlider(leftPos + COLUMN_X, topPos + FIRST_ROW + 3 * ROW_STEP, true));
        addRenderableWidget(new RangeSlider(leftPos + COLUMN_X, topPos + FIRST_ROW + 4 * ROW_STEP, false));

        addRenderableWidget(Button.builder(Component.translatable("gui.tamablearcadianfairy.done"),
                        button -> onClose())
                .bounds(leftPos + 9, topPos + 156, PANEL_WIDTH - 18, ROW_HEIGHT).build());
    }

    /**
     * Vanilla blurs the world behind an open screen. This panel gets opened mid-fight to flip a
     * fairy between passive and active, so the view behind it stays sharp.
     */
    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Intentionally empty: keep the world readable while the panel is open.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.blit(PANEL, leftPos, topPos, 0.0f, 0.0f,
                PANEL_WIDTH, PANEL_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // The fairy itself, so the type reads at a glance rather than from a label.
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                leftPos + PREVIEW_LEFT, topPos + PREVIEW_TOP,
                leftPos + PREVIEW_RIGHT, topPos + PREVIEW_BOTTOM,
                PREVIEW_SCALE, 0.0f, mouseX, mouseY, fairy);

        int centre = leftPos + PANEL_WIDTH / 2;
        graphics.drawCenteredString(font, title, centre, topPos + 8, TITLE_COLOUR);
        graphics.drawCenteredString(font, Component.translatable("gui.tamablearcadianfairy.owner",
                FairyTaming.tameData(fairy).ownerName()), centre, topPos + 18, OWNER_COLOUR);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void send() {
        PacketDistributor.sendToServer(new UpdateFairySettingsPayload(fairy.getId(), state.sanitised()));
    }

    private Component attackModeLabel() {
        return Component.translatable("gui.tamablearcadianfairy.attack_mode", state.attackMode().displayName());
    }

    private Component followLabel() {
        return Component.translatable("gui.tamablearcadianfairy.follow", toggleLabel(state.followOwner()));
    }

    private Component wanderLabel() {
        return Component.translatable("gui.tamablearcadianfairy.wander", toggleLabel(state.wander()));
    }

    private static Component toggleLabel(boolean on) {
        return Component.translatable(on
                ? "state.tamablearcadianfairy.enabled"
                : "state.tamablearcadianfairy.disabled");
    }

    /** One slider class for both ranges; {@code follow} picks which value it edits. */
    private class RangeSlider extends AbstractSliderButton {

        private final boolean follow;

        RangeSlider(int x, int y, boolean follow) {
            super(x, y, COLUMN_WIDTH, ROW_HEIGHT, Component.empty(), 0.0);
            this.follow = follow;
            this.value = normalise(follow ? state.followRange() : state.wanderRange());
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(follow
                            ? "gui.tamablearcadianfairy.follow_range"
                            : "gui.tamablearcadianfairy.wander_range",
                    String.valueOf(current())));
        }

        @Override
        protected void applyValue() {
            int blocks = current();
            state = follow
                    ? new ActState(state.attackMode(), state.followOwner(), state.wander(),
                            blocks, state.wanderRange(), state.wanderCenter())
                    : new ActState(state.attackMode(), state.followOwner(), state.wander(),
                            state.followRange(), blocks, state.wanderCenter());
            send();
        }

        private int current() {
            int min = follow ? ActState.MIN_FOLLOW_RANGE : ActState.MIN_WANDER_RANGE;
            int max = follow ? ActState.MAX_FOLLOW_RANGE : ActState.MAX_WANDER_RANGE;
            return Mth.floor(Mth.lerp(value, min, max));
        }

        private double normalise(int blocks) {
            int min = follow ? ActState.MIN_FOLLOW_RANGE : ActState.MIN_WANDER_RANGE;
            int max = follow ? ActState.MAX_FOLLOW_RANGE : ActState.MAX_WANDER_RANGE;
            return Mth.clamp((double) (blocks - min) / (max - min), 0.0, 1.0);
        }
    }
}
