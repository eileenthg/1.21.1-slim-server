package dev.eteil.butcheryam.event;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import dev.eteil.butcheryam.Config;
import dev.eteil.butcheryam.butcher.ButcherUtil;
import dev.eteil.butcheryam.butcher.MobDef;
import dev.eteil.butcheryam.butcher.MobDefs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Replaces a supported mob's normal drops with its carcass block. */
@EventBusSubscriber(modid = ButcheryAlexsMobs.MODID)
public final class DeathHandler {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();

        MobDef def = MobDefs.forEntity(entity);
        if (def == null || !def.isEnabled() || entity.isBaby()) {
            return;
        }

        Entity killer = event.getSource().getEntity();
        if (Config.SPECIFIC_TOOLS.get() && !ButcherUtil.isCleaver(ButcherUtil.heldItem(killer))) {
            return;
        }

        // The original cancelled LivingDeathEvent to suppress vanilla loot, which also prevents
        // the mob from dying at all. Replacing the drops here has the intended effect instead.
        event.getDrops().clear();
        event.getDrops().add(new ItemEntity(entity.level(),
                entity.getX(), entity.getY(), entity.getZ(),
                new ItemStack(def.carcass().get())));
    }

    private DeathHandler() {}
}
