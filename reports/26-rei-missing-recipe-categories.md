# 26 — Missing recipe categories in REI (Extra Delight, Spectrum)

2026-09-20 · 1.21.1 NeoForge · instance `PrismLauncher/Instances/1.21.1`

## Complaint

Extra Delight's oven and mixing bowl recipes (and the rest of its workstations) do not
appear in REI, though they were visible back when the pack ran JEI. Spectrum's altar,
enchanter and potion stand categories are also missing, despite Spectrum advertising REI
compatibility.

These turned out to be two unrelated problems.

## Spectrum — not broken, gated

Spectrum's REI plugin loads fine. Its classes live in an uppercase package,
`de/dafuqs/spectrum/compat/REI/`, which is why a casual `grep -i rei` on the jar appears to
come back empty — the earlier read of this jar that reported "EMI only" was wrong. The
package holds every category named in the complaint: `PedestalCraftingCategory` (the altar),
`EnchanterCategory`, `PotionWorkshopCategory` and about twenty more.

`latest.log` confirms it registers and reloads without an exception:

```
[REI] Registered plugin provider REIClientIntegration [spectrum] for REIPlugin
```

The categories are wrapped in `GatedDisplayCategory` / `GatedRecipeDisplay`: Spectrum hides
each workstation's category until the matching progression advancement is unlocked. That
gating is separate from `Revelationary.json`'s `HideCloakedEntriesFromRecipeViewers` (which
is already `false` here, so cloaked *items* do show) and has no config toggle of its own.

To confirm gating rather than a fault, grant one unlock and watch the category appear:

```
/advancement grant @s only spectrum:midgame/build_enchanter
```

**Nothing to port.** Spectrum needs progression, not code.

## Extra Delight — genuinely absent, so it was written

`extradelight-2.6.6.jar` contains `integration/jei/JEIPlugin.class` and fourteen JEI
categories, and zero classes in any `rei` or `emi` package. JEI is not installed in this
pack. The categories therefore do not exist for REI at all; no config change can surface
them.

Fixed by a new companion mod, in the same spirit as the pack's existing `rejei-0.1.0.jar`
(which does this for Create).

### The mod

`C:\Users\eteil.000\Claude\extradelight-rei` — mod id `edrei`, built as
`edrei-1.21.1-1.0.0.jar`, installed in the instance.

Client-side only. It has no game content; the `@Mod` class exists only to give NeoForge
something to load `@REIPluginClient EDReiPlugin` from.

Fourteen categories, one per Extra Delight recipe type:

| Category | Recipe type | Notes |
|---|---|---|
| Oven | `OVEN` | 9 input slots, cook time + experience |
| Mixing | `MIXING_BOWL` | items + fluids + utensil, stir count |
| Grinding | `MORTAR` | grind count, optional fluid output |
| Drying | `DRYING_RACK` | drying time |
| Feast Servings | `FEAST` | feast block + container |
| Tool on Block | `TOOL_ON_BLOCK` | block + tool |
| Melting | `MELTING_POT` | item to fluid |
| Chilling | `CHILLER` | items + fluid, chill time |
| Juicing | `JUICER` | item to item + fluid, chance |
| Evaporating | `EVAPORATOR` | fluid to fluid + item |
| Fermenting | `VAT` | per-stage times and lid state |
| Dough Shaping | `DOUGH_SHAPING` | |
| Decanting | `BOTTLE_FLUID_REGISTRY` | |
| Crafting with Jars | `SHAPED_JAR` | pattern padded to 3 columns |

Design notes:

- **Titles reuse Extra Delight's own `extradelight.jei.*` lang keys**, so the categories are
  already translated in every language Extra Delight ships. Only the captions under each
  recipe (`edrei.caption.*`) are new strings. `SHAPED_JAR` is the one exception — Extra
  Delight has no lang key for it, since in JEI it rides the vanilla crafting category — so
  it gets `edrei.category.shaped_jar`, "Crafting with Jars".
- **One display class and one category class for all fourteen.** Fluids go into the same
  input/output lists as items, because REI renders a fluid entry in an ordinary slot. That
  means "what makes this" and "what uses this" cover fluids with no extra plumbing.
  Everything that is not an ingredient — stir counts, cook times, stage timings — becomes a
  caption line under the grid.
- **Fluid units are rescaled.** Extra Delight speaks NeoForge's 1000-per-bucket; REI speaks
  Architectury's `FluidStack`, whose bucket size is platform-defined. `EDFluids` converts via
  `FluidStack.bucketAmount()` rather than assuming they match.
- **Compiled against the instance's own jars** (`libs/` holds copies of extradelight, REI,
  architectury and cloth-config), so the build is checked against exactly what the pack runs
  rather than against a maven version that might drift.

`me.shedaniel.math` (Point, Rectangle) lives in **cloth-config**, not in REI or architectury —
worth remembering for the next REI plugin.

### Not verified

The jar compiles clean against the live jars, which pins every Extra Delight and REI symbol
it touches. What that does **not** cover is in-game appearance: slot alignment for the wider
recipes, whether the vat's five caption lines fit its allotted height, and whether the
evaporator's output volume estimate reads sensibly. Those need a client launch.

If Extra Delight updates, re-check `ExtraDelightRecipes` field names and the
`extradelight.jei.*` lang keys.

## Also noticed

`latest.log` has recurring `[REI] Error while searching` entries from REI's async search
threads, unrelated to missing categories. Not investigated.
