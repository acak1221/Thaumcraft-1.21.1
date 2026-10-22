# Taint spread audit — g22taint (2026-10-23)

## Scope and evidence standard

Hypothesis: native taint spreads at the wrong rate or into the wrong target set.
Specification: private **4.2.3.5** oracle `BlockTaintFibres`, `BlockTaint`, `Config`,
`TileEtherealBloom`, and `BlockFluidPure`. There is no `TaintHelper` in this oracle.
The retained 1.12 `TaintHelper` is not a gameplay specification.

PROVEN means an actual native Fabric GameTest ran in this worktree, not just a
source assertion or an inherited test. Statistical measurements below are **block
random-tick invocations**, not elapsed server ticks. Chunk selection/randomTickSpeed
and long-term ecology are outside the isolated conditional-rate measurement.

## Rules inspected

Twenty-two independently counted rules (including individual material targets):

**Final tally: 22 inspected rule groups, 19 runtime-measured/branch-checked
(PROVEN within the stated fixture), 3 reasoned-only; 5 proven target mismatches,
all fixed.** One group (#3) covers six legacy targets; the complete target
matrix contains 16 blocks. Only #1 is a large-sample statistical experiment;
forced branch checks are not presented as stochastic measurements.

| # | Rule | Oracle / expected | Native evidence / status |
|---|---|---|---|
| 1 | Fibre target-coordinate rate | One choice from 3×5×3 every eligible tick; no extra spread roll | PROVEN: 1013/45000 vs1000 expected (+0.416σ) |
| 2 | Same-tick neighbour cascade | No recursive neighbour spreading | PROVEN in fixture: max1 conversion/invocation |
| 3 | Six established soil targets | Sand, gravel, dirt, farmland, grass, clay convert at ≥3 neighbours | PROVEN baseline6/6 and fixed6/6 |
| 4 | Red sand | Old sand metadata 1, sand material | PROVEN mismatch0/1 → fixed1/1 |
| 5 | Coarse dirt | Old dirt metadata 1, ground material, even though not a creative subtype | PROVEN mismatch0/1 → fixed1/1 |
| 6 | Podzol | Old dirt metadata 2, ground material | PROVEN mismatch0/1 → fixed1/1 |
| 7 | Mycelium | Grass material | PROVEN mismatch0/1 → fixed1/1 |
| 8 | Soul sand | Sand material | PROVEN mismatch0/1 → fixed1/1 |
| 9 | Bedrock | No eligible material/replaceability | PROVEN retained1/1 |
| 10 | Obsidian | No eligible material/replaceability | PROVEN retained1/1 |
| 11 | Stone | No eligible material/replaceability | PROVEN retained1/1 |
| 12 | Chest | No eligible material/replaceability | PROVEN retained1/1 |
| 13 | Rooted dirt | No 1.7 counterpart; native deliberately does not extend targets | PROVEN retained1/1; modern mapping decision, not oracle parity proof |
| 14 | Fibre subtype lottery and RNG | World RNG: 90% type0, 9% type1, 1/120 type2, 1/600 type3 with open top/solid bottom | PROVEN exact branch outcomes and world-RNG draw bounds; not a sampled subtype distribution |
| 15 | Biome chance/threshold | Passed RNG nextInt(rate×5)==0, ≥2 neighbours; default 1/1000 | PROVEN bounds3/3/1000; one neighbour rejects, two convert; existing overflow-safe deviation also passes |
| 16 | Biome rate zero | No biome draws/conversion; existing-taint physical conversion still proceeds | PROVEN physical spread at rate0:1013/45000; exact tick draw schedule3/5/3 |
| 17 | Material neighbour thresholds | Crust ≥2; soil ≥3 | PROVEN log at2 converts; dirt at2 remains; dirt at3 converts |
| 18 | Stalk spore | Failed fibre placement, subtype3, enabled, nextInt10==0, air above; EntityTaintSpore | PROVEN exact nextInt10 draw, actual registered size2 spore, stalk3→4→3 |
| 19 | Crust swarmer | nextInt200==0, enabled, air above, no swarmer within16; EntityTaintSporeSwarmer | Reasoned-only: source agrees, no new stochastic mob measurement |
| 20 | Outside-biome cleanup | Fibres vanish; soil 1/10→dirt; crust 1/20→goo8 | PROVEN exact branch bounds10/20 and outputs; fibres cleared through ordinary scheduled ticks |
| 21 | Global generation gate | Config.genTaint is generation, not a spread guard; no allowTaint in oracle | Reasoned-only; do not invent a spread gate incompatible with original |
| 22 | Purification / bloom | Pure fluid provides warp ward, not taint cleansing. Bloom changes biomes, then ecology cleans blocks over time | Reasoned-only; bloom discrepancies listed below |

## Measurement and reproduction

`taintSpreadMeasuresOracleDistributionAndTargets` creates a real tainted-biome
fixture, fills the 45 possible coordinates with inert stone except one eligible
dirt target, supplies three taint neighbours, resets the dirt each trial, invokes
the registered fibre block's actual `BlockState.randomTick`, and counts converted
cells. Seed **22023**, **N=45000**, **p=1/45**, expected **1000**, binomial
standard deviation **31.27**; acceptance ±160 (about 5.1σ). The target matrix then
uses exact coordinate draws `[2,3,1]` to test each block through that same runtime
path. It does not substitute a pure helper for world mutation.

Baseline (`.tmp/g22-baseline.log`): **1013/45000** conversions (**2.2511%**)
versus oracle **1000 expected (2.2222%)**, **+0.416σ**; maximum **1 conversion per
invocation**. No rate mismatch detected. Target matrix: **6/11** eligible targets
converted, **5/5** exclusions retained. The five omitted targets each converted
**0/1**, expected **1/1**, under forced eligible coordinates and three taint
neighbours. The GameTest intentionally failed on `accepted=6/11`.

Fix: extend only `TaintMaterialRules.soil` to the five omitted old material
counterparts; preserve RNG, spread cadence, thresholds, modern rooted-dirt
exclusion, and all stock/loaded-chunk checks. Correct the misleading material
documentation and unit assertions. Fixed rerun (`.tmp/g22-fixed.log`):
**1013/45000**, maximum **1**, eligible targets **11/11**, exclusions **5/5**.
**Both native Fabric GameTests passed**, including the existing broader
ecology/biome/spore/cleanup/falling suite. Focused material unit suite:
**4 tests, 0 failures/errors**. Gradle **BUILD SUCCESSFUL**. XML evidence:
`fabric/build/gametest-focused-results.xml` and
`common/build/test-results/test/TEST-thaumcraft.taint.TaintMaterialRulesTest.xml`.

This is a headless dedicated GameTest-server check, not client, NeoForge,
multiplayer, or complete survival acceptance.

Gradle runs use Java21 and the worktree-private `.gradle-g22` home (seeded with a
copy of the existing user cache); each server run deletes only
`fabric/run-gametest/world` first. No saved world was reused.

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME="$PWD/.gradle-g22"
rm -rf fabric/run-gametest/world
./gradlew :common:test --tests thaumcraft.taint.TaintMaterialRulesTest \
  :fabric:runGameTest \
  -PgameTestFilter=taintSpreadMeasuresOracleDistributionAndTargets,taintBlocksPreserveOriginalSpreadLootAndFallingCustody \
  -PgameTestHeap=768M --no-daemon
```

The baseline ran just the first GameTest against unchanged production sources.
It compiled before the material fix was edited; its recorded five failures are
from the old runtime classes, not inferred by reversing the fixed code.

## Reasoned-only / limits

- No original 1.7 server was executed. Expected probabilities come from oracle
  control flow; removed vanilla materials are resolved from 1.7 source signatures.
- Oracle material expression is not a universal protection API. Bedrock/obsidian
  are naturally excluded. No addon-protection contract or allowTaint switch exists
  in the supplied oracle. Arbitrary modded protected-block integration remains untested.
- Solid soil's spread-to-air probability and crust's 1/200 swarmer distribution
  were inspected, not measured statistically.
- Bloom source discrepancy: native uniform offsets and unconditional plains
  replacement are not the oracle's triangular `nextInt(8)-nextInt(8)` offsets and
  generator-biome restoration. The native also refuses eerie/magical-forest
  restoration after admitting those biomes to its preliminary predicate.
  Initial randomized counter phase also differs.
  Not runtime-confirmed or changed in this bounded target-set fix.
- Modern biome storage changes whole 4×4 quart columns, not independent legacy
  1×1 columns. No proof of exact long-term spread/cleaning front equivalence.
- Pure fluid does not remove a tainted area in the oracle; neither pure fluid nor
  bloom promises instantaneous block restoration. A temporarily partially cleaned
  area is expected while original random-tick cleanup runs.
- Distribution test measures ordinary neighbour-update cascades in this fixture,
  not malicious addon callback reentry or every possible spread topology.
