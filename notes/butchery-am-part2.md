---
name: butchery-am-part2
description: "Butchery Alex's Mobs Part 2 mod (butchery_alexs_mobs_2): 80 carcass entries converted from AM models, Alex's/AMC Delight drops; built 27 Sep, not installed"
metadata:
  node_type: memory
  type: project
  originSessionId: 67315980-40f4-46de-8992-3c58bb461dd5
  modified: 2026-09-26T12:42:22.283Z
---

On 2026-09-26 the user asked for a separate "Part 2" add-on to the Butchery: Alex's Mobs port (`C:\Users\eteil.000\Claude\alex-mobs-butchery`, Part 1). Part 2 is at `C:\Users\eteil.000\Claude\alex-mobs-butchery-2`, mod id `butchery_alexs_mobs_2`. It covers bison, moose, kangaroo, gazelle, tusklin, grizzly (full bleed/head/skin/3 cuts), bunfungus (bloodless 3 steps), catfish small/medium/large and lobster (one cut), and the cave centipede (Ender Dragon-style block entity renderer, one piece per cut).

User decisions:
- Scope was the "Alex's Delight set + big game" option.
- Carcass models are converted from Alex's Mobs' own entity models, not recolored Butchery templates.
- Hanging carcasses hang head-up from one point, the shoulders (where head or neck meets the body), pinned at the top of the block like Butchery's own mobs (26 Sep). They do not re-anchor per cut.
- Bunfungus stands upright, facing forward.
- The centipede copies how Butchery handles the Ender Dragon.

Drops come from Alex's Delight, plus AMC Delight's predator_cut for the grizzly. Butchery's cow, goat, hoglin and polar bear tables are the templates.

