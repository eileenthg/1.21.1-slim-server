---
name: orphan-configs-archived
description: "27 Sep: 52 configs for uninstalled mods moved to removed-configs/2026-09-27-orphans; how orphans were identified"
metadata:
  type: project
---

On 27 Sep 2026, 52 config entries for mods no longer installed were moved from the live `config/` folder to `modpack-audit/removed-configs/2026-09-27-orphans/`. The file list is in `_list.txt` there, so any entry can be moved back. They included Alex's Caves, Project Atmosphere, TPS Detective, REI (the pack is JEI-only now), YACL, White Noise, WaterUpgrade, Cluttered, CTOV, Better Strongholds, Ecliptic Seasons, Simple Clouds, Sword Blocking Mechanics, godswordspecs (the user's own OSRS specs mod, not installed) and others.

The method that works:
1. Take the tracked-config list from `logs/debug.log` ("Config file X for Y tracking").
2. Take the real mod IDs from `[[mods]]` blocks only. Dependency blocks also declare modIds, which made alexscaves and projectatmosphere look installed.
3. Cross-check against jar filenames.
The strict `[[mods]]` parse misses some mods, so check filenames too.

Kept on purpose: fml.toml, simple_datapacks.json, amc.json (Alex's Mobs), bc_particle, universalenchantmentinfo, xaerohud.txt, xaeropatreon.txt, fabric/ (Connector).

**How to apply:** if a mod is reinstalled, restore its config from that folder.
