package net.eileenthg.maidchef.station;

import com.farmersrespite.common.block.entity.KettleBlockEntity;
import com.farmersrespite.common.crafting.KettleRecipe;
import com.farmersrespite.core.registry.FRRecipeTypes;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

/** Farmer's Respite. Only class-loaded when it is installed. */
public final class FrStations {
    private FrStations() {
    }

    public static void addTo(List<Station> list) {
        list.add(new Kettle());
    }

    /**
     * The kettle (heated): a bucket of water plus up to two ingredients brews a pot of tea, which she pours out into cups,
     * 250 mB each. Brews that start from another drink (cider into cider with milk) are left out.
     */
    static final class Kettle implements Station {
        private static final int INPUTS = 2, CONTAINER = 2, OUT = 3;

        @Override
        public String name() {
            return "kettle";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof KettleBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            // each brewed drink's cup holds 250 mB of it, so drinks can be brewed further (tea into tea with milk)
            for (RecipeHolder<KettleRecipe> h : level.getRecipeManager().getAllRecipesFor(FRRecipeTypes.BREWING.get())) {
                FluidStack made = h.value().getResultFluid();
                if (made.isEmpty()) continue;
                Item drink = KettleBlockEntity.getItemForFluid(made.getFluid());
                Item cup = KettleBlockEntity.getEmptyContainerForFluid(made.getFluid());
                if (drink != null && drink != Items.AIR) {
                    FluidItems.learn(drink, new FluidStack(made.getFluid(), KettleBlockEntity.BOTTLE_AMOUNT), cup == null ? ItemStack.EMPTY : new ItemStack(cup));
                }
            }
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<KettleRecipe> h : level.getRecipeManager().getAllRecipesFor(FRRecipeTypes.BREWING.get())) {
                KettleRecipe r = h.value();
                List<Ingredient> items = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                if (items.isEmpty() || items.size() > INPUTS) continue;
                FluidStack base = r.getInputFluid(), made = r.getResultFluid();
                if (base.isEmpty() || made.isEmpty() || made.getAmount() < KettleBlockEntity.BOTTLE_AMOUNT) continue;
                FluidItems.Carry c = FluidItems.carry(r::matchesFluid, base.getFluid(), base.getAmount());
                if (c == null) continue;
                Item drink = KettleBlockEntity.getItemForFluid(made.getFluid());
                Item cup = KettleBlockEntity.getEmptyContainerForFluid(made.getFluid());
                if (drink == null || drink == Items.AIR) continue;
                List<Ingredient> inputs = Kit.withCarries(items, List.of(c));
                if (inputs == null) continue;
                Set<Item> extras = new HashSet<>(Kit.remainders(items));
                extras.add(Items.BUCKET);
                extras.add(Items.GLASS_BOTTLE);
                ItemStack container = cup == null || cup == Items.AIR ? ItemStack.EMPTY : new ItemStack(cup);
                out.add(new RecipeNeed(h.id(), h, inputs, null, container,
                        new ItemStack(drink, made.getAmount() / KettleBlockEntity.BOTTLE_AMOUNT), r.getBrewTime(), extras));
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
            if (k == null || !k.isHeated(level, pos) || !k.getFluidTank().isEmpty()) return false;
            var h = k.getInventory();
            for (int i = 0; i < h.getSlots(); i++) if (!h.getStackInSlot(i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            // the kettle holds one bucket: one brew at a time
            return 1;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var k = kettle(level, pos);
            if (k == null) return LoadResult.FAILED;
            int nItems = need.inputs().size() - 1;
            IItemHandler bag = Inv.backpack(maid);
            var h = k.getInventory();
            List<Integer> filled = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < nItems && ok; i++) {
                ItemStack one = Inv.take(bag, alloc.perInput().get(i), 1);
                if (one.isEmpty()) ok = false;
                else {
                    h.setStackInSlot(i, one);
                    filled.add(i);
                }
            }
            if (ok) ok = Kit.pourAll(maid, k.getFluidTank(), alloc, nItems, need.inputs().size() - nItems, collected);
            if (!ok) {
                for (int slot : filled) {
                    Inv.giveOrDrop(maid, h.getStackInSlot(slot).copy());
                    h.setStackInSlot(slot, ItemStack.EMPTY);
                }
                k.getFluidTank().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                return LoadResult.FAILED;
            }
            k.setChanged();
            return new LoadResult(1, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            var k = kettle(level, pos);
            if (k == null) return 0;
            int got = 0;
            ItemStack out = k.getInventory().extractItem(OUT, 64, false);
            if (!out.isEmpty()) {
                collected.merge(out.getItem(), out.getCount(), Integer::sum);
                if (out.is(need.resultItem())) got += out.getCount();
                Inv.giveOrDrop(maid, out);
            }
            if (finished(level, pos)) {
                for (int i = 0; i < INPUTS; i++) {
                    ItemStack left = k.getInventory().extractItem(i, 64, false);
                    if (left.isEmpty()) continue;
                    collected.merge(left.getItem(), left.getCount(), Integer::sum);
                    Inv.giveOrDrop(maid, left);
                }
                // pour the tea into the cups she brought
                IItemHandler bag = Inv.backpack(maid);
                var tank = k.getFluidTank();
                while (tank.getFluidAmount() >= KettleBlockEntity.BOTTLE_AMOUNT && tank.getFluid().getFluid() != net.minecraft.world.level.material.Fluids.WATER) {
                    Item drink = KettleBlockEntity.getItemForFluid(tank.getFluid().getFluid());
                    if (drink == null || drink == Items.AIR) break;
                    if (!need.container().isEmpty() && Inv.take(bag, need.container().getItem(), 1).isEmpty()) break;
                    tank.drain(KettleBlockEntity.BOTTLE_AMOUNT, IFluidHandler.FluidAction.EXECUTE);
                    ItemStack d = new ItemStack(drink);
                    collected.merge(drink, 1, Integer::sum);
                    if (d.is(need.resultItem())) got++;
                    Inv.giveOrDrop(maid, d);
                }
            }
            return got + Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            var k = kettle(level, pos);
            if (k == null) return true;
            var h = k.getInventory();
            for (int i = 0; i < INPUTS; i++) {
                ItemStack s = h.getStackInSlot(i);
                // an ingredient's leftover (the bottle from a milk bottle) stays in its slot
                if (!s.isEmpty() && !s.is(Items.GLASS_BOTTLE) && !s.is(Items.BUCKET) && !s.is(Items.BOWL)) return false;
            }
            return true;
        }
    }
}
