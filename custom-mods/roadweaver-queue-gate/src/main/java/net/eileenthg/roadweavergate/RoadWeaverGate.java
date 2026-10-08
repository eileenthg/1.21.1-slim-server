package net.eileenthg.roadweavergate;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod(RoadWeaverGate.MOD_ID)
public final class RoadWeaverGate {

    public static final String MOD_ID = "roadweavergate";

    public RoadWeaverGate(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, GateConfig.SPEC);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> QueueGate.markDirty("server start"));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> QueueGate.markPlayerMoved("login"));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> QueueGate.markPlayerMoved("dimension change"));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> QueueGate.markPlayerMoved("respawn"));
        // Covers /tp, ender pearls, chorus fruit and mod teleports that go through NeoForge's event.
        NeoForge.EVENT_BUS.addListener((EntityTeleportEvent e) -> {
            if (e.getEntity() instanceof Player) QueueGate.markPlayerMoved("teleport");
        });
    }
}
