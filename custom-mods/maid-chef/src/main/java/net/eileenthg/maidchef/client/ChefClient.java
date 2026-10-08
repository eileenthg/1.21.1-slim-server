package net.eileenthg.maidchef.client;

import net.eileenthg.maidchef.MaidChef;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MaidChef.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ChefClient {
    private ChefClient() {
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(MaidChef.CHEF_CONFIG_MENU.get(), ChefConfigGui::new);
    }
}
