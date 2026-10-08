package net.eileenthg.maidchef.net;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.List;
import net.eileenthg.maidchef.ChefConfig;
import net.eileenthg.maidchef.ChefData;
import net.eileenthg.maidchef.ChefPlugin;
import net.eileenthg.maidchef.MaidChef;
import net.eileenthg.maidchef.TaskChef;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Config screen → server: all of one maid's Chef settings. */
public record SetChefSettingsPayload(int entityId, int radius, int target, int recentSize, int minNutrition, float minSaturation,
                                     int minEffects, List<String> blacklist, boolean serveFeasts, String dish) implements CustomPacketPayload {
    public static final Type<SetChefSettingsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MaidChef.MOD_ID, "set_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetChefSettingsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeVarInt(m.entityId);
                buf.writeVarInt(m.radius);
                buf.writeVarInt(m.target);
                buf.writeVarInt(m.recentSize);
                buf.writeVarInt(m.minNutrition);
                buf.writeFloat(m.minSaturation);
                buf.writeVarInt(m.minEffects);
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(ChefData.MAX_BLACKLIST)).encode(buf, m.blacklist);
                buf.writeBoolean(m.serveFeasts);
                buf.writeUtf(m.dish, 256);
            },
            buf -> new SetChefSettingsPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readFloat(), buf.readVarInt(), ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(ChefData.MAX_BLACKLIST)).decode(buf),
                    buf.readBoolean(), buf.readUtf(256)));

    public static SetChefSettingsPayload of(int entityId, ChefData d) {
        return new SetChefSettingsPayload(entityId, d.radius(), d.target(), d.recentSize(), d.minNutrition(), d.minSaturation(), d.minEffects(),
                d.blacklist(), d.serveFeasts(), d.dish());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Keep valid item ids and #tags only, no duplicates. */
    public static List<String> cleanBlacklist(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String raw : in) {
            String e = raw.trim();
            if (e.isEmpty() || out.contains(e) || out.size() >= ChefData.MAX_BLACKLIST) continue;
            if (e.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(e.substring(1));
                if (tag != null && BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag)).isPresent()) out.add(e);
            } else {
                ResourceLocation id = ResourceLocation.tryParse(e);
                if (id != null && BuiltInRegistries.ITEM.containsKey(id)) out.add(id.toString());
            }
        }
        return out;
    }

    /** "" (best food) or a real item id. */
    private static String cleanDish(String dish) {
        String item = net.eileenthg.maidchef.Variant.itemPart(dish), variant = net.eileenthg.maidchef.Variant.variantPart(dish);
        ResourceLocation id = item.isEmpty() ? null : ResourceLocation.tryParse(item);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return "";
        return variant.isEmpty() || variant.length() > 200 ? id.toString() : id + "|" + variant;
    }

    public static void handle(SetChefSettingsPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player().level().getEntity(msg.entityId()) instanceof EntityMaid maid)) return;
            if (!maid.isOwnedBy(ctx.player()) || maid.distanceToSqr(ctx.player()) > 64 * 64) return;
            ChefData next = TaskChef.data(maid).withSettings(
                    Mth.clamp(msg.radius(), 3, ChefConfig.MAX_RADIUS.get()),
                    Mth.clamp(msg.target(), 1, 64),
                    Mth.clamp(msg.recentSize(), 0, ChefData.MAX_RECENT),
                    Mth.clamp(msg.minNutrition(), 0, 20),
                    Mth.clamp(msg.minSaturation(), 0f, 40f),
                    Mth.clamp(msg.minEffects(), 0, 10),
                    cleanBlacklist(msg.blacklist()),
                    msg.serveFeasts(),
                    cleanDish(msg.dish()));
            maid.setAndSyncData(ChefPlugin.CHEF_DATA, next);
        });
    }
}
