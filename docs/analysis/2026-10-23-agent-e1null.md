# e1null — null/error API compatibility review

Date: 2026-10-23. Worktree: `/home/cyberfox-kali/wt/e1null`; branch `agent/e1null`.
Starting commit: `974fde6a`; initial `git status --short` was clean.

## Scope and decisions

Read `docs/audit/API_QUALITY.md`, section 3, and reviewed all N01–N20 against the current implementations. Full classification, source locations, test mapping and the two-option compatibility decision list are in `docs/audit/NULL_POLICY_FIX.md`.

* Category a: **4** groups, N02/N03/N05/N13. Named IAE boundary checks replace unusable deep failures; null NBT is checked before destructive clearing; empty constructor tags are refused before global registration.
* Category b: **10** groups, N04/N06/N07/N08/N09/N11/N15/N16/N18/N20. Javadoc only, preserving explicit rejection, no-op, sentinel, signed arithmetic and callback boundaries.
* Category c: **6** groups, N01/N10/N12/N14/N17/N19. Existing contracts documented; maintainer decides setter strictness, parser consistency, absence sentinel, null knowledge type, wand tag identity and VisNet thread boundaries.

No assets, porting scripts, other worktrees, original/donor material, or primary checkout were modified. No gameplay acceptance, client acceptance, multiplayer acceptance or NeoForge parity is claimed. No subagents spawned.

## Verification

Java: `/usr/lib/jvm/java-21-openjdk-amd64`; isolated Gradle home: `.gradle-e1` in this worktree. Test implementation: `common/src/test/java/thaumcraft/api/NullPolicyTest.java` (six tests covering the four category-a groups).

The fresh isolated Gradle home first downloaded Gradle 8.12.1 and resolved/remapped native dependencies; it does not reuse another agent's Gradle home. The initial foreground invocation timed out during first-time setup. A background retry and foreground confirmation subsequently completed; both executed the unfixed implementation and reported **6 tests completed, 6 failed**. All six failures were the expected assertions, not compilation/bootstrap failures: five unexpected deep NPEs and one accepted empty tag.

After boundary fixes, the same focused command passed all six tests (`BUILD SUCCESSFUL in 17s`). The full `./gradlew :common:test --console=plain` then passed: **2,531 tests across 401 suites; 0 failures, 0 errors, 0 skipped** (JUnit XML totals). Existing native compilation warnings concerned the two mixin mapping targets; there were no new compile errors. `git diff --check` passed.

Ignored local execution logs: `.null-policy-red.log`, `.null-policy-red-confirm.log`, `.null-policy-green.log`, `.null-policy-full.log`. `.gitignore` now excludes this lane's `.gradle-e1/` cache. The six category-a tests are the only tests added; exact full-suite counts are reported instead of assuming the task's approximate 2,522 baseline.

Gradle's existing settings include/configure both loader modules during common tests; no NeoForge/Fabric build, runtime, or gameplay acceptance task was invoked.
