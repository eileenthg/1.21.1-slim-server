# Pale oak leaves + performance

## Pale oak leaves — I was wrong, here's the real cause

My resource pack doesn't work and can't work. I checked: it **is** enabled (`file/PaleOakLeafColor.zip` is in `options.txt`), so the pack isn't the problem — my diagnosis was.

Decompiling `LeafColors.getClientLeafTintColor` shows what `leaf_colors` actually feeds:

```java
if (LeafColorReloadListener.hasCustomColor(block)) return LeafColorReloadListener.getCustomColor(block);
return Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
```

It's called from the **falling-leaf particle** providers, not from block rendering. So `leaf_colors` tints *particles* and otherwise defers to `BlockColors`. Supplying a `pale_oak_leaves.json` was never going to change the block.

VanillaBackport registers only a **render type** for `PALE_OAK_LEAVES` (`BlockRendererEvent.register(RenderType, Block[])`), never a colour handler — which is correct, since vanilla 1.21.4 pale oak leaves are untinted.

**The culprit is Colorful Seasons.** Its `BlockColorHandlers.registerAdditionalBlockColors` walks `BuiltInRegistries.BLOCK`, keeps everything matching `BlockState.is(BlockTags.LEAVES)`, and excludes only a hardcoded list of ten vanilla leaves:

```
OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, DARK_OAK, MANGROVE, CHERRY, AZALEA, FLOWERING_AZALEA
```

`pale_oak_leaves` isn't on that list but *is* in `BlockTags.LEAVES`, so it gets a seasonal foliage tint. That's the green, and why it shifts with the season.

### Options

1. **Turn off "Affect Modded Blocks"** in Colorful Seasons' in-game config (Mods → Colorful Seasons → Config). The field is `SeasonalColorConfigValues.affectModdedBlocks`, and it gates exactly the `registerAdditionalBlockColors` path. There is no config *file* — it's a GUI/`ConfigManager` setting, which is why `config/` has no entry for it. **Cost:** every other modded leaf loses seasonal colour too (Quark azalea/blossom/ancient, Enderscape, Eternal Tales, Deeper and Darker…). There's no per-block exclusion.
2. **Live with it** until the author adds an exclusion list. Worth an issue report — pale oak is meant to be untinted and this will hit every 1.21.4-backport pack.
3. Removing `pale_oak_leaves` from `BlockTags.LEAVES` would work but breaks decay, shears, composting and `#minecraft:leaves` recipes. Don't.

**Keep or bin my resource pack?** It has one real effect left: pale oak *falling-leaf particles* will tint white instead of green, via the `hasCustomColor` branch above. Harmless either way.

---

## Memory and smoothness

Your heap line from the last run:

```
Memory: 6427 MiB / 8192 MiB up to 16384 MiB
```

So 8 GB is **committed at startup** and 6.4 GB in use. That "using a LOT of memory" impression is mostly `-Xms8192m` plus `-XX:+AlwaysPreTouch`, which forces the JVM to touch every page of the initial heap on boot. It's not 8 GB of mod data — it's a reservation.

### 1. Your JVM flags have real problems

Current args (deduplicated for readability) contain several things that actively hurt on a 6-core/12-thread i7-10750H:

| Flag | Problem |
|---|---|
| `-XX:ParallelGCThreads=12` | Gives GC **all 12 hardware threads**. GC pauses then contend with the render and server threads — a direct cause of stutter. Should be ~4–6. |
| `-XX:G1HeapRegionSize=64M` | G1 wants roughly 2048 regions. At 16 GB heap, 64 MB regions gives **256** — coarse regions mean more wasted space and worse pause behaviour. Drop it (let G1 choose) or use 16M. |
| `-XX:NewRatio=3` | Meaningless-to-harmful under G1, which sizes the young gen adaptively to hit the pause target. |
| `-XX:+AlwaysPreTouch` | Commits and touches the whole 8 GB Xms at startup. Slower boot, inflated memory reading. |
| `-XX:+UseLargePages` | On Windows needs the "Lock pages in memory" privilege. Without it this silently does nothing. |
| `-XX:+UseNUMA` | Single-socket laptop. No effect. |
| `-XX:MetaspaceSize=2048m` | Commits 2 GB of metaspace immediately. `MaxMetaspaceSize=2048m` alone is enough. |
| `-XX:+DisableExplicitGC` + `-XX:+ExplicitGCInvokesConcurrent` | Contradictory — the second only matters if explicit GC is allowed. |
| `-Dfile.encoding=GBK` … `-Dfile.encoding=UTF-8` | Duplicated and conflicting. Also `MaxGCPauseMillis` and `sun.rmi.dgc.server.gcInterval` appear twice each. |

