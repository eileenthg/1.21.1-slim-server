package com.github.eileenthg.colonyhandover.mapping;

import com.minecolonies.api.colony.jobs.ModJobs;
import net.conczin.mca.registry.ProfessionsMCA;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.Map;
import java.util.Optional;

/**
 * MineColonies job -> the profession the converted MCA villager is given.
 *
 * <h2>Why the keys are ModJobs constants</h2>
 *
 * Keying on {@code ModJobs.BAKER_ID} rather than the string {@code "baker"}
 * means an upstream rename breaks this file at compile time instead of silently
 * dropping a job to the fallback at runtime. MineColonies ships snapshots almost
 * daily, so that trade is worth the import.
 *
 * <h2>What the values can be</h2>
 *
 * MCA villagers are ordinary {@code Villager}s underneath: the profession lives
 * in vanilla {@code VillagerData}, and {@code VillagerEntityMCA.setProfession}
 * just writes it. So any vanilla {@link VillagerProfession} is a legal value,
 * and so are the six MCA adds in {@link ProfessionsMCA} (guard, archer, outlaw,
 * adventurer, mercenary, cultist). Only guard and archer make sense here.
 *
 * <h2>The NONE fallback is deliberate, not a cop-out</h2>
 *
 * Vanilla villagers with {@code VillagerProfession.NONE} acquire a profession by
 * claiming a nearby workstation POI through their own AI. Every job with no
 * defensible analogue is therefore mapped to NONE rather than to something
 * arbitrary: those villagers pick up whatever trade the converted buildings
 * actually offer, which is usually a better answer than one we could hardcode.
 * A MineColonies builder becoming a mason because we said so is worse than one
 * wandering to the smoker we placed and becoming a butcher on its own.
 */
public final class JobMapping {

    // COOKASSISTANT_ID is @Deprecated upstream (folded into chef) but still
    // registered, so citizens in colonies created before that change can still
    // hold the job. We keep translating it and suppress the warning here rather
    // than project-wide: -Xlint:deprecation stays on precisely so the next
    // upstream deprecation shows up in the build instead of going unnoticed.
    @SuppressWarnings("deprecation")
    private static final Map<ResourceLocation, VillagerProfession> MAP = Map.ofEntries(

            // --- Direct vanilla equivalents ---------------------------------
            Map.entry(ModJobs.FARMER_ID, VillagerProfession.FARMER),
            Map.entry(ModJobs.FISHERMAN_ID, VillagerProfession.FISHERMAN),
            Map.entry(ModJobs.FLETCHER_ID, VillagerProfession.FLETCHER),
            Map.entry(ModJobs.SHEPHERD_ID, VillagerProfession.SHEPHERD),
            Map.entry(ModJobs.STONEMASON_ID, VillagerProfession.MASON),

            // --- Food chain -> butcher --------------------------------------
            // Vanilla has no baker or cook, and butcher is the only food trade.
            Map.entry(ModJobs.BAKER_ID, VillagerProfession.BUTCHER),
            Map.entry(ModJobs.COOK_ID, VillagerProfession.BUTCHER),
            Map.entry(ModJobs.CHEF_ID, VillagerProfession.BUTCHER),
            Map.entry(ModJobs.COOKASSISTANT_ID, VillagerProfession.BUTCHER),
            Map.entry(ModJobs.COWBOY_ID, VillagerProfession.BUTCHER),
            Map.entry(ModJobs.SWINE_HERDER_ID, VillagerProfession.BUTCHER),
            Map.entry(ModJobs.CHICKEN_HERDER_ID, VillagerProfession.BUTCHER),
            Map.entry(ModJobs.RABBIT_ID, VillagerProfession.BUTCHER),

            // --- Stone and ore ----------------------------------------------
            Map.entry(ModJobs.MINER_ID, VillagerProfession.MASON),
            Map.entry(ModJobs.QUARRY_MINER_ID, VillagerProfession.MASON),
            Map.entry(ModJobs.CRUSHER_ID, VillagerProfession.MASON),
            Map.entry(ModJobs.SIFTER_ID, VillagerProfession.MASON),
            Map.entry(ModJobs.CONCRETE_ID, VillagerProfession.MASON),
            Map.entry(ModJobs.GLASSBLOWER_ID, VillagerProfession.MASON),
            Map.entry(ModJobs.SMELTER_ID, VillagerProfession.ARMORER),
            Map.entry(ModJobs.STONE_SMELTERY_ID, VillagerProfession.ARMORER),

            // --- Crafting ---------------------------------------------------
            Map.entry(ModJobs.BLACKSMITH_ID, VillagerProfession.TOOLSMITH),
            Map.entry(ModJobs.MECHANIC_ID, VillagerProfession.TOOLSMITH),
            Map.entry(ModJobs.DYER_ID, VillagerProfession.SHEPHERD),
            Map.entry(ModJobs.STABLEMASTER_ID, VillagerProfession.LEATHERWORKER),

            // --- Growing ----------------------------------------------------
            Map.entry(ModJobs.PLANTER_ID, VillagerProfession.FARMER),
            Map.entry(ModJobs.FLORIST_ID, VillagerProfession.FARMER),
            Map.entry(ModJobs.COMPOSTER_ID, VillagerProfession.FARMER),
            Map.entry(ModJobs.BEEKEEPER_ID, VillagerProfession.FARMER),

            // --- Books and faith --------------------------------------------
            Map.entry(ModJobs.TEACHER_ID, VillagerProfession.LIBRARIAN),
            Map.entry(ModJobs.RESEARCHER_ID, VillagerProfession.LIBRARIAN),
            Map.entry(ModJobs.ENCHANTER_ID, VillagerProfession.CLERIC),
            Map.entry(ModJobs.ALCHEMIST_ID, VillagerProfession.CLERIC),
            Map.entry(ModJobs.HEALER_ID, VillagerProfession.CLERIC),
            Map.entry(ModJobs.DRUID_ID, VillagerProfession.CLERIC),
            Map.entry(ModJobs.UNDERTAKER_ID, VillagerProfession.CLERIC),

            // --- Military -> MCA custom professions -------------------------
            // These are MCA additions, not vanilla. Guards and archers get real
            // combat AI in MCA, so the colony keeps a defence force instead of
            // turning into thirteen unarmed librarians.
            Map.entry(ModJobs.KNIGHT_ID, ProfessionsMCA.GUARD),
            Map.entry(ModJobs.HUSCARL_ID, ProfessionsMCA.GUARD),
            Map.entry(ModJobs.CAVALRY_ID, ProfessionsMCA.GUARD),
            Map.entry(ModJobs.KNIGHT_TRAINING_ID, ProfessionsMCA.GUARD),
            Map.entry(ModJobs.ARCHER_ID, ProfessionsMCA.ARCHER),
            Map.entry(ModJobs.MARKSMAN_ID, ProfessionsMCA.ARCHER),
            Map.entry(ModJobs.ARCHER_TRAINING_ID, ProfessionsMCA.ARCHER),

            // --- Explicitly unemployed --------------------------------------
            // Students and pupils are children; MCA grows them up and they pick
            // a trade later, exactly as vanilla village children do.
            Map.entry(ModJobs.STUDENT_ID, VillagerProfession.NONE),
            Map.entry(ModJobs.PUPIL_ID, VillagerProfession.NONE),
            // No vanilla or MCA analogue: let the AI decide from the buildings.
            Map.entry(ModJobs.BUILDER_ID, VillagerProfession.MASON),
            Map.entry(ModJobs.DELIVERY_ID, VillagerProfession.NONE),
            Map.entry(ModJobs.LUMBERJACK_ID, VillagerProfession.NONE),
            Map.entry(ModJobs.SAWMILL_ID, VillagerProfession.NONE),
            Map.entry(ModJobs.NETHERWORKER_ID, VillagerProfession.CLERIC),
            Map.entry(ModJobs.PLACEHOLDER_ID, VillagerProfession.NONE)
    );

