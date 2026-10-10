package dev.eteil.butcheryam.butcher;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * One step of the butchering progression: which tool advances the carcass from {@code from} to
 * {@code to}, and what it drops.
 *
 * @param tool   tool required to perform this step
 * @param from   stage this step applies to, or {@link #ANY_STAGE} for blocks with no progression
 * @param to     stage to advance to, or {@link #DESTROY} to consume the block
 * @param loot   loot tables rolled once, plus once more per looting level
 * @param organs loot tables rolled only when the Organs option is on
 * @param flag   block-entity flag to set, or null
 * @param sound  sound played on success
 */
public record CutStage(
        Tool tool,
        int from,
        int to,
        List<String> loot,
        List<String> organs,
        @Nullable String flag,
        String sound
) {

    public static final int ANY_STAGE = -1;
    public static final int DESTROY = -1;

    public enum Tool { CLEAVER, KNIFE, ANY }

    public static final String SOUND_CUT = "item.axe.strip";
    public static final String SOUND_PLUCK = "entity.item.pickup";

    public static CutStage cleaver(int from, int to, String loot, @Nullable String organs) {
        return new CutStage(Tool.CLEAVER, from, to,
                loot == null ? List.of() : List.of(loot),
                organs == null ? List.of() : List.of(organs),
                null, SOUND_CUT);
    }

    public static CutStage knife(int from, int to, String loot, @Nullable String flag) {
        return new CutStage(Tool.KNIFE, from, to, List.of(loot), List.of(), flag, SOUND_PLUCK);
    }

    public boolean matches(Tool held, int stage) {
        boolean toolOk = tool == Tool.ANY || tool == held;
        boolean stageOk = from == ANY_STAGE || from == stage;
        return toolOk && stageOk;
    }
}
