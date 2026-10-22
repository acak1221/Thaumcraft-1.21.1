# Save-cycle findings — g7save, 2026-10-23

## Scope and terminology

Hypothesis: port-owned data disappears across real persistence boundaries despite passing codec tests.
These focused Fabric dedicated-server GameTests are in `SaveCycleRuntimeChecks`, registered in
`fabric/.../ThaumcraftGameTests.java`. No NeoForge or client acceptance is implied.

The premise that *all* earlier tests were in-memory is no longer accurate at this checkout:
`AdvancedFurnaceUnloadRuntimeChecks` already requires native chunk retirement and fresh disk-backed
block entities. This change adds coverage, rather than claiming to invent disk-lifecycle testing.

## Checks

| Area | Test | Boundary and assertions |
| --- | --- | --- |
| Block entities | `saveCycleChunkDiskReload` | Remote chunk, native `save(true)`, remove forced ticket, wait until `getChunkNow` is null **and every old BE is removed**, add radius-zero ticket, require different BE objects. Jar water essentia/filter/facing; crucible 725 mB; component-named furnace stock; paired research table note component/bonus pools; selected matrix debt/cost/player/session fields. |
| Player attachments | `saveCyclePlayerDiskRelogin` | Connected `ServerPlayer` with owned UUID; `PlayerList.remove` saves the player file, then `placeNewPlayer` loads a new player of the same UUID through a fresh embedded connection. Exact knowledge attachment (research, aspect pools, scans, all warp counters) and four accessory slots/components compared. |
| World SavedData | `saveCycleWorldDiskReload` | Store a nonempty labyrinth marker, invoke native data storage save, construct a **separate empty** `DimensionDataStorage` for the actual overworld data directory, read with a factory, require a new map object and exact whole-map serialization. |
| Missing registry item | `saveCycleChunkDiskReload` | Inject `thaumcraft:g7_absent_item` into a second furnace's saved inventory. Require quarantine before save; after actual chunk reload require quarantine and identical raw saved data, not empty inventory or a load crash. |

## What is simulated, and what is real

The chunk check flushes `save(true)` again **after retirement**, before adding its reload ticket,
so pending unload writes are drained rather than intentionally relying on IOWorker's NBT cache.
Chunk and player disk writes/reads are real. The world-data check uses a fresh cache but keeps the
same server/registry provider. The matrix input is an explicitly seeded selected-session tag, not
a recipe acquired/started in survival; it is paused, with real Silverwood recipe predicates/input/output. Its structure-dependent
activation and transient tick/beam/scan state are not restart invariants. Ticket expiry permits
ordinary gameplay ticks, so the exact persistent comparison is taken after old-BE retirement;
the custody values also have explicit pre-save constants.

No JVM/server process is stopped and relaunched; no datapack registry rebuild, multiplayer client
handshake, crash/power-loss durability, changed/missing-mod registry, or cross-version upgrade is
covered. Embedded connections exercise server PlayerList persistence, not a graphical client login.
No in-memory `loadStatic` replacement is mislabeled as chunk unloading.

## Regeneration queue limitation

`ChunkRegeneration.QUEUE` is a world-instance-keyed transient concurrent map, **not SavedData**.
The chunk NBT marker is written/read by `Tc4ChunkRegenerationMarkerMixin`; shutdown explicitly clears
the queue. This patch does not establish that pending work survives shutdown, nor change the
original replay/marker semantics without a reproducer and oracle evidence. Marker durability and
pending-work recovery under interrupted regeneration remain unverified here. The labyrinth map is
the world-level SavedData exercised by this patch; other SavedData classes are not exhaustively covered.

## Execution and outcome

**PASS: four persistence areas survived; zero confirmed production persistence failures.**
The final fresh-world Fabric dedicated-server run passed **3/3 required GameTests**;
Gradle reported `BUILD SUCCESSFUL in 20s`. No production fix was warranted or made.

Evidence: `fabric/build/gametest-focused-results.xml`, final server/build log
`/tmp/opencode/g7save-final.log`. XML reports world map 0.016 s, chunk check 0.959 s,
player relogin 0.303 s. The XML clock is `2026-10-22T21:54:01Z` (environment clock differs
from the assigned 2026-10-23 audit date). The run compiles the actual common/Fabric sources.

An initial chunk assertion failed because quarantine intentionally retains the **full native
load tag**, which gains `id`, coordinates and `keepPacked` in vanilla chunk serialization.
A diagnostic rerun showed the complete original `Items` payload unchanged. The assertion now
compares the exact payload after removing only these vanilla envelope fields. This was a test
expectation correction, **not a reproduced save-loss defect**. World map and player tests passed
even on that initial runtime run. The final run passes all cases against unchanged production code.

Focused command (Java 21; private `GRADLE_USER_HOME=$PWD/.gradle-g7`; delete
`fabric/run-gametest/world` before invoking):

```sh
./gradlew --settings-file settings-g7.gradle :fabric:runGameTest \
  -Penabled_platforms=fabric \
  -PgameTestHeap=1536M \
  -PgameTestFilter=saveCycleChunkDiskReload,saveCyclePlayerDiskRelogin,saveCycleWorldDiskReload
```

`settings-g7.gradle` was a temporary copy of root `settings.gradle`, changing only its last line
to `include 'common', 'fabric'`. It was removed after validation, not committed. Recreate that
copy in the root to reproduce this isolated invocation. All gameplay/build/source files otherwise
remain unchanged. No NeoForge compilation or runtime test was executed.

Cold-cache setup initially stalled in dependency HTTP metadata requests (confirmed by a thread
dump) and was canceled. A Fabric-only `--configure-on-demand` retry failed during configuration:
Fabric references common's `gameTest` source set before common is evaluated in that mode. The
explicit-common-first retry compiled successfully, but Architectury's transformer still tried to
access an uninitialized project's mappings. The temporary Fabric-only settings avoided both
issues without changing production build files. One retry attempted a read-only global dependency
cache, but its directory layout was rejected by Gradle and it was not used. Final validation used
only the private Gradle home. These invocation failures are not evidence of save-data loss.
