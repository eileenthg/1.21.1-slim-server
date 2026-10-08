package com.brass_amber.ba_bt.init;

import com.brass_amber.ba_bt.BABattleTowers;
import com.brass_amber.ba_bt.item.item.*;
import com.brass_amber.ba_bt.util.GolemType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;



public class BTItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BABattleTowers.MOD_ID);

	public static final DeferredItem<Item> TAB_ICON = ITEMS.register("tab_icon",() -> new Item(new Item.Properties().stacksTo(16)));
	public static final DeferredItem<Item> LAND_MONOLITH_KEY = ITEMS.register("land_monolith_key", () -> new MonolithKeyItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant()));
	public static final DeferredItem<Item> OCEAN_MONOLITH_KEY = ITEMS.register("ocean_monolith_key", () -> new MonolithKeyItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant()));
	public static final DeferredItem<Item> CORE_MONOLITH_KEY = ITEMS.register("core_monolith_key", () -> new MonolithKeyItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant()));
	public static final DeferredItem<Item> NETHER_MONOLITH_KEY = ITEMS.register("nether_monolith_key", () -> new MonolithKeyItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant()));
	public static final DeferredItem<Item> END_MONOLITH_KEY = ITEMS.register("end_monolith_key", () -> new MonolithKeyItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant()));
	public static final DeferredItem<Item> SKY_MONOLITH_KEY = ITEMS.register("sky_monolith_key", () -> new MonolithKeyItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant()));

	public static final DeferredItem<Item> LAND_GOLEM_EYE = ITEMS.register("land_golem_eye", () -> new GuardianEyeItem(GolemType.LAND, (new Item.Properties()).stacksTo(8).rarity(Rarity.EPIC).fireResistant()));
	public static final DeferredItem<Item> OCEAN_GOLEM_EYE = ITEMS.register("ocean_golem_eye", () -> new GuardianEyeItem(GolemType.OCEAN, (new Item.Properties()).stacksTo(8).rarity(Rarity.EPIC).fireResistant()));
	public static final DeferredItem<Item> CORE_GOLEM_EYE = ITEMS.register("core_golem_eye", () -> new GuardianEyeItem(GolemType.CORE, (new Item.Properties()).stacksTo(8).rarity(Rarity.EPIC).fireResistant()));
	public static final DeferredItem<Item> NETHER_GOLEM_EYE = ITEMS.register("nether_golem_eye", () -> new GuardianEyeItem(GolemType.NETHER, (new Item.Properties()).stacksTo(8).rarity(Rarity.EPIC).fireResistant()));
	public static final DeferredItem<Item> END_GOLEM_EYE = ITEMS.register("end_golem_eye", () -> new GuardianEyeItem(GolemType.END, (new Item.Properties()).stacksTo(8).rarity(Rarity.EPIC).fireResistant()));
	public static final DeferredItem<Item> SKY_GOLEM_EYE = ITEMS.register("sky_golem_eye", () -> new GuardianEyeItem(GolemType.SKY, (new Item.Properties()).stacksTo(8).rarity(Rarity.EPIC).fireResistant()));

	public static final DeferredItem<Item> LAND_MONOLITH = ITEMS.register("land_monolith", () -> new MonolithItem(GolemType.LAND, (new Item.Properties()).stacksTo(1).rarity(Rarity.EPIC)));
	public static final DeferredItem<Item> OCEAN_MONOLITH = ITEMS.register("ocean_monolith", () -> new MonolithItem(GolemType.OCEAN, (new Item.Properties()).stacksTo(1).rarity(Rarity.EPIC)));
	public static final DeferredItem<Item> CORE_MONOLITH = ITEMS.register("core_monolith", () -> new MonolithItem(GolemType.CORE, (new Item.Properties()).stacksTo(1).rarity(Rarity.EPIC)));
	public static final DeferredItem<Item> NETHER_MONOLITH = ITEMS.register("nether_monolith", () -> new MonolithItem(GolemType.NETHER, (new Item.Properties()).stacksTo(1).rarity(Rarity.EPIC)));
	public static final DeferredItem<Item> END_MONOLITH = ITEMS.register("end_monolith", () -> new MonolithItem(GolemType.END, (new Item.Properties()).stacksTo(1).rarity(Rarity.EPIC)));
	public static final DeferredItem<Item> SKY_MONOLITH = ITEMS.register("sky_monolith", () -> new MonolithItem(GolemType.SKY, (new Item.Properties()).stacksTo(1).rarity(Rarity.EPIC)));

	public static final DeferredItem<Item> LAND_CHEST_SHARD = ITEMS.register("land_chest_shard", () -> new Item(new Item.Properties().stacksTo(16)));
	public static final DeferredItem<Item> OCEAN_CHEST_SHARD = ITEMS.register("ocean_chest_shard", () -> new Item(new Item.Properties().stacksTo(16)));
	public static final DeferredItem<Item> CORE_CHEST_SHARD = ITEMS.register("core_chest_shard", () -> new Item(new Item.Properties().stacksTo(16)));
	public static final DeferredItem<Item> NETHER_CHEST_SHARD = ITEMS.register("nether_chest_shard", () -> new Item(new Item.Properties().stacksTo(16)));
	public static final DeferredItem<Item> END_CHEST_SHARD = ITEMS.register("end_chest_shard", () -> new Item(new Item.Properties().stacksTo(16)));
	public static final DeferredItem<Item> SKY_CHEST_SHARD = ITEMS.register("sky_chest_shard", () -> new Item(new Item.Properties().stacksTo(16)));

	public static final DeferredItem<Item> LAND_RESONANCE_CRYSTAL = ITEMS.register("land_resonance_stone", () -> new ResonanceStoneItem(GolemType.LAND, new Item.Properties().stacksTo(1).fireResistant().jukeboxPlayable(song("land"))));
	public static final DeferredItem<Item> OCEAN_RESONANCE_CRYSTAL = ITEMS.register("ocean_resonance_stone", () -> new ResonanceStoneItem(GolemType.OCEAN, new Item.Properties().stacksTo(1).fireResistant().jukeboxPlayable(song("ocean"))));
	public static final DeferredItem<Item> CORE_RESONANCE_CRYSTAL = ITEMS.register("core_resonance_stone", () -> new ResonanceStoneItem(GolemType.CORE, new Item.Properties().stacksTo(1).fireResistant().jukeboxPlayable(song("core"))));
	public static final DeferredItem<Item> NETHER_RESONANCE_CRYSTAL = ITEMS.register("nether_resonance_stone", () -> new ResonanceStoneItem(GolemType.NETHER, new Item.Properties().stacksTo(1).fireResistant().jukeboxPlayable(song("nether"))));
	public static final DeferredItem<Item> END_RESONANCE_CRYSTAL = ITEMS.register("end_resonance_stone", () -> new ResonanceStoneItem(GolemType.END, new Item.Properties().stacksTo(1).fireResistant().jukeboxPlayable(song("end"))));
	public static final DeferredItem<Item> SKY_RESONANCE_CRYSTAL = ITEMS.register("sky_resonance_stone", () -> new ResonanceStoneItem(GolemType.SKY, new Item.Properties().stacksTo(1).fireResistant().jukeboxPlayable(song("sky"))));
	public static final DeferredItem<Item> CITY_RESONANCE_CRYSTAL = ITEMS.register("city_resonance_stone", () -> new ResonanceStoneItem(GolemType.CITY, new Item.Properties().stacksTo(1).fireResistant().jukeboxPlayable(song("city"))));

	private static ResourceKey<JukeboxSong> song(String name) {
		return ResourceKey.create(Registries.JUKEBOX_SONG, BABattleTowers.locate(name));
	}

	public static void register(IEventBus eventBus) {
		ITEMS.register(eventBus);
	}

}