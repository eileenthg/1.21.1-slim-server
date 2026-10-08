package net.eileenthg.colonyexpeditions.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker;
import net.eileenthg.colonyexpeditions.ExpeditionConfig;
import net.eileenthg.colonyexpeditions.core.BuildingExpedition;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Netherworker trip limits come from the config, and the hut keeps Dimensional Tears in stock. */
@Mixin(value = BuildingNetherWorker.class, remap = false)
public abstract class BuildingNetherWorkerMixin
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void colonyExpeditions$keepTears(final IColony colony, final BlockPos pos, final CallbackInfo ci)
    {
        if (!((Object) this instanceof BuildingExpedition))
        {
            ((AbstractBuildingContainerAccessor) this).colonyExpeditions$getKeepX()
              .put(stack -> stack.is(ExpeditionConfig.netherOpener()), new Tuple<>(16, true));
        }
    }

    @Inject(method = "getMaxPerPeriod", at = @At("HEAD"), cancellable = true)
    private static void colonyExpeditions$tripsPerPeriod(final CallbackInfoReturnable<Integer> cir)
    {
        cir.setReturnValue(ExpeditionConfig.NETHER.tripsPerPeriod.get());
    }

    @Inject(method = "getPeriodDays", at = @At("HEAD"), cancellable = true)
    private static void colonyExpeditions$periodDays(final CallbackInfoReturnable<Integer> cir)
    {
        cir.setReturnValue(ExpeditionConfig.NETHER.periodDays.get());
    }
}
