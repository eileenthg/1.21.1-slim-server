# Handoff: DFR JEI 19.40+ fix

Repo: eileenthg/DistractionFreeRecipes (fork of txnimc/DistractionFreeRecipes), branch `fix/jei-19.40-overlay`,
commit dc63b75 "Fix JEI integration for JEI 19.40+ on 1.21.1".

## Status
- Source ported: new JEI mixins for 1.21.1 under `#if AFTER_21_1` (Manifold preprocessor), old code kept under
  `#else` for 1.20.1. MixinPlugin skips JEI mixins on JEI < 19.40. JEI compile dep bumped to 19.57.0.447.
- The same fix, spliced into the released 1.2.1 jar, was tested in-game on NeoForge 21.1.250 + JEI 19.57.0.447:
  hiding, search, Show/Hide buttons and blocked clicks on the hidden grid all passed. Clicking an item in the
  visible grid was not verified (test interrupted).
- NOT DONE: a successful build of the branch. Next step: `./gradlew chiseledBuild` (builds all 4 targets:
  1.20.1 forge/fabric, 1.21.1 neoforge/fabric). A per-version task like `:1.21.1-neoforge:build` compiles
  nothing unless that version is Stonecutter's active one, and produces an empty jar.

## Build prerequisites (upstream maven.txni.dev is offline)
The build needs two artifacts that only lived on that maven. Install them into mavenLocal, and don't commit
any of this to the PR:
1. Blahaj Gradle plugin 1.0.9, which was never committed. Use the nearest commit:
   `git clone https://github.com/txnimc/Blahaj && cd Blahaj && git checkout 60cee5e`, set
   `version = "1.0.9"` in build.gradle.kts, then `./gradlew publishToMavenLocal`.
2. TxniLib 1.0.21 from Modrinth (project `txnilib`, versions `<loader>-<mc>-1.0.21`), installed as
   `toni.txnilib:<loader>-<mc>:1.0.21` (e.g. `toni.txnilib:neoforge-1.21.1:1.0.21`) with a minimal pom.
   Needed for neoforge-1.21.1, fabric-1.21.1, forge-1.20.1 and fabric-1.20.1.
3. Run Gradle with an init script that adds `mavenLocal()` to pluginManagement and all project repositories.
Java 21. Gradle 8.8 (wrapper).

## Remaining
1. chiseledBuild passes for all 4 targets.
2. Re-test the built 1.21.1-neoforge jar in-game. This needs the local Minecraft instance, so do it on the
   user's PC, not in the cloud.
3. Push the branch, then open the PR to txnimc/DistractionFreeRecipes (show the user the PR text first).
4. Create a GitHub Release on the fork with the built jars. It's MIT, so keep the LICENSE notice and label it
   an unofficial fix pending upstream. Ask the user before publishing.
