package eteil.drinkfix.mixin;

import eteil.drinkfix.DrinkFix;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Plain {@link Item}s only: items with their own class override these methods and are left alone. The drinking
 * sound follows from the DRINK animation (LivingEntity picks the drinking sound for it).
 */
@Mixin(Item.class)
public abstract class ItemMixin {
    @Unique
    private static final ThreadLocal<ItemStack> drinkfix$container = new ThreadLocal<>();

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void drinkfix$drink(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
        if (stack.is(DrinkFix.DRINKS)) cir.setReturnValue(UseAnim.DRINK);
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void drinkfix$remember(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        drinkfix$container.set(stack.is(DrinkFix.DRINKS) ? DrinkFix.containerFor(stack) : ItemStack.EMPTY);
    }

    /** Same hand-back as a vanilla honey bottle: into the emptied slot, else the inventory, else dropped. */
    @Inject(method = "finishUsingItem", at = @At("RETURN"), cancellable = true)
    private void drinkfix$giveBack(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack container = drinkfix$container.get();
        drinkfix$container.remove();
        if (container == null || container.isEmpty() || level.isClientSide) return;
        if (entity instanceof Player player && player.getAbilities().instabuild) return;
        ItemStack left = cir.getReturnValue();
        if (left.isEmpty()) {
            cir.setReturnValue(container);
        } else if (entity instanceof Player player && !player.getInventory().add(container)) {
            player.drop(container, false);
        }
    }
}
