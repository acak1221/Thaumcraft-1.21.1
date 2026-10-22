# f3tests — remaining 27 method findings

Baseline `0464d084`, branch `agent/f3tests`. This addresses the **27 method findings**,
not the separate infrastructure findings in `TEST_FINDINGS.md`. No tests were deleted.
No production behaviour is changed in the final diff.

## Fixture provenance

`O/` below means `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/thaumcraft/`.
Numeric results are **offline evaluations of the cited original statements**, frozen as
literals, not claims that the original source literally printed every resulting decimal.
Python `struct.pack/unpack('f')` rounded each float operation. Legacy sine fixtures use
the 65,536-entry sine-table semantics (index factor `10430.378F`), not production `Mth`.
Matrix fixtures use the original OpenGL row order, independently evaluated; JOML is
used only to store/observe the matrices in the tests, not to calculate their expectations.

The recipe fixture has 244 original-key/native-gate entries. Static recipe keys/gates
come from `O/common/config/ConfigRecipes.java:113-394`; loops at :113, :188 and :219
are expanded literally. Three construct keys resolve to existing native **blueprint**
recipes: `AdvAlchemyFurnace` → AdvAlchemyConstruct, `InfusionAltar` → InfusionMatrix,
`Thaumatorium` → AlchemicalConstruct. The last blueprint has `DISTILESSENTIA`, not
the finished construct's `THAUMATORIUM` trigger gate (`O/common/items/wands/WandManager.java:729`).
This test checks recipe mapping, not permission to build the completed multiblock.

Sound bindings and eight sample hashes were also read directly from the worktree's
read-only `Thaumcraft-1.7.10-4.2.3.5.jar` (SHA-256
`3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be`), entries
`assets/thaumcraft/sounds.json` and `assets/thaumcraft/sounds/{jar1,jar2,jar3,jar4,urnbreak,gore1,gore2,crystal}.ogg`.
The original's unqualified sample names are explicitly namespaced in native JSON;
all eight native audio byte streams matched the original. Literal original SHA-256
values in the test additionally reject header-shaped garbage or substituted audio.

## Per-finding ledger

Test paths are relative to `common/src/test/java/thaumcraft/`, except `G/` =
`common/src/gameTest/java/thaumcraft/common/testing/`. “Mutation” names the deleted or
corrupted feature the test must detect; execution evidence is recorded separately below.

