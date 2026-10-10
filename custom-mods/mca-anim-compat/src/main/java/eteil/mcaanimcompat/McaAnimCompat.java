package eteil.mcaanimcompat;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/** Mostly client-side; the Princess Carry teleport and put-down rules run wherever the world is hosted. */
@Mod(McaAnimCompat.MODID)
public class McaAnimCompat {
    public static final String MODID = "mcaanimcompat";
    public static final Logger LOG = LogUtils.getLogger();

    public McaAnimCompat(ModContainer container) {
        boolean romantic = ModList.get().isLoaded("mcaromanticexpansion");
        if (romantic) CarryServer.register();
        if (FMLEnvironment.dist.isClient()) McaAnimCompatClient.init(container);
        LOG.info("[MCA Animation Compat] loaded (Romantic Expansion carry fixes: {})", romantic);
    }
}
