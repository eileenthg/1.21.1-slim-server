package eteil.mcaanimcompat;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** config/mcaanimcompat-client.toml */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LYING_CAMERA;
    public static final ModConfigSpec.BooleanValue LYING_CAMERA_AT_HEAD;
    public static final ModConfigSpec.BooleanValue LYING_HIDE_HANDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BACK_POSES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SIDE_POSES;
    public static final ModConfigSpec.DoubleValue SIDE_TWIST;
    public static final ModConfigSpec.DoubleValue NECK_LIMIT;
    public static final ModConfigSpec.DoubleValue SIDE_HEAD_TURN;
    public static final ModConfigSpec.BooleanValue CARRY_POSE;
    public static final ModConfigSpec.DoubleValue CARRY_TORSO_LIFT;
    public static final ModConfigSpec.DoubleValue CARRY_HIP_BEND;
    public static final ModConfigSpec.DoubleValue CARRY_FACE_TURN;
    public static final ModConfigSpec.DoubleValue CARRY_HIP_FORWARD;
    public static final ModConfigSpec.DoubleValue CARRY_HIP_HEIGHT;
    public static final ModConfigSpec.DoubleValue CARRY_HIP_SIDE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("First-person camera while lying down with Take a Seat (on a bed).").push("lyingCamera");
        LYING_CAMERA = b.comment("Treat the lying head like a real head on a pillow. Your aim stays a real direction; the model's",
                        "head is posed relative to the lying body (looking at the ceiling = resting head, looking ahead = at your feet);",
                        "in first person the view is that head's view, and lying down pivots your aim to the ceiling (sideways for",
                        "side poses), standing up puts your pitch back. Off = Take a Seat's original behaviour.")
                .define("enabled", true);
        LYING_CAMERA_AT_HEAD = b.comment("Also move the camera down and back to where the lying model's head is,",
                        "instead of Take a Seat's fixed sit height.")
                .define("moveToHead", true);
        LYING_HIDE_HANDS = b.comment("Hide the first-person hands and held item while lying.")
                .define("hideHands", true);
        BACK_POSES = b.comment("Take a Seat animation names that lie on the back.")
                .defineListAllowEmpty("backPoses", List.of("bedlyingdown", "bedlyingdown2"), () -> "", o -> o instanceof String);
        SIDE_POSES = b.comment("Take a Seat animation names that lie on the side.")
                .defineListAllowEmpty("sidePoses", List.of("bedlyingdown3"), () -> "", o -> o instanceof String);
        SIDE_TWIST = b.comment("How far a side pose turns the head (and your aim) on lying down, in degrees. -90 matches bedlyingdown3, which rolls onto its right side; flip the sign to turn the other way.")
                .defineInRange("sideTwistDegrees", -90.0, -180.0, 180.0);
        NECK_LIMIT = b.comment("How far the lying head can swing away from resting on the pillow, in degrees. Past this the",
                        "head stops and only the view keeps turning.")
                .defineInRange("neckLimitDegrees", 90.0, 0.0, 180.0);
        SIDE_HEAD_TURN = b.comment("In a side pose, how far the head may turn from the body before the body turns with it, in degrees.",
                        "Vanilla (and every other pose) is 50; a side pose faces 90 to the side, so 90 keeps the body still.")
                .defineInRange("sideHeadTurnDegrees", 90.0, 0.0, 180.0);
        b.pop();
        b.comment("How the carried player is drawn in a Princess Carry (MCA Romantic Expansion). Read every frame.").push("princessCarry");
        CARRY_POSE = b.comment("Lay the carried player across the carrier's arms, placed from the carrier and ignoring the carried",
                        "player's own facing, so looking around no longer rolls the body. Off = Romantic Expansion's original pose.")
                .define("enabled", true);
        CARRY_TORSO_LIFT = b.comment("How far the head end of the torso is raised above level, in degrees.")
                .defineInRange("torsoLiftDegrees", 15.0, -45.0, 90.0);
        CARRY_HIP_BEND = b.comment("How far the legs bend up at the hips, in degrees. 0 = a straight plank.")
                .defineInRange("hipBendDegrees", 30.0, 0.0, 120.0);
        CARRY_FACE_TURN = b.comment("How far the body rolls from face-up toward the carrier, in degrees.")
                .defineInRange("faceTurnDegrees", 25.0, -90.0, 90.0);
        CARRY_HIP_FORWARD = b.comment("Hip position in front of the carrier's feet, in blocks.")
                .defineInRange("hipForward", 0.33, -2.0, 2.0);
        CARRY_HIP_HEIGHT = b.comment("Hip height above the carrier's feet, in blocks.")
                .defineInRange("hipHeight", 0.92, -2.0, 3.0);
        CARRY_HIP_SIDE = b.comment("Hip position toward the carrier's left (negative = right), in blocks.")
                .defineInRange("hipSide", 0.15, -2.0, 2.0);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
