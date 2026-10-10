package eteil.mcarpgbridge;

import dev.otectus.mcareputation.api.McaReputationApi;
import dev.otectus.mcareputation.api.ReputationRequest;
import dev.otectus.mcareputation.api.ReputationResult;
import dev.otectus.mcareputation.community.CommunityKey;
import dev.otectus.mcareputation.incident.IncidentSubject;
import net.mebahelrpgrquest.quest.PlayerQuestProgress;
import net.mebahelrpgrquest.quest.QuestDefinition;
import net.mebahelrpgrquest.quest.QuestManager;
import net.mebahelrpgrquest.quest.QuestPlayer;
import net.mebahelrpgrquest.quest.QuestTranslations;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/** Mebahel quest outcomes recorded as MCA: Reputation deeds. Only reached when both mods are present (see BridgeMixinPlugin). */
public final class ReputationDeeds {
    private static final ResourceLocation SOURCE = ResourceLocation.fromNamespaceAndPath(McaRpgBridge.MOD_ID, "mebahel");
    private static final ResourceLocation COMPLETED = ResourceLocation.fromNamespaceAndPath("mcareputation", "quest_completed");
    private static final ResourceLocation ABANDONED = ResourceLocation.fromNamespaceAndPath("mcareputation", "quest_abandoned");
    private static boolean warned;

    private ReputationDeeds() {
    }

    public static void questClaimed(ServerPlayer player, LivingEntity giver, QuestDefinition quest, @Nullable PlayerQuestProgress progress) {
        long stamp = progress != null ? progress.getClaimedAtMillis() : player.level().getGameTime();
        record(player, giver.blockPosition(), giver, giver.getUUID(), COMPLETED, BridgeConfig.questCompleted(), title(quest),
                "mebahel:claim:" + quest.id() + ":" + giver.getUUID() + ":" + stamp);
    }

    public static void contractCompleted(ServerPlayer player, ResourceLocation contractId) {
        QuestDefinition quest = QuestManager.INSTANCE.getQuest(contractId);
        record(player, player.blockPosition(), null, null, COMPLETED, BridgeConfig.contractCompleted(),
                quest != null ? title(quest) : "a quest board contract", "mebahel:contract:" + contractId);
    }

    public static void questAbandoned(ServerPlayer player, UUID giverId, ResourceLocation questId) {
        if (!(player instanceof QuestPlayer questPlayer)) {
            return;
        }
        for (PlayerQuestProgress progress : questPlayer.mebahelrpgrquest$getQuests()) {
            // Same match and state test Mebahel uses before it marks the quest failed.
            if (progress != null && questId.equals(progress.getQuestId()) && giverId.equals(progress.getQuestGiverUuid())
                    && !progress.isClaimed() && !progress.isFailed()) {
                Entity giver = player.serverLevel().getEntity(giverId);
                BlockPos pos = giver != null ? giver.blockPosition() : progress.getQuestGiverPos();
                if (pos == null) {
                    return;
                }
                QuestDefinition quest = QuestManager.INSTANCE.getQuest(questId);
                // quest_abandoned is a witnessed deed: the villager who gave the quest is the one who sees you walk away.
                record(player, pos, giver, giverId, ABANDONED, BridgeConfig.questAbandoned(), quest != null ? title(quest) : questId.getPath(),
                        "mebahel:abandon:" + questId + ":" + giverId + ":" + progress.getAcceptedAtMillis());
                return;
            }
        }
    }

    private static String title(QuestDefinition quest) {
        return QuestTranslations.fallbackString(quest.title());
    }

    private static void record(ServerPlayer player, BlockPos pos, @Nullable Entity giver, @Nullable UUID witness, ResourceLocation type, int delta, String title, String dedupeKey) {
        if (delta == 0) {
            return;
        }
        try {
            if (!McaReputationApi.isEnabled()) {
                return;
            }
            Optional<CommunityKey> community = giver != null ? McaReputationApi.resolveCommunity(giver) : Optional.empty();
            if (community.isEmpty()) {
                community = McaReputationApi.resolveCommunity(player.serverLevel(), pos, BridgeConfig.villageRadius());
            }
            if (community.isEmpty()) {
                McaRpgBridge.LOGGER.debug("[MCA RPG Bridge] no MCA village near {} for {}; no deed recorded", pos, dedupeKey);
                return;
            }
            ReputationRequest.Builder request = ReputationRequest.builder(player.server, player.getUUID(), community.get(), type, SOURCE)
                    .delta(delta)
                    .dedupeKey(dedupeKey)
                    .context("source_title", title);
            if (witness != null) {
                request.witness(witness);
            }
            if (giver instanceof Villager) {
                request.subject(IncidentSubject.villager(giver.getUUID(), giver.getName().getString(), ""));
            }
            ReputationResult result = McaReputationApi.record(request.build());
            McaRpgBridge.LOGGER.info("[MCA RPG Bridge] {} {} for {} in {}: {}", type.getPath(), delta, player.getGameProfile().getName(), community.get(), result.reason());
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                McaRpgBridge.LOGGER.warn("[MCA RPG Bridge] could not record a Mebahel deed with MCA: Reputation", t);
            }
        }
    }
}
