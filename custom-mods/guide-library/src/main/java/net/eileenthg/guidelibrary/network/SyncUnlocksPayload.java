package net.eileenthg.guidelibrary.network;

import io.netty.buffer.ByteBuf;
import net.eileenthg.guidelibrary.GuideLibrary;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server to client: the player's full set of unlocked book keys, plus the ones that were just
 * added (empty on login), so the client can announce them.
 */
public record SyncUnlocksPayload(List<String> keys, List<String> added) implements CustomPacketPayload {

    public static final Type<SyncUnlocksPayload> TYPE = new Type<>(GuideLibrary.id("sync_unlocks"));

    private static final StreamCodec<ByteBuf, List<String>> LIST =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final StreamCodec<ByteBuf, SyncUnlocksPayload> STREAM_CODEC =
            StreamCodec.composite(LIST, SyncUnlocksPayload::keys, LIST, SyncUnlocksPayload::added,
                    SyncUnlocksPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
