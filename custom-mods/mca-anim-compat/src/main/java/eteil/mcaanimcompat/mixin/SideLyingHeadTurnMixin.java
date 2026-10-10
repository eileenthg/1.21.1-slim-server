package eteil.mcaanimcompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import eteil.mcaanimcompat.LyingCamera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lying on the side, the head faces sideways, 90° from where the body points. Vanilla lets the head get only
 * 50° ahead before the body follows, which would swing the whole lying body 40° every time. So in a side
 * pose the limit is raised (sideHeadTurnDegrees); every other pose keeps vanilla's 50°. Client-side players
 * only: that is where a player's body yaw is worked out for drawing.
 * <p>
 * Hooked where tickHeadTurn reads the limit, rather than on Player's override of it: in this pack a hook on
 * the override was never reached.
 */
@Mixin(LivingEntity.class)
public abstract class SideLyingHeadTurnMixin {
    @ModifyExpressionValue(method = "tickHeadTurn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getMaxHeadRotationRelativeToBody()F"))
    private float mcaanimcompat$sideLyingHeadTurn(float original) {
        return (Object) this instanceof AbstractClientPlayer player ? LyingCamera.maxHeadTurn(player, original) : original;
    }
}
