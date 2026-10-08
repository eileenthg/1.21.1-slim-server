package net.eileenthg.taskcurator;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The player's task menu layout: an ordered top level of loose tasks and folders, plus a hidden set.
 * Task IDs of mods that are no longer installed are kept, so reinstalling a mod restores its place.
 */
public final class CuratorLayout {
    public static final String IDLE = "touhou_little_maid:idle";

    public static final class Folder {
        public String name;
        public boolean open;
        public final List<String> tasks = new ArrayList<>();

        public Folder(String name) {
            this.name = name;
        }
    }

    /** Each element is either a {@link String} task ID or a {@link Folder}. */
    public final List<Object> top = new ArrayList<>();
    public final Set<String> hidden = new LinkedHashSet<>();

    // ---- menu rows ----

    public sealed interface Row permits FolderRow, TaskRow {
    }

    public record FolderRow(Folder folder, int count, boolean holdsCurrent) implements Row {
    }

    public record TaskRow(IMaidTask task, boolean inFolder) implements Row {
    }

    /**
     * Rows for the maid's task menu. {@code available} is TLM's own not-hidden list for this maid;
     * folders with nothing visible in them are left out.
     */
    public List<Row> buildRows(List<IMaidTask> available, IMaidTask current) {
        Map<String, IMaidTask> byId = new LinkedHashMap<>();
        for (IMaidTask t : available) {
            byId.put(t.getUid().toString(), t);
        }
        String currentId = current == null ? null : current.getUid().toString();
        Set<String> placed = new HashSet<>();
        List<Row> rows = new ArrayList<>();
        for (Object o : top) {
            if (o instanceof String id) {
                placed.add(id);
                IMaidTask t = byId.get(id);
                if (t != null && !hidden.contains(id)) {
                    rows.add(new TaskRow(t, false));
                }
            } else if (o instanceof Folder f) {
                List<IMaidTask> members = new ArrayList<>();
                for (String id : f.tasks) {
                    placed.add(id);
                    IMaidTask t = byId.get(id);
                    if (t != null && !hidden.contains(id)) {
                        members.add(t);
                    }
                }
                if (members.isEmpty()) {
                    continue;
                }
                boolean holdsCurrent = members.stream().anyMatch(t -> t.getUid().toString().equals(currentId));
                rows.add(new FolderRow(f, members.size(), holdsCurrent));
                if (f.open) {
                    for (IMaidTask t : members) {
                        rows.add(new TaskRow(t, true));
                    }
                }
            }
        }
        // Anything registered after the last reconcile still shows, at the end.
        for (Map.Entry<String, IMaidTask> e : byId.entrySet()) {
            if (!placed.contains(e.getKey()) && !hidden.contains(e.getKey())) {
                rows.add(new TaskRow(e.getValue(), false));
            }
        }
        return rows;
    }

    // ---- structure queries and edits ----

    /** The folder holding the task, or null when it sits at the top level (or nowhere). */
    public Folder folderOf(String id) {
        for (Object o : top) {
            if (o instanceof Folder f && f.tasks.contains(id)) {
                return f;
            }
        }
        return null;
    }

    public List<Folder> folders() {
        List<Folder> out = new ArrayList<>();
        for (Object o : top) {
            if (o instanceof Folder f) {
                out.add(f);
            }
        }
        return out;
    }

    private void remove(String id) {
        top.remove(id);
        for (Folder f : folders()) {
            f.tasks.remove(id);
        }
    }

    /** Moves a task to the end of {@code target}, or to the end of the top level when target is null. */
    public void moveToFolder(String id, Folder target) {
        remove(id);
        if (target == null) {
            top.add(id);
        } else {
            target.tasks.add(id);
        }
    }

    public void deleteFolder(Folder f) {
        int at = top.indexOf(f);
        if (at < 0) {
            return;
        }
        top.remove(at);
        top.addAll(at, f.tasks);
    }

