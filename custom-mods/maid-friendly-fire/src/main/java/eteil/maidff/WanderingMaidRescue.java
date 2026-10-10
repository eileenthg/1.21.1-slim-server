package eteil.maidff;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Lost Maids turns every maid that joins the world without an owner, items or equipment into a hostile "lost
 * maid": it arms her, gives her the attack-player task, and blocks normal taming. Love &amp; Loathe's wandering
 * maids (the ones that walk up and ask to be taken in) spawn exactly like that, so they were being converted too,
 * and accepting one failed with Love &amp; Loathe's "Check your maid limit" message. The maids a wandering trader
 * sells (leashed, ownerless, empty-handed) get the same treatment.
 *
 * This runs after Lost Maids and undoes the conversion for wandering maids only. Lost Maids only converts a maid
 * whose inventory and equipment were empty, so clearing the equipment restores her exactly.
 *
 * Reflection throughout, like the rest of this mod: if either mod is missing or changes, this does nothing.
 */
final class WanderingMaidRescue {
    private static final String WANDERING_TAG = "callresponse:wandering_maid_special";
    private static boolean broken;
    private static Object isLostKey;
    private static Method getOrCreateData;
    private static Method setData;

    private WanderingMaidRescue() {
    }

    static void onJoin(Entity entity) {
        if (broken || !MaidFriendlyFire.isMaid(entity)) return;
        if (!entity.getPersistentData().getBoolean(WANDERING_TAG) && !TradingMaidFlee.isStockMaid(entity)) return;
        try {
            if (!resolve(entity)) return;
            if (!Boolean.TRUE.equals(getOrCreateData.invoke(entity, isLostKey, Boolean.FALSE))) return;

            setData.invoke(entity, isLostKey, Boolean.FALSE);
            LivingEntity maid = (LivingEntity) entity;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                maid.setItemSlot(slot, ItemStack.EMPTY);
            }
            setIdleTask(entity);
            setDefaultSchedule(entity);
            MaidFriendlyFire.LOG.info("[MaidFriendlyFire] kept wandering maid {} from being turned into a lost maid", entity.getId());
        } catch (Throwable t) {
            broken = true;
            MaidFriendlyFire.LOG.warn("[MaidFriendlyFire] wandering-maid rescue disabled: {}", t.toString());
        }
    }

    private static boolean resolve(Entity maid) throws ReflectiveOperationException {
        if (isLostKey != null) return true;
        Class<?> lostData;
        try {
            lostData = Class.forName("com.github.qichensn.data.LostMaidData");
        } catch (ClassNotFoundException e) {
            broken = true;  // Lost Maids not installed: nothing to undo
            return false;
        }
        Object key = lostData.getField("IS_LOST_MAID").get(null);
        if (key == null) return false;
        Class<?> keyType = Class.forName("com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey");
        for (Method m : maid.getClass().getMethods()) {
            if (m.getParameterCount() != 2 || m.getParameterTypes()[0] != keyType) continue;
            if (m.getName().equals("getOrCreateData")) getOrCreateData = m;
            if (m.getName().equals("setData")) setData = m;
        }
        if (getOrCreateData == null || setData == null) throw new NoSuchMethodException("EntityMaid task data accessors");
        isLostKey = key;
        return true;
    }

    private static void setIdleTask(Entity maid) throws ReflectiveOperationException {
        Class<?> idle = Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.task.TaskIdle");
        Object uid = idle.getField("UID").get(null);
        Class<?> manager = Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager");
        Optional<?> task = (Optional<?>) manager.getMethod("findTask", ResourceLocation.class).invoke(null, uid);
        if (task.isEmpty()) return;
        for (Method m : maid.getClass().getMethods()) {
            if (m.getName().equals("setTask") && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isInstance(task.get())) {
                m.invoke(maid, task.get());
                return;
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setDefaultSchedule(Entity maid) throws ReflectiveOperationException {
        Class schedule = Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule");
        maid.getClass().getMethod("setSchedule", schedule).invoke(maid, Enum.valueOf(schedule, "DAY"));
    }
}
