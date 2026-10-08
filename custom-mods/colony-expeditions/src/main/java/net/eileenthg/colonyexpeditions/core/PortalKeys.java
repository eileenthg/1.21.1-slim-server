package net.eileenthg.colonyexpeditions.core;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Requesting and using up portal keys (Dimensional Tears for the Netherworker, dimension keys for expeditions). */
public final class PortalKeys
{
    private PortalKeys()
    {
    }

    /** How many items the worker must hold: durability keys need one, stackable keys need the full amount. */
    private static int needed(final Item key, final int perTrip)
    {
        return new ItemStack(key).isDamageableItem() ? 1 : perTrip;
    }

    /**
     * Returns true when the key is missing. Also asks the building or the request system for it,
     * which is what checkForToolOrWeapon does for flint and steel.
     */
    public static boolean missing(final AbstractEntityAIBasic<?, ?> ai, final Item key, final int perTrip)
    {
        if (perTrip <= 0 || key == Items.AIR)
        {
            return false;
        }
        final int needed = needed(key, perTrip);
        return !ai.checkIfRequestForItemExistOrCreateAsync(new ItemStack(key), needed, needed);
    }

    /** Uses up the key. Returns false when the worker doesn't have enough, so the portal stays shut. */
    public static boolean consume(final AbstractEntityCitizen worker, final Item key, final int perTrip)
    {
        if (perTrip <= 0 || key == Items.AIR)
        {
            return true;
        }
        final var inv = worker.getInventoryCitizen();
        final int slot = InventoryUtils.findFirstSlotInItemHandlerWith(inv, key);
        if (slot < 0)
        {
            return false;
        }
        final ItemStack stack = inv.getStackInSlot(slot);
        if (stack.isDamageableItem())
        {
            stack.hurtAndBreak(perTrip, (ServerLevel) worker.level(), worker, item -> {});
            return true;
        }
        final ItemStack remainder = stack.getCraftingRemainingItem();
        if (!InventoryUtils.attemptReduceStackInItemHandler(inv, new ItemStack(key), perTrip, true, true))
        {
            return false;
        }
        if (!remainder.isEmpty())
        {
            InventoryUtils.addItemStackToItemHandler(inv, remainder.copyWithCount(perTrip));
        }
        return true;
    }
}
