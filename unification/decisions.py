# (dominant, [other members], reason). The dominant keeps its behaviour and its most involved recipe chain.
D = [
    # crops and raw ingredients
    ("extradelight:soybeans", ["minecolonies:soybean", "youkaishomecoming:soybean"], "ED pod shucking pipeline"),
    ("ramadandelight:chickpea", ["minecolonies:chickpea"], "real crop with wild plant"),
    ("extradelight:corn_seeds", ["brewinandchewin:corn_kernels", "culturaldelights:corn_kernels", "corn_delight:corn_seeds", "hauntedharvest:kernels"], "ED husk/cob/kernel cutting chain"),
    ("extradelight:corn_on_cob", ["brewinandchewin:corn", "hauntedharvest:corn", "minecolonies:corn", "corn_delight:corn", "culturaldelights:corn_cob"], "ED corn crop"),
    ("extradelight:corn_cob", ["corn_delight:corncob"], "leftover cob from ED kernel cutting"),
    ("extradelight:corn_meal", ["brewinandchewin:cornmeal", "minecolonies:cornmeal"], "ED mortar/grinding"),
    ("extradelight:cooked_corn", ["brewinandchewin:cooked_corn"], "cooking pot vs furnace"),
    ("extradelight:ginger", ["culturaldelights:ginger", "snowyspirit:ginger"], "ED crop + processing"),
    ("extradelight:ginger_crate", ["snowyspirit:ginger_crate"], "same storage block"),
    ("extradelight:chili", ["expandeddelight:chili_pepper", "eternal_tales:chili_pepper"], "ED drying/slicing/powder chain"),
    ("extradelight:chili_seeds", ["expandeddelight:chili_pepper_seeds", "eternal_tales:chili_pepper_seeds"], "follows chili"),
    ("extradelight:chili_crate", ["expandeddelight:chili_pepper_crate", "eternal_tales:chili_pepper_crate"], "follows chili"),
    ("extradelight:peanuts", ["expandeddelight:peanut"], "ED crop + nut butter chain"),
    ("extradelight:peanut_butter_bottle", ["delightful:nut_butter_bottle"], "ED fluid then bottling"),
    ("youkaishomecoming:tea_leaves", ["eternal_tales:tea_leaves"], "Youkai tea processing chain"),
    ("youkaishomecoming:tea_seeds", ["eternal_tales:tea_seeds", "farmersrespite:tea_seeds"], "follows tea"),
    ("youkaishomecoming:green_tea_leaves", ["delightful:green_tea_leaf", "farmersrespite:green_tea_leaves"], "steamed from tea leaves"),
    ("youkaishomecoming:matcha", ["delightful:matcha"], "ground from steamed leaves"),
    ("extradelight:salt", ["butchery:salt", "eternal_tales:salt", "expandeddelight:salt", "compatdelight:salt"], "ED evaporator"),
    ("extradelight:cooking_oil", ["rusticdelight:cooking_oil"], "ED oil fluid pressing then bottling"),
    ("extradelight:mint", ["minecolonies:mint"], "ED crop"),
    ("extradelight:ice_cubes", ["youkaishomecoming:ice_cube"], "same process; ED chiller ecosystem"),
    ("farmersdelight:wheat_dough", ["create:dough", "minecolonies:bread_dough"], "base dough"),
    ("culturaldelights:corn_dough", ["brewinandchewin:corn_dough"], "corn dough (split out of c:dough)"),
    ("farmersdelight:raw_pasta", ["minecolonies:raw_noodle"], "dough cutting / ED shaping station"),
    ("extradelight:cooked_pasta", ["minecolonies:pasta_plain"], "cooked in pot"),
    ("extradelight:cheese", ["minecolonies:cheddar_cheese", "farmersdelight_extended:fresh_cheese"], "curdled with vinegar"),
    ("extradelight:soy_sauce_item", ["minecolonies:soysauce", "youkaishomecoming:soy_sauce_bottle"], "ED multi-stage vat fermentation"),
    ("extradelight:coffee_beans", ["youkaishomecoming:coffee_beans", "rusticdelight:roasted_coffee_beans", "farmersrespite:coffee_beans"], "ED oven roasting"),
    # animal products
    ("butchery:animal_fat", ["amcdelight:animal_fat", "delightful:animal_fat"], "carcass butchering"),
    ("aquaculture:fish_bones", ["alexsmobs:fish_bones", "crabbersdelight:fish_bones"], "fillet knife processing"),
    ("alexsmobs:fish_oil", ["eternal_tales:fish_oil"], "rendered from blobfish"),
    ("crabbersdelight:shrimp", ["eternal_tales:shrimp"], "trap-caught"),
    ("eternal_tales:caviar", ["delightful:caviar"], "only loadable recipe (Delightful keg recipe needs Farmer's Respite)"),
    ("alexsmobs:cooked_catfish", ["eternal_tales:cooked_catfish"], "same food"),
    ("amcdelight:cooked_catfish_slice", ["alexsdelight:cooked_catfish_slice"], "knife cutting"),
    ("aquaculture:fish_fillet_cooked", ["amcdelight:cooked_fish_fillet"], "fillet system"),
    ("butchery:raw_sausage", ["brewinandchewin:raw_sausage"], "meat grinder + intestines"),
    ("butchery:cooked_sausage", ["brewinandchewin:cooked_sausage"], "follows raw sausage"),
    ("butchery:bottle_of_blood", ["eternal_tales:bottle_of_blood", "youkaishomecoming:blood_bottle"], "drained carcass blood"),
    ("frightsdelight:bone_shard", ["eternal_tales:bone_shard"], "knife-cut bones"),
    # metals
    ("mythicmetals:raw_silver", ["eternal_tales:raw_silver"], "Mythic Metals ore system"),
    ("mythicmetals:raw_silver_block", ["eternal_tales:raw_silver_block"], "metal"),
    ("mythicmetals:silver_nugget", ["eternal_tales:silver_nugget"], "metal"),
    ("mythicmetals:silver_block", ["eternal_tales:silver_block"], "metal"),
    ("mythicmetals:silver_ingot", ["eternal_tales:silver_ingot"], "metal"),
    ("mythicmetals:raw_tin", ["eternal_tales:raw_tin"], "metal"),
    ("mythicmetals:raw_tin_block", ["eternal_tales:raw_tin_block"], "metal"),
    ("mythicmetals:tin_nugget", ["eternal_tales:tin_nugget"], "metal"),
    ("mythicmetals:tin_block", ["eternal_tales:tin_block"], "metal"),
    ("mythicmetals:tin_ingot", ["eternal_tales:tin_ingot"], "metal"),
    ("mythicmetals:adamantite_ingot", ["eternal_tales:adamantite_ingot"], "alloy forge"),
    ("mythicmetals:adamantite_nugget", ["eternal_tales:adamantite_nugget"], "metal"),
    ("mythicmetals:adamantite_block", ["eternal_tales:adamantite_block"], "metal"),
    # sweets and drinks
    ("snowyspirit:candy_cane", ["eternal_tales:candy_cane"], "only crafted one"),
    ("extradelight:eggnog", ["snowyspirit:eggnog"], "cooking pot with spices"),
    ("extradelight:gingerbread_cookie", ["snowyspirit:gingerbread_cookie"], "dough then oven"),
    ("spectrum:hot_chocolate", [], "heated Create mixing; TAN warming; was tanca:hot_chocolate until 19 Sep"),
    ("extradelight:cactus_juice", [], "purified water base; juicer; was toughasnails:cactus_juice until 19 Sep"),
    ("expandeddelight:apple_juice", [], "purified water base; juicer; was toughasnails:apple_juice until 19 Sep"),
    ("extradelight:glow_berry_juice", ["expandeddelight:glow_berry_juice"], "purified water base; juicer; was toughasnails:glow_berry_juice until 19 Sep"),
    ("farmersdelight:melon_juice", [], "purified water base; juicer; was toughasnails:melon_juice until 19 Sep"),
    ("extradelight:sweet_berry_juice", ["expandeddelight:sweet_berry_juice"], "purified water base; juicer; was toughasnails:sweet_berry_juice until 19 Sep"),
    ("extradelight:ice_cream", ["mctradepost:icecream", "farmersdelight_extended:icecream_scoop"], "chiller"),
    ("extradelight:coffee", ["rusticdelight:coffee", "farmersrespite:coffee"], "roast, grind, brew"),
    ("extradelight:tea", ["eternal_tales:tea_cup"], "brewed in pot"),
    ("spectrum:azalea_tea", ["delightful:azalea_tea"], "potion workshop; Delightful pot recipe does not load"),
    ("brewinandchewin:mead", ["youkaishomecoming:mead"], "keg fermentation"),
    ("brewinandchewin:red_wine", ["youkaishomecoming:red_wine"], "keg fermentation"),
    ("brewinandchewin:white_wine", ["youkaishomecoming:white_wine"], "keg fermentation"),
    ("brewinandchewin:red_grape_seeds", ["youkaishomecoming:red_grape_seeds"], "same seeds"),
    ("brewinandchewin:white_grape_seeds", ["youkaishomecoming:white_grape_seeds"], "same seeds"),
    ("youkaishomecoming:affogato", ["extradelight:affogato"], "moka pot"),
    ("extradelight:candy_apple", ["youkaishomecoming:candy_apple"], "caramel candy pipeline"),
    ("extradelight:caramel_popcorn", ["corn_delight:caramel_popcorn"], "popped corn + melted caramel"),
    # dishes
    ("delightful:cheeseburger", ["extradelight:cheeseburger", "farmersdelight_extended:cheeseburger"], "13 Oct: Delightful's item, made only by Extra Delight's recipes (pickles, processed veg)"),
    ("extradelight:cheese_sandwich", ["expandeddelight:cheese_sandwich"], "shaped with sliced bread"),
    ("extradelight:grilled_cheese", ["expandeddelight:grilled_cheese"], "oven"),
    ("extradelight:macaroni_cheese", ["expandeddelight:mac_and_cheese"], "oven feast"),
    ("extradelight:chocolate_cookie", ["expandeddelight:chocolate_cookie"], "dough then oven"),
    ("extradelight:sugar_cookie", ["expandeddelight:sugar_cookie"], "dough then oven"),
    ("extradelight:glow_berry_cookie", ["eternal_tales:glowberry_cookie", "farmersdelight_extended:glow_berry_cookie"], "dough then oven"),
    ("extradelight:smore", ["delightful:smore"], "graham, chocolate bar, marshmallow"),
    ("extradelight:fried_chicken", ["rusticdelight:fried_chicken", "farmersdelight_extended:fried_chicken"], "breading station"),
    ("extradelight:fried_fish", ["crabbersdelight:fried_fish", "rusticdelight:fried_fish"], "breading station"),
    ("extradelight:fried_mushrooms", ["rusticdelight:fried_mushrooms", "farmersdelight_extended:fried_mushroom"], "breading station"),
    ("rusticdelight:fried_calamari", ["amcdelight:fried_calamari"], "oil + batter in pot"),
    ("extradelight:potato_salad", ["rusticdelight:potato_salad"], "mixing bowl"),
    ("rusticdelight:spring_rolls", ["festive_delight:spring_rolls"], "oil + dough in pot"),
    ("extradelight:toast", ["mynethersdelight:toasts", "farmersdelight_extended:toast"], "oven"),
    ("extradelight:bread_slice", ["mynethersdelight:slices_of_bread", "farmersdelight_extended:bread_slice"], "knife"),
    ("farmersdelight:pumpkin_pie_slice", ["extradelight:pumpkin_pie_slice"], "base mod, same cut"),
    ("extradelight:pumpkin_roll", ["brewinandchewin:slice_of_pumpkin_roll"], "oven feast"),
    ("extradelight:quiche", ["brewinandchewin:quiche", "ramadandelight:quiche"], "omelette mix + crust in oven"),
    ("extradelight:quiche_slice", ["brewinandchewin:quiche_slice", "ramadandelight:quiche_slice"], "follows quiche"),
    ("extradelight:rice_pudding", ["brewinandchewin:rice_pudding", "large_meals:rice_pudding"], "cooking pot"),
    ("extradelight:riceball", ["culturaldelights:rice_ball", "minecolonies:rice_ball"], "fish flakes"),
    ("extradelight:miso_soup", ["youkaishomecoming:miso_soup"], "broth + fermented miso"),
    ("youkaishomecoming:longevity_noodles", ["festive_delight:longevity_noodle"], "cooking pot"),
    ("artifactdelight:meatloaf", ["spectrum:meatloaf"], "minced beef in pot"),
    ("extradelight:meatloaf", ["artifactdelight:meatloaf_plate"], "feast serving"),
    ("extradelight:meatloaf_sandwich", ["spectrum:meatloaf_sandwich"], "feast serving"),
    ("extradelight:fish_cakes", ["spectrum:fishcake"], "cooking pot"),
    ("extradelight:fish_chips", ["minecolonies:fish_n_chips"], "fried fish + fries + vinegar"),
    ("crabbersdelight:fish_sandwich", ["eternal_tales:fish_sandwich"], "cooked fish + bread"),
    ("extradelight:congee", ["minecolonies:congee"], "cooking pot"),
    ("extradelight:corn_chowder", ["minecolonies:corn_chowder"], "cooking pot"),
    ("extradelight:potato_soup", ["large_meals:potato_soup", "minecolonies:potato_soup", "youkaishomecoming:potato_soup"], "cooking pot"),
    ("extradelight:borscht", ["minecolonies:borscht", "youkaishomecoming:borscht"], "cooking pot"),
    ("farmersdelight:apple_pie", ["minecolonies:apple_pie"], "oven pie"),
    ("farmersdelight:fried_rice", ["minecolonies:fried_rice"], "cooking pot"),
    ("farmersdelight:cooked_rice", ["minecolonies:cooked_rice"], "cooking pot"),
    ("farmersdelight:vegetable_soup", ["minecolonies:veggie_soup"], "cooking pot"),
    ("ramadandelight:kebab", ["minecolonies:kebab"], "raw kebab then grill"),
    ("ramadandelight:flat_bread", ["minecolonies:flatbread"], "dough then bake"),
    ("corn_delight:taco", ["minecolonies:tacos"], "tortilla assembly"),
    ("culturaldelights:tortilla", ["brewinandchewin:tortilla", "corn_delight:tortilla", "minecolonies:tortillas"], "corn dough then cook"),
    ("culturaldelights:tortilla_chips", ["corn_delight:tortilla_chip"], "knife cut"),
    ("extradelight:cream_corn", ["corn_delight:creamed_corn", "culturaldelights:creamed_corn"], "cooking pot"),
    ("extradelight:cornbread", ["brewinandchewin:corn_bread", "corn_delight:cornbread", "hauntedharvest:cornbread"], "oven"),
    ("mynethersdelight:stuffed_pepper", ["minecolonies:stuffed_pepper"], "player recipe"),
    ("extradelight:pasta_tomato", ["minecolonies:pasta_tomato"], "mixing bowl"),
    ("extradelight:sweet_berry_custard", ["large_meals:sweet_berry_custard", "farmersdelight_extended:sweet_berry_custard"], "cooking pot"),
    ("endersdelight:chicken_curry", ["large_meals:chicken_curry"], "spiced cooking pot"),
    ("mynethersdelight:spicy_curry", ["culturaldelights:spicy_curry"], "cooking pot"),
    ("extradelight:hot_wings", ["mynethersdelight:hot_wings"], "mixing bowl with sauce"),
    ("amcdelight:kangaroo_stew", ["alexsdelight:kangaroo_stew"], "cooked cuts in pot"),
    ("minersdelight:stuffed_squid", ["oceansdelight:stuffed_squid"], "cooking pot"),
    ("minersdelight:tentacles_on_a_stick", ["oceansdelight:tentacle_on_a_stick"], "more ingredients"),
    ("brewinandchewin:croissant", ["cluttered:croissant"], "shaped raw croissant then bake"),
    ("expandeddelight:sweet_roll", ["create:sweet_roll"], "dough, cinnamon, milk"),
    ("extradelight:cactus_soup", ["delightful:cactus_soup"], "cooking pot"),
]

