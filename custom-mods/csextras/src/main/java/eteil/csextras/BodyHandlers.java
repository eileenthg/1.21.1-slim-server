package eteil.csextras;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Shivering, and keeping the sweat modifier attached to players. */
public final class BodyHandlers {
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 == 0) shiver(player);
        if (player.tickCount % 100 == 0 && Config.SWEAT.get()
                && !Temperature.hasModifier(player, Temperature.Trait.WORLD, SweatTempModifier.class)) {
            Temperature.addModifier(player, new SweatTempModifier().tickRate(10), Temperature.Trait.WORLD,
                    Placement.LAST.noDuplicates(Matcher.SAME_CLASS));
        }
    }

    /** One second of shivering. Returns the warmth added, for the self-test. */
    public static double shiver(Player player) {
        if (!Config.SHIVER.get() || player.isCreative() || player.isSpectator()) return 0;
        if (player.getFoodData().getFoodLevel() <= Config.SHIVER_MIN_FOOD.get()) return 0;
        double body = Temperature.get(player, Temperature.Trait.BODY);
        double start = Config.SHIVER_START.get();
        if (body >= start) return 0;
        double intensity = Mth.clamp((start - body) / Math.max(1e-6, start + 100.0), 0, 1);
        double warmth = Mth.lerp(intensity, Config.SHIVER_MIN_WARMTH.get(), Config.SHIVER_MAX_WARMTH.get());
        Temperature.add(player, Temperature.Trait.CORE, warmth);
        player.causeFoodExhaustion((float) (Config.SHIVER_EXHAUSTION.get() * (0.5 + 0.5 * intensity)));
        return warmth;
    }

    private BodyHandlers() {
    }
}
