package net.eileenthg.gravetagcleaner;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gravestone Carryables and Gravestone Curios Compat tag every item with its slot at the very start of
 * Player.die (baguettelib's PlayerDeathEvent.Pre). The tag is only removed when the items come back out of a
 * grave through the owner's fillPlayerInventory. Anything else - a PlayerRevive knockdown that cancels the
 * death, a grave broken by another player, a grave that failed to place - leaves the tag on for good, and the
 * stack never merges with untagged copies again.
 *
 * A live player's inventory never legitimately holds tagged items (tagging and moving into the grave happen
 * inside the same die() call), so it is always safe to strip them here.
 */
@Mod(GraveTagCleaner.MODID)
public class GraveTagCleaner {
    public static final String MODID = "gravetagcleaner";
    static final Logger LOG = LoggerFactory.getLogger(MODID);

    private static final ResourceLocation[] COMPONENT_IDS = {
            ResourceLocation.fromNamespaceAndPath("gravestonecarryables", "carry_slot_data"),
            ResourceLocation.fromNamespaceAndPath("baguettelib", "curio_slot_data"),
    };
    private static List<DataComponentType<?>> types;
    private static boolean curiosLoaded;

    public GraveTagCleaner() {
        curiosLoaded = ModList.get().isLoaded("curios");
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDeathEvent.class, GraveTagCleaner::onDeath);
        NeoForge.EVENT_BUS.addListener(GraveTagCleaner::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(GraveTagCleaner::onContainerClose);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> types());
    }

    static List<DataComponentType<?>> types() {
        if (types == null) {
            List<DataComponentType<?>> found = new ArrayList<>();
            for (ResourceLocation id : COMPONENT_IDS) {
                BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(id).ifPresent(found::add);
            }
            LOG.info("Stripping {} grave slot component(s): {}", found.size(), found);
            types = found;
        }
        return types;
    }

    /** Returns true if anything was removed. */
    static boolean strip(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean changed = false;
        for (DataComponentType<?> type : types()) {
            if (stack.has(type)) {
                stack.remove(type);
                changed = true;
            }
        }
        return changed;
    }

    static int stripPlayer(ServerPlayer player) {
        int n = 0;
        var inv = player.getInventory();
        for (var list : List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack stack : list) {
                if (strip(stack)) n++;
            }
        }
        if (strip(player.containerMenu.getCarried())) n++;
        if (curiosLoaded) n += CuriosStrip.strip(player);
        if (n > 0) inv.setChanged();
        return n;
    }

    /** Death cancelled (PlayerRevive knockdown etc.) after the Pre-tagging already ran: undo it right away. */
    private static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() && event.getEntity() instanceof ServerPlayer player) {
            stripPlayer(player);
        }
    }

    /** Safety net for items that were already tagged or came in from a broken grave / ground drop. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0 && player.isAlive()) {
            stripPlayer(player);
        }
    }

    /** Cleans stacks already sitting in chests, backpacks etc. as players open them. */
    private static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        for (Slot slot : event.getContainer().slots) {
            if (strip(slot.getItem())) slot.setChanged();
        }
    }
}
