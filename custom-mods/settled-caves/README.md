# Settled Caves

NeoForge 1.21.1. Makes cave fluids and falling blocks generate already settled, instead of
generating unstable and spending the first minutes of every chunk pouring, grinding and
collapsing.

It keeps YUNG's Better Caves' underground lakes, lava and waterfalls. It only removes the part
where they don't stop.

## Why this exists

Better Caves is aquifer-aware - its `carveBlock` calls `Aquifer.computeSubstance` and
`markPosForPostprocessing` exactly like vanilla. So cave fluids are not "wrong"; they are
scheduled to flow and settle after generation, which is vanilla's design. The trouble is volume:
Better Caves carves far more than vanilla, and its liquid regions split 40% water / 60% lava,
which is close to the maximum possible water-lava interface. The result is:

- **water/lava boundaries** that never settle - each cobblestone or obsidian conversion opens a
  new gap for more fluid to move into
- **wide breaches** where a carver opened a lake into a void, so a whole face pours out
- **gravel/sand ceilings** over open caves that collapse as one enormous chain

This mod fixes each at generation time, once, with no runtime cost.

## How it runs

A single worldgen feature in the `top_layer_modification` step - the last decoration step - so
carvers, aquifers and ore/gravel blobs already exist. It runs once per chunk via
`data/settledcaves/neoforge/biome_modifier/add_settle.json` on `#minecraft:is_overworld`.

It never reads a whole chunk blindly. Each rule first asks each 16x16x16 section's palette
(`LevelChunkSection.maybeHas`) whether it contains any relevant block at all, and skips the
section without reading a single block if not. Most sections contain no lava, so the lava pass is
nearly free.

No mixins.

## The three rules

### 1. Fluid seam

Any lava block touching water is replaced. **The lava side is converted**, so water lakes keep
their volume.

| Contact | Result |
|---|---|
| lava **source** + water **source** | **obsidian** |
| anything else (either side flowing) | **stone** |

This keeps obsidian to genuine source-on-source seams, so it doesn't turn every underground
water/lava boundary into a free obsidian mine.

### 2. Aperture seal

An **opening** is a water block with air beside it or directly below it - somewhere it can pour
out. (Air above is ignored; water never flows up, and every lake surface has air above it.)

Connected openings are grouped. If a group is **larger than `maxOpening`** (default 5), stone is
placed in the air next to it and the breach is walled shut. If it is **at or under** the limit,
it is left alone.

This is the distinction you asked for: a pocket of water in a cave wall exposes one or two
blocks and still trickles down; a breached reservoir exposes a wide face and gets sealed. It
measures the *opening*, not the whole lake, because the opening is what pours and because a lake
spans more chunks than generation can see at once.

A narrow waterfall passes too - its source aperture stays small even when the pool behind it is
huge.

Where a lake's surface meets dry cave floor **at the same level** along a long shoreline, this
will add a one-block stone kerb, because water genuinely would flow out there. Better Caves'
liquid-region lakes rarely have this (their shore is uncarved rock), so expect it mostly where
vanilla aquifers or a second carver pass open a void level with the water.

### 3. Gravel footprint cap

Any block extending `FallingBlock` - gravel, sand, red sand, concrete powder, and modded gravels
like Eternal Tales' `mucunfectio_gravel` - with air directly below it is **exposed**.

Falling is per-column, so footprints are 2D: exposed blocks that touch horizontally on the same
layer form one footprint. If a footprint is **larger than `maxFootprint`** (default 9), blocks on a
world-aligned grid are turned into stone, splitting it into cells. Each cell can still cave in.

The exposed block itself is replaced (not the air under it), so the ceiling stays flush instead
of growing stone nubs, and the column above that block is supported.

**The grid quantises the cap.** Support lines sit every `k` blocks where
`k = floor(sqrt(maxFootprint)) + 1`, leaving cells of `(k-1) x (k-1)`:

| `maxFootprint` | real largest cave-in |
|---|---|
| 1-3 | 1 |
| 4-8 | 4 (2x2) |
| 9-15 | **9 (3x3)** |
| 16-24 | 16 (4x4) |

