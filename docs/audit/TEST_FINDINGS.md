# Test-suite adversarial audit — a3tests

## Scope and limits

Baseline: `c6feeda7`. **All line citations below refer to that baseline**, before these edits.
`J/` = `common/src/test/java/thaumcraft/`; `G/` = `common/src/gameTest/java/thaumcraft/common/testing/`.
`F/` and `N/` = respectively `fabric/src/gameTest/java/thaumcraft/fabric/testing/` and
`neoforge/src/gameTest/java/thaumcraft/neoforge/testing/`.

**613 modern Java test/support files screened by Python/grep**: common JUnit 388,
common GameTest support 204, Fabric GameTest 11, NeoForge GameTest 9, NeoForge JUnit 1.
These contain **2,479 `@Test` methods (2,467 common + 12 NeoForge)** and **632
GameTest entry methods (325 Fabric + 307 NeoForge)**. Loader entries are not 632 unique
behaviours; most delegate to common checks. Retained legacy `src/test` is not part of this count.

**41 files received targeted source reads**, including excerpts; **232 complete JUnit
test bodies and six GameTest entry bodies** received direct reading, plus supporting helpers.
The source-read inventory is in `docs/analysis/2026-10-23-audit-a3tests.md`.
Screening is NOT a claim to have manually audited 2,479 tests or all delegated scenarios.

**47 distinct weak/test-hygiene methods identified; 20 repaired, 27 left open.**
Primary categories below are disjoint (a method with several problems is counted once):

| Primary category | Methods | Repaired |
|---|---:|---:|
| No production observation / insufficient behavioural observation | 6 | 2 |
| Same expression on both sides | 2 | 2 |
| Copied formula/helper or production-derived expectation | 29 | 8 |
| Only null/no-throw assertions | 4 | 2 |
| Shared mutable RNG | 3 | 3 |
| Bootstrap failure silently skips checks | 3 | 3 |

Additionally: **10 infrastructure hygiene findings, four repaired**, not inflated into
hundreds of allegedly independent weak methods. No test was deleted. No production code changed.

**No Gradle, javac or suite run. Every fix is compile-unverified.** `git diff --check`
and Python fixture arithmetic are not substitutes for compilation or runtime validation.

## No production observation / insufficient behavioural observation

| Baseline file:line; method | Severity | Evidence / disposition |
|---|---|---|
| `J/common/blocks/CosmeticStairsTextureParityTest.java:68` `theSeedIsTheHashOfTheConcatenatedCoordinates` | High | Only tests private `oracleSeed`; production texture selection can be removed without this failing. **Fixed**: replaced with six-face golden outputs at four coordinates; deleted duplicated seed/RNG helpers. |
| `J/common/lib/events/ChunkRegenerationParityTest.java:72` `theChunkWithoutTheMarkerIsNotMarked` | High | Only asks vanilla `CompoundTag` about a fresh tag. **Fixed**: calls production `ChunkRegeneration.hasMarker`. |
| `J/client/renderers/tile/MirrorProjectionParityTest.java:29` `homogeneousRescalingKeepsTheSameProjectedCoordinates` | High | Homogeneous rescaling is a property of any linear matrix, including identity. Replacing the texture matrix with identity still satisfies this property. **Open**: needs independently pinned projected coordinates as well. |
| `J/essentia/CodeChickenGeometryParityTest.java:394` `theNativeGeometryHasNoCodeChickenLeft` | Medium | Tests only `TubeGeometry`'s own package name, not whether vendored CodeChicken is shipped/referenced. Reintroducing the library leaves it green. **Open**: inspect built artifact entries/dependencies. |
| `J/wands/FocusVisArithmeticTest.java:18` `centivisTruncationZeroAspectsAndClampAreNotRoundedOrFreeRecipes` | High | Cost assertions perform casts/multiplication in the test, never call a payment/cost path. Removing actual centivis spending leaves them green; input-validation assertions do not cover payment. **Open**: exercise actual cost/debit API. |
| `G/EquipmentItemRuntimeChecks.java:31` `boneBowKeepsItsOriginalDrawAndDamage` | High | Creates a bow but never uses/releases it or observes an arrow. Most assertions call `EquipmentRules`; making `ItemBowBone.releaseUsing` a no-op (`common/src/main/java/thaumcraft/common/items/equipment/ItemBowBone.java:73-97`) leaves this scenario green. **Open**: real draw/fire, ammo, durability, projectile speed/damage. |

## Same expression on both sides

