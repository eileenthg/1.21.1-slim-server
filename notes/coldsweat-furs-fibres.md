---
name: coldsweat-furs-fibres
description: "Cold Sweat insulation expanded 26 Sep — thick furs count as goat fur (datapack + tag), cotton/flax/canvas are heat-only insulators"
metadata:
  node_type: memory
  type: project
  originSessionId: ded502b2-df13-426f-ab9a-f6b992dd6b43
  modified: 2026-09-26T04:05:42.851Z
---

On 2026-09-26 the user asked for every reasonable thick fur to count as Cold Sweat goat fur, scoped by real-life warmth.

- `datapacks/coldsweat-thick-furs.zip` (source: `modpack-audit/proposed-datapacks/coldsweat-thick-furs/`): tag `c:furs/thick` = `#c:furs/goat` + 16 furs (Alex's Mobs bison; Butchery polar bear, wolf, fox, snow fox, goat, sheep, 4 llama, panda, sniffer; Eternal Tales bear, raccoon, white raccoon). It overrides the 4 `cold_sweat:goat_fur_*` armor recipes to use that tag.
- Deliberately excluded: deer skin (borderline), cat furs, horse/cow/pig/other short hides, ocelot, capuchin, and Eternal Tales sunfurry.
- `config/coldsweat/item.toml` Insulation Ingredients: goat_fur entry replaced by `["#c:furs/thick", 2, 0]`; heat-only entries added: `rusticdelight:cotton_boll`, `supplementaries:flax`, `farmersdelight:canvas`, each 0 cold / 1 heat. Backup at `proposed-configs/coldsweat-item.BEFORE.20260926.toml`.
- Wool is unchanged (1.5 cold in the Sewing Table, not in the armor recipes). The user chose that because wool is too easy to farm.
- Harness boot: the datapack loads with no errors, and Cold Sweat kept the tag and cotton entries when it rewrote the config. Flax and canvas were added after that boot. Nothing has been tested in-game (Sewing Table, recipes).

**Why:** the user wants insulation to follow real-life warmth: fur > wool; cotton/linen are good for heat and bad for cold.

**How to apply:** when new animal mods add pelts, append them to the tag in the datapack source and re-zip. Cold Sweat rewrites item.toml to a single line on boot, which is normal. See [[survival-stack-twt-coldsweat]].
