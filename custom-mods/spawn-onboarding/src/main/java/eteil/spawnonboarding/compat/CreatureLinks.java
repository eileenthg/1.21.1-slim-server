package eteil.spawnonboarding.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.lang.reflect.Method;

/**
 * Alex's Mobs' tarantula hawk only hunts entities that are vanilla spiders in code ({@code Spider.class}), so
 * Eternal Tales' spiders never get picked. This gives every hawk a second hunting goal for
 * the entity tag {@code spawnonboarding:tarantula_hawk_prey} (filled by the creature-links datapack), with the
 * same conditions as the original: adults only, not while sitting. Its paralysing sting keys off the
 * {@code minecraft:arthropod} tag, which the same datapack extends.
 */
public final class CreatureLinks {
    private static final ResourceLocation HAWK = ResourceLocation.fromNamespaceAndPath("alexsmobs", "tarantula_hawk");
    private static final TagKey<EntityType<?>> HAWK_PREY =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("spawnonboarding", "tarantula_hawk_prey"));
    private static Method isSitting;
    private static boolean lookedUp;

    private CreatureLinks() {
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof PathfinderMob hawk)) return;
        if (!HAWK.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) return;
        hawk.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(hawk, LivingEntity.class, 10, true, true,
                prey -> prey.getType().is(HAWK_PREY)) {
            @Override
            public boolean canUse() {
                return !hawk.isBaby() && !sitting(hawk) && super.canUse();
            }
        });
    }

    private static boolean sitting(PathfinderMob hawk) {
        if (!lookedUp) {
            lookedUp = true;
            try {
                isSitting = hawk.getClass().getMethod("isSitting");
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (isSitting != null) {
            try {
                return (boolean) isSitting.invoke(hawk);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return hawk instanceof TamableAnimal tame && tame.isOrderedToSit();
    }
}
