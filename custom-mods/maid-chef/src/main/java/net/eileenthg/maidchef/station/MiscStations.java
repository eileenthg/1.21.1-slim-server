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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;

/** Smaller stations: Butchery's grinder and pestle (no compile dependency: matched by id), plus mod-specific ones below. */
public final class MiscStations {
    private MiscStations() {
    }

    public static void addTo(List<Station> list) {
        if (ModList.get().isLoaded("butchery")) {
            // grinder: slots 0-3 inputs (slot 2 holds the sausage attachment, which stays), 4-6 output
            list.add(new SlotMachine("meat grinder", "butchery:meat_grinder", "butchery:meatgrinderjei", 4, 4, 7, 150,
                    ResourceLocation.parse("butchery:sausage_attachment")));
            list.add(new SlotMachine("pestle and mortar", "butchery:pestle_and_mortar", "butchery:pestleandmortarrecipetype", 1, 1, 2, 150, null));
        }
        if (ModList.get().isLoaded("expandeddelight")) list.add(new ExdCfbStations.ExdJuicer());
        if (ModList.get().isLoaded("cookingforblockheads")) list.add(new ExdCfbStations.CfbOven());
    }

    static int give(EntityMaid maid, ItemStack out, Item result, Map<Item, Integer> collected) {
        if (out.isEmpty()) return 0;
        collected.merge(out.getItem(), out.getCount(), Integer::sum);
        int got = out.is(result) ? out.getCount() : 0;
        Inv.giveOrDrop(maid, out);
        return got;
    }

    // ---------------------------------------------------------------- a machine that works on whatever is in its slots
    /**
     * A block whose inventory processes on its own (MCreator-style machines): recipe ingredient i goes in slot i, results come out
     * in the output slots. An input matching {@code keptItem} is a fitting that stays in the machine (a tool she brings).
     */
    static final class SlotMachine implements Station {
        private final String name;
        private final ResourceLocation beType;
        private final ResourceLocation recipeType;
        private final int inputs, outFrom, outTo, ticks;
        @Nullable
        private final ResourceLocation keptItem;
        private final Map<ResourceLocation, int[]> slotsOf = new ConcurrentHashMap<>();

