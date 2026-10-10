package dev.eteil.butcheryam2.event;

import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import dev.eteil.butcheryam2.blockentity.CarcassBlockEntity;
import dev.eteil.butcheryam2.blockentity.DrawnCarcassBlockEntity;
import dev.eteil.butcheryam2.butcher.ButcherUtil;
import dev.eteil.butcheryam2.butcher.MobDef;
import dev.eteil.butcheryam2.butcher.MobDefs;
import dev.eteil.butcheryam2.init.ModBlockEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * The carcass itself comes from {@link CarcassLootModifier}. What is left here: AMC Delight adds a
 * random meat cut on death outside the loot table, which a carcass replaces too — it yields those
 * cuts itself.
 */
@EventBusSubscriber(modid = ButcheryAlexsMobs2.MODID)
public final class DeathHandler {

    private static final String CLEAVER_KILL = "butcheryam2_cleaver_kill";

    /**
     * Remembers a cleaver kill at the moment of death. Some creatures roll their loot long after
     * (the void worm waits 80 ticks, and Minecraft forgets the damage source after 40).
     */
    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (ButcherUtil.isCleaver(ButcherUtil.heldItem(event.getSource().getEntity()))
                && MobDefs.forEntity(event.getEntity()) != null) {
            event.getEntity().getPersistentData().putBoolean(CLEAVER_KILL, true);
        }
    }

    public static boolean killedWithCleaver(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getPersistentData().getBoolean(CLEAVER_KILL);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(LivingDropsEvent event) {
        MobDef def = MobDefs.forEntity(event.getEntity());
        if (def == null) {
            return;
        }
        boolean butchered = event.getDrops().stream().anyMatch(e -> e.getItem().is(def.carcass().get().asItem()));
        if (butchered) {
            event.getDrops().removeIf(DeathHandler::isAmcDelightCut);
        }
    }

    private static boolean isAmcDelightCut(ItemEntity drop) {
        return BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()).getNamespace().equals("amcdelight");
    }

    /** Carries a segmented body's length and variant into the placed carcass's block entity. */
    public static ItemStack withBody(ItemStack stack, int segments, boolean variant) {
        if (segments > 0 || variant) {
            CompoundTag data = new CompoundTag();
            if (segments > 0) {
                data.putInt(DrawnCarcassBlockEntity.COUNT, segments);
            }
            if (variant) {
                data.putBoolean(DrawnCarcassBlockEntity.VARIANT, true);
            }
            CompoundTag tag = new CompoundTag();
            tag.put(CarcassBlockEntity.PERSISTENT_KEY, data);
            BlockItem.setBlockEntityData(stack, ModBlockEntities.DRAWN.get(), tag);
        }
        return stack;
    }

    private DeathHandler() {}
}
