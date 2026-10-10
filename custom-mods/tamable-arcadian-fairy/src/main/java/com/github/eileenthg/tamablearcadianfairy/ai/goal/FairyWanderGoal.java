package com.github.eileenthg.tamablearcadianfairy.ai.goal;

import com.github.eileenthg.tamablearcadianfairy.data.ActState;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Replaces the wild fairy's free roaming. A tamed fairy drifts around its
 * anchor point instead, so it never wanders off while the owner is away.
 */
public class FairyWanderGoal extends Goal {

    private static final double SPEED = 0.9;

    private final Mob fairy;
    private Vec3 destination = Vec3.ZERO;

    public FairyWanderGoal(Mob fairy) {
        this.fairy = fairy;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        ActState state = FairyTaming.actState(fairy);
        if (!state.wander()) return false;
        if (!fairy.getNavigation().isDone()) return false;
        if (fairy.getRandom().nextInt(30) != 0) return false;

        Vec3 target = pickDestination(state);
        if (target == null) return false;

        destination = target;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !fairy.getNavigation().isDone() && FairyTaming.actState(fairy).wander();
    }

    @Override
    public void start() {
        fairy.getNavigation().moveTo(destination.x, destination.y, destination.z, SPEED);
    }

    @Override
    public void stop() {
        fairy.getNavigation().stop();
    }

    private Vec3 pickDestination(ActState state) {
        Vec3 center = state.wanderCenter().equals(Vec3.ZERO) ? fairy.position() : state.wanderCenter();
        double range = state.wanderRange();

        for (int attempt = 0; attempt < 8; attempt++) {
            double x = center.x + (fairy.getRandom().nextDouble() * 2.0 - 1.0) * range;
            double y = center.y + (fairy.getRandom().nextDouble() * 2.0 - 1.0) * Math.min(range, 4.0);
            double z = center.z + (fairy.getRandom().nextDouble() * 2.0 - 1.0) * range;
            Vec3 candidate = new Vec3(x, y, z);
            if (fairy.level().noCollision(fairy, fairy.getBoundingBox().move(candidate.subtract(fairy.position())))) {
                return candidate;
            }
        }
        return null;
    }
}
