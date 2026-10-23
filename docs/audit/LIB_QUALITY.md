# Shared library/world quality audit — c5lib

Baseline: `3340abe7`, branch `agent/c5lib`, 2026-10-23. **Review verdict: request changes to ownership and failure boundaries before treating the connective library as finished.** Most wire authority checks are careful; the recurring quality problems are global ownership, replicated adapters, and large transaction classes.

## Scope, counting and evidence

`find common/src/main/java/thaumcraft/common/lib -type f -name '*.java' | wc -l` = **360**. The requested sibling `common/world` does not exist: world code is `common/lib/world`, **82** files (included, not added twice). Final tree: **41,514 lines**. Paths below are relative to `common/src/main/java/thaumcraft/common/lib/` unless explicitly stated.

Java compiler-tree parsing, not grep alone, inventoried all declarations, catches, throws, array accesses and literals, including nested classes and comma-declared fields. Semantic conclusions below are source-review conclusions, **not proof by execution of all paths**. No Gradle, server/client smoke, multiplayer, or fault-injection suite was run. Private method reachability additionally screened method references; record components were not mistaken for unread fields.

| Checklist | Number inspected | Findings (grouped review issues) |
|---|---:|---:|
| 1 Static state | 252 candidate fields; 195 mutable/non-final/scope fields | 7 |
| 2 Network discipline | 36 payloads, 28 other network files | 3 |
| 3 Generation throw safety | 11 Feature subclasses; 82 world files; 22 explicit world throws, 115 `get` calls, 429 array accesses | 2 |
| 4 Duplication | 3,274 method bodies screened; 20 documented pairs/groups | 17 actionable; 3 intentional |
| 5 Size/responsibility | 360 files; 16 over 400 lines | 10 |
| 6 Keys/IDs/numbers | 788 literal key-call sites, 118 ResourceLocation factories, 11,365 numeric literals | 4 |
| 7 Dead code | 469 private non-constructor methods; 3,006 field/component declarations; 360 file-level classes | 4 unused-production classes; 0 confirmed dead private methods |
| 8 Error handling | 91 catches; 529 throws; 34 resource/corpus IllegalStateException sites | 8 |

**55 actionable grouped findings**, not 55 demonstrated gameplay defects. Counts overlap by checklist: a duplicated global throttle is relevant to both ownership and duplication. Severity: **High** = server/world lifetime or generation crash boundary; **Medium** = ownership/data-contract/reviewability risk; **Low** = maintainability/diagnostic debt. “Accepted” is not a finding.

Complete per-field static table: [LIB_QUALITY_STATE.md](LIB_QUALITY_STATE.md). Complete location ledger: [LIB_QUALITY_SITES.tsv](LIB_QUALITY_SITES.tsv). The latter includes every numeric occurrence (including harmless indices, zero/one, named initializers and template cells), every literal-key call, every ResourceLocation factory, every world/library array/get site and every catch/throw. `KEY` deliberately includes JSON/map calls as well as NBT: it is an over-inclusive audit surface, not an assertion that all 788 are NBT keys. Dynamic keys/constants are not misreported as string literals. Site coverage is exhaustive for these syntactic categories; semantic duplicate discovery is not claimed mathematically exhaustive.

## 1. Static mutable state

**Inspected: 252 candidates, 57 immutable collection tables excluded = 195 state fields.** Breakdown: **82 non-final fields**, plus **113 final-but-mutable fields**: 49 arrays, 29 ThreadLocals, 33 mutable collections (including two shallow-immutable lists of arrays), 2 atomic diagnostic counters. Final does not make an array/list element immutable. The full table records every candidate and its disposition.

### Static-state table (ownership summary; individual fields in linked table)