# 16 Sep: Farmer's Respite arrived after the first merge. Youkai's kettle wins the teas because a maid can
# run it (MaidsoulKitchen TaskYhcTeaKettle); Farmer's Respite's kettle still brews them, and its pouring
# recipes are kept as conversions, so both workstations end at the same item.
D += [
    ("youkaishomecoming:black_tea", ["farmersrespite:black_tea"], "Youkai kettle; a maid can run it"),
    ("youkaishomecoming:green_tea", ["farmersrespite:green_tea"], "Youkai kettle; a maid can run it"),
    ("youkaishomecoming:yellow_tea", ["farmersrespite:yellow_tea"], "Youkai kettle; a maid can run it"),
    ("youkaishomecoming:black_tea_leaves", ["farmersrespite:black_tea_leaves"], "Youkai tea processing chain"),
    ("youkaishomecoming:coffee_berries", ["farmersrespite:coffee_berries"], "berries feed the Youkai chain"),
    ("farmersdelight:apple_cider", ["farmersrespite:apple_cider"], "Farmer's Delight cider is used across the pack"),
    ("farmersdelight:hot_cocoa", ["farmersrespite:hot_cocoa"], "Farmer's Delight cocoa is used across the pack"),
    ("extradelight:coffee_cake", ["farmersrespite:coffee_cake"], "Extra Delight oven bake"),
    ("extradelight:coffee_cake_slice", ["farmersrespite:coffee_cake_slice"], "follows the cake"),
    ("ends_delight:ender_pearl_grain", ["endersdelight:ender_shard"], "crushed ender pearl; more uses"),
    ("crabbersdelight:pufferfish_slice", ["oceansdelight:fugu_slice"], "cooks into a cooked slice, drops fish bones, raw poison chance"),
    ("crabbersdelight:coconut_milk", ["compatdelight:coconut_milk"], "Crabber's coconut milk is the one with a working recipe"),
]
# 17 Sep: Compat Delight modules turned on with stand-ins; these outputs duplicate items the pack already makes
D += [
    ("extradelight:lemon_juice", ["compatdelight:lemon_juice"], "Extra Delight juicer"),
    ("extradelight:lime_juice", ["compatdelight:lime_juice"], "Extra Delight juicer"),
    ("extradelight:grapefruit_juice", ["compatdelight:grapefruit_juice"], "Extra Delight juicer"),
    ("pineapple_delight:pineapple_juice", ["compatdelight:pineapple_juice"], "Pineapple Delight"),
    ("expandeddelight:cheese_slice", ["compatdelight:cheese_slice"], "Expanded Delight"),

    # duplicate storage crates/blocks: same crop, different container (29 Sep)
    ("farmersdelight:beetroot_crate", ["quark:beetroot_crate"], "FD crate is used by the Delight ecosystem"),
    ("farmersdelight:carrot_crate", ["quark:carrot_crate"], "FD crate is used by the Delight ecosystem"),
    ("farmersdelight:potato_crate", ["quark:potato_crate"], "FD crate is used by the Delight ecosystem"),
    ("extradelight:corn_crate", ["corn_delight:corn_crate"], "ED crate is what corn already routes to"),
    ("endersdelight:chorus_crate", ["ends_delight:chorus_fruit_crate"], "one chorus crate"),
    ("frightsdelight:flesh_crate", ["eternal_tales:flesh_block"], "crate matches the other food crates"),
    ("the_bumblezone:pile_of_pollen", ["compatdelight:pollen_puff_crate"], "Bumblezone block is the original"),
    ("extradelight:coffee_bean_sack", ["farmersrespite:coffee_beans_sack"], "ED sack has the roasting chain"),
]

