---
name: scrap-meat-sources
description: "scrap-meat-sources datapack adds 4 ways to get butchery:meat_scraps; installed 26 Sep, untested (server was running)"
metadata:
  node_type: memory
  type: project
  originSessionId: c1f75de2-7e54-4d7c-8680-83714d73f953
  modified: 2026-09-26T10:51:55.354Z
---

Before 26 Sep 2026 the only source of `butchery:meat_scraps` was Butchery's meat grinder. Its code checks the `butchery:scrappable` item tag. The 16 Sep head merge (see [[item-merge-pipeline]]) made 43 Spectrum/Supplementaries heads the kept versions, and none of them were scrappable. Scraps are the only input that fits Extra Delight's SOS (`c:beef/scrap`) and one of the few for haggis (`c:meat/scrap`).

`datapacks/scrap-meat-sources.zip` (generator: `modpack-audit/proposed-datapacks/build_scrap_meat_sources.py`, which reads unification/report.json head_groups) adds:
1. The 43 merged heads to `butchery:scrappable`.
2. Spectrum anvil crushing: `scrapmeat:crushable_raw_meat` (31 explicit raw meats, no sausages, innards or mince) -> 2 scraps.
3. FD cutting board with a knife: `scrapmeat:meaty_heads` -> 2 scraps + a bone.
4. Create crushing: meaty heads -> 2 scraps + 50% bone; `scrapmeat:carcasses` -> 4 scraps + 1–2 bones.

5. 13 regular raw meats (chicken, rabbit, bacon, hoglin loin, etc.) added to `butchery:scrappable` so the grinder takes them, at 1 scrap per item (hardcoded for non-block items). Anything in `c:raw_pork`/`c:raw_mutton`/`c:raw_beef` is kept out on purpose: the grinder checks scrappable first, so tagging those would shadow its sausage and lamb/beef mince recipes. Beef, pork and mutton get scraps through the anvil instead.

6. Mince counts as scrap: raw/cooked Butchery beef and lamb mince, FD minced beef and Alex's Delight bison mince go into `c:beef|mutton/scrap/raw|cooked`, and minced strider goes into `c:meat/scrap/raw`. As a side effect they join `c:meat`, `c:meat/raw` and `c:beef`/`c:mutton`, so jerky, chili, hash, omelettes and stir-fry accept them. Grinder tags (`c:raw_beef` etc.) are unchanged. Endless minced beef (artifact) and minced warden heart are left out.

Golem, player, dragon, stray and skeleton-horse heads are excluded from 3 and 4.

**Why:** the user asked for more reasonable scrap sources, picked these four, and chose 2 scraps per raw meat. Spectrum's own shipped recipe outputs NEEPMeat's scrap and uses `c:foods/raw_meats`, which is empty in this pack.

**How to apply:** it is installed in the live instance (`1.21.1 slim/minecraft/datapacks`, which hosts "New World" over LAN). It was not yet listed in level.dat on 26 Sep, so it auto-enables on the next world load (/reload is broken, see [[modpack-datapack-install-location]]). The user skipped testing because they were hosting. If it fails to load, suspect the cutting recipe shape first (Farmer's Contracts parses raw JSON, see [[disable-stub-recipe-shapes]]).
