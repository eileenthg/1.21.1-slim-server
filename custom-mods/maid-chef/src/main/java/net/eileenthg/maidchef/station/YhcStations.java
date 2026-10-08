package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import dev.xkmc.youkaishomecoming.content.item.fluid.IYHFluidItem;
import dev.xkmc.youkaishomecoming.content.item.fluid.YHFluidHandler;
import dev.xkmc.youkaishomecoming.content.pot.base.BasePotBlockEntity;
import dev.xkmc.youkaishomecoming.content.pot.kettle.KettleBlockEntity;
import dev.xkmc.youkaishomecoming.content.pot.kettle.KettleRecipe;
import dev.xkmc.youkaishomecoming.content.pot.moka.MokaMakerBlockEntity;
import dev.xkmc.youkaishomecoming.content.pot.moka.MokaRecipe;
import dev.xkmc.youkaishomecoming.content.pot.rack.DryingRackBlockEntity;
import dev.xkmc.youkaishomecoming.content.pot.rack.DryingRackRecipe;
import dev.xkmc.youkaishomecoming.init.registrate.YHBlocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Youkai's Homecoming. Only class-loaded when it is installed. */
public final class YhcStations {
    private YhcStations() {
    }

    public static void addTo(List<Station> list) {
        list.add(new DryingRack());
        list.add(new Moka());
        list.add(new Kettle());
        YhcMore.addTo(list);
    }

    static int give(EntityMaid maid, ItemStack out, Item result, Map<Item, Integer> collected) {
        if (out.isEmpty()) return 0;
        collected.merge(out.getItem(), out.getCount(), Integer::sum);
        int got = out.is(result) ? out.getCount() : 0;
        Inv.giveOrDrop(maid, out);
        return got;
    }

    /** The drink item a YHC fluid pours into, how much one holds, and its empty container; null if none. */
    @Nullable
    static IYHFluidItem drinkOf(FluidStack fluid) {
        return YHFluidHandler.of(fluid) instanceof IYHFluidItem d ? d : null;
    }

    // ---------------------------------------------------------------- drying rack (sun-dried)
    /** Four hooks; dries only under open sky, by day, out of the rain. Dried items drop off for her to pick up. */
    static final class DryingRack implements Station {
        @Override
        public String name() {
            return "drying rack (Youkai's Homecoming)";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof DryingRackBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<DryingRackRecipe> h : level.getRecipeManager().getAllRecipesFor(YHBlocks.RACK_RT.get())) {
                DryingRackRecipe r = h.value();
                List<Ingredient> inputs = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                ItemStack result = r.getResultItem(level.registryAccess());
                if (inputs.size() != 1 || result.isEmpty()) continue;
                out.add(new RecipeNeed(h.id(), h, inputs, null, ItemStack.EMPTY, result.copy(), r.getCookingTime(), Set.of(result.getItem())));
            }
            return out;
        }

