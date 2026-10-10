package com.github.eileenthg.colonyhandover.handover;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import com.github.eileenthg.colonyhandover.coffer.CofferData;
import com.github.eileenthg.colonyhandover.mapping.BuildingMapping;
import com.github.eileenthg.colonyhandover.mapping.JobMapping;
import com.github.eileenthg.colonyhandover.place.Enclosure;
import com.github.eileenthg.colonyhandover.place.Placer;
import com.github.eileenthg.colonyhandover.place.PlacementEngine;
import com.github.eileenthg.colonyhandover.place.PlacementJournal;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.inventory.InventoryCitizen;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.server.world.data.FamilyTree;
import net.conczin.mca.server.world.data.FamilyTreeNode;
import net.conczin.mca.server.world.data.Village;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The irreversible half, as five separately callable stages.
 *
 * <h2>Order, and why this one</h2>
 *
 * <ol>
 *   <li>{@link #delete} with {@code canDestroy = false} - structures survive, hut
 *       marker blocks and citizens do not (finding #2).</li>
 *   <li>{@link #place} workstations against remembered coordinates.</li>
 *   <li>{@link #spawn} one MCA villager per snapshotted citizen.</li>
 *   <li>{@link #wire} the family tree, once every villager has a UUID (finding #5).</li>
 *   <li>{@link #scan} each building so MCA adopts it.</li>
 * </ol>
 *
 * Deletion goes first because every later step writes to positions MineColonies
 * still considers its own until it is gone - finding #10, and the field loss that
 * taught it. Placement goes before scanning because a scan types a building on the
 * blocks it has at that moment; placing afterwards would mean scanning twice.
 *
 * <h2>Why stages rather than one method</h2>
 *
 * The whole operation is irreversible and touches two mods' data models, so when
 * something goes wrong "it failed" is not a useful report - you need to know which
 * stage. Each stage here is callable on its own and returns its own outcome record,
 * which is what {@code handover debug} steps through. {@link #run} simply calls
 * them in order for the normal path, so debug and live share one code path rather
 * than drifting apart.
 */
public final class Handover {

    /** One villager XP is enough; see {@link #spawn}. */
    private static final int SEED_XP = 1;

    private static final int SITE_RADIUS = 6;
    private static final int SITE_VERTICAL = 3;
    private static final int SEED_RADIUS = 8;
    private static final int SEED_VERTICAL = 6;

    public record PlaceOutcome(int stations, int anchors, int swapped,
                               int shortOfRoom, int skipped) {
        public int touched() {
            return stations + anchors + swapped;
        }
    }

    public record SpawnOutcome(int spawned, int failed, int professions,
                               Map<Integer, VillagerEntityMCA> villagers) {}

    public record WireOutcome(int couples, int parentLinks, int deceased) {}

    public record ScanOutcome(int adopted, int refused) {}

    public record Result(PlaceOutcome placed, SpawnOutcome spawned,
                         WireOutcome wired, ScanOutcome scanned) {}

    /** The normal path: every stage, in order, no pauses. */
    public static Result run(ServerLevel level, ColonySnapshot snap) {
        delete(level, snap);
        final PlaceOutcome placed = place(level, snap);
        final SpawnOutcome spawned = spawn(level, snap);
        final WireOutcome wired = wire(level, snap, spawned.villagers());
        final ScanOutcome scanned = scan(level, snap);
        return new Result(placed, spawned, wired, scanned);
    }

    // ---------------------------------------------------------------- stage 1

    /**
     * The point of no return. Structures survive; citizens and hut blocks do not.
     *
     * @return how many citizen entities were retired quietly beforehand
     */
    public static int delete(ServerLevel level, ColonySnapshot snap) {
        final int rescuedBefore = CofferData.get(level).size();
        final int retired = retireCitizens(level, snap.colonyId());
        IMinecoloniesAPI.getInstance().getColonyManager()
                .deleteColonyByWorld(snap.colonyId(), false, level);

        // Only once the hut block is actually gone, and only if there is anything
        // to come back for.
        if (CofferData.get(level).size() > rescuedBefore) {
            placeCoffer(level, snap.center());
        }
        return retired;
    }

    /**
     * Removes the citizen entities before MineColonies can kill them.
     *
     * <p>{@code ColonyManager.deleteColony} does
     * {@code citizenData.getEntity().ifPresent(e -> e.die(...))}, and
     * {@code EntityCitizen.die} is not quiet about it: it composes a death message
     * with a distance and direction and sends it to every colony manager
     * ({@code EntityCitizen:1627-1651}), and it spawns a grave. For a colony of any
     * size that is a screen of obituaries for people who are not really dying -
     * they are being handed a new life as villagers a stage later.
     *
     * <p>{@code CitizenData.getEntity()} returns empty as soon as the entity
     * {@code isRemoved()} (`CitizenData:391`), so discarding first turns that whole
     * loop into a no-op. No messages, no graves, and no inventories scattered
     * across the village.
     *
     * <p>Their inventories are not lost with them. Each is emptied into
     * {@link CofferData} first, to be collected through
     * {@code /colonyhandover coffer}. A villager cannot inherit a colonist's
     * belongings and dropping several hundred stacks would carpet the village, so
     * holding them somewhere retrievable is the only option that neither destroys
     * goods nor makes a mess.
     */
    private static int retireCitizens(ServerLevel level, int colonyId) {
        final IColony colony = IMinecoloniesAPI.getInstance().getColonyManager()
                .getColonyByWorld(colonyId, level);
        if (colony == null) {
            return 0;
        }
        final CofferData coffer = CofferData.get(level);
        int retired = 0;
        for (final ICitizenData citizen
                : new ArrayList<>(colony.getCitizenManager().getCitizens())) {
            // Lift the inventory before the entity goes. Discarding an entity does
            // not drop anything, which is the whole point - but it would also lose
            // everything the colonist owned, so it has to be rescued first.
            final InventoryCitizen inventory = citizen.getInventory();
            if (inventory != null) {
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    final ItemStack stack = inventory.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        coffer.add(stack.copy());
                        inventory.setStackInSlot(slot, ItemStack.EMPTY);
                    }
                }
            }
            final var entity = citizen.getEntity();
            if (entity.isPresent()) {
                entity.get().discard();
                retired++;
            }
        }
        return retired;
    }

    /**
     * Stands a coffer where the colony was administered from.
     *
     * <p>The colony centre is the town hall's own position, and deletion removes
     * every {@code AbstractBlockHut} unconditionally (finding #2) - so by the time
     * this runs that spot is a hole in the middle of the most recognisable building
     * in the village. It is the best possible place to leave something people are
     * meant to find, and it needed filling anyway.
     *
     * <p>Placed <em>after</em> deletion rather than over the hut block beforehand.
     * Replacing the block early would also destroy its block entity, and
     * {@code building.destroy()} still has to run against that building - no reason
     * to hand MineColonies a gutted tile entity when waiting costs nothing.
     *
     * <p>Runs only when something was actually rescued. A coffer standing over an
     * empty list would be a permanent fixture advertising nothing.
     *
     * <p>Not journalled, deliberately. {@code /colonyhandover undo} reverts
     * workstation placement, and reverting this would remove the way back to the
     * goods while leaving the goods themselves in level data - the command would
     * still reach them, but only if you knew to ask. Better that the signpost
     * outlives the bookkeeping.
     */
    private static void placeCoffer(ServerLevel level, BlockPos townHall) {
        final BlockState coffer = ColonyHandover.COFFER.get().defaultBlockState();

        if (level.getBlockState(townHall).canBeReplaced()
                && coffer.canSurvive(level, townHall)) {
            level.setBlock(townHall, coffer, Block.UPDATE_ALL);
            return;
        }
        // Immediately around it, if the exact spot will not take a block - better
        // a coffer a metre off than no way to find the goods.
        for (final BlockPos candidate : BlockPos.betweenClosed(
                townHall.offset(-1, 0, -1), townHall.offset(1, 1, 1))) {
            if (level.getBlockState(candidate).canBeReplaced()
                    && coffer.canSurvive(level, candidate)) {
                level.setBlock(candidate.immutable(), coffer, Block.UPDATE_ALL);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- stage 2

    /**
     * Workstations, against the snapshot's coordinates rather than live buildings.
     *
     * <p>Three anchor kinds are handled, and they need different treatment:
     * ROOM huts go through {@link PlacementEngine}, which scans, places and rolls
     * back on failure. GROUPED and POI_ONLY huts have nothing to verify against -
     * a bell anchors a town centre on its own and a job site just needs to exist -
     * so those place their blocks beside the hut and stop.
     *
     * <p>MARKER_SWAP replaces field markers in place. It is the one that cost real
     * colony state (finding #10), and running it here - after deletion, with no
     * registry left to deregister anything - is precisely the fix. See
     * {@link #swapMarkers}.
     */
    /**
     * Workstations, and nothing else.
     *
     * <p>This used to try to satisfy MCA's whole building type - place the lectern
     * <em>and</em> four chests, the smoker <em>and</em> the cake, thirty-two
     * bookshelves - scanning each room, ranking floor cells, and rolling the lot back
     * when it did not fit. In a real colony it did not fit often: rooms are furnished,
     * and the cells a villager could stand in are exactly the cells a player already
     * put a table in. Whole buildings reverted for want of space.
     *
     * <p>So the handover now places only what makes a villager <em>employable</em>:
     * the job sites, one per hut that had one, in place of the hut block. Everything
     * else MCA's building types want is decoration and layout, and that is the
     * player's to arrange before they hand the colony over -
     * {@code /colonyhandover readiness} lists exactly what each building still needs.
     *
     * <p>The result is a pipeline that cannot roll back. Placing a single block on
     * standable ground near a known position either works or reports that it could
     * not, and either way no other building is affected.
     *
     * <h2>Why village anchors are still placed</h2>
     *
     * A bell for {@code town_center} and headstones for {@code graveyard} are not
     * decoration - they are {@code grouped} types that match on the anchor block
     * alone (finding #7), and without one MCA never forms a village at all. Leaving
     * those to the player would mean a handover that visibly does nothing. They also
     * cannot fail for want of room, which is what made the old behaviour painful.
     */
    public static PlaceOutcome place(ServerLevel level, ColonySnapshot snap) {
        int stations = 0;
        int anchors = 0;
        int swapped = 0;
        int shortOfRoom = 0;
        int skipped = 0;

        for (final ColonySnapshot.Hut hut : snap.huts()) {
            final BuildingMapping.Conversion plan = BuildingMapping.forHut(hut.hutId());
            switch (plan.anchor()) {
                case ROOM, POI_ONLY -> {
                    final List<BuildingMapping.Need> jobs = BuildingMapping.workstations(plan);
                    if (jobs.isEmpty()) {
                        skipped++;
                        break;
                    }
                    final int wrote = placeBeside(level, hut, plan, jobs);
                    if (wrote > 0) {
                        stations += wrote;
                    } else if (BuildingMapping.workstations(plan).stream()
                            .anyMatch(n -> Placer.countNearby(level, hut.pos(),
                                    SITE_RADIUS, SITE_VERTICAL, n.block()) < n.count())) {
                        // Wanted to write and could not - no standable ground left.
                        shortOfRoom++;
                    }
                }
                case GROUPED -> {
                    final List<BuildingMapping.Need> needs =
                            new ArrayList<>(plan.needs());
                    needs.addAll(plan.extraPoi());
                    if (placeBeside(level, hut, plan, needs) > 0) {
                        anchors++;
                    } else {
                        shortOfRoom++;
                    }
                }
                case MARKER_SWAP -> swapped += swapMarkers(level, snap, plan);
                default -> skipped++;
            }
        }
        return new PlaceOutcome(stations, anchors, swapped, shortOfRoom, skipped);
    }

    /**
     * Replaces field markers with their workstation equivalent.
     *
     * <p>This is the step that cost a real colony its fields (finding #10), and the
     * reason is worth restating because it is entirely about <em>when</em>, not
     * what. Swapping a scarecrow for a composter makes
     * {@code FarmField.isValidPlacement} false, and {@code RegisteredStructureManager}
     * drops the field on its next tick sweep - permanently, because re-registering
     * needs radii, assignment and plant settings that a block journal never saw.
     *
     * <p>Here there is no colony and no registry, so there is nothing left to
     * deregister. The swap is finally just a swap.
     *
     * <p>The positions come from the snapshot rather than a search box. A field's
     * marker can sit anywhere in colony range with a radius up to
     * {@code FarmField.MAX_RANGE} per direction, so no box around the hut is
     * correct - and the registry that knows better is destroyed by the time this
     * runs. Reading the positions before deletion is the only way to have both.
     *
     * <p>Markers are colony-wide rather than per-hut, so two farm huts would both
     * see the same list. Checking that the block really is still the marker makes
     * the second pass a no-op instead of a double swap.
     */
    private static int swapMarkers(ServerLevel level, ColonySnapshot snap,
                                   BuildingMapping.Conversion plan) {
        final Block marker = plan.marker() == null ? null : plan.marker().get();
        final Block replacement = plan.needs().isEmpty() ? null : plan.needs().get(0).block();
        if (marker == null || replacement == null) {
            return 0;
        }

        final PlacementJournal.Transaction tx =
                PlacementJournal.begin("marker swap -> " + plan.mcaType());
        int swapped = 0;
        for (final BlockPos pos : snap.markers()) {
            // A scarecrow is two blocks tall; take the lower half only, so the
            // replacement lands on the ground rather than floating.
            if (!level.getBlockState(pos).is(marker)
                    || level.getBlockState(pos.below()).is(marker)) {
                continue;
            }
            if (Placer.place(level, tx, pos, replacement)) {
                swapped++;
            }
        }
        if (swapped > 0) {
            PlacementJournal.get(level).commit(tx);
        }
        return swapped;
    }

    /**
     * Drops a hut's blocks on standable ground near where it stood.
     *
     * <p>Idempotent by counting first: running placement twice should not leave two
     * bells or a row of brewing stands, which is what happened before the count was
     * added.
     */
    private static int placeBeside(ServerLevel level, ColonySnapshot.Hut hut,
                                   BuildingMapping.Conversion plan,
                                   List<BuildingMapping.Need> needs) {
        if (needs.isEmpty()) {
            return 0;
        }

        final PlacementJournal.Transaction tx = PlacementJournal.begin(
                hut.hutId() + " -> " + plan.mcaType() + " @ " + hut.pos().toShortString());
        final List<BlockPos> sites = new ArrayList<>(
                Placer.nearbySites(level, hut.pos(), SITE_RADIUS, SITE_VERTICAL));
        int wrote = 0;

        for (final BuildingMapping.Need need : needs) {
            final Block block = need.block();
            if (block == null) {
                continue;
            }
            final int present = Placer.countNearby(level, hut.pos(),
                    SITE_RADIUS, SITE_VERTICAL, block);
            for (int i = present; i < need.count() && !sites.isEmpty(); i++) {
                final BlockPos where = sites.remove(0);
                if (Placer.place(level, tx, where, block)) {
                    wrote++;
                }
            }
        }

        if (wrote > 0) {
            PlacementJournal.get(level).commit(tx);
        }
        return wrote;
    }

    // ---------------------------------------------------------------- stage 3

    /** One MCA villager per citizen, carrying across what the snapshot knows. */
    public static SpawnOutcome spawn(ServerLevel level, ColonySnapshot snap) {
        final Map<Integer, VillagerEntityMCA> spawned = new HashMap<>();
        int failed = 0;
        int professions = 0;
        for (final ColonySnapshot.Citizen c : snap.citizens()) {
            final VillagerEntityMCA villager = spawnOne(level, c);
            if (villager == null) {
                failed++;
                continue;
            }
            spawned.put(c.id(), villager);
            if (villager.getProfession() != VillagerProfession.NONE) {
                professions++;
            }
        }
        return new SpawnOutcome(spawned.size(), failed, professions, spawned);
    }

    /**
     * <p>{@code finalizeSpawn} runs before the entity joins the world so MCA's own
     * initialisation - genetics, traits, age state - happens exactly as it would
     * for a naturally spawned villager. {@code setProfession} runs after, because
     * it calls {@code refreshBrain}, which needs the entity to be in a level.
     *
     * <p>The XP seed is not cosmetic. {@code LoseUnimportantJobTask} strips any
     * vanilla profession from a villager with no claimed job site, zero XP and
     * level <= 1 (finding #9). A villager whose workstation placement failed would
     * otherwise quietly revert to unemployed a few minutes later.
     */
    private static VillagerEntityMCA spawnOne(ServerLevel level, ColonySnapshot.Citizen c) {
        final Gender gender = c.female() ? Gender.FEMALE : Gender.MALE;
        final VillagerEntityMCA villager = gender.getVillagerType().create(level);
        if (villager == null) {
            return null;
        }

        final Vec3 where = clearSpot(level, c.where());
        villager.moveTo(where.x, where.y, where.z, level.random.nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(where)),
                MobSpawnType.COMMAND, null);
        // setCustomName, NOT setName. They sound interchangeable and are not:
        // VillagerLike.setName writes only to the family tree entry, while
        // VillagerEntityMCA.setCustomName (:1295) writes the entity name AND calls
        // setName for the tree. Using setName alone left the tree correct and the
        // nametag showing whatever random name VillagerLike.initialize (:123) had
        // already assigned during finalizeSpawn, because our name was not set yet
        // for its null check to see.
        villager.setCustomName(Component.literal(c.name()));
        if (c.child()) {
            villager.setBaby(true);
        }
        if (!level.addFreshEntity(villager)) {
            return null;
        }

        if (c.hasJob()) {
            final VillagerProfession profession = JobMapping.forJob(c.jobId());
            if (profession != VillagerProfession.NONE) {
                villager.setProfession(profession);
                villager.setVillagerXp(SEED_XP);
            }
        }
        return villager;
    }

    /**
     * The nearest spot at or above a position where a villager actually fits.
     *
     * <p>Usually a no-op: the snapshot records exactly where the citizen stood, so
     * that spot was standable a moment ago. It earns its keep on the edges - a
     * citizen who was sitting, or one whose chunk was unloaded and whose position
     * had to be guessed from a hut block.
     *
     * <p>Only ever searches upward, and only two blocks. Downward would drop people
     * through floors into cellars; sideways would scatter a household across a wall.
     * A villager left slightly high simply falls, which is the harmless direction to
     * be wrong in - hence no ceiling check beyond needing head clearance.
     */
    private static Vec3 clearSpot(ServerLevel level, Vec3 wanted) {
        final BlockPos start = BlockPos.containing(wanted);
        for (int dy = 0; dy <= 2; dy++) {
            final BlockPos candidate = start.above(dy);
            if (Enclosure.isOpen(level, candidate) && Enclosure.isOpen(level, candidate.above())) {
                return dy == 0 ? wanted : Vec3.atBottomCenterOf(candidate);
            }
        }
        return wanted;
    }

    // ---------------------------------------------------------------- stage 4

    public static WireOutcome wire(ServerLevel level, ColonySnapshot snap,
                                   Map<Integer, VillagerEntityMCA> spawned) {
        final FamilyTree tree = FamilyTree.get(level);
        final int couples = wirePartners(tree, snap, spawned);
        final Wired parents = wireParents(tree, snap, spawned);
        return new WireOutcome(couples, parents.links(), parents.deceased());
    }

    /** Marriages, wired once per couple rather than once per spouse. */
    private static int wirePartners(FamilyTree tree, ColonySnapshot snap,
                                    Map<Integer, VillagerEntityMCA> spawned) {
        int couples = 0;
        for (final ColonySnapshot.Citizen c : snap.citizens()) {
            // Both halves name each other, so take only the lower id and the pair
            // is handled exactly once.
            if (c.partnerId() < 0 || c.partnerId() < c.id()) {
                continue;
            }
            final VillagerEntityMCA self = spawned.get(c.id());
            final VillagerEntityMCA partner = spawned.get(c.partnerId());
            if (self == null || partner == null) {
                continue;
            }
            tree.getOrCreate(self).updatePartner(tree.getOrCreate(partner));
            couples++;
        }
        return couples;
    }

    private record Wired(int links, int deceased) {}

    /**
     * Parents, including the ones who are long dead.
     *
     * <p>Three things are load-bearing here.
     *
     * <p><b>Clearing first.</b> MCA's {@code finalizeSpawn} invents two random
     * deceased parents for any villager that has neither
     * ({@code VillagerEntityMCA:293-300}). Left alone, those fabrications would sit
     * where the real parents belong, so any villager we know something about gets
     * {@code clearParents()} before we assign.
     *
     * <p><b>Deduplicating the dead by name.</b> {@code FamilyTreeNode.siblings()}
     * is <em>derived</em> - "all persons who share at least one common parent" -
     * not stored. So two children of the same dead parent are only siblings if that
     * parent is one node rather than two, which means reusing a node per name
     * rather than minting one per child. MineColonies' own sibling list therefore
     * needs no wiring at all; it falls out of this.
     *
     * <p><b>Letting MCA choose the slot.</b> {@code assignParent} fills the
     * gender-matching slot when both are empty and "the last slot, independent on
     * gender" when one is taken - exactly the same-gender policy we settled on.
     * Using it means that behaviour comes from upstream rather than from a
     * reimplementation that could drift.
     */
    private static Wired wireParents(FamilyTree tree, ColonySnapshot snap,
                                     Map<Integer, VillagerEntityMCA> spawned) {
        final Map<String, FamilyTreeNode> deceasedByName = new HashMap<>();
        int links = 0;

        for (final ColonySnapshot.Citizen c : snap.citizens()) {
            final VillagerEntityMCA villager = spawned.get(c.id());
            if (villager == null) {
                continue;
            }
            final List<ColonySnapshot.Parent> known = new ArrayList<>();
            for (final ColonySnapshot.Parent p : List.of(c.parentA(), c.parentB())) {
                if (p != null && p.placeable()) {
                    known.add(p);
                }
            }
            if (known.isEmpty()) {
                // Nothing recovered: leave MCA's invented ancestors alone. They keep
                // the tree shaped like a family rather than a list of orphans, which
                // is what finding #4 recommends for a lost generation.
                continue;
            }

            final FamilyTreeNode node = tree.getOrCreate(villager);
            node.clearParents();
            for (final ColonySnapshot.Parent p : known) {
                node.assignParent(nodeFor(p, tree, spawned, deceasedByName));
                links++;
            }
        }
        return new Wired(links, deceasedByName.size());
    }

    /** A living parent's own node, or a shared deceased node for their name. */
    private static FamilyTreeNode nodeFor(ColonySnapshot.Parent parent, FamilyTree tree,
                                          Map<Integer, VillagerEntityMCA> spawned,
                                          Map<String, FamilyTreeNode> deceasedByName) {
        if (parent.resolved()) {
            final VillagerEntityMCA living = spawned.get(parent.id());
            if (living != null) {
                return tree.getOrCreate(living);
            }
        }
        return deceasedByName.computeIfAbsent(parent.name(), name -> {
            final FamilyTreeNode node = tree.getOrCreate(UUID.randomUUID(), name,
                    Boolean.TRUE.equals(parent.female()) ? Gender.FEMALE : Gender.MALE);
            node.setDeceased(true);
            return node;
        });
    }

    // ---------------------------------------------------------------- stage 5

    /**
     * Hands each building to MCA.
     *
     * <p>The town hall goes first. {@code town_center} is {@code grouped}, so it
     * anchors a village on its bell alone with no room or size check (finding #7),
     * and villages merge themselves afterwards - so establishing the centre before
     * the outskirts keeps the village from briefly splitting and re-merging.
     *
     * <p>Seeds come from a search near the remembered hut position, never the
     * position itself: the hut block is gone by now, but what replaced it is not
     * guaranteed to be a walkable anchor either.
     */
    public static ScanOutcome scan(ServerLevel level, ColonySnapshot snap) {
        final VillageManager villages = VillageManager.get(level);
        final List<ColonySnapshot.Hut> ordered = new ArrayList<>(snap.huts());
        ordered.sort((a, b) -> Boolean.compare(
                !a.hutId().equals("townhall"), !b.hutId().equals("townhall")));

        int adopted = 0;
        int refused = 0;
        for (final ColonySnapshot.Hut hut : ordered) {
            final BuildingMapping.Conversion plan = BuildingMapping.forHut(hut.hutId());
            if (plan.anchor() == BuildingMapping.Anchor.SKIP) {
                continue;
            }
            final BlockPos seed = plan.anchor() == BuildingMapping.Anchor.GROUPED
                    ? hut.pos()
                    : Placer.findInteriorSeed(level, hut.pos(), SEED_RADIUS, SEED_VERTICAL)
                            .orElse(null);
            if (seed == null) {
                refused++;
                continue;
            }
            try {
                villages.processBuilding(seed);
                adopted++;
            } catch (Exception e) {
                refused++;
            }
        }
        nameVillage(villages, snap);
        return new ScanOutcome(adopted, refused);
    }

    /**
     * Gives the new MCA village the colony's name.
     *
     * <p>MCA names every village it creates from a random pool
     * ({@code Village.name = pickVillageName(...)}), so without this a colony called
     * Stonehaven becomes some stranger's village the moment it is handed over. The
     * village only exists once buildings have been scanned, which is why this runs
     * last, and it is looked up by the colony's centre - the old town hall.
     */
    private static void nameVillage(VillageManager villages, ColonySnapshot snap) {
        if (snap.colonyName().isBlank()) {
            return;
        }
        villages.findNearestVillage(snap.center(), Village.MERGE_MARGIN).ifPresent(village -> {
            village.setName(snap.colonyName());
            village.markDirty();
        });
    }

    private Handover() {}
}
