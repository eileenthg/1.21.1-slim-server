package net.reimaden.touhouorigins.event;

import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.reimaden.touhouorigins.power.ActionOnEntityDeathPower;
import net.reimaden.touhouorigins.shikigami.ShikigamiPact;

import java.util.OptionalInt;

/**
 * Drives {@link ActionOnEntityDeathPower}: when anything dies, offer that death to every player
 * holding the power.
 *
 * <p>Voile scanned every living entity in every world. Players are the only holders that matter
 * here — the power is granted through a global power set scoped to {@code minecraft:player} — so
 * this walks the player list instead.
 *
 * <p><b>The set is re-seeded first, and that is the point.</b> The shared death reads membership of
 * {@code boundless_servitude_set}, which Apoli keeps in static in-memory maps and strips whenever
 * an entity is removed with a destroying reason. A master who has died even once is already gone
 * from it, so by the time they die again the condition silently fails and the servant lives. The
 * durable record of the pact is our own attachment, so if it names the deceased as this player's
 * master, membership is restored before the power is offered the death.
 *
 * <p>Runs at {@code LOWEST} so a death another listener cancels — Apoli's own prevent-death powers
 * included — never triggers a lethal reaction.
 */
public final class ShikigamiDeathHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        LivingEntity deceased = event.getEntity();
        if (!(deceased.level() instanceof ServerLevel level)) return;

        MinecraftServer server = level.getServer();
        if (server == null) return;

        EntitySetPower.Cfg setCfg = EntitySetPower.resolveCfg(ShikigamiPact.SET_POWER);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == deceased || !player.isAlive()) continue;

            if (setCfg != null && isRecordedMaster(player, deceased)
                && !EntitySetPower.contains(player, ShikigamiPact.SET_POWER, deceased)) {
                EntitySetPower.add(player, ShikigamiPact.SET_POWER, setCfg, deceased, OptionalInt.empty());
            }

            // The servant may be in another dimension, so evaluate in their own level rather than
            // the one their master died in.
            ServerLevel ctxLevel = player.level() instanceof ServerLevel own ? own : level;
            ActionOnEntityDeathPower.fire(player, deceased, ctxLevel);
        }
    }

    private static boolean isRecordedMaster(ServerPlayer servant, LivingEntity candidate) {
        return servant.getData(ShikigamiPact.PACT.get()).master()
            .filter(id -> id.equals(candidate.getUUID()))
            .isPresent();
    }
}