    /**
     * Swaps an element with its nearest neighbour in {@code dir} (-1 up, +1 down) that {@code visible}
     * accepts, so entries of uninstalled mods never make a click look like it did nothing.
     */
    public static <E> boolean shift(List<E> list, E element, int dir, java.util.function.Predicate<E> visible) {
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

    /** Appends every registered task that the layout doesn't mention yet. Returns the ones added. */
    public List<String> reconcile(Collection<String> registered) {
        Set<String> placed = new HashSet<>();
        for (Object o : top) {
            if (o instanceof String id) {
                placed.add(id);
            } else if (o instanceof Folder f) {
                placed.addAll(f.tasks);
            }
        }
        List<String> added = new ArrayList<>();
        for (String id : registered) {
            if (placed.add(id)) {
                top.add(id);
                added.add(id);
            }
        }
        hidden.remove(IDLE);
        return added;
    }

    // ---- defaults ----

    private static final String TLM = "touhou_little_maid:";
    private static final String MSK = "maidsoulkitchen:";

    public static CuratorLayout defaults() {
        CuratorLayout l = new CuratorLayout();
        l.top.add(IDLE);
        l.folder("Combat",
                TLM + "attack", TLM + "ranged_attack", TLM + "crossbow_attack", TLM + "danmaku_attack",
                TLM + "trident_attack", TLM + "gun_attack", "youkaishomecoming:danmaku_attack",
                "maidspell:spell_combat_melee", "maidspell:spell_combat_far",
                "maid_sword_soaring:sword_soaring",
                // Lost Maids' own tasks; TLM normally keeps them out of an owned maid's menu.
                "touhou_lost_maid:attack_player", "touhou_lost_maid:bow_attack", "touhou_lost_maid:gun_attack");
        l.folder("Farming",
                TLM + "farm", MSK + "sereneseasons_farm", MSK + "eclipticseasons_farm",
                TLM + "sugar_cane", TLM + "melon", MSK + "compat_melon", TLM + "cocoa",
                MSK + "berries_farm", MSK + "fruit_farm", TLM + "grass",
                "maid_useful_task:maid_tree");
        l.folder("Animals",
                TLM + "feed_animal", MSK + "feed_animal_t", TLM + "shears", TLM + "milk",
                TLM + "honey", TLM + "fishing");
        l.folder("Cooking",
                "maidchef:chef",
                MSK + "furnace", MSK + "fd_cooking_pot", MSK + "fd_cutting_board", MSK + "fd_skillet",
                MSK + "cd_cuisine_skillet", MSK + "bd_grill", MSK + "bd_basin",
                MSK + "yhc_moka_pot", MSK + "yhc_tea_kettle", MSK + "yhc_drying_rack",
                MSK + "yhc_fermentation_tank", MSK + "drinkbeer_beerbarrel");
        l.folder("Utility",
                TLM + "feed", TLM + "torch", TLM + "snow", TLM + "extinguishing",
                "maid_useful_task:locate", "maid_useful_task:revive_player",
                "maid_storage_manager:storage_manage", "muhc:hand_crank_task",
                "callresponse:lazy", TLM + "board_games", "createtreadmill:use_treadmill");
        // Deprecated by its own mod (leash the maid to the treadmill instead).
        l.hidden.add("createtreadmill:use_treadmill");
        // Its melon list only has Simple Farming melons, which this pack doesn't have.
        l.hidden.add(MSK + "compat_melon");
        return l;
    }

    private void folder(String name, String... ids) {
        Folder f = new Folder(name);
        f.tasks.addAll(List.of(ids));
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
                e.addProperty("task", id);
            } else if (o instanceof Folder f) {
                e.addProperty("folder", f.name);
                e.addProperty("open", f.open);
                JsonArray tasks = new JsonArray();
                f.tasks.forEach(tasks::add);
                e.add("tasks", tasks);
            }
            layout.add(e);
        }
        root.add("layout", layout);
        JsonArray hid = new JsonArray();
        hidden.forEach(hid::add);
        root.add("hidden", hid);
        return root;
    }

    public static CuratorLayout fromJson(JsonObject root) {
        CuratorLayout l = new CuratorLayout();
        for (JsonElement el : root.getAsJsonArray("layout")) {
            JsonObject e = el.getAsJsonObject();
            if (e.has("task")) {
                l.top.add(e.get("task").getAsString());
            } else if (e.has("folder")) {
                Folder f = new Folder(e.get("folder").getAsString());
                f.open = e.has("open") && e.get("open").getAsBoolean();
                if (e.has("tasks")) {
                    for (JsonElement t : e.getAsJsonArray("tasks")) {
                        f.tasks.add(t.getAsString());
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
    public static CuratorLayout load(Path file) throws IOException {
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
        return o instanceof CuratorLayout other && Objects.equals(toJson(), other.toJson());
    }

    @Override
    public int hashCode() {
        return toJson().hashCode();
    }
}