| Sites / fields | Owner and review disposition |
|---|---|
| `research/ObjectAspectResolver.java:62` `INDEXES` | **S1 High.** One static recipe cache is used by client overlays and server resolution. `index` is synchronized (`:172`) and keys by RecipeManager, so this is **not** an observed race or cross-manager result reuse. It still violates the requested side-owned-cache standard and serializes unrelated owners. Move it onto an explicit manager/side lifecycle. Client callers include `common/src/main/java/thaumcraft/client/gui/HoveredSlotAspectRow.java:97` and `client/lib/ScanOverlay.java:101`. |
| `utils/Utils.java:54,364-378` `effectBuffer` | **S2 High.** Public process-global throttle keyed by dimension+position, not world instance. No clear/unload use found. A replacement save at the same dimension/position inherits suppression; entries expire only when revisited. Unlike `network/VisNetFxNetworking.java:22,65-73`, it has no level/server cleanup. |
| `utils/BlockUtils.java:34-38,196-216,229-244` four cursor fields | **S3 Medium.** Recursive greedy search stores an invocation's cursor in global mutable fields. Caller-facing `findBlocks` does not reset it; nested/concurrent/client/server calls can interfere. This is not a registry or documented single-thread state. Use a local search object while preserving traversal order. Not a cheap API-preserving edit. |
| 27 public array fields plus two public lists of arrays, e.g. `mods/ChampionModifier.java:64`, `world/DungeonRooms.java:56,70`, `world/EldritchBlockRules.java:128-130`, `world/VillageWorldGenRules.java:526-582`; `world/StructurePlacementRules.java:103,106` | **S4 Medium.** Mutable rule/template arrays, including shallow-immutable lists of arrays, escape publicly. Any addon/test can change future generation/rules, including array lengths and index invariants. Private arrays were screened separately; no post-init writes found for those. Prefer records/value lists/accessors, not public backing arrays. |
| `WarpEvents.java:35`; `runic/RunicSettings.java:13` | **S5 Medium.** `wussMode` and `current` are process-global configuration, neither a registry nor scoped to a server instance. `volatile` on the runic reference addresses visibility, not independent server lifetime. Document and encode server-start/reset ownership; do not blindly make final or reuse client settings. |
| `utils/InventoryUtils.java:46,61`; `utils/CropUtils.java:30-36`; `world/biomes/BiomeHandler.java:47,86`; `world/NodeEcologyPolicy.java:12-13` | **S6 Medium.** Registries are legitimate statics, but mutation/freeze phases are not encoded. Public lists/maps and unsynchronized registration APIs allow late writes while world/client readers iterate. State the init-only contract or publish immutable snapshots. ConcurrentHashMap alone does not supply coherent multi-setting snapshots. |
| 53 bootstrap flags and 11 installation/config callbacks; owner-keyed state such as `ArcaneEarNotes.java:14`, `network/AccessoryNetworking.java:17`, `utils/AspectOrbQueue.java:32` | **S7 Low.** Registration flags are process registries, not world caches, but installation-thread and retry-after-partial-init rules are mostly implicit. Some server maps assert thread ownership at use sites, others rely on event wiring. Document the contracts locally rather than blanket-marking the fields volatile. Full table distinguishes these from actual unscoped cache findings. |

Positive checks: `events/ChunkRegeneration.java:74,145-149` is concurrent and explicitly cleared per world/server; the old queue-unload issue is not reasserted. `bore/BoreNativeContext.java:33-38` has owner-keyed revocation registries and ThreadLocal scopes, not a single cached static level. `world/WorldGenBigMagicTree.java:16-37` explicitly documents and synchronizes world-scoped shared height state. Diagnostic atomic counters have process lifetime documented. ThreadLocals are not automatically a defect, nor do weak keys alone establish a lifecycle contract.

## 2. Network payload discipline

**Inspected: all 36 payloads and all 28 registration/handler companions.** All encode/decode field sequences agree for valid constructor inputs. All production receivers specify C2S/S2C and queue work. No new wrong-side registration or client-supplied material/price/world/player authority found in this source review. This is not a real-wire test claim.

`u8/s8/u16/s16/vint/i32/i64` below denote widths/signedness; tuples list wire order. Each site is the codec start line.

