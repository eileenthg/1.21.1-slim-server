package eteil.gensokyocompat.client;

import eteil.gensokyocompat.GensokyoCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sfiomn.legendarytabs.api.tabs_menu.TabBase;
import sfiomn.legendarytabs.api.tabs_menu.TabsMenu;

/**
 * Gensokyo Legacy's quest log as a Legendary Tabs tab. Legacy only exposes it through L2Tabs (registered under the
 * id "golem"), which the pack hides; this opens the same QuestInfoScreen from the Legendary Tabs row.
 */
public final class QuestTab extends TabBase {
    private static final String SCREEN = "dev.xkmc.gensokyolegacy.content.ui.quest.QuestInfoScreen";
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(GensokyoCompat.MOD_ID, "textures/gui/quest_tab.png");
    private static Class<?> screenClass;

    static Class<?> screenClass() {
        if (screenClass == null) {
            try { screenClass = Class.forName(SCREEN); } catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
        }
        return screenClass;
    }

    @Override public String getId() { return "gensokyolegacy_quests"; }

    @Override
    public void openTargetScreen(Player player) {
        try {
            Minecraft.getInstance().setScreen((Screen) screenClass().getConstructor().newInstance());
        } catch (ReflectiveOperationException e) {
            GensokyoCompat.LOG.error("[gensokyocompat] could not open the Legacy quest screen", e);
        }
    }

    @Override public boolean isEnabled(Player player) { return true; }

    @Override
    public void initTabOnScreens() {
        TabsMenu.addTabToScreen(this, InventoryScreen.class, p -> 176, p -> 166, 10);
        TabsMenu.addTabToScreen(this, screenClass(), p -> 176, p -> 166, 10);
    }

    @Override public boolean isCurrentlyUsed(Screen screen) { return screenClass().isInstance(screen); }
    @Override public Component getTooltip() { return Component.literal("Quests"); }
    @Override public ResourceLocation getIconTexture() { return ICON; }

    public static void register() {
        TabsMenu.register(new QuestTab());
    }
}
