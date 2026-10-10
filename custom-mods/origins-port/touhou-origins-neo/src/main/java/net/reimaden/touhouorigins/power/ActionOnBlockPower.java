package net.reimaden.touhouorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.Optional;

/**
 * Runs an action when the holder blocks an attack with a shield.
 *
 * <p>Used by {@code wolf_tengu/steadfast_defense} — blocking is the tengu's counter-attack.
 *
 * <p>Despite the name this has nothing to do with world blocks: "block" here is the verb. Voile hooks
 * the moment vanilla decides a shield absorbed the hit, which in 1.21 is the
 * {@code hurtCurrentlyUsedShield} call inside {@code LivingEntity.hurt}.
 *
 * <p>The attacker passed to the action is the projectile's <i>owner</i> where one exists, so blocking
 * an arrow targets the archer rather than the arrow.
 */
public final class ActionOnBlockPower extends PowerType<ActionOnBlockPower.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("action_on_block");

    public record Cfg(BiEntityAction biEntityAction,
                      Optional<DamageCondition> damageCondition,
                      Optional<BiEntityCondition> biEntityCondition,
                      int cooldown) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Cfg::biEntityAction),
            LoggedOptionalField.strict("damage_condition", DamageCondition.CODEC).forGetter(Cfg::damageCondition),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::biEntityCondition),
            Codec.INT.optionalFieldOf("cooldown", 1).forGetter(Cfg::cooldown)
        ).apply(i, Cfg::new));
    }

    /** Remaining cooldown ticks, per (entity, power). Mirrors Apoli's own cooldown powers. */
    private static final java.util.Map<CooldownKey, Integer> COOLDOWNS = new java.util.concurrent.ConcurrentHashMap<>();

    private record CooldownKey(java.util.UUID entity, ResourceLocation power) {}

    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        CooldownKey key = new CooldownKey(holder.rawOwner().getUUID(), powerId);
        Integer remaining = COOLDOWNS.get(key);
        if (remaining == null) return;
        if (remaining <= 1) COOLDOWNS.remove(key);
        else COOLDOWNS.put(key, remaining - 1);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        if (!holder.allPowers().contains(powerId)) {
            COOLDOWNS.remove(new CooldownKey(holder.rawOwner().getUUID(), powerId));
        }
    }

    /** Invoked from {@code LivingEntityShieldMixin} when a shield absorbs a hit. */
    public static void onBlock(LivingEntity defender, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Projectile projectile && projectile.getOwner() != null) {
            attacker = projectile.getOwner();
        }
        if (attacker == null) return;

        final Entity resolved = attacker;
        // forEachEntry rather than forEach: the cooldown is tracked per power id.
        PowerLookup.forEachEntry(defender, CANONICAL, Cfg.class, (powerId, cfg) -> {
            CooldownKey key = new CooldownKey(defender.getUUID(), powerId);
            if (COOLDOWNS.containsKey(key)) return;      // still recovering

            if (cfg.damageCondition().isPresent()
                && !cfg.damageCondition().get().test(new DamageCtx(source, defender, defender.level(), amount))) return;

            BiEntityCtx ctx = BiEntityCtx.of(defender, resolved, defender.level());
            if (cfg.biEntityCondition().isPresent() && !cfg.biEntityCondition().get().test(ctx)) return;

            cfg.biEntityAction().run(ctx);
            if (cfg.cooldown() > 0) COOLDOWNS.put(key, cfg.cooldown());
        });
    }
}
