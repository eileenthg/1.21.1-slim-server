package dev.eteil.butcheryam2.butcher;

import static dev.eteil.butcheryam2.butcher.CutStage.ANY_STAGE;
import static dev.eteil.butcheryam2.butcher.CutStage.DESTROY;
import static dev.eteil.butcheryam2.butcher.CutStage.SOUND_CUT;
import static dev.eteil.butcheryam2.butcher.CutStage.cleaver;
import static dev.eteil.butcheryam2.butcher.CutStage.knife;

import dev.eteil.butcheryam2.butcher.CutStage.Tool;
import dev.eteil.butcheryam2.gen.StepSpecs;
import java.util.ArrayList;
import java.util.List;

/**
 * The butchering progression for each kind of mob, following Part 1.
 *
 * <p>A carcass lying on the ground uses stages {@code 0,6,7,8,9}; one hung on a hook or rope uses
 * {@code 1,2,3,4,5}. Both live in the same {@code blockstate} property, so one table covers them.
 */
public final class CutTables {

    private static final List<String> ALL_ORGANS = List.of(
            "butchery:blocks/organs_drop_1", "butchery:blocks/organs_drop_2", "butchery:blocks/organs_drop_3");
    private static final int[][] RUNS = {{0, 6, 7, 8, 9}, {1, 2, 3, 4, 5}};

    /** Head, skin, then three cuts, with the organs at the end: Part 1's caiman and capuchin. */
    private static List<CutStage> full(MobDef def) {
        List<CutStage> out = new ArrayList<>();
        for (int[] s : RUNS) {
            out.add(cleaver(s[0], s[1], def.loot("head_drop"), null));
            out.add(knife(s[1], s[2], def.loot("skin_drop"), null));
            out.add(new CutStage(Tool.CLEAVER, s[2], s[3], List.of(def.loot("cut_1_drop")), List.of(), null, SOUND_CUT));
            out.add(new CutStage(Tool.CLEAVER, s[3], s[4], List.of(def.loot("cut_2_drop")), List.of(), null, SOUND_CUT));
            out.add(new CutStage(Tool.CLEAVER, s[4], DESTROY, List.of(def.loot("cut_3_drop")), ALL_ORGANS, null, SOUND_CUT));
        }
        return List.copyOf(out);
    }

    /**
     * Three steps on one block: a cleaver cut, a knife (peel or pluck), then the body. Birds follow
     * Part 1's crow (head, feathers, body, organs last); the bunfungus and mungus have no organs.
     */
    private static List<CutStage> staged(MobDef def) {
        List<String> organs = def.head() != null ? ALL_ORGANS : List.of();
        List<CutStage> out = new ArrayList<>();
        for (int[] s : RUNS) {
            out.add(cleaver(s[0], s[1], def.loot("cut_1_drop"), null));
            out.add(knife(s[1], s[2], def.loot("peel_drop"), null));
            out.add(new CutStage(Tool.CLEAVER, s[2], DESTROY, List.of(def.loot("cut_2_drop")), organs, null, SOUND_CUT));
        }
        return List.copyOf(out);
    }

    /** Fish: a single step with either tool, at any stage. */
    private static List<CutStage> single(MobDef def) {
        return List.of(new CutStage(Tool.ANY, ANY_STAGE, DESTROY,
                List.of(def.loot("cut_drop")), List.of(), null, SOUND_CUT));
    }

    /**
     * A generated step list: step {@code i} takes a carcass from state {@code 2i} (ground) or
     * {@code 2i+1} (hanging) to the next step's state; the last step consumes it.
     */
    private static List<CutStage> steps(MobDef def) {
        List<StepSpecs.Step> steps = StepSpecs.STEPS.get(def.name());
        List<CutStage> out = new ArrayList<>();
        for (int hung = 0; hung <= 1; hung++) {
            for (int i = 0; i < steps.size(); i++) {
                StepSpecs.Step s = steps.get(i);
                int from = 2 * i + hung;
                int to = i + 1 < steps.size() ? 2 * (i + 1) + hung : DESTROY;
                Tool tool = Tool.valueOf(s.tool());
                out.add(new CutStage(tool, from, to,
                        s.loot().isEmpty() ? List.of() : List.of(def.loot(s.loot())),
                        s.organs(), null,
                        tool == Tool.KNIFE ? CutStage.SOUND_PLUCK : SOUND_CUT, s.fragile()));
            }
        }
        return List.copyOf(out);
    }

    private static final java.util.Map<String, List<CutStage>> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static List<CutStage> forMob(MobDef def) {
        return CACHE.computeIfAbsent(def.name(), n -> build(def));
    }

    private static List<CutStage> build(MobDef def) {
        return switch (def.kind()) {
            case FULL -> full(def);
            case STAGED -> staged(def);
            case SINGLE -> single(def);
            case STEPS -> steps(def);
            case SEGMENTED, LARGE -> List.of(); // DrawnCarcassBlock cuts itself
        };
    }

    private CutTables() {}
}
