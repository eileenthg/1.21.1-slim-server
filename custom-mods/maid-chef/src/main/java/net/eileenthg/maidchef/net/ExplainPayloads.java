package net.eileenthg.maidchef.net;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.eileenthg.maidchef.MaidChef;
import net.eileenthg.maidchef.Planner;
import net.eileenthg.maidchef.TaskChef;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Status tab asks the server why the maid is (or isn't) cooking; the server answers with the explain lines. */
public final class ExplainPayloads {
    /** Latest answer per maid entity id, read by the Status tab (client side). */
    public static final Map<Integer, List<String>> RECEIVED = new ConcurrentHashMap<>();

    private ExplainPayloads() {
    }

    public record Request(int entityId) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MaidChef.MOD_ID, "explain_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Request::entityId, Request::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Request msg, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
                if (!(level.getEntity(msg.entityId()) instanceof EntityMaid maid)) return;
                if (!maid.isOwnedBy(player) || maid.distanceToSqr(player) > 64 * 64 || maid.getTask() != TaskChef.INSTANCE) return;
                List<String> lines = Planner.explain(level, maid, TaskChef.data(maid), 6);
                PacketDistributor.sendToPlayer(player, new Lines(msg.entityId(), lines.size() > 120 ? lines.subList(0, 120) : lines));
            });
        }
    }

    public record Lines(int entityId, List<String> lines) implements CustomPacketPayload {
        public static final Type<Lines> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MaidChef.MOD_ID, "explain_lines"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Lines> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Lines::entityId,
                ByteBufCodecs.stringUtf8(1024).apply(ByteBufCodecs.list(128)), Lines::lines,
                Lines::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Lines msg, IPayloadContext ctx) {
            ctx.enqueueWork(() -> RECEIVED.put(msg.entityId(), List.copyOf(msg.lines())));
        }
    }
}
