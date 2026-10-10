package com.github.eileen.workingmaidcomputers;

import com.craisinlord.antos.AntOS;
import com.craisinlord.antos.config.AntOSSettings;
import com.craisinlord.antos.content.AntOSObjects;
import com.craisinlord.antos.content.antazon.AntazonData;
import com.craisinlord.antos.content.antazon.AntazonSellData;
import com.craisinlord.antos.content.antmail.AntmailEventData;
import com.craisinlord.antos.content.antmail.AntmailServerData;
import com.craisinlord.antos.content.computer.TaskDebugCommands;
import com.craisinlord.antos.content.computer.blockle.BlockleAnswers;
import com.craisinlord.antos.content.entity.RewardDropEntity;
import com.craisinlord.antos.content.guide.ComputerGuideData;
import com.craisinlord.antos.content.item.FloppyDiskItem;
import com.craisinlord.antos.content.network.AnternetAccountHandler;
import com.craisinlord.antos.content.network.AnternetAccountPayload;
import com.craisinlord.antos.content.network.AnternetAccountResultPayload;
import com.craisinlord.antos.content.network.AnternetComputerPayload;
import com.craisinlord.antos.content.network.AnternetComputerResultPayload;
import com.craisinlord.antos.content.network.AntmailAnternetPayload;
import com.craisinlord.antos.content.network.AntmailAnternetResultPayload;
import com.craisinlord.antos.content.network.AntmailServerHandler;
import com.craisinlord.antos.content.network.ComputerAccessHandler;
import com.craisinlord.antos.content.network.FloppyTextureSync;
import com.craisinlord.antos.content.network.FloppyTextureSyncPayload;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * Hosts the vendored AntOS computer (package {@code com.craisinlord.antos}) as an addon to Touhou Little Maid.
 * <p>
 * Nothing here touches TLM's classes: its computer block and seat entity are recognised by registry name, see
 * {@link MaidComputerSeat}. AntOS keeps its own {@code antos} namespace for registry entries, data and assets.
 */
@Mod(WorkingMaidComputers.MOD_ID)
public final class WorkingMaidComputers {
    public static final String MOD_ID = "workingmaidcomputers";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AntOS.MODID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AntOS.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, AntOS.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> FLOPPY_DISK_COMPONENT = COMPONENTS.register("floppy_disk",
            () -> DataComponentType.<ResourceLocation>builder().persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC).build());
    public static final DeferredHolder<Item, Item> FLOPPY_DISK = ITEMS.register("floppy_disk", () -> new FloppyDiskItem(new Item.Properties()));
    public static final DeferredHolder<EntityType<?>, EntityType<RewardDropEntity>> REWARD_DROP_ENTITY = ENTITY_TYPES.register("reward_drop",
            () -> EntityType.Builder.of(RewardDropEntity::new, MobCategory.MISC).sized(0.8F, 0.8F).clientTrackingRange(8).updateInterval(2).build("antos:reward_drop"));

    public WorkingMaidComputers(IEventBus modBus) {
        AntOSSettings.load(FMLPaths.CONFIGDIR.get().resolve("antos.json"));
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        ENTITY_TYPES.register(modBus);
        AntOSObjects.bind(FLOPPY_DISK::get, FLOPPY_DISK_COMPONENT::get, REWARD_DROP_ENTITY::get);
        modBus.addListener(WorkingMaidComputers::addCreativeItems);
        modBus.addListener(WorkingMaidComputers::registerPayloads);
        NeoForge.EVENT_BUS.addListener(WorkingMaidComputers::registerReloadListeners);
        NeoForge.EVENT_BUS.addListener(WorkingMaidComputers::registerCommands);
        NeoForge.EVENT_BUS.addListener(WorkingMaidComputers::onServerTick);
        NeoForge.EVENT_BUS.addListener(WorkingMaidComputers::onPlayerLogout);
        LOGGER.info("Working Maid Computers: AntOS hosted for Touhou Little Maid computers");
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(FLOPPY_DISK.get());
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        TaskDebugCommands.register(event.getDispatcher());
    }

    private static void registerReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ComputerGuideData.instance());
        event.addListener(BlockleAnswers.instance());
        event.addListener(AntmailEventData.instance());
        event.addListener(AntazonData.instance());
        event.addListener(AntazonSellData.instance());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        FloppyTextureSync.tick(server);
        AntmailServerData.access(server).drainAvailable(server);
        AntmailEventData.onTick(server);
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AnternetAccountHandler.disconnect(player);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(AnternetComputerPayload.TYPE, AnternetComputerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> withPlayer(context, player -> ComputerAccessHandler.handleAnternet(player, payload))));
        registrar.playToServer(AnternetAccountPayload.TYPE, AnternetAccountPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> withPlayer(context, player -> AnternetAccountHandler.handle(player, payload))));
        registrar.playToServer(AntmailAnternetPayload.TYPE, AntmailAnternetPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> withPlayer(context, player -> AntmailServerHandler.handle(player, payload))));
        registrar.playToClient(AnternetComputerResultPayload.TYPE, AnternetComputerResultPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.craisinlord.antos.content.client.ComputerAccessClientState.updateAnternet(payload)));
        registrar.playToClient(AnternetAccountResultPayload.TYPE, AnternetAccountResultPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.craisinlord.antos.content.client.AnternetAccountClientState.update(payload)));
        registrar.playToClient(AntmailAnternetResultPayload.TYPE, AntmailAnternetResultPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.craisinlord.antos.content.client.AntmailClientState.update(payload)));
        registrar.playToClient(FloppyTextureSyncPayload.TYPE, FloppyTextureSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> com.craisinlord.antos.content.client.FloppyTextureClientState.update(payload)));

        ComputerAccessHandler.setAnternetResultSender((player, payload) -> PacketDistributor.sendToPlayer(player, payload));
        AnternetAccountHandler.setResultSender((player, payload) -> PacketDistributor.sendToPlayer(player, payload));
        AntmailServerHandler.setResultSender((player, payload) -> PacketDistributor.sendToPlayer(player, payload));
        FloppyTextureSync.setSender((player, payload) -> PacketDistributor.sendToPlayer(player, payload));
    }

    private static void withPlayer(IPayloadContext context, Consumer<ServerPlayer> action) {
        if (context.player() instanceof ServerPlayer player) {
            action.accept(player);
        }
    }
}
