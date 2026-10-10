package dev.eteil.butcheryam2;

import dev.eteil.butcheryam2.init.ModBlockEntities;
import dev.eteil.butcheryam2.init.ModBlocks;
import dev.eteil.butcheryam2.init.ModItems;
import dev.eteil.butcheryam2.init.ModLootModifiers;
import dev.eteil.butcheryam2.init.ModParticles;
import dev.eteil.butcheryam2.init.ModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * Butchery: Alex's Mobs Part 2 — carcasses for the Alex's Mobs creatures Part 1 does not cover.
 *
 * <p>The carcass models are converted from Alex's Mobs' own entity models by {@code tools/gen.py};
 * the drops come from Alex's Delight (and AMC Delight for the grizzly). Nothing here is a
 * compile-time dependency: every cross-mod reference is a ResourceLocation, tag or loot table.
 */
@Mod(ButcheryAlexsMobs2.MODID)
public class ButcheryAlexsMobs2 {

    public static final String MODID = "butchery_alexs_mobs_2";

    public ButcheryAlexsMobs2(IEventBus modBus, ModContainer container) {
        ModBlocks.REGISTRY.register(modBus);
        ModItems.REGISTRY.register(modBus);
        ModBlockEntities.REGISTRY.register(modBus);
        ModParticles.REGISTRY.register(modBus);
        ModTabs.REGISTRY.register(modBus);
        ModLootModifiers.REGISTRY.register(modBus);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
