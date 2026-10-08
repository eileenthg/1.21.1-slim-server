package eteil.gensokyocompat.mixin;

import eteil.gensokyocompat.Bridge;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Legacy rewards its own hat for blocking Koishi; give the YH hat instead. */
@Mixin(targets = "dev.xkmc.gensokyolegacy.content.attachment.misc.KoishiAttackCapability", remap = false)
public abstract class KoishiHatRewardMixin {
    @ModifyArg(method = "onBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemLike gensokyocompat$yhHat(ItemLike item) {
        return Bridge.toYH(item);
    }
}
