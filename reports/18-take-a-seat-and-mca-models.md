# 18 — Why Take a Seat's animation never played, and the compat mod that fixes it

Date: 18 Sep 2026. Status: **fixed and installed** (`mcaanimcompat-1.21.1-1.0.0.jar`).

## The bug

Take a Seat ships its sit pose as a Player Animation Library (PAL) animation
(`assets/takeaseat/player_animations/buttsit.json`). PAL applies animations by hooking
`PlayerModel`'s compiler-generated bridge method `setupAnim(Entity, float…)`: it resets the pose at HEAD
and writes the animation into the model's parts at RETURN.

MCA replaces the player renderer's model whenever the body type is not VANILLA
(`MixinPlayerRenderer.mca$selectModel` swaps `this.model` for a `PlayerEntityExtendedModel`, used by both
the PLAYER and VILLAGER body types). That class:

- declares its **own** `setupAnim(T, float…)`, and therefore gets its **own** bridge method, and
- calls `super.setupAnim(villager, …)` — the *real* method on `PlayerModel`, not the bridge.

Virtual dispatch from the renderer therefore lands on MCA's bridge and never touches `PlayerModel`'s, so
neither of PAL's injections runs. The animation plays as far as PAL is concerned — its manager reports
`isActive = true` — but nothing ever writes it into the model that is drawn. This is not specific to Take
a Seat: **no PAL animation of any kind shows on a non-vanilla MCA body type.**

An earlier guess — that MCA's layers drop part *scale* when copying the parent pose — was wrong.
`HumanoidModel.copyPropertiesTo` calls `ModelPart.copyFrom`, which copies scale, rotation and position.
That mixin was removed.

## Evidence

Automated two-pass test, same world copy, player on dry ground, values read from the model actually being
rendered on the last frame:

| Body type | Run | `body.xRot` | `leftLeg.xRot` | `rightArm.xRot` |
|---|---|---|---|---|
| VANILLA | base | 0.32 | 1.57 | −2.02 |
| PLAYER | base | 0.00 | −0.00 | −0.33 |
| VILLAGER | base | 0.00 | −0.00 | −0.36 |
| VANILLA | compat | 0.32 | 1.57 | −2.02 |
| PLAYER | compat | 0.22 | −0.61 | −1.65 |
| VILLAGER | compat | 0.32 | 1.57 | −2.02 |

In the base run the two MCA body types while "sitting" are numerically identical to standing. With the
compat mod the villager body reproduces the vanilla sit pose exactly, and the vanilla body is unchanged —
no regression. Screenshots `animtest-base-VILLAGER-sit.png` (standing upright) and
`animtest-compat-VILLAGER-sit.png` (seated) show the same thing.

## The fix

`C:\Users\eteil.000\Claude\mca-anim-compat` → `mcaanimcompat-1.21.1-1.0.0.jar`, client-only, MCA required,
Romantic Expansion and PAL optional (each mixin is gated on the mod it bridges, via `CompatMixinPlugin`).

- **`McaPlayerModelAnimMixin`** — repeats PAL's own logic on `PlayerEntityExtendedModel`: reset the twelve
  standard parts at `setupAnim` HEAD, apply the running animation at RETURN. Wear parts need no handling
  (MCA re-syncs sleeves/pants/jacket from the animated parts while rendering) and the breast parts are
  drawn inside the body part's transform, so they follow automatically.
- **`PlayerModelCarryMixin`** — re-applies Princess Carry's arm pose at `PlayerModel.setupAnim` RETURN.
  Romantic Expansion sets the arms inside `HumanoidModel.setupAnim`, but Not Enough Animations smooths the
  arms afterwards, inside `PlayerModel.setupAnim`, and overwrites them. Also straightens the carried
  player's legs and stops their head turning with the camera.

## Still open

- **Princess Carry's body angle.** `PlayerCarryPoseRendererMixin` rotates the carried player by the
  *carrier's* yaw at `PlayerRenderer.render` HEAD, but vanilla then applies the carried player's *own*
  body yaw inside that transform. The two stack, so the carried body swings out of alignment whenever the
  two players face different ways. A fix belongs at `LivingEntityRenderer.setupRotations`, cancelling the
  passenger's own yaw. Not attempted yet: it needs a real two-player test, which the automated harness
  cannot do (it fakes the carry state client-side with a dummy UUID, which takes the code's
  carrier-not-found fallback).
