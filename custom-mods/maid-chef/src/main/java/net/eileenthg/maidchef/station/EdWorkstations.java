package net.eileenthg.maidchef.station;

import static net.eileenthg.maidchef.station.Kit.carried;
import static net.eileenthg.maidchef.station.Kit.pourAll;
import static net.eileenthg.maidchef.station.Kit.remainders;
import static net.eileenthg.maidchef.station.Kit.withCarries;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.lance5057.extradelight.ExtraDelightBlocks;
import com.lance5057.extradelight.ExtraDelightComponents;
import com.lance5057.extradelight.ExtraDelightRecipes;
import com.lance5057.extradelight.workstations.chiller.ChillerBlockEntity;
import com.lance5057.extradelight.workstations.chiller.ChillerRecipe;
import com.lance5057.extradelight.workstations.chiller.ChillerRecipeWrapper;
import com.lance5057.extradelight.workstations.dryingrack.DryingRackBlockEntity;
import com.lance5057.extradelight.workstations.juicer.JuicerBlockEntity;
import com.lance5057.extradelight.workstations.meltingpot.MeltingPotBlockEntity;
import com.lance5057.extradelight.workstations.mixingbowl.MixingBowlBlockEntity;
import com.lance5057.extradelight.workstations.mixingbowl.recipes.MixingBowlRecipe;
import com.lance5057.extradelight.workstations.mortar.MortarBlockEntity;
import com.lance5057.extradelight.workstations.mortar.recipes.MortarRecipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.eileenthg.maidchef.ChefBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** Extra Delight's other workstations. Only class-loaded when Extra Delight is installed. */
final class EdWorkstations {
    private EdWorkstations() {
    }

    private static int giveAll(EntityMaid maid, ItemStack out, Item result, Map<Item, Integer> collected) {
        if (out.isEmpty()) return 0;
        collected.merge(out.getItem(), out.getCount(), Integer::sum);
        int got = out.is(result) ? out.getCount() : 0;
        Inv.giveOrDrop(maid, out);
        return got;
    }

    // ---------------------------------------------------------------- drying rack
    /** Up to 8 items, one per hook; each dries into its result on its own. */
    static final class DryingRack implements Station {
        @Override
        public String name() {
            return "drying rack";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof DryingRackBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (var h : level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.DRYING_RACK.get())) {
                var r = h.value();
                ItemStack result = r.getResultItem(level.registryAccess());
                if (result.isEmpty() || r.getInput().isEmpty()) continue;
                out.add(new RecipeNeed(h.id(), h, List.of(r.getInput()), null, ItemStack.EMPTY, result.copy(), r.getCookingTime(), Set.of()));
            }
            return out;
        }

