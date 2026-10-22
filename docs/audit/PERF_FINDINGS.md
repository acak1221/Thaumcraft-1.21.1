# Performance findings — g14perf (2026-10-23)

## Hypothesis and scope

Hypothesis: recurring allocation or an unbounded cache explains the GameTest suite's heap requirement. A bounded reproduction can establish churn/retention, **not** establish that it accounts for the whole-suite final-save peak. No lower suite heap requirement is claimed here.

## MEASURED: notification HUD allocations

The real `NotificationQueue.update` source was compiled with Java 21 and exercised by `scripts/audit/NotificationAllocationProbe.java`. JFR `ObjectAllocationOutsideTLAB` with `-XX:-UseTLAB -XX:-DoEscapeAnalysis` counts actual allocation events with stack traces, rather than samples. Escape analysis is deliberately disabled: these are allocation-site counts, not a production heap-pressure benchmark. No physical client/frame-rate benchmark was run.

| Workload | Calls | Before | After |
| --- | ---: | ---: | ---: |
| Empty HUD queue updates | 1,000 | 2,000 allocation events (2/call) | 0 |

The two original sites were `common/src/main/java/thaumcraft/client/lib/NotificationQueue.java:22` (1,000 ArrayLists) and `:24` (1,000 ArrayList iterators). The fix returns the shared immutable empty list before constructing either. Original nonempty timing/expiry logic is unchanged. Regression test checks 1,000 idle snapshots and reuse after clear.

A second, nonempty workload (10 queued rows, 1,000 updates, after fix) recorded **15,002** events: 9,000 Notification records (`NotificationQueue.java:27`), 1,000 ArrayLists (`:24`), 1,000 iterators (`:26`), 3,000 Object arrays (`:27`, `:31`), 1,000 immutable ListN snapshots (`:31`), plus 2 String/byte-array runtime-linkage events. This is the worst **measured** workload here, not a ranking of the entire mod. Tail-row expiry extension intentionally remains original behavior; no speculative active-queue rewrite.

Reproduce from the repository root:

```sh
mkdir -p .tmp/perf-probe
$JAVA_HOME/bin/javac -d .tmp/perf-probe common/src/main/java/thaumcraft/client/lib/NotificationQueue.java scripts/audit/NotificationAllocationProbe.java
$JAVA_HOME/bin/java -XX:-UseTLAB -XX:-DoEscapeAnalysis -Djava.io.tmpdir="$PWD/.tmp" -cp .tmp/perf-probe NotificationAllocationProbe
$JAVA_HOME/bin/java -XX:-UseTLAB -XX:-DoEscapeAnalysis -Djava.io.tmpdir="$PWD/.tmp" -cp .tmp/perf-probe NotificationAllocationProbe 10
```

## Cache retention, chunk footprint, listeners and SavedData

Runtime measurements are pending below; source inspection alone is not a measured finding.
