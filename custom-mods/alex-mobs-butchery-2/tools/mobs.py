"""Per-mob settings: which Alex's Mobs model to convert, how to cut it up, and what it drops.

Stage models follow Part 1 (and Butchery): whole -> headless -> skinned -> cut 1 -> cut 2, on the
ground (block states 0, 6, 7, 8, 9) and hanging (1, 2, 3, 4, 5). `cut1`/`cut2` name the model
parts removed by those cuts; each list is cumulative with the ones before it.

Drops take after Alex's Delight (and AMC Delight for the grizzly), with Butchery's own cow /
goat / hoglin / polar bear tables as the template for fat, hooves, bones and counts.
"""

AD = "alexsdelight"
AM = "alexsmobs"
BU = "butchery"


def cnt(item, lo, hi=None):
    return (item, lo, hi if hi is not None else lo)


FULL = "full"      # bleeds; fresh + drained blocks, five-step table
STAGED = "staged"  # no blood; one block, three steps (bunfungus)
SINGLE = "single"  # one step with either tool (fish, lobster)

MOBS = {
    "bison": dict(
        kind=FULL, entity="alexsmobs:bison", model="ModelBison", texture="bison",
        head="head", head_block="head",
        cut1=["left_arm", "right_arm"], cut2=["left_leg", "right_leg", "tail"],
        name="Bison",
        drops=dict(
            head=[cnt("@head", 1)],
            skin=[cnt(f"{AM}:bison_fur", 2, 4)],
            cut1=[cnt(f"{AD}:raw_bison", 1, 2), cnt(f"{BU}:animal_fat", 1), cnt(f"{BU}:hoof", 2)],
            cut2=[cnt(f"{AD}:raw_bison", 1, 2), cnt(f"{AD}:bison_mince", 1, 2), cnt(f"{BU}:animal_fat", 1)],
            cut3=[cnt(f"{AD}:raw_bison", 1, 2), cnt(f"{BU}:animal_fat", 1), cnt(f"{BU}:hoof", 2),
                  cnt("minecraft:bone", 1, 4)],
        ),
    ),
    "moose": dict(
        kind=FULL, entity="alexsmobs:moose", model="ModelMoose", texture="moose",
        head="neck", head_block="head",
        cut1=["left_arm", "right_arm"], cut2=["left_leg", "right_leg"],
        name="Moose",
        drops=dict(
            head=[cnt("@head", 1)],
            skin=[cnt("minecraft:leather", 1, 3)],
            cut1=[cnt(f"{AM}:moose_ribs", 1), cnt(f"{BU}:animal_fat", 1), cnt(f"{BU}:hoof", 2)],
            cut2=[cnt(f"{AD}:loose_moose_rib", 2, 4), cnt(f"{BU}:animal_fat", 1)],
            cut3=[cnt(f"{AM}:moose_ribs", 1), cnt(f"{BU}:animal_fat", 1), cnt(f"{BU}:hoof", 2),
                  cnt("minecraft:bone", 1, 4)],
        ),
    ),
    "kangaroo": dict(
        kind=FULL, entity="alexsmobs:kangaroo", model="ModelKangaroo", texture="kangaroo",
        head="neck", head_block="head",
        cut1=["arm_left", "arm_right"], cut2=["leg_left", "leg_right", "tail1"],
        name="Kangaroo",
        drops=dict(
            head=[cnt("@head", 1)],
            skin=[cnt(f"{AM}:kangaroo_hide", 1, 2)],
            cut1=[cnt(f"{AM}:kangaroo_meat", 1), cnt(f"{BU}:animal_fat", 1)],
            cut2=[cnt(f"{AD}:kangaroo_shank", 2), cnt(f"{BU}:animal_fat", 1)],
            cut3=[cnt(f"{AM}:kangaroo_meat", 1, 2), cnt(f"{BU}:animal_fat", 1), cnt("minecraft:bone", 1, 3)],
        ),
    ),
    "gazelle": dict(
        kind=FULL, entity="alexsmobs:gazelle", model="ModelGazelle", texture="gazelle",
        render_scale=0.8, head="neck", head_block="head",
        cut1=["frontlegR", "frontlegL"], cut2=["backlegL", "backlegR", "tail"],
        name="Gazelle",
        # Butchery goat template (lamb cuts); Alex's Mobs' own drop is mutton and a horn.
        drops=dict(
            head=[cnt("@head", 1), cnt(f"{AM}:gazelle_horn", 0, 1)],
            skin=[cnt("minecraft:leather", 1, 2)],
            cut1=[cnt(f"{BU}:raw_leg_of_lamb", 2), cnt(f"{BU}:hoof", 2), cnt(f"{BU}:animal_fat", 1, 2)],
            cut2=[cnt(f"{BU}:raw_lamb_rib", 1), cnt(f"{BU}:raw_lamb_sirloin", 1), cnt(f"{BU}:animal_fat", 1)],
            cut3=[cnt(f"{BU}:raw_lamb_loin", 1), cnt(f"{BU}:raw_leg_of_lamb", 2), cnt(f"{BU}:hoof", 2),
                  cnt("minecraft:bone", 1, 3)],
        ),
    ),
    "tusklin": dict(
        kind=FULL, entity="alexsmobs:tusklin", model="ModelTusklin", texture="tusklin",
        head="head", head_block="head",
        cut1=["arm_left", "arm_right"], cut2=["leg_left", "leg_right"],
        name="Tusklin",
        # Butchery hoglin template.
        drops=dict(
            head=[cnt("@head", 1)],
            skin=[cnt(f"{BU}:hoglin_skin", 1, 2)],
            cut1=[cnt(f"{BU}:raw_hoglin_chunk", 1, 3), cnt(f"{BU}:animal_fat", 1, 3), cnt(f"{BU}:hoof", 2)],
            cut2=[cnt(f"{BU}:raw_hoglin_chunk", 1, 2), cnt(f"{BU}:raw_pork_belly", 1), cnt(f"{BU}:animal_fat", 1, 2)],
            cut3=[cnt(f"{BU}:raw_hoglin_chunk", 1, 2), cnt(f"{BU}:hoof", 2), cnt("minecraft:bone", 1, 4)],
        ),
    ),
    "grizzly_bear": dict(
        kind=FULL, entity="alexsmobs:grizzly_bear", model="ModelGrizzlyBear", texture="grizzly_bear",
        head="head", head_block="head", exclude=["hat", "microphone"],
        cut1=["left_arm", "right_arm"], cut2=["left_leg", "right_leg"],
        name="Grizzly Bear",
        # Butchery polar bear template; the meat is AMC Delight's grizzly cut.
        drops=dict(
            head=[cnt("@head", 1)],
            skin=[cnt(f"{AM}:bear_fur", 1, 3)],
            cut1=[cnt("amcdelight:predator_cut", 1, 3), cnt(f"{BU}:animal_fat", 1, 3)],
            cut2=[cnt("amcdelight:predator_cut", 1, 3), cnt(f"{BU}:animal_fat", 1, 3)],
            cut3=[cnt("amcdelight:predator_cut", 1, 3), cnt(f"{BU}:animal_fat", 1, 3), cnt("minecraft:bone", 1, 3)],
        ),
    ),
    "bunfungus": dict(
        kind=STAGED, entity="alexsmobs:bunfungus", model="ModelBunfungus", texture="bunfungus",
        # a fungus: no blood, no head block; legs, then the cap, then the body
        stage1=["left_leg", "right_leg"], stage2=["shroom_cap"],
        ground_pose="upright", hanging_pose="upright",  # it stands upright, so it faces forward
        hang_at="head",
        name="Bunfungus",
        drops=dict(
            cut1=[cnt(f"{AD}:raw_bunfungus_drumstick", 2)],
            peel=[cnt("minecraft:red_mushroom", 1, 3)],
            cut2=[cnt(f"{AD}:raw_bunfungus", 1, 3)],
        ),
    ),
    "catfish_small": dict(
        kind=SINGLE, entity="alexsmobs:catfish", nbt=("CatfishSize", 0),
        model="ModelCatfishSmall", texture="catfish_small", name="Small Catfish",
        drops=dict(cut=[cnt(f"{AD}:raw_catfish_slice", 1, 2)]),
    ),
    "catfish_medium": dict(
        kind=SINGLE, entity="alexsmobs:catfish", nbt=("CatfishSize", 1),
        model="ModelCatfishMedium", texture="catfish_medium", hang_at="head", name="Catfish",
        drops=dict(cut=[cnt(f"{AD}:raw_catfish_slice", 2, 4), cnt("minecraft:bone", 0, 1)]),
    ),
    "catfish_large": dict(
        kind=SINGLE, entity="alexsmobs:catfish", nbt=("CatfishSize", 2),
        model="ModelCatfishLarge", texture="catfish_large", hang_at="head", name="Large Catfish",
        drops=dict(cut=[cnt(f"{AD}:raw_catfish_slice", 4, 7), cnt("minecraft:bone", 1, 2)]),
    ),
    "lobster": dict(
        kind=SINGLE, entity="alexsmobs:lobster", model="ModelLobster", texture="lobster_red",
        ground_pose="upright", name="Lobster",
        drops=dict(cut=[cnt(f"{AM}:lobster_tail", 1, 2)]),
    ),
}

# The centipede follows Butchery's Ender Dragon: one block drawn by a block-entity renderer,
# every cleaver cut removing one more piece of the body.
CENTIPEDE = dict(
    entity="alexsmobs:centipede_head", model="ModelCaveCentipede", texture="cave_centipede",
    name="Cave Centipede",
    drops=dict(
        head=[cnt("@head", 1)],
        segment=[cnt(f"{AM}:centipede_leg", 2, 4)],
        tail=[cnt(f"{AM}:centipede_leg", 2, 3)],
    ),
)
