package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.lance5057.extradelight.ExtraDelightRecipes;
import com.lance5057.extradelight.workstations.oven.OvenBlockEntity;
import com.lance5057.extradelight.workstations.oven.recipes.OvenRecipe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/** Extra Delight stations. Only class-loaded when Extra Delight is installed. */
public final class EdStations {
    private EdStations() {
    }

    public static void addTo(List<Station> list) {
        list.add(new Oven());
        list.add(new EdWorkstations.DryingRack());
        list.add(new EdWorkstations.DoughShaping());
        list.add(new EdWorkstations.Mortar());
        list.add(new EdWorkstations.Juicer());
        list.add(new EdWorkstations.MixingBowl());
        list.add(new EdWorkstations.MeltingPot());
        list.add(new EdWorkstations.Chiller());
        list.add(new EdSlowStations.Vat());
        list.add(new EdSlowStations.Evaporator());
    }



    // ---------------------------------------------------------------- oven
    /**
     * Like the cooking pot: up to 9 ingredients (one of each per bake), heat from below, results to the output slot.
     * Every recipe also wants its baking dish in the dish slot. Most keep it (a tool she brings back afterwards);
     * a few use one up per bake, and those count it as one more ingredient (the last one in the list).
     */
    static final class Oven implements Station {
        private static final int INPUTS = 9, DISH = 10, OUT = 11;