| Baseline file:line; method | Severity | Evidence / disposition |
|---|---|---|
| `J/common/blocks/CosmeticStairsTextureParityTest.java:91` `theTextureIndexDependsOnThePositionAndIsStable` | Medium | `:98` compares identical production calls; other assertions duplicate the RNG. **Fixed**: literal outputs and an intervening different-position lookup. |
| `J/common/lib/network/ResearchTableEditPayloadTest.java:135` `theEncodedFrameIsDeterministic` | High | `wire(payload)` versus `wire(payload)` accepts a deterministic empty/constant encoder. **Fixed**: explicit 26-byte native wire fixture, checked before/after encoding a different payload. This is a native protocol contract, not a legacy wire-format claim. |

## Copied formula/helper or production-derived expectation

These are not all tautologies: many WOULD detect a one-sided implementation mutation. Their
defect is lack of an independent oracle, especially when authoring/correcting both copies together.
Simple unit-conversion arithmetic is lower risk than branch-for-branch duplicated algorithms.

| Baseline file:line; method(s) | Count | Severity | Evidence / disposition |
|---|---:|---|---|
| `J/tiles/TileParityRulesTest.java:54` `theFluidHeightOfTheCrucibleFollowsTheOriginalCurve` | 1 | Medium | Private `crucibleHeight` duplicates production curve and two clamps. **Fixed**: `.3`, `.8`, `.9999`, `1.001` float literals. |
| `J/equipment/LegacyItemsRulesTest.java:10` `boneBowDrawsTwiceAsFastAsTheVanillaBowAndCapsAtOne` | 1 | Medium | Private bow-power helper duplicates arithmetic. **Fixed**: pinned single-precision results for draws 1, 2, 5. |
| `J/items/MagicWardTest.java:10` `aPieceAbsorbsItsArmorOverThirtyFive` | 1 | Low | Expected ratios are the same divisions as `MagicWard.ratio`. **Fixed**: exact double literals. |
| `J/items/MagicWardTest.java:18` `theFullSetTakesSixteenThirtyFifthsOfTheDamageInOneStep` | 1 | Medium | Private `wardedDamage` duplicates Forge-style damage scaling. **Fixed**: `10.857142F`, `16F`; helper removed. |
| `J/common/blocks/CosmeticStairsTextureParityTest.java:61` `theTextureIndexMatchesTheOracleForEverySide` | 1 | Medium | Runtime copy of concatenated-coordinate hash and RNG. **Fixed**: frozen six-face arrays. |
| `J/client/renderers/item/WandContextPoseParityTest.java:87,105,120` inventory, dropped-item, hand tests | 3 | Medium | `expected` calls production `kindOf` then composes the same branches/transforms; separate scalar assertions partly mitigate. **Open**, including leader's already-known matrix finding. |
| `J/client/renderers/item/WandUsePoseParityTest.java:38,51,64` raise, original-sine, running-wave tests | 3 | Medium | Expected raise is derived from production `useTicks`; four sine expressions and later-wave expectation duplicate the implementation. Literal raise checks mitigate the first. **Open**. |
| `J/client/renderers/tile/JarRendererParityTest.java:26,47` brain-bob and wrapped-turn tests | 2 | Medium | Sine bob and wrap arithmetic copied into expected values; some independent endpoints mitigate. **Open**. |
| `J/client/renderers/item/ThaumometerLightmapParityTest.java:26,52` brightness-split and sine tests | 2 | Medium | Input brightness comes from production, expected split uses duplicate division helpers; expected sine is recomputed. **Open**: explicit brightness/light tuples. |
| `J/assets/TravelingTrunkClientAssetsTest.java:54` `theRendererUsesTheOriginalScaleAndLidFormulas` | 1 | Medium | Duplicates scale arithmetic and cubic lid easing in local variables. **Open**: pinned scale vectors/angles, including negative zero. |
| `J/wands/FocusVisArithmeticTest.java:8` `castingKeepsSeparateFloatSubtractionsWhileRecipesIgnoreFrugal` | 1 | Medium | Nested loops reproduce separate float subtractions and floor verbatim. **Open**: literal bit-pattern fixtures for discriminating inputs. |
| `J/items/SpecialMiningRuleTest.java:12` chance-growth test | 1 | Low | Exact same expected arithmetic. **Fixed**: `.2F`, `.275F`, `.35000002F`, `.425F`. |
| `J/crash/SpecialMiningSafetyTest.java:83` `theChanceIsTheOriginalOne` | 1 | Low | Same chance expression. **Fixed**: literal results. |
| `J/common/lib/utils/EntityUtilsParityTest.java:40` padding test | 1 | Low | `Math.max` and width multiplication duplicated. **Fixed**: `.8F`/`8F` oracle outputs. |
| `J/golems/GolemAdvancedTest.java:57,66` speed/range tests | 2 | Medium | Ordered float products and range bonus duplicated; overload-to-overload comparisons cannot independently pin both. **Open**. |
| `J/golems/GolemDartRulesTest.java:42,51,66` spread/damage/reach tests | 3 | Medium | Copied subtraction/product; reach boundary even imports production `REACH_FACTOR`. **Open**: literal float-widened boundaries/damage. |
| `J/essentia/ReservoirLiquidTest.java:24,56` liquid-top/colour-walk tests | 2 | Medium | Expected capacity interpolation and first colour step repeat arithmetic. End colours and monotonicity partly mitigate. **Open**. |
| `J/client/renderers/tile/MirrorProjectionParityTest.java:20` outer-layer matrix-order test | 1 | Medium | Expected UV is calculated from production `q`; a coupled error can agree. **Open**: fixed q and UV fixtures. |
| `J/wands/WandSceptreTest.java:23` capacity test | 1 | Low | Loop computes expected capacity from the production rod catalogue. Existing literal capacities mitigate but do not independently cover every row. **Open**. |

