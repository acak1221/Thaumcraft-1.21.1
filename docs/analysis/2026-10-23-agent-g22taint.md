# g22taint — taint propagation hypothesis

- Branch: `agent/g22taint`; starting commit `9ef54a43`; initial working tree clean.
- Exclusive worktree: `/home/cyberfox-kali/wt/g22taint`.
- Specification read directly from private 4.2.3.5 oracle, especially
  `BlockTaintFibres:156–223`, `BlockTaint:155–218`, `TileEtherealBloom:25–49`,
  `BlockFluidPure:58–67`, and `Config`.
- Found misleading native documentation/tests asserting that mycelium, soul sand,
  red sand, and coarse dirt did not exist in 1.7.10. Podzol is omitted too.
  Original conversion checks materials, independent of metadata.
- Vanilla material-name corroboration (read-only, not copied into the repository):
  `https://raw.githubusercontent.com/eldariamc/client/master/src/main/java/net/minecraft/block/BlockDirt.java`
  (`Material.ground`, metadata1/2), `BlockSand.java` (metadata1 red sand),
  `BlockMycelium.java` (`Material.grass`), `BlockSoulSand.java` (`Material.sand`).
- Built a native GameTest for 45,000 seeded random-tick invocations and a forced
  16-target conversion matrix. Oracle expected1000 eligible conversions with
  standard deviation31.27. Counts/results recorded in `docs/audit/TAINT_FINDINGS.md`.
- Existing native runtime suite covers RNG branch ordering, neighbour thresholds,
  biome writes, stalk→spore, out-of-biome restoration and physical falling custody.
  It must run in this worktree before its assertions are counted as proven here.
- No `allowTaint` or `TaintHelper` exists in this supplied oracle. Do not conflate
  generation configuration or a later-version helper with TC4 spread semantics.
- Bloom cleansing source differences are explicitly reasoned-only, not silently
  mixed into the target-set fix. Pure fluid grants warp ward; it is not an oracle
  biome/taint eraser.

## Execution record

First attempted log redirection failed because the fresh worktree had no `.tmp`;
created the ignored directory and reran. No test executed in that failed command.

Baseline native Fabric GameTest compiled and ran, intentionally failed exactly at
the new material acceptance assertion: **6/11**, not expected11/11. Red sand,
coarse dirt, podzol, mycelium and soul sand each failed actual soil conversion.
The independent rate experiment gave **1013/45000**, expected1000, **+0.416σ**;
maximum one converted cell per invocation. All five excluded targets survived.
Log: `.tmp/g22-baseline.log`.

Implemented one bounded fix: five additions to the native soil compatibility
mapping, plus corrected documentation/unit expectations. No RNG or biome-gate
change. Final native Fabric rerun: **2/2 GameTests passed**, eligible targets
**11/11**, exclusions **5/5**, same **1013/45000**, maximum **1**. Focused material
unit suite: **4/4 passed**. Gradle **BUILD SUCCESSFUL in1m53s**.
Logs: `.tmp/g22-fixed.log`; XML under `fabric/build/gametest-focused-results.xml`
and `common/build/test-results/test/TEST-thaumcraft.taint.TaintMaterialRulesTest.xml`.
No gameplay source changed after that build. `git diff --check` passed.

Audit tally: **22 rule groups inspected, 19 PROVEN runtime-measured/branch-checked,
5 mismatched targets fixed, 3 reasoned-only groups** (crust swarmer, generation
gate interpretation, purification/bloom). Statistical evidence is specifically
the isolated fibre coordinate lottery, not a claim of long-term whole-world
spread rate parity. NeoForge/client/multiplayer/survival acceptance not run.

The optional SRG resolver could not rerun: its external mapping cache was absent,
and the Forge Maven fetch returned403. Existing material-name resolution is
documented in the native rule and independently recognizable in vanilla material
constructors; this unavailable auxiliary fetch is not a passed verification.
