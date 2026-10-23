# g24infusion — bounded ritual audit

Branch/worktree: `agent/g24infusion`, `/home/cyberfox-kali/wt/g24infusion`.
Start: clean status, parent `4f989e0b`.

## Specification inspected

Read-only original oracle outside the worktree:

* `thaumcraft/common/tiles/TileInfusionMatrix.java`, SHA-256
  `496b0fccd1d7c96cbf4ac3abb404dc8d90e3d4faa1c8fa29e543a54d0217ba24`.
* `thaumcraft/common/blocks/BlockStoneDevice.java`, SHA-256
  `8fcf6dd1ada422f66c74735351a956310209714c482ac6c5a922b5ec320fa881`.

Original successful ingredients do not increase instability. Original missing
essentia rolls per failed aspect, and valid-center hazards consume the entire
cycle. Original block comparator capability is true for every stone-device
metadata, but the matrix's comparator strength is always zero. Matrix removal
during crafting deliberately explodes with terrain damage; hazards may deliberately
destroy input stacks. Those losses must not be misreported as resource bugs.

## Test design

`InfusionRitualAuditChecks` uses real registered Silverwood recipes, real formed
pillars/pedestals, finite jar stock, native block destruction, and the actual
matrix ticker. Stock and research are declared fixtures, not survival acquisition.
No ritual session, demand, instability, output, or ingredient list is injected.
Repeated-rate trials independently admit a fresh ritual before one real cycle.
World RNG uses deterministic seeds for exact comparison with the original roll.

Build home is private `.gradle-g24`; Java 21, Fabric only. Every Gradle invocation
first removes only this worktree's `fabric/run-gametest/world`.

## Red evidence and first fix

The first Gradle attempt used `-PgameTestFilter=g24Infusion`; the harness requires
exact names, so no cases ran. Corrected to the four explicit case names.

Pre-fix XML: four cases, comparator fails with
`original matrix advertises analog output capability`; the other three pass.
Log `.tmp/g24-red2.log`, preserved XML `.tmp/g24-red2-results.xml`.
Measurement: 12 hazards/1,000 stocked rituals, 83 instability increments/1,000
seven-aspect-shortage rituals; both match independent seeded oracle replay.

The deliberately reduced 1G heap did not finish server shutdown promptly: a
thread dump found the server executing `ChunkMap.scheduleUnload/processUnloads`
after the completed XML report. The owned test JVM was terminated; **no successful
Gradle exit is claimed for this red run**. Final verification uses the repository's
documented 3G server budget instead. No other session/process was stopped.

Production change: restore `BlockInfusionMatrix.hasAnalogOutputSignal = true`.
No custom output-strength override: native zero is the original matrix value.
No ritual resource or hazard probability behavior was changed.

## Final acceptance

Final `:fabric:runGameTest :fabric:build`, five new cases plus three existing
focused infusion regressions, Java21/private home/3G heap: **exit0**, **8/8 required
GameTests pass**, all dimensions save normally, `BUILD SUCCESSFUL in 1m 15s`.
Fabric production JAR emitted at
`fabric/build/libs/Thaumcraft-fabric-0.1.0-alpha.1.jar` (not exported/pushed).

Final measurements repeat 12/1,000 hazards and 83/1,000 essence-shortage
increments. Additional 100 missing-ingredient trials add 259 debt units and nine
instability increments; all equal the independently replayed original RNG rules.
Full paid crafts, exact output components/count, physical pedestal contents,
interruption/reload, actual hazard effects, render payloads and comparators are
covered by the eight cases. No ritual accounting production edit was justified.

`docs/audit/INFUSION_FINDINGS.md` lists **30 driven / 30 PROVEN bounded rules**, one
pre-fix mismatch (comparator capability) fixed. Its separate reasoned-only list
retains real-client rendering/network delivery, crash durability, complete
unstable-run distribution, guaranteed survival through destructive blasts,
untested recipe families/cap bounds, and NeoForge. No full-client/survival verdict.

Final XML SHA-256:
`95a5d26f87310d46808b7b125fe23c0d5fd9c8ecc6e04fe5a9f792dd83939c64`.
Final log SHA-256:
`76b6bd0a2d92652e41495f358ee88275f1d9871e4575b5baa791dd31114067f9`.
