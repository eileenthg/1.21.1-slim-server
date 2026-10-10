package eteil.packprobe;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.GeneratedEventListener;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventListener;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Wraps one damage-event handler and reports if it cancels or changes a maid's hit on a player. */
public final class TracingListener extends GeneratedEventListener {
    private final EventListener delegate;

    public TracingListener(EventListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void invoke(Event event) {
        if (!(event instanceof LivingIncomingDamageEvent e) || !(e.getEntity() instanceof Player) || !PackProbe.isMaid(e.getSource())) {
            delegate.invoke(event);
            return;
        }
        boolean canceledBefore = e.isCanceled();
        float amountBefore = e.getAmount();
        delegate.invoke(event);
        if (e.isCanceled() != canceledBefore || e.getAmount() != amountBefore) {
            PackProbe.LOG.info("[PackProbe] CHANGED BY {} -> canceled {}→{} amount {}→{}",
                    delegate, canceledBefore, e.isCanceled(), amountBefore, e.getAmount());
        }
    }

    @Override
    public String toString() {
        return "trace(" + delegate + ")";
    }
}
