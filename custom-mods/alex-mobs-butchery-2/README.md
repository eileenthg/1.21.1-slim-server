# Butchery: Alex's Mobs — Part 2

A private add-on to [Part 1](../alex-mobs-butchery) (the NeoForge port of Butchery: Alex's Mobs)
covering the Alex's Mobs creatures Part 1 leaves out. Separate mod id (`butchery_alexs_mobs_2`),
no code shared with Part 1; the two sit side by side.

Requires Butchery, Alex's Mobs (Continued), Alex's Delight, AMC Delight, and whatever those need
(CodxLib, Farmer's Delight). Nothing is a compile-time dependency.

## Mobs

55 creatures: Part 2's first 12 (Alex's Delight drops) and every Alex's Mobs creature AMC Delight
gives a meat cut (`codx.amcdelight.loot.Butchery`). The table lives in `tools/mobs.py` and
`tools/mobs_amcd.py`; the Java side registers from the generated `gen/GeneratedMobs.java`.

| Kind | How it is butchered | Creatures |
|---|---|---|
| Full | bleed, head, skin (knife), 3 cuts, organs last; hangs on a hook | bison, moose, kangaroo, gazelle, tusklin, grizzly, elephant, rhino, gorilla, endergrade, tiger, snow leopard, sea bear, maned wolf, dropbear, Tasmanian devil, skunk, hammerhead & frilled shark, crocodile, komodo, rattlesnake, terrapin, alligator snapping turtle, raccoon, anteater, platypus, seal, gelada, sugar glider |
| Staged | 3 steps, no blood; birds are Part 1's crow (head, pluck, body) | bunfungus, mungus, blue jay, roadrunner, shoebill, seagull, toucan, potoo, hummingbird |
| Single | one cut, either tool | catfish (3 sizes), lobster, mimic octopus, jerboa, pupfish, triops |
| Segmented | Butchery's Ender Dragon: head, tail, one piece per cut, drawn coiled | cave centipede (5-8 segments), anaconda, bone serpent (10; the neck behind the head gives bone blocks, the rest bones), void worm (its real length, 25-39 segments, growing toward the tail and drawn near full size over a wide spiral; the item is a flat sprite like the Ender Dragon carcass) and its splitter worms |
| Large | Butchery's Ender Dragon: drawn full-size (up to 8 blocks), cuts hide parts; cannot hang | orca, cachalot whale, giant squid, sunbird |
| Steps | a list of up to 12 cuts, each removing named parts (e.g. one wing fragment per wing) | banana slug, cockroach, comb jelly, cosmaw, crimson mosquito (fed / unfed), farseer, flutter, fly, froststalker (plain / spiked), leafcutter ant queen, mimicube, murmur, rocky roller, skelewag, soul vulture (with / without soul heart), straddler, tarantula hawk, warped mosco, rain frog and warped toad (dissected like Butchery's frogs: legs, heart, organs; the toad also gives shroomlight and nether wart); the laviathan (and its obsidian variant) is a large one |

Step lists live in `tools/mobs_rest.py`. A step may be *fragile* like Butchery's Enderman heart
(the murmur's kimono, the skelewag's sword): it comes off whole by chance, otherwise it is lost
with a sound and a message. Variants are matched on saved NBT or on an entity getter. Death
extras (the dancing cockroach's maraca and sombrero) drop beside the carcass.

The carcass replaces the death *loot table* only (a global loot modifier), so equipment and
inventories — saddles, chests, held items, a potted flutter's pot — still drop.

Two items of this mod's own: the **Warped Mosco Proboscis** (item tag `proboscises`, so Alex's
Mobs' blood sprayer, hemolymph blaster and mosquito larva recipes take either proboscis) and a
generic **Insect Wing** (tag `insect_wings`, with Butchery's bee wings). The proboscis is
the crimson one recoloured and the wing (one wing, as it drops 1-4) is drawn procedurally; the void
worm carcass sprite is rendered from its head model. All are placeholders; replace them in
`assets/butchery_alexs_mobs_2/textures/item/` and in `gen.py` if drawn by hand.

Drops: the Alex's Delight / AMC Delight cut for the creature (game, predator, fowl, reptile,
small-game cut, blubber, shark steak, calamari, fish fillet), Alex's Mobs' own items where they
fit (furs, hides, scutes, teeth, claws, feathers, ambergris, shed snake skin), and Butchery's
fat, hooves, bones and shared organ tables. Kills need a cleaver. Carcass drops replace the
death loot at low priority, so AMC Delight's random bonus cut is replaced too.

## How the models are made

`tools/gen.py` converts Alex's Mobs' own entity models (their Java constructors, decompiled)
into Butchery-style block models, rather than drawing new ones:

* `ammodel.py` parses the Citadel `AdvancedModelBox` constructors into part trees and reproduces
  Citadel's box geometry and per-vertex UVs exactly.
* `convert.py` poses the model (lying on its side belly towards the viewer; hanging head-up from
  one point, where the head or neck meets the body, pinned at the top of the block the way
  Butchery's own carcasses hang, so the head rises above the hook and cuts never move the body;
  heads upright), scales it to fit a block model's -16..32 limit, and turns each box
  into a block element. An element allows one rotation of -45..45 in 22.5 steps, so each box's
  orientation is factored into an exact right-angle turn plus the nearest legal rotation; the
  leftover is the conversion error, printed per mob. UVs are solved per face, including the face
  `rotation` where a box was turned sideways. Faces whose texture region is fully transparent
  are dropped (e.g. the plain moose texture's antlers).
* Stages remove parts: headless drops the head (or neck), cut 1 the front legs, cut 2 the hind
  legs; skinned swaps in a generated pale, marbled flesh texture in Part 1's style.
* The block models use Alex's Mobs' textures in place (`assets/minecraft/atlases/blocks.json`
  adds them to the block atlas); only the skinned textures are generated files.
* It also writes blockstates, item models (display transforms fitted per model), loot tables,
  entity tags, `butchery:heads`, lang, and `GeneratedShapes.java` (outline boxes per model).

The centipede follows Butchery's Ender Dragon carcass: an invisible block whose renderer draws
Alex's Mobs' centipede pieces (converted to vanilla layers in `gen/CentipedeLayers.java`) coiled
on its back, hiding one more piece per cut. The kill carries its segment count (5-8) into the
carcass item.

`tools/preview.py` software-renders the converted models next to the source models for review.

Regenerate after an Alex's Mobs update (decompile the model classes with Vineflower first):

```bash
python tools/gen.py <decompiled-model-dir> <alexsmobs-jar-extract> --known-items <ids.txt> --preview out.png
```

## Testing

* `gradlew runGameTestServer` — 105 GameTests (one generated death-drop test per creature) against the real jars (put Butchery, Alex's Mobs,
  CodxLib, Alex's Delight, AMC Delight and Farmer's Delight in `run-gametest/mods/`): every
  mob's death drop, catfish size and centipede length, every drop table loading, the full
  cut sequences, tool gating, hanging on a hook, a shape for every block state.
* `gradlew runWorldgenServer` then `runShowcaseClient` (`-PshowcaseOnly=a,b` for some) — a flat world where every stage
  is laid out and photographed automatically into `run-client/screenshots/`
  (`-Dbutcheryam2.showcase=true`; off otherwise).