| # | Test | Previously | Now / mutation caught | Oracle citation |
|---|---|---|---|---|
| 1 | `client/renderers/item/WandContextPoseParityTest.theInventoryRollsTheWandBySixtySixDegreesAndLiftsIt` | Branch selected by production; expected matrix recomposed | Literal full wand/staff matrices; catches deleted/reordered inventory pose or wrong branch | `O/client/renderers/item/ItemWandRenderer.java:59-86` |
| 2 | Same class, `theDroppedItemIsLiftedByOneBlockAndTheStaffByOneAndAHalf` | Repeated transform chain | Literal full matrices, staff origin Y=2, scale=.9; catches dropped pose deletion | Same :59-71 |
| 3 | Same class, `theHandMovesTheWandAndTheFirstPersonStretchesIt` | Production-selected expected branch | Literal full matrices for both hands, first/third person, wand/staff; catches deleted hand transform/stretch | Same :72-79 |
| 4 | `client/renderers/item/WandUsePoseParityTest.theRaiseIsSixtyDegreesOverThreeTicks` | Expected formula fed by production cap | Literal 10/45/60 degree composed cap/raise samples plus 0/20/40/60 endpoints; catches deleted raise/cap | Same :89-100 |
| 5 | Same class, `theWaveOfTheFocusUsesTheOriginalSine` | Four copied runtime sine expressions | Six frozen four-component wave/charge tuples, positive and negative phases; catches missing animation/wrong periods | Same :102-109 |
| 6 | Same class, `theWaveKeepsRunningAfterTheRaiseIsFull` | Copied sine at tick 40 | Literal -7.5677032 at tick 40; catches capped or removed wave | Same :102 |
| 7 | `client/renderers/tile/JarRendererParityTest.theBrainBobsWithTheOriginalSine` | Copied sine | Six nonendpoint frozen values plus original endpoints; catches no bob/wrong period | `O/client/renderers/tile/TileJarRenderer.java:157` |
| 8 | Same class, `theBrainTurnWrapsTheDifferenceIntoOneCircle` | Copied wrap expression | Literal wrapped angles in both directions and partial frame; catches removed wrapping/interpolation | Same :160-165 |
| 9 | `client/renderers/item/ThaumometerLightmapParityTest.theBrightnessOfTheOriginalSplitsIntoBlockLightAndNoSkyLight` | Production brightness + copied split | Literal brightness/block/sky/packed tuples including split boundary and nonzero sky control; catches swapped/missing light halves | `O/client/lib/UtilsFX.java:766-786`, vanilla fixed-function light units |
| 10 | Same class, `theBrightnessIsTheOriginalSine` | Copied Math.sin expression | 25 literal integer brightness samples and jitter-shift controls; catches constant/no sine/ignored jitter | `O/client/renderers/item/ItemThaumometerRenderer.java:157` |
| 11 | `assets/TravelingTrunkClientAssetsTest.theRendererUsesTheOriginalScaleAndLidFormulas` | Copied scale/easing expressions | Literal side/height ordinary/Earth vectors; literal -0.0 and -1.3744469 lid angles; catches deleted scale/Earth/easing/sign | `O/client/renderers/entity/RenderTravelingTrunk.java:38-48` |
| 12 | `wands/FocusVisArithmeticTest.castingKeepsSeparateFloatSubtractionsWhileRecipesIgnoreFrugal` | Nested copy of production arithmetic | Seven literal casting/recipe IEEE bit pairs, discriminating .9/10/3 and clamp cases; catches merged subtractions/Frugal leaking into recipes | `O/common/items/wands/ItemWandCasting.java:247-259` |
| 13 | `golems/GolemAdvancedTest.speedIsAirThenTimesOnePointOne` | Copied ordered products | Literal .418/.5434/.3795 single-precision outputs for both overloads; catches removed Air/advanced boost | `O/common/entities/golems/EntityGolemBase.java:372-379` |
| 14 | Same class, `rangeAddsTwentyPercentOrTwoWhicheverIsLarger` | Copied bonus and overload comparisons | Literal ordinary 16/20/24 and advanced 19.2/24/28.8; catches deleted Water/advanced bonus | Same :460-469 |
| 15 | `golems/GolemDartRulesTest.theSpreadNarrowsByOneAndThreeQuartersForEveryWaterUpgrade` | Copied subtraction | Literal 7/5.25/3.5/0/-1.75; catches removed Water narrowing or clamp | Same :1182 |
| 16 | Same class, `theDartDoesFourTenthsOfTheAttackAsAFloatProduct` | Copied product | Literal widened doubles 2.4000000953674316/3.200000047683716/1; catches deleted/wrong damage or double arithmetic | Same :1183-1184 |
| 17 | Same class, `aShotNeedsTheTargetWithinEightyPercentOfTheRange` | Production REACH_FACTOR and copied square | Literal float-widened boundaries and immediately higher double values at ranges 16/17.6; catches missing gate/wrong factor/double square | `O/common/entities/ai/combat/AIDartAttack.java:64-65` |
| 18 | `essentia/ReservoirLiquidTest.theTankGrowsWithTheStoredUnits` | Copied capacity interpolation | Literal .1875/.18994140625/.34375/.5/.8125; catches deleted/wrong liquid-height interpolation | `O/client/renderers/tile/TileEssentiaReservoirRenderer.java:71-73` |
| 19 | Same class, `theColourWalksToTheAspectColourInTwentyTicks` | Copied first-step arithmetic | Literal first RGB and twentieth-step RGB; catches stopped interpolation/wrong colour/interval | `O/common/tiles/TileEssentiaReservoir.java:209-216`, `O/api/aspects/Aspect.java:28` FIRE colour `0xff5a01` |
| 20 | `client/renderers/tile/MirrorProjectionParityTest.outerLayerPreservesOriginalTimeAndMatrixOrder` | Expected UV derived from production q | Literal independent q input and output UV/w; catches identity/removed projection and wrong q | `O/client/renderers/tile/TileMirrorRenderer.java:101-107` |
| 21 | `wands/WandSceptreTest.aSceptreHoldsOneAndAHalfTimesTheCapacityOfItsRod` | Expected capacity from production catalogue | All 18 literal tag/ordinary/sceptre rows, exact tag set; catches wrong catalogue row/factor | `O/common/config/ConfigItems.java:306-325`, `O/common/items/wands/ItemWandCasting.java:119-121` |
| 22 | `wands/FocusVisArithmeticTest.centivisTruncationZeroAspectsAndClampAreNotRoundedOrFreeRecipes` | Local casts, no cost API | Calls actual FocusVis.price and invalid-price/debit rejection; registered-wand truncation/debit companion in `G/AccessoriesRuntimeChecks.focusCentivis`: 1/0/5 debits and 13→87, atomic rejection | `O/common/items/wands/ItemWandCasting.java:247-259,297-336` |
| 23 | `research/ResearchRecipeCoverageTest.onlyOrdinaryCraftingRecipesStayWithoutAResearchGate` | Non-null gate in possibly empty loop | Exact 244-key set and exact oracle gates; ordinary recipes must have null gates; catches empty mapping and wrong-but-nonnull gate | `O/common/config/ConfigRecipes.java:113-394`; blueprint qualifications above |
| 24 | `common/blocks/CustomStepSoundParityTest.theSoundEventsOfTheCustomSoundsExist` | Non-null event/sample streams; leaked streams | Exact original event→ordered sample binding and stream=false, eight original sample SHA-256 values, closed streams and Ogg/Vorbis headers; catches empty/misbound event, corrupted or substituted samples | Original JAR `assets/thaumcraft/sounds.json` and eight Ogg entries cited above; **not audible playback parity** |
| 25 | `client/renderers/tile/MirrorProjectionParityTest.homogeneousRescalingKeepsTheSameProjectedCoordinates` | Any matrix, including identity, passes | Adds literal projected layer-3 coordinates .6244919/.0519281 and q=.3; catches identity/removed projection | `O/client/renderers/tile/TileMirrorRenderer.java:101-107` |
| 26 | `essentia/CodeChickenGeometryParityTest.theNativeGeometryHasNoCodeChickenLeft` | Only own package name | Scans built common JAR entries and class bytes plus resolved runtime JAR entries, nonempty archive control; catches shipped/referenced CodeChicken | Native migration dependency-removal contract, not an oracle gameplay value |
| 27 | `G/EquipmentItemRuntimeChecks.boneBowKeepsItsOriginalDrawAndDamage` | Audit baseline helper-only; current baseline had partial firing check | Literal native item values; fires real item; exact projectile count/damage/noncritical, bounded 2.5 speed, exact survival ammo/durability, no projectile/ammo/durability for short draw; catches releaseUsing no-op | `O/common/items/equipment/ItemBowBone.java:79-130` |

