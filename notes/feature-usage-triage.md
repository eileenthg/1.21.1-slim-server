---
name: feature-usage-triage
description: 30 Sep usage triage from player stats — no colony founded, Nether/End never visited, big zero-use deco mods; report 29-feature-usage-triage.md
metadata:
  type: project
---

On 30 Sep 2026 I ranked all 524 jars by player-stat interaction, using stats that were last written 28 Sep (about 50 h total: user 34.5 h, Youchrist 16.2 h). The report is `modpack-audit/29-feature-usage-triage.md`.

Key facts:
- No MineColonies colony exists: `minecolonies_colony_manager.dat` is 232 B.
- The Nether and End have never been entered (about 5 MB each, spawn chunks only).
- Zero use for: Colorful Azaleas, Display Delight, Spectral Decorations, Vanilla Backport, Blazing Hot, Showercore, Recipes Plus, MC Trade Post and Reinforced Construction.

**Why:** the user asked which features aren't being used, so they can triage cuts. No decisions were made yet.

**How to apply:** rerun the scratchpad approach before advising any cuts. Read the stats JSON, map namespaces to jar modIds, and check region-file sizes per dimension. Don't cut the MineColonies cluster without asking, because of the [[colony-expeditions-addon]] and [[minecolonies-perf-config]] work.
