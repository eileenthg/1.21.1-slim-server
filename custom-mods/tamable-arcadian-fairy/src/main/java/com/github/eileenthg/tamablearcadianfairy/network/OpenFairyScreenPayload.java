package com.github.eileenthg.tamablearcadianfairy.network;

import com.github.eileenthg.tamablearcadianfairy.TamableArcadianFairy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: open the settings screen for this fairy. */
public record OpenFairyScreenPayload(int entityId) implements CustomPacketPayload {

    public static final Type<OpenFairyScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TamableArcadianFairy.MOD_ID, "open_fairy_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFairyScreenPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenFairyScreenPayload::entityId,
                    OpenFairyScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
