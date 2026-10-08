package com.brass_amber.ba_bt.init;

import com.brass_amber.ba_bt.BABattleTowers;
import com.brass_amber.ba_bt.block.block.*;
import com.brass_amber.ba_bt.item.ChestBlockItem;
import com.brass_amber.ba_bt.item.DataMarkerBlockItem;
import com.brass_amber.ba_bt.util.GolemType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;


public class BTBlocks {

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BABattleTowers.MOD_ID);

	public static final DeferredBlock<Block> LAND_GOLEM_CHEST = registerChestBlock("land_golem_chest",
			() -> new BTChestBlock(BTBlockEntityType.LAND_GOLEM_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 1200.0F)
							.sound(SoundType.STONE).noOcclusion(), GolemType.LAND)
	);
	public static final DeferredBlock<Block> LAND_CHEST = registerChestBlock("land_chest",
			() -> new TowerChestBlock(BTBlockEntityType.LAND_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 1200.0F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(6.0F), GolemType.LAND)
	);

	public static final DeferredBlock<Block> OCEAN_GOLEM_CHEST = registerChestBlock("ocean_golem_chest",
			() -> new BTChestBlock(BTBlockEntityType.OCEAN_GOLEM_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(1200.0F), GolemType.OCEAN)
	);
	public static final DeferredBlock<Block> OCEAN_CHEST = registerChestBlock("ocean_chest",
			() -> new TowerChestBlock(BTBlockEntityType.OCEAN_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 1200.0F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(6.0F), GolemType.OCEAN)
	);

	public static final DeferredBlock<Block> CORE_GOLEM_CHEST = registerChestBlock("core_golem_chest",
			() -> new BTChestBlock(BTBlockEntityType.CORE_GOLEM_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(1200.0F), GolemType.CORE)
	);
	public static final DeferredBlock<Block> CORE_CHEST = registerChestBlock("core_chest",
			() -> new TowerChestBlock(BTBlockEntityType.CORE_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 1200.0F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(6.0F), GolemType.CORE)
	);

	public static final DeferredBlock<Block> NETHER_GOLEM_CHEST = registerChestBlock("nether_golem_chest",
			() -> new BTChestBlock(BTBlockEntityType.NETHER_GOLEM_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(1200.0F), GolemType.NETHER)
	);
	public static final DeferredBlock<Block> NETHER_CHEST = registerChestBlock("nether_chest",
			() -> new TowerChestBlock(BTBlockEntityType.NETHER_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 1200.0F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(6.0F), GolemType.NETHER)
	);

	public static final DeferredBlock<Block> END_GOLEM_CHEST = registerChestBlock("end_golem_chest",
			() -> new BTChestBlock(BTBlockEntityType.END_GOLEM_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(1200.0F), GolemType.END)
	);
	public static final DeferredBlock<Block> END_CHEST = registerChestBlock("end_chest",
			() -> new TowerChestBlock(BTBlockEntityType.END_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 1200.0F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(6.0F), GolemType.END)
	);

	public static final DeferredBlock<Block> SKY_GOLEM_CHEST = registerChestBlock("sky_golem_chest",
			() -> new BTChestBlock(BTBlockEntityType.SKY_GOLEM_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(1200.0F), GolemType.SKY)
	);
	public static final DeferredBlock<Block> SKY_CHEST = registerChestBlock("sky_chest",
			() -> new TowerChestBlock(BTBlockEntityType.SKY_CHEST::get,
					Block.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 1200.0F)
							.sound(SoundType.STONE).noOcclusion().explosionResistance(6.0F), GolemType.SKY)
	);

    public static final DeferredBlock<Block> LAND_SPAWNER = registerBlock("land_spawner",
			() -> new BTSpawnerBlock(Block.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(5.0F).sound(SoundType.METAL).noOcclusion().noLootTable()), 1);

	public static final DeferredBlock<Block> OCEAN_SPAWNER = registerBlock("ocean_spawner",
			() -> new BTSpawnerBlock(Block.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(5.0F).sound(SoundType.METAL).noOcclusion().noLootTable()), 1);

	public static final DeferredBlock<Block> CORE_SPAWNER = registerBlock("core_spawner",
			() -> new BTSpawnerBlock(Block.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(5.0F).sound(SoundType.METAL).noOcclusion().noLootTable()), 1);

	public static final DeferredBlock<Block> NETHER_SPAWNER = registerBlock("nether_spawner",
			() -> new BTSpawnerBlock(Block.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(5.0F).sound(SoundType.METAL).noOcclusion().noLootTable()), 1);

	public static final DeferredBlock<Block> END_SPAWNER = registerBlock("end_spawner",
			() -> new BTSpawnerBlock(Block.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(5.0F).sound(SoundType.METAL).noOcclusion().noLootTable()), 1);

	public static final DeferredBlock<Block> SKY_SPAWNER = registerBlock("sky_spawner",
			() -> new BTSpawnerBlock(Block.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(5.0F).sound(SoundType.METAL).noOcclusion().noLootTable()), 1);

	public static final  DeferredBlock<Block> SPAWNER_MARKER = registerBlock("spawner_marker",
			() -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).noOcclusion().noLootTable().isValidSpawn(BTBlocks::never)), 1);

	public static final DeferredBlock<Block> AIR_FILL = registerBlock("air_fill",
			() -> new BTBlockingAirBlock(BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air().forceSolidOn()), 1);

	public static final DeferredBlock<Block> DATA_MARKER = registerDataMarkerBlock("data_marker",
			() -> new DataMarkerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).noOcclusion().noLootTable().isValidSpawn(BTBlocks::never)), 1);


	public static final DeferredBlock<Block> CORRITE_BLOCK = registerBlock("corrite_block",
			() -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops().lightLevel((blockState) -> 8).strength(15F, 1200.0F)), 64);

	public static final DeferredBlock<Block> CORRITE_STAIR = registerBlock("corrite_stair",
			() -> new StairBlock(CORRITE_BLOCK.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(CORRITE_BLOCK.get())), 64);

	public static final DeferredBlock<Block> CORRITE_SLAB = registerBlock("corrite_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(CORRITE_BLOCK.get())), 64);

	public static final DeferredBlock<Block> CORRITE_WALL = registerBlock("corrite_wall",
			() -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(CORRITE_BLOCK.get())), 64);

	public static final DeferredBlock<Block> CORRITE_CHISELED_BOOKSHELF = registerBlock("corrite_chiseled_bookshelf",
			() -> new ChiseledBookShelfBlock(BlockBehaviour.Properties.ofFullCopy(CORRITE_BLOCK.get()).mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).sound(SoundType.CHISELED_BOOKSHELF)), 64);

	public static final DeferredBlock<Block> CORRITE_LADDER = registerBlock("corrite_ladder",
			() -> new LadderBlock(BlockBehaviour.Properties.ofFullCopy(CORRITE_BLOCK.get()).forceSolidOff().strength(0.4F).sound(SoundType.LADDER).noOcclusion().pushReaction(PushReaction.DESTROY)), 64);

	public static final DeferredBlock<Block> ACTIVE_CORRITE_BLOCK = registerBlock("active_corrite_block",
			() -> new ActiveCorriteBlock(
					Block.Properties.of().mapColor(MapColor.NETHER).strength(5F, 1200.0F).sound(SoundType.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
							.requiresCorrectToolForDrops().lightLevel((blockState) -> 12)
							.isValidSpawn((blockState, blockGetter, blockPos, entityType) -> entityType.fireImmune())
							.hasPostProcess(BTBlocks::always).emissiveRendering(BTBlocks::always)), 64);

	public static final DeferredBlock<Block> ACTIVE_CORRITE_STAIR = registerBlock("active_corrite_stair",
			() -> new ActiveCorriteStair(ACTIVE_CORRITE_BLOCK.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(ACTIVE_CORRITE_BLOCK.get())), 64);

	public static final DeferredBlock<Block> ACTIVE_CORRITE_SLAB = registerBlock("active_corrite_slab",
			() -> new ActiveCorriteSlab(BlockBehaviour.Properties.ofFullCopy(ACTIVE_CORRITE_BLOCK.get())), 64);

	public static final DeferredBlock<Block> ACTIVE_CORRITE_WALL = registerBlock("active_corrite_wall",
			() -> new ActiveCorriteWall(Block.Properties.of().mapColor(MapColor.NETHER).strength(5F, 1200.0F).sound(SoundType.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops().forceSolidOn()), 64);

	public static final DeferredBlock<Block> CORE_MATTER = registerBlock("core_matter_block",
			() -> new CoreMatterBlock(
					BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).forceSolidOn().noCollission().strength(4.0F)
							.pushReaction(PushReaction.DESTROY).lightLevel((blockState) -> 12)
							.hasPostProcess(BTBlocks::always).emissiveRendering(BTBlocks::always)), 64);

	public static final DeferredBlock<Block> CORE_MATTER_STAIR = registerBlock("core_matter_stair",
			() -> new CoreMatterStair(CORE_MATTER.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(CORE_MATTER.get())), 64);

	public static final DeferredBlock<Block> CORE_MATTER_SLAB = registerBlock("core_matter_slab",
			() -> new CoreMatterSlab(
					BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).forceSolidOn().noCollission().strength(4.0F)
							.pushReaction(PushReaction.DESTROY).lightLevel((blockState) -> 12)
							.hasPostProcess(BTBlocks::always).emissiveRendering(BTBlocks::always)), 64);

	public static final DeferredBlock<Block> CORE_MATTER_WALL = registerBlock("core_matter_wall",
			() -> new CoreMatterWall(BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).forceSolidOn().noCollission().strength(4.0F)
					.pushReaction(PushReaction.DESTROY).lightLevel((blockState) -> 12)
					.hasPostProcess(BTBlocks::always).emissiveRendering(BTBlocks::always)), 64);


	private static Boolean never(BlockState p_50779_, BlockGetter p_50780_, BlockPos p_50781_, EntityType<?> p_50782_) {
		return false;
	}

	private static Boolean always(BlockState p_50810_, BlockGetter p_50811_, BlockPos p_50812_) {
		return true;
	}

	private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block, int stackSize) {
		DeferredBlock<T> toReturn = BLOCKS.register(name, block);
		registerBlockItem(name, toReturn, stackSize);
		return toReturn;
	}
	
	private static  <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block, int stackSize) {
		BTItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(stackSize)));
	}


	private static <T extends Block> DeferredBlock<T> registerChestBlock(String name, Supplier<T> block) {
		DeferredBlock<T> toReturn = BLOCKS.register(name, block);
		registerChestBlockItem(name, toReturn, 64);
		return toReturn;
	}

	private static  <T extends Block> void registerChestBlockItem(String name, DeferredBlock<T> block, int stackSize) {
		BTItems.ITEMS.register(name, () -> new ChestBlockItem(block.get(), new Item.Properties().stacksTo(stackSize)));
	}

	private static <T extends Block> DeferredBlock<T> registerDataMarkerBlock(String name, Supplier<T> block, int stackSize) {
		DeferredBlock<T> toReturn = BLOCKS.register(name, block);
		registerDataMarkerBlockItem(name, toReturn, stackSize);
		return toReturn;
	}

	private static  <T extends Block> void registerDataMarkerBlockItem(String name, DeferredBlock<T> block, int stackSize) {
		BTItems.ITEMS.register(name, () -> new DataMarkerBlockItem(block.get(), new Item.Properties().stacksTo(stackSize)));
	}


	public static void register(IEventBus eventBus) {
		BLOCKS.register(eventBus);
	}

}
