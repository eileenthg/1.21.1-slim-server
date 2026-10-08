package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.lance5057.extradelight.ExtraDelightBlocks;
import com.lance5057.extradelight.ExtraDelightRecipes;
import com.lance5057.extradelight.workstations.evaporator.EvaporatorBlockEntity;
import com.lance5057.extradelight.workstations.evaporator.recipes.EvaporatorRecipe;
import com.lance5057.extradelight.workstations.vat.VatBlockEntity;
import com.lance5057.extradelight.workstations.vat.recipes.VatRecipe;
import com.lance5057.extradelight.workstations.vat.recipes.VatRecipeWrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** Extra Delight's slow stations: the vat (pickles, sauces, over in-game days) and the salt evaporator. */
final class EdSlowStations {
    private EdSlowStations() {
    }

    private static int give(EntityMaid maid, ItemStack out, Item result, Map<Item, Integer> collected) {
        if (out.isEmpty()) return 0;
        collected.merge(out.getItem(), out.getCount(), Integer::sum);
        int got = out.is(result) ? out.getCount() : 0;
        Inv.giveOrDrop(maid, out);
        return got;
    }

    // ---------------------------------------------------------------- vat
    /**
     * Ingredients, a fluid, the jar (or bottle) the result goes in, then one or two fermenting stages. Each stage wants its own
     * item in the fermenting slot and the lid on or off; she sets up the first stage, leaves, and comes back between stages to
     * add the next item and put the lid on or take it off.
     */
    static final class Vat implements Station {
        private static final int CONTAINER = 6, FERMENT = 7, OUT = 10;
        /** Per recipe: item inputs, carried fluid items, used containers. */
        private final Map<ResourceLocation, int[]> layout = new ConcurrentHashMap<>();

        @Override
        public String name() {
            return "vat";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof VatBlockEntity;
        }

