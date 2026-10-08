package net.eileenthg.guidelibrary.network;

import io.netty.buffer.ByteBuf;
import net.eileenthg.guidelibrary.GuideLibrary;
import net.eileenthg.guidelibrary.ItemBooks;
import net.eileenthg.guidelibrary.unlock.Unlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client asks the server to "use" a book item it does not hold, for books whose GUI is opened by
 * the server (MCA's books, vanilla-style written books). The client keeps the book in its own
 * hand slot for a moment so the screen the server triggers finds it there.
 */
public record OpenItemBookPayload(ResourceLocation itemId) implements CustomPacketPayload {

    public static final Type<OpenItemBookPayload> TYPE = new Type<>(GuideLibrary.id("open_item_book"));
    public static final StreamCodec<ByteBuf, OpenItemBookPayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(OpenItemBookPayload::new, OpenItemBookPayload::itemId);

    /** Minimum real time between two opens per player, so the packet cannot be spammed. */
    private static final long COOLDOWN_MS = 250;
    private static final Map<UUID, Long> LAST_OPEN = new ConcurrentHashMap<>();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenItemBookPayload msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (!player.isAlive() || player.isSpectator()) return;

        Item item = BuiltInRegistries.ITEM.getOptional(msg.itemId()).orElse(null);
        // The allowlist is the whole security model: without it a modified client could "use"
        // any item in the game for free.
        if (item == null || !ItemBooks.isBook(item)) {
            GuideLibrary.LOGGER.warn("{} asked to open {}, which is not a listed guide book; ignored",
                    player.getGameProfile().getName(), msg.itemId());
            return;
        }

        // Only books this player has found (or the config hands out) can be opened remotely.
        if (!Unlocks.isUnlocked(player, ItemBooks.key(msg.itemId()))) {
            GuideLibrary.LOGGER.warn("{} asked to open {}, which they have not unlocked; ignored",
                    player.getGameProfile().getName(), msg.itemId());
            return;
        }

        // Wall-clock, not game time: singleplayer pauses while a book is open, so two opens seconds
        // apart can be only a tick apart in game time.
        long now = net.minecraft.Util.getMillis();
        Long last = LAST_OPEN.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_MS) return;
        LAST_OPEN.put(player.getUUID(), now);

        simulateUse(player, new ItemStack(item));
    }

    /**
     * Puts {@code book} in the selected hotbar slot, calls its {@code use}, then puts the player's
     * own stack back unconditionally. Whatever the book's {@code use} left in the slot is thrown
     * away, so opening a book can never create or destroy an item.
     */
    static void simulateUse(ServerPlayer player, ItemStack book) {
        Inventory inv = player.getInventory();
        int slot = inv.selected;
        ItemStack original = inv.getItem(slot);
        boolean wasUsing = player.isUsingItem();
        inv.setItem(slot, book);
        try {
            book.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        } catch (Throwable t) {
            GuideLibrary.LOGGER.warn("Opening {} for {} threw", book.getItem(), player.getGameProfile().getName(), t);
        } finally {
            inv.setItem(slot, original);
            if (!wasUsing && player.isUsingItem()) player.stopUsingItem();
        }
    }

    public static void forget(UUID player) {
        LAST_OPEN.remove(player);
    }
}
