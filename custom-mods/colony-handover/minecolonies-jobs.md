# MineColonies jobs and crafting — reference

Source: MineColonies `version/1.21` (commit `a8022f70`, 2026-08-30) and the shipped jar
`minecolonies-1.1.1375-1.21.1-snapshot`. Job ids are `minecolonies:<id>`.
Verify against upstream before relying on details; snapshots ship almost daily.

---

## How crafting works (applies to every crafter)

- **Crafting is a building module, not a job property.** Each hut registers modules in
  `ModBuildingsInitializer`; the recipe rules live in inner classes such as
  `BuildingBaker.CraftingModule` (`core/colony/buildings/workerbuildings/`).
- **Module kinds** (`AbstractCraftingBuildingModule`):
  - `Crafting` — crafting-table recipes (2x2 and 3x3), player-taught. No intermediate block.
  - `Smelting` — furnace recipes, player-taught. Hut has a `FURNACE` module and fuel list.
  - `Brewing` — brewing-stand recipes (Alchemist only).
  - `Domum` ("DO") — Domum Ornamentum Architect's Cutter recipes.
  - `Custom` — fixed, data-driven recipes that cannot be taught; loaded from
    `data/minecolonies/crafterrecipes/<job>/*.json`.
- **Who may learn a recipe** is decided by item tags in `data/minecolonies/tags/item/`:
  `<job>_product`, `<job>_ingredient`, plus `_excluded` variants
  (`CraftingUtils.isRecipeCompatibleBasedOnTags`: output tag is checked first, then inputs).
  Some huts add a Java fallback when no tag matches (noted per job below).
- **Recipe slots:** `2^buildingLevel`, multiplied by the Recipes research effect, and further
  multiplied if the module "can learn many recipes". Builder and Miner cannot.
- **Custom recipes are pre-taught**: they appear automatically and do not use slots.
- Crafters fulfil colony **requests**, not stock targets. Other citizens ask for an item,
  the request system finds a hut whose recipes produce it, and that worker crafts it.

---

## Crafting jobs

