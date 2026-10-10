package com.github.eileenthg.tamablearcadianfairy.ai.goal;

import com.github.eileenthg.tamablearcadianfairy.data.AttackMode;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** Retaliates against whatever last hurt the owner. */
public class FairyOwnerHurtByTargetGoal extends TargetGoal {

    private final PathfinderMob fairy;
    @Nullable private LivingEntity ownerAttacker;
    private int timestamp;

    public FairyOwnerHurtByTargetGoal(PathfinderMob fairy) {
        super(fairy, false);
        this.fairy = fairy;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (FairyTaming.actState(fairy).attackMode() == AttackMode.DISABLED) return false;

        Player owner = FairyTaming.owner(fairy);
        if (owner == null) return false;

        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker == null || attacker == owner) return false;
        if (FairyTaming.shareOwner(fairy, attacker)) return false;

        ownerAttacker = attacker;
        return owner.getLastHurtByMobTimestamp() != timestamp
                && canAttack(attacker, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        mob.setTarget(ownerAttacker);
        Player owner = FairyTaming.owner(fairy);
        if (owner != null) {
            timestamp = owner.getLastHurtByMobTimestamp();
        }
        super.start();
    }
}
