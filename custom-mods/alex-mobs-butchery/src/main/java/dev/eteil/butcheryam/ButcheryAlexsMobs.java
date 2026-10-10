package dev.eteil.butcheryam;

import dev.eteil.butcheryam.init.ModBlockEntities;
import dev.eteil.butcheryam.init.ModBlocks;
import dev.eteil.butcheryam.init.ModItems;
import dev.eteil.butcheryam.init.ModParticles;
import dev.eteil.butcheryam.init.ModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * Butchery: Alex's Mobs — private port to NeoForge 1.21.1.
 *
 * <p>Cross-mod integration with Butchery and Alex's Mobs is done entirely through
 * {@code ResourceLocation}s, tags and loot tables, so neither mod is a compile-time
 * dependency; both are required at runtime.
 */
@Mod(ButcheryAlexsMobs.MODID)
public class ButcheryAlexsMobs {

    public static final String MODID = "butchery_alexs_mobs";

    public ButcheryAlexsMobs(IEventBus modBus, ModContainer container) {
        ModBlocks.REGISTRY.register(modBus);
        ModItems.REGISTRY.register(modBus);
        ModBlockEntities.REGISTRY.register(modBus);
        ModParticles.REGISTRY.register(modBus);
        ModTabs.REGISTRY.register(modBus);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
