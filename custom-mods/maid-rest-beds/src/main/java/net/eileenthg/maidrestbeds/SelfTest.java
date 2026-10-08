package net.eileenthg.maidrestbeds;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidBedTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitPoi;
import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** /maidrestbeds selftest — builds beds high above spawn and runs TLM's real bed task on real maids. Op only. */
final class SelfTest {
    private static int pass, fail;
    private static Consumer<String> out;

    private SelfTest() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maidrestbeds").requires(s -> s.hasPermission(2))
                .then(Commands.literal("selftest").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    pass = 0;
                    fail = 0;
                    out = msg -> {
                        System.out.println("[maidrestbeds-test] " + msg);
                        src.sendSystemMessage(Component.literal(msg));
                    };
                    try {
                        run(src.getServer().overworld());
                    } catch (Throwable t) {
                        t.printStackTrace();
                        check("no exception", false, t);
                    }
                    out.accept("RESULT pass=" + pass + " fail=" + fail);
                    return fail == 0 ? 1 : 0;
                })));
    }

    private static void check(String name, boolean ok, Object detail) {
        if (ok) pass++;
        else fail++;
        out.accept((ok ? "PASS " : "FAIL ") + name + " :: " + detail);
    }

    /** Places a two-block bed whose foot is at {@code foot} and head one block north. Returns the head. */
    private static BlockPos placeBed(ServerLevel level, Block block, BlockPos foot) {
        BlockPos head = foot.north();
        BlockState base = block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        level.setBlock(foot, base.setValue(BedBlock.PART, BedPart.FOOT), 3);
        level.setBlock(head, base.setValue(BedBlock.PART, BedPart.HEAD), 3);
        return head;
    }

    private static void removeBed(ServerLevel level, BlockPos head) {
        level.setBlock(head, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(head.south(), Blocks.AIR.defaultBlockState(), 3);
    }

    private static boolean occupied(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.hasProperty(BedBlock.OCCUPIED) && s.getValue(BedBlock.OCCUPIED);
    }

    private static EntityMaid spawnMaid(ServerLevel level, BlockPos at) {
        EntityMaid maid = Objects.requireNonNull(InitEntities.MAID.get().create(level));
        maid.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        level.addFreshEntity(maid);
        return maid;
    }

    /** Runs TLM's bed task until it starts. The check-rate gate may skip a few calls first. */
    private static void runBedTask(ServerLevel level, EntityMaid maid) {
        MaidBedTask task = new MaidBedTask(0.6F, 2);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        maid.getBrain().eraseMemory(InitEntities.TARGET_POS.get());
        for (int i = 0; i < 40 && !maid.isSleeping(); i++) {
            if (task.tryStart(level, maid, level.getGameTime() + i)) break;
        }
    }

    /** Maid Spell cancels direct setHealth drops on maids, so damage them the normal way. */
    private static void hit(ServerLevel level, EntityMaid maid) {
        maid.invulnerableTime = 0;
        maid.hurt(level.damageSources().generic(), 14);
        out.accept("debug hit: health now " + maid.getHealth() + "/" + maid.getMaxHealth());
    }

    private static void run(ServerLevel level) {
        Block redBed = Blocks.RED_BED;
        Block hammock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("comforts:hammock_white"));
        Block maidBed = InitBlocks.MAID_BED.get();
        check("hammock block exists", hammock != Blocks.AIR, hammock);
        check("hammock POI has 16 colours x 16 head states", MaidRestBeds.HAMMOCK_POI.get().matchingStates().size() == 256,
                MaidRestBeds.HAMMOCK_POI.get().matchingStates().size());

        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos base = new BlockPos(spawn.getX(), 250, spawn.getZ());
        level.setChunkForced(base.getX() >> 4, base.getZ() >> 4, true);
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-10, -1, -10), base.offset(10, 3, 10))) {
            level.setBlock(p, p.getY() == base.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        level.setDayTime(1000);

        // --- A: ordinary bed, no maid bed around ---
        BlockPos bedFoot = base.east(4);
        BlockPos bedHead = placeBed(level, redBed, bedFoot);
        EntityMaid maid = spawnMaid(level, bedFoot);
        check("A findFallback picks the red bed", bedHead.equals(RestBeds.findFallback(level, maid)), RestBeds.findFallback(level, maid));
        float max = maid.getMaxHealth();
        hit(level, maid);
        float h0 = maid.getHealth();
        int favor0 = maid.getFavorability();
        runBedTask(level, maid);
        check("A maid sleeps in the red bed", maid.isSleeping() && maid.getSleepingPos().map(bedHead::equals).orElse(false), maid.getSleepingPos());
        check("A bed head marked occupied", occupied(level, bedHead), level.getBlockState(bedHead));
        check("A bed foot marked occupied", occupied(level, bedFoot), level.getBlockState(bedFoot));
        float expected = Math.min(max, h0 + max * 0.5F);
        check("A heals half of max health", Math.abs(maid.getHealth() - expected) < 0.01, maid.getHealth() + " (expected " + expected + ", max " + max + ")");
        int sleep = com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type.SLEEP.getPoint();
        int half = Math.max(1, Math.round(sleep * 0.5F));
        check("A favorability is half the maid bed's (" + half + " of " + sleep + ")", maid.getFavorability() == favor0 + half, favor0 + " -> " + maid.getFavorability());
        check("A lies at bed height", Math.abs(maid.getY() - (bedHead.getY() + 0.5625)) < 0.01, maid.getY());

        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.moveTo(bedFoot.getX() + 0.5, bedFoot.getY(), bedFoot.getZ() + 1.5, 0, 0);
        PlayerInteractEvent.RightClickBlock click = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, bedFoot,
                new BlockHitResult(Vec3.atCenterOf(bedFoot), Direction.UP, bedFoot, false));
        NeoForge.EVENT_BUS.post(click);
        check("A clicking the bed wakes the maid", !maid.isSleeping(), maid.isSleeping());
        check("A the click is handled", click.isCanceled(), click.isCanceled() + " " + click.getCancellationResult());
        check("A bed free again (daytime, player could not sleep)", !occupied(level, bedHead), level.getBlockState(bedHead));
        removeBed(level, bedHead);

        // --- B: hammock, POI backfill, shared favorability cooldown ---
        BlockPos hamFoot = base.west(4);
        BlockPos hamHead = placeBed(level, hammock, hamFoot);
        check("B placed hammock gets the hammock POI",
                level.getPoiManager().getType(hamHead).map(h -> h.value() == MaidRestBeds.HAMMOCK_POI.get()).orElse(false),
                level.getPoiManager().getType(hamHead));
        level.getPoiManager().remove(hamHead);
        List<BlockPos> scanned = MaidRestBeds.findHammocks(level.getChunkAt(hamHead));
        int added = MaidRestBeds.addMissingPoi(level, scanned);
        check("B chunk scan finds and restores the hammock POI", scanned.contains(hamHead) && added == 1
                && level.getPoiManager().getType(hamHead).isPresent(), scanned + " added=" + added);
        check("B chunk scan adds nothing twice", MaidRestBeds.addMissingPoi(level, scanned) == 0, "second pass");

        maid.moveTo(hamFoot.getX() + 0.5, hamFoot.getY(), hamFoot.getZ() + 0.5, 0, 0);
        hit(level, maid);
        float h1 = maid.getHealth();
        int favor1 = maid.getFavorability();
        runBedTask(level, maid);
        check("B maid sleeps in the hammock", maid.isSleeping() && maid.getSleepingPos().map(hamHead::equals).orElse(false), maid.getSleepingPos());
        check("B hammock head marked occupied", occupied(level, hamHead), level.getBlockState(hamHead));
        check("B hammock foot marked occupied", occupied(level, hamFoot), level.getBlockState(hamFoot));
        float expected1 = Math.min(max, h1 + max * 0.5F);
        check("B hammock heals half of max health", Math.abs(maid.getHealth() - expected1) < 0.01, h1 + " -> " + maid.getHealth());
        check("B no second favorability the same day", maid.getFavorability() == favor1, favor1 + " -> " + maid.getFavorability());
        maid.stopSleeping();
        check("B waking frees the hammock", !occupied(level, hamHead) && !occupied(level, hamFoot), level.getBlockState(hamHead));

        // --- C: a free maid bed beats the hammock; a full one falls back to it ---
        BlockPos mbFoot = base.south(4);
        BlockPos mbHead = placeBed(level, maidBed, mbFoot);
        check("C maid bed has its POI", level.getPoiManager().getType(mbHead).map(h -> h.value() == InitPoi.MAID_BED.get()).orElse(false),
                level.getPoiManager().getType(mbHead));
        EntityMaid second = spawnMaid(level, mbFoot);
        hit(level, second);
        int favor2 = second.getFavorability();
        runBedTask(level, second);
        check("C maid bed chosen over the hammock", second.isSleeping() && second.getSleepingPos().map(mbHead::equals).orElse(false), second.getSleepingPos());
        check("C maid bed still heals fully", Math.abs(second.getHealth() - second.getMaxHealth()) < 0.01, second.getHealth());
        check("C maid bed still gives the full " + sleep, second.getFavorability() == favor2 + sleep, favor2 + " -> " + second.getFavorability());
        check("C maid bed marked occupied", occupied(level, mbHead), level.getBlockState(mbHead));

        EntityMaid third = spawnMaid(level, mbFoot.south());
        runBedTask(level, third);
        BlockPos walkTo = third.getBrain().getMemory(MemoryModuleType.WALK_TARGET).map(WalkTarget::getTarget).map(t -> t.currentBlockPosition()).orElse(null);
        check("C full maid bed -> third maid heads for the hammock", hamHead.equals(walkTo) && !third.isSleeping(), walkTo);

        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, mbHead,
                new BlockHitResult(Vec3.atCenterOf(mbHead), Direction.UP, mbHead, false)));
        check("C clicking a maid bed does not wake the maid", second.isSleeping(), second.isSleeping());

        // --- cleanup ---
        for (EntityMaid m : List.of(maid, second, third)) m.discard();
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-10, -1, -10), base.offset(10, 3, 10))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        level.getEntitiesOfClass(Entity.class, new net.minecraft.world.phys.AABB(base).inflate(12), e -> !(e instanceof FakePlayer))
                .forEach(Entity::discard);
        level.setChunkForced(base.getX() >> 4, base.getZ() >> 4, false);
    }
}
