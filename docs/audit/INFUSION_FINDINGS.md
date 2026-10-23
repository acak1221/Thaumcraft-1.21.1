# Infusion ritual audit — 2026-10-23

Scope: Fabric 1.21.1; real registered Silverwood infusion, finite fixture inputs.
Original specification: private TC4 4.2.3.5 oracle, `TileInfusionMatrix.java`
252–509 and `BlockStoneDevice.java` 291–318. No donor sources changed.

**Outcome: 30 driven rules, 30 PROVEN in the bounded tests, one PROVEN mismatch
fixed. No ingredient/essentia/output duplication or unintended accounting loss
was reproduced. Eight required GameTests and the Fabric build passed.**

## Original rules, not assumptions

The audit distinguishes resource duplication from original destructive hazards.
Successful ingredients do **not** increase instability in the original. Missing
essentia rolls `1/(100-3r)` per failed aspect, missing ingredients may add debt and
roll `1/(50-2r)`, and valid-center hazards roll `nextInt(500) <= I` when `I > 0`.
There is no portal outcome in the original 21-entry switch.

The original matrix advertises comparator capability but returns **zero**, even
while crafting. Pedestal comparators report inventory fullness, not ritual
progress. Client payload state is distinct from visually inspected rendering.

## Initial measured evidence

The pre-fix four-case GameTest run had **one failure**:
`g24InfusionComparator`: `original matrix advertises analog output capability`.
The payment, interruption, and measured-instability cases passed unchanged
production ritual code. This is the failing regression before the comparator
fix, not a source-text check. A first invocation used an invalid abbreviated
filter and ran no cases; it is not counted as evidence.

* 1,000 independently admitted stocked rituals, one real cycle each, seeds
  0–999: **12 hazards / 1,000 cycles (1.2%)**; exact oracle prediction **12**.
  Natural initial instability is 6 (recipe 5 plus asymmetric occupancy 1), so
  the original mathematical probability is 7/500 = 1.4%, not 6/500.
* 1,000 independently admitted rituals with all seven aspects unavailable:
  **83 instability increments**, exact oracle prediction **83**. Each failed
  aspect rolls 1/85 unless the hazard consumes the cycle. This is a count of
  increments, not a claim that 83 distinct rituals increased instability.
* 100 independently admitted, fully essence-paid, stabilized rituals with all
  seven ingredients moved to physical fixture escrow: **259 added debt units
  and 9 instability increments**. Exact independent oracle predictions:
  **259 and 9**. Missing index `a` rolls 1/(1+a) for debt, then 1/40 for an
  instability increment at recipe instability 5.

The deterministic sample is a regression measurement, not a claim of a measured
population frequency equal to the mathematical probability. Every trial compares
the actual matrix state change with independently replayed original RNG rules.

## Driven rules (30 scoped contracts)

Legend: **P** = `g24InfusionProgressivePayment`, **I** =
`g24InfusionInterruptions`, **C** = `g24InfusionComparator`, **S** =
`g24InfusionMeasuredInstability`, **M** = `g24InfusionMissingIngredientRate`,
**N** = existing `infusionConsumesDistilledEssentiaAndPersistsSession`, **H** =
existing `infusionAltarFormsAndExecutesAllInstabilityOutcomes`, **B** = existing
`infusion_jar_brain_awards_warp_once`. These are scoped contract counts, not an
inflated count of repeated assertions. All 30 rows are PROVEN by the final run;
only row 13 was a production mismatch and needed a fix.

