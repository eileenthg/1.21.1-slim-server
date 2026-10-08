package eteil.pregenhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.conczin.immersive_gateways.block.GatewayBlockEntity;
import net.conczin.immersive_gateways.data.PortalDataManager;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.shiroha233.roadweaver.core.model.ConnectionStatus;
import net.shiroha233.roadweaver.core.model.StructureConnection;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.planning.RoadPlanningService;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.api.ChunkyAPI;
import org.slf4j.Logger;

/**
 * Headless pregen driver, run from config/pregenhelper.json on server start:
 * <ol>
 *   <li>freeze world ticking ({@code /tick freeze}; chunk generation and mod tick hooks keep running),</li>
 *   <li>link the listed Immersive Gateways (the destination is rolled on first link) and add each destination as an area,</li>
 *   <li>plan RoadWeaver roads around every area that is not already generated, and wait for every road to be built,</li>
 *   <li>run one Chunky task per area, then save and stop.</li>
 * </ol>
 * RoadWeaver only stamps roads into chunks while they generate, so step 3 must finish before step 4 reaches an area.
 */
@Mod("pregenhelper")
public final class PregenHelper {
    static final Logger LOG = LogUtils.getLogger();
    private static final String WORLD = "minecraft:overworld";

    enum Phase { IDLE, GATEWAYS, PLAN, ROADS, CHUNKY, DONE }

    static final class Job {
        boolean autoRun = true;
        boolean freeze = true;
        boolean stopWhenDone = true;
        int radius = 500;
        int planRadiusChunks = 128;
        String pattern = "concentric";
        int gatewayTimeoutSec = 600;
        int roadTimeoutSec = 7200;
        /** Square radius around gridCenter to plan (tile by tile) and generate; 0 = off. */
        int gridRadius = 0;
        int[] gridCenter = {0, 0};
        boolean planRoads = true;
        List<int[]> gateways = new ArrayList<>();
        /** {x, z} or {x, z, radius}. */
        List<int[]> centers = new ArrayList<>();
    }

    /** RoadWeaver's dynamic-planning tile size: one tile is marked planned per 64x64 chunks. */
    private static final int TILE_CHUNKS = 64;

    private Job job;
    private Phase phase = Phase.IDLE;
    private int ticks;
    private int phaseStart;
    private final List<BlockPos> pendingGateways = new ArrayList<>();
    private final List<int[]> areas = new ArrayList<>();
    private int[] gridArea;
    private CompletableFuture<Void> planning;
    private int planned;
    private int planTotal;
    private int roadsQuietChecks;
    private int chunkyIndex = -1;
    private boolean chunkySeenRunning;
    private int chunkyIdleChecks;

