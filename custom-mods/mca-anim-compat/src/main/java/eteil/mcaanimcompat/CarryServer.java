package eteil.mcaanimcompat;

import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server half of the Princess Carry fixes (MCA Romantic Expansion). Only registered when that mod is loaded.
 * <ul>
 *   <li>Teleports take the carried player along. A vanilla player teleport moves only that player, and waystones
 *   teleport a player's mount but not their passengers, so the carried player was left behind. Within a dimension
 *   the carry is kept; across dimensions Romantic Expansion ends it on departure, so it is started again on arrival.</li>
 *   <li>Sneak + right-click on a block puts the carried player down there (the client no longer ends the carry on
 *   the carrier's sneak alone; see RomanticShiftMixin).</li>
 * </ul>
 */
public final class CarryServer {
    /** A carrier who lands farther than this from their passenger has teleported (movement corrections are shorter). */
    private static final double FOLLOW_DISTANCE = 8.0;
    /** Ticks between moving the passenger and remounting: same level, and after a dimension change. */
    private static final int REMOUNT_TICKS = 3;
    private static final int REMOUNT_TICKS_NEW_LEVEL = 20;
    /** How far from the carrier a put-down spot may be. */
    private static final double PUT_DOWN_REACH = 5.0;

    /** Carrier -> passenger, from Romantic Expansion ending the carry at departure until the carrier arrives. */
    private static final Map<UUID, Departure> DEPARTED = new HashMap<>();
    private static final List<Follow> FOLLOWS = new ArrayList<>();

    private record Departure(UUID passenger, long tick) {
    }

    private static final class Follow {
        final UUID carrier;
        final UUID passenger;
        /** The carry was ended (dimension change) and has to be started again rather than just remounted. */
        final boolean restart;
        boolean moved;
        int wait;

        Follow(UUID carrier, UUID passenger, boolean restart) {
            this.carrier = carrier;
            this.passenger = passenger;
            this.restart = restart;
            this.wait = 1;
        }
    }

    private CarryServer() {
    }

    public static void register() {
        // Before Romantic Expansion's own listener, which ends the carry.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CarryServer::onTravel);
        NeoForge.EVENT_BUS.addListener(CarryServer::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(CarryServer::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CarryServer::onRightClickBlock);
    }

    /** From ServerTeleportMixin, after any server-side teleport of {@code carrier}. */
    public static void onTeleported(ServerPlayer carrier) {
        UUID passengerId = CarryRuntime.getPassengerOf(carrier.getUUID());
        if (passengerId == null) return;
        ServerPlayer passenger = carrier.server.getPlayerList().getPlayer(passengerId);
        if (passenger == null) return;
        // Still riding at this point, so the passenger is where the carrier was before the teleport.
        if (passenger.level() == carrier.level() && passenger.distanceTo(carrier) <= FOLLOW_DISTANCE) return;
        follow(carrier.getUUID(), passengerId, false);
    }

    private static void follow(UUID carrier, UUID passenger, boolean restart) {
        FOLLOWS.removeIf(f -> f.carrier.equals(carrier));
        FOLLOWS.add(new Follow(carrier, passenger, restart));
    }

    private static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer carrier) {
            UUID passenger = CarryRuntime.getPassengerOf(carrier.getUUID());
            if (passenger != null) DEPARTED.put(carrier.getUUID(), new Departure(passenger, carrier.server.getTickCount()));
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Departure departure = DEPARTED.remove(event.getEntity().getUUID());
        if (departure != null) follow(event.getEntity().getUUID(), departure.passenger(), true);
    }

    private static void onTick(ServerTickEvent.Post event) {
        // A departure nobody arrived from (the travel was cancelled after we saw it).
        long now = event.getServer().getTickCount();
        DEPARTED.values().removeIf(d -> now - d.tick() > 100);
        for (Iterator<Follow> it = FOLLOWS.iterator(); it.hasNext(); ) {
            Follow f = it.next();
            if (--f.wait > 0) continue;
            ServerPlayer carrier = event.getServer().getPlayerList().getPlayer(f.carrier);
            ServerPlayer passenger = event.getServer().getPlayerList().getPlayer(f.passenger);
            if (!stillWanted(f, carrier, passenger)) {
                it.remove();
                continue;
            }
            if (!f.moved || passenger.level() != carrier.level() || passenger.distanceTo(carrier) > 2.0) {
                // teleportTo dismounts first and sends the passenger's client a proper position (and level) change.
                passenger.teleportTo(carrier.serverLevel(), carrier.getX(), carrier.getY(), carrier.getZ(), carrier.getYRot(), passenger.getXRot());
                if (!f.moved) {
                    f.moved = true;
                    f.wait = f.restart ? REMOUNT_TICKS_NEW_LEVEL : REMOUNT_TICKS;
                    continue;
                }
            }
            it.remove();
            if (f.restart) {
                CarryRuntime.startCarry(carrier, passenger);
            } else if (passenger.getVehicle() != carrier) {
                passenger.startRiding(carrier, true);
                CarryRuntime.broadcastCarryState(carrier.serverLevel(), f.carrier, f.passenger, true);
            }
            McaAnimCompat.LOG.info("[MCA Animation Compat] {} carried {} along through a teleport", carrier.getName().getString(),
                    passenger.getName().getString());
        }
    }

    private static boolean stillWanted(Follow f, ServerPlayer carrier, ServerPlayer passenger) {
        if (carrier == null || passenger == null || !carrier.isAlive() || !passenger.isAlive()) return false;
        if (f.restart) {
            return !CarryRuntime.isCarrier(f.carrier) && !CarryRuntime.isCarried(f.carrier)
                    && !CarryRuntime.isCarrier(f.passenger) && !CarryRuntime.isCarried(f.passenger);
        }
        // Either of them may have ended the carry (sneak, carry key) while the passenger was being moved.
        return CarryRuntime.isCarryPair(f.carrier, f.passenger);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer carrier) || !carrier.isShiftKeyDown()) return;
        UUID passengerId = CarryRuntime.getPassengerOf(carrier.getUUID());
        if (passengerId == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        ServerLevel level = carrier.serverLevel();
        ServerPlayer passenger = carrier.server.getPlayerList().getPlayer(passengerId);
        Vec3 spot = passenger == null ? null : putDownSpot(level, carrier, passenger, event.getHitVec());
        CarryRuntime.stopCarry(carrier.getUUID(), level);
        if (spot != null) {
            // Standing there facing the carrier.
            float yaw = (float) (Mth.atan2(carrier.getZ() - spot.z, carrier.getX() - spot.x) * Mth.RAD_TO_DEG) - 90.0F;
            passenger.teleportTo(level, spot.x, spot.y, spot.z, yaw, 0.0F);
        }
    }

    /** Where the passenger can stand for a click on {@code hit}, or null to leave it to the normal dismount. */
    private static Vec3 putDownSpot(ServerLevel level, ServerPlayer carrier, ServerPlayer passenger, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        List<Vec3> candidates = new ArrayList<>();
        if (hit.getDirection() == Direction.UP) {
            candidates.add(hit.getLocation());
            candidates.add(new Vec3(pos.getX() + 0.5, hit.getLocation().y, pos.getZ() + 0.5));
        } else {
            BlockPos at = level.getBlockState(pos).canBeReplaced() ? pos : pos.relative(hit.getDirection());
            candidates.add(Vec3.atBottomCenterOf(at));
        }
        for (Vec3 spot : candidates) {
            if (spot.distanceTo(carrier.position()) > PUT_DOWN_REACH) continue;
            if (level.noCollision(passenger, passenger.getDimensions(Pose.STANDING).makeBoundingBox(spot))) return spot;
        }
        return null;
    }
}
