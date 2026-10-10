package eteil.mcarpgbridge.mixin;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mebahel 3.2.0 holds back ambient dialogue and conversations while an MCA screen is open, but it looks for
 * screen classes under "net.mca."; MCA Reborn 7.7 ships them under "net.conczin.mca.", so the check never
 * matches here and villagers talk over MCA's menus. Report conczin screens as MCA screens too.
 * require = 0: on a Mebahel build without this class, nothing is injected and the rest of the bridge is
 * unaffected.
 */
@Pseudo
@Mixin(targets = "net.mebahelrpgrquest.client.dialogue.ClientMcaInteractionState", remap = false)
public abstract class MebahelMcaScreenMixin {
    @Inject(method = "isMcaScreen", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void mcarpgbridge$alsoConczinScreens(Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (screen != null && screen.getClass().getName().startsWith("net.conczin.mca.")) {
            cir.setReturnValue(true);
        }
    }
}
