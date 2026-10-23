# g21golem — end-to-end core hypothesis

Worktree `/home/cyberfox-kali/wt/g21golem`, branch `agent/g21golem`, base `9ef54a43`. Initial requested `git status --short` was clean. No sibling worktree or main checkout was accessed.

## Changes

- `GolemEndToEndRuntimeChecks`: real selector-driven Fill/carry/refusal lanes; matched Gather/Air delivery lanes; measured Water pickup boundaries; Harvest/Lumber plus Gather delivery; Use seed payment in block and empty-space modes; Butcher oldest surplus death with pair/bystander/connected-owner safety.
- Seven default-discoverable/focus-selectable Fabric GameTest entry points, each with its own batch to avoid inter-test fixture interference.
- Audit inventory: `docs/audit/GOLEM_FINDINGS.md`.
- Ignore this lane's private `.gradle-g21/` generated cache. Two pre-existing tracked wrapper-cache files were not intentionally changed; downloader removal of the tracked `.part` was restored from HEAD.

All work inputs (golems/configuration, containers/stacks, blocks and animals) are fixtures. No test supplies the selected target, movement, transfer, harvest, use, attack, death or delivery. Carry samples actual held stacks; speed samples actual pickup ticks. The Butcher animal lifetime ordering is an explicit input, not a supplied victim selection.

## Execution record

Every Gradle invocation uses Java 21 and `GRADLE_USER_HOME=/home/cyberfox-kali/wt/g21golem/.gradle-g21`; `fabric/run-gametest/world` is deleted before each invocation, as requested.

1. Initial normal Fabric invocation: canceled with this home's `./gradlew --stop` while configuration was resolving unrelated NeoForge libraries over HTTP. Thread dump showed `ForgeLibrariesProvider.provide` → Gradle HTTP HEAD. No tests ran; not a gameplay failure.
2. `:fabric:runGameTest --configure-on-demand`: failed before compilation, because Fabric reads `project(':common').sourceSets.gameTest` before on-demand evaluation has created it. No tests ran; not a gameplay failure.
3. Retry requests `:common:gameTestClasses :fabric:runGameTest --configure-on-demand` in that order. **Common/Fabric production and GameTest compilation passed**, but `prepareArchitecturyTransformer` fails because on-demand configuration leaves a mapping provider unset. No tests ran.
4. Same retry with `-Penabled_platforms=fabric`: same transformer setup failure. No tests ran.
5. Normal configuration with an invocation-only init script `.tmp/g21-fabric-only.gradle` removing the deferred `neoforge` project descriptor, plus `-Penabled_platforms=fabric`. Neither settings/build scripts nor NeoForge sources are changed. The script is exactly:

```groovy
settingsEvaluated { settings ->
    settings.rootProject.children.removeIf { descriptor -> descriptor.name == 'neoforge' }
}
```

Runtime evidence is complete below. Logs stay ignored in this worktree (`.tmp-g21-*.log`).

Focused selection (new plus existing live regressions):

```text
g21CarryAndRefusals
g21GatherAndAir
g21HarvestAndLumberDelivery
g21UsePlantsExactlyOneSeed
g21UseEmptySpacePlantsExactlyOneSeed
g21MeasuredWaterPickupRadius
g21ButcherLeavesBreedingPair
entropySwitchesChangeWhatRealGolemsFetchEmptyAndSort
waterUpgradeWidensTheLiveWorkRadiusOfRunningLumberAndHarvestGolems
realFishingGolemsCastIntoPondsAndDeliverCooksAndExtraCatches
alchemyGolemsCollectAndDeliverMixedReservoirs
golemFetterHoldsGolemsWhileItsRedstoneSignalLasts
entropyQueuesTheFarthestConnectedWaterForLiquidGolems
orderGuardTargetSwitchesChooseWhomRealGuardsFight
fireIgnitesGuardTargetsAndOrderReplantsHarvestedCrops
```

## Hypothesis discipline

No production defect/fix is claimed without a valid failing live test. Original-contract notes were consulted: Harvest/Lumber do not themselves collect; Fire adds ignition, not initial hit strength; Entropy's queued work is connected-liquid traversal. Those differences from the request's shorthand are documented rather than “fixed” into non-original behavior.

