package eteil.seedscout;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Samples the surface biome and climate parameters of any seed on a block grid without generating chunks,
 * so two mod sets can be diffed. Rows: x,z,y,biomeIndex,temperature,humidity,continentalness,erosion,weirdness
 * (climate values are the quantized longs the biome lookup uses). The biome palette goes to a second file
 * with each biome's base temperature, downfall and Cold Sweat range.
 */
final class BiomeMap {
    static String run(MinecraftServer server, long seed, int x0, int z0, int x1, int z1, int step, int threads, String name) throws Exception {
        ServerLevel level = server.overworld();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) return "overworld generator is not noise-based";
        RegistryAccess access = server.registryAccess();
        // In the world's own seed, sample exactly what chunk generation uses: the live biome source and RandomState.
        boolean own = seed == level.getSeed();
        BiomeSource source = own ? generator.getBiomeSource() : TerraBlenderBridge.forSeed(generator.getBiomeSource(), access, seed);
        SeedScout.LOG.info("[seedscout] biomemap {} using {} biome source", name, own ? "the world's own" : "a reseeded copy of the");

        int cols = (x1 - x0) / step + 1;
        int rows = (z1 - z0) / step + 1;
        long[][] out = new long[rows][];
        Map<String, Integer> palette = new LinkedHashMap<>();
        Map<String, Holder<Biome>> holders = new LinkedHashMap<>();
        AtomicInteger done = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        RandomState worldRandom = level.getChunkSource().randomState();
        ThreadLocal<RandomState> randoms = ThreadLocal.withInitial(() -> own ? worldRandom
                : RandomState.create(noise.generatorSettings().value(), access.lookupOrThrow(Registries.NOISE), seed));
        long started = System.currentTimeMillis();
        List<Future<?>> jobs = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            final int row = r;
            jobs.add(pool.submit(() -> {
                RandomState random = randoms.get();
                int z = z0 + row * step;
                long[] line = new long[cols * 9];
                for (int c = 0; c < cols; c++) {
                    int x = x0 + c * step;
                    int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                    Holder<Biome> biome = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), random.sampler());
                    Climate.TargetPoint p = random.sampler().sample(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
                    String id = biome.unwrapKey().map(k -> k.location().toString()).orElse("?");
                    int index;
                    synchronized (palette) {
                        index = palette.computeIfAbsent(id, k -> palette.size());
                        holders.putIfAbsent(id, biome);
                    }
                    int o = c * 9;
                    line[o] = x; line[o + 1] = z; line[o + 2] = y; line[o + 3] = index;
                    line[o + 4] = p.temperature(); line[o + 5] = p.humidity(); line[o + 6] = p.continentalness();
                    line[o + 7] = p.erosion(); line[o + 8] = p.weirdness();
                }
                out[row] = line;
                int n = done.incrementAndGet();
                if (n % 100 == 0) SeedScout.LOG.info("[seedscout] biomemap {} rows {}/{} ({} s)", name, n, rows, (System.currentTimeMillis() - started) / 1000);
            }));
        }
        for (Future<?> f : jobs) f.get();
        pool.shutdown();

        Path dir = server.getServerDirectory().resolve(SeedScout.MOD_ID);
        Files.createDirectories(dir);
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(dir.resolve("biomemap-" + name + ".csv.gz"))), StandardCharsets.UTF_8))) {
            w.write("x,z,y,biome,temperature,humidity,continentalness,erosion,weirdness\n");
            for (long[] line : out) {
                for (int o = 0; o < line.length; o += 9) {
                    for (int k = 0; k < 9; k++) { if (k > 0) w.write(','); w.write(Long.toString(line[o + k])); }
                    w.write('\n');
                }
            }
        }
        try (BufferedWriter w = Files.newBufferedWriter(dir.resolve("biomemap-" + name + "-palette.csv"), StandardCharsets.UTF_8)) {
            w.write("index,biome,baseTemperature,downfall,coldsweatLowF,coldsweatHighF\n");
            for (Map.Entry<String, Integer> e : palette.entrySet()) {
                Biome b = holders.get(e.getKey()).value();
                ComfortTable.Range range = ComfortTable.range(e.getKey());
                w.write(e.getValue() + "," + e.getKey() + "," + b.getBaseTemperature() + "," + b.getModifiedClimateSettings().downfall() + ","
                        + (range == null ? "," : range.lowF() + "," + range.highF()) + "\n");
            }
        }
        return "biomemap " + name + ": " + rows * cols + " samples, " + palette.size() + " biomes, "
                + (System.currentTimeMillis() - started) / 1000 + " s -> " + dir;
    }

    private BiomeMap() {}
}
