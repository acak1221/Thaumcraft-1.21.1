# Agent `isolate` — the golem scenarios of the suite must survive their neighbours (2026-10-23)

Branch: `agent/isolate` (worktree `/home/cyberfox-kali/wt/isolate`).
Domain: the GameTest harness of the port — the three golem scenarios that pass alone and fail in
the whole suite.

Sources used:

* the leader's measurement on the current tree (`7453d5eb`, the merge of `agent/golemfix`), a clean
  world, 325 cases and three failures;
* the 1.21.1 `minecraft-merged` jar of the Gradle cache, read with `javap` (the GameTest framework:
  `GameTestInfo`, `GameTestRunner`, `StructureGridSpawner`, `StructureUtils`, `GoalSelector`,
  `WrappedGoal`, `ChunkMap`); nothing of that decompilation is committed;
* the port's own GameTest harness (`common/src/gameTest/java/thaumcraft/common/testing/**`) and the
  production it drives.

Gradle ran in this worktree with the private Gradle home
`/home/cyberfox-kali/wt/isolate/.gradle-iso` (a copy of the warm Loom cache of the tree) and a
daemon heap of 768 MB. The GameTest server ran with `-PgameTestHeap=3G` (see §6).

---

## 1. The symptom

The leader's run on the current tree, on a clean world:

```
GameTest: 325 cases, 3 failed
 - thaumcraftgametests.golemfetterholdsgolemswhileitsredstonesignallasts
     the fetter lanes did not finish within 1500 ticks: released=false; walking phase 0
     held golem (active fetter) fettered=true home 0 source 64 carrying 0 minecraft:air at (...)
 - thaumcraftgametests.ordercoloursroutemarkersandslotsandthebellcyclesthem
     the Fill golem did not deliver from the coloured chest within 1200 ticks:
     home 0, near 64, far 64, carrying 0 minecraft:air, golem at (...)
 - thaumcraftgametests.entropyswitcheschangewhatrealgolemsfetchemptyandsort
     the item-match lanes did not finish within 1800 ticks: fill/damage on [WAITING]
     home empty | other 0=1 minecraft:iron_pickaxe 1=1 minecraft:iron_pickaxe 2=1 ... carrying 0 minecraft:air
```

The three scenarios alone passed (`-PgameTestFilter=…`, 3 of 3). All three are the golem scenarios
that need a golem that **walks**: the entropy item match (the Fill golem fetches two worn pickaxes
from the marked chest), the order colours (the Fill golem empties the colour-3 chest) and the golem
fetter (the free golem walks to its source and back). Every other golem scenario of the suite
(configures a golem, gives it an upgrade, shoots with it) never moves and never failed.

**The production is not the cause.** The three scenarios pass alone on the same tree, and the
mechanism below kills the golems from the outside.

---

## 2. The bisect

The interfering scenario is one of the **36 scenarios that share the lane fixture of the port**
(`WaveLane`), and the smallest companion set is a single one of them. The companion used here is
`banner_keeps_its_colour_and_aspect` (`EldritchBlockRuntimeChecks.bannerKeepsItsColourAndAspect`,
`common/src/gameTest/java/thaumcraft/common/testing/EldritchBlockRuntimeChecks.java:138`), because
it is a lane scenario of the default batch and it ends in a few ticks.

| run (before the fix) | result |
|---|---|
| the three golem scenarios alone (`-PgameTestFilter=golemfetter…,ordercolours…,entropyswitches…`) | **3 of 3 passed** (2.2 s) |
| the item-match scenario + `banner_keeps_its_colour_and_aspect` | **1 failed** — the item-match scenario, with the leader's own message |
| the colours + the fetter + the banner, the two golem scenarios moved into the default batch | **1 failed** — the fetter scenario |
| the whole suite, clean world, `-PgameTestHeap=3G` | 325 of 325 passed (five runs) — the leader measured three failures on the same tree |

The focused runs are the bisect: the two golem scenarios fail **as soon as a lane scenario shares
their batch**, and they pass when the batch holds nothing else. The whole suite did not reproduce
the three failures in this worktree (five runs, 325 of 325 each); the interference is the same in
both configurations, and the focused run is the reproduction that the bisect can name.

The failure of the item-match scenario in the focused run, verbatim (the golem's state is printed
by the new diagnostics of §5):

