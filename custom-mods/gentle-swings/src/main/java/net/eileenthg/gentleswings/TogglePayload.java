package net.eileenthg.gentleswings;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the toggle key was pressed. The server flips the state and answers with {@link StatePayload}. */
public record TogglePayload() implements CustomPacketPayload {

    public static final TogglePayload INSTANCE = new TogglePayload();
    public static final Type<TogglePayload> TYPE = new Type<>(GentleSwings.id("toggle"));
    public static final StreamCodec<ByteBuf, TogglePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<TogglePayload> type() {
        return TYPE;
    }
}
