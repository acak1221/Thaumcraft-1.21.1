# Event/tick registration audit — g3events — 2026-10-23

## Outcome and scope

**106 registration/subscriber sites inspected; one confirmed world-lifecycle defect fixed.**
No unregistered modern handler class or doubled gameplay tick registration was confirmed.
The defect affects the contents of two already-registered lifecycle callbacks: neither unloaded-world nor stopped-server cleanup discarded chunk-regeneration work.

Count: 102 bus-registration expressions + two annotated subscribers + two resource-reload registrations. Each expression counts separately even when two share a source line. Renderer/type/packet registrations and direct mixin calls are not included in this denominator. The table groups sites by file but identifies every expression by line/event. Line numbers refer to this commit's source.

Status **once** means source-reachable once under normal loader initialization, not runtime certification of every event. **Twice (intentional)** means an idempotent reset has two delivery paths, not that a tick listener was installed twice. NeoForge was read only; no NeoForge build/run was performed.

Path prefixes: `C` = `common/src/main/java/thaumcraft/`; `F` = `fabric/src/main/java/thaumcraft/fabric/`; `N` = `neoforge/src/main/java/thaumcraft/neoforge/`.

## Complete inspected registration inventory

| File | Sites: line/event or listener | Count | Status | Reachability / proof / fix |
|---|---|---:|---|---|
| C client/ClientSetup.java | 46 CLIENT_PRE; 48 CLIENT_POST; 55 CLIENT_LEVEL_LOAD | 3 | once | Guarded `init`; Fabric client initializer calls it; NeoForge CLIENT branch enqueues it. Particle tick accepts absent level; UtilsFX resets when player/level changes. |
| C client/ClientStateReset.java | 28 CLIENT_PLAYER_QUIT | 1 | once; reset twice intentionally | ClientSetup calls guarded init; Fabric also calls reset directly on disconnect. Reset is a cleanup, not a rate/effect increment. |
| C client/ThaumcraftBaublesClient.java | 15 INIT_POST | 1 | once | ClientSetup → guarded init; only InventoryScreen adds widget. |
| C client/ThaumcraftConfigClient.java | 30 CLIENT_POST | 1 | once | ClientSetup → init; tick skips absent player/level. |
| C client/ThaumcraftExcavationClient.java | 61 CLIENT_POST; 62 CLIENT_LEVEL_LOAD; 63 CLIENT_PLAYER_QUIT | 3 | once | ClientSetup → guarded init; reset on both world change and quit. |
| C client/ThaumcraftFocusClient.java | 44 RENDER_HUD; 48 KEY_PRESSED; 58 CLIENT_POST | 3 | once | ClientSetup → guarded init; key/held-focus state checks player, level, screen. |
| C client/ThaumcraftGogglesClient.java | 55 CLIENT_LEVEL_LOAD; 56 CLIENT_PLAYER_QUIT | 2 | once | ClientSetup → init; both clear tagScale. Loader render invocation is separate from these reset callbacks. |
| C client/ThaumcraftInfusionClient.java | 26 CLIENT_POST; 27 CLIENT_PLAYER_QUIT | 2 | once | ClientSetup → init; InfusionClientEffects tick owns effect lifetime. |
| C client/ThaumcraftMiscKeyClient.java | 39 CLIENT_POST | 1 | once | ClientSetup → init; no player/level means skip key action. |
| C client/ThaumcraftRunicClient.java | 26 ITEM tooltip | 1 | once | ClientSetup → init; HUD is called only by selected loader. |
| C client/ThaumcraftShockClient.java | 47 CLIENT_POST; 48 CLIENT_LEVEL_LOAD; 49 CLIENT_PLAYER_QUIT | 3 | once | ClientSetup → guarded init; bolts cleared on load and quit. |
| C client/ThaumcraftTaintSporesClient.java | 30 CLIENT_POST; 31 CLIENT_LEVEL_LOAD; 31 CLIENT_PLAYER_QUIT | 3 | once | ClientSetup → init; per-entity map prunes removed/wrong-level entities and reset clears it. |
| C client/ThaumcraftTravelGearClient.java | 64 CLIENT_POST; 67 CLIENT_PLAYER_QUIT; 67 CLIENT_LEVEL_LOAD | 3 | once | ClientSetup → init; keys separate from authoritative armor/bauble tick; resets on quit/load. |
| C client/ThaumcraftWarpClient.java | 76 CLIENT_POST | 1 | once | ClientSetup → init; absent player/paused guard. Authoritative WarpEvents is a different server handler. |
| C client/fx/EssentiaFlowClientEffects.java | 51 CLIENT_LEVEL_LOAD; 52 CLIENT_PLAYER_QUIT; 53 CLIENT_POST | 3 | once | ClientSetup → guarded init; world change/quit clear effect state. |
| C common/Thaumcraft.java | 195 PLAYER_QUIT; 196 CRAFT_ITEM; 200 CHANGE_DIMENSION; 201 PLAYER_CLONE; 205 PLAYER_POST; 208 SERVER_LEVEL_POST; 217 SERVER_LEVEL_UNLOAD; 224 SERVER_STOPPED; 236 SERVER_STARTED | 9 | once; **lifecycle cleanup incomplete, fixed** | Both loader roots call guarded Thaumcraft.init. Craft/dead-player callbacks guard ServerPlayer. World-post calls NodeHarvest, deferred drops, growth lamps, orb queue, regeneration once each. Added regeneration clearLevel/clearServer to unload/stop. |
| C common/config/ThaumcraftAlchemy.java | 59 SERVER_LEVEL_POST | 1 | once | Thaumcraft.init → register; TileCrucible.tickOutputs distinct from block-entity tick. |
| C common/config/ThaumcraftArcaneEar.java | 40 SERVER_LEVEL_POST; 41 SERVER_LEVEL_UNLOAD | 2 | once | Thaumcraft.init → register; notes remain available during world tick, clear afterward/unload. |
| C common/config/ThaumcraftBiomes.java | 50 SETUP | 1 | once | Thaumcraft.init → register; flammability after registration. |
| C common/config/ThaumcraftGolems.java | 197 CHANGE_DIMENSION | 1 | once | Single call from guarded Thaumcraft.init; trunk owner change is not a second entity tick. |
| C common/config/ThaumcraftInfusion.java | 87 SERVER_STOPPED | 1 | once | Guarded register reached from Thaumcraft.init; clears EssentiaHandler's weak world-keyed position caches at shutdown. |
| C common/config/ThaumcraftNaturals.java | 99 SETUP | 1 | once | Thaumcraft.init → register. |
| C common/config/ThaumcraftPlants.java | 39 SETUP | 1 | once | Thaumcraft.init → register. |
| C common/config/ThaumcraftTaintAnimals.java | 73 LIVING_DEATH | 1 | once | Thaumcraft.init → guarded register; distinct animal conversion predicate. |
| C common/config/ThaumcraftWarp.java | 67 PLAYER_POST | 1 | once | Thaumcraft.init → enableEvents, separate eventsEnabled guard; sole modern `WarpEvents.playerTick` call site. |
| C common/entities/DeathAspectOrbDrop.java | 69 LIVING_DEATH | 1 | once | Thaumcraft.init → register; orb drop, not conversion. |
| C common/entities/monster/TaintDeathConversion.java | 42 LIVING_DEATH | 1 | once | Thaumcraft.init → register; distinct conversion logic, not duplicate orb handler. |
| C common/items/armor/Hover.java | 36 PLAYER_QUIT | 1 | once | Thaumcraft.init → HoverNetworking.init → guarded Hover.initialize. |
| C common/lib/baubles/PlayerAccessories.java | 29 PLAYER_JOIN; 30 PLAYER_QUIT; 31 SERVER_STOPPED; 32 PLAYER_CLONE; 38 PLAYER_RESPAWN | 5 | once | Thaumcraft.init → ThaumcraftBaubles.register → AccessoryNetworking.init → guarded initialize. Tick itself uses player mixin, not a bus. |
| C common/lib/events/ThaumcraftCommands.java | 31 command EVENT | 1 | once | End of guarded Thaumcraft.init. Three command aliases are dispatcher nodes, not three event callbacks. |
| C common/lib/mods/ChampionHooks.java | 109 ADD | 1 | once | Root and ChampionWiring both call init, but initialized guard precedes registration. ServerLevel guard inside callback. |
| C common/lib/network/KnowledgeEvents.java | 25 PLAYER_JOIN; 30 PLAYER_CLONE; 34 PLAYER_RESPAWN; 35 CHANGE_DIMENSION | 4 | once | Thaumcraft.init → guarded initServer. ClientSetup invokes a different guarded initClient for S2C receivers. |
| C common/lib/network/ResearchSettingsNetworking.java | 17 PLAYER_JOIN; 29 CLIENT_PLAYER_QUIT | 2 | once; reset repeated intentionally | Root initServer; ClientSetup initClient. Settings reset also occurs through client reset paths, idempotently. |
| C common/lib/runic/RunicShield.java | 39 SERVER_STOPPED; 40 PLAYER_QUIT | 2 | once | ThaumcraftRunic.register → guarded initialize. Server state removed at stop/player logout; tick is a mixin descendant. |
| C common/lib/ward/WardedPearl.java | 43 ADD | 1 | once | Root → guarded enableEvents; ThrownEnderpearl and ServerLevel predicates. |
| C common/lib/world/OuterLandsDimension.java | 58 SERVER_STARTED | 1 | once | Root → ThaumcraftOuterLands.register → registerLogging; missing dimension logged, not dereferenced. |
| C common/lib/world/TradeSwaps.java | 52 SERVER_LEVEL_PRE; 53 SERVER_LEVEL_UNLOAD | 2 | once; phase unresolved | Root → guarded init; JOBS removed on unload. PRE differs from retained 1.12 END reference; see limitation below. |
| F FabricBossPlacement.java | 19 PLACE | 1 | once | ThaumcraftFabric.onInitialize → register. NeoForge alternative is not loaded on Fabric. |
| F ThaumcraftFabric.java | 113 AttackBlock; 124 UseBlock | 2 | once | Fabric mod entrypoint; use-first returns consumed result, preventing normal item/block dispatch after handled action. |
| F ThaumcraftFabricClient.java | 28/31/34 shader callbacks; 66 HUD; 72 DISCONNECT; 130 LAST | 6 | once; reset twice intentionally | Fabric client metadata entrypoint; no NeoForge bootstrap on this loader. Disconnect duplicates common idempotent cleanup only. |
| F ThaumcraftFabricClient.java | 79 resource reload | 1 | once | Same client entrypoint; distinct resource-reload lifecycle, not a tick registration. |
| F loot/FabricThaumcraftLoot.java | 30 MODIFY | 1 | once | Fabric root → init. |
| N NeoForgeBossPlacement.java | 23 onPlace; 24 onMultiPlace | 2 | once (source) | NeoForge constructor → register; two distinct native event types. |
| N NeoForgeCreativeTabs.java | 31 onBuildContents | 1 | once (source) | First call in NeoForge constructor. |
| N NeoForgeCrucibleFluidHandler.java | 27 registerCapabilities | 1 | once (source) | NeoForge constructor → init(bus). |
| N NeoForgeEntityAttributes.java | 50 EntityAttributeCreation | 1 | once (source) | NeoForge constructor → register(bus). |
| N NeoForgeInteractions.java | 48 onRightClickBlock; 49 onLeftClickBlock | 2 | once (source) | NeoForge constructor → register(bus); protection event posts are dispatch, not subscriptions. |
| N NeoForgeLootTables.java | 52 onLootTableLoad | 1 | once (source) | NeoForge constructor → register(bus). |
| N NeoForgeSharedBreakEvent.java | 48 shared BREAK observer; 52 class bus registration; 59 HIGHEST subscriber; 70 LOWEST subscriber | 4 | once (source) | Constructor → guarded init; observer increments delivery count; native callbacks only forward canceled breaks not already delivered. Nested/reentrant native events not runtime-tested here. |
| N NeoForgeSpaStorage.java | 26 registerCapabilities | 1 | once (source) | NeoForge constructor → init(bus). |
| N NeoForgeSpawnPlacements.java | 37 RegisterSpawnPlacements | 1 | once (source) | NeoForge constructor → register(bus). |
| N ThaumcraftNeoForge.java | 49 diagnostic login; 63 FMLClientSetup | 2 | once (source) | @Mod constructor; client setup registered only under Dist.CLIENT. Login is a log, not second settings synchronization. |
| N client/ThaumcraftNeoForgeClient.java | 70 shaders; 71 extensions; 72 reload listeners; 73 GUI; 74 level render; 75 logout | 6 | once (source) | NeoForge constructor CLIENT branch → register(bus); Fabric client callbacks cannot also load here. |
| N client/ThaumcraftNeoForgeClient.java | 188 resource reload | 1 | once (source) | Reached by registered onRegisterReloadListeners; prior reload-deadlock fix not re-audited here. |

