package eteil.mcarpgbridge.client;

import eteil.mcarpgbridge.BridgeConfig;
import eteil.mcarpgbridge.BridgeNetwork.OpenMebahelPayload;
import eteil.mcarpgbridge.McaRpgBridge;
import eteil.mcarpgbridge.mixin.ScreenAccessor;
import net.conczin.mca.client.gui.InteractScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Puts a Request (Mebahel quest) or Rumor button in MCA's villager menu, on the main page next to
 * MCA: Quests' own button. Placement follows MCA: Quests: re-checked every frame because MCA rebuilds
 * its buttons whenever the page changes.
 */
@EventBusSubscriber(modid = McaRpgBridge.MOD_ID, value = Dist.CLIENT)
public final class McaMenuButton {
    private static final String MAIN_PAGE_MARKER = "gui.button.interact";
    private static final String TALK_BUTTON = "gui.button.talk";
    private static final String REQUEST = "gui.button.mcarpgbridge.request";
    private static final String RUMOR = "gui.button.mcarpgbridge.rumor";
    private static Field villagerField;
    private static boolean warned;

    private McaMenuButton() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        tryPlace(event.getScreen());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        tryPlace(event.getScreen());
    }

    private static void tryPlace(Screen screen) {
        if (!(screen instanceof InteractScreen) || !BridgeConfig.mcaMenuButton() || !ModList.get().isLoaded("mebahelrpgrquest")) {
            return;
        }
        try {
            Button interact = findButton(screen, MAIN_PAGE_MARKER);
            if (interact == null || findButton(screen, REQUEST) != null || findButton(screen, RUMOR) != null) {
                return;
            }
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null || !connection.hasChannel(OpenMebahelPayload.TYPE)) {
                return;
            }
            Entity villager = villagerOf(screen);
            String label = villager == null ? null : MebahelGiverView.buttonKey(villager, REQUEST, RUMOR);
            if (label != null) {
                add(screen, interact, label, villager);
            }
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                McaRpgBridge.LOGGER.warn("[MCA RPG Bridge] could not add the Mebahel button to MCA's menu", t);
            }
        }
    }

    private static void add(Screen screen, Button interact, String label, Entity villager) {
        Button talk = findButton(screen, TALK_BUTTON);
        int rowStride = talk != null && talk.getY() != interact.getY() ? Math.abs(interact.getY() - talk.getY()) : interact.getHeight() + 1;
        int y = interact.getY() + rowStride;
        while (rowOccupied(screen, y, interact.getHeight())) {
            y += rowStride;
        }
        Button button = Button.builder(Component.translatable(label), b -> PacketDistributor.sendToServer(new OpenMebahelPayload(villager.getUUID())))
                .bounds(interact.getX(), y, interact.getWidth(), interact.getHeight())
                .build();
        ScreenAccessor lists = (ScreenAccessor) screen;
        lists.mcarpgbridge$renderables().add(button);
        lists.mcarpgbridge$children().add(button);
        lists.mcarpgbridge$narratables().add(button);
    }

    @Nullable
    private static Entity villagerOf(Screen screen) throws ReflectiveOperationException {
        if (villagerField == null) {
            Field field = InteractScreen.class.getDeclaredField("villager");
            field.setAccessible(true);
            villagerField = field;
        }
        return villagerField.get(screen) instanceof Entity entity ? entity : null;
    }

    @Nullable
    private static Button findButton(Screen screen, String key) {
        Component target = Component.translatable(key);
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button && target.equals(button.getMessage())) {
                return button;
            }
        }
        return null;
    }

    private static boolean rowOccupied(Screen screen, int y, int height) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.visible && widget.getY() < y + height && widget.getY() + widget.getHeight() > y) {
                return true;
            }
        }
        return false;
    }
}
