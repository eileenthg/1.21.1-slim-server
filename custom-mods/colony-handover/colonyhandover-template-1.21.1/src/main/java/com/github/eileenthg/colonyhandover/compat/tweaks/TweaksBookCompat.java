package com.github.eileenthg.colonyhandover.compat.tweaks;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import net.neoforged.neoforge.common.NeoForge;
import steve_gall.minecolonies_tweaks.api.client.gui.ResourceScrollBookElementEvent;

/**
 * Lets a Conversion Checklist live in the Tweaks addon's Resource Scroll Book.
 *
 * <h2>Two halves, both additive</h2>
 *
 * <p><b>Getting in the book.</b> {@code ResourceScrollBookInventoryMenu.BookSlot}
 * accepts anything in the item tag
 * {@code minecolonies_tweaks:resourcescrollbook_element} - nothing else is checked.
 * So our datapack adds the scroll to that tag, with {@code required: false} so it is
 * inert when the addon is absent. No coordination with that mod needed.
 *
 * <p><b>Drawing a row.</b> The book posts {@link ResourceScrollBookElementEvent} on
 * the game bus for each stack it holds, and any listener may supply an element. That
 * is a published extension point in the addon's {@code api} package, so this is the
 * intended way in rather than a mixin into someone else's screen.
 *
 * <h2>Why nothing here is referenced unguarded</h2>
 *
 * Both this class and {@link ChecklistBookElement} name types from the addon, which
 * the JVM resolves at class-load time whether or not the code runs. Calling
 * {@link #init()} only after a mod-presence check is what keeps the dependency
 * genuinely optional - the classes are never loaded, so they never fail to link.
 */
public final class TweaksBookCompat {

    public static void init() {
        NeoForge.EVENT_BUS.addListener(TweaksBookCompat::onBookElement);
        ColonyHandover.LOGGER.info("MineColonies Tweaks found - checklists can go in the book.");
    }

    private static void onBookElement(ResourceScrollBookElementEvent event) {
        if (event.getStack().is(ColonyHandover.CHECKLIST_SCROLL.get())) {
            event.register(new ChecklistBookElement(event.getStack()));
        }
    }

    private TweaksBookCompat() {}
}
