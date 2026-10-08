package eteil.seedscout;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Better Sparse Structures rejects a structure start when an already generated structure is within a
 * size-scaled spacing radius, so what exists depends on generation order, not just the seed. This bridge
 * runs the mod's own decision code against a private, in-memory index (never the world's saved one) so a
 * scan can replay generation order around spawn.
 */
final class SparseStructuresBridge {
    private static boolean available;
    private static Method rules;
    private static Method isWhitelisted;
    private static Constructor<?> newIndex;
    private static Method decide;
    private static Method accepted;
    private static Method rejectionReason;
    /** Why the last decision was rejected (OVERLAP / SPACING / REPETITION), for tracing. */
    static final ThreadLocal<String> LAST_REASON = new ThreadLocal<>();

    static void init(Logger log) {
        try {
            Class<?> config = Class.forName("net.z2six.bettersparsestructures.ServerConfig");
            Class<?> ruleSet = Class.forName("net.z2six.bettersparsestructures.StructureRuleSet");
            Class<?> index = Class.forName("net.z2six.bettersparsestructures.GlobalStructureIndexSavedData");
            Class<?> decision = Class.forName("net.z2six.bettersparsestructures.GlobalStructureIndexSavedData$DecisionResult");
            rules = config.getMethod("structureRules");
            isWhitelisted = ruleSet.getMethod("isWhitelisted", String.class);
            newIndex = index.getDeclaredConstructor();
            newIndex.setAccessible(true);
            decide = index.getMethod("decideStructure", ChunkPos.class, String.class, BoundingBox.class, ruleSet, boolean.class);
            accepted = decision.getMethod("accepted");
            rejectionReason = decision.getMethod("rejectionReason");
            available = true;
            log.info("[seedscout] Better Sparse Structures bridge active");
        } catch (ClassNotFoundException e) {
            log.info("[seedscout] Better Sparse Structures not present");
        } catch (Throwable t) {
            log.warn("[seedscout] Better Sparse Structures bridge failed; structure spacing is not simulated", t);
        }
    }

    static boolean active() {
        return available;
    }

    /** A fresh spacing index for one simulated world, or null when the mod is absent. */
    static Object newIndex() {
        if (!available) return null;
        try {
            return newIndex.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Same call the mod makes before a start is stored; accepted starts are remembered in {@code index}. */
    static boolean accept(Object index, ChunkPos chunk, String structureId, BoundingBox box) {
        if (index == null) return true;
        try {
            Object ruleSet = rules.invoke(null);
            boolean whitelisted = (boolean) isWhitelisted.invoke(ruleSet, structureId);
            Object result = decide.invoke(index, chunk, structureId, box, ruleSet, whitelisted);
            boolean ok = (boolean) accepted.invoke(result);
            Object reason = ok ? null : rejectionReason.invoke(result);
            LAST_REASON.set(ok ? (whitelisted ? "whitelisted" : "ok") : String.valueOf(reason));
            return ok;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private SparseStructuresBridge() {}
}
