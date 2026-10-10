package com.github.eileenthg.colonyhandover.handover;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The confirmation gate for an irreversible operation.
 *
 * <h2>Why a token and not "type the command twice"</h2>
 *
 * A repeated command is defeated by the thing it is supposed to prevent: pressing
 * up-enter twice. A token cannot be produced by repetition - you have to read it
 * off the dry run and type it back, which means you have to have looked at the
 * report. That is the whole point. It is a speed bump against your own muscle
 * memory, not a security measure, and it is sized accordingly.
 *
 * <h2>What it is bound to</h2>
 *
 * Player, colony and dimension. Binding the colony is the part that matters: it
 * makes it impossible to preview one colony, walk to another, and confirm there.
 * Tokens also expire, so a token left in the scrollback from an hour ago cannot be
 * pasted into a world that has moved on.
 */
public final class HandoverToken {

    /** Long enough not to be guessed by accident, short enough to retype. */
    private static final int LENGTH = 5;

    /**
     * No vowels, so the generator cannot produce a real word, and no characters
     * that blur together in the chat font: 0/O, 1/I/L.
     */
    private static final String ALPHABET = "23456789BCDFGHJKMNPQRSTVWXZ";

    /** Long enough to read the report, short enough that a stale token is rare. */
    private static final long LIFETIME_MS = 120_000L;

    private record Pending(String token, int colonyId, ResourceKey<Level> dimension, long issuedAt) {}

    private static final Map<UUID, Pending> OUTSTANDING = new HashMap<>();
    private static final Random RANDOM = new Random();

    /** Issues a token for this player and colony, replacing any previous one. */
    public static String issue(UUID player, int colonyId, ResourceKey<Level> dimension) {
        final StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        final String token = sb.toString();
        OUTSTANDING.put(player, new Pending(token, colonyId, dimension, System.currentTimeMillis()));
        return token;
    }

    /** Why a confirmation was refused, so the player is told which mistake they made. */
    public enum Result { OK, NONE_ISSUED, EXPIRED, WRONG_TOKEN, WRONG_COLONY }

    /**
     * Checks a token and, on success, consumes it.
     *
     * <p>Consumed on success only. A mistyped token should not force you back
     * through the dry run, but a used one must never work twice.
     */
    public static Result check(UUID player, String token, int colonyId,
                               ResourceKey<Level> dimension) {
        final Pending pending = OUTSTANDING.get(player);
        if (pending == null) {
            return Result.NONE_ISSUED;
        }
        if (System.currentTimeMillis() - pending.issuedAt() > LIFETIME_MS) {
            OUTSTANDING.remove(player);
            return Result.EXPIRED;
        }
        if (!pending.token().equalsIgnoreCase(token)) {
            return Result.WRONG_TOKEN;
        }
        if (pending.colonyId() != colonyId || !pending.dimension().equals(dimension)) {
            return Result.WRONG_COLONY;
        }
        OUTSTANDING.remove(player);
        return Result.OK;
    }

    /** How long a freshly issued token lasts, for the message that offers it. */
    public static long lifetimeSeconds() {
        return LIFETIME_MS / 1000L;
    }

    private HandoverToken() {}
}
