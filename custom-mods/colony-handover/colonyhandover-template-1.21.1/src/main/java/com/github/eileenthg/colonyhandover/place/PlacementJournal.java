package com.github.eileenthg.colonyhandover.place;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Persistent undo log for everything the handover writes to the world.
 *
 * <h2>Why this exists before any placement code</h2>
 *
 * The handover is the first thing this mod does that cannot be taken back by
 * closing the game. Rather than trying to place blocks correctly first time, the
 * placement engine <em>attempts</em> a placement, asks MCA what it thinks of the
 * result, and undoes it if the answer got worse. That only works if undo is
 * exact, so this is the foundation everything else sits on.
 *
 * <h2>What is recorded</h2>
 *
 * The block state before the write, plus the full block entity NBT if there was
 * one. Capturing the entity matters: a chest holding a colonist's belongings is
 * a plausible thing to overwrite by accident, and restoring the block without
 * its contents would be silent data loss dressed up as an undo.
 *
 * <h2>Persistence</h2>
 *
 * Stored as {@link SavedData} against the level, so history survives a restart
 * or a crash. That is the difference between "undo works while you are testing"
 * and "undo works", and a handover is exactly the operation where the server
 * might not still be running when you decide you want it back.
 *
 * <p>{@code SavedData} is per dimension, which is also the right scope: a
 * recorded position only means anything in the level it was taken from.
 */
public final class PlacementJournal extends SavedData {

    private static final String DATA_ID = "colonyhandover_journal";

    /** Enough to unwind a session of experimenting without growing without bound. */
    private static final int MAX_HISTORY = 16;

    /** One block we overwrote, and everything needed to put it back. */
    public record Change(BlockPos pos, BlockState oldState, CompoundTag oldEntity) {}

    /**
     * A group of changes that succeed or are undone together.
     *
     * <p>Built up in memory during a placement attempt, then either restored
     * (the attempt failed) or handed to {@link #commit} (it worked).
     */
    public static final class Transaction {
        private final String label;
        private final long created;
        private final List<Change> changes = new ArrayList<>();
        /** Positions already captured, so rewriting one block does not stack entries. */
        private final Set<BlockPos> seen = new HashSet<>();

        private Transaction(String label, long created) {
            this.label = label;
            this.created = created;
        }

        public String label() {
            return label;
        }

        public int size() {
            return changes.size();
        }

        public boolean isEmpty() {
            return changes.isEmpty();
        }

        /**
         * Records the current state at a position, unless already recorded in
         * this transaction. Call immediately <em>before</em> writing.
         */
        public void capture(ServerLevel level, BlockPos pos) {
            final BlockPos key = pos.immutable();
            if (!seen.add(key)) {
                return;
            }
            CompoundTag entity = null;
            final BlockEntity be = level.getBlockEntity(key);
            if (be != null) {
                entity = be.saveWithFullMetadata(level.registryAccess());
            }
            changes.add(new Change(key, level.getBlockState(key), entity));
        }

        /**
         * Puts every change back, newest first.
         *
         * <p>Reverse order matters for blocks that depend on their neighbours:
         * undo the last write first and each restore sees the world as it was
         * when that write happened.
         */
        public void restore(ServerLevel level) {
            for (int i = changes.size() - 1; i >= 0; i--) {
                final Change change = changes.get(i);
                level.setBlock(change.pos(), change.oldState(), Block.UPDATE_ALL);
                if (change.oldEntity() != null) {
                    final BlockEntity be = level.getBlockEntity(change.pos());
                    if (be != null) {
                        be.loadWithComponents(change.oldEntity(), level.registryAccess());
                        be.setChanged();
                    }
                }
            }
            changes.clear();
            seen.clear();
        }

