# Vis / essentia conservation audit — g5vis

Scope: 17 transfer families inspected on baseline `96b79de1`. This is a bounded accounting audit, not a claim that every focus, machine, packet, addon or lifecycle interleaving has been exhaustively tested. Units are **essentia**, **whole ordinary-node vis**, or **centivis/raw energized-network quanta** as specified below; they must not be added together without conversion.

## Transfer ledger

`R` means reasoning-only, `T` a new direct numeric accounting check (9 entries), and `E` an existing numeric regression rerun here (7 entries). One family remains reasoning-only. Fixtures provide finite initial stock; production tickers/entry points perform the operation. A passing selected scenario is not exhaustive proof of its entire transfer family.

| # | Transfer path inspected | Conservation check / before → after | Oracle expectation | Test / fix |
|---|---|---|---|---|
| 1 | Item → crucible aspects | Measured coal2→1 gives ENERGY2/FIRE2; torches3→1 gives LIGHT2, then last torch gives LIGHT3 | `TileCrucible.attemptSmelt`: ceil(stack/2) items per collision, not the whole stack | E: `crucibleDissolvesAndCraftsWithOriginalCosts`; no fix |
| 2 | Crucible recipe → result | Measured dust1→0, ENERGY/FIRE/LIGHT each−3, pending output+1→one Nitor item, water1000→950 | Original recipe removes matching aspects and one catalyst; low positive water is sufficient | E: same test and `TileCrucible:318-341`; no fix |
| 3 | Item → alchemy-furnace buffer | Measured coal1→0; buffer0→4 (FIRE2/ENERGY2); further input refused at buffer50 | Original `TileAlchemyFurnace.smeltItem`; no input-container refund | E: `alchemyFurnaceDistillsIntoRealAlembics`; no fix |
| 4 | Furnace → alembic | **Measured baseline `(4,0,0)→(4,0,1)`: total4→5, created1.** Fixed expectation `(3,1,0)`, then unlocked extraction `(3,0,1)` | Original `TileAlchemyFurnace:230-231,244-249` debits BEFORE credit; total must remain4 | T: `furnaceAlembicCallbackConservesEssentia` **FAILED baseline**; fix: paired raw debit/credit before notification, no late destination readback/refund |
| 5 | Alembic → held jar | Debit equals item credit; ordinary capacity returns excess, void item can destroy overflow | Original manual jar path in `BlockMetalDevice`; native `BlockAlembic:118-129` uses paired exchange | R: `EssentiaTransfers.exchangeItem`; existing `alembicPreservesPhialsAndOrientation` |
| 6 | Ordinary jar acceptance / cap | `(stored,offer)=(63,3) → (64,remainder2)`; wrong aspect returns3 | `TileJarFillable:73-85`: bounded insertion returns unused amount | T: `g5CapsAndSpendingAccounting`; no fix |
| 7 | Void jar overflow | `(63,offer3) → (64,remainder0,destroyed2)` | `TileJarFillableVoid:18-36`: accepts compatible overflow and clamps64 | T: same; intentional original sink, NOT a defect |
| 8 | Tube → tube / jar | Measured two coal→8 units; final furnace0/alembics0+4/tubes0/jar4; sum checked after every phase | Original `TileTube:198` takes1 then adds that returned amount | E: `essentiaTubesPullConserveAndToggleSides`, `jarsPullFromNativeTransportAndKeepManualActions`; no fix |
| 9 | Jar → infusion demand | Measured `(stock,demand)=(192,63)→(179,50)→(129,0)`; stock−demand remains129; consumed63 | Original `EssentiaHandler:53`, `TileInfusionMatrix:456` consume one per successful draw | E: `infusionConsumesDistilledEssentiaAndPersistsSession`; no fix |
| 10 | Raw centivis → wand / cap | Wand2499 + offer100 → wand2500 + returned99; simulation writes nothing | `ItemWandCasting:352-361`: unscaled remainder in centivis | T: `g5CapsAndSpendingAccounting`; no fix |
| 11 | Ordinary node → wand | Node4→3 whole vis; wand2499→2500 centivis; **99 centivis lost** | `TileNode:419-422` uses `addVis`; `ItemWandCasting:340-349` truncates remainder99/100→0 | T: same; intentional original rounding loss, NOT a defect |
| 12 | Wand → focus shared debit |2500→2499 for prepriced1; an unpayable second aspect leaves all stock unchanged | Original `consumeAllVis:318-337` validates all costs before writing; float truncation occurs once in pricing | T: same; `FocusVis`; no fix; individual focus action handlers remain reasoned-only |
| 13 | Wand → arcane spend |2499→2399 for prepriced100 centivis =1 whole vis | Original `consumeAllVisCrafting:306-315` multiplies whole vis by100 before pricing | T: same; `ArcaneVis`; no fix |
| 14 | Ordinary-node refill acceptance | Current3 + offer3 at base4 → current4 + returned2 | Original `TileNode.addToContainer` returns amount beyond base; regeneration is an original source, not a donor transfer | T: same (cap acceptance only); timed regeneration R; no fix |
| 15 | Ordinary-node discharge | Measured depleted receiver90→91/donor10→9, total100; full-receiver burn branch reasoned-only | Original `TileNode:578-593` deliberately burns a unit when receiver is full, even if growth roll fails | E: `nodeStabilizerLockPhaseDischargeAndWand`; native `handleDischarge` burn retained, not newly runtime-proven |
| 16 | Energized-source refill / aspect drain | Measured EARTH pulse4→1→0 for requests3 then3, return3 then1; independent ORDER4 unchanged; next source tick replaces pulse with4, not accumulated stock | Original energized node is renewable throughput, NOT a finite ordinary-node fuel reservoir | E: `visNetSourceBudgets`; no fix |
| 17 | Aspectless `AuraHelper.drainVis` | Initial AIR3/EARTH2/FIRE1; simulate4 leaves6; drain4 leaves0/1/1; request99 returns2 and leaves0; empty drain returns0 | Aspectless wrapper is retained 1.12 compatibility, not a TC4-oracle API; TC4 underlying source debits remain raw units. Primal order is AIR,EARTH,FIRE,WATER,ORDER,ENTROPY | T: `g5PrimalNetworkAccounting`; no fix |

