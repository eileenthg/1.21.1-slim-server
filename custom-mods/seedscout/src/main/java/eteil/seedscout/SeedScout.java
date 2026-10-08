package eteil.seedscout;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Mod(SeedScout.MOD_ID)
public final class SeedScout {
    static final String MOD_ID = "seedscout";
    static final Logger LOG = LogUtils.getLogger();

    private static SeedPredictor predictor;
    private static volatile Scan scan;

    public SeedScout(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SeedScout::registerCommands);
        NeoForge.EVENT_BUS.addListener(SeedScout::serverStarted);
        NeoForge.EVENT_BUS.addListener(SeedScout::serverStopping);
    }

    private static void serverStarted(ServerStartedEvent event) {
        TerraBlenderBridge.init(LOG);
        SparseStructuresBridge.init(LOG);
        ComfortTable.load(event.getServer().getServerDirectory().resolve("config"), LOG);
        predictor = new SeedPredictor(event.getServer());
        StringBuilder sets = new StringBuilder();
        predictor.villageSets.forEach(s -> sets.append(' ').append(s.key().location()));
        LOG.info("[seedscout] village structure sets:{}", sets);
    }

    private static void serverStopping(ServerStoppingEvent event) {
        Scan running = scan;
        if (running != null) running.stop();
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal(MOD_ID).requires(s -> s.hasPermission(2))
                .then(Commands.literal("dump").executes(SeedScout::dump))
                .then(Commands.literal("check").executes(SeedScout::check))
                .then(Commands.literal("predict")
                        .then(Commands.argument("seed", LongArgumentType.longArg()).executes(c -> predict(c, LongArgumentType.getLong(c, "seed")))))
                .then(Commands.literal("scan")
                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                .executes(c -> startScan(c, System.nanoTime(), 3, 48))
                                .then(Commands.argument("threads", IntegerArgumentType.integer(1, 16))
                                        .then(Commands.argument("maxDistance", IntegerArgumentType.integer(0, 512))
                                                .executes(c -> startScan(c, System.nanoTime(), IntegerArgumentType.getInteger(c, "threads"), IntegerArgumentType.getInteger(c, "maxDistance")))
                                                .then(Commands.argument("rngSeed", LongArgumentType.longArg())
                                                        .executes(c -> startScan(c, LongArgumentType.getLong(c, "rngSeed"), IntegerArgumentType.getInteger(c, "threads"), IntegerArgumentType.getInteger(c, "maxDistance"))))))))
                .then(Commands.literal("trace")
                        .then(Commands.argument("seed", LongArgumentType.longArg()).executes(c -> {
                            SeedPredictor.Prediction p = predictor.full(LongArgumentType.getLong(c, "seed"));
                            LOG.info("[seedscout] trace {}\n{}", describe(p), String.join("\n", p.trace()));
                            return reply(c, "[seedscout] trace written to log (" + p.trace().size() + " attempts)");
                        })))
                .then(Commands.literal("realcheck")
                        .then(Commands.argument("set", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                .then(Commands.argument("cx", IntegerArgumentType.integer())
                                        .then(Commands.argument("cz", IntegerArgumentType.integer()).executes(c -> {
                                            String out = RealCheck.run(c.getSource().getServer(),
                                                    net.minecraft.commands.arguments.ResourceLocationArgument.getId(c, "set"),
                                                    IntegerArgumentType.getInteger(c, "cx"), IntegerArgumentType.getInteger(c, "cz"));
                                            LOG.info("[seedscout] realcheck {}", out);
                                            return reply(c, "[seedscout] " + out);
                                        })))))
                .then(Commands.literal("recheck")
                        .then(Commands.argument("seeds", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> {
                            String[] seeds = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "seeds").split("[,\s]+");
                            new Thread(() -> {
                                for (String raw : seeds) {
                                    try {
                                        SeedPredictor.Prediction p = predictor.full(Long.parseLong(raw.trim()));
                                        SeedPredictor.Village v = p.nearest();
                                        LOG.info("[seedscout] RECHECK {} temperate={} biome={} village={} dist={}", p.seed(), p.temperate(), p.spawnBiome(),
                                                v == null ? "none" : v.structure(), v == null ? -1 : v.distance());
                                    } catch (RuntimeException e) {
                                        LOG.warn("[seedscout] RECHECK {} failed: {}", raw, e.toString());
                                    }
                                }
                                LOG.info("[seedscout] RECHECK done");
                            }, "seedscout-recheck").start();
                            return reply(c, "[seedscout] rechecking " + seeds.length + " seeds");
                        })))
                .then(Commands.literal("biomemap")
                        .then(Commands.argument("args", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> {
                            // seed x0 z0 x1 z1 step threads name
                            String[] a = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "args").trim().split("\s+");
                            if (a.length != 8) return reply(c, "[seedscout] usage: biomemap <seed> <x0> <z0> <x1> <z1> <step> <threads> <name>");
                            MinecraftServer server = c.getSource().getServer();
                            new Thread(() -> {
                                try {
                                    String out = BiomeMap.run(server, Long.parseLong(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]),
                                            Integer.parseInt(a[3]), Integer.parseInt(a[4]), Integer.parseInt(a[5]), Integer.parseInt(a[6]), a[7]);
                                    LOG.info("[seedscout] BIOMEMAP DONE {}", out);
                                } catch (Exception e) {
                                    LOG.error("[seedscout] BIOMEMAP FAILED", e);
                                }
                            }, "seedscout-biomemap").start();
                            return reply(c, "[seedscout] biomemap started");
                        })))
                .then(Commands.literal("status").executes(SeedScout::status))
                .then(Commands.literal("stop").executes(c -> {
                    Scan running = scan;
                    if (running != null) running.stop();
                    return reply(c, "scan stopped");
                })));
    }

    private static int reply(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static Path outDir(MinecraftServer server) throws IOException {
        Path dir = server.getServerDirectory().resolve(MOD_ID);
        Files.createDirectories(dir);
        return dir;
    }

    private static int dump(CommandContext<CommandSourceStack> c) {
        try {
            Path root = outDir(c.getSource().getServer()).resolve("worldgen-datapack");
            WorldgenDumper.Result result = WorldgenDumper.dump(c.getSource().getServer(), root);
            reply(c, "[seedscout] dumped to " + root + " " + result.written() + " errors=" + result.errors().size());
        } catch (IOException e) {
            LOG.error("[seedscout] dump failed", e);
            reply(c, "[seedscout] dump failed: " + e);
        }
        return 1;
    }

    private static String describe(SeedPredictor.Prediction p) {
        StringBuilder sb = new StringBuilder();
        sb.append("seed=").append(p.seed()).append(" spawn=").append(p.spawnX()).append(',').append(p.spawnY()).append(',').append(p.spawnZ())
                .append(" biome=").append(p.spawnBiome()).append(" temp=").append(p.temperature()).append(" temperate=").append(p.temperate())
                .append(" structuresSimulated=").append(p.structuresSimulated()).append(" sparse=").append(p.sparseSimulated());
        int shown = 0;
        for (SeedPredictor.Village v : p.villages()) {
            if (shown++ == 5) break;
            sb.append("\n  village ").append(v.structure()).append(" at ").append(v.x()).append(',').append(v.z())
                    .append(" dist=").append(v.distance()).append(" biome=").append(v.biome()).append(v.accepted() ? "" : " BLOCKED-by-sparse-structures");
        }
        if (p.villages().isEmpty()) sb.append("\n  no village within search radius");
        return sb.toString();
    }

    private static int predict(CommandContext<CommandSourceStack> c, long seed) {
        String text = describe(predictor.full(seed));
        LOG.info("[seedscout] predict {}", text);
        return reply(c, "[seedscout] " + text);
    }

    /** Predicts the current world's own seed and prints what the world actually did, to validate the predictor. */
    private static int check(CommandContext<CommandSourceStack> c) {
        long seed = c.getSource().getServer().overworld().getSeed();
        String text = describe(predictor.full(seed));
        BlockPos actual = predictor.actualSpawn();
        String out = text + "\n  ACTUAL spawn=" + actual.getX() + ',' + actual.getY() + ',' + actual.getZ()
                + " biome=" + predictor.actualBiomeAt(actual) + " tb=" + TerraBlenderBridge.active() + " bss=" + SparseStructuresBridge.active();
        LOG.info("[seedscout] check {}", out);
        return reply(c, "[seedscout] " + out);
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        Scan running = scan;
        return reply(c, running == null ? "[seedscout] no scan" : "[seedscout] " + running.summary());
    }

    private static int startScan(CommandContext<CommandSourceStack> c, long rngSeed, int threads, int maxDistance) {
        if (scan != null && !scan.done()) return reply(c, "[seedscout] a scan is already running: " + scan.summary());
        int count = IntegerArgumentType.getInteger(c, "count");
        try {
            Path file = outDir(c.getSource().getServer()).resolve("scan-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv");
            scan = new Scan(file, count, threads, maxDistance, rngSeed);
            scan.start();
            return reply(c, "[seedscout] scanning " + count + " seeds on " + threads + " threads, village within " + maxDistance + " blocks -> " + file);
        } catch (IOException e) {
            return reply(c, "[seedscout] could not start: " + e);
        }
    }

    private static String rangeCsv(String biome) {
        ComfortTable.Range r = ComfortTable.range(biome);
        return r == null ? "," : Math.round(r.lowF()) + "," + Math.round(r.highF());
    }

    private static final class Scan {
        private final Path file;
        private final int count;
        private final int threads;
        private final int maxDistance;
        private final long rngSeed;
        private final AtomicInteger done = new AtomicInteger();
        private final AtomicInteger hits = new AtomicInteger();
        private final AtomicInteger candidates = new AtomicInteger();
        private final AtomicInteger temperateSpawns = new AtomicInteger();
        private final AtomicInteger failures = new AtomicInteger();
        private final AtomicLong started = new AtomicLong();
        private final BufferedWriter out;
        private ExecutorService pool;
        private volatile boolean stopped;

        Scan(Path file, int count, int threads, int maxDistance, long rngSeed) throws IOException {
            this.file = file;
            this.count = count;
            this.threads = threads;
            this.maxDistance = maxDistance;
            this.rngSeed = rngSeed;
            this.out = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            out.write("seed,spawn_x,spawn_y,spawn_z,spawn_biome,temperature,village,village_x,village_z,distance,village_biome\n");
            out.flush();
        }

        void start() {
            started.set(System.currentTimeMillis());
            pool = Executors.newFixedThreadPool(threads, r -> {
                Thread t = new Thread(r, "seedscout-scan");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
            SplittableRandom rng = new SplittableRandom(rngSeed);
            for (int i = 0; i < count; i++) {
                long seed = rng.nextLong();
                pool.execute(() -> run(seed));
            }
            pool.shutdown();
            Thread watcher = new Thread(() -> {
                try {
                    pool.awaitTermination(7, TimeUnit.DAYS);
                } catch (InterruptedException ignored) {
                }
                LOG.info("[seedscout] scan finished: {}", summary());
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }, "seedscout-watch");
            watcher.setDaemon(true);
            watcher.start();
        }

        private void run(long seed) {
            if (stopped) return;
            try {
                SeedPredictor.Prediction quick = predictor.quick(seed, 6);
                if (quick.temperate()) temperateSpawns.incrementAndGet();
                SeedPredictor.Village q = quick.nearest();
                if (quick.temperate() && q != null && q.distance() <= maxDistance) {
                    candidates.incrementAndGet();
                    SeedPredictor.Prediction p = predictor.full(seed);
                    SeedPredictor.Village v = p.nearest();
                    if (v != null && v.distance() <= maxDistance) {
                        hits.incrementAndGet();
                        synchronized (out) {
                            out.write(p.seed() + "," + p.spawnX() + "," + p.spawnY() + "," + p.spawnZ() + "," + p.spawnBiome() + ","
                                    + rangeCsv(p.spawnBiome()) + "," + v.structure() + "," + v.x() + "," + v.z() + "," + v.distance() + "," + v.biome() + "\n");
                            out.flush();
                        }
                        LOG.info("[seedscout] HIT seed {} biome {} village {} at {} blocks", seed, p.spawnBiome(), v.structure(), v.distance());
                    }
                }
            } catch (Throwable t) {
                if (failures.incrementAndGet() <= 5) LOG.warn("[seedscout] seed {} failed", seed, t);
            }
            int n = done.incrementAndGet();
            if (n % 250 == 0) LOG.info("[seedscout] progress {}", summary());
        }

        void stop() {
            stopped = true;
            if (pool != null) pool.shutdownNow();
        }

        boolean done() {
            return pool == null || pool.isTerminated();
        }

        String summary() {
            long ms = Math.max(1, System.currentTimeMillis() - started.get());
            return done.get() + "/" + count + " seeds, " + temperateSpawns.get() + " temperate spawns, " + candidates.get() + " pre-sparse candidates, " + hits.get() + " hits, "
                    + failures.get() + " failures, " + String.format("%.1f", done.get() * 1000.0 / ms) + " seeds/s -> " + file.getFileName();
        }
    }
}
