package net.reimaden.touhouorigins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.reimaden.touhouorigins.TouhouOrigins;

/**
 * Writes the level of an enchantment the entity is wearing into a resource, so data can then do
 * arithmetic with it.
 *
 * <p>Exists because {@code apoli:enchantment} is a <i>comparison</i> condition — it answers
 * "is the level at least N", never "what is the level". Getting a number out of it in data alone
 * would mean a ladder of conditioned writes, one rung per level.
 *
 * <p>Used by {@code vampire/banes_of_the_immortal}: Water Protection's level feeds the
 * {@code onset_delay} expression that decides how long a vampire can stand in the rain before it
 * starts to burn. {@code apoli:damage_over_time} used to scale that delay by an enchantment itself,
 * but Overgrown's Apoli strips those fields, so the power now does the arithmetic explicitly.
 *
 * <p>{@code calculation} mirrors {@code apoli:enchantment}'s own option: {@code max} takes the best
 * single piece, {@code sum} adds every slot the way vanilla Protection stacks.
 */
public final class StoreEnchantmentLevelAction
    implements ActionType<EntityCtx, StoreEnchantmentLevelAction.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("store_enchantment_level");

    public enum Calculation implements StringRepresentable {
        SUM("sum"), MAX("max");

        private final String name;

        Calculation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static final Codec<Calculation> CODEC = StringRepresentable.fromEnum(Calculation::values);
    }

    public record Cfg(ResourceLocation enchantment, ResourceLocation resource, Calculation calculation) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("enchantment").forGetter(Cfg::enchantment),
            IdCodecs.ID.fieldOf("resource").forGetter(Cfg::resource),
            Calculation.CODEC.optionalFieldOf("calculation", Calculation.MAX).forGetter(Cfg::calculation)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity living = ctx.living();
        if (living == null) return;

        PowerContainer container = PowerContainer.of(living);
        if (container == null) return;

        Holder<Enchantment> holder = ctx.level().registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolder(ResourceKey.create(Registries.ENCHANTMENT, cfg.enchantment()))
            .orElse(null);
        // Absent enchantment writes 0 rather than bailing, so removing the enchantment (or the
        // datapack defining it) cleanly returns the resource to zero instead of freezing it.
        int value = holder == null ? 0 : levelOf(holder, living, cfg.calculation());

        PowerResources.write(container, cfg.resource(), value);
    }

    private static int levelOf(Holder<Enchantment> holder, LivingEntity living, Calculation calculation) {
        int sum = 0;
        int max = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(holder, living.getItemBySlot(slot));
            sum += level;
            if (level > max) max = level;
        }
        return calculation == Calculation.SUM ? sum : max;
    }
}
