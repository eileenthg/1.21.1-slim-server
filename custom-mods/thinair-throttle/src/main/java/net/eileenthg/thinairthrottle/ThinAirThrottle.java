package net.eileenthg.thinairthrottle;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(ThinAirThrottle.MOD_ID)
public final class ThinAirThrottle {

    public static final String MOD_ID = "thinairthrottle";

    public ThinAirThrottle(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ThrottleConfig.SPEC);
    }
}
