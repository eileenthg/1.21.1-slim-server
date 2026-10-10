---
name: headless-server-stops-ticking-entities
description: On a dedicated test server with no players, chunks stop entity-ticking after a few hundred ticks; entities stay loaded but frozen.
metadata:
  type: project
---

Found 2026-09-07 while debugging the `mcabridge` self-test. With no player connected, the test
world stops ticking entities a few hundred ticks in. They remain loaded and queryable --
`isRemoved()` is false, `getEntitiesOfClass` still returns them -- but `tickCount` stops
advancing and nothing about them changes.

This produced two convincing false failures: a fishing bobber that "died" at tickCount 297, and
a villager whose profession change never triggered a re-attach because its entity tick handler
never ran again. Both looked exactly like real bugs and cost several hours.

**Why:** a frozen entity is indistinguishable from a broken behaviour unless you check
`tickCount`. The tell is any measured value that is byte-identical across widely separated
samples.

**How to apply:** force-load the test area at server start --
`level.setChunkForced(cx, cz, true)` over a 3x3 chunk area -- before trusting any headless run
longer than ~200 ticks. When a headless result looks impossible, log `tickCount` before
investigating the logic. See [[villager-fishing-bobber-dies]].
