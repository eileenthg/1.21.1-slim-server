package net.reimaden.touhouorigins.shikigami;

import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.OptionalInt;
import java.util.UUID;

/**
 * Keeps the entity set in step with the stored pact.
 *
 * <p>Two directions, both needed:
 *
 * <ul>
 *   <li><b>Re-seed.</b> If a servant has a stored master and still reads as linked, but the set has
 *       lost them — after the master died, or after a restart — put them back, provided the master
 *       is online. This is what makes the glow, the teleport and the shared death survive a death.</li>
 *   <li><b>Adopt.</b> If a servant reads as linked but has no stored master, take the one the set
     *       still holds. Pacts sealed before this record existed are otherwise stranded.</li>
     *   <li><b>Forget.</b> If the boolean has gone (the pact was broken through the origin screen),
 *       drop the stored master so a stale UUID cannot resurrect a pact that was deliberately
 *       ended.</li>
 * </ul>
 *
 * <p>Runs on a 40-tick cadence rather than every tick: it is a repair pass, not a hot path, and the
 * only visible cost of a two-second delay is the glow taking a moment to return after a respawn.
 */
public final class ShikigamiPactHandler {

    private static final int INTERVAL = 40;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 0) return;

        EntitySetPower.Cfg setCfg = EntitySetPower.resolveCfg(ShikigamiPact.SET_POWER);
        if (setCfg == null) return;

        for (ServerPlayer servant : server.getPlayerList().getPlayers()) {
            ShikigamiPact.Data pact = servant.getData(ShikigamiPact.PACT.get());

            if (!linked(servant)) {
                if (pact.master().isPresent()) {
                    servant.setData(ShikigamiPact.PACT.get(), ShikigamiPact.Data.EMPTY);
                }
                continue;
            }

            if (pact.master().isEmpty()) {
                UUID adopted = adoptFromSet(servant);
                if (adopted == null) continue;
                pact = new ShikigamiPact.Data(java.util.Optional.of(adopted));
                servant.setData(ShikigamiPact.PACT.get(), pact);
                TouhouOrigins.LOGGER.info("[Shikigami] Adopted {}'s existing link to {} into the pact record",
                    servant.getGameProfile().getName(), adopted);
            }

            UUID masterId = pact.master().get();
            ServerPlayer master = server.getPlayerList().getPlayer(masterId);
            if (master == null || master == servant) continue;

            if (!EntitySetPower.contains(servant, ShikigamiPact.SET_POWER, master)) {
                EntitySetPower.add(servant, ShikigamiPact.SET_POWER, setCfg, master, OptionalInt.empty());
                TouhouOrigins.LOGGER.debug("[Shikigami] Re-seeded {}'s pact with master {}",
                    servant.getGameProfile().getName(), masterId);
            }
        }
    }

    /**
     * Recovers a master from the live set for a pact sealed before the record existed.
     *
     * <p>Without this, every link made by an earlier build would stay invisible to the glow and the
     * shared death until it was broken and re-sworn. The set carries exactly one master per servant,
     * so the first entry is unambiguous.
     */
    private static UUID adoptFromSet(ServerPlayer servant) {
        for (UUID candidate : EntitySetPower.iterationOrder(servant, ShikigamiPact.SET_POWER, false)) {
            if (!candidate.equals(servant.getUUID())) return candidate;
        }
        return null;
    }

    /** The pact's own record of itself: the boolean resource, which unlike the set is persisted. */
    private static boolean linked(ServerPlayer player) {
        PowerContainer container = PowerContainer.of(player);
        if (container == null) return false;
        return PowerResources.read(container, ShikigamiPact.BOOLEAN_POWER).orElse(0) == 1;
    }
}
