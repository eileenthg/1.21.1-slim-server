package eteil.drinkfix.mixin.compat;

import com.lance5057.extradelight.workstations.mixingbowl.recipes.MixingBowlRecipe;
import com.lance5057.extradelight.workstations.mixingbowl.recipes.MixingBowlRecipeWrapper;
import eteil.drinkfix.ContainerCheck;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Slot 9 of the bowl holds the container; the recipe only checked that it was the same item. */
@Mixin(value = MixingBowlRecipe.class, remap = false)
public abstract class MixingBowlRecipeMixin {
    @Inject(method = "matches(Lcom/lance5057/extradelight/workstations/mixingbowl/recipes/MixingBowlRecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"), cancellable = true)
    private void drinkfix$checkContainer(MixingBowlRecipeWrapper input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        MixingBowlRecipe self = (MixingBowlRecipe) (Object) this;
        if (!ContainerCheck.accepts(self.getContainer(), input.getItem(9))) cir.setReturnValue(false);
    }
}
