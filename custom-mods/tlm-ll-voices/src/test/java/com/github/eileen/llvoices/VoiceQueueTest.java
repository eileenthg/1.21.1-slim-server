package com.github.eileen.llvoices;

import com.github.eileen.llvoices.voice.VoiceKind;
import com.github.eileen.llvoices.voice.VoiceQueue;
import com.github.eileen.llvoices.voice.VoiceRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VoiceQueueTest {
    private static VoiceRequest request(VoiceKind kind, String topic, long tick) {
        return new VoiceRequest(kind, topic, "situation", "key", tick);
    }

    @Test
    public void highestPriorityGoesFirst() {
        VoiceQueue queue = new VoiceQueue();
        queue.offer(request(VoiceKind.STATUS, "status", 0));
        queue.offer(request(VoiceKind.HURT_CRITICAL, "hurt_critical", 0));
        assertEquals(VoiceKind.HURT_CRITICAL, queue.poll(10, 0, false).kind());
    }

    @Test
    public void respectsTheGapBetweenLines() {
        VoiceQueue queue = new VoiceQueue();
        queue.offer(request(VoiceKind.HUNGER_PECKISH, "a", 0));
        queue.offer(request(VoiceKind.HEALED, "b", 0));
        queue.poll(100, 500, false);
        assertNull(queue.poll(200, 500, false));
        assertEquals(VoiceKind.HEALED, queue.poll(600, 500, false).kind());
    }

    @Test
    public void topicCooldownDiscardsRepeats() {
        VoiceQueue queue = new VoiceQueue();
        queue.offer(request(VoiceKind.PASSIVE, "passive", 0));
        queue.poll(0, 0, false);
        queue.offer(request(VoiceKind.PASSIVE, "passive", 20));
        // PASSIVE cooldown is 90 s = 1800 ticks.
        assertNull(queue.poll(40, 0, false));
        assertTrue(queue.isEmpty());
    }

    @Test
    public void sameTopicReplacesOlderLine() {
        VoiceQueue queue = new VoiceQueue();
        queue.offer(new VoiceRequest(VoiceKind.MOOD_SHIFT, "mood_shift", "old", "k", 0));
        queue.offer(new VoiceRequest(VoiceKind.MOOD_SHIFT, "mood_shift", "new", "k", 5));
        assertEquals(1, queue.pendingView().size());
        assertEquals("new", queue.poll(10, 0, false).situation());
    }

    @Test
    public void staleLinesExpire() {
        VoiceQueue queue = new VoiceQueue();
        queue.offer(request(VoiceKind.DRIFT_NEAR, "drift_near/panic", 0));
        assertNull(queue.poll(VoiceQueue.PENDING_TTL_TICKS + 1, 0, false));
    }

    @Test
    public void sleepingMutesAllButUrgentLines() {
        VoiceQueue queue = new VoiceQueue();
        queue.offer(request(VoiceKind.DRIFT_BRINK, "drift_brink/panic", 0));
        queue.offer(request(VoiceKind.PASSIVE, "passive", 0));
        assertEquals(VoiceKind.PASSIVE, queue.poll(10, 0, true).kind());
        assertEquals(1, queue.pendingView().size());
    }
}
