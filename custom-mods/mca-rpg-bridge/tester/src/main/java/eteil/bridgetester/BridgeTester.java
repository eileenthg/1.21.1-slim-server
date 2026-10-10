package eteil.bridgetester;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import dev.otectus.mcaquests.compat.ReputationBridge;
import dev.otectus.mcareputation.api.McaReputationApi;
import dev.otectus.mcareputation.api.ReputationIncidentView;
import dev.otectus.mcareputation.community.CommunityKey;
import eteil.mcarpgbridge.MebahelInteraction;
import net.conczin.mca.server.world.data.VillageManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.mebahelrpgrquest.quest.PlayerQuestProgress;
import net.mebahelrpgrquest.quest.QuestAvailability;
import net.mebahelrpgrquest.quest.QuestDefinition;
import net.mebahelrpgrquest.quest.QuestGiver;
import net.mebahelrpgrquest.quest.QuestManager;
import net.mebahelrpgrquest.quest.QuestPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** /bridgetest: drives every MCA RPG Bridge path on a real server with a fake player and prints PASS/FAIL. */
@Mod("bridgetester")
public final class BridgeTester {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UUID TESTER = UUID.fromString("0b1d9e7e-5e57-4a11-9c1e-b1d9e7e5e571");
    private final List<String> lines = new ArrayList<>();
    private int failures;

