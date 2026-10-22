# Test-order audit — 2026-10-23

Scope: Fabric GameTests at `e132014e`, worktree `agent/g2b`. Fresh worlds and a private Gradle home are used for every recorded run. This is server-side acceptance, not physical-client or multiplayer acceptance.

## Water work-radius investigation

**The reported order dependency was not reproduced at the supplied starting commit.** There is no measured preceding-scenario state to identify, and no claim that such a dependency has been fixed. Six correctly selected fresh-world isolated diagnostic runs passed before fixture hardening. Instrumentation changed only evaluation of the assertion's result into a local and printed predicates after the native interaction; it did not add setup, ticks, waits or retries.

In all twelve installations (Lumber and Harvest in each run): result `CONSUME`, Water count 1, empty hand; player alive, health 20, not removed, same level; golem alive; configuration, native reach and world interaction predicates all true. Original failing expression therefore evaluated true without any preceding scenario.

| Diagnostic run | Lumber actor X / Z after positioning | Result | Gradle wall time |
| --- | --- | --- | --- |
| Initial | -10082334.3 / 6852503.5 | 1/1 pass | 33s |
| Repeat 1 | -3882299.3 / -5886697.5 | 1/1 pass | 31s |
| Repeat 2 | 8529652.7 / -5504131.5 | 1/1 pass | 21s |
| Repeat 3 | -7352522.3 / -13610757.5 | 1/1 pass | 20s |
| Repeat 4 | 10392042.7 / 8462873.5 | 1/1 pass | 26s |
| Repeat 5 | 2559410.7 / 5772179.5 | 1/1 pass | 27s |

Mapped vanilla `GameTestServer.startTests` chooses X and Z independently in `[-14999992,14999992]` and Y=-59; this explains the different positions, not an inherited fixture.

### Changes retained

- Explicitly establish a live, scenario-owned connected survival actor and park it above this fixture during the scan window, rather than leave it at login spawn. This is fixture hygiene, **not a measured explanation of the reported failure**.
- Assert life/level identity and native configuration/reach/world/item-use permissions immediately before installation. A future failure now distinguishes fixture preconditions from the actual upgrade transaction; the transaction assertion includes result, upgrade count and held item.
- Remove temporary stdout instrumentation. Keep a single native installation attempt, no retry or extra tick.
- Register idempotent cleanup with the native GameTest listener (pass, failure, timeout and rerun); move chunk acquisition/block writes inside the guarded setup, catch `Error` as well as runtime exceptions, and put chunk/player/channel release in nested `finally` paths. This fixes observable source-level cleanup gaps without modifying gameplay.

### Verification

Final-source isolated run: **1/1 passed**, `BUILD SUCCESSFUL in 22s`, without stdout instrumentation (`.tmp/g2b/isolated-final.log`).

