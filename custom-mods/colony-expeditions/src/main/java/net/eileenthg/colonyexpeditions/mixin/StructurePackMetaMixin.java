package net.eileenthg.colonyexpeditions.mixin;

import com.ldtteam.structurize.storage.StructurePackMeta;
import java.nio.file.Path;
import net.eileenthg.colonyexpeditions.SharedBlueprints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Callers turn a blueprint's file location back into a pack path with getSubPath. For a shared file that location
 * is inside the shared pack, so answer with the path relative to the shared pack (expeditions/...), which every
 * style pack resolves through StructurePacksMixin.
 */
@Mixin(value = StructurePackMeta.class, remap = false)
public abstract class StructurePackMetaMixin
{
    @Inject(method = "getSubPath", at = @At("HEAD"), cancellable = true)
    private void colonyExpeditions$sharedSubPath(final Path subPath, final CallbackInfoReturnable<String> cir)
    {
        final StructurePackMeta shared = SharedBlueprints.pack();
        if (shared != null && (Object) this != shared && SharedBlueprints.isShared(subPath))
        {
            cir.setReturnValue(shared.getSubPath(subPath));
        }
    }
}
