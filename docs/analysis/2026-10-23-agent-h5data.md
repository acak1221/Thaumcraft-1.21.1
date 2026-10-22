# h5data handoff — 2026-10-23

Owned worktree: `/home/cyberfox-kali/wt/h5data`, branch `agent/h5data`.
Initial status was clean; baseline `e79b8558`. No other worktree or primary
checkout was read or modified. The original oracle was read only.

## Outcome

Detailed findings: `docs/audit/DATA_FINDINGS.md`.

* 69 source save/load pairs screened (59 BE, 6 SavedData, 3 player, 1 golem).
* Four asymmetric replacement/recovery paths fixed in four separate commits.
* 12 player lifecycle paths inspected across three data owners; no executed
  multiplayer/dimension/death/restart acceptance is claimed.
* 15 silent-loss loader sites identified; three repaired, 12 remain explicitly
  documented with malformed/missing-registry-input conditions and both citations.
* 26 component registrations indexed, 25 persistent; seven value families deeply
  inspected. This is not an assertion of complete codec coverage.
* Four requested retirement families inspected; five unproven/follow-up items
  are separated from established findings.

## Commits

| Commit | Fix |
| --- | --- |
| `fe91ae42` | Clear old tile inventory before applying complete snapshots. |
| `a21fae9a` | Keep wrong-element accessory inventory lists in existing quarantine. |
| `81d3a6cb` | Retain malformed growth-update collection owners whole. |
| `6f1d521f` | Retain malformed crucible paid-action collection owners whole. |

Tests added/extended: `TileInventorySaveIntegrityTest` (3 cases),
`AccessorySaveIntegrityTest` (2), `GrowthLampTicksTest` (1 additional case with
four malformed collection shapes), `CrucibleActionSaveIntegrityTest` (2 cases,
six malformed collection shapes). These invoke real native classes/NBT readers,
not source-text stubs. The private crucible reader is accessed reflectively in
test only; no public production restore/enqueue API was added.

## Validation record

* `git diff --check`: passed before the fix commits.
* Initial accidental default-JVM Gradle attempt failed during configuration:
  Java 25 / unsupported class-file major version 69. It did not compile or test.
* Correct isolated attempt:

  ```sh
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
  GRADLE_USER_HOME=/home/cyberfox-kali/wt/h5data/.gradle-h5 \
  ./gradlew :common:test --no-daemon -Penabled_platforms=fabric
  ```

  Final outcome to be recorded after the running build completes. The root
  settings include all three projects, so Loom also configures NeoForge despite
  the Fabric property; no NeoForge build/test task was requested.
* No headless GameTest/server smoke has yet been completed on these changes.
  Added regression tests are not a substitute for that acceptance.

## Remaining / unproven

Open source-established losses F05–F09 require a consistent retained-owner and
mutation/retirement policy, rather than broad catches that return empty data.
Do not claim they are fixed by the three collection validators.

Unproven: runic charge through real respawn/entity-ID changes; removed remembered
block IDs in memory/hole restoration; legitimate node-jar empty-base producer;
full persistent component absent/malformed/addon-removal coverage; real server
restart/chunk unload/canceled callback/multiplayer lifecycle acceptance.

`.gradle-h5/` is ignored so the agent-private dependency cache cannot enter a
commit. No public push or release was performed.
