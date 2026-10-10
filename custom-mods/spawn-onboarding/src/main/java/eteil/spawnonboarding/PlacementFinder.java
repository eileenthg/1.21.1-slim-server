package eteil.spawnonboarding;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches outward from a centre for a footprint that overlaps no structure piece (villages,
 * temples, other mods' structures), no reserved box, and is not mostly water.
 *
 * <p>Candidates are tried on rings of growing radius around the centre. Structure data is read at
 * STRUCTURE_REFERENCES / STRUCTURE_STARTS status, which is cheap to generate; full terrain is only
 * generated for the few candidates that pass the structure check.
 */
public final class PlacementFinder {
    private static final int RING_STEP = 16;
    private static final int MAX_RADIUS = 1024;
    private static final int POINT_SPACING = 24;

    private final ServerLevel level;
    private final Map<Long, List<BoundingBox>> piecesByChunk = new HashMap<>();

    public PlacementFinder(ServerLevel level) {
        this.level = level;
    }

    /**
     * @param center     search centre (the destiny position)
     * @param halfX      half the footprint width on X
     * @param halfZ      half the footprint width on Z
     * @param margin     empty border required around the footprint
     * @param minRadius  first ring radius; candidates closer than this to the centre are skipped
     * @param reserved   boxes already claimed (e.g. the player's clearance), checked with the same margin
     * @return centre of the first acceptable footprint, with Y left at 0; null if none was found
     */
    public BlockPos find(BlockPos center, int halfX, int halfZ, int margin, int minRadius, List<BoundingBox> reserved) {
        for (int r = minRadius; r <= MAX_RADIUS; r += RING_STEP) {
            int points = r == 0 ? 1 : Math.max(8, (int) Math.ceil(2 * Math.PI * r / POINT_SPACING));
            for (int i = 0; i < points; i++) {
                double a = 2 * Math.PI * i / points;
                int cx = center.getX() + (int) Math.round(r * Math.cos(a));
                int cz = center.getZ() + (int) Math.round(r * Math.sin(a));
                if (accept(cx, cz, halfX, halfZ, margin, reserved)) {
                    SpawnOnboarding.LOG.info("[SpawnOnboarding] footprint {}x{} placed at {}, {} (ring radius {})",
                            halfX * 2 + 1, halfZ * 2 + 1, cx, cz, r);
                    return new BlockPos(cx, 0, cz);
                }
            }
        }
        SpawnOnboarding.LOG.warn("[SpawnOnboarding] no clear {}x{} footprint within {} blocks of {}", halfX * 2 + 1, halfZ * 2 + 1, MAX_RADIUS, center);
        return null;
    }

    /** Box used to reserve a footprint for later searches. */
    public BoundingBox footprint(BlockPos center, int halfX, int halfZ) {
        return new BoundingBox(center.getX() - halfX, level.getMinBuildHeight(), center.getZ() - halfZ,
                center.getX() + halfX, level.getMaxBuildHeight(), center.getZ() + halfZ);
    }

    private boolean accept(int cx, int cz, int halfX, int halfZ, int margin, List<BoundingBox> reserved) {
        // Only structures near or above the surface matter; deep ones (mineshafts, ancient cities) do not collide.
        int yMin = level.getSeaLevel() - 12;
        BoundingBox box = new BoundingBox(cx - halfX - margin, yMin, cz - halfZ - margin,
                cx + halfX + margin, level.getMaxBuildHeight(), cz + halfZ + margin);
        for (BoundingBox r : reserved) {
            if (r.intersects(box)) return false;
        }
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                for (BoundingBox piece : piecesReferencedBy(chunkX, chunkZ)) {
                    if (piece.intersects(box)) return false;
                }
            }
        }
        return !mostlyWater(cx, cz, halfX, halfZ);
    }

    private List<BoundingBox> piecesReferencedBy(int chunkX, int chunkZ) {
        return piecesByChunk.computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ), key -> {
            List<BoundingBox> out = new ArrayList<>();
            ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_REFERENCES, true);
            for (Map.Entry<Structure, LongSet> ref : chunk.getAllReferences().entrySet()) {
                for (long startChunk : ref.getValue()) {
                    ChunkAccess startAccess = level.getChunk(ChunkPos.getX(startChunk), ChunkPos.getZ(startChunk), ChunkStatus.STRUCTURE_STARTS, true);
                    StructureStart start = startAccess.getStartForStructure(ref.getKey());
                    if (start == null || !start.isValid()) continue;
                    for (StructurePiece piece : start.getPieces()) {
                        out.add(piece.getBoundingBox());
                    }
                }
            }
            return out;
        });
    }

    /** Rejects oceans, rivers and lakes: centre wet, or at least two of the four corners wet. */
    private boolean mostlyWater(int cx, int cz, int halfX, int halfZ) {
        if (isWater(cx, cz)) return true;
        int wet = 0;
        if (isWater(cx - halfX, cz - halfZ)) wet++;
        if (isWater(cx + halfX, cz - halfZ)) wet++;
        if (isWater(cx - halfX, cz + halfZ)) wet++;
        if (isWater(cx + halfX, cz + halfZ)) wet++;
        return wet >= 2;
    }

    private boolean isWater(int x, int z) {
        int surface = surfaceY(level, Heightmap.Types.WORLD_SURFACE, x, z);
        int floor = surfaceY(level, Heightmap.Types.OCEAN_FLOOR, x, z);
        return surface - floor > 2;
    }

    /**
     * Level#getHeight returns the world's minimum height for chunks that are not loaded, so the chunk
     * is loaded (generated if needed) before its heightmap is read.
     */
    public static int surfaceY(ServerLevel level, Heightmap.Types type, int x, int z) {
        return level.getChunk(x >> 4, z >> 4).getHeight(type, x & 15, z & 15) + 1;
    }
}
