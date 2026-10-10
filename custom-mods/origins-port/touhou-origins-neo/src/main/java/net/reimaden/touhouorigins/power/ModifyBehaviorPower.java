package net.reimaden.touhouorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.Optional;

/**
 * Replacement for {@code voile:modify_behavior} — changes how mobs treat the holder.
 *
 * <p>Config matches Voile's exactly ({@code behavior}, {@code entity_condition},
 * {@code bientity_condition}), so the data needs only a type rename.
 *
 * <p><b>Scope note.</b> Voile implemented this across 31 mixins and ~1800 lines, one per awkward
 * mob (bees, foxes, wardens, piglin brains, the ender dragon…). This port covers the goal-based
 * path first, which is what Touhou's five usages mostly need; see NOTES.md for what is deliberately
 * not covered yet.
 */
public final class ModifyBehaviorPower extends PowerType<ModifyBehaviorPower.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("modify_behavior");

    public enum Behavior implements StringRepresentable {
        HOSTILE("hostile"),
        NEUTRAL("neutral"),
        PASSIVE("passive");

        private final String name;

        Behavior(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static final Codec<Behavior> CODEC = StringRepresentable.fromEnum(Behavior::values);
    }

    public record Cfg(Behavior behavior,
                      Optional<EntityCondition> entityCondition,
                      Optional<BiEntityCondition> biEntityCondition) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Behavior.CODEC.fieldOf("behavior").forGetter(Cfg::behavior),
            LoggedOptionalField.strict("entity_condition", EntityCondition.CODEC).forGetter(Cfg::entityCondition),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::biEntityCondition)
        ).apply(i, Cfg::new));
    }
}
