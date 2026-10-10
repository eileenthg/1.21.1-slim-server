package dev.eteil.butcheryam2.test;

import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import dev.eteil.butcheryam2.block.DrawnCarcassBlock;
import dev.eteil.butcheryam2.blockentity.CarcassBlockEntity;
import dev.eteil.butcheryam2.blockentity.DrawnCarcassBlockEntity;
import dev.eteil.butcheryam2.butcher.ButcherUtil;
import dev.eteil.butcheryam2.butcher.Butchering;
import dev.eteil.butcheryam2.butcher.MobDef;
import dev.eteil.butcheryam2.butcher.MobDefs;
import dev.eteil.butcheryam2.event.DeathHandler;
import dev.eteil.butcheryam2.event.InteractHandler;
import dev.eteil.butcheryam2.gen.GeneratedMobs;
import dev.eteil.butcheryam2.gen.StepSpecs;
import dev.eteil.butcheryam2.init.ModBlocks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** End-to-end checks against the real Butchery, Alex's Mobs and Delight jars. */
@GameTestHolder(ButcheryAlexsMobs2.MODID)
@PrefixGameTestTemplate(false)
public class Part2GameTests {

    private static final String TEMPLATE = "platform";
    private static final BlockPos SPOT = new BlockPos(4, 1, 4);
    private static final String CLEAVER = "butchery:iron_cleaver";
    private static final String KNIFE = "butchery:iron_skinning_knife";

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static boolean hasItem(String id) {
        return BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id));
    }

    private static Player holding(GameTestHelper helper, String itemId) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(itemId)));
        return player;
    }

    private static List<ItemEntity> itemsNear(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(SPOT)).inflate(6.0D));
    }

    private static boolean dropped(GameTestHelper helper, String itemId) {
        Item wanted = item(itemId);
        return itemsNear(helper).stream().anyMatch(e -> e.getItem().is(wanted));
    }

    private static void expectDrops(GameTestHelper h, String... ids) {
        h.succeedWhen(() -> {
            for (String id : ids) {
                if (!dropped(h, id)) {
                    throw new GameTestAssertException("no " + id + " dropped");
                }
            }
        });
    }

    private static void placeAt(GameTestHelper helper, Block block, int stage) {
        helper.setBlock(SPOT, Butchering.withStage(block.defaultBlockState(), stage));
    }

    private static int stage(GameTestHelper helper) {
        return Butchering.stage(helper.getBlockState(SPOT));
    }

    private static Block carcass(String mob) {
        return ModBlocks.carcass(mob).get();
    }

    /** The entity type a mob's tag points at. */
    private static EntityType<?> entityOf(MobDef def) {
        TagKey<EntityType<?>> tag = ButcherUtil.entityTag(ButcheryAlexsMobs2.MODID + ":" + def.tag());
        return BuiltInRegistries.ENTITY_TYPE.stream().filter(t -> t.is(tag)).findFirst()
                .orElseThrow(() -> new IllegalStateException("no entity in tag " + def.tag()));
    }

    // ---------------------------------------------------------------- death drops, one test per mob

    /** Killing each creature with a cleaver drops its carcass (catfish by size). */
    @GameTestGenerator
    public static Collection<TestFunction> deathDrops() {
        List<TestFunction> out = new ArrayList<>();
        for (MobDef def : MobDefs.ALL) {
            out.add(new TestFunction("deathdrops", "dropscarcass_" + def.name(),
                    ButcheryAlexsMobs2.MODID + ":" + TEMPLATE, 300, 0L, true, h -> killDrops(h, def)));
        }
        return out;
    }

    /** Puts a freshly spawned entity into the variant a rule asks for. */
    static void makeVariant(Entity entity, GeneratedMobs.Match m) {
        if (m == null) {
            return;
        }
        int value = switch (m.op()) {
            case ">" -> m.value() + 1;
            case "<" -> m.value() - 1;
            default -> m.value();
        };
        if (m.source().equals("method")) {
            String setter = "set" + m.key().substring(3); // getBloodLevel -> setBloodLevel
            try {
                entity.getClass().getMethod(setter, int.class).invoke(entity, value);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("no " + setter + " on " + entity, e);
            }
        } else {
            CompoundTag tag = entity.saveWithoutId(new CompoundTag());
            tag.putInt(m.key(), value);
            entity.load(tag);
        }
    }

    private static void killDrops(GameTestHelper h, MobDef def) {
        Entity entity = h.spawn(entityOf(def), SPOT);
        makeVariant(entity, def.match());
        if (!(entity instanceof LivingEntity living)) {
            h.fail(def.name() + " did not spawn as a LivingEntity");
            return;
        }
        living.setHealth(1.0F); // some bosses cap the damage one hit can do
        living.hurt(h.getLevel().damageSources().playerAttack(holding(h, CLEAVER)), 100000.0F);
        Item carcass = def.carcass().get().asItem();
        h.succeedWhen(() -> {
            h.assertItemEntityPresent(carcass, SPOT, 24.0D);
            // the carcass replaces AMC Delight's bonus cut
            if (itemsNear(h).stream().anyMatch(e -> BuiltInRegistries.ITEM.getKey(e.getItem().getItem())
                    .getNamespace().equals("amcdelight"))) {
                throw new GameTestAssertException("an AMC Delight cut dropped next to the carcass");
            }
        });
    }

    /** The centipede's length comes along with its carcass. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void centipedeCarcassKeepsItsLength(GameTestHelper h) {
        Entity e = h.spawn(entityOf(MobDefs.get("centipede")), SPOT);
        CompoundTag tag = e.saveWithoutId(new CompoundTag());
        tag.putInt("SegCount", 7);
        e.load(tag);
        ((LivingEntity) e).hurt(h.getLevel().damageSources().playerAttack(holding(h, CLEAVER)), 100000.0F);
        h.succeedWhen(() -> {
            ItemEntity found = itemsNear(h).stream().filter(i -> i.getItem().is(carcass("centipede").asItem()))
                    .findFirst().orElseThrow(() -> new GameTestAssertException("no centipede carcass"));
            var data = found.getItem().get(DataComponents.BLOCK_ENTITY_DATA);
            if (data == null || !data.copyTag().getCompound(CarcassBlockEntity.PERSISTENT_KEY)
                    .contains(DrawnCarcassBlockEntity.COUNT)) {
                throw new GameTestAssertException("the carcass item lost the segment count");
            }
        });
    }

    /** No carcass without a cleaver: the mob's own loot drops instead. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void noCarcassWithoutACleaver(GameTestHelper h) {
        Entity bison = h.spawn(entityOf(MobDefs.get("bison")), SPOT);
        ((LivingEntity) bison).hurt(h.getLevel().damageSources().playerAttack(h.makeMockPlayer(GameType.SURVIVAL)), 100000.0F);
        h.runAfterDelay(40, () -> {
            if (dropped(h, ButcheryAlexsMobs2.MODID + ":bison_carcass")) {
                h.fail("a bare-handed kill dropped a carcass");
            }
            h.succeed();
        });
    }

    // ---------------------------------------------------------------- data

    /** Every drop table loads (an unknown item id would empty it) and rolls something. */
    @GameTest(template = TEMPLATE)
    public static void everyDropTableRollsItems(GameTestHelper h) {
        List<String> tables = new ArrayList<>();
        for (MobDef def : MobDefs.ALL) {
            List<String> suffixes = switch (def.kind()) {
                case FULL -> List.of("head_drop", "skin_drop", "cut_1_drop", "cut_2_drop", "cut_3_drop");
                case STAGED -> List.of("cut_1_drop", "peel_drop", "cut_2_drop");
                case SINGLE -> List.of("cut_drop");
                case SEGMENTED -> dev.eteil.butcheryam2.gen.SegmentedSpecs.SPECS.get(def.name()).neckDrop()
                        ? List.of("head_drop", "neck_drop", "tail_drop", "segment_drop")
                        : List.of("head_drop", "tail_drop", "segment_drop");
                case STEPS, LARGE -> {
                    List<String> out = new ArrayList<>();
                    for (StepSpecs.Step st : StepSpecs.STEPS.get(def.name())) {
                        if (!st.loot().isEmpty()) {
                            out.add(st.loot());
                        }
                        if (st.fragile() != null && !st.fragile().failLoot().isEmpty()) {
                            out.add(st.fragile().failLoot());
                        }
                    }
                    if (def.deathExtra()) {
                        out.add("death_extra");
                    }
                    yield out;
                }
            };
            suffixes.forEach(s -> tables.add(def.loot(s)));
        }
        List<String> empty = new ArrayList<>();
        for (String id : tables) {
            LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(
                    ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(id)));
            boolean any = false;
            for (int i = 0; i < 12 && !any; i++) {
                any = !table.getRandomItems(new LootParams.Builder(h.getLevel()).create(LootContextParamSets.EMPTY)).isEmpty();
            }
            // the murmur's ectoplasm comes from Eternal Tales, which the test server does not load
            boolean optional = id.endsWith("murmur_step_3_drop") && !hasItem("eternal_tales:ectoplasm");
            if ((table == LootTable.EMPTY || !any) && !optional) {
                empty.add(id);
            }
        }
        if (!empty.isEmpty()) {
            h.fail("empty or missing drop tables: " + empty);
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void headsAreButcheryHeads(GameTestHelper h) {
        TagKey<Block> heads = TagKey.create(Registries.BLOCK, ResourceLocation.parse("butchery:heads"));
        for (MobDef def : MobDefs.ALL) {
            if (def.head() != null && !def.head().get().defaultBlockState().is(heads)) {
                h.fail(def.name() + " head is not in butchery:heads");
            }
        }
        h.succeed();
    }

    /** Every state of every block has an outline, so no generated model is missing its shape. */
    @GameTest(template = TEMPLATE)
    public static void everyStateHasAShape(GameTestHelper h) {
        for (var holder : ModBlocks.ALL) {
            for (BlockState state : holder.get().getStateDefinition().getPossibleStates()) {
                if (state.getShape(h.getLevel(), BlockPos.ZERO).isEmpty()) {
                    h.fail("empty shape for " + state);
                }
            }
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- butchering

    @GameTest(template = TEMPLATE)
    public static void freshBisonBleeds(GameTestHelper h) {
        placeAt(h, carcass("bison"), 1);
        h.useBlock(SPOT, h.makeMockPlayer(GameType.SURVIVAL));
        if (!(h.getBlockEntity(SPOT) instanceof CarcassBlockEntity be) || !be.getFlag(Butchering.FLAG_BLEEDING)) {
            h.fail("bison carcass did not start bleeding");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void birdsAndFungiDoNotBleed(GameTestHelper h) {
        for (String mob : List.of("bunfungus", "toucan")) {
            placeAt(h, carcass(mob), 0);
            h.useBlock(SPOT, h.makeMockPlayer(GameType.SURVIVAL));
            if (h.getBlockEntity(SPOT) instanceof CarcassBlockEntity be && be.getFlag(Butchering.FLAG_BLEEDING)) {
                h.fail("a " + mob + " started bleeding");
            }
        }
        h.succeed();
    }

    /** Ground sequence 0 -> 6 -> 7 -> 8 -> 9 -> gone, with Alex's Delight drops. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void drainedBisonCutsUpOnTheGround(GameTestHelper h) {
        placeAt(h, ModBlocks.drained("bison").get(), 0);
        int[] expect = {6, 7, 8, 9};
        String[] tools = {CLEAVER, KNIFE, CLEAVER, CLEAVER};
        for (int i = 0; i < expect.length; i++) {
            h.useBlock(SPOT, holding(h, tools[i]));
            if (stage(h) != expect[i]) {
                h.fail("step " + (i + 1) + ": expected stage " + expect[i] + ", got " + stage(h));
                return;
            }
        }
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "the last cut should consume the carcass");
        expectDrops(h, ButcheryAlexsMobs2.MODID + ":bison_head", "alexsmobs:bison_fur",
                "alexsdelight:raw_bison", "alexsdelight:bison_mince", "butchery:hoof");
    }

    /** An AMC Delight mob: the tiger's cuts are predator cuts. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void drainedTigerHangingSequence(GameTestHelper h) {
        placeAt(h, ModBlocks.drained("tiger").get(), 1);
        for (String tool : List.of(CLEAVER, KNIFE, CLEAVER, CLEAVER, CLEAVER)) {
            h.useBlock(SPOT, holding(h, tool));
        }
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "the tiger should be gone after five cuts");
        expectDrops(h, ButcheryAlexsMobs2.MODID + ":tiger_head", "amcdelight:predator_cut", "minecraft:leather");
    }

    @GameTest(template = TEMPLATE)
    public static void wrongToolDoesNotSkin(GameTestHelper h) {
        placeAt(h, ModBlocks.drained("moose").get(), 2);
        h.useBlock(SPOT, holding(h, CLEAVER));
        if (stage(h) != 2) {
            h.fail("a cleaver did the knife's step; expected 2, got " + stage(h));
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void freshCarcassCannotBeCutBeforeBleeding(GameTestHelper h) {
        placeAt(h, carcass("kangaroo"), 1);
        h.useBlock(SPOT, holding(h, CLEAVER));
        if (stage(h) != 1 || h.getBlockState(SPOT).getBlock() != carcass("kangaroo")) {
            h.fail("an undrained kangaroo was butchered");
        }
        h.succeed();
    }

    /** Part 1's crow scheme: head, pluck, body. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void toucanIsBeheadedPluckedAndCut(GameTestHelper h) {
        placeAt(h, carcass("toucan"), 0);
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.useBlock(SPOT, holding(h, KNIFE));
        if (stage(h) != 7) {
            h.fail("expected ground stage 7 after head and feathers, got " + stage(h));
            return;
        }
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "the body cut should consume the toucan");
        expectDrops(h, ButcheryAlexsMobs2.MODID + ":toucan_head", "amcdelight:fowl_cut");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void bunfungusHangingSequence(GameTestHelper h) {
        placeAt(h, carcass("bunfungus"), 1);
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.useBlock(SPOT, holding(h, KNIFE));
        if (stage(h) != 3) {
            h.fail("expected hanging stage 3 after legs and cap, got " + stage(h));
            return;
        }
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "the body cut should consume the bunfungus");
        expectDrops(h, "alexsdelight:raw_bunfungus_drumstick", "alexsdelight:raw_bunfungus");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void catfishFilletsWithAKnife(GameTestHelper h) {
        placeAt(h, carcass("catfish_medium"), 0);
        h.useBlock(SPOT, holding(h, KNIFE));
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "one cut should consume a fish");
        expectDrops(h, "alexsdelight:raw_catfish_slice");
    }

    /** Head, tail, then one cut per segment: 1 + 1 + 5 cuts for a five-segment centipede. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void centipedeComesApartPieceByPiece(GameTestHelper h) {
        ItemStack stack = DeathHandler.withBody(new ItemStack(carcass("centipede")), 5, false);
        h.setBlock(SPOT, carcass("centipede").defaultBlockState());
        if (h.getBlockEntity(SPOT) instanceof DrawnCarcassBlockEntity be) {
            stack.get(DataComponents.BLOCK_ENTITY_DATA).loadInto(be, h.getLevel().registryAccess());
            if (be.count() != 5) {
                h.fail("the item's segment count did not reach the block: " + be.count());
                return;
            }
        }
        for (int i = 0; i < 6; i++) {
            h.useBlock(SPOT, holding(h, CLEAVER));
        }
        if (!(h.getBlockEntity(SPOT) instanceof DrawnCarcassBlockEntity be)
                || DrawnCarcassBlock.piecesLeft("centipede", be).size() != 1) {
            h.fail("expected one segment left after six cuts");
            return;
        }
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "the seventh cut should finish a five-segment centipede");
        expectDrops(h, ButcheryAlexsMobs2.MODID + ":centipede_head", "alexsmobs:centipede_leg");
    }

    /** The anaconda's fixed body: head + 7 pieces, so 8 cuts. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void anacondaTakesEightCuts(GameTestHelper h) {
        h.setBlock(SPOT, carcass("anaconda").defaultBlockState());
        for (int i = 0; i < 7; i++) {
            h.useBlock(SPOT, holding(h, CLEAVER));
        }
        h.assertBlock(SPOT, b -> b == carcass("anaconda"), "the anaconda should outlast seven cuts");
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "the eighth cut should finish the anaconda");
        expectDrops(h, ButcheryAlexsMobs2.MODID + ":anaconda_head", "amcdelight:reptile_cut");
    }

    /** A renderer-drawn giant follows its step list, knife included. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void orcaFollowsItsSteps(GameTestHelper h) {
        h.setBlock(SPOT, carcass("orca").defaultBlockState());
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.useBlock(SPOT, holding(h, CLEAVER)); // wrong tool for the skinning step: nothing happens
        if (!(h.getBlockEntity(SPOT) instanceof DrawnCarcassBlockEntity be) || be.cuts() != 1) {
            h.fail("a cleaver did the orca's knife step");
            return;
        }
        for (String tool : List.of(KNIFE, CLEAVER, CLEAVER, CLEAVER)) {
            h.useBlock(SPOT, holding(h, tool));
        }
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "five steps should finish the orca");
        expectDrops(h, ButcheryAlexsMobs2.MODID + ":orca_head", "amcdelight:blubber");
    }

    /** One wing fragment per wing: two knife steps, then the body. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void cockroachLosesAWingPerCut(GameTestHelper h) {
        placeAt(h, carcass("cockroach"), 0);
        h.useBlock(SPOT, holding(h, CLEAVER)); // wrong tool: wings come off with a knife
        if (stage(h) != 0) {
            h.fail("a cleaver took a cockroach wing");
            return;
        }
        h.useBlock(SPOT, holding(h, KNIFE));
        h.useBlock(SPOT, holding(h, KNIFE));
        if (stage(h) != 4) {
            h.fail("expected stage 4 (third step, on the ground) after both wings, got " + stage(h));
            return;
        }
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "the body cut should consume the cockroach");
        h.succeedWhen(() -> {
            int wings = itemsNear(h).stream().filter(e -> e.getItem().is(item("alexsmobs:cockroach_wing_fragment")))
                    .mapToInt(e -> e.getItem().getCount()).sum();
            if (wings != 2) {
                throw new GameTestAssertException("expected 2 wing fragments, got " + wings);
            }
        });
    }

    /** Undressing a murmur: the kimono comes off whole, or tears into red wool. Either way the step is done. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void murmurKimonoComesOffOrTears(GameTestHelper h) {
        placeAt(h, carcass("murmur"), 0);
        h.useBlock(SPOT, holding(h, KNIFE));
        if (stage(h) != 2) {
            h.fail("undressing did not advance the murmur: " + stage(h));
            return;
        }
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "three cuts should finish the murmur");
        h.succeedWhen(() -> {
            if (!dropped(h, "alexsmobs:unsettling_kimono") && !dropped(h, "minecraft:red_wool")) {
                throw new GameTestAssertException("neither a kimono nor torn wool");
            }
            if (!dropped(h, "alexsmobs:elastic_tendon")
                    || (hasItem("eternal_tales:ectoplasm") && !dropped(h, "eternal_tales:ectoplasm"))) {
                throw new GameTestAssertException("missing tendons or ectoplasm");
            }
        });
    }

    /** The dancing cockroach keeps its maraca on death, alongside its carcass. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void maracaCockroachKeepsItsMaraca(GameTestHelper h) {
        Entity e = h.spawn(entityOf(MobDefs.get("cockroach")), SPOT);
        CompoundTag tag = e.saveWithoutId(new CompoundTag());
        tag.putBoolean("Maracas", true);
        e.load(tag);
        ((LivingEntity) e).hurt(h.getLevel().damageSources().playerAttack(holding(h, CLEAVER)), 100000.0F);
        expectDrops(h, ButcheryAlexsMobs2.MODID + ":cockroach_carcass", "alexsmobs:maraca");
    }

    /** Equipment is not loot: a laviathan's helmet still drops next to its carcass. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void laviathanKeepsItsGear(GameTestHelper h) {
        Entity e = h.spawn(entityOf(MobDefs.get("laviathan")), SPOT);
        CompoundTag tag = e.saveWithoutId(new CompoundTag());
        tag.putBoolean("HeadGear", true);
        e.load(tag);
        ((LivingEntity) e).hurt(h.getLevel().damageSources().playerAttack(holding(h, CLEAVER)), 100000.0F);
        expectDrops(h, ButcheryAlexsMobs2.MODID + ":laviathan_carcass", "alexsmobs:straddle_helmet");
    }

    /** Only a fed mosquito has a blood sac to take. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void fedMosquitoGivesABloodSac(GameTestHelper h) {
        placeAt(h, carcass("crimson_mosquito_fed"), 0);
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.useBlock(SPOT, holding(h, KNIFE));
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "three cuts should finish a fed mosquito");
        expectDrops(h, "alexsmobs:mosquito_proboscis", "alexsmobs:blood_sac");
    }

    /** The warped mosco proboscis works anywhere the crimson mosquito's does. */
    @GameTest(template = TEMPLATE)
    public static void moscoProboscisIsInterchangeable(GameTestHelper h) {
        ItemStack ours = new ItemStack(item(ButcheryAlexsMobs2.MODID + ":warped_mosco_proboscis"));
        for (String id : List.of("alexsmobs:blood_sprayer", "alexsmobs:hemolymph_blaster", "alexsmobs:mosquito_larva")) {
            var holder = h.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id));
            if (holder.isEmpty()) {
                h.fail("no recipe " + id);
                return;
            }
            boolean accepts = holder.get().value().getIngredients().stream().anyMatch(ing -> ing.test(ours));
            if (!accepts) {
                h.fail(id + " does not accept the warped mosco proboscis");
                return;
            }
        }
        h.succeed();
    }

    /**
     * A carcass right-clicked onto a Butchery hook hangs in the block below (stage 1), and the
     * hook survives the butchering; giants cannot hang.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void carcassHangsOnAHook(GameTestHelper h) {
        Block hook = ButcherUtil.block(ButcherUtil.HOOK);
        BlockPos hookPos = SPOT.above();
        h.setBlock(hookPos, hook);
        h.setBlock(SPOT, Blocks.AIR); // the platform is solid; hanging needs room below the hook
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(carcass("bunfungus")));
        // GameTestHelper.useBlock skips PlayerInteractEvent, which is where hanging happens
        InteractHandler.tryHang(h.getLevel(), h.absolutePos(hookPos), player);
        if (h.getBlockState(SPOT).getBlock() != carcass("bunfungus") || stage(h) != 1) {
            h.fail("the bunfungus did not hang below the hook: " + h.getBlockState(SPOT));
            return;
        }
        if (h.getBlockState(hookPos).getBlock() != hook) {
            h.fail("hanging replaced the hook");
            return;
        }
        for (String tool : List.of(CLEAVER, KNIFE, CLEAVER)) {
            h.useBlock(SPOT, holding(h, tool));
        }
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "the bunfungus should be cut up");
        h.assertBlock(hookPos, b -> b == hook, "the hook should survive the butchering");

        // nothing hangs where the block below the hook is taken
        h.setBlock(SPOT, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(carcass("gazelle")));
        InteractHandler.tryHang(h.getLevel(), h.absolutePos(hookPos), player);
        h.assertBlock(SPOT, b -> b == Blocks.STONE, "a carcass replaced the block under the hook");
        h.setBlock(SPOT, Blocks.AIR);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(carcass("orca")));
        InteractHandler.tryHang(h.getLevel(), h.absolutePos(hookPos), player);
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "an orca was hung on a hook");
        h.succeed();
    }
    /** The bone serpent's neck, the last piece off, gives the bone blocks; the rest give bones. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void boneSerpentNeckGivesBoneBlocks(GameTestHelper h) {
        h.setBlock(SPOT, carcass("bone_serpent").defaultBlockState());
        int cuts = 0;
        while (h.getBlockEntity(SPOT) instanceof DrawnCarcassBlockEntity be
                && DrawnCarcassBlock.piecesLeft("bone_serpent", be).size() != 1 && cuts < 40) {
            h.useBlock(SPOT, holding(h, CLEAVER));
            cuts++;
        }
        if (dropped(h, "minecraft:bone_block")) {
            h.fail("bone blocks dropped before the neck, after " + cuts + " cuts");
            return;
        }
        if (!dropped(h, "minecraft:bone")) {
            h.fail("the body pieces gave no bones");
            return;
        }
        h.useBlock(SPOT, holding(h, CLEAVER));
        h.assertBlock(SPOT, b -> b == Blocks.AIR, "the neck should be the last piece");
        expectDrops(h, "minecraft:bone_block", "alexsmobs:bone_serpent_tooth");
    }

    /** Runs a step-list carcass from whole to gone with the tool each step asks for. */
    private static void dissect(GameTestHelper h, String mob) {
        placeAt(h, carcass(mob), 0);
        List<StepSpecs.Step> steps = StepSpecs.STEPS.get(mob);
        for (StepSpecs.Step step : steps) {
            h.useBlock(SPOT, holding(h, step.tool().equals("KNIFE") ? KNIFE : CLEAVER));
        }
        h.assertBlock(SPOT, b -> b == Blocks.AIR, mob + " should be gone after " + steps.size() + " steps");
    }

    /** A rain frog comes apart like Butchery's frogs: legs, heart, then organs. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void rainFrogIsDissected(GameTestHelper h) {
        dissect(h, "rain_frog");
        expectDrops(h, "butchery:raw_orange_frog_leg", "butchery:frog_heart");
    }

    /** A warped toad dissects like a frog and keeps its shroomlight and nether wart. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void warpedToadIsDissected(GameTestHelper h) {
        dissect(h, "warped_toad");
        expectDrops(h, "butchery:raw_gray_frog_leg", "butchery:frog_heart", "minecraft:shroomlight",
                "minecraft:nether_wart");
    }

    /** The void worm keeps its length only in synced data; the carcass reads it through the getter. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void voidWormCarcassKeepsItsLength(GameTestHelper h) {
        Entity worm = h.spawn(entityOf(MobDefs.get("void_worm")), SPOT);
        try {
            worm.getClass().getMethod("setSegmentCount", int.class).invoke(worm, 30);
        } catch (ReflectiveOperationException e) {
            h.fail("no setSegmentCount on the void worm: " + e);
            return;
        }
        ((LivingEntity) worm).hurt(h.getLevel().damageSources().playerAttack(holding(h, CLEAVER)), 100000.0F);
        h.succeedWhen(() -> {
            ItemEntity found = h.getLevel().getEntitiesOfClass(ItemEntity.class,
                            new AABB(h.absolutePos(SPOT)).inflate(24.0D)).stream()
                    .filter(i -> i.getItem().is(carcass("void_worm").asItem()))
                    .findFirst().orElseThrow(() -> new GameTestAssertException("no void worm carcass"));
            var data = found.getItem().get(DataComponents.BLOCK_ENTITY_DATA);
            int count = data == null ? -1 : data.copyTag().getCompound(CarcassBlockEntity.PERSISTENT_KEY)
                    .getInt(DrawnCarcassBlockEntity.COUNT);
            if (count != 27) { // 30 segments, the last 3 of them tail pieces
                throw new GameTestAssertException("expected 27 body pieces on the carcass, got " + count);
            }
        });
    }
}
