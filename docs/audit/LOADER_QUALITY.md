# Loader and configuration quality audit — 2026-10-23

Baseline: `3340abe7`, branch `agent/c6loaders`. Review standard: maintainable, explicit loader integration, not merely “loads without crashing.”

**Verdict: not ready for acceptance.** Two duplicate item identities are release blockers; the spa GUI lacks its NeoForge adapter. No new missing mixin target was found. Two unused loot-access operations were removed and compile-checked.

## Inventory, evidence and limits

| Surface | Inspected |
|---|---:|
| Fabric production Java files | 19 |
| NeoForge production Java files | 29 |
| Common config/registration Java files | 91 |
| Container/support Java files | 27 |
| Menus, including the unused ghost base | 17 |
| Mixin configs / listed mixin classes | 9 / 142 |
| Production / test mixins | 103 / 39 |
| Distinct mixin target classes / loader-specific `javap` checks | 84 / 154 |
| Common deferred-register owners | 209 |
| Registration call sites: literal / computed identifiers | 601 / 34 |
| Expanded registration declarations, all registry kinds | 851 |
| Original configuration keys / supported file keys / editor entries | 62 / 34 / 25 |
| Data JSON files: common / NeoForge / Fabric | 764 / 10 / 0 |

Counts are source inventory, not a claim that a server booted successfully. All 774 data JSON files parse; 1,034 own-namespace item/block/entity references in required tags, `item` fields and item-loot `name` fields resolve against the expanded source catalogue. This is not a full Minecraft codec/datapack reload test. The original's metadata families are deliberately flattened; this audit does not certify every flattened name against the complete original item/block catalogue.

Bytecode checked with `javap -p -s -c`: cached Mojang-mapped 1.21.1 merged Minecraft and NeoForge **21.1.259** patched Minecraft under `~/.gradle-leader/caches/fabric-loom/`; Architectury **13.0.11** remapped jar; shared mod targets additionally checked against cached `/tmp/opencode/javac-out` classes and current source. The last cache is not a fresh build of this branch. No Gradle, client launch, dedicated server, injection application or multiplayer inventory test was run.

Changed production classes compile with `javac --release 21 -proc:none`, the remapped patched Minecraft jar, `common/build/classes/java/main`, and dependency jars from `~/.gradle-leader/caches/modules-2/**`. **This worktree has no `common/build/classes/java/main` output.** The four edited classes do not depend on common classes, so their isolated compile still succeeds; this is not a whole-loader build pass. Scratch evidence is under `.tmp/c6-*` (ignored).

The existing `NeoForgeLoaderParityTest` was compiled and its 12 test methods invoked directly: **11 passed, 1 failed**. The spawn test at `neoforge/src/test/java/thaumcraft/neoforge/NeoForgeLoaderParityTest.java:216` still expects seven Fabric placements; there are eight. Text-parity success is not runtime evidence.

| Checklist | Findings | Fixed | Open |
|---|---:|---:|---:|
| 1. Loader parity | 3 | 0 | 3 |
| 2. Mixin integrity | 0 | 0 | 0 |
| 3. Container/menu coherence | 0 confirmed | 0 | 0 confirmed |
| 4. Config surface | 4 | 0 | 4 |
| 5. Registration completeness | 2 | 0 | 2 |
| 6. Class size and duplication | 1 | 0 | 1 |
| 7. Dead code | 2 | 2 | 0 |
| 8. Errors/logging | 3 | 0 | 3 |
| **Total** | **15** | **2** | **13** |

Severity totals: **2 Critical, 1 High, 9 Medium, 3 Low**. Findings are counted once in their owning section; tables elsewhere cross-reference them.

## 1. Fabric/NeoForge parity

**Inspected: all 48 production loader files; 32 feature groups below. Findings: 3.**

Path abbreviations in this table: `F/` = `fabric/src/main/java/thaumcraft/fabric/`; `N/` = `neoforge/src/main/java/thaumcraft/neoforge/`; `C/` = `common/src/main/java/thaumcraft/`. Each side names its actual wiring, including shared wiring where appropriate.

