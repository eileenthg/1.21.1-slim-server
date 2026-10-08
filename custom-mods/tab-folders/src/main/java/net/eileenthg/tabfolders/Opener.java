package net.eileenthg.tabfolders;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import sfiomn.legendarytabs.api.tabs_menu.KeyPressSimulator;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/** Screens a data tab can't open by itself; called by name from zz-tabs' "reflection" tabs. */
public final class Opener {
    private Opener() {
    }

    /** Vanilla Statistics, normally only reachable from the pause menu. Back returns to the screen it came from. */
    public static void statistics() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new StatsScreen(mc.screen, mc.player.getStats()));
        }
    }

    /** MineColonies' clipboard opens its request list on the client, from any stack bound to a colony. */
    public static void colonyClipboard() {
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = findCarried(item("minecolonies:clipboard"));
        if (mc.player == null || stack.isEmpty()) {
            return;
        }
        try {
            Method open = stack.getItem().getClass().getDeclaredMethod("openWindow", ItemStack.class, Level.class, Player.class);
            open.setAccessible(true);
            open.invoke(null, stack, mc.level, mc.player);
        } catch (ReflectiveOperationException | RuntimeException e) {
            TabFolders.LOG.error("Could not open the colony clipboard", e);
        }
    }

    /** TLM Maid Manager's satchel tablet: its own key when worn as a curio, else used like a right-click. */
    public static void maidTablet() {
        Item tablet = item("tlm_maid_manager:maid_manager");
        if (!curio(tablet).isEmpty()) {
            KeyPressSimulator.press("key.tlm_maid_manager.open_tablet_from_curios", true);
        } else {
            useCarried(tablet);
        }
    }

    /** Spectrum's Bag of Holding opens the player's ender chest when used. */
    public static void bagOfHolding() {
        useCarried(item("spectrum:bag_of_holding"));
    }

    // ---- using an item that isn't in hand ----

    /**
     * Uses an item from wherever it is in the inventory, with plain vanilla packets so any server accepts it:
     * a hand uses it directly, a hotbar slot is selected for the use and then reselected back, and a main
     * inventory slot is swapped with the selected hotbar slot, used, and swapped back once whatever the item
     * opened has closed (a click on the inventory is refused while another container is open).
     */
    static void useCarried(Item item) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null || item == Items.AIR || pending != null) {
            return;
        }
        if (p.getMainHandItem().is(item)) {
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            return;
        }
        if (p.getOffhandItem().is(item)) {
            mc.gameMode.useItem(p, InteractionHand.OFF_HAND);
            return;
        }
        Inventory inv = p.getInventory();
        // the server must agree on which hotbar slot is "in hand" before anything is swapped into it
        p.connection.send(new ServerboundSetCarriedItemPacket(inv.selected));
        int slot = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i).is(item)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            return;
        }
        if (Inventory.isHotbarSlot(slot)) {
            int old = inv.selected;
            inv.selected = slot;
            p.connection.send(new ServerboundSetCarriedItemPacket(slot));
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            inv.selected = old;
            p.connection.send(new ServerboundSetCarriedItemPacket(old));
            return;
        }
        if (p.containerMenu != p.inventoryMenu) {
            p.closeContainer();
        }
        int hotbar = inv.selected;
        // InventoryMenu slots 9..35 are the main inventory slots of the same number
        mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, slot, hotbar, ClickType.SWAP, p);
        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        pending = new PendingSwap(slot, hotbar, mc.screen);
    }

    private static final class PendingSwap {
        final int slot;
        final int hotbar;
        final Screen startScreen;
        int ticks;
        boolean screenChanged;

        PendingSwap(int slot, int hotbar, Screen startScreen) {
            this.slot = slot;
            this.hotbar = hotbar;
            this.startScreen = startScreen;
        }
    }

    private static PendingSwap pending;

    /** Client tick: puts a swapped item back once its screen opened and closed (or nothing opened in 2 s). */
    static void tick() {
        if (pending == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) {
            pending = null;
            return;
        }
        pending.ticks++;
        if (mc.screen != pending.startScreen) {
            pending.screenChanged = true;
        }
        if ((pending.screenChanged || pending.ticks >= 40) && p.containerMenu == p.inventoryMenu) {
            mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, pending.slot, pending.hotbar, ClickType.SWAP, p);
            pending = null;
        }
    }

    // ---- finding items ----

    static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
    }

    private static ItemStack curio(Item item) {
        Player p = Minecraft.getInstance().player;
        if (p == null || item == Items.AIR || !ModList.get().isLoaded("curios")) {
            return ItemStack.EMPTY;
        }
        return CuriosApi.getCuriosInventory(p).flatMap(h -> h.findFirstCurio(item)).map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }

    /** Hands, inventory, then curio slots. */
    static ItemStack findCarried(Item item) {
        Player p = Minecraft.getInstance().player;
        if (p == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        if (p.getMainHandItem().is(item)) {
            return p.getMainHandItem();
        }
        if (p.getOffhandItem().is(item)) {
            return p.getOffhandItem();
        }
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) {
                return s;
            }
        }
        return curio(item);
    }
}
