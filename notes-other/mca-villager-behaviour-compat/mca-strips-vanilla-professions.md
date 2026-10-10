---
name: mca-strips-vanilla-professions
description: MCA's LoseUnimportantJobTask strips a vanilla profession every tick from a villager with no job site, no trade XP and level <= 1.
metadata:
  type: project
---

Read from `net.conczin.mca.entity.ai.brain.tasks.LoseUnimportantJobTask` bytecode
(MCA 7.7.36-beta.3). It sets the profession to `NONE` when **all** of these hold:

- `MemoryModuleType.JOB_SITE` is absent
- the profession is not in `ProfessionsMCA.IS_IMPORTANT` (i.e. it is a vanilla one)
- profession is neither `NONE` nor `NITWIT`
- `getVillagerXp() == 0`
- villager level <= 1

**Why:** this makes a freshly spawned MCA villager shed `minecraft:butcher` or
`minecraft:fisherman` on every single tick, which silently breaks anything keyed on the
profession. It cost several confused debugging runs -- attachment kept happening while the
profession read `minecraft:none`. In a real village, villagers hold a job site or have traded,
so vanilla professions do stick and profession-keyed behaviour is worth building.

**How to apply:** when testing profession-keyed behaviour on MCA villagers, give the test
villager `setVillagerXp(1)` (or a real job site) -- do not re-apply the profession every tick,
because that calls `refreshBrain` and destroys the running task before it can tick. See
[[brain-task-walk-target-contention]] for the other silent-failure trap in this area.