| Feature | Fabric and file | NeoForge and file | Review |
|---|---|---|---|
| Shared content bootstrap | `F/ThaumcraftFabric.java:23` | `N/ThaumcraftNeoForge.java:28` | Same common catalogue; R1/R2 affect both |
| Entity attributes: 15 direct + 4 shared catalogues | `F/ThaumcraftFabric.java:30-109` | `N/NeoForgeEntityAttributes.java:49-72` | Equal, including small taintacle |
| Seven original explicit spawn placements | `F/ThaumcraftFabric.java:32-75` | `N/NeoForgeSpawnPlacements.java:39-59` | Equal predicates/heights |
| Inhabited-zombie placement | Ground + inhabited predicate, `F/ThaumcraftFabric.java:98` | Unrestricted, `N/NeoForgeSpawnPlacements.java:68` | **P1** |
| Guardian fallback placement | No placement, `F/ThaumcraftFabric.java` | Explicit unrestricted, `N/NeoForgeSpawnPlacements.java:65` | Documented vanilla-equivalent adaptation |
| Biome mob spawns | `F/world/FabricBiomeSpawns.java` | `N/NeoForgeBiomeSpawns.java`, `N/OriginalEntitySpawnsBiomeModifier.java` | Shared `OriginalSpawnRules`; modifier JSON present |
| Nine biome feature additions | `F/ThaumcraftFabric.java:147-171` | `neoforge/src/main/resources/data/thaumcraft/neoforge/biome_modifier/` | Same nine features, tags and steps |
| Ten vanilla chest categories and weighted rows | `F/loot/FabricThaumcraftLoot.java` | `N/NeoForgeLootTables.java:38-76` | Shared additions; first-pool append on both |
| Creative-tab publication | Shared `C/common/config/ThaumcraftCreativeTabs.java` | `N/NeoForgeCreativeTabs.java`, installed before common | Native sink adapts Architectury behavior |
| Crucible fluid integration | `F/fluid/FabricCrucibleStorage.java` | `N/NeoForgeCrucibleFluidHandler.java` | Shared water tank; native unit conversion |
| Spa fluid integration | `F/fluid/FabricSpaStorage.java` | `N/NeoForgeSpaStorage.java` | Shared tank; transaction/capability adapters |
| Golem fluid/container lookup | `F/fluid/FabricGolemFluidAccess.java:88-106` | `N/NeoForgeGolemFluidAccess.java:76-91` | **P3**: storage projection differs |
| Bore machine actor and break protection | `F/bore/FabricBorePlatform.java` | `N/NeoForgeBorePlatform.java` | Native actors; documented event differences |
| Golem harvest actor and protection | `F/golems/FabricGolemHarvestPlatform.java` | `N/NeoForgeGolemHarvestPlatform.java` | Same contract, native events |
| Golem use actor | `F/golems/FabricGolemUsePlatform.java` | `N/NeoForgeGolemUsePlatform.java` | Shared action policy; native actor adapters |
| Item-use-first golem, bell, resonator, tube label | `F/ThaumcraftFabric.java:127` | `N/NeoForgeInteractions.java:54` | Both installed |
| Trade-focus left click/protection | `F/ThaumcraftFabric.java:110-123` | `N/NeoForgeInteractions.java:91-117` | Both installed |
| Active-boss placement cancellation | `F/FabricBossPlacement.java` | `N/NeoForgeBossPlacement.java` | Native placement consultation on both |
| Excavation native removal | `F/mixin/compat/FabricExcavationRemovalMixin.java` | `N/mixin/NeoForgeExcavationRemovalMixin.java` | Different patched invocation paths verified |
| Shared break notification | Architectury/native Fabric events, `F/bore/FabricBorePlatform.java` | `N/NeoForgeSharedBreakEvent.java` | NeoForge cancelled-event bridge is intentional |
| Pure/death fluid native types | Tags/common fluid classes, `F/fluid/FabricSpaStorage.java` | `N/mixin/NeoForgeFluidPureMixin.java`, `NeoForgeFluidDeathMixin.java` | Necessary native adaptation, not missing Fabric feature |
| Shared client bootstrap and entity/BE renderers | `F/ThaumcraftFabricClient.java:40` | `N/ThaumcraftNeoForge.java:63` | Both call `ClientSetup.init`; prior renderer wiring exists |
| Builtin item renderer catalogue | 24 registration statements, `F/ThaumcraftFabricClient.java:43-128` | 21 grouped statements, `N/client/ThaumcraftNeoForgeClient.java:107-156` | Same items, grouped/looped differently |
| Three core shaders | `F/ThaumcraftFabricClient.java:28-39` | `N/client/ThaumcraftNeoForgeClient.java:79-98` | Equal; comment saying “two” is stale |
| Robe dye | `F/mixin/client/FabricRobeArmorLayerMixin.java` | `N/client/NeoForgeRobeDye.java` | Documented patched armor invocation difference |
| Robe/fortress/harness models | `F/ThaumcraftFabricClient.java:48`, `F/client/FabricTravelGearClient.java` | `N/mixin/client/NeoForgeGearArmorMixin.java` | Same shared gear rendering |
| Pure fluid world rendering | `F/client/FabricFluidPureRenderHandler.java` | `N/mixin/client/NeoForgePureFluidMixin.java` | Same shared geometry |
| Spa GUI fluid appearance | `F/client/FabricSpaClient.java:11` | No setter call in `N/client/ThaumcraftNeoForgeClient.java` | **P2** |
| Position models and taint fibres | `F/client/FabricPositionBlockModels.java`, `FabricTaintFibresModel.java` | `N/mixin/client/NeoForgePositionBlockModelMixin.java` + common model path | Shared `LegacyBlockQuads` handles fibres; stale test gap text is not evidence of a missing renderer |
| HUD + late world effects | `F/ThaumcraftFabricClient.java:66,131` | `N/client/ThaumcraftNeoForgeClient.java:215,224` | Both callbacks present |
| Resource reload + disconnect reset | `F/ThaumcraftFabricClient.java:72-90` | `N/client/ThaumcraftNeoForgeClient.java:181-247` | NeoForge barrier wait is present; common disconnect reset also installed |
| Sound identities and shared client sound calls | `C/common/config/ThaumcraftSounds.java`, `C/client/ClientSetup.java` | Same common paths | 66 sound identities on both; no separate sound callback in current Fabric bootstrap to compare |

- **P1 — Medium:** `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeSpawnPlacements.java:68` registers inhabited zombies with `NO_RESTRICTIONS` and `unrestricted`; Fabric uses `ON_GROUND` and `EntityInhabitedZombie::checkInhabitedSpawnRules` at `fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabric.java:98-100`. The explanation at NeoForge lines 60-64 incorrectly says Fabric leaves this entity without a placement. This changes natural-spawn admission. Not fixed: align against current common rules, then test actual spawning; do not merely update the stale source-test count.
- **P2 — High:** `fabric/src/main/java/thaumcraft/fabric/client/FabricSpaClient.java:11-15` installs `GuiSpa.setFluidAppearance`; NeoForge never installs it. `common/src/main/java/thaumcraft/client/gui/GuiSpa.java:42` requires it non-null when rendering a nonempty tank. A filled spa GUI has a direct null-failure path on NeoForge. Not an impossible API: NeoForge exposes client fluid-type appearance. Left for a compiled/tested native adapter.
- **P3 — Medium:** `fabric/src/main/java/thaumcraft/fabric/fluid/FabricGolemFluidAccess.java:88-106` returns the first nonblank fluid but totals **all** nonblank storage views. `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeGolemFluidAccess.java:76-91` reports tank zero only. Multi-tank/mixed-fluid addon handlers therefore have incompatible identity/amount contracts. Common projection semantics should be specified before changing either side.

Historical claims were not copied as new findings: the NeoForge reload barrier and builtin renderer registrations are present. Neither loader currently registers a separate sound interception callback in its production bootstrap; that historical issue was not independently reproduced here.

## 2. Mixin integrity

**Inspected: 9 configs, all 142 listed classes, 84 distinct target classes, 154 loader-specific class checks, 188 selector strings and 37 invocation/field target strings. Findings: 0.**

All listed sources exist. No actual source mixin under the applicable source sets is unlisted. All configs are included by their production/test `fabric.mod.json` or `META-INF/neoforge.mods.toml`; the common config is named by both production metadata files. All nine use `required=true`, Java 21 and `injectors.defaultRequire=1`.

