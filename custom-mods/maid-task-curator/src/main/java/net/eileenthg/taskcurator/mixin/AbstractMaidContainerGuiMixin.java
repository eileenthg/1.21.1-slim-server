package net.eileenthg.taskcurator.mixin;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import java.util.ArrayList;
import java.util.List;
import net.eileenthg.taskcurator.CuratorLayout;
import net.eileenthg.taskcurator.CuratorScreen;
import net.eileenthg.taskcurator.FolderButton;
import net.eileenthg.taskcurator.GearButton;
import net.eileenthg.taskcurator.TaskCurator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the flat task list with the curated one: hidden tasks are dropped and folders become
 * header rows that open and close. Task rows are still built by TLM's own drawPerTaskButton, so
 * tooltips, enable conditions and the click-to-switch packet are unchanged.
 */
@Mixin(value = AbstractMaidContainerGui.class, remap = false)
public abstract class AbstractMaidContainerGuiMixin<T extends AbstractMaidContainer> extends AbstractContainerScreen<T> {
    @Unique
    private static final int PER_PAGE = 12;

    @Shadow
    @Final
    protected EntityMaid maid;

    @Shadow
    @Final
    protected List<IMaidTask> notHiddenTasks;

    @Shadow
    private static int TASK_PAGE;

    @Shadow
    private static boolean TASK_LIST_OPEN;

    @Shadow
    private void drawPerTaskButton(List<IMaidTask> tasks, int count, int index) {
        throw new AssertionError();
    }

    @Unique
    private int taskcurator$rowCount;

    /** Slots on the current page that hold a task inside an open folder (drawn with a side stripe). */
    @Unique
    private final List<Integer> taskcurator$memberSlots = new ArrayList<>();

    private AbstractMaidContainerGuiMixin(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "addTaskListButton", at = @At("HEAD"), cancellable = true)
    private void taskcurator$addTaskList(CallbackInfo ci) {
        ci.cancel();
        List<CuratorLayout.Row> rows = TaskCurator.layout().buildRows(this.notHiddenTasks, this.maid.getTask());
        this.taskcurator$rowCount = rows.size();
        this.taskcurator$memberSlots.clear();
        if (TASK_PAGE * PER_PAGE >= rows.size()) {
            TASK_PAGE = Math.max(0, (rows.size() - 1) / PER_PAGE);
        }
        for (int slot = 0; slot < PER_PAGE; slot++) {
            int index = TASK_PAGE * PER_PAGE + slot;
            if (index >= rows.size()) {
                break;
            }
            CuratorLayout.Row row = rows.get(index);
            if (row instanceof CuratorLayout.TaskRow taskRow) {
                this.drawPerTaskButton(List.of(taskRow.task()), slot, 0);
                if (taskRow.inFolder()) {
                    this.taskcurator$memberSlots.add(slot);
                }
            } else if (row instanceof CuratorLayout.FolderRow folderRow) {
                FolderButton button = new FolderButton(this.leftPos - 89, this.topPos + 23 + 19 * slot, folderRow, b -> {
                    folderRow.folder().open = !folderRow.folder().open;
                    TaskCurator.save();
                    this.init();
                });
                button.visible = TASK_LIST_OPEN;
                this.addRenderableWidget(button);
            }
        }
    }

    @Inject(method = "taskPageDown", at = @At("HEAD"), cancellable = true)
    private void taskcurator$pageDown(CallbackInfo ci) {
        ci.cancel();
        if ((TASK_PAGE + 1) * PER_PAGE < this.taskcurator$rowCount) {
            TASK_PAGE++;
            this.init();
        }
    }

    @Inject(method = "drawTaskPageCount", at = @At("HEAD"), cancellable = true)
    private void taskcurator$pageCount(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();
        if (TASK_LIST_OPEN) {
            int pages = Math.max(1, (this.taskcurator$rowCount - 1) / PER_PAGE + 1);
            String text = String.format("%d/%d", TASK_PAGE + 1, pages);
            // Right-aligned so it ends before the gear button.
            graphics.drawString(this.font, text, -35 - this.font.width(text), 12, 0x333333, false);
        }
    }

    @Inject(method = "addTaskControlButton", at = @At("TAIL"))
    private void taskcurator$addGear(CallbackInfo ci) {
        GearButton gear = new GearButton(this.leftPos - 33, this.topPos + 10, b -> this.minecraft.setScreen(new CuratorScreen(this)));
        gear.visible = TASK_LIST_OPEN;
        this.addRenderableWidget(gear);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void taskcurator$folderStripes(GuiGraphics graphics, float partialTicks, int x, int y, CallbackInfo ci) {
        if (!TASK_LIST_OPEN) {
            return;
        }
        for (int slot : this.taskcurator$memberSlots) {
            int top = this.topPos + 23 + 19 * slot;
            graphics.fill(this.leftPos - 92, top + 1, this.leftPos - 90, top + 18, 0xFFC08A3E);
        }
    }
}
