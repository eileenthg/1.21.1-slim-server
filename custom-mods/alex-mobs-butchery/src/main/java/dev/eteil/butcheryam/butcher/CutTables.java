package dev.eteil.butcheryam.butcher;

import static dev.eteil.butcheryam.butcher.CutStage.ANY_STAGE;
import static dev.eteil.butcheryam.butcher.CutStage.DESTROY;
import static dev.eteil.butcheryam.butcher.CutStage.SOUND_CUT;
import static dev.eteil.butcheryam.butcher.CutStage.cleaver;
import static dev.eteil.butcheryam.butcher.CutStage.knife;

import dev.eteil.butcheryam.butcher.CutStage.Tool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The butchering progression for each mob, transcribed from the original addon's procedures.
 *
 * <p>A carcass lying on the ground uses stages {@code 0,6,7,8,9}; one hung on a hook or rope uses
 * {@code 1,2,3,4,5}. Both live in the same {@code blockstate} property, so a single table covers
 * them.
 */
public final class CutTables {

    private static final String ORGANS_1 = "butchery:blocks/organs_drop_1";
    private static final String ORGANS_2 = "butchery:blocks/organs_drop_2";
    private static final String ORGANS_3 = "butchery:blocks/organs_drop_3";

    /** Butchery's shared feather table, used for birds without a bespoke one. */
    private static final String CHICKEN_PLUCKING = "butchery:blocks/chicken_plucking";

    private static final String PLUCKED = Butchering.FLAG_PLUCKED;

    /**
     * Builds the five-step sequence for a mob with a drained carcass, for both the ground stages
     * {@code 0,6,7,8,9} and the hanging stages {@code 1,2,3,4,5}.
     */
    private static List<CutStage> full(String head, String skinOrPluck, boolean pluck,
                                       String cut1, String cut2, String cut3,
                                       List<String> organs1, List<String> organs2, List<String> organs3) {
        int[][] runs = {{0, 6, 7, 8, 9}, {1, 2, 3, 4, 5}};
        List<CutStage> out = new ArrayList<>();
        for (int[] s : runs) {
            out.add(cleaver(s[0], s[1], head, null));
            out.add(pluck ? knife(s[1], s[2], skinOrPluck, PLUCKED)
                          : knife(s[1], s[2], skinOrPluck, null));
            out.add(new CutStage(Tool.CLEAVER, s[2], s[3], list(cut1), organs1, null, SOUND_CUT));
            out.add(new CutStage(Tool.CLEAVER, s[3], s[4], list(cut2), organs2, null, SOUND_CUT));
            out.add(new CutStage(Tool.CLEAVER, s[4], DESTROY, list(cut3), organs3, null, SOUND_CUT));
        }
        return List.copyOf(out);
    }

    private static List<String> list(String s) {
        return s == null ? List.of() : List.of(s);
    }

    private static final List<String> ALL_ORGANS = List.of(ORGANS_1, ORGANS_2, ORGANS_3);
    private static final List<String> NO_ORGANS = List.of();

    private static final Map<String, List<CutStage>> TABLES = Map.of(
            // Bald eagle: wings then body; no dedicated mid cut, and all organs at the end.
            "bald_eagle", full(
                    "butchery_alexs_mobs:blocks/eagle_head_drop", CHICKEN_PLUCKING, true,
                    "butchery_alexs_mobs:blocks/eagle_wing_drop", null,
                    "butchery_alexs_mobs:blocks/eagle_body_drop",
                    NO_ORGANS, NO_ORGANS, ALL_ORGANS),

            "caiman", full(
                    "butchery_alexs_mobs:blocks/caiman_head_drop",
                    "butchery_alexs_mobs:blocks/caiman_skin_drop", false,
                    "butchery_alexs_mobs:blocks/caiman_cut_1_drop",
                    "butchery_alexs_mobs:blocks/caiman_cut_2_drop",
                    "butchery_alexs_mobs:blocks/caiman_cut_3_drop",
                    NO_ORGANS, NO_ORGANS, ALL_ORGANS),

            "capuchin", full(
                    "butchery_alexs_mobs:blocks/capuchin_head_drop",
                    "butchery_alexs_mobs:blocks/capuchin_skin_drop", false,
                    "butchery_alexs_mobs:blocks/capuchin_cut_1_drop",
                    "butchery_alexs_mobs:blocks/capuchin_cut_2_drop",
                    "butchery_alexs_mobs:blocks/capuchin_cut_3_drop",
                    NO_ORGANS, NO_ORGANS, ALL_ORGANS),

            // Emu spreads its organ tables across the three cuts instead of dropping them together.
            // (emu_cut_2_drop ships with the addon but the original never referenced it.)
            "emu", full(
                    "butchery_alexs_mobs:blocks/emu_head_drop",
                    "butchery_alexs_mobs:blocks/emu_feather_drop", true,
                    "butchery_alexs_mobs:blocks/emu_cut_1_drop", null,
                    "butchery_alexs_mobs:blocks/emu_cut_3_drop",
                    List.of(ORGANS_3), List.of(ORGANS_2), List.of(ORGANS_1)),

            // Crow has no drained block: three stages, no hanging progression.
            "crow", List.of(
                    cleaver(0, 1, "butchery_alexs_mobs:blocks/crow_head_drop", null),
                    knife(1, 2, CHICKEN_PLUCKING, PLUCKED),
                    new CutStage(Tool.CLEAVER, 2, DESTROY,
                            List.of("butchery_alexs_mobs:blocks/crow_cut_drop"), ALL_ORGANS, null, SOUND_CUT)),

            // Fish are a single step with either tool, at any stage.
            "blobfish", List.of(
                    new CutStage(Tool.ANY, ANY_STAGE, DESTROY,
                            List.of("butchery_alexs_mobs:blocks/blobfish_cut_drop"), NO_ORGANS, null, SOUND_CUT)),

            "flying_fish", List.of(
                    new CutStage(Tool.ANY, ANY_STAGE, DESTROY,
                            List.of("butchery_alexs_mobs:blocks/flying_fish_cut_drop"), NO_ORGANS, null, SOUND_CUT))
    );

    public static List<CutStage> forMob(MobDef def) {
        return TABLES.getOrDefault(def.name(), List.of());
    }

    private CutTables() {}
}
