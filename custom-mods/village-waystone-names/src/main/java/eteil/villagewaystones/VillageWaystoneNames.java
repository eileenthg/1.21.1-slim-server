package eteil.villagewaystones;

import java.util.Comparator;
import java.util.Optional;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.MutableWaystone;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneOrigin;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.api.event.GenerateWaystoneNameEvent;
import net.blay09.mods.waystones.core.WaystoneManagerImpl;
import net.blay09.mods.waystones.core.WaystoneSyncManager;
import net.conczin.mca.server.world.data.Village;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Village waystones take the name of the MCA village they stand in.
 *
 * Waystones names a generated waystone lazily, on first activation, from its own name generator; we override that
 * via GenerateWaystoneNameEvent. MCA may not have registered the village yet at that point, and players can rename
 * villages (or waystones), so a periodic pass re-applies the village name to every village-origin waystone.
 */
@Mod(VillageWaystoneNames.MOD_ID)
public class VillageWaystoneNames {
    public static final String MOD_ID = "villagewaystones";
    private static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    /** Waystones in villages sit near the well/centre, but allow for ones placed at the village edge. */
    private static final int VILLAGE_MARGIN = 64;
    private static final int SYNC_INTERVAL_TICKS = 100;

    private int tickCounter;

    public VillageWaystoneNames(IEventBus modBus) {
        Balm.getEvents().onEvent(GenerateWaystoneNameEvent.class, this::onGenerateName);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
    }

    private void onGenerateName(GenerateWaystoneNameEvent event) {
        MinecraftServer server = currentServer();
        if (server == null) {
            return;
        }
        villageName(server, event.getWaystone()).ifPresent(name -> event.setName(Component.literal(name)));
    }

    private void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < SYNC_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        MinecraftServer server = event.getServer();
        WaystonesAPI.getAllWaystones(server).toList().forEach(waystone -> {
            if (!(waystone instanceof MutableWaystone mutable)) {
                return;
            }
            villageName(server, waystone).ifPresent(name -> {
                if (name.equals(waystone.getName().getString())) {
                    return;
                }
                LOG.info("Renaming village waystone at {} from '{}' to '{}'",
                        waystone.getPos(), waystone.getName().getString(), name);
                mutable.setName(Component.literal(name));
                WaystoneManagerImpl.get(server).updateWaystone(waystone);
                WaystoneSyncManager.sendWaystoneUpdateToAll(server, waystone);
            });
        });
    }

    private static Optional<String> villageName(MinecraftServer server, Waystone waystone) {
        if (waystone.getOrigin() != WaystoneOrigin.VILLAGE) {
            return Optional.empty();
        }
        ServerLevel level = server.getLevel(waystone.getDimension());
        if (level == null) {
            return Optional.empty();
        }
        BlockPos pos = waystone.getPos();
        // A village with no buildings has MCA's placeholder box at 0,0,0; don't let it claim waystones near spawn.
        return VillageManager.get(level)
                .findVillages(village -> hasBuildings(village) && village.isWithinBorder(pos, VILLAGE_MARGIN))
                .min(Comparator.comparingDouble(village -> village.getCenter().distSqr(pos)))
                .map(Village::getName)
                .filter(name -> name != null && !name.isBlank());
    }

    private static boolean hasBuildings(Village village) {
        BoundingBox box = village.getBox();
        return box.minX() != 0 || box.maxX() != 0 || box.minZ() != 0 || box.maxZ() != 0
                || box.minY() != 0 || box.maxY() != 0;
    }

    private static MinecraftServer currentServer() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }
}
