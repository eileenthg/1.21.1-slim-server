package net.eileenthg.guidelibrary.client;

import net.eileenthg.guidelibrary.GuideLibrary;
import net.eileenthg.guidelibrary.ItemBooks;
import net.eileenthg.guidelibrary.LibraryConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the library's book list. Rebuilt every time the library opens: it is a few dozen
 * registry lookups, and rebuilding means datapack reloads and newly synced books just show up.
 */
public final class BookCatalog {

    private BookCatalog() {}

    public static List<BookEntry> collect() {
        List<BookEntry> all = new ArrayList<>();
        if (ModList.get().isLoaded("patchouli")) {
            try {
                PatchouliSource.collect(all);
            } catch (Throwable t) {
                GuideLibrary.LOGGER.warn("Reading Patchouli's book list failed", t);
            }
        }
        if (ModList.get().isLoaded("modonomicon")) {
            try {
                ModonomiconSource.collect(all);
            } catch (Throwable t) {
                GuideLibrary.LOGGER.warn("Reading Modonomicon's book list failed", t);
            }
        }

        // An item that already stands for a Patchouli or Modonomicon book would only open the
        // same book a second way; keep the richer entry.
        Set<Item> covered = new HashSet<>();
        for (BookEntry e : all) {
            if (!e.icon().isEmpty()) covered.add(e.icon().getItem());
        }
        for (Item item : ItemBooks.all()) {
            if (covered.contains(item)) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            ItemStack stack = new ItemStack(item);
            all.add(new BookEntry(ItemBooks.key(id), "Item", stack.getHoverName(),
                    modName(id.getNamespace()), stack, () -> ItemBookOpener.open(item)));
        }

        List<? extends String> hidden = LibraryConfig.COMMON.hiddenBooks.get();
        all.removeIf(e -> LibraryConfig.matchesAny(hidden, e.key()));
        return all;
    }

    /** The books this player has unlocked (or the config hands out); what the library lists. */
    public static List<BookEntry> unlocked(List<BookEntry> all) {
        return all.stream().filter(e -> ClientUnlocks.isUnlocked(e.key())).toList();
    }

    /** The entry for a {@code <source>/<id>} key, if that book exists. */
    public static Optional<BookEntry> find(String key) {
        String k = key.trim().toLowerCase(java.util.Locale.ROOT);
        return collect().stream().filter(e -> e.key().equals(k)).findFirst();
    }

    static String modName(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .map(c -> c.getModInfo().getDisplayName())
                .orElse(namespace);
    }

    static void tellPlayer(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(message, false);
    }
}
