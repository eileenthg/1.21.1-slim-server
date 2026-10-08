package net.eileenthg.guidelibrary.unlock;

import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.klikli_dev.modonomicon.item.ModonomiconItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Which Modonomicon book a stack is. Only touched when Modonomicon is installed. Works on both sides. */
final class ModonomiconIds {

    private static volatile Map<Item, ResourceLocation> customItems;

    private ModonomiconIds() {}

    static String key(ResourceLocation book) {
        return "modonomicon/" + book;
    }

    static String identify(ItemStack stack) {
        Map<ResourceLocation, Book> books = BookDataManager.get().getBooks();
        if (books == null) return null;
        if (stack.getItem() instanceof ModonomiconItem) {
            ResourceLocation id = ModonomiconItem.getBookId(stack);
            if (id != null && books.containsKey(id)) return key(id);
        }
        ResourceLocation custom = customItems(books).get(stack.getItem());
        return custom == null ? null : key(custom);
    }

    static void allKeys(List<String> out, Map<Item, String> itemsTaken) {
        Map<ResourceLocation, Book> books = BookDataManager.get().getBooks();
        if (books == null) return;
        for (ResourceLocation id : books.keySet()) out.add(key(id));
        customItems(books).forEach((item, id) -> itemsTaken.put(item, key(id)));
    }

    private static Map<Item, ResourceLocation> customItems(Map<ResourceLocation, Book> books) {
        Map<Item, ResourceLocation> m = customItems;
        if (m != null && !m.isEmpty()) return m;
        m = new HashMap<>();
        for (Map.Entry<ResourceLocation, Book> e : books.entrySet()) {
            ResourceLocation item = e.getValue().getCustomBookItem();
            if (item == null) continue;
            Item i = BuiltInRegistries.ITEM.get(item);
            if (i != Items.AIR) m.put(i, e.getKey());
        }
        customItems = m;
        return m;
    }

    static void invalidate() {
        customItems = null;
    }
}