| # | Rule driven | Measurement / assertion | Test | Mismatch / fix |
|---|---|---|---|---|
| 1 | Admission snapshots without charging | 63 demand, 58 available; seven physical components unchanged at start | P | None |
| 2 | Progressive essence payment | One successful unit per ten-tick cycle; 58 units paid in 580 ticks | P | None |
| 3 | Shortfall pauses | Five debt, zero stock, seven components and center retained after 100 more ticks | P | None |
| 4 | Refill pays remaining debt only | Exactly five additional units; no ingredient paid early | P | None |
| 5 | Ingredient cadence | Seven components each take six successful cycles: 42 total | P, N | None |
| 6 | Successful ingredients do not raise instability | Zero increments over all 42 cycles | P | None; hypothesis premise contradicted by original |
| 7 | Exact single output including components | Native `ItemStack.matches` against recipe assembly; count one | P | None |
| 8 | Exact total cost / no repeat completion | 63 essentia and seven components; 100 idle ticks do not repeat output/debit | P | None |
| 9 | Consumed non-container pedestals empty | Seven occupied → zero | P, N | None |
| 10 | Beam render payload follows live craft | Active/crafting, physical target, countdown five | P | None |
| 11 | Finish render payload clears beam | Crafting false, target absent | P | None |
| 12 | Pedestal comparator follows contents | Central reward gives nonzero; all consumed peripherals give zero | P | None |
| 13 | Matrix advertises original comparator capability | Pre-fix false; expected true | C | **PROVEN mismatch; restore flag** |
| 14 | Matrix is not a comparator progress meter | Zero before and during crafting | C | No strength change |
| 15 | Break a peripheral mid-beam | Exactly one matching physical ingredient drop, after deferred-drop delivery | I | None |
| 16 | Missing first ingredient does not finish | Seven pending ingredients; exactly one new debt; center unchanged | I | None |
| 17 | Broken structure suspends session | Inactive but crafting; debt/remaining ingredients survive | I, N | None |
| 18 | Suspended surviving pedestals intact | Every survivor compared with its pre-suspension stack after 100 ticks | I | None |
| 19 | Repair/reactivation is not a new recipe | Existing debt retained | I, N | None |
| 20 | Breaking matrix retires session | Old BE removed; 100 stale ticks cannot drain or emit prepared output | I | None; original removal blast is deliberate |
| 21 | Hazard gate uses inclusive original probability | 12/1,000 actual skipped payments; exact seeded oracle 12 | S | None |
| 22 | Essence shortage raises instability at original rate | 83 increments/1,000 independent seven-aspect shortage cycles; oracle 83 | S | None |
| 23 | Missing-index debt and instability probabilities | 100 independently admitted, fully essence-paid rituals: 259 debt and 9 increments; exact oracle 259/9 | M | None |
| 24 | Genuine stabilizers allow original negative initial instability | Physical symmetric skulls; no instability/session field injection | P, N, M | None |
| 25 | Mid-payment save/reload preserves frozen session | 13 paid, demand50; no rematch/debit; all63 paid exactly once | N | None |
| 26 | Mid-beam reload resets only transient progress | Seven pending, zero-valued aspect keys retained, countdown resets | N | None |
| 27 | All 21 hazard outcomes have real effects | Each original event ID driven; ejection/destruction, goo/gas, zap/harm, explosion and warp assertions | H | None |
| 28 | Crafting remainder is not a lost/duplicated ingredient | Jar-brain craft leaves exactly one bucket; other inputs paid | B | None; not every recipe empties every pedestal |
| 29 | Broken/stale essence source does not duplicate payment | Source break/cache backoff preserves demand and surviving stock | N | None |
| 30 | Invalid central input aborts after original hazard | Actual damaging terrain-safe explosion, no reward, unpaid ingredients retained | N | None |

## Limits / reasoned-only

* Client pixels, animation timing, sound, packet delivery to a real client, and
  multiplayer visuals are **not** established by checking server update payloads.
* The probability sample is one cycle from each independent ritual, not 1,000
  unstable rituals completed end-to-end; no long-run population-frequency verdict.
* No forced server/process crash, concurrent-addon callback fault, or restart
  during a destructive explosion was driven. Ordinary session reload is covered
  separately by N; crash durability is not inferred from it.
* No blanket promise that inputs survive matrix destruction: the original
  terrain-damaging blast and destructive hazards intentionally allow losses.
  A missing peripheral is also not an original automatic abort: source inspection
  shows the cycle can try later ingredients after adding debt. Only the bounded
  missing-pedestal and structure-suspension observations in the table are claimed.
* Enchantment XP, high-charge runic overflow, all other result-patch recipes,
  maximum-instability cap behavior, and NeoForge are outside this bounded run.
* “Portal” is not an original switch outcome (source inspection only); none was
  invented to satisfy the hypothesis wording.

## Harness boundaries

Stock/research/formed altar blocks are declared fixtures. Real recipe admission,
matrix ticks, source transfers, pedestal setters, destruction, and output execute
production code. Ticks are driven synchronously, not accelerated wall time.
The refill test explicitly clears the level's wireless retry cache to avoid
waiting five real seconds; it does not edit demand or any paid-session field.
N separately checks that ordinary refresh does not bypass that real-time backoff.

## Reproduction / evidence

Final command (run from this worktree, with its private Gradle home):

```sh
rm -rf fabric/run-gametest/world
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/g24infusion/.gradle-g24
./gradlew :fabric:runGameTest :fabric:build \
  -PgameTestFilter=g24InfusionComparator,g24InfusionProgressivePayment,g24InfusionInterruptions,g24InfusionMeasuredInstability,g24InfusionMissingIngredientRate,infusionConsumesDistilledEssentiaAndPersistsSession,infusionAltarFormsAndExecutesAllInstabilityOutcomes,infusion_jar_brain_awards_warp_once \
  -PgameTestHeap=3G --no-daemon --max-workers=2
```

Final output: `All 8 required tests passed :)`, normal save of every dimension,
`BUILD SUCCESSFUL in 1m 15s`. The game-test XML contains eight cases and no
failure/error/skipped elements. The pre-fix XML contains four cases and one
comparator failure. Existing matching rules were not deliberately broken merely
to manufacture red tests; the sole claimed production defect has a genuine
pre-fix failing GameTest.

Local, ignored evidence SHA-256:

| Artifact | SHA-256 |
|---|---|
| `.tmp/g24-red2-results.xml` | `dd3eec11e44b9951284c4ab5840a5b55bb78efb24814ff1b290c529a872671bc` |
| `fabric/build/gametest-focused-results.xml` | `95a5d26f87310d46808b7b125fe23c0d5fd9c8ecc6e04fe5a9f792dd83939c64` |
| `.tmp/g24-green.log` | `76b6bd0a2d92652e41495f358ee88275f1d9871e4575b5baa791dd31114067f9` |

The pre-fix reduced-heap run completed its assertions/XML but was terminated
during prolonged shutdown; it is red runtime evidence, not a successful Gradle
run. See the analysis note for the thread-dump diagnosis. Final verification
used the documented 3G heap and exited normally.