        private CompoundTag save(HolderLookup.Provider provider) {
            final CompoundTag tag = new CompoundTag();
            tag.putString("label", label);
            tag.putLong("created", created);
            final ListTag list = new ListTag();
            for (final Change change : changes) {
                final CompoundTag entry = new CompoundTag();
                entry.putLong("pos", change.pos().asLong());
                entry.put("state", NbtUtils.writeBlockState(change.oldState()));
                if (change.oldEntity() != null) {
                    entry.put("entity", change.oldEntity());
                }
                list.add(entry);
            }
            tag.put("changes", list);
            return tag;
        }

        private static Transaction load(CompoundTag tag, HolderGetter<Block> blocks) {
            final Transaction transaction =
                    new Transaction(tag.getString("label"), tag.getLong("created"));
            final ListTag list = tag.getList("changes", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                final CompoundTag entry = list.getCompound(i);
                final BlockPos pos = BlockPos.of(entry.getLong("pos"));
                final CompoundTag stateTag = entry.getCompound("state");
                final BlockState state = NbtUtils.readBlockState(blocks, stateTag);
                final CompoundTag entity = entry.contains("entity", Tag.TAG_COMPOUND)
                        ? entry.getCompound("entity")
                        : null;

                // readBlockState yields air for a block that is no longer
                // registered, so air plus a non-air saved name means the mod
                // that provided it has been removed. Skip rather than restore:
                // writing air over whatever is there now would turn a failed
                // undo into a hole in someone's build.
                final String savedName = stateTag.getString("Name");
                if (state.isAir() && !savedName.isEmpty() && !"minecraft:air".equals(savedName)) {
                    ColonyHandover.LOGGER.warn(
                            "Journal: dropping undo entry at {} - block '{}' is no longer registered",
                            pos, savedName);
                    continue;
                }

                transaction.changes.add(new Change(pos, state, entity));
                transaction.seen.add(pos);
            }
            return transaction;
        }
    }

    private final Deque<Transaction> history = new ArrayDeque<>();

    private PlacementJournal() {
    }

    /** The journal for this dimension, created on first use. */
    public static PlacementJournal get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PlacementJournal::new, PlacementJournal::load, null),
                DATA_ID);
    }

    /** Starts an uncommitted transaction. Nothing is persisted until {@link #commit}. */
    public static Transaction begin(String label) {
        return new Transaction(label, System.currentTimeMillis());
    }

    /** Files a completed transaction so it can be undone later. Empty ones are dropped. */
    public void commit(Transaction transaction) {
        if (transaction.isEmpty()) {
            return;
        }
        history.addLast(transaction);
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
        setDirty();
        ColonyHandover.LOGGER.info("Journal: committed '{}' with {} change(s)",
                transaction.label(), transaction.size());
    }

    /** Undoes the most recent committed transaction in this dimension. */
    public Optional<Transaction> undoLast(ServerLevel level) {
        if (history.isEmpty()) {
            return Optional.empty();
        }
        final Transaction transaction = history.removeLast();
        final int count = transaction.size();
        transaction.restore(level);
        setDirty();
        ColonyHandover.LOGGER.info("Journal: undid '{}' ({} change(s))",
                transaction.label(), count);
        return Optional.of(transaction);
    }

    public int historySize() {
        return history.size();
    }

    public Optional<String> lastLabel() {
        return history.isEmpty() ? Optional.empty() : Optional.of(history.peekLast().label());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        final ListTag list = new ListTag();
        for (final Transaction transaction : history) {
            list.add(transaction.save(provider));
        }
        tag.put("transactions", list);
        return tag;
    }

    private static PlacementJournal load(CompoundTag tag, HolderLookup.Provider provider) {
        final PlacementJournal journal = new PlacementJournal();
        final HolderGetter<Block> blocks = provider.lookupOrThrow(Registries.BLOCK);
        final ListTag list = tag.getList("transactions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            journal.history.addLast(Transaction.load(list.getCompound(i), blocks));
        }
        ColonyHandover.LOGGER.info("Journal: loaded {} undoable transaction(s)",
                journal.history.size());
        return journal;
    }
}
