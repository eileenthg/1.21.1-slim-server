---
name: mca-anim-compat-mod
description: mcaanimcompat fixes PAL animations on MCA bodies, lying camera, and (1.4.0) Princess Carry render/interaction/teleport/put-down
metadata: 
  node_type: memory
  type: project
  originSessionId: 6646a658-2cdb-4a47-8810-94021639f248
  modified: 2026-09-25T02:15:17.559Z
---

Built and installed 18 Sep 2026: `mcaanimcompat-1.21.1-1.0.0.jar`, source at
`C:\Users\eteil.000\Claude\mca-anim-compat`. Full write-up in `modpack-audit/18-take-a-seat-and-mca-models.md`.

Root cause it fixes: Player Animation Library hooks `PlayerModel`'s synthetic bridge
`setupAnim(Entity,…)`. MCA's `PlayerEntityExtendedModel` (used by the PLAYER and VILLAGER body types)
declares its own `setupAnim` and calls `super.setupAnim(T,…)` — the real method, not the bridge — so PAL's
injections never run. **Every** PAL animation is silently dropped on non-vanilla MCA bodies, not just
Take a Seat. Verified by pose numbers read from the rendered model, base vs compat.

**Why it matters later:** an MCA update that changes `PlayerEntityExtendedModel.setupAnim`, or a PAL
update that moves its hook, breaks this mod. Re-check both after either updates.

**How to apply:** the animation half is proven in game. The Princess Carry half is *not* — it needs a
two-player test, because the automated harness fakes carry state with a dummy UUID and so exercises the
mod's carrier-not-found fallback. A known, unfixed defect: Romantic Expansion rotates the carried player
by the carrier's yaw at `PlayerRenderer.render` HEAD while vanilla still applies the passenger's own yaw
inside it, so the two stack.

**1.1.0 (25 Sep 2026)** added two things. McaBodyTransformScaleMixin conjugates the PAL/playerAnimator root-body transform by MCA's genetic scale, so the sit drop is proportional to body height. TakeASeatCameraScaleMixin scales Take a Seat's sit camera drop by eye height ÷ 1.62. This one is built and installed but **untested in game**. Also that day: Take a Seat's `enableThirdPersonOnSit` was set to false in its JSON config.

**1.2.0 (25 Sep 2026)**: the LyingCamera feature. In first person, the view follows Take a Seat's bed poses: back poses look up, the side pose is rolled sideways, and the eye moves to the head. Config is in mcaanimcompat-client.toml. The user wants the body to be able to spin on the bed (vanilla's 50° head turn, then the body follows), so don't clamp it. Bed-mode harness test passed apart from the sign of the side twist. That default was flipped to −90 afterwards and hasn't been re-run.

**1.3.0 (25 Sep 2026, evening)**: the lying head is driven by the real aim. The auto-pivot sweeps the real look, the model head is a no-twist swing relative to the lying body (capped by neckLimitDegrees), and the camera is that head's view. The side pose allows a 90° head turn: it's hooked at LivingEntity.tickHeadTurn's limit read (a hook on Player.getMaxHeadRotationRelativeToBody was never reached here), plus a local body-yaw hold because some unknown code still nudges the body. Harness-verified; the user hasn't played it yet.

**1.4.0 (26 Sep 2026)**: Princess Carry overhaul. This fixes the known defect above (yaw stacking = the user's "spasm"). The mod is now both-sided: `@Mod` has no dist, and there is a common mixin (ServerTeleportMixin) plus CarryServer. Both players need the jar. It also restores a leg bend (the user disliked the "log"), lets the carrier use blocks (a pick-filter fix), makes teleports carry the passenger (waystones/any/cross-dim), and stops the carrier's sneak from dropping; sneak+right-click on a block puts the passenger down. Harness `run-animtest.ps1 -Carry` with a real second ServerPlayer (anim-tester TestPlayer; needs an EmbeddedChannel or NeoForge NPEs and crashes the server): 8/8 PASS. The user's in-game two-player test is still pending. Installed. Details: `18-take-a-seat-and-mca-models.md` § 1.4.0. The friend needs the new jar (see [[friend-update-zips]]).

Test harness: `modpack-audit/test-client/run-animtest.ps1` (`-Bed` for the lying camera) (see [[item-merge-pipeline]] for the other
verification loop).
