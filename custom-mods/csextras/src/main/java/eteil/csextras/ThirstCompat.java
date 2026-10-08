package eteil.csextras;

import dev.ghen.thirst.foundation.common.capability.ModAttachment;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/** Thirst Was Taken access; every call is guarded by {@link #LOADED}. */
public final class ThirstCompat {
    public static final boolean LOADED = ModList.get().isLoaded("thirst");

    static int thirst(Player player) {
        return Impl.thirst(player);
    }

    static void exhaust(Player player, float amount) {
        Impl.exhaust(player, amount);
    }

    /** Separate class so TWT types are only resolved when the mod is present. */
    private static final class Impl {
        static int thirst(Player player) {
            return player.getData(ModAttachment.PLAYER_THIRST.get()).getThirst();
        }

        static void exhaust(Player player, float amount) {
            if (amount > 0) player.getData(ModAttachment.PLAYER_THIRST.get()).addExhaustion(player, amount);
        }
    }

    private ThirstCompat() {
    }
}