| Payload site | Direction; encode = decode contract / authority |
|---|---|
| `network/AccessoryContentsPayload.java:15` | S2C; exactly four optional ItemStacks; client attachment snapshot only |
| `network/AccessoryOpenPayload.java:12` | C2S; zero bytes; `AccessoryNetworking.java:33-38` derives live actor/menu |
| `network/AspectDiscoveryPayload.java:11` | S2C; UTF128; notification only |
| `network/AspectPoolPayload.java:11` | S2C; UTF128,s16,s16; feedback, not server knowledge |
| `network/BlockFxPayload.java:36` | S2C; dimension,vint,BlockPos,vint,Vec3 doubles,Vec3 doubles,i32; visuals |
| `network/BlockSparkleFxPayload.java:29` | S2C; dimension,BlockPos,i32; visuals |
| `network/EssentiaFlowFxPayload.java:27` | S2C; dimension,BlockPos,s8,s8,s8,i32; deliberate original byte wrapping |
| `network/ExcavationFxPayload.java:15` | S2C; dimension,caster vint,two Vec3,BlockPos,crack vint,three booleans; no harvest authority |
| `network/FocalManipulatorAckPayload.java:16` | S2C; u8,UUID,i64,u16,bool,i64; 36-byte exact frame, canonical bool |
| `network/FocalManipulatorStartPayload.java:17` | C2S; u8,UUID,i64,u16; 27-byte frame; `FocalManipulatorNetworking.java:45-59` validates actual menu/session/revision |
| `network/FocusSelectionPayload.java:17` | C2S; hand byte, signed slot OR marker127+u16; 2/4 bytes; actual inventory/pouch source and full plan verified in `FocusSelectionNetworking.java:54-145` |
| `network/GolemBellResetPayload.java:14` | C2S; hand u8; real held bell only (`GolemBellNetworking.java:21-26`) |
| `network/GrowthLampFxPayload.java:13` | S2C; dimension,BlockPos; visuals |
| `network/GuardianSonicPayload.java:11` | S2C; i32 source; visuals |
| `network/HoverTogglePayload.java:12` | C2S; empty; `HoverNetworking.java:10-11` delegates to server-owned equipped/fuel checks |
| `network/InfusionFxPayload.java:14` | S2C; dimension,BlockPos,u8 event,BlockPos,vint; no crafting request |
| `network/KnowledgeSyncPayload.java:14` | S2C; NBT; defensive copy on construction and accessor |
| `network/OpenThaumonomiconPayload.java:12` | S2C; unit/empty; authorized UI action |
| `network/ResearchNoteRequestPayload.java:14` | C2S; UTF128; actual server note issuance/inventory (`ResearchNoteRequestNetworking.java:29-53`) |
| `network/ResearchNoticePayload.java:11` | S2C; UTF256; notification |
| `network/ResearchPurchasePayload.java:14` | C2S; UTF128; server knowledge/costs/prerequisites (`ResearchPurchaseNetworking.java:30-49`) |
| `network/ResearchSettingsPayload.java:12` | S2C; five flag bits,vint difficulty,vint cap; server config only; disconnect reset is wired |
| `network/ResearchTableCombinePayload.java:19` | C2S; sized body(menu vint,session i64,revision i64,two bounded aspect UTFs); server plan/bonus ownership |
| `network/ResearchTableCopyPayload.java:14` | C2S; sized body(menu vint,session i64,revision i64); actual paper/dye/note supplies, no supplied item |
| `network/ResearchTableEditPayload.java:22` | C2S; sized body(menu vint,session i64,revision i64,action u8,q s8,r s8,conditional aspect UTF); menu/revision/ink/RP checked server-side |
| `network/ResearchTableStatePayload.java:21` | S2C; sized body(menu vint,session i64,revision i64,count vint,UTF+vint rows); duplicate/canonical pools checked |
| `network/RunicChargePayload.java:11` | S2C; i32,s16,s16; HUD feedback |
| `network/RunicHitPayload.java:11` | S2C; i32,i32; visuals |
| `network/ScanRuneFxPayload.java:24` | S2C; dimension,three doubles,vint,float; visuals |
| `network/ShockFxPayload.java:15` | S2C; dimension,vint,two Vec3,i64,bool,two optional(bool+Vec3); no damage authority |
| `network/ShovelOrientationPayload.java:12` | C2S; empty; actual held shovel/live player (`ShovelOrientationNetworking.java:27-34`) |
| `network/VisNetFxPayload.java:15` | S2C; dimension,six i32 coordinates,i32 color; intentional unpacked positions |
| `network/WandArchitectPayload.java:12` | C2S; empty; actual held wand/focus and maximum area (`WandArchitectNetworking.java:24-36`) |
| `network/WarpEventPayload.java:9` | S2C; s16 event 0..2; effects |
| `network/WarpMessagePayload.java:11` | S2C; i32,s8; feedback |
| `network/WispZapPayload.java:11` | S2C; i32,i32; visuals |

Findings:

- **N1 Medium — `network/AccessoryContentsPayload.java:18`.** Constructor copies stacks, but the synthesized `stacks()` accessor exposes those mutable ItemStack objects. Snapshot immutability is weaker than `KnowledgeSyncPayload.java:27`. Return defensive copies or explicitly document ownership transfer; no wire authority bypass established.
- **N2 Low — `network/ResearchPurchasePayload.java:14`, `ResearchNoteRequestPayload.java:14`, `OpenThaumonomiconPayload.java:12` and other simple codecs in the table.** **22/36** payloads do not explicitly reject trailing bytes; **14/36** enforce a complete frame. Valid encode/decode parity holds, but frame policy is inconsistent, including C2S UTF intents. Centralize a policy/test, do not infer exploitability from this alone.
- **N3 Low — `network/ScanRuneFxPayload.java:34-38`, `BlockSparkleFxPayload.java:36-39`.** Validation contracts differ: rune positions/duration validated but gravity may be NaN/infinite; sparkle accepts arbitrary color despite sibling `BlockFxPayload.java:60-61` checking it. Server-owned visuals, not an authority flaw; tighten with caller/fixture review rather than incidental behavior changes here.

## 3. World-generation throw safety

