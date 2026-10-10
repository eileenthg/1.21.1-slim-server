package eteil.spawnonboarding.mixin;

import eteil.spawnonboarding.OnboardingController;
import net.conczin.mca.network.c2s.DestinyMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MCA 7.7.36: the destiny screen sends DestinyMessage(location, false) when a start location is
 * picked (MCA teleports then) and DestinyMessage("", true) when the screen is closed.
 */
@Mixin(value = DestinyMessage.class, remap = false)
public abstract class DestinyMessageMixin {
    @Shadow
    public abstract boolean isClosing();

    @Inject(method = "handle(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"))
    private void spawnonboarding$onHandle(Player player, CallbackInfo ci) {
        if (isClosing() && player instanceof ServerPlayer serverPlayer) {
            OnboardingController.onDestinyClosed(serverPlayer);
        }
    }

    @Inject(method = "handleBlockPos(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;)V", at = @At("TAIL"))
    private void spawnonboarding$onTeleport(ServerPlayer player, BlockPos pos, CallbackInfo ci) {
        OnboardingController.onDestinyTeleport(player, pos);
    }
}
