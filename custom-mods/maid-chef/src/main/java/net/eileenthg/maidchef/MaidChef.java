package net.eileenthg.maidchef;

import net.eileenthg.maidchef.net.SetChefSettingsPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MaidChef.MOD_ID)
public final class MaidChef {
    public static final String MOD_ID = "maidchef";
    static volatile boolean selfTestActive;
    /** Keeps the self-test kitchens loaded; NeoForge also counts these when deciding whether an empty server keeps ticking. */
    public static final net.neoforged.neoforge.common.world.chunk.TicketController TICKETS =
            new net.neoforged.neoforge.common.world.chunk.TicketController(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "selftest"));

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<ChefConfigContainer>> CHEF_CONFIG_MENU = MENUS.register(
            "chef_config", () -> IMenuTypeExtension.create((id, inv, buf) -> new ChefConfigContainer(id, inv, buf.readInt())));

    public MaidChef(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ChefConfig.SPEC);
        MENUS.register(modBus);
        modBus.addListener(MaidChef::registerPayloads);
        modBus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent e) -> e.register(TICKETS));
        NeoForge.EVENT_BUS.addListener(ChefCommand::register);
        NeoForge.EVENT_BUS.addListener(ChefClaims::onJoin);
        NeoForge.EVENT_BUS.addListener(ChefClaims::onPlayerPickup);
        // the self-test (/maidchef selftest) is only in test builds
        try {
            Class.forName("net.eileenthg.maidchef.SelfTestHook").getMethod("install").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // release build: no self-test
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("3");
        r.playToServer(SetChefSettingsPayload.TYPE, SetChefSettingsPayload.STREAM_CODEC, SetChefSettingsPayload::handle);
        r.playToServer(net.eileenthg.maidchef.net.ExplainPayloads.Request.TYPE, net.eileenthg.maidchef.net.ExplainPayloads.Request.STREAM_CODEC,
                net.eileenthg.maidchef.net.ExplainPayloads.Request::handle);
        r.playToServer(net.eileenthg.maidchef.net.RequestPayloads.Fetch.TYPE, net.eileenthg.maidchef.net.RequestPayloads.Fetch.STREAM_CODEC,
                net.eileenthg.maidchef.net.RequestPayloads.Fetch::handle);
        r.playToServer(net.eileenthg.maidchef.net.RequestPayloads.Change.TYPE, net.eileenthg.maidchef.net.RequestPayloads.Change.STREAM_CODEC,
                net.eileenthg.maidchef.net.RequestPayloads.Change::handle);
        r.playToClient(net.eileenthg.maidchef.net.RequestPayloads.Sync.TYPE, net.eileenthg.maidchef.net.RequestPayloads.Sync.STREAM_CODEC,
                net.eileenthg.maidchef.net.RequestPayloads.Sync::handle);
        r.playToClient(net.eileenthg.maidchef.net.ExplainPayloads.Lines.TYPE, net.eileenthg.maidchef.net.ExplainPayloads.Lines.STREAM_CODEC,
                net.eileenthg.maidchef.net.ExplainPayloads.Lines::handle);
    }
}
