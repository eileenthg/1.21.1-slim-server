package com.github.eileenthg.tamablearcadianfairy.network;

import com.github.eileenthg.tamablearcadianfairy.TamableArcadianFairy;
import com.github.eileenthg.tamablearcadianfairy.data.ActState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the owner changed a setting in the fairy screen. */
public record UpdateFairySettingsPayload(int entityId, ActState state) implements CustomPacketPayload {

    public static final Type<UpdateFairySettingsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TamableArcadianFairy.MOD_ID, "update_fairy_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateFairySettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, UpdateFairySettingsPayload::entityId,
                    ActState.STREAM_CODEC, UpdateFairySettingsPayload::state,
                    UpdateFairySettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
