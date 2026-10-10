package net.reimaden.touhouorigins.shikigami;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.reimaden.touhouorigins.TouhouOrigins;

/**
 * True when the actor is credited with the target's death.
 *
 * <p>Exists to tell a shikigami who failed their master from one who killed them. Reads
 * {@link LivingEntity#getKillCredit()} rather than the damage source, because the damage source is
 * not carried in a {@code BiEntityCtx} — and kill credit is the same field the vanilla death
 * message itself is built from, so the two can never disagree.
 *
 * <p>Only meaningful while the target is dying or dead; anywhere else the credit is stale or absent.
 */
public final class KilledTargetCondition implements ConditionType<BiEntityCtx, Object> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("killed_target");

    @Override
    public MapCodec<Object> codec() {
        return MapCodec.unit(new Object());
    }

    @Override
    public boolean test(Object cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        if (actor == null || !(ctx.target() instanceof LivingEntity target)) return false;
        return target.getKillCredit() == actor;
    }
}
