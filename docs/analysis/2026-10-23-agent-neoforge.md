# NeoForge half of the TC4 4.2.3.5 -> Minecraft 1.21.1 port — wave 2 (2026-10-23)

Agent: `neoforge`. Worktree: `/home/cyberfox-kali/wt/neoforge`, branch `agent/neoforge`.

Sources of truth (in order):

1. Oracle `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte-exact CFR 0.152
   decompilation of `Thaumcraft-1.7.10-4.2.3.5.jar`).
2. OpenTC4 `/home/cyberfox-kali/OpenTC4` (readable 1.7.10, second opinion only).
3. The shared 1.21.1 port in this worktree (`common/src/main/java`).

Environment of the runs:

```
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
GRADLE_USER_HOME=/home/cyberfox-kali/wt/neoforge/.gradle-neoforge   (isolated from the shared cache)
./gradlew --console=plain :neoforge:build :neoforge:runGameTest :neoforge:test
```

Gradle 8.12.1 (wrapper), NeoForge 21.1.259, Architectury 13.0.11, Minecraft 1.21.1.

---

## 0. What the previous session of this agent had already done

The work of the previous session was **committed** (not uncommitted) and is the ancestor of
this branch:

| Commit | Content |
|---|---|
| `f39307a` | the NeoForge loader got the same registrations as the Fabric bootstrap (attributes, spawn placements, biome spawns, loot rows, spa storage, creative tab, interactions, bore/golem platforms) |
| `f06fc6d` | the shared GameTest suite was ported to NeoForge (300 scenarios) and the gaps it exposed were fixed |
| `baa1c519` | the report of that wave (`docs/analysis/2026-10-21-close-neoforge.md`) |
| `d2782649` | the TC4 chest rows restored on NeoForge |

The report of that wave recorded **300 scenarios, 278 pass, 22 fail**. The wave-1 shared
changes were merged into `work` afterwards, so the wave-2 baseline had to be measured again.

**A warning about the first build of this wave:** the Gradle state of the worktree was stale
(`:neoforge:compileJava` was `UP-TO-DATE` although the shared module had changed), so the very
first `:neoforge:build` reported `BUILD SUCCESSFUL` without compiling anything. The real
compile error only appeared when the common jar was rebuilt:

```
neoforge/src/main/java/thaumcraft/neoforge/NeoForgeGolemHarvestPlatform.java:27: error:
NeoForgeGolemHarvestPlatform is not abstract and does not override abstract method
sharedBreak(ServerLevel,ServerPlayer,BlockPos,BlockState) in Bridge
```

That is the real wave-1 API change the task description mentions: the shared
`GolemHarvestPlatform.Bridge` and `BorePlatform.Bridge` gained a `sharedBreak` entry, and
**both** loaders had to implement it.

---

## 1. The build

```
> Task :neoforge:compileJava
> Task :neoforge:processResources
> Task :neoforge:jar
> Task :neoforge:shadowJar
> Task :neoforge:remapJar
> Task :neoforge:sourcesJar
> Task :neoforge:remapSourcesJar
> Task :neoforge:verifyTestIsolation
:neoforge: test roots are isolated from production and normal runs.
> Task :neoforge:verifyProductionIsolation
:neoforge: final remapped production JAR contains no test harness.
> Task :neoforge:check
> Task :neoforge:build
BUILD SUCCESSFUL in 34s
```

`:neoforge:test` (the new loader parity test, section 4) is part of `:neoforge:build` and is
green: **9 tests, 0 failures**.

---

## 2. The GameTest suite

`:neoforge:runGameTest` runs the shared harness of `common/src/gameTest` plus the NeoForge
harness of `neoforge/src/gameTest` in a real dedicated server.

| Run | Scenarios | Passed | Failed | Report |
|---|---|---|---|---|
| baseline of the wave (before the fixes of this wave) | 300 | 276 | **24** | `neoforge/build/gametest-results.xml` |
| after the probe registration and the break notification fix | 300 | 283 | **17** | same file, overwritten by the last run |

The 24 → 17 movement is the sum of two fixes, both committed in this wave.

### 2.1 The eight (then six) missing harness probes

The baseline run failed eight scenarios with

```
Game-test mod must register node probes before registry freeze
Register Excavation removal probe BEFORE block registry freeze
Game-test bootstrap must register the isolated native crop probe
registerTestProbes must register the test-only callback observer BEFORE registry freeze
test mod registered the native amulet pulse probe before registry freeze
```

The NeoForge game-test mod registered the shared probe blocks in the `RegisterEvent` of the
block registry, but the **modification was never in the compiled artefact**: the file
`neoforge/src/gameTest/java/thaumcraft/neoforge/testing/ThaumcraftGameTestMod.java` of the
branch carried the 26 line version without the registration, and the 101 line version with it
was an uncommitted edit. The first build of this wave therefore compiled a harness that could
not register a single probe.

The fix (`f2245c5c`) is the NeoForge-native equivalent of the Fabric mod initializer phase:
`RegisterEvent` of `Registries.BLOCK` and of `Registries.BLOCK_ENTITY_TYPE` fires while the
registry is still open, so the very same shared probe classes enter the native registry:

* `neoforge/src/gameTest/java/thaumcraft/neoforge/testing/ThaumcraftGameTestMod.java:56` —
  `onRegister(RegisterEvent)`;
* the event itself: `net/neoforged/neoforge/registries/GameData.java:88` (NeoForge 21.1.259
  sources) creates one `RegisterEvent` per registry and posts it to every mod bus.

**Proof from the server log of the run:**

```
[modloading-sync-worker/INFO] [ThaumcraftGameTest/]: DIAG RegisterEvent key=ResourceKey[minecraft:root / minecraft:block] blockKey=ResourceKey[minecraft:root / minecraft:block] equal=true
[modloading-sync-worker/INFO] [ThaumcraftGameTest/]: DIAG probes registered: conversion=true
```

(The two `DIAG` lines were a temporary diagnostic and are not in the committed code.)

Six of the eight scenarios turned green with that one change:
`nodetransductionconversionpersistenceandcooldown`,
`nodetransductionisolatedfailurelifecycle`,
`growthlampconsumesessentiaandfertilisesplants`,
`fluxcallbacksboundariesandneighborwake`,
`visamuletstransferandpaythroughactualnativerelay` and — a real behavioural difference that
the missing probe had hidden — `boreouterdestroycallbackremainsclosed`.

### 2.2 The double break notification (a real behavioural difference)

`lumbergolemfellsonefarlogthroughtheguardedplatform` failed with

```
the protection BREAK event fired once, at the ACTUAL endpoint: [BlockPos{...}, BlockPos{...}]
```

The **shared** code fired the shared Architectury `BlockEvent.BREAK` itself
(`common/src/main/java/thaumcraft/common/entities/golems/GolemHarvestPlatform.java`, the
`BlockEvent.BREAK.invoker().breakBlock(...)` calls) and the **NeoForge** platform fired the
native cancellable `BlockEvent.BreakEvent` as well. NeoForge forwards its native event into the
shared event, so one machine removal notified the other mods **twice** on NeoForge and once on
Fabric.

The fix (`8f041ed2`, `b870abd2`) moves the shared notification into the loader bridge:

* `common/src/main/java/thaumcraft/common/entities/golems/GolemHarvestPlatform.java:53` — the
  new `Bridge.sharedBreak` entry and its two call sites (the water drain and the guarded
  break);
* `common/src/main/java/thaumcraft/common/lib/bore/BorePlatform.java:21` and
  `common/src/main/java/thaumcraft/common/lib/bore/BoreWork.java:424` — the same for the bore;
* `fabric/src/main/java/thaumcraft/fabric/golems/FabricGolemHarvestPlatform.java:35` — Fabric
  has no native counterpart and delivers the shared event, **exactly once**;
* `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeGolemHarvestPlatform.java:53` and
  `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeBorePlatform.java:45` — NeoForge answers
  without a second notification, because its native event already delivers it.

The Fabric behaviour is unchanged byte for byte: the shared event is still fired once, by the
loader, at the same point of the sequence.

---

## 3. The remaining failures and their verdict

The last full run of this wave left **17** failures. The verdict of every one of them:

| # | Scenario | Verdict | Why |
|---|---|---|---|
| 1 | `excavationextraremovalfailureaccounting` | **harness gap** | the probe loads `net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents` by reflection (`common/src/gameTest/java/thaumcraft/common/testing/ExcavationRemovalFailureRuntimeChecks.java:221`); that class does not exist on NeoForge |
| 2 | `excavationharvestpreservesprotectionlootandcontainers` | **harness gap** | the same reflection (`ExcavationHarvestRuntimeChecks.java:397`) |
| 3 | `excavationupgradetreasureandenlarge` | **harness gap** | the same reflection (`ExcavationUpgradeRuntimeChecks.java:199`) |
| 4 | `taintacleskeeporiginalcombatandrootedai` | **shared behaviour, still open** | an exception without a message inside the check; the taintacle AI is shared code, so the failure is a shared defect, not a loader difference |
| 5 | `taintacleskeeporiginalretaliationlifetimeandloot` | **shared behaviour, still open** | the same |
| 6 | `fluxoriginalexposureandgootail` | **shared behaviour, still open** | `original lower25 MISS falls through30 HIT then boolean; exact observed calls=[nextInt(25)]` — the observed RNG bound is the original one, the scenario wants the 30 roll as well |
| 7 | `fluxmovesandexhaustswithoriginalrules` | **shared behaviour, still open** | `pending real Thaumic Slime branch does not silently consume goo (M11 not complete)` — the scenario itself names the missing unit |
| 8 | `fullinventoryreusesfreedpaperslotordrops` | **shared behaviour, still open** | `creative overflow spends one paper and the ink, and creates no note entity` |
| 9 | `magicalbiomesgeneratenaturallyandspawnactualpech` | **shared behaviour, still open** | `region generation admits actual typed BE with original generation-plus-growth age3..7` — the world generation is shared code |
| 10 | `purifyingfluidpreservesoriginalclassicflowwardandsaltexpiry` | **shared behaviour, still open** | `actual native entity tick sees eyes just inside original Forge filledPercentage: 0` |
| 11 | `tablebreakevacuateseachhalfonce` | **fixture depends on a Fabric-only event** | the scenario waits for the `server-level-post` tick of the Fabric harness (`ResearchTableRuntimeChecks.java:309`); the NeoForge loader has no such tick, so the scenario never reaches its own assertion |
| 12 | `entropyqueuesthefarthestconnectedwaterforliquidgolems` | **shared behaviour, still open** | the drained buckets are not conjured; the golem cargo logic is shared code |
| 13 | `entropyswitcheschangewhatrealgolemsfetchemptyandsort` | **shared behaviour, still open** | the item-match lanes do not finish in 1800 ticks |
| 14 | `travelingtrunkfollowsteleportsdefendsandpullsitems` | **shared behaviour, still open** | the trunk phases do not finish in 1500 ticks |
| 15 | `voidmetalgearmendswithersandwarps` | **shared behaviour, still open** | the void sword does not wither the mob for 60 ticks |
| 16 | `advancedfurnacepreservescustodyacrossrealchunkunload` | **shared behaviour, still open** | the partial disassembly does not force-load the remote shell terrain |
| 17 | `lootbagspreserveoriginalpoolsandpaidnativedropcustody` | **loader difference, still open** | the native dungeon table of NeoForge is built by the encoded-table rewrite of `NeoForgeLootTables`, not by the Fabric weighted-pool hook; the scenario draws 2000 seeds from `minecraft:chests/simple_dungeon` and expects a loot bag, and the NeoForge table does not reach one in that many seeds (`LootBagRuntimeChecks.java:157`) |

**The important conclusion for the NeoForge parity:** of the 17 remaining failures, **three are
harness gaps** (the reflection into a Fabric-API class), **one depends on a Fabric-only tick**
of the harness, and **thirteen are shared-code behaviour** that is identical on both loaders.
There is **no remaining failure that is a NeoForge-only behavioural difference**, apart from
the loot table of #17, whose *content* is the same (the same ten tables, the same 22 TC4 rows
in the same weighted pool) but whose *draw order* differs because the NeoForge path rebuilds
the encoded table instead of mutating a mutable builder.

---

## 4. The parity that was never proved

The shared `common` module owns the registry content, so both loaders register the same
blocks, items, block entity types, sounds, entities, menus and creative tab **by
construction**. What the loaders own separately is the wiring around that content, and that
wiring was never compared.

`neoforge/src/test/java/thaumcraft/neoforge/NeoForgeLoaderParityTest.java` (new in this wave,
part of `:neoforge:build`) compares the two loader modules **source against source**, because
the two modules never see each other at runtime. It is green: **9 tests, 0 failures**.

| Registration area | What the test compares | Result |
|---|---|---|
| **entity attributes** | the fifteen direct `FabricDefaultAttributeRegistry.register` entries and the `EntityAttributeCreationEvent` entries of `NeoForgeEntityAttributes`, entry by entry, plus the four shared catalogues (`ThaumcraftEldritchMobs`, `ThaumcraftCultists`, `ThaumcraftTaintCreeper`, `ThaumcraftTaintAnimals`) | equal |
| **spawn placements** | the seven `SpawnPlacements.register` calls and the `RegisterSpawnPlacementsEvent` entries, with the placement type, the height map and the predicate; plus the two TC4 entities that have no placement of their own (`eldritch_guardian`, `inhabited_zombie`), which NeoForge demands explicitly | equal |
| **biome modifiers** | the nine `BiomeModifications.addFeature` calls and the nine `data/thaumcraft/neoforge/biome_modifier/*.json` entries, with the biome tag, the generation step and the placed feature identifier | equal |
| **original entity spawn rules** | the identifier `thaumcraft:original_entity_spawns` of the Fabric modification, of the NeoForge modifier type and of the data pack entry, plus the shared `OriginalSpawnRules` transformation on both sides | equal |
| **loot tables** | the ten vanilla chest tables and their original chest generation category, in `FabricThaumcraftLoot` and `NeoForgeLootTables` | equal |
| **access widener / access transformer** | the three entries of `common/src/main/resources/thaumcraft.accesswidener` and the three entries of `neoforge/src/main/resources/META-INF/accesstransformer.cfg`, normalised to class, member and descriptor | equal |
| **fluid types** | the NeoForge `FluidType` of the purifying fluid and of the death fluid, and their registration in `thaumcraft-neoforge.mixins.json` | present |
| **client registrations** | the two core shaders, the builtin item renderers (the whole item set), the HUD overlay, the late world render, the disconnect reset and the client resource reload | equal (after the fix of section 5) |

The blocks, items, block entity types, sounds and the creative tab need no comparison: they
are the shared catalogues of `common/src/main/java/thaumcraft/common/config/**`, and the NeoForge
bootstrap calls the very same `Thaumcraft.init()` that the Fabric bootstrap calls
(`neoforge/src/main/java/thaumcraft/neoforge/ThaumcraftNeoForge.java:28`).

### 4.1 The excavation mixins

The shared excavation mixin
`common/src/main/java/thaumcraft/common/mixin/ExcavationServerPlayerGameModeMixin.java` owns
only the interceptions that both loader graphs share. The three interceptions of the vanilla
graph are loader specific and live in

* `fabric/src/main/java/thaumcraft/fabric/mixin/compat/FabricExcavationRemovalMixin.java` and
* `neoforge/src/main/java/thaumcraft/neoforge/mixin/NeoForgeExcavationRemovalMixin.java`,

because NeoForge 21.1 replaces `ServerLevel.removeBlock(BlockPos, boolean)` with its own
private `removeBlock(BlockPos, BlockState, boolean)`. Both are registered with `require = 1`,
so a wrong graph is a startup crash, not a silent behaviour change.

### 4.2 The client events

`neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` registers
the two core shaders (`RegisterShadersEvent`), the builtin item renderers
(`RegisterClientExtensionsEvent`), the HUD overlay (`RenderGuiEvent.Post`), the late world
render (`RenderLevelStageEvent`), the client resource reload
(`RegisterClientReloadListenersEvent`) and the disconnect reset
(`ClientPlayerNetworkEvent.LoggingOut`) — the six counterparts of the Fabric client
registrations.

---

## 5. A real gap the parity test found: two missing item renderers

The parity test compares the **item set** of the builtin item renderers of the two loaders,
and the first run failed:

```
every item renderer of the NeoForge client exists on Fabric, missing on Fabric:
[ThaumcraftContent.THAUMOMETER, ThaumcraftEldritchBlocks.BANNER_ITEM]
```

The Fabric client registers two builtin item renderers that the NeoForge client did not:

* `thaumcraft.common.config.ThaumcraftContent.THAUMOMETER` — the original
  `ItemThaumometerRenderer` (oracle `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java`),
  the three dimensional scanner of the original, not a flat sprite;
* `thaumcraft.common.config.ThaumcraftEldritchBlocks.BANNER_ITEM` — the original
  `ItemBannerRenderer` (oracle `thaumcraft/client/renderers/item/ItemBannerRenderer.java`),
  the banner of the cultist, which carries its colour and its aspect.

On NeoForge both items were drawn with the **vanilla baked model** instead of the original TC4
model. The fix is in
`neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java:98` and
`:104`: the NeoForge client item extensions now publish the two shared renderers. The renderer
code itself is the shared code, so the drawing is byte for byte the drawing of Fabric.

**Acceptance criteria (a screenshot must show):** in the inventory, the thaumometer shows the
original scanner body with the glowing scan screen on top of it (not a flat sprite), and the
eldritch banner shows the banner of its colour and its aspect (not a wooden cube).

---

## 6. The three client features without a NeoForge counterpart

The report of the previous wave named three Fabric API client extensions that were not
transferred. This wave found the exact NeoForge-native mechanism for each of them. **None of
the three is implemented yet** — the reason is at the end of this section.

### 6.1 The clean water render (`FluidRenderHandlerRegistry`)

* **Fabric:** `fabric/src/main/java/thaumcraft/fabric/client/FabricFluidPureRenderHandler.java`
  registers a `FluidRenderHandler` for `thaumcraft:purifying_fluid` and
  `thaumcraft:flowing_purifying_fluid`. It draws the Forge 1.7 **Classic** corner heights of
  the purifying fluid: the surface height of a cell is `0.875` for a source and
  `quanta / 8 * 0.875` for a flowing cell, the four corners are averaged with
  `PureFlowRules.renderAverage`, the sides are drawn with the shade of `0.8` (north, south) and
  `0.6` (west, east) and the bottom with `0.5`.
* **Original:** the same geometry comes from the Forge Classic renderer of 1.7.10 that the
  oracle's `BlockFluidPure` relies on (`thaumcraft/common/blocks/BlockFluidPure.java`, the
  `SpaRules.pureLight` level and the `PureFlowRules.quanta` of the shared port).
* **NeoForge 21.1:** **there is no fluid render handler event.** The whole NeoForge client
  fluid API is `net/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions.java`
  (tint, still/flowing/overlay texture, camera overlay, fog) and
  `net/neoforged/neoforge/fluids/FluidType.java` (the server side properties). None of them
  can change the *geometry* of a fluid.
* **The NeoForge-native way:** a mixin into
  `net.minecraft.client.renderer.block.FluidRenderer` that draws the purifying fluid with the
  shared geometry. The shared geometry is already loader neutral: `BlockFluidPure.quanta`,
  `BlockFluidPure.legacySolid` and `PureFlowRules.renderAverage` live in `common`, and the
  Fabric handler is a thin adapter over them. The mixin therefore has to reproduce only the
  quad construction of `FabricFluidPureRenderHandler.renderFluid` (the same sprite
  `thaumcraft:blocks/fluidpure`, the same `0.001` offset, the same UV rotation for a flowing
  cell and the same shade factors).
* **Acceptance criteria:** a screenshot of a pool of the purifying fluid next to a pool of
  water must show the TC4 surface height (a source cell is 7/8 of a block high, a flowing cell
  `quanta/8` of that) and the TC4 side shading, and the fluid must not be drawn with the
  vanilla water corners.

### 6.2 The `taint_fibres` model (`RenderMaterial` / `QuadEmitter`)

* **Fabric:** `fabric/src/main/java/thaumcraft/fabric/client/FabricTaintFibresModel.java`
  replaces the baked model of `thaumcraft:taint_fibres` with one that emits the fibre planes
  and the crossed stalks itself.
* **Original:** the oracle's `thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java`
  draws the six side planes at `0.995` inside the block, the six glow overlays at `0.98` for
  the metadata zero with the glow of the taint, the crossed stalk of the metadata one and two
  with the original `0.5` wide grass offset (the `(x * 3129871) ^ (z * 116129781) ^ y` hash of
  the original) and the crossed stalk of the metadata three and four.
* **The NeoForge-native way:** `ModelEvent.ModifyBakingResult`
  (`net/neoforged/neoforge/client/event/ModelEvent.java:44`) is the counterpart of the Fabric
  `ModelLoadingPlugin`: it hands the mod the map of the baked models and a sprite lookup, so
  the NeoForge module can replace the model of `thaumcraft:taint_fibres`.
* **The obstacle, and it is a real one:** the Fabric model draws its planes from the
  **neighbours** of the cell (`emitBlockQuads(BlockAndTintGetter world, ...)`), and the vanilla
  `BakedModel.getQuads(BlockState, Direction, RandomSource)` has **no world access**. A plain
  `BakedModel` therefore cannot express the neighbour dependent planes, and NeoForge 21.1 has
  no event that hands a world to a block model. The NeoForge-native way is consequently a
  mixin into the vanilla block renderer
  (`net.minecraft.client.renderer.block.model.ModelBlockRenderer` or
  `net.minecraft.client.renderer.block.BlockRenderDispatcher`), where the world and the
  position are available. The quads themselves are buildable with the public
  `net.minecraft.client.renderer.block.model.FaceBakery.bakeQuad(from, to, face, sprite,
  direction, modelState, rotation, shade)`: the crossed stalks of the original are exactly the
  vanilla `block/cross` geometry (the half width `0.05..0.95` and the 45 degree rotation of the
  vanilla cross), and the side plane of the original is the unit square at `0.005` inside the
  face, which is what the Fabric `square(nominalFace, 0, 0, 1, 1, 0.995)` produces.
* **Acceptance criteria:** a screenshot of a taint fibre cell next to a solid block must show
  the fibre skin on the side of the solid neighbour only, the crossed stalk of the metadata
  one and two must carry the original grass offset, and with the glow of the taint enabled the
  metadata zero must carry the four overlay sprites of the original.

### 6.3 The armour renderer of the void robe, the void armour and the fortress armour

* **Fabric:** `net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer` — the Fabric API
  mixin `net.fabricmc.fabric.mixin.client.rendering.ArmorFeatureRendererMixin` injects into
  `HumanoidArmorLayer.renderArmor` at HEAD and cancels it, so a registered item is drawn by
  the custom renderer of the mod instead of the vanilla armour layer. The port registers
  `ThaumcraftGearModelsClient::renderRobe` for the three void robe pieces,
  `ThaumcraftGearModelsClient::renderFortressArmor` for the three fortress pieces and
  `HarnessArmorRenderer::render` for the hover harness
  (`fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java:42`,
  `fabric/src/main/java/thaumcraft/fabric/client/FabricTravelGearClient.java:11`).
* **NeoForge 21.1:** **there is an equivalent hook, and it is native.** The NeoForge patch of
  `HumanoidArmorLayer` (NeoForge 21.1.259 userdev jar,
  `patches/net/minecraft/client/renderer/entity/layers/HumanoidArmorLayer.java.patch`) adds

  ```java
  protected Model getArmorModelHook(T entity, ItemStack itemStack, EquipmentSlot slot, A model) {
      return net.neoforged.neoforge.client.ClientHooks.getArmorModel(entity, itemStack, slot, model);
  }
  ```

  and `ClientHooks.getArmorModel` (`net/neoforged/neoforge/client/ClientHooks.java:322`)
  forwards to `IClientItemExtensions.getGenericArmorModel`. The texture of the piece is the
  item's own, through `ClientHooks.getArmorTexture` (same file, line 260), which calls
  `ArmorItem.getArmorTexture`.
* **The NeoForge-native way:** the TC4 gear models are already `Model` objects —
  `common/src/main/java/thaumcraft/client/renderers/models/gear/ModelGearArmor.java:33` extends
  `HumanoidModel<LivingEntity>` — so the shared module has to publish, per item and per slot,
  the TC4 model (`ThaumcraftGearModelsClient` already builds them), the NeoForge client
  extension returns it from `getGenericArmorModel`, and the TC4 armour items override
  `ArmorItem.getArmorTexture` with the TC4 sheet. `ClientHooks.copyModelProperties` copies the
  wearer's pose to the returned model, so the original biped pose is kept.
* **The obstacle, and it is a real one:** the **hover harness** is not a model. Its original
  renderer draws the chest cube *and* the retained 124 triangle OBJ pack on the back with its
  own unlit render type (`common/src/main/java/thaumcraft/client/renderers/models/gear/HarnessArmorRenderer.java`).
  The NeoForge armour model hook can only return a `Model`, so the back pack of the hover
  harness has **no** NeoForge-native expression and would still need a mixin into
  `HumanoidArmorLayer`. The four TC4 armour families (void robe, cultist plate, fortress
  armour, hover harness chest) can be closed with the model hook; the back pack cannot.
* **Acceptance criteria:** a screenshot of a player wearing the void robe, the cultist plate
  and the fortress armour must show the TC4 models (the own sheet, the own slot visibility and
  the own ornaments of the fortress set) and not the vanilla armour layer; a screenshot of the
  hover harness must show the chest cube and the back pack.

### 6.4 Why the three are not implemented in this wave

* The client cannot be started and inspected from this worktree, and `AGENTS.md` demands
  inspected game evidence for client visuals. A wrong winding, a wrong render type or a wrong
  UV in a custom block model is a **visible** regression, and shipping it unverified would be
  worse than the documented gap.
* The `taint_fibres` model needs a mixin into the vanilla block renderer, because the vanilla
  `BakedModel` has no world access; that is the one change of the three that touches the
  vanilla render path.
* The three gaps are now **mechanically tracked**: the parity test
  `theKnownNeoForgeClientGapsAreTheDocumentedOnes` fails as soon as one of them is closed, so
  the list cannot go stale.

---

## 7. Still open

1. **The three client features** of section 6: the clean water render, the `taint_fibres`
   model and the armour renderer. The NeoForge-native mechanism is known for each of them
   (section 6), the acceptance criteria are written down, and the hover harness back pack has
   no NeoForge-native expression.
2. **The thirteen shared behaviour failures** of section 3. They are shared code and therefore
   a Fabric defect as much as a NeoForge one; they belong to the domains of the shared code
   (taintacles, flux, research table, golems, void gear, advanced furnace) and are not closed
   by this wave.
3. **The loot table of #17**: the NeoForge path rebuilds the encoded table, and the draw order
   of the first pool differs from the Fabric path. The content is the same, the draw is not.
   The fix belongs into `NeoForgeLootTables` (the rows have to be appended with the same
   relative order and the same weights as the Fabric mutable builder produces).
4. **The three harness probes** of section 3 that reflect into
   `net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents`: the NeoForge harness would
   need its own `BEFORE`/`CANCELED`/`AFTER` observer of the native `BlockEvent.BreakEvent`.
5. **The `server-level-post` tick** of the research table scenario: the NeoForge harness would
   need the same tick, or the scenario would have to be written loader neutral.
6. **The client was never started** in this wave: no NeoForge client run, no production
   (remapped) JAR run, no multiplayer run.

---

## 8. The commits of this wave

| Commit | Content |
|---|---|
| `8f041ed2` | `fix(golems,bore)`: the shared break notification is delivered exactly once per break, on both loaders |
| `f2245c5c` | `fix(neoforge)`: the shared test probes are registered in the native `RegisterEvent` |
| `b870abd2` | `fix(neoforge)`: the same break notification fix, plus the native `BLOCK_DESTROY` observation, the failure stack trace in the log and the focused `-PgameTestFilter` diagnostics |
| `f0e13f62` | `test(neoforge)`: the loader parity test, and the two missing item renderers it found |
| `3bef8dc5` | `docs(neoforge)`: the baseline of the wave |
