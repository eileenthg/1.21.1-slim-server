package eteil.maidff;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * TLM decides what a maid may attack in IAttackTask.canAttack. A maid it might target is a TamableAnimal, so an
 * owned one is rejected outright; an ownerless one — a Lost Maid, or a maid that betrayed or was expelled — falls
 * through to DefaultMonsterType.getMonsterType, which calls every TamableAnimal FRIENDLY and refuses the target.
 *
 * That check consults the maid's own attack list first, which is the per-maid table in the status pane. So this
 * writes one default entry into that table, marking maids HOSTILE, and the ownerless ones become valid targets.
 * Owned maids are still filtered out before the table is ever read, so this cannot turn maids against each other.
 *
 * The entry is only added when the table has no opinion about maids yet. Set it back to friendly in the pane and
 * that choice is a value in the table, so it stays.
 */
final class MaidTargeting {
    private static final ResourceLocation MAID_ID = ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "maid");
    private static final String INIT_TASK_DATA = "com.github.tartaricacid.touhoulittlemaid.init.InitTaskData";
    private static final String ATTACK_LIST_DATA = "com.github.tartaricacid.touhoulittlemaid.entity.data.inner.AttackListData";
    private static final String MONSTER_TYPE = "com.github.tartaricacid.touhoulittlemaid.entity.misc.MonsterType";
    private static boolean reported = false;
    private static boolean broken = false;

    private MaidTargeting() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void nudge(Entity entity) {
        if (broken || !MaidFriendlyFire.isMaid(entity)) return;
        if (MaidFriendlyFire.ownerUuid(entity) == null) return;   // hostile maids run their own attack task
        try {
            Object key = Class.forName(INIT_TASK_DATA).getField("ATTACK_LIST").get(null);
            if (key == null) return;                              // task data not registered yet

            Object empty = Class.forName(ATTACK_LIST_DATA).getMethod("empty").invoke(null);
            Object data = method(entity, "getOrCreateData").invoke(entity, key, empty);
            Map<Object, Object> groups = (Map<Object, Object>) data.getClass().getMethod("attackGroups").invoke(data);
            if (groups.containsKey(MAID_ID)) return;              // the owner already has a preference, leave it

            Object hostile = Enum.valueOf((Class<Enum>) Class.forName(MONSTER_TYPE), "HOSTILE");
            groups.put(MAID_ID, hostile);
            method(entity, "setAndSyncData").invoke(entity, key, data);

            if (!reported) {
                MaidFriendlyFire.LOG.info("[MaidFriendlyFire] marking maids hostile in the attack list of owned maids, so they target Lost Maids");
                reported = true;
            }
        } catch (Throwable t) {
            // One complaint, then stay quiet: this runs for every maid that loads.
            broken = true;
            MaidFriendlyFire.LOG.warn("[MaidFriendlyFire] could not set the default attack list entry, maids will ignore Lost Maids: {}", t.toString());
        }
    }

    /** EntityMaid's data accessors are generic, so match on name and arity rather than parameter types. */
    private static Method method(Object owner, String name) throws NoSuchMethodException {
        for (Method m : owner.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 2) return m;
        }
        throw new NoSuchMethodException(name);
    }
}
