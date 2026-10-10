package com.github.eileenthg.colonyhandover.scroll;

import com.github.eileenthg.colonyhandover.mapping.BuildingMapping;
import com.github.eileenthg.colonyhandover.place.Enclosure;
import com.github.eileenthg.colonyhandover.place.Placer;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Walks a colony and writes down what is outstanding.
 *
 * <h2>Two questions per building, in order</h2>
 *
 * Is it sealed, and if so is it furnished. The order matters: a room the survey
 * walks out of cannot be fixed with furniture however much you put in it, so
 * reporting "needs 4 chests" about an open-walled shed would send you off doing the
 * wrong job. Enclosure is checked first and short-circuits.
 *
 * <h2>Two ways in</h2>
 *
 * {@link #of} enumerates a live colony, which is how a scroll is first bound.
 * {@link #refresh} re-surveys from a list the scroll already carries, which is how
 * it keeps working after the handover - by then the colony is unregistered and the
 * hut blocks are gone, so there is nothing left to enumerate and the scroll's own
 * memory is the only roster there is.
 */
public final class IssueSurvey {

    private static final int SEED_RADIUS = 8;
    private static final int SEED_VERTICAL = 6;

    public static ColonyIssues of(ServerLevel level, IColony colony) {
        final List<ColonyIssues.Entry> entries = new ArrayList<>();
        for (final Map.Entry<BlockPos, IBuilding> e
                : colony.getServerBuildingManager().getBuildings().entrySet()) {
            entries.add(inspect(level, e.getKey().immutable(),
                    e.getValue().getBuildingType().getRegistryName().getPath()));
        }
        return new ColonyIssues(colony.getName(), colony.getCenter().immutable(), entries,
                level.getGameTime());
    }

    public static ColonyIssues refresh(ServerLevel level, ColonyIssues previous) {
        final List<ColonyIssues.Entry> entries = new ArrayList<>();
        for (final ColonyIssues.Entry old : previous.buildings()) {
            entries.add(inspect(level, old.pos(), old.hutId()));
        }
        return new ColonyIssues(previous.colonyName(), previous.center(), entries,
                level.getGameTime());
    }

    /**
     * The survey for wherever the player is standing.
     *
     * <p>Inside a live colony, enumerate it afresh, so huts built since the scroll was
     * last used appear. Anywhere else - including after the handover, when no colony
     * exists - re-walk the roster the scroll already carries.
     *
     * @return null only for a blank scroll used outside any colony
     */
    public static ColonyIssues surveyAt(ServerLevel level, BlockPos where, ColonyIssues previous) {
        final IColony colony = IMinecoloniesAPI.getInstance().getColonyManager()
                .getIColony(level, where);
        if (colony != null) {
            return of(level, colony);
        }
        return previous == null ? null : refresh(level, previous);
    }

    private static ColonyIssues.Entry inspect(ServerLevel level, BlockPos pos, String hutId) {
        final BuildingMapping.Conversion plan = BuildingMapping.forHut(hutId);
        if (plan.anchor() != BuildingMapping.Anchor.ROOM) {
            return new ColonyIssues.Entry(hutId, pos, plan.mcaType(),
                    ColonyIssues.Status.NOT_A_ROOM, Optional.empty(), Map.of(), 0);
        }

        // Enclosure first. A gap is both the more basic problem and the more
        // actionable one - it comes with a coordinate you can walk to.
        final Optional<BlockPos> seed =
                Placer.findInteriorSeed(level, pos, SEED_RADIUS, SEED_VERTICAL);
        if (seed.isEmpty()) {
            // No roofed, standable cell near the hut. MarkTracker skips exactly this
            // case, so calling it "not sealed" pointed at a gap marks could never show.
            return new ColonyIssues.Entry(hutId, pos, plan.mcaType(),
                    ColonyIssues.Status.INACCESSIBLE, Optional.empty(), Map.of(), 0);
        }
        final Optional<Enclosure.Leak> leak = Enclosure.findLeak(level, seed.get());
        if (leak.isPresent()) {
            final Enclosure.Leak found = leak.get();
            return new ColonyIssues.Entry(hutId, pos, plan.mcaType(),
                    ColonyIssues.Status.NOT_SEALED,
                    Optional.of(found.ceiling() ? found.escape() : found.crossing()),
                    Map.of(), 0);
        }

        final Checklist.Result result = Checklist.of(level, pos, hutId);
        final Map<String, Integer> missing = new LinkedHashMap<>();
        result.missing().forEach((group, count) -> missing.put(group.getPath(), count));

        final ColonyIssues.Status status = switch (result.status()) {
            case COMPLETE -> ColonyIssues.Status.DONE;
            case UNMAPPABLE -> ColonyIssues.Status.UNMAPPABLE;
            case NOT_A_ROOM -> ColonyIssues.Status.NOT_A_ROOM;
            // Our walk found it sealed, yet MCA's scan still refused it. There is no
            // gap to walk to, so this is not "not sealed" in any sense marks can help.
            case NOT_ENCLOSED -> ColonyIssues.Status.INACCESSIBLE;
            default -> ColonyIssues.Status.NEEDS_BLOCKS;
        };
        return new ColonyIssues.Entry(hutId, pos, plan.mcaType(), status,
                Optional.empty(), missing, result.roomFor());
    }

    private IssueSurvey() {}
}
