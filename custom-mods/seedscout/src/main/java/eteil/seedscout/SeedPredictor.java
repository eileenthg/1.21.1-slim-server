package eteil.seedscout;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Predicts, for any seed, where the overworld spawn lands, its biome, and which villages start near it.
 * Mirrors MinecraftServer.setInitialSpawn (spawn chunk from the climate sampler's spawn target) and
 * ChunkGenerator.createStructures (placement check, weighted pick within the set, full structure generate),
 * using the running server's resolved worldgen so every mod's changes are included.
 *
 * Two passes: {@link #quick} ignores Better Sparse Structures and only looks at village sets (cheap filter);
 * {@link #full} replays every structure start around spawn nearest-first through the mod's spacing rule.
 */
final class SeedPredictor {
    /**
     * Radius (chunks) of the square whose structure starts are generated for the spawn chunk when a new world
     * is created (FULL status of the spawn chunk pulls STRUCTURE_STARTS out to 12; measured: 625+ chunks).
     */
    static final int SPAWN_GEN_RADIUS = 12;

    record Village(String structure, int x, int z, int distance, String biome, boolean accepted) {}

    record Prediction(long seed, int spawnX, int spawnY, int spawnZ, String spawnBiome, float temperature,
                      boolean temperate, List<Village> villages, int structuresSimulated, boolean sparseSimulated,
                      List<String> trace) {
        /** Nearest village that actually generates (all of them in the quick pass). */
        Village nearest() {
            for (Village v : villages) if (v.accepted()) return v;
            return null;
        }
    }

    private record Context(long seed, RandomState random, BiomeSource source, ChunkGeneratorStructureState state,
                           ChunkPos spawnChunk, int sx, int sy, int sz, Holder<Biome> spawnBiome,
                           NoiseBasedChunkGenerator generator, StructureManager structures, boolean wholeChunkComfortable) {}

    private record Candidate(Holder<StructureSet> set, int setOrder, ChunkPos pos) {}

    private final RegistryAccess access;
    private final ServerLevel level;
    private final NoiseBasedChunkGenerator generator;
    private final BiomeSource baseSource;
    private final HolderLookup<StructureSet> structureSets;
    /** Sets that can place a proper surface village (target of the search). */
    final List<Holder.Reference<StructureSet>> villageSets = new ArrayList<>();

    SeedPredictor(MinecraftServer server) {
        this.access = server.registryAccess();
        this.level = server.overworld();
        if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator noise)) {
            throw new IllegalStateException("overworld generator is " + level.getChunkSource().getGenerator().getClass().getName());
        }
        this.generator = noise;
        this.baseSource = noise.getBiomeSource();
        this.structureSets = access.lookupOrThrow(Registries.STRUCTURE_SET);
        structureSets.listElements().forEach(set -> {
            String setPath = set.key().location().getPath();
            if (isExcluded(setPath)) return;
            for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                if (isVillage(entry.structure())) {
                    villageSets.add(set);
                    return;
                }
            }
        });
    }

    /** Floating, underwater, underground and ruined variants are not a usable starting village. */
    private static boolean isExcluded(String path) {
        return path.contains("air") || path.contains("underwater") || path.contains("underground")
                || path.contains("remnant") || path.contains("ruin") || path.contains("abandon");
    }

    private static boolean isVillage(Holder<Structure> structure) {
        String path = structure.unwrapKey().map(k -> k.location().getPath()).orElse("");
        if (isExcluded(path)) return false;
        return structure.is(StructureTags.VILLAGE) || path.contains("village");
    }

    private boolean isTargetVillage(Holder<StructureSet> set, String structureId) {
        return villageSets.stream().anyMatch(s -> s.value() == set.value()) && !isExcluded(structureId);
    }

    private Context context(long seed) {
        RandomState random = RandomState.create(generator.generatorSettings().value(), access.lookupOrThrow(Registries.NOISE), seed);
        BiomeSource source = TerraBlenderBridge.forSeed(baseSource, access, seed);
        ChunkPos spawnChunk = new ChunkPos(random.sampler().findSpawnPosition());
        int sx = spawnChunk.getMiddleBlockX();
        int sz = spawnChunk.getMiddleBlockZ();
        int sy = generator.getBaseHeight(sx, sz, Heightmap.Types.WORLD_SURFACE_WG, level, random);
        Holder<Biome> spawnBiome = source.getNoiseBiome(QuartPos.fromBlock(sx), QuartPos.fromBlock(sy), QuartPos.fromBlock(sz), random.sampler());
        boolean wholeChunkComfortable = true;
        for (int ox = 0; ox < 16 && wholeChunkComfortable; ox += 4) {
            for (int oz = 0; oz < 16 && wholeChunkComfortable; oz += 4) {
                int bx = spawnChunk.getMinBlockX() + ox;
                int bz = spawnChunk.getMinBlockZ() + oz;
                int by = generator.getBaseHeight(bx, bz, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                Holder<Biome> b = source.getNoiseBiome(QuartPos.fromBlock(bx), QuartPos.fromBlock(by), QuartPos.fromBlock(bz), random.sampler());
                wholeChunkComfortable = ComfortTable.comfortable(b, biomeId(b));
            }
        }
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForNormal(random, seed, source, structureSets);
        // A generator bound to this seed's biome source, so the real tryGenerateStructure checks the right biomes.
        NoiseBasedChunkGenerator seeded = new NoiseBasedChunkGenerator(source, generator.generatorSettings());
        // No level behind it: nothing is written to any world, and Better Sparse Structures' own hook
        // steps aside (it accepts when it cannot resolve a ServerLevel); its rule is replayed via our index.
        StructureManager scratch = new StructureManager(null, level.getServer().getWorldData().worldGenOptions(), null);
        return new Context(seed, random, source, state, spawnChunk, sx, sy, sz, spawnBiome, seeded, scratch, wholeChunkComfortable);
    }

    private static boolean temperate(Context c) {
        return c.wholeChunkComfortable() && ComfortTable.comfortable(c.spawnBiome(), biomeId(c.spawnBiome()));
    }

    /** Cheap pass: spawn + village-set starts within {@code radiusChunks}, no spacing simulation. */
    Prediction quick(long seed, int radiusChunks) {
        Context c = context(seed);
        List<Village> villages = new ArrayList<>();
        List<Holder<StructureSet>> order = c.state().possibleStructureSets();
        for (Holder.Reference<StructureSet> set : villageSets) {
            if (!order.contains(set)) continue;
            for (ChunkPos pos : placementChunks(set.value().placement(), c, radiusChunks)) {
                StructureStart start = generateLikeVanilla(set.value(), c, pos, s -> true);
                if (start != null) villages.add(village(c, start, true));
            }
        }
        villages.sort(Comparator.comparingInt(Village::distance));
        return new Prediction(seed, c.sx(), c.sy(), c.sz(), biomeId(c.spawnBiome()), c.spawnBiome().value().getBaseTemperature(),
                temperate(c), villages, 0, false, List.of());
    }

    /**
     * Full pass: every structure set's starts within {@link #SPAWN_GEN_RADIUS} of spawn, generated nearest-first
     * (then in the chunk generator's set order) and filtered through Better Sparse Structures, including its
     * fall-through to the set's next weighted structure when a start is rejected.
     */
    Prediction full(long seed) {
        Context c = context(seed);
        List<Holder<StructureSet>> order = c.state().possibleStructureSets();
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            Holder<StructureSet> set = order.get(i);
            if (set.value().placement() instanceof ConcentricRingsStructurePlacement) continue; // strongholds: never near spawn
            for (ChunkPos pos : placementChunks(set.value().placement(), c, SPAWN_GEN_RADIUS)) {
                candidates.add(new Candidate(set, i, pos));
            }
        }
        // ChunkGenerationTask.scheduleLayer walks the square x-major, then z; within a chunk the generator
        // goes through possibleStructureSets in order.
        candidates.sort(Comparator.<Candidate>comparingInt(cd -> cd.pos().x).thenComparingInt(cd -> cd.pos().z).thenComparingInt(Candidate::setOrder));

        Object index = SparseStructuresBridge.newIndex();
        List<Village> villages = new ArrayList<>();
        List<String> trace = new ArrayList<>();
        int simulated = 0;
        for (Candidate cand : candidates) {
            List<StructureStart> rejectedVillages = new ArrayList<>();
            StructureStart start = generateLikeVanilla(cand.set().value(), c, cand.pos(), s -> {
                String id = structureId(s);
                boolean ok = SparseStructuresBridge.accept(index, s.getChunkPos(), id, s.getBoundingBox());
                trace.add((ok ? "ACCEPT " : "REJECT[" + SparseStructuresBridge.LAST_REASON.get() + "] ") + id + " set=" + cand.set().unwrapKey().map(k -> k.location().toString()).orElse("?")
                        + " startChunk=" + s.getChunkPos() + " box=" + s.getBoundingBox());
                if (!ok && isTargetVillage(cand.set(), id)) rejectedVillages.add(s);
                return ok;
            });
            simulated++;
            for (StructureStart r : rejectedVillages) villages.add(village(c, r, false));
            if (start != null && isTargetVillage(cand.set(), structureId(start))) villages.add(village(c, start, true));
        }
        villages.sort(Comparator.comparingInt(Village::distance));
        return new Prediction(seed, c.sx(), c.sy(), c.sz(), biomeId(c.spawnBiome()), c.spawnBiome().value().getBaseTemperature(),
                temperate(c), villages, simulated, SparseStructuresBridge.active(), trace);
    }

    /** Chunks within the radius where this placement would attempt a start. */
    private List<ChunkPos> placementChunks(StructurePlacement placement, Context c, int radius) {
        List<ChunkPos> out = new ArrayList<>();
        int minX = c.spawnChunk().x - radius, maxX = c.spawnChunk().x + radius;
        int minZ = c.spawnChunk().z - radius, maxZ = c.spawnChunk().z + radius;
        if (placement instanceof RandomSpreadStructurePlacement spread) {
            int spacing = spread.spacing();
            for (int rx = Math.floorDiv(minX, spacing); rx <= Math.floorDiv(maxX, spacing); rx++) {
                for (int rz = Math.floorDiv(minZ, spacing); rz <= Math.floorDiv(maxZ, spacing); rz++) {
                    ChunkPos pos = spread.getPotentialStructureChunk(c.seed(), rx * spacing, rz * spacing);
                    if (pos.x < minX || pos.x > maxX || pos.z < minZ || pos.z > maxZ) continue;
                    if (isStructureChunk(placement, c, pos.x, pos.z)) out.add(pos);
                }
            }
        } else {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isStructureChunk(placement, c, x, z)) out.add(new ChunkPos(x, z));
                }
            }
        }
        return out;
    }

    private static final java.lang.reflect.Method ADDITIONAL_RESTRICTIONS;
    private static final java.lang.reflect.Method IS_PLACEMENT_CHUNK;

    static {
        try {
            ADDITIONAL_RESTRICTIONS = StructurePlacement.class.getDeclaredMethod("applyAdditionalChunkRestrictions", int.class, int.class, long.class);
            ADDITIONAL_RESTRICTIONS.setAccessible(true);
            IS_PLACEMENT_CHUNK = StructurePlacement.class.getDeclaredMethod("isPlacementChunk", ChunkGeneratorStructureState.class, int.class, int.class);
            IS_PLACEMENT_CHUNK.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * StructurePlacement.isStructureChunk, except that an exclusion zone pointing at a concentric-rings set
     * (strongholds) is skipped: computing ring positions for a throwaway state breaks ModernFix's ring cache,
     * and strongholds are never inside the few chunks around spawn that matter here.
     */
    private static boolean isStructureChunk(StructurePlacement placement, Context c, int x, int z) {
        try {
            return placement.isStructureChunk(c.state(), x, z);
        } catch (RuntimeException e) {
            try {
                return (boolean) IS_PLACEMENT_CHUNK.invoke(placement, c.state(), x, z)
                        && (boolean) ADDITIONAL_RESTRICTIONS.invoke(placement, x, z, c.seed());
            } catch (ReflectiveOperationException | RuntimeException inner) {
                return false;
            }
        }
    }

    /** ChunkGenerator.createStructures for one set: weighted pick, falling back through the other entries. */
    private StructureStart generateLikeVanilla(StructureSet set, Context c, ChunkPos pos, Predicate<StructureStart> accept) {
        List<StructureSet.StructureSelectionEntry> entries = new ArrayList<>(set.structures());
        if (entries.size() == 1) return tryGenerate(entries.get(0), c, pos, accept);
        WorldgenRandom rng = new WorldgenRandom(new LegacyRandomSource(0L));
        rng.setLargeFeatureSeed(c.seed(), pos.x, pos.z);
        int total = 0;
        for (StructureSet.StructureSelectionEntry entry : entries) total += entry.weight();
        while (!entries.isEmpty()) {
            int roll = rng.nextInt(total);
            int index = 0;
            for (StructureSet.StructureSelectionEntry entry : entries) {
                roll -= entry.weight();
                if (roll < 0) break;
                index++;
            }
            StructureSet.StructureSelectionEntry picked = entries.get(index);
            StructureStart start = tryGenerate(picked, c, pos, accept);
            if (start != null) return start;
            entries.remove(index);
            total -= picked.weight();
        }
        return null;
    }

    private static final java.lang.reflect.Method TRY_GENERATE;

    static {
        try {
            TRY_GENERATE = ChunkGenerator.class.getDeclaredMethod("tryGenerateStructure", StructureSet.StructureSelectionEntry.class,
                    StructureManager.class, RegistryAccess.class, RandomState.class, StructureTemplateManager.class, long.class,
                    ChunkAccess.class, ChunkPos.class, SectionPos.class);
            TRY_GENERATE.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Runs the real ChunkGenerator.tryGenerateStructure (with every mod's mixins: Integrated Villages disabling
     * vanilla villages, IDAS / Integrated API / YUNG / Moog disable lists, MineColonies Tweaks, ...) on a scratch
     * chunk, then applies {@code accept} (the Better Sparse Structures replay) to the stored start.
     */
    private StructureStart tryGenerate(StructureSet.StructureSelectionEntry entry, Context c, ChunkPos pos, Predicate<StructureStart> accept) {
        ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, access.registryOrThrow(Registries.BIOME), null);
        boolean generated;
        try {
            generated = (boolean) TRY_GENERATE.invoke(c.generator(), entry, c.structures(), access, c.random(),
                    level.getServer().getStructureManager(), c.seed(), chunk, pos, SectionPos.bottomOf(chunk));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        if (!generated) return null;
        StructureStart start = chunk.getStartForStructure(entry.structure().value());
        return start != null && start.isValid() && accept.test(start) ? start : null;
    }

    private String structureId(StructureStart start) {
        return access.registryOrThrow(Registries.STRUCTURE).getResourceKey(start.getStructure())
                .map(k -> k.location().toString()).orElse("?");
    }

    private Village village(Context c, StructureStart start, boolean accepted) {
        BoundingBox box = start.getBoundingBox();
        int vx = box.getCenter().getX();
        int vz = box.getCenter().getZ();
        int vy = generator.getBaseHeight(vx, vz, Heightmap.Types.WORLD_SURFACE_WG, level, c.random());
        String biome = biomeId(c.source().getNoiseBiome(QuartPos.fromBlock(vx), QuartPos.fromBlock(vy), QuartPos.fromBlock(vz), c.random().sampler()));
        return new Village(structureId(start), vx, vz, distanceToBox(box, c.sx(), c.sz()), biome, accepted);
    }

    private static int distanceToBox(BoundingBox box, int x, int z) {
        int dx = Math.max(0, Math.max(box.minX() - x, x - box.maxX()));
        int dz = Math.max(0, Math.max(box.minZ() - z, z - box.maxZ()));
        return (int) Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
    }

    static String biomeId(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.location().toString()).orElse("?");
    }

    BlockPos actualSpawn() {
        return level.getSharedSpawnPos();
    }

    String actualBiomeAt(BlockPos pos) {
        return biomeId(level.getBiome(pos));
    }
}
