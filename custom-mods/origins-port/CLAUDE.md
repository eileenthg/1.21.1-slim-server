# CLAUDE.md — Touhou Origins → Overgrown's Origins (NeoForge 1.21.1)

## Mission

Port two Fabric Origins add-ons to NeoForge 1.21.1, targeting **Overgrown's Origins / Overgrown's Apoli** as the host implementation:

1. **Touhou Origins** (Fabric, MC 1.20.2, Origins 1.12.3) — 19 origins, ~99 power files, 9 Java files
2. **Origins: Player Scale** (pure datapack, MC ≤1.20.4) — 7-size origin layer, Pehkui-backed

These are separate deliverables. **Do Player Scale first** — it is small, has no Java, and validates the whole toolchain before committing to the 125-file Touhou migration.

---

## Verified facts

Everything in this section was established by reading actual source, not recalled. Where a claim is inferred rather than measured, it says so.

### Target stack

| Component | Fact | Source |
|---|---|---|
| Overgrown's Apoli | `MOD_ID = "apoli"` | `grep MOD_ID` on `0vergrown/Apoli` |
| Overgrown's Origins | `MOD_ID = "origins"` | `grep MOD_ID` on `0vergrown/Origins` |
| Branch layout | One branch per loader/MC combo: `Fabric-1.20.1`, `Fabric-1.21.1`, `NeoForge-1.21.1` | both repos' `git ls-remote --heads` |
| **Default branch is `Fabric-1.20.1`** | A plain `git clone` gets you the WRONG branch. Always `-b NeoForge-1.21.1`. | measured |
| Apoli NeoForge-1.21.1 | `minecraft_version=1.21.1`, `mod_version=1.56.0`, 550 registered type ids | `gradle.properties` + `grep 'Apoli.id("...")'` |
| Origins NeoForge-1.21.1 | 76 Java files, 10 built-in origins, all defined as Apoli data | repo README + `find` |

**Why this target:** because both mod ids match upstream (`apoli`, `origins`), existing `apoli:`- and `origins:`-namespaced datapack JSON resolves natively. No translation layer, no type remapping. This is the entire reason we are not targeting NeoOrigins (which translates into its own `neoorigins:` namespace) or IAFEnvoy's Origins-NeoForge (which requires explicit datapack conversion).

### Compatibility measurement (the decisive number)

Diffed Touhou Origins' required types against Apoli's `NeoForge-1.21.1` branch registry:

- Touhou uses **149 distinct `apoli:` types**
- Missing from Overgrown's Apoli NeoForge-1.21.1: **0**
- Coverage: **149 / 149**

Touhou also uses 6 `origins:` types — `chose_origin`, `hurt_by_water`, `meat`, `origin`, `toggle`, `water_protection` — expected to resolve against Overgrown's Origins, **but this was not individually verified.** Verify in Phase 1.

### The actual blocker: Voile

Touhou Origins' real dependency is not Origins, it is **Voile** (`net.reimaden.voile`, repo `Maxmani/voile`), an Apoli extension bundled into the jar via Gradle `include(...)`.

- **30 distinct `voile:` types** used
- across **38 of 99 power files**
- touching **17 of 19 origins** — only `crow_tengu` and `vampire` are Voile-free
- Voile is **Fabric-only and archived on Modrinth** (~2 years stale, built against MC 1.20.2 / Apoli 2.11.4). There will never be a NeoForge 1.21.1 build.

Voile shares the `net.reimaden` package root with Touhou Origins and is by the same author. Treat it as effectively part of the mod, not a third-party library.

**4 Voile types have same-named equivalents in Overgrown's Apoli** — `disguise`, `disguised`, `remove_disguise`, `water_breathing`. Name match only; **field schemas were not compared.** Do not assume drop-in.

**26 Voile types need reimplementation or workaround:**

