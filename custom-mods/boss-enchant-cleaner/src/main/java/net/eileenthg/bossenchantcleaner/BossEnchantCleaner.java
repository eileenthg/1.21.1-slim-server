package net.eileenthg.bossenchantcleaner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dungeons & Taverns gives its structure bosses powers through "enchantments" on a stick or flint they hold
 * (boss_behaviour, shulker_boss, shulker_miniboss). They are in no enchantment tag, but anything that rolls from
 * the whole registry - e.g. an NPC trade that enchants a book "randomly" with no option list - can hand them out.
 *
 * Only player-side stacks and merchant offers are touched, so the bosses keep their gear. Stored enchantments on
 * books are swapped for a random level-1 enchanting-table enchantment so the book is not simply lost; enchantments
 * applied directly to gear are just removed.
 */
@Mod(BossEnchantCleaner.MODID)
public class BossEnchantCleaner {
    public static final String MODID = "bossenchantcleaner";
    static final Logger LOG = LoggerFactory.getLogger(MODID);

    private static final Set<ResourceLocation> MARKERS = Set.of(
            ResourceLocation.fromNamespaceAndPath("nova_structures", "boss_behaviour"),
            ResourceLocation.fromNamespaceAndPath("nova_structures", "shulker_boss"),
            ResourceLocation.fromNamespaceAndPath("nova_structures", "shulker_miniboss"));

    public BossEnchantCleaner() {
        NeoForge.EVENT_BUS.addListener(BossEnchantCleaner::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(BossEnchantCleaner::onContainerClose);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BossEnchantCleaner::onEntityInteract);
    }

    static boolean isMarker(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(k -> MARKERS.contains(k.location())).orElse(false);
    }

    /** Returns true if anything was changed. */
    static boolean clean(ItemStack stack, Level level) {
        if (stack.isEmpty()) return false;
        boolean changed = clean(stack, DataComponents.ENCHANTMENTS, false, level);
        changed |= clean(stack, DataComponents.STORED_ENCHANTMENTS, true, level);
        return changed;
    }

    private static boolean clean(ItemStack stack, DataComponentType<ItemEnchantments> type, boolean replace, Level level) {
        ItemEnchantments ench = stack.get(type);
        if (ench == null || ench.isEmpty()) return false;
        int removed = 0;
        for (Holder<Enchantment> h : ench.keySet()) {
            if (isMarker(h)) removed++;
        }
        if (removed == 0) return false;

        ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ench);
        m.removeIf(BossEnchantCleaner::isMarker);
        if (replace) {
            List<Holder<Enchantment>> pool = new ArrayList<>();
            level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getTagOrEmpty(EnchantmentTags.IN_ENCHANTING_TABLE).forEach(pool::add);
            RandomSource random = level.getRandom();
            for (int i = 0; i < removed && !pool.isEmpty(); i++) {
                Holder<Enchantment> pick = pool.remove(random.nextInt(pool.size()));
                if (m.getLevel(pick) > 0 || !compatible(m, pick)) { i--; continue; }
                m.set(pick, 1);
                LOG.info("Replaced a boss marker enchantment on {} with {}", stack.getItem(), pick.getRegisteredName());
            }
        }
        stack.set(type, m.toImmutable());
        return true;
    }

    private static boolean compatible(ItemEnchantments.Mutable m, Holder<Enchantment> pick) {
        for (Holder<Enchantment> other : m.keySet()) {
            if (!Enchantment.areCompatible(other, pick)) return false;
        }
        return true;
    }

    static void cleanPlayer(ServerPlayer player) {
        var inv = player.getInventory();
        boolean changed = false;
        for (var list : List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack stack : list) {
                changed |= clean(stack, player.level());
            }
        }
        changed |= clean(player.containerMenu.getCarried(), player.level());
        if (changed) {
            inv.setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    /** Fixes the offer at the source before the trade screen opens, for villager-type NPCs. */
    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof AbstractVillager merchant)) return;
        for (MerchantOffer offer : merchant.getOffers()) {
            clean(offer.getResult(), event.getLevel());
        }
    }

    /** Safety net for books already owned, or bought from a merchant that isn't a villager. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0 && player.isAlive()) {
            cleanPlayer(player);
        }
    }

    /** Cleans stacks already sitting in chests, backpacks etc. as players open them. */
    private static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (Slot slot : event.getContainer().slots) {
            if (clean(slot.getItem(), player.level())) slot.setChanged();
        }
    }
}
