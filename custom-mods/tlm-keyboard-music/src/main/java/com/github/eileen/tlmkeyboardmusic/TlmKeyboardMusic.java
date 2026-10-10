package com.github.eileen.tlmkeyboardmusic;

import com.github.eileen.tlmkeyboardmusic.client.KeyboardMusicClient;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Plays music when a maid, or a player, sits at Touhou Little Maid's keyboard.
 * <p>
 * An addon rather than a fork: nothing here touches TLM's classes. The keyboard, its seat and the maid
 * are recognised by registry name, see {@link KeyboardSeats}.
 */
@Mod(TlmKeyboardMusic.MOD_ID)
public class TlmKeyboardMusic {
    public static final String MOD_ID = "tlmkeyboardmusic";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /** Matches the jukebox-ish reach of a keyboard: loud in the room, gone a couple of chunks away. */
    private static final float MUSIC_RANGE = 24.0F;
    /** The pieces shipped with the mod. Datapacks add more; see KeyboardMusicRegistries. */
    private static final List<String> BUILT_IN_TRACKS = List.of("gymnopedie_no1", "sweden", "wet_hands", "clark");

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    static {
        // Registered in code so sounds.json can map them to audio files and give them subtitles. The
        // datapack registry decides which of them actually get played.
        BUILT_IN_TRACKS.forEach(track -> SOUNDS.register("keyboard." + track, () -> SoundEvent.createFixedRangeEvent(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "keyboard." + track), MUSIC_RANGE)));
    }

    public TlmKeyboardMusic(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
        modEventBus.addListener(KeyboardMusicRegistries::onNewDataPackRegistry);
        modEventBus.addListener(TlmKeyboardMusic::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(PlayKeyboardMusicPayload.TYPE, PlayKeyboardMusicPayload.STREAM_CODEC, KeyboardMusicClient::handle);
    }

    @EventBusSubscriber(modid = MOD_ID)
    public static final class Handler {
        /** Reported once at startup so pack authors can see whether their additions were picked up. */
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            event.getServer().registryAccess().registry(KeyboardMusicRegistries.KEYBOARD_TRACKS).ifPresent(
                    registry -> LOGGER.info("Loaded {} keyboard track(s): {}", registry.size(),
                            registry.keySet().stream().map(ResourceLocation::toString).sorted().toList()));
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            SeatMusicManager.clear();
        }

        @SubscribeEvent
        public static void onEntityTick(EntityTickEvent.Post event) {
            Entity entity = event.getEntity();
            if (entity.level() instanceof ServerLevel level && KeyboardSeats.isKeyboardSeat(entity, level)) {
                SeatMusicManager.tickSeat(level, entity);
            }
        }

        @SubscribeEvent
        public static void onStartTracking(PlayerEvent.StartTracking event) {
            if (event.getEntity() instanceof ServerPlayer viewer && viewer.level() instanceof ServerLevel level
                    && KeyboardSeats.isKeyboardSeat(event.getTarget(), level)) {
                SeatMusicManager.resend(level, event.getTarget(), viewer);
            }
        }

        /**
         * A player already sitting on the keyboard right-clicks it. TLM does nothing in that case, so
         * handling it here loses nothing; both sides cancel so the arm swings and the click is not
         * passed on to anything else.
         */
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            Player player = event.getEntity();
            Entity vehicle = player.getVehicle();
            if (vehicle == null || event.getHand() != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()
                    || !KeyboardSeats.isKeyboardSeat(vehicle, event.getLevel())
                    || !event.getPos().equals(vehicle.blockPosition())) {
                return;
            }
            if (event.getLevel() instanceof ServerLevel level) {
                SeatMusicManager.playerPlay(level, vehicle, player);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
