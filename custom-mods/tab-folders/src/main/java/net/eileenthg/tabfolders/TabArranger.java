package net.eileenthg.tabfolders;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import sfiomn.legendarytabs.api.tabs_menu.TabBase;

/**
 * Turns the tabs Legendary Tabs picked for a screen into the row this player sees: their own order, folders
 * collapsed into one tab each (or one folder's tabs behind a Back tab), hidden tabs left out, and the gear last.
 *
 * <p>An open folder belongs to the screen it was opened on. A newly opened screen whose own tab sits in a folder
 * starts with that folder open, so moving between the tabs of one folder stays inside it.
 */
public final class TabArranger {
    private static WeakReference<Screen> stateScreen = new WeakReference<>(null);
    private static TabLayout.Folder stateFolder;

    private TabArranger() {
    }

    public static List<TabBase> arrange(Screen screen, List<TabBase> available) {
        TabLayout layout = TabFolders.layout();
        Map<String, TabBase> byId = new LinkedHashMap<>();
        for (TabBase t : available) {
            byId.putIfAbsent(t.getId(), t);
        }
        Set<String> exact = layout.exactIds();
        Set<String> placed = new LinkedHashSet<>();

        Map<TabLayout.Folder, List<TabBase>> members = new LinkedHashMap<>();
        for (TabLayout.Folder f : layout.folders()) {
            members.put(f, resolve(f.tabs, byId, exact, placed, layout));
        }

        TabBase current = null;
        for (TabBase t : available) {
            if (isCurrent(t, screen)) {
                current = t;
                break;
            }
        }
        if (stateScreen.get() != screen) {
            stateScreen = new WeakReference<>(screen);
            stateFolder = null;
            if (current != null) {
                for (Map.Entry<TabLayout.Folder, List<TabBase>> e : members.entrySet()) {
                    if (e.getValue().size() > 1 && e.getValue().contains(current)) {
                        stateFolder = e.getKey();
                        break;
                    }
                }
            }
        }

        List<TabBase> open = stateFolder == null ? null : members.get(stateFolder);
        if (open != null && open.size() > 1) {
            List<TabBase> row = new ArrayList<>();
            row.add(new BackTab(stateFolder));
            row.addAll(open);
            return row;
        }
        stateFolder = null;

        List<TabBase> row = new ArrayList<>();
        for (Object o : layout.top) {
            if (o instanceof String entry) {
                row.addAll(resolve(List.of(entry), byId, exact, placed, layout));
            } else if (o instanceof TabLayout.Folder f) {
                List<TabBase> in = members.get(f);
                if (in.size() == 1) {
                    row.add(in.get(0));
                } else if (in.size() > 1) {
                    row.add(new FolderTab(f, in.size()));
                }
            }
        }
        // Tabs the layout doesn't know yet (a new mod, a new backpack) still show, before the gear.
        for (TabBase t : byId.values()) {
            if (!placed.contains(t.getId()) && !layout.places(t.getId()) && !layout.isHidden(t.getId())) {
                row.add(t);
            }
        }
        row.add(GearTab.INSTANCE);
        return row;
    }