**27 Sep: Part 2b.** The user asked for every AM mob AMC Delight gives a drop, which added 42 more for 55 total. Kinds:
- FULL, STAGED (the birds use Part 1's crow scheme), SINGLE.
- SEGMENTED: centipede and anaconda, drawn by a BER.
- LARGE: orca, cachalot, giant squid and sunbird, drawn by a BER at up to 8 blocks and never hung, following Butchery's Ender Dragon.

How it's built:
- Configs live in `tools/mobs_amcd.py`.
- Java registers from the generated `gen/GeneratedMobs.java`.
- Shapes are in `resources/butcheryam2/shapes.json`, because the Java version passed the 64 KB static-initialiser limit.

Status:
- 72 GameTests pass, with a negative control checked.
- The showcase was reviewed for all 55.
- The centipede was fixed to lie on its back (it had been upright).
- 31 AM creatures still drop no carcass (bugs, Nether/End/cave oddities); see the chat report.

**27 Sep, later: Part 2c.** Added the user's hand-assigned drops for 21 more creatures; with the variant blocks that is 80 creature entries. Configs are in `tools/mobs_rest.py`.
- STEPS kind: up to 5 cuts, each hiding named parts.
- Fragile steps copy Butchery's Enderman heart (message, sound and particles).
- Variants match on NBT or on a getter called by reflection.
- The cockroach's maraca comes back as a death extra.
- The bone serpent is segmented, with a separate texture per piece.
- Murmur: the body, neck and head models are stacked into one model; it gets a ghost texture after undressing and ectoplasm through the optional tag `butchery_alexs_mobs_2:ectoplasm`.
- The death drop is now a global loot modifier. The earlier clear-all-drops approach destroyed equipment.
- 103 GameTests pass; a negative control was checked.

**27 Sep, revisions:**
- Guster cancelled.
- Froststalker: plain and spiked variants, 1 horn; only the spiked one drops ice.
- Obsidian laviathan drops obsidian.
- Bug bodies drop Miner's Delight arthropod (optional tag); the froststalker body drops the AMCD reptile cut.
- Warped mosco has 9 steps, so STEPS carcasses now use the Long block: step i is state 2i on the ground and 2i+1 hung.
- New items: warped_mosco_proboscis and insect_wing. Their textures are generated; the user offered to draw art. The user chose to make the insect wing interchangeable with bee wings only.
- Void worm is segmented at 12 pieces, near full size over a long curve; splitter worms drop arthropod.
- Cleaver kills are now recorded at death, because the void worm rolls its loot 80 ticks later.
- 105 tests pass.
- 27 Sep round:
  - Bone serpent neck (1st piece behind the head) drops bone blocks; `neck` drop in SegmentedSpecs.neckDrop.
  - Void worm: real length is 25-39 segments (50 is its XP, not its length), read via `getSegmentCount` + offset -3; drawn growing x1.0-1.5 with smaller tails; flat item sprite like the Ender Dragon icon.
  - Rain frog and warped toad are STEPS frog dissections (orange/gray legs, frog heart, organs as a list); the toad keeps shroomlight + nether wart.
  - Insect wing is now a single procedural wing. The wing and void worm sprites are placeholders.
  - 109 tests pass.
  - Void worm segments now joined (spacing 22).
  - Void worm sprite is final: the user's edit of draft "K2" (no middle mandible, bloated head, curled neck), palette-snapped, at tools/art/void_worm_carcass.png; gen.py copies it for both worm carcasses. Reinstalled 27 Sep.
- INSTALLED in `1.21.1 slim` mods on 27 Sep; in-game untested. The friend still needs the jar (next friend ZIP).

**How to apply:**
- Regenerate models and data with `tools/gen.py`. It needs AM model classes decompiled with Vineflower (`_research/vineflower.jar` in Part 1).
- The README documents the converter, GameTests (23 pass, negative control checked) and the auto-screenshot showcase (`runWorldgenServer` then `runShowcaseClient`; port 25611, since 25565 is taken).
- The jar is built at `build/libs/` and was NOT installed in `1.21.1 slim`. Ask before installing, and remember the friend needs it too ([[friend-update-zips]]).
- In-game survival play is untested: real kills, bleeding into a grate, real hook hanging. Only GameTests and the rendered showcase were checked.

- 8 Oct hook fix (Part 1 AND Part 2): tryHang used to replace the hook/rope with the carcass, so the last cut left air and the hook was lost. Big hung models (up to ~15 px above and 12 px sideways) also hid neighbouring hooks, which "reappeared" as cuts removed parts. Now, like Butchery's own Place*Procedure: the carcass goes in the AIR block below the hook/rope and the hook is untouched. Tests: Part 1 hookSurvivesButchering (11/11), Part 2 carcassHangsOnAHook (109/109). Showcase now puts hooks over the hung rows, with `<mob>_hung` close-ups. Both jars reinstalled 8 Oct, in-game untested; carcasses already hung in the world still sit in the hook's spot. Friend needs both jars.- 8 Oct: see-through faces fix (convert.py). Boxes that lost a transparent face, or have holed faces, now get one-sided back faces on the same plane (`<part>_back` elements). Alex's Mobs renders entities noCull; block faces are one-sided. Cockroach wings were the report; ~45 mobs' models changed. 109/109, reinstalled.
- Arthropods: none of Part 1 or Part 2 bleed. FULL kinds are all mammals/reptiles/sharks; bugs are STEPS/SINGLE/SEGMENTED with no drained form. Butchery's spider has no bleeding either: SpidercarcassBlock calls only the broken procedure; its cut-up uses the axe-strip and honey-step sounds plus a 2001 levelEvent, and no blood particles. Ours sends red BLOOD particles on every cleaver cut, bugs included. Asked the user what they saw (8 Oct).
- Generator inputs are gone whenever the scratchpad is cleared. To rebuild: unzip the alexsmobs jar (run-client/mods); vineflower (alex-mobs-butchery/_research/vineflower.jar) on client/model/Model*.class; unzip the butchery jar minus net/*. --known-items is optional. gen.py clears its outputs first, so back up src/main/resources and gen/ before a risky run.
- 8 Oct: tool hint fix (Part 1 + Part 2 HintOverlays). The ported hint vignette only showed when the right tool was ALREADY held. It now shows whatever is held, like Butchery's own hints (Skinningknifehintoverlay and the rest check carcass tags and stage, not the hand). Our blocks aren't in butchery:carcass tags, so Butchery's own hints never fire on them. Showcase gained `hint_<mob>` shots (creative, empty hand, HUD on). Both jars reinstalled, 11/11 + 109/109.
