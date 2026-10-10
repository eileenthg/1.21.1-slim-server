package eteil.mcaanimcompat.mixin;

import com.zigythebird.playeranim.accessors.IAnimatedPlayer;
import com.zigythebird.playeranim.accessors.IMutableModel;
import com.zigythebird.playeranim.animation.PlayerAnimManager;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.conczin.mca.client.model.PlayerEntityExtendedModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes Player Animation Library animations (Take a Seat's sit, emotes, and every other PAL animation)
 * play on MCA's own player models.
 * <p>
 * PAL hooks {@code PlayerModel}'s synthetic bridge {@code setupAnim(Entity, ...)}: it resets the pose at
 * HEAD and applies the running animation at RETURN. MCA replaces the renderer's model with
 * {@link PlayerEntityExtendedModel} for the PLAYER and VILLAGER body types, and that subclass declares its
 * own {@code setupAnim} (plus its own bridge) and calls {@code super.setupAnim(T, ...)} — the real method,
 * not the bridge. Virtual dispatch therefore never reaches PlayerModel's bridge, so PAL's two injections
 * never run and the animation is silently dropped. Only the VANILLA body type animates.
 * <p>
 * This mixin repeats PAL's own logic on MCA's model, in the same order relative to the pose MCA builds.
 * Wear parts (sleeves, pants, jacket) need no handling here: MCA re-syncs them from the animated parts
 * while rendering, and the breast parts are drawn inside the body part's transform.
 */
@Mixin(value = PlayerEntityExtendedModel.class, remap = false)
public abstract class McaPlayerModelAnimMixin<T extends LivingEntity> extends PlayerModel<T> {
    @Unique
    private final PlayerAnimBone mcaanimcompat$head = new PlayerAnimBone("head");
    @Unique
    private final PlayerAnimBone mcaanimcompat$torso = new PlayerAnimBone("torso");
    @Unique
    private final PlayerAnimBone mcaanimcompat$rightArm = new PlayerAnimBone("right_arm");
    @Unique
    private final PlayerAnimBone mcaanimcompat$leftArm = new PlayerAnimBone("left_arm");
    @Unique
    private final PlayerAnimBone mcaanimcompat$rightLeg = new PlayerAnimBone("right_leg");
    @Unique
    private final PlayerAnimBone mcaanimcompat$leftLeg = new PlayerAnimBone("left_leg");

    protected McaPlayerModelAnimMixin(ModelPart root, boolean slim) {
        super(root, slim);
    }

    /**
     * An animation can move and scale parts, and vanilla's setupAnim only ever sets rotations, so without
     * this the last frame of a finished animation would stick. PAL does the same at its own HEAD hook.
     */
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void mcaanimcompat$resetPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                         float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer)) return;
        head.resetPose();
        body.resetPose();
        rightArm.resetPose();
        leftArm.resetPose();
        rightLeg.resetPose();
        leftLeg.resetPose();
        hat.resetPose();
        jacket.resetPose();
        rightSleeve.resetPose();
        leftSleeve.resetPose();
        rightPants.resetPose();
        leftPants.resetPose();
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void mcaanimcompat$applyAnimation(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                              float netHeadYaw, float headPitch, CallbackInfo ci) {
        IMutableModel self = (IMutableModel) this;
        if (!(entity instanceof AbstractClientPlayer player)) {
            self.playerAnimLib$setAnimation(null);
            return;
        }
        IAnimatedPlayer animated = (IAnimatedPlayer) player;
        PlayerAnimManager animation = animated.playerAnimLib$getAnimManager();
        if (animated.playerAnimLib$getAnimProcessor() == null || animation == null || !animation.isActive()) {
            self.playerAnimLib$setAnimation(null);
            return;
        }

        self.playerAnimLib$setAnimation(animation);
        RenderUtil.copyVanillaPart(head, mcaanimcompat$head);
        RenderUtil.copyVanillaPart(body, mcaanimcompat$torso);
        RenderUtil.copyVanillaPart(rightArm, mcaanimcompat$rightArm);
        RenderUtil.copyVanillaPart(leftArm, mcaanimcompat$leftArm);
        RenderUtil.copyVanillaPart(rightLeg, mcaanimcompat$rightLeg);
        RenderUtil.copyVanillaPart(leftLeg, mcaanimcompat$leftLeg);
        animation.updatePart(head, hat, mcaanimcompat$head);
        animation.updatePart(rightArm, rightSleeve, mcaanimcompat$rightArm);
        animation.updatePart(leftArm, leftSleeve, mcaanimcompat$leftArm);
        animation.updatePart(rightLeg, rightPants, mcaanimcompat$rightLeg);
        animation.updatePart(leftLeg, leftPants, mcaanimcompat$leftLeg);
        animation.updatePart(body, jacket, mcaanimcompat$torso);
    }
}
