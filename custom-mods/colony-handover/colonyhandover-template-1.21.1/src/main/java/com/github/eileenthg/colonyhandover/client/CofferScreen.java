package com.github.eileenthg.colonyhandover.client;

import com.github.eileenthg.colonyhandover.coffer.CofferMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The coffer screen: a six-row chest with two page buttons.
 *
 * <h2>Borrowed art</h2>
 *
 * The background is vanilla's own {@code generic_54.png}, which is why the menu lays
 * its slots out at double-chest offsets. A bespoke texture would look no different
 * to anyone using it and would be one more file to keep in step with resource pack
 * changes - and this screen is meant to be unremarkable. It is a place to collect
 * things from once, not somewhere to admire.
 *
 * <h2>Buttons, not a custom packet</h2>
 *
 * Paging goes through {@code clickMenuButton}, so pressing a button here is the same
 * wire message the enchanting table sends. The server changes page and rebroadcasts
 * every slot; this class only has to ask.
 */
public final class CofferScreen extends AbstractContainerScreen<CofferMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private Button previous;
    private Button next;

    public CofferScreen(CofferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(Component.literal("<"),
                        b -> press(CofferMenu.BUTTON_PREV))
                .bounds(leftPos - 24, topPos + 8, 20, 20)
                .build());
        next = addRenderableWidget(Button.builder(Component.literal(">"),
                        b -> press(CofferMenu.BUTTON_NEXT))
                .bounds(leftPos + imageWidth + 4, topPos + 8, 20, 20)
                .build());
        refreshButtons();
    }

    private void press(int button) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        refreshButtons();
    }

    /** Greyed out at the ends, so the page count is legible without reading it. */
    private void refreshButtons() {
        if (previous == null || next == null) {
            return;
        }
        previous.active = menu.currentPage() > 0;
        next.active = menu.currentPage() < menu.totalPages() - 1;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // The page counter arrives as a synced data slot, so it can change without
        // anything on this side happening. Re-check rather than assume a button
        // press was the only cause.
        refreshButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, 125);
        graphics.blit(BACKGROUND, leftPos, topPos + 125, 0, 126, imageWidth, 96);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final Component pages = Component.literal(
                (menu.currentPage() + 1) + " / " + menu.totalPages());
        graphics.drawString(font, pages,
                imageWidth - 8 - font.width(pages), titleLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
