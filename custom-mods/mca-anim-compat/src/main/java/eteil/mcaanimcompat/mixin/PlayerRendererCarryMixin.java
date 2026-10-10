package eteil.mcaanimcompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import eteil.mcaanimcompat.CarryPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the carried player of a Princess Carry from the carrier's frame instead of their own yaw; see {@link CarryPose}. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererCarryMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void mcaanimcompat$carriedRotations(AbstractClientPlayer player, PoseStack pose, float bob, float bodyRot,
                                                 float partialTick, float scale, CallbackInfo ci) {
        if (CarryPose.apply(player, pose, partialTick)) ci.cancel();
    }
}
