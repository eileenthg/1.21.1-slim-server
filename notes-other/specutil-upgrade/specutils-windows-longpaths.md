---
name: specutils-windows-longpaths
description: The specutils repo cannot be checked out on Windows without core.longpaths=true.
metadata:
  type: project
---

`eehunter/Spectral-Utilities` has asset paths (e.g.
`src/main/resources/assets/specutils/models/block/noxwood_deco/chestnut_ebony_noxwood_lantern_hanging_diagonal_tall.json`)
that exceed Windows' 260-char MAX_PATH. A plain `git clone` reports
"Clone succeeded, but checkout failed / Filename too long" and leaves a
half-populated worktree.

**Why:** MAX_PATH applies unless git uses the `\?\` prefix, which it only does
with `core.longpaths` enabled.

**How to apply:** Set `core.longpaths true` on the repo before checkout, and keep
the clone at a short base path. Cloning into the deep session scratchpad path
fails even with longpaths headroom; `C:\Users\eteil.000\Claude\specutil-upgrade`
works. The `reference/spectrum` clone (DaFuqs/Spectrum) has shorter paths and
checks out fine either way.

Related: [[specutils-source-repo]]
