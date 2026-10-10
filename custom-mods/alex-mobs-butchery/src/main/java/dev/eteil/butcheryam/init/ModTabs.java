package dev.eteil.butcheryam.init;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {

    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ButcheryAlexsMobs.MODID);

    public static final Supplier<CreativeModeTab> BLOCKS = REGISTRY.register("butcheryalexsmobs",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.butchery_alexs_mobs.butcheryalexsmobs"))
                    .icon(() -> new ItemStack(ModBlocks.BALD_EAGLE_CARCASS.get()))
                    .displayItems((params, output) -> {
                        for (var block : ModBlocks.ALL) {
                            output.accept(block.get().asItem());
                        }
                    })
                    .build());

    public static final Supplier<CreativeModeTab> FOOD = REGISTRY.register("butcheryalexsmobsfood",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.butchery_alexs_mobs.butcheryalexsmobsfood"))
                    .icon(() -> new ItemStack(ModItems.RAW_BIRD_MEAT.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(
                            ButcheryAlexsMobs.MODID, "butcheryalexsmobs"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.RAW_BIRD_MEAT.get());
                        output.accept(ModItems.COOKED_BIRD_MEAT.get());
                        output.accept(ModItems.RAW_CAIMAN_STEAK.get());
                        output.accept(ModItems.COOKED_CAIMAN_STEAK.get());
                        output.accept(ModItems.RAW_CAPUCHIN_MEAT.get());
                        output.accept(ModItems.COOKED_CAPUCHIN_MEAT.get());
                        output.accept(ModItems.RAW_FISH_FILLET.get());
                        output.accept(ModItems.COOKED_FISH_FILLET.get());
                    })
                    .build());

    public static final Supplier<CreativeModeTab> ITEMS = REGISTRY.register("butcheryalexsmobsitems",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.butchery_alexs_mobs.butcheryalexsmobsitems"))
                    .icon(() -> new ItemStack(ModItems.CAIMAN_LEATHER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.CAIMAN_LEATHER.get());
                        output.accept(ModItems.CAPUCHIN_FUR.get());
                    })
                    .build());

    private ModTabs() {}
}
