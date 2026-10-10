"""Part 2b: every Alex's Mobs creature AMC Delight gives a meat cut on death.

AMC Delight's list lives in codx.amcdelight.loot.Butchery: game, predator, fowl, reptile and
small-game cuts, blubber, shark steak, calamari and fish fillet. These carcasses yield that cut,
with Butchery's own templates (cow, wolf, polar bear, dolphin, turtle) and Part 1's caiman,
capuchin and crow for fat, bones, hides and feathers.
"""

from mobs import AM, BU, FULL, SINGLE, STAGED, cnt

AMCD = "amcdelight"
FAT, BONE, HOOF, LEATHER = f"{BU}:animal_fat", "minecraft:bone", f"{BU}:hoof", "minecraft:leather"
GAME, PRED = f"{AMCD}:game_cut", f"{AMCD}:predator_cut"
SMALL, REPT = f"{AMCD}:small_game_cut", f"{AMCD}:reptile_cut"
FEATHER = cnt("minecraft:feather", 1, 3)


def full(entity, model, texture, name, head, cut1, cut2, meat, skin, head_extra=(), fat=(1, 2),
         meat_count=(1, 2), last_extra=(), cut1_extra=(), cut2_extra=(), bones=(1, 3), **kw):
    """A creature that bleeds: head, skin, three cuts, organs last."""
    return dict(
        kind=FULL, entity=entity, model=model, texture=texture, name=name,
        head=head, head_block=kw.pop("head_block", "head"), cut1=cut1, cut2=cut2,
        drops=dict(
            head=[cnt("@head", 1), *head_extra],
            skin=list(skin),
            cut1=[cnt(meat, *meat_count), cnt(FAT, *fat), *cut1_extra],
            cut2=[cnt(meat, *meat_count), cnt(FAT, *fat), *cut2_extra],
            cut3=[cnt(meat, *meat_count), cnt(FAT, *fat), cnt(BONE, *bones), *last_extra],
        ),
        **kw,
    )


def bird(entity, model, texture, name, head, feathers, meat=(1, 2), **kw):
    """Part 1's crow: the cleaver takes the head, a knife plucks, the cleaver takes the body."""
    return dict(
        kind=STAGED, entity=entity, model=model, texture=texture, name=name,
        stage1=[head], stage2=[], stage2_skinned=True, head=head,
        head_block=kw.pop("head_block", "head"), organs=True,
        drops=dict(
            cut1=[cnt("@head", 1)],
            peel=list(feathers),
            cut2=[cnt(f"{AMCD}:fowl_cut", *meat), cnt(BONE, 0, 1)],
        ),
        **kw,
    )


def single(entity, model, texture, name, drops, **kw):
    return dict(kind=SINGLE, entity=entity, model=model, texture=texture, name=name,
                drops=dict(cut=list(drops)), **kw)


