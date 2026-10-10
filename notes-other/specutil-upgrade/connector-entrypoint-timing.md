---
name: connector-entrypoint-timing
description: Under Connector, Fabric entrypoints run BEFORE NeoForge mods are constructed - so NeoForge Deferred holders are unbound and mod-bus events have not fired.
metadata:
  type: project
---

Verified by disassembling NeoForge 21.1.249's patched `Minecraft`:

- `Minecraft.<init>` offset **542**: `Thread.currentThread()` - where Connector's
  `MinecraftMixin` injects `ConnectorLoader.load()`, which runs Fabric `main`/`client`
  entrypoints.
- `Minecraft.<init>` offset **939**: `ClientModLoader.begin(...)` -> `CommonModLoader.begin`
  -> `ModLoader.gatherAndInitializeMods(...)`, where NeoForge mods are constructed and
  registry/capability events fire.

Both offsets are in the same constructor (no intervening method declaration).

**Why it matters:**
1. NeoForge `DeferredBlock`/`DeferredItem` holders from other mods are **unbound** during a
   Fabric entrypoint. Calling `.get()` there throws. Avoid the dependency rather than timing
   it - `by lazy` does not help if the first access is in the same entrypoint.
2. `RegisterCapabilitiesEvent` (and any `IModBusEvent`) has **not yet fired**, but no mod
   container exists yet either, so a Fabric mod cannot attach a mod-bus listener. Mixin into
   the target mod's own registration method instead.
3. Game-bus events (`NeoForge.EVENT_BUS`, e.g. Fractal's `CreativeSubTabEvent`, which does not
   implement `IModBusEvent`) are fine - it is a static singleton.

Connector calls `NeoForgeRegistriesSetup.modifyRegistries(null)` before entrypoints to prepare
registries; do not read that as "NeoForge already registered" - it means the opposite.

Related: [[specutils-gradle-wrapper-broken]]
