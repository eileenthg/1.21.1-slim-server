package com.github.eileenthg.colonyhandover.mapping;

import net.conczin.mca.registry.ProfessionsMCA;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

/**
 * Vanilla profession -> the block that is its job-site POI.
 *
 * <h2>Why this table has to exist</h2>
 *
 * Assigning a profession is not enough to make it stick.
 * {@code LoseUnimportantJobTask} resets a villager to {@code NONE} when it has
 * no {@code JOB_SITE} memory, zero villager XP, level &lt;= 1, and a profession
 * outside {@code ProfessionsMCA.IS_IMPORTANT} - which contains only the six MCA
 * professions, no vanilla one. So every vanilla profession we hand out needs
 * either a claimable workstation or seeded XP, or the villager quietly goes
 * back to being unemployed.
 *
 * <h2>One block, one villager</h2>
 *
 * Every one of these POI types is registered with {@code maxTickets = 1}
 * (see {@code PoiTypes.register(..., 1, 1)}), so a workstation supports exactly
 * <em>one</em> villager. Provisioning is therefore per head, not per profession:
 * six citizens mapped to {@code MASON} need six stonecutters, not one. Use
 * {@link #required(Map)} to size that up before placing anything.
 */
public final class ProfessionPoi {

    private static final Map<VillagerProfession, Block> POI = Map.of(
            VillagerProfession.ARMORER, Blocks.BLAST_FURNACE,
            VillagerProfession.BUTCHER, Blocks.SMOKER,
            VillagerProfession.CARTOGRAPHER, Blocks.CARTOGRAPHY_TABLE,
            VillagerProfession.CLERIC, Blocks.BREWING_STAND,
            VillagerProfession.FARMER, Blocks.COMPOSTER,
            VillagerProfession.FISHERMAN, Blocks.BARREL,
            VillagerProfession.FLETCHER, Blocks.FLETCHING_TABLE,
            VillagerProfession.LEATHERWORKER, Blocks.CAULDRON,
            VillagerProfession.LIBRARIAN, Blocks.LECTERN,
            VillagerProfession.MASON, Blocks.STONECUTTER
    );

    // Map.of tops out at 10 pairs, so the rest go in a second block.
    private static final Map<VillagerProfession, Block> POI_2 = Map.of(
            VillagerProfession.SHEPHERD, Blocks.LOOM,
            VillagerProfession.TOOLSMITH, Blocks.SMITHING_TABLE,
            VillagerProfession.WEAPONSMITH, Blocks.GRINDSTONE
    );

    /**
     * The workstation block this profession claims, or null when it needs none.
     *
     * <p>Null is correct, not a gap, for three cases: {@code NONE} and
     * {@code NITWIT} have no job site, and the MCA professions (guard, archer,
     * and friends) are registered with {@code PoiType.NONE} <em>and</em> are in
     * {@code IS_IMPORTANT}, so they neither claim a workstation nor decay
     * without one.
     */
    public static Block workstationFor(VillagerProfession profession) {
        final Block b = POI.get(profession);
        if (b != null) {
            return b;
        }
        final Block b2 = POI_2.get(profession);
        if (b2 != null) {
            return b2;
        }
        return moddedWorkstationFor(profession);
    }

    /**
     * Job sites for professions added by optional mods.
     *
     * <p>Resolved at call time rather than in the tables above, because these
     * professions and their blocks may not exist. Compared by identity against
     * the live registry entry, so a mod that is absent simply never matches.
     */
    private static Block moddedWorkstationFor(VillagerProfession profession) {
        if (OptionalMods.chef().map(p -> p == profession).orElse(false)) {
            // farmersdelight:cooking_pot - null if Chef's Delight is installed
            // without Farmer's Delight, which the preview reports as a shortfall.
            return OptionalMods.cookingPot().orElse(null);
        }
        if (OptionalMods.cook().map(p -> p == profession).orElse(false)) {
            return OptionalMods.skillet().orElse(null);
        }
        if (OptionalMods.beekeeper().map(p -> p == profession).orElse(false)) {
            // Friends&Foes accepts any beehive or bee nest via a POI tag; plain
            // vanilla beehive is the one guaranteed to exist and to qualify.
            return Blocks.BEEHIVE;
        }
        return null;
    }

    /** True if this profession survives without a workstation. */
    public static boolean isSelfSustaining(VillagerProfession profession) {
        return ProfessionsMCA.IS_IMPORTANT.contains(profession)
                || profession == VillagerProfession.NONE
                || profession == VillagerProfession.NITWIT;
    }

    /**
     * Given a headcount per profession, how many of each workstation block the
     * colony needs. Professions that need no workstation are omitted.
     *
     * @param headcount how many converted villagers get each profession
     * @return block -> how many of it must exist for every villager to keep its job
     */
    public static Map<Block, Integer> required(Map<VillagerProfession, Integer> headcount) {
        final Map<Block, Integer> needed = new java.util.LinkedHashMap<>();
        headcount.forEach((profession, count) -> {
            if (isSelfSustaining(profession)) {
                return;
            }
            final Block block = workstationFor(profession);
            if (block != null) {
                needed.merge(block, count, Integer::sum);
            }
        });
        return needed;
    }

    private ProfessionPoi() {}
}
