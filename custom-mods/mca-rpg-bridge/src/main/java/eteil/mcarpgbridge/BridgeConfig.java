package eteil.mcarpgbridge;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BridgeConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue MCA_MENU_BUTTON;
    private static final ModConfigSpec.IntValue QUEST_COMPLETED;
    private static final ModConfigSpec.IntValue CONTRACT_COMPLETED;
    private static final ModConfigSpec.IntValue QUEST_ABANDONED;
    private static final ModConfigSpec.IntValue VILLAGE_RADIUS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("mebahel");
        MCA_MENU_BUTTON = b.comment(
                        "Mebahel quests and rumors on MCA villagers open from a Request/Rumor button in MCA's villager menu,",
                        "so a plain right-click always opens MCA's menu. When false, Mebahel takes the right-click as it does alone.")
                .define("mcaMenuButton", true);
        b.pop();
        b.push("standing");
        b.comment("MCA: Reputation standing for Mebahel quest outcomes, applied to the quest giver's village. 0 turns a deed off.",
                "For scale: MCA: Quests gives 2 / 4 / 7 for easy / medium / hard quests.");
        QUEST_COMPLETED = b.comment("Claiming a Mebahel villager quest.").defineInRange("questCompleted", 4, 0, 100);
        CONTRACT_COMPLETED = b.comment("Completing a Mebahel quest board contract.").defineInRange("contractCompleted", 2, 0, 100);
        QUEST_ABANDONED = b.comment("Abandoning a Mebahel quest from its journal.").defineInRange("questAbandoned", -2, -100, 0);
        VILLAGE_RADIUS = b.comment("How far from the giver or board to look for the MCA village the deed belongs to.")
                .defineInRange("villageSearchRadius", 96, 16, 512);
        b.pop();
        SPEC = b.build();
    }

    private BridgeConfig() {
    }

    // Guarded reads: a config read before load throws (the Sophisticated Backpacks crash in this pack), so fall back to defaults.
    public static boolean mcaMenuButton() {
        return SPEC.isLoaded() ? MCA_MENU_BUTTON.get() : MCA_MENU_BUTTON.getDefault();
    }

    public static int questCompleted() {
        return SPEC.isLoaded() ? QUEST_COMPLETED.get() : QUEST_COMPLETED.getDefault();
    }

    public static int contractCompleted() {
        return SPEC.isLoaded() ? CONTRACT_COMPLETED.get() : CONTRACT_COMPLETED.getDefault();
    }

    public static int questAbandoned() {
        return SPEC.isLoaded() ? QUEST_ABANDONED.get() : QUEST_ABANDONED.getDefault();
    }

    public static int villageRadius() {
        return SPEC.isLoaded() ? VILLAGE_RADIUS.get() : VILLAGE_RADIUS.getDefault();
    }
}
