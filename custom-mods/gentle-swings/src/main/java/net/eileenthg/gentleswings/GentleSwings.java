package net.eileenthg.gentleswings;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Gentle Swings: a per-player toggle that makes melee swings pass through NPCs, animals and
 * players.
 *
 * <p>The server owns the state (a saved player attachment) and enforces it: attacks on a spared
 * entity are cancelled, and so is sweep damage, which vanilla deals without an attack event.
 * The client mirrors the state so its swings can look past a spared entity to the block behind
 * it (see {@code MinecraftMixin}); that is what lets you mine the grass at a villager's feet.
 */
@Mod(GentleSwings.MOD_ID)
public final class GentleSwings {

    public static final String MOD_ID = "gentleswings";

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    /** True while this player's swings pass through spared entities. Survives death and relogs. */
    public static final Supplier<AttachmentType<Boolean>> GENTLE = ATTACHMENTS.register("gentle",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    public GentleSwings(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SwingConfig.SERVER_SPEC);
        ATTACHMENTS.register(modBus);
        modBus.addListener(GentleSwings::registerPayloads);

        NeoForge.EVENT_BUS.addListener(GentleSwings::onAttack);
        NeoForge.EVENT_BUS.addListener(GentleSwings::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) sync(sp);
        });
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(TogglePayload.TYPE, TogglePayload.STREAM_CODEC,
                        (payload, ctx) -> {
                            if (ctx.player() instanceof ServerPlayer sp) toggle(sp);
                        })
                // The handler body names a client class, so it is only resolved on a client.
                .playToClient(StatePayload.TYPE, StatePayload.STREAM_CODEC,
                        (payload, ctx) -> {
                            if (FMLEnvironment.dist.isClient()) {
                                net.eileenthg.gentleswings.client.ClientState.gentle = payload.gentle();
                            }
                        });
    }

    public static boolean isGentle(Player player) {
        return player.getData(GENTLE);
    }

    private static void toggle(ServerPlayer player) {
        boolean now = !isGentle(player);
        player.setData(GENTLE, now);
        sync(player);
        player.displayClientMessage(
                Component.translatable(now ? "gentleswings.state.on" : "gentleswings.state.off"), true);
    }

    private static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new StatePayload(isGentle(player)));
    }

    private static void onAttack(AttackEntityEvent event) {
        if (isGentle(event.getEntity()) && SwingConfig.spares(event.getTarget())) {
            event.setCanceled(true);
        }
    }

    /**
     * Sweep attacks hurt nearby mobs directly, with no attack event, so a gentle player sweeping
     * a zombie would still clip the villager beside it. Only melee from the player themself is
     * stopped; arrows, thorns and the like are left alone.
     */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.PLAYER_ATTACK)
                && source.getEntity() instanceof Player player
                && source.getDirectEntity() == player
                && isGentle(player)
                && SwingConfig.spares(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
