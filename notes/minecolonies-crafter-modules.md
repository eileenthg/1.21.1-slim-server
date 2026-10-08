---
name: minecolonies-crafter-modules
description: "Every MineColonies crafter module accepts datapack-taught recipes; what differs is the recipe family it can run. Addon modules from MineColonies_Compatibility."
metadata:
  type: reference
---

All MineColonies crafting modules extend `AbstractCraftingBuildingModule`, whose
`checkForWorkerSpecificRecipes()` pulls from `CustomRecipeManager` and only checks
`CustomRecipe.isValidForBuilding`. So **every** crafter key in `recipe_audit.csv`
accepts datapack `crafterrecipes` — no 2x2 grid limit, no recipe-slot limit, no
product/ingredient tag check. Those gates only apply to in-game teaching.

What actually differs is the **recipe family** the colonist can perform:

| family | modules | source recipe types |
|---|---|---|
| table | `*_crafting`, `lumberjack_custom`, `blacksmith_smithing_template_crafting` | crafting_shaped/shapeless |
| furnace | `*_smelting` | smelting/blasting/smoking/campfire_cooking |
| farmersdelight | `chef_farmers_cooking`, `chef_farmers_cutting_knife`, `chef_farmers_plating`, `lumberjack_farmers_cutting_axe`, `stonemason_farmers_cutting_pickaxe` | `farmersdelight:cooking`, `farmersdelight:cutting` |
| stonecutting | `stonemason_stonecutting` | `minecraft:stonecutting` |
| smithing | `blacksmith_smithing` | `minecraft:smithing_transform` |

`MineColonies_Compatibility-1.21.1-3.56.jar` (steve_gall) adds the FD, stonecutter
and smithing modules. Cooking/Cutting/Plating/Stonecutter/Smithing all extend
`AbstractCraftingBuildingModule$Custom`; `SmithingTemplateCraftingModule` extends
`$Crafting`. All get a request resolver, so all serve colony requests.

Caveat: in-game teaching yields a `CookingRecipeStorage`, a datapack recipe yields
a plain `RecipeStorage`. `CookingCraftingModule.getCraftingTime` uses a **guarded
`instanceof`**, so no crash - just default crafting time instead of FD's. Unverified
in-game whether cooking-pot work completes normally.

**Builder and Miner are the exception that matters:** both use
`SimpleCraftingModule`, which overrides `createResolvers()` to return an empty list
and `getCraftingJob()` to return null. **They cannot serve colony requests at all** -
their recipes only ever feed their own work. Colony-wide tool supply has to come
from the Blacksmith. Used by [[colony-autoteach-datapack]].
