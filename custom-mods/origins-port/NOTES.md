# NOTES.md — running findings log

Evidence log for the Touhou Origins → Overgrown's Origins port.
Rule from CLAUDE.md: **if the source disagrees with CLAUDE.md, the source wins and it gets recorded here.**

Every claim below is measured from `reference/`, not recalled. Commands used are shown so they can be re-run.

---

## Phase 0 — bootstrap (2026-09-02) — COMPLETE

### Environment

| Thing | Value |
|---|---|
| Project root | `C:\Users\eteil.000\Claude\origins-port` |
| git | 2.39.0.windows.1 |
| java | OpenJDK 21.0.7 LTS (Microsoft), via PrismLauncher runtime |
| Gradle | not run yet, per CLAUDE.md Phase 0 |
| Root is a git repo | **No** — not initialised. `.gitignore` written but currently inert. |

Java 21 is the right major version for NeoForge 1.21.1. It resolves from a PrismLauncher-bundled
runtime rather than a system JDK install, so `JAVA_HOME` may need setting before Gradle runs in Phase 2.

### Clones — all six landed on the intended branch

```
reference/og-apoli/              NeoForge-1.21.1 @ 7570faf
reference/og-apoli-fabric-1201/  Fabric-1.20.1   @ bded306
reference/og-origins/            NeoForge-1.21.1 @ a691f76
reference/origins-fabric/        1.20            @ 80bf805
reference/touhou-origins/        master          @ 22a1cb4
reference/voile/                 master          @ 7af4218
```

Note: `origins-fabric` default branch is `1.20`, not `master`. Harmless — it is reference only.

### Version facts confirmed against `gradle.properties`

| Repo | mod_id | version | MC | Loader |
|---|---|---|---|---|
| og-apoli | `apoli` | 1.56.0 | 1.21.1 | neo 21.1.248 |
| og-origins | `origins` | 1.27.0 | 1.21.1 | neo 21.1.248 |
| touhou-origins | `touhouorigins` | 2.0.2 | 1.20.2 | fabric 0.15.6 |
| voile | `voile` | 1.1.0 | 1.20.2 | fabric 0.15.6 |

Both target mod ids match upstream, so the "no translation layer" premise holds at the namespace level.
Extra detail not in CLAUDE.md: og-origins `mod_group_id=dev.overgrown.origins`, and both Overgrown repos
declare `mod_license=The A/O License`. Apoli's Java package root is `dev.overgrown.apoli`.

Touhou pins `origins_version=1.12.3` and `voile_version=1.1.0`; Voile pins `apoli_version=2.11.4`,
`pehkui_version=3.7.11`, plus `impersonate` and `fpa` — two dependencies CLAUDE.md does not mention.
Worth checking in Phase 4 whether the disguise powers pull in Impersonate.

### Content inventory — matches CLAUDE.md exactly

9 Java files, 19 origins, 99 power files, 1 origin layer. All nine Java paths are as CLAUDE.md lists them.

### Compatibility re-measurement — CONFIRMED

```bash
grep -rhoE 'Apoli\.id\("[a-z0-9_./-]+"\)' reference/og-apoli/src | sed -E 's/Apoli\.id\("(.*)"\)/\1/' | sort -u
grep -rhoE '"apoli:[a-z0-9_./-]+"' reference/touhou-origins/src/main/resources/data | tr -d '"' | sed 's/^apoli://' | sort -u
```

- Apoli ids registered: **549** (CLAUDE.md says 550 — off by one, almost certainly a grep-pattern
  difference, not a real discrepancy. Not worth chasing.)
- Touhou `apoli:` types required: **149**
- **Missing: 0. Coverage 149/149.** The decisive number in CLAUDE.md is correct.

### Voile surface — 30 types, confirmed

A naive `"voile:*"` grep returns **31** hits, but one is `voile:textures/gui/resource_bar.png` —
an asset path, not a power type. Real count is **30**, matching CLAUDE.md.

The 4 same-named types in Overgrown's Apoli are confirmed present by name:
`disguise`, `disguised`, `remove_disguise`, `water_breathing`.
**Field schemas still not compared** — this remains name-match-only, as CLAUDE.md warns.

### `origins:` types — PRELIMINARY, needs the real Phase 1 spike

Same asset-path caveat: the raw grep returns 12, of which 6 are `origins:textures/...` sprite paths.
The 6 real types match CLAUDE.md's list. Quick locate pass in `reference/og-origins/`:

| Type | Found | Where |
|---|---|---|
| `hurt_by_water` | yes | `data/origins/damage_type/hurt_by_water.json` |
| `meat` | yes | `data/origins/tags/item/meat.json` |
| `origin` | yes | `data/origins/origin_layers/origin.json` |
| `toggle` | yes | used in `data/origins/powers/arachnid/climbing.json` |
| `chose_origin` | **not found** | — |
| `water_protection` | **not found** | — |

`chose_origin` is interesting: Overgrown's Origins registers `choose_origin` (different spelling).
Could be a rename, could be a different concept, could be that Touhou's `chose_origin` is a *condition*
registered somewhere my locate pass did not look. **Do not treat this as a failure yet** — it is a
locate pass, not the spike. Resolve properly in Phase 1 item 3.

---

## Open items carried into Phase 1

1. **Scale question (BLOCKING both deliverables)** — are `set_size` / `entity_set_size` Pehkui-backed?
   Not investigated yet.
2. **Mixin spike** — do `OrbOfOriginItem` and `OriginCommand.openLayerScreen` exist in og-origins,
   under Mojmap names? Not investigated yet.
3. **`origins:` type spike** — finish `chose_origin` and `water_protection` above.
4. **Player Scale datapack zip** — no upstream repo exists; needs to come from Eileen or Modrinth.
   Deliverable 1 is blocked on this input.
5. **Licensing** — A/O License text not yet read. CLAUDE.md requires reading
   `https://0vergrown.github.io/Handbook/license` before writing redistributable code, and clearing the
   A/O vs GPL-3.0 interaction with Eileen before any public release.
6. **Impersonate / fpa** — Voile dependencies not mentioned in CLAUDE.md. Check relevance in Phase 4.

---

## Phase 1 — spikes (2026-09-02) — COMPLETE

### Repo initialised

`git init` on `master`. `reference/` and `scratch/` correctly ignored — `git status` shows only
`CLAUDE.md`, `NOTES.md`, `.gitignore` as untracked. Nothing committed yet.

---

### SPIKE 1 — scale. **CLAUDE.md's framing was wrong.**

**`set_size` / `entity_set_size` have nothing to do with entity scale.**

`EntitySetSizeCondition.java` is a *condition* (not a power) measuring the **cardinality of an
"entity set"** — the number of entities tracked by `EntitySetPower`:

```java
public record Cfg(ResourceLocation set, Comparison comparison, int compareTo) {}
int size = EntitySetPower.size(ctx.entity(), cfg.set);
return cfg.comparison.compare(size, cfg.compareTo);
```

`entity_set_size` is the registered id; `set_size` is a type alias for it. The name collision with
"size" is coincidental. **Open Question 1 in CLAUDE.md rests on a false premise and should be retired.**

**Overgrown's Apoli has zero Pehkui references** (`grep -rli pehkui` over the whole repo = 0 files)
and **no entity-scaling power of any kind**. No registered id matches scale/size/width/height except
`fluid_height`, `height` (conditions) and the entity-set pair above. The `scale` string appears only
in rendering internals (Bedrock animation, HUD overlay, raycast).

**The viable path is the vanilla 1.21 scale attribute, not Pehkui.** `AttributeModifier` takes
`attribute` as a free-form `ResourceLocation`, resolved generically at runtime:

```java
Holder<Attribute> attr = BuiltInRegistries.ATTRIBUTE.getHolder(attrId.get()).orElse(null);
```

so any vanilla attribute id works via `apoli:attribute`. og-origins' own data already uses
`minecraft:player.block_interaction_range` and `minecraft:player.entity_interaction_range` — both
1.20.5+ additions — which confirms this is the post-1.20.5 attribute set that also contains
`minecraft:generic.scale`. **Not yet confirmed against a real 1.21.1 registry** — confirm at first
launch in Phase 2. (1.21.1 predates the 1.21.2 `generic.` prefix drop, so the id is
`minecraft:generic.scale`, matching the `generic.armor` style already in og-origins data.)

**What Touhou actually asks for — all 6 `voile:modify_scale` usages:**

| Power | scale_types | scale | Migration |
|---|---|---|---|
| earth_rabbit/feeble_nature | *(default)* | 0.85 | `generic.scale` |
| fairy/fodder | *(default)* | 0.75 | `generic.scale` |
| kappa/short_stature | *(default)* | 0.9 | `generic.scale` |
| oni/brawn_of_the_oni | *(default)* | 1.1 | `generic.scale` |
| wolf_tengu/mountainous_blades | `pehkui:reach` | 1.33 | the two `interaction_range` attributes |
| bake_danuki/smelly | `pehkui:visibility` | 2 | **no equivalent — gap** |

Voile's default is `WIDTH, HEIGHT, DROPS, VISIBILITY` (`ModifyScalePower.java:41-46`). Vanilla
`generic.scale` covers width+height; it does **not** cover DROPS (cosmetic, ignorable) or VISIBILITY.

**The one real gap is `bake_danuki/smelly`** — `pehkui:visibility` at 2x makes the player *twice as
noticeable to mobs*. Apoli has `invisibility`/`invisible` but those are binary, not scalar. Options:
accept the loss, approximate with a custom power, or keep Pehkui just for this. Needs Eileen's call.

Net: **Pehkui is not required for Touhou Origins.** 5 of 6 scale usages map onto vanilla attributes.

---

### SPIKE 2 — mixins. **One target exists, one does not. A better single target exists for both.**

Both Touhou mixins have **byte-for-byte identical bodies** — fetch `ActionOnOriginGuiPower`s and call
`onGuiOpen()`. Two injection sites, one behaviour: *"when the origin selection GUI opens, run the
power's actions."*

| Touhou target (Fabric/Yarn) | Status in og-origins |
|---|---|
| `io.github.apace100.origins.content.OrbOfOriginItem#use` | **exists**, moved to `dev.overgrown.origins.item.OrbOfOriginItem` |
| `io.github.apace100.origins.command.OriginCommand#openLayerScreen` | **GONE.** Class is `OriginCommands` (plural); no `openLayerScreen` method exists. |

**Recommended replacement — one mixin instead of two.** Every path that opens the choose screen
funnels through a single server-side method:

```java
// OriginsServerNetwork.java:93
public static void openChooseScreen(ServerPlayer player, OriginLayer layer, boolean fromOrb)
```

Callers: `OrbOfOriginItem.java:52` (orb), `OriginCommands.java:339` (command),
`OriginsServerEvents.java:70` (first join / pending layer). A single `@Inject(at = @At("HEAD"))` there
reproduces both original mixins **and** additionally covers the first-join path the Fabric version
missed. Mojmap, `ServerPlayer` not `ServerPlayerEntity`, and no Yarn descriptor needed.

**The custom power is load-bearing — recommend keeping it.** `touhouorigins:action_on_origin_gui` has
exactly one use, the `unlink` sub-power of `powers/global/boundless_servitude.json`. It is not
decorative: on GUI open it empties the shikigami entity set and resets the link resource. Without it,
re-rolling an origin leaves stale entity-set links behind. 35 lines of Java. **Answers CLAUDE.md Open
Question 2: keep it.**

---

### SPIKE 3 — `origins:` types. **4 of 6 resolve. 2 are genuinely missing.**

First, a mechanism that matters more than the spike itself:

```java
// og-origins Origins.java:36
NamespaceAlias.addAlias(MOD_ID, "apoli");   // "origins" -> "apoli"
```

**Every `origins:X` power type transparently resolves to `apoli:X`.** This is why og-origins' own data
uses `origins:` for 159 of its power types and `apoli:` for only 46. Large consequence for Deliverable 1
(below). It does **not** rescue non-power registries — an `origins:` enchantment or advancement trigger
still has to exist for real.

| Type | Kind | Status |
|---|---|---|
| `origins:hurt_by_water` | damage type | OK — `data/origins/damage_type/hurt_by_water.json` |
| `origins:meat` | item tag | OK — `data/origins/tags/item/meat.json` |
| `origins:origin` | origin layer | OK — `data/origins/origin_layers/origin.json` |
| `origins:toggle` | power type | OK — resolves to `apoli:toggle` via the alias |
| `origins:chose_origin` | **advancement trigger** | **MISSING** |
| `origins:water_protection` | **enchantment** | **MISSING** |

**`chose_origin`** — upstream Fabric has `ChoseOriginCriterion` (`origins-fabric/.../ChoseOriginCriterion.java:19`).
og-origins has **zero** criterion-trigger registrations. The `Origins.id("choose_origin")` found in
Phase 0 is a **network payload id**, not a criterion — different thing, similar name. Blast radius:
1 file, `advancements/close_eye.json`. Low stakes — drop the advancement or write the trigger.

**`water_protection`** — upstream registers it in `ModEnchantments.java`. og-origins has **no
enchantment folder at all** (`data/origins/` = badges, damage_type, keybinds, origin_layers, origins,
powers, tags). Blast radius: `powers/vampire/banes_of_the_immortal.json`, used twice — as an
`apoli:enchantment` condition and as the `protection_enchantment` field of `apoli:damage_over_time`.

**Mitigation is cheap: enchantments are data-driven in 1.21.** Define
`data/touhouorigins/enchantment/water_protection.json` and repoint both references. No Java. Apoli
also registers `enchantment`, `modify_enchantment_level`, `remove_enchantment`, so the condition side
is supported.

**Consequence for the Phase 5 plan:** CLAUDE.md nominates `vampire` and `crow_tengu` as the Voile-free
control group. **`vampire` is not clean** — it depends on the missing `water_protection` enchantment.
Only `crow_tengu` is a true control. Adjust Phase 5 accordingly.

---

### DELIVERABLE 1 — Origins: Player Scale, pulled and inventoried

Pulled from Modrinth with Eileen's authorisation. `origins-player-scale`, project `9JpuntRB`,
**license GPL-3.0-or-later** (matches CLAUDE.md). No source repo published.

Latest datapack: version `3`, MC 1.20.4, `scaleorigins-f26.zip`, 10,655 bytes.
**SHA-1 verified** `f47209fdf9f28f5302336a7a9006cce05b3360cc` — matches Modrinth's manifest.
Stored at `player-scale/scaleorigins-f26.zip`, extracted to `player-scale/upstream/`.

Note: CLAUDE.md calls it a "pure datapack". Modrinth also ships it as a **mod jar** with
fabric/forge/**neoforge**/quilt loaders (1.20.4). Only the datapack variant was pulled.

Contents: namespace `prscaleorigins`, 1 origin layer, 7 origins (normal, micro, verysmall, small,
big, verybig, giant), 14 powers (`*_scale` + `*_balance` per size).

**It does not use a scale *power* at all.** It shells out to Pehkui's `/scale` command through
`origins:execute_command` at permission level 4:

```json
{"type": "origins:execute_command", "command": "scale multiply pehkui:model_height 2", "permission_level": 4}
```

Pehkui scale types driven this way: `eye_height`, `third_person`, `model_height`, `model_width`,
`hitbox_height`, `hitbox_width`, `projectiles`, `visibility`, `falling`.

**All 6 power types it uses resolve today** via the namespace alias — `action_on_callback`, `and`,
`attribute`, `execute_command`, `modify_jump`, `multiple` are all registered `apoli:` ids.
**Zero namespace rewriting needed.** Attributes used are `minecraft:generic.*`, correct for 1.21.1.

Known migration deltas:
1. `pack.mcmeta` `pack_format: 26` (1.20.2-1.20.4) → **48** for 1.21.1.
2. Pehkui must ship — **`3.8.3+1.21-neoforge` exists** (MC 1.21/1.21.1, neoforge). Confirmed on Modrinth.

**Recommendation: keep Pehkui for Player Scale.** Its scale types (`falling`, `visibility`,
`projectiles`, separate model vs hitbox) have no vanilla equivalent, and because it drives them by
command string rather than API, the commands need no porting at all. Realistic scope: a pack_format
bump and a load test.

This diverges from Touhou, which should use vanilla attributes and drop Pehkui. The two deliverables
therefore want *different* scale strategies — an argument for shipping them separately
(CLAUDE.md Open Question 3), independent of the GPL-3.0 issue.

---

## Updated open items

1. ~~Scale question~~ — **RESOLVED.** `set_size` was a red herring; no Pehkui in Apoli; use vanilla
   `generic.scale` for Touhou, keep Pehkui for Player Scale. Confirm `minecraft:generic.scale` exists
   at first 1.21.1 launch.
2. ~~Mixin spike~~ — **RESOLVED.** Collapse both mixins into one on `OriginsServerNetwork.openChooseScreen`.
3. ~~`origins:` type spike~~ — **RESOLVED.** 4/6 OK; `chose_origin` and `water_protection` missing.
4. ~~Player Scale zip~~ — **RESOLVED.** Pulled and verified.
5. **NEW — `bake_danuki/smelly` visibility scaling has no Apoli equivalent.** Needs Eileen's call:
   accept loss / custom power / retain Pehkui for this one power.
6. **NEW — `vampire` is not a valid Phase 5 control origin** (needs `water_protection`). Only
   `crow_tengu` is clean.
7. **Licensing — still open and still blocking public release.** A/O License text not yet read.
   Both Overgrown repos declare `mod_license=The A/O License`. Player Scale is GPL-3.0-or-later.
8. **Impersonate / fpa** — Voile deps absent from CLAUDE.md; check when porting the disguise powers.

---

## Decisions from Eileen (2026-09-02)

### D1 — Personal use only. Licensing unblocked.

Jars are for Eileen's own use, not redistributed. The A/O License vs GPL-3.0-or-later interaction
only binds on **distribution**, so it does not constrain this build. Voile's LGPL-3.0 likewise.

**Open item 7 is downgraded from blocking to dormant.** It reactivates the moment anything is shared
publicly. Still worth writing a NOTICE recording derivation, but it is no longer gating.

Practical consequence: we may read Voile's source freely for behavioural reference. Still preferring
fresh implementations over copied method bodies — partly hygiene, partly because our design genuinely
differs (see D3).

### D2 — Retain Pehkui.

Eileen runs both Touhou and Player Scale, so Pehkui ships regardless. `bake_danuki/smelly` keeps real
`pehkui:visibility` scaling instead of losing the effect.

This does **not** revert Spike 1's conclusion. The split stands:

| Powers | Mechanism |
|---|---|
| 4 default-scale (earth_rabbit, fairy, kappa, oni) | vanilla `minecraft:generic.scale` via `apoli:attribute` |
| wolf_tengu `pehkui:reach` | the two `interaction_range` attributes |
| **bake_danuki `pehkui:visibility`** | **Pehkui, via the D3 modifier** |

Vanilla attributes stay the default because they need no dependency and compose with Player Scale
automatically (independent systems, both multiply the render). Pehkui is used only where vanilla has
no equivalent.

### D3 — `bake_danuki/smelly` scales *relative* to the Player Scale baseline.

Eileen's call, and it fixes a real defect rather than merely refining behaviour.

**Why stock Voile is broken when both mods run.** `ModifyScalePower` is absolute and territorial:

```java
public void tick() { this.scaleTypes.forEach(this::updateScale); }

private void updateScale(ScaleType scaleType) {
    ScaleData data = scaleType.getScaleData(entity);
    if (this.isActive()) {
        if (data.getScale() != this.scale) data.setScale(this.scale);   // clobbers, every tick
    } else if (...) { data.resetScale(); }
}

public void onAdded() {   // resets EVERY other non-persistent scale type in the registry
    ScaleRegistries.SCALE_TYPES.values().stream()
        .filter(t -> !this.scaleTypes.contains(t))
        .filter(t -> !t.getScaleData(entity).shouldPersist())
        .forEach(t -> t.getScaleData(entity).resetScale());
}
```

