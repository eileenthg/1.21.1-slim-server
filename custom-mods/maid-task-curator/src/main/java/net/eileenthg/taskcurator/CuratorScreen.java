package net.eileenthg.taskcurator;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Editor for the task menu: show/hide tasks, create/rename/delete folders, move and reorder. */
public final class CuratorScreen extends Screen {
    private static final int ROW_WIDTH = 400;

    private final Screen parent;
    private EntryList list;

    public CuratorScreen(Screen parent) {
        super(Component.translatable("taskcurator.screen.title"));
        this.parent = parent;
    }

    private static CuratorLayout layout() {
        return TaskCurator.layout();
    }

    private static Map<ResourceLocation, IMaidTask> tasks() {
        return TaskManager.getTaskMap();
    }

    /**
     * A task the editor lists: registered, and not one TLM itself keeps out of this maid's menu
     * (Lost Maids registers tasks only its hostile maids use).
     */
    private IMaidTask listed(String id) {
        IMaidTask t = tasks().get(ResourceLocation.tryParse(id));
        if (t == null) {
            return null;
        }
        EntityMaid maid = this.parent instanceof AbstractMaidContainerGui<?> gui ? gui.getMaid() : null;
        return maid != null && t.isHidden(maid) ? null : t;
    }

    private boolean known(Object o) {
        return o instanceof String id && listed(id) != null;
    }

    /** Folders always count as visible neighbours; task IDs only when the editor lists them. */
    private final Predicate<Object> TOP_VISIBLE = o -> o instanceof CuratorLayout.Folder || known(o);

    @Override
    protected void init() {
        double scroll = this.list == null ? 0 : this.list.getScrollAmount();
        this.list = new EntryList(this.minecraft, this.width, this.height - 36 - 34, 34, 24);
        this.addRenderableWidget(this.list);
        this.fillEntries();
        this.list.setScrollAmount(scroll);

        int y = this.height - 28;
        int x = this.width / 2 - 155;
        this.addRenderableWidget(Button.builder(Component.translatable("taskcurator.screen.new_folder"), b -> {
            layout().top.add(new CuratorLayout.Folder(Component.translatable("taskcurator.screen.new_folder_name").getString()));
            this.changed();
            this.list.setScrollAmount(this.list.getMaxScroll());
        }).bounds(x, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("taskcurator.screen.reset"), b ->
                this.minecraft.setScreen(new ConfirmScreen(yes -> {
                    if (yes) {
                        TaskCurator.resetToDefaults();
                        this.list = null;
                    }
                    this.minecraft.setScreen(this);
                }, Component.translatable("taskcurator.screen.reset"), Component.translatable("taskcurator.screen.reset.confirm")))
        ).bounds(x + 105, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(x + 210, y, 100, 20).build());
    }

    private void fillEntries() {
        this.list.clear();
        for (Object o : layout().top) {
            if (o instanceof CuratorLayout.Folder f) {
                this.list.add(new FolderEntry(f));
                for (String id : f.tasks) {
                    IMaidTask t = this.listed(id);
                    if (t != null) {
                        this.list.add(new TaskEntry(id, t, f));
                    }
                }
            } else if (o instanceof String id) {
                IMaidTask t = this.listed(id);
                if (t != null) {
                    this.list.add(new TaskEntry(id, t, null));
                }
            }
        }
    }

    /** Saves and rebuilds the rows after a structural edit, keeping the scroll position. */
    private void changed() {
        TaskCurator.save();
        double scroll = this.list.getScrollAmount();
        this.fillEntries();
        this.list.setScrollAmount(scroll);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("taskcurator.screen.hint"), this.width / 2, 20, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        for (CuratorLayout.Folder f : layout().folders()) {
            if (f.name.isBlank()) {
                f.name = Component.translatable("taskcurator.screen.new_folder_name").getString();
            }
        }
        TaskCurator.save();
        Minecraft mc = this.minecraft;
        if (this.parent instanceof AbstractMaidContainerGui<?> gui && mc.player != null && mc.player.containerMenu == gui.getMenu()) {
            // Returning to the maid screen re-runs its init, so the new layout shows at once.
            if (gui.getMaid() != null) {
                gui.getMaid().guiOpening = true;
            }
            mc.setScreen(this.parent);
        } else {
            mc.setScreen(null);
        }
    }

    // ---- list ----

    private final class EntryList extends ContainerObjectSelectionList<Row> {
        EntryList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        void clear() {
            this.clearEntries();
        }

        void add(Row row) {
            this.addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return Math.min(ROW_WIDTH, CuratorScreen.this.width - 24);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() + 6;
        }
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
        protected final List<GuiEventListener> children = new ArrayList<>();

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            List<NarratableEntry> out = new ArrayList<>();
            for (GuiEventListener c : this.children) {
                if (c instanceof NarratableEntry n) {
                    out.add(n);
                }
            }
            return out;
        }

        protected static Button small(String glyph, String tooltipKey, Button.OnPress onPress) {
            Button b = Button.builder(Component.literal(glyph), onPress).size(16, 20).build();
            b.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
            return b;
        }
    }

    private final class FolderEntry extends Row {
        private final CuratorLayout.Folder folder;
        private final EditBox name;
        private final Button delete;
        private final Button up;
        private final Button down;

