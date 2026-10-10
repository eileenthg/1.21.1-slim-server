# Rain Source Restore

NeoForge 1.21.1. Lets rain repair water bodies that have been broken up by bucketing water out.

## What it does

With the vanilla gamerule `waterSourceConversion` turned **off**, flowing water never promotes
itself back into a source block. That stops cave floods self-feeding, but every bucket taken out
of a pond leaves a permanent hole. This mod re-allows source conversion, but only where rain is
falling.

## How it works

No mixins. NeoForge already patches `FlowingFluid#getNewLiquid` to post
`net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent`, and this mod listens to it on
`NeoForge.EVENT_BUS`.

The detail that matters is **which block the event is about**. In the patched `getNewLiquid`,
the event is posted inside the loop over horizontal neighbours - once for each neighbour that is
already a source of the same fluid - with that **neighbour's** position and state. Each neighbour
the event allows increments a counter. After the loop, vanilla still requires:

```
counter >= 2
block below is solid, or is itself a source
```

This mod answers "may this neighbouring source count?" with "yes, if rain is falling on it". So a
gap heals only when it is flanked by **at least two rained-on water sources** with support below.
It closes holes in existing surface water and cannot:

- start a new pool
- convert a waterfall
- feed a cave flood

## What "exposed" means

`Level.isRainingAt(pos)` on each contributing source, which requires all of:

1. a storm is active
2. `canSeeSky(pos)`
3. the source is the top of its column (motion-blocking heightmap)
4. the biome's precipitation at that position is `RAIN`

- **Caves and covered reservoirs never heal** - (2) and (3) fail.
- **Submerged layers never heal** - only the surface layer passes (3).
- **Village wells** qualify but have nothing to heal unless drained.
- **Deserts and badlands never heal** - precipitation `NONE`.
- **Snowy biomes never heal by default** - precipitation `SNOW`; see `includeSnowyBiomes`.

## Config

`config/rainsource-common.toml`

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch. |
| `includeSnowyBiomes` | `false` | Also heal while snowing. |
| `minY` | `0` | Nothing below this Y counts, even with sky access. |

## Interaction notes

- Keep `water_source_conversion = false`. If it is `true`, the event arrives with `canConvert()`
  already true and this mod does nothing.
- Lava is excluded: the handler returns unless the neighbour's fluid is in `#minecraft:water`.
- Puddles & Floods places its own puddle blocks and never touches source conversion.

## History

`1.0.0` used a MixinExtras `@ModifyExpressionValue` on the `canConvertToSource(Level)` call in
`getNewLiquid`. That call exists in vanilla but **NeoForge replaces it** with
`EventHooks.canCreateFluidSource(...)`, so the injection found no target and the game refused to
start (`Critical injection failure ... Scanned 0 target(s)`). The target had been checked against
the unpatched vanilla jar rather than the NeoForge-patched one. `1.0.1` uses the event instead.

## Build

```
./gradlew build
```

Output: `build/libs/rainsource-1.21.1-1.0.1.jar`.
