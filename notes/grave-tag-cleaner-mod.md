---
name: grave-tag-cleaner-mod
description: "gravetagcleaner mod strips gravestone slot-restore components that leaked onto items (PlayerRevive knockdowns); installed 26 Sep, in-game untested"
metadata:
  node_type: memory
  type: project
  originSessionId: 0dca5fdb-b131-4d91-a5dc-162fb68cda84
  modified: 2026-09-26T03:42:08.469Z
---

Source: `modpack-audit/grave-tag-cleaner`. Installed in `1.21.1 slim` mods on 2026-09-26. Server boot test passed and both components resolved. It has not been tested in-game.

The problem: Gravestone Carryables tags inventory items with `gravestonecarryables:carry_slot_data`, and Gravestone Curios Compat tags equipped curios with `baguettelib:curio_slot_data`. Both tag at HEAD of `Player.die` (baguettelib PlayerDeathEvent.Pre). The tags are only cleared when the owner empties the grave (fillPlayerInventory). The main leak is PlayerRevive: it cancels LivingDeathEvent later inside die(), so every knockdown tags the whole inventory for good. Graves broken by other players leak too, because `only_owners_can_break = false`.

The fix: strip the tags when LivingDeathEvent is cancelled (LOWEST priority, receiveCanceled). It also sweeps the player's inventory, carried stack and Curios every 20 ticks, and cleans container slots when a container is closed. This is safe because a live player's inventory never legitimately holds tagged items.

**How to apply:** if items still refuse to stack, check their components for other death mods. The fallback is `restore_vanilla_slots = false` in gravestonecarryables-common.toml.
