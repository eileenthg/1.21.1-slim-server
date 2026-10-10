package net.reimaden.touhouorigins.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.client.IconRenderer;
import dev.overgrown.origins.client.screen.ChooseOriginScreen;
import dev.overgrown.origins.client.screen.OriginDisplayScreen;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.reimaden.touhouorigins.client.OriginGrid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the arrow-at-a-time origin chooser with a paged grid, alongside the existing detail
 * panel. Ported from UltrusBot's AltOriginGui (MIT); see {@link OriginGrid}.
 *
 * <p>Overgrown's {@code ChooseOriginScreen} happens to carry the same field names apace100's did,
 * so the shadowed state maps across unchanged. The accessors did not: {@code getImpact().name()}
 * becomes {@code impact().level()}, {@code getDisplayItem()} becomes an {@link IconRenderer} call
 * on {@code icon()}, and the layer's translation key becomes {@code layer.name()}.
 */
@Mixin(ChooseOriginScreen.class)
public abstract class ChooseOriginGridMixin extends OriginDisplayScreen implements OriginGrid {

    @Shadow @Final private List<Origin> originSelection;
    @Shadow @Final private ArrayList<OriginLayer> layerList;
    @Shadow @Final private int currentLayerIndex;
    @Shadow private int currentOrigin;
    @Shadow private int maxSelection;
    @Shadow private Origin randomOrigin;

    @Unique private int touhouorigins$panelLeft;
    @Unique private int touhouorigins$panelTop;
    @Unique private int touhouorigins$page;
    @Unique private int touhouorigins$pages;
    @Unique private float touhouorigins$time;

    private ChooseOriginGridMixin(Component title, boolean showDirtBackground) {
        super(title, showDirtBackground);
    }

    @Unique
    private int touhouorigins$slotX(int column) {
        return touhouorigins$panelLeft + 12 + column * (ICON + 2);
    }

    @Unique
    private int touhouorigins$slotY(int row) {
        return touhouorigins$panelTop + 10 + row * (ICON + 4);
    }

    /**
     * Suppress the two arrow buttons, which the grid replaces.
     *
     * <p>Guarded on {@code maxSelection}, not just ordinal. Overgrown only adds the arrows
     * {@code if (maxSelection > 1)}, so with a single choice ordinal 0 is the <i>Select</i> button
     * — blindly suppressing it would leave a one-origin layer unselectable. AltOriginGui suppresses
     * by ordinal alone, which is safe against apace100's unconditional arrows but not here.
     */
    @WrapOperation(
        method = "init",
        at = @At(value = "INVOKE",
            target = "Ldev/overgrown/origins/client/screen/ChooseOriginScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
            ordinal = 0))
    private GuiEventListener touhouorigins$dropLeftArrow(ChooseOriginScreen screen, GuiEventListener widget,
                                                        Operation<GuiEventListener> original) {
        return maxSelection > 1 ? widget : original.call(screen, widget);
    }

    @WrapOperation(
        method = "init",
        at = @At(value = "INVOKE",
            target = "Ldev/overgrown/origins/client/screen/ChooseOriginScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
            ordinal = 1))
    private GuiEventListener touhouorigins$dropRightArrow(ChooseOriginScreen screen, GuiEventListener widget,
                                                          Operation<GuiEventListener> original) {
        return maxSelection > 1 ? widget : original.call(screen, widget);
    }

