package eteil.mcarpgbridge;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.mebahelrpgrquest.quest.PlayerQuestProgress;
import net.mebahelrpgrquest.quest.QuestAvailability;
import net.mebahelrpgrquest.quest.QuestDefinition;
import net.mebahelrpgrquest.quest.QuestGiver;
import net.mebahelrpgrquest.quest.QuestManager;
import net.mebahelrpgrquest.quest.QuestPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Runs Mebahel's own villager right-click handler for an MCA villager, on request from the MCA menu button. */
public final class MebahelInteraction {
    private static final String HANDLER_CLASS = "net.mebahelrpgrquest.quest.QuestEvents";
    /**
     * The UseEntityCallback lambda that opens villager quests and rumors. Its number is positional, so it
     * moves between Mebahel versions: 2.0.6 had $2 (beside a guard-conversion $1), 3.2.0 has $3 and is the
     * only one left. Never hardcode one: the handler is found by prefix and signature at runtime, and the
     * click mixin lists the known numbers.
     */
    public static final String HANDLER_PREFIX = "lambda$registerQuestInteraction$";
    public static final String HANDLER_206 = HANDLER_PREFIX + "2";
    public static final String HANDLER_320 = HANDLER_PREFIX + "3";
    private static final double MAX_DISTANCE_SQR = 10 * 10;
    private static final ThreadLocal<Boolean> FROM_MCA_MENU = ThreadLocal.withInitial(() -> false);
    private static Method handler;
    private static boolean handlerMissing;

    private MebahelInteraction() {
    }

    /**
     * Logs which interaction handler this Mebahel build has, and warns when it is not one the click mixin
     * targets: in that case Mebahel would take the right-click on MCA villagers again, which is silent
     * otherwise because the injection is require = 0.
     */
    public static void verifyClickGuard() {
        Method found = handler();
        if (found == null) {
            McaRpgBridge.LOGGER.warn("[MCA RPG Bridge] Mebahel is installed but its villager interaction handler was not found; MCA villagers may be taken over by Mebahel again, and the MCA menu button will not work");
        } else if (found.getName().equals(HANDLER_206) || found.getName().equals(HANDLER_320)) {
            McaRpgBridge.LOGGER.info("[MCA RPG Bridge] Mebahel interaction handler {}: click guard and MCA menu button armed", found.getName());
        } else {
            McaRpgBridge.LOGGER.warn("[MCA RPG Bridge] Mebahel interaction handler is {}, which this build does not guard (known: {}, {}). The MCA menu button still works, but Mebahel will also take plain right-clicks on MCA villagers. The bridge needs updating for this Mebahel version.",
                    found.getName(), HANDLER_206, HANDLER_320);
        }
    }

    /** True while the bridge itself is replaying a click, so the click guard lets Mebahel handle it. */
    public static boolean fromMcaMenu() {
        return FROM_MCA_MENU.get();
    }

    /** Returns Mebahel's answer, or PASS when the bridge did not hand the click over. */
    public static InteractionResult openFromMcaMenu(ServerPlayer player, UUID villagerId) {
        Entity entity = player.serverLevel().getEntity(villagerId);
        if (!(entity instanceof VillagerEntityMCA villager) || !villager.isAlive() || !(villager instanceof QuestGiver giver)) {
            return InteractionResult.PASS;
        }
        if (player.distanceToSqr(villager) > MAX_DISTANCE_SQR) {
            player.displayClientMessage(Component.translatable("message.mcarpgbridge.too_far"), true);
            return InteractionResult.PASS;
        }
        if (!hasSomethingFor(player, villager, giver)) {
            // Mebahel's handler would fall back to the villager's own interaction here, reopening MCA's menu.
            player.displayClientMessage(Component.translatable("message.mcarpgbridge.nothing_yet"), true);
            return InteractionResult.PASS;
        }
        Method method = handler();
        if (method == null) {
            return InteractionResult.PASS;
        }
        FROM_MCA_MENU.set(true);
        try {
            return (InteractionResult) method.invoke(null, player, player.level(), InteractionHand.MAIN_HAND, villager, null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            McaRpgBridge.LOGGER.error("[MCA RPG Bridge] Mebahel's villager handler failed for {}", villager.getUUID(), e);
        } finally {
            FROM_MCA_MENU.set(false);
        }
        return InteractionResult.PASS;
    }

    /** Mirrors the checks at the top of Mebahel's handler that decide whether it opens anything at all. */
    private static boolean hasSomethingFor(ServerPlayer player, VillagerEntityMCA villager, QuestGiver giver) {
        if (!giver.mebahelrpgrquest$hasQuest()) {
            return giver.mebahelrpgrquest$hasRumor();
        }
        ResourceLocation questId = giver.mebahelrpgrquest$getQuestId();
        QuestDefinition quest = questId == null ? null : QuestManager.INSTANCE.getQuest(questId);
        if (quest == null) {
            return true;
        }
        PlayerQuestProgress progress = ((QuestPlayer) player).mebahelrpgrquest$getQuestFromGiver(villager.getUUID());
        if (progress != null) {
            boolean finishedOtherQuest = !progress.getQuestId().equals(questId)
                    && (progress.isClaimed() || progress.isCompleted() || progress.isFailed());
            boolean failedThisQuest = progress.isFailed() && progress.getQuestId().equals(questId);
            if (finishedOtherQuest || failedThisQuest) {
                progress = null;
            }
        }
        return progress != null || QuestAvailability.canSeeQuest(player, quest);
    }

    private static synchronized Method handler() {
        if (handler == null && !handlerMissing) {
            try {
                List<Method> found = new ArrayList<>();
                for (Method m : Class.forName(HANDLER_CLASS).getDeclaredMethods()) {
                    if (m.getName().startsWith(HANDLER_PREFIX) && Arrays.equals(m.getParameterTypes(),
                            new Class<?>[]{Player.class, Level.class, InteractionHand.class, Entity.class, EntityHitResult.class})) {
                        found.add(m);
                    }
                }
                if (found.size() == 1) {
                    handler = found.get(0);
                    handler.setAccessible(true);
                } else if (found.isEmpty()) {
                    handlerMissing = true;
                    McaRpgBridge.LOGGER.error("[MCA RPG Bridge] no {}* handler in Mebahel; the MCA menu button cannot open Mebahel quests (unsupported Mebahel version?)", HANDLER_PREFIX);
                } else {
                    // More than one candidate: prefer a number this bridge was verified against, else give up
                    // rather than replay an unknown callback.
                    for (Method m : found) {
                        if (m.getName().equals(HANDLER_320) || m.getName().equals(HANDLER_206)) {
                            handler = m;
                            handler.setAccessible(true);
                            break;
                        }
                    }
                    if (handler == null) {
                        handlerMissing = true;
                    }
                    McaRpgBridge.LOGGER.warn("[MCA RPG Bridge] Mebahel has {} interaction handlers ({}); using {}",
                            found.size(), found.stream().map(Method::getName).toList(), handler == null ? "none" : handler.getName());
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                handlerMissing = true;
                McaRpgBridge.LOGGER.error("[MCA RPG Bridge] could not find Mebahel's villager handler; the MCA menu button cannot open Mebahel quests", e);
            }
        }
        return handler;
    }
}
