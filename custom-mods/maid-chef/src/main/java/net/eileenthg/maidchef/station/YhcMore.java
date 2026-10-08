package net.eileenthg.maidchef.station;

import static net.eileenthg.maidchef.station.YhcStations.drinkOf;
import static net.eileenthg.maidchef.station.YhcStations.give;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import dev.xkmc.youkaishomecoming.content.block.food.PotFoodBlock;
import dev.xkmc.youkaishomecoming.content.item.fluid.IYHFluidItem;
import dev.xkmc.youkaishomecoming.content.pot.basin.BasinBlockEntity;
import dev.xkmc.youkaishomecoming.content.pot.basin.BasinRecipe;
import dev.xkmc.youkaishomecoming.content.pot.basin.SimpleBasinRecipe;
import dev.xkmc.youkaishomecoming.content.pot.cooking.core.CookingBlockEntity;
import dev.xkmc.youkaishomecoming.content.pot.cooking.core.PotCookingRecipe;
import dev.xkmc.youkaishomecoming.content.pot.ferment.FermentationRecipe;
import dev.xkmc.youkaishomecoming.content.pot.ferment.FermentationTankBlock;
import dev.xkmc.youkaishomecoming.content.pot.ferment.FermentationTankBlockEntity;
import dev.xkmc.youkaishomecoming.content.pot.ferment.SimpleFermentationRecipe;
import dev.xkmc.youkaishomecoming.content.pot.steamer.SteamerBlockEntity;
import dev.xkmc.youkaishomecoming.content.pot.steamer.SteamingRecipe;
import dev.xkmc.youkaishomecoming.init.registrate.YHBlocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** The rest of Youkai's Homecoming: fermentation tank, grape basin, cooking pots, steamer. */
final class YhcMore {
    private YhcMore() {
    }

    static void addTo(List<Station> list) {
        list.add(new Ferment());
        list.add(new Basin());
        list.add(new Cooking());
        list.add(new Steamer());
        list.add(new Cuisine());
    }

    /** Pour out a YHC drink fluid into the containers she carries. */
    private static int bottleOut(EntityMaid maid, IFluidHandler tank, RecipeNeed need, Map<Item, Integer> collected) {
        IItemHandler bag = Inv.backpack(maid);
        int got = 0;
        for (int guard = 0; guard < 32; guard++) {
            FluidStack f = tank.getFluidInTank(0);
            IYHFluidItem drink = f.isEmpty() ? null : drinkOf(f);
            if (drink == null || drink.amount() <= 0 || f.getAmount() < drink.amount()) break;
            Item cont = drink.getContainer();
            if (cont != null && cont != Items.AIR && Inv.take(bag, cont, 1).isEmpty()) break;
            tank.drain(drink.amount(), IFluidHandler.FluidAction.EXECUTE);
            got += give(maid, drink.asStack(1), need.resultItem(), collected);
        }
        return got;
    }

    @Nullable
    private static FluidItems.Carry carryYh(FluidStack fluid) {
        IYHFluidItem d = drinkOf(fluid);
        if (d != null && d.amount() > 0) FluidItems.learn(d.asStack(1).getItem(), fluid.copyWithAmount(d.amount()), new ItemStack(d.getContainer()));
        return FluidItems.carry(fluid);
    }

    // ---------------------------------------------------------------- fermentation tank (lid closed while it works)
    static final class Ferment implements Station {
        private final Map<ResourceLocation, Integer> itemCounts = new ConcurrentHashMap<>();

