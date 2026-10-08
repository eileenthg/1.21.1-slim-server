package net.eileenthg.tabfolders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sfiomn.legendarytabs.api.tabs_menu.DataDrivenTabBase;
import sfiomn.legendarytabs.api.tabs_menu.TabBase;
import sfiomn.legendarytabs.api.tabs_menu.TabData;
import sfiomn.legendarytabs.api.tabs_menu.TabsMenu;

@Mod(value = TabFolders.MOD_ID, dist = Dist.CLIENT)
public final class TabFolders {
    public static final String MOD_ID = "tabfolders";
    public static final Logger LOG = LoggerFactory.getLogger("TabFolders");

    private static TabLayout layout;

    public TabFolders() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> Opener.tick());
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("tabfolders.json");
    }

    /**
     * Every tab Legendary Tabs has put on any screen, by ID, in its own order. Data tabs whose mods aren't
     * installed (Legendary Tabs ships tabs for FTB Quests, JourneyMap and so on) are left out.
     */
    public static Map<String, TabBase> knownTabs() {
        Map<String, TabBase> out = new LinkedHashMap<>();
        for (Class<?> screen : TabsMenu.getRegisteredScreens()) {
            TabsMenu.ScreenInfo info = TabsMenu.getScreenInfo(screen);
            if (info == null) {
                continue;
            }
            for (List<TabBase> bucket : info.tabs.values()) {
                for (TabBase tab : bucket) {
                    if (installed(tab)) {
                        out.putIfAbsent(tab.getId(), tab);
                    }
                }
            }
        }
        return out;
    }

    private static boolean installed(TabBase tab) {
        if (tab instanceof DataDrivenTabBase data) {
            TabData d = data.getTabData();
            return d.isEnabled() && d.getRequiredMods().stream().allMatch(ModList.get()::isLoaded);
        }
        return true;
    }

    /** The layout, loaded on first use. New tabs are placed when the editor opens, not here. */
    public static TabLayout layout() {
        if (layout == null) {
            TabLayout loaded = null;
            try {
                loaded = TabLayout.load(file());
            } catch (IOException e) {
                LOG.error("Could not read {}; keeping it as .broken and starting from the default layout", file(), e);
                try {
                    Files.move(file(), file().resolveSibling("tabfolders.json.broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                }
            }
            layout = loaded == null ? TabLayout.defaults() : loaded;
            if (loaded == null) {
                save();
            }
        }
        return layout;
    }

    public static void save() {
        if (layout == null) {
            return;
        }
        try {
            layout.save(file());
        } catch (IOException e) {
            LOG.error("Could not save {}", file(), e);
        }
    }

    public static void resetToDefaults() {
        layout = TabLayout.defaults();
        layout.reconcile(knownTabs().keySet());
        save();
    }
}