### Fixture provenance checked for repairs

Oracle root: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/`.
These are original-behaviour citations, not a claim that generated tests existed in the original:

* `thaumcraft/common/tiles/TileCrucible.java:315-324` versus native
  `common/src/main/java/thaumcraft/common/lib/tiles/TileParityRules.java:62`.
* `thaumcraft/common/items/equipment/ItemBowBone.java:79-86` versus native
  `common/src/main/java/thaumcraft/common/lib/items/EquipmentRules.java:53`.
* `thaumcraft/common/items/armor/ItemVoidRobeArmor.java:229-239` supplies magic ratio;
  native `common/src/main/java/thaumcraft/common/lib/MagicWard.java:45-51` applies reduction.
  Forge aggregation itself was not separately re-audited here.
* `thaumcraft/common/blocks/BlockCosmeticSolid.java:157-162` supplies hash/RNG;
  `thaumcraft/common/blocks/BlockCosmeticStairs.java:38-40` supplies side+100.
  Frozen new face vectors were evaluated once in Python with Java signed-int/hash/48-bit RNG
  semantics; they were NOT obtained from the production method or claimed as JVM measurements.
* Bow/mining single-precision values were independently evaluated with Python `struct`
  rounding at float operations. Mining provenance was checked at
  `thaumcraft/common/lib/events/EventHandlerWorld.java:182`, versus native
  `common/src/main/java/thaumcraft/common/lib/utils/SpecialMining.java:62`.
  Repaired tests' inherited off-by-one oracle citations were corrected.

## Only null/no-throw assertions

| Baseline file:line; method | Severity | Evidence / disposition |
|---|---|---|
| `J/crash/NullSafeRegistryLookupTest.java:32` missing crop key | Low | Only `assertDoesNotThrow`; **fixed** to exact native `"#3"` contract (`CropUtils.java:41-46`). |
| `J/research/ResearchNoteCopyingTest.java:64` already-learned copy | Medium | Non-null alone accepts wrong payment/count/knowledge. **Fixed**: count, copy counter, costs, remaining pools, retained research, unchanged input knowledge. |
| `J/research/ResearchRecipeCoverageTest.java:323` ordinary versus gated recipes | Medium | Only non-null gates after production-driven enumeration; an empty mapping executes no assertion. **Open**: require nonzero expected gated set and exact gates. |
| `J/common/blocks/CustomStepSoundParityTest.java:77` sound events exist | Low | Only existence of events/sample streams, not sound-event sample binding or valid audio. Helper also checks nonempty document, so this is not literally assertion-free. **Open**: resource smoke contract is useful but not playback parity; sample streams are not closed. |

## Shared state / order / skipped coverage

| Baseline file:line; method(s) | Count | Severity | Disposition |
|---|---:|---|---|
| `J/eldritch/EldritchBlockRulesTest.java:60,101,342` drops, maze-size, loot-count tests; static RNG at `:12` | 3 | Medium | Tests consume one shared mutable stream. Current range assertions are broad enough that order may not change pass/fail, but sampled inputs depend on order. **Fixed**: per-method seeded RNG. |
| `J/world/OuterLandsEldritchVariantTest.java:178,195,206` two resolver tests and lock-key test | 3 | High | Registry setup failure becomes `assumeTrue` abort, not failure. **Fixed**: explicit required-fixture assertions. Underlying registry mutation remains open below. |

## Infrastructure hygiene (not counted as extra test methods)

| Baseline file:line | Severity | Evidence / disposition |
|---|---|---|
| `J/blocks/BlockVisibilityTestRegistry.java:110-150,191-195,210-235` | High | Constructs ~24 blocks, writes global built-in registry and reflectively toggles bootstrap flag; swallows setup failures. Listener is installed in `common/src/test/resources/META-INF/services/org.junit.platform.launcher.LauncherSessionListener:2`. Violates no-Block JUnit rule. **Open**: move native block checks to GameTest; not safe to delete fixture or replace it with another bootstrap hack. |
| `J/world/EldritchTestRegistry.java:49-74,87-108` | High | Constructs/registers six blocks under real mod IDs before bootstrap; reflective global flag mutation. Installed by service file `:1`. Duplicate session construction can leave new holders unregistered because duplicate IDs are skipped. **Open**, same required migration. |
| `G/GolemChunks.java:27-33` | High | Throws during chunk acquisition before returning ownership list: caller cannot release already-forced chunks. **Fixed**: rollback in `finally` unless acquisition completed. |
| `G/WaveLane.java:56-57` | Medium | Roof construction can fail after tickets acquired but before caller gets lane instance. **Fixed**: constructor failure cleans acquired lane. |
| `G/GrateRuntimeChecks.java:75-82` | Medium | Force/getChunk loop precedes cleanup-protected try. **Fixed**: acquisition moved inside existing failure-cleanup scope. |
| `G/LevitatorRuntimeChecks.java:79-86` | Medium | Same acquisition-before-try problem. **Fixed**, same change. |
| `F/mixin/GameTestHelperMockPlayerMixin.java:34,50-70` | High | Cleanup only hooks helper `succeed` and `fail(String)`; assertion exceptions/timeouts can bypass both. WeakHashMap GC is not player-list removal. Cleanup also swallows removal exceptions. **Open**: lifecycle completion listener owning player resources on success, failure and timeout. |
| `N/mixin/GameTestHelperMockPlayerMixin.java:34,50-70` | High | Same duplicated lifecycle hole. **Open**; no NeoForge execution performed. |
| `G/GrateRuntimeChecks.java:74,94-97` | Medium | Scenario works at relative Y=100 and extends +40 X; cleanup discards tracked entities/unforces chunks but does not restore changed blocks outside structure bounds. **Open**: owned-state snapshot/restore, including scheduled callbacks. |
| `G/LevitatorRuntimeChecks.java:78,88-97,152-155` | Medium | Same, with fixtures up to +48 X at Y=100; cleanup leaves machines/power blocks. **Open**: snapshot/restore outside structure. |

## Negative findings and rejected false positives

* No confirmed entirely assertion-free JUnit method or bare-succeed common scenario in the
  inspected bodies. Assertion-free-looking methods commonly call asserting helpers; comparing
  raw assertion counts to annotation counts alone is not evidence of a defect.
* `SurfaceStructurePlacementRuntimeChecks.java:101` has `assertTrue(true)` but is inside a
  successful physical `contains` branch followed by a node assertion. It is redundant, not a
  whole no-op test. That scenario checks real generated blocks.
* `ParticleDeflectionTest.java:111-121` repeats RNG arithmetic but ALSO pins literal speed and
  jitter; it is not counted as a weak method merely for redundant expected arithmetic.
* Invalid-input tests passing `Float.NaN` to assert rejection are correct, not NaN-equality
  bugs. No confirmed expected-NaN equality or locale/OS-default defect in the modern files screened.
* Raw float `==` exists in GameTests (e.g. `EquipmentItemRuntimeChecks.java:47`). Exact
  saturated endpoints are reasonable; dynamic arithmetic comparisons need separate tolerance
  review. They are not all automatically failures and were not bulk-rewritten.
* `LootBagRegistrationTest.java:28-40` snapshots/restores global lists; it is not reported as
  an order leak merely because it mutates them. `RunicSettingsTest.java:19` resets settings.

## Tests I could not judge

* The remaining 2,247 screened JUnit bodies and most delegated GameTest behaviours were not
  line-by-line audited. A regex screen does not establish their independence from implementation.
* `OracleMazeGenerator`, the ray-tracer helpers in `CodeChickenGeometryParityTest`, and the
  model animation oracle-table helpers require original-source/bytecode differential review;
  an independent legacy algorithm may be legitimate, so duplicated-looking helpers were not
  automatically condemned. Only the package-name test above is confirmed weak in that class.
* Surface totem search now defers scattered-temple lookup until rarity candidates
  (`G/SurfaceStructurePlacementRuntimeChecks.java:318-341`). Its earlier `structureBuilt`
  call still invokes structure preflight (`common/src/main/java/thaumcraft/common/lib/world/AuraGenRules.java:170-175`).
  No runtime heap/chunk measurement was allowed; I cannot certify the entire search as cheap
  or rule out all siblings of the historic 131,072-chunk issue.
* Shared forced-chunk ownership across concurrent scenarios and timeout cleanup for every
  other resource-owning fixture need lifecycle-level auditing. Local success/failure `finally`
  blocks do not establish timeout safety.

## Required leader follow-up

Compile/run sequentially after cherry-pick, then deliberately mutate: texture matrix to
identity, bone-bow firing to no-op, centivis debit to no-op, and recipe mapping to empty.
The open tests identified above must not be accepted as proof against those regressions.
The repaired golden fixtures also need real target compilation and execution before acceptance.
