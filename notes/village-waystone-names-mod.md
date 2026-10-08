---
name: village-waystone-names-mod
description: "villagewaystones mod renames village-origin waystones to their MCA village name (e.g. Zedreded→Mossburgh); installed 27 Sep, in-game untested"
metadata:
  node_type: memory
  type: project
  originSessionId: e4534241-5eff-47aa-897e-369673efbcb3
  modified: 2026-09-27T04:56:35.130Z
---

Built 27 Sep 2026: `villagewaystones-1.21.1-1.0.0.jar` (source `C:\Users\eteil.000\Claude\village-waystone-names`), installed in `1.21.1 slim` mods while the game was running, so it takes effect on the next restart. Not boot-tested because the user was hosting (see [[no-test-servers-while-hosting]]).

- Waystones names generated waystones lazily on first activation (MrPork generator, `nameGenerationMode = PRESET_FIRST`), so unactivated village waystones have `NameV2 ""` in `data/waystones.dat`. The mod hooks Balm `GenerateWaystoneNameEvent`, and every 100 ticks it re-applies the MCA village name (`VillageManager.findVillages`, margin 64) to every waystone with `Origin VILLAGE`.
- A player's manual rename of a village waystone gets reverted by design (the user asked for ALWAYS). A renamed MCA village propagates to its waystone.
- Trap: MCA villages with no buildings have box 0,0,0. The mod skips those, or a waystone near spawn could match the wrong village.
- Existing case: the waystone at 45,74,30 ("Zedreded", activated by Youchrist) sits about 10 blocks outside Mossburgh's box and should rename within about 5 s of the world loading. The log line to look for is `Renaming village waystone at`.

**How to apply:** if the names don't change, check `latest.log` for that line. Also check whether the MCA village has buildings registered (`mca_villages.dat` structures/buildings/externalBuildings).
