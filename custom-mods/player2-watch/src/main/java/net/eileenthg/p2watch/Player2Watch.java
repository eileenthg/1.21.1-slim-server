package net.eileenthg.p2watch;

import com.mojang.brigadier.Command;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Keeps an eye on the local Player2 app: reminds you on the title screen and when you join a world while it's offline,
 * keeps checking, and tells you when it connects (or drops). The mods that use it (TLM, MCA, Townstead) ask Player2 fresh
 * on every request, so once it's up they work again without a restart.
 */
@Mod(value = Player2Watch.MOD_ID, dist = Dist.CLIENT)
public final class Player2Watch {
    public static final String MOD_ID = "p2watch";
    static final Logger LOG = LogUtils.getLogger();

    enum State { UNKNOWN, ONLINE, OFFLINE }

    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Player2 Watch");
        t.setDaemon(true);
        return t;
    });
    private static final Pattern VERSION = Pattern.compile("\"client_version\"\\s*:\\s*\"([^\"]*)\"");

    private static volatile State state = State.UNKNOWN;
    private static volatile String version = "";
    private static ScheduledFuture<?> next;
    private static volatile List<Uses.Use> uses = List.of();
    // main thread only
    private static boolean toastShown;
    private static boolean pendingJoin;
    private static boolean inWorld;
    private static int ticks;

    public Player2Watch(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, WatchConfig.SPEC);
        modBus.addListener((FMLClientSetupEvent e) -> {
            uses = Uses.detect();
            LOG.info("Player2 Watch: features using Player2: {}", uses);
            checkSoon(0);
        });
        // the first check lands under the loading screen, and this pack may swap the title screen, so keep trying in any menu
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            if (++ticks % 20 == 0) titleToast();
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> {
            // can fire more than once per join; remind only the first time
            if (!inWorld) {
                inWorld = true;
                uses = Uses.detect();
                pendingJoin = true;
                checkSoon(0);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            inWorld = false;
            pendingJoin = false;
        });
        NeoForge.EVENT_BUS.addListener(Player2Watch::commands);
    }

    // ------------------------------------------------------------------ checking

    static String url() {
        String u = "";
        try {
            u = WatchConfig.URL.get().trim();
        } catch (RuntimeException ignored) {
        }
        if (u.isEmpty()) {
            String mca = Uses.mcaUrl();
            u = mca != null ? mca : "http://127.0.0.1:4315/";
        }
        return u.endsWith("/") ? u : u + "/";
    }

    private static synchronized void checkSoon(long delayMs) {
        if (next != null) next.cancel(false);
        next = EXEC.schedule(Player2Watch::check, delayMs, TimeUnit.MILLISECONDS);
    }

    private static void check() {
        boolean ok = false;
        try {
            HttpURLConnection c = (HttpURLConnection) URI.create(url() + "v1/health").toURL().openConnection();
            c.setConnectTimeout(1500);
            c.setReadTimeout(3000);
            c.setRequestProperty("Accept", "application/json");
            if (c.getResponseCode() == 200) {
                ok = true;
                Matcher m = VERSION.matcher(new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                version = m.find() ? m.group(1) : "";
            }
            c.disconnect();
        } catch (Exception ignored) {
            // not running
        }
        State old = state;
        State now = ok ? State.ONLINE : State.OFFLINE;
        state = now;
        if (old != now) LOG.info("Player2 Watch: Player2 is {}", now == State.ONLINE ? "online (app " + version + ")" : "offline");
        Minecraft.getInstance().execute(() -> changed(old, now));
        int secs;
        try {
            secs = ok ? WatchConfig.ONLINE_CHECK_SECONDS.get() : WatchConfig.OFFLINE_CHECK_SECONDS.get();
        } catch (RuntimeException e) {
            secs = ok ? 30 : 5;
        }
        checkSoon(secs * 1000L);
    }

    // ------------------------------------------------------------------ telling you

    /** What needs Player2 right now: everything in the menus or when hosting; only your own client's parts on someone else's server. */
    private static List<Uses.Use> relevant() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.hasSingleplayerServer()) return uses;
        return uses.stream().filter(Uses.Use::onThisPc).toList();
    }

    private static void changed(State old, State now) {
        Minecraft mc = Minecraft.getInstance();
        if (pendingJoin && mc.level != null && mc.player != null) {
            pendingJoin = false;
            if (now == State.OFFLINE) {
                if (!relevant().isEmpty() && WatchConfig.JOIN_MESSAGE.get()) {
                    chat(Component.literal("The Player2 app isn't running, so " + list(relevant()) + " won't work. ")
                            .append(startButton())
                            .append(Component.literal(" I'll tell you when it connects.")));
                }
                return;
            }
            // online: if it only just came up, still say so below
        }
        if (old == now) return;
        if (now == State.OFFLINE) {
            if (mc.level == null) {
                titleToast();
            } else if (old == State.ONLINE && !relevant().isEmpty() && WatchConfig.CONNECT_MESSAGE.get()) {
                chat(Component.literal("Lost the Player2 app, so " + list(relevant()) + " won't work. ")
                        .append(startButton())
                        .append(Component.literal(" I'll keep checking.")));
            }
        } else if (old == State.OFFLINE) {
            toastShown = false;
            if (relevant().isEmpty()) return;
            toast("Player2 connected", cap(list(relevant())) + " work again.");
            if (mc.level != null && WatchConfig.CONNECT_MESSAGE.get()) {
                chat(Component.literal("Connected" + (version.isEmpty() ? "" : " (app " + version + ")") + ". "
                        + cap(list(relevant())) + " work again.").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    private static void titleToast() {
        Minecraft mc = Minecraft.getInstance();
        if (state != State.OFFLINE || toastShown || mc.level != null || mc.getOverlay() != null || mc.screen == null || uses.isEmpty()) return;
        if (!WatchConfig.TITLE_TOAST.get()) return;
        toastShown = true;
        toast("Player2 isn't running", "Start it for " + list(uses) + ". I'll keep checking.");
    }

    private static void toast(String title, String text) {
        LOG.info("Player2 Watch: toast '{}': {}", title, text);
        SystemToast.addOrUpdate(Minecraft.getInstance().getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal(title), Component.literal(text));
    }

    private static void chat(Component msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.displayClientMessage(Component.literal("[Player2] ").withStyle(ChatFormatting.GOLD).append(msg), false);
    }

    private static MutableComponent startButton() {
        return Component.literal("[Start Player2]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/player2 start"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Open the Player2 app"))));
    }

    private static String list(List<Uses.Use> l) {
        List<String> n = l.stream().map(Uses.Use::name).toList();
        if (n.size() <= 1) return n.isEmpty() ? "AI chat" : n.get(0);
        return String.join(", ", n.subList(0, n.size() - 1)) + " and " + n.get(n.size() - 1);
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------------ /player2

    private static void commands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("player2")
                .executes(ctx -> status())
                .then(Commands.literal("status").executes(ctx -> status()))
                .then(Commands.literal("check").executes(ctx -> {
                    pendingJoin = false;
                    chat(Component.literal("Checking..."));
                    checkSoon(0);
                    EXEC.schedule(() -> Minecraft.getInstance().execute(Player2Watch::status), 2500, TimeUnit.MILLISECONDS);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("start").executes(ctx -> start())));
    }

    private static int status() {
        String who = uses.isEmpty() ? "nothing in this pack is set to use it" : "used for " + list(uses);
        boolean remote = Minecraft.getInstance().level != null && !Minecraft.getInstance().hasSingleplayerServer();
        String note = remote && !uses.isEmpty() ? " On someone else's server, only " + (relevant().isEmpty() ? "nothing" : list(relevant()))
                + " uses your own Player2; the rest uses the host's." : "";
        switch (state) {
            case ONLINE -> chat(Component.literal("Online" + (version.isEmpty() ? "" : " (app " + version + ")") + " at " + url() + ", " + who + "." + note)
                    .withStyle(ChatFormatting.GREEN));
            case OFFLINE -> chat(Component.literal("Offline (" + url() + "), " + who + ". ").append(startButton())
                    .append(Component.literal(note)));
            default -> chat(Component.literal("Still checking..."));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int start() {
        if (state == State.ONLINE) {
            chat(Component.literal("Player2 is already running."));
            return Command.SINGLE_SUCCESS;
        }
        String path = "";
        try {
            path = WatchConfig.APP_PATH.get().trim();
        } catch (RuntimeException ignored) {
        }
        if (path.isEmpty() && System.getenv("LOCALAPPDATA") != null) path = System.getenv("LOCALAPPDATA") + "\\Player2\\player2.exe";
        File exe = new File(path);
        if (exe.isFile()) {
            try {
                new ProcessBuilder(exe.getAbsolutePath()).directory(exe.getParentFile()).start();
                chat(Component.literal("Starting Player2..."));
                checkSoon(3000);
                return Command.SINGLE_SUCCESS;
            } catch (Exception ex) {
                LOG.warn("Player2 Watch: couldn't start {}: {}", exe, ex.toString());
            }
        }
        chat(Component.literal("Couldn't find the Player2 app" + (path.isEmpty() ? "" : " at " + path)
                + "; opening player2.game. Set appPath in config/p2watch-client.toml if it's somewhere else."));
        Util.getPlatform().openUri("https://player2.game/");
        return Command.SINGLE_SUCCESS;
    }
}
