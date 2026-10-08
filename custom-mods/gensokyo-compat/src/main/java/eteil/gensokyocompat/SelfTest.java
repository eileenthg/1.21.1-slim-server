package eteil.gensokyocompat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** /gensokyocompat selftest: checks the bridge end to end on a running server. Results go to the log. */
final class SelfTest {
    private static final Logger LOG = LogUtils.getLogger();

    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(GensokyoCompat.MOD_ID).requires(s -> s.hasPermission(2))
                .then(Commands.literal("selftest").executes(c -> {
                    run(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("[gensokyocompat] selftest started, see log"), false);
                    return 1;
                })));
    }

    private static int pass, fail;

    private static void check(String name, boolean ok, String detail) {
        if (ok) pass++; else fail++;
        LOG.info("[gensokyocompat] SELFTEST {} {} {}", ok ? "PASS" : "FAIL", name, detail);
    }

    static void run(MinecraftServer server) {
        pass = fail = 0;
        ServerLevel level = server.overworld();
        BlockPos base = level.getSharedSpawnPos().atY(level.getMaxBuildHeight() - 20);
        level.setChunkForced(base.getX() >> 4, base.getZ() >> 4, true);

        // 1. character swap: spawn every YH character, expect only Legacy ones a few ticks later
        List<EntityType<?>> spawned = new ArrayList<>();
        int i = 0;
        for (String c : Bridge.CHARACTERS) {
            EntityType<?> yh = BuiltInRegistries.ENTITY_TYPE.getOptional(Bridge.id(Bridge.YH, c)).orElse(null);
            if (yh == null) { check("swap " + c, false, "YH type missing"); continue; }
            BlockPos p = base.offset((i++ % 4) * 3, 0, (i / 4) * 3);
            level.setBlockAndUpdate(p.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            yh.spawn(level, p, MobSpawnType.SPAWN_EGG);
            spawned.add(yh);
        }
        AABB box = new AABB(base).inflate(16);
        server.tell(new TickTask(server.getTickCount() + 5, () -> {
            for (String c : Bridge.CHARACTERS) {
                EntityType<?> yh = BuiltInRegistries.ENTITY_TYPE.get(Bridge.id(Bridge.YH, c));
                EntityType<?> gl = BuiltInRegistries.ENTITY_TYPE.get(Bridge.id(Bridge.GL, c));
                int yhN = level.getEntities(yh, box, e -> true).size();
                int glN = level.getEntities(gl, box, e -> true).size();
                check("swap " + c, yhN == 0 && glN == 1, "yh=" + yhN + " legacy=" + glN);
            }
            level.getEntities((Entity) null, box, e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(Bridge.GL)).forEach(Entity::discard);
            level.setChunkForced(base.getX() >> 4, base.getZ() >> 4, false);
            finish();
        }));

        // 2. YH hats: right-click equips into the head slot, with an equip sound
        var player = FakePlayerFactory.getMinecraft(level);
        for (String hat : List.of("koishi_hat", "suwako_hat", "straw_hat", "rumia_hairband", "cirno_hairband")) {
            Item item = BuiltInRegistries.ITEM.get(Bridge.id(Bridge.YH, hat));
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            item.use(level, player, InteractionHand.MAIN_HAND);
            boolean worn = player.getItemBySlot(EquipmentSlot.HEAD).is(item);
            boolean sound = item instanceof net.minecraft.world.item.Equipable eq && eq.getEquipSound() != null;
            check("hat equip " + hat, worn && sound, "worn=" + worn + " sound=" + sound);
        }
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        // 3. item redirects
        for (String p : List.of("koishi_hat", "suwako_hat", "fairy_ice_crystal", "frozen_frog_temperate")) {
            Item gl = BuiltInRegistries.ITEM.get(Bridge.id(Bridge.GL, p));
            Item yh = BuiltInRegistries.ITEM.get(Bridge.id(Bridge.YH, p));
            check("redirect " + p, Bridge.toYH(gl) == yh, "-> " + BuiltInRegistries.ITEM.getKey(Bridge.toYH(gl)));
        }
        for (String t : List.of("fairy_ice_crystal", "frozen_frog")) {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Bridge.id(Bridge.GL, t)).orElse(null);
            Entity e = type == null ? null : type.create(level);
            if (e instanceof ThrowableItemProjectile proj) {
                String shown = BuiltInRegistries.ITEM.getKey(proj.getItem().getItem()).toString();
                check("thrown " + t + " shows YH item", shown.startsWith(Bridge.YH + ":"), shown);
            } else check("thrown " + t, false, "no projectile type");
        }
        check("hidden items resolved", Bridge.hiddenItems().size() >= 30, "count=" + Bridge.hiddenItems().size());

        // 4. datapack: recipes, structure biome tags, Gensouworks set
        check("legacy book pile recipe disabled", server.getRecipeManager().byKey(Bridge.id(Bridge.GL, "book_pile")).isEmpty(), "");
        var biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
        var meadow = biomes.getHolderOrThrow(Biomes.MEADOW);
        var forest = biomes.getHolderOrThrow(Biomes.FOREST);
        check("YH shrine disabled", !meadow.is(tag(Bridge.YH, "has_structure/hakurei_shrine")), "");
        check("Legacy shrine in meadow", meadow.is(tag(Bridge.GL, "has_structure/hakurei_shrine")), "");
        check("Legacy Marisa house in forest", forest.is(tag(Bridge.GL, "has_structure/marisa_house")), "");
        check("Legacy shop in forest", forest.is(tag(Bridge.GL, "has_structure/morichika_shop")), "");
        var sets = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        StructureSet common = sets.get(Bridge.id("gensouworks", "touhou_common_structures"));
        String members = common == null ? "missing" : common.structures().stream()
                .map(e -> e.structure().unwrapKey().map(k -> k.location().getPath()).orElse("?")).toList().toString();
        check("Gensouworks Kirisame/Kourindou removed", common != null && !members.contains("kirisame") && !members.contains("kourindou"), members);
        // 5. round 2: stranger neutrality, Legacy hat -> YH effect, YH straw hat recipe, Arcadian graze, regions
        check("YH straw hat recipe disabled", server.getRecipeManager().byKey(Bridge.id(Bridge.YH, "straw_hat")).isEmpty(), "");
        try {
            var rumiaType = BuiltInRegistries.ENTITY_TYPE.get(Bridge.id(Bridge.GL, "rumia"));
            Entity rumia = rumiaType.create(level);
            rumia.moveTo(base.getX(), base.getY(), base.getZ());
            Object cm = findField(rumia.getClass(), "combatManager").get(rumia);
            Object kind = cm.getClass().getMethod("targetKind", net.minecraft.world.entity.LivingEntity.class).invoke(cm, player);
            check("stranger (rep 0) is not an enemy", String.valueOf(kind).contains("initiateAttack=false"), "targetKind=" + kind);
        } catch (Exception e) { check("stranger (rep 0) is not an enemy", false, e.toString()); }
        for (String[] hatEff : new String[][]{{"koishi_hat", "UNCONSCIOUS"}, {"suwako_hat", "NATIVE"}}) {
            try {
                Item glHat = BuiltInRegistries.ITEM.get(Bridge.id(Bridge.GL, hatEff[0]));
                ItemStack st = new ItemStack(glHat);
                player.removeAllEffects();
                player.setItemSlot(EquipmentSlot.HEAD, st);
                var tick = findMethod(glHat.getClass(), "tick", ItemStack.class, net.minecraft.world.level.Level.class, net.minecraft.world.entity.player.Player.class);
                tick.invoke(glHat, st, level, player);
                var yh = (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) (Object) Bridge.yhEffect(hatEff[1]);
                String got = player.getActiveEffects().stream().map(e -> e.getEffect().unwrapKey().map(k -> k.location().toString()).orElse("?")).toList().toString();
                check("Legacy " + hatEff[0] + " grants YH effect", player.hasEffect(yh) && !got.contains(Bridge.GL + ":"), got);
            } catch (Exception e) { check("Legacy " + hatEff[0] + " grants YH effect", false, e.toString()); }
        }
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.removeAllEffects();
        var amulet = BuiltInRegistries.ENTITY_TYPE.getOptional(Bridge.id("arcadiandream", "amulet_bullet")).map(t -> t.create(level)).orElse(null);
        check("Arcadian Dream bullets grazeable", amulet instanceof dev.xkmc.fastprojectileapi.entity.GrazingEntity, String.valueOf(amulet));
        // 6. chest loot in Legacy's buildings (templates from the bridge datapack)
        var templates = server.getStructureManager();
        for (String[] s : new String[][]{{"marisa_house", "24"}, {"morichika_shop", "11"}, {"hakurei_shrine/root", "7"}, {"hakurei_shrine/warehouse", "6"}}) {
            var tpl = templates.get(Bridge.id(Bridge.GL, s[0])).orElse(null);
            java.util.Set<String> tables = new java.util.TreeSet<>();
            int n = 0;
            if (tpl != null) {
                var tag = tpl.save(new net.minecraft.nbt.CompoundTag());
                for (var b : tag.getList("blocks", 10)) {
                    var nbt = ((net.minecraft.nbt.CompoundTag) b).getCompound("nbt");
                    if (nbt.contains("LootTable")) { n++; tables.add(nbt.getString("LootTable")); }
                }
            }
            boolean loaded = !tables.isEmpty() && tables.stream().allMatch(t -> server.reloadableRegistries()
                    .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, net.minecraft.resources.ResourceLocation.parse(t)))
                    != net.minecraft.world.level.storage.loot.LootTable.EMPTY);
            check("loot in Legacy " + s[0], n == Integer.parseInt(s[1]) && loaded, "containers=" + n + " tables=" + tables + " allLoaded=" + loaded);
        }
        var lootParams = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(base))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        var rareItems = List.of(BuiltInRegistries.ITEM.get(Bridge.id("touhou_little_maid", "broom")), net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE,
                net.minecraft.world.item.Items.TOTEM_OF_UNDYING, net.minecraft.world.item.Items.CONDUIT);
        for (String t : List.of("shop", "junk", "kitchen")) {
            var table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    Bridge.id(GensokyoCompat.MOD_ID, "chests/marisa_house/" + t)));
            int rares = 0, rolls = 3000;
            boolean banned = false;
            for (int r = 0; r < rolls; r++) {
                var items = table.getRandomItems(lootParams);
                if (items.stream().anyMatch(st -> rareItems.stream().anyMatch(st::is))) rares++;
                banned |= items.stream().anyMatch(st -> st.is(net.minecraft.world.item.Items.NETHERITE_INGOT) || st.is(net.minecraft.world.item.Items.HEAVY_CORE) || st.is(net.minecraft.world.item.Items.NETHER_STAR));
            }
            double rate = rares / (double) rolls;
            check("Marisa " + t + " rare chance ~37.5%, never netherite/heavy core/nether star", table != net.minecraft.world.level.storage.loot.LootTable.EMPTY
                    && !banned && rate > 0.33 && rate < 0.42, "rate=" + rate + " banned=" + banned);
        }
        LOG.info("[gensokyocompat] SELFTEST sync part done, swap results in 5 ticks");
    }

    private static java.lang.reflect.Field findField(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { var f = k.getDeclaredField(name); f.setAccessible(true); return f; } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static java.lang.reflect.Method findMethod(Class<?> c, String name, Class<?>... args) throws NoSuchMethodException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { var m = k.getDeclaredMethod(name, args); m.setAccessible(true); return m; } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(name);
    }

    private static TagKey<Biome> tag(String ns, String path) {
        return TagKey.create(Registries.BIOME, Bridge.id(ns, path));
    }

    private static void finish() {
        LOG.info("[gensokyocompat] SELFTEST DONE pass={} fail={} swapped={}", pass, fail, CharacterSwap.swapped);
    }
}
