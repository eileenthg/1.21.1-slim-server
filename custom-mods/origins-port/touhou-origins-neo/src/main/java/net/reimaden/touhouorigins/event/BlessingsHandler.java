package net.reimaden.touhouorigins.event;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;

/**
 * The Java half of the two Touhou Origins Blessings beast-spirit powers. Both powers are plain
 * {@code apoli:simple} flags in data; this handler gives them their effects.
 *
 * <ul>
 *   <li><b>husbandry_master</b> — animals a blessed player breeds are ready to breed again in half
 *       the time (3000 ticks instead of 6000).</li>
 *   <li><b>sturdy_beasts</b> — animals a blessed player tames, and the offspring of their tamed
 *       animals, get +4 armor.</li>
 * </ul>
 *
 * <p>Upstream used four mixins (breed, setOwner, bondWithPlayer, three createChild overrides).
 * NeoForge fires {@link AnimalTameEvent} from every taming site — wolf, cat, parrot, ocelot, and
 * horses via {@code RunAroundLikeCrazyGoal} — and {@link BabyEntitySpawnEvent} from breeding, so
 * events replace all of them.
 *
 * <p>Timing note, from the patched source: {@code BabyEntitySpawnEvent} fires <i>before</i>
 * {@code finalizeSpawnChildFromBreeding} sets the parents' 6000-tick cooldown, so writing the
 * halved cooldown directly in the handler would be overwritten a moment later. The work is
 * deferred one tick with {@code server.execute}, which runs after the whole breeding call
 * completes — the point upstream's {@code @At("TAIL")} injection hooked.
 *
 * <p>The power ids here are the full data paths ({@code beast_spirits/...}). Upstream's Java
 * checked {@code touhoublessings:husbandry_master} — without the folder — which matches no power
 * defined by its own datapack, so both features appear to have been dead upstream. Same class of
 * bug as the unregistered extra keybinds: a reference that resolves to nothing fails silently.
 */
public final class BlessingsHandler {

    private static final ResourceLocation HUSBANDRY_MASTER =
        ResourceLocation.fromNamespaceAndPath("touhoublessings", "beast_spirits/husbandry_master");
    private static final ResourceLocation STURDY_BEASTS =
        ResourceLocation.fromNamespaceAndPath("touhoublessings", "beast_spirits/sturdy_beasts");

    private static final ResourceLocation ARMOR_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath("touhoublessings", "beast_spirits_bonus");
    private static final double ARMOR_BONUS = 4.0;

    private static final int HALVED_BREEDING_COOLDOWN = 3000;

    /** Both blessing powers are condition-less {@code apoli:simple} flags, so held + not suppressed = active. */
    private static boolean hasBlessing(Entity entity, ResourceLocation power) {
        if (entity == null) return false;
        PowerContainer container = PowerContainer.of(entity);
        return container != null && container.hasPower(power) && !container.isSuppressed(power);
    }

    private static void addBeastSpiritArmor(LivingEntity entity) {
        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor == null || armor.hasModifier(ARMOR_MODIFIER_ID)) return;
        armor.addPermanentModifier(new AttributeModifier(
            ARMOR_MODIFIER_ID, ARMOR_BONUS, AttributeModifier.Operation.ADD_VALUE));
    }

    /** LOWEST priority so a cancellation by any other listener is visible before armor is applied. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAnimalTame(AnimalTameEvent event) {
        if (event.isCanceled()) return;
        if (hasBlessing(event.getTamer(), STURDY_BEASTS)) {
            addBeastSpiritArmor(event.getAnimal());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreed(BabyEntitySpawnEvent event) {
        if (event.isCanceled()) return;

        Animal parentA = event.getParentA() instanceof Animal animal ? animal : null;
        Animal parentB = event.getParentB() instanceof Animal animal ? animal : null;
        if (parentA == null) return;

        Player cause = event.getCausedByPlayer();
        boolean halveCooldown = hasBlessing(cause, HUSBANDRY_MASTER);

        // Offspring of a tamed animal whose owner is blessed. The child's own owner is not
        // reliably set yet at event time, so the parents' owners decide, as upstream's
        // createChild overrides effectively did.
        LivingEntity child = event.getChild() instanceof LivingEntity living ? living : null;
        boolean armorChild = child != null
            && (hasBlessing(ownerOf(parentA), STURDY_BEASTS) || hasBlessing(ownerOf(parentB), STURDY_BEASTS));

        if (!halveCooldown && !armorChild) return;
        if (armorChild) addBeastSpiritArmor(child);
        if (!halveCooldown) return;

        Animal finalB = parentB;
        // After spawnChildFromBreeding has set the vanilla 6000-tick cooldown.
        parentA.level().getServer().execute(() -> {
            if (parentA.isAlive()) parentA.setAge(HALVED_BREEDING_COOLDOWN);
            if (finalB != null && finalB.isAlive()) finalB.setAge(HALVED_BREEDING_COOLDOWN);
        });
    }

    private static Entity ownerOf(Animal animal) {
        return animal instanceof OwnableEntity ownable ? ownable.getOwner() : null;
    }
}