| Job (id) | Hut | Modules | What it makes / accepts |
|---|---|---|---|
| **Baker** (`baker`) | Bakery | Crafting + Smelting | Tag-only (`baker_product` e.g. cornmeal, cheese pizza, cheesecake, apple pie; ingredient `#c:crops/wheat`). **Teachable only from building level 3.** 25 custom recipes: doughs → breads (bread, manchet, flatbread, golden/chorus/milky/sugary bread), cake and cake batter, cookies, muffins, pumpkin pie, lembas scone, mushroom pizza, water jug/bottle. Stockpiles up to 128 of each input. |
| **Blacksmith** (`blacksmith`) | Blacksmith's Hut | Crafting | Any recipe whose output is a tool, weapon, shield, shears or armour (vanilla equipment types, plus Tinkers weapons), **but never if leather is an input**. Otherwise tag rules (`blacksmith_product`: shears, lightning rod, mace, nuggets, ingots). Custom: netherite tool/armour upgrades, plate armour set, iron/diamond assistant hammers, iron sifter mesh. |
| **Stonemason** (`stonemason`) | Stonemason's Hut | Crafting + Domum | Tag-only: decorative stone (stone/deepslate/blackstone/tuff bricks, polished and chiseled variants, quartz, nether bricks, dripstone…). Domum cutter work on stonemason-tagged materials. Custom: sandstone, red sandstone, prismarine and prismarine bricks, end stone, flint sifter mesh. |
| **Stone Smelter** (`stonesmeltery`) | Stone Smeltery | Smelting | Tag-only. Smelts stone-type blocks: smooth stone/sandstone/quartz/basalt, terracotta and glazed terracotta, bricks, nether brick, cracked bricks, charcoal/coal, sponge, popped chorus. |
| **Sawmill** (`sawmill`) | Sawmill | Crafting + Domum | Tag rules first (`sawmill_product`: planks, wooden slabs/stairs, boats, bamboo mosaic, MineColonies barrel). **Fallback:** any recipe whose inputs are mostly (above a threshold) planks, logs or `*wood*`-tagged items. Domum work on wood. |
| **Fletcher** (`fletcher`) | Fletcher's Hut | Crafting + Domum | Tag rules (`fletcher_product`: strings, moss carpet; inputs include string, leaves, wool, leather, rabbit hide). **Fallback:** arrows and dyed leather armour. Custom: string, flint, string sifter mesh. |
| **Glassblower** (`glassblower`) | Glassblower's Hut | Crafting + Smelting + Domum | Crafting: anything from glass blocks/panes (stained glass, panes, bottles…). Smelting: sand → glass (`glassblower_smelting_product` = glass blocks). Domum work on glass. |
| **Dyer** (`dyer`) | Dyer's Hut | Crafting + Smelting | Crafting tag-only (`dyer_product`: dyes, firework star, red nether bricks, coloured Domum bricks and cobble). Smelting produces dyes (e.g. cactus → green). Special behaviour: turns any coloured wool back into white wool with white dye when white wool is requested. Can dye leather armour. Custom: black dye, red sand, dark prismarine. |
| **Mechanic** (`mechanic`) | Mechanic's Hut | Crafting + Domum | Tag rules (`mechanic_product`: storage blocks, rails, buttons, pressure plates, packed/blue ice, daylight detector, comparator…; redstone-type ingredients). **Fallback:** minecarts and hoppers. Domum: everything the sawmill, stonemason, fletcher and glassblower do *not* handle. Custom: wooden and iron gates, rails, lanterns and soul lanterns, copper de-oxidising, diamond sifter mesh. |
| **Chef** (`chef`) | Kitchen | Crafting + Smelting | Food. Tag rules (`cook_product`: cheeses, butter, rice, pasta, stews, sushi…; ingredients: fish, raw meats, potato, kelp, egg, milk bottles). **Fallback:** any recipe whose output is edible, or smelts into something edible. Custom (15 dishes): steak dinner, fish dinner, schnitzel, tacos, ramen, sushi roll, lamb stew, borscht, pita hummus, stuffed pita/pepper, hand pie, eggplant dolma, spicy eggplant, stew trencher. |
| **Cook assistant** (`cookassistant`) | — | — | **Deprecated**: folded into Chef but still registered, so old colonies may still have it. |
| **Planter** (`planter`) | Plantation | Crafting + plantation fields | Grows field crops (sugar cane, bamboo, cactus, kelp, cocoa, vines…) on the plantation's fields. Crafting is tag-only: paper, book, writable book, sugar, Domum paper. |
| **Farmer** (`farmer`) | Farm | Crafting (tag rules) + fields | Farms scarecrow fields. Tag rules: hay bale, seeds, composted dirt, melon, coarse dirt, fermented spider eye, glistering melon, mud bricks and packed mud. Custom: carved pumpkin, mud. |
| **Smelter** (`smelter`) | Smeltery | Smelting (hidden) + ore breaking | Not player-taught. Smelts **every smeltable ore automatically**. Ore-breaking module: breaks `#minecolonies:breakable_ore` blocks via their loot tables using a diamond pickaxe with Fortune = building level − 1. |
| **Crusher** (`crusher`) | Crusher | Custom only | Fixed chain: cobblestone → gravel → sand → clay; also tuff and bone meal. Settings choose the target product. Custom recipes: cobble, gravel, sand, clay, clay ball, tuff, bone meal. |
| **Sifter** (`sifter`) | Sifter | Custom only | Sifts gravel/sand/soul sand with meshes (string, flint, iron, diamond); better meshes give better loot. Outputs come from loot tables. |
| **Concrete Mixer** (`concretemixer`) | Concrete Mixer | Custom only | All 16 concrete powders and 16 concretes (powder placed in water and mined). Nothing can be taught. |
| **Lumberjack / Forester** (`lumberjack`) | Forester's Hut | Custom only | Fells and replants trees (sapling list). Custom: stripped logs, stripped stems, stripped bamboo block. |
| **Enchanter** (`enchanter`) | Enchanter's Tower | Custom only | Drains XP from other citizens at chosen huts ("stations"), then makes enchanted books (loot tables enchanter1–5 by building level). Custom: ancient tomes 1–5 and colony scrolls (teleport, area teleport, guard help, highlight). Cannot be taught. |
| **Alchemist** (`alchemist`) | Alchemist Tower | Brewing + limited crafting | Brews any potion (brewing-stand recipes). Crafting only allows MineColonies' magic potion. Also harvests mistletoe (oak leaves + shears) and grows nether wart on soul sand. |
| **Nether Worker** (`netherworker`) | Nether Mine | Custom (expeditions) | Walks through a nether portal on timed "trips"; loot comes from expedition tables (`trip1`–`trip5`, by level) and needs food, armour and tools. Also a lava recipe. |
| **Builder** (`builder`) | Builder's Hut | Simple crafting | Builds and upgrades structures from work orders. Can learn only a few **2x2** recipes (slot count not boosted). |
| **Miner** (`miner`) | Mine | Simple crafting | Digs a mine shaft level by level, bringing up ores and stone. Can learn only a few 2x2 recipes. |
| **Quarrier** (`quarrier`) | Simple/Medium Quarry | — | Digs quarry schematics into the ground. The quarry buildings are served by Miner huts (the large quarry is commented out in this source). |

