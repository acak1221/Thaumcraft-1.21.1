# World-generation crash boundaries — W1 / W2

Branch: `agent/c5fix`; baseline `0dd07be6`; 2026-10-23.

These are high-severity reachable generation exceptions, not hypothetical harmless runtime-tool errors. A normal feature dispatch does not catch them before chunk generation fails. Tests use actual Minecraft classes and native `WorldGenRegion` / `FEATURES` write guards, with supplied terrain and exceptional inputs. They do not claim natural discovery, client, multiplayer, or NeoForge acceptance.

## W1 — absent biome holder

Measured before the fix:

* `worldgenCrashMissingBiomeRefusesColumn`: native generation-region `BiomeColumns.set` with a biome registry lacking `thaumcraft:taint` throws `IllegalStateException: Missing real native biome thaumcraft:taint` at original line 77.
* `worldgenCrashMissingBiomeRefusesFeature`: a real taint holder in the source chunk selects taint decoration, but the region's biome registry deliberately lacks the conversion target. All unrelated registries remain native and present. Seed 42 reaches `ThaumcraftBiomeDecorationFeature.place:54 → decorateTaint:132 → BiomeColumns.set:77`, throwing the same exception. This proves the actual feature route, not just an isolated helper.
* The first feature fixture omitted unrelated registries and failed on a configured-feature lookup; that fixture error was corrected, and the second red run above reached the audited throw. It is not counted as crash-path evidence.

Fix: the taint feature resolves its indispensable conversion target **before any decoration writes or random draws**; a missing target returns `false`. Both region and live-server column setters also refuse missing holders, before rewriting biomes. Successful generation retains its RNG sequence and values.

Diagnostic (one per rejected feature attempt, not eight conversion-loop messages):

```
Skipping biome conversion/decoration at <BlockPos>: Missing native biome thaumcraft:taint
```

Regression assertions require a clean `false`, zero feature block writes, preserved biome storage, and exactly one position/resource diagnostic. The column case additionally tests the live-`ServerLevel` equivalent with an absent resource key.

## W2 — quarantined maze SavedData

Measured before the fix:

* `worldgenCrashMalformedMazeSkipsRooms`: malformed `cells` becomes quarantined, then `OuterLandsMazeFeature.place:181 → MazeSavedData.cell → requireReadable:153` throws `IllegalStateException: Unread thaumcraft_maze map...`.
* `worldgenCrashMalformedMazeSkipsRing`: supplied valid stone/air terrain passes all ring terrain gates; `EldritchRingFeature.placeSelected:82 → MazeSavedData.mazesInRange → requireReadable:153` throws the same exception. The real ring, unlike its former preflight, did not catch it.

Fix: `MazeSavedData.forGeneration(position)` explicitly returns an empty optional for quarantined maps. Both feature consumers check this **before writes** and return `false`. The preflight uses explicit availability rather than a broad `RuntimeException` catch. A per-map atomic flag logs the actual placement refusal once, including across parallel/repeated attempts. The original rejection reason is retained in memory for diagnosis.

Diagnostic:

```
Skipping maze generation at <BlockPos>: unread thaumcraft_maze map (cells must be a compound list); rejected NBT retained; back up and repair cells offline
```

The final runtime tests write malformed native SavedData files, load them through `DimensionDataStorage`, install the resulting quarantined map temporarily, then invoke the real room/ring feature paths twice. Assertions require `false`, zero writes, one diagnostic with position and reason, unchanged rejected NBT (including an unknown field), and no dirtying. The previous map is restored even on failure.

`MazeSaveIntegrityTest` retains every existing throwing fence assertion for direct runtime reads/writes; new assertions cover explicit generation refusal and valid empty-map admission. No malformed map becomes an empty usable map. `save` still returns a defensive copy of the original rejected payload. The maze format (`cells`, int `x`/`z`, short `cell`) is unchanged; availability/reason/log state is not serialized.

## Verification

All runs use Java 21, this worktree's `.gradle-c5fix`, Gradle 8.12.1, and a freshly removed `fabric/run-gametest/world` before Gradle invocation.

From the worktree, export `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` and `GRADLE_USER_HOME=/home/cyberfox-kali/wt/c5fix/.gradle-c5fix`. Remove `fabric/run-gametest/world` before each command:

```sh
./gradlew :fabric:runGameTest -PgameTestFilter=worldgenCrashMissingBiomeRefusesColumn,worldgenCrashMissingBiomeRefusesFeature,worldgenCrashMalformedMazeSkipsRooms,worldgenCrashMalformedMazeSkipsRing --offline --no-daemon
./gradlew :fabric:runGameTest --offline --no-daemon
./gradlew :common:test --offline --no-daemon
```

* Red: `/tmp/opencode/c5fix-red.log` reproduces the region-column throw and both maze consumers; `/tmp/opencode/c5fix-red-feature.log` reproduces W1 through taint decoration.
* Focused fixed run: **4/4 PASS**, `BUILD SUCCESSFUL`; `/tmp/opencode/c5fix-focused.log`. The log records the live-server absent-key refusal, both positional maze refusals, and taint feature preflight refusal.
* Full Fabric GameTest suite: **441/441 PASS** (437 existing + 4 new), `BUILD SUCCESSFUL in 5m 58s`; `/tmp/opencode/c5fix-full.log`, `fabric/build/gametest-results.xml`. This run compiled the final source/test changes and used a fresh world.
* `:common:test`: **2,518/2,518 PASS**, 397 suites, zero failures/errors/skips; `BUILD SUCCESSFUL in 1m 44s`; `/tmp/opencode/c5fix-common-test.log`, `common/build/test-results/test/TEST-*.xml`.

Original 4.2.3.5 ring and `Utils.setBiomeAt` bytecode were inspected from the retained JAR. The original biome helper already returned immediately for a null target (bytecode offsets 0–4); it did not deliberately crash on an unavailable biome. No ring geometry, resource values, maze encoding, or successful-path random draws were changed. This deviation is solely diagnosed exceptional-input refusal instead of server-crashing generation. Bytecode evidence: `/tmp/opencode/c5fix-ring-oracle-bytecode.txt` and `c5fix-utils-oracle-bytecode.txt`.
