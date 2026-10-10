package eteil.spawnonboarding;

import eteil.spawnonboarding.compat.EternalTalesCompat;
import eteil.spawnonboarding.compat.McaCompat;
import eteil.spawnonboarding.compat.OriginsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OnboardingController {
    /** Ticks between the destiny screen closing and placement, so MCA's async teleport has landed. */
    private static final int DESTINY_SETTLE_TICKS = 40;
    private static final int TOWN_MARGIN = 6;
    /** Eternal Tales places its town with the corner at the player minus (13, 5, 13). */
    private static final int TOWN_GROUND_OFFSET_XZ = 13;
    private static final int TOWN_GROUND_OFFSET_Y = 5;
    /** Keeps the town template from being pasted over the player who just spawned. */
    private static final int PLAYER_CLEARANCE = 6;
    private static final String DESTINY_DONE_TAG = "spawnonboarding:destinyDone";

    private OnboardingController() {}

    // ---------------------------------------------------------------- lifecycle

    public static void onServerStarted(MinecraftServer server) {
        OnboardingState state = OnboardingState.get(server);
        if (state.townPending == null) {
            state.townPending = SpawnOnboarding.ETERNAL_TALES
                    && EternalTalesCompat.townFlag(server.overworld()) == EternalTalesCompat.FLAG_NOT_HANDLED;
        }
        state.setDirty();
        SpawnOnboarding.LOG.info("[SpawnOnboarding] world state: stage={} townPending={}", state.stage, state.townPending);
    }

    /** Called from the MCA DestinyMessage mixin when the destiny screen is closed. */
    public static void onDestinyClosed(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.execute(() -> finalizeSpawn(player, "destiny screen closed"));
        }
    }

    /** MCA teleported the player to a picked destiny location (not used by "somewhere"). */
    public static void onDestinyTeleport(ServerPlayer player, BlockPos pos) {
        MinecraftServer server = player.getServer();
        if (server == null || player.level() != server.overworld()) return;
        OnboardingState state = OnboardingState.get(server);
        if (state.stage == OnboardingState.Stage.WAITING || state.stage == OnboardingState.Stage.FINALIZED) {
            state.destinyPos = player.blockPosition();
            state.setDirty();
        }
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        // Next tick, so MCA's own join handling has already decided whether to show destiny.
        server.execute(() -> {
            if (!SpawnOnboarding.MCA) {
                finalizeSpawn(player, "MCA not installed");
            } else if (McaCompat.customizationDone(player)) {
                finalizeSpawn(player, "player already past destiny");
            }
        });
    }

    private static void finalizeSpawn(ServerPlayer player, String reason) {
        player.getPersistentData().putBoolean(DESTINY_DONE_TAG, true);
        OnboardingState state = OnboardingState.get(player.server);
        if (state.stage != OnboardingState.Stage.WAITING) return;
        state.stage = OnboardingState.Stage.FINALIZED;
        state.readyAtTick = player.server.overworld().getGameTime() + DESTINY_SETTLE_TICKS;
        // Locked in now: the player may walk or teleport away before placement runs (menus pause the game).
        if (state.destinyPos == null) {
            state.destinyPos = player.level() == player.server.overworld() ? player.blockPosition() : player.server.overworld().getSharedSpawnPos();
        }
        state.setDirty();
        SpawnOnboarding.LOG.info("[SpawnOnboarding] spawn finalized by {} ({})", player.getName().getString(), reason);
    }

    // ---------------------------------------------------------------- tick

    public static void onServerTick(MinecraftServer server) {
        OnboardingState state = OnboardingState.get(server);
        if (state.stage == OnboardingState.Stage.FINALIZED && server.overworld().getGameTime() >= state.readyAtTick) {
            runPlacement(server, state);
        }
    }

    private static void runPlacement(MinecraftServer server, OnboardingState state) {
        ServerLevel level = server.overworld();
        BlockPos center = state.destinyPos != null ? state.destinyPos : level.getSharedSpawnPos();
        state.destinyPos = center;
        if (Boolean.TRUE.equals(state.townPending)) {
            placeTown(level, new PlacementFinder(level), center, new ArrayList<>(), state);
            state.townPending = false;
        }
        state.stage = OnboardingState.Stage.DONE;
        state.setDirty();
    }

    /** The town is searched on rings around the spawn point, clear of the player and of villages. */
    private static void placeTown(ServerLevel level, PlacementFinder finder, BlockPos center, List<BoundingBox> reserved, OnboardingState state) {
        reserved.add(finder.footprint(center, PLAYER_CLEARANCE, PLAYER_CLEARANCE));
        String name = EternalTalesCompat.templateFor(level, center);
        BlockPos spot = null;
        StructureTemplate template = null;
        // The template depends on the biome where the town ends up, so re-check once after moving.
        for (int attempt = 0; attempt < 2; attempt++) {
            Optional<StructureTemplate> t = level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath("eternal_tales", name));
            if (t.isEmpty()) {
                SpawnOnboarding.LOG.error("[SpawnOnboarding] Eternal Tales template {} not found", name);
                return;
            }
            template = t.get();
            Vec3i size = template.getSize();
            spot = finder.find(center, size.getX() / 2, size.getZ() / 2, TOWN_MARGIN, 0, reserved);
            if (spot == null) return;
            String there = EternalTalesCompat.templateFor(level, spot);
            if (there.equals(name)) break;
            name = there;
        }
        Vec3i size = template.getSize();
        int cornerX = spot.getX() - size.getX() / 2;
        int cornerZ = spot.getZ() - size.getZ() / 2;
        int feetY = PlacementFinder.surfaceY(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cornerX + TOWN_GROUND_OFFSET_XZ, cornerZ + TOWN_GROUND_OFFSET_XZ);
        BlockPos corner = new BlockPos(cornerX, feetY - TOWN_GROUND_OFFSET_Y, cornerZ);
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false);
        template.placeInWorld(level, corner, corner, settings, level.random, 3);
        state.townOrigin = corner;
        EternalTalesCompat.markTownPlaced(level);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            EternalTalesCompat.removeSpawners(p);
        }
        SpawnOnboarding.LOG.info("[SpawnOnboarding] placed eternal_tales:{} ({}x{}x{}) with corner at {}", name, size.getX(), size.getY(), size.getZ(), corner);
    }

    // ---------------------------------------------------------------- gates

    /** Destiny finished and every Origins layer chosen. */
    public static boolean playerReady(ServerPlayer player) {
        boolean destiny = !SpawnOnboarding.MCA
                || player.getPersistentData().getBoolean(DESTINY_DONE_TAG)
                || McaCompat.customizationDone(player);
        boolean origins = !SpawnOnboarding.ORIGINS || OriginsCompat.allLayersChosen(player);
        return destiny && origins;
    }
}
