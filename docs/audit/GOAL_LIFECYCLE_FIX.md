# Goal rebuild lifecycle fix

Date: 2026-10-23. Baseline: `49324b8f`; branch `agent/c3fix`.

## Bytecode evidence

Inspected with Java 21 `javap -c -p` against the Mojang-named Minecraft 1.21.1 jar:

```text
net/minecraft/minecraft-merged/
1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2/
minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar
```

Jar SHA-256: `afeb2c8ec0220e85d22c06cc1b41087db54e5cf52fba6ec9ea744846ca1236d1`.
Reproduce with `javap -c -p -classpath <jar> net.minecraft.world.entity.ai.goal.GoalSelector
net.minecraft.world.entity.ai.goal.WrappedGoal`.

`GoalSelector.removeAllGoals(Predicate)` loads `availableGoals`, creates a predicate,
calls `Set.removeIf` (offset 10), then returns (16). It never stops a goal.
`removeGoal(Goal)` iterates wrappers, checks identity with `getGoal()`, then
`isRunning()` (38), invokes `WrappedGoal.stop()` (45), and only afterwards calls
`Set.removeIf` (61). These methods are **not** lifecycle equivalents.

`getAvailableGoals()` is public and returns `Set<WrappedGoal>`.
`WrappedGoal.isRunning()` and `stop()` are public. The latter checks its running
bit, returns if false, otherwise clears the bit (10) and delegates to the
underlying `Goal.stop()` (17). Stopping the wrapper, rather than just its underlying
goal, also retires selector scheduling state.

Local full disassembly: `.tmp/c3fix/goal-selector.javap.txt` (not committed).

## Regression

`goalLifecycleDecorationClosesGate` delegates to
`GolemEndToEndRuntimeChecks.decorationClosesRunningGate`.
It creates a real admitted Gather golem, a closed oak fence gate, and a connected
survival owner. After the golem can work, it supplies a collision and unfinished
native path, then ticks the **installed selector**, not a mock door goal. The test
requires a running registered `GolemDoorGoal` with an active window and an actually
open gate. The owner installs the real dart decoration through `interact`; the
test checks the installed `R` and exact one-item payment, then requires the gate
to be closed **immediately**, before another server tick can hide the leak.
Fixture listeners restore blocks, entities, players, channels and chunk tickets
on both success and failure. The scenario has its own batch.

The chest-open-count premise in G1 needs qualification: the current
`GolemChestInteraction.open/close` only plays sounds and never modifies vanilla
player open counts. An open-count-only test would pass vacuously. The real gate
state is the chosen resource witness; no chest implementation change is included.

## Fix

Explicitly stop running wrappers in both selectors before bulk removal and before
resetting controllers. Preserve all priorities, flags, registration order, controller
reset policy and paid cargo/cooldown semantics. Existing resource owners now get
their normal stop callback, including the otherwise unretained door goal.

## Medium findings

* **G2 — AvoidSwell: deferred, unchanged.** The original 4.2.3.5 jar was also inspected
  with `javap`: `AIAvoidCreeperSwell.func_75251_c` consists solely of assigning null
  to `closestLivingEntity` (offset 2) and returning (5). The legacy source agrees.
  Adding a navigation stop changes original interrupted-flight behavior, rather
  than merely clearing unused scratch fields. A safe change needs a separate live
  interruption/path-ownership test (threat death, work suspension, and successor
  MOVE goal), including preservation of a replacement path and physical escape
  impulse. This regression verifies a door window, not that flight policy.
* **G3 — PechAvoid: deferred, unchanged.** The original `EntityPech` bytecode constructs
  vanilla `EntityAIAvoidEntity` and removes/reinstalls it during combat-task changes.
  As a cross-check (not an original-game oracle), remapped 1.21.1 vanilla
  `AvoidEntityGoal.start()` starts navigation, while its `stop()` also only writes
  null to `toAvoid` (offset 2) and returns (5); retaining the path is not uniquely
  a mistake introduced by this port.
  That does not by itself establish that retaining navigation on tame/goal handoff
  is a defect. The modern goal retains its candidate path, but has no tracked
  navigation lease; an unconditional stop could cancel a replacement path. No
  live tame/combat handoff and successor-path test was run here, so behavioral
  safety is not claimed. Establish that ownership contract before changing it.

## Verification

All Gradle commands used Java 21 and the private `.gradle-c3fix` user home,
`--offline`, and removed `fabric/run-gametest/world` first.

1. Before the production fix, `:fabric:runGameTest
   -PgameTestFilter=goalLifecycleDecorationClosesGate` failed with **1 required test
   failed** and the exact assertion `decoration rebuild must stop the running door
   goal and close its gate immediately`. The XML includes that failure; all setup,
   active-window, installation and payment assertions preceding it passed.
   An earlier first-build attempt hit the 120-second harness timeout before test
   execution; the completed red retry took 28 seconds.
2. After the production fix, the same focused command passed **1/1**, with the XML
   containing the testcase and no failure element.
3. `:fabric:runGameTest :common:test`: **BUILD SUCCESSFUL**, 5m 43s.
   Full Fabric XML: **438 scenarios, 0 failures/errors** (437 existing plus this
   regression). Common JUnit XML: **2,517 tests across 397 suites, 0 failures/errors,
   0 skipped**. The dedicated server completed its world save and shut down normally.
4. After the final test-class Javadoc clarification, reran
   `:common:compileGameTestJava :fabric:compileGameTestJava :common:test` successfully.
   No executable source changed after the green full suite.

Red/green XML copies and execution logs are in `.tmp/c3fix/` (not committed).
No client/multiplayer/NeoForge acceptance is claimed.
