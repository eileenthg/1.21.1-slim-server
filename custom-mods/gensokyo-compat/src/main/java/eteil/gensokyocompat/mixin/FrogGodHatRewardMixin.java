package eteil.gensokyocompat.mixin;

import eteil.gensokyocompat.Bridge;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Legacy rewards its own hat for feeding the frog god; give the YH hat instead. */
@Mixin(targets = "dev.xkmc.gensokyolegacy.content.attachment.misc.FrogGodCapability", remap = false)
public abstract class FrogGodHatRewardMixin {
    @ModifyArg(method = "eat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/frog/Frog;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemLike gensokyocompat$yhHat(ItemLike item) {
        return Bridge.toYH(item);
    }
}
