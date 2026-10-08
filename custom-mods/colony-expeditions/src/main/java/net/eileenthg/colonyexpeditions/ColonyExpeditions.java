package net.eileenthg.colonyexpeditions;

import com.minecolonies.api.sounds.ModSoundEvents;
import com.minecolonies.api.tileentities.MinecoloniesTileEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ColonyExpeditions.MOD_ID)
public final class ColonyExpeditions
{
    public static final String MOD_ID = "colony_expeditions";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public ColonyExpeditions(final IEventBus modBus, final ModContainer container)
    {
        ModContent.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, ExpeditionConfig.SPEC);
        modBus.addListener(ColonyExpeditions::addHutsToBuildingEntity);
        modBus.addListener(ColonyExpeditions::commonSetup);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> ExpeditionTestCommand.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(ExpeditionTestCommand::onServerTick);
        NeoForge.EVENT_BUS.addListener(ExpeditionTestCommand::onDeath);
        NeoForge.EVENT_BUS.addListener(ExpeditionTestCommand::onLeave);
    }

    public static ResourceLocation id(final String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * MineColonies looks citizen voice lines up by job name and only registers its own jobs. A level-up during a trip
     * plays one, and the missing entry crashed the Netherworker's loot hand-off halfway, so the loot was handed over
     * again on the next tick. Expedition workers use the Netherworker's voice set.
     */
    private static void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            final var netherworker = ModSoundEvents.CITIZEN_SOUND_EVENTS.get("netherworker");
            for (final Destination dest : Destination.values())
            {
                ModSoundEvents.CITIZEN_SOUND_EVENTS.putIfAbsent(dest.workName(), netherworker);
            }
        });
    }

    /** MineColonies' colony building block entity only accepts its own huts unless told otherwise. */
    private static void addHutsToBuildingEntity(final BlockEntityTypeAddBlocksEvent event)
    {
        final Block[] huts = ModContent.HUTS.values().stream().map(hut -> (Block) hut.get()).toArray(Block[]::new);
        event.modify((BlockEntityType<?>) MinecoloniesTileEntities.BUILDING.get(), huts);
    }
}
