package net.reimaden.touhouorigins.condition;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * True when some registered crafting-table recipe produces this item — used by
 * {@code celestial/lavish_lifestyle} to distinguish "prepared" food (bread, cake, golden apples —
 * anything with a crafting recipe) from raw ingredients (a celestial only eats the former).
 *
 * <p>No fields at all, matching Voile's empty {@code SerializableData}. There is nothing in Apoli's
 * data-driven condition or expression vocabulary that reaches a live {@code RecipeManager}, so unlike
 * most of the small preventer conditions in this port, this one genuinely needs Java rather than a
 * rename.
 *
 * <p>Only {@code RecipeType.CRAFTING} is checked, matching Voile — a food with only a furnace or
 * smoker recipe (e.g. baked potato) does not count as "prepared" here, mirroring the original.
 */
public final class CraftableItemCondition implements ConditionType<ItemCtx, CraftableItemCondition.Cfg> {

    public record Cfg() {
        public static final Cfg INSTANCE = new Cfg();
    }

    @Override
    public MapCodec<Cfg> codec() {
        return MapCodec.unit(Cfg.INSTANCE);
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        Level level = ctx.level();
        ItemStack stack = ctx.stack();

        RecipeManager recipes = level.getRecipeManager();
        for (RecipeHolder<CraftingRecipe> holder : recipes.getAllRecipesFor(RecipeType.CRAFTING)) {
            if (stack.is(holder.value().getResultItem(level.registryAccess()).getItem())) {
                return true;
            }
        }
        return false;
    }
}
