---
name: brain-task-walk-target-contention
description: A villager Brain task that requires WALK_TARGET absent will never start on an MCA guard, because guards patrol continuously.
metadata:
  type: project
---

Measured 2026-09-06 with the `mcabridge` self-test against MCA 7.7.36-beta.3.

A `Behavior<Villager>` whose constructor requires `MemoryModuleType.WALK_TARGET` to be
`VALUE_ABSENT` effectively never starts on an `mca:guard`: MCA's patrol behaviours hold a
walk target essentially all the time. Observed: a guard stood 1 block from a damaged iron
golem for 1200 ticks and did nothing.

Raising the task's priority is the wrong fix and makes it worse -- the task then starts, but
higher-priority patrol keeps overwriting `WALK_TARGET`, so the villager wanders off (measured
18 blocks) while the task waits for a proximity that never comes.

**Why:** Liberty's Villagers' original required both `LOOK_TARGET` and `WALK_TARGET` absent.
Porting that requirement verbatim onto MCA produces a task that is installed and silently
never runs -- the same failure mode as the unremapped-names bug, and just as invisible.

**How to apply:** decide movement per attempt instead. If the target is already within the
completion range, act immediately and take no walk target at all; only require `WALK_TARGET`
free when the villager actually has to walk somewhere. Keep the task at a low priority
(high number) so it does not fight patrol. See [[libertyvillagers-tasks-are-inert]].
