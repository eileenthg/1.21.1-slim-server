# 24 — Farmer's Contracts datapack crash (2026-09-20)

## Symptom

`crash-2026-09-20_17.35.36-client.txt` — clicking through the title screen into a
world dies during datapack load:

```
java.lang.ClassCastException: class com.google.gson.JsonObject cannot be cast to
class com.google.gson.JsonArray
  at com.google.gson.JsonObject.getAsJsonArray(JsonObject.java:209)
  at farmerscontracts@1.0.3/…contract.RecipeCostSolver.parseCutting(RecipeCostSolver.java:285)
  at …RecipeCostSolver.parse(RecipeCostSolver.java:232)
  at …RecipeCostSolver.reload(RecipeCostSolver.java:88)
  at …ContractDataReloadListener.apply(ContractDataReloadListener.java:46)
```

Same listener runs server-side, so on a dedicated server this surfaces as
"Failed to load datapacks, can't proceed with server load".

## Cause — not Bountiful

Bountiful is not involved. It ships two recipes (`bountiful:bountyboard`,
`bountiful:decree`), neither of them `farmersdelight:cutting`, and nothing in
its jar touches Farmer's Contracts' data path. The two mods just happen to
occupy the same genre.

The offender is **our own `squid-calamari-dedup` datapack**.

Disassembling `RecipeCostSolver` (javap) pins line 285 exactly:

| line | bytecode | call |
|------|----------|------|
| 282 | `36:` | `json.getAsJsonArray("ingredients")` |
| **285** | **`65:`** | **`json.getAsJsonArray("result")`** |

So the bad field is `result`, and `parseCutting` is reached only by
`type == "farmersdelight:cutting"` (dispatch at `parse` line 229).

`squid-calamari-dedup` switches off Cultural Delights' calamari chain by
overwriting each recipe with a `neoforge:false` stub. The stubs were all written
with the **vanilla** result shape:

```json
"result": { "id": "minecraft:barrier", "count": 1 }
```

That is correct for `crafting_shapeless` / `smelting`, but a
`farmersdelight:cutting` recipe's `result` is a **list of chance results**.
Two files had it wrong:

- `data/culturalrecipes/recipe/cutting/raw_calamari.json`
- `data/culturalrecipes/recipe/cutting/raw_calamari_from_glowsquid.json`

### Why the `neoforge:false` doesn't save us

NeoForge evaluates `neoforge:conditions` before the recipe codec runs, so the
game itself never reads these files — they are correctly disabled in-game.
Farmer's Contracts, however, walks the raw `recipe/` directory in its own
`SimplePreparableReloadListener` and **never checks conditions** (no
`Condition` reference anywhere in `RecipeCostSolver` or
`ContractDataReloadListener`). It parses the dead stub, hits the object-shaped
`result`, and throws. `reload()` has no per-file try/catch around `parse`
(the `Couldn't parse {} '{}'` logger only covers JSON syntax errors), so one bad
file takes down the whole datapack load.

## Scan

Every `farmersdelight:cutting` recipe in the pack — 851 across 37 mods and
3 datapacks — checked for `result` / `ingredients` shape. Exactly **2**
malformed, both the files above. Nothing else in mods, `datapacks/`, or
`saves/datapacks/`.

## Fix

Both stubs rewritten with an array-shaped `result` (and a `tool` field, so the
stub is a structurally valid cutting recipe if anything else ever reads it raw):

```json
{
  "neoforge:conditions": [ { "type": "neoforge:false" } ],
  "type": "farmersdelight:cutting",
  "ingredients": [ { "item": "minecraft:barrier" } ],
  "tool": { "item": "minecraft:barrier" },
  "result": [ { "item": { "id": "minecraft:barrier", "count": 1 } } ]
}
```

Still disabled in-game, now survives Farmer's Contracts' unconditional parse.

Source: `proposed-datapacks/squid-calamari-dedup/`
Rebuilt to `proposed-datapacks/squid-calamari-dedup.zip`, copied to
`datapacks/squid-calamari-dedup.zip` and the live instance. Previous live zip
kept as `squid-calamari-dedup.zip.bak-2026-09-20`.

Rescan after fix: **0 malformed**.

## Side finding — 708 dead recipes still priced by FC

Because FC ignores `neoforge:conditions`, every condition-disabled recipe in the
pack still enters its cost solver:

| datapack | disabled recipes FC parses |
|---|---|
| `recipesplus-dead-compat-off.zip` | 460 (all `crafting_shapeless`) |
| `zz-unified-items.zip` | 236 (all `crafting_shapeless`) |
| `squid-calamari-dedup.zip` | 12 (mixed) |

Checked all 708 results: every one resolves to `minecraft:barrier`. So the only
effect is a junk barrier entry in the cost map — no real item gets mis-priced,
no contract is affected. Left alone.

The structural risk remains, though: **any future `neoforge:false` stub for a
`farmersdelight:cutting` recipe must use an array `result`**, or the server dies
again. `crafting_shapeless` stubs must keep `ingredients` as an array for the
same reason (`parseFlat` line 791). `smelting` / `smoking` / `campfire_cooking` /
`blasting` stubs go through `parseSingleIngredient`, which makes no array
assumption and is safe either way.

## Upstream

Worth reporting to Farmer's Contracts (flomik), two separate bugs:

1. `RecipeCostSolver.parseCutting` / `parseFlat` call `getAsJsonArray` without an
   `isJsonArray` guard — any malformed third-party recipe is a hard crash rather
   than a skipped entry.
2. `ContractDataReloadListener` ignores `neoforge:conditions`, so it parses
   recipes that are disabled and will never exist in-game.

Either fix alone would have prevented this.

## Who needs the fix (checked 2026-09-20)

**Server-side only.** Farmer's Contracts registers its listener exclusively via
`AddReloadListenerEvent` — `FarmersContractsMod.class` is the only class
referencing it, and there is no `RegisterClientReloadListenersEvent` anywhere in
the jar. So `RecipeCostSolver.reload` runs on the logical server (integrated or
dedicated) and never on a remote client. Its four mixins are villager/board
brain logic, nothing data-related, and `RecipeCostSolver` is only reached from
`ContractDataReloadListener` and `ContractPoolEntry`.

`squid-calamari-dedup.zip` also contains only `data/` + `pack.mcmeta` — no
`assets/` — so nothing on a client reads it either way. Recipe state syncs
server→client over the network.

**A joining player needs to add nothing.** Only the machine running the world
needs the corrected zip.

Deployed so far: the `1.21.1` client instance's global `datapacks/` folder.
`config/simple_datapacks.json` has `copyDatapacks: false` and
`requiredDatapacksPaths: ["datapacks"]`, so `simple-datapacks` loads it live at
world load — that covers singleplayer and Open-to-LAN from this instance.

### Stale copies left alone

`servers.dat` holds one entry, `localhost:25571`, which is the `X-expeditions`
test rig (`level-name=expeditions`, 440 mods). Scanned all of `test-server/`:

| rig | Farmer's Contracts | broken stub present |
|---|---|---|
| `A-full` | no | yes |
| `W-seedscout` | no | yes |
| `X-expeditions` (:25571) | no | yes |
| others | no | no |

**None of the test rigs has Farmer's Contracts installed**, so none of them can
reproduce this crash as they stand — the abort has to have come from somewhere
else (a separate dedicated install, or the integrated server). The stale broken
zip in those three is harmless today but will crash the moment FC is added.
Not overwritten: these are deliberate snapshots and swapping a datapack under
one could invalidate a comparison.
