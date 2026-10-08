package net.eileenthg.guidelibrary.unlock;

import com.mojang.serialization.Codec;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The book keys one player has unlocked. Stored on the player as a data attachment. */
public final class PlayerUnlocks {

    public static final Codec<PlayerUnlocks> CODEC =
            Codec.STRING.listOf().xmap(PlayerUnlocks::new, p -> List.copyOf(p.keys));

    private final Set<String> keys = new LinkedHashSet<>();

    public PlayerUnlocks() {}

    private PlayerUnlocks(List<String> keys) {
        this.keys.addAll(keys);
    }

    public boolean add(String key) {
        return keys.add(key);
    }

    public boolean remove(String key) {
        return keys.remove(key);
    }

    public boolean contains(String key) {
        return keys.contains(key);
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(keys);
    }
}
