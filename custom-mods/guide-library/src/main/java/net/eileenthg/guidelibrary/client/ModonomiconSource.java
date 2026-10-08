package net.eileenthg.guidelibrary.client;

import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.BookAddress;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.klikli_dev.modonomicon.registry.DataComponentRegistry;
import com.klikli_dev.modonomicon.registry.ItemRegistry;
import net.eileenthg.guidelibrary.GuideLibrary;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

/**
 * Every Modonomicon book the server has synced to this client. Only touched when Modonomicon is
 * installed.
 */
final class ModonomiconSource {

    private ModonomiconSource() {}

    static void collect(List<BookEntry> out) {
        Map<ResourceLocation, Book> books = BookDataManager.get().getBooks();
        if (books == null) return;
        for (Map.Entry<ResourceLocation, Book> e : books.entrySet()) {
            ResourceLocation id = e.getKey();
            Book book = e.getValue();
            if (book == null) continue;
            out.add(new BookEntry("modonomicon/" + id, "Modonomicon",
                    Component.translatable(book.getName()), BookCatalog.modName(id.getNamespace()),
                    icon(id, book), () -> open(id)));
        }
    }

    private static ItemStack icon(ResourceLocation id, Book book) {
        try {
            ResourceLocation custom = book.getCustomBookItem();
            if (custom != null) {
                Item item = BuiltInRegistries.ITEM.get(custom);
                if (item != Items.AIR) return new ItemStack(item);
            }
            ItemStack stack = new ItemStack(ItemRegistry.MODONOMICON.get());
            stack.set(DataComponentRegistry.BOOK_ID.get(), id);
            return stack;
        } catch (Throwable t) {
            return new ItemStack(Items.BOOK);
        }
    }

    private static void open(ResourceLocation id) {
        try {
            Book book = BookDataManager.get().getBook(id);
            if (book == null) {
                BookCatalog.tellPlayer(Component.translatable("guidelibrary.open_failed", id.toString()));
                return;
            }
            BookGuiManager.get().openBook(BookAddress.defaultFor(book));
        } catch (Throwable t) {
            GuideLibrary.LOGGER.warn("Could not open Modonomicon book {}", id, t);
            BookCatalog.tellPlayer(Component.translatable("guidelibrary.open_failed", id.toString()));
        }
    }
}
