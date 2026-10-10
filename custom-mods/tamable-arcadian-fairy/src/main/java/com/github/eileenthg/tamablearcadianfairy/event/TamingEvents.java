package com.github.eileenthg.tamablearcadianfairy.event;

import com.github.eileenthg.tamablearcadianfairy.TamableArcadianFairy;
import com.github.eileenthg.tamablearcadianfairy.ai.TamedFairyAi;
import com.github.eileenthg.tamablearcadianfairy.data.ActState;
import com.github.eileenthg.tamablearcadianfairy.data.ModAttachments;
import com.github.eileenthg.tamablearcadianfairy.data.TameData;
import com.github.eileenthg.tamablearcadianfairy.network.OpenFairyScreenPayload;
import com.github.eileenthg.tamablearcadianfairy.util.FairyTaming;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.reimaden.arcadiandream.entity.custom.mob.BaseFairyEntity;

/**
 * Everything the player does by right-clicking a fairy: taming, healing, and opening the settings screen.
 */
@EventBusSubscriber(modid = TamableArcadianFairy.MOD_ID)
public final class TamingEvents {

    private TamingEvents() {}

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof BaseFairyEntity fairy)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        boolean tamed = FairyTaming.tameData(fairy).tamed();

        if (!tamed) {
            if (stack.is(Items.CAKE)) {
                if (!player.level().isClientSide()) {
                    tame(fairy, player, stack);
                }
                succeed(event, player);
            }
            return;
        }

        if (!FairyTaming.isOwner(fairy, player)) return;

        if (stack.is(Items.SUGAR)) {
            if (!player.level().isClientSide()) {
                heal(fairy, player, stack);
            }
            succeed(event, player);
            return;
        }

        if (stack.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenFairyScreenPayload(fairy.getId()));
            }
            succeed(event, player);
        }
    }

    private static void tame(BaseFairyEntity fairy, Player player, ItemStack stack) {
        fairy.setData(ModAttachments.TAME_DATA,
                TameData.of(player.getUUID(), player.getGameProfile().getName()));
        fairy.setData(ModAttachments.ACT_STATE, ActState.DEFAULT.withCenter(fairy.position()));
        TamedFairyAi.apply(fairy);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        spawnParticles(fairy, ParticleTypes.HEART, 12);
        fairy.level().playSound(null, fairy.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.NEUTRAL, 0.5f, 1.6f);
        player.displayClientMessage(
                Component.translatable("message.tamablearcadianfairy.tamed", fairy.getDisplayName()), true);
    }

    private static void heal(BaseFairyEntity fairy, Player player, ItemStack stack) {
        if (fairy.getHealth() >= fairy.getMaxHealth()) return;

        fairy.heal(4.0f);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        spawnParticles(fairy, ParticleTypes.HEART, 5);
    }

    private static void spawnParticles(LivingEntity fairy, ParticleOptions particle, int count) {
        if (fairy.level() instanceof ServerLevel level) {
            Vec3 pos = fairy.position();
            level.sendParticles(particle, pos.x, pos.y + 0.7, pos.z, count, 0.5, 0.5, 0.5, 0.1);
        }
    }

    /** Consumes the interaction so the stack is not also eaten or placed. */
    private static void succeed(PlayerInteractEvent.EntityInteract event, Player player) {
        event.setCanceled(true);
        event.setCancellationResult(player.level().isClientSide()
                ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        player.swing(InteractionHand.MAIN_HAND);
    }
}
