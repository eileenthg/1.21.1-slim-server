package net.eileenthg.tabfolders.jei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.eileenthg.tabfolders.TabFolders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import sfiomn.legendarytabs.client.screens.NextTabsButton;
import sfiomn.legendarytabs.client.screens.TabButton;

/** The widened tab row can reach past the panel into JEI's ingredient list; JEI lays itself out around it. */
@JeiPlugin
public final class TabsJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TabFolders.MOD_ID, "tab_row");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                var screen = Minecraft.getInstance().screen;
                if (screen == null) {
                    return List.of();
                }
                List<Rect2i> out = new ArrayList<>();
                for (GuiEventListener l : screen.children()) {
                    if (l instanceof TabButton || l instanceof NextTabsButton) {
                        AbstractWidget w = (AbstractWidget) l;
                        out.add(new Rect2i(w.getX(), w.getY(), w.getWidth(), w.getHeight()));
                    }
                }
                return out;
            }
        });
    }
}