## Verification

### Baseline: real red test

`gradle-g5-baseline-runtime.log`, Java21, own `.gradle-g5`, daemon1GiB/server768MiB, fresh world:

```text
Caused by: GameTestAssertException: conservation: furnace=4, alembic=0, exported=1; expected total4, actual5
3 GAME TESTS COMPLETE
1 required tests failed
  thaumcraftgametests.furnacealembiccallbackconservesessentia
```

Both `g5CapsAndSpendingAccounting` and `g5PrimalNetworkAccounting` passed against unchanged production baseline code. Thus all numeric T rows except4 were observed before the fix, including the intentional overflow/rounding losses. Only row4 is a proven conservation violation.

### Fix

`TileAlchemyFurnace.pushOne` now privately prepares the exact selected target/aspect and delegates to `EssentiaTransfers.pushFurnaceOne`. The existing endpoint mutation scope locks source and receiver, commits source−1 and receiver+1 in a callback-free raw phase, then notifies. A failed post-commit notification is reported without refunding or forgetting the committed unit. The furnace ticker/reload cannot reenter while locked. Original two-pass selection, aspect choice, five-alembic scan and cadence are unchanged.

The regression additionally covers a typed empty alembic (first-pass path) and an untyped empty alembic (second-pass path), then proves the unit can be extracted normally after the scope unlocks. Reentrant extraction during notification is denied consistently with the port's existing paired native-storage authority model; this is not a new suction/capacity/gameplay rule.

### Fixed runtime: green

`gradle-g5-fixed-regression.log`: **12/12 required Fabric GameTests passed**, including the3 new checks and9 existing cases named in the ledger. This observed both typed/untyped outcomes `(3,1,0)` and subsequent unlocked extraction `(3,0,1)`, with total4. The table's E measurements are assertions from that actual rerun, not historical passes. The final source changes after this run only reorder test imports.

Final targeted JUnit: **15/15 passed** (`AuraHelperDrainTest`4, `FocusVisArithmeticTest`2, `WandDataTest`9; zero failures/errors/skips). `:fabric:assemble :fabric:gameTestClasses` **passed**, including remapped production JAR. Log: `gradle-g5-final-build.log`.

Reproduction (from the owned worktree; remove the world before **each** invocation):

```sh
rm -rf fabric/run-gametest/world
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME="$PWD/.gradle-g5"
./gradlew :fabric:runGameTest -Dorg.gradle.jvmargs=-Xmx1G -PgameTestHeap=768M \
  -PgameTestFilter=furnaceAlembicCallbackConservesEssentia,g5PrimalNetworkAccounting,g5CapsAndSpendingAccounting,alchemyFurnaceDistillsIntoRealAlembics,alembicPreservesPhialsAndOrientation,crucibleDissolvesAndCraftsWithOriginalCosts,essentiaTubesPullConserveAndToggleSides,jarsPullFromNativeTransportAndKeepManualActions,infusionConsumesDistilledEssentiaAndPersistsSession,visNetSourceBudgets,nodeStabilizerLockPhaseDischargeAndWand,nodeStabilizerRegenerationRepairAndPersistence \
  --no-daemon --max-workers=2
```

The baseline red run used the same environment and only the first3 test names. It ran the new tests against unmodified production `96b79de1`, before applying the fix. No failing assertion was weakened to obtain the green result; the callback test was strengthened to add typed-path and post-unlock checks.

**Finding count:**17 families inspected;1 conservation violation,1 red-test-proven defect,1 fix. Nine families have new direct numeric checks;7 more have existing numeric regressions rerun; held-jar exchange (row5) remains reasoned-only. Full-receiver discharge burn, per-focus action-handler/packet ownership and unenumerated interleavings are not claimed runtime-proven.

## Limits

- No NeoForge, client, multiplayer or full-suite acceptance claimed.
- The oracle was read only at `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`; no original code/assets were changed or copied into this audit.
- Network pulse refill, ordinary-node regeneration, crucible decay, void sinks and deliberate crafting/casting consumption are sources/sinks in the original, not proof of a port conservation bug.
- No fix is justified solely by an apparent unit mismatch; the ordinary-node tapping and void-jar examples demonstrate why the oracle matters.