**Inspected: 11 features (10 top-level, 1 nested), all 82 world files.** The complete site ledger lists **22 explicit throw sites**, **115 `.get`/`get` calls**, **429 array accesses** in this package. The latter includes non-generation player tools; no site is hidden by selecting only filenames containing Feature. Supplier `.get()` is not confused with Optional `.get()`; **no Optional.get() was found in world generation**.

Feature coverage: `ThaumcraftOreFeature`, `ThaumcraftTreeFeature`, `ThaumcraftNormalNodeFeature`, `ThaumcraftTotemFeature`, `ThaumcraftBiomeDecorationFeature`, `WorldGenMound`, `WorldGenHilltopStones`, `EldritchRingFeature`, `VillageBankerManager`, `VillageWizardManager`, `OuterLandsProvider.OuterLandsMazeFeature`.

### Throw/reachability decisions

| Explicit throw site(s) | Generation reachability and disposition |
|---|---|
| `world/BiomeColumns.java:77` | **W1 High: reachable if target biome registration is absent.** Region `set` throws after optional holder lookup, aborting the feature/chunk. Validate indispensable biome resources at bootstrap or refuse the optional feature without throwing after chunk writes. `:61` is its live-ServerLevel equivalent, not another parallel-region site. |
| `world/OuterLandsProvider.java:153` | **W2 High: reachable after malformed saved maze data.** Load correctly preserves rejected NBT, but `MazeSavedData.cell`/`mazesInRange` throw through `OuterLandsMazeFeature.place` (`:180-181`) and `EldritchRingFeature.placeSelected` (`:81-82`). The preflight catches this; the actual ring does not. Keep quarantine, expose an explicit unavailable result, and skip generation. This is one throw site with two consumers. |
| `world/BiomeColumns.java:25,49` | Thread/context preconditions: normal native WorldGenRegion dispatch does not satisfy the throwing conditions. Alternate/mock WorldGenLevel callers can throw; not counted as reachable exceptional-data failures in native region generation. |
| `world/GreatwoodDungeon.java:53`; `WorldgenLootHelper.java:70` | Weighted-selection consistency assertions. With the fixed positive descriptors supplied by `ThaumcraftLoot.dungeonEntries`, positive total and canonical RNG, a ticket must select a row. No current input path to these throws found. `addExact`, `nextInt(total)` and chest size also rely on those fixed invariants. Duplicated logic is D8, not a demonstrated chunk crash. |
| `world/NodeGeneration.java:79` | Wrong-thread ServerLevel guard; normal worker uses WorldGenRegion, normal runtime uses server thread. Not a native generation failure with valid contexts. |
| `world/OuterLandsProvider.java:92,96,99` | Three malformed-load throws are inside the loader's retaining catch. They do not escape load; later `requireReadable` is W2. |
| `world/FamilyVariant.java:76,83` | Missing property guards. Registered generation block families supply variant properties; misuse/addon blocks can throw, not found in current feature paths. |
| `world/SparseIntegerProperty.java:40,45` | Invalid property construction; bootstrap failures, not data-dependent chunk generation. |
| `world/ExcavationHarvest.java:84,187,278`; `NodeHarvest.java:129`; `TradeSwaps.java:110`; `WardingOverlay.java:26,30` | Seven runtime tool/visual contract throws/rethrow, not called by the Feature generation graph. |

**Reachable exceptional-data explicit throw sites in native generation: 2.** This excludes ordinary VM failures, missing mixins, broken bootstrap, callbacks in other packages, and deliberate wrong-thread direct test calls. It does not claim native `setBlock` or registry APIs cannot throw.

### `get` / array invariant review

| Sites (all individual expressions/lines in TSV) | Reachability decision |
|---|---|
| `ThaumcraftOreFeature.java:89,95,109,149` | Registered suppliers must exist; variant always 1..6 from bounded RNG/closed BIOME_TYPES. `types` nonempty before indexed draw. Missing native registration remains a bootstrap invariant, not unchecked Optional access. |
| `MagicalTreeBiomeAdapter.java:54`, `NormalNodeBiomeAdapter.java:53`, `NodeGeneration.java:53-55` | Type lists empty-checked or fallback; primal/compound pools require canonical aspect bootstrap. Default biome aura remains positive even quartered; do not extend adapter registration with nonpositive aura without a bound check. |
| `VillageBankerManager.java:65-69`, `VillageWizardManager.java:65-69`, `VillageWizardTower.java:98` | `houseIndex` derives index from plan; house slot/list bounds stay together. Tower fetches last content only after adding content. Public invalid helper inputs remain caller preconditions. |
| `MazeGenerator.java:178-188,258-259,310,319` | Direction list populated before get(0), random indices use matching list sizes after nonempty checks. Grid coordinate checks and dimensions constrain internal generated cells. Public `grid` is still a mutable representation, so external corruption is not proven safe. |
| `WorldGenBigMagicTree.java:100-125,154-227`, `WorldGenGreatwoodTrees.java:96-121,149-227` | Private vectors length3, leaf nodes length4, axis selected from 0..2 and OTHER_COORDS length6; leaf-node capacity follows positive bounded height/count. No current canonical RNG overrun found. Repeated algorithm is a maintenance hazard, not fixed here. |
| `DungeonRooms.java:56-83`, `OuterLandsProvider.java:139-140,240-252`, `WorldGenMoundTemplate.java:50-468`, `OuterLandsBossRooms.java:88` | Fixed template dimensions and producer-created coordinate tuples justify current indices. **Not robust to public-array mutation** (S4); no unconditional safety claim for addon/test-corrupted tables. |
| `WorldGenWriteArea.java:35`; `MagicalTreePlacement.java:94-104` | Write-area accessor requires installed mixin; missing knot BE is already skipped/counted, not thrown. Keep both safeguards. |

