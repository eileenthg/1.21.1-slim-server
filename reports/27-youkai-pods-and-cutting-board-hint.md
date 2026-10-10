# 27 — Youkai pods can't be cut; cutting board hint (2026-09-25)

## Youkai's Homecoming pods

`youkaishomecoming:cutting/pods_cutting` (pods -> soybean, knife) was switched off by our own
`zz-unified-items` datapack with a `neoforge:false` stub. The soybean merge made
`extradelight:soybeans` the dominant item ("ED pod shucking pipeline"), and build.py switched
off every other recipe that made soybeans. But Youkai's crop and pods bag still drop
`youkaishomecoming:pods`, so the pods had no use left.

Fix: the stub is removed from the zip. AU still retargets the output, so pods now give
**Extra Delight soybeans** (1 per pod; ED's own pods give 3). build.py has the recipe in `KEEP_IDS`, so a
rebuild keeps it. Pre-change zip: `datapacks-retired/zz-unified-items.BEFORE-PODS-FIX.zip`.

## Cutting board: "Maybe with a different tool..." for everything

Upstream Farmer's Delight 1.3.4 bug. `CuttingBoardBlockEntity.getMatchingRecipe` asks the
`CachedCheck` for a recipe matching **item and tool together** (`CuttingBoardRecipe.matches`
tests both). On a miss it always shows `invalid_tool`. Its `invalid_item` branch ("This doesn't
seem cuttable...") only runs when a recipe matches but its tool check fails, and that never happens.

Patch (`fixed-mods/fd-cutting-hint-patch/`): ASM replaces both message keys with
`CuttingBoardHint.messageKey(getLevel(), getStoredItem())`. That method returns `invalid_tool` only if some
`farmersdelight:cutting` recipe's ingredient accepts the stored item, and `invalid_item` otherwise.
It only runs after a failed cut. It passes the ASM verifier (CheckClassAdapter) with a full classpath.

Rebuild: javac `src/` against neoforge-21.1.250-client + client-1.21.1-srg + FD jar,
`PatchCuttingBoard <orig class> out/classes/...`, then `build_jar.py <orig jar> <out jar>`.
Input jar backup: `fixed-mods/FarmersDelight-1.21.1-1.3.4-patched.jar.ORIGINAL-BACKUP`.

## Install status

Installed 25 Sep, checksums verified against the staged copies. Not yet tested in-game.
