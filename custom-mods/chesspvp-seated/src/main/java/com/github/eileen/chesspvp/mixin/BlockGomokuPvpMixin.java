package com.github.eileen.chesspvp.mixin;

import com.github.eileen.chesspvp.PvpSeats;
import com.github.tartaricacid.touhoulittlemaid.api.game.gomoku.Point;
import com.github.tartaricacid.touhoulittlemaid.api.game.gomoku.Statue;
import com.github.tartaricacid.touhoulittlemaid.block.BlockGomoku;
import com.github.tartaricacid.touhoulittlemaid.block.properties.GomokuPart;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidGomokuAI;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityGomoku;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockGomoku.class)
public abstract class BlockGomokuPvpMixin {
    @Shadow
    private static boolean isClickChessBox(double x, double y, GomokuPart part, Direction direction) {
        throw new AssertionError();
    }

    @Shadow
    private static int[] getChessPos(double x, double y, GomokuPart part) {
        throw new AssertionError();
    }

    // 有玩家坐在棋盘边时，女仆不再入座，避免两种玩法混在一起
    @Inject(method = "startMaidSit", at = @At("HEAD"), cancellable = true)
    private void chesspvp$keepSeatsFree(EntityMaid maid, BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof TileEntityGomoku gomoku && PvpSeats.anySeatTaken(serverLevel, gomoku)) {
            ci.cancel();
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void chesspvp$useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                    BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!(level instanceof ServerLevel serverLevel) || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        GomokuPart part = state.getValue(BlockGomoku.PART);
        BlockPos centerPos = pos.subtract(new Vec3i(part.getPosX(), 0, part.getPosY()));
        if (!(level.getBlockEntity(centerPos) instanceof TileEntityGomoku gomoku)) {
            return;
        }
        // 女仆在场，或者手里拿着残局道具等物品时，交给原版处理
        if (PvpSeats.hasMaid(serverLevel, gomoku) || !itemStack.isEmpty()) {
            return;
        }

        Direction facing = state.getValue(BlockGomoku.FACING);
        PvpSeats.Layout layout = PvpSeats.Layout.gomoku(facing);
        int slot = PvpSeats.slotOf(serverLevel, gomoku, serverPlayer);

        // 没坐下：右键就是入座
        if (slot < 0) {
            int wanted = layout.nearestSlot(centerPos, hit.getLocation());
            if (!PvpSeats.isTaken(serverLevel, gomoku, wanted)) {
                slot = wanted;
            } else if (!PvpSeats.isTaken(serverLevel, gomoku, PvpSeats.other(wanted))) {
                slot = PvpSeats.other(wanted);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("message.chesspvp.full"), true);
                cir.setReturnValue(ItemInteractionResult.FAIL);
                return;
            }
            if (serverPlayer.isPassenger() || !PvpSeats.sit(serverLevel, centerPos, gomoku, layout, slot, serverPlayer, Type.GOMOKU.getTypeName())) {
                cir.setReturnValue(ItemInteractionResult.FAIL);
                return;
            }
            serverPlayer.displayClientMessage(Component.translatable("message.chesspvp.sit",
                    Component.translatable(slot == PvpSeats.FIRST ? "chesspvp.color.black" : "chesspvp.color.white")), true);
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
            return;
        }

        Vec3 location = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());

        // 重置棋盘，座位保持不变
        if (isClickChessBox(location.x, location.z, part, facing)) {
            gomoku.reset();
            gomoku.refresh();
            level.playSound(null, centerPos, InitSounds.GOMOKU_RESET.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
            return;
        }

        if (!PvpSeats.isTaken(serverLevel, gomoku, PvpSeats.other(slot))) {
            serverPlayer.displayClientMessage(Component.translatable("message.chesspvp.waiting"), true);
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
            return;
        }
        if (gomoku.getStatue() != Statue.IN_PROGRESS) {
            cir.setReturnValue(ItemInteractionResult.FAIL);
            return;
        }
        // 黑子先手，黑白棋子数相等时轮到黑子
        int stone = chesspvp$stoneToPlay(gomoku.getChessData());
        if (stone != (slot == PvpSeats.FIRST ? Point.BLACK : Point.WHITE)) {
            serverPlayer.displayClientMessage(Component.translatable("message.chesspvp.not_your_turn"), true);
            cir.setReturnValue(ItemInteractionResult.FAIL);
            return;
        }

        int[] clickPos = getChessPos(location.x, location.z, part);
        if (clickPos == null || gomoku.getChessData()[clickPos[0]][clickPos[1]] != Point.EMPTY) {
            cir.setReturnValue(ItemInteractionResult.FAIL);
            return;
        }
        Point move = new Point(clickPos[0], clickPos[1], stone);
        gomoku.setChessData(move.x, move.y, move.type);
        Statue statue = MaidGomokuAI.getStatue(gomoku.getChessData(), move);
        gomoku.setStatue(statue);
        gomoku.refresh();
        level.playSound(null, pos, InitSounds.GOMOKU.get(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);

        if (statue == Statue.WIN) {
            PvpSeats.notifySeated(serverLevel, gomoku, Component.translatable("message.chesspvp.win",
                    Component.translatable(stone == Point.BLACK ? "chesspvp.color.black" : "chesspvp.color.white")));
        } else if (statue == Statue.DRAW) {
            PvpSeats.notifySeated(serverLevel, gomoku, Component.translatable("message.chesspvp.draw"));
        }
        cir.setReturnValue(ItemInteractionResult.SUCCESS);
    }

    @Unique
    private static int chesspvp$stoneToPlay(byte[][] board) {
        int black = 0;
        int white = 0;
        for (byte[] column : board) {
            for (byte cell : column) {
                if (cell == Point.BLACK) {
                    black++;
                } else if (cell == Point.WHITE) {
                    white++;
                }
            }
        }
        return black <= white ? Point.BLACK : Point.WHITE;
    }
}
