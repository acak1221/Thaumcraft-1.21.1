# GameTest order-dependence audit — 2026-10-23

Base: `07451af6`, branch `agent/g8order`. Fabric only; Java 21; private
`.gradle-g8`; every native invocation deletes `fabric/run-gametest/world` first.
No production gameplay changes. The current suite has **356**, not 353, wrappers
(the three crafting-audit wrappers account for the difference).

## Evidence, not inherited diagnoses

The earlier audits do **not** prove three preceding-scenario polluters:
`TEST_ORDER_FINDINGS.md` explicitly did not reproduce the Water assertion, and
`RECIPE_POLLUTION_FINDINGS.md` found no recipe pollution. The latter did prove
the simulated-unload lifetime revocation and non-exact biome-config restoration
in `regenerationQueueLifecycle`. Those fixes remain intact.

Initial furnace measurements, before any change to its reload deadline:

| Run | Observation | Result |
| --- | --- | --- |
| Cold dependency/bootstrap | JVM process exit 137 before furnace; no furnace measurement | Not acceptance; `.tmp/g8order/full-diagnostic.log` |
| Warm, ownership guard enabled | Both chunks absent/old BEs retired at world tick15059; core ticket33, shell34, native stage `initialize_light`, persisted `full` at15064 | 356/356, 2m06s; `.tmp/g8order/full-diagnostic-warm.log` |
| Guard disabled | Both chunks absent at11903; core unavailable at11908/11913; new core available at11918 | 356/356, 2m18s; `.tmp/g8order/full-unguarded-diagnostic.log` |

In the unguarded run, monotonic timestamps were `88667428326771` at ticket
installation, `88667430795162` / `88667436661558` at the unavailable observations,
and `88667438533416` at readiness: **15 simulated ticks in 10.207 ms**. The native
ticket/load pipeline is genuinely asynchronous. A GameTest tick limit is not a
wall-clock I/O deadline. These passing measurements alone do **not** identify
the cause of the originally reported failure or a preceding polluter.

Single-CPU scheduling (`taskset -c 0`, otherwise the same unguarded source) also
passed 356/356 in5m09s. It needed20 ticks/28.868ms for core readiness. This was
not a reproduction of the reported ordinary-suite failure either.

## Measured reload-deadline defect and controlled red/green experiment

To test the actual asynchronous boundary, a **temporary test-only mixin** wrapped
`ChunkMap.scheduleChunkLoad` for the exact remote core column, armed only after
the old core/shell BEs had actually retired. It returned the original native
future's actual result through `CompletableFuture.delayedExecutor(1, SECONDS)`.
No chunk, block entity, NBT, formation result, payment or visibility answer was
fabricated. This is controlled one-second I/O latency, **not evidence that the
original uninstrumented report experienced exactly that latency**.

Full-suite measurement with the original40 polls ×5 ticks:

| Event | World tick | Monotonic nanoseconds | Native state |
| --- | ---: | ---: | --- |
| Both chunks absent and both old BEs retired; install owned radius0 ticket | 10380 | 89219876316856 | Both debug holders `null` before acquisition |
| Poll1 | 10385 | 89219879235466 | Only `thaumcraft_furnace_custody_test` ticket33; forced=false; FULL future incomplete |
| Actual native disk result ready behind diagnostic latency gate | between polls1/2 | 89219886450932 | Native result retained, not replaced |
| Poll40 / original assertion fails | 10580 | 89219909259034 | Same sole ticket33; forced=false; FULL future incomplete; main-thread pending queue0 |
| Native result released | — | 89220887183023 | After the test has already exhausted its simulated-tick budget |

Thus **200 simulated ticks expired in32.942178ms**, approximately978ms before
the awaited native result was released. An empty main-thread runnable queue did
not mean the async future was complete. XML contains exactly one failure:
`thaumcraftgametests.advancedfurnacepreservescustodyacrossrealchunkunload`, with
the reported message `native radius0 ticket asynchronously reloads the actually
saved core chunk`. Full run:355/356, exit1,2m32s;
`.tmp/g8order/full-delayed-native-load-red.{log,xml}`. This deliberately red
experiment is **not acceptance**. The first diagnostic gate also delayed later
reads of that same column; none caused an additional failure. The positive
experiment made the gate single-shot, and all diagnostic gates/probes are
removed from retained source.

