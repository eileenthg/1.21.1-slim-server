package com.github.eileenthg.tamablearcadianfairy.network;

import com.github.eileenthg.tamablearcadianfairy.TamableArcadianFairy;
import com.github.eileenthg.tamablearcadianfairy.client.ClientNetworkHandler;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TamableArcadianFairy.MOD_ID)
public final class ModNetworking {

    private ModNetworking() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(OpenFairyScreenPayload.TYPE, OpenFairyScreenPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        ClientNetworkHandler.openFairyScreen(payload);
                    }
                });

        registrar.playToServer(UpdateFairySettingsPayload.TYPE, UpdateFairySettingsPayload.STREAM_CODEC,
                ModNetworking::applySettings);
    }

    /** Never trust the client: check the sender really owns this fairy and clamp the values. */
    private static void applySettings(UpdateFairySettingsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;

        Entity target = player.level().getEntity(payload.entityId());
        if (target == null || !FairyTaming.isOwner(target, player)) return;
        if (player.distanceToSqr(target) > 64.0) return;

        FairyTaming.setActState(target, payload.state().sanitised());
    }
}
