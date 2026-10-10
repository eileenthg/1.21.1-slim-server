package net.reimaden.touhouorigins.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.reimaden.touhouorigins.power.ModifyBehaviorPower;

/**
 * The {@code hostile} half of {@code touhouorigins:modify_behavior}: makes mobs that would normally
 * ignore you attack you.
 *
 * <p>Voile needed a mixin per mob for this (iron golems, wolves, bees, piglin brains, …) because
 * Fabric has no general "entity joined the world" hook. NeoForge does, so one
 * {@link EntityJoinLevelEvent} listener adds a single player-targeting goal to every {@link Mob} and
 * lets the goal's own predicate decide. That replaces roughly twenty of Voile's mixins.
 *
 * <p>The goal is inert unless some nearby player actually holds a {@code modify_behavior} power set
 * to {@code hostile} whose conditions accept this mob, so the cost on an ordinary world is one
 * predicate call per targeting tick that fails on its first check.
 *
 * <p>Covers Touhou's two hostile uses: {@code amanojaku/antagonist} (unconditional — everything
 * turns on you) and {@code common/feared} (gated on {@code touhouorigins:villager_aligned}, i.e.
 * villagers and iron golems). Note villagers have no attack goals of their own, so for them the
 * visible effect comes from their brain rather than this goal — see NOTES.md.
 */
public final class HostileTargetingHandler {

    /** Priority 1 matches Voile's placement — above wander, below the mob's own revenge target. */
    private static final int GOAL_PRIORITY = 1;

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        mob.targetSelector.addGoal(GOAL_PRIORITY,
            new NearestAttackableTargetGoal<>(mob, Player.class, false,
                target -> BehaviorLookup.behaviorFor(target, mob) == ModifyBehaviorPower.Behavior.HOSTILE));
    }
}
