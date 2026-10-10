package net.reimaden.touhouorigins.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.List;

/**
 * Registers this mod's sound events.
 *
 * <p>Not optional, and not merely cosmetic: Apoli's {@code apoli:play_sound} looks the id up with
 * {@code BuiltInRegistries.SOUND_EVENT.get(...)} and <b>returns silently when it finds nothing</b>.
 * Without these registrations every {@code touhouorigins:} sound in the datapack is a no-op — the
 * powers still fire, they just make no noise, which is indistinguishable in game from a power that
 * never ran.
 *
 * <p>That is exactly what happened: this class was missed during the port, so kappa's camouflage,
 * satori's paranoia sounds and everything else here were dead from Phase 3 onwards. Two of our own
 * Java powers ({@code ModifyFootstepSoundPower}, {@code TeleportToEntityAction}) fall back to
 * {@code SoundEvent.createVariableRangeEvent} when the lookup misses, which is why footsteps kept
 * working and hid the problem.
 *
 * <p>The list is every key in {@code assets/touhouorigins/sounds.json}, which matches upstream's
 * own registration list exactly — verified by diffing the two sets rather than by eye. Each entry
 * needs a registry object; the client resolves the actual audio through {@code sounds.json}, where
 * several of these are aliases for vanilla samples rather than shipped {@code .ogg} files.
 */
public final class ModSoundEvents {

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, TouhouOrigins.MOD_ID);

    /** Touhou Origins Blessings ships its own namespace, and its sounds need registering too. */
    public static final String BLESSINGS_ID = "touhoublessings";

    public static final DeferredRegister<SoundEvent> BLESSING_SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, BLESSINGS_ID);

    private static final List<String> BLESSING_NAMES = List.of(
        "blessing.miracle.trigger"
    );

    private static final List<String> NAMES = List.of(
        "entity.generic.flip",
        "entity.generic.gust",
        "entity.generic.reverse",
        "entity.generic.shikigami_link",
        "origin.amanojaku.flip",
        "origin.bake_danuki.disguise",
        "origin.bake_danuki.undisguise",
        "origin.celestial.enlightenment",
        "origin.celestial.temper",
        "origin.fairy.fly",
        "origin.fairy.heal",
        "origin.ghost.death",
        "origin.ghost.hurt",
        "origin.haniwa.break",
        "origin.haniwa.resist",
        "origin.kappa.camouflage_activate",
        "origin.kappa.camouflage_deactivate",
        "origin.kappa.dry",
        "origin.moon_rabbit.activate",
        "origin.moon_rabbit.deactivate",
        "origin.nue.transform",
        "origin.oarfish.dry",
        "origin.oarfish.prepare_lightning",
        "origin.oni.step",
        "origin.rabbit.hop",
        "origin.satori.close_eye",
        "origin.satori.recollect",
        "origin.shikigami.channel",
        "origin.shikigami.locate_fail",
        "origin.shikigami.teleport",
        "origin.shikigami.teleport_fail",
        "origin.vengeful_spirit.death",
        "origin.vengeful_spirit.harvest",
        "origin.vengeful_spirit.heal",
        "origin.vengeful_spirit.hurt",
        "origin.wolf_tengu.growl",
        "origin.wolf_tengu.parry"
    );

    private ModSoundEvents() {}

    public static void register(IEventBus modBus) {
        for (String name : NAMES) {
            ResourceLocation id = TouhouOrigins.id(name);
            SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
        }
        SOUNDS.register(modBus);

        for (String name : BLESSING_NAMES) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BLESSINGS_ID, name);
            BLESSING_SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
        }
        BLESSING_SOUNDS.register(modBus);
    }
}
