package com.github.eileenthg.colonyhandover.network;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The two messages the town hall window needs.
 *
 * <h2>Why this mod needs networking at all now</h2>
 *
 * Everything so far avoided packets: commands report through chat, and the checklist
 * scroll carries its survey in an item component that syncs by itself. The town hall
 * window has neither escape hatch. It is opened from a block screen, not an item, and
 * it has to show numbers only the server can compute - a building survey needs the
 * real world. So the window asks, and the server answers.
 *
 * <p>Kept to exactly two payloads - one request carrying an action, one reply
 * carrying a summary - so the protocol stays small enough to reason about.
 */
public final class HandoverPayloads {

    /** What the window is asking the server to do. */
    public enum Action {
        /** Survey the colony and send back a {@link Summary}. */
        SUMMARY,
        /** Hand the player a Conversion Checklist, already surveyed. */
        CHECKLIST,
        /** Sign the charter: run the handover. */
        GRANT;

        public static final StreamCodec<ByteBuf, Action> CODEC =
                ByteBufCodecs.VAR_INT.map(i -> values()[Math.floorMod(i, values().length)], Action::ordinal);
    }

    /** Client to server. The colony id is re-validated on arrival, never trusted. */
    public record Request(int colonyId, Action action) implements CustomPacketPayload {

        public static final Type<Request> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ColonyHandover.MODID, "handover_request"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Request::colonyId,
                Action.CODEC, Request::action,
                Request::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: what the window shows. */
    public record Summary(int colonyId, String colonyName,
                          int buildings, int needWork, int inaccessible,
                          int citizens, int children,
                          int couples, int parentsKnown, int parentsPlaced, int sameGenderPairs)
            implements CustomPacketPayload {

        public static final Type<Summary> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ColonyHandover.MODID, "handover_summary"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Summary> CODEC = StreamCodec.of(
                (buf, s) -> {
                    buf.writeVarInt(s.colonyId());
                    buf.writeUtf(s.colonyName());
                    buf.writeVarInt(s.buildings());
                    buf.writeVarInt(s.needWork());
                    buf.writeVarInt(s.inaccessible());
                    buf.writeVarInt(s.citizens());
                    buf.writeVarInt(s.children());
                    buf.writeVarInt(s.couples());
                    buf.writeVarInt(s.parentsKnown());
                    buf.writeVarInt(s.parentsPlaced());
                    buf.writeVarInt(s.sameGenderPairs());
                },
                buf -> new Summary(buf.readVarInt(), buf.readUtf(),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

        public Summary {
            colonyName = colonyName == null ? "" : colonyName;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private HandoverPayloads() {}
}
