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
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla-style furnace or smoker: slot 0 input, 1 fuel, 2 output. */
public final class FurnaceStation implements Station {
    private static final int IN = 0, FUEL = 1, OUT = 2;
    private final String name;
    private final Class<? extends AbstractFurnaceBlockEntity> beClass;
    private final RecipeType<? extends AbstractCookingRecipe> type;

    public FurnaceStation(String name, Class<? extends AbstractFurnaceBlockEntity> beClass, RecipeType<? extends AbstractCookingRecipe> type) {
        this.name = name;
        this.beClass = beClass;
        this.type = type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        return beClass.isInstance(be);
    }

    @Nullable
    private AbstractFurnaceBlockEntity be(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity f && beClass.isInstance(f) ? f : null;
    }

    @Override
    public List<RecipeNeed> recipes(ServerLevel level) {
        List<RecipeNeed> out = new ArrayList<>();
        for (RecipeHolder<? extends AbstractCookingRecipe> h : level.getRecipeManager().getAllRecipesFor(type)) {
            AbstractCookingRecipe r = h.value();
            if (r.getIngredients().isEmpty() || r.getIngredients().get(0).isEmpty()) continue;
            out.add(new RecipeNeed(h.id(), h, List.of(r.getIngredients().get(0)), null, ItemStack.EMPTY,
                    r.getResultItem(level.registryAccess()).copy(), r.getCookingTime(), Set.of()));
        }
        return out;
    }

    @Override
    public boolean idle(ServerLevel level, BlockPos pos) {
        AbstractFurnaceBlockEntity f = be(level, pos);
        return f != null && f.getItem(IN).isEmpty() && f.getItem(OUT).isEmpty();
    }

    @Override
    public int loadCap(RecipeNeed need) {
        return 64;
    }

    @Override
    public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
        AbstractFurnaceBlockEntity f = be(level, pos);
        if (f == null || !f.getItem(IN).isEmpty()) return LoadResult.FAILED;
        Item item = alloc.perInput().get(0);
        ItemStack stack = Inv.take(Inv.backpack(maid), item, Math.min(units, item.getDefaultMaxStackSize()));
        if (stack.isEmpty()) return LoadResult.FAILED;
        f.setItem(IN, stack);
        f.setChanged();
        return new LoadResult(stack.getCount(), 0);
    }

    @Override
    public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
        AbstractFurnaceBlockEntity f = be(level, pos);
        if (f == null) return 0;
        ItemStack out = f.getItem(OUT);
        if (out.isEmpty()) return 0;
        ItemStack copy = out.copy();
        f.setItem(OUT, ItemStack.EMPTY);
        f.setChanged();
        collected.merge(copy.getItem(), copy.getCount(), Integer::sum);
        int got = copy.is(need.resultItem()) ? copy.getCount() : 0;
        Inv.giveOrDrop(maid, copy);
        return got;
    }

    @Override
    public boolean finished(ServerLevel level, BlockPos pos) {
        AbstractFurnaceBlockEntity f = be(level, pos);
        return f == null || f.getItem(IN).isEmpty();
    }

    @Override
    public boolean usesFuel() {
        return true;
    }

    @Override
    public int storedFuelTicks(ServerLevel level, BlockPos pos) {
        AbstractFurnaceBlockEntity f = be(level, pos);
        if (f == null) return 0;
        ItemStack fuel = f.getItem(FUEL);
        return fuel.isEmpty() ? 0 : fuel.getCount() * fuel.getBurnTime(type);
    }

    @Override
    public void addFuel(ServerLevel level, EntityMaid maid, BlockPos pos, Item fuel, int count) {
        AbstractFurnaceBlockEntity f = be(level, pos);
        if (f == null || count <= 0) return;
        ItemStack slot = f.getItem(FUEL);
        if (!slot.isEmpty() && !slot.is(fuel)) return;
        int room = fuel.getDefaultMaxStackSize() - slot.getCount();
        ItemStack add = Inv.take(Inv.backpack(maid), fuel, Math.min(room, count));
        if (add.isEmpty()) return;
        if (slot.isEmpty()) f.setItem(FUEL, add);
        else slot.grow(add.getCount());
        f.setChanged();
    }
}
