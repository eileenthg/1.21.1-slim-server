"""Part 2c: the remaining Alex's Mobs creatures, with drops as the user assigned them (27 Sep).

Most of these come apart with a list of steps (`kind=STEPS`): each step names the tool, the model
parts it removes, and what it drops, so "one wing fragment per wing" is two knife steps. A step may
be `fragile` (Butchery's Enderman heart): the item comes off whole only by chance, otherwise it is
lost with a sound and a message, and an optional fallback drops instead.

Variants are matched on the entity: ("nbt", key, op, value) reads saved NBT; ("method", name, op,
value) calls the entity's getter, for state Alex's Mobs keeps only in synced data.
"""

from mobs import AM, cnt

STEPS = "steps"
MOD = "butchery_alexs_mobs_2"
MAX_STEPS = 12
ET = "eternal_tales"


def chance(item, p, lo=1, hi=None):
    """An extra drop that only happens with probability p."""
    return (item, lo, hi if hi is not None else lo, p)


FROG_ORGANS = ["butchery:blocks/organs_drop_1_frog", "butchery:blocks/organs_drop_2", "butchery:blocks/organs_drop_3"]


def step(tool, hide=(), drops=(), texture=None, fragile=None, organs=False):
    """organs: False, True (Butchery's usual three organ tables) or a list of Butchery tables."""
    return dict(tool=tool, hide=list(hide), drops=list(drops), texture=texture, fragile=fragile, organs=organs)


def steps_mob(entity, model, texture, name, steps, **kw):
    return dict(kind=STEPS, entity=entity, model=model, texture=texture, name=name, steps=steps, **kw)


def single(entity, model, texture, name, drops, **kw):
    return steps_mob(entity, model, texture, name, [step("ANY", drops=drops)], **kw)


C, K = "CLEAVER", "KNIFE"