**A bigger heap is not better here.** 16 GB max means G1 has more to scan and longer worst-case pauses. For a 380-mod 1.21.1 client, 8–10 GB is the usual sweet spot.

Suggested replacement (Prism → Settings → Java, or per-instance):

```
-Xms8192m -Xmx10240m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=50
-XX:G1HeapRegionSize=16M
-XX:ParallelGCThreads=6
-XX:ConcGCThreads=3
-XX:+UseStringDeduplication
-XX:MaxMetaspaceSize=2048m
-XX:+HeapDumpOnOutOfMemoryError
-XX:-OmitStackTraceInFastThrow
-Xss4M
-Dfile.encoding=UTF-8 -Duser.language=en -Duser.region=US
-Dfml.ignorePatchDiscrepancies=true
-Dfml.ignoreInvalidMinecraftCertificates=true
-Dfml.readTimeout=90
```

Change **one thing at a time** so you can tell what helped. `ParallelGCThreads` and `G1HeapRegionSize` are the two most likely to produce a felt improvement.

### 2. Two free memory wins in configs you already have

**FerriteCore** — two options are off that shouldn't be, in `config/ferritecore-mixin.toml`:

```toml
compactFastMap = true            # was false
useSmallThreadingDetector = true # was false
```

Leave `populateNeighborTable = false`; it's the correct pairing with `replaceNeighborLookup = true`.

**ModernFix dynamic resources** — your `config/modernfix-mixins.properties` is **empty**, so this is off. The jar ships 32 `perf/dynamic_resources` mixin classes. Enabling it makes block/item models load lazily instead of all at once, and in packs this size it commonly saves **1–2 GB**:

```properties
mixin.perf.dynamic_resources=true
```

Caveat, and it's a real one: dynamic resources is the most aggressive thing ModernFix does. It occasionally causes missing or wrong models with mods that bake models unusually — and you run several that do (Domum Ornamentum, Sophisticated Storage, Create). Try it, and if you see untextured blocks, turn it back off. This is the single biggest lever available.

### 3. Where the memory actually goes

I measured every jar. Blockstate variants are the dominant driver — each unique `BlockState` is a live object, and model/texture counts follow from them.

| Mod | Blockstate variants | Models | Textures | Jar |
|---|---:|---:|---:|---:|
| **Youkai's Homecoming** | **27,060** | 5,104 | 1,471 | 9.8 MB |
| **Eternal Tales** | **19,736** | 11,588 | 8,482 | 138 MB |
| **Shower Core** | **9,774** | 591 | 58 | 0.8 MB |
| Cluttered | 8,471 | 3,072 | 1,128 | 5.3 MB |
| Create | 8,231 | 2,684 | 1,295 | 18.7 MB |
| Extra Delight | 7,536 | 3,759 | 1,941 | 10.7 MB |
| Spectrum | 6,580 | 4,428 | 3,037 | 89 MB |
| Quark | 5,646 | 2,541 | 1,263 | 15.2 MB |
| Butchery | 5,441 | 3,546 | 880 | 13.6 MB |
| Supplementaries | 3,887 | 1,794 | 1,196 | 13.2 MB |
| **all 384 jars** | **130,327** | 57,758 | 48,094 | 1.77 GB |

**The top 10 mods are 78% of all blockstate variants.**

Three observations:

- **Youkai's Homecoming is your single heaviest mod** at 21% of every blockstate in the pack — more than Eternal Tales, in a jar 14× smaller. That's the vertical slabs, sikkui and concrete variants we discussed. Since Quark's vertical slabs cover 122 blocks against Youkai's 68 with only 36 shared, dropping Youkai's would be the largest single memory saving available that doesn't touch your RPG content.
- **Shower Core is wildly disproportionate**: 9,774 variants from a 808 KB jar, 71 variants per blockstate. That's a lot of permutations for a bathing mod. Worth checking whether you use it enough to justify being the 3rd heaviest blockstate contributor.
- **Eternal Tales** is the biggest on disk (138 MB, 11,588 models, 8,482 textures) and drives texture-atlas and metaspace pressure more than blockstate count. You've said the RPG is the point of the pack, so I'm not proposing dropping it — just noting it's the reason your atlas and metaspace are large.

### 4. Mods worth adding

You already run FerriteCore, ModernFix, Sodium 0.8.13, Immersive Optimization, ServerCore, Cupboard and MemGuard — a good baseline. Gaps:

- **Clumps** — merges XP orbs. With MineColonies and mob farms, XP orb entities are a common tick sink.
- **Entity Culling** — skips rendering entities behind geometry. Usually a solid FPS gain in dense colonies.
- **Noisium** — faster worldgen; helps the chunk-load stutter you'd see exploring with this many structure mods.

Add them one at a time, and mind the earlier lesson: check each one's dependencies before enabling, since a missing library is what produced the `compatartifacts` class of failure.