- The carry half of the mod is **unverified in play** — only the animation half is proven.
- An emote beats the carry pose when both apply, on every body type. Consistent, but if you would rather
  the carry pose win while being carried, that is a one-line ordering change.

## The sinking legs, and how Take a Seat actually behaves

Tested again on a 17×17 smooth stone arena at y=201, every reading confirmed with `y=201.00
standingOn=Smooth Stone`. The vanilla body and both MCA bodies sink by the same amount, so this is the
animation's own geometry, not MCA's body scaling and not anything the compat mod introduced.

The arithmetic matches the picture. In PAL's bone naming the animation's `body` bone is the whole-model
root, and `buttsit` moves it from 0 to −12 px, i.e. 0.75 of a block, while the legs fold 90° forward
(`leftLeg.xRot = 1.57`). A vanilla leg is 12 px long, so the hip lands exactly on the floor and the folded
legs then occupy the ~4 px below it. About a quarter of a block of clipping is inherent to the pose on
flat ground; it would look correct on a block edge. Lifting the model ~0.25 blocks while a ground sit is
playing would seat it cleanly — a cosmetic change, not yet made.

Two behaviours of the mod worth knowing, from its own log lines:

- The sit key does **not** toggle. Each press starts a sit again and advances to the next ground variant,
  cycling `kneesitting → buttsit → buttsit2 → kneeleaning`. Standing up happens when you move.
- All four variants resolve (`triggerOk=true`) and give visibly different poses, despite the jar shipping
  only `buttsit.json` — the rest come from elsewhere in the pack's resources.

This is why the `-stand` screenshots after the first body type are really "seated in the previous
variant": the tester's second key press re-sits rather than standing up.

## Test harness notes

- `test-client/run-animtest.ps1` runs both passes unattended: refuses to start if a Minecraft client is
  running (matched on command line — `crash_assistant` rewrites the process command line, so a memory/path
  check does not work), clears that tag's screenshots, retries once if a pass produces none, and leaves the
  mods folder as it found it.
- The tester teleports to a platform it builds 25 blocks above world spawn: the spawn point itself is in
  water, where Take a Seat will not sit and the legs are hidden.
- The tester's second sit key press does not stand the player back up, so the `-stand` shots after the
  first model are really "still seated". Harmless for the comparison, worth knowing when reading them.

## Update 25 Sep 2026: no forced third person, and sit height scales with MCA body height (v1.1.0)

**Third person.** Take a Seat already has a switch for this: `enableThirdPersonOnSit` in
`config/TakeASeatConfig.json`. It defaults to `true`, and it is now set to `false` (backup at
`TakeASeatConfig.json.bak-2026-09-25`). The mod reads the file only at startup, and its in-game config
screen is an empty stub, so change it by editing the JSON and restarting.

