package eteil.csextras;

import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

@Mod(CSExtras.MOD_ID)
public final class CSExtras {
    public static final String MOD_ID = "csextras";
    public static final Logger LOG = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public CSExtras(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modBus.addListener(CSExtras::onBuildTabs);
        eteil.csextras.sand.SandBlocks.register(modBus);

        var bus = NeoForge.EVENT_BUS;
        bus.addListener(CSExtras::onRegisterModifiers);
        bus.addListener(WeatherTemp::onPre);
        bus.addListener(WeatherTemp::onPost);
        bus.addListener(BodyHandlers::onPlayerTick);
        bus.addListener(Campfires::onRightClickBlock);
        bus.addListener(Foods::onFinishUsing);
        bus.addListener(Pitchfork::onBreakSpeed);
        bus.addListener(Pitchfork::onBreak);
        bus.addListener(OriginsCompat::onIncomingDamage);
        bus.addListener(eteil.csextras.sand.Sandstorms::onEntityTick);
        bus.addListener(eteil.csextras.sand.Sandstorms::onPotentialSpawns);
        bus.addListener(eteil.csextras.sand.Sandstorms::onPositionCheck);
        bus.addListener((RegisterCommandsEvent e) -> SelfTest.register(e.getDispatcher()));

        if (FMLEnvironment.dist.isClient()) {
            eteil.csextras.client.Heartbeat.init();
            bus.addListener(Foods::onTooltip);
        }
    }

    private static void onRegisterModifiers(TempModifierRegisterEvent event) {
        event.register(id("sweat"), SweatTempModifier::new);
    }

    /** Hides configured items (by default Convenient Decor's plastic shovels) from every creative tab. */
    private static void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
        Set<ResourceLocation> hidden = new HashSet<>();
        for (String s : Config.HIDDEN_ITEMS.get()) {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) hidden.add(rl);
        }
        if (hidden.isEmpty()) return;
        var toRemove = new java.util.LinkedHashMap<net.minecraft.world.item.Item, ItemStack>();
        for (var set : java.util.List.of(event.getParentEntries(), event.getSearchEntries())) {
            for (ItemStack stack : set) {
                if (hidden.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) toRemove.putIfAbsent(stack.getItem(), stack);
            }
        }
        for (ItemStack stack : toRemove.values()) event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
