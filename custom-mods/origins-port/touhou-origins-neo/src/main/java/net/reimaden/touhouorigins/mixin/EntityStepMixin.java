package net.reimaden.touhouorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.reimaden.touhouorigins.power.ModifyFootstepSoundPower;
import net.reimaden.touhouorigins.power.SimplePowers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.overgrown.apoli.power.PowerLookup;

/**
 * Two unrelated Voile powers that happen to live on the same class:
 * {@code modify_footstep_sound} and {@code prevent_sprinting_particles}.
 */
@Mixin(Entity.class)
public abstract class EntityStepMixin {

    /**
     * Source-scoped footstep replacement. Fires on the entity actually taking the step, so unlike
     * a sound-id filter it cannot catch other entities' footsteps.
     */
    @Inject(method = "playStepSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At("HEAD"), cancellable = true)
    private void touhouorigins$modifyFootstepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (ModifyFootstepSoundPower.handleStep((Entity) (Object) this)) {
            ci.cancel();   // silenced, or a replacement was already played
        }
    }

    /**
     * Snow layers, carpets and other {@code COMBINATION_STEP_SOUND_BLOCKS} never reach
     * {@code playStepSound} — vanilla routes them here instead, playing the thin block's sound plus a
     * muffled one for the block beneath. Cancelling here covers both, since
     * {@code playMuffledStepSound} is called from inside this method.
     *
     * <p><b>NeoForge patches this signature.</b> Vanilla takes two {@code BlockState}s; NeoForge
     * 21.1.248 appends the two {@code BlockPos}es. Reading the descriptor from a stray 21.1.125 jar
     * produced a mixin that compiled and then failed to apply — always check
     * {@code build/moddev/artifacts/neoforge-<version>.jar}, not another cached copy.
     */
    @Inject(method = "playCombinationStepSounds(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V",
        at = @At("HEAD"), cancellable = true)
    private void touhouorigins$modifyCombinationFootstepSound(BlockState primary, BlockState secondary,
                                                              BlockPos primaryPos, BlockPos secondaryPos,
                                                              CallbackInfo ci) {
        if (ModifyFootstepSoundPower.handleStep((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @ModifyReturnValue(method = "canSpawnSprintParticle()Z", at = @At("TAIL"))
    private boolean touhouorigins$preventSprintingParticles(boolean original) {
        if (PowerLookup.hasActive((Entity) (Object) this, SimplePowers.PREVENT_SPRINTING_PARTICLES)) {
            return false;
        }
        return original;
    }
}
