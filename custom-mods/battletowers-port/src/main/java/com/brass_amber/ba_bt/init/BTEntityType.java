package com.brass_amber.ba_bt.init;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.neoforged.fml.common.EventBusSubscriber;
import com.brass_amber.ba_bt.BABattleTowers;
import com.brass_amber.ba_bt.entity.CoreDestructionEntity;
import com.brass_amber.ba_bt.entity.LandDestructionEntity;
import com.brass_amber.ba_bt.entity.ExplosionPhysics;
import com.brass_amber.ba_bt.entity.OceanDestructionEntity;
import com.brass_amber.ba_bt.entity.block.*;
import com.brass_amber.ba_bt.entity.hostile.BTCultist;
import com.brass_amber.ba_bt.entity.hostile.FragmentOfObthuuryn;
import com.brass_amber.ba_bt.entity.hostile.SkyMinion;
import com.brass_amber.ba_bt.entity.hostile.golem.*;
import com.brass_amber.ba_bt.entity.hostile.golem.LandGolem;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;


@EventBusSubscriber(modid = BABattleTowers.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class BTEntityType {


	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, BABattleTowers.MOD_ID);

	//*********************** GOLEMS *********************\\
	public static final DeferredHolder<EntityType<?>, EntityType<LandGolem>> LAND_GOLEM = ENTITY_TYPES.register("land_golem", () -> EntityType.Builder.of(LandGolem::new, MobCategory.MONSTER).sized(AbstractGolem.SCALE * 2 * 0.6F, AbstractGolem.SCALE * 2 * 2).setTrackingRange(10).fireImmune().build("land_golem"));
	public static final DeferredHolder<EntityType<?>, EntityType<OceanGolem>> OCEAN_GOLEM = ENTITY_TYPES.register("ocean_golem", () -> EntityType.Builder.of(OceanGolem::new, MobCategory.MONSTER).sized(AbstractGolem.SCALE * 2 * 0.6F, AbstractGolem.SCALE * 2 * 2).setTrackingRange(10).fireImmune().build("ocean_golem"));
	public static final DeferredHolder<EntityType<?>, EntityType<CoreGolem>> CORE_GOLEM = ENTITY_TYPES.register("core_golem", () -> EntityType.Builder.of(CoreGolem::new, MobCategory.MONSTER).sized(AbstractGolem.SCALE * 2 * 0.6F, AbstractGolem.SCALE * 2 * 2).setTrackingRange(10).fireImmune().build("core_golem"));
	public static final DeferredHolder<EntityType<?>, EntityType<NetherGolem>> NETHER_GOLEM = ENTITY_TYPES.register("nether_golem", () -> EntityType.Builder.of(NetherGolem::new, MobCategory.MONSTER).sized(AbstractGolem.SCALE * 2 * 0.6F, AbstractGolem.SCALE * 2 * 2).setTrackingRange(10).fireImmune().build("nether_golem"));
	public static final DeferredHolder<EntityType<?>, EntityType<EndGolem>> END_GOLEM = ENTITY_TYPES.register("end_golem", () -> EntityType.Builder.of(EndGolem::new, MobCategory.MONSTER).sized(AbstractGolem.SCALE * 2 * 0.6F, AbstractGolem.SCALE * 2 * 2).setTrackingRange(10).fireImmune().build("end_golem"));
	public static final DeferredHolder<EntityType<?>, EntityType<SkyGolem>> SKY_GOLEM = ENTITY_TYPES.register("sky_golem", () -> EntityType.Builder.of(SkyGolem::new, MobCategory.MONSTER).sized(AbstractGolem.SCALE * 2 * 0.6F, AbstractGolem.SCALE * 2 * 2).setTrackingRange(10).fireImmune().build("sky_golem"));

	//*********************** MONOLITHS *********************\\
	public static final DeferredHolder<EntityType<?>, EntityType<BTMonolith>> LAND_MONOLITH = ENTITY_TYPES.register("land_monolith", () -> EntityType.Builder.<BTMonolith>of(BTMonolith::new, MobCategory.MISC).sized(1.0F, 2.0F).setTrackingRange(16).updateInterval(Integer.MAX_VALUE).fireImmune().immuneTo(Blocks.TNT).build("land_monolith"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTMonolith>> OCEAN_MONOLITH = ENTITY_TYPES.register("ocean_monolith", () -> EntityType.Builder.<BTMonolith>of(BTMonolith::new, MobCategory.MISC).sized(1.0F, 2.0F).setTrackingRange(16).updateInterval(Integer.MAX_VALUE).fireImmune().immuneTo(Blocks.TNT).build("ocean_monolith"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTMonolith>> CORE_MONOLITH = ENTITY_TYPES.register("core_monolith", () -> EntityType.Builder.<BTMonolith>of(BTMonolith::new, MobCategory.MISC).sized(1.0F, 2.0F).setTrackingRange(16).updateInterval(Integer.MAX_VALUE).fireImmune().immuneTo(Blocks.TNT).build("core_monolith"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTMonolith>> NETHER_MONOLITH = ENTITY_TYPES.register("nether_monolith", () -> EntityType.Builder.<BTMonolith>of(BTMonolith::new, MobCategory.MISC).sized(1.0F, 2.0F).setTrackingRange(16).updateInterval(Integer.MAX_VALUE).fireImmune().immuneTo(Blocks.TNT).build("nether_monolith"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTMonolith>> END_MONOLITH = ENTITY_TYPES.register("end_monolith", () -> EntityType.Builder.<BTMonolith>of(BTMonolith::new, MobCategory.MISC).sized(1.0F, 2.0F).setTrackingRange(16).updateInterval(Integer.MAX_VALUE).fireImmune().immuneTo(Blocks.TNT).build("end_monolith"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTMonolith>> SKY_MONOLITH = ENTITY_TYPES.register("sky_monolith", () -> EntityType.Builder.<BTMonolith>of(BTMonolith::new, MobCategory.MISC).sized(1.0F, 2.0F).setTrackingRange(16).updateInterval(Integer.MAX_VALUE).fireImmune().immuneTo(Blocks.TNT).build("sky_monolith"));

	//*********************** OBELISKS *********************\\
	public static final DeferredHolder<EntityType<?>, EntityType<BTAbstractObelisk>> LAND_OBELISK = ENTITY_TYPES.register("land_obelisk", () -> EntityType.Builder.<BTAbstractObelisk>of(BTLandObelisk::new, MobCategory.MISC).noSummon().sized(1.0F, 3.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).build("land_obelisk"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTAbstractObelisk>> OCEAN_OBELISK = ENTITY_TYPES.register("ocean_obelisk", () -> EntityType.Builder.<BTAbstractObelisk>of(BTOceanObelisk::new, MobCategory.MISC).noSummon().sized(1.0F, 3.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).build("ocean_obelisk"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTAbstractObelisk>> CORE_OBELISK = ENTITY_TYPES.register("core_obelisk", () -> EntityType.Builder.<BTAbstractObelisk>of(BTCoreObelisk::new, MobCategory.MISC).noSummon().sized(1.0F, 3.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).build("core_obelisk"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTAbstractObelisk>> NETHER_OBELISK = ENTITY_TYPES.register("nether_obelisk", () -> EntityType.Builder.<BTAbstractObelisk>of(BTAbstractObelisk::new, MobCategory.MISC).noSummon().sized(1.0F, 3.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).build("nether_obelisk"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTAbstractObelisk>> END_OBELISK = ENTITY_TYPES.register("end_obelisk", () -> EntityType.Builder.<BTAbstractObelisk>of(BTAbstractObelisk::new, MobCategory.MISC).noSummon().sized(1.0F, 3.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).build("end_obelisk"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTAbstractObelisk>> SKY_OBELISK = ENTITY_TYPES.register("sky_obelisk", () -> EntityType.Builder.<BTAbstractObelisk>of(BTAbstractObelisk::new, MobCategory.MISC).noSummon().sized(1.0F, 3.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).build("sky_obelisk"));

	//*********************** TOWER MOBS *********************\\
	public static final DeferredHolder<EntityType<?>, EntityType<SkyMinion>> SKY_MINION = ENTITY_TYPES.register("sky_minion", () -> EntityType.Builder.of(SkyMinion::new, MobCategory.MONSTER).fireImmune().sized(0.8F, 1.9F).clientTrackingRange(8).build("sky_minion"));
	public static final DeferredHolder<EntityType<?>, EntityType<BTCultist>> BT_CULTIST = ENTITY_TYPES.register("bt_cultist", () -> EntityType.Builder.of(BTCultist::new, MobCategory.MONSTER).sized(0.6F, 1.99F).clientTrackingRange(8).build("bt_cultist"));
	public static final DeferredHolder<EntityType<?>, EntityType<FragmentOfObthuuryn>> FRAGMENT_OF_OBTHUURYN = ENTITY_TYPES.register("fragment_of_obthuuryn", () -> EntityType.Builder.of(FragmentOfObthuuryn::new, MobCategory.MONSTER).fireImmune().canSpawnFarFromPlayer().sized(1.0F, 1.0F).clientTrackingRange(10).build("fragment_of_obthuuryn"));

	//*********************** MISC ENTITIES *********************\\
	public static final DeferredHolder<EntityType<?>, EntityType<LandDestructionEntity>> LAND_DESTRUCTION = ENTITY_TYPES.register("land_destruction",
			() -> EntityType.Builder.<LandDestructionEntity>of(LandDestructionEntity::new, MobCategory.MISC)
					.sized(1.0F, 1.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).noSummon().build("land_destruction"));

	public static final DeferredHolder<EntityType<?>, EntityType<OceanDestructionEntity>> OCEAN_DESTRUCTION = ENTITY_TYPES.register("ocean_destruction",
			() -> EntityType.Builder.<OceanDestructionEntity>of(OceanDestructionEntity::new, MobCategory.MISC)
					.sized(1.0F, 1.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).noSummon().build("ocean_destruction"));

	public static final DeferredHolder<EntityType<?>, EntityType<CoreDestructionEntity>> CORE_DESTRUCTION = ENTITY_TYPES.register("core_destruction",
			() -> EntityType.Builder.<CoreDestructionEntity>of(CoreDestructionEntity::new, MobCategory.MISC)
					.sized(1.0F, 1.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).noSummon().build("core_destruction"));

	public static final DeferredHolder<EntityType<?>, EntityType<LandDestructionEntity>> NETHER_DESTRUCTION = ENTITY_TYPES.register("nether_destruction",
			() -> EntityType.Builder.<LandDestructionEntity>of(LandDestructionEntity::new, MobCategory.MISC)
					.sized(1.0F, 1.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).noSummon().build("nether_destruction"));

	public static final DeferredHolder<EntityType<?>, EntityType<LandDestructionEntity>> END_DESTRUCTION = ENTITY_TYPES.register("end_destruction",
			() -> EntityType.Builder.<LandDestructionEntity>of(LandDestructionEntity::new, MobCategory.MISC)
					.sized(1.0F, 1.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).noSummon().build("end_destruction"));

	public static final DeferredHolder<EntityType<?>, EntityType<LandDestructionEntity>> SKY_DESTRUCTION = ENTITY_TYPES.register("sky_destruction",
			() -> EntityType.Builder.<LandDestructionEntity>of(LandDestructionEntity::new, MobCategory.MISC)
					.sized(1.0F, 1.0F).setTrackingRange(100).fireImmune().immuneTo(Blocks.TNT).noSummon().build("sky_destruction"));

	public static final DeferredHolder<EntityType<?>, EntityType<ExplosionPhysics>> PHYSICS_EXPLOSION = ENTITY_TYPES.register("explosion_physics",
			() -> EntityType.Builder.of(ExplosionPhysics::new, MobCategory.MISC)
					.sized(0.0F, 0.0F).setTrackingRange(100).immuneTo(Blocks.TNT).fireImmune().noSave().noSummon().build("explosion_physics"));

	
	/**
	 * Register Spawn Rules
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRegisterEntityTypes(RegisterSpawnPlacementsEvent event) {
		registerSpawnPlacement(event, LAND_GOLEM.get(), Mob::checkMobSpawnRules);
		registerSpawnPlacement(event, OCEAN_GOLEM.get(), Mob::checkMobSpawnRules);
		registerSpawnPlacement(event, NETHER_GOLEM.get(), Mob::checkMobSpawnRules);
		registerSpawnPlacement(event, CORE_GOLEM.get(), Mob::checkMobSpawnRules);
		registerSpawnPlacement(event, END_GOLEM.get(), Mob::checkMobSpawnRules);
		registerSpawnPlacement(event, SKY_GOLEM.get(), Mob::checkMobSpawnRules);

		registerSpawnPlacement(event, SKY_MINION.get(), Mob::checkMobSpawnRules);
		registerSpawnPlacement(event, BT_CULTIST.get(), Mob::checkMobSpawnRules);
		registerSpawnPlacement(event, FRAGMENT_OF_OBTHUURYN.get(), Mob::checkMobSpawnRules);
	}

	/**
	 * Initialize Mob Attributes
	 */
	@SubscribeEvent
	public static void initializeAttributes(EntityAttributeCreationEvent event) {
		event.put(LAND_GOLEM.get(), LandGolem.createBattleGolemAttributes().build());
		event.put(OCEAN_GOLEM.get(), OceanGolem.createBattleGolemAttributes().build());
		event.put(NETHER_GOLEM.get(), NetherGolem.createBattleGolemAttributes().build());
		event.put(CORE_GOLEM.get(), CoreGolem.createBattleGolemAttributes().build());
		event.put(END_GOLEM.get(), EndGolem.createBattleGolemAttributes().build());
		event.put(SKY_GOLEM.get(), SkyGolem.createBattleGolemAttributes().build());

		event.put(SKY_MINION.get(), SkyMinion.createAttributes().build());
		event.put(BT_CULTIST.get(), BTCultist.createAttributes().build());
		event.put(FRAGMENT_OF_OBTHUURYN.get(), FragmentOfObthuuryn.createAttributes().build());
	}
	/**
	 * Helper method for registering Entity Spawning
	 */
	private static <T extends Mob> void registerSpawnPlacement(RegisterSpawnPlacementsEvent event, EntityType<T> entityType, SpawnPlacements.SpawnPredicate<T> placementPredicate) {
		event.register(entityType, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, placementPredicate, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static void register(IEventBus eventBus) {
		ENTITY_TYPES.register(eventBus);
	}
}