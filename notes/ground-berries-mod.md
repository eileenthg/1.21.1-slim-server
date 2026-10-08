---
name: ground-berries-mod
description: "groundberries mod lets Eternal Tales bushes ripen on ground via mixin random ticks; installed 8 Oct, server test queued, in-game untested"
metadata:
  node_type: memory
  type: project
  originSessionId: 93265419-c6ae-4472-8359-c88a4add0c5f
  modified: 2026-10-08T08:27:16.193Z
---

Source `modpack-audit/ground-berries`. Mixin on BlockBehaviour (isRandomlyTicking/randomTick) advances ET bushes one stage (blueberry, wolfberry, tea/chili/aquatura 1-4, flameblossom) at 1/5 chance, light >= 9, only when the block below is in `eternal_tales:plants_placement_blocks` and not in `gardeners_pots`. ET bushes never tick natively; the Gardener's Pot procedure does the growing.

Installed live in `1.21.1 slim` on 2026-10-08 at the user's request, before any test. `test-server/X-expeditions/run-groundberries.sh` waits for no other Minecraft process, then boots, grows bushes, and writes `gb-result.txt` (it was launched in the background; check that file).

**Why:** the user hates ET gardening and wanted sweet-berry-style growth.
**How to apply:** if the game fails to launch, suspect this mixin first. Friend likely needs the jar. See [[test-server-a-full-stale-mods]].
