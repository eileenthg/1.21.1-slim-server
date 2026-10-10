package eteil.mcarpgbridge.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Adds a widget to a screen from outside its init, since MCA rebuilds its menu buttons per page. */
@Mixin(Screen.class)
public interface ScreenAccessor {
    @Accessor("renderables")
    List<Renderable> mcarpgbridge$renderables();

    @Accessor("children")
    List<GuiEventListener> mcarpgbridge$children();

    @Accessor("narratables")
    List<NarratableEntry> mcarpgbridge$narratables();
}
