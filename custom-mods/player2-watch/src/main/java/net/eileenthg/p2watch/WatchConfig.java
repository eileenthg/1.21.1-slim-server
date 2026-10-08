package net.eileenthg.p2watch;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WatchConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> URL;
    public static final ModConfigSpec.IntValue OFFLINE_CHECK_SECONDS;
    public static final ModConfigSpec.IntValue ONLINE_CHECK_SECONDS;
    public static final ModConfigSpec.ConfigValue<String> APP_PATH;
    public static final ModConfigSpec.BooleanValue TITLE_TOAST;
    public static final ModConfigSpec.BooleanValue JOIN_MESSAGE;
    public static final ModConfigSpec.BooleanValue CONNECT_MESSAGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        URL = b.comment("Player2 app address. Empty = use MCA's player2Url, else http://127.0.0.1:4315/")
                .define("url", "");
        OFFLINE_CHECK_SECONDS = b.comment("While Player2 is offline: check this often (seconds)")
                .defineInRange("offlineCheckSeconds", 5, 1, 600);
        ONLINE_CHECK_SECONDS = b.comment("While Player2 is online: check this often (seconds), to notice it closing")
                .defineInRange("onlineCheckSeconds", 30, 5, 3600);
        APP_PATH = b.comment("player2.exe for the [Start Player2] button. Empty = %LOCALAPPDATA%\\Player2\\player2.exe")
                .define("appPath", "");
        TITLE_TOAST = b.comment("Show a toast on the title screen when Player2 is offline")
                .define("titleToast", true);
        JOIN_MESSAGE = b.comment("Remind you in chat when you join a world while Player2 is offline")
                .define("joinMessage", true);
        CONNECT_MESSAGE = b.comment("Say so in chat when Player2 connects or disconnects while you're in a world")
                .define("connectMessage", true);
        SPEC = b.build();
    }

    private WatchConfig() {
    }
}
