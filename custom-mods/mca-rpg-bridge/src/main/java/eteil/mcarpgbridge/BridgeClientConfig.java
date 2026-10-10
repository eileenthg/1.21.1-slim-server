package eteil.mcarpgbridge;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BridgeClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue TRACKER_OFFSET_X;
    private static final ModConfigSpec.IntValue TRACKER_OFFSET_Y;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("questTracker");
        b.comment("Moves Mebahel's quest tracker HUD (normally in the top-left corner, under Xaero's minimap).",
                "GUI pixels at the current GUI scale; positive moves right / down. Editable in game from the mod list.");
        TRACKER_OFFSET_X = b.comment("Horizontal offset.").defineInRange("offsetX", 0, -4000, 4000);
        TRACKER_OFFSET_Y = b.comment("Vertical offset.").defineInRange("offsetY", 0, -4000, 4000);
        b.pop();
        SPEC = b.build();
    }

    private BridgeClientConfig() {
    }

    public static int trackerOffsetX() {
        return SPEC.isLoaded() ? TRACKER_OFFSET_X.get() : TRACKER_OFFSET_X.getDefault();
    }

    public static int trackerOffsetY() {
        return SPEC.isLoaded() ? TRACKER_OFFSET_Y.get() : TRACKER_OFFSET_Y.getDefault();
    }
}
