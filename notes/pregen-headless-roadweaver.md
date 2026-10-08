---
name: pregen-headless-roadweaver
description: "Headless pregen of the survival world with RoadWeaver roads (25 Sep): P-pregen server + pregen-helper mod; RoadWeaver/gateway/GlobalGamerules gotchas"
metadata:
  node_type: memory
  type: project
  originSessionId: de3f9b64-5d45-4649-aa81-e3a96a04b94f
  modified: 2026-09-25T20:55:10.951Z
---

On 2026-09-25 the survival world "New World" was pregenned headless by `modpack-audit/test-server/P-pregen`. It uses hard-linked mods from sync-mods.ps1, with chunky-offline excluded. `libraries` is a junction to X-expeditions, `world` is a junction to the live save, and `config` is a copy of the live config. The `modpack-audit/pregen-helper` mod reads `config/pregenhelper.json`. It freezes ticks (`/tick freeze`; Chunky still generates), links listed Immersive Gateways, plans RoadWeaver roads around the areas, waits for the roads to build, then runs Chunky per area and stops. The pre-run world backup is at `modpack-audit/world-backups/`.

- RoadWeaver stamps roads only while a chunk generates, with no retrofit. Beyond ±2048 of spawn it plans only around online players, so plain Chunky gives road-less land.
- Immersive Gateways rolls a random destination on first activation (+z, 1024–8096 blocks away), so it can't be traced before linking. Links are stored in data/immersive_gateways.dat.
- The GlobalGamerules mod (config/globalrules-common.toml) re-applies its config gamerules when a server loads the world. After any server run, diff level.dat GameRules against the backup. The config has water_source_conversion=false, random_tick_speed=2, do_daylight_cycle=true.
- Never assume gamerule values: after any server run, restore the world's own pre-run GameRules and DayTime from a level.dat snapshot. waterSourceConversion was false on 25 Sep, and on 29 Sep the user confirmed true as intended. Snapshot rather than hardcode.
- Live Chunky 10k task cancelled; chunky-offline scoreboard status set to -1.
- 26 Sep: the world was pregenned to a ±4500 square plus 500 blocks around all 24 gateway pairs' ends. The world is now 8.1 GB. The user chose ±5000 and asked to shrink it to fit gateway areas, because they want disk headroom. `gridRadius` in the job plans RoadWeaver tile by tile.
- Under `/tick freeze`, expired chunk tickets are never purged, so chunks never unload. The heap filled at ~50k chunks. The helper now calls `stepGameIfPaused(1)` every ~200 loops, which cost about 18 s of game time over 2.5 h.
- Raise RoadWeaver's sharedWorkerThreads/threadDutyCycle only in P-pregen's config copy. Planning 40 tiles then took 10 min.

- 29 Sep: new home base at (-2950,-3670), where both players' spawns are set. Pregenned ±2000 around it plus 5 gateway destination areas, in 2 passes (13 grid tiles, then 3 new gateways found in the new land). The world is now 9.2 GB. `gridCenter` was added to the job. Seasons and calendars were unchanged and DayTime was restored. (Its targeted backup was deleted 5 Oct at the user's request.)

- 5 Oct: regenerated all unvisited overworld chunks for Gensokyo Legacy, Magical Forest, and the BSS whitelist. Kept: InhabitedTime>0, gateway pair ends ±1 chunk, the 3 player-activated waystones ±1, plus a closure (the start chunk of any structure touching a kept chunk). That kept 13,095 and deleted 465,923 (trim.py zeroes region/entities/poi headers). The BSS global index had to be pruned of deleted-chunk entries (2769→137): its overlap check runs before the whitelist and would reject a structure against its own stale record. Unactivated waystones in deleted chunks were pruned too (247→14). Regeneration used `planRoads:false`, since stored RoadWeaver roads re-stamp on generation. Full backup: world-backups/New World 2026-10-05 pre-regen; the 25 Sep and 29 Sep backups were deleted at the user's request.
- **Launching P-pregen:** Bash background tasks are killed at ~30 min, which also kills a server started inside them. On 5 Oct this killed it mid-run. Launch via `Invoke-CimMethod Win32_Process Create` on `run-detached.sh` (bash wrapper: `tail -f console.in | java`); stop it via console.in. The wrapper's server-exit.log is NOT written when java exits (tail keeps the pipe open). Detect exit with jps, then kill the leftover bash/tail.
- **5–6 Oct regen result:** finished 6 Oct 01:38 (~12 h). Every target chunk is full, 0 damaged chunks, 2533 structure starts, 0 overlapping pairs; Gensokyo Legacy placed 50 hakurei_shrine, 32 marisa_house, 12 morichika_shop. Artificial roads in regenerated full chunks 98.4% present (control 2.5%), all 61 roads OK. Seasons and calendars unchanged, DayTime and gamerules restored, waterSourceConversion=true kept. The world is now 12.4 GB of region data: chunks average ~24.6 KB vs 22 KB before; only 0.45 GB is dead space. Python on Windows can't read /c/... paths, so use C:/ in python checks.
- **Do not regenerate or pregen with a gensokyocompat older than 1.1.1.** 1.1.0 strips WorldGenSettings from level.dat on every save; see [[gensokyo-bridge]].

- 6 Oct: thinned the Legacy structures (see [[gensokyo-bridge]]); the removal run (52 areas, ~25 min incl. boot) used the pregen helper with planRoads:false. Border note: making a chunk full also creates structure_starts-status ring chunks up to ~8 chunks beyond, with their own structure starts.

**How to apply:** for more pregen, edit pregenhelper.json and re-run P-pregen, then restore gamerules. See [[test-server-a-full-stale-mods]].
