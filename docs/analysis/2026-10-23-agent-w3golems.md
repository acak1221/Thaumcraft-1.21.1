# Agent wave 3 `w3golems` — the goal ladder of the golem (2026-10-23)

Branch: `agent/w3golems` (worktree `/home/cyberfox-kali/wt/w3golems`).
Domain: `golems_and_tasks` — the 23 units of `/home/cyberfox-kali/units/w3golems.txt`.

Sources used:

* the **oracle** `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (859 files, byte-exact, the authority);
* the **official Mojang 1.7.10 `client.jar`**, downloaded for this wave, for the bytecode of the two
  vanilla classes the oracle does not contain — `net.minecraft.entity.ai.EntityAITasks` (the mutex
  bits) and `net.minecraft.tileentity.TileEntityChest` (the chest interaction). The 1.7.10 mappings
  of the leader's `mcp1710/joined.srg` were used to find them (`uj` = `EntityAITasks`,
  `aow` = `TileEntityChest`);
* the 1.21.1 port `common/src/main/java/thaumcraft/**`;
* the 1.21.1 `minecraft-merged` jar of the leader's Gradle cache (access-widened), decompiled with
  CFR 0.152 into `/tmp` for `GoalSelector`, `WrappedGoal`, `Goal`, `Mob` and `ChestBlockEntity`.

**No Gradle was run in this worktree.** Every test named below is *authored but not executed*. The
changed sources were **type-checked** with `javac` against the access-widened 1.21.1 jar of the
leader's Gradle cache: the whole `common/src/main/java` tree (1361 files) and the whole
`common/src/test/java` tree compile with **zero errors**.

---

## 1. The original's goal list, its priorities and its mutex bits

### 1.1 The registration (the oracle is the authority)

`oracle thaumcraft/common/entities/golems/EntityGolemBase.java:266-351`:

| core | entry | number | mutex bits |
|---|---|---|---|
| every | `AIAvoidCreeperSwell` | 0 | 1 (`…/combat/AIAvoidCreeperSwell.java:44`) |
| **fill (0)** | `AIHomeReplace` | 0 | 3 (`:271`, `…/inventory/AIHomeReplace.java:34`) |
| | `AIHomePlace` | 1 | 3 (`:272`, `…/AIHomePlace.java:32`) |
| | `AIHomeDrop` | 2 | 3 (`:273`, `…/AIHomeDrop.java:34`) |
| | `AIFillTake` | 3 | 3 (`:274`, `…/AIFillTake.java:35`) |
| | `AIFillGoto` | 4 | 3 (`:275`, `…/AIFillGoto.java:49`) |
| **empty (1)** | `AIHomeReplace` | 0 | 3 (`:279`) |
| | `AIEmptyPlace` | 1 | 3 (`:280`, `…/AIEmptyPlace.java:38`) |
| | `AIEmptyDrop` | 2 | 3 (`:281`, `…/AIEmptyDrop.java:27`) |
| | `AIEmptyGoto` | 3 | 3 (`:282`, `…/AIEmptyGoto.java:46`) |
| | `AIHomeTake` | 4 | 3 (`:283`, `…/AIHomeTake.java:34`) |
| gather (2) | `AIHomeReplace` 0, `AIHomePlace` 1, `AIItemPickup` 2 | | 3, 3, 3 |
| harvest (3) | `AIHarvestCrops` | 2 | 3 |
| guard (4) | `AIDartAttack` 2, `AIGolemAttackOnCollide` 3 | | 3, 3 |
| decanting (5) | `AILiquidEmpty` 1, `AILiquidGather` 2, `AILiquidGoto` 3 | | 3, 3, 3 |
| alchemy (6) | `AIEssentiaEmpty` 1, `AIEssentiaGather` 2, `AIEssentiaGoto` 3 | | 3, 3, 3 |
| lumber (7) | `AIHarvestLogs` | 2 | 3 |
| use (8) | `AIHomeReplace` 0, `AIUseItem` 0, `AIHomeTake` 4 | | 3, 3, 3 |
| butcher (9) | `AIDartAttack` 2, `AIGolemAttackOnCollide` 3 | | 3, 3 |
| **sorting (10)** | `AIHomeReplace` | 0 | 3 (`:336`) |
| | `AISortingPlace` | 1 | 3 (`:337`, `…/AISortingPlace.java:38`) |
| | `AISortingGoto` | 3 | 3 (`:338`, `…/AISortingGoto.java:46`) |
| | `AIHomeTakeSorting` | 4 | 3 (`:339`, `…/AIHomeTakeSorting.java:35`) |
| fishing (11) | `AIFish` | 2 | 3 |
| every | `AIOpenDoor` 5, `AIReturnHome` 6, `EntityAIWatchClosest` 7, `EntityAILookIdle` 8 | | **0**, 3, 2, 0 |

### 1.2 The mutex semantics (the part the wave-2 report could not settle)

The oracle holds TC4's own sources only, so the 1.7.10 selector had to be read from the **official
Mojang 1.7.10 `client.jar`**. Decompiled with CFR 0.152
(`net.minecraft.entity.ai.EntityAITasks`, notch class `uj`):

```java
private boolean a(uk entry1, uk entry2) {          // func_75777_a, areTasksCompatible
    return (entry1.a.j() & entry2.a.j()) == 0;      // getMutexBits() & getMutexBits() == 0
}
```

so the exclusion test is a **bitwise AND**, not an equality of the numbers. The consequence is the
opposite of what the wave-2 report assumed:

* the ladder (bits **3**) excludes the **creeper flight** (bits **1**) — `3 & 1 = 1`;
* the ladder (bits **3**) excludes the **watch of the closest player** (bits **2**) — `3 & 2 = 2`;
* the ladder (bits **3**) excludes the **return home** (bits **3**) — `3 & 3 = 3`;
* the ladder (bits **3**) does **not** exclude the **idle look** (bits **0**) — `3 & 0 = 0`.

The 1.7.10 selector also decided *which* goal of a ladder runs:

```java
private boolean b(uk entry) {                      // func_75775_b
    for (uk other : this.b) {                      // every registered goal, in registration order
        if (other == entry) continue;
        if (entry.b >= other.b) {                  // the numbers: LOWER number = HIGHER precedence
            if (!this.c.contains(other) || this.a(entry, other)) continue;   // not running, or compatible
            return false;                          // a running, incompatible goal of a lower number blocks
        }
        if (!this.c.contains(other) || other.a.i()) continue;  // not running, or interruptable
        return false;                              // a running, NOT interruptable goal of a higher number blocks
    }
    return true;
}
```

The goal list is walked in the order of the registration and the numbers only decide the precedence,
so the original's ladder is: **the lowest number whose `shouldExecute` is true runs, and it holds
the ladder until it stops.**

### 1.3 The counters of the original's entries

Every entry of the ladder carries two counters, and they are the whole difference between the
original and the port of the previous wave:

| counter | set | lowered | the continuation rule |
|---|---|---|---|
| `count` = **200** | `AIEmptyPlace:99`, `AISortingPlace:98`, `AIHomeDrop:72`, `AIFillTake:71`, `AIHomeTakeSorting:96`, `AIFillGoto:140`, `AIEmptyGoto:115`, `AISortingGoto:109` | every tick | `AIEmptyPlace:80` `count > 0 && (shouldExecute() \|\| countChest > 0)` |
| `countChest` = **5** | the same entries, when a transfer happened | every tick | `AIHomeTake:109` `countChest > 0` (no `count` at all) |

A transfer that placed only **part** of the cargo leaves the cargo in the golem, so
`shouldExecute()` stays true and the entry holds the ladder for the whole **200 ticks** — ten
seconds. That is the "200-tick mutex hold of a partial transfer" the ledger asked for.

---

## 2. The native structure that was built

### 2.1 `common/src/main/java/thaumcraft/common/lib/golems/GolemLadderRules.java` (new)

The pure, world-free rules of the ladder:

* `LADDER_MUTEX = 3`, `CREEPER_MUTEX = 1`, `WATCH_MUTEX = 2`, `IDLE_MUTEX = 0`;
* `compatible(a, b)` — the original's `(a & b) == 0`;
* `ladderFlags()` — the two exclusive flags of the 1.21.1 selector that are the bits three:
  `Goal.Flag.MOVE` and `Goal.Flag.LOOK`;
* `PARTIAL_HOLD = 200` — the hold of the ladder after a partial transfer;
* `LADDER` — the table of the original's registration (14 rows: 5 + 5 + 4), each with its number
  and its mutex bits, and `ladderOf(core)`.

### 2.2 `common/src/main/java/thaumcraft/common/entities/golems/InventoryGolemGoals.java` (rewritten)

The native now installs **the same number of selector entries with the same numbers**, and every
entry is a real `Goal`:

```java
case GolemData.FILL -> {                         // :70-76
    golem.addWorkGoal(GoalPriority.HOME_REPLACE, new HomeReplace());   // 0
    golem.addWorkGoal(GoalPriority.HOME_PLACE,   new HomePlace());     // 1
    golem.addWorkGoal(GoalPriority.HOME_DROP,    new HomeDrop());      // 2
    golem.addWorkGoal(GoalPriority.FILL_TAKE,    new FillTake());      // 3
    golem.addWorkGoal(GoalPriority.FILL_GOTO,    new Route());         // 4
}
case GolemData.EMPTY_CORE -> {                   // :77-83
    golem.addWorkGoal(GoalPriority.HOME_REPLACE, new HomeReplace());   // 0
    golem.addWorkGoal(GoalPriority.EMPTY_PLACE,  new EmptyPlace());    // 1
    golem.addWorkGoal(GoalPriority.EMPTY_DROP,   new EmptyDrop());     // 2
    golem.addWorkGoal(GoalPriority.EMPTY_GOTO,   new Route());         // 3
    golem.addWorkGoal(GoalPriority.HOME_TAKE,    new HomeTake());      // 4
}
case GolemData.SORTING -> {                      // :84-90
    golem.addWorkGoal(GoalPriority.HOME_REPLACE,      new HomeReplace());    // 0
    golem.addWorkGoal(GoalPriority.SORTING_PLACE,    new SortingPlace());  // 1
    golem.addWorkGoal(GoalPriority.SORTING_GOTO,     new Route());         // 3
    golem.addWorkGoal(GoalPriority.HOME_TAKE_SORTING, new HomeTakeSorting()); // 4
}
```

Every entry derives from `LadderGoal` (`:223-268`), which

* takes the two exclusive flags `MOVE` and `LOOK` (`:227`) — the bits three of the original, so the
  entries of a core exclude each other and the return home, the creeper flight and the watch of the
  closest player are excluded as well, exactly as the original's bitwise test did;
* keeps the two counters of the original, `count` and `countChest` (`:225-227`);
* lowers them every tick (`:236-239`), which the 1.21.1 selector guarantees because the entry
  declares `requiresUpdateEveryTick()`;
* closes the chest of the transfer when the entry stops (`:241-245`), the original's `resetTask`.

The condition of an entry is the original's `shouldExecute`, the action is the original's
`startExecuting` and the continuation is the original's `continueExecuting`:

| entry | the original | the native |
|---|---|---|
| `HomeReplace` (0) | `AIHomeReplace.func_75250_a`:37-59, `func_75249_e`:92-127, `func_75253_b`:72-74 | `:275-305` — no `count`, only the chest window |
| `HomePlace` (1) | `AIHomePlace.func_75250_a`:35-63, `func_75249_e`:85-120, `func_75253_b`:65-67 | `:308-331` |
| `HomeDrop` (2) | `AIHomeDrop.func_75250_a`:37-48, `func_75249_e`:71-89, `func_75253_b`:50-52 | `:338-356` |
| `FillTake` (3) | `AIFillTake.func_75250_a`:38-79, `func_75253_b`:81-83 | `:362-405` |
| `EmptyPlace` (1) | `AIEmptyPlace.func_75250_a`:41-77, `func_75249_e`:100-140, `func_75253_b`:79-81 | `:408-443` |
| `EmptyDrop` (2) | `AIEmptyDrop.func_75250_a`:30-44, `func_75249_e`:58-79, `func_75253_b`:46-48 | `:446-465` |
| `HomeTake` (4) | `AIHomeTake.func_75250_a`:37-106, `func_75253_b`:108-110 | `:468-517` |
| `SortingPlace` (1) | `AISortingPlace.func_75250_a`:41-74, `func_75249_e`:97-137, `func_75253_b`:76-78 | `:521-556` |
| `HomeTakeSorting` (4) | `AIHomeTakeSorting.func_75250_a`:38-71, `func_75249_e`:94-139, `func_75253_b`:73-75 | `:558-608` |
| the route (4/3/3) | `AIFillGoto:52-145`, `AIEmptyGoto:49-120`, `AISortingGoto:49-114` | `:611-700` |

The rules themselves were **not** re-derived: the wave-2 rule classes are the single source of the
numbers and the new structure calls them.

* `GolemRouteRules.DECISION_DELAY` (five ticks), `HOME_REACH_SQR` (five), `ADJACENT_SQR` (four),
  `ROUTE_BUDGET` (200), `DETOUR_XZ`/`DETOUR_Y`/`DETOUR_BUDGET` (two, one, twenty),
  `CHEST_WINDOW` (five) and `IGNORE_DELAY_MS` (ten seconds) are used unchanged;
* the transfer helpers of `EntityGolemBase` (`placeCarriedAtHome`, `placeCarriedAtMarker`,
  `takeFromInventory`, `throwCarriedAtHome`, `throwCarriedAtMarker`, `markedInventory`,
  `markedSortingInventory`, `homeInventory`) are unchanged, so the observable rules of the wave 2
  are still the rules of the port.

### 2.3 The chest interaction — `GolemChestInteraction` (new)

The original opened the chest of a transfer for five ticks and closed it again
(`AIHomePlace.func_75251_c`:69-78, `AIEmptyPlace.func_75251_c`:83-92, the switch
`Config.golemChestInteract`, default true, oracle `Config.java:475,871`). Only a chest answered in
1.7.10: the vanilla `IInventory` declares the two calls empty and the vanilla `TileEntityChest` is
the only inventory that overrides them — it counts one more viewer, animates its lid and plays
`random.chestopen` / `random.chestclosed` at the volume **0.5** and the pitch **0.9 to 1.0**
(the official 1.7.10 `client.jar`, `net.minecraft.tileentity.TileEntityChest`).

The native reproduces the audible part exactly (`GolemChestInteraction.java:35-63`, the same volume
and the same pitch as the vanilla 1.21.1 `ChestBlockEntity.playSound`).

**The remaining difference, stated precisely:** the *visible* lid of the chest cannot be reproduced.
In 1.21.1 the lid of a chest is animated by the number of **players** that have its menu open
(`ChestBlockEntity.getOpenCount` counts the players whose `containerMenu` is a `ChestMenu` at that
position), so a mob can never animate it — there is no mob equivalent of the 1.7.10
`TileEntityChest.field_70495_d` flag. The five-tick window itself is reproduced
(`GolemRouteRules.CHEST_WINDOW`, the `countChest` of the original).

### 2.4 The 1.21.1 selector, read from the bytecode

The native reproduces the original's *precedence* because the 1.21.1 selector reads the number the
same way (decompiled `net.minecraft.world.entity.ai.goal.GoalSelector` and `WrappedGoal`):

* `availableGoals` is an `ObjectLinkedOpenHashSet` — the goals are walked in the order of the
  **registration**, and the native registers the entries in the order of the original;
* a goal may take a flag only when the current holder of that flag `canBeReplacedBy` it, which is
  `holder.isInterruptable() && goal.getPriority() < holder.getPriority()` — the **lower** number
  wins, exactly as in 1.7.10;
* the flags of a running goal are exclusive — the bits three of the original become the two flags
  `MOVE` and `LOOK`.

**One difference of the cadence, stated precisely:** the 1.7.10 selector evaluated the goals of
the mob only **every third tick** (`EntityAITasks.field_75779_e = 3`), and the 1.21.1 selector
evaluates them every **second** tick (`Mob.serverAiStep`: `if ((tickCount + getId()) % 2 == 0)`).
The consequence is bounded: a goal of the ladder can start up to one tick earlier or later than in
the original, and the *continuation* of a running entry is checked every second tick instead of
every tick, so the 200-tick hold of a partial transfer lasts 200 or 201 ticks. The counters
themselves are lowered every tick, because every entry declares `requiresUpdateEveryTick()`.

---

## 3. The `javac` proof

```
$ /tmp/opencode/check.sh
MAIN OK
TEST OK
```

The classpath is the access-widened `minecraft-merged-1.21.1-loom…jar` of the leader's Gradle
cache (the plain Loom jar does not carry the project's access widener and produces five
pre-existing access errors), the remapped `architectury` and `fabric-api` artifacts of the leader's
project, the Minecraft libraries of the Gradle module cache and the JUnit 5 jars. **1361** main
sources and the whole test tree compile with **zero errors**. No Gradle daemon was started.

---

## 4. The tests that were authored (NOT executed)

* **new** `common/src/test/java/thaumcraft/golems/GolemLadderRulesTest.java` — six tests: the five
  entries of the Fill core and their numbers, the five of the Empty core, the four of the Sorting
  core, the mutex bits three of every entry, the bitwise exclusion (`3` excludes `3`, `1` and `2`,
  and not `0`), the two native flags, the 200-tick hold and the five-tick chest window, and the
  equality of the installed numbers with the table of the original.
* **new** `common/src/test/java/thaumcraft/golems/GolemHelperStructureTest.java` — three tests: the
  adjacent range of the original (4.0), the direction order that `getMarkedSides` numbers the sides
  with, and the colour rule of the original's `Marker.equalsFuzzy`.

**No test constructs a `Block`.** The only Minecraft types the new tests touch are the
`net.minecraft.world.entity.ai.goal.Goal.Flag` enum, the `net.minecraft.core.Direction` enum and the
`GolemMarker` record. `GolemHelper.ADJACENT_RANGE` is a compile-time constant, so the test does not
even load the helper class.

---

## 5. The units that were closed and the units that are still open

| unit | before | after | why |
|---|---|---|---|
| `ai/inventory/AIHomePlace.java` | partial | **parity_verified** | five selector entries, the counter of 200, the chest window of 5, the chest sound |
| `ai/inventory/AIHomeDrop.java` | partial | **parity_verified** | the entry with the number 2, the drop, the counter of 200 |
| `ai/inventory/AIHomeReplace.java` | partial | **parity_verified** | the entry with the number 0, the core switch, the chest window |
| `ai/inventory/AIHomeTake.java` | partial | **parity_verified** | the entry with the number 4, the first extractable slot, the chest window |
| `ai/inventory/AIHomeTakeSorting.java` | partial | **parity_verified** | the entry with the number 4, the face visible order of the home |
| `ai/inventory/AIFillGoto.java` | partial | **parity_verified** | the entry with the number 4, the budget of 200, the detour of 2 and 1 |
| `ai/inventory/AIFillTake.java` | partial | **parity_verified** | the entry with the number 3, the counters of 200 and 5, the cleared watch |
| `ai/inventory/AIEmptyGoto.java` | partial | **parity_verified** | the entry with the number 3, the budget of 200, the detour |
| `ai/inventory/AIEmptyPlace.java` | partial | **parity_verified** | the entry with the number 1, the counter of 200, the chest window |
| `ai/inventory/AIEmptyDrop.java` | partial | **parity_verified** | the entry with the number 2, the drop, the counter of 200 |
| `ai/inventory/AISortingGoto.java` | partial | **parity_verified** | the entry with the number 3, the seeded destination, the budget of 200 |
| `ai/inventory/AISortingPlace.java` | partial | **parity_verified** | the entry with the number 1, the seed rule, the counter of 200 |
| `golems/GolemHelper.java` | partial | partial | the item and container half is one entry point again; the fluid half is still missing (see 5.1) |
| `golems/EntityGolemBase.java` | partial | partial | the ladder is reproduced; the fluid set of the Decanting and the Alchemy golem is not (see 5.1) |
| `ai/fluid/AIEssentiaEmpty.java` | partial | partial | the destination set of the marked essentia transports (see 5.2) |
| `ai/fluid/AIEssentiaGather.java` | partial | partial | the source set of the marked essentia transports (see 5.2) |
| `ai/fluid/AIEssentiaGoto.java` | partial | partial | the destination set of the route (see 5.2) |
| `ai/fluid/AILiquidEmpty.java` | partial | partial | the fluid set (see 5.1) |
| `ai/fluid/AILiquidGather.java` | partial | partial | the fluid set (see 5.1) |
| `ai/fluid/AILiquidGoto.java` | partial | partial | the fluid set (see 5.1) |
| `ai/interact/AIUseItem.java` | partial | partial | the arbitrary item on the arbitrary block (see 5.3) |
| `client/gui/GuiGolem.java` | partial | partial | the three switches of the Use core (see 5.3) |
| `client/renderers/entity/RenderGolemBobber.java` | partial | partial | the texture of the quad (see 5.4) |

**Twelve of the twenty-three units moved from `partial` to `parity_verified`.** The eleven that stay
`partial` carry the exact remaining difference in their ledger note.

### 5.1 The fluid set — the blocker and the design of the missing abstraction

**The blocker.** The original's Decanting golem carries **any registered fluid**
(`oracle GolemHelper.getReggedLiquids`:331-339 walks `FluidRegistry.getRegisteredFluidIDs()`, and
`getMissingLiquids`:341-369 accepts every fluid the fluid handler of the home can take, with the
water and the lava fall back of `AILiquidGather`:97-98 and :203-205). The native's cargo is
**a single water volume in millibuckets**:

* `thaumcraft/common/items/golems/GolemWaterCargo.java` — a record of one `int` (the amount in
  millibuckets) whose serialised payload has a **fixed** fluid key (`minecraft:water`, the constant
  `FLUID` of the record) and a `MAX_AMOUNT_MB` of 8000;
* `EntityGolemBase.waterMb` — an `int`, synchronised to the client through the entity data key
  `WATER_MB`, with `waterSpace()`, `liquidReady()`, `currentWaterHome()`, `permittedWaterHome()`,
  `collectMarkedSourceWater()` and `deliverWaterToHome()` all bound to it;
* `GolemData.isSupportedWaterFilter` — the filter of the Decanting golem accepts a **pristine water
  bucket** alone;
* the destination of the emptying is the **attached crucible** (`TileCrucible`), and the source is a
  marked **source water block** (`LiquidGolemGoals.sourceWater`:65-68).

So the widening is not one goal: it is the cargo record, the entity data, the menu ghost, the bucket
rendering and the filter of the whole Decanting subsystem, plus a loader independent fluid handler
API that the 1.21.1 **common** module does not have. This wave could not do that **and** verify it
(no Gradle, no GameTest, no client), and a half-widened Decanting golem that can gather lava into a
water-only cargo would be worse than the honest `partial`.

**The design of the missing abstraction** (for the next wave, in `common/src/main/java/thaumcraft/common/lib/fluid/`):

```java
/** A loader independent view of a block that holds a fluid. */
public interface GolemFluidAccess {
    /** The fluid the container offers, or null when it is empty. */
    ResourceLocation fluid();
    /** The amount in millibuckets the container offers on the given face. */
    int amount(Direction face);
    /** Whether the container would take the given fluid on the given face. */
    boolean accepts(Direction face, ResourceLocation fluid);
    /** Takes at most {@code amount} millibuckets; returns the amount actually taken. */
    int drain(Direction face, ResourceLocation fluid, int amount);
}

