# g10world — generated structure boundary hypothesis

Branch/worktree: `agent/g10world`, `/home/cyberfox-kali/wt/g10world` only. Baseline `7008358c`; initial status clean. No other worktree or main checkout touched. No subagents or public pushes.

## Authority

Read byte-exact oracle `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/lib/world/WorldGenMound.java:29-78` and `ThaumcraftWorldGenerator.java:420-453`. Oracle accepts five ground samples and writes its 19×19 mound at `surface-9`; the node is at base+8. Native FEATURES writes only one neighbouring chunk on each side. Local origin14/15 plus18 reaches chunk+2.

## Reproduction

New `MoundBoundaryRuntimeChecks` generates the chunks touched by each deliberately selected mound, supplies a declared stone-ground fixture at Y64, and runs the real feature through native `WorldGenRegion`/FEATURES. Only region cache lookup is replaced with already-generated chunk lookup, never a load. This is synchronous controlled placement, not natural mound discovery or an asynchronous population stress test.

Baseline red: valid local13 control spans four chunks, inspects 2,440 template cells plus chest loot, two spawners and node. Invalid local15 placement reports success after 2,311 writes, so the atomic-refusal assertion fails. Initial proxy attempt failed the harness (`BiomeColumns` requires a native region), was replaced, and is not defect evidence.

## Fix and compatibility choice

One fix: atomic whole-footprint preflight in `WorldGenMound.samplesPass`, shared by actual generation and aura suppression. Check each footprint chunk, writable corners and entire Y extent including optional buried TNT, before sampling/placing. Skip unsafe candidates instead of moving the oracle's position or force-generating remote chunks. Existing per-block guards remain defense in depth. This intentionally reduces eligible placements in the 60/256 local-origin combinations that cannot fit native FEATURES writes; not a claim that 23.4375% of naturally successful mounds were broken.

## Verification

Private Gradle home `.gradle-g10`, Java21. Before **each** Gradle run removed only this worktree's `fabric/run-gametest/world` as requested.

* Initial `:common:test --tests thaumcraft.world.WorldGenWriteAreaTest`: passed before new regression.
* Baseline `:fabric:runGameTest -PgameTestFilter=moundBoundaryIsCompleteOrRefused`: native-region test fails at expected partial-success assertion; ignored log `gradle-g10-red-native.log`.
* Post-fix command: `./gradlew :common:test --tests thaumcraft.world.WorldGenWriteAreaTest :fabric:runGameTest -PgameTestFilter=moundBoundaryIsCompleteOrRefused --no-daemon`; **5 JUnit tests + 1 dedicated-server GameTest passed**, build successful, log `gradle-g10-green.log`.
* Final rerun of that command adds explicit assertions for both urn/crate blocks and immediate feature replay without any additional writes/chest refill. **Passed**, build successful in39s, log `gradle-g10-final.log`. Machine timestamps are October22 UTC; assignment/audit filename date is the supplied October23.

All acceptance in this lane is Fabric; no NeoForge task was requested. Cold Gradle root configuration initializes the retained NeoForge project, but no NeoForge compilation/testing was run. No full suite, client visuals, ocean/desert/mesa matrix or forced-retrogen/save-reload acceptance claimed.

Full catalogue, counts, and reasoned-only follow-ups: `docs/audit/WORLDGEN_FINDINGS.md`.
