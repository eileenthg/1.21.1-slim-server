package eteil.mcaanimcompat;

import com.zigythebird.playeranim.accessors.IAnimatedPlayer;
import com.zigythebird.playeranim.animation.PlayerAnimManager;
import com.takeaseat.client.TakeASeatClient;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;

/**
 * Makes looking around while lying down with Take a Seat work like a real head on a pillow.
 * <p>
 * The player's look direction stays a real world direction (so the crosshair, and everything else, aims where
 * the view points), and three things are derived from it through the lying body's frame (body yaw, then 90°
 * about the hip, which puts the head behind the feet, face up):
 * <ul>
 *   <li>the model's head ({@link #headRotation}): swung from resting on the pillow toward the look, like a neck,
 *       so looking at the ceiling is a resting head and looking horizontally ahead is chin-to-chest, at the feet;</li>
 *   <li>the first-person camera: the head's orientation, which also gives the side pose its roll;</li>
 *   <li>the automatic pivot: on lying down the real aim sweeps to "straight ahead of the head", i.e. the
 *       ceiling, or sideways for a side pose, and on standing up the pitch sweeps back to what it was.</li>
 * </ul>
 * Only loaded when both Take a Seat and PAL are installed.
 */
public final class LyingCamera {
    private static final float DEG = Mth.DEG_TO_RAD;
    private static final float VANILLA_EYE_HEIGHT = 1.62F;
    /** Take a Seat's lying poses drop the root 10 px (0.625 block) and pivot at a 0.75-block hip. */
    private static final float HIP = 0.75F;
    private static final float LYING_DROP = 10.0F / 16.0F;
    private static final float PIVOT_SECONDS = 0.6F;
    private static final Vector3f FORWARD = new Vector3f(0.0F, 0.0F, -1.0F);

    private enum Kind { NONE, BACK, SIDE }

    private static float lie;
    private static Kind kind = Kind.NONE;
    private static long lastNanos;
    private static String lastPose = "";
    private static Vec3 positionOffset = Vec3.ZERO;
    private static float heldBody = Float.NaN;
    private static int driftReports;

    // Automatic pivot of the real aim: a sweep from the look at lie-down to straight ahead of the fully lying
    // head. The target is fixed at the start, in world terms, so the body turning under a sideways look
    // (vanilla's 50° rule) doesn't make it run away.
    private static float pivotT = 1.0F;
    private static final Vector3f pivotFrom = new Vector3f();
    private static final Vector3f pivotTo = new Vector3f();
    // Standing back up: the pitch sweeps back to the one from before lying down.
    private static float savedPitch;
    private static float standT = 1.0F;
    private static float standP0;

