package net.eileenthg.colonyexpeditions.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.items.component.AdventureData;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkNether;
import net.eileenthg.colonyexpeditions.ExpeditionConfig;
import net.eileenthg.colonyexpeditions.core.EntityAIExpedition;
import net.eileenthg.colonyexpeditions.core.PortalKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two changes to the Netherworker AI:
 * <ul>
 *   <li>The Netherworker lights its portal with Dimensional Tears instead of flint and steel.</li>
 *   <li>For EntityAIExpedition, every nether-specific step is routed to the destination instead.</li>
 * </ul>
 */
@Mixin(value = EntityAIWorkNether.class, remap = false)
public abstract class EntityAIWorkNetherMixin
{
    private EntityAIExpedition expedition()
    {
        return (Object) this instanceof EntityAIExpedition e ? e : null;
    }

    private ExpeditionConfig.Trips tripConfig()
    {
        final EntityAIExpedition e = expedition();
        return e == null ? ExpeditionConfig.NETHER : ExpeditionConfig.trips(e.destination());
    }

    /** Expeditions check for their own veil wherever the Netherworker checks for a nether portal block. */
    @WrapOperation(
      method = {"decide", "leaveForNether", "returnFromNether", "openPortal", "closePortal"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean colonyExpeditions$portalBlock(final BlockState state, final Block block, final Operation<Boolean> original)
    {
        final EntityAIExpedition e = expedition();
        if (e != null && block == Blocks.NETHER_PORTAL)
        {
            return state.is(e.portalBlock());
        }
        return original.call(state, block);
    }

    /** The flint and steel check becomes a portal key check (tears for the Netherworker). */
    @WrapOperation(
      method = "decide",
      at = @At(value = "INVOKE",
        target = "Lcom/minecolonies/core/entity/ai/workers/production/EntityAIWorkNether;checkForToolOrWeapon(Lcom/minecolonies/api/equipment/registry/EquipmentTypeEntry;)Z"))
    private boolean colonyExpeditions$portalKey(final EntityAIWorkNether ai, final EquipmentTypeEntry type, final Operation<Boolean> original)
    {
        final EntityAIExpedition e = expedition();
        if (type != ModEquipmentTypes.flint_and_steel.get())
        {
            // Hold out for the configured tool tier; the check also requests that tier from the colony.
            final int minLevel = tripConfig().minToolLevel.get();
            return ((AbstractEntityAIBasicInvoker) ai).colonyExpeditions$checkForToolOrWeapon(type, minLevel);
        }
        // The last of the tool checks: also gate on armour and on the portal key.
        if (e != null)
        {
            return e.isArmorMissing() | e.isKeyMissing();
        }
        return PortalKeys.missing(ai, ExpeditionConfig.netherOpener(), ExpeditionConfig.NETHER_OPENER_PER_TRIP.get());
    }

    /** Flint and steel is never used; keys are used up when the portal opens. */
    @Inject(method = "useFlintAndSteel", at = @At("HEAD"), cancellable = true)
    private void colonyExpeditions$noFlint(final CallbackInfo ci)
    {
        ci.cancel();
    }

    @Inject(method = "openPortal", at = @At("HEAD"), cancellable = true)
    private void colonyExpeditions$openExpeditionPortal(final CallbackInfoReturnable<IAIState> cir)
    {
        final EntityAIExpedition e = expedition();
        if (e != null)
        {
            cir.setReturnValue(e.openExpeditionPortal());
        }
    }

    /** The Netherworker's portal only lights if it can use up its tears. */
    @WrapOperation(method = "openPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/portal/PortalShape;createPortalBlocks()V"))
    private void colonyExpeditions$lightWithTears(final PortalShape shape, final Operation<Void> original)
    {
        final var worker = ((AbstractAISkeletonAccessor) this).colonyExpeditions$getWorker();
        if (PortalKeys.consume(worker, ExpeditionConfig.netherOpener(), ExpeditionConfig.NETHER_OPENER_PER_TRIP.get()))
        {
            original.call(shape);
        }
    }

    @WrapOperation(
      method = "closePortal",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean colonyExpeditions$closeExpeditionPortal(final ServerLevel level, final BlockPos pos, final BlockState state, final Operation<Boolean> original)
    {
        final EntityAIExpedition e = expedition();
        if (e != null)
        {
            e.clearPortal(pos);
            return true;
        }
        return original.call(level, pos, state);
    }

    @WrapOperation(
      method = "stayInNether",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;source(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource colonyExpeditions$damageType(final DamageSources sources, final ResourceKey<DamageType> key, final Operation<DamageSource> original)
    {
        final EntityAIExpedition e = expedition();
        return original.call(sources, e == null ? key : e.destination().damageType());
    }

    @WrapOperation(method = "stayInNether", at = @At(value = "INVOKE", target = "Lcom/minecolonies/api/items/component/AdventureData;damage()F"))
    private float colonyExpeditions$damageMultiplier(final AdventureData data, final Operation<Float> original)
    {
        return (float) (original.call(data) * tripConfig().damageMultiplier.get());
    }

    @ModifyArg(
      method = "goToVault",
      at = @At(value = "INVOKE",
        target = "Lcom/minecolonies/api/colony/managers/interfaces/ITravellingManager;startTravellingTo(Lcom/minecolonies/api/colony/ICitizenData;Lnet/minecraft/core/BlockPos;I)V"),
      index = 2)
    private int colonyExpeditions$tripTime(final int ticks)
    {
        return Math.max(20, (int) Math.round(ticks * tripConfig().tripTimeMultiplier.get()));
    }
}