All target classes and selector method names exist in the checked artifacts; every explicitly supplied method descriptor matches. The only absent invocation is `LivingEntity.removeAllEffects()` in NeoForge's milk method, intentionally optional at `common/src/main/java/thaumcraft/common/mixin/ScopedFluxMilkBucketMixin.java:11-14` (`require=0`, native NeoForge cure-set alternative documented). **Missing target classes: 0. Missing required selector methods: 0. Missing required invocation strings: 0.**

The Fabric robe `ItemStack.is(TagKey)` redirect is loader-local, not applied to NeoForge. NeoForge's excavation redirects target its patched `removeBlock` helper and `canHarvestBlock`, not the absent vanilla removal/tool calls. Both were checked in disassembly. Existence/descriptor checks do not prove locals capture, injection ordering or transformed runtime application. The per-class table is below, after section 8.

## 3. Container/menu coherence

**Inspected: all 27 files; 17 menu classes, 17 quick-move implementations, 17 validity implementations and 6 reusable slot classes. Findings: 0 confirmed.** This is code review, not a dupe-test pass.

| Menu (`common/src/main/java/thaumcraft/common/container/`) | Quick move / slot policy | Server validity |
|---|---|---|
| `ContainerAccessories.java` | 4 accessory slots ↔ 36; bauble equip/unequip predicates | Same owner/accessory inventory |
| `ContainerAlchemyFurnace.java` | 2 ↔ 36; fuel first, aspect fallback; unrestricted manual fuel is original | BE identity + squared distance 64 |
| `ContainerArcaneBore.java` | 2 single-item device slots ↔ 36; focus/pickaxe predicates; copied getter write-back | Lifetime + current BE + squared distance 64 |
| `ContainerArcaneWorkbench.java` | Result/wand/matrix/player boundaries 0/1/2-10/11-46; pay-before-result; output refuses placement | Current BE + squared distance 64 |
| `ContainerDeconstructionTable.java` | 1 ↔ 36, aspect-bearing accepted input, main/hotbar fallback | Current BE + squared distance 64 |
| `ContainerFocalManipulator.java` | Focus-only 1 ↔ 36, main/hotbar fallback | BE lifetime + research + squared distance 64 |
| `ContainerFocusPouch.java` | 18 focus-only ↔ 36; held source slot locked | Exact held item/components + live owner, no world-distance requirement |
| `ContainerGhostSlots.java` | Ghost-source quick move refused; clicked ghosts never extract stock | Always false: unused base, not a registered standalone menu |
| `ContainerGolem.java` | Filter exemplars nonphysical; quick move stays within player inventory | Golem identity/core/owner + `canConfigure` squared distance 64 |
| `ContainerHandMirror.java` | One unrestricted transport input ↔ 36; mirror excluded from quick insertion | Same viewer, no world-distance requirement; bridge uses captured mirror |
| `ContainerHoverHarness.java` | 1 valid fuel jar ↔ 36; held source slot locked | Exact held harness/components + live owner |
| `ContainerMagicBox.java` | 27 visible box slots; linked-size transfer quirk explicitly retained | Backend validity + `stillValidBlockEntity` identity/proximity |
| `ContainerPech.java` | Input + 4 output-only slots ↔ 36 | Live admitted customer, tamed/live entity, squared distance 64 |
| `ContainerResearchTable.java` | 2 class-restricted inputs ↔ 36; original whole-note-stack merge intentionally retained | Current BE + squared distance 64 |
| `ContainerSpa.java` | Salt-only transfer in both directions, 1 ↔ 36 | Current nonretired BE + squared distance 64 |
| `ContainerThaumatorium.java` | 1 unrestricted original catalyst slot ↔ 36 | Current nonquarantined BE + squared distance 64 |
| `ContainerTravelingTrunk.java` | `rows*9` ↔ 36, reverse return | Live trunk, owner/open policy, squared distance 100 |

Bounded native transfer ranges or explicit progress checks terminate each active transfer. The custom research merge increments/decrements its index on every continuing branch and breaks after empty-slot insertion. Ghost and result slots were distinguished from physical stock, and handheld menus were not incorrectly required to check distance to a world block. The ghost base's player-to-ghost branch is unsafe to reuse for physical stock; it currently has no production construction/subclass caller and `stillValid=false`, so no live duplication/stock-loss finding is asserted.

## 4. Config surface and data

**Inspected: 62 original keys, 34 supported properties, all 25 editor entries, all 774 data JSON files. Findings: 4.** Defaults were compared to read-only original `Config.java:369-510` and its initialized defaults. All 34 supported keys retain the original normal-file default; the cheat-sheet configured default is false despite its original pre-load static default being true. The original research-difficulty malformed-value fallback of 8 is intentionally preserved, not “corrected” to 0.

Reader paths below are relative to `common/src/main/java/thaumcraft/`. “Sync” means explicit server→client settings payload, not native entity/BE/world state replication. All readers run on both loaders through common code.

