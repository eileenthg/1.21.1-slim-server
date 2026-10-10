package com.github.eileenthg.tamablearcadianfairy.ai.goal;

import com.github.eileenthg.tamablearcadianfairy.data.AttackMode;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Standard retaliation, except a fairy never turns on its owner or on a sibling fairy.
 */
public class FairyDefendSelfGoal extends HurtByTargetGoal {

    private final PathfinderMob fairy;

    public FairyDefendSelfGoal(PathfinderMob fairy) {
        super(fairy);
        this.fairy = fairy;
    }

    @Override
    public boolean canUse() {
        if (FairyTaming.actState(fairy).attackMode() == AttackMode.DISABLED) return false;
        if (!super.canUse()) return false;

        LivingEntity attacker = fairy.getLastHurtByMob();
        if (attacker == null) return false;
        if (attacker instanceof Player player && FairyTaming.isOwner(fairy, player)) return false;
        return !FairyTaming.shareOwner(fairy, attacker);
    }
}
