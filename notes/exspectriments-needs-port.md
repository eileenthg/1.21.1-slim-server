---
name: exspectriments-needs-port
description: Exspectriments was rejected on 1 Oct 2026. It crashes under Connector against NeoForge Spectrum 1.12.5, and the user decided it wasn't worth porting
metadata:
  type: project
---

On 1 Oct 2026, Exspectriments 0.3.0 (a Fabric Spectrum add-on) crashed at Connector's `main` entrypoint with `NoClassDefFoundError: de/dafuqs/spectrum/api/energy/InkStorage`. NeoForge Spectrum moved `api.energy` to `api.ink`.

Most of its features would be new to the pack:
- lab coats
- Printer (custom item dyeing)
- Pigment Extractor
- Hostile Approximator (mob drops without mobs)
- liquid gems
- Cloaking effect

After seeing the comparison, the user said to ditch it ("we won't miss it"). The jar and the download were sent to the Recycle Bin. Nothing is left in the pack.

**Why:** Port cost: 11 moved classes, a Fabric Transfer API mixin and an EMI-only recipe category. That was too much for features the user didn't value.

**How to apply:** don't suggest Exspectriments again unless the user brings it up. The same check applies to any Fabric Spectrum add-on: it will probably hit the same `api.energy` → `api.ink` break.
