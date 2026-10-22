# Recipe/global-state pollution investigation (2026-10-23, g2c)

Base: `ceedd54d`, branch `agent/g2c`. Fabric only, fresh GameTest worlds.

## Evidence requirements

The reported failure is an observation to reproduce, not proof of its proposed cause.
In this checkout `recipeCodecAndDeclaredTools` never calls `replaceRecipes`.
The only scenario bodies calling it are `BorePistonAspectRuntimeChecks` and
`InfusionComponentPredicateGameTests.selectedSessionReloadAndReplacement`.
Both replacement scopes are synchronous Java `try/finally`: ordinary assertion
exceptions and errors unwind the finally before another scenario executes. A
GameTest timeout cannot interrupt Java execution and resume another scenario
while leaving that synchronous finally suspended. Process death is different:
it cannot contaminate a later scenario in that same process.

`RecipeManagerAuditMixin` is a test-only observer at HEAD/RETURN of the actual
native `replaceRecipes`, recording caller (including line), old size and new
size. It does not restore or alter recipes and therefore does not mask leaks.

## Measured replacement sequence and runs

Initial full run (`.tmp/g2c-baseline.log`, native log lines 1532–1580):

| Order | Scenario / source line | Before | After |
| --- | --- | ---: | ---: |
| 1 | `borePistonAspectsDistillation`, `BorePistonAspectRuntimeChecks:200` | 1635 | 1634 |
| 2 | same, finally `:203` | 1634 | 1635 |
| 3 | `infusionComponentPredicateSession`, `InfusionComponentPredicateGameTests:208` | 1635 | 1 |
| 4 | same, `:219` | 1 | 1635 |
| 5 | same, `:269` | 1635 | 1 |
| 6 | same, `:274` | 1 | 1635 |
| 7 | same, `:283` | 1635 | 1 |
| 8 | same, finally `:309` | 1 | 1635 |

**No preceding recipe polluter was measured.** The only earlier replacement is
the piston test, and it restores the full corpus before the selected-session
scenario begins. The reported infusion assertion did not fail in this run.
The codec scenario produces no replacement event. The quoted premise that its
body also replaces recipes is false at this base commit.

This initial invocation exited 1 after 10m43s (cold private cache plus execution):
later `boreNativeMenuAndReload` failed `actual empty-hand native menu opening`,
then `boreExhaustedRevisionRetainsOneOpaqueOwner` failed its installed-top/base
precondition and its cleanup crashed the server with `No exact native bore
retirement`. It is **not** a full-suite pass. Recipe restoration completed well
before those failures. World origin/fixture coordinates in that run were near
X=-12.77M/Z=-14.98M; coordinate sensitivity is not asserted as a proven cause.

A second full run with the config snapshot fix but **before** the lifetime fix
repeated exactly the same eight recipe transitions (`.tmp/g2c-full-final.log:1026–1074`)
and the same two later Bore failures. It exited 1 in 1m06s. The second location
was X=-11.86M/Z=9.08M. There was still no infusion invocation failure.

## Actual measured static polluter

`regenerationQueueLifecycle` calls the real
`LifecycleEvent.SERVER_LEVEL_UNLOAD.invoker().act(level)` on the **shared, still
running** GameTest level. The production unload callback in `Thaumcraft.java:220`
calls `BoreNativeContext.clearLevel`, which adds that level to the static
`REVOKED_LEVELS` weak map. `BoreNativeContext.live` denies a marked lifetime
forever; it is intentionally not a reconstructible recipe/visnet cache.

This simulated unload had **no restore path**. All later native Bore owners
therefore ran in a level that the production authority correctly regarded as
unloaded. The full log orders `event_lifecycle` at line1271 before the Bore
menu/revision failures at lines1396/1428.

Added a native lifetime regression and ran it against the old unload path:

```
GLOBAL_LIFETIME regenerationQueueLifecycle before=true afterUnload=false
unload fixture must restore the shared level lifetime before later Bore scenarios
1 required tests failed :(
BUILD FAILED in 25s
```

