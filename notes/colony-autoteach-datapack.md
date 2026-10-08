---
name: colony-autoteach-datapack
description: "colony-autoteach crafterrecipes datapack: generator bugs fixed, job-based curation, and the datapack install location this modpack actually uses"
metadata:
  type: project
---

`modpack-audit/colony-autoteach-proposed/` builds a MineColonies `crafterrecipes`
datapack so colonists know recipes without being taught. Built and boot-tested
2026-09-25: **21,646 recipes / 28 crafters**, server logged
`Loaded 21890 recipes for 45 crafters`, `Done` in 43 s, zero parse errors.

Pipeline: `gen_crafterrecipes.py` (audit-driven generation) then
`curate_crafterrecipes.py` (job-based redistribution) → `audit-report.md`.

**Inputs need a server boot with `auditcraftingtags = true`** in
`config/minecolonies-server.toml`. The audit fires on `OnDatapackSyncEvent`, which
a plain start does NOT fire — run `/reload` once after `Done`, then stop. Produces
`<world>/minecolonies/recipe_audit.csv` + `tag_item_audit.csv` (a full item
registry dump, 19,007 ids). Both are copied into the project.

**Three generator bugs found and fixed:**
- Item tags were overwritten, not merged, across sources. 12 jars feed
  `#minecraft:planks`; only the last survived (3 entries instead of 89), silently
  truncating every tag ingredient and variant expansion.
- Recipe conditions ignored → 873 recipes disabled in this pack were still taught.
  Crafter recipes bypass the vanilla recipe book, so this resurrects removed items.
  Relates to [[disable-stub-recipe-shapes]].
- **Unregistered item ids brick the server.** `CustomRecipe.parse` →
  `ItemStorage.<init>` does `Optional.get()` on the registry with no fallback;
  one bad id aborts the datapack load ("can't proceed with server load"). 2,857
  such recipes were being emitted. Fixed with `--items tag_item_audit.csv`.

**Why:** `recipe_audit.csv` is a *permission* matrix, not a job assignment. Read
literally it gave Builder and Miner every 2x2 recipe (10,176 each) and put table
recipes in stonecutter/smithing/cutting-board modules and expedition huts.

**How to apply:** rerun both scripts with `--items`; never ship a crafterrecipe
naming an item no installed mod registers.
