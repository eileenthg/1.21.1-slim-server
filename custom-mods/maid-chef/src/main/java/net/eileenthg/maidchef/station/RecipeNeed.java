package net.eileenthg.maidchef.station;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * One recipe as a station sees it: what one "unit" (one craft) consumes and yields.
 *
 * @param id         recipe id (or a made-up maidchef: id for things that aren't recipes, like serving a feast)
 * @param holder     the recipe, or null when there is none
 * @param inputs     consumed ingredients per unit (empty ingredients already removed)
 * @param tool       a tool that must be present but is not consumed (cutting board), or null
 * @param container  container consumed per result item (Farmer's Delight bowls etc.), or EMPTY
 * @param result     main result of one unit (count = items per unit)
 * @param cookTicks  expected ticks per unit, 0 for instant stations
 * @param extras     other items the station may hand out that she should pick up (byproducts, remainders)
 */
public record RecipeNeed(ResourceLocation id, @Nullable RecipeHolder<?> holder, List<Ingredient> inputs, @Nullable Ingredient tool, ItemStack container,
                         ItemStack result, int cookTicks, Set<Item> extras) {
    public Item resultItem() {
        return result.getItem();
    }

    public int resultCount() {
        return Math.max(1, result.getCount());
    }
}
