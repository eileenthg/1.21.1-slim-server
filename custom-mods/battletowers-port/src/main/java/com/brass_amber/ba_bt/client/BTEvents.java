package com.brass_amber.ba_bt.client;

import net.neoforged.fml.common.EventBusSubscriber;
import com.brass_amber.ba_bt.BABattleTowers;
import net.neoforged.api.distmarker.Dist;

import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;


import static com.brass_amber.ba_bt.BABattleTowers.SAVE_TOWERS;


@EventBusSubscriber(modid = BABattleTowers.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class BTEvents {

    // Upstream's PlayerRespawnEvent handler stopped tower music, but only behind isClientSide(), which is never
    // true for that server-side event. It was dead code that referenced client-only classes, so it was removed.

    @SubscribeEvent
    public static void serverSetup(ServerAboutToStartEvent event) {
        SAVE_TOWERS.setServer(event.getServer());
    }


    @SubscribeEvent
    public static void serverStop(ServerStoppedEvent event) {
        SAVE_TOWERS.serverClosed();
    }

}