The site ledger is a location-complete screen, not a formal range proof for every access. Canonical data/current call graphs were reviewed; corrupted public arrays, absent registrations, and native callbacks need targeted fault tests before stronger safety claims.

## 4. Duplication

**Inspected: 3,274 method bodies; 20 pairs/groups documented, 17 actionable.** Exact-body matching (whitespace-normalized, >140 non-whitespace characters) found five clone groups; semantic review adds adapters, schemas and replays. Preserve original RNG draw order and distinguish fresh-generation from retrogen seeds.

| ID / severity | Pair/group (`file:line`) | Review action |
|---|---|---|
| D1 Accepted | `events/ChunkRegeneration.java:158` ↔ `world/StructurePlacementRules.java:137` | Two chunk-seed derivations intentionally differ: shifted retrogen seeds vs fresh seed. Do **not** merge them as if equivalent. |
| D2 Accepted | `research/ResearchNoteIssuance.java:52` ↔ `research/UnknownKnowledge.java:41` | Random adapters replay native draws into java.util.Random interface; separate authority paths. Keep separately tested. |
| D3 Medium | `world/EldritchRingFeature.java:55,105` | Actual placement and preflight replay same position/size/terrain gates; drift changes totem suppression. Extract a read-only result without changing draw order. |
| D4 Low | `world/WorldGenBigMagicTree.java:128` ↔ `world/WorldGenGreatwoodTrees.java:123` | Exact `layerSize` body. |
| D5 Low | `world/WorldGenBigMagicTree.java:154` ↔ `world/WorldGenGreatwoodTrees.java:149` | Exact `generateLeafNodeBases` body. |
| D6 Medium | `world/WorldGenBigMagicTree.java:186` ↔ `world/WorldGenGreatwoodTrees.java:186` | Exact block-line algorithm; shared vector/axis bounds logic. |
| D7 Low | `network/ExcavationFxPayload.java:41` ↔ `network/ShockFxPayload.java:60` | Exact finite/world-coordinate validator. One named geometry contract would prevent drifting limits. |
| D8 Medium | `world/GreatwoodDungeon.java:46-57` ↔ `world/WorldgenLootHelper.java:57-81` | Weighted dungeon descriptor selection, count, split and slot-overwrite rules duplicated. Preserve distinct random source/custody adapters. |
| D9 Medium | `utils/Utils.java:370-381` ↔ `network/VisNetFxNetworking.java:54-63` | Same wall-clock random throttle and player broadcast; one is owner-scoped, the other not. |
| D10 Low | `world/ElementalShovelHarvest.java:54` ↔ `world/StreamAxeHarvest.java:93` | Exact `realActor` admission predicate. |
| D11 Low | `world/GreatwoodDungeon.java:63` ↔ `world/OriginalBiomeVegetation.java:19` ↔ `world/WorldGenCustomFlowers.java:43` | Equivalent build-height/loaded/write-area checks, parameter names differ. |
| D12 Low | `AdvancedFurnaceFormation.java:56` ↔ `infusion/InfusionStructure.java:125` | Existing loaded-chunk admission implemented twice; consolidate only common no-load predicate, not whole mutation policy. |
| D13 Low | `world/VillageBankerManager.java:59-82` ↔ `world/VillageWizardManager.java:59-77` | House selection, seed, rotation, centered footprint and ground placement repeated. Separate building-specific draws. |
| D14 Medium | `world/NormalNodeBiomeAdapter.java:21` ↔ `world/MagicalTreeBiomeAdapter.java:18` ↔ `world/biomes/BiomeHandler.java:49-80` | Same original type/aura/aspect/greatwood facts represented in multiple tables. Ore adapter (`ThaumcraftOreFeature.java:37`) adds a fourth related projection. |
| D15 Low | `world/StructurePlacementRules.java:39-106` ↔ `world/VillageWorldGenRules.java:511-582` | Structure chances, radii, sample arrays and ground lists have separate definitions. |
| D16 Low | `research/ResearchBrowserData.java:16` ↔ `research/ResearchRegistrationOrder.java:14` | Research category order repeated. |
| D17 Low | `network/ResearchPurchasePayload.java:11-17` ↔ `network/ResearchNoteRequestPayload.java:11-17` | Same bounded key transport/domain, separately maintained. Keep payload IDs separate. |
| D18 Medium | `research/PlayerKnowledge.java:193-198` ↔ `:223-225` ↔ `:243-246` | Aspect-pool row schema (`key`,`amount`) hand-repeated across serialization, restoration and validation. This is not the API AspectList schema; don't conflate them. |
| D19 Low | `network/ResearchTableCombineNetworking.java:69` ↔ `network/ResearchTableCopyNetworking.java:65` ↔ `network/ResearchTableEditNetworking.java:130` | Same authoritative correction pattern; separate implementations can omit knowledge/menu acknowledgement. |
| D20 Accepted | `world/WorldGenMound.java:83,152` ↔ `world/WorldGenHilltopStones.java:52,101` | Independent configured feature passes intentionally recreate the exclusive chain. Hoisting the seed alone is insufficient; merging passes changes draw ownership. |