## Confirmed defect: stale regeneration work crosses worlds

Before this fix, `ChunkRegeneration.QUEUE` was a static concurrent map keyed by `ResourceKey<Level>`. `readMarker` queued coordinates against `level.dimension()`, and `tick` drained coordinates using the same dimension key. Neither this class nor either root shutdown/unload callback removed entries. Proof: repository-wide `rg 'ChunkRegeneration|QUEUE'` and direct reads of those methods and root lifecycle bodies. Thus save A's queued overworld coordinates could be applied using save B's world seed and terrain when B next ticked in the same JVM. Default regeneration options are off; enabling any regeneration option exposes this path.

Fix: queue by the actual `ServerLevel` instance, clear that key on SERVER_LEVEL_UNLOAD, and selectively clear server-owned keys at SERVER_STOPPED. Worker-thread enqueue still uses concurrent map/queue; feature generation stays on the server world-post callback. No generation rate/seed/options are changed. The retained source also uses dimension-only queues; preventing cross-save mutation is an authority/lifetime correction, not a new gameplay rate.

`EventLifecycleRuntimeChecks.regenerationQueueLifecycle` verifies the actual level instance is the map key, dispatches the registered unload event, and leaves 21 entries for the real registered world-post callback to drain over three ticks (11, 1, 0). It never calls `ChunkRegeneration.tick` manually or installs another tick listener. Regeneration options are reset before ticking, so terrain cannot be rewritten. Shutdown fallback is checked directly, not by stopping the live test server. A real two-save reconnect was not performed.