        @Override
        public boolean accepts(ServerLevel level, BlockPos pos, RecipeNeed need) {
            return level.canSeeSky(pos.above()) && level.isDay() && !level.isRainingAt(pos.above());
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof DryingRackBlockEntity r)) return false;
            ItemStackHandler h = r.getInventory();
            for (int i = 0; i < h.getSlots(); i++) if (!h.getStackInSlot(i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 4;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof DryingRackBlockEntity r) || !(need.holder().value() instanceof DryingRackRecipe rec)) {
                return LoadResult.FAILED;
            }
            int done = 0;
            for (int u = 0; u < Math.min(units, 4); u++) {
                ItemStack one = Inv.take(Inv.backpack(maid), alloc.perInput().get(0), 1);
                if (one.isEmpty()) break;
                if (!r.placeFood(one, rec.getCookingTime())) {
                    Inv.giveOrDrop(maid, one);
                    break;
                }
                done++;
            }
            return done == 0 ? LoadResult.FAILED : new LoadResult(done, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            return Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            return idle(level, pos);
        }
    }

    // ---------------------------------------------------------------- moka pot (a Farmer's Delight-style pot)
    static final class Moka implements Station {
        private static final int INPUTS = 4, MEAL = 4, CONTAINER = 5, OUT = 6;

        @Override
        public String name() {
            return "moka pot";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof MokaMakerBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<MokaRecipe> h : level.getRecipeManager().getAllRecipesFor(YHBlocks.MOKA_RT.get())) {
                MokaRecipe r = h.value();
                List<Ingredient> inputs = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                if (inputs.isEmpty() || inputs.size() > INPUTS) continue;
                ItemStack result = r.getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                out.add(new RecipeNeed(h.id(), h, inputs, null, r.getOutputContainer().copy(), result.copy(), r.getCookTime(), Kit.remainders(inputs)));
            }
            return out;
        }

        @Nullable
        private static BasePotBlockEntity<?> pot(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof MokaMakerBlockEntity m ? m : null;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            var p = pot(level, pos);
            if (p == null || !p.isHeated()) return false;
            ItemStackHandler inv = p.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) if (!inv.getStackInSlot(i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return Math.max(1, 64 / need.resultCount());
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var p = pot(level, pos);
            if (p == null) return LoadResult.FAILED;
            for (Item item : alloc.perInput()) units = Math.min(units, item.getDefaultMaxStackSize());
            Item container = need.container().isEmpty() ? null : need.container().getItem();
            if (container != null) units = Math.min(units, container.getDefaultMaxStackSize() / need.resultCount());
            if (units <= 0) return LoadResult.FAILED;
            ItemStackHandler inv = p.getInventory();
            IItemHandler bag = Inv.backpack(maid);
            List<Integer> filled = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < alloc.perInput().size(); i++) {
                ItemStack stack = Inv.take(bag, alloc.perInput().get(i), units);
                if (stack.getCount() < units) {
                    Inv.giveOrDrop(maid, stack);
                    ok = false;
                    break;
                }
                inv.setStackInSlot(i, stack);
                filled.add(i);
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
            p.setChanged();
            return new LoadResult(units, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            int got = Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
            var p = pot(level, pos);
            if (p == null) return got;
            ItemStackHandler inv = p.getInventory();
            got += give(maid, inv.extractItem(OUT, 64, false), need.resultItem(), collected);
            if (finished(level, pos)) give(maid, inv.extractItem(CONTAINER, 64, false), need.resultItem(), collected);
            p.setChanged();
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            var p = pot(level, pos);
            if (p == null) return true;
            ItemStackHandler inv = p.getInventory();
            for (int i = 0; i < INPUTS; i++) if (!inv.getStackInSlot(i).isEmpty()) return false;
            return inv.getStackInSlot(MEAL).isEmpty();
        }
    }

    // ---------------------------------------------------------------- kettle (heated: water + leaves -> tea, poured into cups)
    static final class Kettle implements Station {
        @Override
        public String name() {
            return "kettle (Youkai's Homecoming)";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof KettleBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<KettleRecipe> h : level.getRecipeManager().getAllRecipesFor(YHBlocks.KETTLE_RT.get())) {
                KettleRecipe r = h.value();
                List<Ingredient> items = r.input.stream().filter(i -> !i.isEmpty()).toList();
                if (items.isEmpty() || items.size() > 4 || r.result.isEmpty()) continue;
                IYHFluidItem drink = drinkOf(r.result);
                if (drink == null || drink.amount() <= 0) continue;
                int cups = r.result.getAmount() / drink.amount();
                if (cups <= 0) continue;
                ItemStack one = drink.asStack(1);
                FluidItems.learn(one.getItem(), r.result.copyWithAmount(drink.amount()), new ItemStack(drink.getContainer()));
                List<Ingredient> inputs = Kit.withCarries(items, List.of(new FluidItems.Carry(Ingredient.of(Items.WATER_BUCKET), 1, 1000, true)));
                Set<Item> extras = new HashSet<>(Kit.remainders(items));
                extras.add(Items.BUCKET);
                out.add(new RecipeNeed(h.id(), h, inputs, null, new ItemStack(drink.getContainer()), one.copyWithCount(cups),
                        r.time + 1000, extras));
            }
            return out;
        }

        @Nullable
        private static KettleBlockEntity kettle(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof KettleBlockEntity k ? k : null;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            var k = kettle(level, pos);
            return k != null && k.isHeated(level, pos) && k.getItemHandler().isEmpty() && k.getFluidHandler().getFluidInTank(0).isEmpty();
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 1;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var k = kettle(level, pos);
            if (k == null) return LoadResult.FAILED;
            int nItems = need.inputs().size() - 1;
            IItemHandler bag = Inv.backpack(maid);
            var items = k.getItemHandler();
            List<Integer> filled = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < nItems && ok; i++) {
                ItemStack one = Inv.take(bag, alloc.perInput().get(i), 1);
                if (one.isEmpty()) ok = false;
                else {
                    items.setItem(i, one);
                    filled.add(i);
                }
            }
            if (ok) ok = Kit.pourAll(maid, k.getFluidHandler(), alloc, nItems, 1, collected);
            if (!ok) {
                for (int slot : filled) {
                    Inv.giveOrDrop(maid, items.getItem(slot).copy());
                    items.setItem(slot, ItemStack.EMPTY);
                }
                k.getFluidHandler().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                return LoadResult.FAILED;
            }
            k.notifyTile();
            return new LoadResult(1, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            var k = kettle(level, pos);
            if (k == null || !finished(level, pos)) return 0;
            IFluidHandler tank = k.getFluidHandler();
            IItemHandler bag = Inv.backpack(maid);
            int got = 0;
            for (int guard = 0; guard < 16; guard++) {
                FluidStack f = tank.getFluidInTank(0);
                IYHFluidItem drink = f.isEmpty() ? null : drinkOf(f);
                if (drink == null || f.getAmount() < drink.amount()) break;
                if (!need.container().isEmpty() && Inv.take(bag, need.container().getItem(), 1).isEmpty()) break;
                tank.drain(drink.amount(), IFluidHandler.FluidAction.EXECUTE);
                got += give(maid, drink.asStack(1), need.resultItem(), collected);
            }
            k.notifyTile();
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            var k = kettle(level, pos);
            if (k == null) return true;
            FluidStack f = k.getFluidHandler().getFluidInTank(0);
            return k.getItemHandler().isEmpty() && (f.isEmpty() || !f.getFluid().is(FluidTags.WATER));
        }
    }
}
