package com.github.eileen.workingmaidcomputers.gametest;

import com.craisinlord.antos.content.computer.ComputerWorkspaceData;
import com.craisinlord.antos.content.network.AnternetAccountHandler;
import com.craisinlord.antos.content.network.ComputerAccessHandler;
import com.craisinlord.antos.content.network.AnternetAccountPayload;
import com.github.eileen.workingmaidcomputers.WorkingMaidComputers;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Runs against the real Touhou Little Maid jar: TLM's own code seats the player, then these tests check what the
 * addon does with the next right-click. Only the server half is covered; opening the screen is client-side.
 */
@GameTestHolder(WorkingMaidComputers.MOD_ID)
@PrefixGameTestTemplate(false)
public class ComputerGameTests {
    private static final ResourceLocation COMPUTER = ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "computer");
    private static final ResourceLocation TEST_DISK = ResourceLocation.fromNamespaceAndPath("antos", "test_disk");

    @GameTest(template = "game_test")
    public static void firstClickSitsSecondClickIsConsumed(GameTestHelper helper) {
        BlockPos computer = placeComputer(helper);
        ServerPlayer player = mockPlayer(helper);
        BlockHitResult hit = hitOf(helper, computer);

        InteractionResult first = click(helper, player, ItemStack.EMPTY, hit);
        helper.assertTrue(player.getVehicle() != null, "TLM seats the player on the first click, result was " + first);

        InteractionResult second = click(helper, player, ItemStack.EMPTY, hit);
        helper.assertTrue(second == InteractionResult.SUCCESS, "seated right-click is taken over by the addon, got " + second);
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void clickingAnotherBlockWhileSeatedIsIgnored(GameTestHelper helper) {
        BlockPos computer = placeComputer(helper);
        BlockPos stone = new BlockPos(1, 1, 1);
        helper.setBlock(stone, Blocks.STONE);
        ServerPlayer player = mockPlayer(helper);

        click(helper, player, ItemStack.EMPTY, hitOf(helper, computer));
        helper.assertTrue(player.getVehicle() != null, "seated");
        InteractionResult other = click(helper, player, ItemStack.EMPTY, hitOf(helper, stone));
        helper.assertFalse(other == InteractionResult.SUCCESS, "other blocks are left alone, got " + other);
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void floppyIsInstalledIntoTheSignedInAccount(GameTestHelper helper) {
        BlockPos computer = placeComputer(helper);
        ServerPlayer player = mockPlayer(helper);
        BlockHitResult hit = hitOf(helper, computer);

        AnternetAccountHandler.handle(player, new AnternetAccountPayload(AnternetAccountPayload.CREATE, "gametester", "correct horse battery"));
        var account = AnternetAccountHandler.session(player);
        helper.assertTrue(account != null, "test account was created and signed in");

        ItemStack floppy = new ItemStack(WorkingMaidComputers.FLOPPY_DISK.get());
        floppy.set(WorkingMaidComputers.FLOPPY_DISK_COMPONENT.get(), TEST_DISK);

        // Not seated yet: the floppy must stay untouched.
        click(helper, player, floppy, hit);
        helper.assertTrue(!floppy.isEmpty(), "floppy is not consumed before sitting down");

        click(helper, player, ItemStack.EMPTY, hit);
        helper.assertTrue(player.getVehicle() != null, "seated");
        InteractionResult result = click(helper, player, floppy, hit);
        helper.assertTrue(result == InteractionResult.SUCCESS, "floppy click is consumed, got " + result);
        helper.assertTrue(floppy.isEmpty(), "floppy was used up");
        var disks = ComputerWorkspaceData.access(player.server).profileDisks(account.accountId());
        helper.assertTrue(disks.contains(TEST_DISK), "disk is on the account, found " + disks);

        AnternetAccountHandler.disconnect(player);
        helper.succeed();
    }

    /**
     * Vanilla's mock server player is put on the player list with a connection that has negotiated no mod payloads, so
     * every mod that sends a packet when a player joins throws. This one is never added to the list or the level.
     */
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        player.setPos(helper.absoluteVec(new Vec3(4.5, 1, 2.5)));
        ComputerAccessHandler.setAnternetResultSender((target, payload) -> { });
        AnternetAccountHandler.setResultSender((target, payload) -> { });
        return player;
    }

    private static BlockPos placeComputer(GameTestHelper helper) {
        Block block = BuiltInRegistries.BLOCK.get(COMPUTER);
        helper.assertFalse(block == Blocks.AIR, "Touhou Little Maid must be loaded: computer block is missing");
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, block);
        return pos;
    }

    private static BlockHitResult hitOf(GameTestHelper helper, BlockPos relative) {
        BlockPos absolute = helper.absolutePos(relative);
        return new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
    }

    private static InteractionResult click(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockHitResult hit) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }
}
