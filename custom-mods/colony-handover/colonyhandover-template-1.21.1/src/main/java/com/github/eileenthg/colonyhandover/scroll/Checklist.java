package com.github.eileenthg.colonyhandover.scroll;

import com.github.eileenthg.colonyhandover.mapping.BuildingMapping;
import com.github.eileenthg.colonyhandover.place.PlacementEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What one building still needs before MCA will call it by its proper name.
 *
 * <h2>Why this is a separate thing from the readiness report</h2>
 *
 * {@code /colonyhandover readiness} answers "how is the colony doing" - every
 * building at once, from wherever you are standing. This answers "what do I fetch
 * for <em>this</em> one", which is the question you have while holding a stack of
 * chests in a half-finished room. Same underlying dry run, different question, and
 * the shape of the answer differs enough that sharing the rendering would help
 * neither.
 *
 * <h2>Nothing is written</h2>
 *
 * This runs {@link PlacementEngine} in dry-run mode, which scans and plans but never
 * places. Since the planner it uses is the same one real placement uses, "room for 2
 * of 4" here means exactly what it would mean then - beds counted as the two
 * adjacent cells they actually are.
 */
public final class Checklist {

    /**
     * @param missing  requirement group to how many are still wanted
     * @param roomFor  how many of those would physically fit right now
     * @param status   why there is nothing to list, when there is nothing to list
     */
    public record Result(String hutId, String mcaType, BlockPos at,
                         Map<ResourceLocation, Integer> missing, int roomFor,
                         Status status) {

        public int wanted() {
            return missing.values().stream().mapToInt(Integer::intValue).sum();
        }

        public boolean cramped() {
            return roomFor < wanted();
        }

        public List<String> groupNames() {
            final List<String> names = new ArrayList<>();
            missing.forEach((group, count) -> names.add(count + "x " + group.getPath()));
            return names;
        }
    }

    public enum Status {
        /** Listed below - fetch these. */
        NEEDS_BLOCKS,
        /** Already satisfies its type; nothing to do. */
        COMPLETE,
        /** The survey walks out of it. Seal it before worrying about furniture. */
        NOT_ENCLOSED,
        /** Never becomes an MCA building - job sites, marker swaps, anchors. */
        NOT_A_ROOM,
        /** Our mapping has no block for something the type wants. Our bug, not yours. */
        UNMAPPABLE
    }

    public static Result of(ServerLevel level, BlockPos hutPos, String hutId) {
        final BuildingMapping.Conversion plan = BuildingMapping.forHut(hutId);
        if (plan.anchor() != BuildingMapping.Anchor.ROOM) {
            return new Result(hutId, plan.mcaType(), hutPos, Map.of(), 0, Status.NOT_A_ROOM);
        }

        final PlacementEngine.Report report =
                PlacementEngine.run(level, hutId, hutPos, null, true);

        return switch (report.outcome()) {
            case ALREADY_MATCHES -> new Result(hutId, plan.mcaType(), hutPos,
                    Map.of(), 0, Status.COMPLETE);
            case UNSATISFIABLE -> new Result(hutId, plan.mcaType(), hutPos,
                    new LinkedHashMap<>(report.missing()), report.planned().size(),
                    Status.UNMAPPABLE);
            case WOULD_PLACE -> new Result(hutId, plan.mcaType(), hutPos,
                    new LinkedHashMap<>(report.missing()), report.planned().size(),
                    Status.NEEDS_BLOCKS);
            // NO_SEED and SCAN_REFUSED both mean the same thing to a player holding
            // a scroll: there is no enclosed room here yet.
            default -> new Result(hutId, plan.mcaType(), hutPos,
                    Map.of(), 0, Status.NOT_ENCLOSED);
        };
    }

    private Checklist() {}
}
