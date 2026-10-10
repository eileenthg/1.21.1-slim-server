# 移植计划：擦弹/灵力/Bomb 系统 + 自定义符卡编辑器

## 背景结论（已验证）
- GL 通过 **预编译 jar**（`libs/danmaku_api-3.0.5+3.jar`）消费 DanmakuAPI，源码即 `DanmakuAPI-main-fix`（同版本 3.0.5+3）。
- DanmakuAPI 的 `dev.xkmc.danmakuapi.api.GrazeHelper` 是**空壳**：`graze()` / `forbidDanmaku()` / `shouldPlayerHurt()` / `getHitBoxShrink()` 全部返回默认值，被弹幕实体/物品**直接静态调用**，无任何 hook/事件。
- GL 已注册全部擦弹相关属性（`MAX_POWER`/`INITIAL_RESOURCE`/`GRAZE_EFFECTIVENESS`/`HITBOX` 等，见 GLAttributes），说明本就预留了移植位。
- GL 的 `YoukaiEntity implements IYoukaiEntity`，但弹幕是**真实世界实体**（`level().addFreshEntity`），不像 1.20.1 的虚拟跟踪列表 → Bomb 的「清弹」需改为世界范围查询 + `SimplifiedProjectile.erase/markErased`。
- GL 用 NeoForge 风格：l2core `PlayerCapabilityTemplate` + `AttReg/AttVal`（GLMeta）、`SerialPacketBase` record 包、`LayeredDraw.Layer` HUD、`registerClient/registerSynced` 配置。
- 自定义符卡编辑器在 DanmakuAPI 内**完全自包含**（CustomSpellItem + EditorScreen + 包注册都在 jar 里），GL 只是**没有暴露任何 CustomSpell 物品**。

## 决策（已确认）
1. 擦弹接入方式：**在 DanmakuAPI-fix 的 GrazeHelper 加 provider 委托接口**，重建 jar，GL 在初始化时注册实现。
2. **移植 HUD**（灵力/残机/Bomb 显示）。
3. 东方角色不动（已移植）。

---

## A 部分：擦弹/灵力/Bomb 系统

### A1. 改 DanmakuAPI-fix：GrazeHelper 加可插拔 provider
文件：`DanmakuAPI-main-fix/.../api/GrazeHelper.java`
- 新增接口 `GrazeProvider`，含 4 个方法：`graze(Player, GrazingEntity)`、`getHitBoxShrink(Player)`、`shouldPlayerHurt(Player, LivingEntity)`、`forbidDanmaku(Player)`。
- 加 `static GrazeProvider INSTANCE = new GrazeProvider(){}`（默认空实现 = 当前行为），加 `setProvider(...)`。
- 4 个现有静态方法改为委托 `INSTANCE.xxx(...)`。保留 `globalInvulTime/globalForbidTime` 字段不变。
- **向后兼容**：不传 provider 时行为与现在完全一致，不影响其它依赖方。
- 重新 `gradlew build` + `publishToMavenLocal`，把产出的 `danmaku_api-3.0.5+3.jar(+sources)` 覆盖到 GL 的 `libs/`。
  （注：构建需要联网拉取 NeoGradle；若环境无法构建，此步骤会卡住——届时如实报告，不静默跳过。）

### A2. GL：移植 GrazeCapability（核心状态机）
新建 `content/attachment/graze/GrazeCapability.java`（参考 1.20.1 同名 + KoishiAttackCapability 写法）
- `extends PlayerCapabilityTemplate<GrazeCapability>`，`@SerialClass` + `@SerialField`。
- 字段：`power, hidden, step, bomb, life, invul, weak` + `Map<UUID, CombatSession> sessions`。
- 方法逐一移植：`initStatus / tick(Player) / graze / consumeGraze / performErase / useBomb / powerBonus / getInfoLines / initSession / stopSession / shouldHurt / findAny / setLife/Bomb/Power / isInvul / isWeak / sync / onClone`。
- **GL 适配点**：
  - `isFullCharacter` → GL 无此方法，新建 `GLCharacterHelper.isFullCharacter(LivingEntity)`：`e instanceof YoukaiEntity || hasEffect(YOUKAIFIED) || hasEffect(FAIRY)`（对照 1.20.1 EffectEventHandlers）。
  - `sync()` → 用 `GLMeta.GRAZE.type().network.toClient(sp)`（l2core PlayerCapabilityNetworkHandler）。
  - `YoukaiEntity.targets` 是 `YoukaiTargetContainer`（非 `Set<Player>`）→ session 用 `targets.contains(player)` / `targets.removePlayer(uuid)` 适配；CombatSession 的 `eraseDanmaku/resetTarget` 调 YoukaiEntity 新增方法（见 A4）。
  - 配置引用改 `GLModConfig`（A5）、属性引用 `GLAttributes`（已存在）。