    public PregenHelper(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onStarted);
        NeoForge.EVENT_BUS.addListener(this::onTick);
        NeoForge.EVENT_BUS.addListener(this::onCommands);
    }

    private void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("pregen").requires(s -> s.hasPermission(2))
            .then(Commands.literal("status").executes(c -> {
                c.getSource().sendSuccess(() -> Component.literal(status(c.getSource().getServer())), false);
                return 1;
            })));
    }

    private void onStarted(ServerStartedEvent e) {
        Path file = FMLPaths.CONFIGDIR.get().resolve("pregenhelper.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (!Files.exists(file)) {
                Files.writeString(file, gson.toJson(new Job()));
                LOG.info("[pregen] wrote default {}; nothing to do", file);
                return;
            }
            job = gson.fromJson(Files.readString(file), Job.class);
        } catch (IOException | RuntimeException ex) {
            LOG.error("[pregen] cannot read {}", file, ex);
            return;
        }
        if (!job.autoRun) return;
        if (job.freeze) {
            e.getServer().tickRateManager().setFrozen(true);
            LOG.info("[pregen] world ticking frozen");
        }
        if (job.gridRadius > 0) {
            gridArea = new int[]{job.gridCenter[0], job.gridCenter[1], job.gridRadius};
            areas.add(gridArea);
        }
        for (int[] c : job.centers) areas.add(new int[]{c[0], c[1], c.length > 2 ? c[2] : job.radius});
        for (int[] g : job.gateways) pendingGateways.add(new BlockPos(g[0], g[1], g[2]));
    }

    private void onTick(ServerTickEvent.Post e) {
        if (job == null || !job.autoRun) return;
        MinecraftServer server = e.getServer();
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return;
        ticks++;
        // First tick: every mod's ServerStarted work (RoadWeaver resets its queues there) is done.
        if (phase == Phase.IDLE) {
            enter(Phase.GATEWAYS);
            linkGateways(level);
        }
        // While frozen the server never purges expired chunk tickets, so chunks never unload and the heap fills up.
        // One real tick every ~200 server loops lets the purge run; it adds only seconds of game time over hours.
        if (job.freeze && ticks % 200 == 0 && server.tickRateManager().isFrozen()) server.tickRateManager().stepGameIfPaused(1);
        if (ticks % 20 != 0) return;
        switch (phase) {
            case GATEWAYS -> tickGateways(level);
            case PLAN -> tickPlan(level);
            case ROADS -> tickRoads(level);
            case CHUNKY -> tickChunky(level);
            default -> { }
        }
        if (ticks % 600 == 0 && phase != Phase.DONE) LOG.info("[pregen] {}", status(server));
    }

    private void enter(Phase p) {
        phase = p;
        phaseStart = ticks;
        LOG.info("[pregen] phase {}", p);
    }

    private int phaseSeconds() {
        return (ticks - phaseStart) / 20;
    }

    // ---- 1. gateways ----

    private void linkGateways(ServerLevel level) {
        for (BlockPos pos : pendingGateways) {
            level.getChunkAt(pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GatewayBlockEntity g) {
                // Same call the block's own ticker makes: links the gateway on a worker thread, or finds its existing pair.
                GatewayBlockEntity.serverTick(level, pos, level.getBlockState(pos), g);
            } else {
                LOG.warn("[pregen] no gateway block entity at {} (found {})", pos.toShortString(), be);
            }
        }
    }

    private void tickGateways(ServerLevel level) {
        PortalDataManager.PortalDataLookup state = PortalDataManager.getState(level);
        pendingGateways.removeIf(pos -> {
            PortalDataManager.PortalPair pair = state.search(pos);
            if (pair == null) return !(level.getBlockEntity(pos) instanceof GatewayBlockEntity);
            BlockPos target = pair.getTarget(pos).boundingBox().getCenter();
            LOG.info("[pregen] gateway {} -> {}", pos.toShortString(), target.toShortString());
            areas.add(new int[]{target.getX(), target.getZ(), job.radius});
            return true;
        });
        if (pendingGateways.isEmpty() || phaseSeconds() > job.gatewayTimeoutSec) {
            if (!pendingGateways.isEmpty()) LOG.warn("[pregen] gateways never linked: {}", pendingGateways);
            level.getDataStorage().save();
            startPlanning(level);
        }
    }

    // ---- 2. road planning ----

    private void startPlanning(ServerLevel level) {
        enter(Phase.PLAN);
        planning = CompletableFuture.completedFuture(null);
        // Regenerating chunks whose roads are already stored: RoadWeaver re-stamps them, nothing to plan.
        if (!job.planRoads) return;
        List<int[]> targets = new ArrayList<>();
        for (int[] a : areas) {
            if (a == gridArea) {
                // Grid: one plan per RoadWeaver tile that is not yet generated, like a player walking every tile.
                int gx = job.gridCenter[0], gz = job.gridCenter[1], r = job.gridRadius;
                int loX = Math.floorDiv((gx - r) >> 4, TILE_CHUNKS), hiX = Math.floorDiv((gx + r) >> 4, TILE_CHUNKS);
                int loZ = Math.floorDiv((gz - r) >> 4, TILE_CHUNKS), hiZ = Math.floorDiv((gz + r) >> 4, TILE_CHUNKS);
                for (int kx = loX; kx <= hiX; kx++) {
                    for (int kz = loZ; kz <= hiZ; kz++) {
                        int x = (kx * TILE_CHUNKS + TILE_CHUNKS / 2) * 16, z = (kz * TILE_CHUNKS + TILE_CHUNKS / 2) * 16;
                        if (!areaGenerated(level, x, z, TILE_CHUNKS * 8)) targets.add(new int[]{x, z, kx, kz});
                    }
                }
                LOG.info("[pregen] grid r={}: {} tiles to plan", job.gridRadius, targets.size());
            } else if (areaGenerated(level, a[0], a[1], a[2])) {
                LOG.info("[pregen] area {},{} already generated; roads there can no longer be stamped, not planning", a[0], a[1]);
            } else {
                targets.add(new int[]{a[0], a[1], Math.floorDiv(a[0] >> 4, TILE_CHUNKS), Math.floorDiv(a[1] >> 4, TILE_CHUNKS)});
            }
        }
        planTotal = targets.size();
        for (int[] t : targets) {
            int cx = t[0] >> 4, cz = t[1] >> 4, r = job.planRadiusChunks;
            // Same rectangle RoadWeaver plans around a player standing at the target. Run one at a time.
            planning = planning.thenCompose(v -> RoadPlanningService.planRectAsync(level, (cx - r) * 16, (cz - r) * 16, (cx + r) * 16, (cz + r) * 16)
                .handle((ok, err) -> {
                    if (err != null) {
                        LOG.warn("[pregen] planning around {},{} failed", t[0], t[1], err);
                    } else {
                        level.getServer().execute(() -> markPlannedTile(level, t[2], t[3], cx, cz));
                        LOG.info("[pregen] planned roads around {},{} ({}/{})", t[0], t[1], planned + 1, planTotal);
                    }
                    planned++;
                    return null;
                }));
        }
    }

    /** Mirrors RoadPlanningService.markPlannedTile, so a player walking here later does not re-plan the tile. */
    private static void markPlannedTile(ServerLevel level, int kx, int kz, int centerChunkX, int centerChunkZ) {
        WorldDataProvider provider = WorldDataProvider.getInstance();
        long key = (long) kx << 32 ^ (long) kz & 0xFFFFFFFFL;
        Set<Long> keys = new HashSet<>(provider.getPlannedTileKeys(level));
        keys.add(key);
        Map<Long, Long> centers = new HashMap<>(provider.getPlannedTileCenters(level));
        centers.putIfAbsent(key, (long) centerChunkX << 32 ^ (long) centerChunkZ & 0xFFFFFFFFL);
        provider.setPlannedTileKeys(level, keys);
        provider.setPlannedTileCenters(level, centers);
    }

    /** True when a 5x5 sample of chunks over the square of this half-size is all fully generated. */
    private boolean areaGenerated(ServerLevel level, int x, int z, int half) {
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                ChunkPos cp = new ChunkPos((x + i * (half - 8) / 2) >> 4, (z + j * (half - 8) / 2) >> 4);
                Optional<CompoundTag> tag = level.getChunkSource().chunkMap.read(cp).join();
                if (tag.isEmpty() || !"minecraft:full".equals(tag.get().getString("Status"))) return false;
            }
        }
        return true;
    }

    private void tickPlan(ServerLevel level) {
        if (planning.isDone()) {
            roadsQuietChecks = 0;
            enter(Phase.ROADS);
        }
    }

    // ---- 3. wait for RoadWeaver to build every planned road ----

    private static int[] roadCounts(ServerLevel level) {
        int[] n = new int[ConnectionStatus.values().length];
        List<StructureConnection> list = WorldDataProvider.getInstance().getStructureConnections(level);
        if (list != null) for (StructureConnection c : list) n[c.status().ordinal()]++;
        return n;
    }

    private void tickRoads(ServerLevel level) {
        int[] n = roadCounts(level);
        int open = n[ConnectionStatus.PLANNED.ordinal()] + n[ConnectionStatus.GENERATING.ordinal()];
        // Planning publishes its connections through server.execute, so require a few quiet checks in a row.
        roadsQuietChecks = open == 0 ? roadsQuietChecks + 1 : 0;
        boolean timeout = phaseSeconds() > job.roadTimeoutSec;
        if (roadsQuietChecks >= 5 || timeout) {
            if (timeout) LOG.warn("[pregen] road timeout with {} roads still open", open);
            enter(Phase.CHUNKY);
        }
    }

    // ---- 4. Chunky, one area at a time ----

    private void tickChunky(ServerLevel level) {
        ChunkyAPI api = ChunkyProvider.get().getApi();
        if (chunkyIndex >= 0) {
            // Completion events can arrive late and out of order, so go by isRunning: the task must have been
            // seen running, then idle for two checks in a row (or never start within 30 s).
            if (api.isRunning(WORLD)) {
                chunkySeenRunning = true;
                chunkyIdleChecks = 0;
                return;
            }
            chunkyIdleChecks++;
            if (chunkySeenRunning ? chunkyIdleChecks < 2 : chunkyIdleChecks < 30) return;
        }
        chunkyIndex++;
        if (chunkyIndex >= areas.size()) {
            finish(level.getServer());
            return;
        }
        int[] a = areas.get(chunkyIndex);
        chunkySeenRunning = false;
        chunkyIdleChecks = 0;
        boolean ok = api.startTask(WORLD, "square", a[0], a[1], a[2], a[2], job.pattern);
        LOG.info("[pregen] chunky area {}/{} at {},{} r={} started={}", chunkyIndex + 1, areas.size(), a[0], a[1], a[2], ok);
    }

    private void finish(MinecraftServer server) {
        enter(Phase.DONE);
        server.saveEverything(false, true, true);
        LOG.info("[pregen] PREGEN COMPLETE; areas: {}", areas.stream().map(a -> a[0] + "," + a[1]).toList());
        if (job.stopWhenDone) server.halt(false);
    }

    private String status(MinecraftServer server) {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        int[] n = level == null ? new int[4] : roadCounts(level);
        return "phase=" + phase + " t=" + phaseSeconds() + "s frozen=" + server.tickRateManager().isFrozen()
            + " gatewaysPending=" + pendingGateways.size() + " areas=" + areas.size() + " planned=" + planned + "/" + planTotal
            + " roads[planned=" + n[ConnectionStatus.PLANNED.ordinal()] + " generating=" + n[ConnectionStatus.GENERATING.ordinal()]
            + " completed=" + n[ConnectionStatus.COMPLETED.ordinal()] + " failed=" + n[ConnectionStatus.FAILED.ordinal()] + "]"
            + " chunkyArea=" + (chunkyIndex + 1) + "/" + areas.size();
    }
}
