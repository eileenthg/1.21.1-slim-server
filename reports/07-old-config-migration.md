# Migrating configs from the old kitchen-sink pack

Source: `modpack-audit/config-kitchen-sink-old/` (326 entries). 174 shared with your current config folder, 68 of those differ — but most of that is **version drift**, not your choices: the current files are usually larger because newer mod versions added options. Copying old files wholesale would silently delete those new options.

So I diffed **values**, not files. Only **30 configs** have a key that exists in both with a different value — those are your actual decisions.

---

## Almost Unified — rebuilt, and it's a big upgrade

Your old AU config is far more developed than the one I wrote from scratch. It had **309 custom tags** against my ~60, including the entire mob-head unification (Butchery ↔ Spectrum) that I'd flagged as an open item in report 02. You'd already solved it.

Migrated into `almostunified-proposed/`, ready to copy to `config/almostunified/`:

| File | Result |
|---|---|
| `tags.json` | **185 custom tags** (136 migrated + 47 merged from my version), inheritance mode `DENY` as you had it |
| `unification/materials.json` | 29 mod priorities, 25 priority overrides, 172 tags, 7 ignored items |
| `placeholders.json` | **fixed** — the old one was invalid JSON |
| `duplicates.json`, `startup.json` | carried over unchanged (identical to your current ones) |

### What the migration did

**`pastel:` → `spectrum:` — 262 references.** Only 9 didn't resolve, and 8 of those were renames I mapped by hand:

| Old | New |
|---|---|
| `pastel:frosbite_crystal` | `spectrum:frostbite_crystal` (typo upstream) |
| `pastel:frosbite_essence` | `spectrum:frostbite_essence` |
| `pastel:red_mooshroom_head` | `spectrum:mooshroom_red_head` (word order) |
| `pastel:fox_head` | `spectrum:fox_red_head` (Spectrum split red/arctic) |
| `pastel:amethyst_cluster`, `large/medium/small_amethyst_bud` | `minecraft:*` — these are vanilla; Spectrum doesn't redefine them |

Only `pastel:star_candy` was dropped: Spectrum has `sugary_star_candy`, `mellow_star_candy` and `rock_candy`, and I won't guess which you meant.

**Tag losses are almost entirely harmless.** 164 of the 309 tags had **fewer than two members** — a unification tag with one item does nothing. Only **9** tags were lost because members went missing, from mods you no longer run: `unusualend` (7 ids), `immersiveengineering` (2), `eggdelight` (2), `malum` (2), `tmted`, `phantasm`, `archeological`.

**Priorities:** dropped `productivemetalworks` and `immersiveengineering` (not installed), renamed `pastel`→`spectrum`, appended the mods you've since added (delightful, ramadandelight, crabbersdelight, aquaculture, snowyspirit, expandeddelight, frightsdelight, hauntedharvest, corn_delight, create, quark, mythicmetals), and moved **`eternal_tales` to last** so everything outranks it, as you asked.

**I deleted my `foods.json`.** Two unification files both claiming the same tag would fight; your `materials.json` is the better base and now carries everything.

**One mistake I caught mid-way:** my first pass filtered the `tags` list down to only entries that appear in `custom_tags`, which threw away real mod-provided tags like `c:crops/onion`, `c:crops/corn` and `c:ropes` — that would have silently stopped onions and corn unifying. Fixed by validating against the 3,633 item tags your mods actually ship. Final list is 172 tags; the 9 genuinely dead ones are gone (`c:green_coffee_bag`, `c:arthropod_egg`, `c:tentacle_on_stick`, `c:wild_corn`, `c:candy_apple`, `c:sweet_berry_jam`, `c:raw_endermite`, `c:foods/raw_meat/elder_guardian`, `c:foods/cooked_meat/shulker/slice`).