```
the item-match lanes did not finish within 1800 ticks: fill/damage on [WAITING] home empty |
 other 0=1 minecraft:iron_pickaxe 1=1 minecraft:iron_pickaxe 2=1 minecraft:iron_pickaxe |
 carrying 0 minecraft:air at -3107287, -57, 4821924
 [removed=true alive=false area=true work=false fettered=false nav=true ticks=3]; …
```

**The golem was gone after three ticks.** It was not stuck, not unable to work and not out of its
area: it was **discarded**.

---

## 3. The mechanism, with the evidence

### 3.1 The clean up of the shared lane reached into the neighbouring scenario

`WaveLane` is the fixture of the 36 lane scenarios of the port
(`common/src/gameTest/java/thaumcraft/common/testing/WaveLane.java`). Its clean up swept **sixteen
blocks** around the origin of the lane and discarded every `Mob`, `ItemEntity` and `Projectile` it
found there:

```java
level.getEntities((Entity)null, area(16), entity -> entity instanceof Mob || entity instanceof ItemEntity
                || entity instanceof Projectile).forEach(Entity::discard);
```

**A golem is a `Mob`** (`EntityGolemBase` extends `Mob`), so the clean up of one scenario discarded
the golem of its neighbour.

The geometry of the vanilla GameTest grid makes the overlap certain:

* the empty structure of the Fabric API is **8×8×8** (`data/fabric-gametest-api-v1/gametest/structure/empty.snbt`:
  `size: [8, 8, 8]`), and `StructureGridSpawner.spawnStructure` moves the next structure by
  `bounds.getXsize() + 5` (`javap net/minecraft/gametest/framework/StructureGridSpawner`, the
  `move(… + 5, 0, 0)`), so **the next scenario of a batch is thirteen blocks away** (the rows are
  `rowBounds.getZsize() + 6` = fourteen);
* the lane's own origin is three blocks into its structure (`new WaveLane(test, 3, 2, 3)`), so the
  lane of the neighbour is **sixteen** blocks away — exactly the radius of the sweep.

The focused run of the fetter scenario proves it: the log of the run shows the three lanes of the
fetter scenario losing their golems, one after the other, to the clean up of the lane scenario of
the same batch:

```
Fetter lane held golem (active fetter) was discarded by a neighbouring scenario; the fixture arms it again
Fetter lane free golem (inactive fetter) was discarded by a neighbouring scenario; the fixture arms it again
Fetter lane walking golem (fetter switched on under it) was discarded by a neighbouring scenario; the fixture arms it again
```

The same run showed the *second* defect, which the first one hid.

### 3.2 The ring of chunks around a golem is shared and the forced chunks are a set, not a counter

A golem plans a route only when every chunk within 48 blocks of it and of its destination is loaded
(`EntityGolemBase.loadedNavigationArea`, `common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java:1021-1033`).
`GolemChunks.hold` forces that ring so that the golem has it:

```java
if (!level.getForcedChunks().contains(ChunkPos.asLong(x, z))) { level.setChunkForced(x, z, true); forced.add(new ChunkPos(x, z)); }
level.getChunk(x, z);
```

The forced chunks of a level are a **set**, not a counter: the scenario that forces a chunk first
**owns** it, and the scenarios that find it already forced do not take it over. When the owner
releases its chunks at the end of its test, the neighbour **loses a chunk of its own ring** — and
the rings of two neighbouring scenarios overlap almost completely (the ring is 96 blocks wide, the
neighbour is thirteen blocks away). The chunk unloads, `loadedNavigationArea` is false, and the
route of the golem refuses to start (`InventoryGolemGoals.Route.start`,
`common/src/main/java/thaumcraft/common/entities/golems/InventoryGolemGoals.java:722`:
`if (!golem.loadedNavigationArea(pursued.pos())) { count = 0; return; }`).

The focused run of the fetter scenario shows it, once the golems survive the first defect:

```
Fetter lane held golem (active fetter) has no loaded navigation area
Fetter lane free golem (inactive fetter) has no loaded navigation area
Fetter lane walking golem (fetter switched on under it) has no loaded navigation area
```

and the failure of the scenario names it:

```
the fetter lanes did not finish within 1500 ticks: released=true; walking phase 2
 held golem (active fetter) fettered=false home 0 source 64 carrying 0 minecraft:air at …
 [removed=false alive=true area=false work=true fettered=false nav=true ticks=1500];
 free golem (inactive fetter) … home 8 source 56 carrying 0 minecraft:air … [area=false …];
 walking golem … carrying 8 minecraft:cobblestone … [area=false …];
```

