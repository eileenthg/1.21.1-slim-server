# Tamable Arcadian Fairy

An addon for **Arcadian Dream** on NeoForge 1.21.1 that lets you tame its fairies.
It follows the same design as **Tamable Fairy**, which does this for Touhou Little Maid,
but works on Arcadian Dream's fairy, ice fairy and sunflower fairy instead.

## How to tame

Right-click a wild fairy with a **Cake**. That is the whole interaction.
The cake is consumed unless you are in creative.

## What a tamed fairy gains

- **Keeps its own danmaku.** Arcadian Dream's attack goal is left untouched, so the fairy
  keeps its personality, bullet pattern, colour and cooldown. Only its choice of target changes.
- **Follows you.** It flies after you and teleports if it falls more than 24 blocks behind.
- **Settings screen.** Right-click your fairy with an empty hand.
  - Attack mode: Active hunts nearby hostiles, Passive only retaliates, Disabled never fights.
  - Follow toggle and follow range, from 2 to 32 blocks.
  - Wander toggle and wander range. Turning wandering on re-anchors the fairy where it stands.
- **Healing.** Right-click it with **Sugar** to restore 2 hearts.
- **Protection.** It stops despawning, survives peaceful difficulty, is ignored by hostile mobs,
  cannot hurt you or your other fairies, and cannot be hurt by you unless the
  `allowOwnerHurtArcadianFairy` game rule is turned on.
- **Leashable**, and it no longer stops you sleeping.
- **Death notice.** You are told in chat when one of your fairies dies, and what killed it.

## Touhou Little Maid

Optional. When that mod is installed, any mob killed by one of your tamed fairies drops
power points, mirroring what Tamable Fairy does for tamed maid fairies. The amount follows
Touhou Little Maid's own `MaidFairyPowerPoint` config value.

## Building

Arcadian Dream is not published to a Maven repository, so its jar lives in `libs/`.
Replace it when you update Arcadian Dream, then update `arcadiandream_version` in
`gradle.properties`.

```
./gradlew build
```

## Credits

- Maxmani, for Arcadian Dream.
- qing_gua, whose Tamable Fairy this mod is modelled on.
