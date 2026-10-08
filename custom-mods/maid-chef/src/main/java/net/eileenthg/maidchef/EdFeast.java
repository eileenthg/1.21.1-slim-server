package net.eileenthg.maidchef;

import com.lance5057.extradelight.blocks.RecipeFeastBlock;
import com.lance5057.extradelight.recipe.FeastRecipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Extra Delight feasts: a placed feast block hands out a serving when used with its container
 * (a bowl, or sometimes food such as a bowl of rice), per the extradelight:feast recipes.
 * Only class-loaded when Extra Delight is installed.
 */
final class EdFeast {
    /** One way to serve the feast: what you get and what you use up to get it. */
    record Option(Item serving, Ingredient container) {
    }

    record Info(int servings, List<Option> options) {
    }

    private EdFeast() {
    }

    /** servings x the best option's (serving value - value of the food used up as container). */
    static double value(Object info, java.util.function.ToDoubleFunction<Item> valueOf) {
        Info i = (Info) info;
        double best = 0;
        for (Option o : i.options()) {
            ItemStack[] containers = o.container().getItems();
            double used = containers.length == 0 ? 0 : valueOf.applyAsDouble(containers[0].getItem());
            best = Math.max(best, valueOf.applyAsDouble(o.serving()) - used);
        }
        return i.servings() * best;
    }

    /** The serving with the best net value (for the nutrition/saturation/effect minimums). */
    static Item bestServing(Object info, java.util.function.ToDoubleFunction<Item> valueOf) {
        Info i = (Info) info;
        Item best = null;
        double bestV = -1;
        for (Option o : i.options()) {
            ItemStack[] containers = o.container().getItems();
            double v = valueOf.applyAsDouble(o.serving()) - (containers.length == 0 ? 0 : valueOf.applyAsDouble(containers[0].getItem()));
            if (v > bestV) {
                bestV = v;
                best = o.serving();
            }
        }
        return best;
    }

    interface ServingSink {
        void accept(Item feast, Item serving, @javax.annotation.Nullable Item container, int servings);
    }

    /** Every way to serve every Extra Delight feast. */
    static void servings(RecipeManager rm, net.minecraft.core.HolderLookup.Provider regs, ServingSink sink) {
        index(rm, regs).forEach((feast, info) -> {
            for (Option o : info.options()) {
                ItemStack[] c = o.container().getItems();
                sink.accept(feast, o.serving(), c.length == 0 ? null : c[0].getItem(), info.servings());
            }
        });
    }

    @SuppressWarnings("unchecked")
    static Map<Item, Info> index(RecipeManager rm, net.minecraft.core.HolderLookup.Provider regs) {
        Map<Item, Info> out = new HashMap<>();
        RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.fromNamespaceAndPath("extradelight", "feast"));
        if (type == null) return out;
        for (RecipeHolder<?> h : rm.getAllRecipesFor((RecipeType<FeastRecipe>) type)) {
            if (!(h.value() instanceof FeastRecipe r)) continue;
            BlockItem feast = r.getFeast();
            if (feast == null || !(feast.getBlock() instanceof RecipeFeastBlock block)) continue;
            ItemStack serving = r.getResultItem(regs);
            if (serving.isEmpty()) continue;
            Info info = out.computeIfAbsent(feast, f -> new Info(block.getMaxServings(), new ArrayList<>()));
            info.options().add(new Option(serving.getItem(), r.getContainer()));
        }
        return out;
    }
}
