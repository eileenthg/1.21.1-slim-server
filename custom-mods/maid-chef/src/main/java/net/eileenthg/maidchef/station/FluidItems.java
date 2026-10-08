package net.eileenthg.maidchef.station;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Fluids carried as items: a station recipe that wants "250 mB of milk" becomes "one milk bottle" for the planner.
 * <ul>
 *   <li>Extra Delight's bottle registry: every ED fluid (and milk, cider, broth...) has a 250 mB bottle.</li>
 *   <li>Water: a water bucket (1000 mB). Water is free, so whatever the recipe doesn't use is poured away afterwards.</li>
 *   <li>Other fluids in whole buckets: their bucket.</li>
 * </ul>
 * Amounts that fit none of these (100 mB of cocoa butter) aren't supported; those recipes are left out.
 */
public final class FluidItems {
    public static final int BOTTLE = 250, BUCKET = 1000;

    private FluidItems() {
    }

    /** How a fluid amount is carried: {@code count} items, any of {@code items}, each worth {@code each} mB. */
    public record Carry(Ingredient items, int count, int each, boolean water) {
    }

    /**
     * @param test   which fluids are acceptable
     * @param sample a fluid that passes {@code test} (for the bucket lookup)
     */
    @Nullable
    public static Carry carry(Predicate<FluidStack> test, @Nullable Fluid sample, int amount) {
        if (amount <= 0) return null;
        if (test.test(new FluidStack(Fluids.WATER, BUCKET))) {
            return new Carry(Ingredient.of(Items.WATER_BUCKET), (amount + BUCKET - 1) / BUCKET, BUCKET, true);
        }
        if (amount % BOTTLE == 0) {
            List<Item> bottles = bottlesFor(test);
            if (!bottles.isEmpty()) return new Carry(Ingredient.of(bottles.toArray(Item[]::new)), amount / BOTTLE, BOTTLE, false);
        }
        // any item that holds the fluid in amounts that add up exactly (YHC bottles, jars, modded buckets)
        Map<Integer, List<Item>> bySize = new java.util.TreeMap<>(java.util.Comparator.reverseOrder());
        for (Map.Entry<Item, FluidStack> e : contents().entrySet()) {
            int a = e.getValue().getAmount();
            if (a > 0 && amount % a == 0 && test.test(e.getValue().copyWithAmount(amount))) bySize.computeIfAbsent(a, k -> new ArrayList<>()).add(e.getKey());
        }
        for (Map.Entry<Integer, List<Item>> e : bySize.entrySet()) {
            if (amount / e.getKey() > 8) continue;
            return new Carry(Ingredient.of(e.getValue().toArray(Item[]::new)), amount / e.getKey(), e.getKey(), false);
        }
        if (amount % BUCKET == 0 && sample != null && sample.getBucket() != Items.AIR && sample.getBucket() != Items.BUCKET) {
            return new Carry(Ingredient.of(sample.getBucket()), amount / BUCKET, BUCKET, false);
        }
        return null;
    }

    private static Map<Item, FluidStack> contents;
    /** Drinks other mods pour out of their stations (a tankard of vodka = 250 mB of vodka), and what's left once poured. */
    private static final Map<Item, FluidStack> LEARNED = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Item, ItemStack> EMPTY_OF = new java.util.concurrent.ConcurrentHashMap<>();

    /** A station mod says: this item holds this much of this fluid, and is {@code empty} once poured. */
    public static void learn(Item item, FluidStack fluid, ItemStack empty) {
        if (item == Items.AIR || fluid.isEmpty()) return;
        LEARNED.putIfAbsent(item, fluid.copy());
        if (!empty.isEmpty()) EMPTY_OF.putIfAbsent(item, empty.copyWithCount(1));
    }

    /** What every item holds (bottles, buckets, jars), worked out once. */
    private static synchronized Map<Item, FluidStack> contents() {
        if (contents == null) {
            Map<Item, FluidStack> out = new java.util.HashMap<>();
            for (Item it : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                if (it == Items.AIR || it == Items.POTION) continue;
                try {
                    FluidStack f = contained(new ItemStack(it));
                    if (!f.isEmpty()) out.put(it, f.copy());
                } catch (RuntimeException ignored) {
                    // some items don't like being asked
                }
            }
            contents = out;
        }
        Map<Item, FluidStack> all = new java.util.HashMap<>(contents);
        all.putAll(LEARNED);
        return all;
    }

