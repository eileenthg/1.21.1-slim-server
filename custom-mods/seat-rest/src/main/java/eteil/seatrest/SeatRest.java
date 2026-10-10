package eteil.seatrest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SeatRest.MOD_ID)
public class SeatRest {
    public static final String MOD_ID = "seatrest";
    public static final Logger LOGGER = LoggerFactory.getLogger("Seat Rest");

    public SeatRest(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SeatRestConfig.SPEC);
        LOGGER.info("Seat Rest initialized.");
    }
}
