---
name: yh-4x-fluid-api-shape
description: "The trap in Youkai's Homecoming 4.x bottled-fluid API — two parallel interfaces, and why SakeBottleItem#getFluid() must not be used."
metadata: 
  node_type: memory
  type: reference
  originSessionId: d93927b3-04f1-4d22-88a6-76cbc2fd6ca8
  modified: 2026-09-03T17:33:17.600Z
---

Youkai's Homecoming 4.x has **two parallel, unrelated** bottled-fluid interfaces:
the legacy `IYHSake` (the `YHSake` enum, backing `SakeFluid`) and the newer
`IYHFluidHolder` (`BottledFluid`, backing `YHFluid`). They declare identical members
(`getContainer()`, `asStack(int)`, `amount()`) but share no supertype, and `SakeFluid`
and `YHFluid` are siblings under `BaseFlowingFluid` — neither extends the other.

**Do not reach the fluid through `SakeBottleItem#getFluid()`.** It is declared to return
`YHFluid`, but `YHSake` builds its items with `new SakeBottleItem(fluid::getSource, p)`
over a `FluidEntry<SakeFluid>`; Registrate's `getSource()` is generically typed, so this
compiles and then throws ClassCastException at the call site for every sake item.

Go the other way instead: iterate `BuiltInRegistries.FLUID` and branch on
`instanceof SakeFluid` / `instanceof YHFluid`, reading `.type` from whichever matched.
`VirtualFluidBuilder` calls `this.source(factory)`, so both the source and flowing
variants are instances of the same class and carry the same `type` — iterating the
registry covers both. In MaidsoulKitchen this is wrapped as the `YhFluidItem` record.

Related shape changes in the same version: `SimpleFermentationRecipe.inputFluid` went from
`FluidIngredient` to a plain `FluidStack` and gained a tag-based `inputFluidTag` (String)
alternative plus item `results`; the kettle stopped extending `BasePotBlockEntity`
entirely. See [[maidsoul-kitchen-yh-port-setup]].
