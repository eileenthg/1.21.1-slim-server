package net.eileenthg.guidelibrary.client;

import net.eileenthg.guidelibrary.GuideLibrary;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import java.util.List;
import java.util.Map;

/** Every loaded Patchouli book. Only touched when Patchouli is installed. */
final class PatchouliSource {

    private PatchouliSource() {}

    static void collect(List<BookEntry> out) {
        for (Map.Entry<ResourceLocation, Book> e : BookRegistry.INSTANCE.books.entrySet()) {
            ResourceLocation id = e.getKey();
            Book book = e.getValue();
            if (book == null) continue;
            ItemStack icon;
            try {
                icon = book.getBookItem().copy();
            } catch (Throwable t) {
                icon = ItemStack.EMPTY;
            }
            out.add(new BookEntry("patchouli/" + id, "Patchouli",
                    Component.translatable(book.name), BookCatalog.modName(id.getNamespace()), icon,
                    () -> open(id)));
        }
    }

    private static void open(ResourceLocation id) {
        try {
            PatchouliAPI.get().openBookGUI(id);
        } catch (Throwable t) {
            GuideLibrary.LOGGER.warn("Could not open Patchouli book {}", id, t);
            BookCatalog.tellPlayer(Component.translatable("guidelibrary.open_failed", id.toString()));
        }
    }
}
