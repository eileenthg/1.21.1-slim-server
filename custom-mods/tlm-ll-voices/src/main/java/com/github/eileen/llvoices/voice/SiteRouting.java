package com.github.eileen.llvoices.voice;

import com.github.eileen.llvoices.VoiceConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

/**
 * Decides when a maid's LLM chat goes to the configured site (Player2 by default) instead of her own.
 * Read by {@code MaidAIChatDataMixin} at the head of TLM's {@code getLLMSite()}.
 */
public final class SiteRouting {
    private static final ThreadLocal<Boolean> SPEAKING = ThreadLocal.withInitial(() -> false);

    private SiteRouting() {
    }

    /** Marks that this thread is sending one of our own lines, so it is routed even if routeAllMaidChat is off. */
    public static void runAsVoiceLine(Runnable action) {
        SPEAKING.set(true);
        try {
            action.run();
        } finally {
            SPEAKING.remove();
        }
    }

    /**
     * The site to use instead of the maid's own, or null to leave TLM's choice alone.
     */
    @Nullable
    public static LLMSite override() {
        if (!VoiceConfig.loaded()) {
            return null;
        }
        if (!VoiceConfig.ROUTE_ALL_MAID_CHAT.get() && !SPEAKING.get()) {
            return null;
        }
        String siteId = VoiceConfig.LLM_SITE.get();
        if (StringUtils.isBlank(siteId)) {
            return null;
        }
        LLMSite site = AvailableSites.getLLMSite(siteId.trim());
        return site != null && site.enabled() ? site : null;
    }
}
