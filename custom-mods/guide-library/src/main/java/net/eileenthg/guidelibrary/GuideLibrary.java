package net.eileenthg.guidelibrary;

import com.mojang.logging.LogUtils;
import net.eileenthg.guidelibrary.network.OpenItemBookPayload;
import net.eileenthg.guidelibrary.network.SyncUnlocksPayload;
import net.eileenthg.guidelibrary.unlock.Unlocks;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

/**
 * Guide Library: one inventory button that lists every guide book in the pack.
 *
 * <p>Patchouli and Modonomicon books are discovered from their own registries and opened
 * client-side. Item-based books (MCA, Alex's Mobs, ...) are opened by replaying the item's
 * right-click; the few that only open from the server go through {@link OpenItemBookPayload},
 * which the server checks against the same allowlist the client builds its list from.
 */
@Mod(GuideLibrary.MOD_ID)
public final class GuideLibrary {

    public static final String MOD_ID = "guidelibrary";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GuideLibrary(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, LibraryConfig.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, LibraryConfig.CLIENT_SPEC);
        modBus.addListener(GuideLibrary::registerPayloads);
        Unlocks.init(modBus);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> OpenItemBookPayload.forget(e.getEntity().getUUID()));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // optional(): a client with this mod can still join a server without it. Patchouli and
        // Modonomicon books keep working there; only server-opened item books are unavailable.
        event.registrar("1").optional()
                .playToServer(OpenItemBookPayload.TYPE, OpenItemBookPayload.STREAM_CODEC,
                        OpenItemBookPayload::handle)
                // The handler body names a client class, so it is only resolved on a client.
                .playToClient(SyncUnlocksPayload.TYPE, SyncUnlocksPayload.STREAM_CODEC,
                        (payload, ctx) -> {
                            if (FMLEnvironment.dist.isClient()) {
                                net.eileenthg.guidelibrary.client.ClientUnlocks.onSync(payload);
                            }
                        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
