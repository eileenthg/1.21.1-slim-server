package com.github.eileen.llvoices;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Love & Loathe maids speak up on their own: about their needs, their mood, how close they are to one of
 * Love & Loathe's emotional thresholds, and the passive trust/fear changes that L&L otherwise applies silently.
 */
@Mod(LoveLoatheVoices.MOD_ID)
public final class LoveLoatheVoices {
    public static final String MOD_ID = "llvoices";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public LoveLoatheVoices(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, VoiceConfig.SPEC);
        NeoForge.EVENT_BUS.register(MaidVoiceTracker.INSTANCE);
    }
}
