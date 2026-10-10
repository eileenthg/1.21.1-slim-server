package net.reimaden.touhouorigins.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.reimaden.touhouorigins.behavior.BehaviorLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/**
 * The broad half of {@code touhouorigins:modify_behavior}: stops goal-driven mobs targeting a holder
 * who has asked to be treated as neutral or passive.
 *
 * <p>Every {@code NearestAttackableTargetGoal} constructor funnels into the six-argument one, which
 * builds its {@code TargetingConditions} and calls {@code selector(Predicate)}. Wrapping that one
 * argument covers the whole family in a single injection — the same trick Voile used, translated
 * from Yarn ({@code ActiveTargetGoal} / {@code TargetPredicate.setPredicate}) to Mojmap.
 *
 * <p>The original predicate is preserved and ANDed with ours, so vanilla's own targeting rules still
 * apply; we only ever remove targets, never add them.
 */
@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin extends TargetGoal {

    public NearestAttackableTargetGoalMixin(Mob mob, boolean mustSee) {
        super(mob, mustSee);
    }

    @ModifyArg(
        method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;selector(Ljava/util/function/Predicate;)Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;"))
    private Predicate<LivingEntity> touhouorigins$respectModifyBehavior(Predicate<LivingEntity> original) {
        Predicate<LivingEntity> refusal = target -> !BehaviorLookup.refusesToTarget(target, this.mob);
        return original == null ? refusal : original.and(refusal);
    }
}