| Property | Default | Actual consumer | Side / explicit settings sync |
|---|---|---|---|
| `allow_cheat_sheet` | false | `common/lib/research/ResearchCheatSheet.java`, `common/items/relics/ItemThaumonomicon.java` | Server + client / yes |
| `allow_warded_stone` | true | ward blocks, `common/items/wands/WandWardedStone.java`, research conditions | Server + client / yes |
| `allow_mirrors` | true | `common/lib/research/ResearchConditions.java:35` | Server + client / yes |
| `hard_mode_nodes` | true | `common/tiles/TileNode.java:509`, `common/lib/world/NodeEcology.java`, hungry-node client | Server + client / yes |
| `wuss_mode` | false | `common/lib/WarpEvents.java`, `ResearchCheatSheet.java` | Server + client / yes |
| `research_difficulty` | 0 | `ResearchSettings.purchases`, research purchase flow | Server + client / yes |
| `aspect_total_cap` | 100 | `common/lib/research/ScanManager.java:64` | Server + client / yes |
| `biome_taint_spread` | 200 | `ThaumcraftTaintSettings.taintSpreadRate`, taint spreading rules | Server / no |
| `spawn_taint_spores` | true | `ThaumcraftTaintSettings.spawnTaintSpore`, taint-spore spawning | Server / no |
| `glowing_taint` | true | `ThaumcraftTaintSettings.glowingTaint`, taint overlay rendering | Local client / no |
| `shaders` | true | `ThaumcraftSettings.shaders`, warp post effects | Local client / no |
| `magical_forest_biome_weight` | 5 | `common/lib/world/biomes/BiomePatchPolicy.java:53` | World generation / no |
| `taint_biome_weight` | 2 | `BiomeSettings.warmWeight/coolWeight`, biome weighted lottery | World generation / no |
| `blue_magical_forest` | false | `common/mixin/client/Tc4BiomeColorMixin.java:46` | Local client / no |
| `generate_trees` | true | `common/lib/world/ThaumcraftTreeFeature.java:30`, regeneration | Server / no |
| `generate_taint` | true | Parsed/stored; **no behavioral reader**, matching original ineffectiveness | None / no; documented compatibility, not a new dead-key defect |
| `spawn_angry_zombies` | true | `common/lib/world/biomes/OriginalSpawnRules.java:24,31` | Server / no |
| `spawn_fire_bats` | true | `OriginalSpawnRules.java:33` | Server / no |
| `spawn_wisps` | true | `OriginalSpawnRules.java:21,25,37` | Server / no |
| `spawn_taintacles` | true | `OriginalSpawnRules.java:28` | Server / no |
| `spawn_pechs` | true | `OriginalSpawnRules.java:20,32` | Server / no |
| `spawn_eldercreatures` | true | `OriginalSpawnRules.java:26` | Server / no |
| `generate_aura_nodes` | true | `common/lib/world/ThaumcraftNormalNodeFeature.java:34`, totems/regeneration | Server / no |
| `generate_structures` | true | Totem/structure generation, regeneration | Server / no |
| `generate_cinnibar_ore` | true | `common/lib/world/ThaumcraftOreFeature.java:53` | Server / no |
| `generate_amber_ore` | true | `ThaumcraftOreFeature.java:53` | Server / no |
| `generate_infused_stone` | true | `ThaumcraftOreFeature.java:53` | Server / no |
| `regen_key` | DEFAULT | `common/lib/events/ChunkRegeneration.java:106` | Server / no |
| `aura_nodes` | false | `ChunkRegeneration.java:200` | Server / no |
| `structures` | false | `ChunkRegeneration.java:220` | Server / no |
| `cinnibar_ore` | false | `ChunkRegeneration.java:210` | Server / no |
| `amber_ore` | false | `ChunkRegeneration.java:211` | Server / no |
| `infused_stone` | false | `ChunkRegeneration.java:212` | Server / no |
| `trees` | false | `ChunkRegeneration.java:205` | Server / no |

Seven research facts are encoded by `common/lib/network/ResearchSettingsPayload.java:14-16`, with separate local/remote snapshots in `ThaumcraftSettings`. Unsynced startup worldgen/spawn options are server-owned; unsynced local rendering options are intentional, not automatically parity defects.

- **C1 — Medium:** `common/src/main/java/thaumcraft/common/config/ThaumcraftConfigStore.java:66-97` is not the runtime schema: **9 supported keys are absent from the editor** (two biome weights and seven regeneration options). Lines 93-94 incorrectly say regeneration does not exist, although `OriginalRegenSettings.java:31-40` and `common/lib/events/ChunkRegeneration.java` implement it. There are three independently maintained key/default lists: file generation, parsers, editor. Generate those surfaces from one typed schema while preserving original parsing quirks.
- **C2 — Medium:** `common/src/main/java/thaumcraft/common/config/ThaumcraftSettings.java:66-88` and `Config.java:118-185` do not expose **18 still-meaningful original editable options** through the file reader. They stay constants/default fields or hardcoded behavior: `champion_mobs=true`, `color_blind=false`, `crooked=true`, `wand_dial_bottom=false`, `golem_delay=5`, `golem_ignore_delay=10000`, `golem_link_quality=16`, `notification_delay=5000`, `notification_max=15`, `node_rarity=36`, `special_node_rarity=18`, `display_aspects=false`, `golem_chest_interact=true`, `portablehole_blacklist=iron_door`, `biome_taint_from_flux=true`, `runic_recharge_speed=2000`, `runic_recharge_delay=80`, `runic_cost=50`. A handwritten property with one of these names is preserved by the editor but ignored at startup. Of the other 10 unsupported original keys, **9 numeric IDs** (four biomes, dimension, two enchantments, two villagers) are obsolete registry-era settings; the original unused regen `taint` flag is also absent. Do not count those 10 as missing modern features.
- **C3 — Medium:** `common/src/main/java/thaumcraft/common/config/Config.java:125-177,199-224` advertises the original public runtime options but only publishes the research and taint snapshots. Biome/spawn/worldgen/regeneration parsers publish exclusively to `ThaumcraftBiomeSettings.configure` (`ThaumcraftBiomeSettings.java:25-26`). Consequently e.g. setting `generate_trees=false` changes the real generator while `Config.genTrees` remains true; `blue_magical_forest=true` leaves `Config.blueBiome=false`. This is a misleading compatibility API, not dead-file configuration. Define authoritative ownership and keep the legacy view aligned or explicitly deprecate unsupported mirrors.
- **C4 — Medium:** `common/src/main/java/thaumcraft/common/config/ThaumcraftConfigStore.java:127-137` loads with Java `Properties.load` but saves raw `key=value` text. Its promise to preserve unknown properties is false for escaped backslashes, embedded newlines, significant leading whitespace or separator characters in keys. A preserved `regen_key` containing such characters changes on the next read. Use properties-compatible escaping and test read→edit→write→read preservation. This was reviewed, not executed as a production persistence test.

Data coverage: 344 recipes, 129 loot tables, 238 tag files (153 own namespace, 48 Minecraft, 37 `c`), 32 worldgen files, 7 damage types, 6 `tc4` payload files, 5 enchantments, and one each dimension/dimension-type/dimension-effects file, plus 10 NeoForge biome modifiers. The generated external file is `config/thaumcraft.properties`. The nine NeoForge feature-modifier references have corresponding common placed-feature/tag data; the tenth activates the registered spawn-modifier codec. Resource parsing/reference checks found **0 malformed JSON and 0 missing identities in the 1,034 checked content references**. Dynamic codec loading and all original→flattened resource naming remain runtime/full-catalogue validation limits, not passes.

