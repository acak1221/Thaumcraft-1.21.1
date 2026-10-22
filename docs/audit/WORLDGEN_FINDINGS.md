# Generated-structure audit — g10world

Scope: one hypothesis, incomplete/misplaced generated structures. Fabric only. Source baseline `7008358c`.

## Method and scope limits

`moundBoundaryIsCompleteOrRefused` generates actual chunks with `ServerLevel.getChunk`, finishes all touched chunks, then prepares an explicitly synthetic stone surface at Y64. It invokes the actual mound feature with a native `WorldGenRegion` and native FEATURES step/write guard. A test subclass adapts only cache lookup to already-generated chunks (no loads). Blocks, loot, block entities and heightmaps are real; this is **not** asynchronous chunk decoration, and the mound is deliberately selected, not naturally discovered. No claim of full natural-world or client acceptance.

## Inspected catalogue (10 generators / feature groups)

| Generator/group | Generated in this audit | Finding / test / fix |
| --- | --- | --- |
| Mound | Two controlled placements, locals 13 and 15 on X/Z | PROVEN truncated success at local15; local13 spans four chunks and passes 2,440 template-cell checks, chest loot, both spawners, both urn/crates, node and surface-minus-nine checks. Whole-footprint preflight added; final post-fix run passes, including immediate feature replay without writes/refill. |
| Eldritch ring, altar and obelisk | No; source inspected | Radius-three footprint is clamped inside chunk; bounds/build-height and maze error concerns remain reasoned-only. |
| Hilltop stones | No; source inspected | Footprint clamped inside chunk; height gate differs from oracle (below). |
| Aura totem | No; source inspected | Single-column writes guarded; occupied cosmetic node block intentionally creates no node per original behavior. |
| Wild aura node | No; source inspected | Single-block writes guarded; bounded first-air scan, not surface heightmap. |
| Greatwood | No; source inspected | Uses `MagicalTreePlacement` planning before commit; no new runtime claim. |
| Silverwood | No; source inspected | Uses shared planning transaction and bounds checks; no new runtime claim. |
| Ore | No; source inspected | Single writes guarded, vanilla vein area availability checked; positive random bounds. |
| Flowers | No; source inspected | Each independent candidate checks available chunk, height and writable area. |
| Taint decoration | No; source inspected | Bounded first-air scan and chunk availability; no new runtime claim. |

## PROVEN

**One defect, one structure type.** On baseline `7008358c`, the native-region regression fails with:

> Mound crossing +2 chunks must refuse atomically, not report a partial structure as success; writes=2311

The accepted control at local13 first inspected **2,440 template cells**, plus the actual filled chest, both spawner block entities and the aura node at `surface-1`. It builds at `surface-9`, matching oracle `ThaumcraftWorldGenerator:424,432`. The local15 candidate reaches three chunk columns/rows; the same FEATURES radius-one guard silently drops the far cells while `placeSelected` returns true. The neighbouring chunk's own independent structure roll cannot finish this mound.

Pure arithmetic test `WorldGenWriteAreaTest.nineteenBlockMoundsCanReachTwoChunksAwayDespiteAnInChunkOrigin` enumerates all 256 local X/Z origins at positive, zero and negative chunk coordinates: **60/256 (23.4375%)** overrun the radius-one envelope. This is a fraction of candidate origins, **not** a measured natural-world defect rate (terrain may reject candidates first).

**Fix:** `WorldGenMound.samplesPass` now rejects an unwritable/unavailable footprint before terrain reads or any writes. It checks all footprint chunk availability, all build-height/write-area corners, and includes the possible TNT at base-minus-one. Sharing this predicate with `preflight` keeps aura suppression consistent. Ordinary four-chunk boundary crossings still build. This is an explicit native-platform deviation from the oracle's chunk-forcing writes: skip the entire unsafe candidate; do not clamp/move its chosen location, force-load neighbours, or publish a partial structure. Lower/upper build-height cases are guarded but not separately runtime-exercised.

**Loot:** mound and hilltop use `WorldgenLootHelper`; spider Greatwood uses `GreatwoodDungeon`. Both implementations consume `ThaumcraftLoot.dungeonEntries`, the explicit Forge 1.7.10 `dungeonChest` weighted descriptor, **not** a modern vanilla dungeon resource table. The generated control's chest actually contains items. Thus no missing resource-table lookup is involved in this chest path; no loot-table replacement was made here. Urn/crate break rewards are a separate path and were not exercised.

**Evidence:** `gradle-g10-red-native.log` (ignored local log); initial `gradle-g10-red.log` is discarded harness evidence only (a proxy was rejected by `BiomeColumns`, not a worldgen defect). `gradle-g10-green.log`: five placement-arithmetic JUnit tests and one focused dedicated-server GameTest pass, build successful. Final rerun with explicit urn/crate and immediate-feature-replay assertions also **passes** (`gradle-g10-final.log`, build successful in 39s). Machine timestamps report October22 UTC; this assignment uses the supplied October23 audit date.

Counts are **10 source-inspected groups, 1 runtime-inspected structure type, 2 deliberate initial placement attempts across 13 requested target chunks** (one complete and one partial on baseline; one complete and one clean refusal after fix), **1 PROVEN defect**, **0 demonstrated height misplacements**. Native generation may also generate surrounding dependency chunks; 13 is not a total server chunk count. Repeating the same fixtures in red/green runs is not counted as additional distinct structures.

## Reasoned-only / not fixed in this lane

* Hilltop stones: oracle `ThaumcraftWorldGenerator:425,450` calls them inside `surface-9 < seaLevel`, while modern `WorldGenHilltopStones.samplesPass` refuses that range. Original hilltop minimum Y85 makes the original branch ineffective at ordinary sea level; modern port appears intentionally/accidentally to enable high hilltops. No generated hilltop reproduction or fix here.
* Ring placement clamps X/Z away from boundaries rather than preserving the original drawn centre. Not demonstrated as missing blocks; no change.
* Ring `placeSelected` accesses maze saved data/generation without the `RuntimeException` guard used by `preflight`. A corrupt/unreadable maze could abort chunk generation (high severity if triggered); no corrupt-data fixture tested.
* Repeated native chunk requests and immediate actual feature replay preserve the chest; replay performs no writes and returns false. Forced retrogen after manual terrain restoration and save/reload persistence remain unverified.
* No ocean/desert/mesa runtime matrix, ring capstone/altar runtime inspection, natural mound seed search, or asynchronous native-region boundary test was performed.
* `NodeGeneration.plan` uses `nextInt(aura/2)`; an adapter returning aura below two would throw in a feature. Current biome mappings were not proven to produce such an aura; no claim of a reachable crash.
* Both dungeon loot implementations contain an unguarded descriptor-invariant exception and `Math.addExact`. Invalid/overflowing weights could abort chunk generation (high severity if reachable); no evidence that current fixed descriptors violate those invariants, and the actual mound loot invocation succeeds.
