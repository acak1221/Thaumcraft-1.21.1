# f3tests — adversarial-test repair follow-up

## Scope and custody

Worked only in `/home/cyberfox-kali/wt/f3tests` (`agent/f3tests`), starting clean at
`0464d084`. Read `docs/audit/TEST_FINDINGS.md` first and used its 27 open method
findings as the checklist. The separate infrastructure findings remain out of scope.
Read original behaviour in the read-only private 4.2.3.5 oracle. No production
behaviour is changed in the final diff; temporary mutations are restored before handoff.

## Groups

1. Independent frozen oracle fixtures: context/use wand matrices and angles, jar
   animation, thaumometer light, trunk scale/lid, golem speed/range/darts, reservoir
   liquid/colour, mirror projection, complete sceptre capacities.
2. Actual observable contracts: built common JAR and resolved runtime dependency
   scan; exact custom-sound sample bindings plus Ogg/Vorbis headers and closed
   streams; exact 244-key research recipe gate fixture, including blueprint aliases.
3. Focus cost arithmetic now uses literal IEEE bits and actual casting price API;
   registered native wand price/debit companion added to the existing accessory
   GameTest. Bone bow scenario observes the fired entity, damage, speed, ammo,
   durability, noncritical flag and rejection of an uncharged draw, with shot cleanup.

Full per-method old/new assertions, original citations and mutation targets live in
`docs/audit/TEST_FIXES.md`. No method was deleted to reduce the finding count.

## Validation history

All Gradle invocations use Java 21 and this worktree's own `.gradle-f3` user home,
one invocation at a time. Initial cold-cache configuration also prepared the existing
NeoForge project; no NeoForge target or runtime was executed.

* Initial `:common:test` reached actual compilation; fixed missing `assertFalse` import.
* Next suite executed 2,470 tests and caught an erroneous third-array-element fixture:
  native `trunkScale` returns side/height (renderer repeats the side for Z), not XYZ.
  Removed the invalid array access, not a test or a behaviour assertion.
* `:common:test :common:compileGameTestJava` then passed: **2,470 tests, zero
  failures, zero skipped**, and shared native GameTest sources compiled. Compiler
  deprecation/mixin mapping warnings remain existing warnings, not hidden failures.
* Mutation/final-run results recorded in `TEST_FIXES.md` after completion.
  The JUnit composite mutation run executed 85 tests, 39 failures; all 26 repaired
  JUnit methods were independently listed as red testcases, not inferred from task failure.
* First clean Fabric focused run passed both the bone bow and accessory scenarios.
  A native mutation run then caught the bow no-op, but an accessory cleanup exception
  masked the new debit assertion because the check ran before fixture connection setup.
  Moved the check **after** `f.open()`; that masking failure is not counted as proof
  of debit-mutation detection. The repeat run and final clean run are recorded in the ledger.
  The repeat native run explicitly failed on zero fired arrows and unchanged vis after
  a supposedly successful debit. Both temporary production edits were then restored.
* An intermediate post-mutation JUnit rerun caught the leftover **empty** `codechicken/`
  source/output directories in the rebuilt JAR after the marker file itself was deleted.
  Removed only those two empty mutation-created directories and forced the archive rebuild.
  The archive is also an explicit Gradle test input, not just a task dependency, so
  archive-only changes invalidate test up-to-date/cache decisions. The test was not weakened.
* Final clean combined `:common:jar --rerun :common:test --rerun :fabric:runGameTest`
  passed, including **both** focused native scenarios after mutation restoration
  (`gradle-f3-final2.log`). A further explicit `:common:test --rerun` after the final
  Gradle input wiring passed in 14s (`gradle-f3-final-junit.log`): **2,470 tests,
  zero failures, zero skipped**. Final result: **27/27 repaired, 27 mutation-verified,
  zero deliberately left method findings**. No production code/resource diff remains.

## Limits

Frozen floats are offline evaluations of original expressions, not production-derived
runtime expected values. Resource-header checks do not establish audible playback.
Pure render-number checks are not an inspected client scene. Dependency scans check
the common production artifact/runtime dependency graph, not a release claim for both
loaders. Native GameTest compilation is not runtime acceptance; actual runs, if any,
are listed separately in the final evidence ledger.
