# Rendering test round — 3 behaviours, none previously verified

These are client-only mixins, so nothing about them could be checked by a headless server run — this
is their first real test. Roughly 5 minutes.

Setup: **Creative, cheats on**. Open chat with `T`.

---

## 1. Model flip — `amanojaku/this_side_down`

```
/origin set @s origins:origin touhouorigins:amanojaku
```

Look at yourself in **third person** (`F5`).

- **PASS** — your model renders completely upside-down (the Dinnerbone effect).
- **FAIL** — normal orientation.

> Your own view/camera should stay **upright** for this one — only the model is flipped, so this is
> what other players would see of you.

## 2. Zombie arms — `jiang_shi/zombie_arms`

```
/origin set @s origins:origin touhouorigins:jiang_shi
```

Third person (`F5`), then walk around.

- **PASS** — arms held straight out in front of you (like a classic zombie), with a slight sway as you
  walk rather than being perfectly rigid.
- **FAIL** — normal arm-swing animation.

Try holding still, then walking, then swimming — the pose should drop out while swimming (and while
using an item or flying with an elytra, if you have a way to test those).

## 3. Turnabout — model flip + camera flip + reversed gravity, all together

This is the big one, and the least certain — the camera-flip code was rewritten for 1.21 with no
direct precedent to copy, so this is the first time it's ever run.

```
/origin set @s origins:origin touhouorigins:amanojaku
```

Use the **turnabout** power on something (a mob, or if it can target you in some way — check what the
power actually does; it's meant to flip a *victim* upside-down for ~10 seconds). If you can't easily
trigger it on a target, the important case is if it ever applies to **you** — e.g. testing it back on
yourself if the power allows self-targeting, or having something else use it on you.

For whoever/whatever is flipped:
- **PASS** — model renders upside down, gravity is reversed (they fall/float upward), and if it's
  you: your own view/camera is also flipped upside-down, then goes back to normal after ~10 seconds.
- **FAIL possibilities to describe precisely if it's wrong:**
  - view doesn't flip at all (camera hook didn't fire)
  - view flips but rolls the *wrong way* or the world looks *mirrored* rather than upside-down
    (this would mean the matrix multiply order is backwards — a quick fix)
  - camera gets stuck flipped after the power should have expired

---

## Known-not-working — please ignore

- **Smite vulnerability** on jiang_shi — deferred, not implemented.
- The `is_closed_eye` predicate and vengeful spirit's Searing Rage calculation — still outstanding
  from Phase 3, unrelated to this round.

## Also worth a quick re-check if you have time (fixed since your last report, not yet confirmed)

- **Snow layers / carpets** as oni — should now clank (they use a different vanilla method than solid
  blocks, which was the bug).
- **Footsteps** generally — should be a single sound now, not doubled/offset.
- **Taming** as amanojaku — a wolf/cat/parrot should now show **smoke**, not hearts, when taming fails.
