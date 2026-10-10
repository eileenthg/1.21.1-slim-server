package com.github.eileen.chesspvp.gametest;

import com.github.eileen.chesspvp.ChessPvpMod;
import com.github.tartaricacid.touhoulittlemaid.api.game.gomoku.Point;
import com.github.tartaricacid.touhoulittlemaid.api.game.xqwlight.Position;
import com.github.tartaricacid.touhoulittlemaid.block.BlockJoy;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityCChess;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityGomoku;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityWChess;
import com.github.tartaricacid.touhoulittlemaid.util.CChessUtil;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Drives the board blocks with mock players the same way a right-click would.
 * Board facing is NORTH: the first seat is north of the board, the second south; for Gomoku first = west, second = east.
 */
@GameTestHolder(ChessPvpMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class PvpGameTests {
    private static final Direction FACING = Direction.NORTH;
    private static final BlockPos REL_CENTER = new BlockPos(4, 1, 4);

    private static final AtomicInteger PLAYER_COUNTER = new AtomicInteger();
    private static final Supplier<String> NEXT_NAME = () -> "tester" + PLAYER_COUNTER.incrementAndGet();

    // 玩家不走 placeNewPlayer，这样不会触发女仆模组加入世界时发给客户端的同步包；FakePlayer 又不允许骑乘，所以自己搭一个
    private static ServerPlayer mockPlayer(ServerLevel level, String name) {
        MinecraftServer server = level.getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name), false);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }

    private static BlockPos place(GameTestHelper helper, Block block) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(REL_CENTER);
        BlockState state = block.defaultBlockState().setValue(BlockJoy.FACING, FACING);
        level.setBlock(center, state, Block.UPDATE_ALL);
        block.setPlacedBy(level, center, state, null, ItemStack.EMPTY);
        return center;
    }

    private static BlockHitResult hitAt(BlockPos center, double dx, double dz) {
        Vec3 world = new Vec3(center.getX() + 0.5 + dx, center.getY() + 0.125, center.getZ() + 0.5 + dz);
        return new BlockHitResult(world, Direction.UP, BlockPos.containing(world), false);
    }

    // 把棋盘坐标系里的点击位置换算回世界坐标（BlockCChess 里 yRot 的逆运算）
    private static BlockHitResult hitOnBoard(BlockPos center, double boardX, double boardZ) {
        Vec3 rel = new Vec3(boardX, 0, boardZ).yRot(-FACING.toYRot() * Mth.DEG_TO_RAD);
        return hitAt(center, rel.x, rel.z);
    }

    private static BlockHitResult hitOnChessSquare(BlockPos center, int file, int rank) {
        return hitOnBoard(center, (file - Position.FILE_LEFT) * 0.304 - 1.365, (rank - Position.RANK_TOP) * 0.304 - 1.370);
    }

    // 国际象棋：8x8，列 0-7 对应 a-h，行 0 是第 8 横线
    private static BlockHitResult hitOnWChessSquare(BlockPos center, int col, int row) {
        return hitOnBoard(center, (col + 0.5) * 0.25 - 1, (row + 0.5) * 0.25 - 1);
    }

    private static void click(ServerLevel level, ServerPlayer player, BlockHitResult hit) {
        level.getBlockState(hit.getBlockPos()).useItemOn(ItemStack.EMPTY, level, player, InteractionHand.MAIN_HAND, hit);
    }

    private static Entity seatOf(ServerPlayer player) {
        return player.getVehicle();
    }

    private static boolean stoodUp(ServerPlayer player) {
        return player.getVehicle() == null;
    }

    private static int stones(TileEntityGomoku gomoku) {
        int count = 0;
        for (byte[] column : gomoku.getChessData()) {
            for (byte cell : column) {
                if (cell != Point.EMPTY) {
                    count++;
                }
            }
        }
        return count;
    }

    // 朝北时棋盒（重置区域）在东北角和西南角
    private static boolean inResetBox(double dx, double dz) {
        return (dx > 1.05 && dx < 1.39 && dz > -0.82 && dz < -0.49) || (dx > -1.39 && dx < -1.05 && dz > 0.49 && dz < 0.82);
    }

    /** Sweep the board until a click lands on an intersection; returns whether a stone appeared (skips the reset box, which would clear the board). */
    private static boolean placeStone(ServerLevel level, ServerPlayer player, BlockPos center, TileEntityGomoku gomoku) {
        int before = stones(gomoku);
        for (double dx = -1.45; dx <= 1.45; dx += 0.01) {
            for (double dz = -1.45; dz <= 1.45; dz += 0.01) {
                if (inResetBox(dx, dz)) {
                    continue;
                }
                click(level, player, hitAt(center, dx, dz));
                if (stones(gomoku) > before) {
                    return true;
                }
            }
        }
        return false;
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void cchessSeatsAndTurns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = place(helper, InitBlocks.CCHESS.get());
        TileEntityCChess chess = (TileEntityCChess) level.getBlockEntity(center);
        ServerPlayer red = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer black = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer late = mockPlayer(level, NEXT_NAME.get());

        // 右键靠北的一侧：红方入座
        click(level, red, hitAt(center, 0, -0.9));
        helper.assertTrue(seatOf(red) instanceof EntitySit, "red should be riding a seat");
        helper.assertTrue(seatOf(red).getZ() < center.getZ(), "red's seat should be on the north side");

        // 对手还没来：点棋子不会选中
        click(level, red, hitOnChessSquare(center, 3, 9));
        helper.assertTrue(chess.getSelectChessPoint() == 0, "no piece should be selected without an opponent");

        // 右键靠南的一侧：黑方入座，两把椅子不是同一个
        click(level, black, hitAt(center, 0, 0.9));
        helper.assertTrue(seatOf(black) instanceof EntitySit, "black should be riding a seat");
        helper.assertTrue(seatOf(black) != seatOf(red), "players need separate seats");
        helper.assertTrue(seatOf(black).getZ() > center.getZ(), "black's seat should be on the south side");

        // 轮到红方：黑方点不动，红方可以选中并走一步
        int redPawn = Position.COORD_XY(3, 9);
        click(level, black, hitOnChessSquare(center, 3, 6));
        helper.assertTrue(chess.getSelectChessPoint() == 0, "black must not select on red's turn");
        click(level, red, hitOnChessSquare(center, 3, 9));
        helper.assertTrue(chess.getSelectChessPoint() == redPawn, "red should select the pawn");
        click(level, red, hitOnChessSquare(center, 3, 8));
        helper.assertTrue(!chess.isPlayerTurn(), "turn should pass to black after red moves");
        String afterRed = chess.getChessData().toFen();

        // 红方不能连走两步
        click(level, red, hitOnChessSquare(center, 5, 9));
        click(level, red, hitOnChessSquare(center, 5, 8));
        helper.assertTrue(afterRed.equals(chess.getChessData().toFen()), "red must not move twice in a row");

        // 黑方走一步，轮回红方
        click(level, black, hitOnChessSquare(center, 3, 6));
        click(level, black, hitOnChessSquare(center, 3, 7));
        helper.assertTrue(chess.isPlayerTurn(), "turn should return to red after black moves");
        String afterBlack = chess.getChessData().toFen();

        // 黑方潜行起身（走原版的潜行下车流程）：座位空出来，棋盘保持原样
        black.setShiftKeyDown(true);
        black.rideTick();
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(stoodUp(black), "black should be standing");
            helper.assertTrue(afterBlack.equals(chess.getChessData().toFen()), "board must survive a player standing up");

            // 别人可以接着坐下，并继续同一盘棋
            click(level, late, hitAt(center, 0, 0.9));
            helper.assertTrue(seatOf(late) instanceof EntitySit, "a new player should be able to take the free seat");
            click(level, red, hitOnChessSquare(center, 5, 9));
            click(level, red, hitOnChessSquare(center, 5, 8));
            helper.assertTrue(!chess.isPlayerTurn(), "red moves again");
            click(level, late, hitOnChessSquare(center, 5, 6));
            click(level, late, hitOnChessSquare(center, 5, 7));
            helper.assertTrue(chess.isPlayerTurn(), "the new black player can move");

            // 前两个座位都有人，第三个人坐不下
            ServerPlayer extra = mockPlayer(level, NEXT_NAME.get());
            click(level, extra, hitAt(center, 0, 0.9));
            helper.assertTrue(stoodUp(extra), "a third player must not get a seat");

            // 棋盘被拆，座位一起消失
            level.setBlock(center, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(stoodUp(red) && stoodUp(late), "seats go away with the board");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void cchessCheckmateEndsTheGame(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = place(helper, InitBlocks.CCHESS.get());
        TileEntityCChess chess = (TileEntityCChess) level.getBlockEntity(center);
        ServerPlayer red = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer black = mockPlayer(level, NEXT_NAME.get());
        click(level, red, hitAt(center, 0, -0.9));
        click(level, black, hitAt(center, 0, 0.9));

        // 红车一步杀：双车配合，黑将无处可逃
        chess.setEndgame("4k4/1R7/9/9/9/R8/9/9/9/3K5");
        helper.assertTrue(!chess.isCheckmate(), "not mated yet");
        click(level, red, hitOnChessSquare(center, 3, 8));
        click(level, red, hitOnChessSquare(center, 3, 3));
        helper.assertTrue(chess.isCheckmate(), "black should be checkmated");

        // 将死之后双方都不能再动
        String mated = chess.getChessData().toFen();
        click(level, black, hitOnChessSquare(center, 7, 3));
        click(level, red, hitOnChessSquare(center, 4, 3));
        click(level, red, hitOnChessSquare(center, 4, 4));
        helper.assertTrue(mated.equals(chess.getChessData().toFen()), "no moves after checkmate");

        // 点重置区域，重新开局
        click(level, red, hitOnBoard(center, 1.3, 0.8));
        helper.assertTrue(!chess.isCheckmate(), "reset clears the checkmate");
        Position fresh = new Position();
        fresh.fromFen(CChessUtil.INIT);
        helper.assertTrue(fresh.toFen().equals(chess.getChessData().toFen()), "board is back to the start");
        helper.assertTrue(seatOf(red) instanceof EntitySit && seatOf(black) instanceof EntitySit, "players stay seated after a reset");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void wchessFoolsMate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = place(helper, InitBlocks.WCHESS.get());
        TileEntityWChess chess = (TileEntityWChess) level.getBlockEntity(center);
        ServerPlayer white = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer black = mockPlayer(level, NEXT_NAME.get());
        click(level, white, hitAt(center, 0, -0.9));
        click(level, black, hitAt(center, 0, 0.9));

        // 1. f3 e5 2. g4 Qh4#
        click(level, white, hitOnWChessSquare(center, 5, 6));
        click(level, white, hitOnWChessSquare(center, 5, 5));
        click(level, black, hitOnWChessSquare(center, 4, 1));
        click(level, black, hitOnWChessSquare(center, 4, 3));
        click(level, white, hitOnWChessSquare(center, 6, 6));
        click(level, white, hitOnWChessSquare(center, 6, 4));
        helper.assertTrue(!chess.isCheckmate(), "no mate before the last move");
        click(level, black, hitOnWChessSquare(center, 3, 0));
        click(level, black, hitOnWChessSquare(center, 7, 4));
        helper.assertTrue(chess.isCheckmate(), "white should be checkmated");

        String mated = chess.getChessData().toFen();
        click(level, white, hitOnWChessSquare(center, 4, 7));
        click(level, white, hitOnWChessSquare(center, 4, 6));
        helper.assertTrue(mated.equals(chess.getChessData().toFen()), "no moves after checkmate");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void wchessStalemateIsNotCheckmate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = place(helper, InitBlocks.WCHESS.get());
        TileEntityWChess chess = (TileEntityWChess) level.getBlockEntity(center);
        ServerPlayer white = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer black = mockPlayer(level, NEXT_NAME.get());
        click(level, white, hitAt(center, 0, -0.9));
        click(level, black, hitAt(center, 0, 0.9));

        // 白后 g1-g6，黑王 h8 无路可走却没被将军
        chess.setEndgame("7k/8/5K2/8/8/8/8/6Q1 w - - 0 1");
        click(level, white, hitOnWChessSquare(center, 6, 7));
        click(level, white, hitOnWChessSquare(center, 6, 2));
        helper.assertTrue(chess.getChessData().isMate(), "black has no legal move");
        helper.assertTrue(!chess.isCheckmate(), "a stalemate is not a checkmate");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void cchessMoveLimitIsADraw(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = place(helper, InitBlocks.CCHESS.get());
        TileEntityCChess chess = (TileEntityCChess) level.getBlockEntity(center);
        ServerPlayer red = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer black = mockPlayer(level, NEXT_NAME.get());
        click(level, red, hitAt(center, 0, -0.9));
        click(level, black, hitAt(center, 0, 0.9));

        // 只剩将士，已经走了 60 回合：下一步触发自然限着
        chess.setEndgame("3aka3/9/9/9/9/9/9/9/9/3AKA3");
        chess.getChessData().moveNum = 60;
        click(level, red, hitOnChessSquare(center, 6, 12));
        click(level, red, hitOnChessSquare(center, 7, 11));
        helper.assertTrue(chess.isMoveNumberLimit(), "the move limit should end the game");
        helper.assertTrue(!chess.isCheckmate(), "a draw is not a checkmate");

        String drawn = chess.getChessData().toFen();
        click(level, black, hitOnChessSquare(center, 7, 3));
        click(level, black, hitOnChessSquare(center, 7, 4));
        helper.assertTrue(drawn.equals(chess.getChessData().toFen()), "no moves after the draw");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void wchessMoveLimitIsADraw(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = place(helper, InitBlocks.WCHESS.get());
        TileEntityWChess chess = (TileEntityWChess) level.getBlockEntity(center);
        ServerPlayer white = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer black = mockPlayer(level, NEXT_NAME.get());
        click(level, white, hitAt(center, 0, -0.9));
        click(level, black, hitAt(center, 0, 0.9));

        chess.setEndgame("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        chess.getChessData().moveNum = 100;
        click(level, white, hitOnWChessSquare(center, 0, 7));
        click(level, white, hitOnWChessSquare(center, 0, 6));
        helper.assertTrue(chess.isMoveNumberLimit(), "the 50-move rule should end the game");
        helper.assertTrue(!chess.isCheckmate(), "a draw is not a checkmate");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void wchessRepetitionIsADraw(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = place(helper, InitBlocks.WCHESS.get());
        TileEntityWChess chess = (TileEntityWChess) level.getBlockEntity(center);
        ServerPlayer white = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer black = mockPlayer(level, NEXT_NAME.get());
        click(level, white, hitAt(center, 0, -0.9));
        click(level, black, hitAt(center, 0, 0.9));

        // 白车 a1-a2-a1…，黑王 e8-d8-e8…，没有吃子也没有动兵，局面会一再重复
        chess.setEndgame("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        int[][] rook = {{0, 7, 0, 6}, {0, 6, 0, 7}};
        int[][] king = {{4, 0, 3, 0}, {3, 0, 4, 0}};
        int ply = 0;
        while (ply < 24 && !chess.isRepeat()) {
            boolean whiteToMove = ply % 2 == 0;
            int[] move = (whiteToMove ? rook : king)[(ply / 2) % 2];
            ServerPlayer mover = whiteToMove ? white : black;
            click(level, mover, hitOnWChessSquare(center, move[0], move[1]));
            click(level, mover, hitOnWChessSquare(center, move[2], move[3]));
            ply++;
        }
        helper.assertTrue(chess.isRepeat(), "repeating the position should end the game");
        helper.assertTrue(ply > 4, "one repeat is not enough for a draw (stopped after " + ply + " plies)");
        helper.assertTrue(!chess.isCheckmate(), "a draw is not a checkmate");

        String drawn = chess.getChessData().toFen();
        click(level, white, hitOnWChessSquare(center, 0, 7));
        click(level, white, hitOnWChessSquare(center, 0, 6));
        helper.assertTrue(drawn.equals(chess.getChessData().toFen()), "no moves after the draw");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void wchessSeats(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = place(helper, InitBlocks.WCHESS.get());
        ServerPlayer white = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer black = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer extra = mockPlayer(level, NEXT_NAME.get());

        click(level, white, hitAt(center, 0, -0.9));
        click(level, black, hitAt(center, 0, 0.9));
        helper.assertTrue(seatOf(white) instanceof EntitySit && seatOf(white).getZ() < center.getZ(), "white sits north");
        helper.assertTrue(seatOf(black) instanceof EntitySit && seatOf(black).getZ() > center.getZ(), "black sits south");
        // 已经坐下的人再点同一侧，不会换座或者再生成座位
        Entity whiteSeat = seatOf(white);
        click(level, white, hitAt(center, 0, 0.9));
        helper.assertTrue(seatOf(white) == whiteSeat, "a seated player keeps their seat");
        click(level, extra, hitAt(center, 0, -0.9));
        helper.assertTrue(stoodUp(extra), "no third seat");

        level.setBlock(center, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(stoodUp(white) && stoodUp(black), "seats go away with the board");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void gomokuSeatsAndTurns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = place(helper, InitBlocks.GOMOKU.get());
        TileEntityGomoku gomoku = (TileEntityGomoku) level.getBlockEntity(center);
        ServerPlayer black = mockPlayer(level, NEXT_NAME.get());
        ServerPlayer white = mockPlayer(level, NEXT_NAME.get());

        // 黑方在西侧入座，白方在东侧入座
        click(level, black, hitAt(center, -0.9, 0));
        helper.assertTrue(seatOf(black) instanceof EntitySit && seatOf(black).getX() < center.getX(), "black sits west");
        helper.assertTrue(!placeStone(level, black, center, gomoku), "no stones before an opponent arrives");
        click(level, white, hitAt(center, 0.9, 0));
        helper.assertTrue(seatOf(white) instanceof EntitySit && seatOf(white).getX() > center.getX(), "white sits east");

        // 白方不能先手
        helper.assertTrue(!placeStone(level, white, center, gomoku), "white cannot open");
        helper.assertTrue(placeStone(level, black, center, gomoku), "black can place a stone");
        helper.assertTrue(stones(gomoku) == 1, "one stone on the board");
        helper.assertTrue(!placeStone(level, black, center, gomoku), "black cannot play twice in a row");
        helper.assertTrue(placeStone(level, white, center, gomoku), "white replies");
        helper.assertTrue(stones(gomoku) == 2, "two stones on the board");

        // 起身再被别人接替，棋子仍在
        black.setShiftKeyDown(true);
        black.rideTick();
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(stones(gomoku) == 2, "stones remain after standing up");
            ServerPlayer next = mockPlayer(level, NEXT_NAME.get());
            click(level, next, hitAt(center, -0.9, 0));
            helper.assertTrue(seatOf(next) instanceof EntitySit, "free seat can be retaken");
            helper.assertTrue(placeStone(level, next, center, gomoku), "the newcomer moves as black");
            helper.assertTrue(stones(gomoku) == 3, "three stones on the board");
            helper.succeed();
        });
    }
}
