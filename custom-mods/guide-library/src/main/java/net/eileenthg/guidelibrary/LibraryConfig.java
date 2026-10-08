package net.eileenthg.guidelibrary;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Two files. {@code guidelibrary-common.toml} is the pack author's: which books exist, which are
 * hidden, which are pinned. The server reads it too, so an item book it does not list cannot be
 * opened by a modified client. {@code guidelibrary-client.toml} is the player's: where the button
 * sits and whether it is shown.
 *
 * <p>Entries are written as {@code <source>/<id>}, e.g. {@code patchouli/townstead:guide},
 * {@code modonomicon/spectrum:guidebook} or {@code item/mca:book_death}. {@code *} matches
 * anything, so {@code item/mca:*} or {@code *&#47;apotheosis:*} work. Hold F3+H in the library to see
 * each book's key in its tooltip.
 */
public final class LibraryConfig {

    private LibraryConfig() {}

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;
    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        var common = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
        var client = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
    }

    public static final class Common {
        public final ModConfigSpec.ConfigValue<List<? extends String>> extraBookItems;
        public final ModConfigSpec.ConfigValue<List<? extends String>> hiddenBooks;
        public final ModConfigSpec.ConfigValue<List<? extends String>> featuredBooks;
        public final ModConfigSpec.ConfigValue<String> buttonOpensBook;

        Common(ModConfigSpec.Builder b) {
            b.push("books");
            extraBookItems = b.comment(
                            "Item ids of extra book items to list, for mods that are not Patchouli or Modonomicon.",
                            "They are opened by replaying the item's right-click, so only list real books:",
                            "a spell tome or recipe tool listed here would be 'used' when clicked.",
                            "'*' works, e.g. \"somemod:guide_*\". The #guidelibrary:guide_books item tag does the same job.")
                    .defineListAllowEmpty("extraBookItems", List.of(), () -> "", LibraryConfig::isString);
            hiddenBooks = b.comment(
                            "Books to leave out of the library, as <source>/<id> keys, e.g. \"patchouli/apotheosis:apoth_chronicle\",",
                            "\"item/mca:book_supporters\" or \"*/somemod:*\".")
                    .defineListAllowEmpty("hiddenBooks", List.of(), () -> "", LibraryConfig::isString);
            featuredBooks = b.comment(
                            "Books pinned to the top of the library in this order, e.g. the pack's own guide.")
                    .defineListAllowEmpty("featuredBooks", List.of(), () -> "", LibraryConfig::isString);
            buttonOpensBook = b.comment(
                            "If set to a book key, the inventory button opens that book directly instead of the",
                            "library (shift-click still opens the library). Like PatchouliButton's pack-guide mode.")
                    .define("buttonOpensBook", "");
            b.pop();

            b.push("unlocks");
            unlockMode = b.comment(
                            "OBTAINED: a book shows up in the library once the player has had a physical copy in",
                            "their inventory. EVERYTHING: every book is listed from the start.")
                    .defineEnum("unlockMode", UnlockMode.OBTAINED);
            alwaysUnlocked = b.comment(
                            "Books listed for everyone without finding them first, e.g. the pack's own guide.",
                            "The buttonOpensBook book always counts as unlocked.",
                            "Ops can also use /guidelibrary unlock <players> <book> (e.g. as a quest reward).")
                    .defineListAllowEmpty("alwaysUnlocked", List.of(), () -> "", LibraryConfig::isString);
            b.pop();
        }

        public final ModConfigSpec.EnumValue<UnlockMode> unlockMode;
        public final ModConfigSpec.ConfigValue<List<? extends String>> alwaysUnlocked;
    }

    public enum UnlockMode { OBTAINED, EVERYTHING }

    /** True when the config alone makes {@code key} available, whatever the player has found. */
    public static boolean unlockedByConfig(String key) {
        if (COMMON.unlockMode.get() == UnlockMode.EVERYTHING) return true;
        if (matchesAny(COMMON.alwaysUnlocked.get(), key)) return true;
        String direct = COMMON.buttonOpensBook.get();
        return !direct.isBlank() && direct.trim().toLowerCase(Locale.ROOT).equals(key);
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue showInventoryButton;
        public final ModConfigSpec.BooleanValue showInCreative;
        public final ModConfigSpec.IntValue buttonX;
        public final ModConfigSpec.IntValue buttonY;
        public final ModConfigSpec.IntValue creativeButtonX;
        public final ModConfigSpec.IntValue creativeButtonY;
        public final ModConfigSpec.ConfigValue<String> buttonIcon;

        Client(ModConfigSpec.Builder b) {
            b.push("button");
            showInventoryButton = b.comment("Show the library button in the survival inventory.")
                    .define("showInventoryButton", true);
            showInCreative = b.comment("Show it on the creative inventory's Inventory tab too.")
                    .define("showInCreative", true);
            buttonX = b.comment("Button position, relative to the inventory window's top-left corner.",
                            "The default sits just right of the recipe-book button.")
                    .defineInRange("buttonX", 126, -500, 500);
            buttonY = b.defineInRange("buttonY", 61, -500, 500);
            creativeButtonX = b.comment("Same, for the creative inventory tab.")
                    .defineInRange("creativeButtonX", 130, -500, 500);
            creativeButtonY = b.defineInRange("creativeButtonY", 20, -500, 500);
            buttonIcon = b.comment("Item shown on the button.")
                    .define("buttonIcon", "minecraft:book");
            b.pop();
            showUnlockMessage = b.comment("Show a message above the hotbar when a new book is added to the library.")
                    .define("showUnlockMessage", true);
        }

        public final ModConfigSpec.BooleanValue showUnlockMessage;
    }

    private static boolean isString(Object o) {
        return o instanceof String;
    }

    /** True when {@code key} matches any entry, where {@code *} in an entry matches anything. */
    public static boolean matchesAny(List<? extends String> patterns, String key) {
        for (String p : patterns) {
            if (matches(p, key)) return true;
        }
        return false;
    }

    public static boolean matches(String pattern, String key) {
        if (pattern == null || pattern.isBlank()) return false;
        String p = pattern.trim().toLowerCase(Locale.ROOT);
        if (!p.contains("*")) return p.equals(key);
        StringBuilder re = new StringBuilder();
        for (String part : p.split("\\*", -1)) {
            if (!re.isEmpty()) re.append(".*");
            re.append(Pattern.quote(part));
        }
        return key.matches(re.toString());
    }
}