## 5. Registration completeness

**Inspected: 209 deferred-register owners, 635 registration sites, expanded to 851 declarations across all registry kinds. Findings: 2 Critical.** Every deferred owner has one publication call in its owning registration method; every config owner is reached from common bootstrap. Both loaders invoke common bootstrap. Repeated `ThaumcraftEldritchBlocks.register()` calls are guarded at `ThaumcraftEldritchBlocks.java:654-659`, not duplicate registry inserts.

| Requested registry family | Expanded declarations | Distinct IDs | Loader ownership |
|---|---:|---:|---|
| Blocks | 156 | 156 | Common → both |
| Items | 406 | **404** | Common → both; R1/R2 |
| Block entities | 65 | 65 | Common → both |
| Entities | 56 | 56 | Common → both |
| Sounds | 66 | 66 | Common → both; exactly matches all 66 `sounds.json` keys |
| Registered particle types | 0 | 0 | Custom FX construct particles directly; common particle-engine mixins are wired on both |
| Creative tabs | 1 | 1 | Common tab + native NeoForge contents sink |

Other declarations: 26 components, 17 menus, 13 recipe serializers, 12 armor materials, 11 features, 9 effects, 5 recipe types, 4 fluids, 2 loot function types and 2 professions. Computed IDs were expanded from the 34 helper/loop sites, including candles, primal shards/crystals, infused ores, golem parts, baubles and 66 sounds. These are source declarations; actual runtime registries were not dumped.

- **R1 — Critical:** `common/src/main/java/thaumcraft/common/config/ThaumcraftContent.java:162-163` and `common/src/main/java/thaumcraft/common/config/ThaumcraftWands.java:50` both declare **`thaumcraft:thaumium_nugget`** in the item registry. Both owners are published by common bootstrap (`Thaumcraft.java:79,131`). They also disagree on class: behavioral `ItemNugget` versus plain `Item`. Registration cannot meet a one-identity/one-owner contract on either loader. Consolidate ownership and alias all consumers to the retained behavioral supplier; test recipes and creative publication before merging.
- **R2 — Critical:** `common/src/main/java/thaumcraft/common/config/ThaumcraftContent.java:164-165` and `common/src/main/java/thaumcraft/common/config/ThaumcraftWands.java:54` do the same for **`thaumcraft:void_nugget`**. Same ownership/type problem. Neither collision was edited in this audit: choosing the retained public supplier and removing duplicate tab publication is broader than deleting unused loader methods.

No further duplicate ID was found in the expanded declarations. No assertion is made that every original metadata variant is fully implemented just because its modern registration/model exists. Original naming has documented flattening exceptions (e.g. resource/shard families); complete original-name certification requires the migration catalogue and actual registry dump.

## 6. Class size and duplication

**Inspected: all 48 loader classes; 9 duplicated catalogue/callback blocks. Classes over 250 lines: 0. Findings: 1.** Largest class is `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` at 248 lines. The next are Fabric bootstrap (173), Fabric golem fluid adapter (171), and Fabric client bootstrap (143). No arbitrary splitting is recommended just to meet a line threshold.

| Duplicated information | Fabric owner | NeoForge owner |
|---|---|---|
| 15 direct entity attributes + 4 catalogue adapters | `ThaumcraftFabric.java:30-109` | `NeoForgeEntityAttributes.java:49-72` |
| Seven shared explicit spawn placements (+ one drifted entry) | `ThaumcraftFabric.java:32-100` | `NeoForgeSpawnPlacements.java:39-70` |
| Nine biome feature/tag/step rows | `ThaumcraftFabric.java:147-171` | Nine feature-modifier JSON files |
| Ten chest categories and row construction | `loot/FabricThaumcraftLoot.java` | `NeoForgeLootTables.java:38-76` |
| Builtin item render bindings | `ThaumcraftFabricClient.java:43-128` | `client/ThaumcraftNeoForgeClient.java:107-156` |
| Three shader ID/format/consumer bindings | `ThaumcraftFabricClient.java:28-39` | `client/ThaumcraftNeoForgeClient.java:79-98` |
| HUD effect call list | `ThaumcraftFabricClient.java:66-70` | `client/ThaumcraftNeoForgeClient.java:215-220` |
| Late-world effect call list | `ThaumcraftFabricClient.java:131-140` | `client/ThaumcraftNeoForgeClient.java:224-236` |
| Reload reset call list | `ThaumcraftFabricClient.java:84-88` | `client/ThaumcraftNeoForgeClient.java:206-211` |

Paths in the table use the same Fabric/NeoForge source roots as section 1.

- **D1 — Medium:** `fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabric.java:30-109` and `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeSpawnPlacements.java:39-70` exemplify **nine manually mirrored lists**. Native event bridges must differ, but entity IDs, factories, resource IDs and shared callback sequences need not. P1 and the obsolete seven-placement test demonstrate actual list drift. Publish typed shared catalogues and thin native sinks; do not move native loader APIs into common or rewrite both bootstraps wholesale during an audit.

## 7. Dead code

**Inspected: 48 loader files, 201 matched concrete method declarations before cleanup (199 after), and associated interface contracts/call sites across common and both loader source trees. Findings: 2 Low, both fixed.** Callback method references, interface overrides, annotated mixin handlers and mod entrypoints were retained even when ordinary call syntax was absent. No additional definite uncalled production helper was established.

- **U1 — Low, fixed:** baseline `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeLootPoolAccess.java:23` / `mixin/NeoForgeLootPoolMixin.java:36`: `thaumcraft$entries()` had no caller. The live adapter only invokes `thaumcraft$appendEntries`. Removed declaration and implementation.
- **U2 — Low, fixed:** baseline `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeLootTableAccess.java:20` / `mixin/NeoForgeLootTableMixin.java:36`: `thaumcraft$setPools(...)` had no caller. Removed declaration/implementation, its `ArrayList` import and unnecessary `@Mutable` shadow; retained final read-only pool access. Updated comments so they no longer promise table replacement.

Fix scope: **2 operations, 4 declaration/implementation removals, 4 edited production files**. Java 21 isolated compilation passed for all four together against real remapped Minecraft and cached dependencies. No mixin config change was necessary.

