package com.github.eileenthg.tamablearcadianfairy.ai.goal;

import com.github.eileenthg.tamablearcadianfairy.data.AttackMode;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Active mode only. Picks nearby hostile mobs, skipping anyone's tamed fairy.
 */
public class FairyHuntHostilesGoal extends NearestAttackableTargetGoal<Mob> {

    private final PathfinderMob fairy;

    public FairyHuntHostilesGoal(PathfinderMob fairy) {
        super(fairy, Mob.class, 10, true, false,
                candidate -> candidate instanceof Enemy && !FairyTaming.isTamed(candidate));
        this.fairy = fairy;
    }

    @Override
    public boolean canUse() {
        return FairyTaming.actState(fairy).attackMode() == AttackMode.ACTIVE && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return FairyTaming.actState(fairy).attackMode() == AttackMode.ACTIVE && super.canContinueToUse();
    }
}