        @Override
        public String name() {
            return "fermentation tank";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof FermentationTankBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<FermentationRecipe<?>> h : level.getRecipeManager().getAllRecipesFor(YHBlocks.FERMENT_RT.get())) {
                if (!(h.value() instanceof SimpleFermentationRecipe r) || r.inputFluidTag != null) continue;
                List<Ingredient> items = r.ingredients.stream().filter(i -> !i.isEmpty()).toList();
                List<FluidItems.Carry> carries = new ArrayList<>();
                if (!r.inputFluid.isEmpty()) {
                    FluidItems.Carry c = carryYh(r.inputFluid);
                    if (c == null) continue;
                    carries.add(c);
                }
                ItemStack result;
                ItemStack container = ItemStack.EMPTY;
                Set<Item> extras = new HashSet<>(Kit.remainders(items));
                if (!r.outputFluid.isEmpty()) {
                    IYHFluidItem d = drinkOf(r.outputFluid);
                    if (d == null || d.amount() <= 0 || r.outputFluid.getAmount() < d.amount()) continue;
                    result = d.asStack(r.outputFluid.getAmount() / d.amount());
                    FluidItems.learn(result.getItem(), r.outputFluid.copyWithAmount(d.amount()), new ItemStack(d.getContainer()));
                    if (d.getContainer() != null && d.getContainer() != Items.AIR) container = new ItemStack(d.getContainer());
                    for (ItemStack s : r.results) extras.add(s.getItem());
                } else if (!r.results.isEmpty()) {
                    result = r.results.get(0).copy();
                    for (ItemStack s : r.results) extras.add(s.getItem());
                } else {
                    continue;
                }
                List<Ingredient> inputs = Kit.withCarries(items, carries);
                if (inputs == null || inputs.isEmpty() || result.isEmpty()) continue;
                itemCounts.put(h.id(), items.size());
                extras.add(Items.BUCKET);
                extras.add(Items.GLASS_BOTTLE);
                out.add(new RecipeNeed(h.id(), h, inputs, null, container, result, r.time, extras));
            }
            return out;
        }

        @Nullable
        private static FermentationTankBlockEntity tank(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof FermentationTankBlockEntity t ? t : null;
        }

