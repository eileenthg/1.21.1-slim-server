package com.github.eileenthg.colonyhandover.coffer;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A fixed-size window onto one page of a {@link CofferData}.
 *
 * <h2>Why a view rather than a copy</h2>
 *
 * Vanilla's slot machinery insists on a {@link Container} of known size, but the
 * coffer holds however many stacks the colony happened to have. Copying a page into
 * a temporary container would mean writing it back afterwards and getting that
 * synchronisation right on every path - takes, shift-clicks, drags, closing the
 * screen mid-drag. A view has no second copy to fall out of step: every read and
 * write goes straight through to the list, offset by the page.
 *
 * <p>On the server this is backed by real data. On the client it is backed by an
 * empty {@code CofferData}, because slot contents arrive through the normal
 * container sync rather than from here.
 */
public final class CofferView implements Container {

    /** Six rows of nine - the vanilla double-chest layout, so the shipped texture fits. */
    public static final int PAGE_SIZE = 54;

    private final CofferData data;
    private int page;

    public CofferView(CofferData data) {
        this.data = data;
    }

    public CofferData data() {
        return data;
    }

    public int page() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, Math.min(page, data.pages(PAGE_SIZE) - 1));
    }

    public int pageCount() {
        return data.pages(PAGE_SIZE);
    }

    private int index(int slot) {
        return page * PAGE_SIZE + slot;
    }

    @Override
    public int getContainerSize() {
        return PAGE_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return data.get(index(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        final ItemStack held = data.get(index(slot));
        if (held.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack taken = held.split(count);
        // split() mutates in place, so write the remainder back - which also
        // removes the entry entirely when it hits zero, keeping the list dense so
        // pages do not develop holes as things are collected.
        data.set(index(slot), held);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        final ItemStack held = data.get(index(slot));
        data.set(index(slot), ItemStack.EMPTY);
        return held;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        data.set(index(slot), stack);
    }

    @Override
    public void setChanged() {
        data.setDirty();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < PAGE_SIZE; i++) {
            data.set(index(i), ItemStack.EMPTY);
        }
    }
}
