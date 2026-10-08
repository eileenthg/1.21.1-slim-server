package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

/** Inventory helpers. "Backpack" is the maid's main inventory without her hands. */
public final class Inv {
    /** TLM/Maidsoul Kitchen reserve this backpack slot for the Culinary Hub; never touch it. */
    public static final int HUB_SLOT = 4;

    private Inv() {
    }

    public static IItemHandler backpack(EntityMaid maid) {
        int max = maid.getMaidBackpackType().getAvailableMaxContainerIndex();
        return new RangedWrapper(maid.getMaidInv(), 0, max);
    }

    public static int count(IItemHandler handler, Item item) {
        int n = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    public static void addCounts(IItemHandler handler, Map<Item, Integer> into, boolean skipHubSlot) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (skipHubSlot && i == HUB_SLOT) continue;
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty()) into.merge(s.getItem(), s.getCount(), Integer::sum);
        }
    }

    /** Extract up to n of item. Returns how many were moved into {@code to} (items that don't fit stay put). */
    public static int move(IItemHandler from, IItemHandler to, Item item, int n, boolean skipHubSlot) {
        int moved = 0;
        for (int i = 0; i < from.getSlots() && moved < n; i++) {
            if (skipHubSlot && i == HUB_SLOT) continue;
            ItemStack s = from.getStackInSlot(i);
            if (!s.is(item)) continue;
            int want = Math.min(n - moved, s.getCount());
            ItemStack sim = from.extractItem(i, want, true);
            if (sim.isEmpty()) continue;
            ItemStack left = ItemHandlerHelper.insertItemStacked(to, sim.copy(), true);
            int fits = sim.getCount() - left.getCount();
            if (fits <= 0) continue;
            ItemStack real = from.extractItem(i, fits, false);
            ItemStack rest = ItemHandlerHelper.insertItemStacked(to, real, false);
            moved += real.getCount() - rest.getCount();
            if (!rest.isEmpty()) {
                // put back what didn't fit after all
                from.insertItem(i, rest, false);
            }
        }
        return moved;
    }

    /** Take up to n of item out of a handler as one stack (n must not exceed the item's max stack size). */
    public static ItemStack take(IItemHandler from, Item item, int n) {
        ItemStack out = ItemStack.EMPTY;
        for (int i = 0; i < from.getSlots() && out.getCount() < n; i++) {
            ItemStack s = from.getStackInSlot(i);
            if (!s.is(item)) continue;
            if (!out.isEmpty() && !ItemStack.isSameItemSameComponents(out, s)) continue;
            ItemStack got = from.extractItem(i, n - out.getCount(), false);
            if (got.isEmpty()) continue;
            if (out.isEmpty()) out = got;
            else out.grow(got.getCount());
        }
        return out;
    }

    /** Insert into the maid's backpack; whatever doesn't fit is dropped at her feet so nothing is lost. */
    public static void giveOrDrop(EntityMaid maid, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack left = ItemHandlerHelper.insertItemStacked(backpack(maid), stack, false);
        if (!left.isEmpty()) {
            maid.spawnAtLocation(left);
        }
    }

    public static int freeSpaceFor(IItemHandler handler, Item item) {
        int n = 0;
        int max = item.getDefaultMaxStackSize();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (s.isEmpty()) n += Math.min(max, handler.getSlotLimit(i));
            else if (s.is(item) && s.isComponentsPatchEmpty()) n += Math.max(0, Math.min(max, handler.getSlotLimit(i)) - s.getCount());
        }
        return n;
    }

    /** Find a stack matching the predicate anywhere in the maid's inventory including hands. Returns the live stack. */
    public static ItemStack findLive(EntityMaid maid, Predicate<ItemStack> test) {
        IItemHandler all = maid.getAvailableInv(true);
        for (int i = 0; i < all.getSlots(); i++) {
            ItemStack s = all.getStackInSlot(i);
            if (!s.isEmpty() && test.test(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    /** Pick up item entities of the wanted items near a station into the maid's inventory. */
    public static int vacuum(Level level, EntityMaid maid, BlockPos pos, Predicate<Item> wanted, Item result, Map<Item, Integer> collected) {
        AABB box = new AABB(pos).inflate(1.75, 1.5, 1.75);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && wanted.test(e.getItem().getItem()));
        int got = 0;
        IItemHandler bag = backpack(maid);
        for (ItemEntity e : items) {
            ItemStack stack = e.getItem().copy();
            ItemStack left = ItemHandlerHelper.insertItemStacked(bag, stack, false);
            int moved = stack.getCount() - left.getCount();
            if (moved <= 0) continue;
            collected.merge(stack.getItem(), moved, Integer::sum);
            if (stack.is(result)) got += moved;
            net.eileenthg.maidchef.ChefClaims.release(e.getUUID());
            if (left.isEmpty()) e.discard();
            else e.setItem(left);
        }
        return got;
    }
}
