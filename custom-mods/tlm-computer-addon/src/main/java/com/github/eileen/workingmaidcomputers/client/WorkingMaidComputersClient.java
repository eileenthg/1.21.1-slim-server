package com.github.eileen.workingmaidcomputers.client;

import com.craisinlord.antos.content.client.AntOSClientHooks;
import com.craisinlord.antos.content.client.FloppyDiskModelProperties;
import com.craisinlord.antos.content.client.game.AntOSComputerGames;
import com.craisinlord.antos.content.client.renderer.RewardDropRenderer;
import com.craisinlord.antos.content.client.screen.ComputerScreen;
import com.craisinlord.antos.content.network.AnternetAccountNetworking;
import com.craisinlord.antos.content.network.AntmailNetworking;
import com.craisinlord.antos.content.network.ComputerNetworking;
import com.github.eileen.workingmaidcomputers.WorkingMaidComputers;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = WorkingMaidComputers.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WorkingMaidComputersClient {
    private WorkingMaidComputersClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            NeoForge.EVENT_BUS.addListener(WorkingMaidComputersClient::onClientLogout);
            AntOSComputerGames.register();
            AntOSClientHooks.setComputerOpener(ComputerScreen::openComputer);
            FloppyDiskModelProperties.register(WorkingMaidComputers.FLOPPY_DISK.get());
            ComputerNetworking.setSender(PacketDistributor::sendToServer);
            AnternetAccountNetworking.setSender(PacketDistributor::sendToServer);
            AntmailNetworking.setSender(payload -> {
                if (payload instanceof CustomPacketPayload packet) {
                    PacketDistributor.sendToServer(packet);
                }
            });
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WorkingMaidComputers.REWARD_DROP_ENTITY.get(), RewardDropRenderer::new);
    }

    private static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        AntOSClientHooks.clearConnectionState();
    }
}
