---
name: runic-gods-port
description: "Runic Gods NeoForge 1.21.1 port plan (paused) + BrassAmber Battletowers port (in progress, modpack-audit/battletowers-port)"
metadata: 
  node_type: memory
  type: project
  originSessionId: b1b89305-1490-48c1-b66f-bdedd5b6a485
  modified: 2026-09-19T09:45:20.153Z
---

**Runic Gods** (https://github.com/otectus/runic-gods, Forge 1.20.1, GeckoLib, ~28k lines): port to NeoForge 1.21.1 **paused** since 2026-09-19. No alternative mod exists (RPG Gods is dead at 1.19.2). All Rights Reserved → private use only.

**Decision changed 2026-09-19:** the user cancelled "summon-only + structure hook" and the Apotheosis-tower idea. Instead: **install BrassAmber Battletowers, porting it to 1.21.1**, so Runic Gods keeps its original tower-boss design. Runic Gods only couples to BABT via golem entity IDs (`ba_bt:land_golem`, ocean/core/nether/end/sky) — keep those IDs.

**Battletowers port:** `modpack-audit/battletowers-port` (git repo). Based on upstream commit **52478b14** (2026-01-06) = the released 3.0.0-beta3.2.1 jar (CurseForge, 13 Jan 2026). Do NOT use the branch heads (`1.20.1-forge`, `1.20.1-Forge-Dev`): they are mid-refactor and don't compile. Upstream is All Rights Reserved: private pack use only.
- Enchantments `depth_dropper`/`core_temperature` are now data-driven JSON; resonance stones use jukebox_song JSON.
- Test server: `modpack-audit/test-server/BT-battletowers` (only the BABT jar; drive.sh feeds commands).

**Status 2026-09-19 (evening):** server-side port verified and **installed in the Prism 1.21.1 instance** (`brassamberbattletowers-1.21.1-3.0.0-beta3.2.1-neoport.1.jar`). Land, ocean and core towers generate fully: obelisk setup finishes, spawners are 21/23/25 (matching BTStatics), 8 chests + 1 golem chest each, mobs assigned. Golems and cultist work. Two runtime fixes came from testing: `LootContext.Builder.create(Optional.empty())` (null broke chest setup) and adding the corrite bookshelf to the vanilla CHISELED_BOOKSHELF block entity type (worldgen crash). **Not yet tested:** the client side (renders, music, chest GUI) and fighting a golem end-to-end (key → monolith → golem → tower collapse). The item-frame errors near (-67,8,191) are not from Battletowers.
- Test tips: check tower contents by reading region files with the stdlib NBT reader (`/fill` failures don't show in the server log); delete `logs/` before each run so drive scripts don't see the previous run's "Done".

**How to apply:** when stopping a test server, kill by PID only. On 2026-09-19 `taskkill /IM java.exe` may have closed the user's other Java apps. The Runic Gods port resumes after Battletowers is verified in-game.
