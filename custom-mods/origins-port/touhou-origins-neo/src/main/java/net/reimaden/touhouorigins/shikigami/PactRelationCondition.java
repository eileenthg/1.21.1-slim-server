package net.reimaden.touhouorigins.shikigami;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.UUID;

/**
 * Asks whether two entities are bound by a shikigami pact, in a named direction.
 *
 * <p>Registered twice, because a bientity condition cannot be read backwards and both directions
 * are needed: {@code touhouorigins:pact_master} for the servant's own powers, which look outward at
 * their master, and {@code touhouorigins:pact_servant} for anything the master initiates — severing
 * the tether, where the master is the one holding the shears.
 *
 * <p>This exists at all because {@code apoli:in_set} cannot answer the question in either of the two
 * places the pact needs it:
 *
 * <ul>
 *   <li><b>The glow is client-side.</b> {@code apoli:entity_glow} is evaluated in
 *       {@code EntityGlowMixin} on the viewer's client, but {@code EntitySetPower} refuses to
 *       record anything unless the owner is in a {@code ServerLevel} and keeps its membership in
 *       plain static maps that are never synced. On a LAN host those maps happen to be shared with
 *       the integrated server, so the glow works for exactly one player and nobody else.</li>
 *   <li><b>The dying master is not alive.</b> {@code EntitySetPower.contains} opens with
 *       {@code if (target.isRemoved() || !target.isAlive()) return false}, and
 *       {@code LivingEntity.isAlive()} is {@code !isRemoved() && getHealth() > 0}. By the time
 *       {@code LivingDeathEvent} fires the master's health is already zero, so the set reports them
 *       absent and the shared death never triggers.</li>
 * </ul>
 *
 * <p>The pact attachment has neither problem: it is synced and it does not care whether the master
 * is still breathing.
 *
 * @param targetIsMaster {@code true} to ask "is the target the actor's master", which is the
 *                       direction {@code add_to_set} and {@code remember_master} use;
 *                       {@code false} to ask the reverse, "does the target serve the actor".
 */
public record PactRelationCondition(boolean targetIsMaster) implements ConditionType<BiEntityCtx, Object> {

    /** Target is the actor's master. Actor is the servant. */
    public static final ResourceLocation MASTER = TouhouOrigins.id("pact_master");

    /** Target serves the actor. Actor is the master. */
    public static final ResourceLocation SERVANT = TouhouOrigins.id("pact_servant");

    @Override
    public MapCodec<Object> codec() {
        return MapCodec.unit(new Object());
    }

    @Override
    public boolean test(Object cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();
        if (actor == null || target == null || actor == target) return false;
        Entity servant = targetIsMaster ? actor : target;
        Entity master = targetIsMaster ? target : actor;
        UUID recorded = ShikigamiPact.masterOf(servant);
        return recorded != null && recorded.equals(master.getUUID());
    }
}
