package eteil.csextras.client;

import com.momosoftworks.coldsweat.api.util.Temperature;
import eteil.csextras.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Heartbeat that speeds up as body temperature nears overheating (Scorchful-style warning). */
public final class Heartbeat {
    private static int ticksSinceBeat;
    private static String cachedId;
    private static SoundEvent cachedSound;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(Heartbeat::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.isPaused() || !Config.HEARTBEAT.get() || player.isCreative() || player.isSpectator()) {
            ticksSinceBeat = 0;
            return;
        }
        double body = Temperature.get(player, Temperature.Trait.BODY);
        double start = Config.HEARTBEAT_START.get();
        if (body < start) {
            ticksSinceBeat = 0;
            return;
        }
        double t = Mth.clamp((body - start) / Math.max(1e-6, 100.0 - start), 0, 1);
        int interval = body >= 100 ? 8 : (int) Math.round(Mth.lerp(t, 40, 11));
        if (++ticksSinceBeat < interval) return;
        ticksSinceBeat = 0;
        float volume = (float) (Config.HEARTBEAT_VOLUME.get() * (0.6 + 0.4 * t));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound(mc), 1.0f, volume));
    }

    private static SoundEvent sound(Minecraft mc) {
        String id = Config.HEARTBEAT_SOUND.get();
        if (!id.equals(cachedId)) {
            cachedId = id;
            ResourceLocation rl = ResourceLocation.tryParse(id);
            cachedSound = rl != null && mc.getSoundManager().getSoundEvent(rl) != null
                    ? SoundEvent.createVariableRangeEvent(rl)
                    : SoundEvents.NOTE_BLOCK_BASEDRUM.value();
        }
        return cachedSound;
    }

    private Heartbeat() {
    }
}