    /** The tabs a list of entries stands for, in entry order; a group never takes a tab named exactly elsewhere. */
    private static List<TabBase> resolve(List<String> entries, Map<String, TabBase> byId, Set<String> exact,
                                         Set<String> placed, TabLayout layout) {
        List<TabBase> out = new ArrayList<>();
        for (String entry : entries) {
            if (layout.hidden.contains(entry)) {
                continue;
            }
            if (TabLayout.isGroup(entry)) {
                for (TabBase t : byId.values()) {
                    String id = t.getId();
                    if (TabLayout.matches(entry, id) && !exact.contains(id) && !layout.isHidden(id) && placed.add(id)) {
                        out.add(t);
                    }
                }
            } else {
                TabBase t = byId.get(entry);
                if (t != null && placed.add(entry)) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    private static boolean isCurrent(TabBase t, Screen screen) {
        try {
            return t.isCurrentlyUsed(screen);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Re-runs the screen's init (as Sophisticated Tab does), which rebuilds the row through Legendary Tabs. */
    private static void showFolder(TabLayout.Folder folder) {
        Minecraft mc = Minecraft.getInstance();
        stateScreen = new WeakReference<>(mc.screen);
        stateFolder = folder;
        // After the click has finished going through the screen's widget list.
        mc.tell(() -> {
            if (mc.screen != null && mc.screen == stateScreen.get()) {
                mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        });
    }

    /** A folder icon is an item ID, or a texture path ending in .png (drawn 16x16, like JEI's info icon). */
    public static boolean isTextureIcon(String icon) {
        return icon != null && icon.endsWith(".png") && ResourceLocation.tryParse(icon) != null;
    }

    public static void drawIcon(GuiGraphics gui, String icon, int x, int y) {
        if (isTextureIcon(icon)) {
            gui.blit(ResourceLocation.parse(icon), x, y, 0, 0, 16, 16, 16, 16);
        } else {
            gui.renderItem(icon(icon), x, y);
        }
    }

    public static ItemStack icon(String itemId) {
        ResourceLocation rl = itemId == null ? null : ResourceLocation.tryParse(itemId);
        var item = rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
        return new ItemStack(item == Items.AIR ? Items.CHEST : item);
    }

    // ---- the row's own tabs ----

    private abstract static class OwnTab extends TabBase {
        @Override
        public boolean isEnabled(Player player) {
            return true;
        }

        /** Never put on screens directly; they only exist inside an arranged row. */
        @Override
        public void initTabOnScreens() {
        }

        @Override
        public boolean isCurrentlyUsed(Screen screen) {
            return false;
        }
    }

    private static final class FolderTab extends OwnTab {
        private final TabLayout.Folder folder;
        private final int count;

        FolderTab(TabLayout.Folder folder, int count) {
            this.folder = folder;
            this.count = count;
        }

        @Override
        public String getId() {
            return "tabfolders:folder/" + folder.name;
        }

        @Override
        public void openTargetScreen(Player player) {
            showFolder(folder);
        }

        @Override
        public Component getTooltip() {
            return Component.translatable("tabfolders.tab.folder", folder.name, count);
        }

        @Override
        public ResourceLocation getIconTexture() {
            return null;
        }

        @Override
        public void render(GuiGraphics gui, int x, int y, boolean hover, ResourceLocation buttonTexture, int iconOffsetX, int iconOffsetY) {
            gui.blit(buttonTexture != null ? buttonTexture : DEFAULT_BUTTONS_TEXTURE, x, y, hover ? 27 : 0, 0, 26, 22, 64, 64);
            drawIcon(gui, folder.icon, x + 4 + iconOffsetX, y + 4 + iconOffsetY);
            // a small folder corner, so folders read differently from tabs with item icons
            gui.fill(x + 19 + iconOffsetX, y + 2 + iconOffsetY, x + 24 + iconOffsetX, y + 5 + iconOffsetY, 0xFFC08A3E);
        }
    }

    private static final class BackTab extends OwnTab {
        private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(TabFolders.MOD_ID, "textures/gui/back.png");
        private final TabLayout.Folder folder;

        BackTab(TabLayout.Folder folder) {
            this.folder = folder;
        }

        @Override
        public String getId() {
            return "tabfolders:back";
        }

        @Override
        public void openTargetScreen(Player player) {
            showFolder(null);
        }

        @Override
        public Component getTooltip() {
            return Component.translatable("tabfolders.tab.back").append(" (" + folder.name + ")");
        }

        @Override
        public ResourceLocation getIconTexture() {
            return ICON;
        }
    }

    static final class GearTab extends OwnTab {
        static final GearTab INSTANCE = new GearTab();
        private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(TabFolders.MOD_ID, "textures/gui/gear.png");

        @Override
        public String getId() {
            return "tabfolders:settings";
        }

        @Override
        public void openTargetScreen(Player player) {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new TabsScreen(mc.screen));
        }

        @Override
        public Component getTooltip() {
            return Component.translatable("tabfolders.tab.settings");
        }

        @Override
        public ResourceLocation getIconTexture() {
            return ICON;
        }
    }
}
