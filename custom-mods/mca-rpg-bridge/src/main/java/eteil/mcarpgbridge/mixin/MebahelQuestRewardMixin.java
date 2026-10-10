package eteil.mcarpgbridge.mixin;

import eteil.mcarpgbridge.ReputationDeeds;
import net.mebahelrpgrquest.quest.PlayerQuestProgress;
import net.mebahelrpgrquest.quest.QuestDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** rewardQuestGiver runs once per claimed villager quest, on both the normal and the spared ending. */
@Pseudo
@Mixin(targets = "net.mebahelrpgrquest.network.QuestDialogueNetworking", remap = false)
public abstract class MebahelQuestRewardMixin {
    @Inject(method = "rewardQuestGiver", at = @At("HEAD"), remap = false)
    private static void mcarpgbridge$recordDeed(ServerPlayer player, LivingEntity giver, QuestDefinition quest,
                                                PlayerQuestProgress progress, CallbackInfo ci) {
        ReputationDeeds.questClaimed(player, giver, quest, progress);
    }
}
