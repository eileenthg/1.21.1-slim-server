package com.github.eileen.llvoices.gametest;

import com.github.JumDa5he.callresponse.compat.emotion.EmotionData;
import com.github.JumDa5he.callresponse.compat.hunger.HungerData;
import com.github.eileen.llvoices.LoveLoatheVoices;
import com.github.eileen.llvoices.MaidVoiceTracker;
import com.github.eileen.llvoices.voice.VoiceKind;
import com.github.eileen.llvoices.voice.VoiceQueue;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Runs against the real Touhou Little Maid and Love & Loathe jars. No LLM is reachable here (TLM's default site has
 * no key), so lines come out as canned fallback bubbles, which is enough to see the whole path work.
 */
@GameTestHolder(LoveLoatheVoices.MOD_ID)
@PrefixGameTestTemplate(false)
public class VoiceGameTests {
    @GameTest(template = "game_test")
    public static void driftingTowardPanicPutsUpABubble(GameTestHelper helper) {
        ServerPlayer owner = mockPlayer(helper);
        EntityMaid maid = ownedMaid(helper, owner);
        EmotionData.set(maid, owner.getUUID(), 40, 10);

        MaidVoiceTracker.INSTANCE.scan(maid, owner);
        helper.assertTrue(bubbles(maid) == 0, "first scan only takes a baseline");

        // Fear 20 above trust: 10 points from L&L's panic trigger, so on the brink. The tendency changes too.
        EmotionData.set(maid, owner.getUUID(), 30, 50);
        MaidVoiceTracker.INSTANCE.scan(maid, owner);

        helper.assertTrue(bubbles(maid) == 1, "the maid speaks, bubbles: " + bubbles(maid));
        VoiceQueue queue = MaidVoiceTracker.queueOf(maid.getUUID());
        helper.assertTrue(queue != null && queue.hasPending(VoiceKind.MOOD_SHIFT),
                "the mood change waits for its turn behind the more urgent drift line");
        helper.assertFalse(queue.hasPending(VoiceKind.DRIFT_BRINK), "the brink line was the one said");
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void hungryWithNothingToEatAsksForFood(GameTestHelper helper) {
        ServerPlayer owner = mockPlayer(helper);
        EntityMaid maid = ownedMaid(helper, owner);
        EmotionData.set(maid, owner.getUUID(), 40, 10);
        HungerData.set(maid, 50.0F);
        MaidVoiceTracker.INSTANCE.scan(maid, owner);

        HungerData.set(maid, 20.0F);
        MaidVoiceTracker.INSTANCE.scan(maid, owner);

        helper.assertTrue(bubbles(maid) == 1, "the maid asks for food, bubbles: " + bubbles(maid));
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void quietWhenNothingChanges(GameTestHelper helper) {
        ServerPlayer owner = mockPlayer(helper);
        EntityMaid maid = ownedMaid(helper, owner);
        EmotionData.set(maid, owner.getUUID(), 40, 10);
        HungerData.set(maid, 50.0F);
        for (int i = 0; i < 5; i++) {
            MaidVoiceTracker.INSTANCE.scan(maid, owner);
        }
        helper.assertTrue(bubbles(maid) == 0, "no reason to talk, bubbles: " + bubbles(maid));
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void chatIsRoutedToPlayer2OnceItIsEnabled(GameTestHelper helper) {
        ServerPlayer owner = mockPlayer(helper);
        EntityMaid maid = ownedMaid(helper, owner);
        LLMSite player2 = AvailableSites.getLLMSite("player2");
        helper.assertTrue(player2 != null, "TLM registers a player2 site");

        boolean wasEnabled = player2.enabled();
        try {
            player2.setEnabled(false);
            helper.assertFalse("player2".equals(maid.getAiChatManager().getLLMSite().id()),
                    "a disabled Player2 is not forced on anyone");
            player2.setEnabled(true);
            helper.assertTrue("player2".equals(maid.getAiChatManager().getLLMSite().id()),
                    "with Player2 enabled every maid's chat goes there");
        } finally {
            player2.setEnabled(wasEnabled);
        }
        helper.succeed();
    }

    private static int bubbles(EntityMaid maid) {
        return maid.getChatBubbleManager().getChatBubbleDataCollection().size();
    }

    private static EntityMaid ownedMaid(GameTestHelper helper, ServerPlayer owner) {
        EntityMaid maid = helper.spawn(EntityMaid.TYPE, new Vec3(2.5, 1, 2.5));
        maid.setTame(true, false);
        maid.setOwnerUUID(owner.getUUID());
        maid.setNoAi(true);
        return maid;
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
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        player.setPos(helper.absoluteVec(new Vec3(4.5, 1, 2.5)));
        return player;
    }
}
