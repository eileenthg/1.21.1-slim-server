---
name: mca-rpg-bridge-mod
description: "MCA RPG Bridge (mcarpgbridge): links MCA Quests, MCA Reputation and Mebahel's RPG quests; why it exists, test harness, what's unverified"
metadata: 
  node_type: memory
  type: project
  originSessionId: a50e9a3a-e012-413b-834b-af0bfb7b89b8
  modified: 2026-09-19T15:16:29.740Z
---

On 2026-09-19 the user asked to fix MCA Quests ↔ MCA Reputation and to "merge" MCA Quests with Mebahel's RPG Villager Quests. They picked a **bridge mod** over a full port. A port would lose Mebahel's spare/mercy branches, scripted bosses and quest boards.

The mod is `C:\Users\eteil.000\Claude\mca-rpg-bridge` (mod id `mcarpgbridge`). Report: `modpack-audit/23-mca-quests-reputation-mebahel-bridge.md`. What it does:
- It makes Quests 1.6.5 accept Reputation API v2. On NeoForge Reputation reports 2 and Quests required ==1.
- MCA villagers no longer get their right-click taken by Mebahel. A Request/Rumor button in MCA's menu replays Mebahel's `lambda$registerQuestInteraction$N` handler. That number is positional and moves per Mebahel version (2.0.6 = $2, 3.2.0 = $3), so since bridge 1.2.0 it is found by prefix+signature at runtime and the mixin lists both numbers.
- Mebahel claims, contracts and abandons become Reputation deeds. The giver is named as witness, since `quest_abandoned` is refused when unwitnessed.

**Why:** Mebahel is a Fabric jar loaded through Connector. Compile against the Mojang-mapped copy in the instance's `.cache/connector/`, plus the FFAPI nested jars.

**How to apply:**
- Test server: `test-server/Q-questbridge` (port 25598, minimal mods).
- Harness: `mca-rpg-bridge/tester`. `/bridgetest` gave 14/14 PASS. The tester is test-only and never goes in the pack.
- The client button is untested in game.
- If Quests or Mebahel update, re-check the version gate and the lambda number. 2026-09-28: bridge **1.2.0** built for Mebahel **3.2.0** (report `modpack-audit/28-mebahel-3.2.0-bridge-update.md`). 3.2.0 moved the lambda to $3 and deleted `drawSolidBackdrop`, so 1.1.0's click guard and tracker offset both fail on it. 1.2.0 also fixes Mebahel's own MCA check, which looks for `net.mca.` screens while this pack's MCA is `net.conczin.mca.`. 1.2.0 runs with either Mebahel version; built but never run.
- Installed in the live instance (PrismLauncher `Instances/1.21.1/minecraft/mods`) on 2026-09-20 at the user's go-ahead, without further testing. Quests 1.6.5 + Mebahel 2.0.6 confirmed present, so the version gate and lambda name still match. In-game client button still unverified. Related: [[colony-expeditions-addon]] (test-server conventions).