MOBS = {
    "banana_slug": steps_mob(
        "alexsmobs:banana_slug", "ModelBananaSlug", "banana_slug/banana_slug_0", "Banana Slug", [
            step(K, ["leftAntenna", "rightAntenna"], [cnt(f"{AM}:banana_slug_slime", 1), chance(f"{AM}:banana_slug_slime", 0.3)]),
            step(C, ["tail"], [cnt(f"{AM}:banana_slug_slime", 1), chance(f"{AM}:banana_slug_slime", 0.3)]),
            step(C, [], [cnt(f"{AM}:banana_slug_slime", 1), chance(f"{AM}:banana_slug_slime", 0.3)]),
        ], ground_pose="upright", hanging_pose="upright", render_scale=0.9),

    "cockroach": steps_mob(
        "alexsmobs:cockroach", "ModelCockroach", "cockroach", "Cockroach", [
            step(K, ["left_wing"], [cnt(f"{AM}:cockroach_wing_fragment", 1)]),
            step(K, ["right_wing"], [cnt(f"{AM}:cockroach_wing_fragment", 1)]),
            step(C, [], [cnt("#butchery_alexs_mobs_2:arthropod", 1)]),
        ], ground_pose="upright", render_scale=0.85,
        # the dancing variant keeps its maraca and sombrero on death, as Alex's Mobs drops them
        death_extras=dict(when=("nbt", "Maracas", "==", 1),
                          drops=[cnt(f"{AM}:maraca", 1), chance(f"{AM}:sombrero", 0.2)])),

    "comb_jelly": single("alexsmobs:comb_jelly", "ModelCombJelly", "comb_jelly_blue", "Comb Jelly",
                         [cnt(f"{AM}:rainbow_jelly", 1), chance(f"{AM}:rainbow_jelly", 0.35)],
                         ground_pose="upright"),

    "cosmaw": single("alexsmobs:cosmaw", "ModelCosmaw", "cosmaw", "Cosmaw",
                     [cnt("minecraft:chorus_fruit", 1, 2)]),

    # Alex's Mobs keeps the blood level in synced data only, so the variant asks the entity
    "crimson_mosquito_fed": steps_mob(
        "alexsmobs:crimson_mosquito", "ModelCrimsonMosquito", "crimson_mosquito", "Fed Crimson Mosquito", [
            step(C, ["head"], [cnt(f"{AM}:mosquito_proboscis", 1)]),
            step(K, [], [cnt(f"{AM}:blood_sac", 1)], texture="skinned"),
            step(C, [], [cnt("#butchery_alexs_mobs_2:arthropod", 1)]),
        ], match=("method", "getBloodLevel", ">", 0)),
    "crimson_mosquito": steps_mob(
        "alexsmobs:crimson_mosquito", "ModelCrimsonMosquito", "crimson_mosquito", "Crimson Mosquito", [
            step(C, ["head"], [cnt(f"{AM}:mosquito_proboscis", 1)]),
            step(C, [], [cnt("#butchery_alexs_mobs_2:arthropod", 1)]),
        ], match=("method", "getBloodLevel", "==", 0)),

    "farseer": steps_mob(
        "alexsmobs:farseer", "ModelFarseer", "farseer/farseer", "Farseer", [
            step(C, ["leftArm"], [cnt(f"{AM}:farseer_arm", 1)]),
            step(C, ["rightArm"], [cnt(f"{AM}:farseer_arm", 1)]),
            step(C, ["leftArm2"], [cnt(f"{AM}:farseer_arm", 1)]),
            step(C, ["rightArm2"], [cnt(f"{AM}:farseer_arm", 1)]),
            step(C, [], []),
        ]),

    # a potted flutter's pot still drops on death: it is equipment, not loot
    "flutter": single("alexsmobs:flutter", "ModelFlutter", "flutter", "Flutter",
                      [cnt("minecraft:spore_blossom", 1)], ground_pose="upright"),

    "fly": single("alexsmobs:fly", "ModelFly", "fly", "Fly",
                  [cnt(f"{AM}:maggot", 1), chance(f"{AM}:maggot", 0.3), chance(f"{AM}:maggot", 0.08)],
                  ground_pose="upright"),

    "froststalker": steps_mob(
        "alexsmobs:froststalker", "ModelFroststalker", "froststalker_nospikes", "Froststalker", [
            step(C, ["horn"], [cnt(f"{AM}:froststalker_horn", 1)]),
            step(C, [], [cnt("amcdelight:reptile_cut", 1, 2)]),
        ], match=("nbt", "Spiked", "==", 0), exclude=["icespikesleft", "icespikesright"]),
    "froststalker_spiked": steps_mob(
        "alexsmobs:froststalker", "ModelFroststalker", "froststalker", "Spiked Froststalker", [
            step(C, ["horn"], [cnt(f"{AM}:froststalker_horn", 1)]),
            step(C, ["icespikesleft", "icespikesright"],
                 [cnt("minecraft:packed_ice", 1, 2), cnt("minecraft:blue_ice", 1),
                  chance("minecraft:packed_ice", 0.35), chance("minecraft:blue_ice", 0.2)]),
            step(C, [], [cnt("amcdelight:reptile_cut", 1, 2)]),
        ], match=("nbt", "Spiked", "==", 1)),

    # the saddle and helmet are equipment, so they still drop on death
    "laviathan": steps_mob(
        "alexsmobs:laviathan", "ModelLaviathan", "laviathan", "Laviathan", [
            step(C, ["neck"], [cnt("minecraft:magma_block", 1, 2)]),
            step(K, ["shell"], [cnt("minecraft:blackstone", 2, 4), chance("minecraft:blackstone", 0.4, 1, 2)]),
            step(C, [], [cnt("minecraft:magma_block", 2, 4), chance("minecraft:magma_block", 0.4, 1, 2)]),
        ], large=True, match=("nbt", "Obsidian", "==", 0)),
    "laviathan_obsidian": steps_mob(
        "alexsmobs:laviathan", "ModelLaviathan", "laviathan_obsidian", "Obsidian Laviathan", [
            step(C, ["neck"], [cnt("minecraft:obsidian", 1, 2)]),
            step(K, ["shell"], [cnt("minecraft:blackstone", 2, 4), chance("minecraft:blackstone", 0.4, 1, 2)]),
            step(C, [], [cnt("minecraft:obsidian", 2, 4), chance("minecraft:obsidian", 0.4, 1, 2)]),
        ], large=True, match=("nbt", "Obsidian", "==", 1)),

    # workers have no loot; only a queen leaves a carcass
    "leafcutter_ant_queen": single(
        "alexsmobs:leafcutter_ant", "ModelLeafcutterAntQueen", "leafcutter_ant_queen", "Leafcutter Ant Queen",
        [cnt(f"{AM}:leafcutter_ant_pupa", 1)], match=("nbt", "Queen", "==", 1), ground_pose="upright"),

    "mimicube": single("alexsmobs:mimicube", "ModelMimicube", "mimicube", "Mimicube",
                       [cnt(f"{AM}:mimicream", 1), chance(f"{AM}:mimicream", 0.35)], ground_pose="upright"),

    # body, neck and floating head are separate models; the carcass stacks them
    "murmur": steps_mob(
        "alexsmobs:murmur", "ModelMurmurBody", "murmur", "Murmur", [
            step(K, [], [], texture="ghost", fragile=dict(
                item=f"{AM}:unsettling_kimono", chance=0.2,
                fail_drops=[cnt("minecraft:red_wool", 1, 2)], sound="minecraft:item.armor.equip_leather",
                particle="minecraft:poof",
                ok="You manage to carefully remove the §4Unsettling Kimono§f!",
                fail="The §4Unsettling Kimono§f tore apart as you pulled it off..")),
            step(C, ["neck_root", "head_root"], [cnt(f"{AM}:elastic_tendon", 2, 3)]),
            # Eternal Tales' ectoplasm, through an optional tag so the table still loads without it
            step(C, [], [cnt("#butchery_alexs_mobs_2:ectoplasm", 1, 3)]),
        ], compose=[("ModelMurmurNeck", "neck", (0.0, -31.0, 0.0)), ("ModelMurmurHead", "head", (0.0, -43.0, 0.0))],
        render_scale=0.85),

    "rocky_roller": steps_mob(
        "alexsmobs:rocky_roller", "ModelRockyRoller", "rocky_roller", "Rocky Roller", [
            step(C, ["HSpikes_r1", "HSpikes_r2", "VSpikes_r1", "VSpikes_r2", "VSpikes_r3", "VSpikes_r4"],
                 [cnt("minecraft:pointed_dripstone", 1, 2)]),
            step(K, [], [cnt(f"{AM}:rocky_shell", 1)], texture="skinned"),
            step(C, [], [cnt("minecraft:tuff", 1, 2)]),
        ]),

    "skelewag": steps_mob(
        "alexsmobs:skelewag", "ModelSkelewag", "skelewag_0", "Skelewag", [
            # the sword is held, not modelled: half the time it snaps
            step(K, [], [], fragile=dict(
                item=f"{AM}:skelewag_sword", chance=0.5, fail_drops=[], sound="minecraft:entity.item.break",
                particle="minecraft:crit",
                ok="You manage to carefully pry loose the §bSkelewag Sword§f!",
                fail="The §bSkelewag Sword§f snapped as you pried it loose..")),
            step(C, [], [cnt(f"{AM}:fish_bones", 1, 2), cnt("minecraft:bone", 1, 2)]),
        ]),

    "soul_vulture_heart": steps_mob(
        "alexsmobs:soul_vulture", "ModelSoulVulture", "soul_vulture/soul_vulture", "Soul Vulture (Soul Heart)", [
            step(C, ["neck"], []),
            step(K, ["heart"], [cnt(f"{AM}:soul_heart", 1)]),
            step(C, [], [cnt("minecraft:bone", 1, 2), cnt("minecraft:coal", 0, 1)]),
        ], match=("nbt", "SoulLevel", ">", 2)),
    "soul_vulture": steps_mob(
        "alexsmobs:soul_vulture", "ModelSoulVulture", "soul_vulture/soul_vulture", "Soul Vulture", [
            step(C, ["neck"], []),
            step(C, [], [cnt("minecraft:bone", 1, 2), cnt("minecraft:coal", 0, 1)]),
        ], match=("nbt", "SoulLevel", "<=", 2), exclude=["heart"]),

    "straddler": steps_mob(
        "alexsmobs:straddler", "ModelStraddler", "straddler", "Straddler", [
            step(C, ["horn_left", "horn_right"], [cnt(f"{AM}:straddlite", 1)]),
            step(C, [], [cnt("minecraft:basalt", 0, 3)]),
        ], render_scale=1.2),

    "tarantula_hawk": steps_mob(
        "alexsmobs:tarantula_hawk", "ModelTarantulaHawk", "tarantula_hawk", "Tarantula Hawk", [
            step(K, ["wing_left"], [cnt(f"{AM}:tarantula_hawk_wing_fragment", 1)]),
            step(K, ["wing_right"], [cnt(f"{AM}:tarantula_hawk_wing_fragment", 1)]),
            step(C, [], [cnt("#butchery_alexs_mobs_2:arthropod", 1, 2)]),
        ], render_scale=0.9),

    "rain_frog": steps_mob(
        "alexsmobs:rain_frog", "ModelRainFrog", "rain_frog_0", "Rain Frog", [
            step(K, ["left_leg"], [cnt("butchery:raw_orange_frog_leg", 1)]),
            step(K, ["right_leg"], [cnt("butchery:raw_orange_frog_leg", 1)]),
            step(K, [], [cnt("butchery:frog_heart", 1)], texture="skinned"),
            step(K, [], [], organs=FROG_ORGANS),
        ], exclude=["tongue"], ground_pose="upright", render_scale=0.9),

    # a frog dissection, keeping its glowing glands and nether wart
    "warped_toad": steps_mob(
        "alexsmobs:warped_toad", "ModelWarpedToad", "warped_toad", "Warped Toad", [
            step(K, ["left_gland", "right_gland"], [cnt("minecraft:shroomlight", 2)]),
            step(K, ["left_leg"], [cnt("butchery:raw_gray_frog_leg", 1)]),
            step(K, ["right_leg"], [cnt("butchery:raw_gray_frog_leg", 1)]),
            step(K, [], [cnt("butchery:frog_heart", 1)], texture="skinned"),
            step(K, [], [cnt("minecraft:nether_wart", 1, 3)], organs=FROG_ORGANS),
        ], exclude=["tongue"], ground_pose="upright", render_scale=1.25),

    "warped_mosco": steps_mob(
        "alexsmobs:warped_mosco", "ModelWarpedMosco", "warped_mosco", "Warped Mosco", [
            step(C, ["shoulder_left"], [cnt(f"{AM}:warped_muscle", 1, 2), chance(f"{AM}:hemolymph_sac", 0.3)]),
            step(C, ["shoulder_right"], [cnt(f"{AM}:warped_muscle", 1, 2), chance(f"{AM}:hemolymph_sac", 0.3)]),
            step(K, ["wingtop_left", "wingtop_right", "wingbottom_left", "wingbottom_right"],
                 [cnt(f"{MOD}:insect_wing", 1, 4)]),
            step(C, ["legfront_left"], [cnt(f"{AM}:warped_muscle", 1), chance(f"{AM}:hemolymph_sac", 0.25)]),
            step(C, ["legfront_right"], [cnt(f"{AM}:warped_muscle", 1), chance(f"{AM}:hemolymph_sac", 0.25)]),
            step(C, ["legback_left"], [cnt(f"{AM}:warped_muscle", 1), chance(f"{AM}:hemolymph_sac", 0.25)]),
            step(C, ["legback_right"], [cnt(f"{AM}:warped_muscle", 1), chance(f"{AM}:hemolymph_sac", 0.25)]),
            step(C, ["head"], [cnt(f"{MOD}:warped_mosco_proboscis", 1)]),
            step(C, [], [cnt(f"{AM}:hemolymph_sac", 2, 4)]),
        ]),

}

