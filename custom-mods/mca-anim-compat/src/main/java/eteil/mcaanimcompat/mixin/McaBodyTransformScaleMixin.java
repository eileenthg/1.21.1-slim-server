package eteil.mcaanimcompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.mca.MCAClient;
import net.conczin.mca.client.model.CommonVillagerModel;
import net.conczin.mca.entity.VillagerLike;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes whole-body animation offsets (Take a Seat's sit drop, emote jumps and leans) scale with an MCA
 * player's height and width.
 * <p>
 * Both animation libraries in the pack (PAL and KosmX's playerAnimator) move the whole model at
 * {@code setupRotations} RETURN, in world units: the sit poses drop the root 12 px (0.75 block, one vanilla
 * leg) and pivot it at a fixed 0.75-block hip. MCA shrinks or stretches the body afterwards, in
 * {@code scale()}. So a short MCA body still drops a full vanilla leg length and sinks into the floor.
 * <p>
 * The fix conjugates the animation transform by MCA's scale S: apply S just before the libraries' hooks and
 * S⁻¹ at the head of {@code scale()}, where MCA applies S again. The net pose is S·A·S⁻¹·S = S·A — the
 * animation runs in the model's own units and is then scaled with the body. Everything in between (the
 * render flip and vanilla's 0.9375 player scale) is diagonal, so it commutes with S, and when no animation
 * is running S and S⁻¹ cancel exactly.
 * <p>
 * Priority 500 keeps this RETURN hook ahead of PAL's (priority 2000) and playerAnimator's (default 1000).
 */
@Mixin(value = PlayerRenderer.class, priority = 500)
public abstract class McaBodyTransformScaleMixin {
    @Unique
    private float mcaanimcompat$width = 1.0F;
    @Unique
    private float mcaanimcompat$height = 1.0F;

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("RETURN"))
    private void mcaanimcompat$enterModelScale(AbstractClientPlayer player, PoseStack poseStack, float bob, float bodyYaw,
                                               float partialTick, float scale, CallbackInfo ci) {
        mcaanimcompat$width = 1.0F;
        mcaanimcompat$height = 1.0F;
        // Same condition MCA uses for its own scale in MixinPlayerRenderer.mca$injectScale.
        if (!MCAClient.useGeneticsRenderer(player.getUUID())) return;
        VillagerLike<?> villager = CommonVillagerModel.getVillager(player);
        float width = villager.getRawHorizontalScaleFactor();
        float height = villager.getRawVerticalScaleFactor();
        if (width <= 0.0F || height <= 0.0F) return;
        mcaanimcompat$width = width;
        mcaanimcompat$height = height;
        poseStack.scale(width, height, width);
    }

    @Inject(method = "scale(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
            at = @At("HEAD"))
    private void mcaanimcompat$leaveModelScale(AbstractClientPlayer player, PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (mcaanimcompat$width == 1.0F && mcaanimcompat$height == 1.0F) return;
        poseStack.scale(1.0F / mcaanimcompat$width, 1.0F / mcaanimcompat$height, 1.0F / mcaanimcompat$width);
        mcaanimcompat$width = 1.0F;
        mcaanimcompat$height = 1.0F;
    }
}
