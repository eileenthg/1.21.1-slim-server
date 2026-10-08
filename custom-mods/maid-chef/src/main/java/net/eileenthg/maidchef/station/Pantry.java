package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.List;
import net.eileenthg.maidchef.ChefConfig;
import net.eileenthg.maidchef.HubChests;
import net.eileenthg.maidchef.Planner;
import net.eileenthg.maidchef.TaskChef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Her backpack plus the ingredient chests bound to her Culinary Hub (inside the kitchen). Stations without an
 * inventory of their own (the crafting table) take what's missing from the chests directly, so a small backpack
 * doesn't limit what she can craft.
 */
public final class Pantry {
    private Pantry() {
    }

    static List<IItemHandler> chests(ServerLevel level, EntityMaid maid) {
        List<IItemHandler> out = new ArrayList<>();
        BlockPos c = Planner.center(maid);
        int r = Math.min(TaskChef.data(maid).radius(), ChefConfig.MAX_RADIUS.get()) + 4;
        for (BlockPos p : HubChests.of(maid).ingredient()) {
            if (Math.abs(p.getX() - c.getX()) > r || Math.abs(p.getZ() - c.getZ()) > r || Math.abs(p.getY() - c.getY()) > r) continue;
            IItemHandler h = Planner.handler(level, p);
            if (h != null) out.add(h);
        }
        return out;
    }

    public static int count(ServerLevel level, EntityMaid maid, Item item) {
        int n = Inv.count(Inv.backpack(maid), item);
        for (IItemHandler h : chests(level, maid)) n += Inv.count(h, item);
        return n;
    }

    /** Remove n of item, backpack first, then the chests. Returns how many were removed. */
    public static int remove(ServerLevel level, EntityMaid maid, Item item, int n) {
        int left = n;
        while (left > 0) {
            ItemStack got = Inv.take(Inv.backpack(maid), item, Math.min(left, item.getDefaultMaxStackSize()));
            if (got.isEmpty()) break;
            left -= got.getCount();
        }
        for (IItemHandler h : chests(level, maid)) {
            while (left > 0) {
                ItemStack got = Inv.take(h, item, Math.min(left, item.getDefaultMaxStackSize()));
                if (got.isEmpty()) break;
                left -= got.getCount();
            }
        }
        return n - left;
    }
}
