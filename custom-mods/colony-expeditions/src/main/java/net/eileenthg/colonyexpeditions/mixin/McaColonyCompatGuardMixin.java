package net.eileenthg.colonyexpeditions.mixin;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MCA Colony Integration 0.1.0-beta deletes a MineColonies citizen whenever its entity leaves the level while
 * {@code !isAlive()}. That is also true for an entity discarded on purpose, which MineColonies does every time a
 * Netherworker (or an expedition worker) walks through its portal, so every trip permanently deleted the worker.
 * Only let the cleanup run for citizens that actually died. Skipped entirely when that mod isn't installed.
 */
@Pseudo
@Mixin(targets = "com.jaden.mcacolonycompat.McaColonyCompat", remap = false)
public abstract class McaColonyCompatGuardMixin
{
    @Inject(method = "onCitizenLeave", at = @At("HEAD"), cancellable = true, require = 0)
    private void colonyExpeditions$onlyDeadCitizens(final EntityLeaveLevelEvent event, final CallbackInfo ci)
    {
        if (event.getEntity() instanceof AbstractEntityCitizen citizen && citizen.getRemovalReason() != Entity.RemovalReason.KILLED)
        {
            ci.cancel();
        }
    }
}
