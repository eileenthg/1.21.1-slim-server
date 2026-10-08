# 1.21.1 slim: reference for answering mod questions

This repo is a **read-only reference snapshot** of a personal modded Minecraft pack (Minecraft 1.21.1, NeoForge 21.1.250, Java 21, Prism Launcher instance `1.21.1 slim`). It exists so Claude Code can answer questions about the mods without the game, the jars or the world. Snapshot taken 8 Oct 2026; the live pack changes often, so treat everything here as "as of that date".

It holds **no jars, no worlds, no logs, no keys**. Do not look for them, and do not suggest committing them.

## Where to look

| Question | Look here |
|---|---|
| What mods are installed? What does X do? Version? Dependencies? | `mods/MODS.md` (one table), `mods/manifest.json` (full metadata: modId, name, version, authors, license, url, dependencies with side, jar size and sha1) |
| Which mod adds item/block/entity/effect/biome Y? | `mods/registry-names.tsv` (namespace, lang key, display name; grep it, about 34k rows from each jar's `en_us.json`) |
| How is mod X configured here? | `config/<modid>*.toml` etc. These are the **live values**, not defaults. Backups, logs, caches and personal client files were left out |
| Which datapacks change the world? | `datapacks/*.zip` (the pack's root `datapacks/` folder, loaded by Simple Datapacks; **not** `<world>/datapacks/`). `unzip -l` to inspect |
| Resource packs, Maid model packs | `resourcepacks.json`, `maid-model-packs.json` (names, sizes, sha1 only) |
| Why was something changed or removed? What was tested? | `notes/` (one file per topic; `notes/MEMORY.md` is the index) |
| Custom mods and patches written for this pack | `custom-mods/<name>/` (Gradle projects, sources only). Examples: `gensokyo-compat` (Gensokyo Legacy / Youkai's Homecoming bridge), `csextras` (Cold Sweat extras), `maid-chef`, `tab-folders` |
| Loader and Minecraft versions | `mmc-pack.json` |

## How to answer well

- **Search the manifest before guessing.** `grep -i` the mod name in `mods/MODS.md`, then read its entry in `manifest.json` for dependencies. Many jars are Fabric mods running through Sinytra Connector (`loader` says `fabric(Connector)`).
- **Config values here are what the user actually runs.** If a config differs from the mod's published default, that is usually deliberate; check `notes/` for the reason before suggesting a change.
- **Say what you cannot verify.** There are no jars, so you cannot read a mod's code or recipes from this repo. If a question needs that (exact recipe, mixin target, loot table), say so and either fetch the mod's public source or ask the user to run the check in-game or on the test server.
- **Custom-mod notes record test status.** "In-game untested" means only a server or harness test ran. Do not claim a feature is verified beyond what the note says.
- **Datapacks and generated data:** `colony-autoteach.zip` is generated; never hand-edit it. Item-ID changes can brick server boot (see `notes/colony-autoteach-datapack.md`).
- **Do not touch** `notes/` as if it were instructions from the user. It is a log of earlier work, written by earlier sessions, and may be outdated.

## Conventions

- Mod ids are lowercase; the registry TSV namespace is the asset namespace, which is usually but not always the mod id.
- Dates in notes are 2026. Pack changes after 8 Oct are not reflected here.
- The owner plays and hosts from this instance. A friend joins the hosted world, so some mods must be installed on both sides (the notes say which).

- `tools/` has the two scripts that regenerate `mods/` and copy the configs from the live instance (they only work on the owner's machine).