## First genuine red test (before gameplay edits)

`g21UsePlantsExactlyOneSeed` fails in a fresh focused run at tick 400 (`.tmp-g21-use-red.log`): real home chest seeds 8 → 0, real golem cargo 0 → 8, unchanged farmland with air above, `canWork=true`. Repeated diagnostic run `.tmp-g21-use-diag.log` agrees. The machine actor independently reports native-valid, empty and survival; this is not missing platform wiring or a supplied invalid actor. At tick 350 the golem is within squared distance 3.105 of its actual marker and navigation is done, still holding all eight seeds.

Red tests and diagnostic code were committed as `e62b7482` **before** the production edit. First candidate (preserve the final in-reach navigation update) did **not** fix the failure (`.tmp-g21-use-fix1.log`); it was removed, not retained as an unproven change.

The next fresh red diagnostic run (`.tmp-g21-use-sight-red.log`) measured the actual ray collision while the actual worker was in click reach. A ray aimed at the marked farmland's **block center** hits the preceding **stone floor block**, not the farmland. A ray to the clicked **UP face center** on the identical fixture is unobstructed. Both `EntityGolemBase.useEyesOn` and `GolemUsePlatform.sight` aimed into the floor. Fix **`8fa5b24d`** shares a face-centered sight target, checks its actual finite chunk footprint, and still rejects every earlier solid. The borrowed actor's native hit, item ownership, distance²≤4, current-state/protection checks and one-attempt lifecycle are not weakened. **Focused post-fix run passed** (`.tmp-g21-use-sight-fix.log`). Temporary reflection/ray diagnostic code is removed from final tests.

## Final verification and caveat

- Initial `.tmp-g21-e2e4.log` combined run was canceled after the Use assertion exposed a fixture cleanup-order error (forced-chunk isolation failure obscured the original assertion and stalled later batches). Fixture polling/deadline callbacks now close before propagating failure; fresh focused red runs show the genuine planting failure without that isolation error. No accepted result is claimed for the canceled run.
- `.tmp-g21-final.log`: Common JUnit and Fabric build passed, but 14/15 GameTests; the combined cutter/collector test hit its 1500-tick deadline without enough diagnostics to identify which stage failed. **Not an accepted run and not a proven second production defect.**
- `.tmp-g21-farming-diag.log`: fresh focused farming rerun passed without a farming production edit.
- `.tmp-g21-final2.log`: fresh complete **15/15 PASS**, Common JUnit and Fabric build PASS. This version additionally has the Butcher's real connected survival owner.
- To avoid simultaneous path competition as a fixture confound, final farming starts its real Gather workers only after observing actual melon/trunk removal. Drops and delivery are never supplied. This is deliberately **not concurrent cutter/collector liveness acceptance**; the earlier timeout remains unclassified and disclosed.
- **Final sources**, `.tmp-g21-final3.log`: **15/15 GameTests PASS**, **2509 Common JUnit tests PASS** (396 suites; zero failures/errors/skips), **`:fabric:build` PASS** including remapped production/test isolation. Final actual measurements: carry **8 plain / 16 Earth**; 12-block pickup **54 plain / 43 Air ticks**; pickup radius plain accepts **15.9**, refuses **16.1**, Water accepts **19.9**, refuses **20.1** through an 800-tick negative window.

Final invocation, after deleting `fabric/run-gametest/world`, with the Java/Gradle home above:

```text
./gradlew :common:test :fabric:build :fabric:runGameTest
  -I .tmp/g21-fabric-only.gradle -Penabled_platforms=fabric
  -PgameTestFilter=<the 15 comma-separated names listed above> --console=plain
```

Outcome: **12 cores and 6 upgrades tested end to end; 1 PROVEN gameplay defect (Use), fixed with prior red and subsequent green live tests.** One additional mixed-test deadline is unclassified, not silently counted as fixed. Universal provider/protection coverage, all core/upgrade/fetter combinations, concurrent producer/collector liveness, all Use modes/items/crops/animals, client/network/multiplayer, NeoForge and survival acquisition remain outside this acceptance.
