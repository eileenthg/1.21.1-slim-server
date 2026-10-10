package eteil.mcaanimcompat.mixin;

import eteil.mcaanimcompat.Carry;
import eteil.mcaanimcompat.ClientConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Princess Carry (MCA Romantic Expansion) poses, applied last in PlayerModel#setupAnim.
 * <ul>
 *   <li>Carrier: Romantic Expansion raises the arms inside HumanoidModel#setupAnim, but PlayerModel#setupAnim then
 *   runs Not Enough Animations' smoothing over the arms, so the carry pose lags or never settles. The arms are
 *   set again here, after everything else, and the sleeves follow.</li>
 *   <li>Carried: the player is a passenger, so vanilla bends the legs into a sitting pose and lets the head turn
 *   with the camera, while the body is laid across the carrier's arms (CarryPose). The legs get a gentle bend at
 *   the hips (hipBendDegrees) and the head stays in line with the body, whichever way the player looks.</li>
 * </ul>
 * Runs after Player Animation Library (priority 2001) so a carry is not overridden by an idle animation.
 */
@Mixin(value = PlayerModel.class, priority = 3000)
public abstract class PlayerModelCarryMixin<T extends LivingEntity> extends HumanoidModel<T> {
    @Shadow
    @Final
    public ModelPart leftSleeve;
    @Shadow
    @Final
    public ModelPart rightSleeve;
    @Shadow
    @Final
    public ModelPart leftPants;
    @Shadow
    @Final
    public ModelPart rightPants;

    protected PlayerModelCarryMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void mcaanimcompat$carryPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                          float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        if (Carry.isCarrier(player.getUUID())) {
            rightArm.xRot = Carry.RIGHT_ARM_X;
            rightArm.yRot = Carry.RIGHT_ARM_Y;
            rightArm.zRot = 0.0F;
            leftArm.xRot = Carry.LEFT_ARM_X;
            leftArm.yRot = Carry.LEFT_ARM_Y;
            leftArm.zRot = 0.0F;
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
        }
        if (Carry.isCarried(player.getUUID())) {
            float bend = (float) Math.toRadians(ClientConfig.CARRY_HIP_BEND.get());
            rightLeg.xRot = -bend;
            leftLeg.xRot = -bend * 0.85F;
            rightLeg.yRot = 0.05F;
            leftLeg.yRot = -0.05F;
            rightLeg.zRot = 0.03F;
            leftLeg.zRot = -0.03F;
            head.xRot = 0.0F;
            head.yRot = 0.0F;
            head.zRot = 0.0F;
            hat.copyFrom(head);
            rightPants.copyFrom(rightLeg);
            leftPants.copyFrom(leftLeg);
        }
    }
}