    /**
     * Never returns null. An unrecognised job (a new one upstream, or an addon
     * job from another mod) becomes an unemployed villager, which is the safe
     * outcome: it will claim a workstation on its own.
     *
     * <p>Optional mods are applied on top of the base table here rather than
     * baked into it, because their professions do not exist yet when this
     * class initialises - and may not exist at all.
     */
    public static VillagerProfession forJob(ResourceLocation jobId) {
        return upgrade(jobId).orElseGet(() -> MAP.getOrDefault(jobId, VillagerProfession.NONE));
    }

    /**
     * A better profession than the vanilla fallback, when a supporting mod is
     * installed. Empty means "use the base table".
     *
     * <p>The cooking jobs are the clear win: MineColonies distinguishes chef,
     * cook, cook assistant and baker, and vanilla flattens all of them onto
     * BUTCHER. Chef's Delight restores the distinction.
     */
    @SuppressWarnings("deprecation") // COOKASSISTANT_ID, same reason as above
    private static Optional<VillagerProfession> upgrade(ResourceLocation jobId) {
        if (jobId == null) {
            return Optional.empty();
        }
        // Chef's Delight: chef keeps the cooking pot, the rest of the kitchen
        // staff become cooks on skillets.
        if (jobId.equals(ModJobs.CHEF_ID)) {
            return OptionalMods.chef();
        }
        if (jobId.equals(ModJobs.COOK_ID) || jobId.equals(ModJobs.COOKASSISTANT_ID)) {
            return OptionalMods.cook();
        }
        // More Villagers: a closer trade for jobs vanilla only approximates, or
        // gives nothing at all. Each checks its workstation exists too.
        if (jobId.equals(ModJobs.BUILDER_ID)) {
            return OptionalMods.moreVillagers("engineer", "blueprint_table");
        }
        if (jobId.equals(ModJobs.NETHERWORKER_ID)) {
            return OptionalMods.moreVillagers("netherian", "decayed_workbench");
        }
        if (jobId.equals(ModJobs.LUMBERJACK_ID) || jobId.equals(ModJobs.SAWMILL_ID)) {
            return OptionalMods.moreVillagers("woodworker", "woodworking_table");
        }
        if (jobId.equals(ModJobs.MINER_ID) || jobId.equals(ModJobs.QUARRY_MINER_ID)) {
            return OptionalMods.moreVillagers("miner", "mining_bench");
        }
        if (jobId.equals(ModJobs.FLORIST_ID)) {
            return OptionalMods.moreVillagers("florist", "gardening_table");
        }
        // Friends&Foes: a real beekeeper instead of a generic farmer.
        if (jobId.equals(ModJobs.BEEKEEPER_ID)) {
            return OptionalMods.beekeeper();
        }
        return Optional.empty();
    }

    /** True if we have a considered answer for this job, rather than the fallback. */
    public static boolean isKnown(ResourceLocation jobId) {
        return MAP.containsKey(jobId);
    }

    private JobMapping() {}
}
