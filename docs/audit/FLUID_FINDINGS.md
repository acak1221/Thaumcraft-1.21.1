# Fluid API audit — g18fluid, 2026-10-23

Scope: Fabric 1.21.1, baseline `a2b5bf92`, one hypothesis: fluid API adaptation loses, duplicates or misreports fluid. Counts below distinguish real fluid tanks from essentia/XP/node containers; essentia units are **not mB**. NeoForge is not tested.

## Inspected inventory

22 entries: 20 block variants, the pure bucket and Decanting cargo. Two real fluid tanks (crucible and Spa). Source inspection alone is not a conservation proof.

| # | Container / variant | Conservation / face inspection | Test / evidence | Fix |
|---|---|---|---|---|
| 1 | Crucible | **PROVEN failure:** canceled fill leaves1000mB; canceled drain leaves0 instead of1000mB | `fluidCrucibleAbortConserves`, `fluidCrucibleDrainAbortConserves`, `fluidBoundaryAmountsConserve` | Shared per-owner snapshot participant restores nested/outer aborts |
| 2 | Ordinary essentia jar | 64 essentia, top-only input/output; no fluid API | Reasoned-only: `TileJar` | None |
| 3 | Void essentia jar | 64 retained essentia; overflow intentionally destroyed, not fluid loss | Reasoned-only: `TileJar`, `JarContents` | None |
| 4 | Brain jar | XP, no fluid tank | Reasoned-only: `TileJarBrain` | None |
| 5 | Node jar | Node identity/aspects, no fluid tank | Reasoned-only: `TileJarNode` | None |
| 6 | Alembic | 32 essentia; public transport insertion refuses | Reasoned-only: `TileAlembic` | None |
| 7 | Reservoir | Mixed essentia; sided atomic endpoint, not fluid storage | Reasoned-only: `TileEssentiaReservoir` | None |
| 8 | Essentia tube | One essentia; open sides only | Reasoned-only: `TileTube` | None |
| 9 | Essentia valve | One essentia; original connection/raw-port distinction retained | Reasoned-only: `TileTube.isConnectable/canInputFrom/canOutputTo` | None |
| 10 | Filter tube | One essentia; separate filter marker, not a fluid patch | Reasoned-only: `TileTubeFilter`, `TileTube` | None |
| 11 | Restricted tube | One essentia; same raw storage implementation | Reasoned-only: `ThaumcraftTubes`, `TileTube` | None |
| 12 | One-way tube | One essentia; no fluid storage | Reasoned-only: `ThaumcraftTubes`, `TileTube` | None |
| 13 | Buffer | Eight total mixed essentia, not eight buckets | Reasoned-only: `TileTubeBuffer` | None |
| 14 | Bore base | Stockless Perditio suction; public add/take return zero | Reasoned-only: `TileArcaneBoreBase` | None |
| 15 | Bore top | Inventory/work owner, no fluid tank | Reasoned-only: `TileArcaneBore` | None |
| 16 | Arcane light lamp | No fuel/fluid storage | Reasoned-only: `TileArcaneLamp` | None |
| 17 | Growth lamp | Essentia-derived charges/reserve, not mB; private atomic draw | Reasoned-only: `TileEssentiaLamp`, `TileArcaneLampGrowth` | None |
| 18 | Fertility lamp | Essentia-derived charges, not mB; private atomic draw | Reasoned-only: `TileEssentiaLamp`, `TileArcaneLampFertility` | None |
| 19 | Arcane Spa | **PROVEN adapter failure:** fractional stock committed but not reported; automation excludes UP, direct original API ignores side | `fluidGolemFractionalFillConserves`, `fluidGolemFractionalDrainConserves`, `fluidBoundaryAmountsConserve` | Probe/abort then transfer exactly representable whole mB at golem boundary |
| 20 | Pure-fluid source | World source is a whole bucket, flowing cells cannot be captured | `PureBucketActions`, existing pure-fluid runtime checks; rerun status below | None |
| 21 | Pure bucket | Whole-container transactional storage, partial extraction refused | Existing `pureBucketParticipatesInActualNativeFluidTransactions`; rerun status below | None |
| 22 | Decanting cargo | Integer mB plus fluid ID; bucket =1000mB; loader boundary converts81 droplets/mB | New real-Spa adapter checks; existing `entropyQueuesTheFarthestConnectedWaterForLiquidGolems`; rerun status below | Same bridge fix as row19; not counted as a third defective container |

## PROVEN

Baseline production code plus new tests: **5 tests executed; 4 failed, 1 passed**. The failing assertions were:

