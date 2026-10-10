package com.github.eileen.llvoices.voice;

import com.github.JumDa5he.callresponse.compat.broadcast.BroadcastDialogueTracker;
import com.github.JumDa5he.callresponse.compat.broadcast.DialogueApiLimiter;
import com.github.JumDa5he.callresponse.compat.emotion.EmotionPrompt;
import com.github.eileen.llvoices.LoveLoatheVoices;
import com.github.eileen.llvoices.VoiceConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatManager;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.DefaultLLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Turns a {@link VoiceRequest} into a chat bubble: through the maid's LLM when possible, otherwise a canned line.
 * <p>
 * The LLM message is wrapped exactly like Love & Loathe's own dialogue (its emotion block plus its broadcast marker),
 * so L&L's mixins treat it as one of theirs: the reply is cleaned up, the prompt is kept out of the maid's chat
 * history, and it counts toward L&L's per-minute API cap.
 */
public final class VoiceDispatcher {
    /** Canned lines per fallback key: {@code <key>.0} and {@code <key>.1}. */
    static final int FALLBACK_VARIANTS = 2;
    private static boolean loggedLinkageError = false;

    private VoiceDispatcher() {
    }

    public enum Route {
        LLM,
        FALLBACK,
        RATE_LIMITED,
        SILENT
    }

    public static Route speak(EntityMaid maid, ServerPlayer owner, VoiceRequest request) {
        if (VoiceConfig.USE_LLM.get() && canUseLlm(maid, owner)) {
            // Over L&L's per-minute cap: skip the line rather than swap in a generic canned one.
            if (!tryAcquireApiSlot()) {
                return Route.RATE_LIMITED;
            }
            sendToLlm(maid, owner, request);
            return Route.LLM;
        }
        if (VoiceConfig.FALLBACK_LINES.get()) {
            showFallback(maid, owner, request);
            return Route.FALLBACK;
        }
        return Route.SILENT;
    }

    /**
     * Mirrors the checks in {@link MaidAIChatManager#chat}, which would otherwise answer a background line with a red
     * error in the owner's chat.
     */
    static boolean canUseLlm(EntityMaid maid, ServerPlayer owner) {
        if (!AIConfig.LLM_ENABLED.get()) {
            return false;
        }
        if (owner.getData(InitDataAttachment.CHAT_TOKENS).get() >= AIConfig.MAX_TOKENS_PER_PLAYER.get()) {
            return false;
        }
        MaidAIChatManager manager = maid.getAiChatManager();
        LLMSite site = SiteRouting.override();
        if (site == null) {
            site = manager.getLLMSite();
        }
        if (site == null || !site.enabled()) {
            return false;
        }
        if (site.id().equals(DefaultLLMSite.DEEPSEEK.id()) && site instanceof LLMOpenAISite openAI
                && StringUtils.isBlank(openAI.secretKey())) {
            return false;
        }
        // Without a character setting TLM would spend the turn generating one and our line would be lost.
        return manager.getSetting().isPresent() || StringUtils.isNotBlank(manager.customSetting);
    }

    private static boolean tryAcquireApiSlot() {
        try {
            return DialogueApiLimiter.tryAcquire();
        } catch (LinkageError error) {
            logLinkageError(error);
            return true;
        }
    }

    private static void sendToLlm(EntityMaid maid, ServerPlayer owner, VoiceRequest request) {
        MaidAIChatManager manager = maid.getAiChatManager();
        String message = buildMessage(maid, owner, VoicePrompts.wrap(request.situation()));
        // Same language the maid last chatted in; before her first chat, the owner's game language.
        String language = StringUtils.isNotBlank(manager.chatLanguage) ? manager.chatLanguage : owner.getLanguage();
        ChatClientInfo clientInfo = new ChatClientInfo(language, maid.getName().getString(), List.of());
        SiteRouting.runAsVoiceLine(() -> manager.chat(message, clientInfo, owner));
    }

    private static String buildMessage(EntityMaid maid, ServerPlayer owner, String prompt) {
        try {
            String emotion = EmotionPrompt.buildEmotionContext(maid, owner);
            return BroadcastDialogueTracker.mark(EmotionPrompt.wrapConversation(prompt, emotion), owner.getUUID());
        } catch (LinkageError error) {
            // A Love & Loathe update renamed something; still speak, just without its emotion block.
            logLinkageError(error);
            return prompt;
        }
    }

    private static void showFallback(EntityMaid maid, ServerPlayer owner, VoiceRequest request) {
        int variant = maid.getRandom().nextInt(FALLBACK_VARIANTS);
        MutableComponent line = Component.translatable(request.fallbackKey() + "." + variant);
        maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.type2(line));
        // Same as TLM does for LLM replies: echo the line in the owner's chat.
        MutableComponent echo = Component.literal("<").append(maid.getName()).append(">")
                .append(CommonComponents.SPACE).append(line.copy());
        owner.sendSystemMessage(echo.withStyle(ChatFormatting.GRAY));
    }

    private static void logLinkageError(LinkageError error) {
        if (!loggedLinkageError) {
            loggedLinkageError = true;
            LoveLoatheVoices.LOGGER.warn("Love & Loathe API changed; speaking without it: {}", error.toString());
        }
    }
}
