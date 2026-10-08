"""Builds zz-tabs.zip: extra Legendary Tabs tabs for this pack (data/zztabs/tabs/*.json).
Every screen class and keybind name below was read from the mod jars. Tabs only open their screen;
show_tabs_on_screen is off, so the tab row doesn't sit on top of screens with their own layout
(MCA Quests draws its own Log/Journal tabs exactly where the row would go).
Tooltips are plain text: Legendary Tabs shows an untranslated key as-is."""
import json, os, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'zz-tabs.zip')
tabs = {}


def tab(tid, tooltip, icon, action, mods, target=None, conditions=None):
    t = {'id': tid, 'enabled': True,
         'icon': {'type': 'item', 'item': icon} if ':' in icon and not icon.endswith('.png') else
                 {'type': 'texture', 'texture': icon, 'u': 0, 'v': 0},
         'screen_open_action': action, 'tooltip': tooltip, 'required_mods': mods,
         'show_tabs_on_screen': False}
    if target:
        t['target_screen_class'] = target
    if conditions:
        t['enabled_conditions'] = conditions
    tabs[tid] = t


def key(binding, close=True):
    return {'type': 'key_press', 'key_binding': binding, 'close_screen_first': close}


def screen(cls):
    return {'type': 'open_screen', 'screen_class': cls}


def in_curio(*items):
    conds = [{'type': 'item_in_curio', 'item': i} for i in items]
    return [conds[0] if len(conds) == 1 else {'type': 'or', 'conditions': conds}]


def opener(method):
    return {'type': 'reflection', 'class_name': 'net.eileenthg.tabfolders.Opener', 'method_name': method, 'static': True}


# --- calendar / quests ---
tab('chronicle', 'Chronicle (calendar)', 'minecraft:clock',
    screen('org.z2six.rpgtimeline.client.gui.ChronicleScreen'), ['rpgtimeline'],
    'org.z2six.rpgtimeline.client.gui.ChronicleScreen')
tab('mca_quests', 'MCA Quests', 'minecraft:book',
    screen('dev.otectus.mcaquests.client.QuestLogScreen'), ['mcaquests'],
    'dev.otectus.mcaquests.client.QuestLogScreen')
tab('mca_reputation', 'Standing', 'minecraft:emerald',
    key('key.mcareputation.open'), ['mcareputation'], 'dev.otectus.mcareputation.client.ReputationScreen')
tab('townstead_factions', 'Factions', 'minecraft:white_banner',
    screen('com.drultralux.townsteadfactions.client.screen.FactionScreen'), ['townsteadfactions'],
    'com.drultralux.townsteadfactions.client.screen.FactionScreen')
tab('mebahel_journal', 'Quest Journal', 'minecraft:knowledge_book',
    key('key.mebahelrpgrquest.open_journal'), ['mebahelrpgrquest'], 'net.mebahelrpgrquest.client.gui.QuestJournalScreen')
tab('mebahel_map', 'Quest Map', 'minecraft:filled_map',
    key('key.mebahelrpgrquest.open_quest_map'), ['mebahelrpgrquest'], 'net.mebahelrpgrquest.client.gui.QuestMapScreen')

# --- character ---
tab('curios', 'Curios', 'minecraft:amethyst_shard',
    key('key.curios.open.desc'), ['curios'], 'top.theillusivec4.curios.client.gui.CuriosScreen')
tab('origin', 'Origin', 'origins:orb_of_origin',
    key('key.origins.view_origin'), ['origins'], 'dev.overgrown.origins.client.screen.ViewOriginScreen')
tab('statistics', 'Statistics', 'minecraft:paper',
    opener('statistics'),
    ['tabfolders'], 'net.minecraft.client.gui.screens.achievement.StatsScreen')
tab('world_tier', 'World Tier', 'minecraft:nether_star',
    key('key.apotheosis.open_world_tier_select'), ['apotheosis'], 'dev.shadowsoffire.apotheosis.client.WorldTierSelectScreen')

# --- carried items: shown only while the item is in a curio slot or the inventory ---
def carried(item):
    return [{'type': 'or', 'conditions': [{'type': 'item_in_curio', 'item': item}, {'type': 'item_in_inventory', 'item': item}]}]


tab('colony_clipboard', 'Colony Clipboard', 'minecolonies:clipboard',
    opener('colonyClipboard'), ['minecolonies', 'tabfolders'], conditions=carried('minecolonies:clipboard'))
tab('maid_tablet', 'Satchel Tablet', 'tlm_maid_manager:maid_manager',
    opener('maidTablet'), ['tlm_maid_manager', 'tabfolders'], conditions=carried('tlm_maid_manager:maid_manager'))
tab('bag_of_holding', 'Bag of Holding (Ender Chest)', 'spectrum:bag_of_holding',
    opener('bagOfHolding'), ['spectrum', 'tabfolders'], conditions=carried('spectrum:bag_of_holding'))
tab('ender_pocket', 'Ender Pocket', 'touhou_little_maid_spell:ender_pocket',
    key('key.touhou_little_maid_spell.open_ender_pocket'), ['touhou_little_maid_spell'],
    'com.github.yimeng261.maidspell.client.gui.EnderPocketScreen')

# --- tools ---
tab('guide_library', 'Guide Library', 'minecraft:lectern',
    key('key.guidelibrary.open'), ['guidelibrary'], 'net.eileenthg.guidelibrary.client.LibraryScreen')
tab('keybind_atlas', 'Keybind Atlas', 'minecraft:tripwire_hook',
    screen('com.example.keybindatlas.render.KeyboardScreen'), ['keybindatlas'],
    'com.example.keybindatlas.render.KeyboardScreen')
tab('roadweaver_map', 'RoadWeaver Map', 'minecraft:map',
    key('key.roadweaver.open_map'), ['roadweaver'])
tab('apoli_skill_tree', 'Skill Tree', 'minecraft:experience_bottle',
    key('key.apoli.skill_tree'), ['apoli'], 'dev.overgrown.apoli.client.skill.SkillTreeScreen')

with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
    z.writestr('pack.mcmeta', json.dumps({'pack': {'pack_format': 48,
               'description': 'Extra Legendary Tabs tabs: quests, calendar, character, maids, tools'}}, indent=2))
    for tid, t in tabs.items():
        z.writestr(f'data/zztabs/tabs/{tid}.json', json.dumps(t, indent=2) + '\n')
print(len(tabs), 'tabs ->', OUT)