Evidence: `.tmp/g2c-lifetime-red.log:183–213`. This is a **measured static-lifetime
polluter**, not recipe-list pollution and not evidence that the reported infusion
assertion was reproduced. The earlier golem audit also explicitly says its
reported order dependency was not reproduced; it must not be used as proof of
this proposed recipe mechanism.

Fix: scope the simulated unload with a test-only snapshot of this exact level's
prior `REVOKED_LEVELS` marker. Observe the real callback making the level nonlive
inside the scope, then restore the exact prior marker in `AutoCloseable.close`,
including on assertion/exception unwinding. Do not clear the map or remove other
levels' denials. Assert the shared level is live again before scheduling later
callbacks. **No production authority is weakened or reset API added.**

## Sweep scope

Searched both `common/src/gameTest` and `fabric/src/gameTest` for replacements,
static assignments, public API map/list writes, configuration setters, reflective
static writes, event registrations, thread-local probes and server-global setters.
World RNG/biome/block fixtures are distinct from recipe-manager substitution;
event listeners and framework-owned mock players are also called out separately.

### Global replacements (source inspection)

| Scenario body / fixture | Shared state | Restore path / risk |
| --- | --- | --- |
| `BorePistonAspectRuntimeChecks.pistonRecipeTagsAndFiniteDistillation` | server recipe list | synchronous `finally` at line 203; not skipped by assertions |
| `InfusionComponentPredicateGameTests.selectedSessionReloadAndReplacement` | server recipe list | synchronous outer `finally` at line 309, including scene-close exceptions |
| `ObjectAspectApiRuntimeChecks.nativeDirectAndGroupedTagsReachProductionConsumers` | API object/group maps | `Maps` try-with-resources |
| `ObjectAspectApiRuntimeChecks.complexRegistrationUsesOriginalBranchesAndServerPhase` | same maps; recipe context | try-with-resources; nested context `finally` |
| `ObjectAspectApiRuntimeChecks.callbackCanRegisterComplexTagsAndEarnOnlyOriginalExistingAmounts` | same maps; scan callbacks | async cleanup + 80-tick watchdog; framework timeout bypass risk |
| `ScanApiRuntimeChecks.nativeApiUsesWorldNbtAndNodeLocators` | entity scan registry list | synchronous `finally` |
| `ScanApiRuntimeChecks.callbacksRespectOcclusionAndTwentyTickSession` | scan callback list | async cleanup + 80-tick watchdog; framework timeout bypass risk |
| `HungryNodeRuntimeChecks` | research settings; API maps | close for settings; nested synchronous map `finally` |
| `NodeEcologyRuntimeChecks` | research settings; dimension/biome blacklists; server difficulty | close; asynchronous dark/embedded fixtures also use watchdogs |
| `TaintBlockRuntimeChecks` | taint settings | fixture close |
| `WarpRuntimeChecks` | wuss-mode setting | fixture close |
| `CheatSheetRuntimeChecks` | research settings | synchronous `finally` |
| `EldritchItemsRuntimeChecks` | research settings | synchronous `finally` |
| `KnowledgeNotificationRuntimeChecks` | research settings | synchronous `finally` |
| `WarpNotificationRuntimeChecks` | research settings | synchronous `finally` |
| `LootBagRuntimeChecks.drawOrderAndAliases` | three `WeightedRandomLoot` pool lists | synchronous `finally`, restoring original references rather than mutated fixture lists |
| `EventLifecycleRuntimeChecks.regenerationQueueLifecycle` | biome settings; permanent `BoreNativeContext.REVOKED_LEVELS` marker | **fixed two leaks:** exact config snapshot instead of defaults; simulated unload marker previously had **no restore at all**, now scoped exact-key restore even on exceptions |
| `GolemGuardTargetsRuntimeChecks` | server PvP | async finish/watchdog; `Error` and framework timeout bypass risk |
| `ElementalPickaxeRuntimeChecks` | server PvP | outer synchronous `finally` |
| `VoidGearRuntimeChecks` | server PvP | synchronous `finally` |
| `HellbatFocusRuntimeChecks` | server PvP | synchronous `finally` |
| `ShockFocusRuntimeChecks`, `FrostShardRuntimeChecks` | server PvP/team state | scoped close |

