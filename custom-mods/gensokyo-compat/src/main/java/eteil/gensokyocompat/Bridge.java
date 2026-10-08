package eteil.gensokyocompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Map;

/** Which Youkai's Homecoming and Gensokyo Legacy entries stand in for each other. */
public final class Bridge {
    public static final String YH = "youkaishomecoming";
    public static final String GL = "gensokyolegacy";

    /** YH characters that Legacy also has: every YH copy is replaced by the Legacy one where it stands. */
    public static final List<String> CHARACTERS = List.of("cirno", "hakurei_reimu", "kirisame_marisa", "kochiya_sanae",
            "komeiji_koishi", "luna_child", "mystia_lorelei", "rumia", "star_sapphire", "sunny_milk", "yukari_yakumo");

    /** Legacy items dropped in favour of the YH item with the same name. */
    public static final List<String> GL_DROPPED = List.of("fairy_ice_crystal", "frozen_frog_cold", "frozen_frog_warm",
            "frozen_frog_temperate", "spell_reimu", "spell_marisa", "spell_sanae", "spell_yukari_butterfly", "spell_yukari_laser",
            "spell_mystia", "straw_hat", "suwako_hat", "koishi_hat", "rumia_hairband", "cirno_hairband", "cirno_wings",
            "donation_box", "book_pile", "debug_glasses", "debug_wand", "structure_wand");

    /** YH items dropped in favour of Legacy's: its character beds, and spawn eggs of the characters Legacy now owns. */
    public static final List<String> YH_DROPPED = List.of("cirno_bed", "reimu_bed", "rumia_bed",
            "cirno_spawn_egg", "hakurei_reimu_spawn_egg", "kirisame_marisa_spawn_egg", "kochiya_sanae_spawn_egg",
            "komeiji_koishi_spawn_egg", "luna_child_spawn_egg", "mystia_lorelei_spawn_egg", "rumia_spawn_egg",
            "star_sapphire_spawn_egg", "sunny_milk_spawn_egg", "yukari_yakumo_spawn_egg");

    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> MAGICAL_FOREST =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, id(GL, "magical_forest"));

    private static final Map<String, Object> YH_EFFECTS = new java.util.concurrent.ConcurrentHashMap<>();

    /** YH's own effect holder (its registrate class is also called GLEffects), looked up once by field name. */
    @SuppressWarnings("unchecked")
    public static dev.xkmc.l2core.init.reg.registrate.LegacyHolder<?> yhEffect(String field) {
        return (dev.xkmc.l2core.init.reg.registrate.LegacyHolder<?>) YH_EFFECTS.computeIfAbsent(field, f -> {
            try {
                return Class.forName("dev.xkmc.youkaishomecoming.init.registrate.GLEffects").getField(f).get(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("YH effect " + f + " not found", e);
            }
        });
    }

    public static ResourceLocation id(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    /** The YH stand-in for a dropped Legacy item, or the item itself. */
    public static Item toYH(ItemLike like) {
        Item item = like.asItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (!GL.equals(key.getNamespace()) || !GL_DROPPED.contains(key.getPath())) return item;
        Item yh = BuiltInRegistries.ITEM.get(id(YH, key.getPath()));
        return yh == Items.AIR ? item : yh;
    }

    public static EntityType<?> legacyFor(EntityType<?> type) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (!YH.equals(key.getNamespace()) || !CHARACTERS.contains(key.getPath())) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id(GL, key.getPath())).orElse(null);
    }

    public static List<Item> hiddenItems() {
        java.util.ArrayList<Item> out = new java.util.ArrayList<>();
        for (String p : GL_DROPPED) BuiltInRegistries.ITEM.getOptional(id(GL, p)).ifPresent(out::add);
        for (String p : YH_DROPPED) BuiltInRegistries.ITEM.getOptional(id(YH, p)).ifPresent(out::add);
        return out;
    }

    private Bridge() {}
}