**Proven defect:** an accelerated simulation-tick deadline was used as a disk/
lighting executor deadline. **No preceding leaked-ticket, forced-chunk, recipe
or revoked-level polluter was measured for this case.** The original ordinary
failure was not reproduced on this356-case base; assigning a particular earlier
scenario would be guessing.

### Retained fix

`GameTestChunkAwait.ticketed` has a ten-second **monotonic wall-clock** deadline.
It observes `getChunkNow` and pumps `ServerChunkCache.pollTask`, yielding1ms when
the native queue has no runnable work. That native executor services distance
manager, generation and lighting updates (confirmed against the mapped1.21.1
`MainThreadExecutor.pollTask` bytecode). It never calls a force-loading getter,
requests another generation future, or creates an UNKNOWN/extra region ticket.
The existing core33/shell34 partial-load proof and all custody assertions remain
unchanged. Both reload phases use the same explicit bound; this is not merely a
larger retry count. Actual unload still waits for real world ticks and both old
BE retirements, as required by native ticket expiry/unload semantics.

The furnace now also registers idempotent cleanup on native pass/failure/timeout/
rerun, closing the prior framework-timeout teardown gap.

Positive delayed-load measurement with the fix: ticket at tick14868 /
`89637624625111ns`; actual disk result at`89637633191693ns`; delayed result
released at`89638634405328ns`; FULL core visible at tick14873 /
`89638639660089ns`. The real load took **1015.035ms**, while the fixture advanced
only five simulated ticks. The furnace's **entire** custody scenario passed,
not just the reload assertion. There was no ownership-guard violation. The full
run was nevertheless **355/356, exit1,2m44s** because of the additional Ember
fixture defect below; do not call it full-suite acceptance. Evidence:
`.tmp/g8order/full-delayed-native-load-fixed-2600.{log,xml}`. A preceding attempt
was killed with exit137 before reaching the furnace
(`full-delayed-native-load-fixed.log`,1m42s); it supplies no positive measurement.

## Additional measured fixture dependency: Ember's champion target

The completed delayed-load full run failed only
`embersFlyDamageIgniteAndExpire` at `TC4's explicit fire-immune exclusion prevents
both direct damage and ignition`. Its target helper used unqualified native
`addFreshEntity` for a Blaze, whose real global champion join hook consumes the
shared world RNG. The assertion assumed the target was full-health and cold
without establishing that prerequisite.

A separate isolated negative control used the existing `ChampionJoinRoll` helper
to declare the **real native BOLD promotion draw**, keeping production champion
logic enabled. Measured before/after the actual ember collision:

```
before immune=true health=45.0 max=50.0 fire=-1 champion=0
after  immune=true health=45.0 max=50.0 fire=-1 champion=0
```

It reproduced that same assertion,1/1 failed,27s;
`.tmp/g8order/ember-champion-negative-control.{log,xml}`. The projectile correctly
preserved all target state; the **fixture already violated its own assumed
precondition**. Native promotion adds30 max health and heals25, so a20-health
Blaze can start at45/50. No production fire-immunity or champion-health rule is
changed.

Fix: use the existing scoped **ordinary** `ChampionJoinRoll.join` for collision
targets and assert full health/cold **before** any projectile interaction. The
helper restores the exact world-RNG identity in finally while consuming the
declared original draw; no global champion toggle, seed rollback, fabricated
health or retry is added. The normal world-RNG position is no longer an unstated
fixture input. A source regression pins this declaration/precondition, and Ember
is now an additional independently isolated CI regression.

## Shared prevention mechanism

`GameTestStateGuard`, loaded only by Fabric's GameTest mod, records:

* Per-scenario ownership of explicit region tickets (source/type/chunk/radius/key),
  newly forced chunks (exact level/chunk), and connected players. Native tickets
  with an expiry (including teleport/portal) are not manual fixture leases. Releases of another live
  scenario's owned resource are violations, not silently accepted cleanup.
* Before/after recipe corpus, research/taint/biome/spawn/worldgen/regeneration
  configuration, PvP/Wuss state, gamerules, difficulty/lock, ecology policy maps,
  world-RNG **identity**, and the shared level's native Bore lifetime.

The baseline is taken immediately before the scenario body. The verdict check
runs immediately after native `tickInternal`, **before** vanilla dispatches its
pass/failure listeners. This is after callback/try-with-resources/finally
unwinding, unlike `succeed()`/`finish()`. Failures identify the offending native
test name and resource; the original assertion is retained as a suppressed cause.
No bulk restoration hides leaks or overwrites a neighbouring live fixture.
`GameTestIsolationTest` pins registration and this verdict placement.

