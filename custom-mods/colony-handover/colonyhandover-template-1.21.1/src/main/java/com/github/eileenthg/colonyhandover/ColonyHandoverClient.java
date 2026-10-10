package com.github.eileenthg.colonyhandover;

import com.github.eileenthg.colonyhandover.client.ChecklistWindow;
import com.github.eileenthg.colonyhandover.client.CofferScreen;
import com.github.eileenthg.colonyhandover.scroll.ColonyIssues;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.github.eileenthg.colonyhandover.compat.tweaks.TweaksBookCompat;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ColonyHandover.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = ColonyHandover.MODID, value = Dist.CLIENT)
public class ColonyHandoverClient {
    public ColonyHandoverClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /** Give up waiting after three seconds - the server said why in chat. */
    private static final int AWAIT_TICKS = 60;

    /** The survey timestamp the window must see change before it opens, if waiting. */
    private static long awaitingFrom;
    private static int awaitLeft;

    /**
     * Opens the checklist for a stack, straight away, on whatever it carries.
     *
     * <p>Used by the Tweaks book row, which shows a stored survey rather than taking a
     * new one - the book is for carrying lists around, not for surveying from.
     */
    public static void openChecklist(ItemStack stack) {
        openChecklist(stack, null);
    }

    /** As above, with Close and Escape returning to {@code returnTo} when it is not null. */
    public static void openChecklist(ItemStack stack, @Nullable Screen returnTo) {
        final ColonyIssues issues = stack.get(ColonyHandover.COLONY_ISSUES.get());
        if (issues == null) {
            return;
        }
        new ChecklistWindow(issues, () -> {
            final var player = Minecraft.getInstance().player;
            if (player != null) {
                player.connection.sendCommand("colonyhandover checklist refresh");
                awaitSurvey(issues.surveyedAt());
            }
        }).returningTo(returnTo).open();
    }

    /**
     * Opens the checklist once the scroll in hand carries a survey newer than this one.
     *
     * <p>The survey is taken on the server and arrives back as a component on the
     * stack a tick or so later; opening at once would show the old list. Each survey is
     * timestamped, so waiting for the timestamp to change works even when nothing in
     * the colony did.
     */
    public static void awaitSurvey(long previousSurveyedAt) {
        awaitingFrom = previousSurveyedAt;
        awaitLeft = AWAIT_TICKS;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (awaitLeft <= 0) {
            return;
        }
        awaitLeft--;
        final var player = Minecraft.getInstance().player;
        if (player == null) {
            awaitLeft = 0;
            return;
        }
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ColonyHandover.CHECKLIST_SCROLL.get())) {
                continue;
            }
            final ColonyIssues issues = stack.get(ColonyHandover.COLONY_ISSUES.get());
            if (issues != null && issues.surveyedAt() != awaitingFrom) {
                awaitLeft = 0;
                openChecklist(stack);
            }
            return;
        }
    }

    /**
     * Binds the coffer menu to its screen.
     *
     * <p>Must happen here and nowhere else: screen registration touches a client-only
     * registry, so doing it from common code would crash a dedicated server on load.
     */
    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ColonyHandover.COFFER_MENU.get(), CofferScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        // Guarded, and the guard is the whole point: TweaksBookCompat names types
        // from another mod, which the JVM resolves when the class loads rather than
        // when it runs. Touching it without this check would crash anyone who does
        // not have the addon installed.
        if (ModList.get().isLoaded("minecolonies_tweaks")) {
            TweaksBookCompat.init();
        }
    }
}
