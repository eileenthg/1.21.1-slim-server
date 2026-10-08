package net.eileenthg.tabfolders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import sfiomn.legendarytabs.api.tabs_menu.TabBase;

/** Editor for the tab row: show/hide tabs, create/rename/re-icon/delete folders, move and reorder. */
public final class TabsScreen extends Screen {
    private static final int ROW_WIDTH = 400;
    /** Tabs placed by this session's reconciles, marked "new" in the list until the game restarts. */
    private static final Set<String> NEW_THIS_SESSION = new HashSet<>();

    private final Screen parent;
    private Map<String, TabBase> known;
    private EntryList list;

    public TabsScreen(Screen parent) {
        super(Component.translatable("tabfolders.screen.title"));
        this.parent = parent;
    }

    private static TabLayout layout() {
        return TabFolders.layout();
    }

    /** The tab an entry stands for in the list: itself, or the first tab a group takes. */
    private TabBase sample(String entry) {
        if (!TabLayout.isGroup(entry)) {
            return this.known.get(entry);
        }
        for (Map.Entry<String, TabBase> e : this.known.entrySet()) {
            if (TabLayout.matches(entry, e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private boolean listed(Object o) {
        return o instanceof String entry && this.sample(entry) != null;
    }

    /** Folders always count as visible neighbours; tab entries only when this session has the tab. */
    private final Predicate<Object> TOP_VISIBLE = o -> o instanceof TabLayout.Folder || listed(o);

    @Override
    protected void init() {
        this.known = TabFolders.knownTabs();
        List<String> added = layout().reconcile(this.known.keySet());
        if (!added.isEmpty()) {
            NEW_THIS_SESSION.addAll(added);
            TabFolders.LOG.info("Placed {} tab(s) not in the layout at the top level: {}", added.size(), added);
            TabFolders.save();
        }
        double scroll = this.list == null ? 0 : this.list.getScrollAmount();
        this.list = new EntryList(this.minecraft, this.width, this.height - 36 - 34, 34, 24);
        this.addRenderableWidget(this.list);
        this.fillEntries();
        this.list.setScrollAmount(scroll);

        int y = this.height - 28;
        int x = this.width / 2 - 155;
        this.addRenderableWidget(Button.builder(Component.translatable("tabfolders.screen.new_folder"), b -> {
            layout().top.add(new TabLayout.Folder(Component.translatable("tabfolders.screen.new_folder_name").getString(), "minecraft:chest"));
            this.changed();
            this.list.setScrollAmount(this.list.getMaxScroll());
        }).bounds(x, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("tabfolders.screen.reset"), b ->
                this.minecraft.setScreen(new ConfirmScreen(yes -> {
                    if (yes) {
                        TabFolders.resetToDefaults();
                        this.list = null;
                    }
                    this.minecraft.setScreen(this);
                }, Component.translatable("tabfolders.screen.reset"), Component.translatable("tabfolders.screen.reset.confirm")))
        ).bounds(x + 105, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(x + 210, y, 100, 20).build());
    }

    private void fillEntries() {
        this.list.clear();
        for (Object o : layout().top) {
            if (o instanceof TabLayout.Folder f) {
                this.list.add(new FolderEntry(f));
                for (String entry : f.tabs) {
                    TabBase t = this.sample(entry);
                    if (t != null) {
                        this.list.add(new TabEntry(entry, t, f));
                    }
                }
            } else if (o instanceof String entry) {
                TabBase t = this.sample(entry);
                if (t != null) {
                    this.list.add(new TabEntry(entry, t, null));
                }
            }
        }
    }

    /** Saves and rebuilds the rows after a structural edit, keeping the scroll position. */
    private void changed() {
        TabFolders.save();
        double scroll = this.list.getScrollAmount();
        this.fillEntries();
        this.list.setScrollAmount(scroll);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("tabfolders.screen.hint"), this.width / 2, 20, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        for (TabLayout.Folder f : layout().folders()) {
            if (f.name.isBlank()) {
                f.name = Component.translatable("tabfolders.screen.new_folder_name").getString();
            }
        }
        TabFolders.save();
        Minecraft mc = this.minecraft;
        boolean canReturn = this.parent != null && (!(this.parent instanceof AbstractContainerScreen<?> c)
                || (mc.player != null && mc.player.containerMenu == c.getMenu()));
        // Returning re-runs the screen's init, so the row shows the new layout at once.
        mc.setScreen(canReturn ? this.parent : null);
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
            return Math.min(ROW_WIDTH, TabsScreen.this.width - 24);
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
        private final TabLayout.Folder folder;
        private final EditBox name;
        private final EditBox icon;
        private final Button held;
        private final Button delete;
        private final Button up;
        private final Button down;

        FolderEntry(TabLayout.Folder folder) {
            this.folder = folder;
            this.name = new EditBox(TabsScreen.this.font, 0, 0, 110, 18, Component.translatable("tabfolders.screen.folder_name"));
            this.name.setMaxLength(40);
            this.name.setValue(folder.name);
            this.name.setResponder(s -> folder.name = s);
            this.icon = new EditBox(TabsScreen.this.font, 0, 0, 120, 18, Component.translatable("tabfolders.screen.folder_icon"));
            this.icon.setMaxLength(100);
            this.icon.setValue(folder.icon);
            this.icon.setTooltip(Tooltip.create(Component.translatable("tabfolders.screen.folder_icon")));
            this.icon.setResponder(s -> {
                ResourceLocation rl = ResourceLocation.tryParse(s);
                boolean ok = TabArranger.isTextureIcon(s) || (rl != null && BuiltInRegistries.ITEM.get(rl) != Items.AIR);
                this.icon.setTextColor(ok ? 0xE0E0E0 : 0xFF6060);
                if (ok) {
                    folder.icon = TabArranger.isTextureIcon(s) ? s : rl.toString();
                }
            });
            this.held = small("✋", "tabfolders.screen.held_icon", b -> {
                var player = TabsScreen.this.minecraft.player;
                if (player != null && !player.getMainHandItem().isEmpty()) {
                    this.icon.setValue(BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString());
                }
            });
            this.held.setWidth(20);
            this.delete = Button.builder(Component.translatable("tabfolders.screen.delete_folder"), b -> {
                layout().deleteFolder(folder);
                TabsScreen.this.changed();
            }).size(50, 20).build();
            this.delete.setTooltip(Tooltip.create(Component.translatable("tabfolders.screen.delete_folder.tip")));
            this.up = small("▲", "tabfolders.screen.up", b -> this.move(-1));
            this.down = small("▼", "tabfolders.screen.down", b -> this.move(1));
            this.children.addAll(List.of(this.name, this.icon, this.held, this.delete, this.up, this.down));
        }

        private void move(int dir) {
            // never above the pinned inventory tab
            if (dir < 0 && layout().top.indexOf(this.folder) <= 1 && TabLayout.INVENTORY.equals(layout().top.get(0))) {
                return;
            }
            if (TabLayout.shift(layout().top, this.folder, dir, TOP_VISIBLE)) {
                TabsScreen.this.changed();
            }
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int right = left + width;
            g.fill(left - 2, top, right + 2, top + height - 2, 0x40C08A3E);
            TabArranger.drawIcon(g, this.folder.icon, left + 2, top + 2);
            this.name.setPosition(left + 24, top + 1);
            this.icon.setPosition(left + 140, top + 1);
            this.held.setPosition(left + 264, top);
            this.name.render(g, mouseX, mouseY, partialTick);
            this.icon.render(g, mouseX, mouseY, partialTick);
            this.held.render(g, mouseX, mouseY, partialTick);
            long count = this.folder.tabs.stream().filter(TabsScreen.this::listed).count();
            if (hovering && mouseX >= left && mouseX < left + 22) {
                TabsScreen.this.setTooltipForNextRenderPass(Component.translatable("tabfolders.screen.tab_count", count));
            }
            this.down.setPosition(right - 16, top);
            this.up.setPosition(right - 34, top);
            this.delete.setPosition(right - 88, top);
            this.down.render(g, mouseX, mouseY, partialTick);
            this.up.render(g, mouseX, mouseY, partialTick);
            this.delete.render(g, mouseX, mouseY, partialTick);
        }
    }

    private final class TabEntry extends Row {
        private final String entry;
        private final TabBase tab;
        private final TabLayout.Folder folder;
        private final Button visibility;
        private final Button folderCycle;
        private final Button up;
        private final Button down;

        TabEntry(String entry, TabBase tab, TabLayout.Folder folder) {
            this.entry = entry;
            this.tab = tab;
            this.folder = folder;
            boolean locked = TabLayout.INVENTORY.equals(entry);
            boolean hidden = layout().hidden.contains(entry);
            this.visibility = Button.builder(
                    hidden ? Component.translatable("tabfolders.screen.hidden").withStyle(ChatFormatting.GRAY)
                            : Component.translatable("tabfolders.screen.shown").withStyle(ChatFormatting.GREEN),
                    b -> {
                        if (!layout().hidden.remove(entry)) {
                            layout().hidden.add(entry);
                        }
                        TabsScreen.this.changed();
                    }).size(56, 20).build();
            String where = folder == null ? Component.translatable("tabfolders.screen.no_folder").getString() : folder.name;
            this.folderCycle = Button.builder(
                    Component.literal(TabsScreen.this.font.plainSubstrByWidth(
                            Component.translatable("tabfolders.screen.folder", where).getString(), 100)),
                    b -> this.cycleFolder(Screen.hasShiftDown() ? -1 : 1)).size(110, 20).build();
            this.folderCycle.setTooltip(Tooltip.create(Component.translatable("tabfolders.screen.folder.tip")));
            this.up = small("▲", "tabfolders.screen.up", b -> this.move(-1));
            this.down = small("▼", "tabfolders.screen.down", b -> this.move(1));
            if (locked) {
                for (Button b : List.of(this.visibility, this.folderCycle, this.up, this.down)) {
                    b.active = false;
                    b.setTooltip(Tooltip.create(Component.translatable("tabfolders.screen.inventory_locked")));
                }
            }
            this.children.addAll(List.of(this.visibility, this.folderCycle, this.up, this.down));
        }

        private void cycleFolder(int dir) {
            List<TabLayout.Folder> folders = layout().folders();
            // Position 0 is "no folder"; folders follow in their row order.
            int current = this.folder == null ? 0 : folders.indexOf(this.folder) + 1;
            int size = folders.size() + 1;
            int next = Math.floorMod(current + dir, size);
            layout().moveToFolder(this.entry, next == 0 ? null : folders.get(next - 1));
            TabsScreen.this.changed();
        }

        private void move(int dir) {
            boolean moved;
            if (this.folder == null) {
                int i = layout().top.indexOf(this.entry);
                // never above the pinned inventory tab
                moved = !(dir < 0 && i <= 1 && TabLayout.INVENTORY.equals(layout().top.get(0)))
                        && TabLayout.shift(layout().top, (Object) this.entry, dir, TOP_VISIBLE);
            } else {
                moved = TabLayout.shift(this.folder.tabs, this.entry, dir, TabsScreen.this::listed);
            }
            if (moved) {
                TabsScreen.this.changed();
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
            boolean hidden = layout().hidden.contains(this.entry);
            boolean available = TabsScreen.this.minecraft.player == null || this.tab.isEnabled(TabsScreen.this.minecraft.player);
            this.tab.render(g, x, top, false, null, 0, 0);
            Component label = this.tab.getTooltip();
            if (TabLayout.isGroup(this.entry)) {
                label = Component.translatable("tabfolders.screen.group", label);
            }
            String text = label.getString();
            if (NEW_THIS_SESSION.contains(this.entry)) {
                text += " (" + Component.translatable("tabfolders.screen.new").getString() + ")";
            }
            int nameRoom = right - 208 - (x + 30) - 4;
            g.drawString(TabsScreen.this.font, TabsScreen.this.font.plainSubstrByWidth(text, nameRoom), x + 30, top + 7,
                    hidden || !available ? 0x808080 : 0xFFFFFF, false);
            if (hovering && mouseX >= x && mouseX < x + 30 + nameRoom) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal(this.entry).withStyle(ChatFormatting.DARK_GRAY));
                if (!available) {
                    tip.add(Component.translatable("tabfolders.screen.unavailable").withStyle(ChatFormatting.GRAY));
                }
                TabsScreen.this.setTooltipForNextRenderPass(tip.stream().map(Component::getVisualOrderText).toList());
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