**Preserved as-is:** your `ignored_tags` (9, including `c:cleaver` and the arthropod/whole-fish tags you'd deliberately switched off), `ignored_recipe_ids` (`farmersdelight:cooking/candy_apple`), `ignored_items` (all 7 still valid — bell pepper colours, ghasmati, ghast dough, sculk flour, ender mite meat), `stone_variants` including `blackslag`, and `loot_unification: true`.

---

## Other configs worth carrying

### Clear yes — deliberate choices, still applicable

**`touhou_little_maid-common.toml`** — 17 changed values, the biggest being maid ranges:

| | old | current |
|---|---|---|
| `MaidWorkRange` | **32** | 12 |
| `MaidIdleRange` | **32** | 6 |
| `MaidNonHomeRange` | **32** | 8 |

That's a real gameplay preference — your maids ranged much further. Also `GiveSoulSpell` and `MaidGomokuOwnerLimit` differ. Worth re-applying by hand.

**`Butchery.toml`** — you had `Mobs.Salmon = false` and `Mobs.Cod = false`, now both `true`. You'd disabled fish butchering, which given Aquaculture, Crabber's Delight and the squid work we just did is probably still what you want — otherwise cod and salmon get carcasses competing with the fish mods.

**`supplementaries-common.toml`** — `lunch_basket.enabled = false`, `flute.unbound = true`, `blue_bomb.explosion_radius = 5.15` (now 3.0).

**`cookingforblockheads-common.toml`** — `sinkRequiresWater = true`, `cowJarEnabled = false`.

**`minecolonies-server.toml`** — `gameplay.loadtime = 10` (now 20), `compatibility.auditcraftingtags = false` (now true).

**`item_descriptions.toml`** — tooltip keybind preferences (shift vs ctrl, show mod name).

### Relevant to what we just did

**`simple_datapacks.json`** — old had `copyDatapacks = true`, current is `false`. With `true`, Simple Datapacks copies packs into each world automatically instead of you enabling them per world. Given the four datapacks now on its path, you probably want the old behaviour.

**`simpleclouds-client.toml`** — worth a look for your lag complaint. Current has **`frustumCulling = false`**, old had `true`. Culling off means clouds render outside your view. Current also has `levelOfDetail = "LOW"` and `framesToGenerateMesh = 32` (old: `HIGH` / `5`), which are the faster settings — so it's mixed. I'd turn `frustumCulling` back on and leave the rest.

**`item_obliterator.json5`** — old blacklists `butchery:pestle_and_mortar`. Worth knowing you'd already used this mod to remove a duplicate; relevant if you go ahead with obliterating MineColonies crops.

### Personal, carry only if you still want them

**`mca.json`** — your villager chat AI setup pointed at a local endpoint (`http://127.0.0.1:4315`) with the `player2` model; current uses Conczin's hosted default. Only relevant if you're still running that locally.

**`sodiumextras-client.toml`** — `BORDERLESS` vs `FULLSCREEN`, FPS display style, and the darkness/gamma settings.

**`projectatmosphere-common.toml`** — wind push scales roughly 3× stronger in the old config.

### Don't carry

**`mctradepost-common.toml`** — I originally listed `tradeCoinValue = 100` as a deliberate choice worth carrying. **Wrong.** Decompiling `MCTPConfig` shows the current version defines it as `.define("tradeCoinValue", 1000)` — `sipush 1000` — so **1000 is the mod's default** and your config is untouched. The old `100` was simply the older version's default. Carrying it over would have re-applied a stale default as if it were a preference. `station.trackValidationFrequency` in the same file is likely the same story.


**`ferritecore-mixin.toml`** — the old config had `compactFastMap` and `useSmallThreadingDetector` both `false`. I just turned them **on**. Current is better; skip.

**`modernfix-mixins.properties`** — the old file had **no user overrides at all**, so there's nothing to carry. The `dynamic_resources=true` we just set is new.

**Anything for absent mods** — `immersiveengineering-*`, `betterclouds-v1.json`, `betterfog`, `betterfpsdist.json`, `connectivity.json`, `nether remastered.toml`, `projectvibrantjourneys-common.toml` and the rest of the 50 "only in old" files. Also ignore the `*.toml.bak` files (Butchery-1/2/3, amendments-client-1/2/3) — those are the mods' own rotated backups, not your settings.

---

## Verified against the mods' actual defaults

You were right to push back on Trade Post, so I checked every flagged value against the mod's own default — from the `# Default:` annotations the configs write, or from bytecode where they don't.

**Current value = the mod's default, so the old value really was your edit — carry these:**

| Setting | Mod default | Your old value |
|---|---|---|
| `MaidWorkRange` / `MaidIdleRange` / `MaidNonHomeRange` | 12 / 6 / 8 | **32 / 32 / 32** |
| `minecolonies gameplay.loadtime` | 20 | **10** |
| `projectatmosphere wind.playerPushScale` | 0.013333 | **0.04** |
| `projectatmosphere wind.entityPushScale` | 0.01 | **0.03** |
| `supplementaries blue_bomb.explosion_radius` | 3.0 | **5.15** |
| `Butchery Mobs.Salmon` / `Mobs.Cod` | `true` (`iconst_1`) | **false / false** |

**Current value is NOT the default — someone changed these recently, and both are worth a second look:**

| Setting | Mod default | Current | Note |
|---|---|---|---|
| `simpleclouds frustumCulling` | **true** | `false` | Clouds render outside your view. Pure performance loss — turn back on. |
| `simpleclouds levelOfDetail` | **HIGH** | `LOW` | Deliberate perf choice, fine to keep. |
| `minecolonies compatibility.auditcraftingtags` | **false** | `true` | Writes debug CSVs on every load. Set back to `false`. |

**Confirmed not your edit — leave alone:**

- `mctradepost tradeCoinValue` — default is 1000 (`sipush 1000`), current matches.
- `mctradepost station.trackValidationFrequency` — default is 6000 (`sipush 6000`), current matches. The old 10000 was likely that version's default.

**Still unresolved** (no `# Default:` annotation, and the config class doesn't decompile cleanly): `cookingforblockheads sinkRequiresWater` / `cowJarEnabled`, and `supplementaries lunch_basket.enabled` / `flute.unbound`. All low-stakes toggles — set them to taste rather than trusting my diff.

---

## Applied

| Config | Setting | Change |
|---|---|---|
| `simpleclouds-client.toml` | `frustumCulling` | `false` → **`true`** (back to default; stops clouds rendering outside view) |
| `minecolonies-server.toml` | `auditcraftingtags` | `true` → **`false`** (back to default; stops debug CSV writing on load) |
| `cookingforblockheads-common.toml` | `sinkRequiresWater` | `false` → **`true`** |
| `cookingforblockheads-common.toml` | `cowJarEnabled` | `true` → **`false`** |

`supplementaries lunch_basket.enabled` was already `true` — no change needed.

Backups: `proposed-configs/<name>.BEFORE`.

## PIN — `supplementaries` `tools.flute.unbound`

Currently `false`. Its own config comment explains it:

> Allows flute to be unbound. Unbound flutes will search for pets in a radius and teleport them to the player

So with `unbound = false` (current) a flute must be **bound to one specific pet**, and calls only that pet. With `unbound = true` it becomes a general pet-recall item that teleports **any** of your pets within `unbound_radius` (currently 64 blocks) to you.

Your old kitchen-sink pack had it `true`. Relevant given how many tameable things you run — Touhou maids, Tamable Fairy, Alex's Mobs, MCA pets, Waystones Teleport Pets. Decide whether mass pet-recall is convenience or too strong for your run; it's a one-line flip either way.
