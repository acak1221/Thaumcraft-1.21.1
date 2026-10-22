# Agent g7save — save-cycle investigation

- Branch/worktree: `agent/g7save`, `/home/cyberfox-kali/wt/g7save` only.
- Base: `e8995568`; initial working tree clean.
- Hypothesis: actual persistence loses data that in-memory codec checks miss.
- Added three focused Fabric GameTests covering four persistence areas (block entities,
  player attachments, world SavedData, missing registry item custody).
- Added `docs/audit/SAVE_CYCLE_FINDINGS.md` with boundaries and exclusions.
- Private Gradle home: `.gradle-g7`; Java 21. World removed before Gradle execution.
- Final fresh-world dedicated-server result: **3/3 required GameTests passed**, Gradle successful
  in 20 seconds. Four persistence areas survived, zero confirmed production failures/fixes.
- Initial runtime failure was a fixture assertion including vanilla-added envelope metadata in
  quarantined NBT. Diagnostic rerun proved payload retention; assertion normalized only
  `id/x/y/z/keepPacked`. Final source rebuilt and all three checks passed.
- Actual chunk retirement, flush, disk reload; actual player-file save/new-player login;
  actual SavedData disk write/read using a separate cache. No JVM restart/registry rebuild.
  Matrix is an explicitly seeded paused selected-session fixture with native recipe predicates,
  not a survival-started ritual. Regeneration queue persistence remains unverified.
- Cold-cache networking and cross-project configuration caused setup failures. Final invocation
  used temporary root `settings-g7.gradle` (same plugin repositories; common/Fabric only) and
  `-Penabled_platforms=fabric`; temporary file removed after validation, no build-system changes
  committed. A requested global read-only dependency cache was rejected by Gradle and unused.
- Evidence: `/tmp/opencode/g7save-final.log`, `fabric/build/gametest-focused-results.xml`.
  XML environment timestamp is 2026-10-22T21:54:01Z; audit follows assigned 2026-10-23 date.
