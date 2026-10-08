package net.eileenthg.guidelibrary;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Which items count as openable books. Shared by the client (to build the list) and the server
 * (to refuse anything else), so the two always agree.
 *
 * <p>There is deliberately no keyword guessing: in a real pack "book" and "tome" name far more
 * spell tomes and tools than guides, and opening one here means using it. Books come from the
 * {@code #guidelibrary:guide_books} tag (which ships with known guide items, each optional) and
 * from {@code extraBookItems} in the common config.
 */
public final class ItemBooks {

    public static final TagKey<Item> GUIDE_BOOKS =
            TagKey.create(Registries.ITEM, GuideLibrary.id("guide_books"));

    public static final String KEY_PREFIX = "item/";

    private ItemBooks() {}

    public static String key(ResourceLocation itemId) {
        return KEY_PREFIX + itemId;
    }

    public static boolean isBook(Item item) {
        if (item == null || item == Items.AIR) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (LibraryConfig.matchesAny(LibraryConfig.COMMON.hiddenBooks.get(), key(id))) return false;
        if (BuiltInRegistries.ITEM.wrapAsHolder(item).is(GUIDE_BOOKS)) return true;
        return LibraryConfig.matchesAny(LibraryConfig.COMMON.extraBookItems.get(), id.toString());
    }

    /** Every registered item that {@link #isBook} accepts. */
    public static List<Item> all() {
        List<Item> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (isBook(item)) out.add(item);
        }
        return out;
    }
}
