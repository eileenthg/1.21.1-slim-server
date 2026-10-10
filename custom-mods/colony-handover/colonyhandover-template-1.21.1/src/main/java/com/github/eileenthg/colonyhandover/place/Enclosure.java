package com.github.eileenthg.colonyhandover.place;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;

/**
 * Is this room sealed, and if not, where does it leak?
 *
 * <h2>Why this decides everything</h2>
 *
 * MCA will not seed a structure scan from a spot that can reach the outdoors.
 * {@code StructureScanner.reachesExterior} walks outward through every cell a
 * player could physically stand in, and the moment it stands somewhere with open
 * sky, the whole building is judged to be outdoors - reported, confusingly, as
 * {@code TOO_SMALL} however large the building is.
 *
 * <h2>What stops the walk</h2>
 *
 * Only three blocks: {@code DoorBlock}, {@code FenceGateBlock},
 * {@code TrapDoorBlock}. Everything else is judged by collision shape, and
 * {@code isOpen} means <em>empty collision shape</em> - so banners, torches,
 * carpets on the far side, and plain archways are all holes as far as the scan
 * is concerned. A banner hung as a door curtain stops nobody.
 *
 * <p>This class mirrors those rules so we can report the leak rather than
 * shrugging at a misleading error code.
 */
public final class Enclosure {

    /** MCA's ROOF_SEARCH. */
    private static final int ROOF_SEARCH = 16;

    /** Bounded so a genuinely open build cannot walk us across the map. */
    private static final int WALK_LIMIT = 8192;

    /** How close to the seed an escape must be to read as a hole in the ceiling. */
    private static final int CEILING_RADIUS = 2;

    /**
     * @param escape   the first cell the walk reached that has open sky above it
     * @param crossing the last enclosed cell before it - i.e. the opening itself,
     *                 which is the position actually worth walking to and fixing
     * @param ceiling  true when the sky is directly over the room rather than
     *                 reached by walking out through a wall
     */
    public record Leak(BlockPos escape, BlockPos crossing, boolean ceiling) {}

    /** True if this position is somewhere MCA would accept as a scan seed. */
    public static boolean isWalkableAnchor(ServerLevel level, BlockPos pos) {
        return isOpen(level, pos)
                && isOpen(level, pos.above())
                && isSupported(level, pos)
                && hasRoof(level, pos);
    }

    /**
     * Walks the way {@code reachesExterior} does.
     *
     * @return the leak, or empty when the room is sealed
     */
    public static Optional<Leak> findLeak(ServerLevel level, BlockPos start) {
        final Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        cameFrom.put(start.immutable(), null);
        queue.add(start.immutable());
        int steps = 0;

        while (!queue.isEmpty() && steps++ < WALK_LIMIT) {
            final BlockPos current = queue.removeFirst();
            for (final Direction d : Direction.Plane.HORIZONTAL) {
                final BlockPos horizontal = current.relative(d);
                // MCA allows a single step up or down, so a windowsill or a
                // porch step is walkable and does not seal anything.
                for (final int dy : new int[] {0, 1, -1}) {
                    final BlockPos next = horizontal.offset(0, dy, 0).immutable();
                    if (cameFrom.containsKey(next)) {
                        continue;
                    }
                    final BlockState state = level.getBlockState(next);
                    if (isConnector(state)) {
                        continue;
                    }
                    if (!isOpen(level, next) || !isOpen(level, next.above())
                            || !isSupported(level, next)) {
                        continue;
                    }
                    cameFrom.put(next, current);
                    if (!hasRoof(level, next)) {
                        final boolean ceiling =
                                horizontalDistance(next, start) <= CEILING_RADIUS;
                        return Optional.of(new Leak(next, current, ceiling));
                    }
                    queue.addLast(next);
                }
            }
        }
        return Optional.empty();
    }

    private static int horizontalDistance(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
    }

    /** The only three blocks that stop the exterior walk. */
    public static boolean isConnector(BlockState state) {
        return state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof FenceGateBlock
                || state.getBlock() instanceof TrapDoorBlock;
    }

