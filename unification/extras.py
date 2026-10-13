"""Second pass (16 Sep): geode rarity, mob head merge, workstation merges.
Imported at the end of build.py with the shared names passed in."""
import json, os, re, glob, zipfile, collections


def run(ctx):
    INST, PACK, OUT, idx, put, jar_json, DISABLED_JSON = (
        ctx['INST'], ctx['PACK'], ctx['OUT'], ctx['idx'], ctx['put'], ctx['jar_json'], ctx['DISABLED_JSON'])
    custom, mat, tags_cfg, ph, report = ctx['custom'], ctx['mat'], ctx['tags_cfg'], ctx['ph'], ctx['report']
    item_utag = ctx['item_utag']

    # ---------------------------------------------------------------- geodes
    # Amethyst, citrine and topaz all at the same rarity, leaning rarer than vanilla amethyst (24).
    GEODE_CHANCE = 32
    for name in ('citrine_geode', 'topaz_geode'):
        path = 'data/spectrum/worldgen/placed_feature/%s.json' % name
        j = jar_json('spectrum-*.jar', path)
        for p in j['placement']:
            if p.get('type') == 'minecraft:rarity_filter':
                p['chance'] = GEODE_CHANCE
        put(path, j, 'geode rarity 1 in %d' % GEODE_CHANCE)
    put('data/minecraft/worldgen/placed_feature/amethyst_geode.json',
        {"feature": "minecraft:amethyst_geode",
         "placement": [{"type": "minecraft:rarity_filter", "chance": GEODE_CHANCE},
                       {"type": "minecraft:in_square"},
                       {"type": "minecraft:height_range",
                        "height": {"type": "minecraft:uniform",
                                   "min_inclusive": {"above_bottom": 6},
                                   "max_inclusive": {"absolute": 30}}},
                       {"type": "minecraft:biome"}]},
        'geode rarity 1 in %d' % GEODE_CHANCE)

    # ---------------------------------------------------------------- mob heads
    HEAD_NS = ('supplementaries', 'butchery', 'butchery_alexs_mobs', 'spectrum', 'spectrumcompat')
    heads = [k for k in idx['names'] if re.search(r'_(head|skull)$', k) and k.split(':')[0] in HEAD_NS
             and '_head_mount' not in k]
    by_key = collections.defaultdict(list)
    for h in heads:
        toks = frozenset(re.sub(r'_(head|skull)$', '', h.split(':')[1]).split('_'))
        by_key[toks].append(h)

    # colour or breed words: these mark a *variant* head, where Butchery's (which drop from butchering) win.
    VARIANT_WORDS = {'white', 'orange', 'magenta', 'light', 'blue', 'yellow', 'lime', 'pink', 'gray', 'grey',
                     'cyan', 'purple', 'brown', 'green', 'red', 'black', 'all', 'wild', 'gold', 'leucistic',
                     'calico', 'tabby', 'jellie', 'siamese', 'ragdoll', 'persian', 'british', 'shorthair',
                     'tuxedo', 'creamy', 'chestnut', 'dark', 'bay', 'snowy', 'rusty', 'spotted', 'striped',
                     'ashen', 'woods', 'pale', 'silver', 'cream', 'salt', 'pepper'}

    def pick(toks, members):
        ns = {}
        for m in members:
            ns.setdefault(m.split(':')[0], m)
        if 'supplementaries' in ns:            # wearing these applies Supplementaries' screen effects
            return ns['supplementaries']
        if toks & VARIANT_WORDS:               # a variant head (colour, breed): Butchery's come from butchering
            for p in ('butchery', 'butchery_alexs_mobs'):
                if p in ns:
                    return ns[p]
        for p in ('spectrum', 'spectrumcompat', 'butchery', 'butchery_alexs_mobs'):
            if p in ns:
                return ns[p]
        return members[0]

    head_groups, head_tag, head_dom = [], {}, {}
    for toks, members in sorted(by_key.items(), key=lambda x: sorted(x[1])):
        if len({m.split(':')[0] for m in members}) < 2:
            continue
        dom = pick(toks, members)
        tag = 'c:heads/' + re.sub(r'_(head|skull)$', '', dom.split(':')[1])
        # some heads already sit in a per-head unify tag from the old config; unify on that one instead of
        # a second tag, or AlmostUnified rejects the item as belonging to two unify tags.
        existing = sorted({item_utag[m] for m in members if m in item_utag})
        unify_tag = existing[0] if len(existing) == 1 else tag
        head_groups.append({'dominant': dom, 'members': sorted(members), 'tag': tag, 'unify_tag': unify_tag})
        lst = custom.setdefault(tag, [])
        for m in sorted(members):
            head_tag[m] = tag
            if m != dom:
                head_dom[m] = dom
            if m not in lst:
                lst.append(m)
        if unify_tag != tag:
            lst2 = custom.setdefault(unify_tag, [])
            for m in sorted(members):
                if m not in lst2:
                    lst2.append(m)
        if unify_tag not in mat['tags']:
            mat['tags'].append(unify_tag)
        mat['priority_overrides'][unify_tag] = dom.split(':')[0]
    report['head_groups'] = head_groups

    # Spectrum's own recipe types are not ones AlmostUnified rewrites, so point them at the tags by hand.
    def conv(o, in_result=False):
        if isinstance(o, dict):
            if not in_result and o.get('item') in head_tag and set(o) <= {'item', 'count'}:
                d = {'tag': head_tag[o['item']]}
                if 'count' in o:
                    d['count'] = o['count']
                return d
            return {k: conv(v, in_result or k in ('result', 'results', 'output', 'outputs')) for k, v in o.items()}
        if isinstance(o, list):
            return [conv(x, in_result) for x in o]
        if isinstance(o, str):
            if in_result:
                return head_dom.get(o, o)
            return '#' + head_tag[o] if o in head_tag else o
        return o

    head_recipes = 0
    for pat in ('spectrum-*.jar', 'spectrumcompat-*.jar'):
        for jp in glob.glob(os.path.join(INST, 'mods', pat)):
            z = zipfile.ZipFile(jp)
            for n in z.namelist():
                if not (n.endswith('.json') and '/recipe' in n):
                    continue
                s = z.read(n).decode('utf-8', 'ignore')
                if not any(('"%s"' % m) in s for m in head_tag):
                    continue
                j = json.loads(s)
                j2 = conv(j)
                if j2 != j:
                    put(n, j2, 'head tags')
                    head_recipes += 1
    report['head_recipes_retargeted'] = head_recipes

    # ---------------------------------------------------------------- workstations
    ported = []
    # Mortar and pestle: Butchery's 7 recipes move to Extra Delight's mortar; its block stops being craftable.
    for jp in glob.glob(os.path.join(INST, 'mods', 'butchery-5.2*.jar')):
        z = zipfile.ZipFile(jp)
        for n in z.namelist():
            if not (n.endswith('.json') and '/recipe' in n):
                continue
            j = json.loads(z.read(n))
            if j.get('type') != 'butchery:pestleandmortarrecipetype':
                continue
            put(n, DISABLED_JSON, 'mortar merge')
            name = os.path.basename(n)[:-5]
            put('data/zz_unified/recipe/mortar/%s.json' % name,
                {"type": "extradelight:mortar", "fluidOut": {}, "grinds": 8,
                 "ingredient": j['ingredients'][0], "result": j['output']}, 'ported to Extra Delight mortar')
            ported.append(('butchery pestle', name))
    put('data/butchery/recipe/pestle_and_mortar_recipe.json', DISABLED_JSON, 'mortar merge')
    put('data/zz_unified/recipe/mortar_from_pestle_and_mortar.json',
        {"type": "minecraft:crafting_shapeless", "category": "misc",
         "ingredients": [{"item": "butchery:pestle_and_mortar"}],
         "result": {"count": 1, "id": "extradelight:mortar_stone"}}, 'old pestle and mortar converts')

    # Cutting: Eternal Tales' cutting table recipes move to the Farmer's Delight cutting board
    # (single-input) or to the crafting grid (its multi-input armour recipes, which a board cannot do).
    KNIFE_TOOL = [{"type": "farmersdelight:item_ability", "action": "knife_dig"}, {"tag": "c:tools/knife"}]
    for jp in glob.glob(os.path.join(INST, 'mods', 'Eternal Tales*.jar')):
        z = zipfile.ZipFile(jp)
        for n in z.namelist():
            if not (n.endswith('.json') and '/recipe' in n):
                continue
            j = json.loads(z.read(n))
            t = j.get('type', '')
            if not t.startswith('eternal_tales:cutting_table'):
                continue
            put(n, DISABLED_JSON, 'cutting merge')
            name = os.path.basename(n)[:-5]
            out = j['output']
            oid = out.get('id') or out.get('item')
            if len(j['ingredients']) == 1:
                put('data/zz_unified/recipe/cutting/%s.json' % name,
                    {"type": "farmersdelight:cutting", "ingredients": j['ingredients'],
                     "result": [{"item": {"count": out.get('count', 1), "id": oid}}],
                     "tool": KNIFE_TOOL}, 'ported to the cutting board')
            else:
                put('data/zz_unified/recipe/cutting/%s.json' % name,
                    {"type": "minecraft:crafting_shapeless", "category": "misc",
                     "ingredients": j['ingredients'] + [{"tag": "c:tools/knife"}],
                     "result": {"count": out.get('count', 1), "id": oid}},
                    'ported to the crafting grid (several inputs)')
            ported.append(('eternal tales cutting table', name))
    put('data/eternal_tales/recipe/cutting_table_craft.json', DISABLED_JSON, 'cutting merge')
    put('data/zz_unified/recipe/cutting_board_from_cutting_table.json',
        {"type": "minecraft:crafting_shapeless", "category": "misc",
         "ingredients": [{"item": "eternal_tales:cutting_table"}],
         "result": {"count": 1, "id": "farmersdelight:cutting_board"}}, 'old cutting table converts')
    report['workstation_ported'] = ported

    # ---------------------------------------------------------------- drying racks: both stay, recipes cross-ported
    # Youkai's bamboo rack needs sunlight, so it dries faster; Extra Delight's rack is slower but works anywhere.
    # A maid can operate the Youkai rack (MaidsoulKitchen TaskYhcDryingRack), not the Extra Delight one.
    FAST, SLOW = 0.5, 2.0
    dried = []
    for jarpat, src_type, dst_type, factor, folder in (
            ('extradelight-*.jar', 'extradelight:drying_rack', 'youkaishomecoming:drying_rack', FAST, 'drying_youkai'),
            ('youkaishomecoming-*.jar', 'youkaishomecoming:drying_rack', 'extradelight:drying_rack', SLOW, 'drying_extradelight')):
        for jp in glob.glob(os.path.join(INST, 'mods', jarpat)):
            z = zipfile.ZipFile(jp)
            for n in z.namelist():
                if not (n.endswith('.json') and '/recipe' in n):
                    continue
                j = json.loads(z.read(n))
                if j.get('type') != src_type:
                    continue
                out = {"type": dst_type, "cookingtime": max(100, int(j.get('cookingtime', 1000) * factor)),
                       "experience": j.get('experience', 0.0), "ingredient": j['ingredient'], "result": j['result']}
                if dst_type.startswith('youkaishomecoming'):
                    out['category'] = 'misc'
                put('data/zz_unified/recipe/%s/%s.json' % (folder, os.path.basename(n)[:-5]), out, 'drying rack cross-port')
                dried.append((src_type, os.path.basename(n)[:-5]))
    # Youkai's own recipes also get the faster time where Extra Delight's were slower: halve Youkai's long ones
    report['drying_ported'] = len(dried)

    # ---------------------------------------------------------------- Ecologics compat recipes remapped
    # Ecologics is gone; these compat recipes referenced its items. Point them at what the pack actually has.
    ECO = {'ecologics:coconut': 'crabbersdelight:coconut',
           'ecologics:coconut_slice': 'crabbersdelight:coconut_halve',
           'ecologics:crab_claw': 'crabbersdelight:crab_claw',
           'ecologics:crab_meat': 'youkaishomecoming:crab_meat',
           'ecologics:prickly_pear': 'yungscavebiomes:prickly_peach',
           'ecologics:ice_bricks': 'youkaishomecoming:ice_bricks'}

    def strip_eco(o):
        """drop neoforge:conditions entries that gate on ecologics being present"""
        if isinstance(o, dict):
            d = {}
            for k, v in o.items():
                if k == 'neoforge:conditions':
                    keep = [c for c in v if 'ecologics' not in json.dumps(c)]
                    if keep:
                        d[k] = keep
                else:
                    d[k] = strip_eco(v)
            return d
        if isinstance(o, list):
            return [strip_eco(x) for x in o]
        return o

    eco_done = []
    for jp in glob.glob(os.path.join(INST, 'mods', '*.jar')):
        try:
            z = zipfile.ZipFile(jp)
        except Exception:
            continue
        for n in z.namelist():
            if not (n.endswith('.json') and '/recipe' in n):
                continue
            if '/recipe/ecologicsdelight/' in n:
                continue
            s = z.read(n).decode('utf-8-sig', 'ignore')
            if 'ecologics:' not in s:
                continue
            found = set(re.findall(r'ecologics:[a-z0-9_]+', s))
            if not found or not found <= set(ECO):
                continue                      # something in it has no stand-in; leave it dormant
            for a, b in ECO.items():
                s = s.replace('"%s"' % a, '"%s"' % b)
            j = strip_eco(json.loads(s))
            ids = re.findall(r'"(?:id|item)":\s*"([a-z0-9_.-]+:[a-z0-9_/]+)"', json.dumps(j))
            if any(o not in idx['names'] for o in ids):
                continue                      # compatdelight only registers that item when Ecologics is present
            put(n, j, 'ecologics compat remapped')
            eco_done.append(n.split('/')[-1])
    report['ecologics_remapped'] = eco_done

    # ---------------------------------------------------------------- cutting board: one input, one outcome
    # Two knife recipes on the same input made the board pick one arbitrarily (what CuttingDelight prompts for).
    # Bone dust belongs in a mortar anyway, and compatdelight's cooked duck had two knife recipes of its own.
    put('data/compatdelight/recipe/naturalistdelight/duck_meat.json', DISABLED_JSON, 'cutting ambiguity')

    # ---------------------------------------------------------------- AlmostUnified errors from the old config
    # Temperature damage must not knock the player back (vanilla starving doesn't). Thirst Was Taken tags its
    # dehydrate damage and Cold Sweat tags cold/hot, but not Cold Sweat's difficulty-scaled variants.
    # (Until 19 Sep this held Tough As Nails' thirst and hyperthermia.)
    put('data/minecraft/tags/damage_type/no_knockback.json',
        {"replace": False, "values": [{"id": "cold_sweat:cold_scaling", "required": False},
                                      {"id": "cold_sweat:hot_scaling", "required": False}]},
        'no knockback from scaled temperature damage')
    # Compat Delight typos: its icicle knife recipe outputs icicle_knife1, and glazed frost lily makes a block it
    # never registers
    j = jar_json('compatdelight-*.jar', 'data/compatdelight/recipe/cavebiomesdelight/icicle_knife.json')
    j['result']['id'] = 'compatdelight:icicle_knife'
    put('data/compatdelight/recipe/cavebiomesdelight/icicle_knife.json', j, 'fixed output id')
    put('data/compatdelight/recipe/cavebiomesdelight/glazed_frost_lily.json', DISABLED_JSON, 'output block does not exist')
    # c:squid / c:glow_squid duplicated c:squid_corpse / c:glow_squid_corpse item for item.
    mat['tags'] = [t for t in mat['tags'] if t not in ('c:squid', 'c:glow_squid')]
    # c:foods/raw_meat/squid_tentacle listed the tentacles that c:tentacles already unifies; keep it to calamari.
    st = custom.get('c:foods/raw_meat/squid_tentacle', [])
    custom['c:foods/raw_meat/squid_tentacle'] = [i for i in st if not i.endswith(':tentacles')]
    # Cocktails Delight fills c:tentacles with Crabber's squid AND glow squid tentacles, Alex's lost tentacle and
    # both calamari, so unifying on it merged all of them (squid tentacles crafted the glow squid barrel).
    # Unify the three plain tentacles on a private tag; c:tentacles stays a custom tag for recipe inputs.
    TENT = ['minersdelight:tentacles', 'butchery:tentacles', 'oceansdelight:tentacles']
    mat['tags'] = ['c:unified/tentacles' if t == 'c:tentacles' else t for t in mat['tags']]
    if 'c:tentacles' in mat['priority_overrides']:
        mat['priority_overrides']['c:unified/tentacles'] = mat['priority_overrides'].pop('c:tentacles')
    custom['c:unified/tentacles'] = TENT
    # Eternal Tales tags its one uranium item as both an ingot and a gem; there is nothing to unify it with.
    if 'eternal_tales:uranium' not in mat['ignored_items']:
        mat['ignored_items'].append('eternal_tales:uranium')
    # Culinary mods pull sliced cucumbers into c:crops/cucumber through nested tags; pin it to whole cucumbers.
    put('data/c/tags/item/crops/cucumber.json',
        {"replace": True, "values": ["culturaldelights:cucumber", "extradelight:cucumber", "youkaishomecoming:cucumber",
                                     {"id": "compatdelight:cucumber", "required": False}]}, 'whole cucumbers only')
    # recipes that fail with or without unification
    for path, why in (('data/recipes_plus/recipe/minecraft/material_consistency/hopper.json', 'uses nonexistent minecraft:trapped'),
                      ('data/create/recipe/haunting/compat/farmersdelight/rotten_tomato.json', 'fluid input Create haunting cannot take'),
                      ):
        put(path, DISABLED_JSON, why)
    put('data/minecraft/recipe/red_dye.json',
        {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"tag": "c:coffee_berry"}],
         "result": {"count": 1, "id": "minecraft:red_dye"}}, "Farmer's Respite coffee berry dye, old result key fixed")

    # ---------------------------------------------------------------- Compat Delight modules, substituted
    # The patched compatdelight jar registers these modules against installed mods (see patch_compatdelight.py);
    # here each module's recipes get stand-ins from mods we actually have. Recipes needing an item with no
    # stand-in stay off (their mod_loaded condition still fails), and so do ones whose output duplicates an
    # item another mod already makes (those items are merged in decisions.py instead).
    T = lambda t: {'tag': t}
    I = lambda i: {'item': i}
    CD = {
        'ecologicsdelight': ('ecologics', {
            'ecologics:coconut': I('crabbersdelight:coconut'), 'ecologics:coconut_slice': I('crabbersdelight:coconut_halve'),
            'ecologics:crab_meat': I('youkaishomecoming:crab_meat'), 'ecologics:prickly_pear': I('yungscavebiomes:prickly_peach'),
            'ecologics:cooked_prickly_pear': I('yungscavebiomes:prickly_peach'), 'ecologics:ice_bricks': I('youkaishomecoming:ice_bricks'),
            'ecologics:walnut': T('c:nuts')},
            # husk, logs, azalea flower, penguins; Crabber's claws do not stack, so no claw box; coconut milk is Crabber's
            ('coconut_husk/', 'coconut_knife', '_log.json', '_wood.json', 'azalea_flower', 'penguin', 'walnut_box',
             'walnut_from_walnut_box', 'coconut_milk.json', 'crab_claw_box', 'crab_claw_from_crab_claw_box')),
        'naturalistdelight': ('naturalist', {
            'naturalist:bass': I('aquaculture:smallmouth_bass'), 'naturalist:cooked_bass': I('aquaculture:fish_fillet_cooked'),
            'naturalist:catfish': I('alexsmobs:raw_catfish'), 'naturalist:cooked_catfish': I('alexsmobs:cooked_catfish'),
            'naturalist:venison': I('youkaishomecoming:raw_venison'), 'naturalist:cooked_venison': I('eternal_tales:cooked_venison'),},
            # no lizard meat in the pack since AnotherScape was removed (19 Sep): lizard recipes stay off
            ('duck', 'butterfly', 'lizard')),
        'tropicraftdelight': ('tropicraft', {
            'tropicraft:coconut_chunk': I('crabbersdelight:coconut_halve'), 'tropicraft:coffee_berry': T('c:coffee_berry'),
            'tropicraft:fresh_marlin': I('aquaculture:tuna'), 'tropicraft:seared_marlin': I('aquaculture:fish_fillet_cooked'),
            'tropicraft:cooked_frog_leg': I('crabbersdelight:cooked_frog_leg'),
            'tropicraft:grapefruit': T('c:crops/grapefruit'), 'tropicraft:lemon': T('c:crops/lemon'),
            'tropicraft:lime': T('c:crops/lime'), 'tropicraft:orange': T('c:crops/orange'),
            'tropicraft:pineapple': I('pineapple_delight:pineapple')},
            # papaya and starfish have no stand-in; the juices and coconut milk duplicate Extra Delight,
            # Pineapple Delight and Crabber's Delight items
            ('papaya', 'starfish', 'lemon_juice', 'lime_juice', 'grapefruit_juice', 'pineapple_juice', 'coconut_milk')),
        'hexereidelight': ('hexerei', {
            'hexerei:belladonna_berries': I('eternal_tales:belladonna'), 'hexerei:mandrake_root': I('youkaishomecoming:mandrake_root'),
            'hexerei:mandrake_flowers': I('youkaishomecoming:mandrake_flower')},
            ('moon_knife', 'root_and_leaves', 'salad', 'willow_cup', 'yellow_dock')),
        'netherificdelight': ('nourished_nether', {
            'nourished_nether:ectoplasm': I('eternal_tales:ectoplasm'),
            'nourished_nether:raw_hoglin': T('c:foods/raw_meat/hoglin'), 'nourished_nether:cooked_hoglin': T('c:foods/cooked_meat/hoglin')},
            ('necronium', 'nether_fungus', 'strange')),
        'ratsdelight': ('rats', {'rats:cheese': T('c:cheese')},
            # blue/nether cheese, contaminated food, rats, oratchalcum, potato pancakes have no stand-in;
            # cheese slices already come from Expanded Delight
            ('blue_cheese', 'nether_cheese', 'contaminated', 'rat_', 'oratchalcum', 'potato_pancake', 'cheese_slice')),
        # Apotheosis is installed, but 1.21 moved these items to Apothic Enchanting and replaced the old
        # common/rare materials with rarity materials
        'apotheosisdelight': ('apotheosis', {
            'apotheosis:warden_tendril': I('apothic_enchanting:warden_tendril'),
            'apotheosis:infused_breath': I('apothic_enchanting:infused_breath'),
            'apotheosis:prismatic_web': I('apothic_enchanting:prismatic_web'),
            'apotheosis:common_material': I('apotheosis:mysterious_scrap_metal'),
            'apotheosis:rare_material': I('apotheosis:luminous_crystal_shard')},
            ()),
    }
    STILL_MISSING = {'ecologics', 'naturalist', 'tropicraft', 'hexerei', 'nourished_nether', 'rats'}

    def cd_sub(o, sub):
        if isinstance(o, dict):
            if set(o) <= {'item', 'count'} and isinstance(o.get('item'), str) and o['item'] in sub:
                d = dict(sub[o['item']])
                if 'count' in o:
                    d['count'] = o['count']
                return d
            return {k: cd_sub(v, sub) for k, v in o.items()}
        if isinstance(o, list):
            return [cd_sub(x, sub) for x in o]
        if isinstance(o, str) and o in sub:
            v = sub[o]
            return v.get('item') or '#' + v['tag']
        return o

    cd_done = collections.Counter()
    cdz = zipfile.ZipFile(glob.glob(os.path.join(INST, 'mods', 'compatdelight-*.jar'))[0])
    for n in cdz.namelist():
        m = re.search(r'/recipe/([a-z]+delight)/(.+\.json)$', n)
        if not (m and m.group(1) in CD):
            continue
        folder, rel = m.group(1), m.group(2)
        target, sub, skip = CD[folder]
        if any(x in rel for x in skip):
            continue
        j = json.loads(cdz.read(n).decode('utf-8-sig'))
        j2 = cd_sub(j, sub)
        body = {k: v for k, v in j2.items() if k != 'neoforge:conditions'}
        if (target + ':') in json.dumps(body) and target in STILL_MISSING:
            continue                           # still needs an item we have no stand-in for
        if target in STILL_MISSING:
            conds = [c for c in j2.pop('neoforge:conditions', []) if target not in json.dumps(c)]
            if conds:
                j2 = {'neoforge:conditions': conds, **j2}
        if j2 == j:
            continue
        put(n, j2, 'Compat Delight %s recipe, substituted' % target)
        cd_done[target] += 1
    report['compatdelight_modules'] = dict(cd_done)

    tradepost_crops(jar_json, put)

    # AlmostUnified files are rewritten here because the head tags were added after the first dump.
    json.dump(tags_cfg, open(os.path.join(OUT, 'almostunified', 'tags.json'), 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    json.dump(mat, open(os.path.join(OUT, 'almostunified', 'unification', 'materials.json'), 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    json.dump(ph, open(os.path.join(OUT, 'almostunified', 'placeholders.json'), 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    print('drying ported %d | ecologics remapped %d | compatdelight ecologics %d' % (report['drying_ported'], len(report['ecologics_remapped']), sum(report['compatdelight_modules'].values())))
    print('heads: %d groups, %d Spectrum recipes retargeted | workstations: %d recipes ported'
          % (len(head_groups), head_recipes, len(ported)))


# ---------------------------------------------------------------- MC Trade Post crops (25 Sep)
# Crop-freedom only rewrote the minecolonies namespace, and AU's crop tags don't hold the MineColonies
# crops, so Trade Post's crafting-table foods still asked for colony crops. Same stand-ins as crop-freedom.
TP_CROPS = {
    'minecolonies:bell_pepper': 'rusticdelight:bell_pepper_red',
    'minecolonies:chickpea': 'ramadandelight:chickpea',
    'minecolonies:mint': 'extradelight:mint',
    'minecolonies:durum': 'minecraft:wheat',
}
TP_RECIPES = ('stir_fry', 'chickpea_stroganoff', 'mystic_tea', 'veggie_juice', 'wish_gathering_cow')
# stir fry also needs #mctradepost:seasoning, which only lists MineColonies onion/garlic/mint
TP_TAG_ADDS = {
    'seasoning': ['farmersdelight:onion', 'extradelight:garlic', 'extradelight:mint'],
    'protein': ['ramadandelight:chickpea', 'extradelight:soybeans', 'compatdelight:peas'],
    'stew_ingredients': ['farmersdelight:rice'],
}


def tradepost_crops(jar_json, put):
    def sub(o):
        if isinstance(o, dict):
            return {k: (TP_CROPS.get(v, v) if k == 'item' else sub(v)) for k, v in o.items()}
        if isinstance(o, list):
            return [sub(x) for x in o]
        return o
    for r in TP_RECIPES:
        path = 'data/mctradepost/recipe/%s.json' % r
        j = jar_json('mctradepost-*.jar', path)
        j2 = sub(j)
        if j2 != j:
            put(path, j2, 'Trade Post recipe, crop-freedom crops')
    for t, vals in TP_TAG_ADDS.items():
        put('data/mctradepost/tags/item/%s.json' % t,
            {'replace': False, 'values': [{'id': v, 'required': False} for v in vals]},
            'Trade Post tag, crop-freedom crops')
