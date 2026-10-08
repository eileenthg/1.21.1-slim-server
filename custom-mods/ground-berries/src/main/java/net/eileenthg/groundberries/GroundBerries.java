package net.eileenthg.groundberries;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.common.Mod;

/**
 * Ground Berries: Eternal Tales bushes only ripen inside a Gardener's Pot, because the pot's own
 * procedure does the growing and the bush blocks never tick. This gives each growable bush stage a
 * random tick that advances it one stage, the way sweet berries do, as long as it stands on a
 * normal ground block and has light. Bushes in pots are left to the pot.
 */
@Mod(GroundBerries.MOD_ID)
public final class GroundBerries {

    public static final String MOD_ID = "groundberries";
    private static final String ET = "eternal_tales";

    /** Bush stage to the stage it ripens into. Harvesting already sends the last stage back. */
    private static final Map<String, String> NEXT = new HashMap<>();

    static {
        NEXT.put("blueberry_bush", "blueberry_bush_berry");
        NEXT.put("wolfberry_bush", "wolfberry_bush_1");
        for (String crop : new String[] {"tea_bush", "chili_pepper_bush", "aquatura_bush"}) {
            for (int i = 1; i < 4; i++) {
                NEXT.put(crop + "_" + i, crop + "_" + (i + 1));
            }
        }
        NEXT.put("flameblossom_bush", "flameblossom_bush_2");
        NEXT.put("flameblossom_bush_2", "flameblossom_bush_3");
        NEXT.put("flameblossom_bush_3", "flameblossom_bush_4");
    }

    public static boolean grows(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return ET.equals(id.getNamespace()) && NEXT.containsKey(id.getPath());
    }

    public static void tryGrow(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String next = NEXT.get(id.getPath());
        if (next == null) {
            return;
        }
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(BlockTags.create(ResourceLocation.fromNamespaceAndPath(ET, "plants_placement_blocks")))
                || below.is(BlockTags.create(ResourceLocation.fromNamespaceAndPath(ET, "gardeners_pots")))) {
            return;
        }
        if (level.getRawBrightness(pos, 0) < 9) {
            return;
        }
        Block target = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(ET, next));
        if (target == net.minecraft.world.level.block.Blocks.AIR) {
            return;
        }
        BlockState grown = target.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && grown.hasProperty(BlockStateProperties.WATERLOGGED)) {
            grown = grown.setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));
        }
        level.setBlock(pos, grown, Block.UPDATE_ALL);
    }
}