# 13 Oct: Farmer's Delight: Extended (farmersdelight_extended 0.2.2) arrived. Members of the groups above were added
# in place; these are the groups that did not exist yet. The dominant is the item with the most involved chain.
D += [
    ("extradelight:french_fries", ["farmersdelight_extended:fries"], "potato sticks, 4 per potato"),
    ("extradelight:sliced_potato", ["farmersdelight_extended:cut_potatoes"], "4 slices per potato"),
    ("extradelight:grated_potato", ["farmersdelight_extended:shredded_potatoes"], "FDE's Create milling stays as the automated source"),
    ("extradelight:hashbrowns", ["farmersdelight_extended:hashbrowns"], "grater, 4 per potato"),
    ("extradelight:chocolate_custard", ["farmersdelight_extended:chocolate_custard"], "cooking pot or Create mixing"),
    ("extradelight:chicken_parm", ["farmersdelight_extended:chicken_parmesan"], "oven, 2 per bake"),
    ("extradelight:beef_wellington", ["farmersdelight_extended:beef_wellington"], "oven feast; FDE's single-serving pot recipe stays"),
    ("extradelight:brownie", ["farmersdelight_extended:brownie"], "oven feast; FDE's batter route stays"),
    ("extradelight:sweet_berry_ice_cream", ["farmersdelight_extended:sweet_berry_icecream_scoop"], "ED chiller (3 berries) and FDE ice cream + 1 berry both stay"),
    ("extradelight:glow_berry_ice_cream", ["farmersdelight_extended:glow_berry_icecream_scoop"], "ED chiller (3 berries) and FDE ice cream + 1 berry both stay"),
    ("extradelight:chocolate_ice_cream", ["farmersdelight_extended:chocolate_icecream_scoop"], "ED chiller and FDE scoop + cocoa bean both stay"),
    ("extradelight:sweet_berry_popsicle", ["farmersdelight_extended:sweet_berry_popsicle"], "chiller, 4 per batch"),
    ("extradelight:glow_berry_popsicle", ["farmersdelight_extended:glow_berry_popsicle"], "chiller, 4 per batch"),
    ("extradelight:fudge_popsicle", ["farmersdelight_extended:chocolate_popsicle"], "chiller, 4 per batch; FDE's craft stays"),
    ("ends_delight:chorus_cookie", ["farmersdelight_extended:chorus_cookie"], "identical recipe; End's Delight owns chorus"),
    ("ends_delight:chorus_fruit_popsicle", ["farmersdelight_extended:chorus_popsicle"], "identical recipe; End's Delight owns chorus"),
    ("youkaishomecoming:mochi", ["farmersdelight_extended:mochi"], "Youkai pot recipe; FDE's Create assembly stays as the automated route"),
]

