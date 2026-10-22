# Aura-node time audit — g20nodes

Scope: Fabric/native `common/src`, TC4 4.2.3.5 private oracle (read only).
All runtime fixtures call production tickers on actual installed block entities
in a GameTest server. Tick counts below are synchronous production updates,
not elapsed client frames or multiplayer acceptance. Controlled branch inputs
replace **the world's** RNG temporarily and restore its exact original identity.

## Oracle corrections to the hypothesis

* Recharge has no success roll: one unit at interval600,400,900 for unmodified,
  BRIGHT,PALE; FADING interval0. The random draw selects a depleted current key.
* Stocked normal nodes do **not** passively decay. Every1200 updates, each
  zero-current key loses one base; removal rolls `nextInt(20)==0` or base<=0.
* PALE is a **modifier**, not a type; DIM does not exist. Removed depleted keys
  can degrade the modifier at1/5, then PALE can become FADING at another1/5.
* Ordinary modifiers do not multiply stored capacity. Energized throughput
  applies float1.2/.8/.5, truncates, then floors float sqrt of reduced primal aura.
* Energized output is renewable raw centivis per source update, not essentia,
  not a finite aura-to-essentia exchange, and not a100x larger debit.

## Runtime evidence

Pending execution; no passing-runtime claim yet.

The inventory counts behaviour groups (not assertions, RNG draws, or individual
parameter combinations). Planned inventory:19 groups,17 runtime-measurable and
2 reasoned-only. Exact observations and defect status will replace this marker
after execution.

| # | Behaviour | Oracle formula / threshold | Runtime entry point |
|---|---|---|---|
| 1 | Normal recharge | +1 each600 ticks; world choice of depleted aspect | curves |
| 2 | BRIGHT recharge | +1 each400; ordinary base unchanged | curves |
| 3 | PALE recharge | +1 each900; ordinary base unchanged | curves |
| 4 | FADING recharge | interval0, disabled | curves |
| 5 | Basic lock recharge | interval x2 | curves; stabilizer regeneration |
| 6 | Advanced lock recharge | interval x20 | curves |
| 7 | Stocked normal decay | none | curves |
| 8 | Depleted-key decay | base -1 each1200; prune1/20 or base<=0 | curves |
| 9 | Zero-base depleted-key decay | setter refuses base0 -> -1 overdraw | zero-capacity regression |
| 10 | Depletion degradation | removal then1/5 degradation then1/5 PALE->FADING | curves |
| 11 | Discharge/growth | consume1; growth1/(1+base/shinyFactor); victim base loss1/3; PALE cure1/100 | growth; stabilizer lock |
| 12 | UNSTABLE type repair | every100, world coin, then42 at bound10000/lock | curves |
| 13 | Stabilizer lock/power and FADING repair | lock0/1/2, poll1/every50; repair69 at12500/lock; retain cached0 | curves; stabilizer lifecycle |
| 14 | Converter lifecycle |1000 warm drains then conversion update1001; revert at cooldown<=50 | transduction lifecycle |
| 15 | Energized capacity / epoch conservation | max-overlap primal reduction, modified sqrt, renewable raw units each tick | visnet source budgets |
| 16 | Energized UNSTABLE rates | reset on nextInt500==1; +/-2 with nextInt5 | visnet source budgets |
| 17 | Offline catch-up | elapsed/(interval x75ms), bounded attempts | stabilizer regeneration |
| 18 | Tainted-biome type change | every100,1/500, exclude PURE/TAINTED | reasoned only |
| 19 | Config controls | generation/hard-negative controls; no node recharge-rate setting | reasoned only |

## Reasoned-only inspection

* Taint type change: oracle `TileNode718-723`, native `NodeEcology.taint` use
  a100-tick gate and world `nextInt(500)==0` in the tainted biome; PURE/TAINTED
  types are excluded. Not exercised by this audit's non-tainted fixtures.
* Configuration: oracle `Config` exposes hard-mode negative ecology and generation
  controls, not recharge/decay interval settings. `nodeRefresh` is not the
  ordinary node's600-tick recharge interval. No config-variation runtime claim.

## Test entry points

New `NodeTimeAuditRuntimeChecks`: `nodeTimeAuditCurves`,
`nodeTimeAuditGrowth`, `nodeTimeAuditZeroCapacityDecay`.
Existing production-runtime suites selected for complementary checks:
`nodeStabilizerLockPhaseDischargeAndWand`,
`nodeStabilizerRegenerationRepairAndPersistence`,
`nodeTransductionConversionPersistenceAndCooldown`, `visNetSourceBudgets`.
