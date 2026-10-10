package com.github.eileen.workingmaidcomputers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Recognises "this player is sitting at that Touhou Little Maid computer" using only registry names, so the addon
 * has no compile-time dependency on TLM. The same check runs on both logical sides.
 */
public final class MaidComputerSeat {
    private static final ResourceLocation COMPUTER_BLOCK = ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "computer");
    private static final ResourceLocation SIT_ENTITY = ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "sit");
    /** TLM spawns the seat at the block's lower corner plus (0.5, 1, 0.5); allow some slack for float drift. */
    private static final double SEAT_TOLERANCE_SQR = 0.25D;

    private MaidComputerSeat() {
    }

    public static boolean isSeatedAt(Player player, Level level, BlockPos pos) {
        Entity vehicle = player.getVehicle();
        if (vehicle == null || !SIT_ENTITY.equals(BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()))) {
            return false;
        }
        if (!COMPUTER_BLOCK.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()))) {
            return false;
        }
        return vehicle.position().distanceToSqr(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D) < SEAT_TOLERANCE_SQR;
    }
}
