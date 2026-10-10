package com.github.eileenthg.tamablearcadianfairy.event;

import com.github.eileenthg.tamablearcadianfairy.TamableArcadianFairy;
import com.github.eileenthg.tamablearcadianfairy.data.ModGameRules;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Keeps tamed fairies and their owners from hurting each other, and keeps other
 * mobs from treating a pet fairy as a monster.
 */
@EventBusSubscriber(modid = TamableArcadianFairy.MOD_ID)
public final class CombatEvents {

    private CombatEvents() {}

    /** A tamed fairy never damages its owner, and never damages a sibling fairy. */
    @SubscribeEvent
    public static void onDamageDealtByFairy(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!FairyTaming.isTamed(attacker)) return;

        LivingEntity victim = event.getEntity();
        if (victim instanceof Player player && FairyTaming.isOwner(attacker, player)) {
            event.setCanceled(true);
            return;
        }
        if (FairyTaming.shareOwner(attacker, victim)) {
            event.setCanceled(true);
        }
    }

    /** The owner cannot hurt their own fairy unless the game rule allows it. */
    @SubscribeEvent
    public static void onDamageDealtToFairy(LivingIncomingDamageEvent event) {
        LivingEntity fairy = event.getEntity();
        if (!FairyTaming.isTamed(fairy)) return;

        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.getAbilities().instabuild) return;
        if (!FairyTaming.isOwner(fairy, player)) return;

        if (!fairy.level().getGameRules().getBoolean(ModGameRules.ALLOW_OWNER_HURT_FAIRY)) {
            event.setCanceled(true);
        }
    }

    /** Hostile mobs, iron golems included, leave tamed fairies alone. */
    @SubscribeEvent
    public static void onTargetSet(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget == null || !FairyTaming.isTamed(newTarget)) return;

        // Fairies may still fight each other, so a wild one can retaliate when your pet
        // starts the fight. Only non-fairy bystanders are made to back off.
        LivingEntity hunter = event.getEntity();
        if (FairyTaming.isFairy(hunter) && !FairyTaming.shareOwner(hunter, newTarget)) return;

        event.setCanceled(true);
    }
}