# 14 Oct: two egg rules, taken from the tags.json / materials.json snippets the user supplied.
# Tag names and priority overrides come out of build.py as c:unified/fried_egg -> farmersdelight and
# c:unified/scrambled_eggs -> extradelight, matching those snippets. Note: Incubation is only an optional
# dependency of Delightful in the current mod list; if it is not installed these groups are no-ops for it.
D += [
    ("farmersdelight:fried_egg", ["incubation:fried_egg"], "user rule: Farmer's Delight keeps the fried egg"),
    ("extradelight:scrambled_eggs", ["incubation:scrambled_eggs"], "user rule: Extra Delight keeps the scrambled eggs"),
]

# Recipes switched off outright, by recipe id (any mod). build.py skips them in the group pass and writes the stub.
# The 16 Sep baseline dump predates FDE, so FDE's own recipes never reach the group pass and have to be listed here.
# Rule (user, 13 Oct): several routes to one item are fine, but the harder one must be justified by automation or yield.
_ED = "ED route is harder than FDE's and yields no more"
_FDE = "FDE route is easier but yields less, or duplicates the kept item's recipe"
FORCE_DISABLE = {
    # Extra Delight side
    # Delightful's own cheeseburger recipes are off: Extra Delight's two recipes (kept via KEEP_EXTRA) are the only routes
    'delightful:food/cheeseburger': "cheeseburger is made by Extra Delight's recipes only (user, 13 Oct)",
    'delightful:food/cheeseburger_from_bun': "cheeseburger is made by Extra Delight's recipes only (user, 13 Oct)",
    'delightful:food/cheeseburger_from_hamburger': "cheeseburger is made by Extra Delight's recipes only (user, 13 Oct)",
    'delightful:food/cheeseburger_from_milk': "cheeseburger is made by Extra Delight's recipes only (user, 13 Oct)",
    # Farmer's Delight: Extended side
    'farmersdelight_extended:cutting/bread_slice': "same as Extra Delight bread slice",
    'farmersdelight_extended:campfire_cooking/toast': "same as Extra Delight toast, minus the oven",
    'farmersdelight_extended:smelting/toast': "same as Extra Delight toast, minus the oven",
    'farmersdelight_extended:smoking/toast': "same as Extra Delight toast, minus the oven",
    'farmersdelight_extended:cutting/cut_potatoes': "2 per potato vs Extra Delight's 4",
    'farmersdelight_extended:campfire_cooking/fries': "2 fries per potato vs 4 for Extra Delight",
    'farmersdelight_extended:smelting/fries': "2 fries per potato vs 4 for Extra Delight",
    'farmersdelight_extended:smoking/fries': "2 fries per potato vs 4 for Extra Delight",
    'farmersdelight_extended:campfire_cooking/hashbrowns': "same hashbrowns; Extra Delight's grater gives more",
    'farmersdelight_extended:smelting/hashbrowns': "same hashbrowns; Extra Delight's grater gives more",
    'farmersdelight_extended:smoking/hashbrowns': "same hashbrowns; Extra Delight's grater gives more",
    'farmersdelight_extended:cooking/sweet_berry_custard': "same yield as Extra Delight's pot and Create mixing",
    'farmersdelight_extended:cooking/chocolate_custard': "same yield as Extra Delight's pot and Create mixing",
    'farmersdelight_extended:cooking/chicken_parmesan': "harder than Extra Delight's oven recipe, half the output",
    'farmersdelight_extended:cooking/fresh_cheese': "half of Extra Delight's cheese per milk bucket; cheese stays on the Brewin' and Chewin' path",
    'farmersdelight_extended:mixing/fresh_cheese': "half of Extra Delight's cheese per milk; cheese stays on the Brewin' and Chewin' path",
    'farmersdelight_extended:shapeless/chorus_cookie': "identical to End's Delight",
    'farmersdelight_extended:shaped/chorus_popsicle': "identical to End's Delight",
    'farmersdelight_extended:shapeless/cheeseburger': "cheeseburger is made by Extra Delight's recipes only (user, 13 Oct)",
    # FDE's batter route is off (user, 13 Oct): Extra Delight's breading route stays. The four batters and everything
    # that cooks them go; the fried items are still merged into Extra Delight's (chicken, mushrooms, brownie).
    # Fried cod and brownie batter have no other source, so those FDE items become unobtainable.
    **{'farmersdelight_extended:%s/%s' % (k, n): "FDE batter route off: Extra Delight's breading / oven route stays"
       for n in ('battered_chicken', 'battered_cod', 'battered_mushroom', 'brownie_batter') for k in ('shapeless', 'mixing')},
    **{'farmersdelight_extended:%s/%s' % (k, n): "FDE batter route off: Extra Delight's breading / oven route stays"
       for n in ('fried_chicken', 'fried_cod', 'fried_mushroom', 'brownie') for k in ('campfire_cooking', 'smelting', 'smoking')},
    'farmersdelight_extended:shapeless/fish_and_chips': "removed by request: Extra Delight and Aquaculture Delight keep their own fish and chips",
}

