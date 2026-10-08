package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.wallev.maidsoulkitchen.init.MkItems;
import com.github.wallev.maidsoulkitchen.inventory.container.item.BagType;
import com.github.wallev.maidsoulkitchen.item.ItemCulinaryHub;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.eileenthg.maidchef.station.Inv;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.slf4j.Logger;

/**
 * {@code /maidchef selftest}: builds small kitchens high above spawn in force-loaded chunks, puts real maids on the
 * Chef task and waits for them to cook. Each scene checks the chests/backpack afterwards. Needs FD and Maidsoul Kitchen.
 */
public final class SelfTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int TIMEOUT = 20 * 60 * 5;
    private static final int Y = 235;

    private static final class Scene {
        final String name;
        final BlockPos center;
        EntityMaid maid;
        BlockPos in, out;
        Predicate<Scene> pass;
        String detail = "";
        @Nullable java.util.function.Consumer<Scene> onTick;
        boolean triggered;
        boolean sawGaveUp;
        boolean sawThanks;
        boolean sawChain;
        Set<Item> firstAsk = Set.of();
        long blockedAt;
        String requestLog = "";
        String lastState = "";
        boolean done;
        boolean ok;

        Scene(String name, BlockPos center) {
            this.name = name;
            this.center = center;
        }
    }

    private static final class Run {
        ServerLevel level;
        CommandSourceStack source;
        final List<Scene> scenes = new ArrayList<>();
        final Set<ChunkPos> forced = new LinkedHashSet<>();
        /** Checks that don't need a kitchen: name, passed, detail. */
        final List<String[]> checks = new ArrayList<>();
        BlockPos origin = BlockPos.ZERO;
        UUID owner;
        long ticks;
    }

    @Nullable
    private static Run run;

    private SelfTest() {
    }

    // ------------------------------------------------------------------ setup

    public static void start(CommandSourceStack source) {
        if (run != null) clean(source.getServer());
        ServerLevel level = source.getServer().overworld();
        Run r = new Run();
        r.level = level;
        r.source = source;
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos origin = new BlockPos(spawn.getX(), Y, spawn.getZ());
        UUID owner = source.getPlayer() != null ? source.getPlayer().getUUID() : UUID.nameUUIDFromBytes("maidchef-selftest".getBytes());

        for (int i = 0; i < 55; i++) {
            BlockPos c = origin.offset(i * 30, 0, 0);
            for (int x = -12; x <= 12; x += 16) {
                for (int z = -12; z <= 28; z += 16) r.forced.add(new ChunkPos(c.offset(x, 0, z)));
            }
            r.forced.add(new ChunkPos(c.offset(12, 0, 28)));
        }
        // at y=235 rain falls as snow, and snow on a stove makes it drop its food: keep the sky clear while testing
        level.setWeatherParameters(TIMEOUT + 1200, 0, false, false);
        for (ChunkPos cp : r.forced) {
            level.setChunkForced(cp.x, cp.z, true);
            MaidChef.TICKETS.forceChunk(level, origin, cp.x, cp.z, true, true);
        }
        r.origin = origin;
        run = r;
        MaidChef.selfTestActive = true;
        Planner.ignoreKnownItems = true;

        r.scenes.add(smoker(level, origin, owner));
        r.scenes.add(pot(level, origin.offset(30, 0, 0), owner));
        r.scenes.add(cuttingBoard(level, origin.offset(60, 0, 0), owner));
        r.scenes.add(crafting(level, origin.offset(90, 0, 0), owner));
        r.scenes.add(rotation(level, origin.offset(120, 0, 0), owner));
        r.scenes.add(radius(level, origin.offset(150, 0, 0), owner));
        r.scenes.add(theft(level, origin.offset(180, 0, 0), owner, false));
        r.scenes.add(theft(level, origin.offset(210, 0, 0), owner, true));
        r.scenes.add(filters(level, origin.offset(240, 0, 0), owner));
        r.scenes.add(fallback(level, origin.offset(270, 0, 0), owner));
        r.scenes.add(chain(level, origin.offset(300, 0, 0), owner));
        r.scenes.add(feast(level, origin.offset(330, 0, 0), owner, true));
        r.scenes.add(feast(level, origin.offset(360, 0, 0), owner, false));
        r.scenes.add(pantryCraft(level, origin.offset(390, 0, 0), owner));
        r.scenes.add(potFerry(level, origin.offset(420, 0, 0), owner));
        ChefRequests.get(level.getServer()).clear(owner);
        r.scenes.add(request(level, origin.offset(450, 0, 0), owner));
        r.scenes.add(prepare(level, origin.offset(480, 0, 0), owner, false));
        r.scenes.add(prepare(level, origin.offset(510, 0, 0), owner, true));
        r.owner = owner;
        r.scenes.add(chat(level, origin.offset(540, 0, 0), owner));
        r.scenes.add(oven(level, origin.offset(570, 0, 0), owner));
        String[][] ed = {
                {"ED drying rack", "extradelight:drying_rack", "", "extradelight:drying/chili", "3"},
                {"ED dough shaping", "extradelight:dough_shaping", "", "extradelight:corn_flakes", "2"},
                {"ED mortar + pestle", "extradelight:mortar_stone", "", "extradelight:mortar/chili_powder_dried_chili", "3"},
                {"ED juicer (bottled)", "extradelight:juicer", "", "extradelight:juicer/apple", "2"},
                {"ED mixing bowl", "extradelight:mixing_bowl", "", "extradelight:mixing/buttered_pasta", "2"},
                {"ED mixing bowl + milk", "extradelight:mixing_bowl", "", "extradelight:mixing/apple_milkshake_ice_cream", "1"},
                {"ED melting pot (bottled)", "extradelight:melting_pot", "heat", "extradelight:melting/dark_chocolate_bar", "2"},
                {"ED chiller (ice)", "extradelight:chiller", "", "extradelight:chilling/dark_chocolate_bar", "2"},
                {"B&C keg (beer, poured)", "brewinandchewin:keg", "", "brewinandchewin:fermenting/beer_from_water", "1", "fast"},
                {"FR kettle (cider, poured)", "farmersrespite:kettle", "heat", "farmersrespite:brewing/apple_cider", "1"},
                {"YHC drying rack (sun)", "youkaishomecoming:drying_rack", "", "youkaishomecoming:dried_kelp_from_kelp_drying", "3", "sky"},
                {"YHC moka pot", "youkaishomecoming:moka_pot", "heat", "farmersdelight:cooking/espresso", "2"},
                {"YHC kettle (tea)", "youkaishomecoming:kettle", "heat", "youkaishomecoming:kettle/cornflower_tea", "1"},
                {"YHC basin (grape juice)", "youkaishomecoming:wood_basin", "", "youkaishomecoming:simple_basin/black_grape_juice", "1"},
                {"YHC fermentation tank", "youkaishomecoming:fermentation_tank", "", "youkaishomecoming:simple_fermentation/burgundy", "1"},
                {"YHC cooking pot (served)", "@pot", "heat", "youkaishomecoming:unordered_cooking/pot_of_cod_stew", "1"},
                {"YHC steamer", "youkaishomecoming:steamer_pot[pot_racks=2,water=true,capped=true]", "heat",
                        "youkaishomecoming:baked_potato_from_potato_steaming", "2"},
                {"YHC cuisine board (sushi)", "youkaishomecoming:cuisine_board", "", "youkaishomecoming:cuisine_ordered/egg_nigiri", "1"},
                {"Butchery meat grinder", "butchery:meat_grinder", "", "butchery:beefmincerecipe", "2"},
                {"CfB oven (fuel)", "cookingforblockheads:white_oven", "", "minecraft:baked_potato", "3", "fuel"},
                {"ED evaporator (left, picked up)", "extradelight:evaporator", "", "extradelight:evaporator/evaporate_water", "1", "fast"},
                {"ED vat, 2 stages (left, tended, picked up)", "extradelight:vat", "", "extradelight:vat/kimchi_item", "1", "fast"},
        };
        for (int i = 0; i < ed.length; i++) {
            r.scenes.add(stationScene(level, origin.offset(600 + 30 * i, 0, 0), owner, ed[i][0], ed[i][1], !ed[i][2].isEmpty(), ed[i][3],
                    Integer.parseInt(ed[i][4]), ed[i].length > 5 ? ed[i][5] : ""));
        }
        staticChecks(level, r);
        FoodValue.update(level);
        say(String.format("feast values: extradelight:beef_stew_feast %.1f, farmersdelight:beef_stew %.1f, farmersdelight:roast_chicken_block %.1f, farmersdelight:roast_chicken %.1f",
                FoodValue.of(item("extradelight:beef_stew_feast")), FoodValue.of(item("farmersdelight:beef_stew")),
                FoodValue.of(item("farmersdelight:roast_chicken_block")), FoodValue.of(item("farmersdelight:roast_chicken"))));
        say("started " + r.scenes.size() + " scenes at " + origin.toShortString() + "; results in up to " + TIMEOUT / 20 + " s");
    }

    private static void clearArea(ServerLevel level, BlockPos c) {
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-12, -1, -12), c.offset(12, 5, 28))) {
            level.setBlock(p, p.getY() == c.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        // a roof: snow and sandstorm piles landing on a stove make it drop its food
        if (run != null) {
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-12, 5, -12), c.offset(12, 5, 28))) {
                level.setBlock(p, Blocks.GLASS.defaultBlockState(), 2 | 16);
            }
        }
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(14, 6, 30))) e.discard();
        // maids left over from an earlier run (saved when their chunk unloaded) would cook from the same chests
        for (EntityMaid m : level.getEntitiesOfClass(EntityMaid.class, new AABB(c).inflate(14, 6, 30))) {
            LOG.info("[maidchef selftest] removing leftover maid {} at {}", m.getId(), m.blockPosition().toShortString());
            m.discard();
        }
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static void place(ServerLevel level, BlockPos p, Block b, boolean lit) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(BlockStateProperties.LIT)) s = s.setValue(BlockStateProperties.LIT, lit);
        level.setBlock(p, s, 3);
    }

    private static BlockPos chest(ServerLevel level, BlockPos p, ItemStack... stacks) {
        level.setBlock(p, Blocks.CHEST.defaultBlockState(), 3);
        IItemHandler h = Planner.handler(level, p);
        for (ItemStack s : stacks) ItemHandlerHelper.insertItemStacked(h, s, false);
        return p;
    }

    private static EntityMaid maid(ServerLevel level, BlockPos c, UUID owner, int radius, int target, @Nullable BlockPos in, @Nullable BlockPos out) {
        return maid(level, c, owner, radius, target, in, out, true);
    }

    private static EntityMaid maid(ServerLevel level, BlockPos c, UUID owner, int radius, int target, @Nullable BlockPos in, @Nullable BlockPos out,
                                   boolean bigBackpack) {
        EntityMaid maid = InitEntities.MAID.get().create(level);
        maid.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        maid.setOwnerUUID(owner);
        maid.setTame(true, false);
        level.addFreshEntity(maid);
        maid.setSchedule(MaidSchedule.ALL);
        // a big backpack: some recipes (feasts) need more kinds of ingredients than the 6 starting slots hold
        if (bigBackpack) {
            com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager.findBackpack(
                    ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "big_backpack")).ifPresent(maid::setMaidBackpackType);
        }
        maid.getSchedulePos().setHomeModeEnable(maid, c);
        maid.setHomeModeEnable(true);
        maid.getSchedulePos().restrictTo(maid);
        maid.setAndSyncData(ChefPlugin.CHEF_DATA, new ChefData(radius, target, 3, 0, 0f, 0, List.of(), true, "", List.of(), "", List.of()));
        if (in != null || out != null) {
            ItemStack hub = new ItemStack(MkItems.CULINARY_HUB.get());
            if (in != null) ItemCulinaryHub.actionModePos(hub, BagType.INGREDIENT.name, in);
            if (out != null) ItemCulinaryHub.actionModePos(hub, BagType.OUTPUT.name, out);
            maid.getMaidInv().setStackInSlot(Inv.HUB_SLOT, hub);
        }
        ChefBehavior.reset(maid);
        maid.setTask(TaskChef.INSTANCE);
        return maid;
    }

    private static int count(ServerLevel level, BlockPos chest, Item item) {
        IItemHandler h = Planner.handler(level, chest);
        return h == null ? -1 : Inv.count(h, item);
    }

    private static String contents(ServerLevel level, @Nullable BlockPos chest) {
        if (chest == null) return "-";
        IItemHandler h = Planner.handler(level, chest);
        return h == null ? "?" : summarize(h);
    }

    private static String summarize(IItemHandler h) {
        Map<Item, Integer> m = new HashMap<>();
        Inv.addCounts(h, m, false);
        StringBuilder b = new StringBuilder("[");
        m.forEach((k, v) -> b.append(BuiltInRegistries.ITEM.getKey(k).getPath()).append('x').append(v).append(' '));
        return b.append(']').toString();
    }

    // ------------------------------------------------------------------ scenes

    /** Smoker: 10 beef + 4 coal, batch 8 → 8 steak delivered, 2 beef and the unused coal returned. */
    private static Scene smoker(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("smoker", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), Blocks.SMOKER, false);
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(Items.BEEF, 10), new ItemStack(Items.COAL, 4));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, 8, s.in, s.out);
        // 8 steak first; the last 2 beef may then be cooked as a second round (it's the only dish left). Nothing may be lost or duplicated.
        s.pass = sc -> {
            int cooked = count(level, sc.out, Items.COOKED_BEEF), raw = count(level, sc.in, Items.BEEF);
            sc.detail = "steak " + cooked + ", raw beef back in chest " + raw;
            return cooked >= 8 && cooked + raw == 10 && TaskChef.data(sc.maid).recent().contains("minecraft:cooked_beef")
                    && ChefBehavior.describe(sc.maid).startsWith("PLAN");
        };
        return s;
    }

    /** Cooking pot on a lit stove (the covered stove itself must not be used): 6 beetroot + 2 bowls → 2 soups. */
    private static Scene pot(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("cooking pot", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), block("farmersdelight:stove"), true);
        place(level, c.offset(3, 1, 0), block("farmersdelight:cooking_pot"), false);
        // FD beetroot soup: 3 beetroot + a bowl. (In this pack Extra Delight disables FD's beef stew pot recipe; it's a feast instead.)
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(Items.BEETROOT, 6), new ItemStack(Items.BOWL, 2));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, 2, s.in, s.out);
        s.pass = sc -> {
            IItemHandler h = Planner.handler(level, sc.out);
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack st = h.getStackInSlot(i);
                if (st.getCount() == 2 && st.get(DataComponents.FOOD) != null) {
                    sc.detail = "made " + BuiltInRegistries.ITEM.getKey(st.getItem());
                    return true;
                }
            }
            return false;
        };
        return s;
    }

    /** Cutting board with the knife in the ingredient chest: a cake becomes 7 slices, the knife goes back. */
    private static Scene cuttingBoard(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("cutting board", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), block("farmersdelight:cutting_board"), false);
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(Items.CAKE), new ItemStack(item("farmersdelight:flint_knife")));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, 7, s.in, s.out);
        s.pass = sc -> count(level, sc.out, item("farmersdelight:cake_slice")) == 7
                && count(level, sc.in, item("farmersdelight:flint_knife")) == 1;
        return s;
    }

    /** Crafting table: 9 wheat, batch 3 → 3 bread. */
    private static Scene crafting(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("crafting table", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), Blocks.CRAFTING_TABLE, false);
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(Items.WHEAT, 9));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, 3, s.in, s.out);
        s.pass = sc -> count(level, sc.out, Items.BREAD) == 3 && count(level, sc.in, Items.WHEAT) == 0;
        return s;
    }

    /** No hub, backpack only, one open stove: chicken (worth more) first, then cod because chicken was just made. */
    private static Scene rotation(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("rotation (backpack, stove)", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), block("farmersdelight:stove"), true);
        s.maid = maid(level, c, owner, 6, 3, null, null);
        IItemHandler bag = Inv.backpack(s.maid);
        ItemHandlerHelper.insertItemStacked(bag, new ItemStack(Items.CHICKEN, 3), false);
        ItemHandlerHelper.insertItemStacked(bag, new ItemStack(Items.COD, 3), false);
        s.pass = sc -> {
            IItemHandler b = Inv.backpack(sc.maid);
            List<String> recent = TaskChef.data(sc.maid).recent();
            sc.detail = "backpack " + summarize(b) + " recent " + recent;
            return Inv.count(b, Items.COOKED_CHICKEN) == 3 && Inv.count(b, Items.COOKED_COD) == 3
                    && recent.size() >= 2 && recent.get(0).equals("minecraft:cooked_cod") && recent.get(1).equals("minecraft:cooked_chicken");
        };
        return s;
    }

    /** Kitchen radius 20: the smoker is 16 blocks out, past TLM's global work range of 12. */
    private static Scene radius(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("radius 20 (smoker 16 away)", c);
        clearArea(level, c);
        place(level, c.offset(0, 0, 16), Blocks.SMOKER, false);
        s.in = chest(level, c.offset(-2, 0, 0), new ItemStack(Items.PORKCHOP, 4), new ItemStack(Items.COAL, 1));
        s.out = chest(level, c.offset(-2, 0, 2));
        s.maid = maid(level, c, owner, 20, 4, s.in, s.out);
        s.pass = sc -> count(level, sc.out, Items.COOKED_PORKCHOP) == 4;
        return s;
    }

    /**
     * Stove, chicken + cod in her backpack. While the chicken cooks, a (fake) player "Thief" takes 1 cooked chicken.
     * Not returned: she gives up on chicken after returnWaitSeconds and makes the cod instead.
     * Returned (put into her backpack): she thanks them and finishes the chicken.
     */
    private static Scene theft(ServerLevel level, BlockPos c, UUID owner, boolean giveBack) {
        Scene s = new Scene(giveBack ? "taken item, given back" : "taken item, never returned", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), block("farmersdelight:stove"), true);
        s.maid = maid(level, c, owner, 6, 2, null, null);
        IItemHandler bag = Inv.backpack(s.maid);
        ItemHandlerHelper.insertItemStacked(bag, new ItemStack(Items.CHICKEN, 2), false);
        ItemHandlerHelper.insertItemStacked(bag, new ItemStack(Items.COD, 2), false);
        s.onTick = sc -> {
            String st = ChefBehavior.describe(sc.maid);
            if (!sc.triggered && st.startsWith("COOK") && st.contains("cooked_chicken")) {
                sc.triggered = true;
                net.minecraft.server.level.ServerPlayer thief = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                        new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("maidchef-thief".getBytes()), "Thief"));
                ChefBehavior.owed(sc.maid, thief, Items.COOKED_CHICKEN, 1);
                LOG.info("[maidchef selftest] {}: Thief took 1 cooked chicken", sc.name);
            } else if (giveBack && sc.triggered && !sc.sawThanks && TaskChef.data(sc.maid).status().startsWith("Paused")) {
                ItemHandlerHelper.insertItemStacked(Inv.backpack(sc.maid), new ItemStack(Items.COOKED_CHICKEN, 1), false);
                LOG.info("[maidchef selftest] {}: Thief put 1 cooked chicken in her backpack", sc.name);
                sc.sawThanks = true;
            }
            if (TaskChef.data(sc.maid).status().startsWith("Gave up")) sc.sawGaveUp = true;
        };
        s.pass = sc -> {
            List<String> recent = TaskChef.data(sc.maid).recent();
            IItemHandler b = Inv.backpack(sc.maid);
            sc.detail = "backpack " + summarize(b) + " recent " + recent + (sc.sawGaveUp ? " (gave up seen)" : "");
            if (!sc.triggered) return false;
            if (giveBack) {
                return !sc.sawGaveUp && Inv.count(b, Items.COOKED_CHICKEN) == 3 && !recent.isEmpty() && recent.contains("minecraft:cooked_chicken");
            }
            return sc.sawGaveUp && !recent.isEmpty() && recent.get(0).equals("minecraft:cooked_cod") && recent.contains("minecraft:cooked_chicken")
                    && Inv.count(b, Items.COOKED_COD) == 2;
        };
        return s;
    }

    /** Blacklist cooked chicken, min nutrition 5: she makes cod and baked potato (both 5) but leaves the chicken raw. */
    private static Scene filters(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("filters (blacklist + min nutrition)", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), block("farmersdelight:stove"), true);
        s.maid = maid(level, c, owner, 6, 2, null, null);
        s.maid.setAndSyncData(ChefPlugin.CHEF_DATA, new ChefData(6, 2, 3, 5, 0f, 0, List.of("minecraft:cooked_chicken"), true, "", List.of(), "", List.of()));
        IItemHandler bag = Inv.backpack(s.maid);
        ItemHandlerHelper.insertItemStacked(bag, new ItemStack(Items.CHICKEN, 2), false);
        ItemHandlerHelper.insertItemStacked(bag, new ItemStack(Items.COD, 2), false);
        ItemHandlerHelper.insertItemStacked(bag, new ItemStack(Items.POTATO, 2), false);
        s.pass = sc -> {
            IItemHandler b = Inv.backpack(sc.maid);
            sc.detail = "backpack " + summarize(b) + " recent " + TaskChef.data(sc.maid).recent();
            return Inv.count(b, Items.COOKED_COD) == 2 && Inv.count(b, Items.BAKED_POTATO) == 2
                    && Inv.count(b, Items.CHICKEN) == 2 && Inv.count(b, Items.COOKED_CHICKEN) == 0;
        };
        return s;
    }

    /** Recent = [chicken, cod] (cod oldest), only chicken possible: cod is tried first and dropped, then chicken is made. */
    private static Scene fallback(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("recent fallback (oldest first)", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), block("farmersdelight:stove"), true);
        s.maid = maid(level, c, owner, 6, 2, null, null);
        s.maid.setAndSyncData(ChefPlugin.CHEF_DATA, new ChefData(6, 2, 2, 0, 0f, 0, List.of(), true, "",
                List.of("minecraft:cooked_chicken", "minecraft:cooked_cod"), "", List.of()));
        ItemHandlerHelper.insertItemStacked(Inv.backpack(s.maid), new ItemStack(Items.CHICKEN, 2), false);
        s.pass = sc -> {
            IItemHandler b = Inv.backpack(sc.maid);
            List<String> recent = TaskChef.data(sc.maid).recent();
            sc.detail = "backpack " + summarize(b) + " recent " + recent;
            return Inv.count(b, Items.COOKED_CHICKEN) == 2 && recent.equals(List.of("minecraft:cooked_chicken"));
        };
        return s;
    }

    /** P2 chain: fry 2 eggs in the smoker, then craft an egg sandwich (bread + 2 cooked eggs). */
    private static Scene chain(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("chain (smoke eggs, craft sandwich)", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), Blocks.SMOKER, false);
        place(level, c.offset(3, 0, 2), Blocks.CRAFTING_TABLE, false);
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(Items.EGG, 2), new ItemStack(Items.BREAD), new ItemStack(Items.COAL, 1));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, 1, s.in, s.out);
        Item sandwich = item("farmersdelight:egg_sandwich");
        s.onTick = sc -> {
            String d = ChefBehavior.describe(sc.maid);
            if (d.contains("step 2/")) sc.sawChain = true;
            if (!sc.triggered) {
                sc.triggered = true;
                for (String line : Planner.explain(level, sc.maid, TaskChef.data(sc.maid), 5)) LOG.info("[maidchef selftest] chain explain: {}", line);
            }
        };
        s.pass = sc -> {
            int made = count(level, sc.out, sandwich);
            sc.detail = "egg sandwich " + made + (sc.sawChain ? " (multi-step seen)" : "") + " recent " + TaskChef.data(sc.maid).recent();
            return made == 1 && sc.sawChain;
        };
        return s;
    }

    /** P2 feast: craft FD's gleaming salad feast, then (serve on) place it and serve every portion into bowls, or (serve off) deliver the block. */
    private static Scene feast(ServerLevel level, BlockPos c, UUID owner, boolean serve) {
        Scene s = new Scene(serve ? "feast served into bowls" : "feast kept whole", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), Blocks.CRAFTING_TABLE, false);
        Item feastItem = item("farmersdelight:gleaming_salad_block");
        FdFeast.Feast info = feastItem instanceof net.minecraft.world.item.BlockItem bi ? FdFeast.of(bi.getBlock()) : null;
        int servings = info == null ? 0 : info.servings();
        Item portion = info == null ? Items.AIR : info.serving();
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(Items.GLOW_BERRIES, 2), new ItemStack(Items.HONEY_BOTTLE),
                new ItemStack(item("farmersdelight:tomato")), new ItemStack(Items.GOLDEN_CARROT), new ItemStack(Items.BEETROOT),
                new ItemStack(item("farmersdelight:cabbage"), 2), new ItemStack(Items.BOWL, serve ? 1 + servings : 1));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, serve ? servings : 1, s.in, s.out);
        ChefData d = TaskChef.data(s.maid);
        // Rustic Delight's sweet salad uses the same ingredients and is worth more per item, so keep it out of this test
        s.maid.setAndSyncData(ChefPlugin.CHEF_DATA, d.withSettings(d.radius(), d.target(), d.recentSize(), 0, 0f, 0,
                List.of("rusticdelight:sweet_salad"), serve));
        s.onTick = sc -> {
            // explain at 1 s and again at 3 s: the Status tab must agree with what she actually decides
            if (!sc.triggered || !sc.sawThanks && ChefBehavior.describe(sc.maid).startsWith("FETCH")) {
                boolean second = sc.triggered;
                sc.triggered = true;
                if (second) sc.sawThanks = true;
                for (String line : Planner.explain(level, sc.maid, TaskChef.data(sc.maid), 5))
                    LOG.info("[maidchef selftest] {} explain{}: {}", sc.name, second ? " (while fetching)" : "", line);
                LOG.info("[maidchef selftest] {} state: {} bag {}", sc.name, ChefBehavior.describe(sc.maid), summarize(sc.maid.getAvailableInv(true)));
            }
        };
        net.minecraft.world.level.block.Block feastBlock = ((net.minecraft.world.item.BlockItem) feastItem).getBlock();
        s.pass = sc -> {
            int portions = count(level, sc.out, portion), blocks = count(level, sc.out, feastItem);
            boolean placedLeft = false;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-8, -1, -8), c.offset(8, 3, 8))) {
                if (level.getBlockState(p).is(feastBlock)) placedLeft = true;
            }
            sc.detail = "servings per feast " + servings + ", portions " + portions + ", feast blocks " + blocks
                    + (placedLeft ? ", a feast is still standing" : "") + " | in " + contents(level, sc.in) + " recent " + TaskChef.data(sc.maid).recent();
            return servings > 0 && (serve ? portions == servings && !placedLeft && ChefBehavior.describe(sc.maid).startsWith("PLAN") : blocks == 1);
        };
        return s;
    }

    /** No backpack (5 free slots) but 8 kinds of ingredients: the crafting table takes them straight from the chest. */
    private static Scene pantryCraft(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("pantry craft (small backpack)", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), Blocks.CRAFTING_TABLE, false);
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(Items.GLOW_BERRIES, 2), new ItemStack(Items.HONEY_BOTTLE),
                new ItemStack(item("farmersdelight:tomato")), new ItemStack(Items.GOLDEN_CARROT), new ItemStack(Items.BEETROOT),
                new ItemStack(item("farmersdelight:cabbage"), 2), new ItemStack(Items.BOWL));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, 1, s.in, s.out, false);
        ChefData d = TaskChef.data(s.maid);
        s.maid.setAndSyncData(ChefPlugin.CHEF_DATA, d.withSettings(d.radius(), d.target(), d.recentSize(), 0, 0f, 0,
                List.of("rusticdelight:sweet_salad"), false));
        Item block = item("farmersdelight:gleaming_salad_block");
        s.pass = sc -> {
            int made = count(level, sc.out, block);
            sc.detail = "feast blocks " + made + " | in " + contents(level, sc.in) + " bag " + summarize(Inv.backpack(sc.maid));
            return made == 1;
        };
        return s;
    }

    /** No backpack (5 free slots), a pot recipe with 6 ingredients + a pumpkin as container: she fills the pot over several trips. */
    private static Scene potFerry(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("pot filled over several trips (small backpack)", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), block("farmersdelight:stove"), true);
        place(level, c.offset(3, 1, 0), block("farmersdelight:cooking_pot"), false);
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(item("farmersdelight:rice")), new ItemStack(item("farmersdelight:onion")),
                new ItemStack(Items.BROWN_MUSHROOM), new ItemStack(Items.POTATO), new ItemStack(Items.SWEET_BERRIES), new ItemStack(Items.CARROT),
                new ItemStack(Items.PUMPKIN));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, 1, s.in, s.out, false);
        ChefData d = TaskChef.data(s.maid);
        s.maid.setAndSyncData(ChefPlugin.CHEF_DATA, d.withSettings(d.radius(), d.target(), d.recentSize(), 0, 0f, 0, List.of(), false));
        Item block = item("farmersdelight:stuffed_pumpkin_block");
        s.onTick = sc -> {
            if (!sc.triggered) {
                sc.triggered = true;
                for (String line : Planner.explain(level, sc.maid, TaskChef.data(sc.maid), 4)) LOG.info("[maidchef selftest] {} explain: {}", sc.name, line);
            }
        };
        s.pass = sc -> {
            int made = count(level, sc.out, block);
            sc.detail = "stuffed pumpkins " + made + " | in " + contents(level, sc.in) + " bag " + summarize(Inv.backpack(sc.maid));
            return made == 1;
        };
        return s;
    }

    /**
     * P3: only eggs and coal (she can fry eggs). She should ask for at most one kind of item for a much better dish; the item
     * appears on the shared list; after blocking it, her next request must not include it.
     */
    private static Scene request(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("request, then block", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), Blocks.SMOKER, false);
        place(level, c.offset(3, 0, 2), Blocks.CRAFTING_TABLE, false);
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(Items.EGG, 4), new ItemStack(Items.COAL, 4));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, 2, s.in, s.out);
        s.onTick = sc -> {
            ChefBehavior.State st = ChefBehavior.state(sc.maid);
            ChefRequests reqs = ChefRequests.get(level.getServer());
            if (st.requestCount >= 1 && sc.firstAsk.isEmpty() && st.lastRequest != null) {
                Planner.Request q = st.lastRequest;
                sc.firstAsk = Set.copyOf(q.items().keySet());
                sc.requestLog = "1st: " + q.itemsText() + " -> " + q.amount() + "x " + q.dishName() + String.format(" (%.0f vs %.0f)", q.value(), q.currentBest());
                LOG.info("[maidchef selftest] {}: {}", sc.name, sc.requestLog);
                boolean listed = sc.firstAsk.stream().allMatch(i -> {
                    ChefRequests.Entry e = reqs.list(owner).get(BuiltInRegistries.ITEM.getKey(i).toString());
                    return e != null && !e.blocked();
                });
                boolean ok = q.items().size() <= ChefConfig.REQUEST_MAX_MISSING.get()
                        && q.value() >= q.currentBest() * (1 + ChefConfig.REQUEST_MIN_GAIN_PERCENT.get() / 100.0) - 1e-6 && listed;
                if (!ok) sc.requestLog += " BAD (too many items, too small a gain, or not listed)";
                for (Item i : sc.firstAsk) reqs.setBlocked(owner, BuiltInRegistries.ITEM.getKey(i).toString(), true);
                st.nextRequest = 0;
                st.lastRequestKey = "";
                sc.blockedAt = r(level);
            }
        };
        s.pass = sc -> {
            ChefBehavior.State st = ChefBehavior.state(sc.maid);
            if (sc.firstAsk.isEmpty() || sc.requestLog.contains("BAD")) {
                sc.detail = sc.requestLog.isEmpty() ? "no request yet" : sc.requestLog;
                return false;
            }
            if (st.requestCount >= 2 && st.lastRequest != null) {
                Planner.Request q = st.lastRequest;
                boolean clean = q.items().keySet().stream().noneMatch(sc.firstAsk::contains);
                sc.detail = sc.requestLog + " | 2nd after blocking: " + q.itemsText() + " -> " + q.dishName() + (clean ? "" : " (ASKED FOR A BLOCKED ITEM)");
                return clean;
            }
            // no second request at all is fine too (nothing else close enough): give her 30 s after blocking
            sc.detail = sc.requestLog + " | no 2nd request yet";
            return r(level) - sc.blockedAt > 600 && sc.blockedAt > 0;
        };
        return s;
    }

    /**
     * P4 "Prepare X". Reachable: prepare fried eggs although an egg sandwich (bread is there) is worth more.
     * Unreachable: prepare an egg sandwich with no bread: she says she can't, and asks for what it needs.
     */
    private static Scene prepare(ServerLevel level, BlockPos c, UUID owner, boolean unreachable) {
        Scene s = new Scene(unreachable ? "prepare X, missing an ingredient" : "prepare X (not the best dish)", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), Blocks.SMOKER, false);
        place(level, c.offset(3, 0, 2), Blocks.CRAFTING_TABLE, false);
        s.in = unreachable
                ? chest(level, c.offset(-3, 0, 0), new ItemStack(Items.EGG, 2), new ItemStack(Items.COAL, 2))
                : chest(level, c.offset(-3, 0, 0), new ItemStack(Items.EGG, 4), new ItemStack(Items.BREAD, 2), new ItemStack(Items.COAL, 2));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, unreachable ? 1 : 2, s.in, s.out);
        ChefData d = TaskChef.data(s.maid);
        String dish = unreachable ? "farmersdelight:egg_sandwich" : "farmersdelight:fried_egg";
        s.maid.setAndSyncData(ChefPlugin.CHEF_DATA, d.withSettings(d.radius(), d.target(), d.recentSize(), 0, 0f, 0, List.of(), true, dish));
        Item fried = item("farmersdelight:fried_egg"), sandwich = item("farmersdelight:egg_sandwich");
        s.pass = sc -> {
            if (!unreachable) {
                int f = count(level, sc.out, fried), w = count(level, sc.out, sandwich);
                sc.detail = "fried eggs " + f + ", sandwiches " + w + " | status " + TaskChef.data(sc.maid).status();
                return f >= 2 && w == 0;
            }
            ChefBehavior.State st = ChefBehavior.state(sc.maid);
            String status = TaskChef.data(sc.maid).status();
            Planner.Request q = st.lastRequest;
            sc.detail = "status: " + status + " | request: " + (q == null ? "none" : q.itemsText() + " -> " + q.dishName());
            boolean named = status.contains("can't make") && status.contains("missing");
            boolean asksBread = q != null && q.dish() == sandwich && q.items().size() == 1
                    && q.items().keySet().iterator().next().builtInRegistryHolder().is(
                    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse("c:foods/bread")));
            // plain vanilla bread, not a modded variant from the same tag
            boolean plain = q != null && q.items().containsKey(Items.BREAD) && status.contains("missing Bread");
            return named && asksBread && plain && count(level, sc.out, sandwich) == 0;
        };
        return s;
    }

    /**
     * Extra Delight oven on a lit stove: prepare roasted carrots from 4 carrots. The recipe keeps its baking sheet,
     * so she has to bring the sheet along as a tool, and it must end up back in the ingredient chest.
     */
    private static Scene oven(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("Extra Delight oven", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), block("farmersdelight:stove"), true);
        place(level, c.offset(3, 1, 0), block("extradelight:oven"), false);
        Item sheet = item("extradelight:sheet"), roasted = item("extradelight:roasted_carrot");
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(Items.CARROT, 4), new ItemStack(sheet, 1));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, 4, s.in, s.out);
        ChefData d = TaskChef.data(s.maid);
        s.maid.setAndSyncData(ChefPlugin.CHEF_DATA, d.withSettings(d.radius(), d.target(), d.recentSize(), 0, 0f, 0, List.of(), false,
                "extradelight:roasted_carrot"));
        s.pass = sc -> {
            int made = count(level, sc.out, roasted), back = count(level, sc.in, sheet);
            sc.detail = "roasted carrots " + made + ", sheet back in ingredient chest " + back + " | status " + TaskChef.data(sc.maid).status();
            return made >= 4 && back == 1;
        };
        return s;
    }

    /**
     * One station, one recipe, in Prepare mode: the ingredient chest gets exactly what the recipe asks for (first item of each
     * ingredient, the container, the tool, fluid bottles, ice), and the output chest must end up with the result.
     */
    private static Scene stationScene(ServerLevel level, BlockPos c, UUID owner, String name, String blockSpec, boolean heat, String recipeId, int units,
                                      String flags) {
        Scene s = new Scene(name, c);
        clearArea(level, c);
        net.eileenthg.maidchef.station.RecipeNeed need = null;
        for (var st : net.eileenthg.maidchef.station.Stations.all()) {
            for (var n : net.eileenthg.maidchef.station.Stations.recipes(level, st)) {
                if (n.id().toString().equals(recipeId)) need = n;
            }
        }
        BlockPos at = c.offset(3, 0, 0);
        if (heat) {
            place(level, at, block("farmersdelight:stove"), true);
            at = at.above();
        }
        // "id[prop=value,...]"; "@pot": the pot a Youkai's Homecoming soup is cooked in (its result's leftover)
        String blockId = blockSpec.contains("[") ? blockSpec.substring(0, blockSpec.indexOf('[')) : blockSpec;
        Block b = null;
        if (blockId.equals("@pot") && need != null && need.holder() != null
                && need.holder().value().getResultItem(level.registryAccess()).getCraftingRemainingItem().getItem() instanceof net.minecraft.world.item.BlockItem bi) {
            b = bi.getBlock();
        } else if (!blockId.startsWith("@")) {
            b = block(blockId);
        }
        if (b != null) {
            BlockState state = b.defaultBlockState();
            if (blockSpec.contains("[")) {
                for (String kv : blockSpec.substring(blockSpec.indexOf('[') + 1, blockSpec.length() - 1).split(",")) {
                    String[] p = kv.split("=");
                    for (var prop : state.getProperties()) {
                        if (prop.getName().equals(p[0])) state = withValue(state, prop, p[1]);
                    }
                }
            }
            level.setBlock(at, state, 3);
        }
        if (flags.contains("sky")) {
            // sun-dried: no glass roof over it, and daytime
            for (int y = at.getY() + 1; y <= c.getY() + 6; y++) level.setBlock(new BlockPos(at.getX(), y, at.getZ()), Blocks.AIR.defaultBlockState(), 3);
            level.setDayTime(1000);
        }
        final BlockPos stationPos = at;
        if (flags.contains("fast")) {
            // a keg takes 15 minutes a bucket: skip ahead once it has started
            s.onTick = sc -> {
                var be = level.getBlockEntity(stationPos);
                if (be == null) return;
                for (String[] names : new String[][]{{"fermentTime", "fermentTimeTotal"}, {"cookTime", "cookTimeTotal"}}) {
                    try {
                        var ft = be.getClass().getDeclaredField(names[0]);
                        var tt = be.getClass().getDeclaredField(names[1]);
                        ft.setAccessible(true);
                        tt.setAccessible(true);
                        int f = ft.getInt(be), t = tt.getInt(be);
                        if (f > 0 && t > 40 && f < t - 40) ft.setInt(be, t - 40);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            };
        }
        if (need == null) {
            StringBuilder counts = new StringBuilder();
            for (var st : net.eileenthg.maidchef.station.Stations.all()) {
                counts.append(st.name()).append('=').append(net.eileenthg.maidchef.station.Stations.recipes(level, st).size()).append(' ');
            }
            LOG.info("[maidchef selftest] {}: recipe {} not found; stations: {}", name, recipeId, counts);
            s.pass = sc -> {
                sc.detail = "recipe " + recipeId + " not supported by any station";
                return false;
            };
            return s;
        }
        Map<Item, Integer> stock = new java.util.LinkedHashMap<>();
        for (var ing : need.inputs()) stock.merge(ing.getItems()[0].getItem(), units, Integer::sum);
        if (!need.container().isEmpty()) stock.merge(need.container().getItem(), units * need.resultCount() * need.container().getCount(), Integer::sum);
        if (need.tool() != null) stock.merge(need.tool().getItems()[0].getItem(), 1, Integer::sum);
        if (flags.contains("fuel")) stock.merge(Items.COAL, 4, Integer::sum);
        List<ItemStack> stacks = new ArrayList<>();
        stock.forEach((it, n) -> {
            for (int left = n; left > 0; left -= it.getDefaultMaxStackSize()) stacks.add(new ItemStack(it, Math.min(left, it.getDefaultMaxStackSize())));
        });
        s.in = chest(level, c.offset(-3, 0, 0), stacks.toArray(ItemStack[]::new));
        s.out = chest(level, c.offset(-3, 0, 2));
        int expect = units * need.resultCount();
        Item result = need.resultItem();
        Item tool = need.tool() == null ? null : need.tool().getItems()[0].getItem();
        s.maid = maid(level, c, owner, 6, expect, s.in, s.out);
        ChefData d = TaskChef.data(s.maid);
        s.maid.setAndSyncData(ChefPlugin.CHEF_DATA, d.withSettings(d.radius(), d.target(), d.recentSize(), 0, 0f, 0, List.of(), false,
                BuiltInRegistries.ITEM.getKey(result).toString()));
        String stocked = stock.toString();
        s.pass = sc -> {
            int made = count(level, sc.out, result);
            boolean toolBack = tool == null || count(level, sc.in, tool) >= 1;
            sc.detail = BuiltInRegistries.ITEM.getKey(result).getPath() + " " + made + "/" + expect + (tool == null ? "" : ", tool back " + toolBack)
                    + " | stocked " + stocked + " | status " + TaskChef.data(sc.maid).status();
            return made >= expect && toolBack;
        };
        return s;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> prop, String value) {
        return prop.getValue(value).map(v -> state.setValue(prop, v)).orElse(state);
    }

    /** (2) the prepared-food rule, and the chat's item matching. */
    private static void staticChecks(ServerLevel level, Run r) {
        Set<Item> prepared = net.eileenthg.maidchef.station.Stations.preparedFoods(level);
        Item bread = Items.BREAD, salad = item("extradelight:fish_salad"), croque = item("extradelight:croque_monsieur");
        boolean breadOk = !prepared.contains(bread);
        boolean dishesOk = (salad == Items.AIR || prepared.contains(salad)) && (croque == Items.AIR || prepared.contains(croque));
        r.checks.add(new String[]{"prepared-food rule", String.valueOf(breadOk && dishesOk),
                "bread askable " + breadOk + ", fish salad/croque monsieur prepared " + dishesOk + " (" + prepared.size() + " prepared foods)"});
        Item a = ChefChat.findItem("Fried Egg"), b = ChefChat.findItem("egg sandwich"), c = ChefChat.findItem("minecraft:bread");
        boolean found = a == item("farmersdelight:fried_egg") && b == item("farmersdelight:egg_sandwich") && c == Items.BREAD;
        // Spectrum gates its recipes on an online owner's advancements, so the workshop and barrels can't run headless:
        // check that their recipes are at least understood
        int workshop = 0, barrel = 0;
        for (var st : net.eileenthg.maidchef.station.Stations.all()) {
            if (st.name().equals("potion workshop")) workshop = net.eileenthg.maidchef.station.Stations.recipes(level, st).size();
            if (st.name().equals("titration barrel")) barrel = net.eileenthg.maidchef.station.Stations.recipes(level, st).size();
        }
        r.checks.add(new String[]{"Spectrum recipes read", String.valueOf(workshop > 10 && barrel > 30),
                "potion workshop " + workshop + " recipes, titration barrel " + barrel + " recipes"});
        r.checks.add(new String[]{"chat: dish names", String.valueOf(found), "Fried Egg -> " + a + ", egg sandwich -> " + b + ", minecraft:bread -> " + c});
    }

    /**
     * Talk to a Chef maid through TLM's AI chat (Player2 on this machine): ask what she's cooking, then ask her to make fried eggs.
     * Passes once she has answered both; the second question should switch her to "prepare fried eggs" via the chef_prepare tool.
     */
    private static Scene chat(ServerLevel level, BlockPos c, UUID owner) {
        Scene s = new Scene("chat with her (Player2)", c);
        clearArea(level, c);
        place(level, c.offset(3, 0, 0), Blocks.SMOKER, false);
        place(level, c.offset(3, 0, 2), Blocks.CRAFTING_TABLE, false);
        s.in = chest(level, c.offset(-3, 0, 0), new ItemStack(Items.EGG, 6), new ItemStack(Items.BREAD, 2), new ItemStack(Items.COAL, 2));
        s.out = chest(level, c.offset(-3, 0, 2));
        s.maid = maid(level, c, owner, 6, 1, s.in, s.out);
        s.maid.getAiChatManager().llmSite = "player2";
        net.minecraft.server.level.ServerPlayer tester = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(owner, "Tester"));
        com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo info =
                new com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo("en_us", "Tester", List.of());
        String q1 = "What are you cooking right now, and is there anything you'd like me to bring you?";
        String q2 = "Please make fried eggs for now.";
        s.onTick = sc -> {
            long t = r(level);
            var history = sc.maid.getAiChatManager().getHistory().getDeque();
            long replies = history.stream().filter(m -> m.role() == com.github.tartaricacid.touhoulittlemaid.ai.service.llm.Role.ASSISTANT
                    && m.message() != null && !m.message().isBlank()).count();
            if (sc.blockedAt == 0 && ChefBehavior.describe(sc.maid).startsWith("COOK")) {
                sc.blockedAt = t;
                LOG.info("[maidchef selftest] {}: asking '{}'", sc.name, q1);
                sc.maid.getAiChatManager().chat(q1, info, tester);
            } else if (sc.blockedAt > 0 && !sc.triggered && replies >= 1) {
                sc.triggered = true;
                LOG.info("[maidchef selftest] {}: asking '{}'", sc.name, q2);
                sc.maid.getAiChatManager().chat(q2, info, tester);
            }
            StringBuilder log = new StringBuilder();
            for (var m : history) {
                if (m.role() == com.github.tartaricacid.touhoulittlemaid.ai.service.llm.Role.SYSTEM) continue;
                String text = m.message() == null ? "" : m.message().replaceAll("<context>[\\s\\S]*?</context>", "[context]").replace('\n', ' ');
                log.append(" | ").append(m.role().name()).append(m.toolCalls() != null && !m.toolCalls().isEmpty() ? "(tool call)" : "")
                        .append(": ").append(text.length() > 220 ? text.substring(0, 220) + "..." : text);
            }
            sc.requestLog = log.toString();
            sc.sawChain = replies >= 2;
        };
        s.pass = sc -> {
            String dish = TaskChef.data(sc.maid).dish();
            sc.detail = "dish now '" + dish + "'" + sc.requestLog;
            return sc.sawChain && sc.triggered;
        };
        return s;
    }

    private static long r(ServerLevel level) {
        return level.getGameTime();
    }

    // ------------------------------------------------------------------ run

    public static void tick(ServerTickEvent.Post event) {
        Run r = run;
        if (r == null) return;
        r.ticks++;
        if (r.ticks % 20 != 0) return;
        boolean all = true;
        for (Scene s : r.scenes) {
            if (s.done) continue;
            if (s.maid == null || !s.maid.isAlive()) {
                s.done = true;
                s.detail = s.maid == null ? "maid gone" : "maid gone: removed=" + s.maid.getRemovalReason() + " hp=" + s.maid.getHealth()
                        + " at " + s.maid.blockPosition().toShortString() + " (vanilla-forced chunks still set: " + r.level.getForcedChunks().size() + ")";
                continue;
            }
            if (s.onTick != null) s.onTick.accept(s);
            // maids saved by an earlier run load a moment after the chunks do: keep clearing them for the first seconds
            if (r.ticks <= 100) {
                for (EntityMaid m : r.level.getEntitiesOfClass(EntityMaid.class, new AABB(s.center).inflate(14, 6, 30))) {
                    if (r.scenes.stream().noneMatch(x -> x.maid == m)) {
                        LOG.info("[maidchef selftest] removing leftover maid {} at {}", m.getId(), m.blockPosition().toShortString());
                        m.discard();
                    }
                }
            }
            if (r.ticks % 100 == 0) {
                LOG.info("[maidchef selftest] {} @{}s snapshot: bag {} in {} out {}", s.name, r.ticks / 20, summarize(s.maid.getAvailableInv(true)),
                        contents(r.level, s.in), contents(r.level, s.out));
            }
            String st = ChefBehavior.describe(s.maid) + " | " + TaskChef.data(s.maid).status();
            if (!st.equals(s.lastState)) {
                s.lastState = st;
                LOG.info("[maidchef selftest] {} @{}s: {}", s.name, r.ticks / 20, st);
            }
            if (s.pass.test(s)) {
                s.done = true;
                s.ok = true;
            } else {
                all = false;
            }
        }
        if (all || r.ticks >= TIMEOUT) finish(r);
    }

    /** Log any maid death during a run, with its cause. */
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        Run r = run;
        if (r == null || !(event.getEntity() instanceof EntityMaid maid)) return;
        for (Scene s : r.scenes) {
            if (s.maid == maid) {
                LOG.info("[maidchef selftest] {}: maid died: {} (attacker {})", s.name, event.getSource().getMsgId(),
                        event.getSource().getEntity() == null ? "-" : event.getSource().getEntity().getType().toShortString());
            }
        }
    }

    private static void finish(Run r) {
        int passed = 0;
        for (Scene s : r.scenes) {
            if (s.ok) passed++;
            String line = (s.ok ? "PASS " : "FAIL ") + s.name + (s.detail.isEmpty() ? "" : " - " + s.detail)
                    + (s.ok ? "" : " | in " + contents(r.level, s.in) + " out " + contents(r.level, s.out)
                    + " bag " + (s.maid != null ? summarize(Inv.backpack(s.maid)) : "-") + " | " + s.lastState);
            say(line);
            if (!s.ok) {
                StringBuilder blocks = new StringBuilder();
                for (BlockPos p : BlockPos.betweenClosed(s.center.offset(-12, 0, -12), s.center.offset(12, 4, 28))) {
                    BlockState st = r.level.getBlockState(p);
                    if (!st.isAir()) blocks.append(BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath()).append('@').append(p.toShortString()).append(' ');
                }
                say("    blocks: " + blocks);
            }
            if (!s.ok && s.maid != null && s.maid.level() instanceof ServerLevel sl) {
                for (String e : Planner.explain(sl, s.maid, TaskChef.data(s.maid), 8)) say("    " + e.replace(Planner.ITEM_SEP, '#'));
            }
        }
        for (String[] c : r.checks) {
            boolean ok = Boolean.parseBoolean(c[1]);
            if (ok) passed++;
            say((ok ? "PASS " : "FAIL ") + c[0] + " - " + c[2]);
        }
        say(passed + "/" + (r.scenes.size() + r.checks.size()) + " passed after " + r.ticks / 20 + " s");
        clean(r.level.getServer());
    }

    public static void clean(MinecraftServer server) {
        Run r = run;
        run = null;
        MaidChef.selfTestActive = false;
        Planner.ignoreKnownItems = false;
        if (r == null) return;
        for (Scene s : r.scenes) {
            if (s.maid != null) s.maid.discard();
            clearArea(r.level, s.center);
            for (BlockPos p : BlockPos.betweenClosed(s.center.offset(-12, -1, -12), s.center.offset(12, -1, 28))) {
                r.level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
            for (BlockPos p : BlockPos.betweenClosed(s.center.offset(-12, 5, -12), s.center.offset(12, 5, 28))) {
                r.level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
        if (r.owner != null) ChefRequests.get(r.level.getServer()).clear(r.owner);
        for (ChunkPos cp : r.forced) {
            r.level.setChunkForced(cp.x, cp.z, false);
            MaidChef.TICKETS.forceChunk(r.level, r.origin, cp.x, cp.z, false, true);
        }
    }

    private static void say(String msg) {
        LOG.info("[maidchef selftest] {}", msg);
        Run r = run;
        if (r != null && r.source != null) r.source.sendSuccess(() -> Component.literal("[maidchef] " + msg), false);
    }
}
