package eteil.settledcaves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Runs once per chunk in the TOP_LAYER_MODIFICATION step - the last decoration step, so carvers,
 * aquifers and ore/gravel blobs have all already been placed.
 *
 * <p>Reads may reach into the surrounding 3x3 chunks (that is the region a feature is given), so
 * group sizes are measured properly across chunk borders. Writes are restricted to the centre
 * chunk only, which avoids "setBlock in a far chunk" warnings; the neighbouring chunk handles its
 * own side when it generates.
 */
public class SettleFeature extends Feature<NoneFeatureConfiguration> {

    /** Directions water can escape through. Up is excluded - water never flows upward. */
    private static final Direction[] OUTFLOW = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN
    };
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    /** Hard ceiling on any single flood-fill, so a pathological lake can't stall generation. */
    private static final int BFS_HARD_CAP = 4096;

    public SettleFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ChunkPos cpos = new ChunkPos(ctx.origin());
        ChunkAccess chunk = level.getChunk(cpos.x, cpos.z);
        Area area = new Area(cpos, chunk.getMinBuildHeight(), chunk.getMaxBuildHeight());

        boolean changed = false;
        // Order matters: seal fluid contacts first, then walls, then supports. Each pass re-reads.
        if (Config.FLUID_SEAM_ENABLED.get()) changed |= fluidSeam(level, chunk, area);
        if (Config.APERTURE_ENABLED.get()) changed |= apertureSeal(level, chunk, area);
        if (Config.GRAVEL_CAP_ENABLED.get()) changed |= gravelCap(level, chunk, area);
        return changed;
    }

    // =====================================================================================
    // Rule 1: water touching lava
    // =====================================================================================

    private boolean fluidSeam(WorldGenLevel level, ChunkAccess chunk, Area area) {
        int maxY = Config.FLUID_SEAM_MAX_Y.get();
        Map<BlockPos, BlockState> edits = new HashMap<>();

        forEach(chunk, area, maxY, s -> s.getFluidState().is(FluidTags.LAVA), (pos, state) -> {
            FluidState lava = state.getFluidState();
            for (Direction d : Direction.values()) {
                FluidState other = level.getFluidState(pos.relative(d));
                if (!other.is(FluidTags.WATER)) {
                    continue;
                }
                // Convert the lava side so water lakes keep their volume.
                boolean bothSources = lava.isSource() && other.isSource();
                edits.put(pos.immutable(), bothSources ? Blocks.OBSIDIAN.defaultBlockState() : filler(pos.getY()));
                return;
            }
        });

        return apply(level, area, edits);
    }

    // =====================================================================================
    // Rule 2: wide water breaches
    // =====================================================================================

    private boolean apertureSeal(WorldGenLevel level, ChunkAccess chunk, Area area) {
        int maxY = Config.APERTURE_MAX_Y.get();
        int maxOpening = Config.APERTURE_MAX_OPENING.get();

        // Seeds: every opening in the centre chunk.
        List<BlockPos> seeds = new ArrayList<>();
        forEach(chunk, area, maxY, s -> s.getFluidState().is(FluidTags.WATER), (pos, state) -> {
            if (isOpening(level, pos)) {
                seeds.add(pos.immutable());
            }
        });
        if (seeds.isEmpty()) {
            return false;
        }

        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, BlockState> edits = new HashMap<>();

        for (BlockPos seed : seeds) {
            if (visited.contains(seed)) {
                continue;
            }
            // Flood the opening group, reading across chunk borders to get its true size.
            List<BlockPos> group = flood(seed, visited, area, maxY, p -> isWater(level, p) && isOpening(level, p), true);
            if (group.size() <= maxOpening) {
                continue; // a trickle - leave it be
            }
            for (BlockPos w : group) {
                if (!area.inCentre(w)) {
                    continue;
                }
                for (Direction d : OUTFLOW) {
                    BlockPos n = w.relative(d);
                    if (level.getBlockState(n).isAir()) {
                        edits.put(n, filler(n.getY()));
                    }
                }
            }
        }

        return apply(level, area, edits);
    }

    private static boolean isWater(WorldGenLevel level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    /** True when this water block has air beside it or directly below it. */
    private static boolean isOpening(WorldGenLevel level, BlockPos pos) {
        for (Direction d : OUTFLOW) {
            if (level.getBlockState(pos.relative(d)).isAir()) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================================
    // Rule 3: falling-block footprint
    // =====================================================================================

    private boolean gravelCap(WorldGenLevel level, ChunkAccess chunk, Area area) {
        int maxY = Config.GRAVEL_MAX_Y.get();
        // The grid quantises the cap: cells between support lines are (k-1) x (k-1).
        int k = (int) Math.floor(Math.sqrt(Config.GRAVEL_MAX_FOOTPRINT.get())) + 1;

        List<BlockPos> seeds = new ArrayList<>();
        forEach(chunk, area, maxY, s -> s.getBlock() instanceof FallingBlock, (pos, state) -> {
            if (level.getBlockState(pos.below()).isAir()) {
                seeds.add(pos.immutable());
            }
        });
        if (seeds.isEmpty()) {
            return false;
        }

        int maxFootprint = Config.GRAVEL_MAX_FOOTPRINT.get();
        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, BlockState> edits = new HashMap<>();

        for (BlockPos seed : seeds) {
            if (visited.contains(seed)) {
                continue;
            }
            final int layerY = seed.getY();
            // Horizontal-only flood on this layer: falling is per column, so footprints are 2D.
            List<BlockPos> footprint = flood(seed, visited, area, maxY,
                    p -> p.getY() == layerY && isExposedFaller(level, p), false);
            if (footprint.size() <= maxFootprint) {
                continue; // small enough to be a satisfying cave-in
            }
            for (BlockPos g : footprint) {
                if (!area.inCentre(g)) {
                    continue;
                }
                // World-aligned grid, so neighbouring chunks draw matching lines.
                if (Math.floorMod(g.getX(), k) == 0 || Math.floorMod(g.getZ(), k) == 0) {
                    // Replace the exposed block itself: keeps the ceiling flush instead of a stone
                    // nub hanging into the cave, and supports the column above it.
                    edits.put(g, filler(g.getY()));
                }
            }
        }

        return apply(level, area, edits);
    }

    private static boolean isExposedFaller(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FallingBlock
                && level.getBlockState(pos.below()).isAir();
    }

    // =====================================================================================
    // Shared helpers
    // =====================================================================================

    private static BlockState filler(int y) {
        return (y < 0 && Config.USE_DEEPSLATE_BELOW_ZERO.get())
                ? Blocks.DEEPSLATE.defaultBlockState()
                : Blocks.STONE.defaultBlockState();
    }

    private static boolean apply(WorldGenLevel level, Area area, Map<BlockPos, BlockState> edits) {
        boolean changed = false;
        for (Map.Entry<BlockPos, BlockState> e : edits.entrySet()) {
            if (area.inCentre(e.getKey())) {
                level.setBlock(e.getKey(), e.getValue(), 2);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Flood-fill from a seed. {@code threeD} true = 6-connected (water groups), false = 4-connected
     * on the horizontal plane (gravel footprints). Bounded by the readable 3x3 area and a hard cap.
     */
    private static List<BlockPos> flood(BlockPos seed, Set<BlockPos> visited, Area area, int maxY,
                                        Predicate<BlockPos> member, boolean threeD) {
        List<BlockPos> out = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed);
        visited.add(seed);

        Direction[] dirs = threeD ? Direction.values() : HORIZONTAL;
        while (!queue.isEmpty() && out.size() < BFS_HARD_CAP) {
            BlockPos p = queue.poll();
            out.add(p);
            for (Direction d : dirs) {
                BlockPos n = p.relative(d);
                if (visited.contains(n) || !area.readable(n) || n.getY() > maxY) {
                    continue;
                }
                if (member.test(n)) {
                    visited.add(n);
                    queue.add(n);
                }
            }
        }
        // If the cap stopped us early, anything still queued is already marked visited, so it would
        // never be re-seeded. It was validated as a member when enqueued - keep it rather than drop it.
        out.addAll(queue);
        return out;
    }

    /**
     * Iterates only blocks in the centre chunk that match {@code filter}, skipping whole 16^3
     * sections via the palette check {@link LevelChunkSection#maybeHas} before reading anything.
     */
    private static void forEach(ChunkAccess chunk, Area area, int maxY,
                                Predicate<BlockState> filter, Visitor visitor) {
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(filter)) {
                continue;
            }
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            if (baseY > maxY) {
                continue;
            }
            for (int ly = 0; ly < 16; ly++) {
                int y = baseY + ly;
                if (y > maxY) {
                    break;
                }
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        BlockState state = section.getBlockState(lx, ly, lz);
                        if (filter.test(state)) {
                            pos.set(area.minX + lx, y, area.minZ + lz);
                            visitor.visit(pos, state);
                        }
                    }
                }
            }
        }
    }

    @FunctionalInterface
    private interface Visitor {
        void visit(BlockPos.MutableBlockPos pos, BlockState state);
    }

    /** Centre-chunk bounds plus the readable 3x3 neighbourhood around it. */
    private static final class Area {
        final int minX, minZ, maxX, maxZ;
        final int minY, maxY;

        Area(ChunkPos c, int minBuildHeight, int maxBuildHeight) {
            this.minX = c.getMinBlockX();
            this.minZ = c.getMinBlockZ();
            this.maxX = minX + 15;
            this.maxZ = minZ + 15;
            this.minY = minBuildHeight;
            this.maxY = maxBuildHeight - 1;
        }

        boolean inCentre(BlockPos p) {
            return p.getX() >= minX && p.getX() <= maxX && p.getZ() >= minZ && p.getZ() <= maxZ;
        }

        boolean readable(BlockPos p) {
            return p.getX() >= minX - 16 && p.getX() <= maxX + 16
                    && p.getZ() >= minZ - 16 && p.getZ() <= maxZ + 16
                    && p.getY() >= minY && p.getY() <= maxY;
        }
    }
}
