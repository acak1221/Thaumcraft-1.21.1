# Performance findings — g14perf (2026-10-23)

## Hypothesis and scope

Hypothesis: recurring allocation or an unbounded cache explains the GameTest suite's heap requirement. A bounded reproduction can establish churn/retention, **not** establish that it accounts for the whole-suite final-save peak. No lower suite heap requirement is claimed here.

## MEASURED: notification HUD allocations

The real `NotificationQueue.update` source was compiled with Java 21 and exercised by `scripts/audit/NotificationAllocationProbe.java`. JFR `ObjectAllocationOutsideTLAB` with `-XX:-UseTLAB -XX:-DoEscapeAnalysis` counts actual allocation events with stack traces, rather than samples. Escape analysis is deliberately disabled: these are allocation-site counts, not a production heap-pressure benchmark. No physical client/frame-rate benchmark was run.

| Workload | Calls | Before | After |
| --- | ---: | ---: | ---: |
| Empty HUD queue updates | 1,000 | 2,000 allocation events (2/call) | 0 |

The two original sites were `common/src/main/java/thaumcraft/client/lib/NotificationQueue.java:22` (1,000 ArrayLists) and `:24` (1,000 ArrayList iterators). The fix returns the shared immutable empty list before constructing either. Original nonempty timing/expiry logic is unchanged. Regression test checks 1,000 idle snapshots and reuse after clear.

A second, nonempty workload (10 queued rows, 1,000 updates, after fix) recorded **15,002** events: 9,000 Notification records (`NotificationQueue.java:27`), 1,000 ArrayLists (`:24`), 1,000 iterators (`:26`), 3,000 Object arrays (`:27`, `:31`), 1,000 immutable ListN snapshots (`:31`), plus 2 additional String/byte-array events. This is the worst **measured** workload here, not a ranking of the entire mod. Tail-row expiry extension intentionally remains original behavior; no speculative active-queue rewrite.

Reproduce from the repository root:

```sh
mkdir -p .tmp/perf-probe
$JAVA_HOME/bin/javac -d .tmp/perf-probe common/src/main/java/thaumcraft/client/lib/NotificationQueue.java scripts/audit/NotificationAllocationProbe.java
$JAVA_HOME/bin/java -XX:-UseTLAB -XX:-DoEscapeAnalysis -Djava.io.tmpdir="$PWD/.tmp" -cp .tmp/perf-probe NotificationAllocationProbe
$JAVA_HOME/bin/java -XX:-UseTLAB -XX:-DoEscapeAnalysis -Djava.io.tmpdir="$PWD/.tmp" -cp .tmp/perf-probe NotificationAllocationProbe 10
```

## MEASURED: mirror endpoint retention (baseline)

`mirrorCacheRetirement` creates/searches/removes 64 actual registered essentia mirrors at distinct positions per mode. Empty scans exercise `mirrorRetryAfter`; scans with an adjacent registered empty jar exercise `mirrorSources` (finding a source does not imply it can pay).

Baseline Fabric GameTest output:

```text
PERF mirrorCacheRetirement removalsPerMode=64 retryRetained=64 sourcesRetained=64
```

The regression assertion failed as intended: both collections retain every retired coordinate until world cleanup. The world-key WeakHashMap does not bound either collection while that world remains live. **Two measured unbounded collections**, in `common/src/main/java/thaumcraft/common/lib/events/EssentiaHandler.java:162-163` (`LevelSources.mirrorSources`, `LevelSources.mirrorRetryAfter`). This test counts entries, not retained bytes; it does not establish the size of this leak in a full-suite run.

Fix: native `TileMirrorEssentia.setRemoved()` retires both coordinate entries, covering block destruction and native chunk unload. A live endpoint's 5-second retry and original slab order are unchanged. This is deliberately distinct from matrix `refreshSources`, whose documented original behavior preserves backoff. Retired endpoints no longer preserve historical backoff across replacement/reload; this lifecycle-only deviation prevents indefinite retention. No arbitrary eviction of live endpoints or stock mutation was added.

Fixed native run: **64 removals/mode -> retryRetained=0, sourcesRetained=0**. The regression invokes the real block-removal lifecycle; chunk unload uses the same override, but no separate unload/GC experiment is claimed. Both baseline and fixed fixtures use native registered blocks/BEs; the regression is included in ordinary acceptance runs and has its own batch.

## REASONED-ONLY / unverified areas