SEGMENTED = {
    # 7 to 14 bones long when it spawns; the length is not saved, so the carcass assumes 10
    "bone_serpent": dict(
        entity="alexsmobs:bone_serpent", name="Bone Serpent",
        pieces={"head": ("ModelBoneSerpentHead", None), "body": ("ModelBoneSerpentBody", None),
                "tail": ("ModelBoneSerpentTail", None)},
        piece_textures={"head": "bone_serpent_head", "body": "bone_serpent_mid", "tail": "bone_serpent_tail"},
        head_piece="head", head_part="head", texture="bone_serpent_head",
        layout=["head", "*body", "tail"], default_count=10, max_count=14,
        on_back=False, scale=0.55, spacing=14.4, radius=(15.0, 5.0),
        drops=dict(
            head=[cnt(f"{AM}:bone_serpent_tooth", 1), chance(f"{AM}:bone_serpent_tooth", 0.3)],
            neck=[cnt("minecraft:bone_block", 1, 2), chance("minecraft:bone_block", 0.3)],
            segment=[cnt("minecraft:bone", 1, 2), chance("minecraft:bone", 0.3, 1, 2)],
            tail=[cnt("minecraft:bone", 1, 2), chance("minecraft:bone", 0.3, 1, 2)],
        )),

    # 25 to 39 segments in life, of which the last 3-4 are tail pieces, each larger than the one
    # before (x1.0 to x1.5). The carcass reads the real length from the worm and, like Butchery's
    # Ender Dragon, is drawn near full size over a wide spiral.
    "void_worm": dict(
        entity="alexsmobs:void_worm", name="Void Worm", match=("nbt", "Splitter", "==", 0),
        pieces={"head": ("ModelVoidWorm", None), "body": ("ModelVoidWormBody", None),
                "tail": ("ModelVoidWormTail", None)},
        piece_textures={"head": "void_worm/void_worm_head", "body": "void_worm/void_worm_body",
                        "tail": "void_worm/void_worm_tail"},
        head_piece="head", head_part="root", head_block=False, texture="void_worm/void_worm_head",
        sprite=True,
        layout=["head", "*body", "tail", "tail", "tail"], count_method="getSegmentCount", count_offset=-3,
        default_count=36, max_count=36, grow=0.5, tail_scale=0.85,
        on_back=False, scale=0.85, spacing=22.0, radius=(200.0, 90.0), reach=18.0,
        drops=dict(
            head=[cnt(f"{AM}:void_worm_eye", 1), cnt(f"{AM}:void_worm_mandible", 2)],
            segment=[cnt("#butchery_alexs_mobs_2:arthropod", 1)],
            tail=[cnt("#butchery_alexs_mobs_2:arthropod", 1)],
        )),
    # the worms a void worm splits into: no eye or mandibles, meat only
    "void_worm_splitter": dict(
        entity="alexsmobs:void_worm", name="Void Worm Splitter", match=("nbt", "Splitter", "==", 1),
        pieces={"head": ("ModelVoidWorm", None), "body": ("ModelVoidWormBody", None),
                "tail": ("ModelVoidWormTail", None)},
        piece_textures={"head": "void_worm/void_worm_head", "body": "void_worm/void_worm_body",
                        "tail": "void_worm/void_worm_tail"},
        head_piece="head", head_part="root", head_block=False, texture="void_worm/void_worm_head",
        sprite=True, sprite_art="void_worm_carcass.png",
        layout=["head", "*body", "tail", "tail", "tail"], count_method="getSegmentCount", count_offset=-3,
        default_count=5, max_count=36, grow=0.5, tail_scale=0.85,
        on_back=False, scale=0.85, spacing=22.0, radius=(140.0, 70.0), reach=12.0,
        drops=dict(
            head=[cnt("#butchery_alexs_mobs_2:arthropod", 1)],
            segment=[cnt("#butchery_alexs_mobs_2:arthropod", 1)],
            tail=[cnt("#butchery_alexs_mobs_2:arthropod", 1)],
        )),

}