## Direct ticks, exclusions, ordering, remaining uncertainty

- `ServerTickEventsFML` / `ClientTickEventsFML` exist only under retained, uncompiled `src/main/java`; modern references are oracle comments. Registering these legacy classes would be wrong. No modern `@Subscribe`-style class besides NeoForgeSharedBreakEvent was found.
- `PlayerEnchantmentTickMixin` calls `EquipmentEnchantments.playerLivingTick`, which calls `PlayerAccessories.tick` and `RunicShield.tick`. Those are not additionally subscribed to PLAYER_POST. Warp has one separate PLAYER_POST subscription. Search: `rg 'playerTick\(|RunicShield\.(tick|initialize)|PlayerAccessories\.(tick|initialize)' common/src/main/java fabric/src/main/java neoforge/src/main/java`.
- Root world-post sequencing explicitly keeps NodeHarvest before DeferredItemDrops. ArcaneEar clears note events at world-post, not before tile processing. Regeneration is world-post, consistent with the retained reference's END phase. Node block-entity ticking is not registered through this bus audit; no claim of exhaustive block-entity ticker coverage.
- **Reasoned-only phase concern, not fixed:** TradeSwaps uses SERVER_LEVEL_PRE, whereas retained 1.12 `ServerTickEventsFML.serverWorldTick` runs `tickBlockSwap` at END. Retained 1.12 is not the authoritative 4.2.3.5 oracle; this discrepancy alone does not prove a 4.2.3.5 regression. No authoritative oracle file for that method is present in this worktree. Needs oracle confirmation and focused swap timing test before changing phase.
- EssentiaHandler's world-keyed weak map values contain positions/timestamps rather than levels; global cache is cleared on server stop. NodeHarvest uses weak world keys and global stop cleanup. DeferredItemDrops/AspectOrbQueue use world SavedData and weak receipt keys. These differ from the confirmed dimension-only regeneration map and do not reuse another world's coordinates by dimension.
- `AspectRegistryEvent` is an API listener container, not an unregistered gameplay subscriber. The root posts it after aspect initialization; external addons supply listeners.
- Registration reachability and side checks are source reasoning except for the focused Fabric test recorded below. No client render observation, two-save reconnect, multiplayer cardinality run, or NeoForge runtime certification is claimed.

## Validation

Java 21 isolated-home Gradle run **passed**: `:common:test --tests thaumcraft.common.lib.events.ChunkRegenerationParityTest` (8 tests, zero failures/errors) and `:fabric:runGameTest -PgameTestFilter=regenerationQueueLifecycle` (one required dedicated-server test passed). The real world-post queue drained 21 → 11 → 1 → 0; registered unload cleanup and direct shutdown fallback assertions passed. Build completed successfully in 13m 37s. Reports: `common/build/test-results/test/TEST-thaumcraft.common.lib.events.ChunkRegenerationParityTest.xml`, `fabric/build/gametest-focused-results.xml`. No other runtime behavior is certified by this focused run.
