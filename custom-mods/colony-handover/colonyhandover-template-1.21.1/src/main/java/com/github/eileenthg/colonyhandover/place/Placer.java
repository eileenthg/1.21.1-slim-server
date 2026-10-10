package com.github.eileenthg.colonyhandover.place;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Chooses where a workstation can go inside a building, and puts it there.
 *
 * <h2>Candidates come from MCA, not from us</h2>
 *
 * The obvious approach is to flood fill the building ourselves and look for
 * space. We do not need to: {@code Building.getFloorRegions()} already hands
 * back every floor cell MCA found, and {@code StructureScanner.isSupported}
 * defines a floor cell as a standable position with solid ground beneath it -
 * which is exactly where a workstation belongs. Using MCA's own geometry means
 * our idea of the room cannot drift from the one it will judge us by.
 *
 * <h2>The one way placement can genuinely break a building</h2>
 *
 * A placed block stops the flood fill passing through it. Volume alone is not
 * the risk - the size gate counts the scanned envelope, so swapping an air cell
 * for a solid one is roughly neutral. The real hazard is sealing a chokepoint:
 * put a block in a one-wide corridor and everything beyond it drops out of the
 * scan entirely. {@link #isChokepoint} is what guards against that.
 */
public final class Placer {

    /** How far up MCA looks for a ceiling before calling a spot open sky. */
    private static final int ROOF_SCAN_HEIGHT = 16;

    /** Reasons a position was rejected, for reporting rather than silent skipping. */
    public enum Rejection {
        OCCUPIED, HAS_BLOCK_ENTITY, NO_HEADROOM, NEAR_DOOR, CHOKEPOINT, CANNOT_SURVIVE, NONE
    }

    /**
     * Ranked placement positions, best first.
     *
     * <p>Wall-adjacent cells sort first: a workstation against a wall leaves the
     * middle of the room clear for villagers to path through, which both looks
     * right and avoids boxing anyone in.
     */
    public static List<BlockPos> rankedCandidates(ServerLevel level, Collection<BlockPos> floorCells) {
        final List<BlockPos> usable = new ArrayList<>();
        for (final BlockPos cell : floorCells) {
            if (reject(level, cell) == Rejection.NONE) {
                usable.add(cell.immutable());
            }
        }
        usable.sort(Comparator
                .comparingInt((BlockPos p) -> wallContact(level, p)).reversed()
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));
        return usable;
    }

    /** Why this position is unusable, or {@link Rejection#NONE} if it is fine. */
    public static Rejection reject(ServerLevel level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);

        // Never overwrite anything that holds data - a chest full of a
        // colonist's belongings is exactly the thing not to quietly destroy.
        if (level.getBlockEntity(pos) != null) {
            return Rejection.HAS_BLOCK_ENTITY;
        }
        if (!state.canBeReplaced()) {
            return Rejection.OCCUPIED;
        }
        // Leave head height clear so villagers can still stand here.
        if (!level.getBlockState(pos.above()).canBeReplaced()) {
            return Rejection.NO_HEADROOM;
        }
        if (nearDoor(level, pos)) {
            return Rejection.NEAR_DOOR;
        }
        if (isChokepoint(level, pos)) {
            return Rejection.CHOKEPOINT;
        }
        return Rejection.NONE;
    }

    /**
     * True if blocking this cell could sever the room.
     *
     * <p>Counts open horizontal neighbours: two or fewer means the cell is a
     * corridor or a dead end, and filling it risks cutting off whatever lies
     * beyond. Open rooms have three or four.
     */
    public static boolean isChokepoint(ServerLevel level, BlockPos pos) {
        int open = 0;
        for (final Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(d)).canBeReplaced()) {
                open++;
            }
        }
        return open <= 2;
    }

    /** Doors are how the scan enters and how villagers move; keep clear of them. */
    public static boolean nearDoor(ServerLevel level, BlockPos pos) {
        for (final Direction d : Direction.values()) {
            if (level.getBlockState(pos.relative(d)).getBlock() instanceof DoorBlock) {
                return true;
            }
        }
        return level.getBlockState(pos).getBlock() instanceof DoorBlock;
    }

    /** How many horizontal sides are solid - higher means more tucked against a wall. */
    public static int wallContact(ServerLevel level, BlockPos pos) {
        int solid = 0;
        for (final Direction d : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(d)).canBeReplaced()) {
                solid++;
            }
        }
        return solid;
    }

    /**
     * Whether a villager could stand here and a workstation could sit here.
     *
     * <p>Mirrors {@code StructureScanner.isSupported}: the block below must
     * present at least a quarter of a full face of collision. Used for the
     * open-air sites - barns, pens, mine heads - where there is no room scan to
     * hand us floor cells, so we have to find footing ourselves.
     */
    public static boolean isStandable(ServerLevel level, BlockPos pos) {
        final BlockPos below = pos.below();
        final VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
        if (shape.isEmpty()) {
            return false;
        }
        final double width = shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X);
        final double depth = shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z);
        if (width * depth < 0.25D) {
            return false;
        }
        return level.getBlockState(pos).canBeReplaced()
                && level.getBlockState(pos.above()).canBeReplaced();
    }

    /**
     * Standable sites near a position, nearest first.
     *
     * <p>Used for job-site-only huts, where the workstation goes beside the hut
     * rather than inside a validated room. Deliberately does not apply the
     * chokepoint test: outdoors it is meaningless, and there is no flood fill to
     * protect.
     */
    public static List<BlockPos> nearbySites(ServerLevel level, BlockPos centre,
                                             int radius, int vertical) {
        final List<BlockPos> found = new ArrayList<>();
        for (int dy = vertical; dy >= -vertical; dy--) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    final BlockPos pos = centre.offset(dx, dy, dz);
                    if (level.getBlockEntity(pos) == null && isStandable(level, pos)) {
                        found.add(pos);
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(p -> p.distSqr(centre)));
        return found;
    }

    /**
     * Whether this position has something overhead that counts as a roof.
     *
     * <p>Mirrors MCA's own roof test: scan upward a limited distance for the
     * first non-air block, and reject leaves so a tree canopy is not a ceiling.
     */
    public static boolean hasRoof(ServerLevel level, BlockPos pos) {
        BlockPos cursor = pos;
        for (int i = 0; i < ROOF_SCAN_HEIGHT; i++) {
            cursor = cursor.above();
            final BlockState state = level.getBlockState(cursor);
            if (!state.isAir()) {
                return !state.is(BlockTags.LEAVES);
            }
        }
        return false;
    }

    /**
     * A position MCA will accept as a scan seed.
     *
     * <p>This is the fix for the whole colony reporting TOO_SMALL. MCA seeds a
     * structure scan from a <em>walkable anchor</em> - open at foot and head
     * height, solid ground beneath, roof overhead. A MineColonies hut block is
     * none of those: it is a solid marker. Handing it to
     * {@code analyzeRoomAddition} makes {@code resolveExactSeed} return empty,
     * and the scan fails as TOO_SMALL before it measures a single block.
     *
     * <p>So we look for somewhere a villager could actually stand, searching
     * upward first because the hut block usually sits at or below floor level.
     */
    public static Optional<BlockPos> findInteriorSeed(ServerLevel level, BlockPos near,
                                                      int radius, int vertical) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dy = 0; dy <= vertical; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    final BlockPos pos = near.offset(dx, dy, dz);
                    if (!isStandable(level, pos) || !hasRoof(level, pos)) {
                        continue;
                    }
                    final double d = pos.distSqr(near);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * The best seed near where the player is standing.
     *
     * <p>Tries their own position, then one block up, then a small radius. The
     * step up matters more than it looks. Any block shorter than a full cube -
     * carpet (1/16), a slab (8/16), a dirt path (15/16) - lets the player's feet
     * sink below its top face, so {@code blockPosition()} returns the block
     * itself, and {@code isOpen} means empty collision, so the survey reads that
     * as "inside a block". The cell above is a perfectly good anchor - the short
     * block supports it - so such a room is scannable, we just start one higher.
     */
    public static Optional<BlockPos> findSeedNear(ServerLevel level, BlockPos playerPos) {
        for (final BlockPos candidate : new BlockPos[] {playerPos, playerPos.above()}) {
            if (isStandable(level, candidate) && hasRoof(level, candidate)) {
                return Optional.of(candidate.immutable());
            }
        }
        return findInteriorSeed(level, playerPos, 2, 2);
    }

    /** How many of this block already stand within the box. */
    public static int countNearby(ServerLevel level, BlockPos centre,
                                  int radius, int vertical, Block block) {
        int n = 0;
        for (int dy = -vertical; dy <= vertical; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (level.getBlockState(centre.offset(dx, dy, dz)).is(block)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /**
     * Places a bed, which is two blocks and only counts as one.
     *
     * <p>MCA tallies a bed only when it sees {@code BedPart.HEAD}
     * ({@code Building.addBlock}), and {@code defaultBlockState()} is the FOOT.
     * Placing the default state therefore produces a bed MCA never counts - and
     * a half-bed that pops off on the next block update.
     *
     * @return the two positions used, or empty when there is no room for a pair
     */
    /**
     * Which way a bed could lie from this foot cell, without writing anything.
     *
     * <p>Split out from {@link #placeBed} so a dry run can ask the same question
     * the real placement will answer. Predicting a bed by counting free cells is
     * wrong - two cells are not a bed unless they are <em>adjacent</em> and both
     * usable - and a readiness report that says a room fits when it does not is
     * worse than no report.
     */
    public static Optional<Direction> bedDirection(ServerLevel level, BlockPos foot,
                                                   Block bed, Collection<BlockPos> free) {
        for (final Direction d : Direction.Plane.HORIZONTAL) {
            final BlockPos head = foot.relative(d);
            if (!free.contains(head) || reject(level, head) != Rejection.NONE) {
                continue;
            }
            final BlockState footState = bed.defaultBlockState()
                    .setValue(BedBlock.FACING, d)
                    .setValue(BedBlock.PART, BedPart.FOOT)
                    .setValue(BedBlock.OCCUPIED, false);
            if (footState.canSurvive(level, foot)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    public static List<BlockPos> placeBed(ServerLevel level, PlacementJournal.Transaction tx,
                                          BlockPos foot, Block bed, Collection<BlockPos> free) {
        final Optional<Direction> dir = bedDirection(level, foot, bed, free);
        if (dir.isEmpty()) {
            return List.of();
        }
        final BlockPos head = foot.relative(dir.get());
        final BlockState footState = bed.defaultBlockState()
                .setValue(BedBlock.FACING, dir.get())
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(BedBlock.OCCUPIED, false);
        tx.capture(level, foot);
        tx.capture(level, head);
        level.setBlock(foot, footState, Block.UPDATE_ALL);
        level.setBlock(head, footState.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
        return List.of(foot, head);
    }

    /**
     * Places one block, recording the old state first.
     *
     * @return true if it was placed; false if the block could not survive there,
     *         in which case nothing was written and nothing was journalled
     */
    public static boolean place(ServerLevel level, PlacementJournal.Transaction transaction,
                                BlockPos pos, Block block) {
        final BlockState state = block.defaultBlockState();
        if (!state.canSurvive(level, pos)) {
            return false;
        }
        transaction.capture(level, pos);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        return true;
    }

    private Placer() {}
}
