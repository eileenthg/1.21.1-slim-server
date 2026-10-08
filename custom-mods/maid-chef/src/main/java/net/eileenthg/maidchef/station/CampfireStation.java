package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla (soul) campfire: 4 slots, one item each; cooked items pop out as item entities. */
public final class CampfireStation implements Station {
    public static List<RecipeNeed> campfireRecipes(ServerLevel level) {
        List<RecipeNeed> out = new ArrayList<>();
        for (RecipeHolder<CampfireCookingRecipe> h : level.getRecipeManager().getAllRecipesFor(RecipeType.CAMPFIRE_COOKING)) {
            CampfireCookingRecipe r = h.value();
            if (r.getIngredients().isEmpty() || r.getIngredients().get(0).isEmpty()) continue;
            out.add(new RecipeNeed(h.id(), h, List.of(r.getIngredients().get(0)), null, ItemStack.EMPTY,
                    r.getResultItem(level.registryAccess()).copy(), r.getCookingTime(), Set.of()));
        }
        return out;
    }

    @Override
    public String name() {
        return "campfire";
    }

    @Override
    public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        return be instanceof CampfireBlockEntity;
    }

    @Override
    public List<RecipeNeed> recipes(ServerLevel level) {
        return campfireRecipes(level);
    }

    @Override
    public boolean idle(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(level.getBlockEntity(pos) instanceof CampfireBlockEntity c)) return false;
        if (!state.hasProperty(CampfireBlock.LIT) || !state.getValue(CampfireBlock.LIT)) return false;
        if (state.hasProperty(CampfireBlock.WATERLOGGED) && state.getValue(CampfireBlock.WATERLOGGED)) return false;
        return c.getItems().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public int loadCap(RecipeNeed need) {
        return 4;
    }

    @Override
    public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
        if (!(level.getBlockEntity(pos) instanceof CampfireBlockEntity c)) return LoadResult.FAILED;
        Item item = alloc.perInput().get(0);
        int cookTime = need.cookTicks();
        int loaded = 0;
        for (int i = 0; i < units; i++) {
            ItemStack one = Inv.take(Inv.backpack(maid), item, 1);
            if (one.isEmpty()) break;
            if (!c.placeFood(maid, one, cookTime)) {
                Inv.giveOrDrop(maid, one);
                break;
            }
            loaded++;
        }
        return new LoadResult(loaded, 0);
    }

    @Override
    public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
        return Inv.vacuum(level, maid, pos, it -> it == need.resultItem(), need.resultItem(), collected);
    }

    @Override
    public boolean finished(ServerLevel level, BlockPos pos) {
        return !(level.getBlockEntity(pos) instanceof CampfireBlockEntity c) || c.getItems().stream().allMatch(ItemStack::isEmpty);
    }
}