    private LyingCamera() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LyingCamera::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(LyingCamera::onRenderHand);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> holdSideBody("tick"));
        NeoForge.EVENT_BUS.addListener((RenderPlayerEvent.Pre e) -> {
            if (e.getEntity() instanceof LocalPlayer) holdSideBody("render");
        });
    }

    /**
     * Lying on the side, the local player's body yaw is held where it was when the side pose began, and only
     * moves when the head turns past sideHeadTurnDegrees (then it follows, as vanilla's rule does at 50°).
     * Raising the head-turn limit ({@link #maxHeadTurn}) covers vanilla's own step, but something else in this pack
     * still nudges the body 40° toward the head now and then (source not found), so the held yaw is re-applied
     * every tick and before the player is drawn.
     */
    private static void holdSideBody(String phase) {
        LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null || kind != Kind.SIDE || !ClientConfig.LYING_CAMERA.get()) {
            heldBody = Float.NaN;
            return;
        }
        if (Float.isNaN(heldBody)) heldBody = player.yBodyRot;
        float limit = ClientConfig.SIDE_HEAD_TURN.get().floatValue();
        float diff = Mth.wrapDegrees(player.getYRot() - heldBody);
        if (Math.abs(diff) > limit) heldBody += diff - Math.signum(diff) * limit;
        float drift = Mth.wrapDegrees(player.yBodyRot - heldBody);
        if (Math.abs(drift) > 1.0F && driftReports < 5) {
            driftReports++;
            McaAnimCompat.LOG.debug("[MCA Animation Compat] side pose: body yaw moved {} deg by {} (held at {}, head {})",
                    String.format("%.1f", drift), phase, String.format("%.1f", heldBody), String.format("%.1f", player.getYRot()));
        }
        player.yBodyRot = heldBody;
        player.yBodyRotO = heldBody;
    }

    /** 0 = upright, 1 = fully lying. Take a Seat's own sit camera drop fades out by this much. */
    public static float progress() {
        return lie / 90.0F;
    }

    /** Where the camera goes relative to the standing eye; applied at Camera.setup TAIL. */
    public static Vec3 positionOffset() {
        return positionOffset;
    }

    /**
     * The model's head rotation for a player lying in a Take a Seat bed pose, as ModelPart angles
     * {xRot, yRot, zRot} in radians. The head swings from resting on the pillow toward the look direction
     * along the shortest arc (no twist about the look axis, the way a neck moves) and stops at the neck limit.
     * A yaw-then-pitch head, like vanilla's, rolls onto its ear when glancing sideways near the feet.
     * Uses the body angle the animation is drawing this frame, so it is right for every player on screen.
     *
     * @return false (leave the head alone) when the player is not in a lying pose.
     */
    public static boolean headRotation(AbstractClientPlayer player, float partialTick, Vector3f out) {
        if (!ClientConfig.LYING_CAMERA.get() || kindOf(poseOf(player)) == Kind.NONE) return false;
        PlayerAnimManager manager = ((IAnimatedPlayer) player).playerAnimLib$getAnimManager();
        if (manager == null || !manager.isActive()) return false;
        float bodyLie = Math.min(Math.abs(manager.get3DTransform(new PlayerAnimBone("body")).getRotX()) / DEG, 90.0F);
        if (bodyLie < 0.5F) return false;
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        Quaternionf swing = swing(toLocal(look(headYaw, pitch), bodyFrame(bodyYaw, bodyLie)),
                ClientConfig.NECK_LIMIT.get().floatValue() * DEG);
        // Body-frame rotation of a ModelPart (model space is flipped in x and y) is Rz(z)·Ry(-y)·Rx(-x).
        Vector3f e = swing.getEulerAnglesZYX(new Vector3f());
        out.set(-e.x, -e.y, e.z);
        return true;
    }

    /** Head-to-body turn limit for {@code player}: raised in a side pose so the lying body stays put, else vanilla's. */
    public static float maxHeadTurn(AbstractClientPlayer player, float vanilla) {
        if (!ClientConfig.LYING_CAMERA.get() || kindOf(poseOf(player)) != Kind.SIDE) return vanilla;
        // A hair over the limit, so a look set exactly 90° off doesn't nudge the body by rounding error.
        return Math.max(vanilla, ClientConfig.SIDE_HEAD_TURN.get().floatValue() + 0.01F);
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Camera camera = event.getCamera();
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0.0F : Math.min((now - lastNanos) / 1.0E9F, 0.25F);
        lastNanos = now;

        LocalPlayer player = camera.getEntity() instanceof LocalPlayer p ? p : null;
        if (player == null || !ClientConfig.LYING_CAMERA.get()) {
            lie = 0.0F;
            kind = Kind.NONE;
            pivotT = standT = 1.0F;
            positionOffset = Vec3.ZERO;
            return;
        }
        String pose = poseOf(player);
        if (!pose.equals(lastPose)) {
            McaAnimCompat.LOG.info("[MCA Animation Compat] Take a Seat pose: '{}'", pose);
            lastPose = pose;
        }
        // The pose tracking and the aim pivot run in any view (the head is posed from the aim either way);
        // only the camera itself is first-person only.
        Kind target = kindOf(pose);

        float bodyYaw = Mth.rotLerp((float) event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        if (target != kind) {
            if (kind == Kind.NONE) savedPitch = player.getXRot();
            if (target == Kind.NONE) {
                standT = 0.0F;
                standP0 = player.getXRot();
                pivotT = 1.0F;
            } else {
                float headYaw = target == Kind.SIDE ? -ClientConfig.SIDE_TWIST.get().floatValue() : 0.0F;
                pivotFrom.set(look(player.getYRot(), player.getXRot()));
                heldBody = target == Kind.SIDE ? bodyYaw : Float.NaN;
                bodyFrame(bodyYaw, 90.0F).transform(head(headYaw, 0.0F).transform(pivotTo.set(FORWARD)));
                pivotT = 0.0F;
                standT = 1.0F;
            }
            kind = target;
        }

        float k = 1.0F - (float) Math.exp(-dt * 6.0F);
        lie = approach(lie, kind == Kind.NONE ? 0.0F : 90.0F, k);

        if (pivotT < 1.0F) {
            pivotT = Math.min(pivotT + dt / PIVOT_SECONDS, 1.0F);
            Quaternionf sweep = new Quaternionf().slerp(new Quaternionf().rotationTo(pivotFrom, pivotTo), easeOutCubic(pivotT));
            setLook(player, sweep.transform(new Vector3f(pivotFrom)));
        } else if (standT < 1.0F) {
            standT = Math.min(standT + dt / (PIVOT_SECONDS * 0.7F), 1.0F);
            float p = Mth.lerp(easeOutCubic(standT), standP0, savedPitch);
            player.setXRot(p);
            player.xRotO = p;
        }

        if (lie == 0.0F || camera.isDetached()) {
            positionOffset = Vec3.ZERO;
            return;
        }

        // The camera is the head: the same no-twist swing as the model's head (not limited, so the crosshair
        // always matches the real look), rebuilt through the lying frame, which supplies the roll.
        Quaternionf frame = bodyFrame(bodyYaw, lie);
        Vector3f local = toLocal(look(player.getViewYRot((float) event.getPartialTick()), player.getViewXRot((float) event.getPartialTick())), frame);
        Quaternionf q = new Quaternionf(frame).mul(swing(local, (float) Math.PI));
        // Camera convention (Camera.setRotation): orientation = Ry(π - yaw) · Rx(-pitch) · Rz(-roll), forward -Z.
        Vector3f euler = q.getEulerAnglesYXZ(new Vector3f());
        event.setYaw(180.0F - euler.y / DEG);
        event.setPitch(-euler.x / DEG);
        event.setRoll(-euler.z / DEG);

        positionOffset = ClientConfig.LYING_CAMERA_AT_HEAD.get() ? headOffset(player, bodyYaw) : Vec3.ZERO;
    }

    /** Body frame in camera convention (forward -Z, up +Y): body yaw, then the lie of {@code lieDeg} about the hip. */
    private static Quaternionf bodyFrame(float bodyYaw, float lieDeg) {
        return new Quaternionf().rotationY((float) Math.PI - bodyYaw * DEG).rotateX(lieDeg * DEG);
    }

    /** A head turned {@code yaw} and pitched {@code pitch} relative to its body, the way vanilla poses the head part. */
    private static Quaternionf head(float yaw, float pitch) {
        return new Quaternionf().rotationY(-yaw * DEG).rotateX(-pitch * DEG);
    }

    /** Minecraft's look vector for a yaw and pitch. */
    private static Vector3f look(float yaw, float pitch) {
        float cp = Mth.cos(pitch * DEG);
        return new Vector3f(-Mth.sin(yaw * DEG) * cp, -Mth.sin(pitch * DEG), Mth.cos(yaw * DEG) * cp);
    }

    /** A world direction expressed in {@code frame}'s local axes. */
    private static Vector3f toLocal(Vector3f world, Quaternionf frame) {
        return new Quaternionf(frame).conjugate().transform(new Vector3f(world));
    }

    /** Shortest-arc turn from the resting face direction (local forward) to {@code local}, capped at {@code limit} radians. */
    private static Quaternionf swing(Vector3f local, float limit) {
        Quaternionf q = new Quaternionf().rotationTo(FORWARD, new Vector3f(local).normalize());
        float angle = q.angle();
        return angle > limit ? new Quaternionf().slerp(q, limit / angle) : q;
    }

    private static void setLook(LocalPlayer player, Vector3f v) {
        float pitch = (float) Math.asin(Mth.clamp(-v.y, -1.0F, 1.0F)) / DEG;
        player.setXRot(pitch);
        player.xRotO = pitch;
        // Straight up or down has no yaw of its own; keep the current one rather than a noisy one.
        if (v.x * v.x + v.z * v.z > 1.0E-6F) {
            float yaw = player.getYRot() + Mth.wrapDegrees((float) Math.atan2(-v.x, v.z) / DEG - player.getYRot());
            player.setYRot(yaw);
            player.yRotO = yaw;
            player.setYHeadRot(yaw);
            player.yHeadRotO = yaw;
        }
    }

    /**
     * The lying model's eye relative to the standing eye: the root drops and the body turns about the hip,
     * which swings the eye down and back. Sizes follow eye height, so MCA body height is respected.
     */
    private static Vec3 headOffset(LocalPlayer player, float bodyYaw) {
        float eye = player.getEyeHeight();
        float scale = eye / VANILLA_EYE_HEIGHT;
        float hip = HIP * scale;
        float neckToEye = eye - hip;
        float angle = lie * DEG;
        float up = hip - LYING_DROP * scale * progress() + neckToEye * Mth.cos(angle) - eye;
        float back = neckToEye * Mth.sin(angle);
        Vector3f offset = new Vector3f(0.0F, up, back).rotateY((float) Math.PI - bodyYaw * DEG);
        return new Vec3(offset.x, offset.y, offset.z);
    }

    private static void onRenderHand(RenderHandEvent event) {
        if (lie > 20.0F && ClientConfig.LYING_CAMERA.get() && ClientConfig.LYING_HIDE_HANDS.get()) event.setCanceled(true);
    }

    private static String poseOf(AbstractClientPlayer player) {
        if (player instanceof LocalPlayer && !TakeASeatClient.isSitting()) return "";
        if (!(PlayerAnimationAccess.getPlayerAnimationLayer(player, TakeASeatClient.SIT_LAYER) instanceof PlayerAnimationController controller)
                || !controller.isActive()) return "";
        Animation animation = controller.getCurrentAnimationInstance();
        if (animation == null) return "";
        String name = animation.getNameOrId().toLowerCase(Locale.ROOT);
        int colon = name.lastIndexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    private static Kind kindOf(String pose) {
        if (matches(ClientConfig.BACK_POSES.get(), pose)) return Kind.BACK;
        if (matches(ClientConfig.SIDE_POSES.get(), pose)) return Kind.SIDE;
        return Kind.NONE;
    }

    private static boolean matches(List<? extends String> names, String pose) {
        if (pose.isEmpty()) return false;
        for (String n : names) {
            if (n.equalsIgnoreCase(pose)) return true;
        }
        return false;
    }

    private static float easeOutCubic(float t) {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    private static float approach(float value, float target, float k) {
        float next = value + (target - value) * k;
        return Math.abs(target - next) < 0.05F ? target : next;
    }
}
