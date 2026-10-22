# Runtime robustness audit — h1runtime (2026-10-23)

Scope: targeted review, not a claim that every translated file or lookup is safe. Fabric/1.21.1 only. Work starts at `e79b8558`.

## Findings

| ID / severity | Location | Reachable state | Proof | Fix |
| --- | --- | --- | --- | --- |
| R1 / High (chunk-generation failure risk) | `common/src/main/java/thaumcraft/common/lib/world/WorldGenMound.java:223` (before: 222) | A selected mound passes its terrain samples and builds its two loot containers on a FEATURES worker. `lootState` reached through the region to `ServerLevel.getRandom()`, sharing the live world's non-thread-safe `LegacyRandomSource` with other workers/server ticks. | **Proven RNG ownership defect:** `moundGenerationRandomIsIsolated` exercises the actual factory against a generation-context access trap, with registered crate/urn blocks. **Failed before fix** at old line 222: `AssertionError: Mound escaped generation RNG through getLevel`; **passed after fix**, alongside existing `surfaceStructuresReachTheOverworld` natural-noise coverage (2/2 required GameTests passed). A standalone real-vanilla RNG contention probe also reproduced `ReportedException: Accessing LegacyRandomSource from multiple threads`. No natural-world race/crash reproduction claimed. | **Fixed:** use the supplied `WorldGenLevel.getRandom()`, which is region-owned during generation and remains the world stream for server-thread runtime generation. Preserve the separate feature RNG, one float per container, threshold 0.3 and rarity. |

R1's exception requires overlapping updates of the shared seed (a competing worker or server tick), not merely running one mound on a worker. The ownership violation is deterministic; the crash timing is not.

## Vanilla contracts inspected

Read the named Fabric merged 1.21.1 jar under `/home/cyberfox-kali/.gradle-leader/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/` using Java 21 `javap -c -p`:

- `WorldGenRegion`: constructor creates its own RNG; `getRandom` returns that field; `getLevel` returns the shared server level. Reviewed `getBlockEntity`, `getBlockState`, `getChunk`, and `ensureCanWrite`.
- `Level`: constructor initializes `random` through `RandomSource.create()`, separately from `threadSafeRandom`; `getRandom` returns `random`. An unavailable block state must not be conflated with a nullable block entity: block-state access has an air-state contract, while entity lookup may return null (including off-thread live-level calls).
- `RandomSource`, `LegacyRandomSource`, `SingleThreadedRandomSource`, `ThreadingDetector`: `create(long)` constructs `LegacyRandomSource`; failed atomic seed compare-and-set in `next` constructs the threading exception. Atomic storage does **not** make this implementation safe for competing callers.
- `ServerChunkCache`, `ChunkAccess`: inspected nullable/non-creating chunk access and block-entity contracts. No blanket insertion of null block-state checks.

## Review coverage

Counts overlap across areas; helper review does not imply every transitive dependency was reviewed.

| Area | Java files reviewed | Scope |
| --- | ---: | --- |
| World generation | 28 | All 11 `Feature` subclasses (the nested Outer Lands feature's placement/apply/decorations path); 17 helpers listed below, including a scoped chest-rule review. |
| Optional lookups | 15 | Ten networking classes below, `TileNodeStabilizer`, `TileVisRelay`, `TileNode` generated-initialization path, `WispBoltParticle`, and the `ThaumcraftOuterLands` nullable configuration helpers. Worldgen lookup review is counted separately above. |
| Lifecycle ordering | 4 | `MagicalTreePlacement`, `TileNode` generated initialization, stabilizer construction/current-owner check, relay construction/load/parent-change. |
| Packet handling | 20 | Ten codecs plus their ten receiver classes listed below. Not the entire packet catalogue. |
| Numeric edge cases | 12 | Ore feature, biome decorator, normal-node feature, node-generation reducer, biome adapter, two canopy generators, write-area rule, Shock/Excavation FX geometry, wisp bolt renderer, village tower chest-count reducer (`floorMod` handles negative draws). |
| Re-entry | 3 | Stabilizer tick; relay tick/interact/drop/event paths; node generated-initialization admission. |

Feature classes: `EldritchRingFeature`, `VillageBankerManager`, `ThaumcraftOreFeature`, `ThaumcraftTreeFeature`, `VillageWizardManager`, `ThaumcraftNormalNodeFeature`, `WorldGenMound`, `ThaumcraftTotemFeature`, `WorldGenHilltopStones`, `ThaumcraftBiomeDecorationFeature`, `OuterLandsProvider.OuterLandsMazeFeature`.

Helpers: `WorldGenWriteArea`, `WorldgenLootHelper`, `StructurePlacementRules`, `WorldGenBigMagicTree`, `WorldGenSilverwoodTrees`, `MagicalTreePlacement`, `NodeGeneration`, `NormalNodeBiomeAdapter`, `WorldGenGreatwoodTrees`, `WorldGenManaPods`, `BiomeColumns`, `OriginalBiomeVegetation`, `GreatwoodDungeon`, `AuraGenRules`, `VillageBankerHome`, `VillageWizardTower`, and `VillageWorldGenRules` (chest pool/count only).

Packet pairs: `WandArchitect`, `ShovelOrientation`, `VisNetFx`, `EssentiaFlowFx`, `ResearchTableEdit`, `WispZap`, `ShockFx`, `GrowthLampFx`, `ExcavationFx`, `InfusionFx` (`Payload` and `Networking`). Existing queueing, optional-player guards and bounded edit validation were retained. Wisp entity lookup/zero-length guards were checked in `WispBoltParticle`; malformed entity IDs simply find no entity.

## Concerns, unproven — not defects

1. **Mound footprint clipping:** a 19-block mound drawn at chunk-local 14 or 15 reaches local 32 or 33. Individual writes are refused outside the FEATURES write radius, but the structure reports success. A natural-generation shape/invariant test is needed before changing original placement or rejecting an entire mound. No fix made.
2. **Eldritch maze saved-data concurrency:** ring and Outer Lands features reach shared `MazeSavedData` during generation. The cell map is a `ConcurrentHashMap`, but multi-step range-check/store admission and saved-data lookup lifecycle were not fully validated here; no race or lost update demonstrated. No fix made.
3. **Essentia-flow appearance validation:** unlike VisNet FX, the flow receiver lacks equivalent range/loaded-chunk/color checks. No reachable renderer crash demonstrated; byte-delta wrapping is explicitly original behavior, not itself a defect. No fix made.
4. **Artificial low biome aura:** the node reducer uses `nextInt(aura / 2)` after quartering. The production adapter's minimum aura is 50, so real inputs remain valid. Zero/negative test-only inputs do not establish a reachable defect. No fix made.

## Verification limitations

Results: **1 finding, 1 proven by a before/after test, 1 fixed; 4 concerns remain unproven.** Targeted baseline JUnit: 50/50 passed. Post-fix focused Fabric server run: 2/2 passed (fresh world; 57.38 seconds of GameTests, 3m18s Gradle invocation). Final full common JUnit: **2,461 tests / 381 suites, zero failures/errors/skips**, 19 seconds. Commands and bootstrap limitations are recorded in `docs/analysis/2026-10-23-agent-h1runtime.md`.

No client/rendering, multiplayer, NeoForge, or full-port acceptance is claimed. A synthetic generation-context trap verifies ownership selection, not chunk-worker scheduling. Natural concurrent world-generation crash reproduction remains unperformed.
