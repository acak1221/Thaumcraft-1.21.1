# g19aspect — research graph / aspect hypothesis

Branch `agent/g19aspect`; base `38bdcb87`. Worktree
`/home/cyberfox-kali/wt/g19aspect`. Initial status was clean. No other worktree or
`/home/cyberfox-kali/Thaumcraft-1.21.1` was accessed.

## Result

See `docs/audit/ASPECT_FINDINGS.md` for oracle scope and exact extractor output.
201 research entries match original graph metadata; 591 page descriptors match;
201 entries reached, zero unreachable; zero nonvirtual map overlaps. 220 sampled
native item/block IDs from 176 original literal registrations have zero amount
mismatches. All 42 compound pairs (84 component slots) match.

One failing test confirmed four stale **provenance** anchors, not wrong gameplay
values: Iron/Gold/Cinnabar Cluster 360/361/362 -> 368/369/370; Primordial Pearl
454 -> 451. Corrected only those anchors. The red test and subsequent six-test
green run were executed, not inferred. No graph or aspect amount fix justified.

## Reproduce

```sh
python3 scripts/porting/test_aspect_research_audit.py -v
python3 scripts/porting/extract_tc4_definitions.py --oracle /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle --check
# Refresh independent data-only Java fixture after a verified oracle change:
python3 scripts/porting/test_aspect_research_audit.py --write-fixture
```

Python result: 6 tests, OK. The fixture generator fails on a wrong native
amount/target anchor, and takes expected amounts/components from the oracle.

## Java/Gradle evidence

Gradle is isolated with Java 21 and
`GRADLE_USER_HOME=/home/cyberfox-kali/wt/g19aspect/.gradle-g19`; before each Gradle
invocation the agent removes only its own `fabric/run-gametest/world` as instructed.
The first invocation targets `:common:test --tests thaumcraft.research.ResearchCatalogTest
-Penabled_platforms=fabric --no-daemon`; log `/tmp/opencode/g19-gradle-initial.log`.
Final native behavioral run/result to be appended below.

No server/client gameplay acceptance claim. No push/export. Original source and
JARs unchanged.
