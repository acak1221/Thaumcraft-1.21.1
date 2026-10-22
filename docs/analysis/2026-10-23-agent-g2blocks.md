# g2blocks — block removal and contents

Worktree: `/home/cyberfox-kali/wt/g2blocks`, branch `agent/g2blocks`.
Initial commit: `d6ae5c78`; initial working tree clean.

## Hypothesis and bounded scope

Check whether breaking/removal loses or duplicates content compared with the private TC4 4.2.3.5 oracle. Findings matrix: `docs/audit/BLOCKDROP_FINDINGS.md` (40 block entries, one separately identified entity). Only one removal-path defect is selected for repair: the Magic Box's player-only evacuation hook.

Oracle `BlockMagicBox:72-75` calls `InventoryUtils.dropItems` from `breakBlock`; the port initially calls `TileMagicBox.dropContents` only from `playerWillDestroy`. Therefore destruction not initiated by a player and ordinary block replacement bypass the evacuation callback. The original inventory helper copies stack NBT/count and clears slots, including when removal follows creative destruction. Creative container drops must not be suppressed merely to follow a general assumption in the task prompt.

## Tests

New shared `BlockDropRuntimeChecks` with Fabric wrappers:

* Magic Box: native destroy, native replacement, real explosion, native creative break, native survival Silk Touch break.
* Controls: Hungry Chest, arcane pedestal, deconstruction table, arcane workbench, alchemy furnace, focal manipulator, Thaumatorium.
* Aspect/void jars: exactly one variant-correct ground item, `Amount=37`, `Aspect=aer`, filter component `aer`, maximum stack size one, and no loose duplicate label.
* Component/count conservation on the ground; old block entity removed and inventory empty; no duplicate stock. The seven inventory controls also assert same-state updates retain inventory. Inventory/Magic Box fixtures check that the native piston admission predicate rejects their BE block; the jar cases do not include that predicate assertion.

Baseline actual dedicated-server Fabric runtime: **14 tests, 3 failures, 11 passes**. Only Magic Box explosion, non-player destruction, and replacement fail, with missing exact 17 component-bearing diamonds. Native creative/Silk breaks already pass. Both jars and all seven inventory controls pass; these are negative results against the loss hypothesis, not blanket acceptance of every removal mode.

Red evidence: `.tmp/g2-red-runtime.log`, `.tmp/g2-red-results.xml` (copied before the green run can overwrite the focused report). The test report's native timestamp is `2026-10-22T20:02:37.323740112Z`; this session's task/report date is 2026-10-23. The report contains 14 actual cases, not a manually synthesized count.

Implemented fix: replace the Magic Box's player-only hook with `onRemove`, gated on a different replacement block. `TileMagicBox.dropContents` now transfers exact physical stacks to the existing durable `DeferredItemDrops` adapter, clears the owning list, and does not spawn entities synchronously inside block-entity removal. This also uses the oracle inventory helper's independent scatter RNG rather than the port's previous center-only spawn. No second player hook remains to duplicate evacuation. Native creative behavior is deliberately retained.

**Green runtime: 14 tests, 0 failures, `BUILD SUCCESSFUL`.** Final production sources and test wrappers compiled, Fabric test-source isolation passed, and the actual headless GameTest server ran all fourteen selected cases. Evidence: `.tmp/g2-green-runtime.log`, `.tmp/g2-green-results.xml`, and `fabric/build/gametest-focused-results.xml`. Tests were unchanged between the actual red and green runtime invocations. The fix is one block with three demonstrated losing removal modes, not three distinct broken blocks. No duplication was confirmed. Ten block entries have new runtime evidence; thirty remain source-reviewed only. Full-suite, client, NeoForge, linked-box topology, canceled-spawn callback and physical piston-actuation acceptance were not run.

## Environment

Java 21: `/usr/lib/jvm/java-21-openjdk-amd64`.
Private Gradle home: this worktree's `.gradle-g2`.
First invocation bootstraps Gradle/dependencies from an empty cache. Fabric only; no access to the main checkout or another agent's worktree.

The initial attempt configured the NeoForge module even though only a Fabric task was requested. A thread dump of this agent's daemon showed a mapping download in `ForgeMappingsMerger.mergeMojang` before any compilation/test tasks. This agent terminated only its own daemon and restarted with `-Penabled_platforms=fabric --configure-on-demand`. The first attempt is not red-test evidence (and its abbreviated filter was not a valid exact test name). The actual red/green command selects all 14 exact `blockDrop*` wrapper names, using `.tmp/g2-run-drops.sh`; logs remain in this worktree's ignored `.tmp/`.

Configuration-on-demand additionally requires requesting `:common:gameTestClasses` before `:fabric:runGameTest`, since Fabric's source-set setup reads the common `gameTest` source set during evaluation. The first configure-on-demand attempt failed before compilation for that ordering reason; it is not gameplay evidence. No build-system source changes were made for either bootstrap issue.

The ordered configure-on-demand attempt compiled the actual common production sources, common GameTests, Fabric production sources, and all Fabric GameTest wrappers successfully, but Architectury's transformer setup then requested an unconfigured platform mapping provider. To keep Fabric execution isolated, `.tmp/g2-fabric-only.init.gradle` removes only the NeoForge project descriptor from this invocation's settings model; the repository's settings/build files and NeoForge sources remain unchanged. The eventual runtime invocation uses this temporary init script, not configuration-on-demand.

Temporary init script (not shipped in production):

```groovy
settingsEvaluated { settings ->
    def neo = settings.findProject(':neoforge')
    if (neo != null) settings.rootProject.children.remove(neo)
}
```

Runtime command uses Java 21 and the private Gradle home above:

```sh
./gradlew :common:gameTestClasses :fabric:runGameTest \
  -PgameTestFilter=<comma-separated exact blockDrop wrapper names> \
  -PgameTestHeap=768M -Penabled_platforms=fabric \
  -I .tmp/g2-fabric-only.init.gradle \
  -Dorg.gradle.jvmargs='-Xmx768M -Dfile.encoding=UTF-8' --console=plain
```
