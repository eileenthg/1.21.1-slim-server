package net.reimaden.touhouorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.Optional;

/**
 * Keeps normal movement speed while using an item — see
 * {@code net.reimaden.touhouorigins.mixin.client.LocalPlayerItemSlowdownMixin} for where it applies.
 *
 * <p>Voile's schema kept verbatim: {@code item_condition} narrows which items are exempt (Touhou uses
 * it for shields only), and {@code can_start_sprinting} controls whether sprinting may also begin,
 * which vanilla otherwise forbids while an item is in use.
 */
public final class PreventItemSlowdownPower extends PowerType<PreventItemSlowdownPower.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("prevent_item_slowdown");

    public record Cfg(Optional<ItemCondition> itemCondition, boolean canStartSprinting) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Cfg::itemCondition),
            Codec.BOOL.optionalFieldOf("can_start_sprinting", true).forGetter(Cfg::canStartSprinting)
        ).apply(i, Cfg::new));
    }

    /** True when some active power exempts the item the entity is currently using. */
    public static boolean applies(LivingEntity entity) {
        return matches(entity, false);
    }

    /** As {@link #applies}, but also requires the power to permit starting a sprint. */
    public static boolean canStartSprinting(LivingEntity entity) {
        return matches(entity, true);
    }

    private static boolean matches(LivingEntity entity, boolean requireSprint) {
        if (!entity.isUsingItem()) return false;
        ItemStack using = entity.getUseItem();

        boolean[] hit = {false};
        PowerLookup.forEach(entity, CANONICAL, Cfg.class, cfg -> {
            if (hit[0]) return;
            if (requireSprint && !cfg.canStartSprinting()) return;
            if (cfg.itemCondition().isPresent()
                && !cfg.itemCondition().get().test(new ItemCtx(using, entity.level(), entity))) return;
            hit[0] = true;
        });
        return hit[0];
    }
}
