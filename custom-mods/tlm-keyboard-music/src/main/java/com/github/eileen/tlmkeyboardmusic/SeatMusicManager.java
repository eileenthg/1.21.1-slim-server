package com.github.eileen.tlmkeyboardmusic;

import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side bookkeeping for which keyboard seat is playing what. The server decides, so everyone
 * nearby hears the same track.
 */
public final class SeatMusicManager {
    /** Pause between a seated maid's pieces, and before her first one, in ticks. */
    private static final int MAID_GAP_TICKS = 60;
    private static final Map<UUID, SeatState> STATES = new HashMap<>();

    private SeatMusicManager() {
    }

    public static void clear() {
        STATES.clear();
    }

    /** Called every tick for each keyboard seat. A seated maid plays on her own. */
    public static void tickSeat(ServerLevel level, Entity sit) {
        Entity passenger = sit.getFirstPassenger();
        if (passenger == null) {
            STATES.remove(sit.getUUID());
            return;
        }
        long now = level.getGameTime();
        SeatState state = STATES.computeIfAbsent(sit.getUUID(), id -> new SeatState(now + MAID_GAP_TICKS));
        if (KeyboardSeats.isMaid(passenger) && !state.isPlaying(now) && now >= state.maidNextStart) {
            start(level, sit, state, KeyboardSeats.maidModelId(passenger), now);
        }
    }

    /** Called when a player seated on the keyboard right-clicks it. Does nothing while a piece is playing. */
    public static void playerPlay(ServerLevel level, Entity sit, Player player) {
        if (player.getVehicle() != sit) {
            return;
        }
        long now = level.getGameTime();
        SeatState state = STATES.computeIfAbsent(sit.getUUID(), id -> new SeatState(Long.MAX_VALUE));
        if (!state.isPlaying(now)) {
            start(level, sit, state, null, now);
        }
    }

    /** Lets a player who walks into range of a seat that is already playing hear it too. */
    public static void resend(ServerLevel level, Entity sit, net.minecraft.server.level.ServerPlayer viewer) {
        SeatState state = STATES.get(sit.getUUID());
        if (state != null && state.track != null && state.isPlaying(level.getGameTime())) {
            PacketDistributor.sendToPlayer(viewer, payload(sit, state.track));
        }
    }

    private static void start(ServerLevel level, Entity sit, SeatState state, @Nullable String modelId, long now) {
        KeyboardTrack track = pick(level, modelId, state.lastSoundId, level.getRandom());
        if (track == null) {
            // Nothing loaded: back off so a seated maid does not re-scan the registry every tick.
            state.maidNextStart = now + 20L * 30;
            return;
        }
        state.track = track;
        state.lastSoundId = track.soundId();
        state.playingUntil = now + track.lengthSeconds() * 20L;
        state.maidNextStart = state.playingUntil + MAID_GAP_TICKS;
        PacketDistributor.sendToPlayersTrackingEntity(sit, payload(sit, track));
    }

    private static PlayKeyboardMusicPayload payload(Entity sit, KeyboardTrack track) {
        return new PlayKeyboardMusicPayload(sit.getId(), track.sound(), track.volume());
    }

    /**
     * Weighted pick among the tracks this player or maid may play. The piece that just finished is
     * left out unless it is the only choice.
     */
    @Nullable
    static KeyboardTrack pick(ServerLevel level, @Nullable String modelId, @Nullable String exclude, RandomSource random) {
        Registry<KeyboardTrack> registry = level.registryAccess().registry(KeyboardMusicRegistries.KEYBOARD_TRACKS).orElse(null);
        if (registry == null) {
            return null;
        }
        List<KeyboardTrack> pool = new ArrayList<>();
        for (KeyboardTrack track : registry) {
            if (track.canBePlayedBy(modelId)) {
                pool.add(track);
            }
        }
        if (pool.size() > 1 && exclude != null) {
            pool.removeIf(track -> track.soundId().equals(exclude));
        }
        int total = 0;
        for (KeyboardTrack track : pool) {
            total += track.weight();
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (KeyboardTrack track : pool) {
            roll -= track.weight();
            if (roll < 0) {
                return track;
            }
        }
        return null;
    }

    private static final class SeatState {
        private long maidNextStart;
        private long playingUntil = 0;
        @Nullable
        private KeyboardTrack track;
        @Nullable
        private String lastSoundId;

        private SeatState(long maidNextStart) {
            this.maidNextStart = maidNextStart;
        }

        private boolean isPlaying(long now) {
            return now < this.playingUntil;
        }
    }
}
