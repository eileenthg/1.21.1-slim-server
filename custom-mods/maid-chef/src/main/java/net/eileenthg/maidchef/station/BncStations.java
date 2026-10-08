package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import umpaz.brewinandchewin.common.block.entity.KegBlockEntity;
import umpaz.brewinandchewin.common.crafting.FluidIngredientWithAmount;
import umpaz.brewinandchewin.common.crafting.KegFermentingRecipe;
import umpaz.brewinandchewin.common.crafting.KegPouringRecipe;
import umpaz.brewinandchewin.common.registry.BnCRecipeTypes;
import umpaz.brewinandchewin.common.utility.AbstractedFluidStack;
import umpaz.brewinandchewin.common.utility.FluidUnit;

/** Brewin' and Chewin'. Only class-loaded when it is installed. */
public final class BncStations {
    private BncStations() {
    }

    public static void addTo(List<Station> list) {
        list.add(new Keg());
    }

    /**
     * The keg: up to 4 ingredients plus a base fluid (water from a bucket, milk from bottles), fermenting at the temperature
     * the recipe wants (the keg's own temperature comes from the biome and nearby blocks, so she only picks recipes that fit).
     * A drink comes out as a fluid; she pours it into the drink's containers (tankards, bottles) she brought along.
     * Turning one keg drink into another (vodka into a bloody mary) needs the first drink left in the keg, so those are left out.
     */
    static final class Keg implements Station {
        private static final int INPUTS = 4;
        /** Per recipe: item inputs, carried fluid items per unit, mB per carried item. */
        private final Map<ResourceLocation, int[]> layouts = new ConcurrentHashMap<>();

        @Override
        public String name() {
            return "keg";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof KegBlockEntity;
        }

        @Nullable
        private static KegPouringRecipe pouring(ServerLevel level, AbstractedFluidStack fluid) {
            for (RecipeHolder<KegPouringRecipe> h : level.getRecipeManager().getAllRecipesFor(BnCRecipeTypes.KEG_POURING)) {
                KegPouringRecipe p = h.value();
                if (p.hasSpecialFluid() || p.getRawFluid().isEmpty()) continue;
                if (p.getRawFluid().fluid() == fluid.fluid() && p.getLoaderAmount() > 0 && !p.getOutput().isEmpty()) return p;
            }
            return null;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            // what each poured drink holds, so drinks can go back in as a base (vodka into a bloody mary)
            for (RecipeHolder<KegPouringRecipe> h : level.getRecipeManager().getAllRecipesFor(BnCRecipeTypes.KEG_POURING)) {
                KegPouringRecipe p = h.value();
                if (p.hasSpecialFluid() || p.getRawFluid().isEmpty() || p.getOutput().isEmpty() || p.getLoaderAmount() <= 0) continue;
                FluidItems.learn(p.getOutput().getItem(), new FluidStack(p.getRawFluid().fluid(), (int) p.getLoaderAmount()), p.getContainer());
            }
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<KegFermentingRecipe> h : level.getRecipeManager().getAllRecipesFor(BnCRecipeTypes.FERMENTING)) {
                KegFermentingRecipe r = h.value();
                if (r.isIncomplete()) continue;
                List<Ingredient> items = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                if (items.size() > INPUTS) continue;
                List<FluidItems.Carry> carries = new ArrayList<>();
                if (r.getFluidIngredient().isPresent()) {
                    FluidIngredientWithAmount fi = r.getFluidIngredient().get();
                    Fluid sample = fi.ingredient().displayStacks().isEmpty() ? null : fi.ingredient().displayStacks().get(0).fluid();
                    FluidItems.Carry c = FluidItems.carry(fs -> fi.ingredient().matches(new AbstractedFluidStack(fs.getFluid(), fs.getAmount())),
                            sample, (int) fi.loaderAmount());
                    // the base is another keg drink (a conversion): can't be carried in
                    if (c == null) continue;
                    carries.add(c);
                }
                List<Ingredient> inputs = Kit.withCarries(items, carries);
                if (inputs == null || inputs.isEmpty()) continue;
                ItemStack result;
                ItemStack container = ItemStack.EMPTY;
                if (r.getResult().right().isPresent()) {
                    result = r.getResult().right().get().copy();
                } else {
                    AbstractedFluidStack fluid = r.getResult().left().get();
                    KegPouringRecipe p = pouring(level, fluid);
                    if (p == null) continue;
                    long mb = FluidUnit.convertToLoader(fluid.amount(), fluid.unit());
                    int pours = (int) (mb / p.getLoaderAmount());
                    if (pours <= 0) continue;
                    result = p.getOutput().copyWithCount(pours * Math.max(1, p.getOutput().getCount()));
                    ItemStack cont = p.getContainer();
                    if (!cont.isEmpty()) container = cont.copyWithCount(1);
                }
                if (result.isEmpty()) continue;
                layouts.put(h.id(), new int[]{items.size(), carries.isEmpty() ? 0 : carries.get(0).count(), carries.isEmpty() ? 0 : carries.get(0).each()});
                Set<Item> extras = new HashSet<>(Kit.remainders(items));
                extras.add(Items.BUCKET);
                extras.add(Items.GLASS_BOTTLE);
                out.add(new RecipeNeed(h.id(), h, inputs, null, container, result, r.getFermentTime(), extras));
            }
            return out;
        }