- `CombatSession` 内部类同步移植，`shouldRemove` 用 `EntityStorageHelper.isPresent`。
- `HitType` 枚举原样移植。

### A3. GL：GrazeHelper 实现 + provider 注册 + 网络/音效
- 新建 `content/attachment/graze/GLGrazeProvider.java` implements `danmakuapi…GrazeProvider`，移植 1.20.1 GrazeHelper 的逻辑：
  - `graze()`：取 cap、查 invul、post `DanmakuGrazeEvent`（GL 版，NeoForge bus 可取消）、`cap.graze()` 成功则发 `GrazeToClient(0)`。
  - `forbidDanmaku()`：`cap.isInvul() || cap.isWeak()`。
  - `shouldPlayerHurt()`：`cap.shouldHurt(le)`。
  - `getHitBoxShrink()`：`getHitBoxDelta(player)`（属性）。
  - 静态辅助：`addSession / onDanmakuKill / getInitial*/getMax*/getGrazeEffectiveness/getHitBoxDelta`。
- 新建 `content/attachment/graze/GrazeToClient.java`（record + SerialPacketBase，type=0 放擦弹音、1 放 miss 音）。
- 新建 GL 版事件 `event/DanmakuGrazeEvent.java` + `event/DanmakuLastHitEvent.java`（NeoForge `Event` + `ICancellableEvent`，对照 GeneralEventHandlers 现有写法）。
- `init/registrate/GLSounds.java`：加 `GRAZE`、`MISS` 两个 SoundEvent。
- 客户端音效：在现有 `content/attachment/misc/ClientCapHandler.java` 加 `playGraze()/playMiss()`（或新建 graze 专用 client handler）。

### A4. GL：YoukaiEntity 接入清弹/会话
文件：`content/entity/youkai/YoukaiEntity.java`
- 新增 `eraseAllDanmaku(@Nullable Player)`：因 GL 弹幕是世界实体，用 `level` 范围查询自己作为 owner 的 `SimplifiedProjectile`（`getOwner()==this`），`player==null → markErased(true)`，否则 `erase(player)`。
- 新增 `resetTarget(Player)`：`targets.removePlayer(uuid)` + `setTarget(null)` + `setLastHurtByMob(null)`（GL 无 combatProgress，省略该行）。
- 重写 `danmakuHitTarget`（覆盖 IYoukaiEntity default）：玩家命中时走 `GrazeCapability.performErase(this)` → erase/skipDamage 逻辑，对照 1.20.1。
- `shouldHurt`/`onDanmakuHit`/`onDanmakuImmune` 已存在，必要时让 `shouldHurt` 在玩家发射场景叠加 cap 判定（实际玩家弹幕命中走 `IDanmakuEntity.shouldPlayerHurt` provider，无需改）。

### A5. GL：配置项
文件：`init/data/GLModConfig.java`
- 在 `Server` 的 `danmaku_battle` 段补：`danmakuMaxResource, danmakuMaxPower, danmakuPowerBonus, grazeEffectiveness, missInvulTime, bombInvulTime, maxPowerLossOnMiss, initialResource, initialPower`（默认值照 1.20.1）。
- 新增 `Client` 内部类（`registerClient`）：`powerInfoXAnchor/XOffset/YAnchor/YOffset`（HUD 锚点）。