## 5. Class size and responsibility

**Inspected: 360 files, 16 above 400 lines, 10 decomposition findings.** Counts are physical lines, including comments/data. Large declarative catalogues are not automatically bad design.

| Class (`:1` unless noted) | Lines | Responsibilities / disposition |
|---|---:|---|
| `bore/BoreNativeContext.java` | 441 | **Medium:** world/owner revocation, dispatch scopes, snapshots and inventory observation. Split context ownership from observation values. |
| `bore/BoreWork.java` | 709 | **Medium:** tick admission, reservation, native removal, output routing, repair and retirement recovery. Transaction phases need explicit collaborators. |
| `essentia/EssentiaTransfers.java` | 1,256 | **Medium:** tube/jar/reservoir/centrifuge/golem/Bore transfers, endpoint selection, raw mutation scopes, notification failure aggregation. Several independently reviewable protocols. |
| `items/EquipmentRules.java` | 605 | Accepted related family specifications: attributes, recipes/metadata, durability/tool values; mainly cohesive rule data. |
| `mods/ChampionRules.java` | 441 | Accepted champion eligibility/tiers/modifier selection/visual callback; related policy surface, not size-only defect. |
| `research/ObjectAspectResolver.java` | 483 | **Medium:** recipe indexing, graph resolution, dynamic item/potion/container tags, equipment/enchantment bonuses, capping/culling. Separate index ownership and graph reducer. |
| `research/ResearchRecipeCoverage.java` | 676 | **Low:** runtime gate lookup, corpus-to-native mappings, recipe resource discovery and validation/reporting. Build-time coverage tooling mixed with runtime policy. |
| `utils/BlockUtils.java` | 456 | **Medium:** destructive harvest/drop/follow logic, greedy traversal, raytrace, neighborhood queries. “Utils” hides independent authority/state contracts. |
| `utils/DeferredItemDrops.java` | 985 | **Medium:** paid-output journal, Bore retirement custody, geometry, spawn receipts/retries, player/machine adapters, NBT quarantine/save. Split persisted format from delivery/custody state machine. |
| `utils/EntityUtils.java` | 416 | Accepted entity targeting, champion modifiers and effect mappings; related entity utility domain, no mandatory split based on size alone. |
| `utils/InventoryUtils.java` | 646 | **Medium:** ore registry, insertion, consumer inventory rules, damage/unbreaking, matching. Registry publication should not sit inside inventory mutation helpers. |
| `world/DungeonRooms.java` | 1,309 | **Low:** shared builder/view plus portal, boss, key, nest, library and passage room generators. Room strategies can be separated while sharing a typed placement builder. |
| `world/EldritchBlockRules.java` | 1,222 | Accepted original multi-block-family declarative rule catalogue; public-array encapsulation is S4, not another size-only defect. |
| `world/OuterLandsBossRooms.java` | 580 | Accepted related boss-room geometry/runtime wiring and boss-map state; future extraction useful, no mandatory size finding. |
| `world/VillageWorldGenRules.java` | 792 | **Low:** village planning/seeding/rotation, two house layouts, trade data and unrelated hilltop/ring compatibility rules. Split village and structure/trade projections. |
| `world/WorldGenMoundTemplate.java` | 468 | Accepted fixed declarative template, one responsibility; not a god class. |