**Sinking on short MCA bodies.** PAL (and KosmX's playerAnimator, which is also in the pack) applies the
animation's root `body` bone at `PlayerRenderer.setupRotations` RETURN, in world units. For the sits that
means a −12 px drop (0.75 block) and a pivot at a fixed 0.75-block hip. MCA applies the body's genetic
height and width later, in `scale()` TAIL. So a body at, say, 0.85 height still drops a full vanilla leg
length. The earlier "all bodies sink the same" test used default genetics, which is why it didn't show up.
Beds looked fine because the lying poses rely on rotation, not a drop.

New in `mcaanimcompat` 1.1.0:
- **`McaBodyTransformScaleMixin`** (priority 500, so it runs before PAL's hook at priority 2000 and
  playerAnimator's at 1000) applies MCA's scale S just before the animation libraries' body transform, then
  S⁻¹ at `scale()` HEAD. The net effect is S·A instead of A·S: the animation runs in model units and scales
  with the body. The steps in between (render flip, vanilla 0.9375) are diagonal, so they commute. It only
  applies under MCA's own condition (`MCAClient.useGeneticsRenderer`), and S and S⁻¹ cancel when no
  animation is playing.
- **`TakeASeatCameraScaleMixin`** (only when Take a Seat is loaded) scales Take a Seat's first-person sit
  camera drop (`cameraFocusOffset`, 0.55) by eye height ÷ 1.62, because MCA scales eye height with body
  height.

Status: builds, and is installed in `1.21.1 slim` (the 1.0.0 jar was removed). **Not yet tested in game.**
The client was running at the time, so the automated harness couldn't run. `run-animtest.ps1` now points
at the `1.21.1 slim` instance.

## Update 25 Sep 2026 (later): first-person view follows the lying poses (v1.2.0)

Third person is now off (see above), so lying on a bed in first person showed the room from sitting height.
The new **`LyingCamera`** class, with config in `config/mcaanimcompat-client.toml` under `[lyingCamera]`:
- Face-up poses (`bedlyingdown`, `bedlyingdown2`) look at the ceiling. The side pose (`bedlyingdown3`) looks
  sideways, rolled 90° as you would be lying on your side. When you lie down, your look is reset to straight
  ahead of the pose, and the pose eases in.
- The camera is the head's orientation: body yaw, then 90° about the hip, then the side twist, then your own
  mouse look relative to the body. Mouse look still works, and the head turn is the same as when upright. As
  in vanilla, turning more than 50° swings the body round, on the bed too. That was kept on purpose because
  the user likes it; an earlier clamp was removed.
- The eye moves down and back to where the lying model's head is. Sizes follow eye height, so MCA body height
  is respected. Take a Seat's sit drop fades out while this applies. First-person hands are hidden while lying.
- Only the camera turns. The crosshair still targets along the player's real look direction.
- Settings: `enabled`, `moveToHead`, `hideHands`, `backPoses`, `sidePoses`, and `sideTwistDegrees` (−90,
  which matches the pose, since it rolls onto its right side).

**Test.** `run-animtest.ps1 -Bed` (tester bed mode). The test machine ran at about 1 FPS, so the server-side
teleport to the test room landed after the shots, and the test actually ran on the bed in the user's house.
Camera vectors from the log: in both back poses the view looks straight up (look = +Y, and screen-up points
toward the head end). In the side pose the view is horizontal and rolled. It returns to normal after
standing up. Results were the same for the VILLAGER and VANILLA bodies. The screenshots confirmed that the
model's head lies behind the feet, face up. They also showed the side pose turns the face toward the model's
right, while the first run's +90 turned the camera left, so the default was flipped to −90 and rebuilt.
That flip has **not** been re-run.

The harness was also updated: `make_args.py` now defaults to the `1.21.1 slim` instance and NeoForge
21.1.250, it copies the `New World` save, it refuses to start if any Minecraft window is open, and it
clears every installed compat jar first so a duplicate mod id can't crash the boot.

## Update 25 Sep 2026 (evening): the lying head follows your real aim (v1.3.0)

The user asked for the auto-pivot to move the real aim too, so the crosshair and everything else match the
view. v1.3.0 replaces the camera-only approach:
- **Real aim.** Lying down sweeps the player's actual look up to the ceiling (or sideways, for the side pose)
  over 0.6 s. Standing up sweeps the pitch back to what it was before. This happens in first and third person.
- **Model head** (`LyingHeadMixin`, set after the whole setupAnim so PAL's pass is included). The look is
  re-expressed relative to the lying body, so looking at the ceiling is a resting head, and looking
  horizontally ahead tucks the chin toward the feet. The head no longer bends back over the pillow. The
  first-person camera is the same head's view, so the crosshair and the screen agree. This works for every
  player drawn, since remote players' looks are synced.
- **The neck** (the user spotted this). A yaw-then-pitch head, as vanilla uses, rolls onto its ear when you
  glance sideways near your feet (measured: 40° pitch plus 90° yaw). The head now takes the shortest swing
  from resting toward the look, with no twist, capped at `neckLimitDegrees` (90). The camera uses the same
  swing without the cap, so it stays level with the head. Test readings: at the feet, x 76 / y 20 / z 20;
  looking right from the ceiling, x 27 / y 30 / z 10. The roll is gone.
- **Side pose, 90° head turn** (`sideHeadTurnDegrees`). Vanilla's 50° head turn would swing the lying body
  40° every time you rolled onto your side. The limit is raised only in the side pose, by hooking where
  `LivingEntity.tickHeadTurn` reads it. A hook on `Player.getMaxHeadRotationRelativeToBody` was never
  reached in this pack; it's unclear why. Something else still occasionally nudges the body 40° (source not
  found; it isn't NEA, PAL or Better Combat), so while on its side the local body yaw is also held every
  tick and before drawing. It only moves when the head passes 90°. Every other pose keeps vanilla's 50°,
  spinning included, which the user wants.

Test (`run-animtest.ps1 -Bed`, 25 Sep, final build). Lie down: pitch −90, head 0/0/0. Stand up: pitch
restored (35). Side pose: the body stays 90° from the head (−50/+40 and −10/+80). No crash.

The harness now copies the world with robocopy and skips `session.lock`, because a server running from the
instance folder held it open.

## Load My Chunks crash → `lmctickfix` (25 Sep)

Two "Exception ticking world" crashes today (12:05 in the user's game, 13:23 in a bed test), both
`ArrayIndexOutOfBoundsException` in `Long2ObjectLinkedOpenHashMap$MapIterator` from
`LoadedChunkProviders$1.iterateChunks`. Load My Chunks 1.2.5.1, the latest release, replaces
ServerLevel's entity tick with a walk over ChunkMap's live `updatingChunkMap`. Ticking an entity can add or
drop chunk holders, which rehashes the map under the running iterator. New mod
`C:\Users\eteil.000\Claude\lmc-tick-fix` → `lmctickfix-1.21.1-1.0.0.jar` (installed, both sides) redirects
that `values()` call to a snapshot. C2ME is in the pack and may touch the map off-thread, so the copy is
retried, and if it never comes out clean that one tick's walk is skipped. Four bed tests since then (teleports
and chunk loads) ran without a crash, and the fix never logged its fallback warning. Not proven over long play.

## 1.4.0 — Princess Carry fixes (26 Sep 2026)

Installed as `mcaanimcompat-1.21.1-1.4.0.jar`. The mod is no longer client-only. The carry rules run on whichever side hosts the world. It has no networking of its own, but **both players need the jar**, because the sneak fix and the render fix run on the client.

- **The "spasm" of the carried player.** Romantic Expansion lays the body down at `PlayerRenderer.render` HEAD. Vanilla then turns the body by the carried player's own body yaw inside that frame, and once the body is lying down that turn becomes a roll about the spine. So every head turn rolled the body over. The body yaw also comes from the clamped (±85°) vehicle-relative formula, so the roll jerked. Fix: `PlayerRendererCarryMixin` → `CarryPose` undoes Romantic Expansion's transform and places the body from the carrier's frame. It lies across the arms, head on the carrier's right, torso lifted 15°, face up and turned 25° toward the carrier, and it ignores the carried player's own yaw. The head stays in line with the body.
- **"Stiff as a log."** 1.0.0 had straightened the legs. They now bend 30° at the hips. All pose numbers are in `mcaanimcompat-client.toml` under `[princessCarry]` and are read every frame, so they can be tuned live. `enabled=false` restores Romantic Expansion's original pose.
- **The carrier could not use anything.** The carried player's hitbox surrounds the carrier's eyes. `ProjectileUtil.getEntityHitResult` picks a box that contains the ray start at distance 0, so the crosshair always hit the carried player. `GameRendererPickMixin` removes each carry partner from the other's entity pick.
- **Teleports.** Waystones teleports a player's mount but not the player's passengers. A vanilla player teleport moves only that player. `ServerTeleportMixin` (on `ServerGamePacketListenerImpl.teleport`) plus `CarryServer` teleport the passenger after the carrier and remount them 3 ticks later, keeping the carry. Romantic Expansion ends a carry on any dimension change, so for a dimension change the pair is stashed at `EntityTravelToDimensionEvent` HIGHEST and the carry is started again 20 ticks after arrival.
- **Put down.** Romantic Expansion ended the carry on *either* player's sneak. `RomanticShiftMixin` stops that for the carrier. Sneak + right-click on a block now puts the carried player down at the clicked spot, facing the carrier, via `CarryServer` on `RightClickBlock`. If there is no room there, the normal dismount spot is used. The carried player's own sneak and the carry key (H) still work as before.

**Test:** `test-client/run-animtest.ps1 -Carry` uses a real second `ServerPlayer` (`TestPlayer`, whose packets are dropped). NeoForge's FakePlayer is no use here, because its teleport and riding are stubbed. The run passed all 8 checks: carried player follows the carrier through a same-level teleport, to the Nether, and back; carrier crosshair hits blocks; sneaking keeps carrying; carrier's own teleport there and back; sneak + right-click put-down lands on the exact spot. The head stayed still across four look directions on both the VILLAGER and VANILLA models. Screenshots are `screenshots/animtest-carry-*.png`.

**Not covered:** a real Waystones GUI teleport (the code path goes through the same `ServerPlayer.teleportTo` → `connection.teleport` hook, confirmed in decompiled Waystones 21.1.41); two real clients; the carried player's first-person camera (unchanged: it sits above the carrier's head). Romantic Expansion itself puts the carried player's *entity* in different places on the host and on other clients, because its riding-position mixin reads server-side state. The new render is placed from the carrier, so it looks the same on both.
