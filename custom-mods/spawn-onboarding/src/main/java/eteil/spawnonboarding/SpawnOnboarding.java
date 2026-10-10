package eteil.spawnonboarding;

import com.mojang.logging.LogUtils;
import eteil.spawnonboarding.client.ClientScreenQueue;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Makes MCA's destiny choice the real spawn for Eternal Tales' Eternal Town, and orders the first-join
 * screens: destiny, then Origins. Also lets Alex's Mobs' tarantula hawk hunt other mods' spiders
 * (see CreatureLinks). Every integration is skipped when its mod is absent.
 */
@Mod(SpawnOnboarding.MODID)
public class SpawnOnboarding {
    public static final String MODID = "spawnonboarding";
    public static final Logger LOG = LogUtils.getLogger();

    public static final boolean MCA = ModList.get().isLoaded("mca");
    public static final boolean ORIGINS = ModList.get().isLoaded("origins");
    public static final boolean ETERNAL_TALES = ModList.get().isLoaded("eternal_tales");

    public SpawnOnboarding(IEventBus modBus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> OnboardingController.onServerStarted(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> OnboardingController.onServerTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) OnboardingController.onPlayerLoggedIn(p);
        });
        if (ModList.get().isLoaded("alexsmobs")) {
            NeoForge.EVENT_BUS.addListener(eteil.spawnonboarding.compat.CreatureLinks::onJoin);
        }
        if (FMLEnvironment.dist.isClient()) {
            ClientScreenQueue.register();
        }
        LOG.info("[SpawnOnboarding] integrations: mca={} origins={} eternal_tales={}", MCA, ORIGINS, ETERNAL_TALES);
    }
}