        @Nullable
        private static IItemHandlerModifiable inv(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof DryingRackBlockEntity r ? r.getItemHandler() : null;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            var h = inv(level, pos);
            if (h == null) return false;
            for (int i = 0; i < h.getSlots(); i++) if (!h.getStackInSlot(i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 8;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var h = inv(level, pos);
            if (h == null) return LoadResult.FAILED;
            Item item = alloc.perInput().get(0);
            int done = 0;
            for (int i = 0; i < h.getSlots() && done < units; i++) {
                if (!h.getStackInSlot(i).isEmpty()) continue;
                ItemStack one = Inv.take(Inv.backpack(maid), item, 1);
                if (one.isEmpty()) break;
                ItemStack left = h.insertItem(i, one, false);
                if (!left.isEmpty()) {
                    Inv.giveOrDrop(maid, left);
                    break;
                }
                done++;
            }
            return done == 0 ? LoadResult.FAILED : new LoadResult(done, 0);
        }

        /** Still drying: an item on a hook that has a drying recipe. */
        private static boolean drying(ServerLevel level, ItemStack s) {
            return !s.isEmpty() && level.getRecipeManager().getRecipeFor(ExtraDelightRecipes.DRYING_RACK.get(), new SingleRecipeInput(s), level).isPresent();
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            var h = inv(level, pos);
            if (h == null) return 0;
            int got = 0;
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty() || drying(level, s)) continue;
                got += giveAll(maid, h.extractItem(i, 64, false), need.resultItem(), collected);
            }
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            var h = inv(level, pos);
            if (h == null) return true;
            for (int i = 0; i < h.getSlots(); i++) if (drying(level, h.getStackInSlot(i))) return false;
            return true;
        }
    }

    // ---------------------------------------------------------------- dough shaping (instant, like a stonecutter)
    static final class DoughShaping implements Station {
        @Override
        public String name() {
            return "dough shaping station";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return state.is(ExtraDelightBlocks.DOUGH_SHAPING.get());
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (var h : level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.DOUGH_SHAPING.get())) {
                var r = h.value();
                List<Ingredient> inputs = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                ItemStack result = r.getResultItem(level.registryAccess());
                if (inputs.size() != 1 || result.isEmpty()) continue;
                out.add(new RecipeNeed(h.id(), h, inputs, null, ItemStack.EMPTY, result.copy(), 0, Set.of()));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 64;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            ItemStack in = Inv.take(Inv.backpack(maid), alloc.perInput().get(0), units);
            if (in.isEmpty()) return LoadResult.FAILED;
            int n = in.getCount() * need.resultCount();
            giveAll(maid, need.result().copyWithCount(n), need.resultItem(), collected);
            maid.swing(maid.getUsedItemHand());
            return new LoadResult(in.getCount(), n);
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

    // ---------------------------------------------------------------- mortar (instant, with a pestle)
    static final class Mortar implements Station {
        @Override
        public String name() {
            return "mortar";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof MortarBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            Ingredient pestle = Ingredient.of(ItemTags.create(ResourceLocation.parse("c:pestles")));
            for (var h : level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.MORTAR.get())) {
                MortarRecipe r = h.value();
                List<Ingredient> inputs = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                ItemStack result = r.getResultItem(level.registryAccess());
                if (inputs.size() != 1) continue;
                // ground into a fluid (oils, pastes): bottled afterwards, 250 mB a bottle
                ItemStack main = result;
                ItemStack container = ItemStack.EMPTY;
                Set<Item> extras = new HashSet<>(remainders(inputs));
                if (!r.getFluid().isEmpty()) {
                    if (r.getFluid().getAmount() % FluidItems.BOTTLE != 0) continue;
                    ItemStack bottle = FluidItems.bottleOf(r.getFluid());
                    if (bottle.isEmpty()) continue;
                    if (!result.isEmpty()) extras.add(result.getItem());
                    main = bottle.copyWithCount(r.getFluid().getAmount() / FluidItems.BOTTLE);
                    container = new ItemStack(Items.GLASS_BOTTLE);
                }
                if (main.isEmpty()) continue;
                extras.add(main.getItem());
                out.add(new RecipeNeed(h.id(), h, inputs, pestle, container, main.copy(), 0, extras));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof MortarBlockEntity m && m.getInsertedItem().isEmpty() && m.getFluidTank().isEmpty();
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 64;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof MortarBlockEntity m) || need.tool() == null) return LoadResult.FAILED;
            MortarRecipe r = need.holder() != null && need.holder().value() instanceof MortarRecipe mr ? mr : null;
            int grinds = r != null ? r.getGrinds() : 8;
            boolean fluid = r != null && !r.getFluid().isEmpty();
            // a fluid result: one at a time, so the 1000 mB bowl never overflows before bottling
            int batch = fluid ? 1 : 4;
            Item item = alloc.perInput().get(0);
            IItemHandler bag = Inv.backpack(maid);
            int done = 0, results = 0;
            while (done < units) {
                ItemStack tool = Inv.findLive(maid, need.tool()::test);
                if (tool.isEmpty()) break;
                if (fluid && Inv.count(bag, Items.GLASS_BOTTLE) < need.resultCount()) break;
                ItemStack in = Inv.take(bag, item, Math.min(batch, units - done));
                if (in.isEmpty()) break;
                int n = in.getCount();
                m.getItemHandler().setStackInSlot(0, in);
                FakePlayer fp = Fakes.hands(level, maid, tool.copy());
                for (int g = 0; g < grinds + 2 && !m.getInsertedItem().isEmpty(); g++) m.grind(fp);
                Fakes.clear(fp);
                if (!m.getInsertedItem().isEmpty()) {
                    Inv.giveOrDrop(maid, m.getInsertedItem().copy());
                    m.getItemHandler().setStackInSlot(0, ItemStack.EMPTY);
                    break;
                }
                done += n;
                if (fluid) {
                    for (ItemStack b : FluidItems.bottleUp(m.getFluidTank(), need.resultCount() * n)) {
                        Inv.take(bag, Items.GLASS_BOTTLE, 1);
                        results += giveAll(maid, b, need.resultItem(), collected);
                    }
                }
                results += Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
            }
            if (done > 0) maid.swing(maid.getUsedItemHand());
            return done == 0 ? LoadResult.FAILED : new LoadResult(done, results);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            return Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            return true;
        }
    }

    // ---------------------------------------------------------------- juicer (instant: crank, then bottle the juice)
    static final class Juicer implements Station {
        @Override
        public String name() {
            return "juicer";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof JuicerBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (var h : level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.JUICER.get())) {
                var r = h.value();
                var fluid = r.getFluid();
                if (fluid.isEmpty() || fluid.getAmount() % FluidItems.BOTTLE != 0) continue;
                ItemStack bottle = FluidItems.bottleOf(fluid);
                if (bottle.isEmpty()) continue;
                Set<Item> extras = new HashSet<>();
                ItemStack chance = r.getResultItem(level.registryAccess());
                if (!chance.isEmpty()) extras.add(chance.getItem());
                out.add(new RecipeNeed(h.id(), h, List.of(r.getInput()), null, new ItemStack(Items.GLASS_BOTTLE),
                        bottle.copyWithCount(fluid.getAmount() / FluidItems.BOTTLE), 0, extras));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof JuicerBlockEntity j && j.getInsertedItem().isEmpty() && j.getFluidTank().isEmpty();
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 64;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof JuicerBlockEntity j)) return LoadResult.FAILED;
            Item item = alloc.perInput().get(0);
            IItemHandler bag = Inv.backpack(maid);
            int done = 0, results = 0;
            for (int u = 0; u < units; u++) {
                if (Inv.count(bag, Items.GLASS_BOTTLE) < need.resultCount()) break;
                ItemStack in = Inv.take(bag, item, 1);
                if (in.isEmpty()) break;
                j.getItemHandler().setStackInSlot(0, in);
                FakePlayer fp = Fakes.hands(level, maid, ItemStack.EMPTY);
                for (int g = 0; g < 10 && !j.getInsertedItem().isEmpty(); g++) j.grind(fp);
                Fakes.clear(fp);
                if (!j.getInsertedItem().isEmpty()) {
                    Inv.giveOrDrop(maid, j.getInsertedItem().copy());
                    j.getItemHandler().setStackInSlot(0, ItemStack.EMPTY);
                    break;
                }
                for (ItemStack b : FluidItems.bottleUp(j.getFluidTank(), need.resultCount())) {
                    Inv.take(bag, Items.GLASS_BOTTLE, 1);
                    results += giveAll(maid, b, need.resultItem(), collected);
                }
                Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
                done++;
            }
            if (done > 0) maid.swing(maid.getUsedItemHand());
            return done == 0 ? LoadResult.FAILED : new LoadResult(done, results);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            return Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            return true;
        }
    }

    // ---------------------------------------------------------------- mixing bowl (instant: fill, pour, stir)
    static final class MixingBowl implements Station {
        private static final int CONTAINER = 9;
        private final Map<ResourceLocation, Kit.Layout> layouts = new ConcurrentHashMap<>();

        @Override
        public String name() {
            return "mixing bowl";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof MixingBowlBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (var h : level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.MIXING_BOWL.get())) {
                MixingBowlRecipe r = h.value();
                List<Ingredient> items = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                if (items.size() > 9) continue;
                List<FluidItems.Carry> carries = new ArrayList<>();
                for (var f : r.getFluids()) {
                    var fl = f.getFluids();
                    carries.add(FluidItems.carry(f.ingredient()::test, fl.length > 0 ? fl[0].getFluid() : null, f.amount()));
                }
                List<Ingredient> inputs = withCarries(items, carries);
                ItemStack result = r.getResultItem(level.registryAccess());
                if (inputs == null || inputs.isEmpty() || result.isEmpty()) continue;
                layouts.put(h.id(), new Kit.Layout(items.size(), List.copyOf(carries)));
                ItemStack container = r.getContainer().isEmpty() ? ItemStack.EMPTY : r.getContainer().copyWithCount(1);
                Set<Item> extras = new HashSet<>(remainders(items));
                extras.add(Items.GLASS_BOTTLE);
                extras.add(Items.BUCKET);
                out.add(new RecipeNeed(h.id(), h, inputs, r.getUtensil(), container, result.copy(), 0, extras));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof MixingBowlBlockEntity b)) return false;
            var h = b.getItemHandler();
            for (int i = 0; i <= 11; i++) if (!h.getStackInSlot(i).isEmpty()) return false;
            return FluidItems.isEmpty(b.getFluidTank());
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 64;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof MixingBowlBlockEntity b) || need.tool() == null) return LoadResult.FAILED;
            Kit.Layout l = layouts.getOrDefault(need.id(), Kit.Layout.NONE);
            int stirs = need.holder() != null && need.holder().value() instanceof MixingBowlRecipe r ? r.getStirs() : 4;
            IItemHandler bag = Inv.backpack(maid);
            var h = b.getItemHandler();
            int done = 0, results = 0;
            for (int u = 0; u < units; u++) {
                ItemStack tool = Inv.findLive(maid, need.tool()::test);
                if (tool.isEmpty()) break;
                boolean ok = true;
                for (int i = 0; i < l.items() && ok; i++) {
                    ItemStack one = Inv.take(bag, alloc.perInput().get(i), 1);
                    if (one.isEmpty()) ok = false;
                    else h.setStackInSlot(i, one);
                }
                if (ok && !need.container().isEmpty()) {
                    int k = need.resultCount();
                    ItemStack c = Inv.take(bag, need.container().getItem(), k);
                    if (c.getCount() < k) {
                        Inv.giveOrDrop(maid, c);
                        ok = false;
                    } else {
                        h.setStackInSlot(CONTAINER, c);
                    }
                }
                if (ok) ok = pourAll(maid, b.getFluidTank(), alloc, l.items(), carried(l), collected);
                if (ok) {
                    b.updateInventory();
                    FakePlayer fp = Fakes.hands(level, maid, tool.copy());
                    int got = 0;
                    for (int s = 0; s <= stirs + 1 && got == 0; s++) {
                        b.mix(fp, tool);
                        got += Fakes.drain(fp, maid, need.resultItem(), collected, true);
                    }
                    Fakes.clear(fp);
                    got += Inv.vacuum(level, maid, pos, it -> true, need.resultItem(), collected);
                    b.complete = false;
                    if (got == 0) ok = false;
                    results += got;
                }
                FluidItems.voidWater(b.getFluidTank());
                if (!ok) {
                    // give back what's still in the bowl; fluids already poured are lost
                    for (int i = 0; i <= CONTAINER; i++) {
                        ItemStack s = h.getStackInSlot(i);
                        if (s.isEmpty()) continue;
                        Inv.giveOrDrop(maid, s.copy());
                        h.setStackInSlot(i, ItemStack.EMPTY);
                    }
                    var t = b.getFluidTank();
                    for (int guard = 0; guard < 8 && !FluidItems.isEmpty(t); guard++) t.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                    ChefBehavior.trace(maid, "mixing bowl: " + need.resultItem() + " didn't come out");
                    break;
                }
                done++;
            }
            if (done > 0) maid.swing(maid.getUsedItemHand());
            return done == 0 ? LoadResult.FAILED : new LoadResult(done, results);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            return Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            return true;
        }
    }

    // ---------------------------------------------------------------- melting pot (heated: item -> fluid, bottles itself)
    static final class MeltingPot implements Station {
        private static final int IN = 0, BOTTLES = 1, OUT = 2;

        @Override
        public String name() {
            return "melting pot";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof MeltingPotBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (var h : level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.MELTING_POT.get())) {
                var r = h.value();
                if (r.result.isEmpty() || r.result.getAmount() % FluidItems.BOTTLE != 0) continue;
                ItemStack bottle = FluidItems.bottleOf(r.result);
                if (bottle.isEmpty()) continue;
                out.add(new RecipeNeed(h.id(), h, List.of(r.input), null, new ItemStack(Items.GLASS_BOTTLE),
                        bottle.copyWithCount(r.result.getAmount() / FluidItems.BOTTLE), r.cooktime, Set.of()));
            }
            return out;
        }

        @Nullable
        private static MeltingPotBlockEntity pot(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof MeltingPotBlockEntity p ? p : null;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            var p = pot(level, pos);
            if (p == null || !p.isHeated()) return false;
            var h = p.getItemHandler();
            for (int i = 0; i < h.getSlots(); i++) if (!h.getStackInSlot(i).isEmpty()) return false;
            return p.getFluidTank().isEmpty();
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return Math.max(1, Math.min(64, need.result().getMaxStackSize()) / need.resultCount());
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var p = pot(level, pos);
            if (p == null) return LoadResult.FAILED;
            units = Math.min(units, loadCap(need));
            IItemHandler bag = Inv.backpack(maid);
            int bottles = units * need.resultCount();
            if (Inv.count(bag, Items.GLASS_BOTTLE) < bottles) return LoadResult.FAILED;
            ItemStack in = Inv.take(bag, alloc.perInput().get(0), units);
            if (in.getCount() < units) {
                Inv.giveOrDrop(maid, in);
                return LoadResult.FAILED;
            }
            p.getItemHandler().setStackInSlot(IN, in);
            // she keeps the bottles and fills them herself (in this pack glass bottles carry a fluid capability, which
            // sends the pot's own bottling down a path that never fills them)
            p.updateInventory();
            return new LoadResult(units, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            var p = pot(level, pos);
            if (p == null) return 0;
            var h = p.getItemHandler();
            int got = giveAll(maid, h.extractItem(OUT, 64, false), need.resultItem(), collected);
            IItemHandler bag = Inv.backpack(maid);
            while (p.getFluidTank().getFluidAmount() >= FluidItems.BOTTLE && Inv.count(bag, Items.GLASS_BOTTLE) > 0) {
                List<ItemStack> b = FluidItems.bottleUp(p.getFluidTank(), 1);
                if (b.isEmpty()) break;
                Inv.take(bag, Items.GLASS_BOTTLE, 1);
                got += giveAll(maid, b.get(0), need.resultItem(), collected);
            }
            if (finished(level, pos)) giveAll(maid, h.extractItem(BOTTLES, 64, false), need.resultItem(), collected);
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            var p = pot(level, pos);
            if (p == null) return true;
            return p.getItemHandler().getStackInSlot(IN).isEmpty() && p.getFluidTank().getFluidAmount() < FluidItems.BOTTLE;
        }
    }

    // ---------------------------------------------------------------- chiller (ice-cooled: ingredients + fluid + mold)
    static final class Chiller implements Station {
        private static final int INPUTS = 4, MOLD = 5, OUT = 6, DRIP_OUT = 9, ICE = 10, TANK = 1000;
        /** Per recipe: item inputs, carried bottles per unit, molds used per unit, ice per unit. */
        private final Map<ResourceLocation, int[]> layouts = new ConcurrentHashMap<>();

        @Override
        public String name() {
            return "chiller";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof ChillerBlockEntity;
        }

        /** Ice-like items and how long one keeps the chiller cold. */
        private static Map<Item, Integer> ices() {
            Map<Item, Integer> out = new LinkedHashMap<>();
            for (Item it : BuiltInRegistries.ITEM) {
                var c = it.components().get(ExtraDelightComponents.CHILL.get());
                if (c != null && c.time() > 0) out.put(it, c.time());
            }
            return out;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            Map<Item, Integer> ices = ices();
            if (ices.isEmpty()) return out;
            int minChill = ices.values().stream().min(Integer::compare).orElse(1);
            Ingredient ice = Ingredient.of(ices.keySet().toArray(Item[]::new));
            for (var h : level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.CHILLER.get())) {
                ChillerRecipe r = h.value();
                List<Ingredient> items = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                if (items.size() > INPUTS) continue;
                ItemStack result = r.getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                List<FluidItems.Carry> carries = new ArrayList<>();
                if (!r.getFluid().isEmpty()) {
                    if (r.getFluid().getAmount() > TANK) continue;
                    carries.add(FluidItems.carry(r.getFluid()));
                }
                List<Ingredient> withFluids = withCarries(items, carries);
                if (withFluids == null) continue;
                List<Ingredient> inputs = new ArrayList<>(withFluids);
                ItemStack mold = r.getOutputContainer();
                Ingredient tool = null;
                int moldsUsed = 0;
                if (!mold.isEmpty()) {
                    if (r.shouldConsumeContainer()) {
                        moldsUsed = result.getCount();
                        for (int i = 0; i < moldsUsed; i++) inputs.add(Ingredient.of(mold.getItem()));
                    } else {
                        tool = Ingredient.of(mold.getItem());
                    }
                }
                int icePer = Math.max(1, (r.getCookTime() + minChill - 1) / minChill);
                for (int i = 0; i < icePer; i++) inputs.add(ice);
                layouts.put(h.id(), new int[]{items.size(), carries.isEmpty() ? 0 : carries.get(0).count(), moldsUsed, icePer});
                Set<Item> extras = new HashSet<>(remainders(items));
                extras.add(Items.GLASS_BOTTLE);
                extras.add(Items.BUCKET);
                out.add(new RecipeNeed(h.id(), h, List.copyOf(inputs), tool, ItemStack.EMPTY, result.copy(), r.getCookTime(), extras));
            }
            return out;
        }

        @Nullable
        private static ChillerBlockEntity chiller(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof ChillerBlockEntity c ? c : null;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            var c = chiller(level, pos);
            if (c == null) return false;
            var h = c.getInventory();
            for (int i = 0; i < h.getSlots(); i++) if (i != DRIP_OUT && !h.getStackInSlot(i).isEmpty()) return false;
            return FluidItems.isEmpty(c.getFluidTank());
        }

        @Override
        public int loadCap(RecipeNeed need) {
            int cap = Math.max(1, Math.min(64, need.result().getMaxStackSize()) / need.resultCount());
            if (need.holder() != null && need.holder().value() instanceof ChillerRecipe r && !r.getFluid().isEmpty()) {
                cap = Math.min(cap, Math.max(1, TANK / Math.max(1, r.getFluid().getAmount())));
            }
            return cap;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var c = chiller(level, pos);
            int[] l = layouts.get(need.id());
            if (c == null || l == null) return LoadResult.FAILED;
            units = Math.min(units, loadCap(need));
            for (Item item : alloc.perInput()) units = Math.min(units, item.getDefaultMaxStackSize());
            IItemHandler bag = Inv.backpack(maid);
            var h = c.getInventory();
            int nItems = l[0], nCarry = l[1], nMolds = l[2], nIce = l[3];
            List<Integer> filled = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < nItems && ok; i++) {
                ItemStack s = Inv.take(bag, alloc.perInput().get(i), units);
                if (s.getCount() < units) {
                    Inv.giveOrDrop(maid, s);
                    ok = false;
                } else {
                    h.setStackInSlot(i, s);
                    filled.add(i);
                }
            }
            int idx = nItems;
            for (int u = 0; u < units && ok && nCarry > 0; u++) ok = pourAll(maid, c.getFluidTank(), alloc, idx, nCarry, collected);
            idx += nCarry;
            if (ok) {
                Item moldItem = null;
                int moldCount = 0;
                if (nMolds > 0) {
                    moldItem = alloc.perInput().get(idx);
                    moldCount = units * nMolds;
                } else if (need.tool() != null) {
                    ItemStack t = Inv.findLive(maid, need.tool()::test);
                    if (t.isEmpty()) ok = false;
                    else {
                        moldItem = t.getItem();
                        moldCount = need.holder() != null && need.holder().value() instanceof ChillerRecipe r ? Math.max(1, r.getOutputContainer().getCount()) : 1;
                    }
                }
                if (ok && moldItem != null) {
                    ItemStack m = Inv.take(bag, moldItem, moldCount);
                    if (m.getCount() < moldCount) {
                        Inv.giveOrDrop(maid, m);
                        ok = false;
                    } else {
                        h.setStackInSlot(MOLD, m);
                        filled.add(MOLD);
                    }
                }
            }
            idx += nMolds;
            if (ok && nIce > 0) {
                ItemStack s = Inv.take(bag, alloc.perInput().get(idx), units * nIce);
                if (s.isEmpty()) ok = false;
                else {
                    h.setStackInSlot(ICE, s);
                    filled.add(ICE);
                }
            }
            if (!ok) {
                for (int slot : filled) {
                    Inv.giveOrDrop(maid, h.getStackInSlot(slot).copy());
                    h.setStackInSlot(slot, ItemStack.EMPTY);
                }
                FluidItems.voidWater(c.getFluidTank());
                return LoadResult.FAILED;
            }
            c.updateInventory();
            return new LoadResult(units, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            var c = chiller(level, pos);
            if (c == null) return 0;
            // melted ice just goes away (a full drip tray would stop the chilling)
            c.getDripTray().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            var h = c.getInventory();
            int got = giveAll(maid, h.extractItem(OUT, 64, false), need.resultItem(), collected);
            got += Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
            if (finished(level, pos)) {
                giveAll(maid, h.extractItem(MOLD, 64, false), need.resultItem(), collected);
                giveAll(maid, h.extractItem(ICE, 64, false), need.resultItem(), collected);
                FluidItems.voidWater(c.getFluidTank());
            }
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            var c = chiller(level, pos);
            if (c == null) return true;
            return level.getRecipeManager().getRecipeFor(ExtraDelightRecipes.CHILLER.get(),
                    new ChillerRecipeWrapper(c.getInventory(), c.getFluidTank().getFluid()), level).isEmpty();
        }
    }
}
