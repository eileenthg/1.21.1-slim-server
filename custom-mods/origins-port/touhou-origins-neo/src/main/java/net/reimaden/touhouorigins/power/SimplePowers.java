package net.reimaden.touhouorigins.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.reimaden.touhouorigins.TouhouOrigins;

/**
 * The config-free Voile preventers. Each is a bare flag: a mixin asks
 * {@code PowerLookup.hasActive(entity, ID)} and changes one branch.
 *
 * <p>Voile declared these as separate classes with empty {@code SerializableData}. Grouping them
 * here keeps five near-identical files from existing; the registered ids and behaviour are
 * unchanged, so the data needs only a namespace rename.
 */
public final class SimplePowers {

    private SimplePowers() {}

    /** Shared empty config — Apoli's own convention for a power with no fields. */
    public record Flag() {
        public static final Flag INSTANCE = new Flag();
    }

    /** A {@link PowerType} carrying no configuration at all. */
    public static final class FlagPower extends PowerType<Flag> {
        @Override
        public MapCodec<Flag> configCodec() {
            return MapCodec.unit(Flag.INSTANCE);
        }
    }

    /** Suppresses the dust cloud produced while sprinting. */
    public static final ResourceLocation PREVENT_SPRINTING_PARTICLES =
        TouhouOrigins.id("prevent_sprinting_particles");

    /** Attacks from this entity break shields, as an axe does. */
    public static final ResourceLocation DISABLE_SHIELDS =
        TouhouOrigins.id("disable_shields");

    /**
     * Exempts the holder from the server's "flying is not enabled" kick, so flight-granting powers
     * do not disconnect the player.
     */
    public static final ResourceLocation PREVENT_FLYING_KICK =
        TouhouOrigins.id("prevent_flying_kick");

    /** Healing hurts and harming heals, as for the undead. */
    public static final ResourceLocation REVERSE_INSTANT_EFFECTS =
        TouhouOrigins.id("reverse_instant_effects");

    /** Arms held straight out in front, the jiang-shi pose. Third-person model only. */
    public static final ResourceLocation ZOMBIE_ARMS =
        TouhouOrigins.id("zombie_arms");
}
