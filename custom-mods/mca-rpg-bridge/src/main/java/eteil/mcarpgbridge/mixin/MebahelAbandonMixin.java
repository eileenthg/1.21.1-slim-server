package eteil.mcarpgbridge.mixin;

import eteil.mcarpgbridge.ReputationDeeds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** handleAbandonQuest is the journal's Abandon button; it runs before Mebahel marks the quest failed. */
@Pseudo
@Mixin(targets = "net.mebahelrpgrquest.network.QuestNetworking", remap = false)
public abstract class MebahelAbandonMixin {
    @Inject(method = "handleAbandonQuest", at = @At("HEAD"), remap = false)
    private static void mcarpgbridge$recordDeed(ServerPlayer player, UUID giverId, ResourceLocation questId, CallbackInfo ci) {
        if (player != null && giverId != null && questId != null) {
            ReputationDeeds.questAbandoned(player, giverId, questId);
        }
    }
}
