package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.dafuqs.spectrum.api.recipe.IngredientStack;
import de.dafuqs.spectrum.blocks.potion_workshop.PotionWorkshopBlockEntity;
import de.dafuqs.spectrum.blocks.titration_barrel.TitrationBarrelBlock;
import de.dafuqs.spectrum.blocks.titration_barrel.TitrationBarrelBlockEntity;
import de.dafuqs.spectrum.recipe.potion_workshop.PotionWorkshopCraftingRecipe;
import de.dafuqs.spectrum.recipe.titration_barrel.ITitrationBarrelRecipe;
import de.dafuqs.spectrum.recipe.titration_barrel.TitrationBarrelRecipe;
import de.dafuqs.spectrum.registries.SpectrumItems;
import de.dafuqs.spectrum.registries.SpectrumRecipeTypes;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Spectrum. Only class-loaded when it is installed. */
public final class SpectrumStations {
    private SpectrumStations() {
    }

    public static void addTo(List<Station> list) {
        list.add(new Workshop());
        list.add(new Barrel());
    }

    @Nullable
    private static ServerPlayer owner(ServerLevel level, EntityMaid maid) {
        return maid.getOwnerUUID() == null ? null : level.getServer().getPlayerList().getPlayer(maid.getOwnerUUID());
    }

    /** One planner input per item an IngredientStack wants (3 sugar = three "sugar" inputs). */
    private static void addStack(List<Ingredient> inputs, List<Integer> slots, IngredientStack st, int slot) {
        if (st == null || st.isEmpty()) return;
        for (int i = 0; i < Math.max(1, st.getCount()); i++) {
            inputs.add(st.getIngredient());
            slots.add(slot);
        }
    }

    // ---------------------------------------------------------------- potion workshop (crafting recipes: teas, chocolate...)
    /**
     * Teas, hot chocolate, karak chai, junket... A Mermaid's Gem, the base (usually a glass bottle) and up to three ingredients;
     * the workshop works by itself and only for recipes its owner (who placed it) has unlocked, while they're online.
     */
    static final class Workshop implements Station {
        /** Per recipe: the workshop slot each planner input goes into. */
        private final Map<ResourceLocation, List<Integer>> slotsOf = new ConcurrentHashMap<>();

        @Override
        public String name() {
            return "potion workshop";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof PotionWorkshopBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<PotionWorkshopCraftingRecipe> h : level.getRecipeManager().getAllRecipesFor(SpectrumRecipeTypes.POTION_WORKSHOP_CRAFTING)) {
                PotionWorkshopCraftingRecipe r = h.value();
                if (r.getRequiredExperience() > 0) continue;
                ItemStack result = r.getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                List<Ingredient> inputs = new ArrayList<>();
                List<Integer> slots = new ArrayList<>();
                inputs.add(Ingredient.of(SpectrumItems.MERMAIDS_GEM.get()));
                slots.add(PotionWorkshopBlockEntity.MERMAIDS_GEM_INPUT_SLOT_ID);
                Ingredient tool = null;
                if (r.consumesBaseIngredient()) addStack(inputs, slots, r.getBaseIngredient(), PotionWorkshopBlockEntity.BASE_INPUT_SLOT_ID);
                else if (!r.getBaseIngredient().isEmpty()) tool = r.getBaseIngredient().getIngredient();
                List<IngredientStack> others = r.getOtherIngredients();
                for (int i = 0; i < others.size(); i++) addStack(inputs, slots, others.get(i), PotionWorkshopBlockEntity.FIRST_INGREDIENT_SLOT + i);
                slotsOf.put(h.id(), List.copyOf(slots));
                Set<Item> extras = new HashSet<>(Kit.remainders(inputs));
                extras.add(result.getItem());
                out.add(new RecipeNeed(h.id(), h, List.copyOf(inputs), tool, ItemStack.EMPTY, result.copy(), Math.max(20, r.getCraftingTime()), extras));
            }
            return out;
        }

