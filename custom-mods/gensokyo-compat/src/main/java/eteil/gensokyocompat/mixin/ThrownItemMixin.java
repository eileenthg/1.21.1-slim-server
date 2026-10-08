package eteil.gensokyocompat.mixin;

import eteil.gensokyocompat.Bridge;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy's thrown fairy ice and frozen frogs show (and drop) Legacy's items; use YH's. */
@Mixin(targets = {"dev.xkmc.gensokyolegacy.content.entity.misc.FairyIce", "dev.xkmc.gensokyolegacy.content.entity.misc.FrozenFrog"}, remap = false)
public abstract class ThrownItemMixin {
    @Inject(method = "getDefaultItem", at = @At("RETURN"), cancellable = true)
    private void gensokyocompat$yhItem(CallbackInfoReturnable<Item> cir) {
        cir.setReturnValue(Bridge.toYH(cir.getReturnValue()));
    }
}
