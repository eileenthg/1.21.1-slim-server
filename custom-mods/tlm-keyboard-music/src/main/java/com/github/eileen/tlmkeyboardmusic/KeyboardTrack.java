package com.github.eileen.tlmkeyboardmusic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

/**
 * One piece of music the keyboard can play, loaded from a datapack.
 * <p>
 * {@code sound} uses {@link SoundEvent#CODEC}, so an entry may name a sound event some mod registered
 * or define one inline, the same way the vanilla {@code jukebox_song} registry does:
 *
 * <pre>{@code { "sound": { "sound_id": "mypack:my_song", "range": 24 }, "length_seconds": 120 } }</pre>
 *
 * A datapack cannot add audio by itself; the {@code .ogg} and its {@code sounds.json} entry still come
 * from a resource pack.
 * <p>
 * {@code length_seconds} is required because the server never reads the audio file. It decides when
 * the piece is over, so a seated maid can move on to the next one and a player can start another.
 * <p>
 * {@code models} is the easter-egg hook. Empty means anyone may play the piece; otherwise only a maid
 * wearing one of the listed model ids can, and {@code weight} is what gives it a better chance than the
 * general pieces.
 */
public record KeyboardTrack(Holder<SoundEvent> sound, int lengthSeconds, int weight, float volume, List<String> models) {
    public static final int DEFAULT_WEIGHT = 10;

    public static final Codec<KeyboardTrack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SoundEvent.CODEC.fieldOf("sound").forGetter(KeyboardTrack::sound),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("length_seconds").forGetter(KeyboardTrack::lengthSeconds),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("weight", DEFAULT_WEIGHT).forGetter(KeyboardTrack::weight),
            Codec.floatRange(0.0F, 4.0F).optionalFieldOf("volume", 1.0F).forGetter(KeyboardTrack::volume),
            Codec.STRING.listOf().optionalFieldOf("models", List.of()).forGetter(KeyboardTrack::models)
    ).apply(instance, KeyboardTrack::new));

    public boolean isGeneral() {
        return this.models.isEmpty();
    }

    /**
     * @param modelId the sitting maid's model id, or null when a player is playing
     */
    public boolean canBePlayedBy(String modelId) {
        return this.isGeneral() || (modelId != null && this.models.contains(modelId));
    }

    public String soundId() {
        return this.sound.unwrapKey().map(key -> key.location().toString())
                .orElseGet(() -> this.sound.value().getLocation().toString());
    }
}
