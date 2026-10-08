package com.brass_amber.ba_bt.init;

import com.brass_amber.ba_bt.BABattleTowers;
import com.brass_amber.ba_bt.effect.CoreTemperatureEffect;
import com.brass_amber.ba_bt.effect.DepthDropperEffect;
import net.minecraft.resources.ResourceKey;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BTExtras {

    // Enchantments are data-driven in 1.21: see data/ba_bt/enchantment/*.json
    public static final ResourceKey<Enchantment> DEPTH_DROPPER = ResourceKey.create(Registries.ENCHANTMENT, BABattleTowers.locate("depth_dropper"));
    public static final ResourceKey<Enchantment> CORE_TEMPERATURE = ResourceKey.create(Registries.ENCHANTMENT, BABattleTowers.locate("core_temperature"));
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, BABattleTowers.MOD_ID);


    public static final DeferredHolder<MobEffect, MobEffect> DEPTH_DROPPER_EFFECT = EFFECTS.register("depth_dropper_effect",
            () -> new DepthDropperEffect(MobEffectCategory.NEUTRAL, 13565951)
    );

    public static final DeferredHolder<MobEffect, MobEffect> CORE_TEMPERATURE_EFFECT = EFFECTS.register("core_temperature_effect",
            () -> new CoreTemperatureEffect(MobEffectCategory.HARMFUL, 10027008)
    );

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