    @Nullable
    public static Carry carry(FluidStack exact) {
        return carry(f -> FluidStack.isSameFluidSameComponents(f, exact), exact.getFluid(), exact.getAmount());
    }

    private static List<Item> bottlesFor(Predicate<FluidStack> test) {
        Set<Item> out = new LinkedHashSet<>();
        if (ModList.get().isLoaded("extradelight")) out.addAll(EdBottles.bottlesFor(test));
        return new ArrayList<>(out);
    }

    /** The fluid an item carries (bottle or bucket), or EMPTY. */
    public static FluidStack contained(ItemStack stack) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        FluidStack learned = LEARNED.get(stack.getItem());
        if (learned != null) return learned.copy();
        if (ModList.get().isLoaded("extradelight")) {
            FluidStack f = EdBottles.fluidOf(stack);
            if (!f.isEmpty()) return f;
        }
        if (stack.getItem() instanceof BucketItem || stack.is(Items.MILK_BUCKET)) {
            return FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        }
        return FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
    }

    /** What's left of the item after pouring it out (glass bottle, bucket). */
    public static ItemStack emptied(ItemStack stack) {
        ItemStack known = EMPTY_OF.get(stack.getItem());
        if (known != null) return known.copy();
        boolean edBottle = ModList.get().isLoaded("extradelight") && !EdBottles.fluidOf(stack).isEmpty();
        if (!edBottle) {
            var cap = stack.copyWithCount(1).getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
            if (cap != null) {
                cap.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                if (!cap.getContainer().isEmpty()) return cap.getContainer().copy();
            }
        }
        ItemStack rem = stack.getCraftingRemainingItem();
        if (!rem.isEmpty()) return rem.copy();
        if (stack.getItem() instanceof BucketItem || stack.is(Items.MILK_BUCKET)) return new ItemStack(Items.BUCKET);
        return new ItemStack(Items.GLASS_BOTTLE);
    }

    /** A bottle of the fluid (for stations that make a fluid), or EMPTY. */
    public static ItemStack bottleOf(FluidStack fluid) {
        if (ModList.get().isLoaded("extradelight")) return EdBottles.bottleOf(fluid);
        return ItemStack.EMPTY;
    }

    /** Pour one carried item into a tank. Returns the emptied item, or null if it didn't all go in. */
    @Nullable
    public static ItemStack pour(IFluidHandler tank, ItemStack one) {
        FluidStack f = contained(one);
        if (f.isEmpty()) return null;
        if (tank.fill(f, IFluidHandler.FluidAction.SIMULATE) < f.getAmount()) return null;
        tank.fill(f, IFluidHandler.FluidAction.EXECUTE);
        return emptied(one);
    }

    /** Bottle what's in a tank, 250 mB per glass bottle. Returns the filled bottles. */
    public static List<ItemStack> bottleUp(IFluidHandler tank, int maxBottles) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < maxBottles; i++) {
            FluidStack f = tank.drain(BOTTLE, IFluidHandler.FluidAction.SIMULATE);
            if (f.getAmount() < BOTTLE) break;
            ItemStack b = bottleOf(f);
            if (b.isEmpty()) break;
            tank.drain(BOTTLE, IFluidHandler.FluidAction.EXECUTE);
            out.add(b);
        }
        return out;
    }

    /** Throw away leftover water (anything else stays). */
    public static void voidWater(IFluidHandler tank) {
        for (int i = 0; i < tank.getTanks(); i++) {
            FluidStack f = tank.getFluidInTank(i);
            if (!f.isEmpty() && f.getFluid().isSame(Fluids.WATER)) tank.drain(f.copy(), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public static boolean isEmpty(IFluidHandler tank) {
        for (int i = 0; i < tank.getTanks(); i++) {
            if (!tank.getFluidInTank(i).isEmpty()) return false;
        }
        return true;
    }
}
