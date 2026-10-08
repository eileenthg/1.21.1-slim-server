---
name: gentle-swings-mod
description: "Gentle Swings (modid gentleswings): per-player keybind (H) so swings pass through NPCs/animals/players; built & installed 25 Sep, in-game untested"
metadata:
  type: project
---

On 2026-09-25 the user asked for a mod that makes their swings pass through NPCs, animals and players, so they stop hurting them while mining the grass next to them. It is built at `modpack-audit/gentle-swings` and `gentleswings-1.21.1-1.0.0.jar` is installed in the slim instance.

**Design:**
- The state is a server-side player attachment that is saved and copied on death. It is synced to the client with a payload. Default key is H.
- Spared entities are every LivingEntity except `Enemy`. The server config `spareHostileMobs` (default false) spares hostile mobs too.
- On the server, AttackEntityEvent is cancelled for spared targets. LivingIncomingDamageEvent is also cancelled for direct PLAYER_ATTACK damage, which catches sweeps.
- On the client, the mixin swaps `Minecraft.hitResult` for a block-only pick, but only inside startAttack and continueAttack, so right-click on NPCs still works. It restores the real target at the HEAD of startUseItem/pickBlock and at the RETURN of handleKeybinds.
- The Minecraft mixin runs at priority 500 so it goes before Better Combat's startAttack HEAD cancel. A @Pseudo mixin filters BC's `TargetFinder.findAttackTargets`.

**Status:** a dev client booted to the title screen with no injection errors. Not yet tested in-game, and the BC mixin has not been checked against the real BC jar.

**How to apply:** if it misbehaves, first check the log for `gentleswings` mixin warnings about TargetFinder in the real pack.
