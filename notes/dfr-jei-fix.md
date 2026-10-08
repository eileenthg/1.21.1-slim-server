---
name: dfr-jei-fix
description: "Distraction Free Recipes JEI 19.40+ fix — spliced jar installed & tested 25 Sep; upstream PR branch on user's fork, unbuilt/unpushed"
metadata:
  node_type: memory
  type: project
  originSessionId: 50e68780-7d5d-453f-8db6-854804e265fd
  modified: 2026-09-25T06:05:40.837Z
---

DFR 1.2.1's JEI mixins broke at JEI 19.40.0 (Jul 2026: overlay refactor). As of 2026-09-25:
- A spliced fix jar `distraction_free_recipes-neoforge-1.2.1-1.21.1-jeifix.jar` is installed in `1.21.1 slim` (original in modpack-audit/fixed-mods). In-game test passed except "click item in visible grid" (user accidentally closed the inventory).
- Work dir `C:\Users\eteil.000\Claude\dfr-jei-fix`; see HANDOFF.md there. Fork eileenthg/DistractionFreeRecipes, local branch `fix/jei-19.40-overlay` (commit dc63b75), NOT pushed, NOT built yet.
- Upstream maven.txni.dev is dead: Blahaj 1.0.9 is rebuilt from commit 60cee5e and TxniLib 1.0.21 comes from Modrinth into mavenLocal (local-maven.init.gradle). Use `chiseledBuild`; per-version build tasks produce empty jars.
- User wants a PR upstream plus a GitHub Release of the jar on their fork. Ask before pushing, opening the PR or releasing.

**Why:** the upstream author has been inactive since Feb 2025.
**How to apply:** resume from HANDOFF.md. In-game tests need the local instance (test-client/run-dfrtest.ps1 + dfr-tester). Related: [[main-instance-renamed-slim]].