MOBS = {
    # ---- game cut (Butchery cow / camel template)
    "elephant": full(
        "alexsmobs:elephant", "ModelElephant", "elephant/elephant", "Elephant", "head",
        ["left_arm", "right_arm"], ["left_leg", "right_leg", "tail"], GAME, [cnt(LEATHER, 3, 5)],
        meat_count=(2, 4), fat=(2, 4), bones=(3, 6), render_scale=1.1,
        exclude=["cabin", "left_chest", "right_chest", "left_megatusk", "right_megatusk"]),
    "rhinoceros": full(
        "alexsmobs:rhinoceros", "ModelRhinoceros", "rhinoceros", "Rhinoceros", "head",
        ["leftArm", "rightArm"], ["leftLeg", "rightLeg"], GAME, [cnt(LEATHER, 2, 4)],
        meat_count=(2, 3), fat=(1, 3), bones=(2, 4), render_scale=1.1,
        cut1_extra=[cnt(HOOF, 2)], last_extra=[cnt(HOOF, 2)]),
    "gorilla": full(
        "alexsmobs:gorilla", "ModelGorilla", "gorilla", "Gorilla", "head",
        ["leftArm", "rightArm"], ["leftLeg", "rightLeg"], GAME, [cnt(LEATHER, 1, 2)]),
    "endergrade": full(
        "alexsmobs:endergrade", "ModelEndergrade", "endergrade", "Endergrade", "head",
        ["legfrontL", "legfrontR", "legmidL", "legmidR"], ["legbackL", "legbackR", "tail"],
        GAME, [cnt("minecraft:chorus_fruit", 1, 2)], render_scale=1.2),
    "mungus": dict(
        kind=STAGED, entity="alexsmobs:mungus", model="ModelMungus", texture="mungus", name="Mungus",
        # no head apart from its body: legs, then the spore sack, then the body
        stage1=["leg_left", "leg_right"], stage2=["sack"], ground_pose="upright", hanging_pose="upright",
        drops=dict(cut1=[cnt(GAME, 1)], peel=[cnt(f"{AM}:mungal_spores", 1, 2)],
                   cut2=[cnt(GAME, 1, 2), cnt(FAT, 1)])),

    # ---- predator cut (Butchery wolf / polar bear template)
    "tiger": full(
        "alexsmobs:tiger", "ModelTiger", "tiger/tiger", "Tiger", "head",
        ["armleft", "armright"], ["legleft", "legright", "tail"], PRED, [cnt(LEATHER, 1, 3)],
        meat_count=(1, 3)),
    "snow_leopard": full(
        "alexsmobs:snow_leopard", "ModelSnowLeopard", "snow_leopard", "Snow Leopard", "head",
        ["armLeft", "armRight"], ["legLeft", "legRight", "tail1"], PRED, [cnt(LEATHER, 1, 2)],
        exclude=["bubble"], render_scale=0.9),
    "sea_bear": full(
        "alexsmobs:sea_bear", "ModelSeaBear", "sea_bear", "Sea Bear", "head",
        ["left_arm", "right_arm"], ["left_leg", "right_leg", "tail"], PRED, [cnt(f"{AM}:bear_fur", 1, 2)],
        meat_count=(1, 3), fat=(1, 3)),
    "maned_wolf": full(
        "alexsmobs:maned_wolf", "ModelManedWolf", "maned_wolf", "Maned Wolf", "neck",
        ["left_arm", "right_arm"], ["left_leg", "right_leg", "tail"], PRED, [cnt(LEATHER, 1, 2)],
        render_scale=0.85),
    "dropbear": full(
        "alexsmobs:dropbear", "ModelDropBear", "dropbear", "Dropbear", "head",
        ["arm_left", "arm_right"], ["leg_left", "leg_right"], PRED, [cnt(LEATHER, 1, 2)],
        cut1_extra=[cnt(f"{AM}:dropbear_claw", 1, 2)]),
    "tasmanian_devil": full(
        "alexsmobs:tasmanian_devil", "ModelTasmanianDevil", "tasmanian_devil", "Tasmanian Devil", "head",
        ["armLeft", "armRight"], ["legLeft", "legRight", "tail"], PRED, [cnt(LEATHER, 1)],
        meat_count=(1, 1), fat=(1, 1), bones=(1, 2)),
    "skunk": full(
        "alexsmobs:skunk", "ModelSkunk", "skunk", "Skunk", "head",
        ["leftArm", "rightArm"], ["leftLeg", "rightLeg", "tail"], PRED, [cnt("minecraft:rabbit_hide", 1)],
        meat_count=(1, 1), fat=(1, 1), bones=(1, 2)),

    # ---- blubber (Butchery dolphin template)
    "orca": full(
        "alexsmobs:orca", "ModelOrca", "orca_ne", "Orca", "head",
        ["fin_left", "fin_right", "fintop"], ["tail1"], f"{AMCD}:blubber", [cnt(f"{AMCD}:blubber", 2, 4)],
        meat_count=(2, 4), fat=(2, 4), bones=(2, 5), render_scale=1.3, large=True),
    "cachalot_whale": full(
        "alexsmobs:cachalot_whale", "ModelCachalotWhale", "cachalot/cachalot_whale", "Cachalot Whale", "head",
        ["arm_left", "arm_right", "top_fin"], ["tail1"], f"{AMCD}:blubber", [cnt(f"{AMCD}:blubber", 3, 6)],
        head_extra=[cnt(f"{AM}:cachalot_whale_tooth", 1, 3)], meat_count=(3, 6), fat=(3, 6), bones=(4, 8),
        last_extra=[cnt(f"{AM}:ambergris", 0, 1)], large=True),

    # ---- shark steak
    "hammerhead_shark": full(
        "alexsmobs:hammerhead_shark", "ModelHammerheadShark", "hammerhead_shark", "Hammerhead Shark", "head",
        ["finL", "finR", "topfin"], ["tail1"], f"{AMCD}:shark_steak", [cnt(LEATHER, 1, 2)],
        head_extra=[cnt(f"{AM}:shark_tooth", 1, 3)], fat=(0, 1), bones=(1, 2)),
    "frilled_shark": full(
        "alexsmobs:frilled_shark", "ModelFrilledShark", "frilled_shark", "Frilled Shark", "head",
        ["pectoralfin_left", "pectoralfin_right"], ["tail1"], f"{AMCD}:shark_steak", [cnt(LEATHER, 1)],
        head_extra=[cnt(f"{AM}:shark_tooth", 0, 2)], fat=(0, 1), meat_count=(1, 1), bones=(1, 2),
        render_scale=0.85),

    # ---- calamari
    "giant_squid": dict(
        kind=STAGED, entity="alexsmobs:giant_squid", model="ModelGiantSquid", texture="giant_squid",
        name="Giant Squid",
        stage1=["left_FrontTentacle", "left_FrontMidTentacle", "right_FrontMidTentacle", "left_BackMidTentacle",
                "right_BackMidTentacle", "left_BackTentacle", "right_BackTentacle", "right_FrontTentacle",
                "left_arm", "right_arm"],
        stage2=["beak", "left_eye", "right_eye"],
        hang_at="mantle", large=True,
        drops=dict(cut1=[cnt(f"{AMCD}:calamari", 3, 5)], peel=[cnt("minecraft:ink_sac", 2, 4)],
                   cut2=[cnt(f"{AMCD}:calamari", 2, 4), cnt(f"{AM}:lost_tentacle", 0, 1)])),
    "mimic_octopus": single(
        "alexsmobs:mimic_octopus", "ModelMimicOctopus", "mimic_octopus", "Mimic Octopus",
        [cnt(f"{AMCD}:calamari", 1, 2), cnt("minecraft:ink_sac", 1)], ground_pose="upright", render_scale=0.9),

    # ---- reptile cut (Part 1 caiman template)
    "crocodile": full(
        "alexsmobs:crocodile", "ModelCrocodile", "crocodile_0", "Crocodile", "neck",
        ["left_arm", "right_arm"], ["left_leg", "right_leg", "tail1"], REPT,
        [cnt(f"{AM}:crocodile_scute", 1, 3)], meat_count=(1, 3), exclude=["crown"], render_scale=0.9),
    "komodo_dragon": full(
        "alexsmobs:komodo_dragon", "ModelKomodoDragon", "komodo_dragon", "Komodo Dragon", "neck",
        ["left_shoulder", "right_shoulder"], ["left_hip", "right_hip", "tail1"], REPT,
        [cnt(LEATHER, 1, 2)], exclude=["tongue"]),
    "rattlesnake": full(
        "alexsmobs:rattlesnake", "ModelRattlesnake", "rattlesnake", "Rattlesnake", "head",
        ["neck1"], ["tail1"], REPT, [cnt(f"{AM}:shed_snake_skin", 1)],
        exclude=["tongue"], meat_count=(1, 1), fat=(0, 1), bones=(0, 1),
        cut2_extra=[cnt(f"{AM}:rattlesnake_rattle", 1)], ground_pose="upright"),
    "terrapin": full(
        "alexsmobs:terrapin", "ModelTerrapin", "terrapin/terrapin_green", "Terrapin", "head",
        ["left_arm", "right_arm"], ["left_leg", "right_leg", "tail"], REPT,
        [cnt("minecraft:turtle_scute", 1)], meat_count=(1, 1), fat=(0, 1), bones=(0, 1),
        ground_pose="upright"),
    "alligator_snapping_turtle": full(
        "alexsmobs:alligator_snapping_turtle", "ModelAlligatorSnappingTurtle", "alligator_snapping_turtle",
        "Alligator Snapping Turtle", "neck", ["arm_left", "arm_right"], ["leg_left", "leg_right", "tail"],
        REPT, [cnt(f"{AM}:spiked_scute", 1, 2)], ground_pose="upright"),

    # ---- small game cut (Part 1 capuchin template)
    "raccoon": full(
        "alexsmobs:raccoon", "ModelRaccoon", "raccoon", "Raccoon", "head",
        ["arm_left", "arm_right"], ["leg_left", "leg_right", "tail"], SMALL,
        [cnt("minecraft:rabbit_hide", 1)], meat_count=(1, 1), fat=(1, 1), bones=(1, 2),
        cut2_extra=[cnt(f"{AM}:raccoon_tail", 1)], render_scale=0.75),
    "anteater": full(
        "alexsmobs:anteater", "ModelAnteater", "anteater", "Anteater", "head",
        ["left_arm", "right_arm"], ["left_leg", "right_leg", "tail"], SMALL, [cnt(LEATHER, 1)],
        exclude=["tongue1"]),
    "platypus": full(
        "alexsmobs:platypus", "ModelPlatypus", "platypus", "Platypus", "head",
        ["arm_left", "arm_right"], ["leg_left", "leg_right", "tail"], SMALL,
        [cnt("minecraft:rabbit_hide", 1)], meat_count=(1, 1), fat=(1, 1), bones=(0, 1),
        exclude=["fedora"], render_scale=0.9),
    "seal": full(
        "alexsmobs:seal", "ModelSeal", "seal/seal_brown_0", "Seal", "head",
        ["leftArm", "rightArm"], ["tail"], SMALL, [cnt(LEATHER, 1, 2)],
        cut2_extra=[cnt(f"{AMCD}:blubber", 1, 2)]),
    "gelada_monkey": full(
        "alexsmobs:gelada_monkey", "ModelGeladaMonkey", "gelada_monkey", "Gelada Monkey", "neck",
        ["left_arm", "right_arm"], ["left_leg", "right_leg", "tail"], SMALL, [cnt(LEATHER, 1)]),
    "sugar_glider": full(
        "alexsmobs:sugar_glider", "ModelSugarGlider", "sugar_glider", "Sugar Glider", "head",
        ["leftArm", "rightArm"], ["leftLeg", "rightLeg", "tail"], SMALL,
        [cnt("minecraft:rabbit_hide", 1)], meat_count=(1, 1), fat=(0, 1), bones=(0, 1)),
    "jerboa": single(
        "alexsmobs:jerboa", "ModelJerboa", "jerboa", "Jerboa",
        [cnt(SMALL, 1), cnt("minecraft:rabbit_hide", 0, 1)], render_scale=0.8),
    # ---- fish fillet
    "devils_hole_pupfish": single(
        "alexsmobs:devils_hole_pupfish", "ModelDevilsHolePupfish", "devils_hole_pupfish",
        "Devil's Hole Pupfish", [cnt(f"{AMCD}:fish_fillet", 1)]),
    "triops": single(
        "alexsmobs:triops", "ModelTriops", "triops", "Triops", [cnt(f"{AMCD}:fish_fillet", 1)],
        ground_pose="upright"),

    # ---- fowl cut (Part 1 crow template)
    "blue_jay": bird("alexsmobs:blue_jay", "ModelBlueJay", "blue_jay", "Blue Jay", "head", [FEATHER],
                     meat=(1, 1), render_scale=0.9),
    "roadrunner": bird("alexsmobs:roadrunner", "ModelRoadrunner", "roadrunner", "Roadrunner", "neck",
                       [FEATHER, cnt(f"{AM}:roadrunner_feather", 0, 1)], head_block="neck"),
    "shoebill": bird("alexsmobs:shoebill", "ModelShoebill", "shoebill", "Shoebill", "headPivot",
                     [cnt("minecraft:feather", 2, 4)], meat=(1, 3)),
    "seagull": bird("alexsmobs:seagull", "ModelSeagull", "seagull", "Seagull", "head", [FEATHER]),
    "toucan": bird("alexsmobs:toucan", "ModelToucan", "toucan/toucan_0", "Toucan", "head", [FEATHER],
                   render_scale=0.9),
    "potoo": bird("alexsmobs:potoo", "ModelPotoo", "potoo", "Potoo", "head", [FEATHER]),
    "hummingbird": bird("alexsmobs:hummingbird", "ModelHummingbird", "hummingbird_0", "Hummingbird", "head",
                        [cnt("minecraft:feather", 0, 1)], meat=(1, 1), render_scale=0.75),
    "sunbird": bird("alexsmobs:sunbird", "ModelSunbird", "sunbird", "Sunbird", "neck",
                    [cnt("minecraft:feather", 2, 4)], meat=(1, 3), large=True,
                    ground_pose="upright"),  # wings spread flat, belly down
}

