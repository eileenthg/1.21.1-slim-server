---
name: libertyvillagers-tasks-are-inert
description: Liberty's Villagers 1.0.2 NeoForge ships six Behavior subclasses with unremapped Yarn method names, so they override nothing and never run.
metadata:
  type: project
---

Verified 2026-09-06 with `javap -v` on `libertyvillagers-1.21.1-NeoForge-1.0.2.jar`.
`HealTargetTask`, `HealGolemTask`, `ThrowRegenPotionAtTask`, `FeedTargetTask`,
`GoFishingTask` and `FisherWorkTask` declare `shouldRun` / `run` / `keepRunning` /
`finishRunning` / `shouldKeepRunning` — Yarn names. `net.minecraft.world.entity.ai.behavior.Behavior`
declares `checkExtraStartConditions` / `start` / `tick` / `stop` / `canStillUse`. The LV classes
have **zero** override methods and **zero** `ACC_BRIDGE` methods; MCA's `GreetPlayerTask` has both.

Consequence: each task starts (default `checkExtraStartConditions` returns true), does nothing
(`start`/`tick` are inherited no-ops), and stops immediately (`canStillUse` defaults false).
So `armorerHealsGolems`, `clericThrowsPotionsAtVillagers`, `clericThrowsPotionsAtPlayers`,
`fishermanFish` and every `*Feed*` key are dead — for vanilla villagers as much as MCA ones.
LV's *mixins* are unaffected and do work; only its own `tasks/` package is broken.

**Why:** this is the actual root cause of "MCA villagers don't heal golems / throw potions",
not anything about MCA. See also [[mca-extends-vanilla-villager]].

**How to apply:** don't try to bridge these LV features onto MCA — reimplement them. Any port
must override the Mojmap names; check the result with `javap -v` and confirm `ACC_BRIDGE`
methods exist. `mcabridge` in this repo does exactly that.
