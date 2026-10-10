package eteil.mcaanimcompat.mixin;

import com.takeaseat.TakeASeatConfig;
import eteil.mcaanimcompat.LyingCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Take a Seat lowers the camera by a fixed {@code cameraFocusOffset} (0.55 block) while sitting, sized for a
 * vanilla player. MCA scales a player's eye height with their body, and {@link McaBodyTransformScaleMixin}
 * now scales the sit drop the same way, so the camera drop has to follow: it is set to the configured value
 * times the camera entity's eye height over vanilla's 1.62, just before Take a Seat's own hook at
 * {@code Camera.setup} TAIL reads it.
 * <p>
 * While lying, {@link LyingCamera} places the eye at the lying model's head instead, so Take a Seat's drop
 * fades out as the lying pose fades in, and the head offset is added at TAIL.
 */
@Mixin(Camera.class)
public abstract class TakeASeatCameraScaleMixin {
    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Unique
    private static final float VANILLA_EYE_HEIGHT = 1.62F;
    @Unique
    private static double mcaanimcompat$configuredOffset = Double.NaN;

    @Inject(method = "setup", at = @At("HEAD"))
    private void mcaanimcompat$scaleSitCameraDrop(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse,
                                                  float partialTick, CallbackInfo ci) {
        TakeASeatConfig cfg = TakeASeatConfig.getConfig();
        if (Double.isNaN(mcaanimcompat$configuredOffset)) mcaanimcompat$configuredOffset = cfg.cameraFocusOffset;
        float ratio = 1.0F;
        if (entity instanceof Player player && player.getPose() == Pose.STANDING) {
            ratio = Math.clamp(player.getEyeHeight() / VANILLA_EYE_HEIGHT, 0.3F, 2.0F);
        }
        float lying = LyingCamera.positionOffset() == Vec3.ZERO ? 0.0F : LyingCamera.progress();
        cfg.cameraFocusOffset = mcaanimcompat$configuredOffset * ratio * (1.0F - lying);
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void mcaanimcompat$moveToLyingHead(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse,
                                               float partialTick, CallbackInfo ci) {
        Vec3 offset = LyingCamera.positionOffset();
        if (!detached && offset != Vec3.ZERO) setPosition(getPosition().add(offset));
    }
}