    /**
     * Shift the detail window right to make room for the grid, then lay one invisible button over
     * each slot. Injected after {@code super.init()} has set guiLeft/guiTop, so those values are
     * overwritten rather than raced.
     */
    @Inject(method = "init", at = @At(value = "INVOKE",
        target = "Ldev/overgrown/origins/client/screen/OriginDisplayScreen;init()V", shift = At.Shift.AFTER))
    private void touhouorigins$layOutGrid(CallbackInfo ci) {
        this.touhouorigins$panelTop = (this.height - PANEL_HEIGHT) / 2;
        this.touhouorigins$panelLeft = (this.width - (PANEL_WIDTH + 10 + windowWidth)) / 2;
        this.guiTop = (this.height - windowHeight) / 2;
        this.guiLeft = touhouorigins$panelLeft + PANEL_WIDTH + 10;
        this.touhouorigins$pages = Math.max(1, (int) Math.ceil(maxSelection / (float) PER_PAGE));
        this.touhouorigins$page = Math.min(touhouorigins$page, touhouorigins$pages - 1);

        for (int slot = 0; slot < Math.min(maxSelection, PER_PAGE); slot++) {
            int x = touhouorigins$slotX(slot % COLUMNS);
            int y = touhouorigins$slotY(slot / COLUMNS);
            int index = slot;
            addRenderableWidget(Button.builder(Component.empty(), b -> {
                int chosen = index + touhouorigins$page * PER_PAGE;
                if (chosen >= maxSelection) return;
                currentOrigin = chosen;
                Origin origin = touhouorigins$currentOrigin();
                showOrigin(origin, layerList.get(currentLayerIndex), origin == randomOrigin);
            }).bounds(x, y, ICON, ICON).build());
        }

        if (maxSelection > PER_PAGE) {
            addRenderableWidget(Button.builder(Component.literal("<"), b ->
                touhouorigins$page = (touhouorigins$page - 1 + touhouorigins$pages) % touhouorigins$pages
            ).bounds(touhouorigins$panelLeft, guiTop + windowHeight + 5, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b ->
                touhouorigins$page = (touhouorigins$page + 1) % touhouorigins$pages
            ).bounds(touhouorigins$panelLeft + PANEL_WIDTH - 20, guiTop + windowHeight + 5, 20, 20).build());
        }
    }

    /**
     * Mirrors Overgrown's own private {@code getCurrentOriginInternal}. Reimplemented rather than
     * invoked because that method lazily builds the random origin, and calling it through an
     * accessor would duplicate that construction; the arrow path has already built it by the time
     * a grid click can select the random slot.
     */
    @Unique
    private Origin touhouorigins$currentOrigin() {
        return currentOrigin == originSelection.size() ? randomOrigin : originSelection.get(currentOrigin);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void touhouorigins$renderGrid(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        graphics.blit(SPRITES, touhouorigins$panelLeft, touhouorigins$panelTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        int first = touhouorigins$page * PER_PAGE;
        int last = Math.min(first + PER_PAGE, maxSelection);
        for (int i = first; i < last; i++) {
            int slot = i - first;
            int x = touhouorigins$slotX(slot % COLUMNS);
            int y = touhouorigins$slotY(slot / COLUMNS);
            if (i >= originSelection.size()) {
                renderRandomIcon(graphics, mouseX, mouseY, x, y, randomOriginSelected());
            } else {
                Origin origin = originSelection.get(i);
                renderOriginIcon(graphics, mouseX, mouseY, x, y, originSelected(i),
                    origin.impact().level(), origin.name());
                IconRenderer.render(graphics, origin.icon(), x + 5, y + 5);
            }
        }

        if (touhouorigins$pages > 1) {
            graphics.drawCenteredString(this.font, (touhouorigins$page + 1) + "/" + touhouorigins$pages,
                touhouorigins$panelLeft + PANEL_WIDTH / 2,
                guiTop + windowHeight + 5 + this.font.lineHeight / 2, 0xFFFFFF);
        }
        touhouorigins$time += partialTick;
    }

    @Override
    public Font gridFont() {
        return this.font;
    }

    @Override
    public Component currentLayerName() {
        OriginLayer layer = getCurrentLayer();
        return layer == null ? Component.empty() : layer.name();
    }

    @Override
    public boolean iconHighlighted(int mouseX, int mouseY, int x, int y) {
        return mouseOverIcon(mouseX, mouseY, x, y)
            || (getFocused() instanceof Button button && button.getX() == x && button.getY() == y);
    }

    @Override
    public boolean originSelected(int index) {
        return index == currentOrigin;
    }

    @Override
    public boolean randomOriginSelected() {
        return randomOrigin != null && getCurrentOrigin() == randomOrigin;
    }

    @Override
    public float gridTime() {
        return touhouorigins$time;
    }
}