        private static boolean open(BlockState state) {
            return state.hasProperty(FermentationTankBlock.OPEN) && state.getValue(FermentationTankBlock.OPEN);
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            var t = tank(level, pos);
            return t != null && t.items.isEmpty() && t.fluids.getFluidInTank(0).isEmpty();
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 1;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var t = tank(level, pos);
            if (t == null) return LoadResult.FAILED;
            int nItems = itemCounts.getOrDefault(need.id(), 0);
            IItemHandler bag = Inv.backpack(maid);
            List<Integer> filled = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < nItems && ok; i++) {
                ItemStack one = Inv.take(bag, alloc.perInput().get(i), 1);
                if (one.isEmpty()) ok = false;
                else {
                    t.items.setItem(i, one);
                    filled.add(i);
                }
            }
            if (ok) ok = Kit.pourAll(maid, t.fluids, alloc, nItems, need.inputs().size() - nItems, collected);
            if (!ok) {
                for (int slot : filled) {
                    Inv.giveOrDrop(maid, t.items.getItem(slot).copy());
                    t.items.setItem(slot, ItemStack.EMPTY);
                }
                t.fluids.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                return LoadResult.FAILED;
            }
            // close the lid: it only ferments shut, and pops open when done
            BlockState s = level.getBlockState(pos);
            if (open(s)) level.setBlockAndUpdate(pos, s.setValue(FermentationTankBlock.OPEN, false));
            t.notifyTile();
            return new LoadResult(1, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            var t = tank(level, pos);
            if (t == null || !finished(level, pos)) return 0;
            int got = 0;
            for (int i = 0; i < t.items.getContainerSize(); i++) {
                ItemStack s = t.items.getItem(i);
                if (s.isEmpty()) continue;
                t.items.setItem(i, ItemStack.EMPTY);
                got += give(maid, s.copy(), need.resultItem(), collected);
            }
            got += bottleOut(maid, t.fluids, need, collected);
            t.notifyTile();
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            return tank(level, pos) == null || open(level.getBlockState(pos));
        }
    }

    // ---------------------------------------------------------------- grape basin (instant: squash, then bottle)
    static final class Basin implements Station {
        @Override
        public String name() {
            return "basin";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof BasinBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<BasinRecipe<?>> h : level.getRecipeManager().getAllRecipesFor(YHBlocks.BASIN_RT.get())) {
                if (!(h.value() instanceof SimpleBasinRecipe r) || r.output.isEmpty() || r.input.isEmpty()) continue;
                IYHFluidItem d = drinkOf(r.output);
                if (d == null || d.amount() <= 0 || d.amount() % r.output.getAmount() != 0) continue;
                int per = d.amount() / r.output.getAmount();
                if (per > 16) continue;
                ItemStack bottle = d.asStack(1);
                FluidItems.learn(bottle.getItem(), r.output.copyWithAmount(d.amount()), new ItemStack(d.getContainer()));
                List<Ingredient> inputs = new ArrayList<>();
                for (int i = 0; i < per; i++) inputs.add(r.input);
                ItemStack container = d.getContainer() == null || d.getContainer() == Items.AIR ? ItemStack.EMPTY : new ItemStack(d.getContainer());
                out.add(new RecipeNeed(h.id(), h, List.copyOf(inputs), null, container, bottle, 0, Set.of()));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof BasinBlockEntity b && b.items.isEmpty() && b.fluids.getFluidInTank(0).isEmpty();
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 64;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof BasinBlockEntity b)) return LoadResult.FAILED;
            IItemHandler bag = Inv.backpack(maid);
            int per = need.inputs().size(), done = 0, results = 0;
            Item fruit = alloc.perInput().get(0);
            for (int u = 0; u < units; u++) {
                if (Inv.count(bag, fruit) < per) break;
                if (!need.container().isEmpty() && Inv.count(bag, need.container().getItem()) < 1) break;
                boolean ok = true;
                for (int k = 0; k < per && ok; k++) {
                    b.items.setItem(0, Inv.take(bag, fruit, 1));
                    b.process();
                    if (!b.items.getItem(0).isEmpty()) {
                        Inv.giveOrDrop(maid, b.items.getItem(0).copy());
                        b.items.setItem(0, ItemStack.EMPTY);
                        ok = false;
                    }
                }
                results += bottleOut(maid, b.fluids, need, collected);
                if (!ok) break;
                done++;
            }
            b.notifyTile();
            if (done > 0) maid.swing(maid.getUsedItemHand());
            return done == 0 ? LoadResult.FAILED : new LoadResult(done, results);
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

    // ---------------------------------------------------------------- cooking pots (heated; the pot becomes a pot of soup)
    /**
     * Small, middle and large pots. A finished soup turns the pot into a "pot of ..." block; she serves it out into bowls
     * right there, which leaves the pot for the next soup.
     */
    static final class Cooking implements Station {
        private final Map<ResourceLocation, Item> potFor = new ConcurrentHashMap<>();

        @Override
        public String name() {
            return "cooking pot (Youkai's Homecoming)";
        }

        /** The empty pot block (what's left after serving) and the cooking pot it becomes once it has water in it. */
        @Nullable
        private static net.minecraft.world.level.block.Block cookingFor(BlockState state) {
            if (state.is(YHBlocks.IRON_BOWL.get())) return YHBlocks.SMALL_POT.get();
            if (state.is(YHBlocks.IRON_POT.get())) return YHBlocks.SHORT_POT.get();
            if (state.is(YHBlocks.STOCKPOT.get())) return YHBlocks.LARGE_POT.get();
            return null;
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof CookingBlockEntity || cookingFor(state) != null;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<PotCookingRecipe<?>> h : level.getRecipeManager().getAllRecipesFor(YHBlocks.COOKING_RT.get())) {
                PotCookingRecipe<?> r = h.value();
                List<Ingredient> inputs = r.getInput().stream().filter(i -> !i.isEmpty()).toList();
                ItemStack made = r.getResult();
                if (inputs.isEmpty() || inputs.size() > 12 || made.isEmpty() || !made.hasCraftingRemainingItem()) continue;
                potFor.put(h.id(), made.getCraftingRemainingItem().getItem());
                ItemStack result = made.copy();
                ItemStack container = ItemStack.EMPTY;
                if (made.getItem() instanceof BlockItem bi && bi.getBlock() instanceof PotFoodBlock pf) {
                    result = pf.asBowls();
                    if (result.hasCraftingRemainingItem()) container = result.getCraftingRemainingItem().copyWithCount(1);
                }
                if (result.isEmpty()) continue;
                Set<Item> extras = new HashSet<>(Kit.remainders(inputs));
                extras.add(result.getItem());
                // water to fill an empty pot with: she brings a bucket and keeps it (water is free)
                out.add(new RecipeNeed(h.id(), h, inputs, Ingredient.of(Items.WATER_BUCKET), container, result, r.getProcessTime(), extras));
            }
            return out;
        }

        @Override
        public boolean accepts(ServerLevel level, BlockPos pos, RecipeNeed need) {
            Item pot = potFor.get(need.id());
            if (pot == null) return true;
            if (level.getBlockEntity(pos) instanceof CookingBlockEntity c) return c.container() == pot;
            return level.getBlockState(pos).getBlock().asItem() == pot;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            if (!CookingBlockEntity.isHeatedPos(level, pos)) return false;
            if (level.getBlockEntity(pos) instanceof CookingBlockEntity c) return c.items.isEmpty();
            return cookingFor(level.getBlockState(pos)) != null;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 1;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            BlockState empty = level.getBlockState(pos);
            var cooking = cookingFor(empty);
            if (cooking != null) {
                // pour water in: the empty pot becomes a cooking pot
                if (Inv.findLive(maid, s -> s.is(Items.WATER_BUCKET)).isEmpty()) return LoadResult.FAILED;
                BlockState pot = cooking.defaultBlockState();
                for (Property<?> p : empty.getProperties()) {
                    if (p.getName().equals("facing") && pot.hasProperty(p)) pot = copy(empty, pot, p);
                }
                level.setBlockAndUpdate(pos, pot);
            }
            if (!(level.getBlockEntity(pos) instanceof CookingBlockEntity c)) return LoadResult.FAILED;
            IItemHandler bag = Inv.backpack(maid);
            List<ItemStack> in = new ArrayList<>();
            boolean ok = true;
            for (Item item : alloc.perInput()) {
                ItemStack one = Inv.take(bag, item, 1);
                if (one.isEmpty()) {
                    ok = false;
                    break;
                }
                in.add(one);
                c.items.addItem(one);
            }
            if (!ok) {
                for (int i = 0; i < c.items.getContainerSize(); i++) {
                    if (!c.items.getItem(i).isEmpty()) Inv.giveOrDrop(maid, c.items.getItem(i).copy());
                }
                c.items.clearContent();
                return LoadResult.FAILED;
            }
            c.notifyTile();
            return new LoadResult(1, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            int got = Inv.vacuum(level, maid, pos.above(), need.extras()::contains, need.resultItem(), collected);
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PotFoodBlock)) return got;
            // serve the pot of soup into bowls
            IItemHandler bag = Inv.backpack(maid);
            Item bowl = need.container().isEmpty() ? null : need.container().getItem();
            FakePlayer fp = Fakes.hands(level, maid, ItemStack.EMPTY);
            for (int guard = 0; guard < 16 && level.getBlockState(pos).getBlock() instanceof PotFoodBlock; guard++) {
                ItemStack held = bowl == null ? ItemStack.EMPTY : Inv.take(bag, bowl, 1);
                if (bowl != null && held.isEmpty()) break;
                fp.setItemInHand(InteractionHand.MAIN_HAND, held);
                level.getBlockState(pos).useItemOn(held, level, fp, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                got += Fakes.drain(fp, maid, need.resultItem(), collected, false);
            }
            Fakes.clear(fp);
            got += Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            return !(level.getBlockEntity(pos) instanceof CookingBlockEntity c) || c.items.isEmpty();
        }

        private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> p) {
            return to.setValue(p, from.getValue(p));
        }
    }

    // ---------------------------------------------------------------- steamer (pot of water + racks)
    static final class Steamer implements Station {
        @Override
        public String name() {
            return "steamer";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof SteamerBlockEntity && state.is(YHBlocks.STEAMER_POT.get());
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<SteamingRecipe> h : level.getRecipeManager().getAllRecipesFor(YHBlocks.STEAM_RT.get())) {
                SteamingRecipe r = h.value();
                List<Ingredient> inputs = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                ItemStack result = r.getResultItem(level.registryAccess());
                if (inputs.size() != 1 || result.isEmpty()) continue;
                out.add(new RecipeNeed(h.id(), h, inputs, null, ItemStack.EMPTY, result.copy(), r.getCookingTime(), Set.of()));
            }
            return out;
        }

        private static boolean hasWater(BlockState state) {
            for (Property<?> p : state.getProperties()) {
                if (p.getName().equals("water") && p instanceof BooleanProperty b) return state.getValue(b);
            }
            return true;
        }

        @Nullable
        private static IItemHandlerModifiable racks(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof SteamerBlockEntity s ? s.getItemCap(null) : null;
        }

        private static boolean steaming(ServerLevel level, ItemStack s) {
            return !s.isEmpty() && level.getRecipeManager().getRecipeFor(YHBlocks.STEAM_RT.get(), new SingleRecipeInput(s), level).isPresent();
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof SteamerBlockEntity s) || !s.isHeated(level, pos) || !hasWater(level.getBlockState(pos))) return false;
            var h = racks(level, pos);
            if (h == null || h.getSlots() == 0) return false;
            for (int i = 0; i < h.getSlots(); i++) if (!h.getStackInSlot(i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 8;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            var h = racks(level, pos);
            if (h == null) return LoadResult.FAILED;
            int done = 0;
            for (int i = 0; i < h.getSlots() && done < units; i++) {
                if (!h.getStackInSlot(i).isEmpty()) continue;
                ItemStack one = Inv.take(Inv.backpack(maid), alloc.perInput().get(0), 1);
                if (one.isEmpty()) break;
                ItemStack left = h.insertItem(i, one, false);
                if (!left.isEmpty()) Inv.giveOrDrop(maid, left);
                else done++;
            }
            return done == 0 ? LoadResult.FAILED : new LoadResult(done, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            var h = racks(level, pos);
            if (h == null) return 0;
            int got = 0;
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty() || steaming(level, s)) continue;
                got += give(maid, h.extractItem(i, 64, false), need.resultItem(), collected);
            }
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            var h = racks(level, pos);
            if (h == null) return true;
            for (int i = 0; i < h.getSlots(); i++) if (steaming(level, h.getStackInSlot(i))) return false;
            return true;
        }
    }

    // ---------------------------------------------------------------- cuisine board (instant: build the dish step by step)
    /**
     * Sushi, rolls and the like: the dish is built on the board in order (its base first, a roll of the mat where the base
     * needs one, then the recipe's own ingredients), and the finished dish taken off.
     */
    static final class Cuisine implements Station {
        /** Per recipe: the steps in order; null = an empty-handed click (rolling the mat). */
        private final Map<ResourceLocation, List<Ingredient>> steps = new ConcurrentHashMap<>();

        @Override
        public String name() {
            return "cuisine board";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof dev.xkmc.youkaishomecoming.content.pot.table.board.CuisineBoardBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<dev.xkmc.youkaishomecoming.content.pot.table.recipe.CuisineRecipe<?>> h
                    : level.getRecipeManager().getAllRecipesFor(YHBlocks.CUISINE_RT.get())) {
                var r = h.value();
                ItemStack result = r.getResult();
                if (result.isEmpty()) continue;
                List<Ingredient> base = new ArrayList<>(), clicks = new ArrayList<>();
                var variant = dev.xkmc.youkaishomecoming.content.pot.table.item.VariantTableItemBase.MAP.get(r.base());
                var fixed = dev.xkmc.youkaishomecoming.content.pot.table.item.IngredientTableItem.FIXED.get(r.base());
                if (variant != null) {
                    variant.collectIngredients(base, clicks);
                } else if (fixed != null) {
                    fixed.collectIngredients(base);
                } else {
                    Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(r.base());
                    if (item == Items.AIR) continue;
                    base.add(Ingredient.of(item));
                }
                List<Ingredient> seq = new ArrayList<>();
                // an empty step in the base is a roll of the mat (an empty-handed click)
                for (Ingredient i : base) seq.add(i.isEmpty() ? null : i);
                for (int i = 0; i < clicks.size(); i++) seq.add(null);
                for (Ingredient i : r.getCustomIngredients()) if (!i.isEmpty()) seq.add(i);
                List<Ingredient> inputs = seq.stream().filter(java.util.Objects::nonNull).toList();
                if (inputs.isEmpty()) continue;
                steps.put(h.id(), seq);
                out.add(new RecipeNeed(h.id(), h, inputs, null, ItemStack.EMPTY, result.copy(), 0, Set.of(result.getItem())));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof dev.xkmc.youkaishomecoming.content.pot.table.board.CuisineBoardBlockEntity b)) return false;
            try {
                var f = b.getClass().getDeclaredField("contents");
                f.setAccessible(true);
                for (Object o : (List<?>) f.get(b)) {
                    if (o instanceof ItemStack st && !st.isEmpty()) {
                        return false;
                    }
                }
                return true;
            } catch (ReflectiveOperationException | RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().info("[maidchef] cuisine board idle check: {}", e.toString());
                return true;
            }
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 16;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof dev.xkmc.youkaishomecoming.content.pot.table.board.CuisineBoardBlockEntity b)) {
                return LoadResult.FAILED;
            }
            List<Ingredient> seq = steps.get(need.id());
            if (seq == null) return LoadResult.FAILED;
            IItemHandler bag = Inv.backpack(maid);
            int done = 0, results = 0;
            for (int u = 0; u < units; u++) {
                int k = 0;
                boolean ok = true;
                for (Ingredient step : seq) {
                    if (step == null) {
                        b.addItem(ItemStack.EMPTY);
                        continue;
                    }
                    Item item = alloc.perInput().get(k++);
                    ItemStack have = Inv.take(bag, item, 64);
                    int used = have.isEmpty() ? 0 : b.addItem(have);
                    if (used <= 0 && !have.isEmpty()) {
                        // the board may want a roll of the mat first
                        b.addItem(ItemStack.EMPTY);
                        used = b.addItem(have);
                    }
                    if (used > 0) have.shrink(used);
                    if (!have.isEmpty()) Inv.giveOrDrop(maid, have);
                    if (used <= 0) {
                        StringBuilder hint = new StringBuilder();
                        for (Ingredient i : b.getModel().getHints(level)) {
                            for (ItemStack st : i.getItems()) hint.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem())).append(' ');
                            hint.append("| ");
                        }
                        net.eileenthg.maidchef.ChefBehavior.trace(maid, "cuisine board refused " + item + "; it wants: " + hint);
                        ok = false;
                        break;
                    }
                }
                FakePlayer fp = Fakes.hands(level, maid, ItemStack.EMPTY);
                if (ok) ok = b.addToPlayer(fp);
                int got = Fakes.drain(fp, maid, need.resultItem(), collected, false);
                Fakes.clear(fp);
                results += got;
                if (!ok || got == 0) {
                    // take back what went on the board
                    try {
                        var f = b.getClass().getDeclaredField("contents");
                        f.setAccessible(true);
                        List<?> list = (List<?>) f.get(b);
                        for (Object o : list) if (o instanceof ItemStack st && !st.isEmpty()) Inv.giveOrDrop(maid, st.copy());
                        list.clear();
                        var m = b.getClass().getDeclaredField("model");
                        m.setAccessible(true);
                        m.set(b, null);
                        b.notifyTile();
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        com.mojang.logging.LogUtils.getLogger().info("[maidchef] couldn't clear the cuisine board: {}", e.toString());
                    }
                    net.eileenthg.maidchef.ChefBehavior.trace(maid, "cuisine board: " + need.resultItem() + " didn't come together");
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
}
