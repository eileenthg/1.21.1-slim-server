package com.github.eileenthg.colonyhandover.mapping;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * Soft integration with mods we do not depend on.
 *
 * <h2>Why everything here is resolved at runtime</h2>
 *
 * We deliberately have no compile-time dependency on these mods: they are
 * enhancements, not requirements, and a player without them must still be able
 * to run a handover. So professions and blocks are looked up by
 * {@link ResourceLocation} against the live registries, and every accessor
 * returns an {@link Optional} that is simply empty when the mod is absent.
 *
 * <h2>Lookups are lazy on purpose</h2>
 *
 * Registry contents do not exist while class initialisers run. Nothing here is
 * cached in a static field - each call asks the registry, which is cheap and
 * cannot go stale if a datapack reload changes what is registered.
 */
public final class OptionalMods {

    // --- Chef's Delight ---------------------------------------------------
    // Adds two vanilla villager professions with their own POIs. Not a
    // MineColonies addon, despite the name overlap with MineColonies' jobs.
    public static final String CHEFS_DELIGHT = "chefsdelight";
    private static final ResourceLocation CHEF = id(CHEFS_DELIGHT, "chef");
    private static final ResourceLocation COOK = id(CHEFS_DELIGHT, "cook");
    // Its POIs point at Farmer's Delight blocks, so Chef's Delight without
    // Farmer's Delight would give us a profession with no claimable job site.
    public static final String FARMERS_DELIGHT = "farmersdelight";
    private static final ResourceLocation COOKING_POT = id(FARMERS_DELIGHT, "cooking_pot");
    private static final ResourceLocation SKILLET = id(FARMERS_DELIGHT, "skillet");

    // --- Friends & Foes ---------------------------------------------------
    // The beekeeper profession lives in Friends&Foes itself, NOT in the
    // Beekeeper Hut addon - that addon is worldgen only and registers no
    // profession or POI. It is also behind a config toggle in Friends&Foes,
    // which is the other reason we check the registry rather than the mod list.
    public static final String FRIENDS_AND_FOES = "friendsandfoes";
    private static final ResourceLocation BEEKEEPER = id(FRIENDS_AND_FOES, "beekeeper");
    public static final String BEEKEEPER_HUT = "beekeeperhut";

    // --- More Villagers ----------------------------------------------------
    // The official More Villagers (6.x) registers its professions and POIs in code,
    // one workstation block each. Only the five with a MineColonies counterpart are
    // used; oceanographer, hunter and enderian have none (MineColonies has no
    // hunter job - herders stay butchers). Everything is looked up by id at runtime,
    // so the earlier fan port with the same ids worked unchanged.
    public static final String MORE_VILLAGERS = "morevillagers";

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static boolean isLoaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }

    private static Optional<VillagerProfession> profession(ResourceLocation rl) {
        return BuiltInRegistries.VILLAGER_PROFESSION.getOptional(rl);
    }

    private static Optional<Block> block(ResourceLocation rl) {
        return BuiltInRegistries.BLOCK.getOptional(rl);
    }

    /** {@code chefsdelight:chef}, if Chef's Delight registered it. */
    public static Optional<VillagerProfession> chef() {
        return profession(CHEF);
    }

    /** {@code chefsdelight:cook}, if Chef's Delight registered it. */
    public static Optional<VillagerProfession> cook() {
        return profession(COOK);
    }

    /**
     * {@code friendsandfoes:beekeeper}, if Friends&amp;Foes registered it.
     * Empty when the mod is absent <em>or</em> when its beekeeper profession is
     * switched off in config - the registry answers both cases correctly.
     */
    public static Optional<VillagerProfession> beekeeper() {
        return profession(BEEKEEPER);
    }

    /** Job site for {@link #chef()}: a Farmer's Delight cooking pot. */
    public static Optional<Block> cookingPot() {
        return block(COOKING_POT);
    }

    /** Job site for {@link #cook()}: a Farmer's Delight skillet. */
    public static Optional<Block> skillet() {
        return block(SKILLET);
    }

    /**
     * A More Villagers profession, but only if its workstation also exists.
     *
     * <p>Both are checked because assigning a profession whose job site cannot be
     * placed would leave the villager relying on the XP seed alone, which is a worse
     * result than the vanilla profession it would otherwise have had.
     */
    public static Optional<VillagerProfession> moreVillagers(String profession, String block) {
        return block(id(MORE_VILLAGERS, block)).isPresent()
                ? profession(id(MORE_VILLAGERS, profession))
                : Optional.empty();
    }

    /** A More Villagers workstation block, if registered. */
    public static Optional<Block> moreVillagersBlock(String block) {
        return block(id(MORE_VILLAGERS, block));
    }

    private OptionalMods() {}
}