```
action_on_block          action_on_entity_death   change_resource_with_status_effects
check_set                convert_entity           craftable
disable_shields          enchantment_vulnerability flip_model
instant_effect_immunity  kill                     modify_behavior
modify_death_sound       modify_divergence        modify_footstep_sound
modify_hurt_sound        modify_scale             nearby_entities
precise_on_block         prevent_flying_kick      prevent_item_slowdown
prevent_sprinting_particles  prevent_taming       reverse_instant_effects
teleport_to_entity       zombie_arms
```

Voile's registered power classes (from `power/VoilePowers.java`) include several not in the list above — `ModifyPickupRangePower`, `PreventTotemPower`, `ModifyProjectileSpeedPower` — i.e. Voile is larger than Touhou's usage. **Only port what Touhou actually uses.**

### Scale is the shared seam between both deliverables

`voile:modify_scale` is a **Pehkui wrapper**. Confirmed by imports in `ModifyScalePower.java`:

```java
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleType;
```

Its serialized signature:

```java
new SerializableData()
    .add("scale_types", SerializableDataTypes.IDENTIFIERS, DEFAULT_SCALE_TYPES)
    .add("scale", SerializableDataTypes.FLOAT)
```

Touhou Origins **pre-2.0 depended on Pehkui directly**; 2.0 moved it behind Voile (per its README). Origins: Player Scale depends on Pehkui directly and always has. Pehkui has a 1.21.1 NeoForge build.

Overgrown's Apoli registers `set_size` and `entity_set_size` natively. **Unverified whether these are Pehkui-backed or use a different scaling mechanism.** This is the single most important unknown — resolve it in Phase 1, because it determines whether Pehkui is a dependency at all or whether both mods can drop it.

### Licensing

| Artifact | License | Consequence |
|---|---|---|
| Touhou Origins | MIT (Maxmani, 2023–2024) | Permissive; port freely with attribution |
| Voile | **LGPL-3.0** | Reimplementing 26 types by reading LGPL source. Clean-room is safest; at minimum do not copy method bodies. Note derivation in the port's NOTICE. |
| Origins: Player Scale | **GPL-3.0-or-later** | Any derivative distribution must be GPL-3.0+. This constrains the whole deliverable if shipped together. |
| Overgrown's Apoli / Origins | Custom "A O License" (GitHub reports `NOASSERTION`) | **Read `https://0vergrown.github.io/Handbook/license` before writing any redistributable code.** Non-standard licenses can conflict with GPL-3.0. Flag to Eileen if it does. |
| Touhou Project IP | Fan work, Team Shanghai Alice (ZUN) | Keep the upstream disclaimer in any README |

**Do not ship anything publicly until the A O License / GPL-3.0 interaction has been checked with Eileen.**

---

## Workspace setup

Eileen will designate the project folder. Assume you are at its root. Create this layout:

```
<project-root>/
├── CLAUDE.md                  # this file
├── NOTES.md                   # your running findings log (create in Phase 0)
├── reference/                 # read-only upstream clones — GITIGNORED, never edit
│   ├── touhou-origins/        # Maxmani/touhou-origins @ master (1.20.2 Fabric)
│   ├── voile/                 # Maxmani/voile @ master (1.20.2 Fabric)
│   ├── og-apoli/              # 0vergrown/Apoli @ NeoForge-1.21.1
│   ├── og-apoli-fabric-1201/  # 0vergrown/Apoli @ Fabric-1.20.1  (for cross-version diffing)
│   ├── og-origins/            # 0vergrown/Origins @ NeoForge-1.21.1
│   └── origins-fabric/        # apace100/origins-fabric (upstream reference)
├── player-scale/              # DELIVERABLE 1 — datapack, no Java
└── touhou-origins-neo/        # DELIVERABLE 2 — NeoForge MDK mod
```

### Phase 0 — bootstrap

