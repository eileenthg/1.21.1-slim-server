---
name: extradelight-rei-plugin
description: "edrei mod: ports Extra Delight's 14 JEI-only workstation categories to REI; also why Spectrum's REI categories look missing"
metadata: 
  node_type: memory
  type: project
  originSessionId: 05e27856-32b5-44b9-951a-497881c93f6f
  modified: 2026-09-20T15:58:34.886Z
---

On 2026-09-20 the user reported Extra Delight and Spectrum workstation recipes missing from
REI on 1.21.1 NeoForge. Two unrelated causes.

**Extra Delight ships a JEI plugin and nothing else** (`integration/jei/JEIPlugin`, 14
categories, zero `rei`/`emi` classes), and JEI is not installed. So a companion mod was
written: `C:\Users\eteil.000\Claude\extradelight-rei`, mod id `edrei`, built as
`edrei-1.21.1-1.0.0.jar` and installed in `PrismLauncher/Instances/1.21.1/minecraft/mods`.
Report: `modpack-audit/26-rei-missing-recipe-categories.md`. Precedent in the pack:
`rejei-0.1.0.jar` does the same for Create.

**Spectrum is not broken.** Its REI plugin lives in the uppercase package
`de/dafuqs/spectrum/compat/REI/` — a `grep -i rei` that looks only at lowercase paths will
wrongly conclude it is EMI-only. The plugin registers cleanly in `latest.log`. The altar,
enchanter and potion stand categories are hidden by Spectrum's own `GatedDisplayCategory`
progression gating, which is separate from `Revelationary.json` and has no config toggle.

**Why:** the `edrei` design keeps one display class and one category class for all 14 types.
Fluids go in the same input/output lists as items, because REI renders a fluid entry in an
ordinary slot — that gets recipe lookup for fluids for free. Non-ingredient data (stirs,
cook times, vat stage timings) becomes caption lines. Titles reuse Extra Delight's own
`extradelight.jei.*` lang keys, so the categories inherit its translations.

**How to apply:**
- `me.shedaniel.math` (Point, Rectangle) is in **cloth-config**, not REI or architectury. Any
  REI plugin build needs cloth-config on the compile classpath.
- REI fluid entries use Architectury's `FluidStack`; rescale NeoForge amounts via
  `FluidStack.bucketAmount()` instead of assuming 1000.
- Build compiles against copies of the instance's own jars in `libs/`, so it is checked
  against what the pack actually runs. Gradle needs network (offline mode lacks the NeoForge
  modDev artifacts).
- Not verified in game: slot alignment on wide recipes, whether the vat's 5 caption lines fit,
  and the evaporator output-volume estimate. Needs a client launch.
- If Extra Delight updates, re-check `ExtraDelightRecipes` field names and the
  `extradelight.jei.*` lang keys.

Related: [[test-instance-copy-resets-configs]], [[disable-stub-recipe-shapes]].

- **Obsolete (10 Oct):** the pack is JEI-only since ~24 Sep; REI and edrei are not installed, and the source folder `Claude/extradelight-rei` no longer exists.
