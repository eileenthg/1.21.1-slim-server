package eteil.gensokyocompat;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Set;

/**
 * Lets Gensokyo Legacy run beside the Youkai's Homecoming 1.21.1 port and bridges the two:
 * <ul>
 * <li>mixins keep Legacy's magical forest to vanilla dark-forest climate without re-rolling TerraBlender regions,
 *     and the mods.toml turns off the one Lithium optimisation Legacy's access transformer breaks;</li>
 * <li>YH characters Legacy also has are replaced by the Legacy ones where they stand ({@link CharacterSwap});</li>
 * <li>Legacy hands out YH's fairy ice, frozen frogs and hats instead of its own copies, and the dropped
 *     duplicates are hidden from creative tabs and JEI. Data-side retargeting (loot, trades, quests, dialog,
 *     tags, structures) lives in the zz-gensokyo-bridge datapack.</li>
 * </ul>
 */
@Mod(GensokyoCompat.MOD_ID)
public final class GensokyoCompat {
    public static final String MOD_ID = "gensokyocompat";
    public static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    public GensokyoCompat(IEventBus modBus) {
        modBus.addListener(GensokyoCompat::creativeTabs);
        NeoForge.EVENT_BUS.addListener(CharacterSwap::onJoin);
        NeoForge.EVENT_BUS.addListener(CharacterSwap::onTick);
        NeoForge.EVENT_BUS.addListener(GensokyoCompat::commands);
        if (net.neoforged.fml.ModList.get().isLoaded("biolith")) BiolithForest.register();
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) modBus.addListener(GensokyoCompat::clientSetup);
    }

    private static void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        var mods = net.neoforged.fml.ModList.get();
        if (mods.isLoaded("legendarytabs") && mods.isLoaded(Bridge.GL)) event.enqueueWork(eteil.gensokyocompat.client.QuestTab::register);
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        Set<Item> hidden = Set.copyOf(Bridge.hiddenItems());
        for (var entry : java.util.List.copyOf(event.getParentEntries())) {
            if (hidden.contains(entry.getItem())) event.remove(entry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        for (var entry : java.util.List.copyOf(event.getSearchEntries())) {
            if (hidden.contains(entry.getItem())) event.remove(entry, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }

    private static void commands(RegisterCommandsEvent event) {
        SelfTest.register(event.getDispatcher());
    }
}
