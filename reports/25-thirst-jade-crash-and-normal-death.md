# 25 — Thirst Was Taken: Jade crash, and thirst killing on Normal (2026-09-20)

Both fixed in one patched jar. Script: `fixed-mods/patch_thirst.py`,
pristine copy at `fixed-mods/ThirstWasTaken-1.21.1-2.1.5.jar.ORIGINAL-BACKUP`.
Rerun the script against that backup after any Thirst Was Taken update.

## 1. Jade crash (`crash-2026-09-20_18.20.45-server.txt`)

```
MixinTransformerError: An unexpected critical error was encountered
Caused by: InjectionError: Critical injection failure: Redirector
  read(Lsnownee/jade/api/fluid/JadeFluidObject;)Lnet/minecraft/network/chat/Component;
  in thirst.mixins.json:jade.MixinFluidView from mod thirst
  failed injection check, (0/1) succeeded. Scanned 0 target(s). No refMap loaded.
```

`jade.MixinFluidView` is an `@Redirect(method = "readDefault")` on
`snownee.jade.api.view.FluidView` around `CommonProxy.getFluidName`; it appends the
water-purity text to Jade's fluid tooltip. Against Jade 15.10.6 the selector matches
nothing. `thirst.mixins.json` is `"required": true` with `injectors.defaultRequire: 1`,
so the failure is fatal rather than skipped.

`FluidView` loads lazily — `FluidStorageProvider.appendServerData → CommonProxy.wrapFluidStorage
→ JadeForgeUtils.fromFluidHandler → FluidView` — so it fires the first time Jade reads a
fluid-holding block (tank, cauldron, Create basin), not at startup. That is why it only
appeared today although TWT went in on 19 Sep and Jade has not changed since 31 Aug.

**Fix:** drop the entry and its class from the jar. Only loss is purity text in Jade's
fluid tooltip.

Ruled out: the `Class version 65 … higher than JAVA_17` debug line appears for *every*
thirst mixin including ones that apply fine; the `toughasnails.Mixin*` "target not found"
warnings are TWT's optional TAN compat, harmless since TAN was removed; and only one copy
of `FluidView` exists anywhere in the instance, so it is not a duplicate-class problem.

## 2. Thirst could kill on Normal

`PlayerThirst.tick` gates its 1.0F dehydration hit with

```java
getHealth() > 10.0F || difficulty == HARD || (getHealth() > 0.0F && difficulty == NORMAL)
```

Vanilla `FoodData` uses `> 1.0F` in that last clause — that is exactly why starvation
cannot kill below Hard. TWT's `0.0F` lets the hit land at 1.0 health and finish you.

**Fix:** the `0.0F` is a single `fconst_0` (`0x0B`) at code offset 515 in `tick`; it becomes
`fconst_1` (`0x0C`). Same opcode length, so no offsets, branch targets or stack map frames
move. Byte pattern `2B B6 00 F4 0B 95 9E 00 1A` is unique in the class.

Behaviour now matches hunger exactly: Easy stops at 10 health, **Normal stops at 1 health**
(a hit at 1.5 still lands and leaves you at 0.5, as vanilla does), Hard still lethal.

Not affected: poison/nausea from dirty water (vanilla poison never kills on its own), and
dehydration's slowness and halted regen still apply.

## Status
Patched jar installed in `PrismLauncher/instances/1.21.1/minecraft/mods`. Not yet tested
in game — worth confirming by looking at a fluid tank (no crash) and letting thirst empty
on Normal (drops to half a heart and holds).