## 6. Magic numbers and string keys

**Inspected syntactically: 788 literal key calls, 118 ResourceLocation factories, 11,365 numeric occurrences (465 distinct numeric spellings).** Every location/value is in the TSV, including ordinary harmless values; these counts are not 12,271 defects. Canonical TC4 numbers are not changed merely because unnamed. Dynamic NBT keys such as chunk regen's configured key are intentional.

- **K1 Medium — repeated storage schemas.** `research/PlayerKnowledge.java:180-260` repeats `key`, `amount` and knowledge field keys across save/load/type validation; `utils/AspectOrbQueue.java:165-205` repeats identity/vector/aspect/value keys; `utils/DeferredItemDrops.java:730-979` repeats the larger journal schema. Hoist each format's keys once; do **not** globally alias unrelated formats which happen to use `Id`/`Count`. **Cheap fix applied:** `world/OuterLandsProvider.java:76-79` now names four maze keys (`cells`,`x`,`z`,`cell`) once, used by save/load/validation; byte types and spelling unchanged. Other schemas remain.
- **K2 Low — repeated resource identity spellings.** `world/OuterLandsProvider.java:53-56,190`, `world/OuterLandsDimensionRules.java:34-40` repeat Outer Lands dimension identity; `network/ResearchPurchaseNetworking.java:38` and `ResearchNoteRequestNetworking.java:42` rebuild the same `thaumcraft:learn` event while `ResearchTableEditNetworking.java:24` has its own named event. Prefer registry/domain-owned IDs. Hardcoded valid namespace/path factories in the full ledger are not inherently bad; repeated meaning is the issue.
- **K3 Low — wire/game event tags lack one vocabulary.** `network/InfusionFxPayload.java:28-29` accepts event0..7 and event4 special data, and `WarpEventPayload.java:10` accepts event0..2. Raw numeric event semantics are duplicated at producers/consumers. Named stable protocol constants improve review without changing bytes.
- **K4 Low — unnamed original arithmetic/geometry.** `world/NodeGeneration.java:42-74` embeds node type rolls, aura multipliers and environment thresholds; `world/WorldGenBigMagicTree.java:97-120` and Greatwood counterpart repeat slope/density factors; `world/VillageWorldGenRules.java:163` embeds a seed salt. Name canonical values within their domain with oracle references, not blanket replace every 0/1/index/template cell. Full numeric ledger makes residual unnamed sites searchable rather than pretending a few examples are the whole surface.

## 7. Dead code

**Inspected: 469 private non-constructor methods and 3,006 field/component declarations, plus external production references for all 360 file-level classes.** No confirmed caller-free private method or write-only private non-record field survived local/member-reference screening. “No confirmed” is not interprocedural proof. Records' synthesized accessors and reflective/mixin contracts were not deleted.

Four classes have **no external main-source reference** across common/Fabric/NeoForge or resource references found by name. Retained compatibility surface may be intentional; removal needs an API decision, not a cheap private deletion:

| Finding / severity | Site | References outside own file |
|---|---|---|
| DC1 Low | `RefGui.java:1` | Only `SharedInfrastructureParityTest.java` |
| DC2 Low | `FakeThaumcraftPlayer.java:1` | None found |
| DC3 Low | `utils/UUIDHelper.java:19` | Only `LegacyUtilsTest.java` |
| DC4 Low | `mods/ChampionModDummy.java:1` | Only `ChampionModifierTest.java` |

Mark as compatibility-only or remove deliberately; tests proving an unused shim works do not establish production integration. No public class was deleted in this audit.

## 8. Error handling

**Inspected: all 91 catches and all 529 throw statements.** **34 IllegalStateException sites in seven resource/corpus loader classes** were traced/classified; all locations and messages are in the TSV. Fail-fast bootstrap validation is appropriate; failing lazily during a gameplay query is not equivalent. The inventory retains accepted rethrows, wrapped decoder failures and quarantine catches so “swallow” does not mean every catch returning a result.

