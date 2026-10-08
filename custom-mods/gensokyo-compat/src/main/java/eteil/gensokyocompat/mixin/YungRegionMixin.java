package eteil.gensokyocompat.mixin;

import eteil.gensokyocompat.Bridge;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import terrablender.api.ModifiedVanillaOverworldBuilder;

import java.util.function.Consumer;

/** YUNG's Cave Biomes region keeps vanilla's surface; its forest and dark forest slots become magical forest. */
@Mixin(targets = "com.yungnickyoung.minecraft.yungscavebiomes.world.CaveBiomeRegion", remap = false)
public abstract class YungRegionMixin {
    @ModifyArg(method = "addBiomes", index = 1, at = @At(value = "INVOKE",
            target = "Lcom/yungnickyoung/minecraft/yungscavebiomes/world/CaveBiomeRegion;addModifiedVanillaOverworldBiomes(Ljava/util/function/Consumer;Ljava/util/function/Consumer;)V"))
    private Consumer<ModifiedVanillaOverworldBuilder> gensokyocompat$magical(Consumer<ModifiedVanillaOverworldBuilder> yung) {
        return yung.andThen(b -> {
            b.replaceBiome(Biomes.FOREST, Bridge.MAGICAL_FOREST);
            b.replaceBiome(Biomes.DARK_FOREST, Bridge.MAGICAL_FOREST);
        });
    }
}
