package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.items.IItemHandler;

/** Shared bits for stations from other mods (no mod classes here, so any station can use them). */
final class Kit {
    private Kit() {
    }

    /** Items an input may leave behind (buckets, bottles, bowls). */
    static Set<Item> remainders(List<Ingredient> inputs) {
        Set<Item> out = new HashSet<>();
        for (Ingredient ing : inputs) {
            for (ItemStack s : ing.getItems()) {
                ItemStack rem = s.getCraftingRemainingItem();
                if (!rem.isEmpty()) out.add(rem.getItem());
            }
        }
        return out;
    }

    /** Where a recipe's item inputs end and its fluid carriers (bottles, buckets) begin. */
    record Layout(int items, List<FluidItems.Carry> carries) {
        static final Layout NONE = new Layout(0, List.of());
    }

    /** Item inputs followed by one ingredient per carried bottle/bucket. Null if a fluid can't be carried. */
    @Nullable
    static List<Ingredient> withCarries(List<Ingredient> items, List<FluidItems.Carry> carries) {
        List<Ingredient> out = new ArrayList<>(items);
        for (FluidItems.Carry c : carries) {
            if (c == null) return null;
            for (int i = 0; i < c.count(); i++) out.add(c.items());
        }
        return List.copyOf(out);
    }

    /** Pour the carriers at alloc positions [from, from+n) into the tank; emptied bottles/buckets go back to her. */
    static boolean pourAll(EntityMaid maid, net.neoforged.neoforge.fluids.capability.IFluidHandler tank, Alloc alloc, int from, int n,
                           Map<Item, Integer> collected) {
        IItemHandler bag = Inv.backpack(maid);
        for (int i = from; i < from + n; i++) {
            ItemStack one = Inv.take(bag, alloc.perInput().get(i), 1);
            if (one.isEmpty()) return false;
            ItemStack empty = FluidItems.pour(tank, one);
            if (empty == null) {
                Inv.giveOrDrop(maid, one);
                return false;
            }
            if (!empty.isEmpty()) {
                collected.merge(empty.getItem(), empty.getCount(), Integer::sum);
                Inv.giveOrDrop(maid, empty);
            }
        }
        return true;
    }

    static int carried(Layout l) {
        int n = 0;
        for (FluidItems.Carry c : l.carries()) n += c.count();
        return n;
    }
}
