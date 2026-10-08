package net.eileenthg.maidchef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Ingredients Chef maids have asked their owner for, shared by all of that owner's maids. An item only appears after a
 * maid asks for it; the owner can block it (never ask again) and unblock it later. Saved with the world.
 */
public final class ChefRequests extends SavedData {
    public record Entry(boolean blocked, String forDish, long lastAsked, int times) {
    }

    private final Map<UUID, Map<String, Entry>> byOwner = new HashMap<>();

    public static ChefRequests get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ChefRequests::new, ChefRequests::load, null), "maidchef_requests");
    }

    public synchronized Map<String, Entry> list(UUID owner) {
        return new LinkedHashMap<>(byOwner.getOrDefault(owner, Map.of()));
    }

    public synchronized Set<Item> blocked(UUID owner) {
        Set<Item> out = new HashSet<>();
        byOwner.getOrDefault(owner, Map.of()).forEach((id, e) -> {
            if (e.blocked()) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) BuiltInRegistries.ITEM.getOptional(rl).ifPresent(out::add);
            }
        });
        return out;
    }

    public synchronized void asked(UUID owner, Item item, Item dish, long time) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        Map<String, Entry> m = byOwner.computeIfAbsent(owner, k -> new LinkedHashMap<>());
        Entry old = m.get(id);
        m.put(id, new Entry(old != null && old.blocked(), BuiltInRegistries.ITEM.getKey(dish).toString(), time, old == null ? 1 : old.times() + 1));
        setDirty();
    }

    public synchronized void setBlocked(UUID owner, String id, boolean blocked) {
        Map<String, Entry> m = byOwner.computeIfAbsent(owner, k -> new LinkedHashMap<>());
        Entry old = m.get(id);
        m.put(id, old == null ? new Entry(blocked, "", 0, 0) : new Entry(blocked, old.forDish(), old.lastAsked(), old.times()));
        setDirty();
    }

    public synchronized void forget(UUID owner, String id) {
        Map<String, Entry> m = byOwner.get(owner);
        if (m != null && m.remove(id) != null) setDirty();
    }

    /** For the self-test: drop everything about one owner. */
    public synchronized void clear(UUID owner) {
        if (byOwner.remove(owner) != null) setDirty();
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag owners = new ListTag();
        byOwner.forEach((owner, entries) -> {
            CompoundTag o = new CompoundTag();
            o.putUUID("Owner", owner);
            ListTag list = new ListTag();
            entries.forEach((id, e) -> {
                CompoundTag t = new CompoundTag();
                t.putString("Item", id);
                t.putBoolean("Blocked", e.blocked());
                t.putString("For", e.forDish());
                t.putLong("Last", e.lastAsked());
                t.putInt("Times", e.times());
                list.add(t);
            });
            o.put("Items", list);
            owners.add(o);
        });
        tag.put("Owners", owners);
        return tag;
    }

    private static ChefRequests load(CompoundTag tag, HolderLookup.Provider registries) {
        ChefRequests r = new ChefRequests();
        for (Tag ot : tag.getList("Owners", Tag.TAG_COMPOUND)) {
            CompoundTag o = (CompoundTag) ot;
            Map<String, Entry> m = new LinkedHashMap<>();
            for (Tag it : o.getList("Items", Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) it;
                m.put(t.getString("Item"), new Entry(t.getBoolean("Blocked"), t.getString("For"), t.getLong("Last"), t.getInt("Times")));
            }
            r.byOwner.put(o.getUUID("Owner"), m);
        }
        return r;
    }
}
