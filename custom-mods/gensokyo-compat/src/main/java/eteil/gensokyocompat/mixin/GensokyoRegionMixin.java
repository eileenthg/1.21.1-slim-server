package eteil.gensokyocompat.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import terrablender.api.ModifiedVanillaOverworldBuilder;

import java.util.function.Consumer;

/**
 * Adds Gensokyo Legacy's magical forest to Youkai's Homecoming's existing TerraBlender region, in the slots where
 * that region would otherwise place a vanilla dark forest. Because no new region is weighted in, every other area
 * of an existing world keeps the biome it would have had.
 */
@Mixin(targets = "dev.xkmc.youkaishomecoming.compat.terrablender.GensokyoRegion", remap = false)
public abstract class GensokyoRegionMixin {
    private static final ResourceKey<Biome> MAGICAL_FOREST =
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("gensokyolegacy", "magical_forest"));

    @ModifyArg(method = "addBiomes", index = 1, at = @At(value = "INVOKE",
            target = "Ldev/xkmc/youkaishomecoming/compat/terrablender/GensokyoRegion;addModifiedVanillaOverworldBiomes(Ljava/util/function/Consumer;Ljava/util/function/Consumer;)V"))
    private Consumer<ModifiedVanillaOverworldBuilder> gensokyocompat$addMagicalForest(Consumer<ModifiedVanillaOverworldBuilder> yh) {
        return yh.andThen(builder -> builder.replaceBiome(Biomes.DARK_FOREST, MAGICAL_FOREST));
    }
}
