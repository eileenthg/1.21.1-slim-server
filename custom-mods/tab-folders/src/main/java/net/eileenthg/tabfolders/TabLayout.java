package net.eileenthg.tabfolders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The player's tab row: an ordered top level of tab IDs and folders, plus a hidden set.
 * An entry ending in {@code *} is a group that takes every tab whose ID starts with the rest (Sophisticated Tab
 * makes one tab per carried backpack). IDs of tabs that aren't around this session are kept, so they come back
 * in place when their mod or datapack does.
 */
public final class TabLayout {
    public static final String INVENTORY = "inventory";

    public static final class Folder {
        public String name;
        public String icon;
        public final List<String> tabs = new ArrayList<>();

        public Folder(String name, String icon) {
            this.name = name;
            this.icon = icon;
        }
    }

    /** Each element is either a {@link String} tab ID / group or a {@link Folder}. */
    public final List<Object> top = new ArrayList<>();
    public final Set<String> hidden = new LinkedHashSet<>();

    public static boolean isGroup(String entry) {
        return entry.endsWith("*");
    }

    public static boolean matches(String entry, String id) {
        return isGroup(entry) ? id.startsWith(entry.substring(0, entry.length() - 1)) : entry.equals(id);
    }

    /** Every entry the layout names exactly (not through a group). */
    public Set<String> exactIds() {
        Set<String> out = new HashSet<>();
        for (Object o : top) {
            if (o instanceof String id && !isGroup(id)) {
                out.add(id);
            } else if (o instanceof Folder f) {
                f.tabs.stream().filter(e -> !isGroup(e)).forEach(out::add);
            }
        }
        return out;
    }

    /** True when some entry, exact or group, places this tab. */
    public boolean places(String id) {
        for (Object o : top) {
            if (o instanceof String e && matches(e, id)) {
                return true;
            }
            if (o instanceof Folder f && f.tabs.stream().anyMatch(e -> matches(e, id))) {
                return true;
            }
        }
        return false;
    }

    public boolean isHidden(String id) {
        return hidden.stream().anyMatch(e -> matches(e, id));
    }

    // ---- structure queries and edits ----

    public List<Folder> folders() {
        List<Folder> out = new ArrayList<>();
        for (Object o : top) {
            if (o instanceof Folder f) {
                out.add(f);
            }
        }
        return out;
    }

    private void remove(String entry) {
        top.remove(entry);
        for (Folder f : folders()) {
            f.tabs.remove(entry);
        }
    }

    /** Moves an entry to the end of {@code target}, or to the end of the top level when target is null. */
    public void moveToFolder(String entry, Folder target) {
        remove(entry);
        if (target == null) {
            top.add(entry);
        } else {
            target.tabs.add(entry);
        }
    }

    public void deleteFolder(Folder f) {
        int at = top.indexOf(f);
        if (at < 0) {
            return;
        }
        top.remove(at);
        top.addAll(at, f.tabs);
    }

    /**
     * Swaps an element with its nearest neighbour in {@code dir} (-1 up, +1 down) that {@code visible}
     * accepts, so entries of tabs that aren't around never make a click look like it did nothing.
     */
    public static <E> boolean shift(List<E> list, E element, int dir, Predicate<E> visible) {
        int i = list.indexOf(element);
        if (i < 0) {
            return false;
        }
        for (int j = i + dir; j >= 0 && j < list.size(); j += dir) {
            if (visible.test(list.get(j))) {
                list.set(i, list.get(j));
                list.set(j, element);
                return true;
            }
        }
        return false;
    }

    /** Appends every known tab that no entry places yet. Returns the ones added. */
    public List<String> reconcile(Collection<String> known) {
        List<String> added = new ArrayList<>();
        for (String id : known) {
            if (!places(id)) {
                top.add(id);
                added.add(id);
            }
        }
        // The inventory tab is the way back from every other screen: always first, never hidden or filed away.
        if (top.remove(INVENTORY) | folders().stream().anyMatch(f -> f.tabs.remove(INVENTORY)) | known.contains(INVENTORY)) {
            top.add(0, INVENTORY);
        }
        hidden.remove(INVENTORY);
        return added;
    }

