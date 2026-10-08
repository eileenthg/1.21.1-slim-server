---
name: thinair-roadweaver-perf-mods
description: "thinairthrottle + roadweavergate patch mods (27 Sep) — what they gate, configs, test status; LMC removal breaks colony-autoteach"
metadata:
  node_type: memory
  type: project
  originSessionId: dcb96848-cc08-4664-9404-470eb2075637
  modified: 2026-09-27T07:37:52.629Z
---

Built 2026-09-27 from the spark findings in [[villager-tps-tuning]]. Sources: `modpack-audit/thinair-throttle`, `modpack-audit/roadweaver-queue-gate` (gentle-swings Gradle template, target jar in `libs/` as compileOnly).

- **thinairthrottle** (pins thinair [1.0.5]): @Redirect in TickAirHandler.onLivingBreathe on `AirQualityHelper.getAirQualityAtLocation(LivingEntity)` → per-entity cache (@Unique field on LivingEntity). Eye-level block still checked every tick; cache refreshed every `checkIntervalTicks` (default 20, 1 = original) or on moving > `recheckDistance` (4) or dimension change. Air drain/refill still per tick. Config `config/thinairthrottle-common.toml`.
- **roadweavergate** (pins roadweaver [2.3.1-1.21.1]): cancels RoadGenerationService.refreshQueue unless dirty. Dirty on StructureFileStorage.setStructureConnections/mergeStructureConnections TAIL (= RoadWeaver's own planner reaching unplanned tiles, status changes, API), server start, player teleport/dimension change/respawn/login, plus `fallbackRefreshTicks` (600). Config `config/roadweavergate-common.toml`, `logRefreshes` for verification.
- Both installed in `1.21.1 slim` 27 Sep; in-game untested. Gate verified on X-expeditions (refresh at startup, then only +600-tick fallback). Thin Air verified on `test-server/mini-thinair` (only thinair + throttle, junctioned libraries): interval 1 vs 20 gave identical Air (y200 drains ~0.25/tick, y100 stays 300, underwater drains, refills on surfacing); mixin export shows the redirect applied.
- Headless harness can't test entities: forceloaded chunks never load without a player in this pack. Use mini-thinair-style servers.

**LMC removal 27 Sep 13:45:** user deleted lazychunkload, loadmychunks and lmctickfix (Recycle Bin). colony-autoteach.zip still has recipes for `loadmychunks:*` items → MineColonies CrafterRecipeListener "can't proceed with server load" (reproduced on harness). Fixed 27 Sep at user's request: 144 `loadmychunks_*` recipes stripped (backup `datapacks-retired/colony-autoteach.zip.bak-2026-09-27-pre-lmc-removal`); no other missing namespaces; harness boots (`Loaded 21746 recipes for 45 crafters`). See [[colony-autoteach-datapack]].

**Why:** villager/tick lag in the hosted world.
**How to apply:** on Thin Air or RoadWeaver updates, the version pins will block loading — recheck the target methods and bump.

**Thin Air removal 29 Sep:** the user removed Thin Air, and thinairthrottle went with it. 10 `thinair_*` crafterrecipes were stripped from the installed colony-autoteach.zip; the backup is `datapacks-retired/colony-autoteach.zip.bak-2026-09-29-pre-thinair-removal`. The project copy (`colony-autoteach-proposed/colony-autoteach.zip` + `autoteach/`) had also missed the LMC strip, so both thinair and loadmychunks were removed there too. Configs were archived to `removed-configs/2026-09-29-thinair`. Amendments' optional ThinAirCompat class is harmless. `tag_item_audit.csv` and `recipe_audit.csv` still list these items, so a rebuild re-adds them. Regenerate the audit, or filter out removed namespaces, before rebuilding.
