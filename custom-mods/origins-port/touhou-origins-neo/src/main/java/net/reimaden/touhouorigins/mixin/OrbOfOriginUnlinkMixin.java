package net.reimaden.touhouorigins.mixin;

import dev.overgrown.origins.item.OrbOfOriginItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.reimaden.touhouorigins.power.ActionOnOriginGuiPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@code touhouorigins:action_on_origin_gui} when the Orb of Origin is used.
 *
 * <p>One of the two triggers upstream had, and a faithful copy of its {@code OrbOfOriginItemMixin}.
 *
 * <p>This replaces an earlier single mixin on {@code OriginsServerNetwork.openChooseScreen}, which
 * looked like a tidier choke point and was in fact wrong: <i>every</i> route to the choose screen
 * passes through it, including {@code OriginsServerEvents} opening it at login for any layer the
 * player has not picked yet, and {@code advanceOrClose} chaining from one layer to the next. With
 * the blessings layer added there are now two layers to choose, so a shikigami who had not picked a
 * blessing would have had their contract quietly dissolved every time they logged in.
 */
@Mixin(OrbOfOriginItem.class)
public class OrbOfOriginUnlinkMixin {

    @Inject(method = "use", at = @At("HEAD"))
    private void touhouorigins$invokeActions(Level level, Player user, InteractionHand hand,
                                             CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ActionOnOriginGuiPower.onGuiOpen(user);
    }
}