### A6. GL：HUD 覆盖层
- 新建 `content/client/PowerInfoOverlay.java` implements `LayeredDraw.Layer`，移植 1.20.1 渲染逻辑，签名改 `render(GuiGraphics, DeltaTracker)`，配置改 `GLModConfig.CLIENT`。
- `init/YHClient.java` 的 `registerOverlay` 加一行 `registerAbove(VanillaGuiLayers.CROSSHAIR, loc("power_info"), new PowerInfoOverlay())`。

### A7. GL：注册接线
- `init/registrate/GLMeta.java`：加 `GRAZE = ATT.player("graze", GrazeCapability.class, GrazeCapability::new, PlayerCapabilityNetworkHandler::new)`。
- `init/GensokyoLegacy.java`：`HANDLER` 列表加 `GrazeToClient`（PLAY_TO_CLIENT）；在合适处调用 `GLGrazeProvider` 注册（`GrazeHelper.setProvider(new GLGrazeProvider())`，放在通用 setup/FMLCommonSetup）。
- 玩家发射时建立 session：DanmakuAPI 的 `DanmakuItem/LaserItem/SpellItem.use()` 无 session hook → 用 GL 已订阅的 `DanmakuUseEvent`（GeneralEventHandlers.onDanmakuUse 已存在）里追加 `GLGrazeProvider.addSession(player, target)`（target 由 `RayTraceUtil` 或玩家视线取），覆盖 1.20.1 在 use() 里 addSession 的行为。

### A8. 资源
- 复制 1.20.1 的 `assets/youkaishomecoming/sounds/graze.ogg`、`miss.ogg`、`textures/gui/elements.png` 到 GL `src/main/resources/assets/youkaishomecoming/...`。
- `sounds.json` 加 graze/miss 条目；lang 加 subtitle + powerinfo（可选）。

---

## B 部分：自定义符卡编辑器（暴露给玩家）

编辑器逻辑全在 jar 内，GL 只需**注册并暴露** CustomSpell 物品：

### B1. GL：注册自定义符卡物品
文件：`init/registrate/GLItems.java`（仿现有 `spell_reimu` 等 SpellItem 写法）
- 加两个 `ItemEntry<CustomSpellItem>`：
  - `custom_spell_ring` → `new CustomSpellItem(p.stacksTo(1), false, RingSpellFormData.FLOWER)`
  - `custom_spell_homing` → `new CustomSpellItem(p.stacksTo(1), true, HomingSpellFormData.RING)`
- `.tag(DanmakuTagGen.CUSTOM_SPELL)`（服务端保存校验依赖此 tag）、给 model（复用 `item/spell/...` 或自带贴图）、`.lang(...)`、放进 GL 创意标签 `TAB`。
- 不需要任何客户端注册：EditorScreen 由 `CustomSpellItem.use()` shift 右键自动打开；`SpellSetToServer` 包由 `DanmakuAPI.HANDLER` 自注册。

### B2. （可选）合成/获取
- 如需可获得性，给两件物品加简单合成或留在创意标签即可。先放创意标签，后续按需加配方。

---

## 验证
- 改完 DanmakuAPI-fix 后 `gradlew build`（fix 工程）确认编译通过并出 jar；覆盖 GL libs。
- GL 工程 `gradlew compileJava`（或 build）确认整体编译通过。
- 因这是 MC mod，无单测；编译通过为主要验证，运行期行为（擦弹加灵力、Bomb 清弹、HUD 显示、shift 开编辑器）需游戏内手测，会在交付说明里标注「未运行期验证」。

## 风险/不确定
- DanmakuAPI-fix 构建需联网（NeoGradle/NeoForge 依赖）。若无法构建出 jar，A 部分无法落地——会如实报告而非假装完成。
- GL `YoukaiEntity` 清弹用世界查询（GL 弹幕为真实体），与 1.20.1 虚拟列表语义略有差异，但 owner 过滤可达到等效「清掉该 youkai 的弹幕」。
- session 建立点改用 DanmakuUseEvent，与原版 use() 内 addSession 时机基本等价。
