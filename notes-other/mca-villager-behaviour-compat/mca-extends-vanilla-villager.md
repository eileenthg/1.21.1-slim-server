---
name: mca-extends-vanilla-villager
description: MCA 7.7.36-beta.3 villagers DO extend vanilla Villager, so Liberty's Villagers already reaches them — contradicting the project brief.
metadata:
  type: project
---

Verified 2026-09-06 with `javap` against `mca-neoforge-7.7.36-beta.3+1.21.1.jar`:
`net.conczin.mca.entity.VillagerEntityMCA extends net.minecraft.world.entity.npc.Villager`,
and `VillagerTasksMCA.getWorkPackage` merely re-wraps the result of vanilla
`VillagerGoalPackages.getWorkPackage`. Liberty's Villagers replaces that vanilla method
outright (`@Inject` at HEAD, cancellable), so its `HealGolemTask` and
`ThrowRegenPotionAtTask` are already on adult MCA armorers/clerics — gated on
`MemoryModuleType.JOB_SITE` being present.

**Why:** the checked-in `CLAUDE.md` brief asserts the opposite and scopes priority-1 work
around it. Building that work as written would double the behaviours, not add them.

**How to apply:** before writing any bridge code, run the protocol in `DIAGNOSIS.md` to
find out whether MCA villagers actually hold `JOB_SITE`. The one gap needing no test:
MCA's own professions (guard, archer, outlaw, adventurer, mercenary, cultist) route
through `getGuardWorkPackage()`/`getMercenaryPackage()` and never call
`VillagerGoalPackages`, so no Liberty's Villagers behaviour reaches them.