# Non-dominant recipes that survive the "duplicate of the dominant" pass.
KEEP_EXTRA = {
    'extradelight:cheeseburger',            # Extra Delight's recipes stay; AlmostUnified rewrites their output to Delightful's item
    'extradelight:cheeseburger_burger',
    # 29 Sep decision (reports/25-recipe-shortcuts.md): Farmer's Delight's own fried egg recipes stay. The shortcut rule only
    # fires on grouped items, so the 14 Oct fried_egg group would otherwise have disabled them. Rustic's cooking-oil recipe is
    # overridden to the extra-nutritious egg by zzz-recipe-progression, which loads after this pack.
    'farmersdelight:fried_egg',
    'farmersdelight:fried_egg_from_campfire_cooking',
    'farmersdelight:fried_egg_from_smoking',
}

# New recipes (13 Oct, user): the five FDE items whose own batter route is off stay obtainable, built the way Extra Delight
# builds its fried foods: the food item + Extra Delight's breading (its batter) + the usual cooking step.
# Written to data/zz_unified/recipe/fde_ed/<name>.json by build.py. Edit here to change inputs or yields.
_BREAD = {"item": "extradelight:breading_misanplas"}
_CREATE = {"type": "neoforge:mod_loaded", "modid": "create"}
def _battered(food, out):
    return {
        "craft": {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [food, _BREAD],
                  "result": {"count": 1, "id": "farmersdelight_extended:" + out}},
        "mix": {"neoforge:conditions": [_CREATE], "type": "create:mixing", "ingredients": [food, _BREAD],
                "results": [{"id": "farmersdelight_extended:" + out}]},
    }