Concretely, with Player Scale's `micro_balance` (`scale multiply pehkui:visibility .5`) plus
bake_danuki (`visibility` = 2): Voile's tick loop hard-sets visibility to 2.0, discarding the 0.5.
And `onAdded()` can wipe the model/hitbox scales Player Scale just set. They fight.

**The fix — Pehkui's ScaleModifier layer.** Pehkui already separates the two concerns:

```java
// ScaleData.getScale(float delta)
float value = getBaseScale(delta);
for (final ScaleModifier m : getBaseValueModifiers()) {
    value = m.modifyScale(this, value, delta);
}
```

- **base scale** is what `/scale multiply` writes — verified: `ScaleCommand` does
  `data.setTargetScale(operation.apply(data.getTargetScale(), scale))`, and `ScaleOperations.MULTIPLY`
  is `(curr, arg) -> curr * arg`. **Player Scale owns this layer.**
- **base value modifiers** are a composable layer applied on top. **Touhou owns this layer.**

So instead of `setScale(2.0)` we attach a `ScaleModifier` returning `value * 2.0`:

| Player Scale origin | base visibility | x our modifier | net |
|---|---|---|---|
| micro | 0.5 | x2 | **1.0** |
| normal | 1.0 | x2 | **2.0** |
| giant | (its own) | x2 | proportional |

That is precisely "relative to the initial origin scaling". Removing the power detaches the modifier
and leaves base scale untouched — Player Scale's value survives intact, which stock Voile's
`resetScale()` would have destroyed.

**Design sketch (implement in Phase 4, not before a skeleton exists):**

Register one `ScaleModifier` for the mod in `ScaleRegistries.SCALE_MODIFIERS` under a
`touhouorigins:` id — on **both** client and server, since Pehkui syncs modifiers by id.
Make it stateless and data-driven: in `modifyScale(scaleData, value, delta)`, resolve the entity
from `scaleData`, collect the active instances of our `modify_scale` power that name this scale
type, and return `value * product(factors)`. One registered modifier then serves any number of
powers at any factors, and needs no per-power instance bookkeeping.

Attachment is idempotent — `getBaseValueModifiers()` is a `SortedSet`, so re-adding is a no-op.

**Known wrinkle to handle:** `ScaleData.resetScale()` **clears all base value modifiers**
(`ScaleData.java:639`, `baseValueModifiers.clear()`), then re-adds the type defaults. Player Scale
fires `scale reset` in every `entity_action_removed`, i.e. on origin swap. So our modifier will be
detached whenever Eileen changes Player Scale origin. Mitigation: keep a light tick that re-asserts
the modifier while the power is active. Cheap, idempotent, and far less invasive than Voile's
per-tick `setScale`.

Reference for this design: `reference/pehkui/` @ `neoforge/1.21` (4a38c60) — matches the shipping
Modrinth build `3.8.3+1.21-neoforge`. Cloned in this phase; the default branch is
`fabric/1.14.4-1.x.x`, so the checkout is deliberate.

### Revised open items

5. ~~bake_danuki visibility gap~~ — **RESOLVED by D2 + D3.** Retain Pehkui; implement as a relative
   `ScaleModifier` rather than an absolute `setScale`.
7. **Licensing — dormant, not resolved.** Personal use only. Reactivates on any public release.
9. **NEW — Player Scale origin swap detaches our modifier** (`resetScale()` clears modifiers).
   Handle with a re-assert tick in the Phase 4 implementation.

---

## Phase 2 — build skeleton (2026-09-02) — IN PROGRESS

### Dependency coordinates — resolved

CLAUDE.md said to settle coordinates before scaffolding. Result: **the two dependencies resolve very
differently, and one of them is not published at all.**

**Apoli — available on Modrinth, under a slug that is not "apoli".**

The slug is **`opoli`** (project `i2NQbLvK`, license `LicenseRef-A-O-License`, loaders fabric +
neoforge). Found not by searching but from `og-origins/build.gradle`, which carries both branches:

```groovy
if (project.apoli_local.toBoolean()) {
    implementation "dev.overgrown.apoli:apoli:${apoli_maven_version}"
} else {
    implementation "maven.modrinth:opoli:${apoli_version}"
}
```

Searching Modrinth for `apoli` returns nothing; `origins` returns **apace100's** Origins
(`3BeIrqZR`), which is the wrong mod. Worth recording because the obvious search is misleading.

Coordinate: **`maven.modrinth:opoli:XFigHo5S`** = Apoli **1.56.0**, exactly the version
`reference/og-apoli` is pinned to. og-origins pins version id `6SX1Vcpj`, also a valid 1.21.1
neoforge build.

**Overgrown's Origins — not published anywhere. Must be built from source.**

