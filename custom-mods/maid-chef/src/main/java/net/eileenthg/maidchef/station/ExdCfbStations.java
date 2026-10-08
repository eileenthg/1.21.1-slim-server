package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Expanded Delight's juicer and Cooking for Blockheads' oven. Each class only loads when its mod is installed. */
final class ExdCfbStations {
    private ExdCfbStations() {
    }

    // ---------------------------------------------------------------- Expanded Delight juicer (a Farmer's Delight-style pot, unheated)
    static final class ExdJuicer implements Station {
        private static final int INPUTS = 2, MEAL = 2, CONTAINER = 3, OUT = 4;

        @Override
        public String name() {
            return "juicer (Expanded Delight)";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof ianm1647.expandeddelight.common.block.entity.JuicerBlockEntity;
        }

        @Nullable
        private static ItemStackHandler inv(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof ianm1647.expandeddelight.common.block.entity.JuicerBlockEntity j ? j.getInventory() : null;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<?> any : level.getRecipeManager().getRecipes()) {
                if (!(any.value() instanceof ianm1647.expandeddelight.common.crafting.JuicerRecipe r)) continue;
                var h = any;
                List<Ingredient> inputs = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                ItemStack result = r.getResultItem(level.registryAccess());
                if (inputs.isEmpty() || inputs.size() > INPUTS || result.isEmpty()) continue;
                out.add(new RecipeNeed(h.id(), h, inputs, null, r.getOutputContainer().copy(), result.copy(), r.getJuiceTime(), Kit.remainders(inputs)));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            var inv = inv(level, pos);
            if (inv == null) return false;
            for (int i = 0; i < inv.getSlots(); i++) if (!inv.getStackInSlot(i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return Math.max(1, 64 / need.resultCount());
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var inv = inv(level, pos);
            if (inv == null) return LoadResult.FAILED;
            for (Item item : alloc.perInput()) units = Math.min(units, item.getDefaultMaxStackSize());
            Item container = need.container().isEmpty() ? null : need.container().getItem();
            if (container != null) units = Math.min(units, container.getDefaultMaxStackSize() / need.resultCount());
            if (units <= 0) return LoadResult.FAILED;
            IItemHandler bag = Inv.backpack(maid);
            List<Integer> filled = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < alloc.perInput().size() && ok; i++) {
                ItemStack s = Inv.take(bag, alloc.perInput().get(i), units);
                if (s.getCount() < units) {
                    Inv.giveOrDrop(maid, s);
                    ok = false;
                } else {
                    inv.setStackInSlot(i, s);
                    filled.add(i);
                }
            }
            if (ok && container != null) {
                ItemStack c = Inv.take(bag, container, units * need.resultCount());
                if (c.getCount() < units * need.resultCount()) {
                    Inv.giveOrDrop(maid, c);
                    ok = false;
                } else {
                    inv.setStackInSlot(CONTAINER, c);
                    filled.add(CONTAINER);
                }
            }
            if (!ok) {
                for (int slot : filled) {
                    Inv.giveOrDrop(maid, inv.getStackInSlot(slot).copy());
                    inv.setStackInSlot(slot, ItemStack.EMPTY);
                }
                return LoadResult.FAILED;
            }
            return new LoadResult(units, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            int got = Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
            var inv = inv(level, pos);
            if (inv == null) return got;
            got += MiscStations.give(maid, inv.extractItem(OUT, 64, false), need.resultItem(), collected);
            if (finished(level, pos)) MiscStations.give(maid, inv.extractItem(CONTAINER, 64, false), need.resultItem(), collected);
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            var inv = inv(level, pos);
            if (inv == null) return true;
            for (int i = 0; i < INPUTS; i++) if (!inv.getStackInSlot(i).isEmpty()) return false;
            return inv.getStackInSlot(MEAL).isEmpty();
        }
    }

    // ---------------------------------------------------------------- Cooking for Blockheads oven (furnace recipes, 9 at a time)
    static final class CfbOven implements Station {
        private static final int IN = 0, IN_N = 3, FUEL = 3, OUT = 4, OUT_N = 3, PROC = 7, PROC_N = 9;

        @Override
        public String name() {
            return "oven (Cooking for Blockheads)";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof net.blay09.mods.cookingforblockheads.block.entity.OvenBlockEntity;
        }

        @Nullable
        private static Container box(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof net.blay09.mods.cookingforblockheads.block.entity.OvenBlockEntity o ? o.getInternalContainer() : null;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<? extends AbstractCookingRecipe> h : level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
                AbstractCookingRecipe r = h.value();
                ItemStack result = r.getResultItem(level.registryAccess());
                // only food goes in an oven
                if (r.getIngredients().isEmpty() || r.getIngredients().get(0).isEmpty() || result.isEmpty()
                        || result.get(net.minecraft.core.component.DataComponents.FOOD) == null) continue;
                out.add(new RecipeNeed(h.id(), h, List.of(r.getIngredients().get(0)), null, ItemStack.EMPTY, result.copy(), r.getCookingTime(), Set.of()));
            }
            return out;
        }

        private static boolean cooking(ServerLevel level, ItemStack s) {
            return !s.isEmpty() && level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(s), level).isPresent();
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            Container c = box(level, pos);
            if (c == null) return false;
            for (int i = 0; i < IN_N; i++) if (!c.getItem(IN + i).isEmpty()) return false;
            for (int i = 0; i < OUT_N; i++) if (!c.getItem(OUT + i).isEmpty()) return false;
            for (int i = 0; i < PROC_N; i++) if (!c.getItem(PROC + i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 64;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            Container c = box(level, pos);
            if (c == null) return LoadResult.FAILED;
            Item item = alloc.perInput().get(0);
            ItemStack s = Inv.take(Inv.backpack(maid), item, Math.min(units, item.getDefaultMaxStackSize()));
            if (s.isEmpty()) return LoadResult.FAILED;
            c.setItem(IN, s);
            c.setChanged();
            return new LoadResult(s.getCount(), 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            Container c = box(level, pos);
            if (c == null) return 0;
            int got = 0;
            for (int i = 0; i < OUT_N; i++) {
                ItemStack s = c.getItem(OUT + i);
                if (s.isEmpty()) continue;
                c.setItem(OUT + i, ItemStack.EMPTY);
                got += MiscStations.give(maid, s.copy(), need.resultItem(), collected);
            }
            // cooked food waiting in the grid (the oven moves it out over time) can be taken straight away
            for (int i = 0; i < PROC_N; i++) {
                ItemStack s = c.getItem(PROC + i);
                if (s.isEmpty() || cooking(level, s)) continue;
                c.setItem(PROC + i, ItemStack.EMPTY);
                got += MiscStations.give(maid, s.copy(), need.resultItem(), collected);
            }
            c.setChanged();
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            Container c = box(level, pos);
            if (c == null) return true;
            for (int i = 0; i < IN_N; i++) if (!c.getItem(IN + i).isEmpty()) return false;
            for (int i = 0; i < PROC_N; i++) if (cooking(level, c.getItem(PROC + i))) return false;
            return true;
        }

        @Override
        public boolean usesFuel() {
            return true;
        }

        @Override
        public int storedFuelTicks(ServerLevel level, BlockPos pos) {
            Container c = box(level, pos);
            if (c == null) return 0;
            ItemStack f = c.getItem(FUEL);
            return f.isEmpty() ? 0 : f.getCount() * f.getBurnTime(RecipeType.SMELTING);
        }

        @Override
        public void addFuel(ServerLevel level, EntityMaid maid, BlockPos pos, Item fuel, int count) {
            Container c = box(level, pos);
            if (c == null || count <= 0) return;
            ItemStack slot = c.getItem(FUEL);
            if (!slot.isEmpty() && !slot.is(fuel)) return;
            ItemStack add = Inv.take(Inv.backpack(maid), fuel, Math.min(fuel.getDefaultMaxStackSize() - slot.getCount(), count));
            if (add.isEmpty()) return;
            if (slot.isEmpty()) c.setItem(FUEL, add);
            else slot.grow(add.getCount());
            c.setChanged();
        }
    }
}
