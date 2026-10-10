package eteil.settledcaves;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(SettledCaves.MODID)
public class SettledCaves {
    public static final String MODID = "settledcaves";

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MODID);

    public static final DeferredHolder<Feature<?>, SettleFeature> SETTLE =
            FEATURES.register("settle", SettleFeature::new);

    public SettledCaves(IEventBus modBus, ModContainer container) {
        FEATURES.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
