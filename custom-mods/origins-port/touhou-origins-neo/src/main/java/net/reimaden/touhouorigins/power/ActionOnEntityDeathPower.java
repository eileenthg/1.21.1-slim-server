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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.Optional;

/**
 * Runs an action when <i>some other</i> entity dies — not when the holder does.
 *
 * <p>That distinction is the whole point, and it is why Apoli's {@code apoli:action_on_death} is
 * not a substitute. Apoli's fires for the holder's own death, with the killer as actor and the
 * holder as target. Voile's fires for <i>any</i> death anywhere, then looks for holders whose
 * bientity condition accepts the deceased, with the holder as actor and the dead entity as target.
 *
 * <p>{@code global/linked_by_the_soul} needs the second shape: a shikigami dies when their
 * <i>master</i> does. Ported onto {@code apoli:action_on_death} it had become the reverse — nothing
 * happened when the master died, and killing your own servant dealt you a million damage, because
 * the killer was being handed to the actor action.
 *
 * <p>Fired from {@link net.reimaden.touhouorigins.event.ShikigamiDeathHandler}.
 */
public final class ActionOnEntityDeathPower extends PowerType<ActionOnEntityDeathPower.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("action_on_entity_death");

    public record Cfg(BiEntityAction bientityAction, Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Cfg::bientityAction),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC)
                .forGetter(Cfg::bientityCondition)
        ).apply(i, Cfg::new));
    }

    /**
     * @param holder   a candidate reactor, e.g. the shikigami
     * @param deceased the entity that just died, e.g. their master
     */
    public static void fire(LivingEntity holder, LivingEntity deceased, ServerLevel level) {
        if (holder == deceased) return;
        PowerLookup.forEach(holder, CANONICAL, Cfg.class, cfg -> {
            BiEntityCtx ctx = new BiEntityCtx(holder, deceased, level);
            if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(ctx)) return;
            cfg.bientityAction().run(ctx);
        });
    }
}
