package com.brass_amber.ba_bt.init;

import net.minecraft.core.registries.Registries;
import com.brass_amber.ba_bt.BABattleTowers;
import com.brass_amber.ba_bt.inventory.BTChestMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BTMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BABattleTowers.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<BTChestMenu>> GENERIC_9x8 = MENUS.register("generic_9x8", () -> new MenuType<>(BTChestMenu::eightRows, FeatureFlags.REGISTRY.allFlags()));

    private static  <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(IContainerFactory<T> factory, String name) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
