package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * Items that pop out of a station a Chef is using (skillet, campfire, stove, cutting board) belong to her job.
 * They get a pickup delay so players and other maids can't grab them before she does (TLM maids respect it).
 * If a player picks one up anyway, the Chef is told so she can ask for it back.
 */
public final class ChefClaims {
    static final int PICKUP_DELAY = 200;

    private record Active(WeakReference<EntityMaid> maid, Predicate<Item> wanted) {
    }

    private record Key(ResourceKey<Level> dim, BlockPos pos) {
    }

    private static final Map<Key, Active> STATIONS = new HashMap<>();
    private static final Map<UUID, WeakReference<EntityMaid>> CLAIMED = new HashMap<>();

    private ChefClaims() {
    }

    public static synchronized void activate(EntityMaid maid, BlockPos station, Predicate<Item> wanted) {
        STATIONS.put(new Key(maid.level().dimension(), station.immutable()), new Active(new WeakReference<>(maid), wanted));
    }

    public static synchronized void deactivate(EntityMaid maid, BlockPos station) {
        STATIONS.remove(new Key(maid.level().dimension(), station));
    }

    public static synchronized void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item) || STATIONS.isEmpty()) return;
        BlockPos at = item.blockPosition();
        Item it = item.getItem().getItem();
        for (Iterator<Map.Entry<Key, Active>> i = STATIONS.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<Key, Active> e = i.next();
            EntityMaid maid = e.getValue().maid().get();
            if (maid == null || maid.isRemoved()) {
                i.remove();
                continue;
            }
            Key k = e.getKey();
            if (k.dim() != event.getLevel().dimension()) continue;
            if (Math.abs(at.getX() - k.pos().getX()) > 2 || Math.abs(at.getZ() - k.pos().getZ()) > 2
                    || at.getY() < k.pos().getY() - 1 || at.getY() > k.pos().getY() + 3) continue;
            if (!e.getValue().wanted().test(it)) continue;
            item.setPickUpDelay(PICKUP_DELAY);
            CLAIMED.put(item.getUUID(), new WeakReference<>(maid));
            return;
        }
    }

    public static void onPlayerPickup(ItemEntityPickupEvent.Post event) {
        WeakReference<EntityMaid> ref;
        synchronized (ChefClaims.class) {
            ref = CLAIMED.remove(event.getItemEntity().getUUID());
            if (CLAIMED.size() > 4096) CLAIMED.clear();
        }
        EntityMaid maid = ref == null ? null : ref.get();
        if (maid == null || maid.isRemoved()) return;
        ItemStack original = event.getOriginalStack();
        int taken = original.getCount() - event.getCurrentStack().getCount();
        if (taken <= 0) return;
        ChefBehavior.owed(maid, event.getPlayer(), original.getItem(), taken);
    }

    /** Called when the Chef picks the item up herself (it is hers again). */
    public static synchronized void release(UUID itemEntity) {
        CLAIMED.remove(itemEntity);
    }
}
