package com.github.eileenthg.colonyhandover.coffer;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A read-only, paged window over the rescued goods.
 *
 * <h2>One-way by construction, not by policy</h2>
 *
 * Every coffer slot returns {@code false} from {@link Slot#mayPlace}, and
 * {@link #quickMoveStack} refuses to move anything from the player side. So there is
 * no path that puts an item in - not a click, not a shift-click, not a drag. The
 * coffer is a place things leave.
 *
 * <p>That matters because the coffer is not a storage solution people should come to
 * rely on. It exists to hand back what the colonists were carrying when their colony
 * ended, once. Letting it accept deposits would quietly turn it into an infinite
 * bottomless chest, which is a different mod.
 *
 * <h2>Paging without a custom packet</h2>
 *
 * Page changes go through {@link #clickMenuButton}, the same vanilla mechanism the
 * enchanting table and stonecutter use. The client already knows how to send it, so
 * this needs no networking code of its own - which is worth a lot, since a custom
 * payload is the sort of thing that works in single player and breaks on a server.
 */
public final class CofferMenu extends AbstractContainerMenu {

    public static final int BUTTON_PREV = 0;
    public static final int BUTTON_NEXT = 1;

    private final CofferView view;
    private final DataSlot page = DataSlot.standalone();
    private final DataSlot pageCount = DataSlot.standalone();

    /** Server side: a real coffer, tidied of last session's gaps before it is shown. */
    public CofferMenu(int containerId, Inventory playerInventory, CofferData data) {
        this(containerId, playerInventory, compacted(data));
    }

    private static CofferView compacted(CofferData data) {
        data.compact();
        return new CofferView(data);
    }

    /** Client side: the contents arrive through normal slot sync, so back it with nothing. */
    public CofferMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new CofferView(CofferData.empty()));
    }

    private CofferMenu(int containerId, Inventory playerInventory, CofferView view) {
        super(ColonyHandover.COFFER_MENU.get(), containerId);
        this.view = view;

        // Six rows of nine, laid out for the vanilla generic_54 texture.
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(view, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        // Player inventory and hotbar, at the offsets a six-row chest uses.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 139 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 197));
        }

        addDataSlot(page);
        addDataSlot(pageCount);
        syncCounters();
    }

    private void syncCounters() {
        page.set(view.page());
        pageCount.set(view.pageCount());
    }

    public int currentPage() {
        return page.get();
    }

    public int totalPages() {
        return Math.max(1, pageCount.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        final int target = switch (id) {
            case BUTTON_PREV -> view.page() - 1;
            case BUTTON_NEXT -> view.page() + 1;
            default -> view.page();
        };
        if (target < 0 || target >= view.pageCount()) {
            return false;
        }
        view.setPage(target);
        syncCounters();
        // Every slot now shows a different stack, so an incremental diff is not
        // enough - the client's whole picture has to be replaced.
        broadcastFullState();
        return true;
    }

    /**
     * Shift-click moves goods out, and only out.
     *
     * <p>A shift-click on the player's own side would normally push into the
     * container; here it does nothing at all rather than silently failing halfway.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (index >= CofferView.PAGE_SIZE) {
            return ItemStack.EMPTY;
        }

        final ItemStack inSlot = slot.getItem();
        final ItemStack original = inSlot.copy();
        if (!moveItemStackTo(inSlot, CofferView.PAGE_SIZE, slots.size(), true)) {
            return ItemStack.EMPTY;
        }
        // An emptied entry stays as a gap until the coffer is next opened, so nothing
        // slides under the cursor while you are still taking things out.
        slot.set(inSlot.isEmpty() ? ItemStack.EMPTY : inSlot);
        slot.onTake(player, inSlot);
        // Server only. The client predicts shift-clicks by running this same method
        // against its empty placeholder view, which would reset the counters to
        // 0 of 0 - and the server, whose values never changed, would never resend them.
        if (!player.level().isClientSide) {
            syncCounters();
        }
        return original;
    }

    /** Nothing to stand next to - the coffer is opened by command, not by a block. */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            view.data().compact();
        }
        // A carried stack would otherwise vanish with the screen. Hand it back
        // rather than deleting goods we went to some trouble to rescue.
        if (!player.level().isClientSide) {
            final ItemStack carried = getCarried();
            if (!carried.isEmpty()) {
                setCarried(ItemStack.EMPTY);
                if (!player.getInventory().add(carried)) {
                    player.drop(carried, false);
                }
            }
        }
    }
}
