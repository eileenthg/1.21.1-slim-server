package com.github.eileenthg.tamablearcadianfairy.ai.goal;

import com.github.eileenthg.tamablearcadianfairy.data.ActState;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Keeps a tamed fairy near its owner. Fairies fly, so this simply navigates and
 * teleports when the owner gets too far away or crosses terrain the fairy cannot path over.
 */
public class FairyFollowOwnerGoal extends Goal {

    private static final double TELEPORT_DISTANCE = 24.0;
    private static final double SPEED = 1.1;

    private final Mob fairy;
    @Nullable private Player owner;
    private int recalculateCooldown;

    public FairyFollowOwnerGoal(Mob fairy) {
        this.fairy = fairy;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        ActState state = FairyTaming.actState(fairy);
        if (!state.followOwner()) return false;

        Player candidate = FairyTaming.owner(fairy);
        if (candidate == null || candidate.isSpectator()) return false;
        if (fairy.distanceToSqr(candidate) < square(state.followRange())) return false;

        owner = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null || !FairyTaming.actState(fairy).followOwner()) return false;
        // Stop a little closer than we started so the fairy does not jitter at the edge.
        return fairy.distanceToSqr(owner) > square(Math.max(2, FairyTaming.actState(fairy).followRange() - 1));
    }

    @Override
    public void start() {
        recalculateCooldown = 0;
    }

    @Override
    public void stop() {
        owner = null;
        fairy.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;

        fairy.getLookControl().setLookAt(owner, 10.0f, fairy.getMaxHeadXRot());
        if (--recalculateCooldown > 0) return;
        recalculateCooldown = 10;

        if (fairy.distanceToSqr(owner) > square(TELEPORT_DISTANCE)) {
            teleportNearOwner();
            return;
        }
        fairy.getNavigation().moveTo(owner, SPEED);
    }

    /** Fairies fly, so a short hop above the owner is always a valid landing spot. */
    private void teleportNearOwner() {
        if (owner == null || fairy.isLeashed() || fairy.isPassenger()) return;

        double x = owner.getX() + (fairy.getRandom().nextDouble() - 0.5) * 4.0;
        double y = owner.getY() + 1.5;
        double z = owner.getZ() + (fairy.getRandom().nextDouble() - 0.5) * 4.0;
        fairy.moveTo(x, y, z, fairy.getYRot(), fairy.getXRot());
        fairy.getNavigation().stop();
    }

    private static double square(double value) {
        return value * value;
    }
}
