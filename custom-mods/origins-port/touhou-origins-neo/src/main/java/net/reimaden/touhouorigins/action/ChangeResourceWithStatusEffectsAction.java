package net.reimaden.touhouorigins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.OptionalInt;

/**
 * Sets or adjusts a resource based on how many status effects of a given category the entity has.
 *
 * <p>Used by {@code oni/drunken_stupor}: every 10 ticks it counts the oni's <i>harmful</i> effects,
 * multiplies by 3, and writes that to a resource which feeds an attack-damage bonus. The more
 * debuffed an oni is, the harder it hits — fighting better while impaired, which is the joke.
 *
 * <p>Needs Java for the same reason as the recollection name lookup: Apoli's expression vocabulary
 * exposes health, food, position and resources, but nothing that counts active effects by category,
 * so there is no data-only way to ask this question.
 *
 * <p>Voile computed its total as {@code effects.stream().mapToInt(e -> change).sum()} — a roundabout
 * way of writing {@code count * change}, reproduced here as the multiplication it actually is.
 */
public final class ChangeResourceWithStatusEffectsAction
    implements ActionType<EntityCtx, ChangeResourceWithStatusEffectsAction.Cfg> {

    public static final ResourceLocation CANONICAL =
        TouhouOrigins.id("change_resource_with_status_effects");

    public enum Category implements StringRepresentable {
        BENEFICIAL("beneficial", MobEffectCategory.BENEFICIAL),
        HARMFUL("harmful", MobEffectCategory.HARMFUL),
        NEUTRAL("neutral", MobEffectCategory.NEUTRAL);

        private final String name;
        private final MobEffectCategory vanilla;

        Category(String name, MobEffectCategory vanilla) {
            this.name = name;
            this.vanilla = vanilla;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);
    }

    public enum Operation implements StringRepresentable {
        ADD("add"), SET("set");

        private final String name;

        Operation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);
    }

    public record Cfg(ResourceLocation resource, Category category, int change, Operation operation) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("resource").forGetter(Cfg::resource),
            Category.CODEC.fieldOf("category").forGetter(Cfg::category),
            Codec.INT.optionalFieldOf("change", 1).forGetter(Cfg::change),
            Operation.CODEC.optionalFieldOf("operation", Operation.ADD).forGetter(Cfg::operation)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof LivingEntity living)) return;

        PowerContainer container = PowerContainer.of(living);
        if (container == null) return;

        int matching = 0;
        for (MobEffectInstance effect : living.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == cfg.category().vanilla) matching++;
        }
        int total = matching * cfg.change();

        if (cfg.operation() == Operation.SET) {
            // The resource clamps to its own declared min/max, so no bounding needed here.
            PowerResources.write(container, cfg.resource(), total);
            return;
        }

        OptionalInt current = PowerResources.read(container, cfg.resource());
        if (current.isEmpty()) return;
        PowerResources.write(container, cfg.resource(), current.getAsInt() + total);
    }
}