## Execution and mutation evidence

### JUnit mutation run — verified for each of rows 1–26

Actual Java 21/Gradle execution, not source inspection: `gradle-f3-mutations.log`.
The focused 14-class selection executed **85 tests, 39 failed, zero skipped**.
All **26 repaired JUnit methods** appear as failing testcases in the XML retained
locally at `.tmp/f3tests/mutation-results/`. Extra failing unchanged methods are
not counted as repaired findings. Mutations were batched across independent feature
families; this is deletion-sensitivity evidence, not an exhaustive one-mutant-at-a-time
mutation score or proof that every possible wrong constant is detected.

| Finding rows (each verified red) | Temporary mutation | Observed failure |
|---|---|---|
| 1–3 | Context pose forced to NONE | Full matrix origin/rotation mismatches |
| 4–6 | Raise and waveRoll return zero | 20 vs 0; .24924606 vs 0; -7.5677032 vs 0 |
| 7–8 | Jar bob and turn return zero | .03 vs 0; 1 vs 0 |
| 9–10 | Block-light half and brightness return zero | Block 11 vs 0; brightness 200 vs 0 |
| 11 | Trunk scale becomes {1,1} | .952381 vs 1 |
| 12 | Merge gear and Frugal into one percentage subtraction | IEEE bits 1062836635 vs 1062836634 |
| 13–14 | Initial golem speed/range become zero | .38 vs 0; 16 vs 0 |
| 15–17 | Dart spread/damage zero; reach predicate false | 7 vs 0; 2.4000000953674316 vs 0; boundary false |
| 18–19 | Liquid height zero; disable colour-selection branch | .1875 vs 0; display FIRE vs null |
| 20,25 | Mirror texture matrix is identity | .16604477 vs 2; projected .6244919 vs 6.6666665 |
| 21 | Wand capacity API returns zero | 2500 vs 0 |
| 22 | Actual FocusVis.price returns empty map | Exact four-aspect cost map vs {} (after scalar modifier check passed) |
| 23 | Native recipe mapping returns empty map | Exact 244-key gated set vs empty |
| 24 | Bind first jar sample to gore1 instead | Exact ordered jar binding mismatch |
| 26 | Include `codechicken/f3-mutation.marker` in production resources | Built JAR scan finds the shipped entry |

