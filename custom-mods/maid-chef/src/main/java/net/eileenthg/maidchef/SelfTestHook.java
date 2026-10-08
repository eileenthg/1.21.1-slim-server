package net.eileenthg.maidchef;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Test-only: wires up {@code /maidchef selftest}. Left out of release builds ({@code ./gradlew build -Prelease});
 * {@link MaidChef} only installs it when the class is present.
 */
public final class SelfTestHook {
    private SelfTestHook() {
    }

    public static void install() {
        // SelfTest touches Maidsoul Kitchen classes, so it's only loaded once a test is running
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (MaidChef.selfTestActive) SelfTest.tick(e);
        });
        // stopping the server mid-test would otherwise save the test maids into the world
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> {
            if (MaidChef.selfTestActive) SelfTest.clean(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> {
            if (MaidChef.selfTestActive) SelfTest.onDeath(e);
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> event.getDispatcher().register(
                Commands.literal(MaidChef.MOD_ID).then(Commands.literal("selftest").requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            if (!ModList.get().isLoaded("farmersdelight") || !ModList.get().isLoaded("maidsoulkitchen")) {
                                ctx.getSource().sendFailure(Component.literal("The self-test needs Farmer's Delight and Maidsoul Kitchen."));
                                return 0;
                            }
                            SelfTest.start(ctx.getSource());
                            return 1;
                        })
                        .then(Commands.literal("clean").executes(ctx -> {
                            SelfTest.clean(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("maidchef self-test area cleared"), true);
                            return 1;
                        })))));
    }
}
