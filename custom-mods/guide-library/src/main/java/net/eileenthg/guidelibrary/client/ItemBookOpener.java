package net.eileenthg.guidelibrary.client;

import net.eileenthg.guidelibrary.GuideLibrary;
import net.eileenthg.guidelibrary.network.OpenItemBookPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Opens an item-based book the player does not hold.
 *
 * <p>First the item's right-click is replayed on the client with the book briefly in the selected
 * hotbar slot. Most guide items (Alex's Mobs, Tinkers, IE...) open their screen right there.
 *
 * <p>If no screen opened, the book is one the server opens (MCA's books, vanilla-style written
 * books): the server gets {@link OpenItemBookPayload} and replays the use on its side, and its
 * reply makes the client build the screen from "the book in your main hand". So the book stays in
 * the client's slot until a new screen appears or two seconds pass, then the player's own stack
 * goes back. The server's inventory is never touched by this; it is purely what this client shows.
 */
public final class ItemBookOpener {

    private static final int PENDING_TICKS = 40;

    private static Pending pending;

    private record Pending(int slot, ItemStack original, ItemStack fake, Screen waitingOn, int[] ticksLeft) {}

    private ItemBookOpener() {}

    public static void open(Item item) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        restorePending();

        Screen before = mc.screen;
        Inventory inv = player.getInventory();
        int slot = inv.selected;
        ItemStack original = inv.getItem(slot);
        ItemStack fake = new ItemStack(item);
        inv.setItem(slot, fake);
        try {
            item.use(player.level(), player, InteractionHand.MAIN_HAND);
        } catch (Throwable t) {
            GuideLibrary.LOGGER.warn("Client-side use of {} threw", BuiltInRegistries.ITEM.getKey(item), t);
        }

        if (mc.screen != before) {
            inv.setItem(slot, original);
            return;
        }

        var connection = mc.getConnection();
        if (connection == null || !connection.hasChannel(OpenItemBookPayload.TYPE)) {
            inv.setItem(slot, original);
            BookCatalog.tellPlayer(Component.translatable("guidelibrary.needs_server", fake.getHoverName()));
            return;
        }
        pending = new Pending(slot, original, fake, before, new int[]{PENDING_TICKS});
        PacketDistributor.sendToServer(new OpenItemBookPayload(BuiltInRegistries.ITEM.getKey(item)));
    }

    /** Called every client tick. */
    static void tick() {
        Pending p = pending;
        if (p == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            pending = null;
            return;
        }
        if (mc.screen != p.waitingOn() || --p.ticksLeft()[0] <= 0) restorePending();
    }

    private static void restorePending() {
        Pending p = pending;
        pending = null;
        if (p == null) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        Inventory inv = player.getInventory();
        // Identity check: if the server has since sent this slot's real contents, those win.
        if (inv.getItem(p.slot()) == p.fake()) inv.setItem(p.slot(), p.original());
    }

    static void clear() {
        pending = null;
    }
}
