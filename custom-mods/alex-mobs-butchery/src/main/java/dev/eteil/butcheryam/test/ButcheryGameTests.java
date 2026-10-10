package dev.eteil.butcheryam.test;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import dev.eteil.butcheryam.blockentity.CarcassBlockEntity;
import dev.eteil.butcheryam.butcher.ButcherUtil;
import dev.eteil.butcheryam.butcher.Butchering;
import dev.eteil.butcheryam.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * End-to-end checks of the butchering mechanics, run headlessly against the real Butchery and
 * Alex's Mobs jars.
 */
@GameTestHolder(ButcheryAlexsMobs.MODID)
@PrefixGameTestTemplate(false)
public class ButcheryGameTests {

    private static final String TEMPLATE = "platform";
    private static final BlockPos SPOT = new BlockPos(4, 1, 4);

    private static final String CLEAVER = "butchery:iron_cleaver";
    private static final String KNIFE = "butchery:iron_skinning_knife";

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static Player playerHolding(GameTestHelper helper, String itemId) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(itemId)));
        return player;
    }

    /** Places a drained emu carcass at the given butchering stage. */
    private static void placeDrainedEmu(GameTestHelper helper, int stage) {
        BlockState state = Butchering.withStage(
                ModBlocks.DRAINED_EMU_CARCASS.get().defaultBlockState(), stage);
        helper.setBlock(SPOT, state);
        if (helper.getBlockEntity(SPOT) instanceof CarcassBlockEntity be) {
            be.setFlag(Butchering.FLAG_DRAINED, true);
        }
    }

    /** Item entities lying around the test spot. */
    private static java.util.List<ItemEntity> itemsNear(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(SPOT);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(6.0D));
    }

    private static int stageAt(GameTestHelper helper) {
        return Butchering.stage(helper.getBlockState(SPOT));
    }

    // ---------------------------------------------------------------- cross-mod wiring

    @GameTest(template = TEMPLATE)
    public static void butcheryToolsAreTagged(GameTestHelper helper) {
        if (!ButcherUtil.isCleaver(new ItemStack(item(CLEAVER)))) {
            helper.fail("iron_cleaver is not in the c:cleaver tag");
        }
        if (!ButcherUtil.isSkinningKnife(new ItemStack(item(KNIFE)))) {
            helper.fail("iron_skinning_knife is not in the c:skinning_knives tag");
        }
        helper.succeed();
    }

    /** The bug fix: the entity is capuchin_monkey, which the tag must now match. */
    @GameTest(template = TEMPLATE)
    public static void capuchinTagMatchesTheRealEntity(GameTestHelper helper) {
        EntityType<?> capuchin = BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.parse("alexsmobs:capuchin_monkey"));
        if (capuchin == null) {
            helper.fail("alexsmobs:capuchin_monkey is not registered");
            return;
        }
        if (!capuchin.is(ButcherUtil.entityTag("alexsmobs:capuchin"))) {
            helper.fail("capuchin_monkey is not in the alexsmobs:capuchin tag - the original bug");
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------- death drop

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void killingAnEmuWithACleaverDropsItsCarcass(GameTestHelper helper) {
        EntityType<?> emuType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("alexsmobs:emu"));
        if (emuType == null) {
            helper.fail("alexsmobs:emu is not registered");
            return;
        }

        Entity emu = helper.spawn(emuType, SPOT);
        Player player = playerHolding(helper, CLEAVER);
        if (!(emu instanceof LivingEntity living)) {
            helper.fail("emu did not spawn as a LivingEntity");
            return;
        }
        living.hurt(helper.getLevel().damageSources().playerAttack(player), 1000.0F);

        helper.succeedWhen(() ->
                helper.assertItemEntityPresent(ModBlocks.EMU_CARCASS.get().asItem(), SPOT, 5.0D));
    }

    // ---------------------------------------------------------------- bleeding

    @GameTest(template = TEMPLATE)
    public static void rightClickingAFreshCarcassStartsItBleeding(GameTestHelper helper) {
        helper.setBlock(SPOT, ModBlocks.EMU_CARCASS.get().defaultBlockState());
        helper.useBlock(SPOT, helper.makeMockPlayer(GameType.SURVIVAL));

        if (!(helper.getBlockEntity(SPOT) instanceof CarcassBlockEntity be)
                || !be.getFlag(Butchering.FLAG_BLEEDING)) {
            helper.fail("carcass did not start bleeding");
            return;
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------- cut-up progression

    @GameTest(template = TEMPLATE)
    public static void cleaverSeversTheHead(GameTestHelper helper) {
        placeDrainedEmu(helper, 1);
        helper.useBlock(SPOT, playerHolding(helper, CLEAVER));

        int stage = stageAt(helper);
        if (stage != 2) {
            helper.fail("expected hanging stage 1 -> 2 after a cleaver, got " + stage);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void knifePlucksFeathers(GameTestHelper helper) {
        placeDrainedEmu(helper, 2);
        helper.useBlock(SPOT, playerHolding(helper, KNIFE));

        int stage = stageAt(helper);
        if (stage != 3) {
            helper.fail("expected stage 2 -> 3 after a skinning knife, got " + stage);
            return;
        }
        // emu_feather_drop is weighted 85% vanilla feather / 15% alexsmobs:emu_feather, so
        // asserting on the vanilla one alone fails about one run in seven.
        Item emuFeather = item("alexsmobs:emu_feather");
        helper.succeedWhen(() -> {
            boolean plucked = itemsNear(helper).stream().anyMatch(
                    e -> e.getItem().is(Items.FEATHER)
                            || (emuFeather != null && e.getItem().is(emuFeather)));
            if (!plucked) {
                throw new GameTestAssertException("plucking dropped no feather of either kind");
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void aCleaverDoesNotPluck(GameTestHelper helper) {
        placeDrainedEmu(helper, 2);
        helper.useBlock(SPOT, playerHolding(helper, CLEAVER));

        int stage = stageAt(helper);
        if (stage != 2) {
            helper.fail("a cleaver advanced the pluck stage; expected 2, got " + stage);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bareHandsDoNotButcher(GameTestHelper helper) {
        placeDrainedEmu(helper, 1);
        helper.useBlock(SPOT, helper.makeMockPlayer(GameType.SURVIVAL));

        int stage = stageAt(helper);
        if (stage != 1) {
            helper.fail("an empty hand butchered the carcass; expected 1, got " + stage);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void theFinalCutConsumesTheCarcass(GameTestHelper helper) {
        placeDrainedEmu(helper, 5);
        helper.useBlock(SPOT, playerHolding(helper, CLEAVER));

        helper.assertBlock(SPOT, b -> b == Blocks.AIR, "final cleaver cut should consume the carcass");
        helper.succeed();
    }

    /** Walks the whole hanging sequence 1 -> 2 -> 3 -> 4 -> 5 -> gone. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void fullHangingSequence(GameTestHelper helper) {
        placeDrainedEmu(helper, 1);

        helper.useBlock(SPOT, playerHolding(helper, CLEAVER));
        helper.useBlock(SPOT, playerHolding(helper, KNIFE));
        helper.useBlock(SPOT, playerHolding(helper, CLEAVER));
        helper.useBlock(SPOT, playerHolding(helper, CLEAVER));

        int beforeFinal = stageAt(helper);
        if (beforeFinal != 5) {
            helper.fail("sequence stalled before the final cut at stage " + beforeFinal);
            return;
        }
        helper.useBlock(SPOT, playerHolding(helper, CLEAVER));

        helper.assertBlock(SPOT, b -> b == Blocks.AIR, "carcass should be gone after the full sequence");
        helper.succeed();
    }
    /** A carcass hangs below a Butchery hook, and the hook is still there once it is cut up. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void hookSurvivesButchering(GameTestHelper helper) {
        BlockPos hookPos = SPOT.above();
        net.minecraft.world.level.block.Block hook = ButcherUtil.block(ButcherUtil.HOOK);
        helper.setBlock(hookPos, hook);
        helper.setBlock(SPOT, Blocks.AIR); // the platform is solid; hanging needs room below the hook
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.DRAINED_EMU_CARCASS.get()));
        // GameTestHelper.useBlock skips PlayerInteractEvent, which is where hanging happens
        dev.eteil.butcheryam.event.InteractHandler.tryHang(helper.getLevel(), helper.absolutePos(hookPos), player);
        if (helper.getBlockState(SPOT).getBlock() != ModBlocks.DRAINED_EMU_CARCASS.get() || stageAt(helper) != 1) {
            helper.fail("the emu did not hang below the hook: " + helper.getBlockState(SPOT));
            return;
        }
        if (helper.getBlockState(hookPos).getBlock() != hook) {
            helper.fail("hanging replaced the hook");
            return;
        }
        if (helper.getBlockEntity(SPOT) instanceof CarcassBlockEntity be) {
            be.setFlag(Butchering.FLAG_DRAINED, true);
        }
        for (String tool : new String[] {CLEAVER, KNIFE, CLEAVER, CLEAVER, CLEAVER}) {
            helper.useBlock(SPOT, playerHolding(helper, tool));
        }
        helper.assertBlock(SPOT, b -> b == Blocks.AIR, "carcass should be gone after the full sequence");
        helper.assertBlock(hookPos, b -> b == hook, "the hook should survive the butchering");
        helper.succeed();
    }
}
