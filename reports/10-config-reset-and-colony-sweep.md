# Config reset (15 Sep) and MineColonies-suite sweep

## What reset the configs

At 17:11 on 15 Sep, the `minecraft` folder of the Prism instance **"Test minecolonies to MCA conversion"** was copied over `instances/1.21.1/minecraft`, and every file with the same name was replaced. Evidence: `hs_err_pid21376.log` and older crash reports have 1 Sep file dates but were created at 17:11, and 23 config files are now byte-identical to the test instance.

Files that had real settings and were rolled back:

| File | Lost setting | Restored from |
|---|---|---|
| `config/ferritecore-mixin.toml` | `compactFastMap`, `useSmallThreadingDetector` = true | 13 Sep snapshot (`test-server/A-full/config`) |
| `config/friendsandfoes.json` | crabs off, illusioner in raids off | 13 Sep snapshot |
| `options.txt` | render/sim distance 6, graphics Fast, particles Decreased, biome blend 1, entity distance 50%, VSync off, clouds off, subtitles on, `saveToolbarActivator` unbound | earlier backups + 13 Sep choices |
| `config/simple_datapacks.json` | `modpack-audit\datapacks` path missing (rewritten at 17:33) | re-added |

The other overwritten configs (`neoforge-*`, `minecolonies-*`, `structurize-*`, `farmersdelight-*`, `chefsdelight`, `fml.toml`, `mca.json`) had the same content as the 13 Sep snapshot. Nothing was lost there.

Backups of the files as found: `proposed-configs/reset-2026-09-15/`.

Still differs from the 6 Sep tuning but was changed deliberately later (left alone): RoadWeaver OpenCL on (13 Sep 23:03), Better Caves `liquidregions.json` restored (13 Sep).

**Avoid next time:** don't copy a whole test-instance `minecraft` folder into the main instance. Copy only `mods/<new jars>` and the new mods' own config files.

## Sweep results

| Finding | Action |
|---|---|
| Two Farmer's Delight jars (`-1.3.4-patched` + plain `1.3.4` from the test instance) | Plain jar moved to `removed-mods/` |
| SmallColonies v1.8 and SmallColonies_ME v1.9 both register "Littleton", "Sandyton" and "Decorations" (log shows each twice; ME is the newer superset) | v1.8 moved to `removed-mods/` |
| Retired AnotherScape packs were still on the Simple Datapacks path, and `anotherscape-townsfolk-no-guild.zip` was listed twice | Moved to `datapacks-retired/` |
| `saves/New World` has none of the audit datapacks enabled (crop-freedom, ExtraButchery-fixed, squid dedup, eternaltown set, gravel-tame, etc.) | Enable them when creating the real world |
| Arcadian Dream throws on world exit (`ModConfigEvent.Unloading` reads config values) | Separate task suggested |
| Longer Chat History and Even Better Chat (chatoptimizer) both patch chat history length | Pick one |
| "SmallColonies_ME keep" pack needs Beautify (not installed), so it is hidden | Info |
| Townstead Factions: "no factions configured" (factions map to `townstead_classic` origins, only 1 Townstead origin loaded) | Info, pre-existing |

### MineColonies item overlaps

Already unified by AlmostUnified: tomato, onion, cabbage, rice, cooked rice, garlic, eggplant, butter, tofu, kimchi, copper nugget.

Not unified, same ingredient:
- **Soybean**: `minecolonies:soybean`, `youkaishomecoming:soybean` (Youkai's soybean isn't in `c:crops/soybean`)
- **Chickpea**: `minecolonies:chickpea`, `ramadandelight:chickpea` (priority override exists, but neither item has the tag)
- **Cornmeal**: `minecolonies:cornmeal`, `brewinandchewin:cornmeal`
- **Corn**: `brewinandchewin:corn` has no corn tag

Same name but a different dish, block or tool (leave; MineColonies' cook menus and building requests use their own ids): apple pie, fried rice, borscht, congee, corn chowder, potato soup, tomato pasta, rice ball, kebab, stuffed pepper, ice cream; clipboard (Create), grave (Gravestone/Eternal Tales), iron gate (Supplementaries), thatch ×3 (Quark vs Trade Post), plaster, sand bricks, chiller (Extra Delight vs More Villagers workstation). The Quark ancient tome is already disabled.
