# Working Maid Computers

An addon for [Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid) (NeoForge 1.21.1) that makes the
maid computer do something. Sit down at it, right-click it again, and the **AntOS** desktop opens: files, a terminal,
mail, paint, tasks, an archive, and a handful of small games.

It never touches TLM's classes. The computer block and the seat entity are recognised by registry name
(`touhou_little_maid:computer`, `touhou_little_maid:sit`), so TLM needs no changes and no hook.

## Using it

1. Right-click a TLM computer to sit down, as before.
2. Right-click it again with an empty hand. The desktop opens.
3. Create an Anternet account. Your files, mail and progress belong to the account, not to the block, so any
   computer you sit at shows the same desktop.
4. Sit at the computer holding a floppy disk and right-click to install the disk into your account.

## Games

| Game | Notes |
|---|---|
| Blockle | Word game from AntOS |
| Coin Miner | Click the coin, buy upgrades. Progress lasts until the client closes |
| Paddle Ball | Versus the computer, Easy / Medium / Hard. Arrow keys or W / S |
| Tic-Tac-Toe | Hard cannot be beaten |
| Minesweeper | Three sizes. Right-click or the FLAG button plants a flag; click a number to clear around it |
| Checkers | Mandatory captures, multi-jumps, kings. Easy / Medium / Hard |

All of them are single player.

## Layout

- `src/main/java/com/craisinlord/antos` is a vendored copy of [AntOS](https://github.com/craisinlord/AntOS)
  (`common-1.21.1`) with its computer block, block entity, item, renderer and phone removed. See `package-info.java`
  there for every difference from upstream.
- `src/main/java/com/github/eileen/workingmaidcomputers` is the addon itself: registration, the seat check
  (`MaidComputerSeat`), the right-click handler (`ComputerInteraction`) and the floppy installer.
- `content/minigame` (logic, unit-tested) and `content/client/game/mini` (rendering) hold the games under the AntOS
  package. Paddle Ball is ported from MrCrayfish's Furniture Mod: Refurbished (MIT, notice kept in `PaddleBallSim`).
- `src/gametest` holds game tests that run against the real TLM jar. They are not part of the shipped jar.

## Building and testing

```
./gradlew build                      # jar + unit tests
./gradlew runGameTestServer -PtlmJar=<path to a TLM neoforge 1.21.1 jar>
./gradlew runClient         -PtlmJar=<path to a TLM neoforge 1.21.1 jar>
```

`tlmJar` is only used to load TLM next to the addon in dev runs; it is never a compile dependency.

## Licence

AntOS has no published licence, so this repository is **for private use only** and must not be published or
redistributed. The addon's own code is by the repository owner; Paddle Ball's ported parts keep their MIT notice.
