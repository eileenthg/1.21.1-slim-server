package eteil.spawnonboarding.compat;

import eteil.spawnonboarding.SpawnOnboarding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Eternal Tales 8.2.4 is reached reflectively (its jar name breaks classpath tooling, and only two
 * members are needed). Template choice mirrors EternalTownGUIGenerateNowProcedure exactly.
 */
public final class EternalTalesCompat {
    private static final ResourceLocation SPAWNER = ResourceLocation.fromNamespaceAndPath("eternal_tales", "eternal_town_spawner");

    private EternalTalesCompat() {}

    /** Same checks, same order as the Eternal Tales procedure. */
    public static String templateFor(ServerLevel level, BlockPos pos) {
        Holder<Biome> b = level.getBiome(pos);
        if (tag(b, "minecraft:is_mangrove_swamp") || tag(b, "forge:is_mangrove_swamp") || b.is(ResourceLocation.parse("mangrove_swamp")))
            return "eternal_town_mangrove_swamp";
        if (tag(b, "minecraft:is_cherry_grove") || tag(b, "forge:is_cherry_grove") || b.is(ResourceLocation.parse("cherry_grove")))
            return "eternal_town_cherry_grove";
        if (tag(b, "minecraft:is_desert") || tag(b, "forget:is_desert") || b.is(ResourceLocation.parse("desert")))
            return "eternal_town_desert";
        if (tag(b, "minecraft:is_badlands") || tag(b, "forge:is_badlands") || tag(b, "eternal_tales:badlands_biomes"))
            return "eternal_town_badlands";
        if (tag(b, "eternal_tales:mucunfectio_biomes"))
            return "eternal_town_mucunfectio";
        if (tag(b, "minecraft:is_cold/overworld") || tag(b, "forge:is_cold/overworld") || tag(b, "eternal_tales:snowy_biomes")
                || b.value().getBaseTemperature() * 100f <= 0.14)
            return "eternal_town_cold";
        if (tag(b, "minecraft:is_jungle") || tag(b, "forge:is_jungle") || tag(b, "eternal_tales:jungle_biomes"))
            return "eternal_town_jungle";
        return "eternal_town";
    }

    private static boolean tag(Holder<Biome> biome, String id) {
        return biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse(id)));
    }

    /**
     * Eternal Tales' world variable {@code eternaltowngen}. It defaults to 2 ("not handled"); the login hook
     * hands out an Eternal Town Spawner while it is 2 and sets it to 1, and placing a town also sets 1.
     * Read it before the first login to know whether a world still needs its town.
     */
    public static double townFlag(LevelAccessor level) {
        try {
            Object vars = mapVariables(level);
            return vars.getClass().getField("eternaltowngen").getDouble(vars);
        } catch (ReflectiveOperationException | RuntimeException e) {
            SpawnOnboarding.LOG.error("[SpawnOnboarding] could not read Eternal Tales eternaltowngen", e);
            return 1;
        }
    }

    public static final double FLAG_NOT_HANDLED = 2.0;

    /** Sets the flag to 1 and syncs it, exactly as Eternal Tales does after placing a town. */
    public static void markTownPlaced(LevelAccessor level) {
        try {
            Object vars = mapVariables(level);
            Field f = vars.getClass().getField("eternaltowngen");
            f.setDouble(vars, 1.0);
            Method sync = vars.getClass().getMethod("syncData", LevelAccessor.class);
            sync.invoke(vars, level);
        } catch (ReflectiveOperationException | RuntimeException e) {
            SpawnOnboarding.LOG.error("[SpawnOnboarding] could not set Eternal Tales eternaltowngen", e);
        }
    }

    public static void removeSpawners(ServerPlayer player) {
        Item spawner = BuiltInRegistries.ITEM.get(SPAWNER);
        int removed = player.getInventory().clearOrCountMatchingItems(s -> s.is(spawner), -1, player.inventoryMenu.getCraftSlots());
        if (removed > 0) {
            SpawnOnboarding.LOG.info("[SpawnOnboarding] removed {} Eternal Town Spawner(s) from {}", removed, player.getName().getString());
        }
    }

    private static Object mapVariables(LevelAccessor level) throws ReflectiveOperationException {
        Class<?> c = Class.forName("net.eternal_tales.network.EternalTalesModVariables$MapVariables");
        return c.getMethod("get", LevelAccessor.class).invoke(null, level);
    }
}
