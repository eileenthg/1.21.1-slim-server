package eteil.spawnonboarding.compat;

import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.origin.OriginManager;
import net.minecraft.server.level.ServerPlayer;

/** Only call when Origins is loaded. */
public final class OriginsCompat {
    private OriginsCompat() {}

    public static boolean allLayersChosen(ServerPlayer player) {
        return OriginManager.firstUnchosenLayer(player, PlayerOriginsAttachment.getOrCreate(player)) == null;
    }
}