Because the grid is aligned to world coordinates, neighbouring chunks draw matching lines.

Pointed dripstone is deliberately **not** affected. It implements `Fallable`, but not
`FallingBlock` - stalactites hanging over air are supposed to.

## Config

`config/settledcaves-common.toml`

| Section | Key | Default | Meaning |
|---|---|---|---|
| `fluidSeam` | `enabled` | `true` | |
| | `maxY` | `64` | only process lava at or below this |
| `apertureSeal` | `enabled` | `true` | |
| | `maxOpening` | `5` | largest opening left unsealed |
| | `maxY` | `50` | kept under sea level so surface rivers, cliff waterfalls and shorelines are never touched |
| `gravelCap` | `enabled` | `true` | |
| | `maxFootprint` | `9` | target largest cave-in (see quantisation table) |
| | `maxY` | `320` | |
| (root) | `useDeepslateBelowZero` | `true` | deepslate instead of stone below Y 0, so fixes blend into the deepslate layer; obsidian is unaffected |

## Chunk borders

A feature is allowed to read the surrounding 3x3 chunks but should only write to its own. This mod
**reads across the border** (so a group's true size is measured correctly) and **writes only
inside the centre chunk** (so it never triggers "setBlock in a far chunk"). The neighbouring
chunk handles its own side when it generates.

That leaves one real gap: a water/lava contact lying exactly on a border can be seen from both
sides but converted only once, and a group larger than the 3x3 reach is truncated. Worst case you
get the occasional unsealed seam block or one slightly oversized gravel patch. Vanilla's runtime
lava/water conversion cleans a missed seam up as a one-off, which is acceptable.

A single flood-fill is also capped at 4,096 blocks as a safety valve. Openings are shorelines and
breach faces, not lake volumes, so this should never be reached in practice.

## Interaction with the rest of the pack

- **`eternaltales-gravel-tame.zip` is now stricter than it needs to be.** It sets
  `discard_chance_on_air_exposure: 1.0`, removing every air-exposed mucunfectio vein outright - no
  cave-ins from that block at all. With the footprint cap handling falling blocks generally,
  relax it toward `0.3`-`0.5` so Eternal Tales gravel participates in (small) cave-ins again.
- **Restore `liquidregions.json`** if you want Better Caves' lakes. It is currently emptied
  (`{"liquidRegions": {}}`); the backup is at `proposed-configs/liquidregions.json.BEFORE`.
- Keep `water_source_conversion = false`. This mod assumes it.
- `rain-source-restore` is unaffected. It only acts on exposed surface water during rain, and
  this mod's aperture rule stops at `maxY` 50.
- **New chunks only.** It is a generation feature. Existing chunks keep whatever they have.

## Build

```
./gradlew build
```

Output: `build/libs/settledcaves-1.21.1-1.0.0.jar`.

Toolchain matches `delightful-port` and `rain-source-restore`: NeoForge 21.1.219, Gradle 8.8,
Java 21, `net.neoforged.moddev` 1.0.0.

## Untested

This has not been compiled or run. Every Minecraft API it calls was checked against the 1.21.1
client jar: `LevelChunkSection.maybeHas/hasOnlyAir/getBlockState(int,int,int)`,
`ChunkAccess.getSections`, `getSectionYFromSectionIndex`, `getMinBuildHeight/getMaxBuildHeight`,
`LevelReader.getChunk(int,int)`, `LevelWriter.setBlock(pos,state,int)`,
`BlockGetter.getFluidState/getBlockState`, `FluidState.isSource/is(TagKey)`,
`Feature(Codec)`, `FeaturePlaceContext.level/origin`, `NoneFeatureConfiguration.CODEC`,
`ChunkPos(BlockPos)/getMinBlockX/Z`, `MutableBlockPos.set/immutable`, and `FallingBlock`.

What is not verified is behaviour: whether the step placement, the aperture threshold and the grid
spacing produce the caves you want. Tune `maxOpening` and `maxFootprint` after looking at a fresh
world.
