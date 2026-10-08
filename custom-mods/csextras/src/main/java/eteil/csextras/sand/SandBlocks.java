package eteil.csextras.sand;

import eteil.csextras.CSExtras;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SandBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CSExtras.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CSExtras.MOD_ID);

    public static final DeferredBlock<SandPileBlock> SAND_PILE = BLOCKS.register("sand_pile",
            () -> new SandPileBlock(14406560, pile(MapColor.SAND)));
    public static final DeferredBlock<SandPileBlock> RED_SAND_PILE = BLOCKS.register("red_sand_pile",
            () -> new SandPileBlock(11098145, pile(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<SandCauldronBlock> SAND_CAULDRON = BLOCKS.register("sand_cauldron",
            () -> new SandCauldronBlock(false, BlockBehaviour.Properties.ofLegacyCopy(Blocks.CAULDRON)));
    public static final DeferredBlock<SandCauldronBlock> RED_SAND_CAULDRON = BLOCKS.register("red_sand_cauldron",
            () -> new SandCauldronBlock(true, BlockBehaviour.Properties.ofLegacyCopy(Blocks.CAULDRON)));

    public static final DeferredItem<BlockItem> SAND_PILE_ITEM = ITEMS.registerSimpleBlockItem(SAND_PILE);
    public static final DeferredItem<BlockItem> RED_SAND_PILE_ITEM = ITEMS.registerSimpleBlockItem(RED_SAND_PILE);

    private static BlockBehaviour.Properties pile(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.2f).sound(SoundType.SAND)
                .pushReaction(PushReaction.DESTROY).isViewBlocking((s, l, p) -> s.getValue(SandPileBlock.LAYERS) >= 8);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(SandBlocks::onBuildTabs);
    }

    private static void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(SAND_PILE_ITEM.get());
            event.accept(RED_SAND_PILE_ITEM.get());
        }
    }

    public static Item pileItem(boolean red) {
        return (red ? RED_SAND_PILE_ITEM : SAND_PILE_ITEM).get();
    }

    private SandBlocks() {
    }
}