    // ---- defaults ----

    public static TabLayout defaults() {
        TabLayout l = new TabLayout();
        l.top.addAll(List.of(INVENTORY,
                // Sophisticated Tab's way back from a backpack screen
                "sophisticatedtab_back_to_inventory",
                "xaeros_map", "colony_clipboard", "maid_tablet"));
        // A folder with only one tab showing puts that tab on the row instead, so a lone backpack is one click.
        l.folder("Backpack", "sophisticatedbackpacks:backpack",
                "sophisticatedtab_backpack_*", "sophisticated_backpacks", "bag_of_holding", "ender_pocket");
        l.folder("Quests", "minecraft:writable_book",
                "gensokyolegacy_quests", "mca_quests", "mebahel_journal", "mebahel_map",
                "mca_reputation", "townstead_factions");
        l.folder("Info", "jei:textures/gui/sprites/icons/info.png",
                "world_tier", "origin", "chronicle", "advancements", "statistics");
        // Already reachable elsewhere (Guide Library and Curios have inventory buttons), or not wanted on the row.
        l.top.addAll(List.of("guide_library", "curios", "keybind_atlas", "roadweaver_map", "apoli_skill_tree",
                "sophisticatedtab_settings"));
        l.hidden.addAll(List.of("guide_library", "curios", "keybind_atlas", "roadweaver_map", "apoli_skill_tree",
                "sophisticatedtab_settings"));
        return l;
    }

    private void folder(String name, String icon, String... ids) {
        Folder f = new Folder(name, icon);
        f.tabs.addAll(List.of(ids));
        top.add(f);
    }

    // ---- JSON ----

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonArray layout = new JsonArray();
        for (Object o : top) {
            JsonObject e = new JsonObject();
            if (o instanceof String id) {
                e.addProperty("tab", id);
            } else if (o instanceof Folder f) {
                e.addProperty("folder", f.name);
                e.addProperty("icon", f.icon);
                JsonArray tabs = new JsonArray();
                f.tabs.forEach(tabs::add);
                e.add("tabs", tabs);
            }
            layout.add(e);
        }
        root.add("layout", layout);
        JsonArray hid = new JsonArray();
        hidden.forEach(hid::add);
        root.add("hidden", hid);
        return root;
    }

    public static TabLayout fromJson(JsonObject root) {
        TabLayout l = new TabLayout();
        for (JsonElement el : root.getAsJsonArray("layout")) {
            JsonObject e = el.getAsJsonObject();
            if (e.has("tab")) {
                l.top.add(e.get("tab").getAsString());
            } else if (e.has("folder")) {
                Folder f = new Folder(e.get("folder").getAsString(),
                        e.has("icon") ? e.get("icon").getAsString() : "minecraft:chest");
                if (e.has("tabs")) {
                    for (JsonElement t : e.getAsJsonArray("tabs")) {
                        f.tabs.add(t.getAsString());
                    }
                }
                l.top.add(f);
            }
        }
        if (root.has("hidden")) {
            for (JsonElement t : root.getAsJsonArray("hidden")) {
                l.hidden.add(t.getAsString());
            }
        }
        return l;
    }

    /** Returns null when the file is missing; throws on a broken file so the caller can keep a copy. */
    public static TabLayout load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return fromJson(JsonParser.parseReader(r).getAsJsonObject());
        } catch (RuntimeException e) {
            throw new IOException("Unreadable layout in " + file, e);
        }
    }

    public void save(Path file) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(toJson(), w);
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TabLayout other && Objects.equals(toJson(), other.toJson());
    }

    @Override
    public int hashCode() {
        return toJson().hashCode();
    }
}
