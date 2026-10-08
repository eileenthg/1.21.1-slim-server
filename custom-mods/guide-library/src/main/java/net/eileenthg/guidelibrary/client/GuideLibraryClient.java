package net.eileenthg.guidelibrary.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.eileenthg.guidelibrary.GuideLibrary;
import net.eileenthg.guidelibrary.LibraryConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = GuideLibrary.MOD_ID, dist = Dist.CLIENT)
public final class GuideLibraryClient {

    /** Unbound by default so it cannot clash with anything; set it in Controls. */
    public static final KeyMapping OPEN_KEY = new KeyMapping("key.guidelibrary.open",
            KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, "key.categories.guidelibrary");

    public GuideLibraryClient(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(OPEN_KEY));
        NeoForge.EVENT_BUS.addListener(GuideLibraryClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(GuideLibraryClient::onScreenRender);
        NeoForge.EVENT_BUS.addListener(GuideLibraryClient::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> ClientUnlocks.onLogin());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ItemBookOpener.clear();
            ClientUnlocks.onLogout();
        });
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ItemBookOpener.tick();
        ClientUnlocks.tick();
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new LibraryScreen(null));
        }
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        boolean creative = screen instanceof CreativeModeInventoryScreen;
        if (!(screen instanceof InventoryScreen) && !creative) return;
        if (!LibraryConfig.CLIENT.showInventoryButton.get()) return;
        if (creative && !LibraryConfig.CLIENT.showInCreative.get()) return;

        AbstractContainerScreen<?> container = (AbstractContainerScreen<?>) screen;
        LibraryButton button = new LibraryButton(0, 0, buttonIcon(),
                Component.translatable("guidelibrary.button"), b -> onButton(screen));
        place(container, button);
        event.addListener(button);
    }

    /**
     * Re-placed every frame: the survival inventory slides sideways when the recipe book opens,
     * and the creative screen only has room for it on the Inventory tab.
     */
    private static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> container)) return;
        for (var child : container.children()) {
            if (child instanceof LibraryButton button) place(container, button);
        }
    }

    private static void place(AbstractContainerScreen<?> screen, LibraryButton button) {
        if (screen instanceof CreativeModeInventoryScreen creative) {
            button.visible = creative.isInventoryOpen();
            button.setPosition(screen.getGuiLeft() + LibraryConfig.CLIENT.creativeButtonX.get(),
                    screen.getGuiTop() + LibraryConfig.CLIENT.creativeButtonY.get());
        } else {
            button.setPosition(screen.getGuiLeft() + LibraryConfig.CLIENT.buttonX.get(),
                    screen.getGuiTop() + LibraryConfig.CLIENT.buttonY.get());
        }
    }

    private static void onButton(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        String direct = LibraryConfig.COMMON.buttonOpensBook.get();
        if (!direct.isBlank() && !Screen.hasShiftDown()) {
            var entry = BookCatalog.find(direct);
            if (entry.isPresent()) {
                entry.get().opener().run();
                return;
            }
            GuideLibrary.LOGGER.warn("buttonOpensBook is '{}', but no such book exists; opening the library", direct);
        }
        mc.setScreen(new LibraryScreen(parent));
    }

    private static ItemStack buttonIcon() {
        ResourceLocation id = ResourceLocation.tryParse(LibraryConfig.CLIENT.buttonIcon.get());
        Item item = id == null ? Items.BOOK : BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item == Items.AIR ? Items.BOOK : item);
    }
}
