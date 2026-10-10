package net.reimaden.touhouorigins.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.Optional;

/**
 * Item conditions for two things 1.21 moved from NBT into components, and which Apoli has no
 * built-in condition for.
 *
 * <p>Same root cause as {@link NamedItemCondition}: {@code apoli:nbt} reads only
 * {@code minecraft:custom_data}, so upstream's {@code {Potion:"minecraft:water"}} and
 * {@code {Charged:1b}} — both root-NBT paths in 1.20 — silently match nothing here. Every power
 * relying on them has been inert since the port, with no error to show for it.
 */
public final class ComponentItemConditions {

    private ComponentItemConditions() {}

    /**
     * Matches the potion an item holds, via {@code minecraft:potion_contents}.
     *
     * <p>Used by oni's {@code heavy_drinker} (which treats water, mundane, thick and awkward
     * bottles as drinkable) and oarfish's {@code veils_like_water}.
     */
    public static final class PotionCondition implements ConditionType<ItemCtx, PotionCondition.Cfg> {

        public static final ResourceLocation CANONICAL = TouhouOrigins.id("potion");

        public record Cfg(ResourceLocation potion) {}

        @Override
        public MapCodec<Cfg> codec() {
            return RecordCodecBuilder.mapCodec(i -> i.group(
                IdCodecs.ID.fieldOf("potion").forGetter(Cfg::potion)
            ).apply(i, Cfg::new));
        }

        @Override
        public boolean test(Cfg cfg, ItemCtx ctx) {
            PotionContents contents = ctx.stack().get(DataComponents.POTION_CONTENTS);
            if (contents == null) return false;
            Optional<Holder<Potion>> potion = contents.potion();
            return potion.isPresent()
                && cfg.potion().equals(BuiltInRegistries.POTION.getKey(potion.get().value()));
        }
    }

    /**
     * Matches a crossbow with a projectile loaded, via {@code minecraft:charged_projectiles}.
     *
     * <p>Used by moon rabbit's {@code military_of_the_moon}, which reloads for the player. In 1.20
     * this was the boolean {@code Charged}; the component instead holds the list of loaded
     * projectiles, so "charged" is simply "the list is not empty".
     */
    public static final class ChargedCondition implements ConditionType<ItemCtx, ChargedCondition.Cfg> {

        public static final ResourceLocation CANONICAL = TouhouOrigins.id("charged");

        public record Cfg(boolean charged) {}

        @Override
        public MapCodec<Cfg> codec() {
            return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.BOOL.optionalFieldOf("charged", true).forGetter(Cfg::charged)
            ).apply(i, Cfg::new));
        }

        @Override
        public boolean test(Cfg cfg, ItemCtx ctx) {
            ChargedProjectiles projectiles = ctx.stack().get(DataComponents.CHARGED_PROJECTILES);
            boolean loaded = projectiles != null && !projectiles.isEmpty();
            return loaded == cfg.charged();
        }
    }
}
