---
name: maidsoul-kitchen-yh-port-setup
description: "How the MaidsoulKitchen / Youkai's Homecoming compat work is set up — which trees, which jars, and how to get MaidsoulKitchen to compile."
metadata: 
  node_type: memory
  type: project
  originSessionId: d93927b3-04f1-4d22-88a6-76cbc2fd6ca8
  modified: 2026-09-03T17:33:06.443Z
---

Work started 2026-09-04. Fixing MaidsoulKitchen's Youkai's Homecoming tasks against
yinyuze's unofficial 1.21.1 port (`youkaishomecoming-4.2.14`, NeoForge), which has a
much-changed API versus the Modrinth 3.0.6 the mod targets.

The live instance is `C:\Users\eteil.000\AppData\Roaming\PrismLauncher\instances\1.21.1\minecraft`.
Its `logs/maidsoulkitchen_task_clazz_analysis.txt` is the fastest ground truth for what
broke — MaidsoulKitchen's own class analyzer lists every missing method/field per task.

The jar running there is `maidsoulkitchen-1.21.1-beta-v0.1.4`, released by "azumic" in
March 2026. **Its source is not public** — not on `Wall-ev/MaidsoulKitchen` (whose `1.21.1`
branch tip is v0.1.3) nor on any of its three forks. It differs from upstream: different
task UIDs (`youkaishomecoming_kettle_kettle` vs `yhc_tea_kettle`), a split
`maidsoulkitchen-compat.mixins.json`, and an extra `modclazzchecker` package. The user
said to drop that thread, so work targets the upstream `1.21.1` branch.

To make MaidsoulKitchen's `1.21.1` branch compile at all, in
`setting/version/1.21.1/gradle.properties` blank out `youkaishomecoming_version`,
`registrate_version`, `l2library_file_id` and `do_api_version` (do-api's pinned version is
gone from Modrinth and nothing in the source uses it), then drop the YH jar plus the l2
libraries from its `META-INF/jarjar/` into `libs/1.21.1/compileOnly/`. That compiles
against the exact bytecode the user runs. See [[yh-4x-fluid-api-shape]].