Full suite: **326/326 passed**, `BUILD SUCCESSFUL in 2m 31s`; native summary `326 GAME TESTS COMPLETE IN 1.876 min` (`.tmp/g2b/full-final.log`). Parsed `fabric/build/gametest-results.xml`: 326 test cases, zero failure/error elements, and the Water case present under `thaumcraftgametests.waterupgradewidenstheliveworkradiusofrunninglumberandharvestgolems`.

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/g2b/.gradle-g2b
rm -rf fabric/run-gametest/world
./gradlew :fabric:runGameTest -PgameTestFilter=waterUpgradeWidensTheLiveWorkRadiusOfRunningLumberAndHarvestGolems
rm -rf fabric/run-gametest/world
./gradlew :fabric:runGameTest
```

Runs were serial, never concurrent. The first private-home dependency/bootstrap download took 10m44s and ended at the invalid selector, not at a scenario assertion. Temporary diagnostic/fault-injection source is not part of the retained changes.

Failure-path check: temporarily schedule `test.fail` at tick630 **outside the fixture's guarded callbacks**. The native failure listener ran cleanup and measured `forcedStillOwned=0`, `actorPresent=false`, `actorRemoved=true`, `channelOpen=false`, `lumberRemoved=true`, `harvestRemoved=true`. That deliberately failing run correctly exited1 with one required failure (26s, `.tmp/g2b/failure-cleanup-injected.log`); it is not counted as a passing run. The injection and cleanup stdout probes were then removed.

Restored-source verification after removing the injection: **1/1 passed**, `BUILD SUCCESSFUL in 22s` (`.tmp/g2b/isolated-restored.log`). Final retained test source is the same as the successful 326-case run. Total valid isolated acceptance: six pre-hardening diagnostic passes plus two final-source passes.

### Selection caveat

The supplied snake-case filter does not match this starting commit: the first fresh-world command exited 1 with `Expected one GameTest for 'water_upgrade_widens_the_live_work_radius_of_running_lumber_and_harvest_golems', found 0`. It executed no scenario. `GameTestSelectionMixin` compares lowercased method names, without snake-case conversion. Subsequent runs use `-PgameTestFilter=waterUpgradeWidensTheLiveWorkRadiusOfRunningLumberAndHarvestGolems`.

## Sweep

These are source-audit findings, **not** a claim that every scenario was independently executed.

| Surface / scenarios | Ownership or risk found | Status |
| --- | --- | --- |
| `GolemWaterRangeRuntimeChecks.run` | Connected actor waits 640 ticks before first interaction. Hand-written chunk halo, cleanup originally only on selected runtime exceptions. | Six pre-hardening isolated passes, no order dependency reproduced. Actor setup/assertions and cleanup hardened. |
| `WaveLane` users: Champion (4), EquipmentItem (3), EldritchBestiary (3), Projectile (3), DeviceContainer (2), CultistFaction (3), TaintedAnimal (3), RunicBauble (2), OuterLands (3), EldritchBlock (6) | 32 separately constructed lane instances; no static shared lane. Cleanup sweeps a five-block radius. `WaveLane.dropped` reads a wider radius (16), so spatially adjacent drops remain a possible contamination source. | No new order dependency proven. Wider drop reads need isolated / reordered execution before changing expectations. |
| `GolemChunks.hold/refresh` and users | Forced chunks are a shared set, not reference-counted. Existing documentation and refresh logic explicitly handle neighbours releasing overlapping navigation rings. | Existing known overlap mechanism, not a newly proven defect. Callers with hand-written halos remain candidates. |
| `BiologicalLampRuntimeChecks.crop`, `NodeTransductionRuntimeChecks.conversionProbe/failureProbe`, `VisAmuletRuntimeChecks.probeBlock/probeType`, `FluxOverfullRuntimeChecks.callbackProbe`, `ExcavationRemovalFailureRuntimeChecks.probe` | Mutable static registered test blocks/types. | Registered by `ThaumcraftGameTestBootstrap` before registry freeze, not by a preceding scenario. Probe callback state still needs per-case reset. |
| `NativeBreakPhases.Holder.factory` | Mutable static loader factory. | Installed by `ThaumcraftGameTestBootstrap`, fails explicitly if absent; not scenario-order initialization. |
| `VisNetRuntimeChecks`, `VisNetLifecycleChecks`, VisChargeRelay, VisAmulet, FocalManipulator and infusion consumers | Read/mutate static level-indexed visnet graph/cache state. | Owned sources/relays are fixture inputs. The lifecycle level-clear case explicitly asserts sole-root ownership and requires its separate wrapper batch. No missing preceding-scenario initialization proven. |
| `ExcavationRemovalFailureRuntimeChecks.active` | Mutable static callback context. | Candidate for nested/concurrent execution audit; no cross-scenario dependency proven. |
| CheatSheet, KnowledgeNotification, WarpNotification, EldritchItems, HungryNode, NodeEcology, TaintBlock | Mutate shared Thaumcraft settings / ecological policy. | Save/restore paths present. Concurrent settings interference remains distinct from missing initialization; no new order dependency proven. |
| Game rules / time: ThaumiumEquipment, AlchemyResource, JarItem, WispEssence, PureFluid, FireBatWisp, TaintSwarm, Accessories, Ember, BrainyZombie, NaturalBiome, Warp | World-global settings. | Save/restore present in inspected paths; not an independent reordered-suite proof. |
| `makeMockServerPlayerInLevel` users, especially the older WaveLane cases | Each inspected body constructs its own mock rather than looking up another scenario's actor. Many older bodies do not explicitly discard that mock. | Cleanup/contamination candidates; do not equate a source smell with a demonstrated dependency. |

No test was changed merely because it appeared in this table.

Mock-player cleanup candidates returned by the source sweep (construction is local, but an explicit disposal audit remains): DeviceContainer, ElementalPickaxe, EquipmentItem, CultistFaction, VoidCap, UnstableNode, EldritchBestiary, TaintedAnimal, ArcaneEar, RuntimeChecks, CrystalDrop, Administration, Projectile, OuterLands, EldritchBlock, RunicBauble, Thaumatorium, Champion, SurvivalProgression and ElementalHoe runtime checks. Presence of another entity discard/finally in a file is not proof that its actor is released.

**Additional order-dependent scenarios proven/fixed: 0/0.** The table lists remaining risks, not established failures. A failing run's exact commit, dirty diff, native test name, complete console log and server/world inputs are needed to compare the original report with these successful reproductions.
