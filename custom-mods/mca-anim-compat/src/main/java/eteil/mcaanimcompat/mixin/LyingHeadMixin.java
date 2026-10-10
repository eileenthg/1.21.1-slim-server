package eteil.mcaanimcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import eteil.mcaanimcompat.LyingCamera;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While a player lies in a Take a Seat bed pose, the model's head is posed from their look relative to the
 * lying body ({@link LyingCamera#headRotation}), instead of vanilla's world-relative angles.
 * Looking at the ceiling is then a neutral head on the pillow rather than one bent back over it, and looking
 * horizontally ahead tucks the chin toward the feet. Applies to every player drawn, not just the local one.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LyingHeadMixin {
    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    private void mcaanimcompat$lyingHead(EntityModel<Entity> model, Entity entity, float limbSwing, float limbSwingAmount,
                                         float ageInTicks, float netHeadYaw, float headPitch, Operation<Void> original,
                                         LivingEntity living, float entityYaw, float partialTick, PoseStack poseStack,
                                         MultiBufferSource buffer, int packedLight) {
        original.call(model, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // Set after the whole setupAnim (PAL's animation included): the lying head needs a tilt (zRot) as well,
        // which vanilla's yaw/pitch arguments can't express.
        if (entity instanceof AbstractClientPlayer player && model instanceof HumanoidModel<?> humanoid) {
            Vector3f rot = new Vector3f();
            if (LyingCamera.headRotation(player, partialTick, rot)) {
                humanoid.head.setRotation(rot.x, rot.y, rot.z);
                humanoid.hat.copyFrom(humanoid.head);
            }
        }
    }
}