```bash
mkdir -p reference
cd reference

git clone --depth 1 https://github.com/Reimaden-Modding/touhou-origins.git
git clone --depth 1 https://github.com/Maxmani/voile.git
git clone --depth 1 -b NeoForge-1.21.1 https://github.com/0vergrown/Apoli.git og-apoli
git clone --depth 1 -b Fabric-1.20.1   https://github.com/0vergrown/Apoli.git og-apoli-fabric-1201
git clone --depth 1 -b NeoForge-1.21.1 https://github.com/0vergrown/Origins.git og-origins
git clone --depth 1 https://github.com/apace100/origins-fabric.git

cd ..
echo "reference/" >> .gitignore
```

Then:

1. Confirm every clone landed on the intended branch: `for d in reference/*/; do echo -n "$d "; git -C "$d" rev-parse --abbrev-ref HEAD; done`
2. Create `NOTES.md` and log anything that contradicts this document. **This file is evidence, not scripture** — if the source disagrees, the source wins and you record it.
3. Do **not** run Gradle yet.

### Reference folder rules

- `reference/` is **read-only**. Never edit, never commit. It exists so you can grep real API signatures instead of recalling them.
- When you need an Apoli type's schema, read it from `reference/og-apoli/` — do not infer it from the Fabric Apoli docs, which describe a different implementation.
- `og-apoli-fabric-1201` is there for one purpose: diffing type schemas across the 1.20.1 → 1.21.1 jump, so you can find breaking JSON changes in bulk rather than one parse failure at a time.

---

## Deliverable 1 — Origins: Player Scale

No Java. No repo upstream (Modrinth only) — Eileen will need to supply the datapack zip; ask for it.

**Phase 1 — resolve the scale question (BLOCKING, do this before anything else)**

Read `reference/og-apoli/` for `set_size` and `entity_set_size`. Determine:

- Are they Pehkui-backed, or an independent scaling implementation?
- What are their serialized fields? Compare against Pehkui's `scale_types` / `scale` model.
- Does Overgrown's Apoli declare a Pehkui dependency at all?

Report the answer to Eileen before proceeding. It determines the shape of both deliverables.

**Phase 2 — inventory**

Unzip the datapack. Enumerate every `"type"` value. Diff against `reference/og-apoli/` and `reference/og-origins/` registries the same way this document did for Touhou. Produce a coverage number and a missing-type list.

**Phase 3 — migrate**

Mechanical first: namespace and type renames, JSON schema updates for 1.21.1 (component changes, `Identifier` → `ResourceLocation` serialization shifts, attribute id changes). Load it, read the log, fix what actually breaks. **Do not pre-emptively fix things the loader has not complained about.**

---

## Deliverable 2 — Touhou Origins

### Java surface (small — 9 files)

| File | What it is | Port note |
|---|---|---|
| `TouhouOrigins.java` / `TouhouOriginsClient.java` | Fabric entrypoints | Replace with NeoForge `@Mod` + event bus |
| `power/ActionOnOriginGuiPower.java` | Custom power, 35 lines, `touhouorigins:action_on_origin_gui`, used **once** | Consider dropping. Confirm with Eileen whether the single use is worth a custom power type. |
| `power/ModPowers.java` | Registers the above | Follows from the above |
| `mixin/OrbOfOriginItemMixin.java` | Single `@Inject` at `HEAD` on `use` | **HIGH RISK.** Targets Origins' `OrbOfOriginItem` — Overgrown's class may not exist or may differ. Spike first. |
| `mixin/OriginCommandMixin.java` | Single `@Inject` at `HEAD` on `openLayerScreen(ServerPlayerEntity)` | **HIGH RISK.** Same reasoning; the descriptor is Yarn-mapped and will need Mojmap. |
| `sound/ModSoundEvents.java` | Sound registry | Mechanical |
| `particle/ModParticles.java`, `particle/OrbParticle.java` | Particle registry + client particle | Mechanical, but particle registration changed in 1.21 |

### Phased work order

**Phase 1 — spikes (before ANY bulk work)**

Both spikes exist because failures here invalidate the plan, and silently-failing mixins produce confusing downstream behaviour that is far more expensive to debug later than up front.

