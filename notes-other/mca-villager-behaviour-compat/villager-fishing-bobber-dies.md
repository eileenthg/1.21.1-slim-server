---
name: villager-fishing-bobber-dies
description: A villager-owned FishingHook is discarded after exactly one tick; Liberty's Villagers' bobber patch does not actually prevent it.
metadata:
  type: project
---

Measured 2026-09-06 with the `mcabridge` `reel` self-test, reproducible on a fresh world:
`longestBobberLife=1t`, `everBiting=false`.

Vanilla `FishingHook.tick()` calls `getPlayerOwner()` and calls `discard()` when it is null,
which it always is when the owner is a `Villager`. Liberty's Villagers' `FishingBobberEntityMixin`
adds a `removeIfInvalidOwner()` that is meant to allow a villager owner holding a fishing rod,
but the bobber still dies immediately. Since LV's own `GoFishingTask` never ran (unremapped
method names, see [[libertyvillagers-tasks-are-inert]]), its bobber patch was presumably never
exercised either.

**Why:** casting a line works and looks right, so villager fishing appears functional until you
check whether anything is ever caught. Nothing is.

**Fixed in this repo** by `dev.eteil.mcabridge.mixin.FishingHookMixin`: substitute a NeoForge
`FakePlayer` for the null local in `tick()` (it is read only in that one guard) and cancel
`shouldStopFishing`, both only when `getOwner() instanceof Villager`. Bobber lifetime went from
1 tick to indefinite; it now flies to water and bobs.

**Resolved 2026-09-07.** Villager fishing now works end to end -- cast, bite, catch into the
villager's inventory. The apparent "bobber freezes at tickCount 297" was not a bug at all: see
[[headless-server-stops-ticking-entities]].

**How to apply:** do not route the catch through vanilla `retrieve` -- it casts the owner to
`ServerPlayer` for advancements, stats and luck. Roll `BuiltInLootTables.FISHING` directly.
Also note `FishingHook.DATA_HOOKED_ENTITY` (Integer) is declared immediately before
`DATA_BITING` (Boolean); selecting the bite tracker reflectively by type order picks the wrong one.
