---
name: maid-chef-mod
description: "Maid Chef (maidchef) TLM add-on — Chef task; P1 built 1 Oct, harness self-test pending; phases P2-P4 planned; user design decisions"
metadata:
  node_type: memory
  type: project
  originSessionId: 76bb3d86-d612-4532-b107-6f9e3d682b3c
  modified: 2026-09-30T22:34:32.722Z
---

`modpack-audit/maid-chef` holds mod id `maidchef`. It adds one TLM task, "Chef": she picks the highest-value food she can make, makes N of it, delivers it, then rotates to another dish. Design and task-bloat list: `modpack-audit/30-maid-chef-and-task-list.md`. How it works: `maid-chef/README.md`.

**State on 1 Oct 2026:**
- P1–P4 are done, plus TLM AI chat integration (`ChefChat`: a prompt-context "cooking" category and the tools `chef_kitchen_report` and `chef_prepare`).
- The self-test passes 21/21, including a real chat through Player2 on 127.0.0.1:4315.
- The release jar (`-Prelease`, no SelfTest/SelfTestHook) has been INSTALLED in `1.21.1 slim/mods` since 1 Oct 13:51 (it's the build that passed 21/21). The user's in-game check is pending. Youchrist needs the jar too.

P4 adds "Prepare X": `ChefData.dish`, the Held/x buttons in the Setup tab, clicking a Status line to set the dish, and target-mode requests (`targetMaxMissingKinds` 3).

P2 adds:
- Recursive chain planner (`chainDepth` 3).
- Feast serving via FeastServeStation (fake player, per-maid `serveFeasts` toggle).
- Per-step fetching; crafting pulls from the chests ("Pantry"); pots are filled over several trips (staging).
- The Status tab shows the explain output (ExplainPayloads).

P3 adds:
- Wish-mode planner and `findRequest`; `ChefRequests` SavedData holds a per-owner shared list.
- Requests tab (RequestPayloads); chat [Never ask] buttons that run `/maidchef block`.
- Filters on what she asks for: block list, the `neverRequest` config, rare/epic items, prepared food (any recipe made from food), and `onlyAskForKnownItems` (owner stats: picked up or crafted).

Harness gotchas: a stale background watcher can fire on a later boot. Maids saved by an interrupted test reload late (the test now clears them for 5 s). TLM AI: a maid's llmSite defaults to deepseek; set `getAiChatManager().llmSite = "player2"`. use NeoForge TicketController, not setChunkForced (with the latter, maids were UNLOADED_TO_CHUNK). Remove leftover saved maids in the test area. Force clear weather and add glass roofs, because snow and sandstorm red-sand piles on an FD stove make it drop its food. Recipe overrides in this pack: Extra Delight disables FD's beef stew pot recipe and the roast chicken, shepherd's pie and honey glazed ham feasts; Cultural Delights empties vanilla rabbit stew. Verify that a test dish's recipe is active before writing a test scene.

User decisions:
- Feasts: FD and Extra Delight feasts are valued as servings x the best serving (net of any food used as a container, e.g. a rice bowl). Serving out of a placed feast is planned for P2.
- Phases: P1 single-step, per-maid radius, value ranking. P2 multi-step. P3 ingredient requests plus a **shared** (not per-maid) block list, where items appear only once requested and can be unblocked. P4 "prepare X" as a toggle inside Chef, not a separate task.
- Per-maid GUI settings: radius, dishes per round, recent-list size, min nutrition, min saturation, min beneficial effects, food blacklist (TLM attack-list style).
- Recent fallback: when only recent dishes are possible, try the OLDEST first. If it fails, remove it. A success re-queues it normally.
- Taken item: she asks the player to give it back ("Master" for the owner) and aborts after `returnWaitSeconds`, then picks a new dish.

Implementation facts:
- Chef owns its station code. MSK's station code was not reused, because its furnace cooks anything in the backpack and its pot always cooks a full stack.
- Only MSK's Culinary Hub bindings are read (slot 4; INGREDIENT and OUTPUT).
- TLM's `SchedulePos.tick` pulls maids back to the global MaidWorkRange=12. `SchedulePosMixin` widens it to the Chef radius.
- TLM maid pickup respects the pickup delay.

**How to apply:** check that the user isn't playing or hosting before a harness boot ([[no-test-servers-while-hosting]]). Install through `extra-mods/` ([[test-server-a-full-stale-mods]]).
