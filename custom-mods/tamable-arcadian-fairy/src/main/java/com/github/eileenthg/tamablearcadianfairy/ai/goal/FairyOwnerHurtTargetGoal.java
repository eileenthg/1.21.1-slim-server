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

/** Helps the owner finish whatever they just attacked. */
public class FairyOwnerHurtTargetGoal extends TargetGoal {

    private final PathfinderMob fairy;
    @Nullable private LivingEntity ownerTarget;
    private int timestamp;

    public FairyOwnerHurtTargetGoal(PathfinderMob fairy) {
        super(fairy, false);
        this.fairy = fairy;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (FairyTaming.actState(fairy).attackMode() == AttackMode.DISABLED) return false;

        Player owner = FairyTaming.owner(fairy);
        if (owner == null) return false;

        LivingEntity victim = owner.getLastHurtMob();
        // Never help the owner fight another player, and never turn on a sibling fairy.
        if (victim == null || victim instanceof Player || FairyTaming.shareOwner(fairy, victim)) return false;

        ownerTarget = victim;
        return owner.getLastHurtMobTimestamp() != timestamp
                && canAttack(victim, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        mob.setTarget(ownerTarget);
        Player owner = FairyTaming.owner(fairy);
        if (owner != null) {
            timestamp = owner.getLastHurtMobTimestamp();
        }
        super.start();
    }
}
