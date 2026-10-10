package net.reimaden.touhouorigins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.Optional;

/**
 * Animals refuse to be tamed by the holder. Used by {@code amanojaku/antagonist} — the contrarian
 * spirit that nothing warms to.
 *
 * <p>Vanilla taming is a dice roll: the interaction picks {@code random.nextInt(bound)} and succeeds
 * only on zero. Rather than intercept each animal's taming logic, the mixins wrap that roll and force
 * a non-zero result, so the attempt fails through vanilla's own path — feeding the item, particles
 * and all. Voile's approach, and the reason this needs one small mixin per animal rather than one
 * central hook.
 */
public final class PreventTamingPower extends PowerType<PreventTamingPower.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("prevent_taming");

    public record Cfg(Optional<BiEntityAction> biEntityAction,
                      Optional<BiEntityCondition> biEntityCondition) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("bientity_action", BiEntityAction.CODEC).forGetter(Cfg::biEntityAction),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::biEntityCondition)
        ).apply(i, Cfg::new));
    }

    /**
     * Wraps a taming roll. Returns a guaranteed-failing value when an active power applies to
     * {@code tameable}; otherwise passes the original through untouched.
     *
     * @param original the value vanilla's {@code nextInt} produced
     */
    public static int preventTaming(int original, Player player, Entity tameable) {
        if (original != 0) return original;      // already failing — nothing to do
        return applies(player, tameable) ? original + 1 : original;
    }

    /**
     * Horses use a temper threshold rather than a zero check: the goal succeeds once the roll is
     * below the current temper. Returning temper guarantees the comparison fails.
     */
    public static int preventTamingHorse(int original, Player player, Entity horse, int temper) {
        if (original >= temper) return original;
        return applies(player, horse) ? original + temper : original;
    }

    /** True when some active power on {@code player} refuses taming of {@code tameable}. */
    public static boolean applies(Player player, Entity tameable) {
        if (player == null || tameable == null) return false;

        boolean[] hit = {false};
        PowerLookup.forEach(player, CANONICAL, Cfg.class, cfg -> {
            if (hit[0]) return;
            BiEntityCtx ctx = BiEntityCtx.of(player, tameable, player.level());
            if (cfg.biEntityCondition().isPresent() && !cfg.biEntityCondition().get().test(ctx)) return;
            hit[0] = true;
            cfg.biEntityAction().ifPresent(action -> action.run(ctx));
        });
        return hit[0];
    }
}
