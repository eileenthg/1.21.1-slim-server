package net.eileenthg.guidelibrary.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.eileenthg.guidelibrary.GuideLibrary;
import net.eileenthg.guidelibrary.LibraryConfig;
import net.eileenthg.guidelibrary.network.SyncUnlocksPayload;
import net.eileenthg.guidelibrary.unlock.BookIds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The client's view of which books are unlocked.
 *
 * <p>Normally the server owns this and sends it in {@link SyncUnlocksPayload}. On a server without
 * Guide Library the client tracks it itself instead: it watches its own inventory the same way
 * and keeps the result in {@code config/guidelibrary-unlocks/<server>.json}.
 */
public final class ClientUnlocks {

    private static final Set<String> keys = new HashSet<>();
    private static boolean localMode;
    private static Path localFile;
    private static int tick;

    private ClientUnlocks() {}

    public static boolean isUnlocked(String key) {
        return LibraryConfig.unlockedByConfig(key) || keys.contains(key);
    }

    public static int count() {
        return keys.size();
    }

    public static void onSync(SyncUnlocksPayload payload) {
        localMode = false;
        keys.clear();
        keys.addAll(payload.keys());
        announce(payload.added());
    }

    static void onLogin() {
        keys.clear();
        tick = 0;
        var connection = Minecraft.getInstance().getConnection();
        localMode = connection == null || !connection.hasChannel(SyncUnlocksPayload.TYPE);
        localFile = null;
        if (localMode) {
            var server = Minecraft.getInstance().getCurrentServer();
            String name = server == null ? "unknown" : server.ip;
            localFile = FMLPaths.CONFIGDIR.get().resolve("guidelibrary-unlocks")
                    .resolve(name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_") + ".json");
            load();
            GuideLibrary.LOGGER.info("Server has no Guide Library; tracking unlocks locally in {}", localFile);
        }
    }

    static void onLogout() {
        keys.clear();
        localMode = false;
        localFile = null;
    }

    static void tick() {
        if (!localMode) return;
        var player = Minecraft.getInstance().player;
        if (player == null || ++tick % 20 != 0) return;
        Inventory inv = player.getInventory();
        List<String> added = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            String key = BookIds.identify(inv.getItem(i));
            if (key != null && keys.add(key)) added.add(key);
        }
        if (!added.isEmpty()) {
            save();
            announce(added);
        }
    }

    private static void announce(List<String> added) {
        if (added.isEmpty() || !LibraryConfig.CLIENT.showUnlockMessage.get()) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        List<BookEntry> all = BookCatalog.collect();
        for (String key : added) {
            Component name = all.stream().filter(e -> e.key().equals(key)).findFirst()
                    .map(BookEntry::name).orElse(null);
            if (name == null) continue; // hidden books unlock quietly
            player.displayClientMessage(Component.translatable("guidelibrary.unlocked",
                    name.copy().withStyle(ChatFormatting.YELLOW)), true);
        }
    }

    private static void load() {
        if (localFile == null || !Files.exists(localFile)) return;
        try {
            JsonElement root = JsonParser.parseString(Files.readString(localFile, StandardCharsets.UTF_8));
            if (root.isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray()) {
                    if (e.isJsonPrimitive()) keys.add(e.getAsString());
                }
            }
        } catch (Exception e) {
            GuideLibrary.LOGGER.warn("Could not read {}", localFile, e);
        }
    }

    private static void save() {
        if (localFile == null) return;
        JsonArray arr = new JsonArray();
        keys.stream().sorted().forEach(arr::add);
        try {
            Files.createDirectories(localFile.getParent());
            Files.writeString(localFile, new Gson().toJson(arr), StandardCharsets.UTF_8);
        } catch (IOException e) {
            GuideLibrary.LOGGER.warn("Could not save {}", localFile, e);
        }
    }
}
