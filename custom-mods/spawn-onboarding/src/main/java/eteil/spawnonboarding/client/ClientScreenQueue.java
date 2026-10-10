package eteil.spawnonboarding.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Origins opens its choose-origin screen exactly once, on login. MCA's destiny screen and other
 * first-join screens open around the same time and replace it, and Origins never reopens it.
 *
 * <p>This keeps the Origins screen aside whenever another screen is showing (or is about to, while
 * MCA's destiny effects are still on the player) and opens it again once nothing else is on screen.
 */
public final class ClientScreenQueue {
    /** MCA applies both effects on join while destiny is pending and removes both when it closes. */
    private static final int JOIN_GRACE_TICKS = 40;

    private static Screen pendingOrigins;
    private static boolean reopening;
    private static int ticksInWorld;

    private ClientScreenQueue() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientScreenQueue::onOpening);
        NeoForge.EVENT_BUS.addListener(ClientScreenQueue::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            pendingOrigins = null;
            ticksInWorld = 0;
        });
    }

    private static boolean isOrigins(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return name.equals("dev.overgrown.origins.client.screen.ChooseOriginScreen")
                || name.equals("dev.overgrown.origins.client.screen.WaitForNextLayerScreen");
    }

    private static boolean destinyPending() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(MobEffects.INVISIBILITY) && player.hasEffect(MobEffects.HEALTH_BOOST);
    }

    private static void onOpening(ScreenEvent.Opening event) {
        if (reopening) return;
        Screen current = event.getCurrentScreen();
        Screen next = event.getNewScreen();
        if (isOrigins(next)) {
            boolean blocked = (current != null && !isOrigins(current)) || destinyPending() || ticksInWorld < JOIN_GRACE_TICKS;
            if (blocked) {
                pendingOrigins = next;
                event.setCanceled(true);
            }
        } else if (isOrigins(current) && next != null) {
            pendingOrigins = current;
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            ticksInWorld = 0;
            return;
        }
        ticksInWorld++;
        if (pendingOrigins == null || mc.screen != null || ticksInWorld < JOIN_GRACE_TICKS || destinyPending()) return;
        Screen screen = pendingOrigins;
        pendingOrigins = null;
        reopening = true;
        try {
            mc.setScreen(screen);
        } finally {
            reopening = false;
        }
    }
}