| Defect | Exact baseline observation | Correct result |
|---|---|---|
| Crucible aborted fill | `aborted Fabric fill must leave0mB, actual=1000` |0mB retained after canceled1000mB fill |
| Crucible aborted drain | `aborted Fabric drain must leave1000mB, actual=0` |1000mB retained after canceled1000mB drain |
| Golem fractional fill into Spa | Reported0mB, actual tank increase80 droplets | Report0 and insert0; retain the80-droplet free space |
| Golem fractional drain from Spa | Reported999mB, actual decrease80999 droplets | Remove80919 droplets =999mB; leave80 droplets |

The mismatch is exactly **80/81mB** per fractional transfer, not an imprecise “about1mB”. Initial Spa fill fixture is404920 droplets (5000mB capacity minus80 droplets); drain fixture is80999 droplets (one bucket minus one droplet). This proves the real adapter misreports actual real-container changes; it does not claim that an artificial integer-only mock reproduces the golem.

Two defective container paths, two root-cause fixes, four red regression scenarios. No native Spa tank fill/drain implementation change is needed.

`fluidBoundaryAmountsConserve` already passed on the baseline. It exercises **every integer amount1..1000mB for crucible and1..5000mB for Spa**, simulated versus executed fill/drain, wrong-fluid rejection, accepted-versus-requested overflow, full rejection, empty `FluidStack`, detached input/output stacks, and advertised Spa face availability. The bridge fix then passed all3 selected fractional/boundary checks in `.tmp/g18-bridge-green.log` (`BUILD SUCCESSFUL`, all3 required tests passed).

Combined retest: **8/8 required server GameTests passed**, `BUILD SUCCESSFUL in32s` (`.tmp/g18-green.log`, lines445/471). This includes the five new checks plus existing real-Spa nested transactions/retired owners, pure-bucket item/tank transactions, and real Decanting golem AI. The last test checks after each source removal that golem cargo + crucible water equals exactly1000mB times the removed-source count, and checks final crucibles contain1000mB. One source pickup is1000mB; a golem's total carry capacity is material/upgrade dependent, not universally one bucket.

The final boundary expansion additionally covers native Spa amounts1,80,81,80999,81000,404999,405000 droplets and actual fill/drain attempts on all six faces. It checks caller stack immutability on both accepted and rejected fills. **Final11/11 required server tests passed and `:fabric:build` succeeded** (`.tmp/g18-final.log`, lines457/502/512,46s). Additional existing checks cover pure source pickup/placement/custody, Spa component identity/container quirks and pure-fluid flow. Rows20–22 therefore have executed behavioral coverage, not merely source inspection.

### Reproduction

Use Java21 and an isolated `GRADLE_USER_HOME`. Before **each** Gradle invocation remove `fabric/run-gametest/world`. Focus selection takes exact comma-separated test names, not a substring:

```sh
rm -rf fabric/run-gametest/world
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME="$PWD/.gradle-g18"
./gradlew :fabric:runGameTest -PgameTestFilter=fluidGolemFractionalFillConserves,fluidGolemFractionalDrainConserves,fluidCrucibleAbortConserves,fluidCrucibleDrainAbortConserves,fluidBoundaryAmountsConserve --console=plain
```

The final command also requested `:fabric:build` and appended these six exact test names to the filter: `spaUsesNativeFabricTransactionsAndRetiredOwners`, `pureBucketParticipatesInActualNativeFluidTransactions`, `entropyQueuesTheFarthestConnectedWaterForLiquidGolems`, `pureBucketPreservesOriginalSourcePickupPlacementAndCustody`, `spaPreservesOriginalContainersFluidPatchesAndInventory`, `purifyingFluidPreservesOriginalClassicFlowWardAndSaltExpiry`.

Red run: `.tmp/g18-red-exact.log`, lines423/453/484/518; all four baseline assertions reproduced. An earlier `-PgameTestFilter=fluid` was rejected by the harness before running tests and is **not** the red proof. Local logs/worlds/caches are intentionally uncommitted.

## Reasoned-only / limitations

Rows 2–18 have no mB fluid tank. Their essentia, XP, node, work and fuel semantics were inspected, not exhaustively runtime-tested in this lane. No claim of whole-system conservation follows from their classification. Arbitrary third-party fluid handlers, component-bearing golem cargo, reentrant direct crucible mutation/save during an open native transaction, client behavior and NeoForge parity remain outside the executed proof. The crucible snapshot fix is not claimed to provide Spa's stronger common-layer lease/persistence model. Existing comments cite the TC4 oracle; the retained legacy crucible source was read before gameplay changes.
