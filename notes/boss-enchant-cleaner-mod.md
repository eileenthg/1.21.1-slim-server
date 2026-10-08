---
name: boss-enchant-cleaner-mod
description: "bossenchantcleaner mod strips D&T boss-only marker enchants (nova_structures:boss_behaviour/shulker_boss/shulker_miniboss) from player items + villager offers; installed 26 Sep, untested"
metadata:
  node_type: memory
  type: project
  originSessionId: da970794-0684-40ee-9443-9d63baee96e8
  modified: 2026-09-26T10:56:21.380Z
---

Source: `modpack-audit/boss-enchant-cleaner` (cloned from [[grave-tag-cleaner-mod]] build setup). Installed in `1.21.1 slim` mods 2026-09-26. It compiles, but it has not had a server boot or in-game test ([[no-test-servers-while-hosting]]).

The problem: a friend got an enchanted book with `nova_structures:boss_behaviour` + `shulker_boss` from an NPC ("Enchanter"/cleric-like) in a generated structure. These are Dungeons & Taverns' mob-gear markers and are in no enchantment tag. So the source rolls from the whole registry, probably `enchant_randomly` with no options. I have not identified the source. It is NOT Eternal Tales (that mod never makes books), NOT the MineColonies Enchanter (vanilla-only loot lists), and NOT Quark Matrix (undiscoverable/treasure enchants off).

The fix: the mod cleans villager offers on interact (AbstractVillager). It sweeps player inventories every 20 ticks and cleans container slots on close. Book entries become a random level-1 `#in_enchanting_table` enchantment, and gear entries are just removed. Boss mobs are untouched.

**How to apply:** if bad books keep appearing, ask which mod the NPC is from. If it isn't an AbstractVillager, only the post-purchase sweep catches it.
