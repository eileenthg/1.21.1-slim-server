package com.brass_amber.ba_bt.init;

import net.neoforged.fml.common.EventBusSubscriber;
import com.brass_amber.ba_bt.BABattleTowers;
import com.brass_amber.ba_bt.item.ItemPool;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@EventBusSubscriber(modid = BABattleTowers.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class BTRegistries {

    public static final class Keys {
        public static final ResourceKey<Registry<ItemPool>> ITEM_POOLS = key("item_pools");
    }

    private static <T> ResourceKey<Registry<T>> key(String name) {
            return ResourceKey.createRegistryKey(BABattleTowers.locate(name));
    }

    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(
                Keys.ITEM_POOLS,
                ItemPool.CODEC
        );
    }
}
