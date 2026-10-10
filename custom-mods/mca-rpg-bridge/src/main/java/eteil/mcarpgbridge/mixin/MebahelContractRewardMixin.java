package eteil.mcarpgbridge.mixin;

import eteil.mcarpgbridge.ReputationDeeds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** creditContractReputation runs once per completed quest board contract, next to the board's own shared reputation. */
@Pseudo
@Mixin(targets = "net.mebahelrpgrquest.network.QuestBoardNetworking", remap = false)
public abstract class MebahelContractRewardMixin {
    @Inject(method = "creditContractReputation", at = @At("HEAD"), remap = false)
    private static void mcarpgbridge$recordDeed(ServerPlayer player, ResourceLocation questId, CallbackInfo ci) {
        ReputationDeeds.contractCompleted(player, questId);
    }
}