_chicken, _cod, _mush = ({"item": "farmersdelight:chicken_cuts"}, {"item": "farmersdelight:cod_slice"}, {"tag": "c:mushrooms"})
NEW_RECIPES = {}
for _name, _food, _out in (("battered_chicken", _chicken, "battered_chicken"), ("battered_cod", _cod, "battered_cod"),
                           ("battered_mushroom", _mush, "battered_mushroom")):
    _r = _battered(_food, _out)
    NEW_RECIPES[_name] = _r["craft"]
    NEW_RECIPES[_name + "_mixing"] = _r["mix"]
# fried cod: same shape as Extra Delight's fried fish (pot, and heated Create mixing)
NEW_RECIPES["fried_cod"] = {"type": "farmersdelight:cooking", "experience": 0.35, "ingredients": [_cod, _BREAD],
                            "recipe_book_tab": "meals", "result": {"count": 1, "id": "farmersdelight_extended:fried_cod"}}
NEW_RECIPES["fried_cod_mixing"] = {"neoforge:conditions": [_CREATE], "type": "create:mixing", "heat_requirement": "heated",
                                   "ingredients": [_cod, _BREAD], "results": [{"id": "farmersdelight_extended:fried_cod"}]}
# brownie batter: Extra Delight's brownie pan ingredients, one batter per portion of its 4-serving feast
NEW_RECIPES["brownie_batter"] = {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [{"tag": "c:flour"}, {"tag": "extradelight:sweetener"}, {"tag": "c:flour"}, {"tag": "c:liquid_chocolate"},
                    {"tag": "c:chocolate/chips"}, {"tag": "c:eggs"}, {"tag": "c:butter"}, {"tag": "c:eggs"}],
    "result": {"count": 4, "id": "farmersdelight_extended:brownie_batter"}}
