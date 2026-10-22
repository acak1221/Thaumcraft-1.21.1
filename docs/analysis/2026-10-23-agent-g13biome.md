# g13biome — spawn hypothesis audit

Worktree `/home/cyberfox-kali/wt/g13biome`, branch `agent/g13biome`, starting commit `67099d1a`. First action was the requested status/log command: clean. No other worktree or primary repository was accessed.

## Oracle and scope

Read AGENTS.md and existing original-biome audit. Inspected immutable bundled TC4 JAR using Java `javap -p -c`: ConfigEntities, EventHandlerWorld, TileNode, EntityEldritchGuardian, EntityInhabitedZombie, EntityFireBat, EntityWisp, EntityPech. Original `ConfigEntities.initEntitySpawns` uses WorldChunkManager.allowedBiomes for Brainy Zombies, not every Overworld biome; fixed an initial test expectation that incorrectly demanded a Brainy row in Magical Forest. Production adapter was correct on that point.

Inventory and evidence separation: `docs/audit/SPAWN_FINDINGS.md` (36 mobs). Five no-discovered-survival-path types are reasoned-only, not automatically missing TC4 features: watcher, cultist_portal_greater, taint_crawler, taint_seed, taint_seed_prime. In particular original Watcher exists as a class but is not registered by ConfigEntities. No speculative node-wisp spawn added: original TileNode has no Wisp reference either.

## Verification setup

Java21, own `GRADLE_USER_HOME=$PWD/.gradle-g13`, heap1G, max-workers1. Initial empty-home dependency resolution was terminated (exit143); seeded only this owned home with a copy of the external `/home/cyberfox-kali/.gradle-leader/caches` and continued offline. No shared daemon/cache was used for writing. Loom configures the retained NeoForge project but no NeoForge task/build/test was run.

Always deleted `fabric/run-gametest/world` before each Gradle invocation. Logs in `/tmp/opencode/g13biome-*.log` are transient local evidence, not committed generated outputs.

1. `:fabric:runGameTest -PgameTestFilter=spawnAudit` compiled actual common/Fabric sources but rejected the filter: harness requires exact names, not prefixes. No runtime pass claimed.
2. Exact four-test run before guardian fix: `spawnAuditNativeBiomePools,spawnAuditGuardianIsolation,spawnAuditSpecialPaths,spawnAuditChampionNaturalLifecycle`. Guardian isolation failed at the assertion rejecting a neighbour within32x16x32. Pool test failed only at its incorrect Forest/Brainy expectation. Champion and special paths passed.
3. Guardian fix restores exact original expanded unit AABB and retains inherited rule. Native pool test now asks Plains for Brainy. Final results follow below.
4. Four-test after run: **all4 passed**, build successful (`g13biome-guardian-after.log`). Guardian fix committed as `c624314a`.
5. Additional oracle audit found that earlier port comments misinterpreted `World.func_72957_l`. Verified directly with `javap -p -c ahb apx` against external official vanilla1.7.10 client JAR. `ahb.k` calls `b(IIIZ)` with true; that calls `apx.b(IIII)` passing sky subtraction, and the chunk method subtracts it from saved SKY and returns max with BLOCK. Native DARK-node giant code already implements this correctly. Wisp bytecode also reads saved SKY (`func_72972_b`) first, not darkened SKY.
6. New actual registered-predicate regression at open-sky y200 (SKY15/BLOCK0/combined15) failed before light fix: `original firebat nextInt7 rejects combined daylight15` (`g13biome-light-before.log`). Shared helper now uses saved SKY then combined light, with original thunder subtraction10; Firebat/Swarm use combined light for their distinct nextInt7 gate.
7. Nine focused runtime tests passed after that correction (`g13biome-final-runtime.log`, build successful58s): the four audit cases, the new daylight/night-seed case, existing Brainy zombie gameplay, flying monster population, taint swarm gameplay, and Pech entity gameplay. This includes actual post-call RNG equality for128 Wisp night seeds.
8. Taintacle has a separate copy of the same incorrect BLOCK-only reading. Added a real taint-biome/fibre-column per-seed predicate fixture before correcting that copy; subsequent results below.
9. Taintacle regression failed on its128-seed actual registered-predicate comparison (`g13biome-taintacle-before.log`); after using the shared corrected helper it passed in the next two-case run. Light correction committed as `97ef04cd`.
10. Additional missing placement confirmed: Inhabited Zombie has a native biome row but no Fabric static placement registration, so vanilla defaults to always true. Original override calls superclass Zombie spawning after isolation and therefore retains the inherited monster light rule. `g13biome-inhabited-before.log` failed only its new daylight-placement assertion; the expanded Taintacle/Wisp light case passed. Added Fabric ON_GROUND registration plus original32/8 predicate; retained instance isolation. NeoForge's explicit unrestricted adapter is not changed/validated under Fabric-first scope.

Dependency provenance for light interpretation: official1.7.10 client SHA256 `a4fc2284657544e0f4bcc964f927c2fda3e3a205178ed1d5d58883aaf9780cce`; immutable TC4 oracle SHA256 `3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be`. External `javap` output: `/tmp/opencode/g13biome-original-world-chunk.javap`. Original BlockAiry was checked as well: no EntityWisp creation, only wisp-essence drops on node harvest.

Final verification command adds three targeted common JUnit classes (`EntityParityRulesTest`, `OriginalSpawnGateTest`, `OriginalSpawnRulesTest`) and eleven exact server cases (six audit cases, original biome-source/duplicate-row test, Brainy gameplay, flying population, Swarm gameplay, Pech entity). JUnit XML: **17 tests, zero failures/errors** (7+5+5). **All11 server GameTests passed; BUILD SUCCESSFUL57s**, `/tmp/opencode/g13biome-final-verification.log`. Actual final common/Fabric production and test sources compiled. No later gameplay edits.

Outcome:36 mobs inspected, five no-discovered-survival-creator types (reasoned-only), four confirmed rule defects across nine types fixed in three gameplay commits. No missing required Fabric biome registration was confirmed. Final docs commit records the audit, not an additional gameplay change.

Commands use `./gradlew :fabric:runGameTest -PgameTestFilter=<comma-separated-exact-names> -PgameTestHeap=1G --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx1G --max-workers=1`.

## Limitations

Native spawn-pool membership proves eligibility, not eventual RNG acquisition, density/cap balance, or all entity placement conditions. Natural champion fixture uses actual NATURAL finalization and production entity-add event with deterministic world RNG, not a full random-tick spawning replay. The special fixtures seed a real swarmer and inhabited zombie, then exercise actual death/tick creators; they do not claim to replay taint spread or acquisition of the Outer Lands. All other special creators remain explicitly source-reasoned. Thunder subtraction10 is oracle-backed code but not a separately forced-weather runtime fixture. No full suite, client or NeoForge validation.