| Finding / severity | Site(s) | Review issue / recommendation |
|---|---|---|
| E1 Low | `utils/UUIDHelper.java:43,67,83` | Broad Exception silently turns network/JSON failures and programming defects into not-found. Narrow expected exceptions; optional debug diagnostic for remote failures. This code is currently compatibility-only (DC3), not chunk/network payload transport. |
| E2 Medium | `research/ResearchNoteIssuance.java:71` | Any RuntimeException during generation becomes INVALID_GENERATION without original diagnostic/cause. Expected invalid input should be distinct from reducer bugs; log unexpected exceptions without leaking paid resources. |
| E3 Medium | `world/AuraGenRules.java:99` | Broad RuntimeException converts structure lookup failures into no aura candidate. Expected absent structure may be fine; malformed registry/native defects need a diagnostic or typed unavailable result. |
| E4 Medium | `world/EldritchRingFeature.java:120` | Broad preflight catch returns false while actual ring path lets the same failure escape (W2). Error boundary and result semantics disagree. |
| E5 Low | `utils/AspectOrbQueue.java:205`; `utils/DeferredItemDrops.java:742,777` | Correctly retains malformed raw records, but silently loses rejection cause/row identity. Add rate-limited diagnostics or retained reason metadata; **not** an observed stock-loss bug. |
| E6 Medium | `utils/Utils.java:249,254` | Catching Throwable in reflection fallback can suppress VM/linkage/thread failures; inner catch logs outer failure rather than the final reflective failure. Narrow expected reflective/unsupported-operation exceptions and retain both causes. |
| E7 Medium | `crafting/SmeltingBonusCatalog.java:60-86`; `research/EarlyObjectAspectBindings.java:46-100`; `EntityAspectBindings.java:168-186`; `ObjectAspectCatalog.java:60-204`; `ResearchCatalog.java:30-72`; `ResearchRecipeCoverage.java:130,642,648`; `ResearchRegistrationOrder.java:37-49` | 34 resource/corpus invariant throws. Retain fail-fast required-resource checks, but establish a single eagerly validated loading phase with resource/row identity and consistent wrapping of JSON/runtime parse failures (several catch IOException only). Do not add a blanket “empty registry” fallback. |
| E8 Medium | `events/ChunkRegeneration.java:227-233` | Catch logs that terrain is kept, but earlier feature writes can already have happened; marks chunk unsaved and queue item is gone. This is partial completion, not rollback. Marker save can make failed regeneration appear finished. Document partial state and use an explicit failed/retry/quarantine status before claiming success. |

### Silent/fallback catch disposition table

These **17 catches** have no logging or propagation at that site (some return/retain a structured result). Other 74 catches propagate, retain a cause/result, aggregate, or log; full bodies in TSV.

| Sites | Disposition |
|---|---|
| `potions/FluxEffects.java:51` | Accepted optional loader API probe; missing method selects other platform path. |
| `research/ResearchNoteIssuance.java:71` | E2. |
| `research/ResearchNoteOverstackCodec.java:43`; `runic/RunicSettings.java:29` | Expected malformed numeric text fallback; diagnostics desirable if corrupt persisted counts matter, not silently relabeled valid parsing. |
| `utils/AspectOrbQueue.java:205`; `utils/DeferredItemDrops.java:742,777` | E5, data retained. |
| `utils/DeferredItemDrops.java:367` | Validation predicate for candidate ownership, safe false; not a save swallow. |
| `utils/EntityUtils.java:412` | Accepted optional effect ID mapping; invalid identifier => no effect. |
| `utils/UUIDHelper.java:43,67,83` | E1. |
| `utils/Utils.java:227` | Optional absent EE3 class probe; broad Exception could hide unexpected reflection defects (Low), included in E6 reflection-boundary finding. |
| `utils/Utils.java:266` | Accepted superclass field search, expected NoSuchFieldException. |
| `world/AuraGenRules.java:99`; `world/EldritchRingFeature.java:120` | E3/E4. |
| `world/SparseIntegerProperty.java:60` | Accepted string-property parse Optional.empty. |

## Fix and validation

Only source change: four maze storage-key constants in `world/OuterLandsProvider.java`. No RNG, gameplay value, stored schema, registration or exception-policy change. This is a partial K1 cleanup, not a claim that the rest of the key debt is fixed.

- Full **common main tree** compiled successfully with Java21 `javac -proc:none --release 21`, cached **remapped Minecraft 1.21.1** jar, access-widening overlay and remapped Architectury/dependencies, producing this worktree's `common/build/classes/java/main`. No other worktree or main checkout was accessed.
- Changed `OuterLandsProvider.java` independently recompiled against that remapped classpath **plus `common/build/classes/java/main`**, exit0.
- Temporary native-NBT check compiled and executed: populated negative-X maze row and empty list round-trip preserved original key spellings/types and lookup semantics, exit0. This is a small native-class test, **not** a dedicated-server generation test. Initial all-cache runtime classpath emitted an SLF4J version warning; rerun excluding the old SLF4J1.8 beta was clean.
- Logs: `/tmp/opencode/c5lib-bootstrap.log`, `c5lib-final-javac.log`, `c5lib-maze-test.log`. Temporary inventory/check tools stayed outside version control.

Left deliberately: side/server ownership redesign, mutable public API cleanup, feature unavailable-result policy, duplicated RNG-sensitive logic, class decomposition, remaining schemas, compatibility dead classes, and typed/logged error boundaries. Those are not cheap behavior-neutral fixes.
