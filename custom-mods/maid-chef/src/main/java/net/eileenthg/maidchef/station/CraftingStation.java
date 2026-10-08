package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A crafting table in the kitchen. Only plain shaped/shapeless recipes with at least two ingredients,
 * so unpacking recipes (crate → 9 carrots) never count as cooking.
 */
public final class CraftingStation implements Station {
    @Override
    public String name() {
        return "crafting table";
    }

    @Override
    public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        return state.is(Blocks.CRAFTING_TABLE);
    }

    @Override
    public List<RecipeNeed> recipes(ServerLevel level) {
        List<RecipeNeed> out = new ArrayList<>();
        for (RecipeHolder<CraftingRecipe> h : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe r = h.value();
            if (!(r instanceof ShapedRecipe) && !(r instanceof ShapelessRecipe)) continue;
            List<Ingredient> inputs = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
            if (inputs.size() < 2) continue;
            ItemStack result = r.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;
            out.add(new RecipeNeed(h.id(), h, inputs, null, ItemStack.EMPTY, result.copy(), 0, Set.of()));
        }
        return out;
    }

    @Override
    public boolean idle(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.CRAFTING_TABLE);
    }

    @Override
    public int loadCap(RecipeNeed need) {
        return 64;
    }

    @Override
    public boolean usesPantry() {
        return true;
    }

    private static CraftingInput grid(CraftingRecipe r, Alloc alloc) {
        List<ItemStack> items = new ArrayList<>();
        int k = 0;
        for (Ingredient ing : r.getIngredients()) {
            if (ing.isEmpty()) items.add(ItemStack.EMPTY);
            else items.add(new ItemStack(alloc.perInput().get(k++)));
        }
        if (r instanceof ShapedRecipe s) {
            return CraftingInput.of(s.getWidth(), s.getHeight(), items);
        }
        int n = items.size();
        int w = n <= 1 ? 1 : n <= 4 ? 2 : 3;
        int h = (n + w - 1) / w;
        while (items.size() < w * h) items.add(ItemStack.EMPTY);
        return CraftingInput.of(w, h, items);
    }

    @Override
    public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
        if (!(need.holder().value() instanceof CraftingRecipe r)) return LoadResult.FAILED;
        CraftingInput input = grid(r, alloc);
        if (!r.matches(input, level)) return LoadResult.FAILED;
        int done = 0, results = 0;
        for (int u = 0; u < units; u++) {
            // her backpack first, then straight from the ingredient chests: a crafting table holds nothing between trips
            boolean enough = alloc.perUnit().entrySet().stream().allMatch(e -> Pantry.count(level, maid, e.getKey()) >= e.getValue());
            if (!enough) break;
            ItemStack result = r.assemble(input, level.registryAccess());
            NonNullList<ItemStack> remaining = r.getRemainingItems(input);
            for (Map.Entry<Item, Integer> e : alloc.perUnit().entrySet()) {
                Pantry.remove(level, maid, e.getKey(), e.getValue());
            }
            for (ItemStack rem : remaining) {
                if (!rem.isEmpty()) {
                    collected.merge(rem.getItem(), rem.getCount(), Integer::sum);
                    Inv.giveOrDrop(maid, rem.copy());
                }
            }
            collected.merge(result.getItem(), result.getCount(), Integer::sum);
            if (result.is(need.resultItem())) results += result.getCount();
            Inv.giveOrDrop(maid, result);
            done++;
        }
        if (done > 0) maid.swing(maid.getUsedItemHand());
        return new LoadResult(done, results);
    }

    @Override
    public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
        return 0;
    }

    @Override
    public boolean finished(ServerLevel level, BlockPos pos) {
        return true;
    }
}
