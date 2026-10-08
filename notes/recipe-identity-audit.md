---
name: recipe-identity-audit
description: "Recipe audit tooling + the 29 Sep fixes for recipes that erase item variants (cheese block, egg crate, coffee, bell peppers, duplicate crates)"
metadata:
  node_type: memory
  type: project
  originSessionId: 79ff7ded-31b6-488a-af71-bcce79fc21b7
  modified: 2026-09-29T01:45:07.583Z
---

`modpack-audit/tools/recipe-audit/` scans the live pack for recipes that destroy item identity:
lossy round trips (many variants in, one item back) and conflicts (same inputs, different results).
It models datapack override order, `neoforge:false`, AlmostUnified (placeholders, `ignored_tags`,
`ignored_items`, `priority_overrides`, stone variants), `neoforge:difference` ingredients, and skips
items no mod registers. `scan_recipes.py` is importable for ad-hoc questions (`resolve`, `canon`, `EXISTS`).
Report: `modpack-audit/23-recipe-identity-audit.md`.

Fixes on 2026-09-29, all still **untested in game** (pregen server was running):
- New datapack `datapacks/zzz-recipe-identity.zip` (sorts after zz-unified-items, so it wins). Restricts
  5 Extra Delight compaction recipes to the item their uncraft returns, and subtracts the 13 variant-bookshelf
  woods from the Recipes Plus wildcard bookshelf via `neoforge:difference`.
- 8 duplicate crates merged (added to `decisions.py`/`candidates.py` and the live AU config).
- `c:crops/bell_pepper` removed from unify tags as a **precaution**, not a live bug: AU skips a tag whose members
  all come from one mod, so the colours were never collapsing. Caught by checking the AU log, per
  [[recipe-progression-rule]] — always confirm a suspected merge there before acting on materials.json.

**Why:** the user wants variants kept; merging is only for genuine cross-mod duplicates.

**How to apply:** when adding mods, re-run the two scripts. A unify tag holding items from 2+ mods where one mod
contributes several is the tell-tale of an over-merge — a tag whose members are all one mod is skipped by AU, so
check the log before "fixing" it. See [[item-merge-pipeline]] for the rebuild pipeline (index.json is
still missing, so hand-patch the installed files and mirror into the scripts, as done here).
