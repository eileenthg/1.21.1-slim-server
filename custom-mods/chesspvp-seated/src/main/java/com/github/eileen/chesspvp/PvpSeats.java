package com.github.eileen.chesspvp;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityJoy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Two player seats per board, kept as TLM {@link EntitySit}s whose UUIDs live in the board's persistent data.
 * Whoever rides a seat plays that side; the seat removes itself shortly after its rider leaves.
 */
public final class PvpSeats {
    public static final int FIRST = 0;
    public static final int SECOND = 1;

    private static final String[] SEAT_KEYS = {"ChessPvpSeat1", "ChessPvpSeat2"};
    // 旧版 ChessPVP（潜行右键）写入的键，已不再使用
    private static final String[] LEGACY_KEYS = {"ChessPvpP1", "ChessPvpP2", "ChessPvpTurn"};
    // 玩家比女仆坐得高一些，像坐在椅子上，腿不会陷进地面
    private static final double SEAT_HEIGHT = 0.45;

    private PvpSeats() {
    }

    /**
     * Seat positions as horizontal offsets from the middle of the board.
     * {@link #FIRST} is the side a lone human sits on (opposite the maid) and moves first.
     */
    public record Layout(Vec3 first, Vec3 second) {
        /** Chess and Chinese chess: the maid sits two blocks out along the opposite of FACING. */
        public static Layout chess(Direction facing) {
            return new Layout(offset(facing, 2), offset(facing.getOpposite(), 2));
        }

        /** Gomoku: the maid sits 1.5 blocks out along the clockwise side of FACING. */
        public static Layout gomoku(Direction facing) {
            Direction maidSide = facing.getClockWise();
            return new Layout(offset(maidSide.getOpposite(), 1.5), offset(maidSide, 1.5));
        }

        private static Vec3 offset(Direction side, double distance) {
            return new Vec3(side.getStepX() * distance, 0, side.getStepZ() * distance);
        }

        public Vec3 offset(int slot) {
            return slot == FIRST ? first : second;
        }

        /** The seat's yaw, facing the middle of the board. */
        public float yRot(int slot) {
            Vec3 off = offset(slot);
            return (float) (Mth.atan2(off.x, -off.z) * Mth.RAD_TO_DEG);
        }

        /** The seat closest to a world position. */
        public int nearestSlot(BlockPos center, Vec3 worldPos) {
            double dx = worldPos.x - (center.getX() + 0.5);
            double dz = worldPos.z - (center.getZ() + 0.5);
            return dx * first.x + dz * first.z >= 0 ? FIRST : SECOND;
        }
    }

    public static int other(int slot) {
        return slot == FIRST ? SECOND : FIRST;
    }

    /** A maid sitting at the board means the original player-versus-maid game is in charge. */
    public static boolean hasMaid(ServerLevel level, TileEntityJoy board) {
        return level.getEntity(board.getSitId()) instanceof EntitySit sit && sit.getFirstPassenger() instanceof EntityMaid;
    }

    public static boolean anySeatTaken(ServerLevel level, TileEntityJoy board) {
        return isTaken(level, board, FIRST) || isTaken(level, board, SECOND);
    }

    public static boolean isTaken(ServerLevel level, TileEntityJoy board, int slot) {
        EntitySit seat = findSeat(level, board.getPersistentData(), slot);
        return seat != null && seat.getFirstPassenger() instanceof Player;
    }

    /** Which side this player is sitting on, or -1 when they are not seated at the board. */
    public static int slotOf(ServerLevel level, TileEntityJoy board, Player player) {
        CompoundTag data = board.getPersistentData();
        for (int slot = FIRST; slot <= SECOND; slot++) {
            EntitySit seat = findSeat(level, data, slot);
            if (seat != null && player.getVehicle() == seat) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean sit(ServerLevel level, BlockPos center, TileEntityJoy board, Layout layout, int slot, ServerPlayer player, String joyType) {
        CompoundTag data = board.getPersistentData();
        EntitySit seat = findSeat(level, data, slot);
        if (seat == null) {
            Vec3 off = layout.offset(slot);
            Vec3 pos = new Vec3(center.getX() + 0.5 + off.x, center.getY() + SEAT_HEIGHT, center.getZ() + 0.5 + off.z);
            seat = new EntitySit(level, pos, joyType, center);
            seat.setYRot(layout.yRot(slot));
            level.addFreshEntity(seat);
            data.putUUID(SEAT_KEYS[slot], seat.getUUID());
            for (String legacy : LEGACY_KEYS) {
                data.remove(legacy);
            }
            board.setChanged();
        }
        return player.startRiding(seat);
    }

    public static void discardSeats(ServerLevel level, TileEntityJoy board) {
        CompoundTag data = board.getPersistentData();
        for (int slot = FIRST; slot <= SECOND; slot++) {
            EntitySit seat = findSeat(level, data, slot);
            if (seat != null) {
                seat.discard();
            }
        }
    }

    /** Tell whoever is seated at the board; used for game results. */
    public static void notifySeated(ServerLevel level, TileEntityJoy board, Component message) {
        for (int slot = FIRST; slot <= SECOND; slot++) {
            EntitySit seat = findSeat(level, board.getPersistentData(), slot);
            if (seat != null && seat.getFirstPassenger() instanceof ServerPlayer player) {
                player.displayClientMessage(message, true);
            }
        }
    }

    @Nullable
    private static EntitySit findSeat(ServerLevel level, CompoundTag data, int slot) {
        if (!data.hasUUID(SEAT_KEYS[slot])) {
            return null;
        }
        Entity entity = level.getEntity(data.getUUID(SEAT_KEYS[slot]));
        return entity instanceof EntitySit seat && seat.isAlive() ? seat : null;
    }
}