        FolderEntry(CuratorLayout.Folder folder) {
            this.folder = folder;
            this.name = new EditBox(CuratorScreen.this.font, 0, 0, 160, 18, Component.translatable("taskcurator.screen.folder_name"));
            this.name.setMaxLength(40);
            this.name.setValue(folder.name);
            this.name.setResponder(s -> folder.name = s);
            this.delete = Button.builder(Component.translatable("taskcurator.screen.delete_folder"), b -> {
                layout().deleteFolder(folder);
                CuratorScreen.this.changed();
            }).size(60, 20).build();
            this.delete.setTooltip(Tooltip.create(Component.translatable("taskcurator.screen.delete_folder.tip")));
            this.up = small("▲", "taskcurator.screen.up", b -> this.move(-1));
            this.down = small("▼", "taskcurator.screen.down", b -> this.move(1));
            this.children.addAll(List.of(this.name, this.delete, this.up, this.down));
        }

        private void move(int dir) {
            if (CuratorLayout.shift(layout().top, this.folder, dir, TOP_VISIBLE)) {
                CuratorScreen.this.changed();
            }
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int right = left + width;
            g.fill(left - 2, top, right + 2, top + height - 2, 0x40C08A3E);
            g.drawString(CuratorScreen.this.font, "■", left + 3, top + 6, 0xC08A3E, false);
            this.name.setPosition(left + 14, top + 1);
            this.name.render(g, mouseX, mouseY, partialTick);
            long count = this.folder.tasks.stream().filter(CuratorScreen.this::known).count();
            g.drawString(CuratorScreen.this.font, Component.translatable("taskcurator.screen.task_count", count),
                    left + 180, top + 6, 0xA0A0A0, false);
            this.down.setPosition(right - 16, top);
            this.up.setPosition(right - 34, top);
            this.delete.setPosition(right - 98, top);
            this.down.render(g, mouseX, mouseY, partialTick);
            this.up.render(g, mouseX, mouseY, partialTick);
            this.delete.render(g, mouseX, mouseY, partialTick);
        }
    }

    private final class TaskEntry extends Row {
        private final String id;
        private final IMaidTask task;
        private final CuratorLayout.Folder folder;
        private final Button visibility;
        private final Button folderCycle;
        private final Button up;
        private final Button down;

        TaskEntry(String id, IMaidTask task, CuratorLayout.Folder folder) {
            this.id = id;
            this.task = task;
            this.folder = folder;
            boolean hidden = layout().hidden.contains(id);
            this.visibility = Button.builder(
                    hidden ? Component.translatable("taskcurator.screen.hidden").withStyle(ChatFormatting.GRAY)
                            : Component.translatable("taskcurator.screen.shown").withStyle(ChatFormatting.GREEN),
                    b -> {
                        if (!layout().hidden.remove(id)) {
                            layout().hidden.add(id);
                        }
                        CuratorScreen.this.changed();
                    }).size(56, 20).build();
            if (CuratorLayout.IDLE.equals(id)) {
                this.visibility.active = false;
                this.visibility.setTooltip(Tooltip.create(Component.translatable("taskcurator.screen.idle_locked")));
            }
            String where = folder == null ? Component.translatable("taskcurator.screen.no_folder").getString() : folder.name;
            this.folderCycle = Button.builder(
                    Component.literal(CuratorScreen.this.font.plainSubstrByWidth(
                            Component.translatable("taskcurator.screen.folder", where).getString(), 100)),
                    b -> this.cycleFolder(Screen.hasShiftDown() ? -1 : 1)).size(110, 20).build();
            this.folderCycle.setTooltip(Tooltip.create(Component.translatable("taskcurator.screen.folder.tip")));
            this.up = small("▲", "taskcurator.screen.up", b -> this.move(-1));
            this.down = small("▼", "taskcurator.screen.down", b -> this.move(1));
            this.children.addAll(List.of(this.visibility, this.folderCycle, this.up, this.down));
        }

        private void cycleFolder(int dir) {
            List<CuratorLayout.Folder> folders = layout().folders();
            // Position 0 is "no folder"; folders follow in their menu order.
            int current = this.folder == null ? 0 : folders.indexOf(this.folder) + 1;
            int size = folders.size() + 1;
            int next = Math.floorMod(current + dir, size);
            layout().moveToFolder(this.id, next == 0 ? null : folders.get(next - 1));
            CuratorScreen.this.changed();
        }

        private void move(int dir) {
            boolean moved = this.folder == null
                    ? CuratorLayout.shift(layout().top, (Object) this.id, dir, TOP_VISIBLE)
                    : CuratorLayout.shift(this.folder.tasks, this.id, dir, CuratorScreen.this::known);
            if (moved) {
                CuratorScreen.this.changed();
            }
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int right = left + width;
            int x = left;
            if (this.folder != null) {
                g.fill(left + 4, top, left + 6, top + height - 2, 0xFFC08A3E);
                x += 14;
            }
            boolean hidden = layout().hidden.contains(this.id);
            g.renderItem(this.task.getIcon(), x, top + 2);
            int nameRoom = right - 208 - (x + 20) - 4;
            String name = CuratorScreen.this.font.plainSubstrByWidth(this.task.getName().getString(), nameRoom);
            g.drawString(CuratorScreen.this.font, name, x + 20, top + 6, hidden ? 0x808080 : 0xFFFFFF, false);
            if (hovering && mouseX >= x && mouseX < x + 20 + nameRoom) {
                CuratorScreen.this.setTooltipForNextRenderPass(Component.literal(this.id).withStyle(ChatFormatting.DARK_GRAY));
            }
            this.down.setPosition(right - 16, top);
            this.up.setPosition(right - 34, top);
            this.folderCycle.setPosition(right - 148, top);
            this.visibility.setPosition(right - 208, top);
            this.visibility.render(g, mouseX, mouseY, partialTick);
            this.folderCycle.render(g, mouseX, mouseY, partialTick);
            this.up.render(g, mouseX, mouseY, partialTick);
            this.down.render(g, mouseX, mouseY, partialTick);
        }
    }
}
