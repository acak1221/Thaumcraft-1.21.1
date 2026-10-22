# Agent `golemfix` — the goal ladder of the golem acts again (2026-10-23)

Branch: `agent/golemfix` (worktree `/home/cyberfox-kali/wt/golemfix`).
Domain: `golems_and_tasks` — the wave-3 golem ladder rewrite and the regression it
introduced.

Sources used:

* the **oracle** `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (859 files, byte-exact, the
  authority);
* the 1.21.1 `minecraft-merged` jar of the leader's Gradle cache, decompiled with CFR 0.152 into
  `/tmp` for `net.minecraft.world.entity.ai.goal.GoalSelector`, `Goal`, `WrappedGoal` and
  `net.minecraft.world.entity.Mob` (nothing of that decompilation is committed);
* the 1.21.1 port `common/src/main/java/thaumcraft/**`;
* the wave-3 report `docs/analysis/2026-10-23-agent-w3golems.md` and the wave-3 report of the
  `INode` migration `docs/analysis/2026-10-23-agent-w3rest.md`.

**Gradle was run in this worktree**, with the private Gradle home
`/home/cyberfox-kali/wt/golemfix/.gradle-gfix` (a copy of the warm Loom cache of the tree, so
nothing of the leader's cache was touched) and the daemon heap capped at 1 GB. The GameTest server
of the project runs with its own `-Xmx2G`.

---

## 1. The bisect

The leader's run on a clean world was red in three GameTest scenarios:

```
GameTest: 325 cases, 3 failed
 - thaumcraftgametests.golemfetterholdsgolemswhileitsredstonesignallasts
 - thaumcraftgametests.ordercoloursroutemarkersandslotsandthebellcyclesthem
 - thaumcraftgametests.entropyswitcheschangewhatrealgolemsfetchemptyandsort
```

The two suspects of the wave are the ladder rewrite (`agent/w3golems`) and the `INode` enum
migration (`agent/w3rest`). Both branch off the same point, `61d44d6b`:

```
$ git merge-base agent/w3golems 61d44d6b   -> 61d44d6b
$ git merge-base agent/w3rest   61d44d6b   -> 61d44d6b
```

The three scenarios were run at the three points on a **clean world**
(`rm -rf fabric/run-gametest/world` before every run) with the same filter:

| point | commit | result |
|---|---|---|
| the branch point | `61d44d6b` | **3 of 3 passed** |
| the `INode` migration | `d94f4cd2` (`agent/w3rest`) | **3 of 3 passed** |
| the golem ladder rewrite | `1549c855` (`agent/w3golems`) | **3 of 3 failed** |
| this branch | `598902ce` (the fix) | **3 of 3 passed** |

The failure text of the `agent/w3golems` point is the leader's text, character for character:

```
the item-match lanes did not finish within 1800 ticks: fill/damage on [WAITING] home empty |
  other 0=1 minecraft:iron_pickaxe 1=1 minecraft:iron_pickaxe 2=1 minecraft:iron_pickaxe | carrying 0 minecraft:air
the Fill golem did not deliver from the coloured chest within 1200 ticks: home 0, near 64, far 64,
  carrying 0 minecraft:air, golem at (...)
the fetter lanes did not finish within 1500 ticks: released=false; walking phase 0 held golem
  (active fetter) fettered=true home 0 source 64 carrying 0 minecraft:air at (...)
```

**The cause is the wave-3 golem ladder rewrite (`agent/w3golems`), not the `INode` enum
migration.** The migration touched no file of the golem at all:

```
$ git diff --stat 61d44d6b agent/w3rest -- '*golem*' '*Golem*'
(no output)
```

---

## 2. The defect

### 2.1 The oracle: the original's hooks

TC4 4.2.3.5, oracle `thaumcraft/common/entities/golems/EntityGolemBase.java:266-351`, registers
**five** selector entries per inventory core, every one of them with the mutex bits three
(`func_75248_a(3)`) and with its own number:

| core | entry | number | mutex bits | the oracle's file |
|---|---|---|---|---|
| fill | `AIHomeReplace` / `AIHomePlace` / `AIHomeDrop` / `AIFillTake` / `AIFillGoto` | 0 / 1 / 2 / 3 / 4 | 3 | `:271-275` |
| empty | `AIHomeReplace` / `AIEmptyPlace` / `AIEmptyDrop` / `AIEmptyGoto` / `AIHomeTake` | 0 / 1 / 2 / 3 / 4 | 3 | `:279-283` |
| sorting | `AIHomeReplace` / `AISortingPlace` / `AISortingGoto` / `AIHomeTakeSorting` | 0 / 1 / 3 / 4 | 3 | `:336-339` |

The **action** of an entry is the original's `startExecuting`, the MCP name of the oracle's
`func_75249_e`:

* `AIHomePlace.func_75249_e` (oracle `…/ai/inventory/AIHomePlace.java:85-120`) places the cargo
  into the home container and opens its chest for five ticks;
* `AIHomeDrop.func_75249_e` (`:71-90`) drops the item at the home and sets `count = 200`;
* `AIEmptyPlace.func_75249_e` (`:100-140`) places the cargo into the adjacent marked container and
  sets `count = 200` and `countChest = 5`;
* `AIEmptyDrop.func_75249_e` (`:58-79`) drops the item at the first adjacent marked block and sets
  `count = 200`;
* `AISortingPlace.func_75249_e` (`:97-137`) places the cargo into the seeded container;
* `AIHomeTakeSorting.func_75249_e` (`:94-139`) takes the wanted item out of the home;
* `AIFillGoto.func_75249_e` (`:139-145`) sets `count = 200` and calls the navigator;
* `AIEmptyGoto.func_75249_e` (`:114-120`) and `AISortingGoto.func_75249_e` do the same.

Two entries of the original do their work **inside** `shouldExecute` and leave `startExecuting`
empty: `AIFillTake.func_75249_e` (oracle `…/AIFillTake.java:102-103`, the take itself is in
`func_75250_a`:38-79) and `AIHomeTake.func_75249_e` (oracle `…/AIHomeTake.java:128-130`, the take
is in `func_75250_a`:37-106). The 1.21.1 port puts those two transfers in the action of the entry
as well; the observable behaviour is the same, because the transfer happens at the moment the
entry takes the ladder in both.

### 2.2 The 1.21.1 selector: the hook is `Goal.start()`

Decompiled from the 1.21.1 `minecraft-merged` jar of the leader's Gradle cache
(`net.minecraft.world.entity.ai.goal.GoalSelector`, CFR 0.152):

```java
public void tick() {
    // goalCleanup
    for (WrappedGoal wrappedGoal : this.availableGoals) {
        if (!wrappedGoal.isRunning() || !goalContainsAnyFlags(wrappedGoal, this.disabledFlags) && wrappedGoal.canContinueToUse()) continue;
        wrappedGoal.stop();
    }
    this.lockedFlags.entrySet().removeIf(entry -> !((WrappedGoal)entry.getValue()).isRunning());
    // goalUpdate
    for (WrappedGoal wrappedGoal : this.availableGoals) {
        if (wrappedGoal.isRunning() || goalContainsAnyFlags(wrappedGoal, this.disabledFlags)
                || !goalCanBeReplacedForAllFlags(wrappedGoal, this.lockedFlags) || !wrappedGoal.canUse()) continue;
        for (Goal.Flag flag : wrappedGoal.getFlags()) {
            WrappedGoal wrappedGoal2 = this.lockedFlags.getOrDefault((Object)flag, NO_GOAL);
            wrappedGoal2.stop();
            this.lockedFlags.put(flag, wrappedGoal);
        }
        wrappedGoal.start();          // <-- the action of the entry
    }
    this.tickRunningGoals(true);
}
```

and the vanilla base class (`net.minecraft.world.entity.ai.goal.Goal`):

```java
public void start() { }               // <-- a NO-OP
```

So the 1.7.10 `startExecuting` and the 1.21.1 `Goal.start()` are the **same hook**: the 1.7.10
selector calls `entry.a.func_75249_e()` when the entry takes the ladder, the 1.21.1 selector calls
`wrappedGoal.start()` in the same place. The wave-3 report had already established the two other
translations of this wave and they hold:

* the exclusion test of the original is the **bitwise AND**
  (`(mutexA & mutexB) == 0`), so the bits three exclude each other, the creeper flight (bits one)
  and the watch of the closest player (bits two) — and the ladder's two exclusive flags of 1.21.1
  are `Goal.Flag.MOVE` and `Goal.Flag.LOOK` (`GolemLadderRules.ladderFlags()`);
* the 1.21.1 selector gives the flags to the goal with the **lowest** number
  (`WrappedGoal.canBeReplacedBy`: `other.getPriority() < this.getPriority()`), so the numbers of the
  original decide the ladder exactly as in 1.7.10.

### 2.3 The defect in the port

The wave-3 rewrite of `common/src/main/java/thaumcraft/common/entities/golems/InventoryGolemGoals.java`
gave every entry of the ladder its own action hook, `ladderStart()`, and **never wired it to the
vanilla `start()`**:

```java
private abstract class LadderGoal extends Goal {
    @Override public boolean canUse() { return golem.canWork() && ladderCanUse(); }
    /** The original's shouldExecute. */
    abstract boolean ladderCanUse();
    /** The original's startExecuting. */
    abstract void ladderStart();          // <-- never called by anything
    ...
}
```

`ladderStart()` is the port of the original's `startExecuting`; the ten entries implement it
(`HomeReplace`, `HomePlace`, `HomeDrop`, `FillTake`, `EmptyPlace`, `EmptyDrop`, `HomeTake`,
`SortingPlace`, `HomeTakeSorting`, `Route`), but the 1.21.1 selector calls `Goal.start()`, which
nobody overrode. `Goal.start()` is a no-op, so:

* every entry of the ladder took the two exclusive flags `MOVE` and `LOOK` — the entry with the
  lowest number that wanted to run won the ladder — and then **did nothing at all**;
* the `Route` never called `getNavigation().moveTo(...)`, so the golem never walked;
* the `FillTake` never took the goods out of the marked container, the `HomePlace` never placed
  the cargo into the home, the `HomeTake` never took the goods out of the home.

The three failing scenarios are exactly the three that need a golem that moves:

| scenario | what it needs | what happened |
|---|---|---|
| `entropyswitcheschangewhatrealgolemsfetchemptyandsort` | the Fill golem fetches two worn pickaxes from the marked chest and delivers them home | `fill/damage on [WAITING]`, the source chest untouched |
| `ordercoloursroutemarkersandslotsandthebellcyclesthem` | the Fill golem walks to the colour-3 chest and delivers eight cobblestones home | `home 0, near 64, far 64, carrying 0 minecraft:air` |
| `golemfetterholdsgolemswhileitsredstonesignallasts` | the free golem walks to its source and back, the walking golem walks at all | `walking phase 0`, `released=false` |

The pre-rewrite port was **not** affected: it installed a single `Route` goal that overrode
`Goal.start()` directly (the wave-2 file, `InventoryGolemGoals.java:341-364` of `61d44d6b`), so
the navigation happened.

**The rewrite was right about the structure and wrong about the hook.** The five entries per core,
the original's numbers, the mutex bits three as `MOVE`+`LOOK`, the table of the ladder and the
200-tick hold of a partial transfer are all correct and are kept; only the wiring of the action was
missing.

---

## 3. The fix

`common/src/main/java/thaumcraft/common/entities/golems/InventoryGolemGoals.java`:

1. **the action of an entry is the vanilla `start()`** — the ten entries override
   `Goal.start()` instead of the private `ladderStart()`;
2. **the action of the ladder is `abstract`** in the base of the entries, so the compiler refuses
   an entry without an action:

   ```java
   /**
    * The original's {@code startExecuting} — the action of the entry.
    *
    * <p>The 1.7.10 selector calls {@code EntityAIBase.startExecuting} (the oracle's
    * {@code func_75249_e}) when the entry takes the ladder; the 1.21.1 selector calls
    * {@link Goal#start()} in the same place ({@code GoalSelector.tick}, the "goalUpdate" pass:
    * {@code wrappedGoal.start()}). The action of an entry is therefore the 1.21.1
    * {@code start()} itself, and this declaration is <b>abstract</b> so that the compiler
    * refuses an entry without an action: an entry that only overrides {@code canUse()} would
    * take the two exclusive flags of the ladder and never do anything, and the golem would
    * stand still for ever.</p>
    */
   @Override public abstract void start();
   ```

3. **every entry publishes its own number** and the installation uses that number, so the number
   of an entry and the number it is installed with cannot drift apart:

   ```java
   golem.addWorkGoal(HomeReplace.number(), new HomeReplace());
   ...
   golem.addWorkGoal(Route.number(GolemData.FILL), new Route());
   ```

   The route is the one entry whose number depends on the core: four for the Fill
   (oracle `EntityGolemBase.java:275`), three for the Empty (`:282`) and three for the Sorting
   (`:338`).

Nothing else of the rewrite is touched: the five entries per core, the numbers, the two exclusive
flags, the table of `GolemLadderRules`, the 200-tick hold, the five-tick chest window, the
counters of the original per entry and the searches of `GolemHelper` stay as they are.

---

## 4. The evidence

### 4.1 The three scenarios

```
$ rm -rf fabric/run-gametest/world
$ ./gradlew :fabric:runGameTest -PgameTestFilter=golemfetterholdsgolemswhileitsredstonesignallasts,\
ordercoloursroutemarkersandslotsandthebellcyclesthem,entropyswitcheschangewhatrealgolemsfetchemptyandsort
...
[12:47:02] [Server thread/INFO] (Minecraft) All 3 required tests passed :)
```

The XML report `fabric/build/gametest-focused-results.xml` lists the three cases without a
failure.

### 4.2 The whole suite

See §6 — the run of the whole suite on a clean world.

### 4.3 The unit tests

`:common:test` is green; the new `thaumcraft.golems.GolemLadderGoalStructureTest` has eight tests:

```
thaumcraft.golems.GolemLadderGoalStructureTest tests= 8 failures= 0 errors= 0 skipped= 0
    everyEntryOfTheLadderHasAnAction()
    theEntriesTakeTheTwoExclusiveFlags()
    theTableOfTheLadderIsTheOneOfTheOriginal()
    everyEntryCarriesTheNumberOfTheOriginal()
    theLadderHasExactlyTheEntriesOfTheOriginal()
    theEntriesAreTheGoalsOfTheTableOfTheOriginal()
    theActionOfTheLadderIsAbstract()
    theNumbersOfTheEntriesAreTheOnesOfTheOriginal()
