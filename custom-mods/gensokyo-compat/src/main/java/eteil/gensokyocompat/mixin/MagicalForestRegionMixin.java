package eteil.gensokyocompat.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * Legacy's region paints magical forest and its own sakura forest over most of the climate space, including
 * oceans and frozen peaks. This keeps the vanilla layout of the region and only swaps the forest and dark
 * forest slots for magical forest (same 0.7 temperature), the way Youkai's Homecoming places its sakura biomes.
 * Legacy's duplicate sakura forest is no longer placed.
 */
@Mixin(targets = "dev.xkmc.gensokyolegacy.init.data.biome.MagicalForestRegion", remap = false)
public abstract class MagicalForestRegionMixin extends Region {
    private static final ResourceKey<Biome> MAGICAL_FOREST =
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("gensokyolegacy", "magical_forest"));

    protected MagicalForestRegionMixin(ResourceLocation name, RegionType type, int weight) {
        super(name, type, weight);
    }

    /**
     * Weight 0 keeps TerraBlender's total region weight unchanged, so adding Legacy does not re-roll which
     * region owns each area of an existing world. The magical forest is placed through YH's region instead
     * (see GensokyoRegionMixin).
     */
    @Override
    public int getWeight() {
        return 0;
    }

    /**
     * @author eileen
     * @reason keep Legacy's biomes inside the climate band of the vanilla biomes they replace
     */
    @Overwrite
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            builder.replaceBiome(Biomes.FOREST, MAGICAL_FOREST);
            builder.replaceBiome(Biomes.DARK_FOREST, MAGICAL_FOREST);
        });
    }
}
