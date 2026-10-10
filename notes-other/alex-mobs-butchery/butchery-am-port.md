---
name: butchery-am-port
description: The Butchery Alex's Mobs addon port targets NeoForge 1.21.1 (not Forge) and is private-use only
metadata:
  type: project
---

This project is a port of Jmods' "Butchery: Alex's Mobs Addon" from Forge 1.20.1 to 1.21.1,
started 2026-09-05.

Two constraints that are not visible from the code:

- **NeoForge, not Forge.** Butchery publishes no Forge build for 1.21.1 — 1.21.1 is NeoForge-only
  on its Modrinth page. The loader was forced, not chosen.
- **Alex's Mobs Continued** (`alexs-mobs-continued`) is the Alex's Mobs the user runs on 1.21.1,
  not the official mod (which stops at 1.20.1) and not the `Codx-org/Alexs-Mobs-Updated-Ported`
  repo's main branch (which targets MC 26.1.x). AMC keeps mod id `alexsmobs` and needs `codxlib`.

The original addon is **All Rights Reserved with no public source**. The user confirmed this port
is for their own game only and must not be redistributed.
