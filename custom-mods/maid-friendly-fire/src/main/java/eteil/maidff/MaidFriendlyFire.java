package eteil.maidff;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Touhou Little Maid: Spell registers a damage rule that cancels every hit a maid lands on a maid OR on any
 * player, with no config option. That makes hostile maids (Lost Maids, betrayed maids) harmless.
 *
 * This replaces that one rule. A maid can no longer hurt its own owner, and maids belonging to the same owner
 * still cannot hurt each other, but everything else now connects: hostile maids hit players, your maids hit
 * hostile maids, and maids of different owners can fight. Maid Spell's spell damage multiplier is kept as-is.
 *
 * Landing a hit is only half of it, so {@link MaidTargeting} also nudges the attack list of owned maids so they
 * will actually pick a hostile maid as a target.
 *
 * Everything is done by reflection, so this mod does not need Maid Spell or TLM at compile time, and it goes
 * quiet rather than crashing if either one changes or is removed.
 */
@Mod("maidfriendlyfire")
public class MaidFriendlyFire {
    static final Logger LOG = LogUtils.getLogger();
    private static final String GLOBAL = "com.github.yimeng261.maidspell.Global";
    private static final String CONFIG = "com.github.yimeng261.maidspell.Config";
    private static boolean installed = false;

    public MaidFriendlyFire(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> install());
        // Maid Spell rebuilds its rule list whenever its config reloads, so check back periodically.
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 200 == 0) install();
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getLevel().isClientSide) return;
            MaidTargeting.nudge(e.getEntity());
            TradingMaidFlee.onJoin(e.getEntity());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 10 == 0) TradingMaidFlee.tick();
        });
        // After Lost Maids' own join listener (normal priority), which is the one that converts the maid.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide) WanderingMaidRescue.onJoin(e.getEntity());
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static synchronized void install() {
        try {
            Class<?> global = Class.forName(GLOBAL);
            Field field = global.getField("commonHurtHandlers");
            List handlers = (List) field.get(null);
            if (handlers.stream().anyMatch(h -> h instanceof MaidHurtRule)) {
                installed = true;
                return;
            }
            handlers.clear();
            handlers.add(new MaidHurtRule());
            if (!installed) {
                LOG.info("[MaidFriendlyFire] replaced Maid Spell's blanket player-damage block");
                installed = true;
            }
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            LOG.info("[MaidFriendlyFire] Maid Spell not present or changed ({}), nothing to do", e.toString());
        } catch (Throwable t) {
            LOG.warn("[MaidFriendlyFire] could not replace the rule: {}", t.toString());
        }
    }

    /** Runs for every hit whose attacker is a maid. */
    @SuppressWarnings("rawtypes")
    static final class MaidHurtRule implements BiFunction {
        @Override
        public Object apply(Object eventObj, Object maidObj) {
            if (!(eventObj instanceof LivingIncomingDamageEvent event)) return null;
            Object victim = event.getEntity();

            if (isMaid(victim)) {
                // Maids under the same master do not brawl. A hostile maid has no owner, and another player's
                // maid has a different one, so both of those stay fair game.
                UUID attackerOwner = ownerUuid(maidObj);
                if (attackerOwner != null && attackerOwner.equals(ownerUuid(victim))) {
                    event.setCanceled(true);
                    return null;
                }
            } else if (victim instanceof Player player && player.getUUID().equals(ownerUuid(maidObj))) {
                event.setCanceled(true);      // a maid still cannot hurt the player it belongs to
                return null;
            }

            applySpellMultiplier(event, maidObj);
            return null;
        }

        /** Maid Spell scales damage from its own spell tasks; keep that behaviour. */
        private static void applySpellMultiplier(LivingIncomingDamageEvent event, Object maid) {
            try {
                Object task = maid.getClass().getMethod("getTask").invoke(maid);
                Object uid = task.getClass().getMethod("getUid").invoke(task);
                if (!String.valueOf(uid).startsWith("maidspell")) return;
                double multiplier = Class.forName(CONFIG).getField("spellDamageMultiplier").getDouble(null);
                event.setAmount((float) (event.getAmount() * multiplier));
            } catch (Throwable ignored) {
            }
        }
    }

    static boolean isMaid(Object entity) {
        return entity != null && entity.getClass().getName().endsWith("EntityMaid");
    }

    /** The owner of a maid, or null for a Lost Maid and for a maid that was betrayed or expelled. */
    static UUID ownerUuid(Object maid) {
        try {
            Method getOwnerUUID = maid.getClass().getMethod("getOwnerUUID");
            Object owner = getOwnerUUID.invoke(maid);
            return owner instanceof UUID uuid ? uuid : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static Entity asEntity(Object o) {
        return o instanceof Entity e ? e : null;
    }
}
