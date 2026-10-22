# Agent g8order — native GameTest isolation

Worktree `/home/cyberfox-kali/wt/g8order`, branch `agent/g8order`, base `07451af6`.
Initial required status/log command succeeded with a clean worktree. No other
worktree or the primary checkout was read or modified. Fabric only; Java21;
private `.gradle-g8`; serial Gradle invocations; fresh disposable GameTest world
before each invocation.

## Implemented

* Replace the furnace fixture's accelerated-tick reload countdowns with an
  explicit ten-second monotonic wait servicing the existing native chunk
  executor. No extra ticket/force-load and no production gameplay changes.
* Add idempotent native verdict/timeout/rerun cleanup to that fixture.
* Add a shared GameTest verdict guard for per-scenario forced-chunk/manual-ticket/
  connected-player ownership, foreign releases, recipe corpus, config/rules/
  ecology/RNG identity and native level lifetime. Check after callback/finally
  unwinding and before vanilla publishes the result.
* Add paired fresh-world full/isolated verification, CI wiring, runner tests,
  and JUnit source regressions. `--all` inventories every wrapper in the actual
  native full report (not the lagging317-case manifest), not only
  the known regressions.
* Fix an additional measured Ember fixture defect: declare an ordinary native
  champion join draw and assert the target's initial full-health/cold state.
  Preserve canonical champion promotion and fire-immunity gameplay.

## Measured evidence

Three ordinary original-deadline suites passed356/356 (one constrained to one
CPU); the reported ordinary furnace failure was **not reproduced** on this base.
Do not invent a preceding polluter. Normal native reload observations already
showed asynchronous FULL/lighting readiness and only10.207ms for15 simulated ticks.

A temporary one-second delay of the **actual native disk future** reproduced
the exact reported furnace assertion in the full suite:200 ticks expired in
32.942ms, while the sole owned ticket33 remained present, the chunk was not
forced, and its FULL future was incomplete. With the fix, a delayed actual load
completed in1015.035ms at only+5 world ticks and the entire furnace custody case
passed. That diagnostic suite was still355/356 because Ember failed separately;
it is not misreported as acceptance.

The Ember negative control admitted a real native BOLD champion Blaze:
health45/max50/fire-1 before **and after** the ember. The exact-health fixture,
not the fire-immunity implementation, was wrong. Its ordinary join input is now
explicit through the existing finally-restored RNG facade.

The guard negative control introduced six leaks **after `succeed()`**. Native
XML correctly failed `regenerationQueueLifecycle` under its own name and listed
configuration, recipes, lifetime, connected player, forced chunk and region
ticket. All negative controls, delay gates and stdout probes were removed.

Two earlier native processes exited137 before reaching the target; these are
aborted runs, not assertion diagnoses or acceptance. Subsequent full acceptance
uses2600M GameTest heap on this contended host; no production heap default was
changed.

Details, exact monotonic timestamps, evidence paths, source-audited global
finally/close restoration inventory and limitations:
[`ORDER_DEPENDENCE_FINDINGS.md`](../audit/ORDER_DEPENDENCE_FINDINGS.md).

## Final verification

* Final native full suite: **356/356**, zero XML failures/errors,2m35s.
* Separate fresh native processes: Water radius **1/1**(32s), infusion session
  **1/1**(27s), furnace unload **1/1**(32s), additional Ember **1/1**(27s).
* `GameTestIsolationTest`: **6/6**, zero skipped/failures/errors.
* `:fabric:build`, including test/production packaging-isolation gates:
  **SUCCESS**; combined targeted-JUnit/build invocation1m09s.
* Python runner orchestration tests: **3/3**. Exhaustive `--all` inventories
  native XML instead of omitting39 newer cases absent from the old manifest;
  all356 cases were **not** individually isolated during this task.
* `git diff --check`: clean. Temporary fault-injection/probe source removed.

Native evidence: `.tmp/order-verification/summary.json` and its five log/XML
pairs. Build evidence: `.tmp/g8order/final-build-junit.log` and the native/JUnit
XML referenced in the audit. All retained native runs have the guard enabled.

This is dedicated-server fixture
acceptance, not physical-client, multiplayer, survival acquisition, NeoForge
parity, or a claim that every possible Java static can be snapshotted.
