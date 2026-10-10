package com.github.eileenthg.tamablearcadianfairy.event;

import com.github.eileenthg.tamablearcadianfairy.TamableArcadianFairy;
import com.github.eileenthg.tamablearcadianfairy.ai.TamedFairyAi;
import com.github.eileenthg.tamablearcadianfairy.compat.TouhouLittleMaidPower;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.reimaden.arcadiandream.entity.custom.mob.BaseFairyEntity;

/**
 * Reapplying the tamed brain on load, keeping pets from despawning, and telling
 * owners when a fairy dies.
 */
@EventBusSubscriber(modid = TamableArcadianFairy.MOD_ID)
public final class LifecycleEvents {

    private LifecycleEvents() {}

    /** Goals are not saved, so the tamed brain has to be rebuilt every time the entity loads. */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof BaseFairyEntity fairy && FairyTaming.isTamed(fairy)) {
            TamedFairyAi.apply(fairy);
        }
    }

    /** A pet fairy stays put even when its owner is far away. */
    @SubscribeEvent
    public static void onDespawn(MobDespawnEvent event) {
        if (FairyTaming.isTamed(event.getEntity())) {
            event.setResult(MobDespawnEvent.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity fairy = event.getEntity();
        if (fairy.level().isClientSide() || !FairyTaming.isTamed(fairy)) return;

        Player owner = FairyTaming.owner(fairy);
        if (!(owner instanceof ServerPlayer serverOwner)) return;

        LivingEntity killer = fairy.getKillCredit();
        serverOwner.sendSystemMessage(killer == null
                ? Component.translatable("message.tamablearcadianfairy.death_no_killer", fairy.getDisplayName())
                : Component.translatable("message.tamablearcadianfairy.death",
                        fairy.getDisplayName(), killer.getDisplayName()));
    }

    /** A kill made by a tamed fairy yields Touhou Little Maid power, if that mod is present. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!FairyTaming.isTamed(killer)) return;

        TouhouLittleMaidPower.dropFor(event.getEntity());
    }
}
