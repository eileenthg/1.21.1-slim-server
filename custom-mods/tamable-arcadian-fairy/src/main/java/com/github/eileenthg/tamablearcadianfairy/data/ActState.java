package com.github.eileenthg.tamablearcadianfairy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The per-fairy behaviour settings the owner edits in the fairy screen.
 */
public record ActState(AttackMode attackMode, boolean followOwner, boolean wander,
                       int followRange, int wanderRange, Vec3 wanderCenter) {

    public static final int MIN_FOLLOW_RANGE = 2;
    public static final int MAX_FOLLOW_RANGE = 32;
    public static final int MIN_WANDER_RANGE = 2;
    public static final int MAX_WANDER_RANGE = 32;

    public static final ActState DEFAULT = new ActState(AttackMode.PASSIVE, true, true, 8, 8, Vec3.ZERO);

    private static final Codec<Vec3> VEC3 = Codec.DOUBLE.listOf().comapFlatMap(
            list -> list.size() == 3
                    ? com.mojang.serialization.DataResult.success(new Vec3(list.get(0), list.get(1), list.get(2)))
                    : com.mojang.serialization.DataResult.error(() -> "Expected 3 doubles"),
            vec -> java.util.List.of(vec.x, vec.y, vec.z));

    public static final Codec<ActState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AttackMode.CODEC.optionalFieldOf("attack_mode", AttackMode.PASSIVE).forGetter(ActState::attackMode),
            Codec.BOOL.optionalFieldOf("follow_owner", true).forGetter(ActState::followOwner),
            Codec.BOOL.optionalFieldOf("wander", true).forGetter(ActState::wander),
            Codec.INT.optionalFieldOf("follow_range", 8).forGetter(ActState::followRange),
            Codec.INT.optionalFieldOf("wander_range", 8).forGetter(ActState::wanderRange),
            VEC3.optionalFieldOf("wander_center", Vec3.ZERO).forGetter(ActState::wanderCenter)
    ).apply(instance, ActState::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_STREAM = StreamCodec.of(
            (buf, vec) -> {
                buf.writeDouble(vec.x);
                buf.writeDouble(vec.y);
                buf.writeDouble(vec.z);
            },
            buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> AttackMode.values()[i], AttackMode::ordinal), ActState::attackMode,
            ByteBufCodecs.BOOL, ActState::followOwner,
            ByteBufCodecs.BOOL, ActState::wander,
            ByteBufCodecs.VAR_INT, ActState::followRange,
            ByteBufCodecs.VAR_INT, ActState::wanderRange,
            VEC3_STREAM, ActState::wanderCenter,
            ActState::new);

    /** Clamps anything a client could have sent us into a sane range. */
    public ActState sanitised() {
        return new ActState(attackMode, followOwner, wander,
                Mth.clamp(followRange, MIN_FOLLOW_RANGE, MAX_FOLLOW_RANGE),
                Mth.clamp(wanderRange, MIN_WANDER_RANGE, MAX_WANDER_RANGE),
                wanderCenter);
    }

    public ActState withCenter(Vec3 center) {
        return new ActState(attackMode, followOwner, wander, followRange, wanderRange, center);
    }
}
