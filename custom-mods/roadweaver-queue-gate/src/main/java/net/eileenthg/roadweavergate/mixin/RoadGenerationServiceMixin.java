package net.eileenthg.roadweavergate.mixin;

import net.eileenthg.roadweavergate.QueueGate;
import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.generation.RoadGenerationService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RoadGenerationService.tick calls refreshQueue every server tick. refreshQueue reads the whole structure
 * state file from disk and parses it with Gson, only to queue connections that are PLANNED or GENERATING.
 * Skip it unless something could have changed. The rest of tick(), which dispatches already-queued work, still runs.
 */
@Mixin(value = RoadGenerationService.class, remap = false)
public abstract class RoadGenerationServiceMixin {

    @Inject(method = "refreshQueue", at = @At("HEAD"), cancellable = true)
    private static void roadweavergate$gate(ServerLevel level, CallbackInfo ci) {
        if (!QueueGate.shouldRefresh(level)) ci.cancel();
    }
}