        SlotMachine(String name, String beType, String recipeType, int inputs, int outFrom, int outTo, int ticks, @Nullable ResourceLocation keptItem) {
            this.name = name;
            this.beType = ResourceLocation.parse(beType);
            this.recipeType = ResourceLocation.parse(recipeType);
            this.inputs = inputs;
            this.outFrom = outFrom;
            this.outTo = outTo;
            this.ticks = ticks;
            this.keptItem = keptItem;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
            return be instanceof Container && beType.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()));
        }

        @Nullable
        private Container box(ServerLevel level, BlockPos pos) {
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof Container c && matches(level, pos, level.getBlockState(pos), be) ? c : null;
        }

        @Override
        public List<RecipeNeed> recipes(ServerLevel level) {
            List<RecipeNeed> out = new ArrayList<>();
            Item kept = keptItem == null ? null : BuiltInRegistries.ITEM.get(keptItem);
            for (RecipeHolder<?> h : level.getRecipeManager().getRecipes()) {
                Recipe<?> r = h.value();
                // the JSON "type" is the serializer's id (the recipe type itself may be registered under another name)
                if (!recipeType.equals(BuiltInRegistries.RECIPE_SERIALIZER.getKey(r.getSerializer()))) continue;
                ItemStack result = r.getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                List<Ingredient> ins = new ArrayList<>();
                List<Integer> slots = new ArrayList<>();
                Ingredient tool = null;
                List<Ingredient> all = r.getIngredients();
                for (int i = 0; i < Math.min(inputs, all.size()); i++) {
                    Ingredient ing = all.get(i);
                    // "nothing here" placeholders (an empty tag) show up as air
                    if (ing.isEmpty() || java.util.Arrays.stream(ing.getItems()).allMatch(ItemStack::isEmpty)) continue;
                    if (kept != null && ing.test(new ItemStack(kept))) {
                        tool = ing;
                        continue;
                    }
                    ins.add(ing);
                    slots.add(i);
                }
                if (ins.isEmpty()) continue;
                int[] arr = new int[slots.size() + 1];
                for (int i = 0; i < slots.size(); i++) arr[i] = slots.get(i);
                arr[slots.size()] = -1;
                for (int i = 0; i < all.size() && i < inputs; i++) {
                    if (tool != null && all.get(i) == tool) arr[slots.size()] = i;
                }
                slotsOf.put(h.id(), arr);
                Set<Item> extras = new HashSet<>(Kit.remainders(ins));
                extras.add(result.getItem());
                out.add(new RecipeNeed(h.id(), h, List.copyOf(ins), tool, ItemStack.EMPTY, result.copy(), ticks, extras));
            }
            return out;
        }

        @Override
        public boolean idle(ServerLevel level, BlockPos pos) {
            Container c = box(level, pos);
            if (c == null) return false;
            for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty()) return false;
            return true;
        }

        @Override
        public int loadCap(RecipeNeed need) {
            return Math.max(1, Math.min(64, need.result().getMaxStackSize()) / need.resultCount());
        }

        @Override
        public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
            Container c = box(level, pos);
            int[] slots = slotsOf.get(need.id());
            if (c == null || slots == null) return LoadResult.FAILED;
            units = Math.min(units, loadCap(need));
            for (Item item : alloc.perInput()) units = Math.min(units, item.getDefaultMaxStackSize());
            IItemHandler bag = Inv.backpack(maid);
            List<Integer> filled = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < alloc.perInput().size() && ok; i++) {
                ItemStack s = Inv.take(bag, alloc.perInput().get(i), units);
                if (s.getCount() < units) {
                    Inv.giveOrDrop(maid, s);
                    ok = false;
                } else {
                    c.setItem(slots[i], s);
                    filled.add(slots[i]);
                }
            }
            int toolSlot = slots[slots.length - 1];
            if (ok && need.tool() != null && toolSlot >= 0) {
                ItemStack t = Inv.findLive(maid, need.tool()::test);
                if (t.isEmpty()) ok = false;
                else {
                    c.setItem(toolSlot, Inv.take(bag, t.getItem(), 1));
                    filled.add(toolSlot);
                }
            }
            if (!ok) {
                for (int slot : filled) {
                    Inv.giveOrDrop(maid, c.getItem(slot).copy());
                    c.setItem(slot, ItemStack.EMPTY);
                }
                return LoadResult.FAILED;
            }
            c.setChanged();
            // these machines tick through scheduled block ticks
            level.scheduleTick(pos, level.getBlockState(pos).getBlock(), 1);
            return new LoadResult(units, 0);
        }

        @Override
        public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
            Container c = box(level, pos);
            if (c == null) return 0;
            int got = 0;
            for (int i = outFrom; i < Math.min(outTo, c.getContainerSize()); i++) {
                ItemStack s = c.getItem(i);
                if (s.isEmpty()) continue;
                c.setItem(i, ItemStack.EMPTY);
                got += give(maid, s.copy(), need.resultItem(), collected);
            }
            if (finished(level, pos)) {
                // leftovers and the attachment come back with her
                for (int i = 0; i < Math.min(inputs, c.getContainerSize()); i++) {
                    ItemStack s = c.getItem(i);
                    if (s.isEmpty()) continue;
                    c.setItem(i, ItemStack.EMPTY);
                    give(maid, s.copy(), need.resultItem(), collected);
                }
            }
            c.setChanged();
            return got;
        }

        @Override
        public boolean finished(ServerLevel level, BlockPos pos) {
            Container c = box(level, pos);
            return c == null || c.getItem(0).isEmpty();
        }
    }
}
