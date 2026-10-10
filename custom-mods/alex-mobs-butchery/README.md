# Butchery: Alex's Mobs — NeoForge 1.21.1 port

A private port of [Butchery: Alex's Mobs Addon](https://modrinth.com/mod/butchery-alexs-mobs-addon)
(by Jmods, originally Forge 1.20.1) to **NeoForge 1.21.1**.

> The original is All Rights Reserved and ships no source. This port is for personal use only and
> must not be redistributed.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | **NeoForge** 21.1.176+ (Butchery has no Forge build for 1.21.1) |
| [Butchery](https://modrinth.com/mod/butchery) | 4.0+ (tested against 5.2) |
| [Alex's Mobs Continued](https://modrinth.com/mod/alexs-mobs-continued) | tested against 2.1.9+1.21.1 |
| CodxLib | required by Alex's Mobs Continued |

Neither Butchery nor Alex's Mobs is a compile-time dependency — every cross-mod reference is a
`ResourceLocation`, tag or loot table resolved at runtime.

## Supported mobs

Bald Eagle · Blobfish · Caiman · Capuchin · Crow · Emu · Flying Fish

## How it works

1. Killing a supported mob (with a cleaver, if *Only Cleavers Drop Carcasses* is on) drops a
   **carcass block** instead of its normal loot.
2. Right-clicking a Butchery **hook** or **rope** with the carcass hangs it there.
3. Right-clicking the hanging carcass makes it **bleed**; blood drains into a Butchery
   **blood grate** up to 4 blocks below, via the fluid capability. After 900 ticks it becomes a
   **drained carcass**.
4. A **cleaver** severs the head and works through the cuts; a **skinning knife** plucks or skins.
   Drops come from loot tables, rolled once more per level of Looting.

Ground carcasses use `blockstate` stages `0 → 6 → 7 → 8 → 9`; hanging ones use `1 → 2 → 3 → 4 → 5`.

## Bugs fixed from the original

These were all broken in the 1.20.1 release and are corrected here:

- **Capuchin never butcherable.** The entity tag listed `alexsmobs:capuchin`, but the Alex's Mobs
  entity id is `alexsmobs:capuchin_monkey`, so the tag silently matched nothing.
- **Caiman cut drops failed to load.** `caiman_cut_1/2/3_drop` referenced an item
  `butchery_alexs_mobs:rawcaimansteak`, which was never registered (the item is `raw_caiman_steak`).
- **Blobfish cut drop failed to load.** It contained a leftover `deleted_mod_element` entry.
- **Mobs did not actually die.** The original cancelled `LivingDeathEvent` to suppress vanilla loot,
  which also cancels the death itself. This port replaces the drops in `LivingDropsEvent` instead.
- **Drained carcasses could not be butchered after being moved.** Being "drained" was tracked by a
  block-entity flag that was lost when the block was picked up; it is now implied by the block.
- **Two unparsable models.** `blobfish_carcass_bounding.json` and `hanging_blobfish_carcass_bounding.json`
  were raw Blockbench exports using `"rotation": {"x":..,"y":..,"z":..}` rather than Minecraft's
  `{"angle":.., "axis":..}`, and their `y: -90` is not even a legal element angle. Both were orphaned
  (no blockstate referenced them) and only produced load errors, so they are removed.

## Deliberate differences

- One shared `CarcassBlockEntity` replaces the original's eleven identical ones, and its unused
  9-slot container (MCreator boilerplate — no menu was ever registered) is gone.
- The bleed pays blood into the grate in ten instalments rather than one lump.
- `emu_cut_2_drop.json` ships but is unreferenced, exactly as in the original.

## Adding another mob

1. Add its blocks to `ModBlocks` plus a class in `block/` carrying the shapes.
2. Add a `MobDef` in `MobDefs` and its stages in `CutTables`.
3. Add models, blockstates, textures, loot tables, and an `alexsmobs` entity-type tag.

## Testing

`gradlew runGameTestServer` runs ten GameTests headlessly against the real Butchery and
Alex's Mobs jars, covering the death drop, bleeding, each cut-up step, the tool gating, and the
full hanging sequence.

The suite was validated with a negative control: disabling `CutUp.tryCut` and restoring the
original capuchin tag made exactly the five dependent tests fail, and only those five.

## Building

```bash
gradle build
```

The jar lands in `build/libs/`. `gradle runServer` / `runClient` launch a test instance; put
Butchery, Alex's Mobs Continued and CodxLib in `run/mods/`.
