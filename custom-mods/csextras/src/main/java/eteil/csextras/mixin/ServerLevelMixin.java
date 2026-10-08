package eteil.csextras.mixin;

import eteil.csextras.sand.Sandstorms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sand accumulates on the same per-chunk weather tick vanilla uses for snow layers and cauldrons. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickPrecipitation", at = @At("HEAD"))
    private void csextras$sandstorm(BlockPos pos, CallbackInfo ci) {
        Sandstorms.onPrecipitationTick((ServerLevel) (Object) this, pos);
    }
}
