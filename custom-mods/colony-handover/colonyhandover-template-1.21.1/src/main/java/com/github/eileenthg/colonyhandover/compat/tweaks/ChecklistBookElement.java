package com.github.eileenthg.colonyhandover.compat.tweaks;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import com.github.eileenthg.colonyhandover.ColonyHandoverClient;
import com.github.eileenthg.colonyhandover.scroll.ColonyIssues;
import com.ldtteam.blockui.Pane;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import steve_gall.minecolonies_tweaks.core.common.config.MCTweaksConfigClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import steve_gall.minecolonies_tweaks.api.client.gui.ResourceScrollBookElement;

/**
 * One row of the Tweaks addon's Resource Scroll Book, showing a bound checklist.
 *
 * <h2>Why this class is isolated</h2>
 *
 * It extends a type from another mod, which cannot be done reflectively - the
 * reference is baked into the class file. So the class must never be loaded when
 * that mod is absent, or the JVM fails to link it. Keeping it in its own class,
 * referenced only from code guarded by a mod-presence check, is what makes the
 * dependency genuinely optional rather than optional-looking.
 *
 * <h2>What it shows</h2>
 *
 * Three lines is all the book gives each row, so they have to be the three that
 * matter: which building, what it is becoming, and what is still missing. The
 * shortfall goes last because it is the only line that changes as you work.
 */
public final class ChecklistBookElement extends ResourceScrollBookElement {

    public ChecklistBookElement(ItemStack stack) {
        super(stack);
    }

    @Override
    public void update(int index, Pane pane) {
        final ColonyIssues issues = stack.get(ColonyHandover.COLONY_ISSUES.get());
        // The base update ends with getOpenButton(pane).setVisible(valid), and valid
        // starts false - so a row that never sets it has no Open button at all.
        valid = issues != null;
        super.update(index, pane);

        if (issues == null) {
            getDesc1Label(pane).setText(Component.literal("Unbound checklist"));
            getDesc2Label(pane).setText(Component.literal("Sneak + use inside a colony"));
            getDesc3Label(pane).setText(Component.empty());
            return;
        }

        final int outstanding = issues.outstanding().size();
        getResourceIcon(pane).setItem(stack);
        getDesc1Label(pane).setText(Component.literal(issues.colonyName()));
        getDesc2Label(pane).setText(Component.literal(
                        issues.buildings().size() + " building(s) surveyed")
                .withStyle(ChatFormatting.DARK_GRAY));
        getDesc3Label(pane).setText(Component.literal(outstanding == 0
                        ? "Nothing outstanding"
                        : outstanding + " need attention")
                .withStyle(outstanding == 0 ? ChatFormatting.GREEN : ChatFormatting.AQUA));
    }

    /**
     * Opens the same screen the item does.
     *
     * <p>No server round trip: the row is drawn from a stack the client already
     * holds, and that stack carries the whole survey. Reading it from the book and
     * reading it from the hand are the same operation on the same data.
     */
    @Override
    public void onOpenClicked() {
        if (stack.get(ColonyHandover.COLONY_ISSUES.get()) != null) {
            // Back to the book on Close/Escape, like the book's own rows - and under
            // the same Tweaks client setting, so turning it off turns it off here too.
            final Screen book = MCTweaksConfigClient.INSTANCE.escToReturn.get()
                    ? Minecraft.getInstance().screen
                    : null;
            ColonyHandoverClient.openChecklist(stack, book);
        }
    }
}
