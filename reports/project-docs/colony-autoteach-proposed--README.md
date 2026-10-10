# colony-autoteach

A MineColonies `crafterrecipes` datapack so colonists already know their recipes
instead of you teaching each one by hand.

`colony-autoteach.zip` — **21,646 recipes across 28 crafters**. Boot-tested on
`test-server/X-expeditions`: `Loaded 21890 recipes for 45 crafters` (the pack's
own 244 plus ours), `Done` in 42.8 s, zero parse errors.

> Boot goes from ~29 s to ~43 s, almost all of it the stonecutting module (4,620
> recipes). If that is not worth it, drop `"minecraft:stonecutting"` from
> `RECIPE_TYPES` in `gen_crafterrecipes.py` and rebuild.

## Building it

```bash
python gen_crafterrecipes.py \
  --source "<instance>/libraries/net/minecraft/server/1.21.1-20240808.144430/server-1.21.1-20240808.144430-extra.jar" \
  --source "<instance>/mods" --source "<extracted datapack data>" \
  --audit recipe_audit.csv --items tag_item_audit.csv \
  --out ./autoteach --namespace autoteach
python curate_crafterrecipes.py --pack ./autoteach --namespace autoteach \
  --source ... --items tag_item_audit.csv --report audit-report.md
```

`recipe_audit.csv` and `tag_item_audit.csv` come from `<world>/minecolonies/`
after booting once with `auditcraftingtags = true` in `minecolonies-server.toml`.
**The audit only runs on `OnDatapackSyncEvent`, which a plain server start does
not fire** — run `/reload` once after `Done`, then stop.

## Where to install it

The **instance's root `datapacks/` folder**, next to the pack's other 19 zips —
not `<world>/datapacks/`.

