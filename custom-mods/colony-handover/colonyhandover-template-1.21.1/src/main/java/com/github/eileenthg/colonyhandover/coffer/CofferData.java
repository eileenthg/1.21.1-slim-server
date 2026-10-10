package com.github.eileenthg.colonyhandover.coffer;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * What the colonists were carrying, held until someone comes to collect it.
 *
 * <h2>Why this exists</h2>
 *
 * The handover retires citizen entities silently rather than letting MineColonies
 * kill them, because {@code EntityCitizen.die} sends an obituary to every colony
 * manager and spawns a grave. That quiet removal takes their inventories with it -
 * 27 slots each, expanding with upgrades, so a mid-sized colony is several hundred
 * stacks. Dropping them would carpet the village; discarding them loses real goods.
 * They come here instead.
 *
 * <h2>Why a flat list rather than a container</h2>
 *
 * Vanilla containers have a fixed size, and there is no size that is right: one
 * chest is far too small and enough chests to be safe is mostly empty. A list grows
 * to whatever the colony actually had, and {@link CofferView} pages a window over
 * it, so the storage and the way it is displayed are independent.
 *
 * <p>Per dimension, like the placement journal - a colony lives in one world, and
 * so should the things its people were carrying.
 */
public final class CofferData extends SavedData {

    private static final String DATA_ID = "colonyhandover_coffer";
    private static final String TAG_ITEMS = "Items";

    private final List<ItemStack> items = new ArrayList<>();

    public static CofferData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CofferData::new, CofferData::load, null), DATA_ID);
    }

    private CofferData() {}

    /** A detached instance for the client menu, which never reads from it. */
    public static CofferData empty() {
        return new CofferData();
    }

    /**
     * Adds a stack, merging into a partial stack of the same item where possible.
     *
     * <p>Merging matters more than it looks: fourteen colonists each carrying nine
     * cobblestone is fourteen slots of clutter to page through, or one stack to
     * take. Rescued goods are worth tidying on the way in.
     */
    public void add(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        final ItemStack incoming = stack.copy();
        for (final ItemStack held : items) {
            if (!ItemStack.isSameItemSameComponents(held, incoming)) {
                continue;
            }
            final int room = held.getMaxStackSize() - held.getCount();
            if (room <= 0) {
                continue;
            }
            final int moved = Math.min(room, incoming.getCount());
            held.grow(moved);
            incoming.shrink(moved);
            if (incoming.isEmpty()) {
                setDirty();
                return;
            }
        }
        items.add(incoming);
        setDirty();
    }

    public int size() {
        return items.size();
    }

    /** True when nothing is left to take - placeholders from this session do not count. */
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    /**
     * Drops the gaps left by taken stacks.
     *
     * <p>Only ever called when the coffer is opened or closed, never while someone is
     * looking at it. Removing entries mid-session shifted every later stack one slot
     * to the left, so the thing under your cursor changed as you clicked.
     */
    public void compact() {
        if (items.removeIf(ItemStack::isEmpty)) {
            setDirty();
        }
    }

    public ItemStack get(int index) {
        return index >= 0 && index < items.size() ? items.get(index) : ItemStack.EMPTY;
    }

    /**
     * Writes one entry, leaving an empty placeholder rather than removing it.
     *
     * <p>Grows the list when writing past its end. The server never needs that, but
     * the client's copy starts empty and is filled by ordinary slot sync, which writes
     * by index - refusing out-of-range writes is why the coffer's items were invisible
     * while still being clickable.
     */
    public void set(int index, ItemStack stack) {
        if (index < 0) {
            return;
        }
        while (items.size() <= index) {
            items.add(ItemStack.EMPTY);
        }
        items.set(index, stack);
        setDirty();
    }

    /** How many pages a window of this size needs; always at least one. */
    public int pages(int pageSize) {
        return Math.max(1, (items.size() + pageSize - 1) / pageSize);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (final ItemStack stack : items) {
            if (!stack.isEmpty()) {
                list.add(stack.save(registries));
            }
        }
        tag.put(TAG_ITEMS, list);
        return tag;
    }

    private static CofferData load(CompoundTag tag, HolderLookup.Provider registries) {
        final CofferData data = new CofferData();
        final ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            // An item whose mod has been removed since the handover cannot be
            // restored; skipping beats refusing to load the whole coffer.
            ItemStack.parse(registries, list.getCompound(i))
                    .ifPresent(stack -> data.items.add(stack));
        }
        return data;
    }
}