## 8. Error handling and logging

**Inspected: 14 catch sites (5 loader, 9 config), plus both loader bootstraps' logging. Findings: 3. Silent registration catches in bootstrap: 0.**

| Catch sites | Review |
|---|---|
| `fabric/.../fluid/FabricGolemFluidAccess.java:62,119,133,146` | Four broad runtime catches, return absent/false/zero; E1 |
| `neoforge/.../client/ThaumcraftNeoForgeClient.java:96` | Shader I/O wrapped and thrown; fails visibly, no swallow |
| `ThaumcraftConfigStore.java:52,118,138` | Editor integer fallback; file read/write errors logged |
| `ResearchSettings.java:35` | Original malformed-int fallback retained |
| `BiomeSettings.java:22` | Invalid input rethrown with context |
| `ThaumcraftBiomeSettings.java:34` | Error logged; explicit defaults snapshot |
| `ThaumcraftSettings.java:89` | Error logged; all settings groups reset to defaults |
| `ThaumcraftTaintSettings.java:19` | Silent malformed spread fallback to 200; operator observability limitation, not a skipped registration |
| `ThaumcraftOuterLands.java:338` | Reflection failures erased; E2 |

Config paths above are under `common/src/main/java/thaumcraft/common/config/`.

- **E1 — Medium:** `fabric/src/main/java/thaumcraft/fabric/fluid/FabricGolemFluidAccess.java:62,119,133,146` catches every `RuntimeException` from third-party lookups/transfers and reports “no fluid/no capacity.” A broken provider and ordinary transaction contention become indistinguishable; repeated transfer failures cannot be diagnosed. Narrow expected transaction exceptions and add bounded/rate-limited contextual diagnostics for unexpected provider failures. These are runtime callbacks, **not** bootstrap registration catches.
- **E2 — Medium:** `common/src/main/java/thaumcraft/common/config/ThaumcraftOuterLands.java:334-340` erases both reflective lookup and invoked setter failures into false. This was a temporary parallel-port integration seam; target setters are now concrete classes. It hides genuine altar/banner/spawner setup errors and leaves untyped string contracts in the data layer. Prefer typed interfaces/direct calls; if compatibility reflection remains, distinguish missing optional methods from failed invocation and log context.
- **E3 — Low:** `neoforge/src/main/java/thaumcraft/neoforge/ThaumcraftNeoForge.java:49-61` installs permanent highest-priority login **INFO** diagnostics including connection/channel object identity hashes and payload-setup implementation details. This is troubleshooting instrumentation in the production bootstrap, not user-actionable operational logging. Gate behind a debug option or move to DEBUG/test harness; preserve any separate functional login/sync behavior.

## Per-class mixin table

`V` = remapped vanilla/Fabric path; `N` = patched NeoForge path. “yes” means target class and declared selector checks described in section 2 passed, **not** runtime mixin application. Every row is listed in its group's config; every group's metadata includes that config; every group has `defaultRequire=1`. Source roots and configs are given per group so `Class:line` resolves unambiguously to `Class.java:line`. Shared mod targets use cached compiled mod classes, as disclosed above.

### Common production — 91 classes

Config: `common/src/main/resources/thaumcraft-common.mixins.json`. Source root: `common/src/main/java/thaumcraft/common/mixin/`; rows after `Tc4ChunkRegenerationMarkerMixin` are under `client/`.

