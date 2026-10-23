# G21 order/liveness investigation — 2026-10-23

Branch `agent/g21b`, baseline `231b2221`. Fabric 1.21.1, Java 21,
private `.gradle-g21b`; fresh `fabric/run-gametest/world` for every launch.

## Measurement contradicts the proposed precondition failure

**No `canWork` clause was false. No earlier polluting scenario is established.**
The deadline also reproduces with `g21HarvestAndLumberDelivery` alone.
Harvest/Lumber intentionally have empty cargo; that is not evidence that they
never started.

| Unfixed run | Measured result |
|---|---|
| `.tmp-g21b-diagnostic.log` | 407/407 pass; 626 cutter samples, every work gate and navigation area true; delivery tick 312 |
| `.tmp-g21b-diagnostic2.log` | Full suite: 406/407 pass; 3,000 cutter samples, zero `canWork=false` or `area=false`; melon/trunk air; loose drops present; collector carrying two logs |
| `.tmp-g21b-isolated-diagnostic.log` | **Isolated failure**; 5,700 worker samples, zero work/navigation refusals; three slices delivered; two logs carried and one log still loose |
| `.tmp-g21b-fractional-red.log` | New four-cell-edge Gather-only regression fails, without cutters or earlier scenarios |

The actual false clause is in **`GatherGolemGoals.Pickup.canContinueToUse`**:

```java
!golem.getNavigation().isDone() || golem.distanceToSqr(pursued) <= 2
```

At isolated tick 1455, Pickup's path is done (`path=2/2`), but the golem is
`(-8566166.583534105, -47, -7179083.715847766)` and the eligible log is
`(-8566166.835927371, -47, -7179082.001237956)`: distance squared is about
3.004, outside the original pickup reach of 2. Tick 1456 starts ReturnHome;
tick 1460 starts Pickup again. This repeats through the deadline. This is
**active oscillation**, not workers unable to start.

## Cause and fix

The 1.21.1 native `PathNavigation.moveTo(Entity, double)` calls
`createPath(entity, 1)` (verified against the actual mapped runtime bytecode).
Accuracy one permits an adjacent endpoint, and normal waypoint tolerance can
finish that path outside the original TC4 pickup reach when drops land near a
cell edge. The failing scenario creates precisely those fractional native drops.
Its own Pickup/ReturnHome interaction is sufficient; **there is no measured
foreign polluter to name**.

Request `createPath(pursued, 0)` and pass that path to ordinary native `moveTo`.
This approaches the actual drop cell, rather than accepting an adjacent cell.
No teleport, supplied harvest/drop outcome, reach increase, deadline increase,
goal-priority change, or refusal bypass. The original distance-squared-two
pickup limit, five-tick scan, 200-tick pursuit, eligibility and conservation
checks remain. Original contract: `2026-10-18-straw-golem-runtime-notes.md`,
AIItemPickup lines 34–99.

## Why the previous guard missed it

The ownership/configuration/recipe/level-lifetime guard checks **integrity**, not
the geometric adequacy or liveness of native AI paths. All of its old invariants
can hold while Pickup completes an unusable path forever. Six injected resource
leaks do not test this separate failure class.

The guard now watches registered G21 workers on each scenario tick, retains the
full work-gate/selected-goal/path witness, and rejects three completed approaches
to the same still-eligible drop outside actual pickup reach. Cargo changes reset
the progress counter. Refusal invokes the owning fixture's cleanup before the
existing isolation verdict, so it introduces no ticket/player leak. Failure
publication logs worker witnesses even after fixture cleanup. Deadline messages
are bounded; detailed state goes to the server log instead of exceeding the
native 1,024-character failure-message limit.

**Negative control before the navigation fix:** `.tmp-g21b-guard-red.log`
fails at worker tick **37**, naming
`thaumcraftgametests.g21gatherfractionaldroppositions`, Pickup, completed path,
`actual=3.8253791156796213`, `work=true`, `area=true`, and the eligible target.
No isolation-leak error is emitted. This proves the new check sees what the old
six-leak control did not. The four-position runtime regression and a Common
JUnit wiring guard remain. This closes the measured loop, not every conceivable
future AI or cross-scenario defect.

## Final verification

Final-source full run `.tmp-g21b-final-full.log`:

```text
All 408 required tests passed :)
BUILD SUCCESSFUL in 3m 7s
```

This is the original 407 plus the new fractional-drop regression, with noisy
per-tick trace logging removed and the per-tick guard still enabled. The same
invocation runs `:common:test --tests thaumcraft.integration.GameTestIsolationTest`:
**7 tests, zero failures/errors/skips**, including the new worker-progress wiring
check. Earlier pre-fix green runs above are diagnostic evidence, not acceptance.

Final fresh single-scenario run `.tmp-g21b-final-isolated.log`,
`-PgameTestFilter=g21HarvestAndLumberDelivery`: **1/1 pass**, `BUILD SUCCESSFUL
in 19s`. No client, multiplayer or NeoForge acceptance is claimed.
