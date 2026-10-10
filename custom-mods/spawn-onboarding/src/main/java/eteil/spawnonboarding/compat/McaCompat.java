package eteil.spawnonboarding.compat;

import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.server.level.ServerPlayer;

/** Only call when MCA is loaded. */
public final class McaCompat {
    private McaCompat() {}

    /** MCA launches the destiny screen on join exactly when this is false. */
    public static boolean customizationDone(ServerPlayer player) {
        return PlayerSaveData.get(player).isEntityDataSet();
    }
}
