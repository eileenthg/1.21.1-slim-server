package com.github.eileen.llvoices.voice;

/**
 * One thing a maid wants to say, waiting for a gap in her chatter.
 *
 * @param kind        what kind of line it is
 * @param topic       cooldown key, e.g. {@code drift_near/betrayal}; lines sharing a topic do not repeat quickly
 * @param situation   English description of what is happening, sent to the maid's LLM
 * @param fallbackKey lang key prefix for the canned line used when no LLM is available ({@code .0}/{@code .1} appended)
 * @param createdTick game time when it was queued
 */
public record VoiceRequest(VoiceKind kind, String topic, String situation, String fallbackKey, long createdTick) {
    public int priority() {
        return this.kind.priority();
    }
}
