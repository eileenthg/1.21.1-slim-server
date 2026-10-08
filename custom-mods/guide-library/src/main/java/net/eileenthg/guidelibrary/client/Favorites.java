package net.eileenthg.guidelibrary.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.eileenthg.guidelibrary.GuideLibrary;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** The player's favorited book keys, kept in {@code config/guidelibrary-favorites.json}. */
final class Favorites {

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("guidelibrary-favorites.json");
    private static Set<String> keys;

    private Favorites() {}

    static boolean has(String key) {
        return load().contains(key);
    }

    static void toggle(String key) {
        Set<String> k = load();
        if (!k.remove(key)) k.add(key);
        save();
    }

    private static Set<String> load() {
        if (keys != null) return keys;
        keys = new LinkedHashSet<>();
        if (Files.exists(FILE)) {
            try {
                JsonElement root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8));
                if (root.isJsonArray()) {
                    for (JsonElement e : root.getAsJsonArray()) {
                        if (e.isJsonPrimitive()) keys.add(e.getAsString());
                    }
                }
            } catch (Exception e) {
                GuideLibrary.LOGGER.warn("Could not read {}; starting with no favorites", FILE, e);
            }
        }
        return keys;
    }

    private static void save() {
        JsonArray arr = new JsonArray();
        load().forEach(arr::add);
        try {
            Files.writeString(FILE, new Gson().toJson(arr), StandardCharsets.UTF_8);
        } catch (IOException e) {
            GuideLibrary.LOGGER.warn("Could not save {}", FILE, e);
        }
    }
}
