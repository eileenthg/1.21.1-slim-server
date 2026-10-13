# Merge table (generated from report.json)

Kept item = the one that stays in the game; the others turn into it everywhere (recipes, loot, colony workers). Removed = recipes switched off in `zz-unified-items.zip`.

| Kept item | Merged into it | Why this one | Recipes left | Removed |
|---|---|---|---|---|
| `extradelight:soybeans` | `minecolonies:soybean`, `youkaishomecoming:soybean` | ED pod shucking pipeline | `extradelight:cutting/shucked_soybeans_knife` | 2 |
| `ramadandelight:chickpea` | `minecolonies:chickpea` | real crop with wild plant | `ramadandelight:integration/immersiveengineering/cloche/chickpea` | 1 |
| `extradelight:corn_seeds` | `brewinandchewin:corn_kernels`, `culturaldelights:corn_kernels`, `corn_delight:corn_seeds`, `hauntedharvest:kernels` | ED husk/cob/kernel cutting chain | (harvest/loot only) | 5 |
| `extradelight:corn_on_cob` | `brewinandchewin:corn`, `hauntedharvest:corn`, `minecolonies:corn`, `corn_delight:corn`, `culturaldelights:corn_cob` | ED corn crop | (harvest/loot only) | 1 |
| `extradelight:corn_cob` | `corn_delight:corncob` | leftover cob from ED kernel cutting | (harvest/loot only) | 0 |
| `extradelight:corn_meal` | `brewinandchewin:cornmeal`, `minecolonies:cornmeal` | ED mortar/grinding | `extradelight:mortar/cornmeal_oil_corn_seeds` | 2 |
| `extradelight:cooked_corn` | `brewinandchewin:cooked_corn` | cooking pot vs furnace | `create:mixing/corn`, `farmersdelight:corn` | 3 |
| `extradelight:ginger` | `culturaldelights:ginger`, `snowyspirit:ginger` | ED crop + processing | (harvest/loot only) | 0 |
| `extradelight:ginger_crate` | `snowyspirit:ginger_crate` | same storage block | (harvest/loot only) | 1 |
| `extradelight:chili` | `expandeddelight:chili_pepper`, `eternal_tales:chili_pepper` | ED drying/slicing/powder chain | (harvest/loot only) | 1 |
| `extradelight:chili_seeds` | `expandeddelight:chili_pepper_seeds`, `eternal_tales:chili_pepper_seeds` | follows chili | `extradelight:chili_seeds` | 2 |
| `extradelight:chili_crate` | `expandeddelight:chili_pepper_crate`, `eternal_tales:chili_pepper_crate` | follows chili | (harvest/loot only) | 2 |
| `extradelight:peanuts` | `expandeddelight:peanut` | ED crop + nut butter chain | `extradelight:cutting/peanuts` | 0 |
| `extradelight:peanut_butter_bottle` | `delightful:nut_butter_bottle` | ED fluid then bottling | (harvest/loot only) | 2 |
| `youkaishomecoming:tea_leaves` | `eternal_tales:tea_leaves` | Youkai tea processing chain | (harvest/loot only) | 1 |
| `youkaishomecoming:tea_seeds` | `eternal_tales:tea_seeds` | follows tea | `eternal_tales:shed_raccoon_tea_seeds` | 0 |
| `youkaishomecoming:green_tea_leaves` | `delightful:green_tea_leaf` | steamed from tea leaves | `youkaishomecoming:green_tea_leaves_from_tea_leaves_steaming` | 0 |
| `youkaishomecoming:matcha` | `delightful:matcha` | ground from steamed leaves | `youkaishomecoming:cutting/green_tea_leaves` | 0 |
| `extradelight:salt` | `butchery:salt`, `eternal_tales:salt`, `expandeddelight:salt`, `compatdelight:salt` | ED evaporator | (harvest/loot only) | 2 |
| `extradelight:cooking_oil` | `rusticdelight:cooking_oil` | ED oil fluid pressing then bottling | (harvest/loot only) | 1 |
| `extradelight:mint` | `minecolonies:mint` | ED crop | (harvest/loot only) | 1 |
| `extradelight:ice_cubes` | `youkaishomecoming:ice_cube` | same process; ED chiller ecosystem | `extradelight:cutting/ice_cubes_axe` | 1 |
| `farmersdelight:wheat_dough` | `create:dough`, `minecolonies:bread_dough` | base dough | `create:mixing/wheat_dough`, `create:mixing/wheat_dough_egg`, `extradelight:mixing/wheat_dough` +1 | 7 |
| `culturaldelights:corn_dough` | `brewinandchewin:corn_dough` | corn dough (split out of c:dough) | `culturalrecipes:corn_dough` | 2 |
| `farmersdelight:raw_pasta` | `minecolonies:raw_noodle` | dough cutting / ED shaping station | `farmersdelight:cutting/tag_dough`, `extradelight:raw_pasta` | 1 |
| `extradelight:cooked_pasta` | `minecolonies:pasta_plain` | cooked in pot | `create:mixing/cooked_pasta`, `farmersdelight:cooked_pasta` | 1 |
| `extradelight:cheese` | `minecolonies:cheddar_cheese` | curdled with vinegar | `create:mixing/cheese_vinegar`, `farmersdelight:cheese_vinegar` | 1 |
| `extradelight:soy_sauce_item` | `minecolonies:soysauce`, `youkaishomecoming:soy_sauce_bottle` | ED multi-stage vat fermentation | `extradelight:vat/soy_sauce_item` | 1 |
| `extradelight:coffee_beans` | `youkaishomecoming:coffee_beans`, `rusticdelight:roasted_coffee_beans` | ED oven roasting | `extradelight:cooking/oven/oven/bulk/coffee_oven1`, `extradelight:cooking/oven/oven/bulk/coffee_oven2`, `extradelight:cooking/oven/oven/bulk/coffee_oven3` +6 | 8 |
| `butchery:animal_fat` | `amcdelight:animal_fat`, `delightful:animal_fat` | carcass butchering | `amcdelight:cutting/blubber_to_fat` | 0 |
| `aquaculture:fish_bones` | `alexsmobs:fish_bones`, `crabbersdelight:fish_bones` | fillet knife processing | (harvest/loot only) | 0 |
| `alexsmobs:fish_oil` | `eternal_tales:fish_oil` | rendered from blobfish | `alexsmobs:fish_oil` | 0 |
| `crabbersdelight:shrimp` | `eternal_tales:shrimp`, `anotherscape:shrimps` | trap-caught | (harvest/loot only) | 0 |
| `eternal_tales:caviar` | `delightful:caviar` | only loadable recipe (Delightful keg recipe needs Farmer's Respite) | `eternal_tales:caviar_craft` | 0 |
| `alexsmobs:cooked_catfish` | `eternal_tales:cooked_catfish` | same food | `alexsmobs:cooked_catfish`, `alexsmobs:cooked_catfish_campfire`, `alexsmobs:cooked_catfish_smoke` | 1 |
| `amcdelight:cooked_catfish_slice` | `alexsdelight:cooked_catfish_slice` | knife cutting | `amcdelight:cutting/cooked_catfish_slice` | 6 |
| `aquaculture:fish_fillet_cooked` | `amcdelight:cooked_fish_fillet` | fillet system | `aquaculture:cooked_fish_fillet`, `aquaculture:cooked_fish_fillet_from_campfire`, `aquaculture:cooked_fish_fillet_from_smoking` | 4 |
| `butchery:raw_sausage` | `brewinandchewin:raw_sausage` | meat grinder + intestines | `butchery:sausagerecipe` | 1 |
| `butchery:cooked_sausage` | `brewinandchewin:cooked_sausage` | follows raw sausage | `butchery:sausagerecipe_1`, `butchery:sausagerecipe_2`, `butchery:sausagerecipe_3` | 3 |
| `butchery:bottle_of_blood` | `eternal_tales:bottle_of_blood`, `youkaishomecoming:blood_bottle` | drained carcass blood | `draft:bloodtobottles` | 1 |
| `frightsdelight:bone_shard` | `eternal_tales:bone_shard` | knife-cut bones | (harvest/loot only) | 0 |
| `mythicmetals:raw_silver` | `eternal_tales:raw_silver` | Mythic Metals ore system | (harvest/loot only) | 1 |
| `mythicmetals:raw_silver_block` | `eternal_tales:raw_silver_block` | metal | `mythicmetals:blocks/raw_silver_block` | 1 |
| `mythicmetals:silver_nugget` | `eternal_tales:silver_nugget` | metal | `mythicmetals:blasting/silver_nugget_from_armor`, `mythicmetals:crafting/silver_nugget`, `mythicmetals:smelting/silver_nugget_from_armor` | 1 |
| `mythicmetals:silver_block` | `eternal_tales:silver_block` | metal | `mythicmetals:blocks/silver_block` | 1 |
| `mythicmetals:silver_ingot` | `eternal_tales:silver_ingot` | metal | `mythicmetals:alloy_forge/forge_silver_ingot_from_ores`, `mythicmetals:alloy_forge/forge_silver_ingot_from_raw_ore`, `mythicmetals:blasting/silver_from_ores` +3 | 5 |
| `mythicmetals:raw_tin` | `eternal_tales:raw_tin` | metal | (harvest/loot only) | 0 |
| `mythicmetals:raw_tin_block` | `eternal_tales:raw_tin_block` | metal | `mythicmetals:blocks/raw_tin_block` | 1 |
| `mythicmetals:tin_nugget` | `eternal_tales:tin_nugget` | metal | `mythicmetals:crafting/tin_nugget` | 1 |
| `mythicmetals:tin_block` | `eternal_tales:tin_block` | metal | `mythicmetals:blocks/tin_block` | 1 |
| `mythicmetals:tin_ingot` | `eternal_tales:tin_ingot` | metal | `mythicmetals:alloy_forge/forge_tin_ingot_from_ores`, `mythicmetals:alloy_forge/forge_tin_ingot_from_raw_ore`, `mythicmetals:blasting/tin_from_ores` +3 | 4 |
| `mythicmetals:adamantite_ingot` | `eternal_tales:adamantite_ingot` | alloy forge | `mythicmetals:alloy_forge/forge_adamantite_ingot_from_ores`, `mythicmetals:alloy_forge/forge_adamantite_ingot_from_raw_ore`, `mythicmetals:blasting/adamantite_from_ores` +1 | 1 |
| `mythicmetals:adamantite_nugget` | `eternal_tales:adamantite_nugget` | metal | `mythicmetals:blasting/adamantite_nugget_from_armor`, `mythicmetals:crafting/adamantite_nugget` | 1 |
| `mythicmetals:adamantite_block` | `eternal_tales:adamantite_block` | metal | `mythicmetals:blocks/adamantite_block` | 1 |
| `snowyspirit:candy_cane` | `eternal_tales:candy_cane` | only crafted one | `snowyspirit:candy_cane` | 0 |
| `extradelight:eggnog` | `snowyspirit:eggnog` | cooking pot with spices | `create:mixing/eggnog`, `farmersdelight:eggnog` | 1 |
| `extradelight:gingerbread_cookie` | `snowyspirit:gingerbread_cookie` | dough then oven | `extradelight:cooking/oven/gingerbread_cookie` | 1 |
| `tanca:hot_chocolate` | `spectrum:hot_chocolate` | heated Create mixing; TAN warming | `tanca:filling/hot_chocolate`, `tanca:mixing/hot_chocolate` | 1 |
| `toughasnails:cactus_juice` | `extradelight:cactus_juice` | purified water base; TAN thirst | `tanca:filling/cactus_juice`, `toughasnails:cactus_juice` | 1 |
| `toughasnails:apple_juice` | `expandeddelight:apple_juice` | purified water base; TAN thirst | `tanca:filling/apple_juice`, `toughasnails:apple_juice` | 1 |
| `toughasnails:glow_berry_juice` | `extradelight:glow_berry_juice`, `expandeddelight:glow_berry_juice` | purified water base; TAN thirst | `tanca:filling/glow_berry_juice`, `toughasnails:glow_berry_juice` | 2 |
| `toughasnails:melon_juice` | `farmersdelight:melon_juice` | purified water base; TAN thirst | `tanca:filling/melon_juice`, `toughasnails:melon_juice` | 1 |
| `toughasnails:sweet_berry_juice` | `extradelight:sweet_berry_juice`, `expandeddelight:sweet_berry_juice` | purified water base; TAN thirst | `tanca:filling/sweet_berry_juice`, `toughasnails:sweet_berry_juice` | 2 |
| `extradelight:ice_cream` | `toughasnails:ice_cream`, `mctradepost:icecream` | chiller | `minecraft:chilling/ice_cream` | 3 |
| `extradelight:coffee` | `rusticdelight:coffee` | roast, grind, brew | `create:mixing/coffee`, `farmersdelight:coffee` | 1 |
| `extradelight:tea` | `eternal_tales:tea_cup` | brewed in pot | `create:mixing/tea`, `farmersdelight:tea` | 1 |
| `spectrum:azalea_tea` | `delightful:azalea_tea` | potion workshop; Delightful pot recipe does not load | `spectrum:potion_workshop_crafting/azalea_tea`, `spectrum:potion_workshop_crafting/azalea_tea_milk` | 0 |
| `brewinandchewin:mead` | `youkaishomecoming:mead` | keg fermentation | `brewinandchewin:fermenting/mead_from_honey` | 0 |
| `brewinandchewin:red_wine` | `youkaishomecoming:red_wine` | keg fermentation | `brewinandchewin:fermenting/red_wine_from_water` | 0 |
| `brewinandchewin:white_wine` | `youkaishomecoming:white_wine` | keg fermentation | `brewinandchewin:fermenting/white_wine_from_water` | 0 |
| `brewinandchewin:red_grape_seeds` | `youkaishomecoming:red_grape_seeds` | same seeds | `brewinandchewin:red_grape_seeds` | 1 |
| `brewinandchewin:white_grape_seeds` | `youkaishomecoming:white_grape_seeds` | same seeds | `brewinandchewin:white_grape_seeds` | 1 |
| `youkaishomecoming:affogato` | `extradelight:affogato` | moka pot | `youkaishomecoming:cooking/affogato`, `youkaishomecoming:cooking/affogato_remix` | 2 |
| `extradelight:candy_apple` | `youkaishomecoming:candy_apple` | caramel candy pipeline | `create:mixing/candy_apple`, `create:mixing/candy_apple_sugar`, `farmersdelight:candy_apple` +1 | 1 |
| `extradelight:caramel_popcorn` | `corn_delight:caramel_popcorn` | popped corn + melted caramel | `extradelight:caramel_popcorn` | 2 |
| `extradelight:cheeseburger` | `delightful:cheeseburger` | pickles, processed veg | `extradelight:cheeseburger`, `extradelight:cheeseburger_burger` | 4 |
| `extradelight:cheese_sandwich` | `expandeddelight:cheese_sandwich` | shaped with sliced bread | `extradelight:cheese_sandwich` | 1 |
| `extradelight:grilled_cheese` | `expandeddelight:grilled_cheese` | oven | `extradelight:cooking/oven/oven/bulk/grilled_cheese1`, `extradelight:cooking/oven/oven/bulk/grilled_cheese2`, `extradelight:cooking/oven/oven/bulk/grilled_cheese3` +6 | 4 |
| `extradelight:macaroni_cheese` | `expandeddelight:mac_and_cheese` | oven feast | `extradelight:feast/mac_cheese_bowl_feast` | 1 |
| `extradelight:chocolate_cookie` | `expandeddelight:chocolate_cookie` | dough then oven | `extradelight:cooking/oven/chocolate_cookie` | 1 |
| `extradelight:sugar_cookie` | `expandeddelight:sugar_cookie` | dough then oven | `extradelight:cooking/oven/sugar_cookie` | 1 |
| `extradelight:glow_berry_cookie` | `eternal_tales:glowberry_cookie` | dough then oven | `extradelight:cooking/oven/glow_berry_cookie` | 1 |
| `extradelight:smore` | `delightful:smore` | graham, chocolate bar, marshmallow | `extradelight:smore` | 2 |
| `extradelight:fried_chicken` | `rusticdelight:fried_chicken` | breading station | `create:mixing/fried_chicken`, `farmersdelight:fried_chicken` | 1 |
| `extradelight:fried_fish` | `crabbersdelight:fried_fish`, `rusticdelight:fried_fish` | breading station | `create:mixing/fried_fish`, `farmersdelight:fried_fish` | 2 |
| `extradelight:fried_mushrooms` | `rusticdelight:fried_mushrooms` | breading station | `create:mixing/fried_mushrooms`, `farmersdelight:fried_mushrooms` | 1 |
| `rusticdelight:fried_calamari` | `amcdelight:fried_calamari` | oil + batter in pot | `rusticdelight:cooking/fried_calamari` | 3 |
| `extradelight:potato_salad` | `rusticdelight:potato_salad` | mixing bowl | `create:mixing/potato_salad`, `extradelight:mixing/potato_salad` | 1 |
| `rusticdelight:spring_rolls` | `festive_delight:spring_rolls` | oil + dough in pot | `rusticdelight:cooking/spring_rolls` | 0 |
| `extradelight:toast` | `mynethersdelight:toasts` | oven | `extradelight:cooking/oven/oven/bulk/toast1`, `extradelight:cooking/oven/oven/bulk/toast2`, `extradelight:cooking/oven/oven/bulk/toast3` +6 | 6 |
| `extradelight:bread_slice` | `mynethersdelight:slices_of_bread` | knife | `extradelight:cutting/bread_knife` | 1 |
| `farmersdelight:pumpkin_pie_slice` | `extradelight:pumpkin_pie_slice` | base mod, same cut | `farmersdelight:cutting/pumpkin_pie` | 1 |
| `extradelight:pumpkin_roll` | `brewinandchewin:slice_of_pumpkin_roll` | oven feast | `extradelight:cutting/pumpkin_roll_knife`, `extradelight:feast/pumpkin_roll_feast_knife_feast` | 1 |
| `extradelight:quiche` | `brewinandchewin:quiche`, `ramadandelight:quiche` | omelette mix + crust in oven | `extradelight:cooking/oven/quiche` | 3 |
| `extradelight:quiche_slice` | `brewinandchewin:quiche_slice`, `ramadandelight:quiche_slice` | follows quiche | `extradelight:cutting/quiche_knife` | 2 |
| `extradelight:rice_pudding` | `brewinandchewin:rice_pudding`, `large_meals:rice_pudding` | cooking pot | `create:mixing/rice_pudding`, `farmersdelight:rice_pudding` | 2 |
| `extradelight:riceball` | `culturaldelights:rice_ball`, `minecolonies:rice_ball` | fish flakes | `extradelight:riceball` | 2 |
| `extradelight:miso_soup` | `youkaishomecoming:miso_soup` | broth + fermented miso | `farmersdelight:miso_soup` | 1 |
| `youkaishomecoming:longevity_noodles` | `festive_delight:longevity_noodle` | cooking pot | `youkaishomecoming:cooking/longevity_noodles` | 0 |
| `artifactdelight:meatloaf` | `spectrum:meatloaf` | minced beef in pot | `artifactdelight:cooking/meatloaf` | 2 |
| `extradelight:meatloaf` | `artifactdelight:meatloaf_plate` | feast serving | `extradelight:feast/meatloaf_bowl_feast` | 0 |
| `extradelight:meatloaf_sandwich` | `spectrum:meatloaf_sandwich` | feast serving | `extradelight:feast/meatloaf_sandwich_feast`, `extradelight:feast/meatloaf_sandwich_slice_feast` | 2 |
| `extradelight:fish_cakes` | `spectrum:fishcake` | cooking pot | `create:mixing/fish_cakes`, `farmersdelight:fish_cakes` | 1 |
| `extradelight:fish_chips` | `minecolonies:fish_n_chips` | fried fish + fries + vinegar | `extradelight:fish_and_chips` | 1 |
| `crabbersdelight:fish_sandwich` | `eternal_tales:fish_sandwich` | cooked fish + bread | `crabbersdelight:fish_sandwich` | 1 |
| `extradelight:congee` | `minecolonies:congee` | cooking pot | `create:mixing/congee`, `farmersdelight:congee` | 1 |
| `extradelight:corn_chowder` | `minecolonies:corn_chowder` | cooking pot | `create:mixing/corn_chowder`, `farmersdelight:corn_chowder` | 2 |
| `extradelight:potato_soup` | `large_meals:potato_soup`, `minecolonies:potato_soup`, `youkaishomecoming:potato_soup` | cooking pot | `create:mixing/potato_soup`, `farmersdelight:potato_soup` | 2 |
| `extradelight:borscht` | `minecolonies:borscht`, `youkaishomecoming:borscht` | cooking pot | `create:mixing/borscht`, `farmersdelight:borscht` | 0 |
| `farmersdelight:apple_pie` | `minecolonies:apple_pie` | oven pie | `extradelight:cooking/oven/apple_pie`, `extradelight:cooking/oven/apple_pie_stewed` | 2 |
| `farmersdelight:fried_rice` | `minecolonies:fried_rice` | cooking pot | `farmersdelight:cooking/fried_rice`, `hauntedharvest:fried_rice_cooking` | 1 |
| `farmersdelight:cooked_rice` | `minecolonies:cooked_rice` | cooking pot | `farmersdelight:cooking/cooked_rice` | 1 |
| `farmersdelight:vegetable_soup` | `minecolonies:veggie_soup` | cooking pot | `farmersdelight:cooking/vegetable_soup` | 1 |
| `ramadandelight:kebab` | `minecolonies:kebab` | raw kebab then grill | `ramadandelight:kebab_from_campfire_cooking`, `ramadandelight:kebab_from_smelting`, `ramadandelight:kebab_from_smoking` | 1 |
| `ramadandelight:flat_bread` | `minecolonies:flatbread` | dough then bake | `ramadandelight:flat_bread_from_campfire_cooking`, `ramadandelight:flat_bread_from_smelting`, `ramadandelight:flat_bread_from_smoking` | 0 |
| `corn_delight:taco` | `minecolonies:tacos` | tortilla assembly | `ramadandelight:compat/corndelight/taco`, `corn_delight:taco` | 0 |
| `culturaldelights:tortilla` | `brewinandchewin:tortilla`, `corn_delight:tortilla`, `minecolonies:tortillas` | corn dough then cook | `culturalrecipes:smelting/tortilla`, `culturalrecipes:smelting/tortilla_from_campfire`, `culturalrecipes:smelting/tortilla_from_smoking` | 5 |
| `culturaldelights:tortilla_chips` | `corn_delight:tortilla_chip` | knife cut | `culturalrecipes:cutting/tortilla_chips` | 2 |
| `extradelight:cream_corn` | `corn_delight:creamed_corn`, `culturaldelights:creamed_corn` | cooking pot | `create:mixing/cream_corn`, `farmersdelight:cream_corn` | 2 |
| `extradelight:cornbread` | `brewinandchewin:corn_bread`, `corn_delight:cornbread`, `hauntedharvest:cornbread` | oven | `extradelight:feast/cornbread_bowl_feast` | 6 |
| `mynethersdelight:stuffed_pepper` | `minecolonies:stuffed_pepper` | player recipe | `mynethersdelight:crafting/stuffed_pepper` | 0 |
| `extradelight:pasta_tomato` | `minecolonies:pasta_tomato` | mixing bowl | `create:mixing/pasta_tomato`, `extradelight:mixing/pasta_tomato` | 1 |
| `extradelight:sweet_berry_custard` | `large_meals:sweet_berry_custard` | cooking pot | `create:mixing/sweet_berry_custard`, `farmersdelight:sweet_berry_custard` | 1 |
| `endersdelight:chicken_curry` | `large_meals:chicken_curry` | spiced cooking pot | `endersdelight:cooking/chicken_curry` | 1 |
| `mynethersdelight:spicy_curry` | `culturaldelights:spicy_curry` | cooking pot | `mynethersdelight:cooking/spicy_curry` | 1 |
| `extradelight:hot_wings` | `mynethersdelight:hot_wings` | mixing bowl with sauce | `extradelight:mixing/hot_wings_mixing` | 2 |
| `amcdelight:kangaroo_stew` | `alexsdelight:kangaroo_stew` | cooked cuts in pot | `amcdelight:cooking/kangaroo_stew` | 1 |
| `minersdelight:stuffed_squid` | `oceansdelight:stuffed_squid` | cooking pot | `minersdelight:cooking/stuffed_squid` | 1 |
| `minersdelight:tentacles_on_a_stick` | `oceansdelight:tentacle_on_a_stick` | more ingredients | `minersdelight:tentacles_on_a_stick` | 1 |
| `brewinandchewin:croissant` | `cluttered:croissant` | shaped raw croissant then bake | `brewinandchewin:croissant_from_campfire_cooking`, `brewinandchewin:croissant_from_smelting`, `brewinandchewin:croissant_from_smoking` | 1 |
| `expandeddelight:sweet_roll` | `create:sweet_roll` | dough, cinnamon, milk | `expandeddelight:sweet_roll` | 1 |
| `extradelight:cactus_soup` | `delightful:cactus_soup` | cooking pot | `create:mixing/cactus_soup`, `farmersdelight:cactus_soup` | 1 |

## MineColonies crafting-table recipes turned into colony-worker recipes

| Old player recipe | Worker now makes | Worker |
|---|---|---|
| `minecolonies:cornmeal` | `extradelight:corn_meal` | baker_crafting |
| `minecolonies:raw_noodle` | `farmersdelight:raw_pasta` | chef_crafting |
| `minecolonies:pasta_plain` | `extradelight:cooked_pasta` | chef_crafting |
| `minecolonies:cheddar_cheese` | `extradelight:cheese` | chef_crafting |
| `minecolonies:soysauce` | `extradelight:soy_sauce_item` | chef_crafting |
| `minecolonies:rice_ball` | `extradelight:riceball` | chef_crafting |
| `minecolonies:fish_n_chips` | `extradelight:fish_chips` | chef_crafting |
| `minecolonies:congee` | `extradelight:congee` | chef_crafting |
| `minecolonies:corn_chowder` | `extradelight:corn_chowder` | chef_crafting |
| `minecolonies:soy_corn_chowder` | `extradelight:corn_chowder` | chef_crafting |
| `minecolonies:potato_soup` | `extradelight:potato_soup` | chef_crafting |
| `minecolonies:apple_pie` | `farmersdelight:apple_pie` | baker_crafting |
| `minecolonies:fried_rice` | `farmersdelight:fried_rice` | chef_crafting |
| `minecolonies:cooked_rice` | `farmersdelight:cooked_rice` | chef_crafting |
| `minecolonies:veggie_soup` | `farmersdelight:vegetable_soup` | chef_crafting |
| `minecolonies:mutton_kebab` | `ramadandelight:kebab` | chef_crafting |
| `minecolonies:tortillas` | `culturaldelights:tortilla` | chef_crafting |
| `minecolonies:pasta_tomato` | `extradelight:pasta_tomato` | chef_crafting |
