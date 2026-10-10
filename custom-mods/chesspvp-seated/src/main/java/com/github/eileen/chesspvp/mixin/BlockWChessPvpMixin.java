package com.github.eileen.chesspvp.mixin;

import com.github.eileen.chesspvp.PvpSeats;
import com.github.tartaricacid.touhoulittlemaid.api.game.chess.Position;
import com.github.tartaricacid.touhoulittlemaid.block.BlockWChess;
import com.github.tartaricacid.touhoulittlemaid.block.properties.GomokuPart;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityWChess;
import com.github.tartaricacid.touhoulittlemaid.util.WChessUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockWChess.class)
public abstract class BlockWChessPvpMixin {
    // 有玩家坐在棋盘边时，女仆不再入座，避免两种玩法混在一起
    @Inject(method = "startMaidSit", at = @At("HEAD"), cancellable = true)
    private void chesspvp$keepSeatsFree(EntityMaid maid, BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof TileEntityWChess chess && PvpSeats.anySeatTaken(serverLevel, chess)) {
            ci.cancel();
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void chesspvp$useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                    BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!(level instanceof ServerLevel serverLevel) || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        GomokuPart part = state.getValue(BlockWChess.PART);
        BlockPos centerPos = pos.subtract(new Vec3i(part.getPosX(), 0, part.getPosY()));
        if (!(level.getBlockEntity(centerPos) instanceof TileEntityWChess chess)) {
            return;
        }
        // 女仆在场，或者手里拿着残局道具等物品时，交给原版处理
        if (PvpSeats.hasMaid(serverLevel, chess) || !itemStack.isEmpty()) {
            return;
        }

        Direction facing = state.getValue(BlockWChess.FACING);
        PvpSeats.Layout layout = PvpSeats.Layout.chess(facing);
        int slot = PvpSeats.slotOf(serverLevel, chess, serverPlayer);

        // 没坐下：右键就是入座
        if (slot < 0) {
            int wanted = layout.nearestSlot(centerPos, hit.getLocation());
            if (!PvpSeats.isTaken(serverLevel, chess, wanted)) {
                slot = wanted;
            } else if (!PvpSeats.isTaken(serverLevel, chess, PvpSeats.other(wanted))) {
                slot = PvpSeats.other(wanted);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("message.chesspvp.full"), true);
                cir.setReturnValue(ItemInteractionResult.FAIL);
                return;
            }
            if (serverPlayer.isPassenger() || !PvpSeats.sit(serverLevel, centerPos, chess, layout, slot, serverPlayer, Type.GOMOKU.getTypeName())) {
                cir.setReturnValue(ItemInteractionResult.FAIL);
                return;
            }
            serverPlayer.displayClientMessage(Component.translatable("message.chesspvp.sit",
                    Component.translatable(slot == PvpSeats.FIRST ? "chesspvp.color.white" : "chesspvp.color.black")), true);
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
            return;
        }

        Vec3 clickPos = hit.getLocation()
                .subtract(pos.getX(), pos.getY(), pos.getZ())
                .add(part.getPosX() - 0.5, 0, part.getPosY() - 0.5)
                .yRot(facing.toYRot() * Mth.DEG_TO_RAD);

        // 重置棋盘，座位保持不变
        if (WChessUtil.isClickResetArea(clickPos)) {
            chess.reset();
            chess.refresh();
            level.playSound(null, centerPos, InitSounds.GOMOKU_RESET.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
            return;
        }

        if (!PvpSeats.isTaken(serverLevel, chess, PvpSeats.other(slot))) {
            serverPlayer.displayClientMessage(Component.translatable("message.chesspvp.waiting"), true);
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
            return;
        }
        // 白方先手，isPlayerTurn 为 true 时轮到白方
        if (chess.isPlayerTurn() != (slot == PvpSeats.FIRST)) {
            serverPlayer.displayClientMessage(Component.translatable("message.chesspvp.not_your_turn"), true);
            cir.setReturnValue(ItemInteractionResult.FAIL);
            return;
        }
        cir.setReturnValue(chesspvp$play(serverLevel, pos, serverPlayer, chess, clickPos, slot == PvpSeats.FIRST));
    }

    private static ItemInteractionResult chesspvp$play(ServerLevel level, BlockPos pos, ServerPlayer player, TileEntityWChess chess, Vec3 clickPos, boolean white) {
        // 没有点击到棋盘上
        int nowClick = WChessUtil.getClickPosition(clickPos);
        if (nowClick < 0 || !Position.IN_BOARD(nowClick)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        // 已经将死、50 回合限着、长将，都不能继续下棋
        if (chess.isCheckmate() || chess.isMoveNumberLimit() || chess.isRepeat()) {
            return ItemInteractionResult.FAIL;
        }

        Position chessData = chess.getChessData();
        byte[] squares = chessData.squares;
        int preClick = chess.getSelectChessPoint();
        if (preClick < 0 || squares.length <= preClick) {
            preClick = 0;
        }
        byte prePiece = squares[preClick];
        byte nowPiece = squares[nowClick];
        boolean ownNow = white ? WChessUtil.isWhite(nowPiece) : WChessUtil.isBlack(nowPiece);
        boolean ownPre = prePiece > 0 && (white ? WChessUtil.isWhite(prePiece) : WChessUtil.isBlack(prePiece));

        // 之前没选中自己的棋子，或者点的是另一颗自己的棋子：选中它
        if (!ownPre || ownNow) {
            if (ownNow) {
                chess.setSelectChessPoint(nowClick);
                chess.refresh();
                level.playSound(null, pos, InitSounds.GOMOKU.get(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);
            }
            return ItemInteractionResult.SUCCESS;
        }

        int move = Position.MOVE(preClick, nowClick);
        if (!chessData.legalMove(move)) {
            return ItemInteractionResult.FAIL;
        }
        if (chessData.makeMove(move)) {
            int pcSrc = chessData.squares[Position.SRC(move)];
            // 如果吃子、动兵了，那么重置计数器（该计数器用于判断自然限着和长将）
            if (chessData.captured() || Position.PIECE_TYPE(pcSrc) == Position.PIECE_PAWN) {
                chessData.setIrrev();
            }
            chess.addChessCounter();
            chess.setSelectChessPoint(nowClick);
            // 对方无棋可走：被将军是将死，否则是逼和
            if (chessData.isMate()) {
                if (chessData.inCheck()) {
                    chess.setCheckmate(true);
                    PvpSeats.notifySeated(level, chess, Component.translatable("message.chesspvp.win",
                            Component.translatable(white ? "chesspvp.color.white" : "chesspvp.color.black")));
                } else {
                    PvpSeats.notifySeated(level, chess, Component.translatable("message.chesspvp.stalemate"));
                }
            } else if (WChessUtil.reachMoveLimit(chessData)) {
                // 50 回合限着
                chess.setMoveNumberLimit(true);
                PvpSeats.notifySeated(level, chess, Component.translatable("message.chesspvp.draw"));
            } else if (WChessUtil.isRepeat(chessData)) {
                // 三次重复局面
                chess.setRepeat(true);
                PvpSeats.notifySeated(level, chess, Component.translatable("message.chesspvp.draw"));
            }
            chess.refresh();
            level.playSound(null, pos, InitSounds.GOMOKU.get(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);
            return ItemInteractionResult.SUCCESS;
        }

        // 这步棋会让自己被将军
        player.sendSystemMessage(Component.translatable("message.touhou_little_maid.cchess.check"));
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0f, 0.8F + level.random.nextFloat() * 0.4F);
        return ItemInteractionResult.FAIL;
    }
}
