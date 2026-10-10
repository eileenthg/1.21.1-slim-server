package com.github.eileenthg.colonyhandover.mapping;

import com.minecolonies.api.blocks.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * MineColonies hut  ->  the vanilla blocks we must place so that MCA classifies
 * the building the way we intend.
 *
 * <h2>Why this table places blocks instead of just naming a type</h2>
 *
 * The obvious design would be "tell MCA this hut is a bakery". MCA does not
 * allow that. {@code VillageManager.commitBuilding(scan, forcedType)} rejects
 * the forced type unless it is already one of the types the scan matched:
 *
 * <pre>
 *     if (forcedType != null &amp;&amp; !scan.matchesType(forcedType))
 *         return validationResult.INVALID_TYPE;
 * </pre>
 *
 * and {@code scan.matchingTypes} comes from {@code getVisibleMatchingTypes()},
 * which keeps only types with {@code visible: true} (plus {@code house}).
 * So {@code forcedType} is a <em>disambiguator</em>, not an override: it can
 * pick between candidates a building already qualifies for, and nothing more.
 *
 * The only lever that actually decides a type is which blocks sit inside the
 * building volume. Hence this table is a shopping list, and {@code mcaType}
 * records what we expect to fall out, not what we command.
 *
 * <h2>How MCA breaks ties</h2>
 *
 * {@code Building.determineType()} takes all matching types sorted by
 * {@code priority} descending then {@code name} ascending, and takes the first.
 * Priorities in MCA shipped data: music_store 5; armory, blacksmith, infirmary,
 * inn, prison, storage 4; big_house 3; the thirteen plain workstation types 1;
 * town_center, graveyard, building 0.
 *
 * Two consequences worth knowing before reading the table:
 * <ul>
 *   <li>{@code building} declares no required blocks, so it matches everything
 *       and acts as the priority-0 fallback. Any hut that passes the flood fill
 *       becomes at least a generic building.</li>
 *   <li>At priority 1 the alphabetical tiebreak bites. {@code house} (one bed)
 *       sorts before {@code leatherworker}, {@code mason}, {@code toolsmith},
 *       {@code weaponsmith} and {@code weaving_mill}, so if one of those huts
 *       contains a bed it is typed {@code house} instead. See
 *       {@link #losesToHouse(String)}.</li>
 * </ul>
 */
public final class BuildingMapping {

    /** How the building gets registered with MCA. */
    public enum Anchor {
        /**
         * Normal path: MCA flood-fills from a source block and needs an
         * enclosed, roofed room with a door and at least Config.minBuildingSize
         * (default 32) interior blocks.
         */
        ROOM,
        /**
         * The {@code grouped: true} types, which are only {@code town_center}
         * and {@code graveyard}. {@code processBuilding} matches these on the
         * anchor block alone and skips room validation entirely, so they work
         * where a flood fill would fail. This is why the town hall is the
         * cheapest and most reliable thing to convert.
         */
        GROUPED,
        /**
         * Replace a leftover MineColonies marker block, in place, with a vanilla
         * workstation. For open-air work sites that can never pass a room scan:
         * the point is not to create an MCA building but to leave a job-site POI
         * where the work actually happens.
         *
         * <p>This also cleans up after {@code deleteColony}, which only removes
         * {@code AbstractBlockHut} instances. Scarecrows and plantation fields
         * descend from {@code AbstractBlockMinecolonies} instead, so they would
         * otherwise be left stranded across the fields forever.
         */
        MARKER_SWAP,
        /**
         * Place workstations at the hut's own position, expecting no MCA
         * building at all. For work sites whose geometry will never pass a room
         * scan - barns, mine shafts, open pens - but whose workers still need a
         * job site or they decay to unemployed.
         *
         * <p>The hut block is removed by {@code deleteColony} regardless, so the
         * workstation simply takes over the spot the workplace already occupied.
         */
        POI_ONLY,
        /** No sensible MCA analogue; let it fall through to {@code building}. */
        SKIP
    }

    /**
     * A block we place, and how many copies the target type requires.
     *
     * <p>Held as a {@link Supplier} rather than a {@link Block} so an entry can
     * name a block from another mod's registry, which does not exist yet when
     * this table's static initialiser runs. {@link #block()} resolves it, and
     * returns null if the block is not registered - callers must handle that.
     */
    public record Need(Supplier<Block> supplier, int count) {
        public Need(Block block, int count) {
            this(() -> block, count);
        }

        public static Need of(Block block) {
            return new Need(block, 1);
        }

        /** The resolved block, or null if nothing is registered under that id. */
        public Block block() {
            return supplier.get();
        }
    }

    /**
     * An MCA headstone, resolved from the registry at use time.
     *
     * <p>{@code cross_headstone} is MCA's own {@code Config.defaultHeadstoneType},
     * so seeded stones match the ones villagers place for themselves on death.
     */
    private static final Supplier<Block> MCA_HEADSTONE = () ->
            BuiltInRegistries.BLOCK.getOptional(
                    ResourceLocation.fromNamespaceAndPath("mca", "cross_headstone")).orElse(null);

    /**
     * @param mcaType   the type we expect MCA to derive, or null for SKIP
     * @param anchor    how it gets registered
     * @param forceable whether mcaType is visible, i.e. legal to pass as
     *                  {@code forcedType}. Non-visible types must be reached by
     *                  block placement alone.
     * @param needs     the target type's COMPLETE block requirement, not just
     *                  the part we expect to be missing. Most MineColonies
     *                  schematics already satisfy some of it - a Tavern has
     *                  beds, a Library has bookshelves - so the placement step
     *                  scans first and places only the shortfall. Declaring the
     *                  full requirement keeps this table readable as a spec and
     *                  stops it silently depending on what a schematic happens
     *                  to contain.
     */
    /**
     * @param extraPoi job-site blocks placed <em>in addition</em> to the type
     *                 requirement. Kept separate from {@code needs} so that list
     *                 stays an honest statement of what the MCA type demands:
     *                 a cooking pot in the Chef's Kitchen grants a profession
     *                 but contributes nothing to becoming a {@code butcher}.
     *                 Entries whose block is not registered resolve to null and
     *                 are skipped, which is how optional-mod blocks work.
     */
    /**
     * Whether a block is somewhere a villager can actually take a job.
     *
     * <p>Asked of the game rather than answered from a hand-written list.
     * {@code PoiTypes.forState} plus the {@code acquirable_job_site} tag is the same
     * test the villager brain uses, so anything a modpack registers as a workstation
     * counts here for free - Chef's Delight cook stations and the like - and nothing
     * can drift out of date.
     */
    public static boolean isWorkstation(Block block) {
        return PoiTypes.forState(block.defaultBlockState())
                .map(holder -> holder.is(PoiTypeTags.ACQUIRABLE_JOB_SITE))
                .orElse(false);
    }

    /**
     * Just the job sites from a conversion, ignoring everything else it asks for.
     *
     * <p>A conversion's {@code needs} mixes two unrelated things: blocks that make a
     * villager employable (a smoker, a lectern, a grindstone) and blocks that merely
     * satisfy MCA's building type (a cake, a jukebox, thirty-two bookshelves, four
     * chests). The handover places only the first kind. The second kind is the
     * player's to arrange - it is decoration and layout, it is what makes a village
     * look like someone lives there, and guessing at it was what produced rooms too
     * cramped to place into and buildings rolled back for want of space.
     */
    public static List<Need> workstations(Conversion plan) {
        final List<Need> stations = new ArrayList<>();
        for (final Need need : plan.needs()) {
            if (need.block() != null && isWorkstation(need.block())) {
                stations.add(need);
            }
        }
        for (final Need need : plan.extraPoi()) {
            if (need.block() != null && isWorkstation(need.block())) {
                stations.add(need);
            }
        }
        return stations;
    }

    /** Everything a conversion wants that is NOT a job site - the player's list. */
    public static List<Need> decor(Conversion plan) {
        final List<Need> rest = new ArrayList<>();
        for (final Need need : plan.needs()) {
            if (need.block() != null && !isWorkstation(need.block())) {
                rest.add(need);
            }
        }
        return rest;
    }

    public record Conversion(String mcaType, Anchor anchor, boolean forceable,
                             List<Need> needs, List<Need> extraPoi, Supplier<Block> marker) {}

    private static Conversion room(String type, boolean forceable, Need... needs) {
        return new Conversion(type, Anchor.ROOM, forceable, List.of(needs), List.of(), null);
    }

    private static Conversion roomPlusPoi(String type, boolean forceable,
                                          List<Need> needs, Need... extraPoi) {
        return new Conversion(type, Anchor.ROOM, forceable, needs, List.of(extraPoi), null);
    }

    private static Conversion grouped(String type, Need... needs) {
        // Grouped types are visible:false, so they are never forceable.
        return new Conversion(type, Anchor.GROUPED, false, List.of(needs), List.of(), null);
    }

    /** Workstations at the hut position; no MCA building expected. */
    private static Conversion poiOnly(Need... needs) {
        return new Conversion(null, Anchor.POI_ONLY, false, List.of(needs), List.of(), null);
    }

    /**
     * @param marker MineColonies block to find and replace. Resolved through a
     *               Supplier rather than referenced directly, because
     *               {@code ModBlocks} fields are populated during MineColonies'
     *               registration and would still be null when this table's
     *               static initialiser runs.
     */
    private static Conversion markerSwap(Supplier<Block> marker, Block replacement) {
        // No mcaType: the field will not become an MCA building, and that is
        // fine. We are placing a POI, not a room.
        return new Conversion(null, Anchor.MARKER_SWAP, false,
                List.of(Need.of(replacement)), List.of(), marker);
    }

    private static final Conversion SKIP =
            new Conversion(null, Anchor.SKIP, false, List.of(), List.of(), null);

    // --- Optional-mod job sites -------------------------------------------
    // Each resolves to null when the mod is absent, and a null block is simply
    // skipped at placement time. That is the whole conditionality mechanism.

    /** Farmer's Delight cooking pot - the {@code chefsdelight:chef} job site. */
    private static final Supplier<Block> CHEF_STATION = () ->
            OptionalMods.chef().isPresent() ? OptionalMods.cookingPot().orElse(null) : null;

    /** Farmer's Delight skillet - the {@code chefsdelight:cook} job site. */
    private static final Supplier<Block> COOK_STATION = () ->
            OptionalMods.cook().isPresent() ? OptionalMods.skillet().orElse(null) : null;

    /**
     * A beehive, but only when Friends&amp;Foes actually registered its
     * beekeeper profession. Without it a beehive would be inert decoration -
     * vanilla has no beekeeper - so we place nothing.
     */
    /**
     * A More Villagers workstation, else a vanilla one - or {@code null} when there is
     * no vanilla equivalent, which places nothing (the villager stays unemployed).
     *
     * <p>Resolved on every call, never cached: registry contents do not exist while
     * this class's static initialisers run.
     */
    private static Supplier<Block> moreVillagersOr(String block, Block vanilla) {
        return () -> OptionalMods.moreVillagersBlock(block).orElse(vanilla);
    }

    private static final Supplier<Block> APIARY = () ->
            OptionalMods.beekeeper().isPresent() ? Blocks.BEEHIVE : null;

    /**
     * Keyed by the MineColonies building id (the path of its ResourceLocation,
     * e.g. "baker" for minecolonies:baker). All 55 registered huts on
     * version/1.21 are listed, including the ones we deliberately skip, so a hut
     * added upstream shows up as a missing key rather than silently doing
     * nothing.
     */
    private static final Map<String, Conversion> MAP = Map.ofEntries(

            // --- Village anchors -------------------------------------------
            // A bell IS the town_center type (#mca:town_center = minecraft:bell).
            // Grouped, so no room validation. Convert this first and every later
            // building has a village to attach to.
            Map.entry("townhall", grouped("town_center", Need.of(Blocks.BELL))),
            // MCA graveyards want #mca:tombstones - its own headstones, which
            // MineColonies gravestones are not in. Rather than convert the
            // existing graves, seed EMPTY headstones on the old graveyard site
            // and let the village fill them itself: Relationship.onDeath looks
            // for an empty tombstone within 10 chunks before placing a new one
            // wherever the villager happened to die. Seeding here is what keeps
            // burials in the cemetery instead of scattered across the map.
            //
            // Grouped, so it needs no room scan; the "x3" is the non-grouped
            // requirement, but three stones also make it read as a graveyard.
            // The MineColonies graves stay put either way - they are not
            // AbstractBlockHut, so deleteColony leaves them alone.
            Map.entry("graveyard", grouped("graveyard", new Need(MCA_HEADSTONE, 3))),

            // --- Homes ------------------------------------------------------
            // Residences already contain beds, and MCA counts bed HEAD blocks,
            // so house (1 bed) or big_house (4) falls out with nothing placed.
            Map.entry("residence", room("house", true, Need.of(Blocks.WHITE_BED))),

            // --- Priority-4 targets (visible, therefore also forceable) ------
            // NOTE (cauldron / leatherworker): the cauldron in the two infirmary
            // entries below is also the vanilla leatherworker POI, so a passing
            // unemployed villager can claim it and set up shop in the hospital.
            // It cannot simply be dropped: MCA's infirmary type *requires*
            // #minecraft:cauldrons, and all four cauldron variants are in both
            // the tag and the POI set. Remove it and the hut loses infirmary and
            // falls back to house on its bed. The real mitigation is to seed the
            // intended occupant with >=1 villager XP so LoseUnimportantJobTask
            // cannot strip them - see section 9 of CLAUDE.md.
            Map.entry("tavern", room("inn", true,
                    Need.of(Blocks.JUKEBOX), Need.of(Blocks.SMOKER),
                    new Need(Blocks.WHITE_BED, 4))),
            Map.entry("hospital", room("infirmary", true,
                    Need.of(Blocks.BREWING_STAND), Need.of(Blocks.CAULDRON),
                    Need.of(Blocks.FLOWER_POT), Need.of(Blocks.WHITE_BED))),
            Map.entry("alchemist", room("infirmary", true,
                    Need.of(Blocks.BREWING_STAND), Need.of(Blocks.CAULDRON),
                    Need.of(Blocks.FLOWER_POT), Need.of(Blocks.WHITE_BED))),
            Map.entry("library", room("library", true,
                    new Need(Blocks.BOOKSHELF, 32), Need.of(Blocks.LECTERN))),
            Map.entry("university", room("library", true,
                    new Need(Blocks.BOOKSHELF, 32), Need.of(Blocks.LECTERN))),
            Map.entry("warehouse", room("storage", true,
                    Need.of(Blocks.LECTERN), new Need(Blocks.CHEST, 4))),
            Map.entry("stash", room("storage", true,
                    Need.of(Blocks.LECTERN), new Need(Blocks.CHEST, 4))),
            Map.entry("deliveryman", room("storage", true,
                    Need.of(Blocks.LECTERN), new Need(Blocks.CHEST, 4))),
            Map.entry("blacksmith", room("blacksmith", true,
                    Need.of(Blocks.GRINDSTONE), new Need(Blocks.BLAST_FURNACE, 2),
                    Need.of(Blocks.ANVIL))),
            Map.entry("combatacademy", room("armory", true,
                    Need.of(Blocks.SMITHING_TABLE), Need.of(Blocks.GRINDSTONE),
                    Need.of(Blocks.ANVIL))),
            Map.entry("barracks", room("armory", true,
                    Need.of(Blocks.SMITHING_TABLE), Need.of(Blocks.GRINDSTONE),
                    Need.of(Blocks.ANVIL))),
            // Archery goes to fletcher, not armory: a fletching table fits the
            // bow-and-arrow theme, and it is also the only FLETCHER job site in
            // the colony besides the fletcher's own hut.
            Map.entry("archery", room("fletcher", false, Need.of(Blocks.FLETCHING_TABLE))),

            // --- Priority-1 targets (invisible: reached by blocks alone) -----
            Map.entry("baker", room("bakery", false,
                    Need.of(Blocks.CAKE), Need.of(Blocks.SMOKER))),
            // "Dining Hall", staffed by the Waiter. Chef's Delight's cook sells
            // handheld food and drinks, which is what a waiter hands out.
            Map.entry("cook", roomPlusPoi("butcher", false,
                    List.of(Need.of(Blocks.SMOKER)), new Need(COOK_STATION, 1))),
            // "Chef's Kitchen", staffed by the Chef. Chef's Delight's chef sells
            // full meals and feast blocks - the kitchen's actual output.
            Map.entry("kitchen", roomPlusPoi("butcher", false,
                    List.of(Need.of(Blocks.SMOKER)), new Need(CHEF_STATION, 1))),
            Map.entry("smeltery", room("armorer", false, Need.of(Blocks.BLAST_FURNACE))),
            Map.entry("stonesmeltery", room("armorer", false, Need.of(Blocks.BLAST_FURNACE))),
            Map.entry("fisherman", room("fishermans_hut", false, Need.of(Blocks.BARREL))),
            Map.entry("fletcher", room("fletcher", false, Need.of(Blocks.FLETCHING_TABLE))),
            Map.entry("school", room("bookkeeper", false, Need.of(Blocks.LECTERN))),
            // The next three sort AFTER "house" at priority 1: a stray bed wins.
            Map.entry("stonemason", room("mason", false, Need.of(Blocks.STONECUTTER))),
            Map.entry("mechanic", room("toolsmith", false, Need.of(Blocks.SMITHING_TABLE))),
            Map.entry("dyer", room("weaving_mill", false, Need.of(Blocks.LOOM))),

            // --- Marker swaps (POI only, no building) ------------------------
            // Fields can never pass the room scan, so there is no MCA type to
            // aim at. What they can have is a composter, which is the vanilla
            // FARMER job site - and without one, every converted farmer loses
            // its profession to LoseUnimportantJobTask. See CLAUDE.md section 9.
            Map.entry("farmer", markerSwap(() -> ModBlocks.blockScarecrow, Blocks.COMPOSTER)),
            Map.entry("plantation", markerSwap(() -> ModBlocks.blockPlantationField, Blocks.COMPOSTER)),

            // --- Deliberately skipped ---------------------------------------
            // Open-air work sites: no roof and no door, so validateBuilding
            // returns NO_DOOR or TOO_SMALL whatever we place. The workers still
            // use their farmland and animals through vanilla AI.
            Map.entry("florist", poiOnly(new Need(moreVillagersOr("gardening_table", null), 1))),
            Map.entry("composter", SKIP),
            Map.entry("lumberjack", poiOnly(new Need(moreVillagersOr("woodworking_table", null), 1))),
            Map.entry("stable", SKIP),

            // --- POI-only work sites ----------------------------------------
            // Barns, pens and shafts will never pass a room scan, but their
            // workers still need a claimable job site. Placing one at the hut
            // position keeps the profession from decaying and puts the
            // workstation where the work actually happened.
            Map.entry("cowboy", poiOnly(Need.of(Blocks.SMOKER), Need.of(Blocks.CAULDRON))),
            Map.entry("swineherder", poiOnly(Need.of(Blocks.SMOKER))),
            Map.entry("chickenherder", poiOnly(Need.of(Blocks.SMOKER))),
            Map.entry("rabbithutch", poiOnly(Need.of(Blocks.SMOKER))),
            Map.entry("shepherd", poiOnly(Need.of(Blocks.LOOM))),
            // Only useful with Friends&Foes; otherwise nothing is placed.
            Map.entry("beekeeper", poiOnly(new Need(APIARY, 1))),
            // Six more stonecutters, which is what closes the MASON shortfall:
            // seven jobs map to MASON and the stonemason's hut supplied one.
            Map.entry("miner", poiOnly(new Need(moreVillagersOr("mining_bench", Blocks.STONECUTTER), 1))),
            Map.entry("simplequarry", poiOnly(new Need(moreVillagersOr("mining_bench", Blocks.STONECUTTER), 1))),
            Map.entry("mediumquarry", poiOnly(new Need(moreVillagersOr("mining_bench", Blocks.STONECUTTER), 1))),
            Map.entry("largequarry", poiOnly(new Need(moreVillagersOr("mining_bench", Blocks.STONECUTTER), 1))),
            Map.entry("concretemixer", poiOnly(Need.of(Blocks.STONECUTTER))),
            Map.entry("crusher", poiOnly(Need.of(Blocks.STONECUTTER))),
            Map.entry("enchanter", poiOnly(Need.of(Blocks.BREWING_STAND))),
            Map.entry("netherworker", poiOnly(new Need(moreVillagersOr("decayed_workbench", Blocks.BREWING_STAND), 1))),
            // The Mystical Site has no worker of its own - it is a pure
            // happiness monument. It gets a brewing stand anyway, because MCA
            // ships no cleric or church building type at all, and a shrine is
            // the most fitting home the village can offer its clerics. It also
            // happens to close the last CLERIC shortfall: five jobs, five stands.
            Map.entry("mysticalsite", poiOnly(Need.of(Blocks.BREWING_STAND))),

            Map.entry("guardtower", SKIP), Map.entry("barrackstower", SKIP),
            Map.entry("gatehouse", SKIP),
            // No MCA analogue exists. Note MCA ships no cleric or church type at
            // all, so the enchanter and the mystical site have nowhere to go.
            Map.entry("glassblower", SKIP),
            Map.entry("sawmill", poiOnly(new Need(moreVillagersOr("woodworking_table", null), 1))),
            Map.entry("sifter", SKIP),
            Map.entry("builder", poiOnly(new Need(moreVillagersOr("blueprint_table", Blocks.STONECUTTER), 1))),
            Map.entry("postbox", SKIP)
    );

    /**
     * Priority-1 MCA types that lose the alphabetical tiebreak to {@code house}.
     * If one of these is the intended type and the hut contains a bed, expect
     * {@code house} instead. Worth logging as a warning during conversion.
     */
    public static boolean losesToHouse(String mcaType) {
        return switch (mcaType) {
            case "leatherworker", "mason", "toolsmith", "weaponsmith", "weaving_mill" -> true;
            default -> false;
        };
    }

    public static Conversion forHut(String mineColoniesId) {
        return MAP.getOrDefault(mineColoniesId, SKIP);
    }

    public static boolean isKnown(String mineColoniesId) {
        return MAP.containsKey(mineColoniesId);
    }

    private BuildingMapping() {}
}
