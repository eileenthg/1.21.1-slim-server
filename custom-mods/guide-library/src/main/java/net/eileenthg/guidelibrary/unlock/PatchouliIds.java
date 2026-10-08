package net.eileenthg.guidelibrary.unlock;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;
import vazkii.patchouli.common.item.PatchouliDataComponents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Which Patchouli book a stack is. Only touched when Patchouli is installed. Works on both sides. */
final class PatchouliIds {

    private static final ResourceLocation GENERIC = ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book");
    private static volatile Map<Item, ResourceLocation> customItems;

    private PatchouliIds() {}

    static String key(ResourceLocation book) {
        return "patchouli/" + book;
    }

    static String identify(ItemStack stack) {
        // The generic Patchouli book carries its id in a component.
        ResourceLocation id = stack.get(PatchouliDataComponents.BOOK);
        if (id != null) return BookRegistry.INSTANCE.books.containsKey(id) ? key(id) : null;
        // Books with their own item (Botania's Lexica style) are matched by item.
        ResourceLocation custom = customItems().get(stack.getItem());
        return custom == null ? null : key(custom);
    }

    static void allKeys(List<String> out, Map<Item, String> itemsTaken) {
        for (Map.Entry<ResourceLocation, Book> e : BookRegistry.INSTANCE.books.entrySet()) {
            out.add(key(e.getKey()));
        }
        customItems().forEach((item, id) -> itemsTaken.put(item, key(id)));
    }

    private static Map<Item, ResourceLocation> customItems() {
        Map<Item, ResourceLocation> m = customItems;
        if (m != null) return m;
        m = new HashMap<>();
        Item generic = BuiltInRegistries.ITEM.get(GENERIC);
        for (Map.Entry<ResourceLocation, Book> e : BookRegistry.INSTANCE.books.entrySet()) {
            try {
                ItemStack s = e.getValue().getBookItem();
                if (!s.isEmpty() && s.getItem() != generic) m.put(s.getItem(), e.getKey());
            } catch (Throwable ignored) {
            }
        }
        customItems = m;
        return m;
    }

    static void invalidate() {
        customItems = null;
    }
}
