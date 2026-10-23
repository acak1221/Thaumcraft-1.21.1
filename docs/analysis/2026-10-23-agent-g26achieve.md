# g26achieve — automated devices

- Worktree `/home/cyberfox-kali/wt/g26achieve`, branch `agent/g26achieve`.
- Initial status clean; base `7f37e8a6`.
- Scope: prove/disprove consumption/output failure of actual built devices, not decorative state or manually advanced isolated machine fixtures.
- Added world-scheduled GameTests and reused bounded real-world formation/menu helpers. Detailed coverage and limitations: `docs/audit/DEVICE_FINDINGS.md`.
- Initial `:fabric:compileGameTestJava --offline` failed in the new private `.gradle-g26` home because Loom was not cached. Online resolution started with Java21 and that same private Gradle home. No shared caches, other worktrees or primary checkout modified.
- Every Gradle launch is preceded by removing only this worktree’s `fabric/run-gametest/world`.
- Initial real runtime:15/16 passed; hungry-chest declared input randomly launched sideways and did not reach the chest (stored0, live input on floor). Preserved failure in `docs/audit/DEVICE_RUN_INITIAL.json`. Added zero initial velocity to declare a vertical drop; did not change machine behavior.
- Final fresh-world runtime:16/16 required GameTests passed,14 named device types driven. All world saving and server shutdown completed; `BUILD SUCCESSFUL in 35s`. Detailed measured ratios and nonclaims are in `docs/audit/DEVICE_FINDINGS.md` and `DEVICE_RUN_FINAL.json`.
- No production changes:0 PROVEN machine defects. One test-input correction only. Hypothesis not reproduced for the measured paths.
- World tests cover source-funded bore mining, basic furnace/alembics, wand-formed advanced furnace, centrifuge, crystallizer, research-point claiming, hungry chest contact, source/relay/consumer routing, tubes/buffer and wand-formed thaumatorium. Existing natural-world levitator/lamp tests also passed. Four controlled-ticker cases supplement exact cadence/94-CV acceleration/accounting.
- Initial test commit: `d6e910cb`. Follow-up commit corrects only the hungry-chest input and records green evidence.

## Reproduction

```sh
rm -rf fabric/run-gametest/world
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME="$PWD/.gradle-g26"
./gradlew :fabric:runGameTest --offline -PgameTestHeap=1500M -PgameTestFilter=deviceWorldPoweredBoreMinesAndReturnsItem,deviceWorldHungryChestCollectsPhysicalInput,deviceWorldRelayPaysActualConsumer,deviceWorldAdvancedFurnacePaysAndDelivers,deviceWorldThaumatoriumPaysAndDelivers,deviceWorldDeconstructionPaysAndDelivers,deviceWorldFurnacePaysAndDelivers,deviceWorldCentrifugePaysAndDelivers,deviceWorldCrystallizerPaysAndDelivers,deviceWorldTubeAndBufferDeliverToConsumer,arcaneLevitatorLiftsItemsAndCreaturesAndStacks,arcaneLampLightsDarkAirAndCleansItsOriginalRange,alchemyFurnaceDistillsIntoRealAlembics,centrifugeCompartmentsTimingPowerAndPersistence,deconstructionConsumesItemsAndProducesOriginalResearchPoints,visNetCrystallizer
```

The offline command requires the private home to have been provisioned online first. Do not enable configuration-on-demand: existing Fabric source-set wiring expects common to be evaluated first. No NeoForge tasks were run; initial whole-build configuration did provision its Loom cache as an incidental effect of the existing root build.
