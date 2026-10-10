package com.github.eileen.llvoices;

import com.github.eileen.llvoices.logic.ChangeCause;
import com.github.eileen.llvoices.logic.DriftLevel;
import com.github.eileen.llvoices.logic.DriftZone;
import com.github.eileen.llvoices.logic.HungerStage;
import com.github.eileen.llvoices.voice.VoicePrompts;
import com.github.eileen.llvoices.voice.VoiceRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Every line the mod can queue must have both canned variants in en_us.json. */
public class FallbackLinesTest {
    private static final String[] TENDENCIES = {"BOND", "FRIENDLY", "STRANGER", "FEARFUL", "TERRIFIED", "CONFLICTED", "NEUTRAL"};

    @Test
    public void everyFallbackKeyExists() throws Exception {
        JsonObject lang;
        try (InputStreamReader reader = new InputStreamReader(
                FallbackLinesTest.class.getResourceAsStream("/assets/llvoices/lang/en_us.json"), StandardCharsets.UTF_8)) {
            lang = JsonParser.parseReader(reader).getAsJsonObject();
        }

        List<VoiceRequest> requests = new ArrayList<>();
        requests.add(VoicePrompts.hungerPeckish(0));
        requests.add(VoicePrompts.hungerNoFood(0, HungerStage.STARVING));
        requests.add(VoicePrompts.hungerSated(0));
        requests.add(VoicePrompts.hungerFull(0));
        requests.add(VoicePrompts.hurtLow(0));
        requests.add(VoicePrompts.hurtCritical(0));
        requests.add(VoicePrompts.healed(0));
        for (HungerStage stage : HungerStage.values()) {
            requests.add(VoicePrompts.status(0, stage, 1.0F, "BOND"));
            requests.add(VoicePrompts.status(0, stage, 0.3F, "BOND"));
        }
        for (String from : TENDENCIES) {
            for (String to : TENDENCIES) {
                requests.add(VoicePrompts.moodShift(0, from, to));
            }
        }
        for (DriftZone zone : DriftZone.values()) {
            requests.add(VoicePrompts.drift(0, zone, DriftLevel.NEAR, false));
            requests.add(VoicePrompts.drift(0, zone, DriftLevel.BRINK, true));
            requests.add(VoicePrompts.easing(0, zone));
        }
        for (ChangeCause cause : ChangeCause.values()) {
            if (!cause.voicedElsewhere()) {
                requests.add(VoicePrompts.passive(0, cause, 1, -1));
            }
        }

        for (VoiceRequest request : requests) {
            for (int i = 0; i < 2; i++) {
                String key = request.fallbackKey() + "." + i;
                assertTrue("missing " + key, lang.has(key));
            }
            assertFalse(request.topic(), request.situation().isBlank());
        }
    }

    @Test
    public void passiveTextDescribesDirection() {
        String text = VoicePrompts.passive(0, ChangeCause.HURT_BY_OWNER, -2, 3).situation();
        assertTrue(text, text.contains("less") && text.contains("more afraid"));
    }
}
