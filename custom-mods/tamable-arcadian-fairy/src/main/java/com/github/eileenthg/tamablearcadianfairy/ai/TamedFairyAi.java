package com.github.eileenthg.tamablearcadianfairy.ai;

import com.github.eileenthg.tamablearcadianfairy.ai.goal.FairyDefendSelfGoal;
import com.github.eileenthg.tamablearcadianfairy.ai.goal.FairyFollowOwnerGoal;
import com.github.eileenthg.tamablearcadianfairy.ai.goal.FairyHuntHostilesGoal;
import com.github.eileenthg.tamablearcadianfairy.ai.goal.FairyOwnerHurtByTargetGoal;
import com.github.eileenthg.tamablearcadianfairy.ai.goal.FairyOwnerHurtTargetGoal;
import com.github.eileenthg.tamablearcadianfairy.ai.goal.FairyWanderGoal;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.reimaden.arcadiandream.entity.custom.mob.BaseFairyEntity;

import java.util.List;

/**
 * Rewrites a fairy's brain when it is tamed.
 *
 * <p>Arcadian Dream's danmaku goal already fires at whatever {@code getTarget()} returns, so it is
 * deliberately left in place. A tamed fairy keeps its own bullet patterns, personality and
 * cooldowns, and only its choice of target and its idle movement change.
 */
public final class TamedFairyAi {

    /** Priority of Arcadian Dream's own danmaku goal, which we slot our movement goals underneath. */
    private static final int FOLLOW_PRIORITY = 2;
    private static final int WANDER_PRIORITY = 3;

    private TamedFairyAi() {}

    public static void apply(BaseFairyEntity fairy) {
        if (fairy.level().isClientSide()) return;
        if (hasBeenApplied(fairy)) return;

        clearHostility(fairy);
        removeWildMovement(fairy);
        removeWildTargeting(fairy);

        fairy.goalSelector.addGoal(FOLLOW_PRIORITY, new FairyFollowOwnerGoal(fairy));
        fairy.goalSelector.addGoal(WANDER_PRIORITY, new FairyWanderGoal(fairy));

        fairy.targetSelector.addGoal(1, new FairyOwnerHurtByTargetGoal(fairy));
        fairy.targetSelector.addGoal(2, new FairyOwnerHurtTargetGoal(fairy));
        fairy.targetSelector.addGoal(3, new FairyDefendSelfGoal(fairy));
        fairy.targetSelector.addGoal(4, new FairyHuntHostilesGoal(fairy));

        // A tamed fairy is a pet, not a wandering monster.
        fairy.setPersistenceRequired();
    }

    /** Guards against applying the same goals twice when an entity reloads. */
    private static boolean hasBeenApplied(BaseFairyEntity fairy) {
        return fairy.goalSelector.getAvailableGoals().stream()
                .map(WrappedGoal::getGoal)
                .anyMatch(FairyFollowOwnerGoal.class::isInstance);
    }

    private static void clearHostility(BaseFairyEntity fairy) {
        fairy.setTarget(null);
        fairy.setLastHurtByMob(null);
        if (fairy instanceof NeutralMob neutral) {
            neutral.setPersistentAngerTarget(null);
            neutral.setRemainingPersistentAngerTime(0);
        }
    }

    /** Drops the wild roaming goal so the fairy stays around its anchor instead. */
    private static void removeWildMovement(BaseFairyEntity fairy) {
        List<WrappedGoal> goals = List.copyOf(fairy.goalSelector.getAvailableGoals());
        for (WrappedGoal wrapped : goals) {
            Goal goal = wrapped.getGoal();
            if (goal instanceof RandomStrollGoal) {
                fairy.goalSelector.removeGoal(goal);
            }
        }
    }

    /** Removes every goal that would make the fairy pick a target on its own, players included. */
    private static void removeWildTargeting(BaseFairyEntity fairy) {
        List<WrappedGoal> goals = List.copyOf(fairy.targetSelector.getAvailableGoals());
        for (WrappedGoal wrapped : goals) {
            fairy.targetSelector.removeGoal(wrapped.getGoal());
        }
    }
}