| Mixin source:line | Target class(es) | Bytecode |
|---|---|---|
| `Tc4LevelStemLifecycleMixin:42` | `MappedRegistry` | V/N: yes |
| `PlayerKnowledgeMixin:14` | `Player` | V/N: yes |
| `ServerLevelEntityManagerAccess:9` | `ServerLevel` | V/N: yes |
| `PersistentEntitySectionManagerAccess:11` | `PersistentEntitySectionManager` | V/N: yes |
| `EntityLevelCallbackAccess:8` | `Entity` | V/N: yes |
| `FluxEffectInstanceMixin:16` | `MobEffectInstance` | V/N: yes |
| `ScopedFluxMilkBucketMixin:12` | `MilkBucketItem` | V/N: yes; optional invocation exception described above |
| `VanillaFurnaceBellowsAccess:7` | `AbstractFurnaceBlockEntity` | V/N: yes |
| `CrystalChestInventoryMixin:12` | `ChestBlockEntity` | V/N: yes |
| `CrystalHopperInventoryMixin:12` | `HopperBlockEntity` | V/N: yes |
| `CrystalFurnaceInventoryMixin:12` | `AbstractFurnaceBlockEntity` | V/N: yes |
| `DeferredDropRemovalMixin:13` | `Entity` | V/N: yes |
| `DeferredDropPickupMixin:11` | `ItemEntity` | V/N: yes |
| `ExcavationServerPlayerGameModeMixin:28` | `ServerPlayerGameMode` | V/N: yes |
| `ExcavationInfestedBlockMixin:14` | `InfestedBlock` | V/N: yes |
| `BoreLevelMutationMixin:16` | `Level` | V/N: yes |
| `BoreChunkMutationMixin:22` | `LevelChunk` | V/N: yes |
| `BoreNativeChunkOwnersMixin:15` | `ChunkAccess` | V/N: yes |
| `BoreNativeTickerMixin:21` | `LevelChunk$BoundTickingBlockEntity` | V/N: yes |
| `BoreNativeTickerWrapperMixin:10` | `LevelChunk$RebindableTickingBlockEntityWrapper` | V/N: yes |
| `BoreNativeChunkCacheMixin:14` | `ServerChunkCache` | V/N: yes |
| `BoreNativeInventoryMixin:11` | `TileArcaneBore` | V/N: yes |
| `ZombieBrainDropsMixin:12` | `LivingEntity` | V/N: yes |
| `GolemUseServerPlayerGameModeMixin:20` | `ServerPlayerGameMode` | V/N: yes |
| `TradeFocusServerPlayerGameModeMixin:27` | `ServerPlayerGameMode` | V/N: yes |
| `StreamAxeItemMixin:24` | `ItemEntity` | V/N: yes |
| `SpecialMiningBlockMixin:21` | `Block` | V/N: yes |
| `ServerGamePacketListenerFloatAccess:8` | `ServerGamePacketListenerImpl` | V/N: yes |
| `LivingEntityAttackStrengthAccess:8` | `LivingEntity` | V/N: yes |
| `PlayerAttackSweepMixin:12` | `Player` | V/N: yes |
| `LivingEntityMagicWardMixin:19` | `LivingEntity` | V/N: yes |
| `ArcaneEarNoteMixin:16` | `NoteBlock` | V/N: yes |
| `WardedPlayerGameModeMixin:15` | `ServerPlayerGameMode` | V/N: yes |
| `WardedCallbackMutationMixin:13` | `Level` | V/N: yes |
| `WardedEnchantingTableMixin:13` | `EnchantingTableBlock` | V/N: yes |
| `WardedBeaconMixin:13` | `BeaconBlockEntity` | V/N: yes |
| `WardedLeavesMixin:13` | `LeavesBlock` | V/N: yes |
| `WardedPlantMixin:16` | `BushBlock, CactusBlock, MushroomBlock, SugarCaneBlock` | V/N: yes |
| `PlayerEnchantmentTickMixin:12` | `LivingEntity` | V/N: yes |
| `ResearchNoteStackCodecMixin:11` | `ItemStack` | V/N: yes |
| `ResearchNoteContainerCountMixin:13` | `SimpleContainer, BaseContainerBlockEntity` | V/N: yes |
| `ResearchNoteMenuCountMixin:12` | `AbstractContainerMenu` | V/N: yes |
| `ThaumatoriumChestInventoryMixin:12` | `ChestBlockEntity` | V/N: yes |
| `ThaumatoriumHopperInventoryMixin:12` | `HopperBlockEntity` | V/N: yes |
| `ThaumatoriumFurnaceInventoryMixin:12` | `AbstractFurnaceBlockEntity` | V/N: yes |
| `WarpFoodMixin:14` | `LivingEntity` | V/N: yes |
| `PlayerRunicSaveMixin:12` | `Player` | V/N: yes |
| `PlayerAccessoriesMixin:15` | `Player` | V/N: yes |
| `ItemEntityPechDropMixin:12` | `ItemEntity` | V/N: yes |
| `PlayerTravelGearMixin:14` | `Player` | V/N: yes |
| `Tc4BuiltinBiomeParameterListsMixin:14` | `MultiNoiseBiomeSourceParameterLists` | V/N: yes |
| `Tc4BiomeParameterListMixin:19` | `MultiNoiseBiomeSourceParameterList` | V/N: yes |
| `Tc4ClimateSamplerMixin:8` | `Climate$Sampler` | V/N: yes |
| `Tc4RandomStateMixin:13` | `RandomState` | V/N: yes |
| `Tc4MultiNoiseBiomeSourceMixin:29` | `MultiNoiseBiomeSource` | V/N: yes |
| `Tc4OuterLandsFlatGeneratorMixin:25` | `FlatLevelGeneratorSettings` | V/N: yes |
| `BathSaltExpiryMixin:10` | `ItemEntity` | V/N: yes |
| `PureWaterMaterialMixin:13` | `WaterFluid` | V/N: yes |
| `PureVanillaWaterFlowMixin:14` | `FlowingFluid` | V/N: yes |
| `PureBucketFillMixin:16` | `BucketItem` | V/N: yes |
| `DeathBucketFillMixin:23` | `BucketItem` | V/N: yes |
| `ChampionEntityDataMixin:23` | `LivingEntity` | V/N: yes |
| `ChampionMobMixin:22` | `Mob` | V/N: yes |
| `ChampionTickMixin:16` | `Mob` | V/N: yes |
| `ChampionHurtMixin:21` | `LivingEntity, Player` | V/N: yes |
| `ChampionDeathLootMixin:19` | `LivingEntity` | V/N: yes |
| `VillagerVillageTradesMixin:28` | `Villager` | V/N: yes |
| `WorldGenRegionStepAccess:13` | `WorldGenRegion` | V/N: yes |
| `LevelChunkPendingBlockEntityMixin:25` | `LevelChunk` | V/N: yes |
| `PerStateBlockEntityMixin:31` | `BlockBehaviour$BlockStateBase` | V/N: yes |
| `PistonPushabilityMixin:29` | `PistonBaseBlock` | V/N: yes |
| `Tc4ChunkRegenerationMarkerMixin:34` | `ChunkSerializer` | V/N: yes |
| `Tc4ParticleEngineMixin:13` | `ParticleEngine` | V/N: yes |
| `ParticleLevelAccess:8` | `Particle` | V/N: yes |
| `Tc4ParticleWorldRenderMixin:17` | `LevelRenderer` | V/N: yes |
| `TaintOverlayChunkLayerMixin:26` | `SectionCompiler` | V/N: yes |
| `OreAnisotropyMixin:15` | `TextureAtlas` | V/N: yes |
| `PositionBlockModelMixin:18` | `ModelBlockRenderer` | V/N: yes |
| `WandRenderContextMixin:17` | `ItemRenderer` | V/N: yes |
| `BoneBowHeldPoseMixin:19` | `ItemInHandLayer` | V/N: yes |
| `OuterLandsDimensionEffectsMixin:19` | `DimensionSpecialEffects` | V/N: yes |
| `TableOutlineMixin:15` | `LevelRenderer` | V/N: yes |
| `WandFocusKeyMixin:12` | `Minecraft` | V/N: yes |
| `MinecraftItemColorsAccess:9` | `Minecraft` | V/N: yes |
| `WarpFogMixin:9` | `FogRenderer` | V/N: yes |
| `WarpPostEffectsMixin:9` | `GameRenderer` | V/N: yes |
| `WardedHitParticleMixin:19` | `ParticleEngine` | V/N: yes |
| `ResearchPopupScreenMixin:11` | `Screen` | V/N: yes |
| `Tc4BiomeColorMixin:26` | `Biome` | V/N: yes |
| `ContainerScreenAspectRowMixin:32` | `Screen` | V/N: yes |
| `AbstractContainerScreenAccess:16` | `AbstractContainerScreen` | V/N: yes |

### Fabric production — 3 classes

Sources: `fabric/src/main/java/thaumcraft/fabric/mixin/`. Client row belongs to `thaumcraft-fabric-client.mixins.json`; compatibility rows belong to `thaumcraft-fabric-compat.mixins.json`, both in `fabric/src/main/resources/`.

