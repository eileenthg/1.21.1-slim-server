---
name: seedscout-seed-search
description: "Seed Scout helper mod: dumps resolved worldgen datapack and scans seeds for a village at a temperate spawn; what the pack's worldgen actually does (BSS ordering, TerraBlender)"
metadata: 
  node_type: memory
  type: project
  originSessionId: ef775b41-1b59-4491-a95e-5a278ad98396
  modified: 2026-09-19T17:02:23.931Z
---

Since 2026-09-20 the server-only helper mod `modpack-audit/seedscout` (mod id `seedscout`) does the pack's seed searching.
- The user asked for this instead of mcseedmap.net, since those tools can't read modded worldgen.
- Test server: `test-server/W-seedscout`, port 25611.
- Driver: `seedscout/server.sh restart|stop|cmd`. It only ever kills the PID it launched.
- Other test servers run at the same time. Keep to this folder and PID, and check free RAM first.

Commands:
- `/seedscout dump`: writes `seedscout/worldgen-datapack`, zipped at `seedscout/out/modpack-overworld-worldgen.zip`.
- `check` / `predict <seed>` / `trace <seed>`
- `scan <n> <threads> <maxDist> <rngSeed>`: writes a CSV under the server's `seedscout/`.

What decides a spawn village in this pack (each was verified against real generated worlds):
- **Terrain and biomes:** Tectonic, CliffTree and Lithostitched, plus TerraBlender regions (VanillaBackport). The dump uses Tectonic's custom density functions (config_constant, invert, etc.), so vanilla-only map tools can't render it.
- **Spawn:** the climate sampler's `findSpawnPosition` chunk. The prediction matched the real world.
- **Better Sparse Structures:** blocks any structure start within a size-scaled spacing of one generated earlier, so which structure appears depends on generation order. At world creation the spawn chunk's structure starts are generated over a square of radius 12, x outer then z (ChunkGenerationTask.scheduleLayer). Replaying in that order matched the real accepted list exactly (seed -5498407288441967890). Nearest-first order did not.
- The Quark village spawn nerf is OFF in the config, so it isn't the reason villages are missing.
- Villages are very rare: nearest-first `/locate` found them 1500–2500 blocks away, mostly because of BSS.

Tools: `seedscout/bss_index.py <world> x z` lists the BSS accepted index. `seedscout/region_starts.py <world> cx cz r` lists real structure starts.

**How to apply:** always confirm a candidate seed by generating a real world on W-seedscout (edit level-name/level-seed) before handing it to the user. Related: [[survival-stack-twt-coldsweat]] (the "safe temperature" rule uses Cold Sweat's world.toml biome °F ranges: at least 40°F at night, at most 85°F at noon).

More findings from 2026-09-20:
- **Integrated Villages** `disableVanillaVillages` is on, so vanilla villages never generate. The replay now calls the real mixin-patched `ChunkGenerator.tryGenerateStructure` (reflection, scratch ProtoChunk, StructureManager with a null level) so every mod's disable-mixin applies.
- **Better Sparse Structures** spacing is `distanceModifier^(2t−1)` × 10 chunks, which gives a village about 800 blocks. With the pack's config, a village at spawn is effectively impossible, and 2 verified seeds had no village.
- Whitelisting the villages (tested on the W-seedscout config copy only, saved as `seedscout/bettersparsestructures-server.toml.WHITELIST`) gives 87 hits per 4000 seeds. Verified real spawn villages:
  - 4589974112285131388: Mossy Mounds, plains
  - -482566574268728454: Tavern village, forest
  - 9192056500796087225: predicted, but no village in the real world
- Known replay bug: under the whitelist it sometimes wrongly rejects or accepts which Integrated Villages variant generates. Always verify candidates in a real world with `seedscout/verify.sh <seed>`. The user has not yet decided whether to apply the whitelist to the real instance.

Config decision (user, 2026-09-20): vanilla villages are wanted back, and the only worry about un-sparsing villages is structures overlapping (they recall a swamp village over witch huts in an older CTOV kitchen-sink world). Test config on W-seedscout now has:
- `integrated_villages-neoforge-1_21.toml`: "Disable Vanilla Villages" = false (original saved as `seedscout/integrated_villages.toml.ORIG`).
- BSS: villages whitelisted, `countWhitelistedStructuresForSpacing = true` (so villages are remembered and later structures keep away from them), `spacingRadiusOverrides` of 2 chunks for villages (so a village does not become an ~800-block dead zone).
- Verified in BSS bytecode: `decideStructure` runs the OVERLAP check BEFORE the whitelist bypass, so a whitelisted village still cannot be placed on an existing structure. Only spacing/repetition are skipped. With `countWhitelisted...=false` a later structure could land on a village, which is why it is now true.

Final state 2026-09-20: the config change is APPLIED to the Prism instance (backups `proposed-configs/*.BEFORE.20260920.toml`). Instance had no saves, so a dedicated server would need the same two edits.
Predictor: spawn biome now requires every quart cell of the spawn chunk to be comfortable (a seed failed with the chunk centre in plains but the real spawn point in ocean). New `/seedscout recheck <seed,seed,...>` re-runs candidates without a full scan.
Village predictions are only ~60% right (5 of 8 verified), spawn/biome has been right every time. Always run verify.sh.
Verified seeds (village hull contains spawn unless noted), all with 0 overlapping structures nearby:
- 463772255524938046: plains, Marketstead village
- 4589974112285131388: plains, Mossy Mounds
- -482566574268728454: forest, Tavern village
- 677032262762537796: plains, vanilla plains village, edge 14 blocks from spawn

**4 Oct 2026, a bug found by real regeneration:** `TerraBlenderBridge.forSeed` (cloning the parameter list and swapping in a new uniqueness Area) yields the WRONG TerraBlender region map. Climate and terrain match (heights 100%), but biomes agreed only 81% with the world's own biome source and with actually regenerated chunks. It invents YH sakura birch where real gen has old_growth_birch, and so on. Any prediction for a seed other than the running world's own seed is unreliable for modded regions. BiomeMap now uses the world's own source and RandomState when the seed equals `level.getSeed()`. To map a seed, boot a world whose level.dat has that seed (copy level.dat into an empty world folder). The spawn checks probably passed only because spawns mostly land in vanilla regions.