Snapshot checks are not a universal detector of arbitrary mutable Java statics,
nor an excuse to overlap asynchronous global overrides. Do not add such an
override to a shared batch: use a dedicated batch and exact restoration.
Derived caches, normal world-RNG advancement, and intended gameplay mutations
are not rolled back to a process-wide snapshot.

`scripts/porting/verify-gametest-order.py` independently runs a fresh full suite
and then each of the three reported regressions plus Ember in its **own process/world**.
It rejects nonzero exit, stale/missing XML, incorrect selection and native
failure/error elements. `--all` inventories the fresh native full-run XML and
isolates **every executed wrapper**; it is
the exhaustive option for assumptions that leave no detectable resource leak.
CI now runs these four paired regressions and retains their logs/XML. Runner
unit tests verify fresh-world deletion, all-case selection despite an outdated
manifest, and stale-XML refusal;
they are not native-game acceptance.

The checked-in required-case manifest contains317 entries at this base while
the actual full run contains356. The exhaustive runner intentionally uses the
native report, so all39 additional authored wrappers are included automatically
instead of silently disappearing from isolation coverage.

Native guard negative control: temporarily mutate configuration, empty recipes,
revoke the live level, retain a forced chunk and manual region ticket, and create
a connected helper mock **after `test.succeed()`** in the lifecycle callback.
The isolated run exited1 and its XML failed
`thaumcraftgametests.regenerationqueuelifecycle`, listing **all six** violations
under that test's own name. Evidence:
`.tmp/g8order/guard-negative-control.{log,xml}`,42s. This proves both native
resource hooks and the before-publication verdict placement, not just a source
substring assertion. The temporary mutation was removed; this deliberately red
run is not acceptance.

The lifetime guard would have named `regenerationQueueLifecycle` rather than a
later infusion/Bore victim. Configuration checks catch non-exact restoration
when it changes the caller's state (the existing nondefault-input regression
remains necessary). Ticket/player ownership catches forgotten cleanup and
foreign releases. Paired isolated execution catches the Water-style missing
fixture prerequisite even when there is no leak; neither its cause nor an
unmeasured recipe-manager defect is invented here.

## Final retained-source acceptance

All temporary latency gates, forced champion inputs, leak injections and stdout
probes were removed before these native runs. Guard enabled throughout; no
scenario excluded or wrapper added to change the count. Parsed archived XML:

| Fresh-world invocation | Native/XML result | Gradle result |
| --- | --- | --- |
| Full suite | **356/356**, zero failure/error elements | **SUCCESS**,2m35s |
| Water work-radius alone | **1/1**, exact requested native name, zero failure/error elements | **SUCCESS**,32s |
| Infusion component session alone | **1/1**, exact requested native name, zero failure/error elements | **SUCCESS**,27s |
| Furnace real unload alone | **1/1**, exact requested native name, zero failure/error elements | **SUCCESS**,32s |
| Additional Ember regression alone | **1/1**, exact requested native name, zero failure/error elements | **SUCCESS**,27s |