`simple_datapacks` sets `requiredDatapacksPaths: ["datapacks"]` and mixes into
`PackRepository`, `ServerPacksSource` and `MinecraftServer#getDataPackConfig`. A
pack in `<world>/datapacks/` is discovered by vanilla ("Found new data pack …,
loading it automatically") but the mod then substitutes its own selection, so it
never reaches the resource manager. Verified with a two-file probe: even a single
recipe in the `minecolonies` namespace itself was ignored from `<world>/datapacks/`
and picked up immediately from the root folder.

- Once a pack name has been seen and then goes missing, the server records it in
  `level.dat` under `DataPacks.Disabled` and never auto-enables that name again.
  Keep the filename stable, or use a new one — re-adding a name already in
  `Disabled` silently does nothing.
- `/reload` is **broken in this modpack independent of this datapack** — it dies
  with `Failed to parse thing: 'Unknown registry key … croptopia:soy_milk'` →
  `NoSuchElementException`. Install and restart; don't rely on `/reload`.

## Crafter families

Every module in the audit extends `AbstractCraftingBuildingModule`, so **all of
them accept datapack-taught recipes** — including the addon ones. What differs is
the kind of work the colonist can actually perform there, so each recipe is
matched to a module of its own family:

| family | modules | source recipe types |
|---|---|---|
| table | `*_crafting`, `lumberjack_custom`, `blacksmith_smithing_template_crafting` | `crafting_shaped`, `crafting_shapeless` |
| furnace | `*_smelting` | `smelting`, `blasting`, `smoking`, `campfire_cooking` |
| farmersdelight | `chef_farmers_cooking`, `chef_farmers_cutting_knife`, `chef_farmers_plating`, `lumberjack_farmers_cutting_axe`, `stonemason_farmers_cutting_pickaxe` | `farmersdelight:cooking`, `farmersdelight:cutting` |
| stonecutting | `stonemason_stonecutting` | `minecraft:stonecutting` |
| smithing | `blacksmith_smithing` | `minecraft:smithing_transform` |

`MineColonies_Compatibility` (steve_gall) supplies the Farmer's Delight,
stonecutter and smithing modules. `CookingCraftingModule`, `CuttingCraftingModule`,
`PlatingCraftingModule`, `StonecutterCraftingModule` and `SmithingCraftingModule`
all extend `AbstractCraftingBuildingModule$Custom`; `SmithingTemplateCraftingModule`
extends `$Crafting`. All of them therefore get a request resolver and serve the
colony normally.

One caveat worth knowing: teaching in-game produces a `CookingRecipeStorage`,
while a datapack recipe produces a plain `RecipeStorage`. `CookingCraftingModule.
getCraftingTime` tests that with a **guarded `instanceof`**, so nothing crashes —
the colonist just uses the default crafting time rather than Farmer's Delight's
own. Worth an in-game sanity check that cooking-pot work actually completes.

### Still left empty, and why

| crafter | why |
|---|---|
| `alchemist_brewing` | vanilla brewing is code-defined, not datapack data; the Alchemist brews every potion natively |
| `*_domum` (5) | Domum Ornamentum's Architect's Cutter needs DO's material system; not expressible as a plain custom recipe |
| `sifter_custom`, `crusher_custom`, `concretemixer_custom`, `enchanter_custom`, `netherworker_custom`, `smelter_custom`, `smelter_smelting` | accept custom teach, but MineColonies ships their complete fixed sets and the work is loot-table / fixed-chain driven. Giving the Sifter a carrot recipe would "work" and be nonsense |
| `expedition_*_custom` (11) | your expeditions addon — trip loot, not crafting. Say the word if these should hold recipes after all |
| `fluid_manager_bucket_filling` | no qualifying recipes in the pack |

## The early-colony bootstrap

The intent is that nothing waits on you for tools:

1. Hand the **Builder** raw logs → it makes its own wooden pickaxe, axe, shovel
   and hoe, going logs → planks → sticks on the way.
2. The Builder puts up the **Forester's Hut** → wood stops being something you
   hand over and becomes a supply the colony produces.
3. The Builder puts up the **Mine** → its cobblestone is the stone-tool upgrade
   for both of them.

**One correction to how step 3 works.** The Mine cannot hand a finished tool to
another hut: the Builder's Hut and the Mine both use `SimpleCraftingModule`,
which overrides `createResolvers()` to return `Collections.emptyList()` and
`getCraftingJob()` to return `null`. With no request resolver the request system
cannot ask either of them for anything.

What the Mine *does* supply is the **cobblestone it digs**, which couriers deliver
to the warehouse like any other mining output. So the Builder and the Lumberjack
each get the stone tools too and upgrade *themselves* from that cobblestone. Same
ecosystem, one hop different.

| hut | wood chain | wooden tools | stone tools | serves colony requests |
|---|---|---|---|---|
| Builder | ✅ | ✅ | ✅ | no — `SimpleCraftingModule` |
| Lumberjack (`lumberjack_custom`) | ✅ | ✅ | ✅ | ✅ |
| Miner | — | — | ✅ | no — `SimpleCraftingModule` |

The Blacksmith separately carries all five wooden and all five stone tools, so
colony-wide tool requests still get answered once that hut is up.

Custom crafter recipes bypass the normal gating entirely — no 2×2 grid limit, no
recipe-slot limit, no tag checks (`checkForWorkerSpecificRecipes` only calls
`CustomRecipe.isValidForBuilding`) — which is why 3×3 tool recipes work on huts
that can normally only be taught 2×2 ones.

## Bugs fixed in `gen_crafterrecipes.py`

1. **Item tags were overwritten instead of merged.** 12 jars contribute to
   `#minecraft:planks`; only the last survived, so the tag resolved to 3 entries
   instead of 89 and every tag-based ingredient and variant expansion was
   silently truncated (`sawmill_product` 12 → 268, `stonemason_product` 84 → 992).
2. **Recipe conditions were ignored.** 873 recipes disabled in this pack
   (`neoforge:false`, `neoforge:never`, `mod_loaded` for absent mods) were still
   emitted. Crafter recipes don't consult the vanilla recipe book, so teaching one
   quietly brings a deliberately removed item back.
3. **Recipes naming unregistered items bricked the server.** `CustomRecipe.parse`
   → `ItemStorage.<init>` calls `Optional.get()` on the registry lookup with no
   fallback, so one bad item id aborts the entire datapack load: *"Failed to load
   datapacks, can't proceed with server load"*. Compat datapacks are full of
   recipes for mods you don't have — 2,857 such recipes were being emitted.
   `--items tag_item_audit.csv` now filters them.

## What the audit changed

See `audit-report.md` for the tables. The generator drove everything off
`recipe_audit.csv`, which is a *permission* matrix ("this crafter may be taught
this item"), not a statement of whose job it is. Taken literally that gave the
Builder and the Miner every 2×2 recipe in the pack (10,176 each), and 7,346
recipes sat in a module that could not run that kind of work.
