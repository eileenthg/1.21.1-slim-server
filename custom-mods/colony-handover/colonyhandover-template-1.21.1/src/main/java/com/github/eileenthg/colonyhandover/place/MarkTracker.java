package com.github.eileenthg.colonyhandover.place;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import com.github.eileenthg.colonyhandover.mapping.BuildingMapping;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps gap beams alight until the player turns them off.
 *
 * <h2>Why a tick loop rather than a longer-lived particle</h2>
 *
 * Particle lifetime is not ours to set - it is baked into each particle type
 * client-side. END_ROD lives 60-71 ticks, but SOUL_FIRE_FLAME lives as little as
 * 12, so a single emission fades within seconds and at different rates for the
 * two colours. The only way to hold a beam is to keep re-sending it, which is
 * what {@link #EMIT_INTERVAL} does - set below the shortest lifetime so the two
 * colours stay equally solid.
 *
 * <h2>Why it re-scans</h2>
 *
 * The gap set is recomputed every {@link #RESCAN_INTERVAL} ticks against the
 * colony, not frozen at the moment you switched marks on. So when you seal a
 * hole its beam goes out within a few seconds - the tool tells you the fix
 * worked without you having to run anything again. When the last beam goes out
 * the watch ends itself and says so.
 *
 * <p>The colony is remembered by id rather than by looking up wherever the
 * player currently stands, because the whole point is to walk away from the
 * building and look back at it from outside.
 */
@EventBusSubscriber(modid = ColonyHandover.MODID)
public final class MarkTracker {

    private static final int SEED_RADIUS = 8;
    private static final int SEED_VERTICAL = 6;

    /** Below SOUL_FIRE_FLAME's 12-tick floor, so neither colour strobes. */
    private static final int EMIT_INTERVAL = 10;

    /** Five seconds - fast enough to feel responsive, slow enough not to cost. */
    private static final int RESCAN_INTERVAL = 100;

    private static final int BEAM_HEIGHT = 24;
    private static final int PARTICLES_PER_STEP = 3;
    private static final double SPREAD = 0.12D;

    /** Wall gaps want a door; ceiling gaps want roofing. Two colours, two jobs. */
    public record Gaps(Set<BlockPos> walls, Set<BlockPos> ceilings) {
        public boolean isEmpty() {
            return walls.isEmpty() && ceilings.isEmpty();
        }
    }

    private record Watch(int colonyId, ResourceKey<Level> dimension, Gaps gaps) {}

    private static final Map<UUID, Watch> WATCHING = new HashMap<>();
    private static int tick;

    public static boolean isWatching(ServerPlayer player) {
        return WATCHING.containsKey(player.getUUID());
    }

    public static void stop(ServerPlayer player) {
        WATCHING.remove(player.getUUID());
    }

    /** Switches marks on and returns what it found, so the caller can report it. */
    public static Gaps start(ServerPlayer player, IColony colony) {
        final ServerLevel level = player.serverLevel();
        final Gaps gaps = scan(level, colony);
        if (!gaps.isEmpty()) {
            WATCHING.put(player.getUUID(),
                    new Watch(colony.getID(), level.dimension(), gaps));
        }
        return gaps;
    }

    /**
     * Every gap in the colony, one entry per hole rather than one per hut -
     * neighbouring huts commonly share a doorway, and marking it six times just
     * makes six identical beams in the same block.
     */
    public static Gaps scan(ServerLevel level, IColony colony) {
        final Set<BlockPos> walls = new HashSet<>();
        final Set<BlockPos> ceilings = new HashSet<>();

        for (final Map.Entry<BlockPos, IBuilding> e
                : colony.getServerBuildingManager().getBuildings().entrySet()) {
            final String hutId = e.getValue().getBuildingType().getRegistryName().getPath();
            if (BuildingMapping.forHut(hutId).anchor() != BuildingMapping.Anchor.ROOM) {
                continue;
            }
            final Optional<BlockPos> seed =
                    Placer.findInteriorSeed(level, e.getKey(), SEED_RADIUS, SEED_VERTICAL);
            if (seed.isEmpty()) {
                continue;
            }
            Enclosure.findLeak(level, seed.get()).ifPresent(leak -> {
                if (leak.ceiling()) {
                    ceilings.add(leak.escape());
                } else {
                    walls.add(leak.crossing());
                }
            });
        }
        return new Gaps(walls, ceilings);
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (WATCHING.isEmpty()) {
            return;
        }
        tick++;
        final boolean rescan = tick % RESCAN_INTERVAL == 0;
        if (tick % EMIT_INTERVAL != 0) {
            return;
        }

        final MinecraftServer server = event.getServer();
        for (final Map.Entry<UUID, Watch> entry : new ArrayList<>(WATCHING.entrySet())) {
            final ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            // Logged out - drop the watch rather than ticking it forever.
            if (player == null) {
                WATCHING.remove(entry.getKey());
                continue;
            }
            Watch watch = entry.getValue();
            final ServerLevel level = server.getLevel(watch.dimension());
            if (level == null) {
                continue;
            }

            if (rescan) {
                final IColony colony = IMinecoloniesAPI.getInstance().getColonyManager()
                        .getColonyByWorld(watch.colonyId(), level);
                // A colony that has gone away mid-watch (handed over, deleted) has
                // nothing left to mark, so stop quietly rather than erroring.
                if (colony == null) {
                    WATCHING.remove(entry.getKey());
                    continue;
                }
                final Gaps fresh = scan(level, colony);
                if (fresh.isEmpty()) {
                    WATCHING.remove(entry.getKey());
                    player.sendSystemMessage(Component
                            .literal("Every building is sealed - marks off.")
                            .withStyle(ChatFormatting.GREEN));
                    continue;
                }
                watch = new Watch(watch.colonyId(), watch.dimension(), fresh);
                WATCHING.put(entry.getKey(), watch);
            }

            for (final BlockPos pos : watch.gaps().walls()) {
                beam(level, player, pos, true);
            }
            for (final BlockPos pos : watch.gaps().ceilings()) {
                beam(level, player, pos, false);
            }
        }
    }

    /**
     * A column rather than a puff.
     *
     * <p>Denser in the bottom three blocks so the base still marks the exact
     * cell, thinner above so the beam reads from outside the building without
     * becoming a fog bank up close.
     */
    private static void beam(ServerLevel level, ServerPlayer player, BlockPos pos, boolean wall) {
        for (int dy = 0; dy < BEAM_HEIGHT; dy++) {
            final int count = dy < 3 ? PARTICLES_PER_STEP * 3 : PARTICLES_PER_STEP;
            level.sendParticles(player,
                    wall ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME,
                    true,
                    pos.getX() + 0.5D, pos.getY() + 0.5D + dy, pos.getZ() + 0.5D,
                    count, SPREAD, SPREAD, SPREAD, 0.0D);
        }
    }

    private MarkTracker() {}
}