Every production mutation was then restored with an inverse patch; `git diff --stat
-- common/src/main` is empty. The temporary artifact marker is deleted, not shipped.
An intermediate rerun additionally caught its leftover empty `codechicken/` directory
in the JAR; both mutation-created empty source/output directories were removed and the
JAR rebuilt. The built archive is an explicit test-task input as well as a dependency,
so artifact-only changes cannot silently reuse an unrelated cached audit result.

### Clean and native-runtime verification

* Before mutations: `:common:test :common:compileGameTestJava` **passed**, with
  **2,470 JUnit tests, zero failed, zero skipped**; both modified native scenario
  support classes compiled against the actual Minecraft/loader dependencies.
* First clean Fabric focused run: **2/2 required GameTests passed**, including the
  registered-wand debit companion. Command: `:fabric:runGameTest
  -PgameTestFilter=bone_bow_keeps_its_original_draw_and_damage,accessoriesUseNativeMenuPersistenceAndDeathCustody
  -PgameTestHeap=768M`; log `gradle-f3-runtime.log`.
* **Row 27 mutation verified**: temporarily return immediately for players in
  `ItemBowBone.releaseUsing`. The native test failed with “the bone bow fires exactly
  one arrow when it is released at full charge: 0”.
* **Row 22 native debit companion mutation additionally verified**: temporarily return
  true without changing vis in `FocusVis.debit`. The native test failed with
  “actual component loses 1/0/5, not rounded prices or whole vis”. Both native
  testcases were explicitly red in `gradle-f3-native-mutations2.log` (2 required
  failures). They mutate separate paths in separate scenarios, not one shared helper.
* The first native mutation attempt had a cleanup NPE masking the debit assertion:
  the added check initially ran before `f.open()`. It now runs after fixture setup;
  the masking exception is **not** counted as debit-mutation evidence.
* Both native production mutations were restored. Final clean combined run
  (`gradle-f3-final2.log`) **passed**: `:common:jar --rerun :common:test --rerun
  :fabric:runGameTest` with the two filters above. All **2 required Fabric GameTests
  passed** after restoration; the full common suite was executed, not skipped.
* After the final Gradle archive-input configuration change, a further explicit
  `:common:test --rerun` **passed in 14s** (`gradle-f3-final-junit.log`). Final XML:
  **2,470 tests, zero failures, zero skipped**. No production changes remain.

Thus **27/27 findings have executed mutation evidence**, with no deliberately
unrepaired method findings. This is not a claim that the separate infrastructure
findings or the entire port's client/multiplayer acceptance are finished.
