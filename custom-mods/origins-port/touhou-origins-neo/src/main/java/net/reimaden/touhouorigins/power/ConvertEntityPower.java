package net.reimaden.touhouorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.Optional;

/**
 * Turns the holder's victims into something else instead of letting them die.
 *
 * <p>Used by {@code jiang_shi/pestilence}: killing a villager infects it into a zombie villager,
 * mirroring what a vanilla zombie does on Hard difficulty.
 *
 * <p>Voile implemented this on Fabric's {@code ServerLivingEntityEvents.AllowDeath}. NeoForge has no
 * direct twin, so the conversion runs from a cancellable {@link
 * net.neoforged.neoforge.event.entity.living.LivingDeathEvent} — see
 * {@code net.reimaden.touhouorigins.behavior.ConvertEntityHandler}.
 */
public final class ConvertEntityPower extends PowerType<ConvertEntityPower.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("convert_entity");

    public record Cfg(Optional<EntityCondition> entityCondition,
                      Optional<BiEntityCondition> biEntityCondition,
                      ResourceLocation convertTo,
                      boolean ignoreDifficulty) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("entity_condition", EntityCondition.CODEC).forGetter(Cfg::entityCondition),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::biEntityCondition),
            IdCodecs.ID.fieldOf("convert_to").forGetter(Cfg::convertTo),
            Codec.BOOL.optionalFieldOf("ignore_difficulty", true).forGetter(Cfg::ignoreDifficulty)
        ).apply(i, Cfg::new));
    }

    /** First applicable config for {@code killer} against {@code victim}, or empty. */
    public static Optional<Cfg> findFor(LivingEntity killer, Entity victim) {
        Cfg[] hit = {null};
        PowerLookup.forEach(killer, CANONICAL, Cfg.class, cfg -> {
            if (hit[0] != null) return;
            if (cfg.entityCondition().isPresent()
                && !cfg.entityCondition().get().test(new EntityCtx(victim, victim.level()))) return;
            if (cfg.biEntityCondition().isPresent()
                && !cfg.biEntityCondition().get().test(BiEntityCtx.of(killer, victim, killer.level()))) return;
            hit[0] = cfg;
        });
        return Optional.ofNullable(hit[0]);
    }
}
