# Colony Handover — MineColonies → MCA Reborn

A NeoForge mod that converts an abandoned MineColonies colony into an independent
MCA Reborn village: citizens become MCA villagers, their family relationships are
preserved, and the colony's buildings are handed over to MCA's village system.

Player-facing trigger: **"Grant Independence"** (confirm dialog: *Sign the Charter*).

---

## Toolchain

| Thing | Version |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.x |
| Java | 21 |
| Mappings | Parchment `1.21.1` / `2024.11.17` (matches MCA) |

Both upstream mods are GPL-3.0 (verified in their `LICENSE` files — MineColonies'
CurseForge page says "Custom License", which is a listing artifact, not what ships).
**This mod must therefore be GPL-3.0 as well.**

---

## Upstream repositories

Clone these read-only for reference. Do not fork or vendor either one.

### MineColonies — `https://github.com/ldtteam/minecolonies`

**Use branch `version/1.21`.** The default branch `version/main` is still
**1.20.1 / Forge** and will send you down the wrong path silently.

```bash
git clone --depth 1 -b version/1.21 https://github.com/ldtteam/minecolonies.git
```

Maven: `https://ldtteam.jfrog.io/ldtteam/modding/`, group `com.ldtteam`.
Also needs their transitive deps from the same repo: `structurize`, `blockui`,
`domumornamentum`, `multipiston`.

### MCA Reborn — `https://github.com/Luke100000/minecraft-comes-alive`

**Do NOT use the `1.21.1` branch.** *(Corrected 2026-09-01.)* It looks right and
is not: the released 1.21.1 betas are built from `feature/1.21.1-floor-clean-squash`,
and the `1.21.1` branch is a separate, older lineage missing the entire
floor/room subsystem (`BuildingRoomScanner`, `RoomTypeResolver`, `Structure`,
`StructureFloor`, …). Its `VillageManager` has `analyzeBuilding` /
`commitBuilding` and a 4-arg `processBuilding`; the shipped jar has none of those,
offering `analyzeRoom` / `analyzeRoomAddition` / `commitRoomAddition` /
`forceRoomType` and a **1-arg** `processBuilding` instead.

**Always clone the tag matching the jar you run**, and re-check when the jar
changes:

```bash
git clone --depth 1 -b "7.7.36-beta.3+1.21.1" \
  https://github.com/Luke100000/minecraft-comes-alive.git
```

Confirm alignment before trusting anything: the tag `7.7.36-beta.3+1.21.1` and
the branch `feature/1.21.1-floor-clean-squash` both point at `80bfe7d0`. Note the
project's mainline has moved to 8.x on Minecraft 26.x, so `1.21.1` work now lives
only in tags and feature branches.

Multiloader layout (`common/`, `fabric/`, `neoforge/`) — depend on the
**neoforge** jar, which contains the `common` classes.

**MCA publishes no Maven artifact.** There is no publishing block anywhere in its
Gradle files. Use the Modrinth Maven (`https://api.modrinth.com/maven`,
coordinate `maven.modrinth:minecraft-comes-alive-reborn:<version>`), or fall back to
a local `compileOnly files('libs/mca-neoforge-....jar')`.

### NeoForge — `https://github.com/neoforged/neoforge`, docs at `https://docs.neoforged.net/`

---

## Survey findings

These were established by reading the source directly. They are the reason the
design looks the way it does.

### 1. `ColonyDeletedModEvent` fires too late — do not use it

MineColonies has a clean public event bus
(`com.minecolonies.api.eventbus`, subscribe via
`IMinecoloniesAPI.getInstance().getEventBus().subscribe(...)`) and it publishes a
`ColonyDeletedModEvent`. It looks like the obvious hook. It is not.

In `ColonyManager.deleteColony()` the order is:

1. Unclaim chunks
2. Loop every `ICitizenData` → `entityCitizen.die(...)`
3. Loop buildings → `deconstruct()` (only if `canDestroy`), `destroy()`, and
   `world.removeBlock()` on any `AbstractBlockHut`