### Related shared state (not replacement of a production singleton)

* Early bootstrap only: node-transduction, vis-amulet, flux-overfull,
  excavation-removal-failure and biological-lamp test blocks are registered
  before registry freeze; they are additive test content, not scenario swaps.
* Test probes: `BoreNativeDispatchRuntimeChecks.ACTIVE` and node-transduction
  `InputRandom.ACTIVE` are thread locals removed in `finally`;
  `BoreProtectionProbe`/`GolemPlacementProbe` use scoped active observations.
* Temporary Architectury listeners are registered by unstable-node, pure-fluid,
  pure-bucket, excavation-special/focus/upgrade/harvest/removal-failure,
  combat-focus-special, taint-block, trade, loot-bag, focal-manipulator,
  frost-shard, shock-focus, stream-axe, bore-work and golem-fire-order checks.
  Synchronous listeners use `finally`; asynchronous fixture-owned listeners
  rely on fixture close/watchdogs and can be bypassed by framework timeout.
* `MockPlayerCleanup` already owns players through the native `finish` hook;
  `EmbeddedPlayerTickDriver` unregisters its two listeners and owner map in close.
* Explicit level-cache clears: `VisNetLifecycleChecks.callbacksReloadAndIsolation`
  clears visnet graph/cache lifetime (its sole-root check requires a separate batch);
  `ArcaneEarRuntimeChecks` clears note history; `ReservoirRuntimeChecks` and
  `InfusionRuntimeChecks` clear essentia search caches; `WardingRuntimeChecks`
  and `WardingBehaviourRuntimeChecks` clear casts. These do **not** snapshot and
  restore the cleared entries: native fixture activity repopulates derived
  caches. They are not permanent denial markers like `REVOKED_LEVELS`. No
  additional cross-scenario failure was measured for these clears.
* World-global gamerule/time mutations appear in accessories, alchemy-resource,
  ember, jar-item, wisp-essence, thaumium-equipment, pure-fluid, taint-swarm,
  brainy-zombie, fire-bat/wisp and natural-biome fixtures. These are restored in
  `finally`/close, not recipe substitutions. RNG seed fixtures generally advance
  shared world randomness rather than snapshot it; that is not recipe evidence.

Exact loader scenario names for the grouped config/API/server rows above:

```
borePistonAspectsDistillation
infusionComponentPredicateSession
objectAspectApiDirectAndGroupedTagsReachProductionConsumers
objectAspectApiComplexRegistrationUsesOriginalServerPhase
objectAspectApiCallbacksRetainOriginalExistingAmounts
scanApiUsesNativeNbtAndNodeLocators
scanCallbacksRespectOcclusionAndNativeTiming
hungryNodesRetainOriginalDamageConsumptionAndBlockDestruction
pureNodesCleanseOnlyTaintUnlessEmbeddedInSilverwood
darkNodesConvertBiomesAndRespectOriginalFourthGiantCap
taintedNodesSpreadRealFibresAndInfectBeforeDarkEcology
embeddedSilverwoodNodesHarvestOnlyThroughRealPlayerCustody
taintBlocksPreserveOriginalSpreadLootAndFallingCustody
warpDispatchesOriginalBranchesWithNativeEffects
cheatSheetUsesServerPermissionAndOriginalKnowledgeRules
crimsonRitesCompleteResearchWithoutConsumingTheBook
knowledgeNotificationsUseOriginalCommittedFeedbackAndNativePackets
warpNotificationsFollowOriginalCommittedDeltasAndNativePackets
lootBagsPreserveOriginalPoolsAndPaidNativeDropCustody
regenerationQueueLifecycle
orderGuardTargetSwitchesChooseWhomRealGuardsFight
pickaxeOfTheCoreBurnsWearsAndFindsClusters
voidMetalGearMendsWithersAndWarps
hellbatFocusUsesOriginalTargetPaymentAndUpgradeGates
shockFocusPaysTargetsAndRespectsCooldown
frostShardRicochetsDamagesAndPersists
```

