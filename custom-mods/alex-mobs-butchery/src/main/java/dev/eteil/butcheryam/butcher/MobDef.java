package dev.eteil.butcheryam.butcher;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

/**
 * One butcherable Alex's Mobs creature.
 *
 * <p>Adding another mob means adding an entry here plus its blocks, models and loot tables —
 * no new Java classes beyond the block's shapes.
 *
 * @param name      short id, e.g. {@code "emu"}
 * @param entityTag entity-type tag gating the death drop, e.g. {@code "alexsmobs:emu"}
 * @param enabled   config toggle
 * @param carcass   fresh carcass block
 * @param drained   drained carcass block, or null when the mob has no bleeding stage
 * @param head      severed head block, or null
 * @param lootPrefix prefix of this mob's loot tables; usually {@code name}, but the bald eagle's
 *                   tables are named {@code eagle_*} in the original addon
 */
public record MobDef(
        String name,
        String lootPrefix,
        String entityTag,
        ModConfigSpec.BooleanValue enabled,
        Supplier<Block> carcass,
        @Nullable Supplier<Block> drained,
        @Nullable Supplier<Block> head
) {

    /** Loot table id for one of this mob's drop stages, e.g. {@code emu_cut_1_drop}. */
    public String loot(String suffix) {
        return ButcheryAlexsMobs.MODID + ":blocks/" + lootPrefix + "_" + suffix;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public boolean bleeds() {
        return drained != null;
    }
}
