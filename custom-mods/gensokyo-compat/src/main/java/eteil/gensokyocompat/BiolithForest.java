package eteil.gensokyocompat;

import com.terraformersmc.biolith.api.biome.BiomePlacement;
import net.minecraft.world.level.biome.Biomes;

/**
 * With Biolith present (bundled in Quark), every overworld cell goes through Biolith after TerraBlender picks it,
 * including TerraBlender's vanilla tree and deferred cells, which region mixins never see. A quarter of forest
 * becomes magical forest, bringing the two to about the same share on top of the YUNG-region conversion.
 */
final class BiolithForest {
    static final double FOREST_SHARE = 0.25;

    static void register() {
        BiomePlacement.replaceOverworld(Biomes.FOREST, Bridge.MAGICAL_FOREST, FOREST_SHARE);
    }
}
