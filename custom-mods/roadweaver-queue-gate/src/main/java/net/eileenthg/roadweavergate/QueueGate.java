package net.eileenthg.roadweavergate;

import com.mojang.logging.LogUtils;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/**
 * Decides whether RoadGenerationService.refreshQueue may run this tick. Dirty marks can come from
 * RoadWeaver's worker threads (planning saves connections off-thread), so the reason is held atomically.
 */
public final class QueueGate {

    private static final Logger LOG = LogUtils.getLogger();
    /** The pending refresh reason, or null when clean. Starts dirty so the first tick always refreshes. */
    private static final AtomicReference<String> PENDING = new AtomicReference<>("startup");
    private static long lastRefreshTick = Long.MIN_VALUE;

    private QueueGate() {}

    public static void markDirty(String reason) {
        PENDING.compareAndSet(null, reason);
    }

    public static void markPlayerMoved(String reason) {
        if (GateConfig.refreshOnPlayerMove()) markDirty(reason);
    }

    /** Called at the head of refreshQueue on the server thread. */
    public static boolean shouldRefresh(ServerLevel level) {
        if (!GateConfig.enabled()) return true;

        long now = level.getGameTime();
        String reason = PENDING.getAndSet(null);
        if (reason == null) {
            int fallback = GateConfig.fallbackTicks();
            if (fallback <= 0 || (lastRefreshTick != Long.MIN_VALUE && now - lastRefreshTick < fallback && now >= lastRefreshTick)) {
                return false;
            }
            reason = "fallback timer";
        }
        lastRefreshTick = now;
        if (GateConfig.logRefreshes()) LOG.info("[roadweavergate] refreshing road queue at tick {} ({})", now, reason);
        return true;
    }
}
