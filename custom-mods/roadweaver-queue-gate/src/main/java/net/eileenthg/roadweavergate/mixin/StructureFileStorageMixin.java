package net.eileenthg.roadweavergate.mixin;

import net.eileenthg.roadweavergate.QueueGate;
import net.shiroha233.roadweaver.persistence.files.StructureFileStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every change to the connection list goes through these two methods. That covers dynamic planning around
 * players, initial generation, status updates (PLANNED, GENERATING, COMPLETED) and the public RoadNetworkApi.
 */
@Mixin(value = StructureFileStorage.class, remap = false)
public abstract class StructureFileStorageMixin {

    @Inject(method = "setStructureConnections", at = @At("TAIL"))
    private static void roadweavergate$afterSet(CallbackInfo ci) {
        QueueGate.markDirty("connections saved");
    }

    @Inject(method = "mergeStructureConnections", at = @At("TAIL"))
    private static void roadweavergate$afterMerge(CallbackInfo ci) {
        QueueGate.markDirty("connections merged");
    }
}