Checked and came up empty on all three:
- No public Maven. `og-apoli/build.gradle` publishes only to `file://${project.projectDir}/repo`.
- No Modrinth project. Overgrown has 28 Modrinth projects; only `opoli` is the base library.
  Everything titled "Overgrown's Origins: X" is an **addon datapack for apace100's Origins**
  (verified: `fairy-origin` declares dependency `3BeIrqZR`, not Overgrown's fork).
- No GitHub releases on `0vergrown/Origins` or `0vergrown/Apoli`.

So CLAUDE.md's local-jar fallback is the only path. Building from
`build-deps/og-origins` (a copy — `reference/` stays pristine and clean per `git status`) with:

```bash
./gradlew build -Papoli_local=false
```

`-P` overrides the project property on the command line, so Apoli comes from Modrinth Maven and
**we never have to build Apoli at all** — and no file in `reference/` is edited.

**Practical consequence for Eileen:** Overgrown's Origins has no download. Running this mod in a real
instance means using the jar we build here. `build-deps/jars/` is where it lands.

**Pehkui** — `maven.modrinth:pehkui:nKHizsl6` = `3.8.3+1.21-neoforge` (MC 1.21/1.21.1).

### Toolchain

| Thing | Value |
|---|---|
| JDK for Gradle | `C:\Program Files\Java\jdk-21.0.7` |
| `JAVA_HOME` (ambient) | PrismLauncher's runtime — **does have `javac`**, so it is a JDK, not a JRE |
| Also present | jdk-18.0.1.1, **jdk-26** — must not be used; 1.21.1 wants 21 |
| Gradle | wrapper, 9.6.1 (from og-origins); og-apoli uses 9.2.1 |
| Build plugin | ModDevGradle `net.neoforged.moddev` **2.0.143**, matched to Overgrown's |

Phase 0 flagged JAVA_HOME as a likely problem. It is set to the Prism runtime, which turns out to be
a full JDK — but builds still pass `JAVA_HOME=jdk-21.0.7` explicitly, so the result does not depend
on PrismLauncher staying installed.

### Skeleton

`touhou-origins-neo/`, modelled on og-origins' build rather than a stock MDK zip, so plugin version,
Parchment mappings and NeoForge version match the mods we compile against exactly — which matters
because the Phase 4 mixin targets their internals.

```
build.gradle          settings.gradle       gradle.properties
gradlew / gradlew.bat  gradle/wrapper/
src/main/java/net/reimaden/touhouorigins/TouhouOrigins.java
src/main/templates/META-INF/neoforge.mods.toml
```

Package root `net.reimaden.touhouorigins` kept from upstream. `mod_id=touhouorigins`, version 2.0.2,
license MIT (Touhou Origins' own).

The entrypoint deliberately **registers nothing** — Phase 2 proves the dependency graph resolves and
launches before any content moves. Registration is Phase 3/4.

Useful pattern learned from `og-origins/Origins.java` for later: Apoli registration is a direct call,
not a DeferredRegister —

```java
ConditionTypes.ENTITY.register(id("origin"), new OriginCondition());
ActionTypes.BI_ENTITY.register(id("copy_origin"), new CopyOriginAction());
```

`@Mod` constructor signature is `(IEventBus modBus, ModContainer container)`.

All four dependencies are declared **hard** in `neoforge.mods.toml` (neoforge, minecraft, apoli,
origins, pehkui) so a missing one fails fast at load instead of surfacing as confusing datapack parse
errors — the mistake CLAUDE.md explicitly warns against repeating.

Pehkui is declared `required` rather than `optional` because Eileen runs Player Scale alongside this,
which needs it regardless. Flagged in the toml as worth revisiting if ever shipped standalone.

### CORRECTION — the Modrinth Apoli release is NOT usable. Build the pair from source.

The plan above ("never build Apoli at all, pull `maven.modrinth:opoli` and override
`-Papoli_local=false`") **was tried and failed.** Recording it because the failure is informative.

`./gradlew build -Papoli_local=false` on og-origins fails at `:compileJava` with ~20 errors, all of
the same shape — Origins HEAD referencing Apoli classes the published jar does not contain:

```
Origin.java:7: error: cannot find symbol
  import dev.overgrown.apoli.data.IconData;
SwapOriginScreen.java:3: error: cannot find symbol
  import dev.overgrown.apoli.client.IconRenderer;
OriginExpressions.java:4: error: cannot find symbol
  import dev.overgrown.apoli.data.expr.ExprPeer;
OriginExpressions.java:27: error: constructor ResolvedVar cannot be applied to given types;
  required: ExprVar,boolean        <- published jar
  found:    (lambda),boolean,boolean
```

Verified directly rather than inferred:

| Class | in `reference/og-apoli` source | in published `opoli` jar |
|---|---|---|
| `IconData` | yes — `data/IconData.java` | **no** |
| `IconRenderer` | yes — `client/IconRenderer.java` | **no** |
| `ExprPeer` | yes — `data/expr/ExprPeer.java` | **no** |

`unzip -l` on the cached Modrinth jar matches **0** of the three. And source's
`ExprVars.java:20` declares `record ResolvedVar(ExprVar accessor, boolean needsContainer,
boolean needsPeer)` — three components, matching what Origins HEAD calls, while the published jar's
takes two.

**Conclusion: Overgrown's Apoli source HEAD is ahead of its own published 1.56.0 jar, despite both
carrying the version string 1.56.0.** The Modrinth release is a snapshot of an older commit.
`gradle.properties` shipping `apoli_local=true` by default is the upstream author telling us this:
the two repos are developed and must be built in lockstep.

Correct sequence, which is what og-origins' own build comment says:

```bash
# 1. Apoli -> ~/.m2 as dev.overgrown.apoli:apoli:1.56.0+1.21.1-neoforge
cd build-deps/og-apoli  && ./gradlew publishToMavenLocal
# 2. Origins against it (apoli_local=true is already the default)
cd build-deps/og-origins && ./gradlew build --refresh-dependencies
```

Our own `build.gradle` was updated to match: Apoli now comes from `mavenLocal()` as
`dev.overgrown.apoli:apoli:${apoli_maven_version}`, not from Modrinth. Modrinth is kept in the
repository list for Pehkui only.

**Consequence for Eileen's actual game — this is the part that matters beyond the build.**
She cannot mix the Modrinth Apoli with the Origins jar we build. Loading Origins-from-HEAD against
Apoli 1.56.0-from-Modrinth would fail at runtime with `NoClassDefFoundError: .../IconData`.
Her instance needs **both** jars from `build-deps/jars/`, built together. Modrinth's Apoli must not
be installed alongside them.

### Process note

The first Origins build was launched as `./gradlew ... | tail -60`, which reported **exit code 0
even though Gradle failed** — the pipeline's status is `tail`'s, and the 60-line window kept only
stack frames, discarding the actual "What went wrong" block. Re-run without the pipe, capturing the
full log to `scratch/` and echoing `$?` directly, which gave exit 1 and the real errors.
Worth remembering for the remaining builds.

### Phase 2 result — compiles. Launch not yet attempted.

Working build sequence, all three green (exit codes read directly, not through a pipe):

```bash
export JAVA_HOME="C:\Program Files\Java\jdk-21.0.7"
cd build-deps/og-apoli   && ./gradlew publishToMavenLocal      # 2m44s, exit 0
cd build-deps/og-origins && ./gradlew build --refresh-dependencies   # 1m20s, exit 0
cd touhou-origins-neo    && ./gradlew build                    # 59s,   exit 0
```

Artifacts:

| Jar | Size | Notes |
|---|---|---|
| `build-deps/jars/apoli-1.56.0.jar` | 30.4 MB | contains `neoforge.mods.toml`; also in `~/.m2` as `1.56.0+1.21.1-neoforge` |
| `build-deps/jars/origins-1.27.0.jar` | 577 KB | filename matches the `flatDir` coordinate exactly |
| `touhou-origins-neo/build/libs/touhouorigins-2.0.2.jar` | 2.2 KB | skeleton, no content |

Verified the locally built Apoli jar contains the three classes the Modrinth release lacks —
`IconRenderer.class`, `IconData.class`, `ExprPeer.class` all present. That closes the loop on the
divergence diagnosis above.

`neoforge.mods.toml` expands correctly in the output jar, with all five dependencies hard-declared
(neoforge, minecraft, apoli, origins, pehkui).

**What compiling proves:** the Apoli/Origins/Pehkui APIs resolve, Parchment mappings line up, the
toolchain and ModDevGradle 2.0.143 setup are correct, and the dependency graph is coherent.

**What it does not prove:** that FML actually loads the four mods together at runtime. That needs a
run, which is deliberately not done yet:

- `runServer` requires accepting the **Minecraft EULA** (writing `eula=true`), which is Eileen's to
  accept, not mine.
- `runClient` launches a Minecraft window on her machine.

Both are hers to choose. Ask before running either.

Also still open from Spike 1: **`minecraft:generic.scale` has not been confirmed against a live
1.21.1 registry.** The first successful run is the cheapest place to settle it — with
`forge.logging.markers=REGISTRIES` already set in the run config, the attribute registry dump will
show whether the id exists under that exact name.

### Phase 2 COMPLETE — server runs, all four mods load.

Eileen authorised accepting the Minecraft EULA on 2026-09-02. `run/eula.txt` records that
authorisation in a comment. `run/` is gitignored.

`./gradlew runServer` → **`Done (1.391s)`**.

Decisive lines from `scratch/server-run.log`:

```
[ModSorter/LOADING]: Found 0 mod requirements missing (0 mandatory, 0 optional)
[touhouorigins]:     Touhou Origins (NeoForge port) loading — skeleton, no content registered yet.
[ApoliReloadListener]: [Apoli] Loaded 71 power(s).
[ApoliKeybindLoader]:  [Apoli] Loaded 12 data-driven keybind(s).
[origins]:           Loaded 11 origins.
[origins]:           Loaded 1 origin layers.
```

"0 mod requirements missing" is the line that matters — all five hard dependencies
(neoforge, minecraft, apoli, origins, pehkui) resolved. Pehkui's mixins applied and its config
generated. NeoForge in use is **21.1.248**, matching `gradle.properties`.

Exactly **one** ERROR in 1074 lines: `DedicatedServerProperties: No key layers in MapLike[{}]`.
That is self-inflicted — the `level-type=minecraft\:flat` written into `run/server.properties` to
speed up first boot, with no `generator-settings`. Vanilla falls back to a default flat world.
Unrelated to any mod; harmless; remove the line if it ever gets noisy.

Other warnings were all vanilla noise: brigadier `teleport` argument ambiguity, `.mcassetsroot`
union-schema notices, and the offline-mode banner (from `online-mode=false`, also mine).

### SPIKE 1 CLOSED — `minecraft:generic.scale` exists in 1.21.1. Confirmed, not inferred.

The one inference left dangling from Phase 1 is now settled from the actual Mojang-mapped
1.21.1 jar (`~/.gradle/caches/fabric-loom/1.21.1/neoforge/.../minecraft-merged-mojang.jar`),
by extracting `Attributes.class` and reading its constant pool. 29 attribute ids, including:

```
generic.scale                     <- the Touhou body-scale mapping
player.block_interaction_range    <- the wolf_tengu pehkui:reach mapping
player.entity_interaction_range   <-  "
```

So the Spike 1 migration table is now fully backed by evidence:

| Powers | Attribute | Status |
|---|---|---|
| earth_rabbit, fairy, kappa, oni | `minecraft:generic.scale` | **confirmed present** |
| wolf_tengu (`pehkui:reach`) | the two `*_interaction_range` ids | **confirmed present** |
| bake_danuki (`pehkui:visibility`) | none — Pehkui ScaleModifier (D3) | no vanilla equivalent, as established |

Note `generic.scale` still carries the `generic.` prefix in 1.21.1; the prefix drop is 1.21.2.
That matches the `generic.armor` style already used in og-origins' data.

### Maintenance consequence worth remembering

Because Overgrown's Origins is unpublished and its Apoli must be the matching source build, **every
upstream update means rebuilding both together**:

```bash
cd build-deps/og-apoli   && ./gradlew publishToMavenLocal
cd build-deps/og-origins && ./gradlew build --refresh-dependencies
cp build-deps/og-*/build/libs/*.jar build-deps/jars/
```

This ongoing cost was not anticipated in CLAUDE.md's plan.

### Next — Phase 3

Copy `data/` and `assets/` from `reference/touhou-origins/`, mechanical renames only, launch, and let
the datapack loader produce the real error list. Known-missing going in (from Spike 3):
`origins:chose_origin` (1 advancement) and `origins:water_protection` (vampire, fixable as a
1.21 data-driven enchantment). Everything `voile:` will fail until Phase 4 — expected, and the
failures are the inventory of what actually needs writing.

---

## Phase 3 — data migration (2026-09-02) — content loads; Voile remains

Copied `data/` (156 files) and `assets/` (25 files) from `reference/touhou-origins/` into
`touhou-origins-neo/src/main/resources/`. Mechanical work only, then launched and let the loader
produce the error list, per CLAUDE.md.

### Directory renames — 1.21 made vanilla registry dirs singular

```
advancements     -> advancement          tags/entity_types -> tags/entity_type
functions        -> function             tags/game_events  -> tags/game_event
item_modifiers   -> item_modifier        tags/items        -> tags/item
predicates       -> predicate
```

`damage_type` and `tags/damage_type` were already singular. **Mod-defined dirs keep their plural
names** — `powers`, `origins`, `origin_layers`, `global_powers` — confirmed against
`GlobalPowerLoader.java:27` (`super(GSON, "global_powers")`) and og-origins' own data tree, which
uses `tags/block` + `tags/item` alongside plural `powers`/`origins`.

### Result — it runs

`Done (1.702s)`, with Touhou's content live:

```
[Apoli]  Loaded 338 power(s)          (71 before content -> +267)
[Apoli]  Loaded 1 global power set(s)
[origins] Loaded 30 origins            (11 Overgrown + 19 Touhou)
[origins] Loaded 1 origin layers
```

Touhou's `data/origins/origin_layers/origin.json` uses `"replace": false`, same as og-origins' own,
so the two layer definitions **merge additively** rather than clobbering — 19 origins appended to
Overgrown's 10 + human.

### Errors: 61 → 51. Everything non-Voile is fixed except two that need redesign.

| Fixed | Was | Cause |
|---|---|---|
| `origins/satori.json` | `Failed to load origin ... Not a JSON object` | Overgrown's `OriginUpgrade.condition` is an **EntityCondition object**; upstream took an advancement-id string. Rewrote to `{"type":"apoli:advancement","advancement":"touhouorigins:close_eye"}` (verified `AdvancementCondition.Cfg(ResourceLocation advancement)`). Also wrapped `announcement` as `{"translate": …}` — `TextComponent.CODEC` is `ComponentSerialization.CODEC`, which would otherwise have accepted the bare key as **literal text** and displayed the raw key. |
| `advancement/close_eye.json` | `Unknown trigger_type origins:chose_origin` | Predicted by Spike 3. The advancement is granted in code by `powers/satori/rejection_of_nature.json` via `apoli:grant_advancement`, so its trigger only needs to parse. Switched to `minecraft:impossible`, the standard idiom for a code-granted advancement. This also matters because the satori upgrade above now depends on it. |
| `item_modifier/crossbow_charge{,_multishot}.json` | `Unknown loot_function_type minecraft:set_nbt` | `set_nbt` was removed in 1.20.5. Rewrote as `minecraft:set_components` with `minecraft:charged_projectiles`. |
| `tags/item/{melee_weapons,tridents}.json` | `missing following references` | `#c:spears` referenced as required but does not exist in 1.21.1. Made it optional, matching how the file already treats `#c:hammers` and `#impaled:tridents`. `tools` failed only because it referenced `melee_weapons`; it recovered for free. |
| 3 powers (fairy, kappa, nue) | `references undefined badge origins:toggle` | Overgrown has no `origins:toggle` data badge — `BadgeManager.java:91` **auto-generates** a keybind badge for any `TogglePower`. Removed the explicit reference; the badge still appears. |

All 4 `origins`-side errors and 6 of 8 `minecraft`-side errors are gone.

### Remaining — 51 errors, three groups

**1. Voile — 48 errors across 18 origins.** Expected; this is the Phase 4 inventory.

Runtime **confirms the Phase 0 static analysis**: 29 of the 30 Voile types surfaced. Only `kill` did
not, because Apoli stops at the first unknown type per file and `kill` sits behind another failure in
its own file. Failures by origin (worst first): jiang_shi, celestial, bake_danuki 5 each; wolf_tengu,
global, amanojaku 4; satori_closed_eye, ghost, common 3; vengeful_spirit, oni, oarfish, nue 2;
satori, kappa, haniwa, fairy, earth_rabbit 1.

Note `crow_tengu` and `vampire` have **zero** failures — the Voile-free control group from CLAUDE.md
holds up at runtime. (Vampire's separate `water_protection` gap is a runtime-use issue, not a load
error, so it does not appear here — still outstanding from Spike 3.)

**2. `touhouorigins:action_on_origin_gui` — 1 error.** The custom power's Java is not ported yet.
Blocks `powers/global/boundless_servitude_unlink`. Phase 4.

**3. Two that need redesign, not renaming — flagged rather than bodged:**

- `predicate/is_closed_eye.json` uses loot condition `origins:origin`. Overgrown registers `origin`
  as an **Apoli entity condition**, not a vanilla `loot_condition_type`, and neither mod registers any
  `LootItemConditionType`. Used once, by `function/recollection.mcfunction` via a `predicate=`
  selector. There is no vanilla way to ask "what origin does this player have" inside a predicate, so
  this needs a different mechanism (e.g. the origin applying a scoreboard tag the selector can test).

- `function/vengeful_spirit_calculate.mcfunction` fails on line 16:
  `Incorrect argument for command at position 9: resource <--[HERE]`. Overgrown's
  `ApoliResourceCommand` implements `get/list/set/change/has` but **not `operation`**, and `set` takes
  an integer literal, not a scoreboard value. No scoreboard bridge for resources exists either.
  Promising path: `apoli:change_resource` (`ModifyResourceAction`) takes an `AttributeModifier` whose
  `value` is an `Expression`, so the max-health-minus-current-health math could become a declarative
  expression and the mcfunction could be dropped entirely.

Both are Searing Rage / Recollection behaviour — they affect two powers, not loading.

### Updated open items

10. **NEW — `is_closed_eye` predicate needs a non-predicate mechanism** (no vanilla origin query).
11. **NEW — `vengeful_spirit_calculate` needs rewriting**, ideally as an `apoli:change_resource`
    expression rather than an mcfunction.

---

## Phase 4 pre-work — the sweep: all 30 Voile types vs Apoli's 549 (2026-09-02)

CLAUDE.md said to check each Voile type against Apoli before writing Java. Done for all 30, by
reading both sides' serialized schemas — not by name matching, which produced at least one confident
false positive (see `zombie_arms`).

**Headline: 13 of 30 need no Java at all, and 3 more are a single shared candidate.
The real Java surface is ~14 types, not 26 as CLAUDE.md assumed.**

### A. Data-only translation — no Java (12 types)

| Voile type | Apoli replacement | Evidence |
|---|---|---|
| `water_breathing` | `apoli:water_breathing` | **Verified.** Voile's takes an *empty* `SerializableData()`; Apoli's only adds optional `suffocate_outside_water`. Pure namespace swap. |
| `instant_effect_immunity` | `apoli:effect_immunity` | **Verified identical fields** — `effect`, `effects`, `inverted`. Touhou uses it once with `minecraft:instant_health`. Voile hooked the instant-apply path specifically; Apoli blocks the effect generally, which subsumes it. |
| `action_on_entity_death` | `apoli:action_on_death` | **Verified identical fields** — `bientity_action`, `bientity_condition`, `damage_condition`. Confirm actor/target orientation in game. |
| `nearby_entities` | `apoli:entity_in_radius` | Field rename: `distance`→`radius`, `count`→`compare_to`. Apoli defaults `comparison` to `>=`, matching Voile's "at least N" semantics. |
| `check_set` | `apoli:entity_set_size` | Touhou's only use pairs it with `apoli:exists`, i.e. "set is non-empty" → `comparison: ">", compare_to: 0`. (Note: this is the type Spike 1 mistook for a scaling power.) |
| `disguise` | `apoli:disguise` | Voile's uses **Impersonate** (`Impersonator.impersonate` with a GameProfile). Apoli has a *native* disguise with `DisguisePlayerDummy` client rendering and `overwrite`/`change_name` options. Same outcome, different engine — so Impersonate is **not** needed. |
| `disguised` | `apoli:disguised` | Voile's condition takes no fields. |
| `remove_disguise` | `apoli:remove_disguise` | Same family. |
| `kill` | `apoli:damage` | Voile's `KillAction` only carries `damage_type` (default `GENERIC_KILL`). Express as lethal damage. |
| `teleport_to_entity` | `apoli:teleport_to` + `apoli:and` | Apoli covers the teleport; Voile's extra `sound`/`volume`/`pitch` compose with `apoli:play_sound`, and `fail_action` with `apoli:if_else`. |
| `modify_hurt_sound` | `apoli:replace_sound_emission` | see below |
| `modify_death_sound` | `apoli:replace_sound_emission` | see below |

**The sound trio (`modify_footstep_sound`, `modify_hurt_sound`, `modify_death_sound` — 7 power files)**
all plausibly collapse into one Apoli type. `ReplaceSoundPower.Config` takes `sounds`
(`SoundReplacements`), which maps an original sound id — **exact string or regex, with capture-group
references** — to weighted replacements. So `minecraft:entity.player.hurt` →
`touhouorigins:origin.ghost.hurt`, and footsteps via a pattern over `minecraft:block\..*\.step`.

Two things to confirm in game before relying on it:
1. that Apoli actually intercepts footstep/hurt/death emissions (it is a general emission hook, so
   likely, but unverified);
2. how to express Voile's `silent: true` (used by oni and satori_closed_eye). Mapping to
   `minecraft:intentionally_empty` is the obvious workaround if suppression is not supported.

### B. Split — `modify_scale` (6 files, the most-used type)

Already resolved in Spike 1 + decision D3, and unchanged by this sweep:
5 of 6 uses become vanilla attributes (`generic.scale`, the two `*_interaction_range`), and only
`bake_danuki/smelly` needs the relative Pehkui `ScaleModifier`. Apoli has **no** scaling type —
`grep -i scale` over its 549 ids returns nothing relevant.

### C. Genuinely needs Java (16 types)

Ordered by how many Touhou power files each blocks:

| Files | Type | Note |
|---|---|---|
| 5 | `modify_behavior` | entity AI goals; Apoli has no `behavior`/`goal` id at all |
| 2 | `flip_model` | Apoli's model types are `modify_player_model`/`modify_model_parts`/`model_color` — none flip |
| 2 | `prevent_sprinting_particles` | `apoli:prevent_sprinting` prevents *sprinting itself*, not the particles |
| 2 | `prevent_flying_kick` | no counterpart |
| 1 each | `zombie_arms` | **False positive caught:** `apoli:show_both_arms` hooks `ItemInHandRendererBothArmsMixin` (first-person item rendering); Voile hooks `BipedEntityModelMixin` (third-person arm pose). Different features that happen to share vocabulary. |
| 1 each | `convert_entity`, `disable_shields`, `enchantment_vulnerability`, `modify_divergence`, `prevent_item_slowdown`, `prevent_taming`, `reverse_instant_effects`, `action_on_block`, `precise_on_block`, `craftable`, `change_resource_with_status_effects` | no equivalent found; `precise_on_block` may partly reduce to `apoli:on_block`, and `action_on_block` to `apoli:action_on_land`/`block_collision` — both worth a closer look when implementing |

Searched-and-absent in Apoli's registry: `behavior`, `goal`, `convert`, `shield`, `scale`, `nearby`,
`reverse`, `taming`, `slowdown`. `craft` returns only `crafting_table`/`modify_crafting`.

### What this changes

CLAUDE.md budgeted "26 types need reimplementation or workaround". Measured, it is **16**, and the
cheapest wins are bigger than expected: the sound trio is one type, and the disguise family drops the
Impersonate dependency entirely (open item 8 resolved — Impersonate is not needed).

Suggested Phase 4 order, revised:

1. **Data-only batch** — the 12 in group A. One pass of JSON edits, no Java, and it should clear
   roughly 15 of the 48 failing powers. Verify the sound trio in game first since 7 files ride on it.
2. **`modify_scale`** — vanilla attributes for 5, then the Pehkui `ScaleModifier` (D3) for bake_danuki.
3. **`action_on_origin_gui`** — port the 35-line custom power; unblocks `boundless_servitude`.
4. **The 16** — cheapest first, leaving `modify_behavior`, `convert_entity`, `flip_model` and
   `zombie_arms` (all rendering or AI) for last.

### Resolved open items

8. ~~Impersonate / fpa~~ — **RESOLVED.** Apoli's disguise is native; Impersonate is not a dependency.

---

## Phase 4 step 1 — the data-only batch (2026-09-02)

33 conversions across 14 files, no Java. **Errors 51 → 31. Apoli powers 338 → 357.**
Every converted type parsed on first run — zero errors mentioning any of them.

| Voile type | → Apoli | Uses | Transform |
|---|---|---|---|
| `water_breathing` | `apoli:water_breathing` | 2 | rename |
| `instant_effect_immunity` | `apoli:effect_immunity` | 1 | rename |
| `action_on_entity_death` | `apoli:action_on_death` | 1 | rename |
| `disguise` | `apoli:disguise` | 1 | rename |
| `disguised` | `apoli:disguised` | 4 | rename |
| `remove_disguise` | `apoli:remove_disguise` | 4 | rename |
| `nearby_entities` | `apoli:entity_in_radius` | 11 | `distance`→`radius`, `count`→`compare_to`; one use had `entity_condition`, wrapped as `apoli:target_condition` |
| `check_set` | `apoli:entity_set_size` | 1 | `comparison: ">", compare_to: 0` (Touhou's use is just "set non-empty") |
| `kill` | `apoli:damage` | 1 | kept `damage_type`, added a lethal `amount` |
| `modify_hurt_sound` | `apoli:replace_sound_emission` | 2 | key `minecraft:entity\.player\.hurt.*` (regex, covers the on_fire/drown/freeze variants) |
| `modify_death_sound` | `apoli:replace_sound_emission` | 2 | key `minecraft:entity.player.death` (exact) |
| `modify_footstep_sound` | `apoli:replace_sound_emission` | 3 | key `minecraft:block\..*\.step` (regex); `silent:true` → `minecraft:empty` |

The disguise family confirmed as a clean swap: Apoli registers `disguised` in **both** entity and
bi-entity contexts and `remove_disguise` as an entity action, mirroring Voile's own split exactly.

### The sound trio — as far as source can settle it

Both open questions from the sweep are answered in Apoli's source:

1. **Coverage.** `EntityPlaySoundMixin` `@WrapOperation`s
   `Entity.playSound(SoundEvent,FF)` at its `Level.playSound(...)` call. In 1.21 `playStepSound`,
   `playHurtSound` and `die()` all emit through that exact method, so all three are in scope.
2. **Muting.** Native — `WeightedSound.MUTE = "minecraft:empty"`, checked via `replacement.mutes()`
   in `SoundReplacer.apply`, which skips the emit entirely.

Key syntax, from `SoundReplacements.compile`: a key containing any of `[\^$|?*+()[]{}]` compiles as a
**full-match regex**; otherwise it is an exact (namespaced) id. Note `.` alone does **not** make a key
a regex, so `minecraft:entity.player.death` stays exact — which is why the footstep and hurt keys use
explicit `\.` escapes to force regex mode.

**Still needs ears.** Source cannot prove the sounds actually surface correctly in a live game —
player sounds can take side paths (`ServerPlayer` sending sound packets directly, which is why Apoli
also ships `PlayerPlaySoundMixin` and `ServerPlayerNotifySoundMixin`). Test script written for Eileen
at `SOUND-TEST.md`; the load-bearing step is Test 3 (oni footsteps), the only one exercising a regex
key, which 5 of the 7 sound blocks depend on.

Verified the referenced sounds exist: `origin.ghost.hurt`, `origin.ghost.death`, `origin.oni.step`,
`origin.vengeful_spirit.{hurt,death}` are all in `sounds.json`, with 17 `.ogg` files shipped.

### Deferred from group A — `teleport_to_entity` (1 use)

Not a rename. Voile's is an **entity** action that searches a set for a target and teleports to it;
Apoli's `teleport_to` is a **bi-entity** action needing a target supplied. The faithful translation
wraps it in `apoli:action_on_set` (with `limit: 1`) and rebuilds the fail path via `apoli:if_else`,
since Voile's `fail_action` fires when no target is found. Worth doing carefully rather than
mechanically — it is `global/by_masters_side`, the shikigami teleport.

### Remaining 31 errors

- 26 Voile power types (15 distinct) — group C, needs Java
- 2 Voile condition types — `craftable` (item condition, `celestial/lavish_lifestyle`) and one entity
  condition in `ghost/withering_soul`
- 1 `touhouorigins:action_on_origin_gui` — the custom power's Java
- 2 redesign items carried from Phase 3 — `is_closed_eye` predicate, `vengeful_spirit_calculate`

---

## Sound spike — instrumented Apoli, root-caused, partly fixed (2026-09-02)

Eileen's in-game test came back **vanilla on all four steps**. Rather than guess, temporarily
instrumented Apoli's `SoundReplacer` (three probes: hook entry with side, `collect` results,
key match/no-match) in `build-deps/og-apoli`, rebuilt, and had her play for 30 seconds.

### The probe answered everything

2438 probe lines. Filtered to the player:

```
SERVER type=apoli:replace_sound_emission holder=Dev sound=minecraft:block.sand.step   entries=2   (x29)
SERVER type=apoli:replace_sound_emission holder=Dev sound=minecraft:entity.player.hurt entries=2   (x1)

collect: powersOfType(apoli:replace_sound_emission)=
    [touhouorigins:ghost/fading_body_death_sound, touhouorigins:ghost/fading_body_hurt_sound]
    allPowers=43

power=touhouorigins:ghost/fading_body_hurt_sound
  keys=exact[] regex[minecraft:entity\.player\.hurt.*] vs sound=minecraft:entity.player.hurt
  -> MATCH touhouorigins:origin.ghost.hurt
```

| Question | Answer |
|---|---|
| Does the hook fire? | **Yes** — 30 player events |
| Are the powers on the container? | **Yes** — both found, `allPowers=43` |
| Is the config missing client-side? | **No** — `ApoliPowers.get()` never returned null |
| Did the key match? | **Yes** — regex matched, correct replacement selected |
| Which side? | **`SERVER` only. Zero `CLIENT` lines for the player.** |

**Root cause: the data conversion was correct; the mechanism does not reach the emitting player.**
The server replaces the sound and passes it to `Level.playSound(except = thatPlayer, …)`, which
broadcasts to everyone *except* them. The player's own client plays the vanilla sound by local
prediction on a path this hook never touches. Another player nearby would have heard the replacement
correctly all along.

This vindicates the sweep's schema analysis and invalidates only the assumption that emission covers
self-heard sound — which is exactly what the sweep flagged as needing an in-game check.

### Fix — two thirds free, one third needs Java

`apoli:replace_sound_reception` hooks `SoundEngine.play(SoundInstance)` client-side
(`SoundEngineReceptionMixin`). Critically it receives **only the sound instance, no source entity**,
so it can match by sound id but cannot be scoped to "sounds I emit".

- **hurt + death — FIXED, no Java.** Added `_self` reception twins beside the emission blocks in
  `ghost/fading_body.json` and `vengeful_spirit/extra.json`. In singleplayer the player is the only
  player, so replacing every `minecraft:entity.player.hurt` is exactly right. Emission kept so other
  players still hear it. Verified: **361 powers** (was 357, +4), zero reception errors.
- **footsteps — NEEDS JAVA.** Reception would match `minecraft:block\..*\.step` from *any* source, so
  a passing cow would emit oni footsteps. Not acceptable. The three footstep blocks
  (`oni/brawn_of_the_oni`, `nue/undefined_flying_youkai`, `satori_closed_eye/unconscious_manipulation`)
  stay emission-only and move to group C. The implementation is small — a mixin on
  `Entity.playStepSound` keyed to the holder, essentially what Voile did.

**Group C is therefore 17 types, not 16.** Still well under CLAUDE.md's estimate of 26.

### Hygiene

The instrumented Apoli was reverted by re-copying from `reference/og-apoli`, rebuilding, and
republishing; verified `SNDPROBE` is absent from the published jar. `build-deps/` is gitignored, so
nothing instrumented ever entered the tree.

### Other findings from the same session

**Black HUD bars — FIXED (commit 52e5f0b).** All six sprite paths Touhou referenced are missing from
Overgrown's Origins, but the textures exist, moved from `origins:textures/gui/community/<author>/…`
to `origins:textures/gui/sprites/hud_render/<author>/…`; plain `resource_bar.png` is now
`…/hud_render/apace/resource_bar.png`. Repointed 28 references across 25 files.

**Oni in the Nether — not a bug.** The Nether spawn is intended (`common/denizen_of_hell`,
`apoli:modify_player_spawn`, no Voile, untouched). Being attacked there is also correct: oni's power
list is byte-identical to upstream, and none of its four powers touch mob targeting. The "mobs ignore
you" power is `common/feared` (`voile:modify_behavior`), held only by **jiang_shi** and **nue** — it
will start working for those two once `modify_behavior` is implemented.

**Oni size — not the mod.** Eileen reported feeling slightly larger. `brawn_of_the_oni` carries only
`max_health +10` and `attack_damage +6`; there is no scale attribute and `voile:modify_scale` did not
load. Most likely the two extra hearts reading as "bigger". Worth an A/B against `origins:human` once
scaling is implemented.

---

## Phase 4 step 2 — modify_scale (2026-09-02)

`voile:modify_scale` fully retired. **Errors 31 → 25, Apoli powers 361 → 367.** First Java in the port.

### 5 of 6 uses → vanilla attributes, no Java

| Origin | Voile | Now |
|---|---|---|
| earth_rabbit / fairy / kappa / oni | `scale` 0.85 / 0.75 / 0.9 / 1.1 | `apoli:attribute` → `minecraft:generic.scale` |
| wolf_tengu | `scale_types: [pehkui:reach]`, 1.33 | `apoli:attribute` → both `player.block_interaction_range` and `player.entity_interaction_range` |

Operation is **`standard_multiply_base`** (`base * mod`), not `multiply_base`. Apoli's `multiply_base`
is `MULTIPLY_BASE_ADDITIVE` (`base + base*mod`), so expressing ×0.85 through it would mean writing
`-0.15`. Full operation list is in `AttributeModifierOperation.java`; `serializedName()` is just the
enum name lowercased.

**Accepted loss:** Voile's default scale types were WIDTH/HEIGHT/DROPS/VISIBILITY. `generic.scale`
covers the body-size part; the four body-size origins no longer get the DROPS (cosmetic) or
VISIBILITY (gameplay) components. Agreed in Spike 1; noting it is now real rather than theoretical.

### 1 use → Java: `touhouorigins:modify_scale` for `bake_danuki/smelly`

Two classes, implementing decision D3 (relative, not absolute).

`scale/RelativeScaleModifier.java` — one **stateless** Pehkui `ScaleModifier`, shared by all powers of
this type. Pehkui computes `getScale()` as `baseScale` passed through each base-value modifier, so:

- **Player Scale owns the base layer** (its `/scale multiply` writes `setTargetScale`)
- **we own a layer on top**

`modifyScale`/`modifyPrevScale` return `value * product(active power factors naming this scale type)`,
recomputed live from `PowerContainer` each call — honouring `isSuppressed` and each power's own
`condition`. Stateless means one instance serves any number of powers at any factors with no
bookkeeping to desync, and an orphaned attachment returns 1.0 rather than misbehaving.

`power/ModifyScalePower.java` — the Apoli `PowerType<Cfg>`. Config is Voile's schema unchanged
(`scale_types` defaulting to Voile's four, `scale`), so the data needed only a type rename.
Attaches the modifier in `onAdded` and **re-asserts in `tick`**: `ScaleData.resetScale()` clears all
base value modifiers (`ScaleData.java:639`) and Player Scale calls `/scale reset` in every
`entity_action_removed`, i.e. on every origin swap — attach-once would silently stop working.
Re-adding to a `SortedSet` is a no-op, so the tick is cheap. `onRemoved` detaches and leaves base
scale untouched, which is precisely what Voile's `resetScale()` destroyed.

Registered in the `@Mod` constructor via `PowerTypeRegistry.register` (Apoli registers directly, not
through DeferredRegister) and `ScaleRegistries.register(SCALE_MODIFIERS, …)`. Both run on both sides
— Pehkui syncs modifiers by id.

### Method note — read the jar, not the source

Pehkui's `neoforge/1.21` **source** is Yarn-mapped (`net.minecraft.util.Identifier`,
`NbtCompound`); the published NeoForge jar is remapped at build time. Signatures were taken with
`javap` against `pehkui-nKHizsl6.jar`, which is Mojmap (`ResourceLocation`). Writing against the
source tree would have produced code that does not compile. Same discipline as CLAUDE.md's "read
source, don't recall", one level deeper: for a remapped dependency, read the *artifact*.

### Verification status

Registers and parses clean — `Done (1.303s)`, 367 powers, zero scale/Pehkui errors, and
`voile:modify_scale` no longer appears among unknown types.

**Not yet verified in game.** Two things need a real session, and one needs Player Scale installed:

1. the four body-size origins actually look bigger/smaller (A/B against `origins:human`);
2. wolf_tengu's reach is longer;
3. **the relative behaviour** — meaningful only with Player Scale present, since a bare dev instance
   has no base scale to compose with. Expected: micro + smelly bake_danuki → 0.5 × 2 = 1.0 net
   visibility, and swapping Player Scale origin should not permanently lose the effect (the tick
   re-assert covers `/scale reset`).

### Remaining — 25 errors

20 Voile power-type errors across 13 distinct types, 1 `touhouorigins:action_on_origin_gui`, and 4
non-power items (the `is_closed_eye` predicate, `vengeful_spirit_calculate`, `voile:craftable` item
condition, one entity condition in `ghost/withering_soul`).

Group C, revised after the sound spike and this step: **17 types**, of which `modify_behavior` (5
files) is the largest single win remaining.

---

## In-game verification round (2026-09-02) — Eileen

### The scale bug, caught in play

First run of the vanilla-attribute scaling was wrong, and only measurement revealed it:

| | expected | measured (buggy) | measured (fixed) |
|---|---|---|---|
| human | ~1.8 blocks | 1.8 | 1.8 |
| oni ×1.1 | ~2.0 | **4** | "exactly two blocks" |
| fairy ×0.75 | ~1.35 | **3+** (bigger!) | **1.5** |
| wolf tengu reach | 4.5 → 6.0 | **10** | +2 over human |

Fairy coming out *larger* despite a shrink factor was the tell. Root cause in
`AttributeModifierOperation.vanillaOperation()`: every base-phase multiply, `standard_multiply_base`
included, flattens onto vanilla `ADD_MULTIPLIED_BASE` (`base + base*value`). So on a real attribute
that name does not mean `base*value`. With scale base 1.0: oni `1 + 1.1 = 2.1`, fairy
`1 + 0.75 = 1.75` — matching the measurements exactly. Wolf tengu independently confirmed it:
`4.5 + 4.5*1.33 = 10.49`, measured 10.

**Rule to remember: Apoli's rich operation enum governs its own resource math. Anything applied to a
vanilla attribute collapses to vanilla's three operations, so express "×f" as `value = f - 1`.**

Also learned: `wolf_tengu/mountainous_blades` is gated on holding a sword
(`apoli:equipped_item` + `minecraft:swords`). The first "reach test failed" was a defect in the test
script, not the code.

### Confirmed working

- **Ghost hurt + death sounds** — the `replace_sound_reception` twins fixed the self-heard case.
  Closes the sound spike for those two. Footsteps remain deliberately unported.
- **HUD bars** on nue and satori_closed_eye — the `hud_render` sprite path fix landed.
- **`action_on_origin_gui` mixin applied** at runtime.

### Scale composition with Player Scale — verified, better than designed for

Player Scale was installed into the dev world (`player-scale/dist`, pack_format 48) and works on
1.21.1 — **Deliverable 1 is effectively proven**.

Eileen observed fairy size is *relative* to Player Scale size. That falls out of using vanilla
attributes: Player Scale drives Pehkui's `model_height`/`hitbox_height` base scales, fairy drives
vanilla `minecraft:generic.scale`, and neither knows about the other, so they multiply. This is the
Spike 1 prediction holding in practice, and the reason the four body-size origins needed no Pehkui
while `bake_danuki` did (`visibility` has no vanilla counterpart).

**No clamp at the extremes.** Eileen initially reported micro+fairy "stops" shrinking. Pehkui
multiplies rather than replaces (`scale != 1.0F ? scale * value : value`) and vanilla `generic.scale`
has no floor near 0.75, so the predicted difference was ~0.14 blocks — about two pixels, made worse
by micro's `third_person 0.8` camera pull. Settled with a **0.5-block slab gap**: micro (0.56 tall)
does not fit, micro+fairy (0.42) does. Composition holds all the way down; the earlier report was
perceptual. Eye height also shifts noticeably, which third person hides.

### bake_danuki relative visibility — decision D3 confirmed in play

`smelly` is passive, not toggled (bare scale block, description reads "Mobs can detect you from
twice as far away") — being permanently noticeable is the origin's downside.

All three predicted signals observed:
- normal + bake_danuki → detection range further than normal
- micro vs micro + bake_danuki → range differs, so our modifier composes with Player Scale's base
  rather than replacing it
- micro + bake_danuki → roughly **human** range, i.e. `0.5 × 2 = 1.0`

Under Voile's absolute `setScale` this last case would have been a flat 2.0 — *more* noticeable than
a normal human, discarding the micro-ness. The relative design works.

---

## Phase 4 step 3 — modify_behavior (2026-09-02)

`voile:modify_behavior` retired. **Errors 25 → 19.** Both mixins confirmed applying at runtime.

### The scope finding

Voile implements this across **31 mixin files, ~1806 lines** — one per awkward mob (bees, cats,
foxes, ocelots, llamas, pandas, polar bears, pufferfish, dolphins, endermen, spiders, snow and iron
golems, wandering traders, wardens, withers, the ender dragon) plus separate *brain* handling for
piglins, hoglins and villagers. That is ~18x CLAUDE.md's "stop and ask above ~100 lines" threshold.

Ported in **~200 lines across 4 files**, because NeoForge offers hooks Fabric does not.

### neutral — one mixin covers the goal-driven path

`NearestAttackableTargetGoalMixin` `@ModifyArg`s the `TargetingConditions.selector(Predicate)` call
inside the six-argument constructor that every other constructor delegates to — so one injection
covers the whole family. Voile's trick, translated Yarn (`ActiveTargetGoal` /
`TargetPredicate.setPredicate`) to Mojmap. The original predicate is preserved and ANDed, so we only
ever remove targets, never add them.

Covers `jiang_shi/one_of_us`, `satori/animal_friends`, `satori_closed_eye/presence_of_no_merit`.

### hostile — one event listener replaces ~20 mixins

Voile needed a mixin per mob because Fabric has no general entity-spawn hook. NeoForge has
`EntityJoinLevelEvent`, so `HostileTargetingHandler` adds a single player-targeting goal to every
`Mob` as it spawns and lets the goal's predicate decide:

```java
mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, Player.class, false,
    target -> BehaviorLookup.behaviorFor(target, mob) == Behavior.HOSTILE));
```

Inert unless a nearby player holds a hostile power whose conditions accept that mob, so the ordinary
cost is one predicate call failing on its first check.

Covers `amanojaku/antagonist` (unconditional) and `common/feared` (gated on
`touhouorigins:villager_aligned` = villagers + iron golem).

`BehaviorLookup` replaces Voile's `BehaviorHelper`: a plain query returning the first matching
behaviour rather than building and filtering a mutable list per call, since the callers are AI hot
paths.

### Known gap — villagers do not flee

`villager_aligned` is villagers + iron golem. **Iron golems will attack**, having attack goals for
the new targeting goal to drive. **Villagers will not visibly react**: they have no attack goals at
all, and their fear behaviour lives in the brain (`VillagerHostilesSensor`), which Voile hooked with
a separate mixin. So `feared` lands as "iron golems hunt you" rather than "the village panics".

Eileen asked for `feared` working and this is the substantive half; the villager-flee sensor mixin is
a deliberate follow-up rather than a silent omission.

### Remaining — 19 errors

12 Voile power types across 11 distinct types, plus the 4 non-power items (`is_closed_eye` predicate,
`vengeful_spirit_calculate`, `voile:craftable`, one condition in `ghost/withering_soul`) and the 3
footstep sound blocks awaiting their mixin.

### `feared` completed — villagers now flee too

Follow-up to the gap noted above, prompted by Eileen asking how villagers react to zombies.

**The vanilla mechanism.** Villager fear is brain-driven, not goal-driven:
`VillagerHostilesSensor.isMatchingEntity(villager, target)` tests each nearby entity against a
static map of scary types (zombie, husk, drowned, illagers, vex, ravager, witch, zoglin), each with
its own radius, and writes any match into the `NEAREST_HOSTILE` brain memory; a separate brain
behaviour reads that memory and runs the panic activity. Players are absent from that map, which is
why villagers ignore them however aggressive they are — and why the targeting goal could not reach
them.

**The fix is one injection, not another subsystem.** Because the panic behaviour already exists and
merely reads a memory, making the player *count as scary* is sufficient.
`VillagerHostilesSensorMixin` returns true at HEAD when the player holds a `hostile`
`modify_behavior` accepting this villager and is within 8 blocks (Voile's radius, comparable to
vanilla's own entries). Creative and spectator players are skipped — villagers already ignore those.

Like the targeting mixin, it only ever *adds*: it returns true or falls through, never false. Since
players are not in the vanilla map, a neutral/passive holder has nothing to suppress, so vanilla
behaviour cannot be broken by this hook.

`common/feared` is therefore complete: **iron golems hunt you** (targeting goal) and **villagers flee
you** (sensor). Three mixins now confirmed applying at runtime — `OriginsServerNetworkMixin`,
`NearestAttackableTargetGoalMixin`, `VillagerHostilesSensorMixin` — errors steady at 19.

Total for `modify_behavior` including this: **~220 lines across 5 files**, against Voile's 31 mixins
and ~1806 lines.

### `feared` verified in play, plus a difficulty caveat

Eileen confirmed both halves working: iron golems hunt, villagers flee.

**Known nuance, not a porting gap:** `feared` is inert on Peaceful — and it is stronger than "iron
golems don't attack". Eileen asked whether angry wolves attack on Peaceful, which sent us to the
decompiled source:

```java
// LivingEntity.canAttack(LivingEntity)
return target instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL
    ? false : target.canBeSeenAsEnemy();
```

That is a blanket rule on `LivingEntity`, so **no mob targets a player on Peaceful** — hostile,
neutral, angered wolves, iron golems alike. It has nothing to do with despawning (a separate check in
`Mob`, and neutral mobs do not despawn anyway).

Consequence — and the two halves of `feared` diverge here, which Eileen caught in play:

| half | machinery | on Peaceful |
|---|---|---|
| iron golems | targeting goal → `LivingEntity.canAttack` | **blocked** |
| villagers | `VillagerHostilesSensor` → brain memory → panic | **still flee** |

`NearestVisibleLivingEntitySensor` picks candidates from the `NEAREST_VISIBLE_LIVING_ENTITIES` memory
and filters them with `isMatchingEntity` — no `canAttack` call anywhere in that path, so difficulty
never enters it. The fear pipeline is genuinely independent of the targeting pipeline, which is why
villagers needed their own mixin in the first place.

Our goal sits above `canAttack` and cannot override it, which is correct: an origin quietly defeating
Peaceful mode would be a bad surprise. So on Peaceful `feared` reads as "the village panics but the
golem does not come", rather than being wholly inert.

**Method note.** Two wrong searches preceded the right one. `build/moddev/artifacts/*-sources.jar` is
**NeoForge's** sources (~25 files), not Minecraft's — grepping it for `PEACEFUL` finds one unrelated
hit and looks conclusive. The real decompiled Minecraft source lives in
`~/.gradle/caches/neoformruntime/intermediate_results/decompile_*_output.jar`. Use that when a
vanilla-behaviour question comes up.

**Deliberately not implemented:** villagers ringing the alarm bell when a feared player approaches.
Eileen raised it as a nicer flavour and then declined it, on the grounds that it is not in the
upstream mod. Recorded because the hook already exists — `VillagerHostilesSensorMixin` is the place
it would go if that ever changes.

---

## Phase 4 step 4 — preventer batch + footsteps (2026-09-02)

Four power types, three mixins, eight data conversions.

| Power | Hook | Config |
|---|---|---|
| `prevent_sprinting_particles` | `Entity.canSpawnSprintParticle` | none |
| `disable_shields` | `LivingEntity.canDisableShield` | none |
| `prevent_flying_kick` | `ServerGamePacketListenerImpl.tick` | none |
| `modify_footstep_sound` | `Entity.playStepSound` | `sound` / `silent` |

The three flag powers share one `SimplePowers.FlagPower` using Apoli's own `MapCodec.unit(...)`
convention. Voile had five near-identical classes with empty `SerializableData`; there was no reason
to reproduce that shape.

`prevent_flying_kick` zeroes `aboveGroundTickCount` (and the vehicle counter) each tick while the
power is active, rather than disabling the anti-fly check outright — the protection still applies to
every other player.

Note the Yarn→Mojmap rename here is not mechanical: `shouldSpawnSprintingParticles` becomes
`canSpawnSprintParticle`, and `disablesShield` becomes `canDisableShield`.

### Footsteps — gap closed by changing mechanism

The sound spike left footsteps unported: `replace_sound_emission` never reaches the emitting player,
and `replace_sound_reception` carries no source entity, so a `minecraft:block\..*\.step` key would
have rewritten every passing cow's footsteps too.

Reading Voile's preventer mixins surfaced the answer beside them — **hook `Entity.playStepSound`**.
That is source-scoped by construction: it fires on the entity taking the step, so only the holder's
footsteps change, and everyone nearby hears the change. One injection covers both silence and
replacement.

The three footstep blocks moved off `apoli:replace_sound_emission` onto
`touhouorigins:modify_footstep_sound`, so oni's heavy steps and nue / satori_closed_eye's silent
movement should now work — the case that failed Eileen's very first sound test.

**Method note:** this is twice now that reading Voile's implementation of one type has solved a
different type. Worth reading neighbouring mixins before concluding something has no hook.

## Phase 4 step 5 — second preventer batch (2026-09-02)

`reverse_instant_effects` and `prevent_item_slowdown` retired.
**Errors 14 → 12, powers 367 → 380**, clean run at `Done (2.892s)`.

**`reverse_instant_effects`** (`jiang_shi/pestilence`) is a one-liner. 1.21 removed `MobType`, so
"counts as undead" for potions is exactly `LivingEntity.isInvertedHealAndHarm()`; inverting that
single return gives both directions, because vanilla calls it from the instant-health and
instant-damage effects alike.

**`prevent_item_slowdown`** (`wolf_tengu/steadfast_defense`, shields) is the port's first
**client-side** mixin. Worth recording why: Voile registers this power in its *common* source set but
only consumes it in its *client* set, because the movement penalty is applied by the client's own
player tick, which asks `isUsingItem()` twice — once for the slowdown, once for whether sprinting may
start. Mirrored here: the power type registers commonly so the server knows it for syncing and
conditions, and the mixin is declared under `client` in `touhouorigins.mixins.json`.
Mojmap targets are `LocalPlayer.aiStep()` and the private `LocalPlayer.canStartSprinting()`.

### Scope findings on the remaining two in this group

**`enchantment_vulnerability` cannot be ported as written.** Voile hooks `DamageEnchantment` and
`ImpalingEnchantment`; **neither class exists in 1.21**, which made enchantments fully data-driven.
Its single use is `jiang_shi/pestilence` taking extra damage from Smite. In 1.21 Smite's bonus is a
datapack effect gated on the `minecraft:sensitive_to_smite` entity-type tag — and the tag route is
unusable, since adding `minecraft:player` would make *every* player smite-vulnerable. Needs a hook
into the data-driven enchantment damage path. Genuine design question, deliberately deferred.

**`prevent_taming` is six mixins, not one.** Voile routes wolf, cat, ocelot, parrot, horse and
`TamableAnimal` through `TameUtil`, each wrapping the `Random.nextInt` roll that decides taming
success and forcing a non-zero (failing) result. Each mixin is small, but it is six files for one
flavour power on `amanojaku/antagonist`.

### Environment: verification runs are now isolated

Eileen runs her own Minecraft client alongside this work, which surfaced two collisions:

1. **World lock** — `runServer` died with `IOException: another process has locked a portion of the
   file`, because a client held `run/saves/New World`.
2. **Port clash** — after fixing that, `FAILED TO BIND TO PORT: Address already in use` on 25565.

Both fixed permanently rather than worked around: verification runs use their own world
(`level-name=verify-world`, flat, Player Scale pre-installed) and their own port (**25599**). A client
can now stay open indefinitely, LAN-shared or not, without blocking a server check. The flat world
also got proper `generator-settings`, removing the self-inflicted `No key layers` error present in
every server log since Phase 2.

**Process hygiene:** cleanup previously killed *any* process matching `fml.modFolders`, which would
have terminated Eileen's unrelated instance. Now scoped to command lines containing `origins-port`.

### Near-miss worth remembering

The world-locked run reported **"ERRORs: 1 (was 14)"** with an empty unknown-types list — which reads
exactly like every remaining Voile type having been fixed. It actually meant the server died before
datapack loading, so there was nothing to count. The check was testing for the *absence* of errors
rather than the *presence* of content; it now greps `Loaded N power(s)` and `Loaded N origins`
explicitly. Same shape as the Phase 2 `| tail` trap that hid a Gradle failure behind exit code 0:
**silence is not success.**

### Remaining — 12 errors

7 Voile types: `flip_model` (2 uses), then `zombie_arms`, `prevent_taming`, `modify_divergence`,
`enchantment_vulnerability`, `convert_entity`, `action_on_block` (1 each). Plus the 4 non-power items
(`is_closed_eye` predicate, `vengeful_spirit_calculate`, `voile:craftable`,
one condition in `ghost/withering_soul`).

## Phase 4 step 6 — prevent_taming (2026-09-02)

`voile:prevent_taming` retired. **Errors 12 → 11, powers 380 → 381.** Three mixins, not the six
Voile's structure implied.

### First attempt failed at mixin-apply time

Translating Voile's four per-animal mixins literally — each `@ModifyExpressionValue` wrapping the
`nextInt` taming roll inside `mobInteract` — compiled cleanly and died on load:

```
Critical injection failure: touhouorigins$preventTaming(ILnet/.../Player;)I
in CatTamingMixin failed injection check, (0/1) succeeded. Scanned 0 target(s).
```

**1.21 moved the roll.** It now lives in a private `tryToTame(Player)`, so `mobInteract` has no roll
to wrap. This is the failure mode Spike 2 warned about: mixins fail at *apply* time, not compile time.

### The rework is better than the thing it was copying

Chasing where the roll went showed Cat, Wolf and Parrot all funnel into
**`TamableAnimal.tame(Player)`** once their roll succeeds. Cancelling that one method covers all
three, plus any modded tameable, replacing four brittle RNG wrappers with one stable hook.

Behaviour is unchanged: the food is consumed and the roll has already happened, so it reads as an
ordinary failed taming attempt rather than a dead interaction.

Two genuine exceptions remain:

- **Ocelot** — extends `Animal`, not `TamableAnimal`. Ocelots have not been tameable since 1.14; they
  gain *trust*, rolled inline in `mobInteract`. Keeps a wrapper, and is now the only one.
- **Horses** — `RunAroundLikeCrazyGoal` (Yarn's `HorseBondWithPlayerGoal`) uses a temper threshold
  rather than a zero-roll, and the rider is the horse's first passenger rather than an argument.
  Returning temper guarantees the comparison fails.

### Rule earned twice now

**Voile reliably says what a power should _do_; it does not say where to hook it in 1.21.**
First `enchantment_vulnerability` (the classes it hooks no longer exist), now `prevent_taming` (the
roll moved). Voile targets 1.20.2 and 1.21 reorganised entity internals substantially. Check the
decompiled 1.21 source for the actual call site before translating a mixin target.

Also worth noting the improved verification check paid for itself immediately: it reported an empty
"content loaded" line rather than "0 errors, no unknown types", so the crash read as a crash instead
of a clean sweep.

### Remaining — 11 errors, 6 Voile types

`flip_model` (2 uses), `zombie_arms`, `modify_divergence`, `convert_entity`, `action_on_block`,
`enchantment_vulnerability` (deferred by agreement). Plus the 4 non-power items.

## Phase 4 step 7 — action_on_block + modify_divergence (2026-09-02)

Both wolf_tengu powers. **Errors 11 → 9, powers 381 → 383**, clean at `Done (3.915s)`.

**`action_on_block`** is misleadingly named — nothing to do with world blocks; "block" is the verb.
It fires when a shield absorbs a hit (`wolf_tengu/steadfast_defense`, where blocking is the
counter-attack). Hooked at the `hurtCurrentlyUsedShield` call inside `LivingEntity.hurt`, which is
vanilla's own decision point for "the shield ate that". Shares `LivingEntityShieldMixin` with
`disable_shields`, since both concern shields. Voile's nicety preserved: when a projectile is
blocked, the action targets the projectile's **owner**, so blocking an arrow hits the archer.

**`modify_divergence`** (`wolf_tengu/clumsy_archer`) — "divergence" is vanilla's `inaccuracy` cone.
`Projectile.shootFromRotation` (Yarn `setVelocity`) covers bows, crossbows and thrown items in one
place; ordinal 4 of five floats is the inaccuracy. Voile also wrapped ordinal 3 (velocity) for its
`modify_projectile_speed` power, which Touhou does not use, so that is deliberately not ported.
Semantics kept: a single `set` power wins outright, otherwise values sum onto vanilla's.

`DamageCtx` takes four arguments in Overgrown's Apoli, not the two Voile's shape implied — caught at
compile time.

### Environment: TaskStop does not kill the forked server JVM

A verification run failed with the same world-lock `IOException` as before, but this time the culprit
was **ours**: `TaskStop` ends the Gradle wrapper while the forked Minecraft JVM survives and keeps
holding `verify-world`. Two orphans from the previous run were still alive.

Fix: stop verification servers by killing the JVMs whose command line contains `origins-port`, rather
than relying on `TaskStop` alone. Scoping on that string matters — Eileen runs her own client, and an
unscoped kill would take it down.

**The presence-check has now prevented three false passes.** This run reported "ERRORs: 1 (was 11)"
with an empty unknown-types list, which reads as a clean sweep; the empty "content loaded" line is
what identified it as a crash. Absence of errors is not evidence of success.

### Remaining — 9 errors, 3 Voile types

`flip_model` (2 uses) and `zombie_arms` — client rendering, the last real unknowns.
`convert_entity` — Voile uses Fabric's `ServerLivingEntityEvents.AllowDeath`, which has no direct
NeoForge twin; `LivingDeathEvent` is cancellable and Apoli already uses `LivingConversionEvent`, so
the pieces exist but it needs designing rather than translating.
`enchantment_vulnerability` — deferred by agreement.

Plus the 4 non-power items unchanged since Phase 3.

## Test round 2 — four fixes from Eileen's play session (2026-09-02)

Eileen's second in-game round found three real bugs and surfaced one gap I had missed. All fixed;
verification green at `Done (4.916s)`, 383 powers, 13 mixins applied, errors steady at 9.

### 1. Snow layers and carpets did not clank — real bug

Snow layers and carpets are `COMBINATION_STEP_SOUND_BLOCKS`. Vanilla routes them through
`playCombinationStepSounds` (the thin block's sound plus a muffled one for the block beneath), never
`playStepSound`. The mixin only hooked the latter. Now hooks both; cancelling the combination method
also covers the `playMuffledStepSound` call nested inside it.

Eileen's instinct that carpets made sense but snow did not was the useful signal — internally they are
the same category, so the pattern was "thin blocks", not "soft blocks".

### 2. Doubled, offset footsteps with a delay — real bug

`handleStep` played the replacement with `Level.playSound(null, …)`, which broadcasts to **everyone,
holder included**. So the holder heard the server's copy a network round-trip late, on top of the
vanilla step their own client had already played — audibly doubled and offset.

Fixed by using `entity.playSound(sound, volume, pitch)`: vanilla's own routing, which excludes the
player from the broadcast and plays locally on their client instead. One sound, immediate.

The initial delay shares a root cause: Apoli syncs power data to clients shortly after an origin
change (`ClientPowerState` → `ApoliPowers.replaceAll`), so the first steps occur before the client
knows about the power. Should be far shorter now the sound is client-local.

Animal footsteps staying normal confirmed the source-scoping works, which was the whole reason for
abandoning sound-id replacement.

### 3. Taming showed hearts but gave no collar — real bug

Cancelling `TamableAnimal.tame()` was not enough. Vanilla's `tryToTame`:

```java
if (this.random.nextInt(3) == 0) {
    this.tame(player);
    this.level().broadcastEntityEvent(this, (byte)7);   // hearts
} else {
    this.level().broadcastEntityEvent(this, (byte)6);   // smoke
}
```

The hearts broadcast *after* `tame()`, so cancelling the call left the animation intact — the animal
appeared to accept the player yet never gained a collar. Now the roll itself is failed, so vanilla
takes its own else-branch and emits **smoke**: the honest "it refused you". Eileen's doubt about lore
alignment was well placed.

Wolf and Cat hook `tryToTame`; Parrot has none, so its inline roll in `mobInteract` is hooked instead.
`TamableAnimalMixin` is retained as a backstop for modded tameables.

### 4. Blocking an arrow — **not** a bug, but it exposed one

`steadfast_defense` declares `"damage_condition": {"type": "apoli:in_tag", "tag":
"touhouorigins:is_melee"}`. It is a melee parry by design, so blocking an arrow correctly does
nothing.

Reading that config did reveal a real omission: `"cooldown": 100`, parsed and then ignored, so the
parry fired on every block instead of once per five seconds. Now implemented with a per-power
cooldown map decremented in `tick` and cleared in `onRemoved`, mirroring Apoli's own cooldown powers.
`hud_render` is parsed but not yet wired to Apoli's HUD.

### The mixin that failed to apply — third variant of one mistake

The snow fix compiled and then died at load:

```
@Inject ... could not find any targets matching
'playCombinationStepSounds(Lnet/minecraft/world/level/block/state/BlockState;Lnet/.../BlockState;)V'
```

**NeoForge patches that signature.** Vanilla takes two `BlockState`s; NeoForge 21.1.248 appends the
two `BlockPos`es. The descriptor had been read from a stray fabric-loom cache for NeoForge
**21.1.125**, not the artifact we compile and run against.

That is the third variant of the same error: Pehkui's *source* instead of its remapped jar; NeoForge's
sources instead of Minecraft's decompiled ones; and now the *wrong NeoForge version's* jar.

**Rule: verify mixin targets against `build/moddev/artifacts/neoforge-<version>.jar`.** Nothing else
is authoritative. Noted in `EntityStepMixin` itself so it does not get re-broken. Every other target
was re-checked against that jar — `tryToTame`, `mobInteract`, `shootFromRotation`,
`hurtCurrentlyUsedShield`, `isInvertedHealAndHarm`, `canDisableShield` — all correct, so this was an
isolated slip.

Incidental finding: `LivingEntity` overrides `playStepSound`, yet oni footsteps worked, so the
override calls through to `Entity`'s. Worth knowing, since the whole feature rests on that hook.

### Confirmed working in play

Oni footsteps on solid blocks, satori_closed_eye silence, jiang_shi reverse healing, wolf tengu
melee parry, clumsy archer (comically so — upstream's value, unchanged), and wolf tengu reach at
**6 blocks**, exactly the corrected prediction.

**Untestable:** `disable_shields` — vanilla mobs never raise shields, so it cannot be verified without
a mob-uses-shields datapack. Not a failure, just unverified.

**Not proven by server runs:** `ServerGamePacketListenerImplMixin` (`prevent_flying_kick`) only loads
when a player connects, and `LocalPlayerItemSlowdownMixin` is client-only. Neither appears in a
headless verification.

## Phase 4 step 8 — convert_entity (2026-09-02)

`voile:convert_entity` retired. **Errors 9 → 8, powers 383 → 384**, clean at `Done (4.070s)`.
Only the rendering pair and the deferred `enchantment_vulnerability` remain.

### The Fabric-only event, resolved without a mixin

Voile hangs this on Fabric's `ServerLivingEntityEvents.AllowDeath`, which returns a boolean to veto
the death. NeoForge has no twin — but `LivingDeathEvent` is **cancellable**, which amounts to the
same contract: convert the victim, then cancel so vanilla's death handling never runs.
`Mob.convertTo` already discards the original entity, so nothing is left to kill. No mixin required.

For `jiang_shi/pestilence` this means killing a villager infects it into a zombie villager — exactly
what a vanilla zombie does on Hard.

### Villager conversion is special-cased on purpose

A generic `convertTo` silently drops everything that makes the villager *that* villager, so the
villager → zombie villager path carries it over by hand, as vanilla's own infection does:

```java
zombie.setVillagerData(villager.getVillagerData());   // profession, level
zombie.setGossips(villager.getGossips().store(ops));  // reputation memory
zombie.setTradeOffers(villager.getOffers());          // trades
zombie.setVillagerXp(villager.getVillagerXp());
```

Without this, curing the zombie later yields a **blank villager** — profession, trades and reputation
gone. The difference between "infected the librarian" and "deleted the librarian".

Also preserved from Voile: the difficulty gate (never on Peaceful/Easy, coin-flip on Normal, always
on Hard — though `ignore_difficulty` defaults true, so pestilence bypasses it), the
`SOUND_ZOMBIE_INFECTED` level event, and the guard stopping a zombie killer from double-converting.

### Three Mojmap shapes differed from Voile's Yarn

All caught by checking `build/moddev/artifacts/neoforge-21.1.248.jar` first, per the rule from the
previous step:

| Voile (Yarn) | Overgrown/Mojmap 21.1.248 |
|---|---|
| `setOfferData(villager.getOffers().toNbt())` | `setTradeOffers(MerchantOffers)` — takes the object, not NBT |
| `setXp(villager.getExperience())` | `setVillagerXp(villager.getVillagerXp())` |
| `WorldEvents.ZOMBIE_INFECTS_VILLAGER` | `LevelEvent.SOUND_ZOMBIE_INFECTED` |

### Remaining — 8 errors

**`flip_model`** (2 uses: `common/flip_entity`, `amanojaku/this_side_down`) — renders the holder
upside-down. **`zombie_arms`** (`jiang_shi/zombie_arms`) — arms outstretched in third person.
Both purely cosmetic client rendering, and the last real unknowns; server verification cannot judge
them, so they need visual confirmation.

**`enchantment_vulnerability`** — deferred by agreement.

Plus the 4 non-power items unchanged since Phase 3: `is_closed_eye` predicate,
`vengeful_spirit_calculate`, `voile:craftable`, one condition in `ghost/withering_soul`.

## Phase 4 step 9 — the rendering pair (2026-09-02)

`voile:flip_model` and `voile:zombie_arms` retired. **Errors 8 → 5, powers 384 → 387.**
Every Voile type Touhou uses is now ported bar the one deferred by agreement.

### `flip_model` rides on vanilla's easter egg

Voile's neatest trick, and it survived the version jump untouched.
`LivingEntityRenderer.isEntityUpsideDown` is the single question the renderer already asks — it is how
Minecraft renders anyone named **Dinnerbone** or **Grumm** inverted. Answering it differently flips
the model with no transform work at all.

Subtlety preserved: an entity *genuinely* named Dinnerbone is excluded, because vanilla already flips
it and flipping twice leaves it upright.

### `flip_view` needed a real redesign — Voile's approach does not exist in 1.21

Voile multiplied a 180° Z rotation onto the `MatrixStack` that 1.20's `renderWorld` received.
**1.21's `renderLevel(DeltaTracker)` takes no matrix parameter at all.** It does still build one
internally, which is what we wrap:

```java
Quaternionf q = camera.rotation().conjugate(new Quaternionf());
Matrix4f matrix4f1 = new Matrix4f().rotation(q);   // <- @ModifyExpressionValue here
```

That matrix feeds both the level renderer and the held-item renderer, so pre-multiplying a 180° roll
rolls the whole view. **`rotateLocalZ` is deliberate** — it pre-multiplies; post-multiplying would
rotate in world space and give a quite different (wrong) result.

Why `flip_view` matters: `amanojaku/turnabout` grants `common/flip_entity` to a *victim*, combining
the flipped model, the flipped view, inverted Y velocity (they fall upward) and
`prevent_flying_kick`, auto-revoked after 200 ticks. That is also the answer to why
`prevent_flying_kick` exists at all — without it the server boots the victim for flying.

### `zombie_arms`

Tail of `HumanoidModel.setupAnim`, damping the computed arm pitch to 5% and offsetting by
≈ −81° (Voile's constants — they keep a trace of the walk animation rather than locking the arms
rigid). Skipped while swimming, elytra-flying or using an item, since those poses drive the arms
themselves.

Restating for the record: this is **not** `apoli:show_both_arms`, the lookalike the type sweep caught.
That hooks first-person item rendering; this is the third-person biped model.

### Verification limits

The server run proves only that the power types parse — it cannot load client rendering classes, so
all three client mixins are absent from its applied list. Expected, not failure. The same already
applies to `LocalPlayerItemSlowdownMixin` and (for a different reason) to
`ServerGamePacketListenerImplMixin`, which needs a connected player.

**Needs visual confirmation.** What correct looks like:

- `amanojaku/this_side_down` — inverted to others, own view upright.
- `jiang_shi/zombie_arms` — third person, arms straight out in front, still swaying slightly.
- `amanojaku/turnabout` on a victim — model inverts, camera inverts, gravity reverses ~10s.

`flip_view` is the least certain thing written in this port: the multiply order is right in
principle, but only rendering confirms it. A view that rolls the wrong way, or a world that looks
mirrored rather than inverted, is the multiply order and a one-line fix.

### Remaining — 5 errors, and none of them are Voile power types

1. `voile:enchantment_vulnerability` (`jiang_shi/pestilence_smite`) — deferred by agreement; needs a
   hook into 1.21's data-driven enchantment damage path.
2. `is_closed_eye` predicate — no vanilla way to query an origin inside a loot predicate.
3. `vengeful_spirit_calculate` — Overgrown's resource command has no `operation` subcommand; the
   promising route is an `apoli:change_resource` expression.
4. `voile:craftable` item condition (`celestial/lavish_lifestyle`).
5. one condition in `ghost/withering_soul`.

---

## Keybind and convention-tag audit (2026-09-05)

Triggered by Eileen relaying that the original mod had "10 different keybinds", and that closed-eye
satori should not be able to use ranged weapons. Both reports were correct, and chasing them found
three defects that every clean-load check had passed over.

### Apoli's default active key is not Origins'

`dev.overgrown.apoli.data.Key.PRIMARY_ACTIVE` is `key.apoli.primary_active`. Fabric Apoli defaulted
to `key.origins.primary_active`. Any power that omits `key` therefore moves between platforms.

Fifteen Touhou powers omit it. On this platform they landed on Apoli's key, which ships **unbound**
(`key.keyboard.unknown`), in a different controls category from every other binding — so they could
not be triggered until the player happened to bind a key they had no reason to think was involved.

This also explains a test report from 2026-09-02 that was never accounted for: kappa's
`optical_camouflage` had its `toggle` on `key.origins.primary_active` and its companion `sound` on
the Apoli default. One key toggled invisibility; a different key played a sound with nothing
attached to it. Read at the time as "second keybind plays a random noise".

All fifteen now carry explicit keys.

### Keybinds are data-driven, and Origins ships ten

`data/<ns>/keybinds/<path>.json` → translation key `key.<ns>.<path>` (`Keybind.translationKey()`).
Apoli ships 2 (`primary_active`, `secondary_active`, both unbound). **Origins ships 10** — primary,
secondary, ternary … denary — with primary bound to `G` and secondary to `H`, the rest unbound.
That is the "10 keybinds" Eileen's BF remembered.

We shipped none, so `key.touhouorigins.primary_extra` and `secondary_extra` resolved to null via
`ApoliKeyMappings.resolve`, and both shikigami powers were unreachable. Upstream Fabric shipped no
registration either — only the lang strings — so this is a platform difference, not a port omission.

Toggles now sit on `key.origins.ternary_active` at Eileen's request. Global `spiritual_gaze` stays
on its own extra key: every player has it regardless of origin, so it would collide with an origin's
own toggle.

### Fabric convention tags are not NeoForge convention tags

`c:bows` and `c:shields` are Fabric names. NeoForge ships `c:tools/bow`, `c:tools/crossbow`,
`c:tools/shield`, `c:tools/ranged_weapon`, `c:tools/spear`.

**An ingredient naming an unknown tag loads silently** — no error, no dropped-field warning. It
simply matches nothing. This is a different failure mode from `#c:spears`, which errored loudly in
Phase 3, and it is why the defect survived every "0 errors" check.

Consequences: closed-eye satori's `distracted_state_of_mind` blocked nothing at all, and wolf
tengu's `steadfast_defense` shield-slowdown exemption never applied.

Upstream's own description string reads "You are unable to use bows, crossbows, and shields", so
`c:tools/crossbow` restores documented intent rather than widening it. `c:tools/ranged_weapon` was
rejected because it includes tridents, which upstream did not block.

**Rule going forward:** absence of an error proves nothing about a tag reference. Verify the tag
exists in the NeoForge jar, or verify the behaviour in game.

### Things checked and found already correct

- `OriginUpgrade.condition` is an Apoli `EntityCondition` object, not Fabric's bare advancement id.
  The port's `{"type": "apoli:advancement", ...}` rewrite of `satori.json` is right for this
  platform.
- `upgrades` and `unchoosable` are both supported; `OriginUpgrades.tick` runs every 20 ticks.
  Closed-eye is therefore permanent while the player stays satori — re-upgrading within a second —
  but is lost by switching to an unrelated origin. That matches upstream; Eileen has been asked
  whether she wants it to survive any origin change.
- A per-file diff of every power's `type` values against upstream found 20 files changed and **all
  20 are deliberate substitutions** (Voile → Apoli equivalents, Pehkui → attribute modifiers). No
  silent omissions.
- Every origin's power list matches upstream exactly.
- `unconscious_manipulation` keeps its at-will invisibility toggle, unchanged, at Eileen's call. Its
  description documents "at will", so the toggle is the author's intent.

### Per-origin keybind and badge audit (2026-09-05, same session)

Eileen's screenshot: `fairy/fluttering_wings` showing three keybind badges, two identical.

`BadgeManager.badgesFor` merges badges from **every sub-power** of an `apoli:multiple`, with no
deduplication, and `autoBadges` emits one per `TogglePower` / `ActionOnKeyPressPower`. So a power
with three keyed sub-powers shows three badges regardless of how many distinct keys are involved.
`fluttering_wings` has `toggle` (ternary), `flight` (key.jump) and `hunger` (key.jump) — the last two
are one gesture, hold-jump, split into levitation and hunger drain. Correct mechanically, wrong in
the tooltip. Upstream had the same duplication.

Fix: a power may carry a top-level `"badges": [...]` array, which `BadgeLoader` puts in `BY_POWER`
and which **overrides** the auto-merge entirely. Applied to `fluttering_wings` (toggle + one active),
`optical_camouflage` and `undefined_flying_youkai` (single toggle each; their `sound` sub-power rides
the same key as the toggle). Confirmed by the loader line going from 1 to 4 powers with inline
badges — a badge that failed to parse would drop out and lower that count.

One genuine cross-ability collision: amanojaku ran `turnabout` and `this_side_down` both on
`key.origins.primary_active`, disambiguated only by `apoli:sneaking` / `sneaking inverted`. That
sneak pair existed solely to share one key. `this_side_down` moves to ternary — it is a self-toggle,
so the key is semantically right — and both sneak gates are removed. `turnabout` keeps its
`cost_of_misfortune_resource >= 1` gate, which is the bar Eileen could not work out how to charge.

Audit result: every origin now has at most one ability per key, and one badge per ability.

### Tag reference sweep

All 39 tag references across the datapack resolve. Two methodology notes for whoever repeats this:

- **Vanilla tags are not in `minecraft_1.21.1_server.jar`** — that jar contains zero
  `data/minecraft/tags/`. They live in
  `build/moddev/artifacts/neoforge-21.1.248-client-extra-aka-minecraft-resources.jar`. A sweep
  against the server jar reports every vanilla tag as missing.
- NBT blobs also live under a `"tag"` key (item icons carry `{CustomPotionColor:...}`), so a naive
  scan of `"tag"` fields produces false positives. Skip values starting with `{`.

---

## TLM character models via apoli:custom_model_render (2026-09-05)

Eileen's idea: per-origin player-model features (wings, horns, third eye), no items required.
Expected to need a custom RenderLayer mod; it does not — **Overgrown's Apoli ships this as data**:

- `apoli:custom_model_render` — GEOMETRY mode renders a Bedrock-format model
  (Blockbench export, parsed by `BedrockModelParser`) with a texture on the player, with scale,
  color, animations, `hide_cape`, `hidden_slots`, `show_first_person`. Models load from
  `assets/<ns>/geo/` or `assets/<ns>/models/apoli/`; the id is the path minus prefix and suffix.
- Related: `modify_model_parts`, `prevent_feature_render` (can hide the elytra feature for the
  crow-tengu wings idea), `model_color`, `player_model_type`.
- The wing resource packs Eileen supplied are OptiFine CIT item retextures — item-bound and
  non-functional on NeoForge; useful only as texture art references.

**Touhou Little Maid** (TartaricAcid) supplies ready-made character models. Licensing splits
cleanly: code MIT, **assets CC BY-NC-SA 4.0** — personal use fine, publishing not, hence:

### The strippable-personal-content architecture

`src/personal/resources/` is a second resource root added to the main source set **unless**
`-PstripPersonal` is passed. Rules that keep the strip clean:

- Nothing under `src/main` may reference anything under `src/personal`.
- Cosmetic powers attach through their own **global power set** file
  (`global_powers/tlm_cosmetics.json`) with per-origin `origins:origin` conditions — never by
  editing an origin's power list, which would leave a dangling reference when stripped.
- Verified both ways: personal jar carries 9 tlm entries, `-PstripPersonal` jar carries 0, and
  both build clean.

### TLM model format is mixed-era

`komeiji_satori.json` is legacy 1.10 (`geometry.model` top key, `texturewidth`); `ibuki_suika.json`
is already modern (`minecraft:geometry` + `description`). Apoli parses only the modern form. The
conversion is a mechanical rewrap (bones carry over untouched); script handles both cases.

### Unverified — needs eyes on a client

Server proves parsing only (415 powers, 2 global sets, 0 errors). Not yet seen: whether the model
renders positioned/scaled sensibly over the player, whether TLM bone names (`armLeft`, `head`, …)
map onto vanilla part tracking (`ModelParts.slot(normalize(name))` — unmatched bones will log
client-side warnings), and how it composes with Pehkui scaling and amanojaku's flip. First client
run: watch the log for `[Apoli] Loaded 2 custom model(s)` and bone warnings.

Also unresolved: whether to hide the vanilla player model under the chibi model, and which of
`modify_model_parts` / `body_parts` does that job — deliberately left for visual iteration.

### Partial attachment beats full-model replacement (2026-09-05, later)

Eileen's follow-up: attach only identifiable parts (oni horns, satori's strings), not a whole
character model. Both questions answered from source.

**Does the custom model scale with the player?** Yes, automatically. In
`LivingEntityRenderer.render` the order inside a single push/pop is: `entity.getScale()` ->
`poseStack.scale(...)` (line ~98) -> main model -> **`for (RenderLayer ... : this.layers)`**
(line ~132). `CustomModelRenderLayer` is one of those layers, so it inherits the scale transform.
Pehkui on 1.21 drives the vanilla `generic.scale` attribute, so Player Scale sizes carry through
too. The power's own `scale` field multiplies on top of that.

**Which bone names matter?** `ModelParts.SLOTS` maps normalized names onto vanilla parts:
head/hat/body/rightarm/leftarm/rightleg/leftleg, plus aliases (`armright`, `torso`, `jacket`,
`headwear`, ...). `GeometryRenderer.syncPlayer` binds bones by those names only. TLM's rigs already
use `head`, `body`, `armLeft`, `armRight`, `legLeft`, `legRight`, so they bind with no renaming.

TLM bones worth harvesting, found by walking the hierarchy:

- `ibuki_suika`: `hornLeft` / `hornRight` under `head` (6 bones each, tapered segments + `tip`).
- `komeiji_satori`: `eye` under `body` -- 4 cubes for the third eye, with `line`..`line4` cable
  chains hanging off it (~55 cubes total).

**How attachment works.** A bone named after a vanilla part, carrying no cubes, acts as a tracker;
`pose()` adds that player part's deviation from a rest pose to the tracker's baked transform, so
children follow the limb. `src/personal/tools/extract_tlm_part.py` copies a subtree, re-roots it
under such a tracker, and fixes coordinates.

Two coordinate facts that make or break it:

- Bedrock pivots are absolute; `BedrockModelParser` converts them to parent-relative offsets, with
  root bones measured from `BEDROCK_FLOOR` = y 24.
- **TLM maids are chibi**: head pivot y 18, body pivot y 10.5, where a vanilla player has both at
  y 24. Every pivot and cube origin in an extracted subtree must be lifted by the difference
  (+6 for head parts, +13.5 for body parts) or the piece renders sunk into the player.

**Do not use `body_parts` to filter to a sub-part.** `applyVisibility` sets every bone invisible
then re-shows only exactly-named ones, and an invisible parent hides its children -- so listing
`head` shows the head bone's own cubes and none of its descendants. Filtering to a feature means
authoring a model that contains only that feature, which is what the extractor produces.

`RenderMode` accepts translucent, translucent_cull, cutout, cutout_no_cull, solid, emissive, eyes
(plus glow/glowing/translucent_emissive -> emissive, eyes_emissive -> eyes). Attachments use
`cutout`.

Still unverified: rendering itself. Server load proves parsing only (415 powers, 2 global sets,
0 errors). The lift is a proportion approximation -- TLM heads are relatively larger, so horizontal
spread and size will probably want a `scale` tweak after the first look.

### Regression: toggles landed on a key that ships unbound (2026-09-05)

Eileen reported closed-eye satori's invisibility and spooky sounds dead in the latest build; her
friend reported "no keybinds".

Cause, mine. Moving toggles to `key.origins.ternary_active` satisfied the "toggles get their own
key" request, but **Origins ships ternary through denary with `key.keyboard.unknown`** -- only
primary (G) and secondary (H) carry defaults. `ApoliKeyMappings.probe` returns `Probe.UP` for any
key whose value is negative, so an unbound key is permanently un-pressed. The toggle was not
broken, it was unreachable, which looks identical from inside the game.

Fix: stop borrowing Origins' unbound slots. This mod now ships its own keybinds with real defaults
-- `touhouorigins:toggle` (J), `primary_extra` (K), `secondary_extra` (N) -- and the five toggles
point at `key.touhouorigins.toggle`. G, H, J, K, N sit together on the row. J/K/N verified free
against the vanilla keybind table in `Options.java`; **L is vanilla's advancements key** (76) and F
and P are also taken, so avoid those.

Existing bindings survive: `DynamicKeyMappingManager.restoreSavedBinding` reads options.txt, so a
player who already rebound something keeps it. Defaults only fill the unbound case.

**Rule going forward: never point a power at a keybind whose default is `key.keyboard.unknown`.**
An audit that every keyed power resolves to a bound key is now cheap -- walk the powers, collect
distinct key strings, check each against a keybind file with a real default.

Also worth knowing, and a likely part of the friend's report: Apoli's data-driven keybinds are
**session-scoped**. They register when the client receives `SyncKeybindsS2C` on world join and are
removed on disconnect, so `Options -> Controls` shows none of them from the title screen. That is
normal, not a fault.

Not the cause, checked and cleared: `paranoia` (the spooky sounds) has been on
`key.origins.secondary_active` since Phase 3 and was untouched by the keybind work -- `git log -p`
over the folder shows the ternary line as the only change in `58f7255`. On H it should have kept
working; the "no keybinds" half of the report is the better explanation for both powers failing at
once.

---

## Two silent-failure bugs from Eileen's play session (2026-09-05, late)

### Every touhouorigins: sound was dead, since Phase 3

`sound/ModSoundEvents.java` is listed in CLAUDE.md's Java surface as "Mechanical" — and was never
ported. Apoli's `PlaySoundAction` opens with:

```java
SoundEvent registered = BuiltInRegistries.SOUND_EVENT.get(cfg.sound);
if (registered == null) return;
```

**It returns, silently.** An earlier note in this file claimed play_sound "falls back to raw ids";
that was wrong, written while skimming this method during the Blessings work, and it is now
corrected. There is no fallback. Every `apoli:play_sound` naming a `touhouorigins:` sound has been
a no-op since the data was migrated — the power fires, nothing is heard, and from inside the game
that is indistinguishable from a power that never ran.

This is the missing half of the closed-eye satori report: the invisibility failure was the unbound
key, and *the spooky sounds were this*. Two unrelated silent failures wearing one coat.

Why it hid so long: our own `ModifyFootstepSoundPower` and `TeleportToEntityAction` both do
`get(...)` then fall back to `SoundEvent.createVariableRangeEvent(id)` when it misses. Footsteps
therefore worked, which made the sound system look healthy.

Ported as a `DeferredRegister<SoundEvent>` over the 37 keys in `assets/touhouorigins/sounds.json`.
That set was diffed programmatically against upstream's `registerSound(...)` calls — identical, so
the list is complete by construction rather than by eye. Verified positively by extracting the
string constants from the compiled class in the shipped jar and comparing to sounds.json: exact
match, 37/37.

### Custom models need the .geo.json suffix

`CustomModelManager` scans prefixes `geo/` and `models/apoli/` but with
`SUFFIX = ".geo.json"`. Our files were plain `.json`, so **no model ever loaded** and the cosmetic
powers rendered nothing. Zero errors, because a model that is never found is never parsed.

GeckoLib (bundled with Touhou Little Maid, which Eileen has installed) was reporting the answer:
`Found geo model file with improper file name format; geo model files should end in .geo.json`.
Its warning about our files named the exact convention Apoli also requires.

Fixed by renaming to `.geo.json` and moving from `geo/` to `models/apoli/`. The id is the path
after the prefix minus the suffix, so `touhouorigins:tlm/oni_horns` is unchanged and the powers
needed no edit. `models/apoli/` also keeps our files out of GeckoLib's scan, so the cross-mod
warnings stop.

### Not bugs — checked and confirmed faithful

- **Crow tengu's toggle only showing/hiding wings is correct.** `youkai_of_the_sky` holds two
  `apoli:elytra_flight` sub-powers, `elytra_visible` and `elytra_hidden`; the toggle picks between
  them. Flight is always on by design.
- **Vampire water damage needs rain or *flowing* water.** The condition is
  `in_rain OR (block_in_radius, fluid still inverted, block water, radius 0)` — byte-identical to
  upstream. Standing in a still pool does nothing; only the night-vision loss applies there.
  `origins:hurt_by_water` exists in Origins' jar, so the damage type resolves.
- **Fairy flight**: `fluttering_wings` is identical to upstream apart from the key, and its toggle
  carries `active_by_default: true`. Pressing the new J key turns it off, which is almost certainly
  what happened while testing the toggle key.
- `touhouorigins:probe` damage type is used by `kappa/soul_snatch`, not leftover debug.

### Fairy hold-to-fly: the default 1-tick cooldown gates a continuous key (2026-09-05)

Eileen: toggling the wings back on restored the float, but holding space no longer sustains a
climb — one hop, then drift down.

That symptom is vanilla jump plus the power's own passive `floating` (slow falling) with the
`flight` sub-power's levitation lapsing between applications.

`ActionOnKeyPressPower.Config` defaults `cooldown` to `Expression.constant(1)`, not 0. Both ends
enforce it: `ApoliKeyHandler` skips sending while `ClientPowerState.getCooldown(powerId) > 0`, and
`run()` refuses while the server's own counter is non-zero. So a `continuous` key fires at best
every *other* tick, and the client's counter is set from a server sync, so latency widens the gap
further.

Upstream's `flight` applies levitation for **3 ticks** and specifies no cooldown. On Fabric that
was fine; here the best case leaves one tick of margin and multiplayer latency spends it, so the
effect expires and the player falls. Nothing about the file changed in the port — it is identical
to upstream apart from the toggle key — the platform's default differs.

Fix: `"cooldown": 0` on the `flight` sub-power, which removes the gate on both ends so it fires
every tick the key is held. Duration stays at upstream's 3. `hunger` keeps `cooldown: 20`, which is
what meters the 1-hunger-per-second cost.

Only these two sub-powers use `continuous` anywhere in the datapack, so the audit is complete.

**Generalisation worth remembering:** on this platform, a `continuous` key with no explicit
cooldown cannot fire every tick. Any power that reapplies a short-duration effect while a key is
held needs `"cooldown": 0`, or an effect duration longer than the gate plus latency.

Unverified: this is reasoned from the cooldown code paths, not observed. It needs a client to
confirm.

---

## The 1.21 component migration was never done for item NBT (2026-09-05, late)

Four powers plus the shikigami link were inert because they matched **1.20 root NBT** that no
longer exists. `apoli:nbt` on an item reads only `minecraft:custom_data` — a bag for modder NBT —
so `{display:{Name:...}}`, `{title:Contract}`, `{Potion:"..."}` and `{Charged:1b}` can never match
on 1.21. No error, because a condition that returns false is just a condition that returns false.

Casualties: the whole shikigami bond (so the teleport was never reachable either), oni's
`heavy_drinker`, oarfish's `veils_like_water`, moon rabbit's `military_of_the_moon` reload.

Overgrown's Apoli ships no component-aware item conditions — the registry has `nbt` and
`custom_data` and nothing for names, potions or charged crossbows — so three small ones live here
now:

| condition | reads | replaces |
|---|---|---|
| `touhouorigins:named` | `custom_name`, `written_book_content.title` | `{display:{Name}}`, `{title}` |
| `touhouorigins:potion` | `potion_contents.potion` | `{Potion:"..."}` |
| `touhouorigins:charged` | `charged_projectiles` non-empty | `{Charged:1b}` |

`named` checks both a rename and a book title because the two halves of the shikigami link ask the
same question of different items. Matching is case-insensitive, preserving upstream's acceptance of
`Contract`/`contract`.

The link's `/clear @s minecraft:lead{display:{Name:...}}` commands were 1.20 selector syntax too;
replaced with `apoli:equipped_item_action` + `apoli:consume`, which removes the exact stack that
was used rather than any lead the master happens to carry.

**Sweep result: zero `apoli:nbt` uses remain in the datapack.**

## Water Protection: the legacy shim ate the expression

`onset_delay` scaling never worked. `LegacyPowerShapes.damageOverTime` reads it with

```java
int onsetDelay = power.get("onset_delay").asNumber().result()...orElse(interval);
...
.set("onset_delay", power.createInt(onsetDelay))
```

`asNumber()` on our string expression fails, falls back to `interval`, and then **overwrites the
expression with that constant**. So the enchantment scaled nothing, silently — and the shim's own
warning text recommends exactly the expression form it destroys.

Fix: write the power as `apoli:action_over_time` directly, which takes `onset_delay` as a real
`Expression`, reproducing by hand what the shim would have generated (interval, difficulty-branched
damage, damage type). The shim only rewrites `apoli:damage_over_time`, so a native power is left
alone.

**Rule: never pass an expression to a field on a legacy-shimmed power type.** The shim re-emits
its fields as primitives.

## Third eye placement, and horn variants

The eye sat at the head because aligning the `body` pivots is not the same as matching proportions:
a chibi is ~24 units tall against a vanilla player's 32, and the eye is positioned against the
character's whole torso. Its cubes landed at y 24.5-27.5, above the tracker pivot at the neck line.
`--offset 0 -12.5 0` puts it at y 12-15, which is the waist. The extractor now takes `--offset` for
exactly this, and documents the vanilla reference points: feet 0, waist 12, neck 24, head top 32.

Horn variants per player use a resource as persistent per-player state: `variant` (0 = unrolled),
an `action_over_time` that rolls 1-3 via `apoli:choice` when the holder is an oni and the resource
is 0, and a second that clears it back to 0 when they are not, so changing origin re-rolls while
keeping it stable otherwise. Resources live in the power container attachment, so a roll survives
relog. Geometry from TLM's three canonical oni: Suika, Yuugi and Kasen.

### Wave 1 origin features, and per-player horn variants (2026-09-05)

Seven origins now carry a TLM-derived feature. All head-tracked except the third eye.

| origin | feature | source |
|---|---|---|
| oni | horns, 3 variants | ibuki_suika / hoshiguma_yugi / ibaraki_kasen_2 |
| satori | third eye + cables | komeiji_satori |
| wolf_tengu | ears | inubashiri_momizi |
| bake_danuki | ears | hutatsuiwa_mamizou |
| moon_rabbit | ears | reisen_udongein_inaba |
| earth_rabbit | ears | tewi_inaba |
| amanojaku | horns | kijin_seija |

**Third eye placement.** It first rendered at the neck. Aligning the tracker pivot to the vanilla
body pivot is not enough: a chibi is ~24 units tall against a vanilla player's 32, and the eye is
positioned against the whole torso rather than against the body pivot, so the +13.5 body lift
overshot by about a head. `--offset 0 -12.5 0` puts the cubes at y 12-15 -- the waist. Net lift is
+1.0, i.e. TLM's own coordinates were nearly right all along.

Reference points on a vanilla player, Bedrock units: feet 0, waist 12, neck 24, skull top 32.
Head-mounted pieces should land above 30; all five wave-1 features do.

**Per-player variants, data-only.** `cosmetics/oni_horns` is an `apoli:multiple` holding a
`variant` resource (0 = unrolled, 1-3), an `assign` action_over_time that rolls once via
`apoli:choice` while the holder is an oni and the resource is 0, a `reset` that clears it while the
holder is *not* an oni, and three `custom_model_render` sub-powers each gated on their number.
Resources live on the player, so a roll survives relogs and stays fixed while the origin does, and
clearing on origin change stops a new oni inheriting the previous set. No Java.

**Extractor now reads TLM's own files, either era.** `read_geometry` normalises the legacy
`geometry.model` shape, so there is no converted-copy step and no unused full character models
sitting in the tree -- only extracted parts ship, which also keeps borrowed CC BY-NC-SA content to
what is actually used. Two unused full models were deleted for that reason.

Verified: 426 powers (+11: six new oni sub-powers, five wave-1 powers), 0 errors, 0 dropped
fields, no dangling model or texture references. Strip switch still clean -- 21 tlm entries and 7
cosmetic powers personal, 0 of each with `-PstripPersonal`.

Unverified: how any of it looks. Suika's horns span x -14.2..14.4 against an 8-wide head, so they
may read as oversized; that is the first thing to judge on screen.

### Wave 2 — torso features (2026-09-05)

Seven more, all tracking `body`: crow tengu wings (syameimaru_aya — note the spelling, `sh`
finds nothing), vampire wings (remilia_scarlet), nue wings, fairy wings (cirno), and tails for
wolf tengu, bake danuki and vengeful spirit. Fourteen origins now carry a feature.

**Raw cube extents are meaningless for these.** Every wing root carries `rotation: [0, 60, 0]`,
and cube origins are pre-rotation coordinates the renderer folds into place. A naive min/max over
cube origins reported crow wings reaching 22 units below the feet, which is an artefact of reading
unrotated geometry, not a placement bug. **The meaningful anchor is the root bone's pivot.** After
lift: wings at y 19-21.5 (shoulder blades), tails at 13.5-14.5 (lower back), all at z 3-4, behind
the body. Offsets used: wings -8, tails -10, on top of the +13.5 body lift.

**Two deliberately skipped.**

- `wakasagihime` (oarfish) is a mermaid rig: body pivot at y 0 rather than 10.5, tail at z 28. It
  replaces the legs rather than attaching to a torso, so it needs its own treatment.
- `saigyouji_yuyuko` (ghost) has eight wing groups parented to `sinFloat` / `cosFloat` /
  `_sinFloat` / `_cosFloat` — TLM's own animation driver bones, interpreted by its renderer, not
  data. Left and right of each pair share one pivot because the drivers separate them at runtime,
  so extracted statically all eight would stack in four places.

**Animation is authorable but not imported.** `custom_model_render` takes an `animations` list of
Bedrock `.animation.json` entries with per-bone position/rotation/scale keyframes, a condition,
speed and loop. **TLM ships zero animation files** — it animates through those magic bone names
instead — so there is nothing to lift, and a flap would have to be written from scratch. Held back
deliberately: wings are unverified as static, and a wrong flap on top of a wrong rest pose is two
unknowns at once. Worth doing once placement is confirmed.

433 powers, 0 errors, 0 dropped fields, no dangling asset references. Strip switch: 33 tlm entries
and 15 cosmetic powers personal, 0 of each stripped.

Known cosmetic oddity: Cirno's two wing pivots are both right of centre (x 0.25 and 0.75) in the
source model, so fairy wings may sit slightly askew.

### I dropped `inverted` while swapping the NBT conditions (2026-09-05)

The script that replaced `apoli:nbt` with the new component conditions built fresh dicts and did
not carry `inverted` across. Upstream had it on **five of the six**:

- `moon_rabbit/military_of_the_moon` — the reload fires when the crossbow is **not** charged. That
  inversion is the whole machine-gun mechanic: every tick, if the ability resource is up, the
  crossbow is empty and an arrow is in the inventory, an item modifier reloads it and `/clear`
  takes one arrow. Losing the inversion made it fire only when already loaded, i.e. never usefully.
  Expressed now as `"charged": false` rather than an `inverted` wrapper, which reads better.
- `oni/heavy_drinker` ×4 — water/mundane/thick/awkward bottles are *excluded* from counting as a
  drink; without the inversion only those four counted.
- `oarfish/veils_like_water` was correctly not inverted.

**Lesson: when programmatically replacing a condition, copy the generic wrapper fields
(`inverted`, and anything else Apoli applies outside the type's own codec), not just the payload.**

The crossbow item modifiers themselves were already fine — migrated to 1.21's `set_components`
with `minecraft:charged_projectiles`, in the singular `item_modifier/` folder.

### Blessings kept 1.20 plural data folders

Copying the Blessings datapack verbatim carried `touhoublessings/functions/` and
`touhoublessings/item_modifiers/` across. 1.21 singularised those registry directories, so both
were dead: `function touhoublessings:permadeath` had nothing to run, and the `delete` item modifier
could not resolve. Renamed to `function/` and `item_modifier/`.

Worth re-checking after any verbatim copy from a pre-1.21 pack: advancement, function,
item_modifier, loot_table, predicate, recipe, structure, and `tags/<singular>`.

### Note on power counts

421 -> 433 across these edits looked alarming but was stale build output; a `gradlew clean` build
reports 433 with 0 errors and no duplicate warnings. Compare counts only across clean builds.

### Wing toggle now announces itself

`apoli:toggle` gives no feedback, so "wings folded" and "power broken" look identical — which cost
two rounds of debugging. `fluttering_wings` now shows an actionbar line on each change.

Implemented as a mirrored resource (`wing_state`, `start_value: 1`) watched by an
`action_over_time` that announces when the mirror disagrees with the toggle. The obvious approach —
an `active_self` on the same key reading `power_active` — is wrong here: two powers bound to one
key are dispatched in whatever order `ClientPowerState.localPowers()` iterates, so the message
would report the state from before or after the flip unpredictably. Upstream's kappa camouflage
sound has exactly that latent bug.

### Fairy flight, still open

Eileen confirms sparkles and the flutter sound while descending. Those come from `particles_down`
and `sound_down`, which require `power_active` on the toggle plus `creative_flying` inverted — so
the toggle is **on** and creative flight is **off**, disproving the earlier guess that a stale
toggle was to blame. It works on a fresh world, so it is per-world state.

What that leaves for `flight` specifically, since it shares every other gate with the sub-powers
that do work:

- `food_level > 0`
- the keypress reaching the server

`flight` is the only one of the working set that is client-dispatched: `active_self` needs
`ApoliKeyHandler` to send `PowerActivationC2S`, while `floating`, `particles_*` and `sound_*` are
`action_over_time` and run server-side regardless. That asymmetry matches the symptom exactly, so
the next thing to test is whether *any* key-driven power works in that world — fairy's
`regeneration` on G is the cheapest probe.

Ruled out so far: `prevent_key_press` (none exists in the datapack, so `BlockedKeys.any` is false),
the toggle, creative flight, and the cooldown gate (`cooldown: 0` is never stored server-side).

### Fairy flight: what is and is not ruled out (2026-09-05, end of session)

Symptom: works in a fresh singleplayer world; in the hosted world it worked at first and then
stopped mid-session. Descending still shows sparkles and plays the flutter sound.

Ruled out, each by evidence rather than reasoning alone:

- **The toggle and creative flight** — `particles_down` and `sound_down` require both, and Eileen
  sees and hears them.
- **Client power dispatch** — fairy's `regeneration` on G is also an `active_self` and works in the
  same world, so `PowerActivationC2S` is flowing.
- **`prevent_key_press`** — none exists in the datapack, so `BlockedKeys.any` is false.
- **The cooldown gate** — `cooldown: 0` is never stored server-side, and the client's copy
  decrements every tick in `ApoliKeyHandler.onClientTick`.
- **Blessings** — the layer arrived mid-session, which fit the timing, but nothing in it touches
  status effects, jump or flight.
- **Packet rate** — `continuous` sends one activation per tick, 20/sec, which singleplayer never
  exercises. It fit "singleplayer works, server does not" but not "the server worked at first".
- **Pehkui's falling scale** — it feeds gravity, and vanilla's levitation branch in `travel()`
  bypasses gravity entirely.

That leaves two, and one command separates them:

    effect give @s minecraft:levitation 3 4

Rising means levitation is fine and the power is not firing, which points at `food_level > 0` —
the only gate `flight` does not share with the sub-powers that work. Not rising means something in
that world suppresses levitation and the power was never at fault.

Meanwhile the silence itself is fixed: `flight_blocked` announces "Too exhausted to fly" when the
key is held with the toggle on, airborne, and hunger at zero. Between that and the wing toggle
message, both invisible failure states now say what they are.

### The two wave-2 holdouts, resolved (2026-09-09)

**Oarfish was a false alarm.** Wakasagihime's *body* is a mermaid rig (body pivot y 0, tail at
z 28), but her **head pivot is the standard chibi y 18**, and she carries `earRight` / `earLeft`
on the head — ear fins. Head-tracked extraction never touches the mermaid body, so it works like
any other head feature. Fins land at x ±4, y 26, z -2: the sides of the skull. The lesson is that
"this rig is unusual" was true of the wrong half of the model; checking the specific attachment
point would have settled it immediately.

**Ghost wings shipped static, at Eileen's call.** Yuyuko's eight wing groups are four pairs, and
each pair shares one pivot exactly, because TLM's `sinFloat` / `cosFloat` driver bones separate
them at runtime. Extracted statically the pairs coincide, so expect four positions with two
identical wings each, and z-fighting where they coincide. After the -8 offset the four sit at
(-9.5, 25.5, 5), (12.5, 15.5, 8), (12.5, 27.5, -8), (-10.5, 14.5, 16) — 9 to 16 units out, so a
scattered halo around the torso rather than wings on a back.

The obvious next step if it reads badly: mirror one of each pair across X to get eight distinct
wings. That is not a plain negation — a cube's origin is its minimum corner, so the mirrored origin
is `-(origin.x + size.x)` and the cube wants `mirror: true` for its UVs. Deliberately not done
blind.

**Also found, unused so far:** Yuyuko has `ghost` and `ghostSymbol` bones parented to the *head* —
a small floating spirit and her spirit sigil, both clean single-pivot subtrees. Probably a better
read on "ghost" than the butterfly wings, and a one-command extraction if the wings disappoint.

Sixteen cosmetic powers across all nineteen origins now (several carry two). 438 powers, 0 errors,
0 dropped fields. Strip switch: 37 tlm entries and 17 cosmetic files personal, 0 of each stripped.

### Ghost: spirit and sigil instead of the butterfly wings (2026-09-09)

Eileen's call after seeing the wings described: the floating spirit reads as the hallmark, the
wings do not. `ghost_wings` removed entirely — model, power and global-set entry — and replaced
with `ghost_spirit`, extracted from Yuyuko's head-parented `ghost` and `ghostSymbol` bones.

Both are clean single-pivot subtrees under `head`, so extraction is ordinary. One adjustment:
the cubes are **flat billboards** (zero depth) sitting at z -4.5, and the vanilla head's front face
is at z -4 — half a unit of clearance, close enough to clip or z-fight. `--offset 0 0 -2.5` moves
them to z -7, so the spirit floats clear in front rather than stuck to the forehead. Final extent
x -2.5..2.5, y 31..36, i.e. just above the skull top at 32.

Worth noting the wings were never wrong so much as unrecognisable — four coincident pairs scattered
in a halo. The lesson is not about geometry: shipping the imperfect version was what made the
better option visible, and the better option had been sitting in the same model all along.

438 powers, 0 errors, 0 dropped fields. No dangling asset references, no unreferenced models, and
the cosmetic power files match the global set exactly.

### Crow tengu: modelled wings replace the textured elytra (2026-09-09)

The shipped mod gives crow tengu elytra flight rendered as a textured elytra
(`crow_wings.png`), with a toggle switching between `elytra_visible` and `elytra_hidden`. Now that
a modelled pair exists, the toggle should drive the model and the textured elytra should be
unreachable.

**This needed the personal root to override, not merely add.** The elytra power lives in
`src/main`; the modelled wings live in `src/personal`. Hiding the elytra in `src/main` would leave
a `-PstripPersonal` build with no wings at all, since the model is stripped — a regression in the
publishable jar caused by a personal-only feature.

`src/personal/resources` is now registered as the *last* resource root with
`duplicatesStrategy = INCLUDE` on `processResources`, so a personal file replaces its `src/main`
counterpart at the same path. Verified both directions from the built jars:

| build | sub-powers | textured elytra reachable |
|---|---|---|
| personal | `toggle`, `flight` | no |
| `-PstripPersonal` | `toggle`, `elytra_visible`, `elytra_hidden` | yes |

So the shipped mod keeps its original behaviour untouched and the personal build swaps in the
model. This mechanism generalises: any future personal change to shipped behaviour goes in as an
override rather than an edit to `src/main`.

The toggle gained `active_by_default: true`, so a crow tengu has wings without pressing anything.
That also makes the existing description — "You can hide your wings by toggling this power" —
accurate for the first time; upstream's toggle defaulted to inactive, so wings were hidden until
you toggled them *on*, the opposite of what it says.

Sub-power ids are `namespace:path_key` (`ApoliReloadListener.subPowerId`), so the wings' gate reads
`touhouorigins:crow_tengu/youkai_of_the_sky_toggle`. Checked against the loader rather than
assumed, because `apoli:power_active` against an id that does not exist evaluates false in silence
— the same failure shape as the dead tags and the unregistered sounds.

`crow_wings.png` is kept though now unreferenced in the personal build: it is upstream MIT art that
the stripped build still uses.

437 powers (one fewer, the removed elytra sub-power), 0 errors, 0 dropped fields, no duplicate
resource warnings.

### Grid origin picker, ported from AltOriginGui (2026-09-09)

Overgrown's chooser steps through origins one at a time with `<` and `>`. Tolerable for its ten
built-ins; poor for nineteen Touhou origins, and this pack also stacks a nine-entry Blessings layer
and a seven-entry size layer. UltrusBot's **AltOriginGui** (MIT) solves exactly this for apace100's
Origins on 1.20.1 — Fabric/Forge, so not usable as a dependency, but portable.

Ported rather than glued: 6 files became 2, ~250 lines. It went smoothly because Overgrown's
`ChooseOriginScreen` happens to carry the **same private field names** apace100's did —
`originSelection`, `currentOrigin`, `maxSelection`, `randomOrigin`, `layerList`,
`currentLayerIndex` — so the shadowed state maps across untouched. The accessors did not:

| apace100 | Overgrown |
|---|---|
| `origin.getImpact().name()` | `origin.impact().level()` |
| `origin.getDisplayItem()` | `IconRenderer.render(g, origin.icon(), x, y)` |
| `getCurrentLayer().getTranslationKey()` | `layer.name()` |
| `origin.getIdentifier()` | `origin.id()` |

**One upstream bug deliberately not carried over.** AltOriginGui removes the arrow buttons by
suppressing `addRenderableWidget` at ordinals 0 and 1. Overgrown adds the arrows only
`if (maxSelection > 1)`, so on a single-origin layer ordinal 0 is the **Select** button — blind
suppression would leave it unselectable. Ours guards on `maxSelection > 1` inside the wrap, so it
suppresses only when those ordinals really are arrows.

**Verified by disassembly, not assumption.** The `@WrapOperation` targets name
`ChooseOriginScreen.addRenderableWidget`, which is only right if javac emitted the subclass as the
methodref owner rather than `Screen`. `javap -v` on the shipped Origins jar shows
`#202 = Methodref … ChooseOriginScreen.addRenderableWidget` and exactly three call sites in
`init()` — two arrows plus Select — so the ordinals are right too. This build carries no refmap
(it runs on Mojmap), so those literal names are what Mixin resolves at runtime.

Lives in `src/main`, not `src/personal`: AltOriginGui is MIT, so unlike the TLM assets it can ship.
Attribution added to the pack README alongside Maxmani and Voile.

Unverified: whether it renders correctly. Client mixins do not apply on a dedicated server, so the
server run proves only that nothing else broke. 437 powers, 0 errors, no mixin config complaints.

### Kappa hat (2026-09-09)

Kappa was the last origin without a feature. Nitori is `kawasiro_nitori` — spelled with an `s`,
like `syameimaru_aya`; TLM romanises inconsistently, so a name miss is not evidence a character is
absent. Her head carries a `cap` bone: a 9-wide crown plus a flat forward brim.

Placement needed no offset. The crown lands at y 30-33 against a skull topping out at 32, and
x/z ±4.5 against a head of ±4, so it overhangs half a unit all round — proportionally what it was
on the chibi, because a cap is positioned against the skull rather than against the whole body.
That is the opposite of the third eye, which needed -12.5 precisely because it hung off the torso.

**Hidden while a helmet is worn**, via
`apoli:equipped_item{slot: head, item_condition: apoli:empty}`. The alternative — declaring the
head in `hidden_slots` — would suppress the helmet and take away the visual cue that the player is
actually wearing armour. Better for the cosmetic to yield to real equipment than to hide it or
clip through it.

`decoration` (four small cubes at the sides of the head) was left out: it reads as hair clips
rather than hat, and Eileen asked for the hat.

All nineteen origins now carry a feature. 438 powers, 0 errors, 0 dropped fields, no dangling or
unreferenced assets. Strip switch: 39 tlm entries personal, 0 stripped.

### Talisman, Koishi's eye, spirit orbs, variants, and a culling fix (2026-09-10)

**Jiang-shi's talisman was already modelled.** Not painted into the face texture as I assumed —
Yoshika's `hat` carries a `Magic` bone: the paper strip, with `ling1`/`ling2` children built from
tiny cubes forming the 令 inscription. Nothing had to be drawn. It also needed no offset: TLM
already sits it 0.006 in front of a face plane at z -4, which is where a vanilla face is too, and
both heads are 8 tall so the lift alone lands it on the forehead.

Checking the head cube's front face first was what found this. An 8x8x8 cube at uv [40,0] puts its
front face at (48,8) by Bedrock box-UV layout; reading those 64 pixels showed hair and eyes and no
talisman, which is what sent me to the hat.

**Koishi's eye needed aligning, not placing.** It hangs off `sinFloat` at [-11, 4, 12] — parked out
of the way for the driver bone to position at runtime. Rather than guess, the offset was solved
against Satori's: extract, measure the eyeball, and correct by the difference. Both eyeballs now
occupy x 0.5..3.5, y 12..15, z ≈ -6, which is right since it is the same organ, closed.

**Vengeful spirit's orbs are original work.** The tail read as a nekomata, so it is removed. TLM has
no orb geometry anywhere — Orin's `sinFloat`/`cosFloat` roots are childless, and a sweep for
hitodama/spirit/orb/fire/soul/wisp names hit only Yuyuko's ghost. So the model and its 16x16 ember
texture are hand-made: five embers ringing the torso at radius 7.5-9.5 and heights 13-22, each its
own bone so an animation can drive them separately.

Because they are not TLM-derived they live in **`src/main` with their own global power set**, and
survive `-PstripPersonal` — the first cosmetics that ship publicly. Same for haniwa's terracotta,
which is `apoli:model_color` and therefore just numbers.

**Backface culling was hitting most models.** Eileen saw it on the ghost spirit and one oarfish fin.
Two causes, one fix:

- Flat (zero-depth) cubes are single-sided. They are everywhere: 18/18 cubes in `ghost_spirit`,
  53/55 in `satori_third_eye`, 252/274 in `vampire_wings_flandre`.
- `oarfish_fins` has no flat cubes at all — its `earRight` carries `mirror: true`, which flips the
  winding, so that fin culled from the opposite side to its twin. That is why only one disappeared.

All 18 TLM-derived cosmetic powers moved from `cutout` to `cutout_no_cull`. The orbs stay
`emissive`; they are solid cubes and want the glow.

**Variants**, same rolled-once resource mechanism as the oni horns: fairy gets five (Cirno,
Daiyousei, Star Sapphire, Sunny Milk, Luna Child) and vampire two (Remilia, Flandre — hers is 274
cubes of crystal wing, worth watching for cost). Nothing else has more than one plausible source:
only Momiji has wolf ears, only Reisen has moon-rabbit ears, only Aya has crow wings.

454 powers, 3 global power sets, 0 errors, 0 dropped fields. Strip: 51 tlm entries and 22 cosmetic
files personal, 0 tlm and 4 cosmetic files stripped.

### Fairy flight: solved, and it was never our bug (2026-09-10)

Eileen found it. **Arcadian Dream's Hourai elixir cancels all negative effects**, and vanilla
classes `LEVITATION` as `MobEffectCategory.HARMFUL` (`MobEffects.java`:
`new MobEffect(MobEffectCategory.HARMFUL, 13565951)`). `fluttering_wings` flies by applying
levitation, so the elixir strips it the moment it lands.

Every symptom follows from that one fact, including the ones that made it look like our port:

- **Worked at first, stopped later in the same world** — she drank the elixir in between.
- **Fine on a fresh singleplayer world** — no elixir there. This looked like per-world state, and
  sent me hunting Player Scale, packet rate and client dispatch.
- **Sparkles, flutter sound and the drift down survived** — `SLOW_FALLING` is `BENEFICIAL`, so the
  cleanse leaves it alone, and `particles_down`/`sound_down` key off it.
- **Regeneration on G kept working** — it applies a beneficial effect.

The one gate `flight` did not share with the sub-powers that still worked was `food_level`, so I
kept returning to hunger. The real asymmetry was not a *condition* at all, it was the **category of
the effect each power applies**. Worth remembering: when some powers of one origin survive and
others do not, compare what they *apply*, not only what they *check*.

Not fixed, deliberately — it is a third-party interaction and arguably correct behaviour, and the
fix is a design choice for Eileen. If it should be made immune, `apoli:add_velocity`
(`{x,y,z,reference,set}`) reproduces the climb without an effect for a cleanse to remove; vanilla
levitation drives y toward `0.05 * (amplifier + 1)`, so amplifier 4 corresponds to a target of
0.25/tick.

Also worth noting: `flight_blocked` ("Too exhausted to fly") correctly stayed silent throughout,
since hunger was never the cause. The message that would have misled is the one I did not add.

### Animations (2026-09-10)

Six clips in one file, `assets/touhouorigins/animations/cosmetics.animation.json` →
`touhouorigins:cosmetics`. `AnimationManager.strip` trims the `animations/` prefix and the
`.animation.json` suffix, and `get(file, name)` is an exact map lookup, so the entry's `name` must
be the literal key in the file's `animations` object (`animation.glide`, not `glide`).

| clip | length | drives |
|---|---|---|
| glide | 0.9s | big wings while `apoli:fall_flying` |
| idle | 3.2s | big wings otherwise, barely moving |
| flutter | 0.34s | fairy wings, always |
| sway | 2.6s | tails |
| drift | 4.0s | third eyes and the ghost |
| embers | 3.6s | the five orbs, each on its own phase |

**Values are deltas, not poses.** `AnimationPlayer.apply` does `part.xRot += …` and
`part.xScale *= …`, so a wing keeps its 60° base yaw and the flap rides on top — writing absolute
rotations would have flattened that yaw and swung the wings to the wrong place. Position is
additive too but **Y is subtracted** (`part.y -= …`), because Minecraft model space has +Y down
while Bedrock animation +Y is up; a positive Y keyframe therefore reads as upward.

**Entry order matters.** `ModelAnimation.select` returns the *first* entry whose condition passes,
so conditional entries come first and the unconditional fallback last. With none passing, Apoli
logs that the model stayed in its bind pose — the big wings therefore list `glide` (conditioned on
fall_flying) then `idle` with no condition.

Two other traps Apoli warns about and this avoids: a clip with a single keyframe per channel is a
static pose, and Blockbench's "Play Once" writes no loop flag so the model snaps back to bind pose
after one cycle. Every clip here declares `loop: true` and has more than one keyframe per channel,
both asserted from the built jar.

Verified by cross-check rather than by eye: for all 19 animated renders, the clip's bone names
(normalised the same way `ModelParts.normalize` does) were intersected with the referenced model's
bones. Every one matches; none is empty. That is the failure this would otherwise have — a clip
naming a bone the model does not have simply does nothing.

Flap direction is the remaining unknown. It is roll (Z), mirrored between `wingLeft` and
`wingRight`. If the sign is inverted the wings will beat in antiphase, which is one negation to
fix.

454 powers, 0 errors, 0 dropped fields.

### Feedback round: z-fighting, a parked rotation, animation polish, and dead icon NBT (2026-09-10)

**Kappa hat z-fighting.** The vanilla skin's *hat overlay* is the head box extended by 0.5
(`HumanoidModel.createMesh`, `cubeDeformation.extend(0.5F)`), so it spans exactly ±4.5 — precisely
where the crown's sides sat. Coplanar faces, hence the fight, and only for skins that use the
overlay, which is most. Fixed with a new `--inflate` on the extractor: 0.3 puts the crown at ±4.8,
clear of the boundary. **Reference planes worth remembering: head ±4, head overlay ±4.5, body
±4/±2, jacket overlay ±4.25/±2.25.**

**Jiang-shi talisman**, same class. It sat 0.006 off the face plane — TLM's own clearance against
*their* head, too fine to survive depth precision here. Pushed to z -5.61..-4.61, clear of both the
face at -4 and the overlay at -4.5.

**Koishi's eye was not misaligned, it was rotated.** Its bone carries `rotation: [40, -60, -35]`
while Satori's carries none, because `sinFloat` re-aims it at runtime. Aligning the cube *origins*
therefore aligned pre-rotation coordinates, and the rotation then swung the geometry away about a
pivot sitting ~12 units from it. New `--drop-root-rotation` zeroes rotation on extracted roots only;
children keep theirs, since those build the shape. Same lesson as the wing extents: on a rotated
chain, cube coordinates are not positions.

**The orb "teleport" was a loop seam.** Keyframes were generated at `(phase + k*L/4) % L`, so a
phase-shifted orb had no keyframe at t=0 or t=L and the sampler jumped at the wrap. Replaced with a
real sine sampled at fixed times, which makes `value(0) == value(L)` by construction — asserted in
the generator rather than eyeballed.

Also: tails no longer animate, fairy wings flutter only while the wing toggle is on *and* the
player is off the ground (with a still fallback so Apoli does not log a bind-pose warning every
frame), crow tengu holds a spread glide pose instead of beating, and haniwa's tint moved from
red toward orange by raising green (0.55 → 0.72) rather than dropping red.

### Icon NBT was 1.20-era across three files, silently

`ItemStackData` takes `item` / `amount` / `components`. The blessings' icons, and two Touhou ones,
still carried a 1.20 `tag` string — and **a RecordCodec ignores fields it does not know**, so those
icons had been rendering as plain default items all along. Nine blessings showed a generic Steve
head instead of their characters; amanojaku lost its potion colour; wolf tengu lost its banner.

Converted: `SkullOwner` → `minecraft:profile` (`id` int-array plus a `properties` list),
`CustomPotionColor` → `minecraft:potion_contents.custom_color`, `BlockEntityTag.Patterns` →
`minecraft:banner_patterns`, with short codes resolved from `BannerPatternFormatFix` rather than
recalled.

The banner then failed loudly, which was useful: **Apoli parses components with plain JsonOps**, so
a `Holder<BannerPattern>` cannot resolve a registry id and falls through to
`BannerPattern.DIRECT_CODEC`, which wants `{asset_id, translation_key}` inline. `components` is a
`LoggedOptionalField`, so that surfaced as a dropped-field warning rather than an error — worth
grepping for on any component work, since the silent-ignore path above gives nothing at all.

454 powers, 39 origins, 0 errors, 0 dropped fields.

**Environment note:** verification kept failing on world locks and port binds. Neither was ours —
a parallel task held port 25599, and orphaned server JVMs from earlier runs outlived their Gradle
wrappers and kept `run/<world>/session.lock`. The four running java processes were all
PrismLauncher, i.e. Eileen's own clients, so nothing was killed; each run moved to a fresh
level-name and port instead.

### Shikigami: the powers are servant-only, and one of them was inverted (2026-09-10)

Eileen: "link works, every other power did not." Two separate things.

**By design, all four extras belong to the servant.** `boundless_servitude`'s link sets
`*:*_boolean = 1` in an `actor_action`, and `apoli:add_to_set` (verified in
`AddToEntitySetAction.run`) adds the **target to the actor's set**. The actor is whoever held the
`Contract` book — the servant. Every other power gates on
`global/boundless_servitude_boolean == 1`, so the master, who held the `Tether`, gets none of them
and is only the anchor. Testing from the master's side looks exactly like "nothing works".

Wildcards were checked rather than assumed: `IdWildcards.expand` maps a leading `*:` to the file's
namespace and `*:*` to its full id, so `*:global/boundless_servitude_boolean` and
`*:*_boolean` resolve to the same id. Those references were fine.

**`linked_by_the_soul` was genuinely broken, and backwards.** Voile's `action_on_entity_death`
fires on *any* death and then searches for holders whose bientity condition accepts the deceased —
so the shikigami reacts to their **master's** death. Apoli's `apoli:action_on_death` fires only for
the **holder's own** death, with the killer as actor. Ported onto it, the power did nothing when
the master died, and instead dealt a million damage to whoever killed the servant.

Restored as `touhouorigins:action_on_entity_death` plus a `LivingDeathEvent` handler at LOWEST
priority (so a cancelled death never triggers a lethal reaction). The handler walks the player list
rather than Voile's every-living-entity-in-every-world scan, since the power is granted through a
global set scoped to `minecraft:player`.

**A guide book is now given to both sides when the pact is sealed** — six pages covering which role
each player ended up in, the servant's keys and costs, the shared death, and how to break the pact.
This confusion is precisely what it exists to prevent.

### Blessings: only the sound was missing

Every `touhoublessings:` reference in its data resolves — origins, powers, lang. The one gap was
that its sound was never registered, the same miss as the `touhouorigins:` ones: Apoli's
`play_sound` returns silently when the id is absent from `BuiltInRegistries.SOUND_EVENT`, so
Moriya's bell could never ring. A second `DeferredRegister` now covers the `touhoublessings`
namespace.

### Model fixes from the same round

- **Ghost sigil** fought itself: `ghostSymbol` and its `bone53` child are two layers of flat
  strokes, the second rotated 45°, all at identical z. Separated by 0.12.
- **Crow tengu** now holds a fixed 30° glide pose while `fall_flying`; the previous clip's
  few-degree "breathe" read as flutter.
- **Koishi's cables** were aligned by the eyeball, which was the wrong landmark — the closed eye
  sits off the shoulder in the source rig. Aligning the *cable bundle's* centre onto Satori's
  (1.26, 14.51, 1.29) wraps them round the torso as intended.
- **Second jiang-shi talisman** from Qiqi's ofuda, as a variant. Per-face `uv`/`uv_size` lets a
  16x48 image sit on a 3x9 quad — without it Bedrock samples one texture pixel per model unit and
  the art would be a 3x9 smudge. Genshin art, so personal-tree only.

459 powers, 39 origins, 0 errors, 0 dropped fields. Strip: 53 tlm entries personal, 0 stripped.

### Placement must be measured after the transform, not in raw coordinates (2026-09-10)

Third attempt at Koishi's eye, and the first two failed the same way: **raw Bedrock cube
coordinates are not where geometry ends up.** The parser positions each bone at
`pivot - parentPivot` (Y inverted), applies its rotation, and bakes cubes relative to that pivot, so
a bundle's final place depends on the whole chain. Aligning raw coordinates aligns nothing.

Attempt one matched the *eyeball's* raw coords to Satori's — but Koishi's eye carries
`rotation: [40, -60, -35]`, so the rotation then swung everything away about a distant pivot.
Attempt two dropped that rotation and matched the *cable bundle's* raw centre — which put the
bundle at model-space z -22.9..-6.7, entirely in front of the player. That is what the screenshot
showed.

Fixed by simulating the parser instead of guessing: `src/personal/tools/simulate_placement.py`
reproduces `BedrockModelParser.build` plus `ModelPart.translateAndRotate` — including that
ModelPart uses `rotationZYX(z, y, x)`, so R = Rz·Ry·Rx — and reports where a subtree actually
lands in model space. Satori's correct bundle measures centre (-0.01, 4.00, -0.49) spanning
z -6.30..5.32, i.e. straddling a torso that occupies z -2..2. Koishi's now measures the same centre
and z -8.57..7.59.

The conversion that makes this solvable: a Bedrock-space `--offset` D shifts the final model-space
result by **(D.x, -D.y, D.z)** — a clean translation, because the tracker's pivot does not move with
the offset and the cube-relative-to-pivot coordinates are unchanged. So measure, subtract, apply.

**Reference, model space** (tracker at origin): neck y 0, waist y 12, body x ±4 and z ±2. Note Y
runs *down* from the neck, opposite to the Bedrock coordinates the extractor takes.

459 powers, 39 origins, 0 errors, 0 dropped fields.

### The shikigami pact could not survive a death, and a silent no-op on the wings (2026-09-10)

**Why the link was inconsistent.** Two halves of the pact are stored very differently:

| | where | survives death | survives restart |
|---|---|---|---|
| `boundless_servitude_boolean` | `apoli:resource` in a container attachment, `copyOnDeath` + serialised | yes | yes |
| `boundless_servitude_set` | `EntitySetPower`'s **static in-memory maps** | **no** | **no** |

`Apoli.onEntityLeaveLevel` calls `EntitySetPower.onEntityGone` whenever a removal reason
"destroys", and `Entity.RemovalReason` marks both `KILLED` and `DISCARDED` as destroying — a
respawning player discards their old entity. So **the master's UUID is stripped from the servant's
set the moment the master dies**, and everything goes on a restart. Logout is
`UNLOADED_WITH_PLAYER`, which does not destroy, so that one is safe.

The result is a pact that reads as intact — the boolean is still 1 — while the set behind it is
empty. The glow has nothing to glow, the teleport reports "unable to locate your master", and the
shared death never fires. Exactly the inconsistency reported.

Fixed by storing the master's UUID in our own `copyOnDeath` attachment when the contract is sealed
(`touhouorigins:remember_master`, placed beside the existing `add_to_set`), and repairing set
membership from it on a 40-tick pass. The repair runs both ways: re-seed when the boolean says
linked but the set has lost the master, and clear the stored UUID when the boolean has gone, so
breaking a pact through the origin screen cannot be undone by a stale record.

`in_set` was checked rather than assumed while here: `InEntitySetCondition` tests
`EntitySetPower.contains(actor, set, target)`, i.e. whether the **target is in the actor's set**, so
the death handler's actor/target order was already right. The shared death was failing because the
set was empty, not because the condition was backwards.

**The crow tengu wings never got the fix I claimed.** The power is a `custom_model_render` at the
**root** of its file, not a sub-power under a key, and the script that was meant to point it at
`animation.spread` iterated `o.items()` looking for sub-powers. It matched nothing, wrote the file
back unchanged, and reported success — so the shipped power still selected the flapping
`animation.glide`. Now on `spread`, verified by reading the clip name back out of the built jar
rather than trusting the edit.

Worth generalising: **an edit script that reports what it changed, without asserting it changed
something, is a silent no-op waiting to happen.** The same shape of bug produced the dead icon NBT
and the unregistered sounds.

Not possible, and worth recording: Apoli animations are keyframed against time with entity
*conditions* only. There is no continuous entity input, so an elytra-style wing tilt that tracks
yaw cannot be expressed in the animation system; it would need a client mixin driving the bones
each frame.

459 powers, 39 origins, 0 errors, 0 dropped fields.

### Crow tengu glide: the spread pose was already modelled (2026-09-10)

Eileen asked for the wings to stay outstretched while gliding. Rather than guess an axis, the
placement simulator was reused to measure the wing envelope for candidate deltas — animation values
being additive, every candidate is "base ±60° yaw plus this".

| delta | lateral (x) | fore-aft (z) |
|---|---|---|
| **0 (bind pose)** | **37.3** | **11.3** |
| yaw -15 | 31.2 | 15.0 |
| yaw -45 | 13.9 | 19.8 |
| roll -30 (what shipped) | 27.7 | — |

The modelled base pose is already the widest the wings get. Every delta made it worse: negative yaw
folds them back toward the spine, and the -30° roll I had been using narrowed them from 37.3 to
27.7 while lifting them above the back — which is what a mid-flap looks like, so it read as motion
even though the clip was static.

`animation.spread` is now a hold at **zero delta**: two keyframes of `[0,0,0]`, which keeps the
wings exactly as modelled — spanning y -7.4..9.9, from above the shoulders to waist height, and
about 37 wide. Two keyframes rather than one because Apoli warns that a single-keyframe channel is
a static pose, and an unlooped clip snaps back to bind after one cycle.

The lesson is the cheap one: the right answer was to apply nothing, and that was only obvious after
measuring rather than reasoning about which axis "opens" a wing.

Still not expressible: a yaw-tracking tilt. Apoli animations take time and entity conditions, with
no continuous input, so elytra-style banking needs a client mixin driving the bones per frame.

### Koishi's eye, kept in its original orientation (2026-09-10)

Eileen's call: keep the model exactly as authored — rotation and all — and move it by translation
only. Extracted without `--drop-root-rotation`, so the eye keeps `[40, -60, -35]`.

With the rotation intact the bundle lands at centre (-4.86, -0.62, 2.93). Satori's known-good
bundle sits at (-0.01, 4.00, -0.49), so the model-space correction is (4.85, 4.62, -3.42), applied
as Bedrock `--offset 4.85 -4.62 -3.42` under the `(D.x, -D.y, D.z)` mapping. Both bundles now share
a centre, and Koishi's straddles the torso in z and sits between neck and waist in y.

This is the fourth placement of the same part and the first that changes nothing but position,
which is what was asked for. The simulator made it a single measurement rather than another guess.

### Shared death depended on the very thing that breaks

The death path read membership of `boundless_servitude_set`, which is the in-memory structure Apoli
strips whenever an entity is removed with a destroying reason. A master who has died even once is
already absent from it, so the next time they die the `in_set` gate fails quietly and the servant
survives — which is exactly what kept happening.

`ShikigamiDeathHandler` now re-seeds membership from the durable pact attachment before offering
the death to the power, so the data-driven condition sees a set that reflects reality. It also
evaluates in the servant's own level rather than the master's, which matters when the pact spans
dimensions.

**Old pacts cannot be repaired**: the attachment is only written by `remember_master` when a
contract is sealed, so a pact made before that existed carries no record. Those need re-linking
once.

### The guide book was unreadable in the colours I chose

Written books render on parchment, so yellow keybinds washed out — and worse, the item names were
`§f` white, i.e. invisible. Repalleted to dark codes only: purple headings, dark red servant, dark
blue master, dark green keys, dark grey asides, black body.

The first verification of this was vacuous and worth recording as a trap: `§` is stored escaped as
`§`, so a regex for `§` against `json.dumps` output matches nothing and happily reports "no
light codes". Decode the page strings first — each page is itself a serialised component — then
search. Done properly: 110 codes, all from `1 2 4 5 8 l r`, no light ones.

### `minecraft:custom_name` must be a flat string in Apoli item stacks

`ItemStackData` parses `components` with `DataComponentPatch.CODEC` under **plain JsonOps**, with no
registry lookup attached. `ComponentSerialization.CODEC` cannot reach its object branch that way, so
the canonical text-component form is rejected:

```
Ignoring the 'components' field of touhouorigins:global/boundless_servitude_sever_tether_silk
— Not a string: {"text":"Tether"}
```

Write `"minecraft:custom_name": "Tether"` instead. Note that `minecraft:written_book_content` in the
same file *does* take an object — this is specific to components whose codec needs registry ops,
which is the same reason the badge banner needed an inline `{asset_id, translation_key}`.

The field is `LoggedOptionalField.of`, i.e. **non-strict**: a rejected `components` block is dropped
with a warning and the power still loads. The lead would have come back unnamed and therefore
useless for re-binding, with nothing in the log unless you were looking for it.

### Trap: `[WARN]` does not appear in a Minecraft log line

Log lines read `[main/WARN]` and `[Server thread/ERROR]`, never bare `[WARN]`/`[ERROR]`. Every
`grep -cE "\[WARN\]|\[ERROR\]"` in this project returned 0 regardless of content — a vacuous check
that reported clean runs for several commits. **Use `grep -E "/(WARN|ERROR)\]"`**, then filter the
routine vanilla noise: command-ambiguity lines, `.mcassetsroot` schema warnings, and the
offline-mode block.

Re-audited every verification log in the session with the correct pattern afterwards: the earlier
work was in fact clean, but that was luck, not evidence.
