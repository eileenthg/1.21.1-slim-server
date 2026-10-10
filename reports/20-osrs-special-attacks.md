# 20: OSRS special attacks for AnotherScape (18 Sep)

New mod **OSRS Special Attacks** (`godswordspecs-1.21.1-1.1.0.jar`, source `C:\Users\eteil.000\Claude\godsword-specs`). Needed on the server **and every client** (key + HUD). Config: `config/godswordspecs-common.toml` (every cost and number, OSRS defaults).

## How it plays
- Special energy 0–100, regains 20 per minute (OSRS: 10% per 30 s), full again after death.
- **V** (rebindable, "Special Attacks" category) readies the special of the weapon in your main hand, like the OSRS spec orb; your next hit with it fires. Press again or switch item to cancel, no cost. Specials marked *instant* fire on the key.
- Bar left of the hotbar while holding a weapon with a special: green = enough energy, gold = readied, white tick = cost. Soulreaper axe and sunlight spear show their stacks instead.
- Which items have which special: item tags `godswordspecs:special/<name>`, so a datapack can add weapons. `#godswordspecs:demons` (OSRS demon list) and `#godswordspecs:freeze_immune` (bosses) are tags too.
- OSRS accuracy bonuses are dropped (Minecraft melee never misses), except where a special would otherwise do nothing (Behead, Eviscerate).

## The specials (OSRS Wiki numbers; ✓ = measured in the scripted test)

| Weapon | Special (cost) | Here | Test |
|---|---|---|---|
| Armadyl godsword | The Judgement (50) | ×1.375 | ✓ 7.00 → 9.63 |
| Bandos godsword | Warstrike (50) | ×1.21; drains armor, then toughness, then attack by the damage dealt | ✓ armor 10 → 1.53 on 8.47 dealt |
| Saradomin godsword | Healing Blade (50) | ×1.1; heal 50% (min 10), hunger 25% (min 5) in place of prayer | ✓ +10 HP, +5 food |
| Zamorak godsword | Ice Cleave (50) | ×1.1; frozen 19.2 s; bosses immune | ✓ frozen; Graardor not |
| Ancient godsword | Blood Sacrifice (50) | ×1.1; 4.8 s later 25 damage + heal if within 5 blocks | ✓ +25 exactly, healed |
| Dragon dagger | Puncture (25) | two hits, each ×1.15 | ✓ 2 × 4.63 |
| Dragon scimitar | Sever (55) | OSRS: no protection prayers 5 s. Here: target can't shield-block 5 s | ran; not measured |
| Dragon sword | Wild Stab (40) | ×1.25, goes through shield blocks (OSRS: through Protect from Melee) | ✓ ×1.25 |
| Dragon mace | Shatter (25) | ×1.5 | ✓ |
| Dragon warhammer | Smash (50) | armor −30% (compounds) | ✓ 10 → 7 |
| Elder maul | Pulverize (50) | armor −35% | ✓ 10 → 6.5 |
| Abyssal bludgeon | Penance (50) | +2.5% per missing hunger point (0.5% per prayer point, 99 → 20) | ✓ ×1.25 at 10 missing |
| Osmumten's fang | Eviscerate (25) | "true max hit": full damage even on an uncharged swing | ✓ 0 → 6.51 |
| Voidwaker | Disrupt (50) | replaces the hit with magic damage 50–150% of max hit, ignores armor | ✓ 13.4 vs 7.6 plain |
| Soulreaper axe | Behead (stacks) | passive +6%/stack (max 5, one lost per 30 s idle); Behead spends all: +6% and +12% (its accuracy bonus) per stack | ✓ bigger hit after 3 stacks |
| Emberlight | Weaken (50) | armor and attack −5%, ×3 vs demons | ✓ 9.5 / 5.7 |
| Noxious halberd | Virulence (50, instant) | cures your Poison; the poison damage left is added to the next halberd hit | ✓ cured, bonus hit |
| Thunder khopesh | Lingering Lightning (50) | two hits, then a lightning bolt on 3×3 for up to 100% max hit | ✓ 2 × 7 + 7.75 bolt |
| Sunlight spear | Sol Slam (7 stacks, instant) | +1 stack per attack (max 20); hits every hostile mob within 3 blocks, ignores armor | ✓ 15 to both dummies |
| Crimson kisten | Brutal Swing (50) | 130–170% of max hit (all four accuracy rolls land) | ✓ ×1.45 |
| Dinh's bulwark | Shield Bash (50) | target hit twice, up to 9 other hostiles within 5 blocks once, attack −5% | ✓ mechanics; **see note** |
| Dragon axe, 3rd age axe | Lumber Up (100, instant) | Haste 3 min (OSRS +3 Woodcutting) | ✓ |
| Dragon pickaxe, 3rd age pickaxe | Rock Knocker (100, instant) | Haste 3 min (OSRS +3 Mining) | ✓ |
| Magic shortbow | Snapshot (55) | next shot fires a second arrow 6 ticks later (the extra arrow can't be picked up) | ✓ two arrows, −55 |
| Scorching bow | Scorching Shackles (25) | demons only: bound 12 s + burn 1 every 2.4 s; non-demons: message, energy kept | ✓ both cases |
| Blazing blowpipe | Toxic Siphon (50) | ×1.5, heals 50% of damage | not tested (blowpipe firing not scriptable) |
| Eye of ayak | Soul Rend (50) | ×1.3 (its Magic Defence drain has no counterpart) | not tested |
| Purging staff | Scatter Ashes (25) | ×1.5 vs demons; energy refunded if it kills the demon | not tested |

**No special, on purpose:**
- Armadyl and zaryte crossbows (OSRS: accuracy and enchanted-bolt procs only; neither exists here).
- Weapons without an OSRS special: twisted bow, bowfa, scythes, DHCB, DHL, inquisitor's mace, staves, tridents, rune/iron/etc.
- Drygore blowpipe (RS3, not OSRS) and Dinh's blazing bulwark (no special on the Wiki).

**Shield Bash note:** AnotherScape makes the bulwark a plain shield with no attack damage, so the bash hits for fist damage (about 1). The mechanics are right; the numbers are weak. In OSRS the bulwark is a two-handed weapon with a big Strength bonus. Giving it main-hand attack damage would fix it.

## Bugs found while testing
- Shield Bash recursed (its area hits re-fired the special) → StackOverflow. Fixed: the special is cleared before effects run.
- The first HUD spot collided with the performance overlay; moved left of the hotbar.

## Crossbows (item 10 from report 19)
Root cause: AnotherScape puts `armadyl_bow` and `zaryte_bow` (and both blowpipes) in `#minecraft:swords`. **Sword Blocking Mechanics** turns right-click into a block for anything in that tag, so the crossbow never started loading. As the user asked, Sword Blocking Mechanics is **removed** (jar in `removed-mods/`). The interim tag datapack was taken out again (`datapacks-retired/anotherscape-ranged-fix`). Test after removal: armadyl, zaryte, dragon hunter and vanilla crossbows all load (`charged=true`). Puzzles Lib stays (Mutant Monsters, Respawning Animals, Fast Item Frames).

## Test harness
`test-client/spec-tester` (client test mod; `-Dspectest.resume=true` runs the short list). Logs: `test-client/spec-run2-latest.log` (full run with the recursion bug), `spec-run3` = resumed run (in `latest` at the time), `spec-run5-latest.log` (lightning + scorching). One launch hung after startup with nothing in the log; a relaunch was fine.
