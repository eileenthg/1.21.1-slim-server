package eteil.maidff;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Call &amp; Response's wandering traders sell maids that stand leashed beside them. If one of those maids ends up
 * without her leash (it snapped, or never reloaded after a chunk unload) she should not turn on the player: she
 * runs away from the player or trader and vanishes once she is out of sight.
 *
 * Her "for sale" flag is Call &amp; Response's own persistent tag; once the player buys her the flag is cleared and
 * this stops touching her. Everything is reflection-free but only reads that tag, so without Call &amp; Response
 * nothing is ever tracked.
 */
final class TradingMaidFlee {
    private static final String TRADING_TAG = "callresponse:trading_maid_active";
    private static final String TRADER_TAG = "callresponse:trading_maid_trader";
    private static final String FLEE_SINCE_TAG = "maidff:flee_since";
    private static final String FLEE_DX_TAG = "maidff:flee_dx";
    private static final String FLEE_DZ_TAG = "maidff:flee_dz";

    /** A maid that has only just loaded has not had the chance to restore her leash yet. */
    private static final int LEASH_GRACE_TICKS = 100;
    private static final double PLAYER_FEAR_RANGE = 16.0;
    private static final double RUN_STEP = 12.0;
    private static final double VANISH_DISTANCE = 24.0;
    private static final long MIN_FLEE_TICKS = 60;
    /** Stuck maids that no player is anywhere near still go away eventually. */
    private static final long GIVE_UP_TICKS = 1800;
    private static final double GIVE_UP_DISTANCE = 8.0;

    private static final Set<Mob> TRACKED = Collections.newSetFromMap(new WeakHashMap<>());

    private TradingMaidFlee() {
    }

    static boolean isStockMaid(Entity entity) {
        return entity.getPersistentData().getBoolean(TRADING_TAG);
    }

    static void onJoin(Entity entity) {
        if (entity instanceof Mob mob && MaidFriendlyFire.isMaid(entity) && isStockMaid(entity)) {
            TRACKED.add(mob);
        }
    }

    static void tick() {
        for (Mob maid : new ArrayList<>(TRACKED)) {
            if (maid.isRemoved() || !(maid.level() instanceof ServerLevel level)) {
                if (maid.isRemoved()) TRACKED.remove(maid);
                continue;
            }
            CompoundTag data = maid.getPersistentData();
            if (!data.getBoolean(TRADING_TAG)) {
                // Bought (or otherwise released): she is a normal maid again.
                data.remove(FLEE_SINCE_TAG);
                data.remove(FLEE_DX_TAG);
                data.remove(FLEE_DZ_TAG);
                TRACKED.remove(maid);
                continue;
            }
            try {
                if (data.contains(FLEE_SINCE_TAG)) {
                    flee(level, maid, data);
                } else if (!maid.isLeashed() && maid.tickCount > LEASH_GRACE_TICKS) {
                    startFleeing(level, maid, data);
                    flee(level, maid, data);
                }
            } catch (RuntimeException e) {
                MaidFriendlyFire.LOG.warn("[MaidFriendlyFire] could not make stock maid {} flee: {}", maid.getId(), e.toString());
                TRACKED.remove(maid);
            }
        }
    }

    private static void startFleeing(ServerLevel level, Mob maid, CompoundTag data) {
        Entity from = level.getNearestPlayer(maid, PLAYER_FEAR_RANGE);
        if (from == null && data.hasUUID(TRADER_TAG)) from = level.getEntity(data.getUUID(TRADER_TAG));
        if (from == null) from = level.getNearestPlayer(maid, VANISH_DISTANCE);

        double dx = 0;
        double dz = 0;
        if (from != null) {
            dx = maid.getX() - from.getX();
            dz = maid.getZ() - from.getZ();
        }
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.5) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
        } else {
            dx /= length;
            dz /= length;
        }
        data.putLong(FLEE_SINCE_TAG, level.getGameTime());
        data.putDouble(FLEE_DX_TAG, dx);
        data.putDouble(FLEE_DZ_TAG, dz);
        MaidFriendlyFire.LOG.info("[MaidFriendlyFire] stock maid {} lost her leash, sending her running", maid.getId());
    }

    @SuppressWarnings("unchecked")
    private static void flee(ServerLevel level, Mob maid, CompoundTag data) {
        long elapsed = level.getGameTime() - data.getLong(FLEE_SINCE_TAG);
        Player nearest = level.getNearestPlayer(maid, VANISH_DISTANCE);
        if ((elapsed > MIN_FLEE_TICKS && nearest == null)
                || (elapsed > GIVE_UP_TICKS && level.getNearestPlayer(maid, GIVE_UP_DISTANCE) == null)) {
            maid.discard();
            TRACKED.remove(maid);
            return;
        }

        maid.setTarget(null);
        double x = maid.getX() + data.getDouble(FLEE_DX_TAG) * RUN_STEP;
        double z = maid.getZ() + data.getDouble(FLEE_DZ_TAG) * RUN_STEP;
        BlockPos column = BlockPos.containing(x, maid.getY(), z);
        double y = level.hasChunkAt(column)
                ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ())
                : maid.getY();
        Brain<LivingEntity> brain = (Brain<LivingEntity>) (Brain<?>) maid.getBrain();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new Vec3(x, y, z), 1.0F, 1));
    }
}