        @Override
        public String name() {
            return "oven";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof OvenBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<OvenRecipe> h : level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.OVEN.get())) {
                OvenRecipe r = h.value();
                List<Ingredient> inputs = new ArrayList<>(r.getIngredients().stream().filter(i -> !i.isEmpty()).toList());
                if (inputs.isEmpty() || inputs.size() > INPUTS) continue;
                ItemStack result = r.getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                Set<Item> extras = Kit.remainders(inputs);
                ItemStack dish = r.getOutputContainer();
                Ingredient tool = null;
                if (!dish.isEmpty()) {
                    if (r.shouldConsumeContainer()) inputs.add(Ingredient.of(dish.getItem()));
                    else tool = Ingredient.of(dish.getItem());
                }
                out.add(new RecipeNeed(h.id(), h, List.copyOf(inputs), tool, ItemStack.EMPTY, result.copy(), r.getCookTime(), extras));
            }
            return out;
        }

        private static boolean consumesDish(RecipeNeed need) {
            return need.holder() != null && need.holder().value() instanceof OvenRecipe r && r.shouldConsumeContainer()
                    && !r.getOutputContainer().isEmpty();
        }

        /** Ingredients that go in the 9 input slots (a used-up dish goes in the dish slot instead). */
        private static int slotInputs(RecipeNeed need) {
            return need.inputs().size() - (consumesDish(need) ? 1 : 0);
        }

        @Nullable
        private static Item dishItem(RecipeNeed need) {
            if (need.holder() != null && need.holder().value() instanceof OvenRecipe r && !r.getOutputContainer().isEmpty()) {
                return r.getOutputContainer().getItem();
            }
            return null;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof OvenBlockEntity oven) || !oven.isHeated()) return false;
            ItemStackHandler inv = oven.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return cap(need);
        }

        /** The output slot holds one stack of the result. */
        private static int cap(RecipeNeed need) {
            return Math.max(1, Math.min(64, need.result().getMaxStackSize()) / need.resultCount());
        }

        private static int units(RecipeNeed need, Alloc alloc, int units) {
            for (Item item : alloc.perInput()) units = Math.min(units, item.getDefaultMaxStackSize());
            return Math.min(units, cap(need));
        }

        /** How many of the dish go in the dish slot for {@code units} bakes. */
        private static int dishCount(RecipeNeed need, int units) {
            return consumesDish(need) ? units : 1;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof OvenBlockEntity oven)) return LoadResult.FAILED;
            units = units(need, alloc, units);
            if (units <= 0) return LoadResult.FAILED;
            ItemStackHandler inv = oven.getInventory();
            IItemHandler bag = Inv.backpack(maid);
            int n = slotInputs(need);
            List<Integer> filled = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < n; i++) {
                ItemStack stack = Inv.take(bag, alloc.perInput().get(i), units);
                if (stack.getCount() < units) {
                    Inv.giveOrDrop(maid, stack);
                    ok = false;
                    break;
                }
                inv.setStackInSlot(i, stack);
                filled.add(i);
            }
            Item dish = dishItem(need);
            if (ok && dish != null) {
                int want = dishCount(need, units);
                ItemStack d = Inv.take(bag, dish, want);
                if (d.getCount() < want) {
                    Inv.giveOrDrop(maid, d);
                    ok = false;
                } else {
                    inv.setStackInSlot(DISH, d);
                    filled.add(DISH);
                }
            }
            if (!ok) {
                for (int slot : filled) {
                    Inv.giveOrDrop(maid, inv.getStackInSlot(slot).copy());
                    inv.setStackInSlot(slot, ItemStack.EMPTY);
                }
                return LoadResult.FAILED;
            }
            oven.setChanged();
            return new LoadResult(units, 0);
        }

        @Override
        public boolean canStage() {
            return true;
        }

        private static boolean wrongMatch(ServerLevel level, OvenBlockEntity oven, RecipeNeed need) {
            return level.getRecipeManager().getRecipeFor(ExtraDelightRecipes.OVEN.get(), new RecipeWrapper(oven.getInventory()), level)
                    .map(h -> !h.id().equals(need.id())).orElse(false);
        }

        @Override
        public boolean stage(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units) {
            if (!(level.getBlockEntity(pos) instanceof OvenBlockEntity oven)) return false;
            units = Math.max(1, units(need, alloc, units));
            ItemStackHandler inv = oven.getInventory();
            IItemHandler bag = Inv.backpack(maid);
            // the dish first: without it nothing can match, so partial loads never bake the wrong thing early
            Item dish = dishItem(need);
            if (dish != null && inv.getStackInSlot(DISH).isEmpty()) {
                int want = dishCount(need, units);
                if (Inv.count(bag, dish) >= want) inv.setStackInSlot(DISH, Inv.take(bag, dish, want));
            }
            // carrying all that's left: put it all in at once (a partial load of e.g. 2 carrots matches the 1-carrot recipe)
            boolean all = stillToStage(level, pos, need, alloc, units).entrySet().stream().allMatch(e -> Inv.count(bag, e.getKey()) >= e.getValue());
            for (int i = 0; i < slotInputs(need); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) continue;
                Item item = alloc.perInput().get(i);
                if (Inv.count(bag, item) < units) continue;
                inv.setStackInSlot(i, Inv.take(bag, item, units));
                if (!all && wrongMatch(level, oven, need)) {
                    Inv.giveOrDrop(maid, inv.getStackInSlot(i).copy());
                    inv.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
            oven.setChanged();
            return stillToStage(level, pos, need, alloc, units).isEmpty();
        }

        @Override
        public Map<Item, Integer> stillToStage(ServerLevel level, BlockPos pos, RecipeNeed need, Alloc alloc, int units) {
            if (!(level.getBlockEntity(pos) instanceof OvenBlockEntity oven)) return Map.of();
            units = Math.max(1, units(need, alloc, units));
            ItemStackHandler inv = oven.getInventory();
            Map<Item, Integer> out = new LinkedHashMap<>();
            for (int i = 0; i < slotInputs(need); i++) {
                if (inv.getStackInSlot(i).isEmpty()) out.merge(alloc.perInput().get(i), units, Integer::sum);
            }
            Item dish = dishItem(need);
            if (dish != null && inv.getStackInSlot(DISH).isEmpty()) out.merge(dish, dishCount(need, units), Integer::sum);
            return out;
        }

        @Override
        public void unstage(ServerLevel level, EntityMaid maid, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof OvenBlockEntity oven)) return;
            ItemStackHandler inv = oven.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                if (i == OUT) continue;
                ItemStack s = inv.getStackInSlot(i);
                if (s.isEmpty()) continue;
                Inv.giveOrDrop(maid, s.copy());
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }
            oven.setChanged();
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            int got = Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
            if (!(level.getBlockEntity(pos) instanceof OvenBlockEntity oven)) return got;
            ItemStackHandler inv = oven.getInventory();
            ItemStack out = inv.extractItem(OUT, 64, false);
            if (!out.isEmpty()) {
                collected.merge(out.getItem(), out.getCount(), Integer::sum);
                if (out.is(need.resultItem())) got += out.getCount();
                Inv.giveOrDrop(maid, out);
            }
            if (finished(level, pos)) {
                // the baking dish (or what's left of used-up ones) comes back with her and goes back to the ingredient chest
                ItemStack d = inv.extractItem(DISH, 64, false);
                if (!d.isEmpty()) {
                    collected.merge(d.getItem(), d.getCount(), Integer::sum);
                    Inv.giveOrDrop(maid, d);
                }
            }
            oven.setChanged();
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof OvenBlockEntity oven)) return true;
            ItemStackHandler inv = oven.getInventory();
            for (int i = 0; i < INPUTS; i++) {
                if (!inv.getStackInSlot(i).isEmpty()) return false;
            }
            return true;
        }
    }
}