---

## Non-crafting jobs (no recipes)

| Job (id) | Hut | Role |
|---|---|---|
| Courier (`deliveryman`) | Courier's Hut, Warehouse | Moves items between huts and the warehouse; fulfils requests. |
| Fisherman (`fisherman`) | Fisher's Hut | Fishes in nearby water. |
| Shepherd (`shepherd`) | Shepherd's Hut | Breeds and shears sheep; wool, mutton. |
| Cowhand (`cowboy`) | Cowhand's Hut | Breeds and milks cows; beef, leather, milk. |
| Swineherd (`swineherder`) | Swineherd's Hut | Breeds pigs; porkchops. |
| Chicken Farmer (`chickenherder`) | Chicken Farmer's Hut | Breeds chickens; eggs, feathers, chicken. |
| Rabbit Herder (`rabbitherder`) | Rabbit Hutch | Breeds rabbits; hide, meat, feet. |
| Stablemaster (`stablemaster`) | Stable | Breeds horses; the stable also houses cavalry. |
| Beekeeper (`beekeeper`) | Apiary | Tends hives; honeycomb and honey bottles. |
| Composter (`composter`) | Composter's Hut | Turns compostables in barrels into compost and dirt. |
| Florist (`florist`) | Flower Shop | Grows the flowers selected in its list using compost (compost from the Composter). |
| Cook (`cook`) | Restaurant | Cooks food in furnaces and serves meals to citizens from its menu. Does **not** craft complex dishes (the Chef does). |
| Knight (`knight`), Ranger (`ranger`), Druid (`druid`), Huscarl (`huscarl`), Marksman (`marksman`), Cavalry (`cavalry`) | Guard Tower, Barracks Tower, Gate House, Stable | Defence. Druids use magic potions from the Alchemist. |
| Combat / archery trainee (`combattraining`, `archertraining`) | Combat Academy, Archery | Raise guard skills. |
| Healer (`healer`) | Hospital | Cures sick and injured citizens, using the requested cure items. |
| Undertaker (`undertaker`) | Graveyard | Buries dead citizens; can attempt resurrection. |
| Researcher (`researcher`) | University | Carries out the colony's research tree. |
| Teacher (`teacher`), Pupil (`pupil`) | School | Teacher raises children's skills (pupils are children). |
| Student (`student`) | Library | Adults read books and paper to level skills. |
| Placeholder (`placeholder`) | — | Internal; not a real job. |

---

## Where to look in source

- Building → module wiring: `apiimp/initializer/ModBuildingsInitializer.java`
- Module definitions: `core/colony/buildings/modules/BuildingModules.java`
- Per-hut recipe rules: `core/colony/buildings/workerbuildings/Building<Name>.java`, inner `CraftingModule` / `SmeltingModule` / `DOCraftingModule`
- Shared logic: `core/colony/buildings/modules/AbstractCraftingBuildingModule.java`, `api/util/CraftingUtils.java`
- Data: `data/minecolonies/tags/item/<job>_{product,ingredient}[_excluded].json`, `data/minecolonies/crafterrecipes/<job>/`, `data/minecolonies/loot_table/recipes/`