This list counts **26 scenarios**, including the subject recipe scenario (25
others); it does not count additive bootstrap registrations, observers, or
world-local gamerule/RNG mutation as singleton replacements. The two other
EldritchItems wrappers do not enter the research-settings override in the rites
body. A source risk is not labeled a measured leak.

## Fix and verification

No recipe-manager production or teardown change is justified by the measured
sequence: the existing synchronous finally blocks already restore it. Adding a
per-scenario global snapshot would not explain this report and could itself
overwrite another live test's global fixture if tests overlap.

Besides the permanent lifetime revocation above, the sweep proved a second
singleton-pollution defect in the **same scenario**:
`regenerationQueueLifecycle.enqueue` always called `ThaumcraftBiomeSettings.defaults()`.
It erased a caller's nondefault biome/spawn/worldgen/regeneration snapshot even
on successful execution. Added a regression that installs nondefault blue-forest
configuration and calls the real enqueue path. With the old restore, the native
isolated scenario failed with:

```
regeneration fixture restores the EXACT prior nondefault biome configuration, not defaults
1 required tests failed :(
BUILD FAILED in 26s
```

Evidence: `.tmp/g2c-config-red.log:202–210`. Its outer scope still restored the
original settings on this deliberate failure, so this negative-control run did
not leave modified config behind. The fix captures/restores the private immutable
startup snapshot in a test-only `AutoCloseable`; no production API or gameplay
semantics change. Both configuration installation and queue population are inside
the resource scope, so ordinary exceptions/errors cannot bypass restoration.

### Final retained-source acceptance

| Run | Native/XML result | Process result / evidence |
| --- | --- | --- |
| Full suite | **353/353 required**, zero XML failure/error elements; target, lifecycle, Bore menu and exhausted revision cases present | **BUILD SUCCESSFUL in 2m14s**, `.tmp/g2c-full-restored.log`, `fabric/build/gametest-results.xml` |
| Isolated `infusionComponentPredicateSession` after full suite | **1/1**, zero XML failure/error elements | **BUILD SUCCESSFUL in 24s**, `.tmp/g2c-infusion-isolated-final.log`, `fabric/build/gametest-focused-results.xml` |

The full retained-source log records the same eight recipe replacements above
and this real callback/restoration sequence:

```
GLOBAL_LIFETIME regenerationQueueLifecycle before=true afterUnload=false
GLOBAL_LIFETIME regenerationQueueLifecycle restored=true
353 GAME TESTS COMPLETE IN 1.463 min
All 353 required tests passed :)
```

The isolated target records all six of its own replacement/restoration calls,
each returning the manager to **1635** recipes. No scenario wrapper was added or
excluded to obtain these results. Earlier crashes and deliberately red regression
runs are not counted as passes. This is dedicated-server GameTest acceptance,
not client/multiplayer/survival acceptance.

Commands (Java21 and private Gradle home exported before every invocation):

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/g2c/.gradle-g2c
rm -rf fabric/run-gametest/world
./gradlew :fabric:runGameTest --no-daemon
rm -rf fabric/run-gametest/world
./gradlew :fabric:runGameTest -PgameTestFilter=infusionComponentPredicateSession --no-daemon
```

Additional global-overriding scenarios
audited: **25**. Scenarios with proven pollution fixed: **1** (`regenerationQueueLifecycle`),
containing **two defects** (configuration reset and permanent lifetime revocation).
