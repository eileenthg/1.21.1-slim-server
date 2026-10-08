package net.eileenthg.gentleswings;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Which entities a gentle swing passes through. A server config, so clients get the server's copy. */
public final class SwingConfig {

    public static final ModConfigSpec SERVER_SPEC;
    private static final ModConfigSpec.BooleanValue SPARE_HOSTILES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SPARE_HOSTILES = b
                .comment("If true, gentle swings also pass through hostile mobs (zombies, creepers, ...).",
                        "Off by default so you can still defend yourself without toggling first.")
                .define("spareHostileMobs", false);
        SERVER_SPEC = b.build();
    }

    private SwingConfig() {}

    /** Whether a gentle swing should pass through this entity: any living thing except hostile mobs. */
    public static boolean spares(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity instanceof Enemy) return SERVER_SPEC.isLoaded() && SPARE_HOSTILES.get();
        return true;
    }
}
