package eteil.mcaanimcompat.mixin;

import eteil.mcaanimcompat.CarryServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/** Every server-side player teleport (waystones, commands, pearls, dimension changes) ends here. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerTeleportMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("TAIL"))
    private void mcaanimcompat$carryFollows(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relative,
                                           CallbackInfo ci) {
        CarryServer.onTeleported(player);
    }
}
