package net.eileenthg.maidchef.net;

import java.util.ArrayList;
import java.util.List;
import net.eileenthg.maidchef.ChefRequests;
import net.eileenthg.maidchef.MaidChef;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Requests tab: the player's shared list of ingredients maids asked for, and blocking/unblocking/forgetting them. */
public final class RequestPayloads {
    /** Latest list received by this client, and a counter so the screen knows to rebuild its buttons. */
    public static volatile List<Row> SYNCED = List.of();
    public static volatile int VERSION;

    private RequestPayloads() {
    }

    public record Row(String item, boolean blocked, String forDish, int times) {
        static final StreamCodec<RegistryFriendlyByteBuf, Row> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Row::item,
                ByteBufCodecs.BOOL, Row::blocked,
                ByteBufCodecs.STRING_UTF8, Row::forDish,
                ByteBufCodecs.VAR_INT, Row::times,
                Row::new);
    }

    public static void sendTo(ServerPlayer player) {
        List<Row> rows = new ArrayList<>();
        ChefRequests.get(player.getServer()).list(player.getUUID())
                .forEach((id, e) -> rows.add(new Row(id, e.blocked(), e.forDish(), e.times())));
        PacketDistributor.sendToPlayer(player, new Sync(rows.size() > 256 ? rows.subList(0, 256) : rows));
    }

    /** Client asks for its list. */
    public record Fetch() implements CustomPacketPayload {
        public static final Type<Fetch> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MaidChef.MOD_ID, "requests_fetch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Fetch> STREAM_CODEC = StreamCodec.unit(new Fetch());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Fetch msg, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer p) sendTo(p);
            });
        }
    }

    /** Client blocks (0), unblocks (1) or forgets (2) one item. */
    public record Change(String item, int action) implements CustomPacketPayload {
        public static final Type<Change> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MaidChef.MOD_ID, "requests_change"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Change> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Change::item, ByteBufCodecs.VAR_INT, Change::action, Change::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Change msg, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer p)) return;
                ChefRequests r = ChefRequests.get(p.getServer());
                switch (msg.action()) {
                    case 0 -> r.setBlocked(p.getUUID(), msg.item(), true);
                    case 1 -> r.setBlocked(p.getUUID(), msg.item(), false);
                    case 2 -> r.forget(p.getUUID(), msg.item());
                    default -> {
                        return;
                    }
                }
                sendTo(p);
            });
        }
    }

    /** Server sends the list. */
    public record Sync(List<Row> rows) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MaidChef.MOD_ID, "requests_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(
                Row.CODEC.apply(ByteBufCodecs.list(256)), Sync::rows, Sync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Sync msg, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                SYNCED = List.copyOf(msg.rows());
                VERSION++;
            });
        }
    }
}
