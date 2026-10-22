# a3tests — adversarial suite inspection

Branch: `agent/a3tests`; baseline `c6feeda7`. Work confined to `/home/cyberfox-kali/wt/a3tests`.
Required initial status was clean. PLAN §§3.9–3.10 read from the requested reference path;
the reference checkout was not modified. No other worktree was accessed.

## Result

See `docs/audit/TEST_FINDINGS.md`: **47 weak/hygiene test methods, 20 repaired, 27 open;
10 additional infrastructure findings, four repaired.** No tests deleted, no production edits.
The most serious false confidence is in tests that never observe the claimed feature:
local-only stairs seed and missing-marker checks (repaired), homogeneous mirror projection,
test-side centivis casts, package-name-as-dependency-absence, and a bone-bow runtime scenario
that never fires the bow (open).

The two session listeners constructing blocks are an explicit no-Block-rule violation,
not evidence that the JVM is safely isolated. They require migration to native GameTests.
Mock-player cleanup is not completion-safe for assertion exceptions/timeouts in either loader.

## Method and counts

Python/grep screened **613 modern Java test/support files**, containing **2,479 JUnit
methods and 632 loader GameTest entry definitions** (325 Fabric / 307 NeoForge; mostly
shared implementations). Common JUnit contributes 2,467; NeoForge JUnit contributes 12.
The retained legacy test tree is excluded from these counts.

Passes: per-method brace extraction for no/trivial assertions and <3 statements;
per-file assertion/annotation counts; identical-expression checks; private-static helper
inventory; expected-expression arithmetic; block construction/factory registration;
mutable statics and assumption gates; chunk forcing, entity spawning and mock-player creation;
missing exception cleanup and bare-success scenarios; locale/default charset and NaN patterns.
Candidates were read before being labelled findings. Constant pins and meaningful asserting
delegates were not treated as empty tests.

**41 targeted source-read files** are listed below. Some were excerpt reads (marked `*`).
This encompasses **232 fully read JUnit bodies**, six GameTest entry bodies, and support
helpers. It is deliberately not described as a full manual audit of every screened method.

## Targeted source-read inventory

Paths below are relative to the repository. `J` = `common/src/test/java/thaumcraft`,
`G` = `common/src/gameTest/java/thaumcraft/common/testing`.

1. `J/tiles/TileParityRulesTest.java`
2. `J/equipment/LegacyItemsRulesTest.java`
3. `J/items/MagicWardTest.java`
4. `J/blocks/BlockVisibilityTestRegistry.java`
5. `J/world/EldritchTestRegistry.java`
6. `J/client/renderers/item/WandContextPoseParityTest.java`
7. `J/wands/WandSceptreTest.java`
8. `J/common/blocks/CosmeticStairsTextureParityTest.java`
9. `G/GolemChunks.java`
10. `fabric/src/gameTest/java/thaumcraft/fabric/testing/mixin/GameTestHelperMockPlayerMixin.java`
11. `J/client/renderers/item/WandUsePoseParityTest.java`
12. `J/client/renderers/tile/JarRendererParityTest.java`
13. `J/client/renderers/tile/MirrorProjectionParityTest.java`
14. `J/client/renderers/item/ThaumometerLightmapParityTest.java`
15. `J/eldritch/EldritchBlockRulesTest.java`
16. `J/crash/NullSafeRegistryLookupTest.java`
17. `J/research/ResearchNoteCopyingTest.java`
18. `J/wands/FocusVisArithmeticTest.java`
19. `J/items/SpecialMiningRuleTest.java`
20. `J/assets/TravelingTrunkClientAssetsTest.java`
21. `G/EquipmentItemRuntimeChecks.java`
22. `J/common/lib/events/ChunkRegenerationParityTest.java`
23. `J/common/lib/network/ResearchTableEditPayloadTest.java`
24. `J/loot/LootBagRegistrationTest.java`
25. `J/common/blocks/CustomStepSoundParityTest.java`
26. `J/research/ResearchRecipeCoverageTest.java` * (`310–364`; count only one annotated complete target body)
27. `G/SurfaceStructurePlacementRuntimeChecks.java` * (`45–119`, `270–381`)
28. `G/LevitatorRuntimeChecks.java` * (`50–169`)
29. `G/GrateRuntimeChecks.java` * (`45–109`)
30. `J/runic/RunicSettingsTest.java`
31. `G/WaveLane.java`
32. `J/golems/GolemAdvancedTest.java`
33. `J/golems/GolemDartRulesTest.java`
34. `J/client/fx/ParticleDeflectionTest.java` * (`105–135`)
35. `J/crash/SpecialMiningSafetyTest.java` * (`75–88`)
36. `J/common/lib/utils/EntityUtilsParityTest.java`
37. `J/essentia/ReservoirLiquidTest.java`
38. `J/blocks/BlockVisibilityAndPistonTest.java`
39. `neoforge/src/gameTest/java/thaumcraft/neoforge/testing/mixin/GameTestHelperMockPlayerMixin.java`
40. `J/world/OuterLandsEldritchVariantTest.java` * (`170–217`)
41. `J/essentia/CodeChickenGeometryParityTest.java` * (`385–401`)

Counts exclude production and oracle files opened for comparison and the service-resource read.

## Repairs

* Removed five copied helper methods across four test families (crucible height, bow power,
  ward reduction, and the stairs' seed and RNG helpers).
  Assertions now pin outputs rather than executing expected algorithms at test runtime.
* Replaced local-only marker and stairs seed checks with production observations.
* Replaced identical wire-encode comparison with a native golden byte vector and an
  intervening encode; stairs stability similarly has a pinned result and intervening query.
* Strengthened null-only crop-key/copy-plan checks; localized three shared RNG consumers;
  made three registry-fixture assumption aborts fail explicitly.
* Roll back partial chunk acquisition in `GolemChunks`, clean failed `WaveLane` construction,
  and protect Grate/Levitator acquisition loops with their existing cleanup scopes.

## Verification and residual risk

**Compile-unverified. No Gradle, javac, JUnit or GameTest execution was performed.**
`git diff --check` only checks patch whitespace. Numeric fixture calculations used Python
`struct` float rounding and Java-compatible signed integer/hash/48-bit RNG arithmetic,
with original snippets read where recorded in TEST_FINDINGS. These are not fabricated
claims of JVM or game observations. Leader must run real gates sequentially.

No claim that every helper is independent, every test cleans up on timeout, every raw float
comparison is sound, or every generated-world scan is bounded enough for the 3 GB heap.
Open findings and unjudged coverage remain explicit rather than being hidden by the green count.
