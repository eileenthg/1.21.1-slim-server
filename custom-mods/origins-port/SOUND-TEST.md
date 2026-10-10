# In-game test: does `apoli:replace_sound_emission` cover footstep / hurt / death?

**Why this test exists.** Seven Touhou power files depended on three Voile sound types. The sweep
found they all plausibly collapse into one Apoli type, `apoli:replace_sound_emission`. I verified as
much as source can prove:

- Apoli's `EntityPlaySoundMixin` wraps `Entity.playSound(SoundEvent,FF)` → `Level.playSound(...)`,
  and in 1.21 footsteps (`playStepSound`), hurt (`playHurtSound`) and death (`die`) all route through
  exactly that call.
- Muting is supported natively: `WeightedSound.MUTE = "minecraft:empty"`.

What source **cannot** prove is that the sounds actually come out right in a running game — player
sounds in particular can take side paths (`ServerPlayer` sending sound packets directly). Hence ears.

Takes about 5 minutes. If any step fails, tell me which one and I'll adjust the mapping.

---

## Start

The wrapper lives in the subproject, **not** the repo root — `cd` first or it will not be found:

```bash
cd C:/Users/eteil.000/Claude/origins-port/touhou-origins-neo && ./gradlew runClient
```

First launch also downloads the Minecraft client assets, so give it a few minutes before the
window appears.

Then: **Singleplayer → Create New World → Game Mode: Creative → Allow Cheats: ON → Create.**

When the origin-selection screen appears, pick anything — we override it by command.

Open chat with `T` for each command below.

---

## Test 1 — baseline (so you know what "unchanged" sounds like)

```
/origin set @s origins:origin origins:human
```

```
/damage @s 1 minecraft:generic
```

Listen: normal player "oof". Now walk a few blocks on stone or grass — normal footsteps.

**This is your control.** Everything below should sound *different* from this.

---

## Test 2 — Ghost hurt + death sound  (`apoli:replace_sound_emission`, exact + regex keys)

```
/origin set @s origins:origin touhouorigins:ghost
```

```
/damage @s 1 minecraft:generic
```

- **PASS** — a ghostly custom sound, clearly not the vanilla oof.
- **FAIL** — the same vanilla oof as Test 1.

Now the death sound (creative blocks damage, so switch first):

```
/gamemode survival
```
```
/damage @s 1000 minecraft:generic
```

- **PASS** — custom ghost death sound.
- **FAIL** — vanilla death sound.

Then respawn and `/gamemode creative`.

> Note: Ghost also has a −10 max-health penalty, so it dies easily. That penalty applying is itself
> a good sign the origin loaded.

---

## Test 3 — Oni footsteps replaced  (regex key `minecraft:block\..*\.step`)

```
/origin set @s origins:origin touhouorigins:oni
```

Walk on stone/grass (stay on the ground — no flying).

- **PASS** — heavy custom oni footstep instead of the normal step sound.
- **FAIL** — normal footsteps.

This is the single most important step: it is the only one exercising a **regex** key rather than an
exact sound id, and 3 of the 7 files rely on regex.

---

## Test 4 — Muted footsteps  (`minecraft:empty`)

```
/origin set @s origins:origin touhouorigins:satori_closed_eye
```

This power is conditional — it only silences while the toggle is active. Press the Origins
**secondary active** key (default `G`; check Options → Controls → Origins if unsure), then walk.

- **PASS** — footsteps go completely silent while active, and return when you toggle off.
- **FAIL** — footsteps audible while active.

If you cannot get the toggle to fire, say so and I will test muting through the Nue power instead —
this is the one step where the power's own condition, not the sound mapping, might be what fails.

---

## What to tell me

Just the pass/fail per test — e.g. "1 ok, 2 pass, 3 fail, 4 pass". If something failed, whether you
heard *the vanilla sound* or *no sound at all* is the useful detail: vanilla means the mapping key
did not match, silence means it matched but resolved to nothing.

Quit with `/gamemode creative` then close the window; the throwaway world lives in
`touhou-origins-neo/run/` and can be deleted freely.
