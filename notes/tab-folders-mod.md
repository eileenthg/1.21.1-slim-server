---
name: tab-folders-mod
description: Tab Folders (tabfolders) client mod + zz-tabs datapack for Legendary Tabs; layout, test results, installed live 7 Oct
metadata:
  type: project
---

Built 5 Oct 2026 in `modpack-audit/tab-folders/` (mod) and `tab-folders/datapack/build.py` → `zz-tabs.zip`.
Client-only mod (`tabfolders`): mixin on Legendary Tabs `TabsMenu.initScreenButtons` swaps the enabled-tab list
for an arranged row (folders open in place behind a Back tab, single-tab folders inline, gear editor last) and widens the
row to the window edge (+ JEI exclusion areas). Editor copies Maid Task Curator's button design ([[maid-task-curator-mod]]);
layout in `config/tabfolders.json`. `Opener` handles reflection tabs: Statistics, MineColonies clipboard
(private static ItemClipboard.openWindow, client-side), TLM satchel tablet (curio key, else inventory use), Spectrum Bag of
Holding (swap-into-hand use; swap back after the container closes).

User's layout (5 Oct): bar = Inventory, Xaero map, Colony Clipboard, Satchel Tablet (both only when carried), folders
Backpack (Sophisticated backpacks group `sophisticatedtab_backpack_*`, Bag of Holding, Ender Pocket), Quests (Legacy, MCA
Quests, Mebahel journal+map, Standing, Factions), Info (JEI blue info icon: world tier, origin, chronicle, advancements,
stats). Hidden: guide library, curios, keybind atlas, roadweaver, apoli skill tree, sophisticatedtab settings. Never add
Amelet. Eternal Tales has no quest log (notice boards + advancements).

Gotchas: Legendary Tabs reports version `1.21.1-2.0` (use versionRange "*"); LT data tabs are datapack JSON
(data/<ns>/tabs, server-synced; plain-text tooltip works as untranslated key); Paginated Advancements replaces the vanilla
advancements screen. Tested in [[client-test-scaffold]]: 29/29 on 5 Oct. INSTALLED live 7 Oct (mods/ + root datapacks/zz-tabs.zip); in-game
use by the user still unverified; friend needs the tabfolders jar (tab definitions come from the host's datapack).
