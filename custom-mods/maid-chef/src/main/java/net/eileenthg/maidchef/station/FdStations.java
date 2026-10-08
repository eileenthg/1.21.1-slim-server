package net.eileenthg.maidchef.station;

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
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import vectorwing.farmersdelight.common.block.AbstractStoveBlock;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.crafting.ingredient.ChanceResult;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

/** Farmer's Delight stations. Only class-loaded when Farmer's Delight is installed. */
public final class FdStations {
    private FdStations() {
    }

    public static void addTo(List<Station> list) {
        list.add(new Pot());
        list.add(new Skillet());
        list.add(new Stove());
        list.add(new CuttingBoard());
    }

    /** Items an input may leave behind (buckets, bottles, bowls). */
    private static Set<Item> remainders(List<Ingredient> inputs) {
        Set<Item> out = new HashSet<>();
        for (Ingredient ing : inputs) {
            for (ItemStack s : ing.getItems()) {
                ItemStack rem = s.getCraftingRemainingItem();
                if (!rem.isEmpty()) out.add(rem.getItem());
                Item over = CookingPotBlockEntity.INGREDIENT_REMAINDER_OVERRIDES.get(s.getItem());
                if (over != null) out.add(over);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- cooking pot
    static final class Pot implements Station {
        private static final int INPUTS = 6, MEAL = 6, CONTAINER = 7, OUT = 8;

        @Override
        public String name() {
            return "cooking pot";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof CookingPotBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<CookingPotRecipe> h : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.COOKING.get())) {
                CookingPotRecipe r = h.value();
                List<Ingredient> inputs = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                if (inputs.isEmpty() || inputs.size() > INPUTS) continue;
                ItemStack result = r.getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                out.add(new RecipeNeed(h.id(), h, inputs, null, r.getOutputContainer().copy(), result.copy(), r.getCookTime(), remainders(inputs)));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot) || !pot.isHeated()) return false;
            ItemStackHandler inv = pot.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return Math.max(1, 64 / need.resultCount());
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot)) return LoadResult.FAILED;
            for (Item item : alloc.perInput()) units = Math.min(units, item.getDefaultMaxStackSize());
            Item container = need.container().isEmpty() ? null : need.container().getItem();
            if (container != null) units = Math.min(units, container.getDefaultMaxStackSize() / need.resultCount());
            if (units <= 0) return LoadResult.FAILED;
            ItemStackHandler inv = pot.getInventory();
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
            pot.setChanged();
            return new LoadResult(units, 0);
        }

        @Override
        public boolean canStage() {
            return true;
        }

        private static int stageUnits(RecipeNeed need, Alloc alloc, int units) {
            for (Item item : alloc.perInput()) units = Math.min(units, item.getDefaultMaxStackSize());
            if (!need.container().isEmpty()) units = Math.min(units, need.container().getItem().getDefaultMaxStackSize() / need.resultCount());
            return Math.max(1, units);
        }

        /** Would what's in the pot right now cook something other than the recipe she wants? */
        private static boolean wrongMatch(ServerLevel level, CookingPotBlockEntity pot, RecipeNeed need) {
            return level.getRecipeManager().getRecipeFor(ModRecipeTypes.COOKING.get(),
                            new net.neoforged.neoforge.items.wrapper.RecipeWrapper(pot.getInventory()), level)
                    .map(h -> !h.id().equals(need.id())).orElse(false);
        }

        @Override
        public boolean stage(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units) {
            if (!(level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot)) return false;
            units = stageUnits(need, alloc, units);
            ItemStackHandler inv = pot.getInventory();
            IItemHandler bag = Inv.backpack(maid);
            for (int i = 0; i < alloc.perInput().size(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) continue;
                Item item = alloc.perInput().get(i);
                if (Inv.count(bag, item) < units) continue;
                inv.setStackInSlot(i, Inv.take(bag, item, units));
                if (wrongMatch(level, pot, need)) {
                    // these inputs alone make a different dish: hold this one back until the rest is in
                    Inv.giveOrDrop(maid, inv.getStackInSlot(i).copy());
                    inv.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
            if (!need.container().isEmpty() && inv.getStackInSlot(CONTAINER).isEmpty()) {
                int n = units * need.resultCount();
                Item c = need.container().getItem();
                if (Inv.count(bag, c) >= n) inv.setStackInSlot(CONTAINER, Inv.take(bag, c, n));
            }
            pot.setChanged();
            return stillToStage(level, pos, need, alloc, units).isEmpty();
        }

        @Override
        public Map<Item, Integer> stillToStage(ServerLevel level, BlockPos pos, RecipeNeed need, Alloc alloc, int units) {
            if (!(level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot)) return Map.of();
            units = stageUnits(need, alloc, units);
            ItemStackHandler inv = pot.getInventory();
            Map<Item, Integer> out = new java.util.LinkedHashMap<>();
            for (int i = 0; i < alloc.perInput().size(); i++) {
                if (inv.getStackInSlot(i).isEmpty()) out.merge(alloc.perInput().get(i), units, Integer::sum);
            }
            if (!need.container().isEmpty() && inv.getStackInSlot(CONTAINER).isEmpty()) {
                out.merge(need.container().getItem(), units * need.resultCount(), Integer::sum);
            }
            return out;
        }

        @Override
        public void unstage(ServerLevel level, EntityMaid maid, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot)) return;
            ItemStackHandler inv = pot.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                if (i == MEAL || i == OUT) continue;
                ItemStack s = inv.getStackInSlot(i);
                if (s.isEmpty()) continue;
                Inv.giveOrDrop(maid, s.copy());
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }
            pot.setChanged();
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            int got = Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
            if (!(level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot)) return got;
            ItemStackHandler inv = pot.getInventory();
            ItemStack out = inv.extractItem(OUT, 64, false);
            if (!out.isEmpty()) {
                collected.merge(out.getItem(), out.getCount(), Integer::sum);
                if (out.is(need.resultItem())) got += out.getCount();
                Inv.giveOrDrop(maid, out);
            }
            if (finished(level, pos)) {
                // leftover containers go back to her too
                ItemStack c = inv.extractItem(CONTAINER, 64, false);
                if (!c.isEmpty()) {
                    collected.merge(c.getItem(), c.getCount(), Integer::sum);
                    Inv.giveOrDrop(maid, c);
                }
            }
            pot.setChanged();
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot)) return true;
            ItemStackHandler inv = pot.getInventory();
            for (int i = 0; i < INPUTS; i++) {
                if (!inv.getStackInSlot(i).isEmpty()) return false;
            }
            return inv.getStackInSlot(MEAL).isEmpty();
        }
    }

    // ---------------------------------------------------------------- skillet (placed)
    static final class Skillet implements Station {
        @Override
        public String name() {
            return "skillet";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof SkilletBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            return CampfireStation.campfireRecipes(level);
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) return false;
            return level.getBlockEntity(pos) instanceof SkilletBlockEntity s && s.isHeated() && !s.hasStoredStack();
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 64;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof SkilletBlockEntity s) || s.hasStoredStack()) return LoadResult.FAILED;
            Item item = alloc.perInput().get(0);
            ItemStack stack = Inv.take(Inv.backpack(maid), item, Math.min(units, item.getDefaultMaxStackSize()));
            if (stack.isEmpty()) return LoadResult.FAILED;
            ItemStack left = s.addItemToCook(stack, FakePlayerFactory.getMinecraft(level));
            int loaded = stack.getCount() - left.getCount();
            if (!left.isEmpty()) Inv.giveOrDrop(maid, left);
            return new LoadResult(loaded, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            return Inv.vacuum(level, maid, pos, it -> it == need.resultItem(), need.resultItem(), collected);
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            return !(level.getBlockEntity(pos) instanceof SkilletBlockEntity s) || !s.hasStoredStack();
        }
    }

    // ---------------------------------------------------------------- stove
    static final class Stove implements Station {
        @Override
        public String name() {
            return "stove";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof AbstractStoveBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            return CampfireStation.campfireRecipes(level);
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (!state.hasProperty(AbstractStoveBlock.LIT) || !state.getValue(AbstractStoveBlock.LIT)) return false;
            // a stove with a pot or skillet on top drops anything placed on it
            return level.getBlockEntity(pos) instanceof AbstractStoveBlockEntity s && !s.shouldDropItems() && s.isEmpty();
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 6;
        }

        @Override
        @SuppressWarnings("unchecked")
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof AbstractStoveBlockEntity s)) return LoadResult.FAILED;
            if (!(need.holder().value() instanceof AbstractCookingRecipe)) return LoadResult.FAILED;
            RecipeHolder<? extends AbstractCookingRecipe> holder = (RecipeHolder<? extends AbstractCookingRecipe>) need.holder();
            Item item = alloc.perInput().get(0);
            int loaded = 0;
            for (int i = 0; i < units; i++) {
                ItemStack one = Inv.take(Inv.backpack(maid), item, 1);
                if (one.isEmpty()) break;
                if (!s.placeFood(maid, one, holder)) {
                    Inv.giveOrDrop(maid, one);
                    break;
                }
                loaded++;
            }
            return new LoadResult(loaded, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            return Inv.vacuum(level, maid, pos.above(), it -> it == need.resultItem(), need.resultItem(), collected);
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            return !(level.getBlockEntity(pos) instanceof AbstractStoveBlockEntity s) || s.isEmpty();
        }
    }

    // ---------------------------------------------------------------- cutting board
    static final class CuttingBoard implements Station {
        @Override
        public String name() {
            return "cutting board";
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof CuttingBoardBlockEntity;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            for (RecipeHolder<CuttingBoardRecipe> h : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.CUTTING.get())) {
                CuttingBoardRecipe r = h.value();
                List<Ingredient> inputs = r.getIngredients().stream().filter(i -> !i.isEmpty()).toList();
                if (inputs.size() != 1) continue;
                ItemStack result = r.getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                Set<Item> extras = new HashSet<>();
                for (ChanceResult c : r.getRollableResults()) extras.add(c.stack().getItem());
                extras.add(result.getItem());
                out.add(new RecipeNeed(h.id(), h, inputs, r.getTool(), ItemStack.EMPTY, result.copy(), 0, extras));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity b && b.isEmpty();
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return 64;
        }

        @Override
        public boolean needsTool() {
            return true;
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            if (!(level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board) || !board.isEmpty() || need.tool() == null) return LoadResult.FAILED;
            Item item = alloc.perInput().get(0);
            int done = 0, results = 0;
            for (int u = 0; u < units; u++) {
                ItemStack tool = Inv.findLive(maid, need.tool()::test);
                if (tool.isEmpty()) break;
                ItemStack one = Inv.take(Inv.backpack(maid), item, 1);
                if (one.isEmpty()) break;
                ItemStack left = board.addItem(one);
                if (!left.isEmpty()) {
                    Inv.giveOrDrop(maid, left);
                    break;
                }
                if (!board.processStoredItemUsingTool(tool, null)) {
                    Inv.giveOrDrop(maid, board.removeItem());
                    break;
                }
                done++;
                results += Inv.vacuum(level, maid, pos, need.extras()::contains, need.resultItem(), collected);
            }
            if (done > 0) maid.swing(maid.getUsedItemHand());
            return new LoadResult(done, results);
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