**The free golem had already delivered eight cobblestones** and the walking golem carried eight more
— the golems work, they just cannot plan the way home, because the ring of chunks around the
fixture is not whole.

### 3.3 What the vanilla does and does not do

* `GameTestInfo.succeed` removes the entities of the structure of the finished scenario
  (`getEntitiesOfClass(Entity.class, getStructureBounds().inflate(1), …)`, `javap` of
  `net/minecraft/gametest/framework/GameTestInfo`). The bounds of the 8×8×8 empty structure plus one
  block reach **eleven** blocks — they reach the fixture of the neighbour, whose base is three blocks
  into its structure. **The vanilla's own clean up of a finished scenario is wide enough to touch the
  neighbour**, and the port's scenarios have to live with it.
* `GameTestRunner$1.testCompleted` **unforces every forced chunk of the level at the end of a
  batch** (`new LongArraySet(level.getForcedChunks())` and then `setChunkForced(…, false)`). The
  forced chunks of a level are therefore shared between all the scenarios of a batch, and a scenario
  that releases its own chunks in the middle of the batch takes a chunk away from its neighbours.
* The batches run one after the other (`GameTestRunner.runBatch` is called by the listener of the
  finished batch), so the scenarios of two different batches never run at the same time. The
  interference is always **inside one batch**, between the scenarios the grid packs next to each
  other.

---

## 4. The fix

The production is **not** touched: the golems of the port work, the harness of the port killed them.
The fix is in the harness and it makes the three golem scenarios independent of what their
neighbours do.

### 4.1 The clean up of the lane stays inside the lane

`common/src/gameTest/java/thaumcraft/common/testing/WaveLane.java`

```java
/** The radius of the clean up sweep of the lane, in blocks. … */
static final int CLEAN_RADIUS = 5;
```

The lane's own footprint is a floor and a roof of four blocks around its origin, so five blocks
catch everything the content spawns inside the lane (the champion spiderlings, the cultist minions
of the portal, the drops of the dead mobs) and stop **eight blocks short** of the fixture of the
neighbour, which starts nine blocks away.

### 4.2 The golem scenarios take their ring of chunks back

`common/src/gameTest/java/thaumcraft/common/testing/GolemChunks.java`

```java
static void refresh(ServerLevel level, Iterable<BlockPos> fixture, List<ChunkPos> forced) { … }
```

The ring is forced again on every tick of the three golem scenarios, and the chunks that another
scenario had taken away are recorded, so that the scenario releases them itself at the end. The
scenario no longer depends on the chunk tickets of another one.

### 4.3 The golem scenarios arm their golem again

* `GolemItemMatchRuntimeChecks` keeps the configuration of every lane and arms the lane again when
  its golem was discarded (`arm(lane)`);
* `GolemColorsRuntimeChecks` keeps the configuration of the routing golem and arms it again
  (`armRouter()`);
* `GolemFetterRuntimeChecks` already armed its lanes again; its rule assertion now waits for the
  fresh golem's first living update, because the original reads the block under its feet on every
  living update (`lane.golem.tickCount > 0`).

The assertions are **not** weakened: the golem still has to deliver the goods, the negative lanes
still have to leave every chest untouched, and the fetter still has to hold and release the golem.

### 4.4 The failure message names the state of the golem

`common/src/gameTest/java/thaumcraft/common/testing/GolemState.java` (new) prints
`removed=… alive=… area=… work=… fettered=… nav=… ticks=…` in the failure message of the three
scenarios, and logs the entities that stand around a fixture when a scenario starts. A red golem
scenario now says in the log of the run whether its golem was gone, unable to work or out of its
area, instead of only in a debugger.

---

## 5. The proof

### 5.1 The focused runs (the reproduction)

| run | before the fix | after the fix |
|---|---|---|
| the item-match scenario + `banner_keeps_its_colour_and_aspect` | **1 failed** (the item-match scenario; the golem `removed=true … ticks=3`) | **2 of 2 passed** |
| the colours + the fetter + the banner (the two golem scenarios moved into the default batch) | **1 failed** (the fetter scenario; first the discard, then `area=false`) | **3 of 3 passed** |
| the three golem scenarios alone | 3 of 3 passed | 3 of 3 passed |

### 5.2 The whole suite

```
$ rm -rf fabric/run-gametest/world
$ ./gradlew --console=plain :fabric:runGameTest -PgameTestHeap=3G
[13:48:46] [Server thread/INFO] (Minecraft) ========= 325 GAME TESTS COMPLETE IN 1.472 min ======================
[13:48:46] [Server thread/INFO] (Minecraft) All 325 required tests passed :)
```

