package net.reimaden.touhouorigins.mixin;

import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.reimaden.touhouorigins.power.SimplePowers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code touhouorigins:prevent_flying_kick} — stops the server disconnecting a player for
 * "flying is not enabled on this server".
 *
 * <p>The server counts consecutive ticks a player spends airborne without permission
 * ({@code aboveGroundTickCount}, and the vehicle equivalent) and kicks past
 * {@code MAXIMUM_FLYING_TICKS}. Powers that grant unusual movement trip this. Zeroing the counters
 * each tick while the power is active keeps the check intact for everyone else, rather than
 * disabling it outright.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;
    @Shadow private int aboveGroundTickCount;
    @Shadow private int aboveGroundVehicleTickCount;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void touhouorigins$preventFlyingKick(CallbackInfo ci) {
        if (this.player == null) return;
        if (!PowerLookup.hasActive(this.player, SimplePowers.PREVENT_FLYING_KICK)) return;
        this.aboveGroundTickCount = 0;
        this.aboveGroundVehicleTickCount = 0;
    }
}
