package com.github.eileen.llvoices.voice;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One maid's pending lines and cooldowns. Lines queue up while she is busy (another bubble is showing, or she spoke
 * a moment ago) and the most important one goes first; stale ones expire instead of piling up.
 */
public final class VoiceQueue {
    /** A line that could not be said within a minute is no longer relevant. */
    public static final long PENDING_TTL_TICKS = 60 * 20;
    static final int MAX_PENDING = 6;

    private final List<VoiceRequest> pending = new ArrayList<>();
    private final Map<String, Long> lastSpokenByTopic = new HashMap<>();
    private long lastLineTick = Long.MIN_VALUE / 2;

    /** Queues a line; a newer line on the same topic replaces the older one. */
    public void offer(VoiceRequest request) {
        this.pending.removeIf(old -> old.topic().equals(request.topic()));
        this.pending.add(request);
        if (this.pending.size() > MAX_PENDING) {
            this.pending.stream().min(Comparator.comparingInt(VoiceRequest::priority)
                            .thenComparingLong(r -> -r.createdTick()))
                    .ifPresent(this.pending::remove);
        }
    }

    /** Drops pending lines whose topic starts with the prefix, e.g. a "getting worse" line once it is getting better. */
    public void dropTopicsStartingWith(String prefix) {
        this.pending.removeIf(request -> request.topic().startsWith(prefix));
    }

    public boolean hasPending(VoiceKind kind) {
        return this.pending.stream().anyMatch(request -> request.kind() == kind);
    }

    public boolean isEmpty() {
        return this.pending.isEmpty();
    }

    public void clear() {
        this.pending.clear();
    }

    /**
     * Picks the line to say now, or null if she should stay quiet this time. The chosen line is removed and its
     * topic goes on cooldown; lines whose topic is still on cooldown are discarded.
     *
     * @param minGapTicks quiet time required since her last line
     * @param sleeping    whether she is asleep, which mutes everything but urgent lines
     */
    @Nullable
    public VoiceRequest poll(long now, long minGapTicks, boolean sleeping) {
        this.pending.removeIf(request -> now - request.createdTick() > PENDING_TTL_TICKS);
        if (this.pending.isEmpty() || now - this.lastLineTick < minGapTicks) {
            return null;
        }
        this.pending.sort(Comparator.comparingInt(VoiceRequest::priority).reversed()
                .thenComparingLong(VoiceRequest::createdTick));
        for (VoiceRequest request : List.copyOf(this.pending)) {
            if (sleeping && !request.kind().allowedWhileSleeping()) {
                continue;
            }
            this.pending.remove(request);
            Long last = this.lastSpokenByTopic.get(request.topic());
            if (last != null && now - last < request.kind().cooldownSeconds() * 20L) {
                continue;
            }
            this.lastSpokenByTopic.put(request.topic(), now);
            this.lastLineTick = now;
            return request;
        }
        return null;
    }

    public List<VoiceRequest> pendingView() {
        return List.copyOf(this.pending);
    }
}
