package com.github.eileenthg.tamablearcadianfairy.client;

import com.github.eileenthg.tamablearcadianfairy.network.OpenFairyScreenPayload;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client-only entry point, kept apart so the server never loads Minecraft client classes. */
@OnlyIn(Dist.CLIENT)
public final class ClientNetworkHandler {

    private ClientNetworkHandler() {}

    public static void openFairyScreen(OpenFairyScreenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        Entity fairy = minecraft.level.getEntity(payload.entityId());
        if (fairy instanceof LivingEntity living && FairyTaming.isTamed(living)) {
            minecraft.setScreen(new FairyScreen(living));
        }
    }
}
