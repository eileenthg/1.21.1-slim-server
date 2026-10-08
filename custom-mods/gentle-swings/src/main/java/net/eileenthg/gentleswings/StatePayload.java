package net.eileenthg.gentleswings;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: this player's current gentle-swing state. */
public record StatePayload(boolean gentle) implements CustomPacketPayload {

    public static final Type<StatePayload> TYPE = new Type<>(GentleSwings.id("state"));
    public static final StreamCodec<ByteBuf, StatePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(StatePayload::new, StatePayload::gentle);

    @Override
    public Type<StatePayload> type() {
        return TYPE;
    }
}
