package dev.eteil.butcheryam.util;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Runs a task a fixed number of server ticks from now.
 *
 * <p>Replaces MCreator's {@code queueServerWork}, which the bleeding sequence uses to turn a
 * carcass into its drained form after a delay.
 */
@EventBusSubscriber(modid = ButcheryAlexsMobs.MODID)
public final class ServerWorkQueue {

    private record Work(Runnable action, int[] ticksLeft) {}

    private static final Queue<Work> QUEUE = new ConcurrentLinkedQueue<>();

    public static void schedule(int ticks, Runnable action) {
        QUEUE.add(new Work(action, new int[]{ticks}));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (QUEUE.isEmpty()) {
            return;
        }
        List<Work> due = new ArrayList<>();
        for (Work work : QUEUE) {
            if (--work.ticksLeft()[0] <= 0) {
                due.add(work);
            }
        }
        for (Work work : due) {
            QUEUE.remove(work);
            work.action().run();
        }
    }

    private ServerWorkQueue() {}
}
