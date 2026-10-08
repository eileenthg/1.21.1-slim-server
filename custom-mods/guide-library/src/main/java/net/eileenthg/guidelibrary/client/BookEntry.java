package net.eileenthg.guidelibrary.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One row in the library.
 *
 * @param key      stable {@code <source>/<id>} key, used by favorites and the config lists
 * @param source   human-readable system name ("Patchouli", "Modonomicon", "Item")
 * @param name     book title
 * @param modName  display name of the mod that owns the book
 * @param icon     stack drawn beside the title
 * @param opener   opens the book's own GUI; runs on the client thread
 */
public record BookEntry(String key, String source, Component name, String modName, ItemStack icon,
                        Runnable opener) {

    public String sortName() {
        return name.getString().toLowerCase(java.util.Locale.ROOT);
    }
}
