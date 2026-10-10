package com.github.eileenthg.tamablearcadianfairy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * Who owns a fairy, if anyone. Stored as an attachment on the fairy itself.
 */
public record TameData(boolean tamed, Optional<UUID> owner, String ownerName) {

    public static final TameData EMPTY = new TameData(false, Optional.empty(), "");

    public static final Codec<TameData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("tamed").forGetter(TameData::tamed),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(TameData::owner),
            Codec.STRING.optionalFieldOf("owner_name", "").forGetter(TameData::ownerName)
    ).apply(instance, TameData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TameData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TameData::tamed,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), TameData::owner,
            ByteBufCodecs.STRING_UTF8, TameData::ownerName,
            TameData::new);

    public static TameData of(UUID owner, String ownerName) {
        return new TameData(true, Optional.of(owner), ownerName);
    }

    public boolean isOwnedBy(UUID candidate) {
        return tamed && owner.isPresent() && owner.get().equals(candidate);
    }
}
