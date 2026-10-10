package eteil.mcaanimcompat;

import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.util.UUID;

/**
 * Client-side Princess Carry state from MCA Romantic Expansion. Every call checks {@link #PRESENT} first, so the
 * Romantic Expansion classes are never resolved when that mod is not installed.
 */
public final class Carry {
    public static final boolean PRESENT = ModList.get().isLoaded("mcaromanticexpansion");

    /** Arm angles Romantic Expansion gives the carrier (PlayerCarryArmsMixin), in radians. */
    public static final float RIGHT_ARM_X = (float) Math.toRadians(-40.0);
    public static final float RIGHT_ARM_Y = (float) Math.toRadians(29.0);
    public static final float LEFT_ARM_X = (float) Math.toRadians(-40.0);
    public static final float LEFT_ARM_Y = (float) Math.toRadians(-22.0);

    private Carry() {
    }

    public static boolean isCarrier(UUID player) {
        return PRESENT && CarryClientState.isCarrier(player);
    }

    public static boolean isCarried(UUID player) {
        return PRESENT && CarryClientState.isCarried(player);
    }

    /** The carrier of a carried player, if that carrier is loaded on this client. */
    public static Player carrierOf(UUID passenger) {
        if (!PRESENT) return null;
        UUID carrier = CarryClientState.carrierOf(passenger);
        return carrier == null ? null : CarryClientState.getPlayerByUUID(carrier);
    }

    /** The other half of a carry pair (carrier or passenger), or null. */
    public static UUID partnerOf(UUID player) {
        if (!PRESENT) return null;
        UUID passenger = CarryClientState.passengerOf(player);
        return passenger != null ? passenger : CarryClientState.carrierOf(player);
    }
}
