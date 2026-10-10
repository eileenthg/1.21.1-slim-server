package dev.eteil.butcheryam2.init;

import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block items, plus the two drops no other mod has: the warped mosco's proboscis and a generic insect wing. */
public final class ModItems {

    public static final DeferredRegister.Items REGISTRY =
            DeferredRegister.createItems(ButcheryAlexsMobs2.MODID);

    /** The warped mosco's own proboscis; interchangeable with the crimson mosquito's (item tag). */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> WARPED_MOSCO_PROBOSCIS =
            REGISTRY.registerSimpleItem("warped_mosco_proboscis");
    /** A generic insect wing; interchangeable with Butchery's bee wings (item tag). */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> INSECT_WING =
            REGISTRY.registerSimpleItem("insect_wing");

    static {
        for (var block : ModBlocks.ALL) {
            REGISTRY.registerSimpleBlockItem(block);
        }
    }

    private ModItems() {}
}
