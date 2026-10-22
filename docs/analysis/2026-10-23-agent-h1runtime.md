# h1runtime — reliability review (2026-10-23)

- Worktree: `/home/cyberfox-kali/wt/h1runtime`, branch `agent/h1runtime`.
- First required status/log command succeeded; clean start at `e79b8558`.
- No other worktree or primary checkout was accessed or changed. No delegation or push. No NeoForge task was requested; Gradle's default all-project configuration nevertheless attempted NeoForge setup before the local Fabric-only exclusion described below.
- Audit inventory, severity, trigger states and unproven concerns: [`../audit/ROBUSTNESS_FINDINGS.md`](../audit/ROBUSTNESS_FINDINGS.md).

## R1: mound RNG ownership

The TC4 algorithm intentionally draws loot rarity from the feature RNG and crate-versus-urn from a second world stream. The retained reference `src/main/java/thaumcraft/common/lib/world/WorldGenMound.java:88-93` shows one float with the 0.3 threshold on that second stream; the current port's source comments cite the TC4 oracle at `WorldGenMound:2520-2524`. Preserve that separation and distribution, but adapt the second stream to the native generation context rather than dereferencing the shared server world.

Named 1.21.1 bytecode inspected with `javap -c -p`: `WorldGenRegion`, `Level`, `RandomSource`, `LegacyRandomSource`, `SingleThreadedRandomSource`, `ThreadingDetector`, `ServerChunkCache`, `ChunkAccess`. Outputs retained locally in `.tmp/h1runtime/`, not committed. Key distinction: `Level` creates both a non-thread-safe `random` and a separate `threadSafeRandom`; `getRandom()` exposes the former. `WorldGenRegion.getRandom()` exposes its own field. `LegacyRandomSource` raises the threading exception when an update loses its compare-and-set.

The GameTest calls the actual mound loot factory, uses registered real crate/urn block states, traps any escape from a supplied generation context, checks both sides of 0.3, verifies rarity, counts draws and verifies the feature RNG remains unadvanced. It is a regression test of context isolation, **not** a natural concurrent chunk-generation crash reproducer and not evidence of native C2S/S2C dispatch or client visuals.

## Commands and results

Java home `/usr/lib/jvm/java-21-openjdk-amd64`; private Gradle user home `/home/cyberfox-kali/wt/h1runtime/.gradle-h1`. Gradle commands are serialized.

- First online baseline attempt was stopped during dependency bootstrap after `jcmd Thread.print` showed the daemon blocked in an HTTP response read. It did not execute tests. Copied `/home/cyberfox-kali/.gradle-leader/caches/.` read-only into the private Gradle home's cache (no symlinking/shared writes), then retried offline.
- Standalone eight-worker probe using the real named 1.21.1 `RandomSource.create(42L)` and competing `nextFloat()` calls: observed `net.minecraft.ReportedException: Accessing LegacyRandomSource from multiple threads`, at `LegacyRandomSource.next` / `BitRandomSource.nextFloat`; exit 0 after asserting the expected exception. Probe source/log retained locally under `.tmp/h1runtime/`. The first diagnostic attempt lacked Minecraft version initialization; it is not the successful evidence. The successful run initialized `SharedConstants.tryDetectVersion()` and excluded an obsolete SLF4J API from the diagnostic classpath. This confirms the RNG failure mechanism only, not a naturally generated mound crash.
- Offline all-project baseline failed in NeoForge configuration (missing mappings download during Loom cache recovery), before tests. To keep verification Fabric-only without changing repository settings, used an ignored init script `.tmp/h1runtime/fabric-only.init.gradle` whose `settingsEvaluated` callback removes the `neoforge` project descriptor, with `-Penabled_platforms=fabric`.
- Baseline targeted `./gradlew --offline -I .tmp/h1runtime/fabric-only.init.gradle -Penabled_platforms=fabric :common:test --tests '*WorldGen*' --console=plain`: **PASS**, 50 tests / four XML suites, zero failures/errors (43 seconds). Existing mixin mapping/deprecation/unchecked warnings were emitted; no source compilation errors.
- Before-fix focused GameTest: **EXPECTED FAIL**, one required failure with `AssertionError: Mound escaped generation RNG through getLevel`, through the actual `WorldGenMound.lootState` at old line 222. Gradle exited 1 after the assertion; test compiled and ran in the real Fabric server. Log `.tmp/h1runtime/mound-before.log`, preserved XML `.tmp/h1runtime/mound-before-results.xml`. Run used a freshly deleted `fabric/run-gametest/world`, never a reused save.
- After-fix focused GameTests (`moundGenerationRandomIsIsolated,surfaceStructuresReachTheOverworld`): **PASS**, 2/2 required tests, 57.38 seconds of tests / 3m18s Gradle invocation. Same Fabric-only/offline prefix, freshly deleted GameTest world. The second case is existing natural-noise surface-structure coverage, not a new test of parallel scheduling. Log `.tmp/h1runtime/mound-after.log`, preserved XML `.tmp/h1runtime/mound-after-results.xml`. Fabric `verifyTestIsolation` also passed during this invocation. Existing startup messages report missing entity data fixers; this is not a claim of an entirely warning/error-free mod startup.
- Final common JUnit suite (`./gradlew --offline -I .tmp/h1runtime/fabric-only.init.gradle -Penabled_platforms=fabric :common:test --console=plain`): **PASS**, 2,461 tests in 381 XML suites, zero failures/errors/skips, 19 seconds. Log `.tmp/h1runtime/final-junit.log`; reports `common/build/test-results/test/`. Production source was compiled after the final fix during the successful Fabric GameTest invocation; the final JUnit invocation reused that unchanged output.

Reproduction commands (with the environment and Fabric-only init script above):

```sh
rm -rf fabric/run-gametest/world
./gradlew --offline -I .tmp/h1runtime/fabric-only.init.gradle -Penabled_platforms=fabric :fabric:runGameTest -PgameTestFilter=moundGenerationRandomIsIsolated --console=plain
```

Run the same case against the parent source for the expected assertion failure; against this fix it must pass. The post-fix validation also selects `surfaceStructuresReachTheOverworld` in the comma-separated filter. The test helper is placed in the production factory's package to access its package-private method, but lives only in the `gameTest` source set; production code does not reference the harness.

## Outcome

Scoped files reviewed by area (overlapping counts): world generation **28** (all 11 Feature subclasses plus 17 helper files/scopes), optional lookups **15**, lifecycle **4**, packets **20**, numeric edges **12**, re-entry **3**. **1 finding; 1 test-proven ownership defect; 1 fix.** Four unproven concerns are kept separate in the audit: mound footprint clipping, maze multi-step concurrency, essentia-flow appearance admission, and artificial low-aura reducer inputs.

No full game world generation, client, multiplayer or NeoForge acceptance is claimed. No natural-world concurrent crash was reproduced; the isolation regression and standalone native RNG contention probe establish the defect and failure mechanism separately.
