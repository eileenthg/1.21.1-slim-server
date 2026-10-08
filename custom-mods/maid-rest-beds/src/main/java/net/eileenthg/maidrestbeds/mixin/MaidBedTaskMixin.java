package net.eileenthg.maidrestbeds.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidBedTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.eileenthg.maidrestbeds.RestBeds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Love &amp; Loathe replaces findBed from its HEAD, so this wraps the call to it instead: whatever
 * maid bed either mod picks is kept when it has room, and beds and hammocks are only the fallback.
 */
@Mixin(value = MaidBedTask.class, remap = false)
abstract class MaidBedTaskMixin {

    @WrapOperation(method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;)Z",
            at = @At(value = "INVOKE", target = "Lcom/github/tartaricacid/touhoulittlemaid/entity/ai/brain/task/MaidBedTask;findBed(Lnet/minecraft/server/level/ServerLevel;Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos maidrestbeds$fallBackToBeds(MaidBedTask self, ServerLevel level, EntityMaid maid, Operation<BlockPos> original) {
        BlockPos picked = original.call(self, level, maid);
        if (picked != null && RestBeds.isUsableMaidBed(level, picked)) return picked;
        BlockPos fallback = RestBeds.findFallback(level, maid);
        return fallback != null ? fallback : picked;
    }

    /** TLM's own start only lies down in maid beds; this covers the rest and leaves its body to clear the memories. */
    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;J)V",
            at = @At("HEAD"))
    private void maidrestbeds$sleepInBed(ServerLevel level, EntityMaid maid, long gameTime, CallbackInfo ci) {
        if (maid.isSleeping()) return;
        maid.getBrain().getMemory(InitEntities.TARGET_POS.get()).ifPresent(target -> {
            BlockPos pos = target.currentBlockPosition();
            if (RestBeds.isFreeFallback(level, pos)) {
                maid.startSleeping(pos);
                maid.setPos(pos.getX() + 0.5, pos.getY() + 0.5625, pos.getZ() + 0.5);
            }
        });
    }
}
