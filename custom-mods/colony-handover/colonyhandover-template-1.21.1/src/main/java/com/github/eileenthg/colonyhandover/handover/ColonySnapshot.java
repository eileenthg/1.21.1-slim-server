package com.github.eileenthg.colonyhandover.handover;

import com.minecolonies.api.colony.CitizenNameFile;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything we need from a colony, read before it is destroyed.
 *
 * <h2>Why a snapshot at all</h2>
 *
 * {@code ColonyManager.deleteColony} kills every citizen and removes every hut
 * block <em>before</em> it posts {@code ColonyDeletedModEvent} (finding #1), so
 * there is no "after" in which to read any of this. Everything the handover
 * needs has to be lifted out first and held in plain data that outlives the
 * colony object.
 *
 * <h2>Why it is plain data and not a list of live objects</h2>
 *
 * Holding {@code ICitizenData} references across the deletion would be holding
 * references to gutted objects - the citizens are dead, their colony is
 * unregistered, and any getter might touch state that has been torn down. Copies
 * of the primitives cannot rot that way.
 *
 * <h2>The ordering this enables</h2>
 *
 * The pipeline in CLAUDE.md says to swap hut blocks for workstations
 * <em>before</em> deletion. Finding #10 - learned after that section was written,
 * from a colony that permanently lost its fields - says the opposite: any write
 * that changes what MineColonies considers valid must happen <em>after</em>
 * deletion, when there is no registry left to damage. Finding #10 wins, and a
 * snapshot is what makes it possible: we read positions and hut types now, delete,
 * and then place blocks against remembered coordinates rather than live buildings.
 */
public record ColonySnapshot(
        int colonyId,
        String colonyName,
        BlockPos center,
        List<Hut> huts,
        List<Citizen> citizens,
        List<BlockPos> markers) {

    /**
     * Where the colony registered its building extensions - the scarecrows and
     * plantation markers a MARKER_SWAP replaces.
     *
     * <p>These have to be captured here for the same reason as everything else,
     * but the pressure is sharper. A marker swap must happen <em>after</em>
     * deletion or MineColonies deregisters the field permanently (finding #10),
     * and the only correct way to find the markers is to ask the colony -
     * a field sits anywhere in colony range with a radius up to
     * {@code FarmField.MAX_RANGE} per direction, so no fixed search box around
     * the hut is right. Deletion destroys that registry. So the positions must
     * be read before the step that makes using them safe.
     */
    /** A building reduced to the three things placement needs afterwards. */
    public record Hut(BlockPos pos, String hutId, int level) {}

    /**
     * One parent, as much as MineColonies actually knows.
     *
     * <p>{@code ICitizenData.getParents()} is a {@code Tuple<String, String>} of
     * <em>names</em>, and {@code ReproductionManager} fills it as
     * {@code setParents(firstParent.getName(), secondParent.getName())} - there is
     * no father/mother ordering and no gender stored anywhere. MCA's
     * {@code FamilyTreeNode} wants an explicit {@code father()} and
     * {@code mother()}, so the only way to tell them apart is to look the name
     * back up among the living and read {@code isFemale()}.
     *
     * @param id     the citizen this name resolved to, or -1 when nobody matched
     * @param female null when unresolved - the parent is dead, renamed, or the
     *               name is ambiguous, and we simply do not know
     */
    public record Parent(String name, int id, Boolean female, ParentGender.Source source) {
        public boolean known() {
            return !name.isEmpty();
        }

        /** Whether this parent matched a citizen who is still alive. */
        public boolean resolved() {
            return id >= 0;
        }

        /** Whether we know enough to place them in MCA's tree at all. */
        public boolean placeable() {
            return known() && female != null;
        }
    }

    /** One citizen, flattened. Relationship fields stay as ids, resolved later. */
    public record Citizen(
            int id,
            String name,
            boolean female,
            boolean child,
            ResourceLocation jobId,
            int partnerId,
            List<Integer> children,
            List<Integer> siblings,
            Parent parentA,
            Parent parentB,
            Vec3 where) {

        public boolean hasJob() {
            return jobId != null;
        }
    }

    /**
     * Lifts a live colony into plain data.
     *
     * <p>Parent names are resolved here rather than later because this is the
     * only moment the roster still exists to resolve them against.
     */
    public static ColonySnapshot of(IColony colony) {
        final List<Hut> huts = new ArrayList<>();
        for (final Map.Entry<BlockPos, IBuilding> e
                : colony.getServerBuildingManager().getBuildings().entrySet()) {
            huts.add(new Hut(e.getKey().immutable(),
                    e.getValue().getBuildingType().getRegistryName().getPath(),
                    e.getValue().getBuildingLevel()));
        }

        final List<ICitizenData> living =
                new ArrayList<>(colony.getCitizenManager().getCitizens());

        // Name -> citizen, for turning parent names back into ids and genders.
        // Duplicate names collapse to "ambiguous" rather than to whoever was last,
        // because guessing here would silently invent a parent relationship.
        final Map<String, ICitizenData> byName = new HashMap<>();
        final Map<String, Boolean> duplicated = new HashMap<>();
        for (final ICitizenData c : living) {
            if (byName.putIfAbsent(c.getName(), c) != null) {
                duplicated.put(c.getName(), true);
            }
        }

        // Built once rather than per lookup: the lists run to a thousand entries
        // and a colony can hold a couple of hundred parent names.
        final CitizenNameFile nameFile = colony.getCitizenNameFile();
        final Set<String> maleNames = nameFile == null
                ? Set.of() : Set.copyOf(nameFile.maleFirstNames);
        final Set<String> femaleNames = nameFile == null
                ? Set.of() : Set.copyOf(nameFile.femalefirstNames);

        final List<BlockPos> markers = new ArrayList<>();
        for (final IBuildingExtension extension
                : colony.getServerBuildingManager().getBuildingExtensions(e -> true)) {
            markers.add(extension.getPosition().immutable());
        }

        final List<Citizen> citizens = new ArrayList<>();
        for (final ICitizenData c : living) {
            final ResourceLocation jobId = c.getJob() == null
                    ? null
                    : c.getJob().getJobRegistryEntry().getKey();

            citizens.add(new Citizen(
                    c.getId(),
                    c.getName(),
                    c.isFemale(),
                    c.isChild(),
                    jobId,
                    c.getPartner() == null ? -1 : c.getPartner().getId(),
                    List.copyOf(c.getChildren()),
                    List.copyOf(c.getSiblings()),
                    resolveParent(c.getParents().getA(), byName, duplicated,
                            nameFile, maleNames, femaleNames),
                    resolveParent(c.getParents().getB(), byName, duplicated,
                            nameFile, maleNames, femaleNames),
                    positionOf(c, colony)));
        }

        return new ColonySnapshot(colony.getID(), colony.getName(),
                colony.getCenter().immutable(),
                Collections.unmodifiableList(huts),
                Collections.unmodifiableList(citizens),
                Collections.unmodifiableList(markers));
    }

    /**
     * Two passes, best evidence first.
     *
     * <p>A living citizen with that exact name gives the gender directly and an id
     * we can link the spawned villager to. Failing that, the name itself is
     * evidence: MineColonies generated it from a gendered list, so reading it back
     * recovers the gender even for a parent who died generations ago
     * ({@link ParentGender}). Only when both fail is the parent genuinely lost.
     */
    private static Parent resolveParent(String name, Map<String, ICitizenData> byName,
                                        Map<String, Boolean> duplicated,
                                        CitizenNameFile nameFile,
                                        Set<String> maleNames, Set<String> femaleNames) {
        if (name == null || name.isEmpty()) {
            return new Parent("", -1, null, ParentGender.Source.UNKNOWN);
        }

        // Duplicates are deliberately not resolved to a citizen - we cannot tell
        // which one is meant - but the shared name still carries its gender.
        if (!duplicated.containsKey(name)) {
            final ICitizenData match = byName.get(name);
            if (match != null) {
                return new Parent(name, match.getId(), match.isFemale(),
                        ParentGender.Source.LIVING);
            }
        }

        if (nameFile != null) {
            final Boolean guess =
                    ParentGender.fromNameFile(name, nameFile, maleNames, femaleNames);
            if (guess != null) {
                return new Parent(name, -1, guess, ParentGender.Source.NAME_FILE);
            }
        }
        return new Parent(name, -1, null, ParentGender.Source.UNKNOWN);
    }

    /**
     * Where to put the villager that replaces this citizen.
     *
     * <p>Prefers the live entity's own position, so people stay roughly where
     * they were standing. A citizen in an unloaded chunk has no entity, so fall
     * back to their home, then their workplace, then the town centre - anywhere
     * inside the colony beats dropping them at world spawn.
     */
    private static Vec3 positionOf(ICitizenData citizen, IColony colony) {
        final var entity = citizen.getEntity();
        if (entity.isPresent()) {
            // The exact position, not blockPosition(). A citizen sitting on a slab
            // stands at y+0.5, and flooring that to a BlockPos names the slab
            // itself - so respawning there put people inside the block they had
            // been sitting on. Same family of problem as the carpet and dirt-path
            // seeds: anything shorter than a full cube puts feet below a block
            // boundary.
            return entity.get().position();
        }
        // No entity means an unloaded chunk, and every fallback here is a HUT
        // BLOCK - solid, and about to be removed by the deletion. Aim a block
        // above it so nobody materialises inside masonry; the spawn step checks
        // for clearance anyway, and anyone left hanging simply falls.
        if (citizen.getHomeBuilding() != null) {
            return Vec3.atBottomCenterOf(citizen.getHomeBuilding().getPosition().above());
        }
        if (citizen.getWorkBuilding() != null) {
            return Vec3.atBottomCenterOf(citizen.getWorkBuilding().getPosition().above());
        }
        return Vec3.atBottomCenterOf(colony.getCenter().above());
    }
}
