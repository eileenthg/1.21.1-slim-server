package eteil.gensokyocompat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Replaces every Youkai's Homecoming character that Legacy also has with the Legacy one, at the same spot:
 * ones already in the world when their chunk loads, ones placed by YH structures, spawn eggs and summons.
 * The YH entity never joins the level; the Legacy one is spawned on the next server tick, when adding entities is safe.
 */
public final class CharacterSwap {
    private record Pending(ServerLevel level, EntityType<?> type, double x, double y, double z, float yRot, Component name) {}

    private static final Queue<Pending> PENDING = new ArrayDeque<>();
    public static int swapped;

    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity e = event.getEntity();
        EntityType<?> legacy = Bridge.legacyFor(e.getType());
        if (legacy == null) return;
        event.setCanceled(true);
        synchronized (PENDING) {
            PENDING.add(new Pending(level, legacy, e.getX(), e.getY(), e.getZ(), e.getYRot(), e.getCustomName()));
        }
    }

    public static void onTick(ServerTickEvent.Post event) {
        while (true) {
            Pending p;
            synchronized (PENDING) { p = PENDING.poll(); }
            if (p == null) return;
            Entity created = p.type.spawn(p.level, BlockPos.containing(p.x, p.y, p.z), MobSpawnType.CONVERSION);
            if (created == null) continue;
            created.moveTo(p.x, p.y, p.z, p.yRot, 0);
            if (p.name != null) created.setCustomName(p.name);
            swapped++;
        }
    }

    private CharacterSwap() {}
}
