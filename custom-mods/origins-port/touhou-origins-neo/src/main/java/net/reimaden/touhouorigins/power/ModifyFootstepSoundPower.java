package net.reimaden.touhouorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.Optional;

/**
 * Replaces or silences the holder's footstep sound.
 *
 * <p>This is the third of Voile's sound types, and the one that could <i>not</i> fold into
 * {@code apoli:replace_sound_emission}. Two reasons, both established by the earlier sound spike:
 *
 * <ul>
 *   <li>emission replacement never reaches the emitting player — the server broadcasts to everyone
 *       except them, and their own client predicts the sound locally;</li>
 *   <li>{@code replace_sound_reception} does reach them but carries no source entity, so keying on
 *       {@code minecraft:block.*.step} would also rewrite every passing cow's footsteps.</li>
 * </ul>
 *
 * <p>Hooking {@code Entity.playStepSound} instead is source-scoped by construction: it fires on the
 * entity taking the step, so only the holder's own footsteps change, for everyone who can hear them.
 * This is the approach Voile used.
 */
public final class ModifyFootstepSoundPower extends PowerType<ModifyFootstepSoundPower.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("modify_footstep_sound");

    public record Cfg(Optional<ResourceLocation> sound, boolean silent, float volume, float pitch) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("sound").forGetter(Cfg::sound),
            Codec.BOOL.optionalFieldOf("silent", false).forGetter(Cfg::silent),
            Codec.FLOAT.optionalFieldOf("volume", 1.0F).forGetter(Cfg::volume),
            Codec.FLOAT.optionalFieldOf("pitch", 1.0F).forGetter(Cfg::pitch)
        ).apply(i, Cfg::new));
    }

    /**
     * @return true when the vanilla step sound should be suppressed — either because a power asked
     *         for silence, or because one played a replacement in its place.
     */
    public static boolean handleStep(Entity entity) {
        boolean[] handled = {false};
        PowerLookup.forEach(entity, CANONICAL, Cfg.class, cfg -> {
            if (cfg.silent()) {
                handled[0] = true;
                return;
            }
            cfg.sound().ifPresent(id -> {
                SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(id);
                if (sound == null) sound = SoundEvent.createVariableRangeEvent(id);
                // Use the entity's own playSound rather than Level.playSound(null, …).
                // For a player, vanilla's version excludes them from the server broadcast and plays
                // locally on their own client instead. Broadcasting to everyone (except = null) made
                // the holder hear the server's copy a network round-trip late, on top of the vanilla
                // step their client had already played — audible as a doubled, offset footstep.
                entity.playSound(sound, cfg.volume(), cfg.pitch());
                handled[0] = true;
            });
        });
        return handled[0];
    }
}
