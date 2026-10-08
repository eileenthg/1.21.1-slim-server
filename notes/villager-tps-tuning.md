---
name: villager-tps-tuning
description: 27 Sep spark profile findings (villagers ~21% of tick) + config changes applied; Thin Air/RoadWeaver hotspots from source
metadata:
  node_type: memory
  type: project
  originSessionId: dcb96848-cc08-4664-9404-470eb2075637
  modified: 2026-09-27T05:57:07.904Z
---

Spark profile 2026-09-27 (https://spark.lucko.me/JAKcm5CVHF, standing in a village): 20 TPS, MSPT median 22.7 / p95 76.9 / max 339. MCA villagers ≈21% of tick. Parse raw data from spark-usercontent.lucko.me/<id>; StackTraceNode fields: class=3, method=4, line=6, desc=7, times=8 (packed doubles), children_refs=9.

Applied 27 Sep in `1.21.1 slim/minecraft/config` (backups `*.bak-<timestamp>`):
- LibertyVillagers.json findPOIRange 128→64, pathfindingMaxRange 256→128
- townstead-server.toml enableGroundItemSourcing / enableGroundItemThirstSourcing → false (container sourcing left on; RefuelTask storage scan ≈4.3% of tick)
User removed LazyChunkLoad (its CpuMonitor.getProcessCpuLoad ran every tick, 15% of tick).

Source findings (not yet patched):
- Thin Air 1.0.5: LivingBreatheEvent (HIGH) for tag `thinair:air_quality_sensitive`; MCA's MixinEntityType `villagerTagsHacks` makes MCA villagers match any tag containing minecraft:villager, so they're checked. Lookup iterates every stored air-provider block (lava/fire included) in 5×5 chunks per entity per tick.
- RoadWeaver 2.3.1: RoadGenerationService.tick → refreshQueue re-reads+Gson-parses `data/roadweaver/persistence/structures/minecraft_overworld/state.json` (~100 KB) from disk EVERY tick; all 225 connections were COMPLETED. ≈8% of tick. Patch candidate: cache/throttle.
- MineColonies DataPackSyncEventHandler.load is one-shot (static `loaded` flag) — the 5.6% was startup, not recurring. Profile likely included world load.

Related: [[lmc-tick-fix]], [[pregen-headless-roadweaver]], [[timm-structure-lag]]
