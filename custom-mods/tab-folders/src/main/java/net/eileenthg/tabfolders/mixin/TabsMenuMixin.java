package net.eileenthg.tabfolders.mixin;

import java.util.List;
import net.eileenthg.tabfolders.TabArranger;
import net.eileenthg.tabfolders.TabFolders;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sfiomn.legendarytabs.api.tabs_menu.TabBase;
import sfiomn.legendarytabs.api.tabs_menu.TabsMenu;
import sfiomn.legendarytabs.config.Config;

/**
 * {@code TabsMenu.initScreenButtons} first collects the enabled tabs for the screen, then makes one button per
 * tab while they fit. Between the two, the list is swapped for the arranged row, and the room for buttons
 * runs to the right edge of the window instead of stopping at the screen's panel width (6 tabs on the inventory).
 */
@Mixin(value = TabsMenu.class, remap = false)
public abstract class TabsMenuMixin {
    @Shadow
    private static List<TabBase> enabledTabs;
    @Shadow
    private static int leftScreenPos;

    @Unique
    private static Screen tabfolders$screen;

    @Inject(method = "initScreenButtons",
            at = @At(value = "FIELD", target = "Lsfiomn/legendarytabs/config/Config$Baked;tabsMenuOffsetX:I", opcode = Opcodes.GETSTATIC, ordinal = 0))
    private static void tabfolders$arrange(ScreenEvent.Init.Post event, CallbackInfo ci) {
        tabfolders$screen = event.getScreen();
        try {
            enabledTabs = TabArranger.arrange(event.getScreen(), enabledTabs);
        } catch (RuntimeException e) {
            TabFolders.LOG.error("Could not arrange the tab row for {}; showing Legendary Tabs' own row", event.getScreen().getClass().getName(), e);
        }
    }

    @ModifyVariable(method = "initScreenButtons", at = @At("STORE"), ordinal = 0)
    private static int tabfolders$widen(int remainingWidth) {
        Screen screen = tabfolders$screen;
        if (screen == null) {
            return remainingWidth;
        }
        return Math.max(remainingWidth, screen.width - leftScreenPos - Config.Baked.tabsMenuOffsetX - 4);
    }
}
