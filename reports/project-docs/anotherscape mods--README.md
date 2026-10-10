# AnotherScape archive (removed 19 Sep 2026)

Everything that existed for, or was changed because of, AnotherScape, as it was just before removal. Enough to put it all back.

## What's here

| Folder | Contents | Where it came from |
|---|---|---|
| `mods/` | `anotherscape-1.0.35.jar` (the chunk-guard **patched** build that was installed), `godswordspecs-1.21.1-1.1.0.jar` (OSRS special attacks), `spawnonboarding-1.21.1-1.0.0.jar` (the AnotherScape version), `compatdelight-1.0.1.1-substitutes.jar` (naturalist module keyed to AnotherScape) | instance `mods/` |
| `datapacks/` | `anotherscape-townsfolk-no-guild.zip`, `creature-links.zip` (with AnotherScape entries), `zz-unified-items.zip` (with AnotherScape bass/marlin/lizard stand-ins) | instance `datapacks/` |
| `config/` | `spawnonboarding-common.toml` (+ 5 `.bak`), `touhou_lost_maid-server.toml` (69 AnotherScape items banned from lost maid gear), `almostunified/tags.json` + `unification/materials.json` (shrimp and ruby entries, mod priority), `roughlyenoughitems/config.json5` (osrs_raids category) | instance `config/` |
| `world/New World (1)` | the world with the AnotherScape spawn hub built | instance `saves/` (the original is still there too) |
| `source/spawn-onboarding` | full source of the AnotherScape version (hub build/paste/protection/respawn, townsfolk, guild NPCs, sea spawns, pet screen). Compile libs not copied: `libs/anotherscape.jar` = `mods/anotherscape-1.0.35.jar`; `mca.jar` and `origins.jar` are still in the live project | `Claude/spawn-onboarding` |
| `source/godsword-specs` | OSRS Special Attacks mod project (moved here, not copied) | `Claude/godsword-specs` |
| `source/spec-tester` | client test mod for the specials (moved) | `modpack-audit/test-client/spec-tester` |
| `source/creature-links` | datapack source with AnotherScape entries | `modpack-audit/datapacks/creature-links` |
| `source/unification` | `build.py`, `extras.py`, `decisions.py`, `candidates.py`, `patch_compatdelight.py` before the change | `modpack-audit/unification` |
| `other/fixed-mods` | `anotherscape-1.0.35.jar.ORIGINAL-BACKUP` (unpatched), `chunkguard-patch`, `chunkguard-batch2` (the patch work), `spawnonboarding-1.21.1-1.0.0.jar` (old copy) | `modpack-audit/fixed-mods` (moved) |
| `other/datapacks-retired` | `anotherscape-ranged-fix` (crossbow/Sword Blocking tag workaround, already retired) | `modpack-audit/datapacks-retired` (moved) |

## To restore
1. Copy `mods/*.jar` back into the instance `mods/` (and remove `spawnonboarding-1.21.1-1.1.0.jar`); copy `datapacks/*.zip` and `config/*` back.
2. Move `source/godsword-specs` back to `Claude/godsword-specs`; restore `Claude/spawn-onboarding/src` from `source/spawn-onboarding` and put `anotherscape.jar` back in its `libs/`.
3. `unification/`: restore the five scripts, or set `REMOVED_MODS = ()` in build.py and revert the naturalist/tropicraft stand-ins in extras.py.
4. Waystones was never removed (it predates AnotherScape).

Reports that describe the AnotherScape work: 17, 18 (hub parts), 19, 20. The removal itself: report 21.
