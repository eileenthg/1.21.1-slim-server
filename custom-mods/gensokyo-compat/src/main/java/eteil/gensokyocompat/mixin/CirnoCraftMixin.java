package eteil.gensokyocompat.mixin;

import eteil.gensokyocompat.Bridge;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy's Cirno crafts Legacy fairy ice; hand out YH's instead. */
@Mixin(targets = "dev.xkmc.gensokyolegacy.content.entity.characters.fairy.CirnoEntity", remap = false)
public abstract class CirnoCraftMixin {
    @Inject(method = "doCraft", at = @At("RETURN"), cancellable = true)
    private void gensokyocompat$yhIce(boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack s = cir.getReturnValue();
        if (s != null && !s.isEmpty()) cir.setReturnValue(s.transmuteCopy(Bridge.toYH(s.getItem()), s.getCount()));
    }
}
