package net.reimaden.touhouorigins.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.reimaden.touhouorigins.power.FlipModelPower;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@code flip_view} — inverts the holder's own camera, not just how others see them.
 *
 * <p>Voile multiplied a 180° Z rotation onto the {@code MatrixStack} that 1.20's {@code renderWorld}
 * received. 1.21's {@code renderLevel(DeltaTracker)} takes no matrix at all, so that approach does not
 * translate. It does still build a view-rotation matrix internally:
 *
 * <pre>
 *   Quaternionf q = camera.rotation().conjugate(new Quaternionf());
 *   Matrix4f matrix4f1 = new Matrix4f().rotation(q);   // ← wrapped here
 * </pre>
 *
 * <p>That matrix is what goes on to the level renderer and the held item, so pre-multiplying a 180°
 * roll onto it rolls the whole rendered view. {@code rotateLocalZ} pre-multiplies, which is the
 * order that produces a roll of the final image rather than a rotation in world space.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererFlipViewMixin {

    @Shadow public abstract Camera getMainCamera();

    @ModifyExpressionValue(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
        at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotation(Lorg/joml/Quaternionfc;)Lorg/joml/Matrix4f;"))
    private Matrix4f touhouorigins$flipView(Matrix4f original) {
        Entity entity = this.getMainCamera().getEntity();
        if (entity == null) return original;
        if (FlipModelPower.isVanillaFlipped(entity)) return original;
        if (!FlipModelPower.shouldFlipView(entity)) return original;
        return original.rotateLocalZ((float) Math.PI);
    }
}
