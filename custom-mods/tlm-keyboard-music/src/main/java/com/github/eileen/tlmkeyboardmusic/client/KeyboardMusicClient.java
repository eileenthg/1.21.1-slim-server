package com.github.eileen.tlmkeyboardmusic.client;

import com.github.eileen.tlmkeyboardmusic.PlayKeyboardMusicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Client side of the keyboard music. Only ever loaded on the client, from the payload handler.
 */
public final class KeyboardMusicClient {
    private static final Map<Integer, KeyboardMusicInstance> PLAYING = new HashMap<>();

    private KeyboardMusicClient() {
    }

    public static void handle(PlayKeyboardMusicPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity sit = minecraft.level.getEntity(payload.sitEntityId());
        if (sit == null) {
            return;
        }
        KeyboardMusicInstance old = PLAYING.remove(sit.getId());
        if (old != null) {
            minecraft.getSoundManager().stop(old);
        }
        KeyboardMusicInstance instance = new KeyboardMusicInstance(payload.sound().value(), payload.volume(), sit);
        PLAYING.put(sit.getId(), instance);
        minecraft.getSoundManager().play(instance);
    }

    /**
     * Follows the seat and stops when it is gone or empty. The first second is a grace period: a client
     * that has just started tracking the seat can receive this before the passenger list.
     */
    private static final class KeyboardMusicInstance extends AbstractTickableSoundInstance {
        private static final int GRACE_TICKS = 20;
        private final Entity sit;
        private int age = 0;

        private KeyboardMusicInstance(SoundEvent sound, float volume, Entity sit) {
            super(sound, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.sit = sit;
            this.volume = volume;
            this.x = sit.getX();
            this.y = sit.getY();
            this.z = sit.getZ();
        }

        @Override
        public void tick() {
            this.age++;
            if (this.sit.isRemoved() || (this.age > GRACE_TICKS && this.sit.getPassengers().isEmpty())) {
                this.stop();
                PLAYING.remove(this.sit.getId(), this);
                return;
            }
            this.x = this.sit.getX();
            this.y = this.sit.getY();
            this.z = this.sit.getZ();
        }
    }
}