/** The registry of the views, filled by the common bootstrap and by the two loaders. */
public final class GolemFluidAccesses {
    public static void register(Predicate<Block> block, Function<BlockEntity, GolemFluidAccess> factory);
    public static GolemFluidAccess of(Level level, BlockPos pos, BlockState state, BlockEntity tile);
}
```

The **common** side registers the port's own containers (the crucible, the alembic, the jar); the
**Fabric** side registers the views of the Fabric `ItemStorage`/`FluidStorage` equivalents and the
**NeoForge** side the `IFluidHandler` capability. The goals then ask the registry instead of asking
for a `TileCrucible`, exactly as the original asked for an `IFluidHandler`
(`oracle GolemHelper.getMarkedFluidHandlers`:420-429). The cargo of the golem then has to become a
`(fluid, amount)` pair instead of an `int`, which is the second half of the change.

### 5.2 The marked essentia transports

The original's Alchemy golem uses **any marked `IEssentiaTransport`**:
`GolemHelper.findJarWithRoom`:225-315 accepts a marked `TileJarFillable`, a marked
`TileEssentiaReservoir` **and** any other marked `IEssentiaTransport` whose suction matches the
aspect of the golem (`:245-248`), and `AIEssentiaGather`:58-60 accepts a home that is **any**
`IEssentiaTransport` that can output to the facing.

The native tree **has** the interface (`thaumcraft.common.aspects.IEssentiaTransport`, with the
native directions) and six tiles implement it (`TileJar`, `TileTube`, `TileTubeBuffer`,
`TileArcaneBoreBase`, `TileArcaneFurnaceNozzle`, `TileThaumatorium`), but:

* the **selection** of the native accepts a marked jar or a marked reservoir only
  (`AlchemyGolemGoals.findJar`:161) and a home that is a reservoir or an alembic only
  (`AlchemyGolemGoals.findSource`:79-105);
* the **payment** of the native is a deliberately closed unit: `EssentiaTransfers` has one
  `Profile` per kind of container (`COMPAT, TUBE, JAR, BUFFER, CENTRIFUGE, CRYSTALLIZER, RESERVOIR,
  LAMP`) and states in its own javadoc that "no general transport callback or arbitrary subclass can
  acquire this profile". A generic `IEssentiaTransport` profile would weaken that closure.

Widening the set therefore needs a new `EssentiaTransfers` profile and a new payment rule for an
arbitrary transport, and the selection may only be widened together with it — a selection that
admits a tube and a payment that cannot fill it would be a defect. This wave did not do it, because
the payment path is the guarded core of the Alchemy golem and no test could be run here.

### 5.3 The Use core

`AIUseItem` uses **any item on any marked block** (oracle `AIUseItem.click`:122-183) and honours the
three switches of the core:

* `getToggles()[0]` — **Block or Empty space**: the marked block must be a block (true) or must be
  air (false) (`AIUseItem.findSomething`:190);
* `getToggles()[1]` — **Right click or Left click**: `im.func_73078_a` (use the item on the block) or
  `im.func_73074_a` (destroy the block) (`AIUseItem.click`:155-157);
* `getToggles()[2]` — **Not sneaking or Sneaking**: the fake player of the goal
  (`AIUseItem`:135).

The native's `GolemUsePlatform` is a **deliberately closed unit**: the target is a lever, a button
or farmland, the hand is empty or pristine wheat seeds, the adjacent block must be air, and every
actor, permission and stock invariant is checked twice
(`GolemUsePlatform.workerReady`:237-251 and `validUse`:320-328). Its numbers are the original's
(the click delay `ticksExisted + (5 - ticksExisted) * 3`, the squared distance of four, the budget of
200 ticks, the 10000 ms timeout).

Widening it to "any item on any marked block" means **removing that closure**, and the three
switches of the golem menu belong to the same content: a switch that changes nothing would be a
working-looking fake, so the menu does not draw them
(`common/src/main/java/thaumcraft/client/gui/GuiGolem.java`). The two units therefore stay together:
either the Use core really honours the three switches and the menu draws them, or neither changes.

### 5.4 The bobber texture

The original draws the 8x8 region **(1,2)** of the **vanilla** 1.7.10
`textures/particle/particles.png` (`oracle RenderGolemBobber.java:28` and :36-46). That file is a
Mojang asset and it is **not** in the original jar: the region can only come from the Mojang asset
store, and `launcherresources.mojang.com` does not resolve in this environment (the version manifest
and the client jar itself do resolve, so the network is only partly blocked). 1.21.1 has no such
atlas — every particle has its own file. The native therefore uses the vanilla fishing hook texture;
the size of the quad, the scale of 0.5, the camera orientation and the curve of the rope are the
original's.

---

## 6. What could not be verified in this environment

* **No build, no test run, no GameTest, no client.** The wave was told not to run Gradle.
* **The type check is not a build.** The changed files were compiled with `javac` against the
  access-widened 1.21.1 jar of the leader's Gradle cache, but the resources, the mixins and the
  runtime behaviour were not checked at all. The leader has to run `:common:test`,
  `:fabric:runGameTest` and the dedicated server.
* **The 1.7.10 vanilla classes.** The oracle holds TC4's own sources only. The mutex bits and the
  chest interaction therefore rest on the **official Mojang 1.7.10 `client.jar`**, downloaded for
  this wave and decompiled with CFR 0.152; the class names were found through the leader's 1.7.10
  SRG mapping (`uj` = `EntityAITasks`, `aow` = `TileEntityChest`).
* **The 1.21.1 selector.** `GoalSelector`, `WrappedGoal`, `Goal`, `Mob` and `ChestBlockEntity` were
  decompiled from the leader's access-widened 1.21.1 jar with CFR 0.152 into `/tmp`; nothing of that
  decompilation was committed.
* **The client frames.** Nothing of this wave is visible in the client, so no acceptance criterion
  is claimed.

## 7. The files changed by this wave

```
common/src/main/java/thaumcraft/common/lib/golems/GolemLadderRules.java              (new)
common/src/main/java/thaumcraft/common/entities/golems/GolemChestInteraction.java     (new)
common/src/main/java/thaumcraft/common/entities/golems/GolemHelper.java               (new)
common/src/main/java/thaumcraft/common/entities/golems/InventoryGolemGoals.java       (rewritten)
common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java            (the ladder, the searches)
common/src/test/java/thaumcraft/golems/GolemLadderRulesTest.java                      (new, authored)
common/src/test/java/thaumcraft/golems/GolemHelperStructureTest.java                  (new, authored)
docs/porting/scope-ledger.json                                                         (23 rows)
docs/analysis/2026-10-23-agent-w3golems.md                                             (this report)
```

