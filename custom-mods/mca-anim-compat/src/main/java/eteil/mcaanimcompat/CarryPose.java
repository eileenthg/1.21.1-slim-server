package eteil.mcaanimcompat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Where the carried player's body is drawn during a Princess Carry.
 * <p>
 * Romantic Expansion lays the carried player down at {@code PlayerRenderer.render} HEAD, but vanilla then turns
 * the body by the carried player's own body yaw inside that frame. Once the body is lying down that yaw axis runs
 * along the spine, so every head turn rolls the whole body over (the "spasm"). Its position also follows the
 * carried player's entity, which Romantic Expansion only moves beside the carrier on the host; on every other
 * client the entity sits on the carrier's head.
 * <p>
 * This replaces the whole thing in {@code setupRotations}: Romantic Expansion's transform is undone, and the body
 * is placed in the carrier's frame (hips in front of the carrier at forearm height, head toward the carrier's
 * right, face up and turned toward the carrier), ignoring the carried player's own yaw.
 */
public final class CarryPose {
    /** Hip joint height in the model frame after vanilla's flip, 0.9375 scale and 1.501 drop, in blocks. */
    private static final float HIP_HEIGHT = (1.501F - 0.75F) * 0.9375F;

    private CarryPose() {
    }

    /** @return true if the carried pose was applied and vanilla's body rotation must be skipped. */
    public static boolean apply(AbstractClientPlayer player, PoseStack pose, float partialTick) {
        if (!Carry.isCarried(player.getUUID()) || !ClientConfig.CARRY_POSE.get()) return false;
        Player carrier = Carry.carrierOf(player.getUUID());

        // Undo Romantic Expansion's transform (PlayerCarryPoseRendererMixin, 1.1.4), computed exactly as it does.
        Player yawSource = carrier != null ? carrier : player;
        float reYaw = yawSource.yBodyRotO + (yawSource.yBodyRot - yawSource.yBodyRotO) * partialTick;
        Matrix4f re = new Matrix4f()
                .rotateY((float) Math.toRadians(-reYaw))
                .rotateX((float) Math.toRadians(90.0))
                .rotateZ((float) Math.toRadians(90.0))
                .rotateZ((float) Math.toRadians(-5.0))
                .translate(0.2F, -1.2F, 0.3F);
        pose.mulPose(re.invert());

        // Move from the carried player's render origin to the carrier's feet, facing the carrier's way.
        float yaw = Mth.rotLerp(partialTick, yawSource.yBodyRotO, yawSource.yBodyRot);
        double px = Mth.lerp(partialTick, player.xOld, player.getX());
        double py = Mth.lerp(partialTick, player.yOld, player.getY());
        double pz = Mth.lerp(partialTick, player.zOld, player.getZ());
        if (carrier != null) {
            pose.translate(Mth.lerp(partialTick, carrier.xOld, carrier.getX()) - px,
                    Mth.lerp(partialTick, carrier.yOld, carrier.getY()) - py,
                    Mth.lerp(partialTick, carrier.zOld, carrier.getZ()) - pz);
        }
        // Carrier frame: +Z forward, +X to the carrier's left, +Y up.
        pose.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-yaw)));
        pose.translate(ClientConfig.CARRY_HIP_SIDE.get().floatValue(), ClientConfig.CARRY_HIP_HEIGHT.get().floatValue(),
                ClientConfig.CARRY_HIP_FORWARD.get().floatValue());
        pose.mulPose(bodyRotation());
        pose.translate(0.0F, -HIP_HEIGHT, 0.0F);
        return true;
    }

    /**
     * Turns the model frame (spine along +Y, face toward -Z) so the spine points to the carrier's right, raised by
     * the torso lift, and the face points up, rolled toward the carrier by the face turn.
     */
    private static Quaternionf bodyRotation() {
        float lift = (float) Math.toRadians(ClientConfig.CARRY_TORSO_LIFT.get());
        float turn = (float) Math.toRadians(ClientConfig.CARRY_FACE_TURN.get());
        Vector3f spine = new Vector3f(-Mth.cos(lift), Mth.sin(lift), 0.0F);
        Vector3f up = new Vector3f(Mth.sin(lift), Mth.cos(lift), 0.0F);
        Vector3f face = up.mul(Mth.cos(turn)).add(0.0F, 0.0F, -Mth.sin(turn));
        Vector3f back = face.negate(new Vector3f());
        Vector3f side = spine.cross(back, new Vector3f());
        return new Quaternionf().setFromNormalized(new Matrix3f(side, spine, back));
    }
}
