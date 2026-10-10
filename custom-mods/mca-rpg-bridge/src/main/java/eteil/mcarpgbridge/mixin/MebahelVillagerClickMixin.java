package eteil.mcarpgbridge.mixin;

import eteil.mcarpgbridge.BridgeConfig;
import eteil.mcarpgbridge.MebahelInteraction;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mebahel answers a right-click on any villager that holds one of its quests or rumors, which on an MCA
 * villager swallows the click before MCA opens its menu (and with it the MCA: Quests button). Hand MCA
 * villagers back to MCA; the menu's Request/Rumor button replays the click through MebahelInteraction.
 * Runs on both sides, so the client does not swing or cancel either.
 *
 * <p>Both known lambda numbers are listed because the number is positional (2.0.6 = $2, 3.2.0 = $3) and
 * only one exists per version; require = 0 keeps a future rename from crashing the game, and
 * {@link MebahelInteraction#verifyClickGuard()} logs a warning when nothing could have been injected.
 * Every lambda under this name is a UseEntityCallback of Mebahel's own registration, so hitting an extra
 * one on some version only means an MCA villager skips that callback's bookkeeping.
 */
@Pseudo
@Mixin(targets = "net.mebahelrpgrquest.quest.QuestEvents", remap = false)
public abstract class MebahelVillagerClickMixin {
    @Inject(method = {MebahelInteraction.HANDLER_206, MebahelInteraction.HANDLER_320},
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void mcarpgbridge$leaveMcaVillagersToMca(Player player, Level level, InteractionHand hand, Entity entity,
                                                            EntityHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (entity instanceof VillagerEntityMCA && BridgeConfig.mcaMenuButton() && !MebahelInteraction.fromMcaMenu()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
