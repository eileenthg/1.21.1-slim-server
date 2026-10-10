package eteil.seatrest;

import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

/**
 * Reads Take a Seat's authoritative sit state.
 *
 * <p>Take a Seat keeps no entity and no capability: the client decides it is sitting (keybind, or
 * right-clicking a sittable block) and sends a StartSit payload, and the server records the player
 * in {@code TakeASeatNetworking.SITTING}, a {@code ConcurrentHashMap<UUID, ResourceLocation>} it
 * clears again on StopSit, on logout, and in its own server tick. That map is the only place both
 * ways of sitting converge, so it is what we read. It is private, hence the reflection; a lookup
 * failure disables the feature rather than breaking the game.
 */
public final class SitState {
    private static final String NETWORKING_CLASS = "com.takeaseat.network.TakeASeatNetworking";
    private static final String FIELD_NAME = "SITTING";

    private static Field sittingField;
    private static boolean resolved;
    private static boolean warned;

    private SitState() {
    }

    public static boolean isSitting(Player player) {
        if (player == null) return false;
        Map<UUID, ?> sitting = sittingMap();
        return sitting != null && sitting.containsKey(player.getUUID());
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ?> sittingMap() {
        Field field = field();
        if (field == null) return null;
        try {
            return (Map<UUID, ?>) field.get(null);
        } catch (Throwable t) {
            warnOnce(t);
            return null;
        }
    }

    private static Field field() {
        if (!resolved) {
            resolved = true;
            try {
                Field f = Class.forName(NETWORKING_CLASS).getDeclaredField(FIELD_NAME);
                f.setAccessible(true);
                sittingField = f;
            } catch (Throwable t) {
                warnOnce(t);
            }
        }
        return sittingField;
    }

    private static void warnOnce(Throwable t) {
        if (warned) return;
        warned = true;
        SeatRest.LOGGER.warn("Could not read Take a Seat's sit state ({}.{}); "
                + "hunger and thirst will drain at their normal rate while sitting.",
                NETWORKING_CLASS, FIELD_NAME, t);
    }
}