4. **Then** post `ColonyDeletedModEvent` (line ~229)
5. Delete save data

By the time a handler runs, citizens are dead and hut blocks are gone. All
conversion work must happen **before** deletion.

### 2. Hut blocks are always removed; structures survive if `canDestroy = false`

```java
if (canDestroy) { building.deconstruct(); }
building.destroy();
if (world.getBlockState(location).getBlock() instanceof AbstractBlockHut) {
    world.removeBlock(location, false);   // unconditional
}
```

`canDestroy` only gates `deconstruct()` (which razes the actual structure).
Deleting with `canDestroy = false` leaves walls, roof, doors, beds and decoration
intact and removes only the marker block — which is exactly what we want.

### 3. Two distinct notions of "abandoned", and nothing auto-deletes

- **Explicit:** `IPermissions.setOwnerAbandoned()`, reached via
  `/mc colony setAbandoned` (`CommandSetAbandoned`) or a player abandoning their
  own colony (`ColonyAbandonOwnMessage`). `WindowTownHallColonyReactivate` exists,
  so abandoned colonies persist and can be reclaimed.
- **Passive:** hours since last player contact
  (`packageManager.getLastContactInHours()`, saved as `TAG_ABANDONED`), exposed via
  `IColonyManager.getColoniesAbandonedSince(int hours)`.

**`getColoniesAbandonedSince` is declared and implemented but called nowhere in the
codebase.** It is a dormant API. Nothing in MineColonies currently auto-deletes
inactive colonies on this branch, so we are not racing anything and we get to define
the trigger ourselves.

### 4. Citizen family data maps — except parents

`ICitizenData` exposes:

- `getPartner()` → `ICitizenData`
- `getChildren()` → `List<Integer>` (citizen IDs)
- `getSiblings()` → `List<Integer>` (citizen IDs)
- `getParents()` → `Tuple<String, String>` — **names only, not IDs**

Partner, children and siblings transfer cleanly via an `int → UUID` map built during
conversion. Parents are lossy. Since that generation is usually dead anyway, create
MCA nodes for them with `FamilyTreeNode.setDeceased(true)` to preserve the tree's shape.

**Parent gender is recoverable from the name.** *(Added 2026-09-11.)* Worse than
"names only": `ReproductionManager:181` calls
`setParents(firstParent.getName(), secondParent.getName())` with **no father/mother
ordering and no gender stored**, while MCA's `FamilyTreeNode` needs an explicit
`father()` and `mother()`. But the name is not decoration — `CitizenData.generateName`
draws the first name from `CitizenNameFile.maleFirstNames` or `.femalefirstNames`
according to gender, so reading it back inverts a function rather than guessing.

Measured across all 22 shipped styles: 4117 male and 3429 female first names, only
**133 in both — 1.76%**. Thirteen styles have zero overlap; the largest, `default`,
has 76 of 2108 (3.6%). Name layout is `"Last First"` for `EASTERN` and
`"First M. Last"` / `"First Last"` for `WESTERN`, so the first name is the last token
or the first token respectively (`CitizenData.generateName:658-675`).

**MineColonies pairs citizens without checking gender.** `ReproductionManager:128`
filters candidates by partnered / same-named / related / child and applies *no*
gender filter, and partners themselves are formed by that same gender-blind
selection at `:159-160`. Same-gender couples and same-gender parent pairs are
therefore ordinary — roughly half of unpartnered pairings by chance — and
complementary inference ("she is female, so he must be male") is **unsound**.

MCA's `FamilyTreeNode` has exactly two slots, `UUID father` and `UUID mother`
(`Util.NIL_UUID` = absent), with no generic parent list, so two mothers cannot both
be recorded as mothers. `handover/ParentSlots.java` resolves this with two rules
that deliberately pull opposite ways:

- **Never invent a gender** — a parent whose gender we could not recover is left
  out; their child keeps the other parent and an empty slot.
