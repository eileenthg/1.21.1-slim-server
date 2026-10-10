package dev.eteil.butcheryam2.init;

import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {

    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ButcheryAlexsMobs2.MODID);

    public static final Supplier<CreativeModeTab> BLOCKS = REGISTRY.register("carcasses",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ButcheryAlexsMobs2.MODID))
                    .icon(() -> new ItemStack(ModBlocks.head("bison").get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.WARPED_MOSCO_PROBOSCIS.get());
                        output.accept(ModItems.INSECT_WING.get());
                        for (var block : ModBlocks.ALL) {
                            output.accept(block.get().asItem());
                        }
                    })
                    .build());

    private ModTabs() {}
}
