package net.eileenthg.guidelibrary.unlock;

import net.eileenthg.guidelibrary.GuideLibrary;
import net.eileenthg.guidelibrary.ItemBooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a physical stack to the library key of the book it is, on either side. The same keys are
 * used by the library list, the unlock store, the config and the commands.
 */
public final class BookIds {

    private BookIds() {}

    /** The book key this stack counts as, or null if it is not a guide book. */
    public static String identify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        try {
            if (ModList.get().isLoaded("patchouli")) {
                String k = PatchouliIds.identify(stack);
                if (k != null) return k;
            }
            if (ModList.get().isLoaded("modonomicon")) {
                String k = ModonomiconIds.identify(stack);
                if (k != null) return k;
            }
        } catch (Throwable t) {
            GuideLibrary.LOGGER.debug("Identifying {} failed", stack, t);
        }
        Item item = stack.getItem();
        return ItemBooks.isBook(item) ? ItemBooks.key(BuiltInRegistries.ITEM.getKey(item)) : null;
    }

    /**
     * Every book key that currently exists, for commands. Item books whose item already stands for
     * a Patchouli or Modonomicon book are left out, as in the library.
     */
    public static List<String> allKeys() {
        List<String> out = new ArrayList<>();
        Map<Item, String> taken = new HashMap<>();
        try {
            if (ModList.get().isLoaded("patchouli")) PatchouliIds.allKeys(out, taken);
            if (ModList.get().isLoaded("modonomicon")) ModonomiconIds.allKeys(out, taken);
        } catch (Throwable t) {
            GuideLibrary.LOGGER.warn("Listing Patchouli/Modonomicon books failed", t);
        }
        for (Item item : ItemBooks.all()) {
            if (!taken.containsKey(item)) out.add(ItemBooks.key(BuiltInRegistries.ITEM.getKey(item)));
        }
        return out;
    }

    /** Forget cached item-to-book maps after books are reloaded. */
    public static void invalidate() {
        try {
            if (ModList.get().isLoaded("patchouli")) PatchouliIds.invalidate();
            if (ModList.get().isLoaded("modonomicon")) ModonomiconIds.invalidate();
        } catch (Throwable ignored) {
        }
    }
}