- **Never drop a real parent** — for a same-gender pair the second takes the vacant
  slot. `FamilyTreeNode.gender` is a separate field, so she stays recorded as
  female; only the slot label is wrong (MCA's family screen says "Father"), and
  `getRelatives` walks structure rather than labels.

So parent resolution is two passes, in `handover/ParentGender.java`: match a living
citizen (gives gender *and* an id to link), else read the gender off the name file
(gives gender only — enough for a deceased node). Names in both lists or in neither
stay unknown; guessing a parent's gender is worse than admitting ignorance.

### 5. MCA's family tree is writable world SavedData

`net.conczin.mca.server.world.data.FamilyTree` — `FamilyTree.get(ServerLevel)`,
`getOrCreate(UUID, String name, Gender)`, `getOrCreate(Entity)`.
`FamilyTreeNode` exposes `father()`, `mother()`, `partner()`, `children()`,
`siblings()`, `updatePartner(...)`, `setDeceased(...)`, `setProfession(...)`,
`getRelatives(parentDepth, childrenDepth)`.

**Spawn all villagers first, then wire relationships in a second pass**, since
relationships are keyed by UUID and every UUID must exist before linking.

### 6. MCA detects buildings by geometry, not registration

`Building.validateBuilding(Level, Set<BlockPos> blocked)` is a flood fill from a
source block:

- Expands through air, but air only counts as interior if it has a roof — scans up
  to 16 blocks upward for a non-air, non-leaves block
- Solid blocks bound the fill; fluids count as passable interior
- A `DoorBlock` sets `hasDoor = true`; in non-strict mode the fill continues through
  into adjoining rooms
- Failure modes on the shipped jar: `TOO_SMALL`, `OVERLAP`, `BLOCK_LIMIT`,
  `SIZE_LIMIT`. **`NO_DOOR` is dead code** *(corrected 2026-09-03)* — it is
  declared in `Building.validationResult` and returned nowhere;
  `BuildingRoomScanner.Status` does not even contain it. Doors are not required
  at all, and `StructureConnector` counts `DoorBlock`, `FenceGateBlock` **and**
  `TrapDoorBlock` as connectors anyway.

**`TOO_SMALL` is a catch-all, not a size verdict.** `StructureScanner` returns it
from two places: `seed == null` (no valid scan seed) and
`regions.isEmpty() || scannedEnvelopeSize <= minSize`. The first is by far the
more common in practice.

**Seed the scan from a walkable anchor, never from a hut block.**
*(Learned the hard way, 2026-09-03 — it made every building in a real colony
report `TOO_SMALL`.)* `resolveExactSeed` requires
`isWalkableAnchor`: open at foot and head height, solid ground beneath, roof
overhead, and not reaching the exterior. A MineColonies hut block is a solid
marker, so it fails `isOpen` immediately and the scan dies before measuring
anything. MCA's own comment says "manual scans start at the player's feet".
Use the player position, or search nearby for a standable roofed cell.

Nothing checks which mod placed the blocks. **MineColonies buildings are genuine
schematic-built structures and should pass validation unchanged.**

Type assignment is data-driven JSON in `data/mca/building_types/*.json`. Example —
the whole of `bakery.json` is a colour, a priority, and
`{"minecraft:cake": 1, "minecraft:smoker": 1}`. Blocks resolve by `ResourceLocation`
against the block registry; tags are supported too. `isBuildingBlock()` returns true
for any block named by *any* registered type, so shipping our own building-type JSON
would make arbitrary modded blocks countable.

### 7. Villages create and merge themselves

`VillageManager.commitBuilding(...)` does `new Village(lastVillageId++, world)` when
no village is nearby, and automatically merges villages whose bounding boxes overlap
within `Village.MERGE_MARGIN`, keeping the more populous one. Converting
building-by-building coalesces into a single village on its own.

**`forcedType` is a disambiguator, not an override.** *(Corrected 2026-08-31 —
the earlier claim here, that we can simply force the type we want, is wrong.)*
`commitBuilding` returns `INVALID_TYPE` when `forcedType` is null and the scan is
ambiguous, but it *also* rejects a non-null `forcedType` that the building does
not already qualify for:

```java
if (forcedType != null && !scan.matchesType(forcedType))
    return validationResult.INVALID_TYPE;
```

`scan.matchingTypes` is `Building.getVisibleMatchingTypes()`, which keeps only
types with `visible: true` (plus `house`). So `forcedType` can only choose among
types the building's blocks already satisfy *and* that are visible — 10 of the 26
shipped types. It cannot invent a type, and it can never name `bakery`, `mason`,
`butcher` or the other 13 `visible: false` workstation types.

**The only real lever is block placement.** This makes pipeline step 3 (swap huts
→ vanilla workstations) load-bearing rather than cosmetic: the blocks decide the
type, and `forcedType` only breaks ties afterwards.

Type selection is `determineType()` — all matching types sorted by `priority`
descending, then `name` **ascending**, take the first. Two traps follow:

- `building.json` declares no required blocks, so it matches everything at
  priority 0. Any hut that passes the flood fill becomes at least a generic
  building — there is no "unclassified" failure mode.
- At priority 1 the alphabetical tiebreak bites. `house` (one bed) sorts before
  `leatherworker`, `mason`, `toolsmith`, `weaponsmith` and `weaving_mill`, so a
  stray bed in one of those huts silently produces a `house` instead.

**`town_center` and `graveyard` are `grouped: true`** and bypass the flood fill
entirely — `processBuilding` matches them on the anchor block alone, with no room,
door or size check. `#mca:town_center` is just `minecraft:bell`, so placing a bell
where the Town Hall stood reliably anchors the village. Convert it first.

**Call sequence — corrected 2026-09-01.** The 4-arg
`processBuilding(pos, enforce, strictScan, forcedType)` described earlier exists
only on the stale `1.21.1` branch, not in the shipped jar. Against
`7.7.36-beta.3` the surface is:

- `analyzeRoomAddition(pos)` → `BuildingScanResult` — read-only; tries an
  existing room, else scans a new structure. Use this to inspect before acting.
- `commitRoomAddition(scan, forcedType)` — commits, with the same forcedType
  rule quoted above (line 514).
- `forceRoomType(pos, type)` → `BuildingEditResult` — not yet studied.
- `processBuilding(pos)` — **1 argument only**.

The type-selection findings above were re-verified against the correct tag and
still hold: `matchesType`, `getVisibleMatchingTypes` and the
priority-then-alphabetical `determineType` are unchanged.

Not yet surveyed on the correct source: the floor/room/structure layer
(`Structure`, `StructureFloor`, `RoomIdentityPolicy`, `MainRoomSelector`).
Buildings are now grouped into multi-room structures, which likely changes how
adjacent MineColonies huts interact. **Survey this before writing the placement
engine.**

Force an immediate scan with
`VillageManager.get(level).processBuilding(pos, /*enforce*/ true, /*strictScan*/ false)`
rather than waiting for a villager to wander past and call `reportBuilding`.

### 8. A town hall button is possible via mixin

BlockUI panes can be built programmatically — MineColonies does this itself in
`TabsWindowModule`, `WindowHireWorker` and `WindowInteraction`
(`new ButtonImage()` + `addChild()`), and
`AbstractWindowSkeleton.registerButton(String, Runnable)` is `public final`.
So a mixin at the tail of the town hall window constructor can add the pane and
register the handler in ~30 lines.

A resource-pack override of their XML **does not work alone** — handlers are wired in
Java during construction, so a new button ID with no handler does nothing.

Target the **Actions tab** (`layoutactions.xml`). Note that
`windowdeleteabandoncolony.xml` is *not* a general option — that window only opens
when founding a new colony while already owning one, and its abandon button is gated
behind the `allowInfiniteColonies` config.

### 9. Every workstation block is also a vanilla POI, and assigned professions decay

*(Added 2026-09-01.)* The blocks we place to satisfy MCA's building scan do
double duty, and the second job is the one that actually sticks.

Every block in `BuildingMapping` is a vanilla job-site POI
(`net.minecraft.world.entity.ai.village.poi.PoiTypes`):

| Block | POI / profession | | Block | POI / profession |
|---|---|---|---|---|
| `blast_furnace` | armorer | | `cauldron` (all 4 variants) | leatherworker |
| `smoker` | butcher | | `lectern` | librarian |
| `cartography_table` | cartographer | | `stonecutter` | mason |
| `brewing_stand` | cleric | | `loom` | shepherd |
| `composter` | farmer | | `smithing_table` | toolsmith |
| `barrel` | fisherman | | `grindstone` | weaponsmith |
| `fletching_table` | fletcher | | | |

MCA villagers use the **vanilla** POI acquisition brain — `VillagerTasksMCA`
wires `LazyFindPointOfInterestTask`, `ValidateNearbyPoi` and `YieldJobSite`
against `JOB_SITE` / `POTENTIAL_JOB_SITE`. So any unemployed villager can claim
any workstation we place, regardless of which citizen used to work there.

**And `LoseUnimportantJobTask` strips professions we assign.** It fires when the
villager has *no* `JOB_SITE` memory, `getVillagerXp() == 0`, `level <= 1`, and
the profession is not in `ProfessionsMCA.IS_IMPORTANT`:

```java
entity.setVillagerData(entity.getVillagerData().setProfession(VillagerProfession.NONE));
```

`IS_IMPORTANT` is exactly the six MCA professions (`register(name, canTradeWith,
important, …)` — all six pass `important = true`). **No vanilla profession is in
it.** So a converted mason with no stonecutter to claim reverts to unemployed,
while our guards and archers are immune.

Two consequences for the design:

- Setting a profession is not enough. Either place a matching POI **or** give the
  villager ≥1 villager XP, which alone disqualifies `LoseUnimportantJobTask`.
  Seeding XP is the cheap, general fix and should be part of step 5.
- A building can carry POIs it did not ask for. The infirmary is the sharp case:
  MCA's `infirmary` type *requires* `#minecraft:cauldrons`, and a cauldron is the
  leatherworker POI. There is no way out via block choice — all four cauldron
  variants are both in the tag and in the POI set. Dropping the cauldron loses
  the `infirmary` type entirely (the hut falls back to `house` on its bed).

### 10. A block journal cannot undo MineColonies' data model

*(Added 2026-09-03, from the first in-game test.)* Replacing a scarecrow with a
composter works, and undo restores the scarecrow block exactly — but the field is
gone from the colony for good.

`RegisteredStructureManager`'s per-tick sanity sweep drops any extension whose
placement no longer validates:

```java
buildingExtensions.entrySet().removeIf(extension ->
    WorldUtil.isBlockLoaded(colony.getWorld(), extension.getValue().getPosition())
    && (!colony.isCoordInColony(...) || !extension.getValue().isValidPlacement(colony)))
```

and `FarmField.isValidPlacement` is only
`blockState.is(ModBlocks.blockScarecrow)`. So the swap deregisters the field
within a tick, and restoring the block does not bring it back —
`addBuildingExtensionIfMissing` is driven by a player placing a scarecrow, not by
one appearing.

**The lesson generalises beyond fields:** any write that changes what MineColonies
considers valid has consequences outside the block layer, and a
position/state/blockentity journal cannot see them.

**The fix is ordering, not more journalling.** Marker swaps — and every other
POI placement on a colony-owned position — belong *after* `deleteColony`, when
there is no registry left to damage. Doing them earlier is simply out of order.
`/colonyhandover place` now refuses on a live field and explains why;
`place force` overrides for throwaway worlds.

`IRegisteredStructureManager` does expose `addBuildingExtension` and
`removeBuildingExtension`, so restoring a field is *possible* — but it would also
need its radii, assignment and plant settings, which is a lot of machinery to
work around doing the operation at the wrong time.

### 11. Two placement rules that are not obvious from the API

*(Added 2026-09-06, from the second in-game test.)*

**A bed only counts as its HEAD half.** `Building.addBlock` guards every tally
with

```java
if (relevant && (!(block instanceof BedBlock) || state.getValue(BedBlock.PART) == BedPart.HEAD))
```

and `Block.defaultBlockState()` for a bed is `BedPart.FOOT`. So placing the
default state produces a bed MCA never counts *and* a half-bed that pops off on
the next block update. Beds must be written as a FOOT/HEAD pair with matching
`FACING` — `Placer.placeBed`. This is why `house`, `residence`, `inn` and the
Tavern all silently failed to match.

**Never place on the cell the scan was seeded from.** Seeding is
`resolveExactSeed` on a walkable anchor (finding #6); fill that cell with a
workstation and the *next* scan has no seed and fails `TOO_SMALL`. The symptom
reads as "placement broke the building" when in fact it only broke the vantage
point — the building is untouched and a second attempt from a different standing
position succeeds. `PlaceCommand` now removes the seed from the candidate list.

The general shape of both: MCA's scan reads block *state*, not block identity,
and reads it from a position we are also allowed to write to. Any placement pass
has to respect both.

### 12. MineColonies gates are `DoorBlock`s, and two systems disagree about it

*(Added 2026-09-09.)* `AbstractBlockGate extends DoorBlock`
(`api/blocks/decorative/AbstractBlockGate.java:40`). Everything below follows
from that one line, and it cuts both ways depending on whether the consuming
code tests the **class** or a **tag**.

**Survives handover.** `BlockGate` is not an `AbstractBlockHut`, so the
unconditional hut removal in `deleteColony` (finding #2) does not touch it. With
`canDestroy = false` the gate stays exactly where it is.

**Right-click keeps working, and gets freer.** `useItemOn` toggles with no colony
lookup and no permission check (`:92`). The *live* colony is the restrictive
case: `ColonyPermissionEventHandler:384` early-returns only for
`#minecraft:doors` and `#minecraft:fence_gates`, and `gate_wood` is in neither,
so a non-member currently falls through to the generic `RIGHTCLICK_BLOCK`
cancellation. After deletion the handler never fires.

**Counts as a connector for MCA.** `StructureConnector:24` tests
`instanceof DoorBlock`, so a gated archway seals a room for the escape walk and
will not be reported as drafty.

**But villagers could not open it — class vs tag.**

| System | Test | Gate result |
|---|---|---|
| `WalkNodeEvaluator:518` (pathing) | `instanceof DoorBlock`, then `type().canOpenByHand()` | `DOOR_WOOD_CLOSED` — **routes through it** |
| `InteractWithDoor:59`/`:110` (opening) | `is(BlockTags.MOB_INTERACTABLE_DOORS, …)` | **not in the tag** |
| `SmarterOpenDoorsTask:61` (MCA's alternative) | same tag | same |

`BlockSetType("spruce")` sets `canOpenByHand = true` (`BlockSetType.java:199`),
and `minecolonies:gate_wood` ships in only three tags — `minecolonies:decoblocks`,
`minecraft:mineable/axe`, `minecolonies:tier3blocks`. So villagers pathed *to* a
gate expecting to open it and then stalled: worse than avoiding it.

**Fix:** `data/minecraft/tags/block/mob_interactable_doors.json` in this mod adds
`minecolonies:gate_wood` with `required: false`. Wooden only — iron gates stay
villager-proof by design. Safe because:

- `MOB_INTERACTABLE_DOORS` is read by exactly one vanilla class,
  `InteractWithDoor` (verified across all 5,364 decompiled sources), so there is
  no raid or door-breaking side effect.
- `AbstractBlockGate` overrides `setOpen` to delegate to `toggleGate` (`:401`),
  so a villager moves the **whole** multi-block gate, not one column.
- It changes nothing about zombies: `DoorBlock.isWoodenDoor` (`:297`) is
  `instanceof DoorBlock && canOpenByHand()` with no tag, so `BreakDoorGoal`
  already treated these gates as breakable wooden doors before this change.

One caveat if `Config.useSmarterDoorAI` is turned on (default `false`):
`SmarterOpenDoorsTask.setOpen` writes the `OPEN` property directly via
`world.setBlock` rather than calling `DoorBlock.setOpen`, bypassing the
`toggleGate` override — so a villager would open a single column of the gate
rather than all of it. Functional, but it looks wrong.

---

---

## Planned architecture

Build as a **third mod depending on both**. Never fork, never vendor.

Conversion pipeline, in order:

1. **Trigger.** Start with a `Charter of Independence` item (right-click the Town
   Hall block) and/or a command — zero coupling to MineColonies internals, easy to
   iterate. Add the town hall button via mixin once the mechanic is solid.
   Permission-gate on `colony.getPermissions().getRank(player).isColonyManager()`,
   the way `CommandSetAbandoned` does. Irreversible, so require confirmation.
2. **Snapshot everything** into plain data - `handover/ColonySnapshot.java`. Both
   citizens (identity, job, gender, child flag, partner/children/sibling ids, parent
   names, position) **and** buildings (position, hut id, level). There is no "after"
   in which to re-read any of it, so the snapshot has to be complete.
3. **Delete the colony with `canDestroy = false`** via
   `IColonyManager.deleteColonyByWorld(id, false, level)`. Structures survive; only
   the hut marker blocks and the citizens go (finding #2).
4. **Place workstations** against the remembered coordinates. *(Reordered
   2026-09-11.)* This step used to sit before deletion. Finding #10 - a colony that
   permanently lost its fields - established that any write changing what
   MineColonies considers valid must happen **after** deletion, when there is no
   registry left to damage. The snapshot is what makes the later placement possible.
5. **Spawn MCA villagers**, building an `int citizenId -> UUID` map as we go, and
   seed >=1 villager XP so `LoseUnimportantJobTask` cannot strip the profession
   (finding #9).
6. **Wire the family tree** in a second pass using that map.
7. **Force building scans**. Process the town hall first - `town_center` is
   `grouped` and anchors the village on a bell alone (finding #7).

### Mapping tables to build

- **MineColonies building type → vanilla workstation block** (Baker's Hut → smoker,
  Library → lectern, Smeltery → blast furnace, …)
- **MineColonies job → `VillagerProfession`**, with a sensible fallback for jobs that
  have no vanilla equivalent (Builder, Courier, Deliveryman)
- Optionally, custom `building_types` JSON for MineColonies buildings with no vanilla
  analogue (Warehouse, Guard Tower, Builder's Hut) keyed on chest/barrel counts

### Expected failure modes (acceptable)

- Open-air buildings (fields, farms, plantations, lumberjack camps, mine entrances)
  fail `NO_DOOR` / `TOO_SMALL` and simply don't become MCA buildings. Farmers still
  use farmland via vanilla AI.
- Level-1 huts may fail `TOO_SMALL`; `minBuildingSize` is config-tunable.
- `OVERLAP` in dense colonies where adjacent huts share walls — first commit wins.

Beds need no handling: MineColonies residences already contain them and MCA counts
bed `HEAD` blocks, so residency falls out for free.

---

## Open questions

- ~~Exact MineColonies building-type → workstation mapping~~ — **done**, all 55 huts
  in `mapping/BuildingMapping.java`, all 51 jobs in `mapping/JobMapping.java`.
- Whether to call MineColonies' `deleteColony` at the end or unregister the colony
  directly
- ~~Whether MCA's `VillagerProfession` assignment respects a forced building type~~ —
  **answered: the two are unrelated.** Profession lives in vanilla `VillagerData`
  (`VillagerEntityMCA.setProfession` just writes it) and is independent of building
  type. So we set professions directly on spawn, and a villager left as
  `VillagerProfession.NONE` will claim a workstation POI through vanilla AI.
- Child citizens: MineColonies `setIsChild` vs MCA's age/growth system

---

## Working agreement

- Explain what the code is doing, not just what changed — the human has Java and OOP
  background but is new to Minecraft modding, and wants to review the direction.
- Verify against upstream source before asserting how either mod behaves. Both are
  actively developed; MineColonies ships snapshots almost daily.
- Prefer APIs over mixins. Where a mixin is unavoidable, keep it small and isolate it
  so upstream churn breaks one file, not the design.