The whole suite was run **twice on a clean world** after the fix, and the log of neither run holds
a "discarded by a neighbouring scenario" or a "no loaded navigation area" line.

### 5.3 The unit tests

```
$ ./gradlew --console=plain :common:test
BUILD SUCCESSFUL
2263 tests, 0 failures
```

The new `thaumcraft.integration.GameTestIsolationTest` has three tests:

* `theLaneCleanupStaysInsideItsOwnFixture` — the radius of the sweep of the lane stays below the
  distance of the neighbour's fixture (the vanilla grid: an eight block wide structure and five
  blocks of margin, so thirteen blocks between the lanes, and the neighbour's fixture nine blocks
  away);
* `theGolemScenariosArmTheirGolemAgain` — the three golem scenarios watch their golem and arm it
  again;
* `theGolemScenariosTakeTheirChunkRingBack` — the three golem scenarios take their ring back.

**The guard was proved**: with `CLEAN_RADIUS` set back to sixteen the first test fails
(`theLaneCleanupStaysInsideItsOwnFixture FAILED`, `3 tests completed, 1 failed`), and it is green
again with the radius of five. The JUnit task now declares the GameTest harness as an input
(`build.gradle`), so a change of the harness runs the suite again.

### 5.4 The ledger

`docs/porting/scope-ledger.json` is **not** touched: the units of the ledger are the classes of the
original 4.2.3.5, and this wave changes the GameTest harness of the port, not a unit of the port.

```
$ python3 scripts/porting/update_scope_ledger.py --check --original-root /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle
… "stale_review_fingerprints": 0
```

---

## 6. What is still open

* **The heap of the GameTest server.** The suite of 325 scenarios needs **3 GB**: with the default
  `-Xmx2G` of `fabric/build.gradle` the run dies with `OutOfMemoryError: Java heap space` in the
  chunk save path — the known item of `docs/analysis/2026-10-23-agent-memory.md` (§2, the table of
  the wave). `fabric/build.gradle` now takes the heap of the server from the property
  `gameTestHeap` (the default stays `2G`), so that a lane can run the suite with the heap it needs:
  `./gradlew :fabric:runGameTest -PgameTestHeap=3G`. The CI runner with 1.75 GB still cannot run
  the suite; that is the open item of the memory lane, not of this one.
* **The three failures of the leader were not reproduced in the whole suite** in this worktree: five
  whole-suite runs on a clean world were green before the fix. The interference is the same in both
  configurations — the focused run of §5.1 is the same set of scenarios in one batch — but the
  whole suite may hide it behind the order of its batches. The fix removes the mechanism in both.
* **The world generation scenario `real25-chunk forest decoration produces actual Vishrooms and
  typed ManaPods without fixture insertion`** failed in one of the whole-suite runs of this lane
  (`the real25-chunk forest decoration produces actual Vishrooms and typed ManaPods without fixture
  insertion`) and passed in the others. It is a world generation scenario of another domain and it
  is not touched here.
* **The clean up of the vanilla reaches into the neighbour**: `GameTestInfo.succeed` removes the
  entities of an eleven block box around the structure of the finished scenario, and the fixture of
  the next scenario starts nine blocks away. The port's scenarios now survive that (they arm their
  golem again), but a scenario that reads the world of its neighbour right after the neighbour ends
  still sees the neighbour's clean up. A fixture that keeps its own area (the lane of this wave) is
  the safe answer.
* The `dropped(item)` read of `WaveLane` still counts the drops of a sixteen block area. It only
  reads, so it can over-count a drop of a neighbour; it was left alone because narrowing it could
  hide a legitimate drop of the lane's own mobs.

---

## 7. The commits

| commit | what |
|---|---|
| `fix(gametest): the clean up of the shared lane stays inside the lane` | the sweep of the lane is five blocks, not sixteen |
| `fix(gametest): the golem scenarios take their ring of chunks back` | `GolemChunks.refresh` and its three callers |
| `fix(gametest): the golem scenarios arm their golem again` | the re-arm of the item-match, the colours and the fetter scenario, and the first living update of the fresh golem |
| `test(gametest): the isolation rules of the golem scenarios` | the JUnit guard and the heap property of the GameTest server |
| `docs(isolate): the bisect, the mechanism and the fix` | this report |