- Matrix source entries already have a `setRemoved -> refreshSources` removal path. Matrix retry timestamps are only lazily removed or discarded at world unload: candidate for a separate measured test, **not fixed or counted as confirmed here**.
- `ChunkRegeneration.QUEUE` has level-unload/server-stop cleanup and a 10-per-tick drain. Queue growth under repeated unmarked reloads was not measured.
- `ArcaneEarNotes` clears at world POST; taint/fluid reentrancy maps remove in `finally`; relay player associations expose quit/server cleanup. These are inspected removal paths, not weak-reference/heap proofs.
- `EmbeddedPlayerTickDriver` registers two callbacks per owner and unregisters them on close/failure. No accumulating-listener reproduction was established; **zero measured listener leaks**, not a proof that every registration is safe.
- No SavedData retention/weak-reference experiment was performed; no SavedData leak finding is claimed.
- Growth-lamp scans and VisNet/renderer allocations remain unprofiled. Their source-level `new` sites are not measured hot-spot findings.

## MEASURED: chunk footprint

Fresh-world focused run, Java 21, **1,024 MiB server heap**, three selected diagnostics. `getLoadedChunksCount` counts chunk-map **holders**, including proto/partially generated neighbours; it is not a count of FULL terrain chunks. All dimensions start from this worktree's deleted test-world directory. These are normal-generator chunks, not manually inserted structures.

| Diagnostic | Holder snapshots | Delta | Forced-chunk entries |
| --- | --- | ---: | --- |
| First ELDRITCH_RING candidate `(0,27)` | 0 -> 529 | +529 | unchanged |
| First HILLTOP_STONES candidate `(1,10)` | 529 -> 926 | +397 | unchanged |
| First MOUND candidate `(1,33)` | 926 -> 1,081 | +155 | unchanged |
| Three structure candidates combined | 0 -> 1,081 | **+1,081** | **0 -> 0** |
| First cinnabar regeneration `(12345,-12345)` | 0 -> 529 | **+529** | **0 -> 0** |
| Replay at the same regeneration coordinate | 529 -> 529 | **0** | **0 -> 0** |

Structure candidates use the suite's exact first-match exclusive-chain selection; this is **not** the complete `surfaceStructuresReachTheOverworld` search, and does not assert that each selected structure successfully placed. Generation through the natural dimension runs its registered feature pipeline. Different candidates share some neighbours, so their deltas are not equal. Between diagnostic batches the prior candidates' holders had already drained (the next regeneration snapshot was 0), rather than remaining permanently forced.

After the successful server's final save, `python3 scripts/audit/count_chunk_records.py fabric/run-gametest/world` read actual nonzero terrain-region location entries (not estimated from holder counts):

```json
{"dimensions/thaumcraft_gametest/natural/region":1610,"region":1624,"total":3234}
```

Thus this **focused run** persisted **1,610 natural-dimension terrain/proto records**, matching its two diagnostic footprints (1,081 + 529), plus 1,624 ordinary GameTest-world records. These are not new whole-suite totals.

### Failed larger experiment — not a pass

The earlier combined run with the complete `surfaceStructuresReachTheOverworld` case exhausted its **1,024 MiB** heap (`.tmp-perf-fixed.log`). Before the OOM it recorded mirror **0/0** and regeneration **+529 / replay 0**; the full surface scenario never reached its final counter. Its full delta is **unmeasured**, not 1,081. It is not counted as accepted test evidence. Given the shared host's memory load, no larger/full-suite replay was attempted here.

The permanent existing surface test now logs per-stage holder snapshots on completion, so a normal full-suite run can produce its exact delta. The two added chunk-generating diagnostics are explicitly excluded from ordinary no-filter acceptance selection; they run only when named through `-PgameTestFilter`, so this audit does **not** add their remote chunk footprint to the normal suite.

## Verification and conclusion

- Baseline mirror regression failed intentionally, retaining **64/64** entries.
- Fixed bounded server run: **3/3 required GameTests passed**, including normal shutdown/final save (`.tmp-perf-bounded.log`).
- Focused JUnit: **8/8 passed** (4 NotificationQueueTest, 4 NotificationMotionTest).
- One allocation hot method measured, with **two idle allocation sites eliminated**; two retaining mirror collections fixed. No measured listener or SavedData leak finding.
- **Causality remains unproven:** the dedicated server never executes the HUD path, and no full-suite retained-byte/heap comparison attributes the 3 GB requirement to mirror caches. The measured chunk-generation footprint is real; do not lower the 3 GB suite heap or claim the entire suite was profiled from these experiments.

Final verification: `:fabric:build` **passed**; the same three focused GameTests passed again (**3/3**, `.tmp-perf-final.log`, 5.072 seconds of tests; Gradle 1m 4s). The stopped-world census repeated exactly (**1,610 natural / 3,234 total records**). Focused JUnit outputs remained **8/8 passing**. Neither the full GameTest suite nor a physical client run is claimed.
