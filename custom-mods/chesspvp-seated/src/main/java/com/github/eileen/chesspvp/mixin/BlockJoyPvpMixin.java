package com.github.eileen.chesspvp.mixin;

import com.github.eileen.chesspvp.PvpSeats;
import com.github.tartaricacid.touhoulittlemaid.block.BlockJoy;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityJoy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockJoy.class)
public abstract class BlockJoyPvpMixin {
    // 棋盘被破坏时，把两侧的玩家座位一起清掉
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void chesspvp$removeSeats(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving, CallbackInfo ci) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof TileEntityJoy joy) {
            PvpSeats.discardSeats(serverLevel, joy);
        }
    }
}