1. **Mixin spike.** Do the two mixin targets exist in `reference/og-origins/`? Under what names, what signatures, Mojmap not Yarn? If Overgrown's Origins has no `OrbOfOriginItem` or no `OriginCommand.openLayerScreen`, **stop and report** — the mixins may need replacing with events, or the behaviour may already be covered natively.
2. **Scale spike.** Same question as Player Scale Phase 1. Shared answer.
3. **`origins:` type spike.** Verify the 6 `origins:` types Touhou uses resolve against Overgrown's Origins.

Report all three to Eileen before Phase 2.

**Phase 2 — build skeleton**

NeoForge MDK 1.21.1. Get an empty mod compiling and launching with Overgrown's Origins + Apoli as dependencies, **before** moving any content in. Resolve dependency coordinates first — check whether Overgrown publishes a Maven, and fall back to Modrinth Maven or local jars if not. Declare Origins and Apoli as hard dependencies in `neoforge.mods.toml`; do not repeat the classloading-dependency-not-declared mistake.

**Phase 3 — move the data, compile, let the loader speak**

Copy `src/main/resources/data/` and `assets/` across. Mechanical renames only. Launch, read the log, fix real errors. Resist architectural guessing before the loader has spoken.

Content inventory to migrate: 19 origins, 99 powers, 1 global power, 17 tag files, 5 damage types, 5 functions, 2 predicates, 2 advancements, 2 item modifiers.

**Phase 4 — Voile reimplementation**

Only after Phase 3 has narrowed the list to what actually fails.

Suggested order, cheapest and most-reused first:

1. `modify_scale` — highest reuse, shared with Player Scale, already spiked
2. The four possible free wins — `disguise`, `disguised`, `remove_disguise`, `water_breathing`: compare Voile's schema against Overgrown's same-named types. If compatible, this is a namespace rewrite.
3. Simple preventers — `prevent_taming`, `prevent_flying_kick`, `prevent_item_slowdown`, `prevent_sprinting_particles`, `disable_shields`
4. Sound modifiers — `modify_footstep_sound`, `modify_hurt_sound`, `modify_death_sound`
5. The hard ones — `convert_entity`, `modify_behavior` (entity AI goals), `zombie_arms`, `flip_model` (rendering)

For each: check whether Overgrown's Apoli already has an equivalent under a different name **before** writing Java. It registers 550 types; the odds are good.

**Phase 5 — per-origin verification**

17 of 19 origins touch Voile. Verify each in-game. Start with `crow_tengu` and `vampire` (Voile-free) as the control group — if those two are broken, the problem is not Voile.

---

## Working agreements

- **Read source, don't recall.** Every Apoli/Origins API claim must be grepped from `reference/`. Recalled knowledge of Fabric Apoli is actively misleading here — Overgrown's is a rewrite, not a port.
- **Mechanical first, compile, then architect.** Rename and move before redesigning. Let the compiler and the datapack loader produce the real error list.
- **Explain alongside changes.** Eileen reviews direction, not just diffs. When you make a non-obvious choice, say why in one or two sentences.
- **Spikes gate bulk work.** Do not start Phase 3 before Phase 1 is reported and acknowledged.
- **Log contradictions.** If this document is wrong, `NOTES.md` records it and the source wins.
- **Stop and ask** on: the A O License / GPL-3.0 interaction, mixin targets that do not exist, and any Voile type that turns out to need more than ~100 lines of Java.
- **No public release** until licensing is cleared with Eileen.

## Open questions for Eileen

1. Are `set_size` / `entity_set_size` Pehkui-backed? (Claude Code resolves this in Phase 1 and reports.)
2. Keep or drop the single-use `touhouorigins:action_on_origin_gui` custom power?
3. Ship the two deliverables together or separately? Player Scale's GPL-3.0-or-later constrains a combined release.
4. Does she have the Player Scale datapack zip, or should it be pulled from Modrinth?