Evidence: `.tmp/order-verification/summary.json`, `full.{log,xml}` and the
four selector-named `{log,xml}` pairs in that directory;
`.tmp/g8order/retained-order-verification.log`. No guard violation in these logs.
Each isolated row is a separate native process/world, **not** a four-case batch.

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/g8order/.gradle-g8
rm -rf fabric/run-gametest/world
python3 -B scripts/porting/verify-gametest-order.py --heap 2600M
```

The runner deletes the world and old native XML before **each** Gradle process;
Gradle processes are serial. The2600M GameTest heap and768M daemon bound are
explicit environmental inputs, not changes to production or the normal3G
GameTest default. `--all` inventory/dispatch is covered by runner unit tests;
356 individual isolated processes were **not** executed in this task.

Final build/source checks, after native acceptance:

* `:common:test --tests thaumcraft.integration.GameTestIsolationTest :fabric:build`:
  **SUCCESS in1m09s**; JUnit **6/6**, zero skipped/failure/error;
  `:fabric:verifyTestIsolation` and `:fabric:verifyProductionIsolation` both ran.
  Evidence: `.tmp/g8order/final-build-junit.log` and
  `common/build/test-results/test/TEST-thaumcraft.integration.GameTestIsolationTest.xml`.
* `python3 -B -m unittest discover -s scripts/porting -p test_gametest_order.py`:
  **3/3 passed**. These check runner orchestration, including stale-result refusal
  and exhaustive inventory of wrappers absent from the old manifest, not gameplay.
* `git diff --check`: clean.

This is dedicated-server GameTest acceptance. It is not physical-client,
multiplayer, survival acquisition or NeoForge parity, and it does not turn the
unreproduced original report into a proven earlier-scenario polluter.

## Finally-only/global-override inventory

Source inventory of other scenarios. “Finally-only” includes a fixture `close`
reached through synchronous try-with-resources/finally, or asynchronous callback
finally/watchdog, rather than a native verdict cleanup hook. A synchronous Java
finally is **not skipped** by an ordinary assertion or exception. Async closures
and watchdogs are not equivalent to a native framework-timeout hook. These are
restoration surfaces, **not newly proven pollution bugs**. Subject furnace,
Water and infusion-session cases are excluded from the following lists.

### Singleton/API/server overrides

| Native wrapper(s) | Shared state / restore |
| --- | --- |
| `borePistonAspectsDistillation` | Recipe corpus; synchronous finally |
| `craftingAuditShapedDamage`, `craftingAuditShapelessDamage` | Recipe corpus; synchronous finally (only shapeless installs the synthetic corpus) |
| `objectAspectApiDirectAndGroupedTagsReachProductionConsumers`, `objectAspectApiComplexRegistrationUsesOriginalServerPhase` | Object/group maps; scoped close, nested recipe-context finally |
| `objectAspectApiCallbacksRetainOriginalExistingAmounts` | Object/group maps and scan handlers; callback cleanup/watchdog |
| `scanApiUsesNativeNbtAndNodeLocators` | Scan entity registry; synchronous finally |
| `scanCallbacksRespectOcclusionAndNativeTiming` | Scan handlers; fixture close/watchdog |
| `hungryNodesRetainOriginalDamageConsumptionAndBlockDestruction` | Research settings; scoped close; nested object/group-map finally |
| `pureNodesCleanseOnlyTaintUnlessEmbeddedInSilverwood`, `darkNodesConvertBiomesAndRespectOriginalFourthGiantCap`, `taintedNodesSpreadRealFibresAndInfectBeforeDarkEcology`, `embeddedSilverwoodNodesHarvestOnlyThroughRealPlayerCustody` | Research settings, ecology blacklists, difficulty/lock; close; dark/embedded also async watchdogs |
| `taintBlocksPreserveOriginalSpreadLootAndFallingCustody` | Taint settings; scoped close |
| `warpDispatchesOriginalBranchesWithNativeEffects` | Wuss state; close |
| `cheatSheetUsesServerPermissionAndOriginalKnowledgeRules` | Research settings; synchronous finally |
| `crimsonRitesCompleteResearchWithoutConsumingTheBook` | Research settings; synchronous finally (the other two EldritchItems wrappers do not enter this override) |
| `knowledgeNotificationsUseOriginalCommittedFeedbackAndNativePackets`, `warpNotificationsFollowOriginalCommittedDeltasAndNativePackets` | Research settings; synchronous finally |
| `lootBagsPreserveOriginalPoolsAndPaidNativeDropCustody` | Three public loot-pool references; synchronous finally |
| `orderGuardTargetSwitchesChooseWhomRealGuardsFight` | Server PvP; local finally plus async finish/watchdog |
| `pickaxeOfTheCoreBurnsWearsAndFindsClusters`, `voidMetalGearMendsWithersAndWarps`, `hellbatFocusUsesOriginalTargetPaymentAndUpgradeGates` | Server PvP; synchronous finally |
| `shockFocusPaysTargetsAndRespectsCooldown`, `frostShardRicochetsDamagesAndPersists` | PvP/team state; fixture close |

`regenerationQueueLifecycle` is the already-fixed special case: scoped exact
configuration/lifetime restore, now also checked by the shared boundary guard.

### Shared world rules/time/RNG fixtures

| Native wrapper(s) | State |
| --- | --- |
| `thaumiumEquipmentCraftsMinesProtectsAndRepairs` | Limited-crafting rule |
| `alchemyResourcesPlaceBurnAndExplode`, `embersFlyDamageIgniteAndExpire` | Mob-griefing rule |
| `jarItemsCraftPreserveComponentsAndExposeContents`, `harvestedNodeEssenceDistillsRealAura` | Block-drops rule |
| `purifyingFluidPreservesOriginalClassicFlowWardAndSaltExpiry` | Random-tick-speed rule |
| `accessoriesUseNativeMenuPersistenceAndDeathCustody` | Keep-inventory rule |
| `brainyZombiesFightRageCureAndDropOriginalLoot`, `taintSwarmsPreserveOriginalFlightCombatAndSummonedLifetime` | Mob-loot rule, day time |
| `fireBatPreservesNativeAttacksVariantsAndLoot`, `wispsKeepOriginalCombatMovementAndEssence`, `flyingMonstersUseOriginalNaturalPopulationRules` | Shared FireBatWisp fixture restores mob-loot; population branch also day time |
| `warpDispatchesOriginalBranchesWithNativeEffects`, `magicalBiomesGenerateNaturallyAndSpawnActualPech` | Day time |
| `infusionAltarFormsAndExecutesAllInstabilityOutcomes`, `infusedOresHarvestAndSmelt`, `magicalTreesGrowDropAndCraft`, `fluxMovesAndExhaustsWithOriginalRules`, `hoeOfGrowthTillsFertilisesAndGrowsSaplings`, `originalBiomeTreesAndDungeonsUseRealWorldgen`, `magicalBiomesGenerateNaturallyAndSpawnActualPech` | World-RNG reseeding/resume-seed finally. This does **not** reconstruct the prior RNG stream; do not label it an exact snapshot. |
| `nodeTransductionConversionPersistenceAndCooldown`, `nodeTransductionIsolatedFailureLifecycle`, `nodeStabilizerLockPhaseDischargeAndWand`, `nodeStabilizerRegenerationRepairAndPersistence`, `fluxOverfullFlowStateAndPersistence`, `fluxOriginalExposureAndGooTail`, `fluxCallbacksBoundariesAndNeighborWake`, `darkNodesConvertBiomesAndRespectOriginalFourthGiantCap`, `taintedNodesSpreadRealFibresAndInfectBeforeDarkEcology`, `taintBlocksPreserveOriginalSpreadLootAndFallingCustody` | Scoped reflective world-RNG identity inputs, restored in close/finally; branch-specific |

Other fixture RNG seeds intentionally select branch inputs without claiming to
restore a prior stream: ExcavationSpecial, Pech, KnowledgeFragment,
KnowledgeNotification, LootBag, FireBatWisp, PrimalCharm,
ResearchAspectCombination, Infusion, TaintSwarm, EldritchItems, UnstableNode,
Deconstruction and BrainyZombie helpers. These are **not** finally-restored
singletons and are not silently counted as such.

The shared `ChampionJoinRoll` helper is another **synchronous finally-restored
world-RNG facade**, not a settings replacement. Its caller files cover these
additional entry points (some overlap the tables above; only branches admitting
a hostile mob install the facade):

* `frostShardRicochetsDamagesAndPersists`, `golemUpgradesInstallPersistAndApplyEarthAirEntropy`, `fireIgnitesGuardTargetsAndOrderReplantsHarvestedCrops`, `orderGuardTargetSwitchesChooseWhomRealGuardsFight`, `embersFlyDamageIgniteAndExpire`
* `advancedGolemsGainSlotSpeedRangeAndKeepTheirFlag`, `golemAccessoriesInstallChangeStatsAndSurviveTheBell`, `golemFetterHoldsGolemsWhileItsRedstoneSignalLasts`, `golemDartLauncherShootsAtItsIntervalAndHurtsWhatItHits`
* `travelingTrunkItemUpgradesMenuBellDamageAndSaving`, `travelingTrunkFollowsTeleportsDefendsAndPullsItems`, `thaumiumFortressWardsSetsAndMasks`
* `warpDispatchesOriginalBranchesWithNativeEffects`, `brainyZombiesFightRageCureAndDropOriginalLoot`, `fireBatPreservesNativeAttacksVariantsAndLoot`, `wispsKeepOriginalCombatMovementAndEssence`, `flyingMonstersUseOriginalNaturalPopulationRules`
* `pechOriginalEntityUsesNativeGameplay`, `pechOriginalTradeUsesNativeGameplay`, `pechOriginalCurseUsesNativeGameplay`, `pechOriginalBeansUsesNativeGameplay`, `pechAvoidAndWatchGoalsRetainOriginalPlayerAndMutexRules`
* `taintBlocksPreserveOriginalSpreadLootAndFallingCustody`, `taintaclesKeepOriginalCombatAndRootedAi`, `taintaclesKeepOriginalRetaliationLifetimeAndLoot`, `taintaclesRequireNoInventedFloorOrPotionMutation`, `taintacleExperienceFollowsOriginalDeathTimeAndPlayerCredit`
* `eldritch_warden_caps_its_hits_and_keeps_its_ward`, `eldritch_golem_loses_its_head_and_hits_for_three_quarters`, `eldritch_crab_watcher_slime_and_zombie_keep_their_numbers`
* `cultist_faction_keeps_its_numbers_gear_and_loot`, `cultist_praetor_keeps_its_boss_behaviour`, `cultist_portal_runs_its_stage_machine`
* `champion_promotion_uses_the_original_bonuses`, `champion_modifiers_answer_their_tick`, `champion_modifiers_answer_their_hit_and_wound`, `champion_whitelist_covers_the_cave_spider`
* `bottle_of_taint_poisons_and_grows_fibres`, `golem_orb_homes_and_deals_the_original_damage`, `primal_arrow_keeps_its_damage_and_effects`

### Temporary process-global event listeners / probes

The following scenario entry points install a temporary Architectury listener or
test probe and remove it in local finally/scoped close/callback cleanup. Some
also have a native verdict listener; local finally remains the transaction scope.
No callback leak from these entry points was measured in the passing diagnostics:

* `unstableNodesEmitPaidAspectOrbsAndCanStabilise`
* `purifyingFluidPreservesOriginalClassicFlowWardAndSaltExpiry`, `pureBucketPreservesOriginalSourcePickupPlacementAndCustody`
* `excavationPreservesSpecialDropsAndDurableOutputs`, `excavationFocusChannelsAndPaysCompletedAttempts`, `excavationUpgradeTreasureAndEnlarge`, `excavationHarvestPreservesProtectionLootAndContainers`, `excavationExtraRemovalFailureAccounting`
* `combatFocusSpecialisationsKeepOriginalCastingAndProjectiles`, `hellbatFocusUsesOriginalTargetPaymentAndUpgradeGates`, `frostShardRicochetsDamagesAndPersists`, `shockFocusPaysTargetsAndRespectsCooldown`, `frostFocusCraftsSelectsAndPays`
* `shovelOfTheEarthmoverMinesAndLaysSquares`, `streamAxeChopsEndpointVacuumsAndFollowsDrops`, `tradeSwapsPayOnceAndRespectArchitectAndProtection`, `wardingPreservesOwnersStatesAndArchitectPayments`
* `taintBlocksPreserveOriginalSpreadLootAndFallingCustody`, `taintSporesGrowOnRealFibresAndBurstIntoActualSpiders`
* `lootBagsPreserveOriginalPoolsAndPaidNativeDropCustody`, `primalCharmUsesActualInventoryTicksForFreeOrbsAndClues`, `pechOriginalCurseUsesNativeGameplay`
* `crystallizerDurableEscrowCallbacksAndSpawnRetries`, `focalDurableDropRefusal`, `voidJarsPullOverflowAndKeepManualActions`, `harvestedNodeEssenceDistillsRealAura`
* `tubeFilterLabelReturnsSurviveNativeCancellationAndFailure`, `canceledTableOutputRetriesAfterCooldown`, `tableSpawnCallbackPreservesReplacementInventory`, `rejectedSpawnPreservesExistingHiddenUuidOwner`
* `fireIgnitesGuardTargetsAndOrderReplantsHarvestedCrops`
* `boreExternalRequestsCannotBorrowNativeOrigin`, `boreOrdinaryOreUsesRealNativeTicksAndNoXp`, `boreBaseEssentiaBindsThenPrefillsWhileOff`, `boreExhaustedRevisionRetainsOneOpaqueOwner`, `boreOuterDestroyCallbackRemainsClosed`
* `boreProtectionArchitecturyDeny`, `boreProtectionFabricDeny`, `boreProtectionAllowed`, `boreProtectionTargetAba`, `boreProtectionBeforeTop`, `boreProtectionAfterTop`, `boreProtectionAfterThrow`, `boreProtectionUnknownFalse`, `boreProtectionUnknownThrow` (scoped native probe/ThreadLocal is also closed/removed on unwind)
* `visAmuletsTransferAndPayThroughActualNativeRelay` and `primalCharmUsesActualInventoryTicksForFreeOrbsAndClues` additionally scope the embedded-player tick driver's two global listeners/owner map.

Additive registry bootstrap blocks/types and permanent production event wiring
are deliberately excluded: they are startup inputs, not temporary overrides.
Ordinary per-fixture blocks, entities, inventory/knowledge, local biome cells,
and reconstructible cache clears are not process-global replacements.