        @Override
        public boolean accepts(ServerLevel level, BlockPos pos, RecipeNeed need) {
            if (!(level.getBlockEntity(pos) instanceof PotionWorkshopBlockEntity w) || need.holder() == null) return false;
            var owner = w.getOwnerIfOnline(level);
            return owner != null && need.holder().value() instanceof de.dafuqs.spectrum.recipe.potion_workshop.PotionWorkshopRecipe r
                    && r.canPlayerCraft(owner);
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof PotionWorkshopBlockEntity w)) return false;
            for (int i = 0; i < PotionWorkshopBlockEntity.INVENTORY_SIZE; i++) if (!w.getItem(i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            // twelve output slots
            return Math.max(1, 12 * need.result().getMaxStackSize() / Math.max(1, need.resultCount()) / 4);
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof PotionWorkshopBlockEntity w)) return LoadResult.FAILED;
            List<Integer> slots = slotsOf.get(need.id());
            if (slots == null) return LoadResult.FAILED;
            // how many of each item go in each slot per craft
            Map<Integer, Map<Item, Integer>> per = new java.util.TreeMap<>();
            for (int i = 0; i < slots.size(); i++) per.computeIfAbsent(slots.get(i), k -> new java.util.LinkedHashMap<>()).merge(alloc.perInput().get(i), 1, Integer::sum);
            for (var slot : per.values()) for (var e : slot.entrySet()) units = Math.min(units, e.getKey().getDefaultMaxStackSize() / e.getValue());
            IItemHandler bag = Inv.backpack(maid);
            List<Integer> filled = new ArrayList<>();
            boolean ok = units > 0;
            for (var e : per.entrySet()) {
                if (!ok) break;
                if (e.getValue().size() != 1) {
                    ok = false;
                    break;
                }
                var only = e.getValue().entrySet().iterator().next();
                int n = only.getValue() * units;
                ItemStack s = Inv.take(bag, only.getKey(), n);
                if (s.getCount() < n) {
                    Inv.giveOrDrop(maid, s);
                    ok = false;
                } else {
                    w.setItem(e.getKey(), s);
                    filled.add(e.getKey());
                }
            }
            if (ok && !need.container().isEmpty()) {
                // a container for the result goes in the base slot
                int n = units * need.resultCount();
                ItemStack b = Inv.take(bag, need.container().getItem(), n);
                if (b.getCount() < n) {
                    Inv.giveOrDrop(maid, b);
                    ok = false;
                } else {
                    w.setItem(PotionWorkshopBlockEntity.BASE_INPUT_SLOT_ID, b);
                    filled.add(PotionWorkshopBlockEntity.BASE_INPUT_SLOT_ID);
                }
            }
            if (ok && need.tool() != null) {
                ItemStack t = Inv.findLive(maid, need.tool()::test);
                if (t.isEmpty()) ok = false;
                else {
                    w.setItem(PotionWorkshopBlockEntity.BASE_INPUT_SLOT_ID, Inv.take(bag, t.getItem(), 1));
                    filled.add(PotionWorkshopBlockEntity.BASE_INPUT_SLOT_ID);
                }
            }
            if (!ok) {
                for (int slot : filled) {
                    Inv.giveOrDrop(maid, w.getItem(slot).copy());
                    w.setItem(slot, ItemStack.EMPTY);
                }
                return LoadResult.FAILED;
            }
            w.inventoryChanged();
            w.setChanged();
            return new LoadResult(units, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof PotionWorkshopBlockEntity w)) return 0;
            int got = 0;
            for (int i = PotionWorkshopBlockEntity.FIRST_INVENTORY_SLOT; i < PotionWorkshopBlockEntity.INVENTORY_SIZE; i++) {
                ItemStack s = w.getItem(i);
                if (s.isEmpty()) continue;
                w.setItem(i, ItemStack.EMPTY);
                got += YhcLike.give(maid, s.copy(), need.resultItem(), collected);
            }
            if (finished(level, pos)) {
                for (int i = 0; i < PotionWorkshopBlockEntity.FIRST_INVENTORY_SLOT; i++) {
                    ItemStack s = w.getItem(i);
                    if (s.isEmpty()) continue;
                    w.setItem(i, ItemStack.EMPTY);
                    YhcLike.give(maid, s.copy(), need.resultItem(), collected);
                }
            }
            w.setChanged();
            return got + Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            return !(level.getBlockEntity(pos) instanceof PotionWorkshopBlockEntity w) || PotionWorkshopBlockEntity.calculateRecipe(level, w) == null;
        }
    }

    // ---------------------------------------------------------------- titration barrel (sealed for real hours, then tapped)
    /**
     * Fill it (items plus water or another fluid), seal it, and come back once it has aged long enough (the recipe's time is in
     * real hours); then unseal it and tap it, a glass bottle per drink. Tapping needs the owner online with the recipe unlocked.
     */
    static final class Barrel implements Station {
        private final Map<ResourceLocation, int[]> layout = new ConcurrentHashMap<>();

        @Override
        public String name() {
            return "titration barrel";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof TitrationBarrelBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<ITitrationBarrelRecipe> h : level.getRecipeManager().getAllRecipesFor(SpectrumRecipeTypes.TITRATION_BARREL)) {
                if (!(h.value() instanceof TitrationBarrelRecipe r)) continue;
                List<IngredientStack> stacks = r.getIngredientStacks();
                if (stacks.isEmpty() || stacks.size() > 5) continue;
                List<Ingredient> inputs = new ArrayList<>();
                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < stacks.size(); i++) addStack(inputs, slots, stacks.get(i), i);
                int itemInputs = inputs.size();
                int carried = 0;
                if (r.fluid != null && !r.fluid.isEmpty()) {
                    FluidStack[] fl = r.fluid.getStacks();
                    FluidItems.Carry c = FluidItems.carry(r.fluid::test, fl.length > 0 ? fl[0].getFluid() : null, FluidItems.BUCKET);
                    if (c == null) continue;
                    for (int i = 0; i < c.count(); i++) inputs.add(c.items());
                    carried = c.count();
                }
                ItemStack result = r.getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                layout.put(h.id(), new int[]{itemInputs, carried});
                ItemStack container = r.getTappingItem() == null || r.getTappingItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(r.getTappingItem());
                Set<Item> extras = new HashSet<>(Kit.remainders(inputs));
                extras.add(Items.BUCKET);
                // real hours, as ticks (for "this takes long, leave it")
                int ticks = (int) Math.min(Integer.MAX_VALUE / 2, (long) Math.max(1, r.getMinFermentationTimeHours()) * 72000L);
                out.add(new RecipeNeed(h.id(), h, List.copyOf(inputs), null, container, result.copy(), ticks, extras));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof TitrationBarrelBlockEntity b && b.isEmpty() && b.getFluidTank().isEmpty()
                    && level.getBlockState(pos).getValue(TitrationBarrelBlock.BARREL_STATE) == TitrationBarrelBlock.BarrelState.EMPTY;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 1;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof TitrationBarrelBlockEntity b)) return LoadResult.FAILED;
            int[] l = layout.get(need.id());
            if (l == null || !(need.holder().value() instanceof TitrationBarrelRecipe r)) return LoadResult.FAILED;
            IItemHandler bag = Inv.backpack(maid);
            List<IngredientStack> stacks = r.getIngredientStacks();
            boolean ok = true;
            int k = 0;
            for (int i = 0; i < stacks.size() && ok; i++) {
                int n = Math.max(1, stacks.get(i).getCount());
                Item item = alloc.perInput().get(k);
                k += n;
                ItemStack s = Inv.take(bag, item, n);
                if (s.getCount() < n) {
                    Inv.giveOrDrop(maid, s);
                    ok = false;
                } else {
                    b.getItems().set(i, s);
                }
            }
            if (ok && l[1] > 0) ok = Kit.pourAll(maid, b.getFluidTank(), alloc, l[0], l[1], collected);
            if (!ok) {
                for (int i = 0; i < b.getItems().size(); i++) {
                    if (!b.getItems().get(i).isEmpty()) Inv.giveOrDrop(maid, b.getItems().get(i).copy());
                    b.getItems().set(i, ItemStack.EMPTY);
                }
                b.getFluidTank().setFluid(FluidStack.EMPTY);
                b.inventoryChanged();
                return LoadResult.FAILED;
            }
            b.inventoryChanged();
            TitrationBarrelBlock.sealBarrel(level, pos, level.getBlockState(pos), b, null);
            return new LoadResult(1, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof TitrationBarrelBlockEntity b) || !finished(level, pos)) return 0;
            ServerPlayer owner = owner(level, maid);
            if (owner == null) return 0;
            BlockState state = level.getBlockState(pos);
            if (state.getValue(TitrationBarrelBlock.BARREL_STATE) == TitrationBarrelBlock.BarrelState.SEALED) {
                b.tap();
                level.setBlockAndUpdate(pos, state.setValue(TitrationBarrelBlock.BARREL_STATE, TitrationBarrelBlock.BarrelState.TAPPED));
            }
            IItemHandler bag = Inv.backpack(maid);
            int got = 0;
            for (int guard = 0; guard < 64; guard++) {
                state = level.getBlockState(pos);
                if (state.getValue(TitrationBarrelBlock.BARREL_STATE) != TitrationBarrelBlock.BarrelState.TAPPED) break;
                ItemStack hand = need.container().isEmpty() ? ItemStack.EMPTY : Inv.take(bag, need.container().getItem(), 1);
                if (!need.container().isEmpty() && hand.isEmpty()) break;
                ItemStack drink = b.tryHarvest(level, pos, state, hand, owner);
                if (!hand.isEmpty()) Inv.giveOrDrop(maid, hand);
                if (drink.isEmpty()) break;
                got += YhcLike.give(maid, drink, need.resultItem(), collected);
            }
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof TitrationBarrelBlockEntity b)) return true;
            var st = level.getBlockState(pos).getValue(TitrationBarrelBlock.BARREL_STATE);
            if (st == TitrationBarrelBlock.BarrelState.TAPPED || st == TitrationBarrelBlock.BarrelState.EMPTY) return true;
            if (st != TitrationBarrelBlock.BarrelState.SEALED) return false;
            return b.getRecipeForInventory(level).map(h -> h.value().isFermentingLongEnoughToTap(b.getSealSeconds())).orElse(true);
        }
    }

    /** Small shared helper. */
    static final class YhcLike {
        static int give(EntityMaid maid, ItemStack out, Item result, Map<Item, Integer> collected) {
            if (out.isEmpty()) return 0;
            collected.merge(out.getItem(), out.getCount(), Integer::sum);
            int got = out.is(result) ? out.getCount() : 0;
            Inv.giveOrDrop(maid, out);
            return got;
        }
    }
}