        @Override
        public boolean accepts(ServerLevel level, BlockPos pos, RecipeNeed need) {
            if (!(level.getBlockEntity(pos) instanceof KegBlockEntity keg) || !(need.holder() != null && need.holder().value() instanceof KegFermentingRecipe r)) {
                return true;
            }
            return KegBlockEntity.isValidTemp(keg.getTemperature(), r.getTemperature());
        }

        @Nullable
        private static KegBlockEntity keg(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof KegBlockEntity k ? k : null;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            var k = keg(level, pos);
            if (k == null || !k.getFluidTank().isEmpty()) return false;
            var h = k.getInventory();
            for (int i = 0; i < h.getSlotCount(); i++) if (!h.getStackInSlot(i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            int[] l = layouts.get(need.id());
            if (!(need.holder() != null && need.holder().value() instanceof KegFermentingRecipe r)) return 1;
            if (r.getResult().left().isPresent()) {
                // a drink: one batch at a time, unless the base fluid scales it (more water, more beer)
                if (!r.isFluidConversion() || l == null || l[1] == 0) return 1;
                long result = FluidUnit.convertToLoader(r.getResult().left().get().amount(), r.getResult().left().get().unit());
                long base = (long) l[1] * l[2];
                return (int) Math.max(1, Math.min(KegBlockEntity.FLUID_CAPACITY / Math.max(1, base), KegBlockEntity.FLUID_CAPACITY / Math.max(1, result)));
            }
            int cap = Math.max(1, need.result().getMaxStackSize() / need.resultCount());
            if (l != null && l[1] > 0) cap = (int) Math.min(cap, KegBlockEntity.FLUID_CAPACITY / ((long) l[1] * l[2]));
            return Math.max(1, cap);
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var k = keg(level, pos);
            int[] l = layouts.get(need.id());
            if (k == null || l == null) return LoadResult.FAILED;
            units = Math.min(units, loadCap(need));
            for (int i = 0; i < l[0]; i++) units = Math.min(units, alloc.perInput().get(i).getDefaultMaxStackSize());
            IItemHandler bag = Inv.backpack(maid);
            var h = k.getInventory();
            List<Integer> filled = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < l[0] && ok; i++) {
                ItemStack s = Inv.take(bag, alloc.perInput().get(i), units);
                if (s.getCount() < units) {
                    Inv.giveOrDrop(maid, s);
                    ok = false;
                } else {
                    h.setStackInSlot(i, s);
                    filled.add(i);
                }
            }
            // pour the base fluid in
            for (int u = 0; u < units && ok; u++) {
                for (int j = 0; j < l[1] && ok; j++) {
                    ItemStack one = Inv.take(bag, alloc.perInput().get(l[0] + j), 1);
                    FluidStack f = FluidItems.contained(one);
                    if (one.isEmpty() || f.isEmpty()) {
                        Inv.giveOrDrop(maid, one);
                        ok = false;
                        break;
                    }
                    k.getFluidTank().fill(new AbstractedFluidStack(f.getFluid(), f.getAmount(), DataComponentMap.EMPTY, FluidUnit.MILLIBUCKET), false);
                    ItemStack empty = FluidItems.emptied(one);
                    collected.merge(empty.getItem(), empty.getCount(), Integer::sum);
                    Inv.giveOrDrop(maid, empty);
                }
            }
            if (!ok) {
                for (int slot : filled) {
                    Inv.giveOrDrop(maid, h.getStackInSlot(slot).copy());
                    h.setStackInSlot(slot, ItemStack.EMPTY);
                }
                k.getFluidTank().setAbstractedFluid(AbstractedFluidStack.EMPTY);
                return LoadResult.FAILED;
            }
            k.setChanged();
            return new LoadResult(units, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            var k = keg(level, pos);
            if (k == null) return 0;
            var h = k.getInventory();
            int got = 0;
            ItemStack out = h.extractItem(5, 64, false);
            if (!out.isEmpty()) {
                collected.merge(out.getItem(), out.getCount(), Integer::sum);
                if (out.is(need.resultItem())) got += out.getCount();
                Inv.giveOrDrop(maid, out);
            }
            if (finished(level, pos) && !k.getFluidTank().isEmpty()) {
                // the drink is ready: pour it into the containers she brought
                if (need.container().isEmpty()) {
                    // no container needed? still bottle what's there through the pouring recipe's empty hand: nothing to do
                    return got;
                }
                IItemHandler bag = Inv.backpack(maid);
                for (int guard = 0; guard < 16 && !k.getFluidTank().isEmpty(); guard++) {
                    ItemStack cont = Inv.take(bag, need.container().getItem(), 16);
                    if (cont.isEmpty()) break;
                    List<ItemStack> outs = k.extractInWorld(cont, cont.getCount(), false);
                    boolean poured = false;
                    for (ItemStack s : outs) {
                        if (s.isEmpty()) continue;
                        if (s.is(need.resultItem())) {
                            got += s.getCount();
                            poured = true;
                        }
                        collected.merge(s.getItem(), s.getCount(), Integer::sum);
                        Inv.giveOrDrop(maid, s);
                    }
                    if (!poured) break;
                }
            }
            got += Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            var k = keg(level, pos);
            if (k == null) return true;
            if (k.isFermenting()) return false;
            var h = k.getInventory();
            for (int i = 0; i < INPUTS; i++) if (!h.getStackInSlot(i).isEmpty()) return false;
            return true;
        }
    }
}
