# GameTest mock-player failure cleanup (fixmock)

## Framework inspection

Inspected with Java `javap -p -c`, using the leader's read-only mapped vanilla JAR:
`~/.gradle-leader/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2/minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`.
Classes: `GameTestHelper`, `GameTestInfo`, `GameTestSequence`, `GameTestAssertException`, `TestFunction`.

- `GameTestHelper.assertTrue(boolean, String)` constructs and throws
  `GameTestAssertException`, which extends `RuntimeException` (not `AssertionError`).
- `GameTestHelper.fail(String)` also constructs and throws that exception; it
  does not record a verdict itself.
- `GameTestInfo.startTest()` constructs a helper and invokes `TestFunction.run`.
  Its exception table catches `Exception` and calls `fail(Throwable)`.
- `GameTestHelper.runAtTickTime(long, Runnable)` delegates to
  `GameTestInfo.setRunAtTickTime`. `tickInternal()` runs due callbacks, catches
  `Exception` from each, and calls `fail(Throwable)`.
- `tickInternal()` handles timeouts by calling `fail(Throwable)` with a
  `GameTestTimeoutException`; expired sequences can also report their failure.
- `GameTestInfo.fail(Throwable)` assigns `error`, then calls private `finish()`.
  `GameTestInfo.succeed()` also calls `finish()`. That method marks `done` and
  stops the timer, guarded against repeated completion.
- Raw Java `Error` escaping a directly supplied callback is **not** caught by
  these vanilla `Exception` handlers. This change concerns framework verdicts,
  not recovery from arbitrary VM errors.

## Verification

All Gradle invocations use this worktree and its own `.gradle-fixmock` home,
with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`. Results recorded below
only after the corresponding command finishes.

Initial cold-cache invocation used `-PgameTestFilter=fixmockMutation` incorrectly:
the selection mixin requires exact names, not prefixes. It built and launched but
selected no tests; **not** leak evidence:

```text
java.lang.IllegalArgumentException: Expected one GameTest for 'fixmockMutation', found 0
> Task :fabric:runGameTest FAILED
BUILD FAILED in 12m 8s
```

The corrected filter is
`-PgameTestFilter=fixmockMutationAssertion,fixmockMutationCallback,fixmockMutationTimeout`.
The three temporary scenarios each create one connected mock player and deliberately
fail via `assertTrue(false, ...)`, a tick-1 assertion callback, or no verdict before
`timeoutTicks=5`. Temporary `finish()` return instrumentation checks that exact
player UUID in `PlayerList.getPlayer(UUID)` and also prints the total player count.
This measures the player list directly, not merely the cleanup registry.

### BEFORE: original helper-only cleanup

Command: `./gradlew --console=plain :fabric:runGameTest -PgameTestFilter=fixmockMutationAssertion,fixmockMutationCallback,fixmockMutationTimeout`
(exit 1, expected deliberate failures). Real output, omitting timestamps:

```text
FIXMOCK created present=true
FIXMOCK finish thaumcraftgametests.fixmockmutationassertion ownPlayerPresent=true players=1
FIXMOCK created present=true
FIXMOCK created present=true
FIXMOCK finish thaumcraftgametests.fixmockmutationcallback ownPlayerPresent=true players=3
FIXMOCK finish thaumcraftgametests.fixmockmutationtimeout ownPlayerPresent=true players=3
3 required tests failed :(
BUILD FAILED in 44s
```

### AFTER: framework finish cleanup

Same command, same three deliberately failing scenarios (exit 1, expected):

```text
FIXMOCK created present=true
FIXMOCK finish thaumcraftgametests.fixmockmutationassertion ownPlayerPresent=false players=0
FIXMOCK created present=true
FIXMOCK created present=true
FIXMOCK finish thaumcraftgametests.fixmockmutationcallback ownPlayerPresent=false players=1
FIXMOCK finish thaumcraftgametests.fixmockmutationtimeout ownPlayerPresent=false players=0
3 required tests failed :(
BUILD FAILED in 39s
```

The one remaining player at callback completion belongs to the still-running timeout
test; each completed test's UUID is absent and the final count is zero. This does not
measure heap usage or count chunk tickets directly. Both mutations intentionally
remain red: the fix releases players, it does not swallow test failures.

## Implementation

- Both helper mixins shadow their owning `GameTestInfo` and track into shared
  `common/src/gameTest/java/thaumcraft/common/testing/MockPlayerCleanup.java`.
- Shared ownership is keyed by info, so a second helper for the same test has the
  same teardown. Registry draining is idempotent and retains the existing
  player-list removal/discard behavior and exception guards.
- Both loaders inject at `GameTestInfo.finish()` return, covering success and
  all framework-recorded failures. Removed the helper succeed/fail hooks.
- Fabric registers the new mixin in `thaumcraft-gametest-selection.mixins.json`.
  The actual NeoForge config is `thaumcraft-neoforge-gametest.mixins.json`;
  inspection also found the old helper cleanup mixin was never registered there.
  Registered both tracker and finish hook, retaining its connection setup mixin.
- Removed all three temporary scenarios, their UUID registry and diagnostic logging
  before the full-suite run. No existing scenario is weakened or changed.
- Ignored this worktree's private Gradle home.

Raw local logs: `/tmp/opencode/fixmock-before-three.log` and
`/tmp/opencode/fixmock-after-three.log`; bytecode inspection:
`/tmp/opencode/fixmock-javap.txt`, `/tmp/opencode/fixmock-exceptions.txt`.

## Final clean-world suite

After removing all mutation code and instrumentation:

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/fixmock/.gradle-fixmock
rm -rf fabric/run-gametest/world && ./gradlew --console=plain :fabric:runGameTest
```

Exit 0; real output from `/tmp/opencode/fixmock-full.log`:

```text
325 tests are now running at position 6049397, -59, -3151672!
========= 325 GAME TESTS COMPLETE IN 3.452 min ======================
All 325 required tests passed :)
ThreadedAnvilChunkStorage: All dimensions are saved
BUILD SUCCESSFUL in 4m 49s
```

Independent XML count from `fabric/build/gametest-results.xml`:
`testcases=325 failures=0 errors=0`.

## NeoForge compile check and limits

Ran `./gradlew --console=plain :neoforge:compileGameTestJava` with the same
Java/isolated Gradle home, sequentially after Fabric. Exit 1:

```text
> Task :neoforge:compileGameTestJava FAILED
neoforge/src/gameTest/java/thaumcraft/neoforge/testing/ThaumcraftGameTests.java:115: error: method run in class PlantRuntimeChecks cannot be applied to given types;
        PlantRuntimeChecks.run(test);
                          ^
  required: GameTestHelper,ServerPlayer
  found:    GameTestHelper
  reason: actual and formal argument lists differ in length
1 error
BUILD FAILED in 20s
```

Confirmed `git show 5a6b279c:neoforge/src/gameTest/java/thaumcraft/neoforge/testing/ThaumcraftGameTests.java`
already contains that one-argument call; this task does not edit that file or
`PlantRuntimeChecks`. Left the unrelated existing mismatch unchanged. Raw log:
`/tmp/opencode/fixmock-neoforge-compile.log`.

NeoForge runtime and a successful whole-source-set NeoForge compile remain
**unverified**. Both loader mixins/configs were updated consistently; Fabric's
real runtime is the behavioral proof. No claim is made about raw `AssertionError`
recovery, reduced heap requirements, or direct chunk-ticket measurements.

Implementation commit: `386fdd39` (`fix(gametest): release mock players at framework completion`).
