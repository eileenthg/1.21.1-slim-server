package net.eileenthg.colonyexpeditions;

import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.items.ItemBlockHut;
import com.minecolonies.api.research.util.ResearchConstants;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.buildings.modules.AbstractCraftingBuildingModule;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.CraftingWorkerBuildingModule;
import com.minecolonies.core.colony.buildings.modules.ExpeditionLogModule;
import com.minecolonies.core.colony.buildings.modules.SettingsModule;
import com.minecolonies.core.colony.buildings.modules.settings.BoolSetting;
import com.minecolonies.core.colony.buildings.modules.settings.CrafterRecipeSetting;
import com.minecolonies.core.colony.buildings.moduleviews.CraftingModuleView;
import com.minecolonies.core.colony.buildings.moduleviews.ExpeditionLogModuleView;
import com.minecolonies.core.colony.buildings.moduleviews.SettingsModuleView;
import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import com.minecolonies.core.colony.buildings.views.EmptyView;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker;
import com.minecolonies.core.colony.jobs.views.CrafterJobView;
import java.util.EnumMap;
import java.util.Map;
import net.eileenthg.colonyexpeditions.core.BlockHutExpedition;
import net.eileenthg.colonyexpeditions.core.BuildingExpedition;
import net.eileenthg.colonyexpeditions.core.ExpeditionPortalBlock;
import net.eileenthg.colonyexpeditions.core.JobExpedition;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModContent
{
    private static final DeferredRegister.Blocks BLOCK_REG = DeferredRegister.createBlocks(ColonyExpeditions.MOD_ID);
    private static final DeferredRegister.Items ITEM_REG = DeferredRegister.createItems(ColonyExpeditions.MOD_ID);
    private static final DeferredRegister<BuildingEntry> BUILDING_REG = DeferredRegister.create(CommonMinecoloniesAPIImpl.BUILDINGS, ColonyExpeditions.MOD_ID);
    private static final DeferredRegister<JobEntry> JOB_REG = DeferredRegister.create(CommonMinecoloniesAPIImpl.JOBS, ColonyExpeditions.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TAB_REG = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ColonyExpeditions.MOD_ID);

    public static final Map<Destination, DeferredBlock<BlockHutExpedition>> HUTS = new EnumMap<>(Destination.class);
    public static final Map<Destination, DeferredItem<ItemBlockHut>> HUT_ITEMS = new EnumMap<>(Destination.class);
    public static final Map<Destination, DeferredBlock<ExpeditionPortalBlock>> PORTALS = new EnumMap<>(Destination.class);
    public static final Map<Destination, DeferredHolder<BuildingEntry, BuildingEntry>> BUILDINGS = new EnumMap<>(Destination.class);
    public static final Map<Destination, DeferredHolder<JobEntry, JobEntry>> JOBS = new EnumMap<>(Destination.class);

    static
    {
        for (final Destination dest : Destination.values())
        {
            final DeferredBlock<BlockHutExpedition> hut = BLOCK_REG.register(dest.hutName(), () -> BlockHutExpedition.create(dest));
            HUTS.put(dest, hut);
            HUT_ITEMS.put(dest, ITEM_REG.register(dest.hutName(), () -> new ItemBlockHut(hut.get(), new Item.Properties())));

            PORTALS.put(dest, BLOCK_REG.register(dest.portalName(), () -> new ExpeditionPortalBlock(
              BlockBehaviour.Properties.of()
                .noCollission()
                .noOcclusion()
                .noLootTable()
                .strength(-1.0F, 3600000.0F)
                .lightLevel(state -> 11)
                .sound(SoundType.GLASS)
                .pushReaction(PushReaction.BLOCK))));

            JOBS.put(dest, JOB_REG.register(dest.workName(), () -> new JobEntry.Builder()
              .setJobProducer(JobExpedition::new)
              .setJobViewProducer(() -> CrafterJobView::new)
              .setRegistryName(dest.workId())
              .createJobEntry()));

            BUILDINGS.put(dest, BUILDING_REG.register(dest.workName(), () -> buildingEntry(dest)));
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TAB_REG.register("expeditions", () -> CreativeModeTab.builder()
      .title(Component.translatable("itemGroup.colony_expeditions"))
      .icon(() -> new ItemStack(HUT_ITEMS.get(Destination.END).get()))
      .displayItems((params, output) -> HUT_ITEMS.values().forEach(item -> output.accept(item.get())))
      .build());

    private ModContent()
    {
    }

    /**
     * Built during the building registry event, which runs in the same order on client and server. Module producers get
     * network ids from a shared counter, so they must not be created at class load, which can differ between the sides.
     */
    private static BuildingEntry buildingEntry(final Destination dest)
    {
        // Make sure MineColonies' own producers exist first.
        final BuildingEntry.ModuleProducer<?, ?> minStock = BuildingModules.MIN_STOCK;
        final String prefix = dest.workName();

        final BuildingEntry.ModuleProducer<CraftingWorkerBuildingModule, WorkerBuildingModuleView> work = new BuildingEntry.ModuleProducer<>(
          prefix + "_work",
          () -> new CraftingWorkerBuildingModule(JOBS.get(dest).get(), Skill.Adaptability, Skill.Strength, false, b -> 1),
          () -> WorkerBuildingModuleView::new);
        final BuildingEntry.ModuleProducer<BuildingNetherWorker.CraftingModule, CraftingModuleView> craft = new BuildingEntry.ModuleProducer<>(
          prefix + "_craft",
          () -> new BuildingNetherWorker.CraftingModule(JOBS.get(dest).get()),
          () -> CraftingModuleView::new);
        final BuildingEntry.ModuleProducer<SettingsModule, SettingsModuleView> settings = new BuildingEntry.ModuleProducer<>(
          prefix + "_settings",
          () -> new SettingsModule()
                  .with(AbstractCraftingBuildingModule.RECIPE_MODE, new CrafterRecipeSetting())
                  .with(BuildingNetherWorker.CLOSE_PORTAL, new BoolSetting(true))
                  .with(BuildingExpedition.MIN_GEAR, BuildingExpedition.gearSetting()),
          () -> SettingsModuleView::new);
        final BuildingEntry.ModuleProducer<ExpeditionLogModule, ExpeditionLogModuleView> log = new BuildingEntry.ModuleProducer<>(
          prefix + "_expedition",
          () -> new ExpeditionLogModule(ResearchConstants.NETHER_LOG),
          () -> ExpeditionLogModuleView::new);

        return new BuildingEntry.Builder()
                 .setBuildingBlock(HUTS.get(dest).get())
                 .setBuildingProducer((colony, pos) -> new BuildingExpedition(colony, pos, dest))
                 .setBuildingViewProducer(() -> EmptyView::new)
                 .setRegistryName(dest.workId())
                 .addBuildingModuleProducer(work)
                 .addBuildingModuleProducer(craft)
                 .addBuildingModuleProducer(log)
                 .addBuildingModuleProducer(settings)
                 .addBuildingModuleProducer(BuildingModules.NETHERMINER_MENU)
                 .addBuildingModuleProducer(BuildingModules.CRAFT_TASK_VIEW)
                 .addBuildingModuleProducer(minStock)
                 .addBuildingModuleProducer(BuildingModules.STATS_MODULE)
                 .createBuildingEntry();
    }

    public static void register(final IEventBus modBus)
    {
        BLOCK_REG.register(modBus);
        ITEM_REG.register(modBus);
        BUILDING_REG.register(modBus);
        JOB_REG.register(modBus);
        TAB_REG.register(modBus);
    }
}