    public BridgeTester(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::commands);
    }

    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bridgetest").requires(s -> s.hasPermission(2)).executes(c -> run(c.getSource())));
    }

    private int run(CommandSourceStack source) {
        lines.clear();
        failures = 0;
        try {
            runChecks(source.getServer());
        } catch (Throwable t) {
            fail("harness threw " + t);
            LOGGER.error("[bridgetest] harness error", t);
        }
        lines.add(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAIL");
        for (String line : lines) {
            LOGGER.info("[bridgetest] {}", line);
            source.sendSystemMessage(Component.literal(line));
        }
        return failures == 0 ? 1 : 0;
    }

    private void runChecks(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld();

        // 0. MCA: Quests uses MCA: Reputation as its standing store.
        check("Quests bound to Reputation (backend " + ReputationBridge.backend().backendName() + ")", ReputationBridge.isCanonical());

        // 1. A one-room house, so MCA registers a village.
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(40, 0, 40));
        BlockPos inside = buildHouse(level, ground);
        VillageManager villages = VillageManager.get(level);
        Object scan = villages.processBuilding(inside);
        boolean hasVillage = villages.findNearestVillage(inside, 64).isPresent();
        check("MCA village registered around the test house (scan: " + scan + ")", hasVillage);

        // 2. Givers: an MCA villager and a vanilla villager, both holding a Mebahel quest.
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(TESTER, "BridgeTester"));
        player.moveTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 1.5, 0, 0);
        QuestPlayer questPlayer = (QuestPlayer) player;
        List<QuestDefinition> visible = QuestManager.INSTANCE.getAllQuests().stream()
                .filter(q -> QuestAvailability.canSeeQuest(player, q) && !q.id().getPath().startsWith("quest_board_contract/"))
                .toList();
        check("Mebahel has quests visible to a new player (" + visible.size() + " of " + QuestManager.INSTANCE.size() + ")", visible.size() >= 2);
        QuestDefinition questA = visible.get(0);
        QuestDefinition questB = visible.get(1);

        Entity mca = EntityType.byString("mca:male_villager").orElseThrow().spawn(level, inside, MobSpawnType.COMMAND);
        Villager vanilla = EntityType.VILLAGER.spawn(level, inside.east(), MobSpawnType.COMMAND);
        check("spawned MCA villager " + (mca == null ? null : mca.getClass().getSimpleName()) + " and vanilla villager", mca != null && vanilla != null);
        give((QuestGiver) mca, questA);
        give((QuestGiver) vanilla, questB);

        // 3. A plain right-click runs the whole Fabric UseEntityCallback chain, as a real click would.
        InteractionResult mcaClick = UseEntityCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, mca, null);
        check("right-click on MCA villager with a Mebahel quest is left to MCA (got " + mcaClick + ")", mcaClick == InteractionResult.PASS);
        InteractionResult vanillaClick = UseEntityCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, vanilla, null);
        check("right-click on vanilla villager still opens Mebahel (got " + vanillaClick + ")", vanillaClick != InteractionResult.PASS);

        // 4. The MCA menu button hands the click to Mebahel.
        InteractionResult button = MebahelInteraction.openFromMcaMenu(player, mca.getUUID());
        check("MCA menu Request button opens the Mebahel quest (got " + button + ")", button.consumesAction());
        ((QuestGiver) mca).mebahelrpgrquest$setQuestId(null);
        ((QuestGiver) mca).mebahelrpgrquest$setRumor(null, "");
        InteractionResult empty = MebahelInteraction.openFromMcaMenu(player, mca.getUUID());
        check("button on a villager with nothing to offer does nothing (got " + empty + ")", empty == InteractionResult.PASS);
        give((QuestGiver) mca, questA);

        // 5. Deeds reach MCA: Reputation.
        Optional<CommunityKey> community = McaReputationApi.resolveCommunity(mca)
                .or(() -> McaReputationApi.resolveCommunity(level, inside, 96));
        check("Reputation resolves the test village " + community.map(Object::toString).orElse("-"), community.isPresent());
        if (community.isEmpty()) {
            return;
        }
        CommunityKey key = community.get();
        int s0 = score(server, key);

        questPlayer.mebahelrpgrquest$startQuest(questA.id(), mca.getUUID(), mca.blockPosition());
        PlayerQuestProgress progress = questPlayer.mebahelrpgrquest$getQuestFromGiver(mca.getUUID());
        progress.setSyncData(progress.getProgress(), true, true, false);
        Method reward = method("net.mebahelrpgrquest.network.QuestDialogueNetworking", "rewardQuestGiver",
                ServerPlayer.class, LivingEntity.class, QuestDefinition.class, PlayerQuestProgress.class);
        reward.invoke(null, player, mca, questA, progress);
        int s1 = score(server, key);
        check("claimed Mebahel quest: standing " + s0 + " -> " + s1 + " (expect +4)", s1 - s0 == 4);
        reward.invoke(null, player, mca, questA, progress);
        int s2 = score(server, key);
        check("same claim again is deduplicated: " + s1 + " -> " + s2, s2 == s1);

        Method contract = method("net.mebahelrpgrquest.network.QuestBoardNetworking", "creditContractReputation",
                ServerPlayer.class, ResourceLocation.class);
        contract.invoke(null, player, ResourceLocation.fromNamespaceAndPath("mebahelrpgrquest",
                "quest_board_contract/0123456789abcdef0123456789abcdef/bridgetest"));
        int s3 = score(server, key);
        check("board contract: standing " + s2 + " -> " + s3 + " (expect +2)", s3 - s2 == 2);

        questPlayer.mebahelrpgrquest$startQuest(questB.id(), vanilla.getUUID(), vanilla.blockPosition());
        Method abandon = method("net.mebahelrpgrquest.network.QuestNetworking", "handleAbandonQuest",
                ServerPlayer.class, UUID.class, ResourceLocation.class);
        abandon.invoke(null, player, vanilla.getUUID(), questB.id());
        int s4 = score(server, key);
        PlayerQuestProgress abandoned = questPlayer.mebahelrpgrquest$getQuestFromGiver(vanilla.getUUID());
        check("abandoned quest: standing " + s3 + " -> " + s4 + " (expect -2)", s4 - s3 == -2);
        check("Mebahel still marked the abandoned quest failed", abandoned != null && abandoned.isFailed());

        for (ReputationIncidentView incident : McaReputationApi.recentIncidents(server, TESTER, key, 5)) {
            lines.add("  deed: " + incident.type() + " \"" + incident.display().getString() + "\"");
        }

        mca.discard();
        vanilla.discard();
    }

    private static void give(QuestGiver giver, QuestDefinition quest) {
        giver.mebahelrpgrquest$setQuestRollDone(true);
        giver.mebahelrpgrquest$setQuestId(quest.id());
        giver.mebahelrpgrquest$setQuestClaimed(false);
    }

    private static int score(MinecraftServer server, CommunityKey key) {
        return McaReputationApi.getScoreOrZero(server, TESTER, key);
    }

    private static Method method(String owner, String name, Class<?>... params) throws Exception {
        Method m = Class.forName(owner).getDeclaredMethod(name, params);
        m.setAccessible(true);
        return m;
    }

    /** 7x7 stone-brick room, 3 high, with a door and a bed; returns a floor-level spot inside. */
    private static BlockPos buildHouse(ServerLevel level, BlockPos ground) {
        BlockPos origin = ground.above(0);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(x, 4, z), Blocks.STONE_BRICKS.defaultBlockState());
                for (int y = 0; y < 4; y++) {
                    boolean wall = x == 0 || z == 0 || x == 6 || z == 6;
                    level.setBlockAndUpdate(origin.offset(x, y, z), wall ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        level.setBlockAndUpdate(origin.offset(3, 0, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(origin.offset(3, 1, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(origin.offset(1, 0, 5), bed.setValue(BedBlock.PART, BedPart.FOOT));
        level.setBlockAndUpdate(origin.offset(2, 0, 5), bed.setValue(BedBlock.PART, BedPart.HEAD));
        level.setBlockAndUpdate(origin.offset(5, 0, 5), Blocks.TORCH.defaultBlockState());
        return origin.offset(3, 0, 3);
    }

    private void check(String what, boolean ok) {
        if (!ok) {
            failures++;
        }
        lines.add((ok ? "PASS " : "FAIL ") + what);
    }

    private void fail(String what) {
        check(what, false);
    }
}
