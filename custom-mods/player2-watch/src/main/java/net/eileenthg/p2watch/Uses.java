package net.eileenthg.p2watch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Which features of the installed mods are set up to use Player2, read from their config files.
 * <p>
 * "Host" features run on the server (the world host's Player2 answers them); "this PC" features run on your own client.
 */
public final class Uses {
    public record Use(String name, boolean onThisPc) {
    }

    private Uses() {
    }

    public static List<Use> detect() {
        List<Use> out = new ArrayList<>();
        Path cfg = FMLPaths.CONFIGDIR.get();
        try {
            if (ModList.get().isLoaded("touhou_little_maid")) {
                String toml = read(cfg.resolve("touhou_little_maid-common.toml"));
                if (flag(toml, "LLMEnabled") && siteEnabled(cfg.resolve("touhou_little_maid/sites/llm.json"))) out.add(new Use("maid AI chat", false));
                if (flag(toml, "TTSEnabled") && siteEnabled(cfg.resolve("touhou_little_maid/sites/tts.json"))) out.add(new Use("maid voices", false));
                String stt = value(toml, "STTType");
                if (flag(toml, "STTEnabled") && (stt == null || stt.equalsIgnoreCase("PLAYER2"))
                        && siteEnabled(cfg.resolve("touhou_little_maid/sites/stt.json"))) out.add(new Use("maid voice input", true));
            }
            if (ModList.get().isLoaded("mca")) {
                JsonObject mca = json(cfg.resolve("mca.json"));
                if (mca != null) {
                    if (bool(mca, "enableVillagerChatAI") && (str(mca, "villagerChatAIModel").equals("player2")
                            || str(mca, "villagerChatAIEndpoint").contains(":4315"))) out.add(new Use("villager AI chat", false));
                    // Townstead's villager lines use MCA's voice setting too
                    if (bool(mca, "enableOnlineTTS") && str(mca, "onlineTTSModel").equals("player2")) out.add(new Use("villager voices", true));
                }
            }
        } catch (RuntimeException e) {
            Player2Watch.LOG.warn("Couldn't read which mods use Player2: {}", e.toString());
        }
        return out;
    }

    /** MCA's player2Url, if MCA is set up. */
    @Nullable
    public static String mcaUrl() {
        JsonObject mca = json(FMLPaths.CONFIGDIR.get().resolve("mca.json"));
        String url = mca == null ? "" : str(mca, "player2Url");
        return url.isBlank() ? null : url;
    }

    private static boolean siteEnabled(Path file) {
        JsonObject o = json(file);
        if (o == null || !(o.get("player2") instanceof JsonObject p2)) return false;
        return !p2.has("enabled") || p2.get("enabled").getAsBoolean();
    }

    private static boolean flag(String toml, String key) {
        String v = value(toml, key);
        return v == null || v.equalsIgnoreCase("true");
    }

    @Nullable
    private static String value(String toml, String key) {
        Matcher m = Pattern.compile("(?m)^\\s*" + key + "\\s*=\\s*\"?([^\"\\r\\n]*)\"?").matcher(toml);
        return m.find() ? m.group(1).trim() : null;
    }

    private static boolean bool(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsBoolean();
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString().toLowerCase(Locale.ROOT) : "";
    }

    private static String read(Path file) {
        try {
            return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Nullable
    private static JsonObject json(Path file) {
        if (!Files.exists(file)) return null;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement e = JsonParser.parseReader(r);
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
