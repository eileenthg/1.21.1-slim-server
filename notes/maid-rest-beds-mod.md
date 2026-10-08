---
name: maid-rest-beds-mod
description: "maidrestbeds mod lets TLM maids sleep in ordinary beds/Comforts hammocks (bed marked occupied, half heal/favour); installed 1 Oct 2026, selftest 28/28, in-game unverified"
metadata:
  node_type: memory
  type: project
  originSessionId: e475fedf-3891-4b7a-a5ee-e84013367748
  modified: 2026-09-30T16:15:11.194Z
---

On 30 Sep–1 Oct 2026 the user asked for maids to sleep in player beds and Comforts hammocks as well as maid beds, getting less benefit than from a maid bed. They also asked that the bed or hammock be marked occupied while a maid is in it. Project: `modpack-audit/maid-rest-beds/` (README has the details). The jar `maidrestbeds-1.21.1-1.0.0.jar` was installed in `1.21.1 slim/mods` on 1 Oct. `/maidrestbeds selftest` passed 28/28 on X-expeditions. Not yet seen in-game: pathing at night, the render pose in a hammock, a real player clicking the bed.

Pack facts found along the way:
- **Vanilla Companion** (`touhou_little_maid_vanilla_companion`) redirects TLM `Type` constants: Sleep is 18 points (TLM default 2), Death is -32, and so on. It also overwrites the health-per-level values. So favourability shares are scaled from `Type.SLEEP.getPoint()`, not hard-coded.
- **Love & Loathe** has `MaidBedCapacityMixin`, which cancels `MaidBedTask.findBed` at HEAD for multi-maid beds. The mod wraps the *call* to findBed instead and uses `FacilityCapacityManager.hasVacancy`.
- **Maid Spell** cancels any direct `setHealth` decrease on an EntityMaid. Tests have to use `hurt()`.
- Server-side only (no synced POI registry, common config, no packets). The friend does not need the jar.

**How to apply:** if TLM, L&L or Vanilla Companion updates, rerun the selftest. See [[love-loathe-trader-pull]] for the L&L jar in use and [[test-server-a-full-stale-mods]] for the harness.