| Mixin source:line | Target | Bytecode |
|---|---|---|
| `client/FabricRobeArmorLayerMixin:26` | `HumanoidArmorLayer` | V: yes |
| `compat/ArchitecturyFluidPatchMixin:17` | `FluidStackImpl$Pair` | V: yes |
| `compat/FabricExcavationRemovalMixin:27` | `ServerPlayerGameMode` | V: yes |

### NeoForge production — 9 classes

Sources: `neoforge/src/main/java/thaumcraft/neoforge/mixin/`. Client rows belong to `thaumcraft-neoforge-client.mixins.json`; other rows to `thaumcraft-neoforge.mixins.json`, both in `neoforge/src/main/resources/`.

| Mixin source:line | Target | Bytecode |
|---|---|---|
| `client/NeoForgePositionBlockModelMixin:20` | `ModelBlockRenderer` | N: yes |
| `client/NeoForgePureFluidMixin:50` | `BlockRenderDispatcher` | N: yes |
| `client/NeoForgeGearArmorMixin:44` | `HumanoidArmorLayer` | N: yes |
| `NeoForgeExcavationRemovalMixin:39` | `ServerPlayerGameMode` | N: yes |
| `NeoForgeFluidPureMixin:30` | `FluidPure` | N: yes |
| `NeoForgeFluidDeathMixin:17` | `BlockFluidDeath$DeathFluid` | N: yes |
| `NeoForgeDamageContainerMixin:31` | `LivingEntity` | N: yes |
| `NeoForgeLootPoolMixin:29` | `LootPool` | N: yes |
| `NeoForgeLootTableMixin:19` | `LootTable` | N: yes |

### Fabric client test — 23 classes

Config: `fabric/src/clientTest/resources/thaumcraft-client-smoke.mixins.json`. Sources: `fabric/src/clientTest/java/thaumcraft/fabric/clienttest/mixin/`.

| Mixin source:line | Target | Bytecode |
|---|---|---|
| `FocalXpPickupObserverMixin:13` | `ExperienceOrb` | V: yes |
| `FocalInheritedLifecycleMixin:11` | `Entity` | V: yes |
| `FocalLaterMobMixin:11` | `Mob` | V: yes |
| `BoreServerHeartbeatMixin:11` | `MinecraftServer` | V: yes |
| `BoreMachineTickObserverMixin:14` | `TileArcaneBore` | V: yes |
| `BoreAlchemyTickObserverMixin:13` | `TileAlchemyFurnace` | V: yes |
| `BoreInfusionStartObserverMixin:11` | `TileInfusionMatrix` | V: yes |
| `BoreItemTouchObserverMixin:11` | `ItemEntity` | V: yes |
| `BoreXpTouchObserverMixin:11` | `ExperienceOrb` | V: yes |
| `BoreEntityLifecycleObserverMixin:10` | `Entity` | V: yes |
| `BoreMobLifecycleObserverMixin:10` | `Mob` | V: yes |
| `BoreOrdinaryNodeTickObserverMixin:15` | `TileNode` | V: yes |
| `BoreNormalStopObserverMixin:11` | `MinecraftServer` | V: yes |
| `BoreMobileTickObserverMixin:13` | `ServerLevel` | V: yes |
| `BoreInheritedItemTickObserverMixin:12` | `ItemEntity` | V: yes |
| `BoreCustodyMenuObserverMixin:13` | `AbstractContainerMenu` | V: yes |
| `BoreAmbientDeathLootObserverMixin:13` | `LivingEntity` | V: yes |
| `BoreAmbientSpawnObserverMixin:14` | `Entity` | V: yes |
| `BoreItemManagerLifecycleObserverMixin:12` | `PersistentEntitySectionManager` | V: yes |
| `FocalInputIsolationMixin:12` | `MouseHandler` | V: yes |
| `BoreClientHeartbeatMixin:10` | `Minecraft` | V: yes |
| `BoreInputIsolationMixin:12` | `MouseHandler` | V: yes |
| `MultiplayerCensusMixin:26` | `ClientPacketListener` | V: yes |

### Fabric game tests — 10 classes

Sources: `fabric/src/gameTest/java/thaumcraft/fabric/testing/mixin/`. First two rows belong to `fabric/src/gameTest/resources/thaumcraft-bore-gametest.mixins.json`; remaining eight to `thaumcraft-gametest-selection.mixins.json` in the same resource directory.

| Mixin source:line | Target | Bytecode |
|---|---|---|
| `BoreNativeGameEventMixin:14` | `ServerLevel` | V: yes |
| `BoreRemovalObserverMixin:16` | `Level` | V: yes |
| `GameTestSelectionMixin:15` | `GameTestRegistry` | V: yes |
| `GameTestHelperMockPlayerMixin:30` | `GameTestHelper` | V: yes |
| `GameTestInfoMockPlayerMixin:11` | `GameTestInfo` | V: yes |
| `RecipeManagerAuditMixin:11` | `RecipeManager` | V: yes |
| `GameTestStateGuardMixin:10` | `GameTestInfo` | V: yes |
| `GameTestForcedChunkGuardMixin:11` | `ServerLevel` | V: yes |
| `GameTestRegionTicketGuardMixin:12` | `ServerChunkCache` | V: yes |
| `GameTestConnectedPlayerGuardMixin:13` | `PlayerList` | V: yes |

### NeoForge game tests — 6 classes

Config: `neoforge/src/gameTest/resources/thaumcraft-neoforge-gametest.mixins.json`. Sources: `neoforge/src/gameTest/java/thaumcraft/neoforge/testing/mixin/`.

| Mixin source:line | Target | Bytecode |
|---|---|---|
| `GameTestHelperMockConnectionMixin:27` | `PlayerList` | N: yes |
| `GameTestHelperMockPlayerMixin:30` | `GameTestHelper` | N: yes |
| `GameTestInfoMockPlayerMixin:11` | `GameTestInfo` | N: yes |
| `BoreNativeGameEventMixin:22` | `ServerLevel` | N: yes |
| `GameTestSelectionMixin:21` | `GameTestRegistry` | N: yes |
| `NeoForgeBreakPhaseMixin:33` | `ServerPlayerGameMode` | N: yes |
