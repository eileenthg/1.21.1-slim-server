---
name: maid-chef-stations-overnight
description: "1 Oct 2026 night — user asleep; the instruction is \"keep going until all the work stations are covered\" for Maid Chef; progress and order"
metadata:
  node_type: memory
  type: project
  originSessionId: 76bb3d86-d612-4532-b107-6f9e3d682b3c
  modified: 2026-10-01T12:22:25.895Z
---

The user asked on 1 Oct 2026 (night) to keep going, while they sleep, until every cooking station in the pack is covered by Maid Chef. They named Spectrum's potion workshop and titration barrels explicitly as in scope (they output food).

The order and recipe counts are in maid-chef/README.md under "Stations". Each station is tested on the X-expeditions harness via /maidchef selftest; the dev jar goes in extra-mods/. Install the release build into the live pack only after the self-test passes. The live instance currently has the 1 Oct 13:51 release jar.

**Why:** the user is unavailable overnight, so the work has to be autonomous.
**How to apply:** after compaction, read the README "Stations" section and the SelfTest scenes to see what's done, then continue with the next station. Always check that no other java/server is running before each boot ([[wait-for-other-servers-before-boot]]). Related: [[maid-chef-mod]].

**Done 2 Oct 07:39:** every cooking station is covered (list in maid-chef/README "Stations"). The harness self-test passed 45/45 and the release jar is installed in the live pack (the old jar is in removed-mods/maidchef-2026-10-01-1351). Spectrum stations are untested headless, because they need an online owner with advancements: verify them in game. The Butchery pestle and ExD juicer have zero recipes because zz-unified-items disables them. Leave-and-return (ChefData Brewing, PICKUP, attendNeeds for vat stages) is verified via the vat and evaporator scenes.