    /** Open means <em>no collision at all</em> - which is why banners do not seal. */
    public static boolean isOpen(ServerLevel level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty()
                && state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isSupported(ServerLevel level, BlockPos pos) {
        final BlockPos below = pos.below();
        final VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
        if (shape.isEmpty()) {
            return false;
        }
        final double width = shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X);
        final double depth = shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z);
        return width * depth >= 0.25D;
    }

    /** Leaves deliberately do not count, so a tree canopy is not a ceiling. */
    public static boolean hasRoof(ServerLevel level, BlockPos pos) {
        BlockPos cursor = pos;
        for (int i = 0; i < ROOF_SEARCH; i++) {
            cursor = cursor.above();
            final BlockState state = level.getBlockState(cursor);
            if (state.getFluidState().isEmpty()
                    && !state.is(BlockTags.LEAVES)
                    && !state.getCollisionShape(level, cursor).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The opening a leak passes through, measured in the plane of the wall.
     *
     * @param cells  every open cell in the hole
     * @param facing the direction from inside to outside through it
     * @param width  the hole's span across the wall
     * @param height the hole's span vertically
     * @param bottom the lowest cell of the hole, where a door's lower half goes
     */
    public record Gap(List<BlockPos> cells, Direction facing, int width, int height,
                      BlockPos bottom) {

        /**
         * Whether a door or trapdoor can honestly close this.
         *
         * <p>Two blocks wide is a double door; anything wider is a missing wall,
         * and filling it is a construction decision belonging to the player.
         * Two blocks tall takes a door, one takes a trapdoor - both are
         * connectors, so either stops the survey walking out.
         */
        public boolean fixable() {
            return width >= 1 && width <= 2 && height >= 1 && height <= 2;
        }

        public boolean needsTrapdoor() {
            return height == 1;
        }
    }

    /** Beyond this many open cells the hole is a missing wall, not a doorway. */
    private static final int GAP_CELL_LIMIT = 12;

    /**
     * Measures the hole the survey walked through.
     *
     * <p>Measured at the <em>crossing</em> cell rather than the outdoor one. In
     * the ordinary case - a doorway in a wall with blocks above it - the crossing
     * is the doorway itself: still roofed by the lintel, with open sky only once
     * you are past it. That is also exactly where a door belongs.
     *
     * @return the gap, or empty when it is unbounded or too large to be a doorway
     */
    public static Optional<Gap> measureGap(ServerLevel level, Leak leak) {
        final BlockPos from = leak.crossing();
        final BlockPos to = leak.escape();
        final Direction facing = directionBetween(from, to);
        if (facing == null) {
            return Optional.empty();
        }

        // Walk the hole within the wall plane only: the two directions across
        // the wall, plus up and down.
        final Direction across = facing.getClockWise();
        final List<BlockPos> cells = new ArrayList<>();
        final Set<BlockPos> seen = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(from);
        queue.add(from);

        while (!queue.isEmpty()) {
            final BlockPos current = queue.removeFirst();
            cells.add(current);
            if (cells.size() > GAP_CELL_LIMIT) {
                return Optional.empty();
            }
            for (final BlockPos next : new BlockPos[] {
                    current.relative(across), current.relative(across.getOpposite()),
                    current.above(), current.below()}) {
                if (seen.contains(next) || !isOpen(level, next)) {
                    continue;
                }
                seen.add(next);
                queue.addLast(next);
            }
        }

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minAcross = Integer.MAX_VALUE;
        int maxAcross = Integer.MIN_VALUE;
        for (final BlockPos c : cells) {
            minY = Math.min(minY, c.getY());
            maxY = Math.max(maxY, c.getY());
            final int a = across.getAxis() == Direction.Axis.X ? c.getX() : c.getZ();
            minAcross = Math.min(minAcross, a);
            maxAcross = Math.max(maxAcross, a);
        }

        BlockPos bottom = cells.get(0);
        for (final BlockPos c : cells) {
            if (c.getY() < bottom.getY()) {
                bottom = c;
            }
        }
        return Optional.of(new Gap(List.copyOf(cells), facing,
                maxAcross - minAcross + 1, maxY - minY + 1, bottom));
    }

    private static Direction directionBetween(BlockPos from, BlockPos to) {
        for (final Direction d : Direction.Plane.HORIZONTAL) {
            if (from.relative(d).getX() == to.getX() && from.relative(d).getZ() == to.getZ()) {
                return d;
            }
        }
        return null;
    }

    private Enclosure() {}
}
