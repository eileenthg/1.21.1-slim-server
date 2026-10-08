---
name: anotherscape-removed
description: AnotherScape was removed from the 1.21.1 pack on 2026-09-19; the archive location and what was changed
metadata: 
  node_type: memory
  type: project
  originSessionId: a7532cd9-95f7-4714-9ac8-6b40e36e846f
  modified: 2026-09-18T23:31:24.540Z
---

On 2026-09-19 the user removed AnotherScape (OSRS mod) from the pack. Eternal Tales and every other mod stay, including Waystones, which predates AnotherScape. The user plans a fresh world. New World (1) is kept but has AnotherScape ore holes and hub remnants.

Archive: `modpack-audit/anotherscape mods/`, with a README describing how to restore. The user asked for it "if we change our mind again". It holds the jars (including the chunk-guard patched AnotherScape build), the old spawn-onboarding source, the godsword-specs project (moved there), configs, datapacks and a world copy.

What changed:
- spawn-onboarding 1.1.0: all AnotherScape code is cut and there is no config any more. What remains: MCA destiny timing, the Origins screen queue, Eternal Town placement, and CreatureLinks (tarantula hawk).
- The godswordspecs and townsfolk datapacks are removed.
- creature-links has no AnotherScape entries.
- Compat Delight's naturalist module is keyed to aquaculture; marlin and bass use Aquaculture stand-ins.
- AlmostUnified shrimp/ruby entries and mod priority are removed.
- Lost Maids and REI lists are cleaned.
- Load errors before and after the removal were identical (126).

**Why:** the user reversed course. There had been many AnotherScape-specific tweaks (reports 17–20).

**How to apply:** don't propose AnotherScape fixes. Anything in old reports or memories about the hub, sea spawns, OSRS specials or AnotherScape NPCs is historical. The test-server folders (A-full, B, X-expeditions, baseline) still contain AnotherScape jars; A-full is shared with another session. colony-autoteach-proposed still has AnotherScape recipes but isn't installed. Related: [[item-merge-pipeline]].