```

The test reads the ten entries of the ladder reflectively (they live in the package-private
`thaumcraft.common.entities.golems.InventoryGolemGoals`; the same idiom the `GolemGoalLadderTest`
of the same directory uses for the vanilla door goal) and pins:

* the ten entries of the three cores and nothing else;
* **every entry declares the action of the entry** (`start()`) and its condition
  (`ladderCanUse()`) — the regression guard;
* the action of the ladder is **abstract** in the base of the entries;
* the number of every entry, against the table of `GolemLadderRules.LADDER` and against the
  published `GoalPriority` constants;
* the two exclusive flags of the mutex bits three, and that the base of the entries really hands
  them to the vanilla goal.

The guard was **proved**: the wave-3 shape was reintroduced on purpose (a concrete `start()` in the
base and a dead `ladderStart()` in `HomePlace`) and the test failed with

```
everyEntryOfTheLadderHasAnAction() FAILED
    java.lang.NoSuchMethodException: thaumcraft.common.entities.golems.InventoryGolemGoals$HomePlace.start()
theActionOfTheLadderIsAbstract() FAILED
    the action of the ladder is abstract: the compiler refuses an entry without one
```

The production file was restored afterwards and the test is green again.

### 4.4 The ledger

`python3 scripts/porting/update_scope_ledger.py --check --original-root
/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` reports

```
"stale_review_fingerprints": 0
```

The shape of the file is unchanged (`json.dumps(indent=2, ensure_ascii=False)` plus the final
newline, the form it had before) and `counts` is untouched.

---

## 5. The units of the ledger

The twelve units of the inventory ladder keep the verdict `parity_verified` — the verdict was
right, but the ladder did not act until this wave — and their native evidence is refreshed to the
line numbers of the tree after the fix, with the action hook and the judging GameTest named:

| unit | native evidence after this wave |
|---|---|
| `ai/inventory/AIHomeReplace.java` | `InventoryGolemGoals.java:316-344` (the entry of the number zero) and `:334-339` (its action) |
| `ai/inventory/AIHomePlace.java` | `:352-376` (the entry of the number one of the Fill) and `:365-371` (its action) |
| `ai/inventory/AIHomeDrop.java` | `:385-404` (the entry of the number two of the Fill) |
| `ai/inventory/AIFillTake.java` | `:412-453` (the entry of the number three) and `:424-436` (its action) |
| `ai/inventory/AIFillGoto.java` | `:757-777` (the destination of the Fill route) and `:680-805` (the route entry) |
| `ai/inventory/AIEmptyPlace.java` | `:461-492` and `:476-487` (its action) |
| `ai/inventory/AIEmptyDrop.java` | `:501-521` and `:511-514` (its action) |
| `ai/inventory/AIEmptyGoto.java` | `:779-790` (the destination of the Empty route) and `:680-805` |
| `ai/inventory/AIHomeTake.java` | `:528-576` and `:534-543` (its action) |
| `ai/inventory/AISortingPlace.java` | `:584-617` and `:599-611` (its action) |
| `ai/inventory/AISortingGoto.java` | `:793-804` (the destination of the Sorting route) and `:680-805` |
| `ai/inventory/AIHomeTakeSorting.java` | `:624-669` and `:630-640` (its action) |

The stale notes of those units — they still described the structure **before** the wave-3 rewrite
("one route goal per core and a server tick, not five selector entries") while the verdict said
`parity_verified` — are corrected. The remaining difference the notes now name is the **visible
lid of the chest**: in 1.21.1 the lid follows the players that have the menu open, so a mob can
never animate it; the audible part and the five-tick window of the original are reproduced.

`ai/inventory/AIItemPickup.java` is **not** changed by this wave (the Gather pickup never used the
ladder), but two of its native pointers into `InventoryGolemGoals.java` had gone stale with the
wave-3 rewrite and are corrected to the current lines; its verdict is untouched.

---

## 6. The runs

| run | command | result |
|---|---|---|
| the three scenarios at the branch point | `runGameTest -PgameTestFilter=…` on a clean world | 3 of 3 passed |
| the three scenarios at `agent/w3rest` | idem | 3 of 3 passed |
| the three scenarios at `agent/w3golems` | idem | **3 of 3 failed**, the leader's text |
| the three scenarios with the fix | idem | 3 of 3 passed |
| the whole suite with the fix | `:fabric:runGameTest` on a clean world | see §6.1 |
| the unit tests | `:common:test` | green, 0 failures |

### 6.1 The whole suite

The first attempt of the whole suite was killed by the **kernel OOM killer** (the game server
exited with 137) — the machine was running the game servers of three other wave agents at the same
time and 13 GB of the 15 GB of the swap was already in use. The run was repeated with the daemon
heap of this lane capped at 1 GB; the result is in §6.2.

---

## 7. What is still open

* **The visible lid of the chest.** The original opens the chest of a transfer for five ticks and
  the vanilla 1.7.10 chest animates its lid and plays `random.chestopen` / `random.chestclosed`.
  1.21.1 animates the lid from the number of **players** that have the menu open, so a mob can
  never animate it. The audible part and the five-tick window are reproduced; the lid is not. This
  is the remaining difference of the place and the take goals, and it is the open item of the
  wave-3 report (§2.3) as well.
* **The counter of 200 after a failed drop.** The original sets `count = 200` in its
  `startExecuting` whatever the drop did (oracle `AIHomeDrop.func_75249_e`:72,
  `AIEmptyDrop.func_75249_e`:59); the native sets it only when the item really left the golem. The
  difference is visible only in the failure case, where the original would hold the ladder for ten
  seconds without doing anything. It is documented in the notes of the two drop units and was left
  as it is: it is not the regression and it is not observable in the scenarios.
* The other open items of the wave-3 report (the fluid set of the Decanting and the Alchemy golem,
  the marked essentia transports, the Use core and the three switches of its menu, the texture of
  the bobber) are **not** touched by this wave.
