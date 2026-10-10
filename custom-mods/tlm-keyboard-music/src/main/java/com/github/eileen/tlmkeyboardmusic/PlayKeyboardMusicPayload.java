package com.github.eileen.tlmkeyboardmusic;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Tells clients to start a track on a keyboard seat. Stopping needs no packet: the client stops by
 * itself when the seat is empty or gone.
 */
public record PlayKeyboardMusicPayload(int sitEntityId, Holder<SoundEvent> sound, float volume) implements CustomPacketPayload {
    public static final Type<PlayKeyboardMusicPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TlmKeyboardMusic.MOD_ID, "play"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayKeyboardMusicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayKeyboardMusicPayload::sitEntityId,
            SoundEvent.STREAM_CODEC, PlayKeyboardMusicPayload::sound,
            ByteBufCodecs.FLOAT, PlayKeyboardMusicPayload::volume,
            PlayKeyboardMusicPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
