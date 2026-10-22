# g15client — resolve the five orphan mobs

## Scope and outcome

Branch `agent/g15client`, owned worktree `/home/cyberfox-kali/wt/g15client`, starting commit `d2abbdf0`. First action was the requested `cd … && git status --short && git log --oneline -1`; successful and clean. Read `docs/audit/SPAWN_FINDINGS.md` first, then the applicable AGENTS.md instructions. No other worktree or `/home/cyberfox-kali/Thaumcraft-1.21.1` was accessed.

All five targets are **deliberately unreachable in TC4-faithful survival**, not missing spawn mechanisms. Full per-mob evidence/test names/changes: `docs/audit/ORPHAN_MOB_FINDINGS.md`.

Watcher is an unused original entity implementation: no registration or creator, not a demonstrated client-only effect. Greater portal is a retained alias; original boss-room creation uses ordinary CultistPortal. Crawler/seed/prime seed have no class or caller anywhere in the original artifact; the seed ecology belongs to later TC versions and must not be invented for TC4.

## Evidence collected

- Read the private CFR oracle at `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`: ConfigEntities, original taint blocks/fibres, TileEldritchLock; inspected references across the full tree using `rg --no-ignore` (ordinary ignore-aware search misses this external ignored oracle).
- Independently enumerated the bundled immutable original JAR ZIP and searched every `.class` byte stream for the five names using Python. Artifact SHA256: `3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be`.
- Watcher reference inventory contains exactly eight classes: its implementation/four nested classes, model, renderer and ConfigEntities. The other four type names have zero class-byte references; only ordinary EntityCultistPortal exists.
- Ran Java21 `javap -classpath Thaumcraft-1.7.10-4.2.3.5.jar -p -c thaumcraft.common.config.ConfigEntities`; transient output `/tmp/opencode/g15client-configentities.javap`. Ordinary portal is registered; Watcher is only in the champion whitelist. No original/donor content was modified or copied into source.

## Changes

- Five artifact-backed JUnit contracts in `common/src/test/java/thaumcraft/eldritch/OrphanMobOracleContractTest.java`: each pins the original SHA and absence/reference inventory with source citations. Additional source-reference tripwires flag new common/Fabric server references outside the explicit existing config/implementation/rules allowlists. These static guards are not a general dynamic reachability proof and do not inspect NeoForge.
- Declared the original JAR as a common test-task input so changing the oracle cannot silently reuse an up-to-date test result. Ignored this lane's private Gradle home.
- Clarified `EntityCultistPortalGreater` Javadoc: ordinary portal is the original boss, alias has deliberately no survival creator. No gameplay behavior changed.
- Added the orphan findings and linked the older spawn audit to the resolution without rewriting its historical test claims.
- No positive GameTest: none of these five has an oracle-backed survival mechanism. Manual entity construction would not prove one. No new spawn, item recipe, ritual or creative-only removal.

## Verification

Java21; private `GRADLE_USER_HOME=$PWD/.gradle-g15` seeded by copying external `/home/cyberfox-kali/.gradle-leader/caches` into the owned worktree (no writes to shared cache). Gradle may configure the retained NeoForge project; no NeoForge task was requested. Before every Gradle invocation: `rm -rf fabric/run-gametest/world`.

Command:

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/g15client/.gradle-g15
./gradlew :common:test --tests thaumcraft.eldritch.OrphanMobOracleContractTest :fabric:compileJava --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx1G --max-workers=1
```

Initial attempt timed out after 120 seconds during the wrapper distribution download; no test result claimed. Log: `/tmp/opencode/g15client-orphan-tests.log`. A retry incorrectly placed `--tests` after `:fabric:compileJava` and was rejected as an unknown option (`/tmp/opencode/g15client-orphan-final.log`); no test pass claimed for that invocation. The corrected final command above ran successfully after final Java/build-script changes.

**Final: BUILD SUCCESSFUL in 2m 28s**, all seven tasks executed, including actual common production/test compilation and Fabric production compilation. **Five JUnit tests passed, zero skipped/failures/errors**: `common/build/test-results/test/TEST-thaumcraft.eldritch.OrphanMobOracleContractTest.xml`. Final log: `/tmp/opencode/g15client-verification.log`. Existing mixin mapping/deprecation/unchecked warnings remain; no new compile error.

`git diff --check` passed. No runtime-affecting gameplay change; no server/client/NeoForge run, full-suite or survival acquisition claim. No unresolved missing mechanism among the five; ordinary portal/spore/swarmer acquisition remains outside this bounded audit.
