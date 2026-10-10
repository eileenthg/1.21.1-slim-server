package com.github.eileenthg.colonyhandover.handover;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A handover paused between stages, for {@code handover debug}.
 *
 * <h2>Why stepping matters for this operation specifically</h2>
 *
 * The handover is irreversible and spans two mods' data models. Run end to end, a
 * failure tells you only that it failed - and by then the colony is gone, so there
 * is no re-running it to narrow things down. Stepping lets you look at the world
 * between stages: are the villagers where you expected, did the bell land, did the
 * family screen fill in. If something is wrong you stop there instead of finding
 * out after four more stages have run on top of it.
 *
 * <h2>Why the session has to hold state</h2>
 *
 * Two things cannot be re-derived once you start. The snapshot is taken while the
 * colony still exists and is unreadable afterwards (finding #1), and the
 * citizen-id to villager map produced by the spawn stage is what the wiring stage
 * links relationships with (finding #5). Holding both here is what lets the stages
 * run minutes apart instead of in one call.
 *
 * <p>Deliberately in memory only. A half-finished handover surviving a server
 * restart would be worse than losing it: the snapshot would describe a colony that
 * no longer exists, against a world that has moved on.
 */
public final class HandoverSession {

    public enum Stage {
        SNAPSHOT("snapshot the colony"),
        DELETE("delete the colony"),
        PLACE("place workstations"),
        SPAWN("spawn villagers"),
        WIRE("wire the family tree"),
        SCAN("hand buildings to MCA"),
        DONE("finished");

        private final String description;

        Stage(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }

        public Stage next() {
            return this == DONE ? DONE : values()[ordinal() + 1];
        }
    }

    private static final Map<UUID, HandoverSession> ACTIVE = new HashMap<>();

    private final int colonyId;
    private Stage stage = Stage.SNAPSHOT;
    private ColonySnapshot snapshot;
    private Map<Integer, VillagerEntityMCA> villagers = Map.of();

    private HandoverSession(int colonyId) {
        this.colonyId = colonyId;
    }

    public static HandoverSession start(UUID player, int colonyId) {
        final HandoverSession session = new HandoverSession(colonyId);
        ACTIVE.put(player, session);
        return session;
    }

    public static HandoverSession of(UUID player) {
        return ACTIVE.get(player);
    }

    public static void end(UUID player) {
        ACTIVE.remove(player);
    }

    public Stage stage() {
        return stage;
    }

    public int colonyId() {
        return colonyId;
    }

    public ColonySnapshot snapshot() {
        return snapshot;
    }

    public boolean finished() {
        return stage == Stage.DONE;
    }

    /**
     * Runs whichever stage is next and advances.
     *
     * @return a one-line summary of what that stage did
     */
    public String advance(ServerLevel level, ColonySnapshot fresh) {
        final String summary;
        switch (stage) {
            case SNAPSHOT -> {
                snapshot = fresh;
                summary = snapshot.huts().size() + " building(s), "
                        + snapshot.citizens().size() + " citizen(s) captured.";
            }
            case DELETE -> {
                final int retired = Handover.delete(level, snapshot);
                summary = "Colony " + colonyId + " deleted, " + retired
                        + " citizen(s) retired quietly. Structures left standing.";
            }
            case PLACE -> {
                final Handover.PlaceOutcome out = Handover.place(level, snapshot);
                summary = out.stations() + " workstation(s), " + out.anchors()
                        + " village anchor(s), " + out.swapped() + " field marker(s), "
                        + out.shortOfRoom() + " with nowhere to stand a block, "
                        + out.skipped() + " skipped.";
            }
            case SPAWN -> {
                final Handover.SpawnOutcome out = Handover.spawn(level, snapshot);
                villagers = out.villagers();
                summary = out.spawned() + " spawned, " + out.failed() + " failed, "
                        + out.professions() + " with a profession.";
            }
            case WIRE -> {
                final Handover.WireOutcome out = Handover.wire(level, snapshot, villagers);
                summary = out.couples() + " couple(s), " + out.parentLinks()
                        + " parent link(s), " + out.deceased() + " deceased ancestor(s).";
            }
            case SCAN -> {
                final Handover.ScanOutcome out = Handover.scan(level, snapshot);
                summary = out.adopted() + " handed to MCA, " + out.refused() + " refused.";
            }
            default -> {
                return "Already finished.";
            }
        }
        stage = stage.next();
        return summary;
    }
}
