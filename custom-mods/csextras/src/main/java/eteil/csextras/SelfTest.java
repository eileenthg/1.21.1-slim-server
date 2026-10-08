package eteil.csextras;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.momosoftworks.coldsweat.api.temperature.modifier.ShadeTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.WaterTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.core.init.ModEffects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;

/** /csextras selftest — exercises every feature on a fake player so a headless server can verify the build. */
final class SelfTest {
    private static int pass, fail;
    private static Consumer<String> out;

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("csextras").requires(s -> s.hasPermission(2))
                .then(Commands.literal("selftest").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    out = msg -> {
                        CSExtras.LOG.info("[csextras-test] {}", msg);
                        src.sendSystemMessage(Component.literal(msg));
                    };
                    try {
                        run(src.getServer().overworld());
                    } catch (Throwable t) {
                        CSExtras.LOG.error("[csextras-test] crashed", t);
                        fail++;
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

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static ItemStack withFlag(String id, String key, boolean value) {
        ItemStack stack = new ItemStack(item(id));
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(key, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static void strip(FakePlayer p) {
        for (EquipmentSlot slot : EquipmentSlot.values()) p.setItemSlot(slot, ItemStack.EMPTY);
    }

    private static int tagSize(TagKey<?> key) {
        if (key.registry().equals(Registries.ITEM)) {
            @SuppressWarnings("unchecked") TagKey<Item> k = (TagKey<Item>) key;
            return BuiltInRegistries.ITEM.getTag(k).map(h -> h.size()).orElse(0);
        }
        @SuppressWarnings("unchecked") TagKey<Block> k = (TagKey<Block>) key;
        return BuiltInRegistries.BLOCK.getTag(k).map(h -> h.size()).orElse(0);
    }

    private static void run(ServerLevel level) throws Exception {
        pass = fail = 0;

        // --- data ---
        check("tag raincoats", tagSize(Gear.RAINCOATS) == 16, tagSize(Gear.RAINCOATS));
        check("tag rain_hats", tagSize(Gear.RAIN_HATS) == 16, tagSize(Gear.RAIN_HATS));
        check("tag umbrellas", tagSize(Gear.UMBRELLAS) == 18, tagSize(Gear.UMBRELLAS));
        check("tag sun_hats", tagSize(Gear.SUN_HATS) >= 5, tagSize(Gear.SUN_HATS));
        check("tag pitchforks", tagSize(Pitchfork.PITCHFORKS) == 1, tagSize(Pitchfork.PITCHFORKS));
        check("tag pitchfork_sweepable", tagSize(Pitchfork.SWEEPABLE) > 20, tagSize(Pitchfork.SWEEPABLE));
        check("tag pitchfork_mineable", tagSize(Pitchfork.MINEABLE) > 5, tagSize(Pitchfork.MINEABLE));
        var recipes = level.getServer().getRecipeManager();
        check("plastic shovel recipe disabled", recipes.byKey(ResourceLocation.parse("convenientdecor:red_plastic_shovel")).isEmpty(), "red");
        check("umbrella recipe kept", recipes.byKey(ResourceLocation.parse("convenientdecor:red_umbrella")).isPresent(), "red");
        TagKey<Item> hidden = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:hidden_from_recipe_viewers"));
        check("plastic shovels hidden in JEI", new ItemStack(item("convenientdecor:red_plastic_shovel")).is(hidden), "c:hidden_from_recipe_viewers");

        // --- fake player on the surface at spawn ---
        BlockPos spawn = level.getSharedSpawnPos();
        var found = level.findClosestBiome3d(b -> b.is(net.minecraft.world.level.biome.Biomes.PLAINS)
                || b.is(net.minecraft.world.level.biome.Biomes.FOREST), spawn, 6400, 32, 64);
        BlockPos base = found != null ? found.getFirst() : spawn;
        level.getChunk(base);
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, base);
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("6c8f4f10-4c5e-4d7a-9a4e-0c5e5e5e5e01"), "[csextras]"));
        p.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("?");
        out.accept("at " + pos + " biome " + biome);

        Class<?> flags = Class.forName("dev.overgrown.apoli.condition.builtin.entity.SimpleFlagConditions");
        Method inRain = flags.getDeclaredMethod("lambda$inRain$8", Entity.class);
        Method sun = flags.getDeclaredMethod("lambda$exposedToSun$6", Entity.class);
        inRain.setAccessible(true);
        sun.setAccessible(true);

        ItemStack coatDown = withFlag("convenientdecor:red_raincoat", "hooded", false);
        ItemStack coatUp = withFlag("convenientdecor:red_raincoat", "hooded", true);
        ItemStack hat = new ItemStack(item("convenientdecor:red_rain_hat"));
        ItemStack umbrella = new ItemStack(item("convenientdecor:red_umbrella"));
        ItemStack folded = withFlag("convenientdecor:red_umbrella", "folded", true);
        ItemStack artifactsUmbrella = new ItemStack(item("artifacts:umbrella"));

        // --- rain ---
        level.setDayTime(6000);
        level.setWeatherParameters(0, 6000, true, false);
        level.setRainLevel(1f);
        level.setThunderLevel(0f);
        boolean raining = level.isRainingAt(p.blockPosition()) || level.isRainingAt(p.blockPosition().above());
        out.accept("raining at player: " + raining);
        if (raining) {
            strip(p);
            check("in_rain bare", (boolean) inRain.invoke(null, p), "expect true");
            p.setItemSlot(EquipmentSlot.CHEST, coatDown);
            check("in_rain hood down", (boolean) inRain.invoke(null, p), "expect true");
            p.setItemSlot(EquipmentSlot.CHEST, coatUp);
            check("in_rain hood up", !(boolean) inRain.invoke(null, p), "expect false");
            p.setItemSlot(EquipmentSlot.CHEST, coatDown);
            p.setItemSlot(EquipmentSlot.HEAD, hat);
            check("in_rain coat + rain hat", !(boolean) inRain.invoke(null, p), "expect false");
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            check("in_rain rain hat only", (boolean) inRain.invoke(null, p), "expect true");
            strip(p);
            p.setItemInHand(InteractionHand.MAIN_HAND, umbrella.copy());
            check("in_rain umbrella only", (boolean) inRain.invoke(null, p), "expect true (config default)");
            check("rain hook applied", OriginsCompat.rainHookSeen, "mixin ran");

            // Cold Sweat rain soaking: 60 updates of the water modifier with and without gear
            strip(p);
            double bare = soak(p);
            p.setItemSlot(EquipmentSlot.CHEST, coatDown);
            double coat = soak(p);
            p.setItemSlot(EquipmentSlot.CHEST, coatUp);
            double full = soak(p);
            strip(p);
            p.setItemInHand(InteractionHand.MAIN_HAND, umbrella.copy());
            double umb = soak(p);
            out.accept(String.format("soak after 60 updates: bare %.3f coat %.3f full %.3f umbrella %.3f", bare, coat, full, umb));
            check("raincoat soaks less", bare < 0 && coat > bare && full > coat, "");
            check("umbrella soaks like hood-down coat", Math.abs(umb - coat) < 1e-6, "");

            // thunder: condition stays, damage halves
            level.setWeatherParameters(0, 6000, true, true);
            level.setThunderLevel(1f);
            strip(p);
            p.setItemSlot(EquipmentSlot.CHEST, coatUp);
            check("in_rain thunder, hood up", (boolean) inRain.invoke(null, p), "expect true");
            DamageSource water = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(OriginsCompat.HURT_BY_WATER));
            float mult = OriginsCompat.thunderMultiplier(p, water);
            check("thunder damage halved", mult == 0.5f, mult);
            strip(p);
            check("thunder damage bare", OriginsCompat.thunderMultiplier(p, water) == 1f, "");
        }

        // --- sun & shade ---
        level.setWeatherParameters(6000, 0, false, false);
        level.setRainLevel(0f);
        level.setThunderLevel(0f);
        level.setDayTime(6000);
        strip(p);
        boolean sunBare = (boolean) sun.invoke(null, p);
        out.accept("exposed_to_sun bare: " + sunBare);
        if (sunBare) {
            p.setItemInHand(InteractionHand.MAIN_HAND, umbrella.copy());
            check("sun blocked by umbrella", !(boolean) sun.invoke(null, p), "");
            p.setItemInHand(InteractionHand.MAIN_HAND, folded.copy());
            check("folded umbrella doesn't block sun", (boolean) sun.invoke(null, p), "");
            p.setItemInHand(InteractionHand.MAIN_HAND, artifactsUmbrella.copy());
            check("Artifacts umbrella blocks sun", !(boolean) sun.invoke(null, p), "");
            check("sun hook applied", OriginsCompat.sunHookSeen, "mixin ran");
        }
        strip(p);
        double shadeBare = new ShadeTempModifier().update(1.0, p, Temperature.Trait.WORLD);
        p.setItemInHand(InteractionHand.MAIN_HAND, umbrella.copy());
        double shadeUmb = new ShadeTempModifier().update(1.0, p, Temperature.Trait.WORLD);
        strip(p);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item("youkaishomecoming:straw_hat")));
        double shadeHat = new ShadeTempModifier().update(1.0, p, Temperature.Trait.WORLD);
        strip(p);
        out.accept(String.format("shade world temp: bare %.4f umbrella %.4f straw hat %.4f", shadeBare, shadeUmb, shadeHat));
        check("umbrella shades", shadeUmb < shadeBare, "");
        check("sun hat shades partly", shadeHat < shadeBare && shadeHat > shadeUmb, "");

        // --- sweat ---
        Temperature.set(p, Temperature.Trait.BASE, 0);
        Temperature.set(p, Temperature.Trait.CORE, 80);
        double body = Temperature.get(p, Temperature.Trait.BODY);
        SweatTempModifier sweat = new SweatTempModifier().tickRate(10);
        double sweaty = sweat.update(1.0, p, Temperature.Trait.WORLD);
        out.accept(String.format("body %.1f -> sweat world %.4f (thirst %s)", body, sweaty, ThirstCompat.LOADED ? ThirstCompat.thirst(p) : "n/a"));
        check("sweat cools when hot", sweaty < 1.0, sweaty);
        Temperature.set(p, Temperature.Trait.CORE, 0);
        check("no sweat at 0 body", new SweatTempModifier().tickRate(10).update(1.0, p, Temperature.Trait.WORLD) == 1.0, "");

        // --- shiver ---
        Temperature.set(p, Temperature.Trait.CORE, -80);
        p.getFoodData().setFoodLevel(20);
        double before = Temperature.get(p, Temperature.Trait.CORE);
        double warmth = BodyHandlers.shiver(p);
        double after = Temperature.get(p, Temperature.Trait.CORE);
        check("shiver warms core", warmth > 0 && after > before, String.format("%.2f -> %.2f", before, after));
        p.getFoodData().setFoodLevel(4);
        check("no shiver when starving", BodyHandlers.shiver(p) == 0, "");
        Temperature.set(p, Temperature.Trait.CORE, 0);

        // --- campfire ---
        BlockPos fire = pos.offset(3, 0, 0);
        BlockState oldFire = level.getBlockState(fire);
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
        ItemStack logs = new ItemStack(Items.OAK_LOG, 4);
        check("campfire stokable", Campfires.canStoke(level.getBlockState(fire), logs), "");
        // fake players aren't in the level's entity list, so warm a real armour stand next to the fire
        var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, fire.getX() + 1.5, fire.getY(), fire.getZ() + 0.5);
        level.addFreshEntity(stand);
        int warmed = Campfires.stoke(level, fire, p, logs);
        check("campfire gives warmth", stand.hasEffect(ModEffects.WARMTH) && warmed >= 1 && logs.getCount() == 3, "warmed " + warmed);
        stand.discard();
        level.setBlockAndUpdate(fire, oldFire);

        // --- warming foods ---
        p.removeEffect(ModEffects.WARMTH);
        ItemStack borscht = new ItemStack(item("extradelight:borscht"));
        check("borscht is a warming food", borscht.is(Foods.WARMING), "");
        net.neoforged.neoforge.event.EventHooks.onItemUseFinish(p, borscht.copy(), 0, ItemStack.EMPTY);
        {
            var w = p.getEffect(ModEffects.WARMTH);
            check("eating borscht gives warmth", w != null && w.getDuration() == Config.WARMING_FOOD_DURATION.get(),
                    w == null ? "none" : w.getDuration() + " ticks");
        }
        p.removeEffect(ModEffects.WARMTH);
        net.neoforged.neoforge.event.EventHooks.onItemUseFinish(p, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY);
        check("bread gives no warmth", !p.hasEffect(ModEffects.WARMTH), "");

        // --- pitchfork ---
        Block pile = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("immersive_weathering:oak_leaf_pile"));
        BlockPos centre = pos.offset(-4, 0, -4);
        BlockState[] saved = new BlockState[25];
        int i = 0;
        for (BlockPos q : BlockPos.betweenClosed(centre.offset(-2, 0, -2), centre.offset(2, 0, 2))) {
            saved[i++] = level.getBlockState(q);
            level.setBlockAndUpdate(q, pile.defaultBlockState());
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("convenientdecor:pitchfork")));
        float fast = p.getDigSpeed(Blocks.HAY_BLOCK.defaultBlockState(), null);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        float slow = p.getDigSpeed(Blocks.HAY_BLOCK.defaultBlockState(), null);
        check("pitchfork digs hay faster", fast > slow * 5, fast + " vs " + slow);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("convenientdecor:pitchfork")));
        p.gameMode.destroyBlock(centre);
        int left = 0;
        for (BlockPos q : BlockPos.betweenClosed(centre.offset(-2, 0, -2), centre.offset(2, 0, 2))) {
            if (level.getBlockState(q).is(pile)) left++;
        }
        check("pitchfork sweeps 5x5 leaf piles", left == 0, left + " left");
        i = 0;
        for (BlockPos q : BlockPos.betweenClosed(centre.offset(-2, 0, -2), centre.offset(2, 0, 2))) {
            level.setBlockAndUpdate(q, saved[i++]);
        }
        strip(p);
        sandstorms(level, p);
        level.setWeatherParameters(6000, 0, false, false);
        level.setRainLevel(0f);
        level.setThunderLevel(0f);
    }

    private static BlockPos surfaceIn(ServerLevel level, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome) {
        var found = level.findClosestBiome3d(b -> b.is(biome), level.getSharedSpawnPos(), 12800, 32, 64);
        if (found == null) return null;
        level.getChunk(found.getFirst());
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, found.getFirst());
    }

    private static void sandstorms(ServerLevel level, FakePlayer p) {
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        int sandTag = biomes.getTag(eteil.csextras.sand.Sandstorms.SAND_BIOMES).map(h -> h.size()).orElse(0);
        int redTag = biomes.getTag(eteil.csextras.sand.Sandstorms.RED_BIOMES).map(h -> h.size()).orElse(0);
        check("sandstorm biome tags", sandTag >= 1 && redTag >= 1, "sand " + sandTag + ", red " + redTag);
        biomes.getTag(eteil.csextras.sand.Sandstorms.SAND_BIOMES).ifPresent(t -> out.accept("  sand biomes: " + t.stream().map(h -> h.unwrapKey().get().location().toString()).toList()));
        biomes.getTag(eteil.csextras.sand.Sandstorms.RED_BIOMES).ifPresent(t -> out.accept("  red biomes: " + t.stream().map(h -> h.unwrapKey().get().location().toString()).toList()));

        BlockPos top = surfaceIn(level, net.minecraft.world.level.biome.Biomes.DESERT);
        if (top == null) { check("found a desert", false, ""); return; }
        out.accept("desert surface " + top + " on " + level.getBlockState(top.below()).getBlock());
        level.setWeatherParameters(0, 6000, true, false);
        level.setRainLevel(1f);
        level.setThunderLevel(0f);
        var S = eteil.csextras.sand.Sandstorms.class;
        check("desert has sandstorm", eteil.csextras.sand.Sandstorms.exposedTypeAt(level, top) == eteil.csextras.sand.Sandstorms.Type.SAND, "");

        // slowed mobs
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        zombie.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
        double base = zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        boolean slowed = eteil.csextras.sand.Sandstorms.updateSlow(zombie);
        double slow = zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        check("zombie slowed in sandstorm", slowed && slow < base, String.format("%.3f -> %.3f", base, slow));
        var husk = net.minecraft.world.entity.EntityType.HUSK.create(level);
        husk.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
        check("husk ignores sandstorm", !eteil.csextras.sand.Sandstorms.updateSlow(husk), "");

        // breezes
        var spawns = new net.neoforged.neoforge.event.level.LevelEvent.PotentialSpawns(level, net.minecraft.world.entity.MobCategory.MONSTER, top,
                net.minecraft.util.random.WeightedRandomList.create());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(spawns);
        check("breeze in spawn list", spawns.getSpawnerDataList().stream().anyMatch(d -> d.type == net.minecraft.world.entity.EntityType.BREEZE), spawns.getSpawnerDataList().size() + " entries");
        var breeze = net.minecraft.world.entity.EntityType.BREEZE.create(level);
        breeze.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
        var check = new net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck(breeze, level, net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(check);
        check("breeze may spawn in daylight sandstorm", check.getResult() == net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.SUCCEED, check.getResult());

        // sand piles through the real weather tick (proves the ServerLevel mixin)
        BlockPos column = top.offset(2, 0, 0);
        level.getChunk(column);
        column = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        BlockState savedCol = level.getBlockState(column);
        for (int n = 0; n < 60; n++) level.tickPrecipitation(column);
        BlockState pileState = level.getBlockState(column);
        check("sand piles accumulate (capped at 2)", pileState.is(eteil.csextras.sand.SandBlocks.SAND_PILE.get())
                && pileState.getValue(eteil.csextras.sand.SandPileBlock.LAYERS) == 2, pileState);
        level.setBlockAndUpdate(column, savedCol);

        // cauldron: empty -> sand cauldron -> full -> scoop
        BlockPos cpos = top;
        level.getChunk(cpos);
        cpos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, cpos);
        BlockState savedC = level.getBlockState(cpos);
        level.setBlockAndUpdate(cpos, Blocks.CAULDRON.defaultBlockState());
        eteil.csextras.sand.Sandstorms.accumulate(level, cpos.above(), eteil.csextras.sand.Sandstorms.Type.SAND);
        BlockState c = level.getBlockState(cpos);
        check("exposed cauldron catches sand", c.is(eteil.csextras.sand.SandBlocks.SAND_CAULDRON.get()), c);
        out.accept("  cauldron at " + cpos + " biome " + level.getBiome(cpos).unwrapKey().get().location() + " exposed "
                + eteil.csextras.sand.Sandstorms.exposedTypeAt(level, cpos.above()) + " typeAt " + eteil.csextras.sand.Sandstorms.typeAt(level, cpos.above()));
        if (c.getBlock() instanceof eteil.csextras.sand.SandCauldronBlock sc) {
            sc.fill(c, level, cpos);
            sc.fill(level.getBlockState(cpos), level, cpos);
            c = level.getBlockState(cpos);
            check("sand cauldron fills to 3", c.getValue(eteil.csextras.sand.SandCauldronBlock.LEVEL) == 3, c);
            p.getInventory().clearContent();
            c.useWithoutItem(level, p, new net.minecraft.world.phys.BlockHitResult(cpos.getCenter(), net.minecraft.core.Direction.UP, cpos, false));
            check("scooping gives sand, leaves empty cauldron", p.getInventory().countItem(Items.SAND) == 1 && level.getBlockState(cpos).is(Blocks.CAULDRON),
                    p.getInventory().countItem(Items.SAND) + " sand, " + level.getBlockState(cpos).getBlock());
        }
        level.setBlockAndUpdate(cpos, savedC);

        // red sand in badlands
        BlockPos red = surfaceIn(level, net.minecraft.world.level.biome.Biomes.BADLANDS);
        if (red != null) check("badlands has red sandstorm", eteil.csextras.sand.Sandstorms.exposedTypeAt(level, red) == eteil.csextras.sand.Sandstorms.Type.RED, red);

        // storm over: modifiers come off
        level.setWeatherParameters(6000, 0, false, false);
        level.setRainLevel(0f);
        eteil.csextras.sand.Sandstorms.updateSlow(zombie);
        check("slow removed after storm", zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == base, "");
    }

    /** Runs a fresh water modifier 60 times at the player's position and returns its wetness. */
    private static double soak(FakePlayer p) {
        WaterTempModifier water = new WaterTempModifier().tickRate(5);
        for (int n = 0; n < 60; n++) water.update(0.0, p, Temperature.Trait.WORLD);
        return water.getTemperature();
    }

    private SelfTest() {
    }
}
