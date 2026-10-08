---
name: minecolonies-perf-config
description: MineColonies server config tuned for a small "pet" colony on 27 Sep 2026 (citizens 50, raiders 25, strictness 6, tree 150)
metadata:
  type: project
---

On 2026-09-27, before founding any colony, the user tuned `1.21.1 slim/minecraft/config/minecolonies-server.toml`. The backup is `.bak-20260927`.

- maxcitizenpercolony 250 → 50
- maxRaiders 80 → 25
- colonyloadstrictness 3 → 6
- maxtreesize 400 → 150

loadtime stays 20 and pathNodeLimitMultiplier stays 1.0; the user chose to keep both. The file has CRLF line endings, and `sed -i` in Git Bash strips them, so restore the CRLF endings after any edit.

**Why:** a performance audit ranked MineColonies 🔴 High. The user plans to use MineColonies only to revive abandoned colonies (from Tweaks `emptyColoniesGenerationChance = 1.0`), "like raising a pet".

**How to apply:** if colony lag shows up, check the citizen count, couriers/warehouse and the autoteach recipe load ([[colony-autoteach-datapack]]) before changing settings again. Profile with spark.
