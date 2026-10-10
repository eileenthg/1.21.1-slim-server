package net.reimaden.touhouorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.reimaden.touhouorigins.TouhouOrigins;

/**
 * Renders the holder upside-down, and optionally flips their own view too.
 *
 * <p>Two users: {@code amanojaku/this_side_down} (model only) and {@code common/flip_entity}, which
 * {@code amanojaku/turnabout} grants to a <i>victim</i> along with inverted Y velocity and
 * {@code prevent_flying_kick} — the contrarian spirit turning someone upside-down for ten seconds.
 * That is the reason {@code prevent_flying_kick} exists at all: without it the server boots the
 * victim for flying.
 *
 * <p>The model flip rides on vanilla's own Dinnerbone/Grumm easter egg
 * ({@code LivingEntityRenderer.isEntityUpsideDown}) rather than touching the renderer's transforms.
 */
public final class FlipModelPower extends PowerType<FlipModelPower.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("flip_model");

    public record Cfg(boolean flipView) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("flip_view", false).forGetter(Cfg::flipView)
        ).apply(i, Cfg::new));
    }

    /** True when this entity should render inverted. */
    public static boolean shouldFlip(Entity entity) {
        return PowerLookup.hasActive(entity, CANONICAL);
    }

    /** True when some active power also asks for the holder's own camera to be inverted. */
    public static boolean shouldFlipView(Entity entity) {
        boolean[] hit = {false};
        PowerLookup.forEach(entity, CANONICAL, Cfg.class, cfg -> {
            if (cfg.flipView()) hit[0] = true;
        });
        return hit[0];
    }

    /**
     * Vanilla already renders anything named Dinnerbone or Grumm upside-down. Flipping such an entity
     * again would cancel out and leave it upright, so those names are excluded — Voile's own
     * handling, preserved.
     */
    public static boolean isVanillaFlipped(Entity entity) {
        String name = ChatFormatting.stripFormatting(entity.getName().getString());
        return "Dinnerbone".equals(name) || "Grumm".equals(name);
    }
}