# `large=True`: too big for a block model (it would shrink below half size), so like Butchery's
# Ender Dragon these are drawn by a block-entity renderer at up to 8 blocks, lying on the ground,
# with each cut hiding parts. They cannot be hung.

# Segmented creatures follow Butchery's Ender Dragon: one block drawn by a block-entity renderer,
# every cleaver cut removing one more piece of the body. Layouts are head first; "*body" repeats.
SEGMENTED = {
    "centipede": dict(
        entity="alexsmobs:centipede_head", model="ModelCaveCentipede", texture="cave_centipede",
        name="Cave Centipede", pieces={"head": 0, "body": 1, "tail": 2}, head_piece="head", head_part="head",
        layout=["head", "*body", "tail"], count_nbt="SegCount", default_count=6, max_count=8,
        on_back=True, scale=0.5, spacing=13.4, radius=(13.0, 7.0),
        drops=dict(head=[cnt("@head", 1)], segment=[cnt(f"{AM}:centipede_leg", 2, 4)],
                   tail=[cnt(f"{AM}:centipede_leg", 2, 3)])),
    "anaconda": dict(
        entity="alexsmobs:anaconda", model="ModelAnaconda", texture="anaconda", variant_texture="anaconda_yellow",
        variant_nbt="Yellow", name="Anaconda", pieces={"head": "HEAD", "neck": "NECK", "body": "BODY", "tail": "TAIL"},
        head_piece="head", head_part="part",
        # AnacondaPartIndex.sizeAt: the head, then neck, three body, two neck, tail
        layout=["head", "neck", "body", "body", "body", "neck", "neck", "tail"],
        on_back=False, scale=0.75, spacing=15.0, radius=(12.0, 5.0),
        drops=dict(head=[cnt("@head", 1)], segment=[cnt(REPT, 1, 2), cnt(FAT, 0, 1)],
                   tail=[cnt(REPT, 1), cnt(f"{AM}:shed_snake_skin", 0, 1)])),
}
