package net.reimaden.touhouorigins.behavior;

import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.Entity;
import net.reimaden.touhouorigins.power.ModifyBehaviorPower;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves what behaviour a given {@code holder} has asked for towards a given {@code mob}.
 *
 * <p>Voile's equivalent ({@code BehaviorHelper}) built a mutable list per call and filtered it in
 * place. This is a plain query instead: it returns the first matching behaviour, or {@code null} if
 * no power applies. Callers are mixins on hot AI paths, so it allocates nothing in the common case
 * of "this entity has no such power".
 */
public final class BehaviorLookup {

    private BehaviorLookup() {}

    /**
     * @param holder the entity that may hold {@code modify_behavior} (typically the player)
     * @param mob    the mob whose behaviour is being decided
     * @return the requested behaviour, or {@code null} if no applicable power is active
     */
    public static @Nullable ModifyBehaviorPower.Behavior behaviorFor(@Nullable Entity holder, @Nullable Entity mob) {
        if (holder == null || mob == null) return null;

        // PowerLookup.forEach already skips suppressed powers and ones whose own condition fails.
        var result = new ModifyBehaviorPower.Behavior[1];
        PowerLookup.forEach(holder, ModifyBehaviorPower.CANONICAL, ModifyBehaviorPower.Cfg.class, cfg -> {
            if (result[0] != null) return;   // first match wins, as in Voile
            if (!applies(cfg, holder, mob)) return;
            result[0] = cfg.behavior();
        });
        return result[0];
    }

    /** True when this power's own entity/bientity conditions accept {@code mob}. */
    private static boolean applies(ModifyBehaviorPower.Cfg cfg, Entity holder, Entity mob) {
        if (cfg.entityCondition().isPresent()) {
            if (!cfg.entityCondition().get().test(new EntityCtx(mob, mob.level()))) return false;
        }
        if (cfg.biEntityCondition().isPresent()) {
            if (!cfg.biEntityCondition().get().test(BiEntityCtx.of(holder, mob, holder.level()))) return false;
        }
        return true;
    }

    /**
     * True when {@code holder} has asked {@code mob} not to attack it — i.e. neutral or passive.
     * This is the question the targeting mixins ask.
     */
    public static boolean refusesToTarget(@Nullable Entity holder, @Nullable Entity mob) {
        ModifyBehaviorPower.Behavior behavior = behaviorFor(holder, mob);
        return behavior == ModifyBehaviorPower.Behavior.NEUTRAL
            || behavior == ModifyBehaviorPower.Behavior.PASSIVE;
    }
}
