package dev.eteil.butcheryam.init;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items REGISTRY =
            DeferredRegister.createItems(ButcheryAlexsMobs.MODID);

    /** {@code FoodProperties.Builder#meat()} was removed in 1.21; nutrition/saturation are unchanged. */
    private static Item.Properties food(int nutrition, float saturation) {
        return new Item.Properties().food(new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation)
                .build());
    }

    public static final DeferredItem<Item> RAW_BIRD_MEAT =
            REGISTRY.registerSimpleItem("raw_bird_meat", food(4, 0.3F));
    public static final DeferredItem<Item> COOKED_BIRD_MEAT =
            REGISTRY.registerSimpleItem("cooked_bird_meat", food(9, 0.3F));

    public static final DeferredItem<Item> CAIMAN_LEATHER =
            REGISTRY.registerSimpleItem("caiman_leather", new Item.Properties());
    public static final DeferredItem<Item> RAW_CAIMAN_STEAK =
            REGISTRY.registerSimpleItem("raw_caiman_steak", food(7, 0.3F));
    public static final DeferredItem<Item> COOKED_CAIMAN_STEAK =
            REGISTRY.registerSimpleItem("cooked_caiman_steak", food(11, 0.3F));

    public static final DeferredItem<Item> CAPUCHIN_FUR =
            REGISTRY.registerSimpleItem("capuchin_fur", new Item.Properties());
    public static final DeferredItem<Item> RAW_CAPUCHIN_MEAT =
            REGISTRY.registerSimpleItem("raw_capuchin_meat", food(5, 0.3F));
    public static final DeferredItem<Item> COOKED_CAPUCHIN_MEAT =
            REGISTRY.registerSimpleItem("cooked_capuchin_meat", food(8, 0.3F));

    public static final DeferredItem<Item> RAW_FISH_FILLET =
            REGISTRY.registerSimpleItem("raw_fish_fillet", food(5, 0.3F));
    public static final DeferredItem<Item> COOKED_FISH_FILLET =
            REGISTRY.registerSimpleItem("cooked_fish_fillet", food(9, 0.3F));

    static {
        // Block items, registered under the same names as their blocks.
        for (var block : ModBlocks.ALL) {
            REGISTRY.registerSimpleBlockItem(block);
        }
    }

    private ModItems() {}
}
