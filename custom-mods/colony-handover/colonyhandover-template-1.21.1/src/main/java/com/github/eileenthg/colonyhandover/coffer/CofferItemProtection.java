package com.github.eileenthg.colonyhandover.coffer;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;

/**
 * Keeps a dropped coffer from being destroyed by the world.
 *
 * <h2>Why the item and not just the block</h2>
 *
 * The coffer is a signpost: it stands where the town hall did so that someone
 * returning to a converted village knows there is something to collect. Break it to
 * move it somewhere better and it becomes an item entity - and an item entity is
 * fragile in ways a block is not. A creeper, a lava flow or an unlucky cactus would
 * quietly delete the only visible sign that the colony left anything behind.
 *
 * <h2>Why an event rather than item properties</h2>
 *
 * {@code Item.Properties.fireResistant()} only sets the {@code FIRE_RESISTANT} data
 * component, and {@code ItemStack.canBeHurtBy} consults it for exactly one thing:
 * {@code IS_FIRE}. Explosions and cacti go down a different path entirely, so
 * fire-resistance alone would fix the lava case and leave the other two.
 * {@code isInvulnerableTo} is checked before any of that
 * ({@code ItemEntity.hurt:278}), so hooking it covers every damage source at once -
 * present and future, including whatever a modpack adds.
 *
 * <h2>What this deliberately does not do</h2>
 *
 * <p><b>Despawning still works.</b> That is driven by age against lifespan, not by
 * damage, so it never reaches this check. A coffer left on the ground and forgotten
 * disappears like anything else - which is right, since the goods it points at are
 * in level data and reachable by command regardless.
 *
 * <p><b>The void still wins.</b> {@code Entity.onBelowWorld} calls {@code discard()}
 * directly rather than dealing damage, so {@code isInvulnerableTo} is never
 * consulted. There is no hook to take. Dropping one into the void is final.
 */
@EventBusSubscriber(modid = ColonyHandover.MODID)
public final class CofferItemProtection {

    @SubscribeEvent
    static void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)) {
            return;
        }
        if (item.getItem().is(ColonyHandover.COFFER_ITEM.get())) {
            event.setInvulnerable(true);
        }
    }

    private CofferItemProtection() {}
}