        private static Item lidItem() {
            return ExtraDelightBlocks.LID.get().asItem();
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<VatRecipe> h : level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.VAT.get())) {
                VatRecipe r = h.value();
                List<Ingredient> items = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                if (items.size() > 6 || r.getStageIngredients().isEmpty()) continue;
                ItemStack result = r.getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                List<FluidItems.Carry> carries = new ArrayList<>();
                if (r.getFluid() != null && r.getFluid().amount() > 0) {
                    var fl = r.getFluid().getFluids();
                    FluidItems.Carry c = FluidItems.carry(r.getFluid().ingredient()::test, fl.length > 0 ? fl[0].getFluid() : null, r.getFluid().amount());
                    if (c == null) continue;
                    carries.add(c);
                }
                List<Ingredient> with = Kit.withCarries(items, carries);
                if (with == null) continue;
                List<Ingredient> inputs = new ArrayList<>(with);
                int used = 0;
                if (!r.getUsedItem().isEmpty()) {
                    used = result.getCount();
                    for (int i = 0; i < used; i++) inputs.add(Ingredient.of(r.getUsedItem().getItem()));
                }
                // the first stage's item goes in now; later stages' items are fetched when she comes back
                var first = r.getStageIngredients().get(0);
                if (!first.ingredient.isEmpty()) inputs.add(first.ingredient);
                boolean anyLid = r.getStageIngredients().stream().anyMatch(si -> si.lid);
                int ticks = r.getStageIngredients().stream().mapToInt(si -> si.time).sum();
                layout.put(h.id(), new int[]{items.size(), Kit.carried(new Kit.Layout(items.size(), carries)), used});
                Set<Item> extras = new HashSet<>(Kit.remainders(items));
                extras.add(Items.BUCKET);
                extras.add(Items.GLASS_BOTTLE);
                out.add(new RecipeNeed(h.id(), h, List.copyOf(inputs), anyLid ? Ingredient.of(lidItem()) : null, ItemStack.EMPTY,
                        result.copy(), Math.max(20, ticks), extras));
            }
            return out;
        }

        @Nullable
        private static VatBlockEntity vat(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof VatBlockEntity v ? v : null;
        }

        private static boolean lidOn(ServerLevel level, BlockPos pos) {
            return level.getBlockState(pos.above()).is(ExtraDelightBlocks.LID.get());
        }

        @Nullable
        private static VatRecipe current(ServerLevel level, VatBlockEntity v) {
            return level.getRecipeManager().getRecipeFor(ExtraDelightRecipes.VAT.get(), new VatRecipeWrapper(v.getItemHandler(), v.getFluidTank()), level)
                    .map(RecipeHolder::value).orElse(null);
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            var v = vat(level, pos);
            if (v == null || !FluidItems.isEmpty(v.getFluidTank())) return false;
            var h = v.getItemHandler();
            for (int i = 0; i < h.getSlots(); i++) if (!h.getStackInSlot(i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 1;
        }

        /** Put the lid on or take it off; true if it ends up the way the stage wants. */
        private static boolean setLid(ServerLevel level, BlockPos pos, EntityMaid maid, boolean want, Map<Item, Integer> collected) {
            boolean on = lidOn(level, pos);
            if (on == want) return true;
            if (on) {
                level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
                give(maid, new ItemStack(lidItem()), Items.AIR, collected);
                return true;
            }
            if (!level.getBlockState(pos.above()).isAir()) return false;
            ItemStack lid = Inv.take(Inv.backpack(maid), lidItem(), 1);
            if (lid.isEmpty()) return false;
            level.setBlockAndUpdate(pos.above(), ExtraDelightBlocks.LID.get().defaultBlockState());
            return true;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var v = vat(level, pos);
            int[] l = layout.get(need.id());
            if (v == null || l == null || !(need.holder().value() instanceof VatRecipe r)) return LoadResult.FAILED;
            IItemHandler bag = Inv.backpack(maid);
            IItemHandlerModifiable h = v.getItemHandler();
            List<Integer> filled = new ArrayList<>();
            boolean ok = true;
            int k = 0;
            for (int i = 0; i < l[0] && ok; i++, k++) {
                ItemStack one = Inv.take(bag, alloc.perInput().get(k), 1);
                if (one.isEmpty()) ok = false;
                else {
                    h.setStackInSlot(i, one);
                    filled.add(i);
                }
            }
            if (ok && l[1] > 0) ok = Kit.pourAll(maid, v.getFluidTank(), alloc, k, l[1], collected);
            k += l[1];
            if (ok && l[2] > 0) {
                ItemStack c = Inv.take(bag, alloc.perInput().get(k), l[2]);
                if (c.getCount() < l[2]) {
                    Inv.giveOrDrop(maid, c);
                    ok = false;
                } else {
                    h.setStackInSlot(CONTAINER, c);
                    filled.add(CONTAINER);
                }
            }
            k += l[2];
            var first = r.getStageIngredients().get(0);
            if (ok && !first.ingredient.isEmpty()) {
                ItemStack one = Inv.take(bag, alloc.perInput().get(k), 1);
                if (one.isEmpty()) ok = false;
                else {
                    h.setStackInSlot(FERMENT, one);
                    filled.add(FERMENT);
                }
            }
            if (ok) ok = setLid(level, pos, maid, first.lid, collected);
            if (!ok) {
                for (int slot : filled) {
                    Inv.giveOrDrop(maid, h.getStackInSlot(slot).copy());
                    h.setStackInSlot(slot, ItemStack.EMPTY);
                }
                FluidItems.voidWater(v.getFluidTank());
                return LoadResult.FAILED;
            }
            v.updateInventory();
            return new LoadResult(1, 0);
        }

        /** What the current stage wants and isn't there yet. */
        private static List<Ingredient> missing(ServerLevel level, BlockPos pos, VatBlockEntity v, VatRecipe r) {
            List<Ingredient> out = new ArrayList<>();
            int stage = v.getStage();
            if (stage >= r.getStageIngredients().size()) return out;
            var si = r.getStageIngredients().get(stage);
            ItemStack slot = v.getItemHandler().getStackInSlot(FERMENT);
            if (!si.ingredient.isEmpty() && !si.ingredient.test(slot)) out.add(si.ingredient);
            if (si.lid && !lidOn(level, pos)) out.add(Ingredient.of(lidItem()));
            return out;
        }

        @Override
        public List<Ingredient> attendNeeds(ServerLevel level, BlockPos pos, RecipeNeed need) {
            var v = vat(level, pos);
            if (v == null || !v.getItemHandler().getStackInSlot(OUT).isEmpty()) return List.of();
            VatRecipe r = current(level, v);
            return r == null ? List.of() : missing(level, pos, v, r);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            var v = vat(level, pos);
            if (v == null) return 0;
            var h = v.getItemHandler();
            int got = Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
            ItemStack out = h.getStackInSlot(OUT);
            if (!out.isEmpty()) {
                h.setStackInSlot(OUT, ItemStack.EMPTY);
                got += give(maid, out.copy(), need.resultItem(), collected);
                // done: the lid and any leftovers come back with her
                setLid(level, pos, maid, false, collected);
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (s.isEmpty()) continue;
                    h.setStackInSlot(i, ItemStack.EMPTY);
                    give(maid, s.copy(), need.resultItem(), collected);
                }
                FluidItems.voidWater(v.getFluidTank());
                v.updateInventory();
                return got;
            }
            VatRecipe r = current(level, v);
            if (r == null) return got;
            int stage = v.getStage();
            if (stage < r.getStageIngredients().size()) {
                var si = r.getStageIngredients().get(stage);
                ItemStack slot = h.getStackInSlot(FERMENT);
                if (!si.ingredient.isEmpty() && !si.ingredient.test(slot) && slot.isEmpty()) {
                    ItemStack one = Inv.findLive(maid, si.ingredient::test);
                    if (!one.isEmpty()) h.setStackInSlot(FERMENT, Inv.take(Inv.backpack(maid), one.getItem(), 1));
                }
                setLid(level, pos, maid, si.lid, collected);
                v.updateInventory();
            }
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            var v = vat(level, pos);
            if (v == null || !v.getItemHandler().getStackInSlot(OUT).isEmpty()) return true;
            VatRecipe r = current(level, v);
            if (r == null) return true;
            int stage = v.getStage();
            if (stage >= r.getStageIngredients().size()) return false;
            var si = r.getStageIngredients().get(stage);
            boolean itemOk = si.ingredient.isEmpty() ? v.getItemHandler().getStackInSlot(FERMENT).isEmpty()
                    : si.ingredient.test(v.getItemHandler().getStackInSlot(FERMENT));
            return !itemOk || si.lid != lidOn(level, pos);
        }
    }

    // ---------------------------------------------------------------- evaporator (water -> salt, by the sun)
    static final class Evaporator implements Station {
        @Override
        public String name() {
            return "evaporator";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof EvaporatorBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<EvaporatorRecipe> h : level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.EVAPORATOR.get())) {
                EvaporatorRecipe r = h.value();
                ItemStack result = r.getResultItem();
                if (result.isEmpty() || r.getFluid() == null) continue;
                var fl = r.getFluid().getFluids();
                FluidItems.Carry c = FluidItems.carry(r.getFluid().ingredient()::test, fl.length > 0 ? fl[0].getFluid() : null, r.getFluid().amount());
                if (c == null) continue;
                List<Ingredient> inputs = Kit.withCarries(List.of(), List.of(c));
                if (inputs == null) continue;
                out.add(new RecipeNeed(h.id(), h, inputs, null, ItemStack.EMPTY, result.copy(), r.getCookTime(), Set.of(Items.BUCKET, result.getItem())));
            }
            return out;
        }

        @Override
        public boolean accepts(ServerLevel level, BlockPos pos, RecipeNeed need) {
            // it only dries in the warmth
            return level.getBiome(pos).value().getBaseTemperature() > 0.05f;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof EvaporatorBlockEntity e && e.getFluidTank().isEmpty() && e.isInventoryEmpty();
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 1;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof EvaporatorBlockEntity e)) return LoadResult.FAILED;
            if (!Kit.pourAll(maid, e.getFluidTank(), alloc, 0, need.inputs().size(), collected)) {
                e.getFluidTank().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                return LoadResult.FAILED;
            }
            e.updateInventory();
            return new LoadResult(1, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof EvaporatorBlockEntity e)) return 0;
            var h = e.getItemHandler();
            int got = 0;
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty()) continue;
                h.setStackInSlot(i, ItemStack.EMPTY);
                got += give(maid, s.copy(), need.resultItem(), collected);
            }
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            return !(level.getBlockEntity(pos) instanceof EvaporatorBlockEntity e) || e.getFluidTank().isEmpty();
        }
    }
}
