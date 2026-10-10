package eteil.mcarpgbridge;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;

public final class BridgeNetwork {
    private BridgeNetwork() {
    }

    /** Client asks the server to open the Mebahel quest or rumor of the MCA villager whose menu is open. */
    public record OpenMebahelPayload(UUID villager) implements CustomPacketPayload {
        public static final Type<OpenMebahelPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(McaRpgBridge.MOD_ID, "open_mebahel"));
        public static final StreamCodec<ByteBuf, OpenMebahelPayload> CODEC =
                UUIDUtil.STREAM_CODEC.map(OpenMebahelPayload::new, OpenMebahelPayload::villager);

        @Override
        public Type<OpenMebahelPayload> type() {
            return TYPE;
        }
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(OpenMebahelPayload.TYPE, OpenMebahelPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && ModList.get().isLoaded("mebahelrpgrquest")) {
                        MebahelInteraction.openFromMcaMenu(player, payload.villager());
                    }
                }));
    }
}
