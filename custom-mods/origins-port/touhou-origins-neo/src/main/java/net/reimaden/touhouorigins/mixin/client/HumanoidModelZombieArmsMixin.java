package net.reimaden.touhouorigins.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import net.reimaden.touhouorigins.power.SimplePowers;
import dev.overgrown.apoli.power.PowerLookup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code touhouorigins:zombie_arms} — arms held straight out in front, the jiang-shi pose.
 *
 * <p>Runs at the tail of {@code setupAnim} so it overrides whatever pose vanilla just computed.
 * The two constants are Voile's: the existing pitch is damped to 5% and offset by about -81°, which
 * keeps a trace of the walk animation rather than locking the arms rigid.
 *
 * <p>Skipped only while swimming, since that pose drives the arms itself and overriding it looks
 * broken. Elytra-flying keeps the pose too, at Eileen's request — arms locked forward while gliding
 * reads as comically consistent with the jiang-shi bit rather than broken.
 *
 * <p>Item use is skipped except for {@code EAT} and {@code DRINK} — raising a hand to the mouth
 * looks odd on a jiang-shi regardless of whether it's food, a potion, or milk, so the pose stays
 * locked through all three rather than dropping to vanilla's animation. Checked via
 * {@code ItemStack.getUseAnimation()} rather than a data component: 1.21.1 predates the unified
 * Consumable component (that landed in 1.21.2), so food, potions and milk are still three separate
 * item classes with no shared "this is edible" marker to query — the use-animation enum is the one
 * thing all three agree on.
 *
 * <p>Note this is <b>not</b> {@code apoli:show_both_arms}, which the type sweep flagged as a
 * lookalike: that one hooks first-person item rendering, this one the third-person biped model.
 */
@Mixin(HumanoidModel.class)
public class HumanoidModelZombieArmsMixin {

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    private static final float DAMPING = 0.05F;
    private static final float FORWARD = 1.4137167F;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void touhouorigins$zombieArms(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                          float ageInTicks, float netHeadYaw, float headPitch,
                                          CallbackInfo ci) {
        if (!PowerLookup.hasActive(entity, SimplePowers.ZOMBIE_ARMS)) return;
        if (entity.isVisuallySwimming()) return;
        if (entity.isUsingItem()) {
            UseAnim anim = entity.getUseItem().getUseAnimation();
            if (anim != UseAnim.EAT && anim != UseAnim.DRINK) return;
        }

        this.rightArm.xRot = this.rightArm.xRot * DAMPING - FORWARD;
        this.leftArm.xRot = this.leftArm.xRot * DAMPING - FORWARD;
    }
}
