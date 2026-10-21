# Agent wave `golems` — Golemancy parity re-check (2026-10-23)

Branch: `agent/golems` (worktree `/home/cyberfox-kali/wt/golems`).
Domain: `golems_and_tasks` — 41 open ledger units (`/home/cyberfox-kali/units/golems.txt`).

Sources used:

* the readable 1.7.10 source of TC4 4.2.3.5 in `/home/cyberfox-kali/OpenTC4` (the oracle of this
  wave; every `oracle …:line` below points into it);
* the original jar `Thaumcraft-1.7.10-4.2.3.5.jar` (read only) — used to confirm the class
  layout, because OpenTC4 is a *patched* 1.7.10 tree and differs from the shipped jar in places
  (see “OpenTC4 is not the jar” below);
* the port in `common/src/main/java/thaumcraft/**`.

**No Gradle was run in this worktree.** Every test named below is *authored but not executed*.

---

## 1. Result

| | before | after |
|---|---|---|
| `missing` | 1 | 0 |
| `partial` | 40 | 34 |
| `parity_verified` | 0 | 6 |
| `not_assessed` | 1 | 1 |

Units moved to `parity_verified` (6):

| unit | note |
|---|---|
| `thaumcraft/client/fx/beams/FXBeamGolemBoss.java` | `missing` → `parity_verified`; the client beam of the original is dead code |
| `thaumcraft/common/entities/golems/EntityGolemBobber.java` | the size, the cast velocity, the physics, the lifetime, the render distance |
| `thaumcraft/common/entities/golems/ItemGolemBell.java` | after the marker-selection fix of this wave |
| `thaumcraft/common/entities/golems/ItemGolemPlacer.java` | the placement, the fence offset, the home, the owner, the upgrades, the decoration, the markers |
| `thaumcraft/common/entities/ai/inventory/AIItemPickup.java` | the Gather pickup goal with its original number 2 |
| `thaumcraft/common/entities/ai/misc/AIReturnHome.java` | the shared return home with its original number 6 |

The other 35 rows keep their coverage; each of them now carries the evidence and the *exact*
remaining difference of this wave (section 5). `docs/porting/scope-ledger.json` was rewritten with
the same JSON shape (`json.dumps(..., ensure_ascii=False, indent=2)`, verified byte-identical on a
round trip), `counts` was not touched, and the `reviewed_sha256` anchors are unchanged: 827 legacy
fingerprints re-hashed clean, 0 stale, 0 original-side fingerprints changed.

---

## 2. Defects found and fixed

### D1 — the bell deleted a coloured marker instead of adding an uncoloured one

* **Original.** `oracle thaumcraft/common/entities/golems/ItemGolemBell.java:104-184`
  (`changeMarkers`) selects the marker of the clicked spot **by colour**:
  * without Order it searches `markers.indexOf(new Marker(x, y, z, dim, side, -1))` — line 120 — so
    only the *uncoloured* marker of that spot is found. A coloured marker left over from an earlier
    Order golem is **not** found, and the `else` branch (line 150) adds an uncoloured marker next to
    it;
  * with Order it searches the colours `-1 .. 15` in **ascending** order (lines 122-129) and takes
    the **lowest colour present** at that spot.
* **Port.** `common/src/main/java/thaumcraft/common/items/golems/GolemMarker.java:113-126` (before
  this wave) took the **first marker of that spot, whatever its colour**
  (`edited.get(index).sameSpot(spot)`).
* **Effect.** A click without Order on a spot that holds a coloured marker **deleted** the coloured
  marker instead of adding an uncoloured one next to it; a click with Order did not necessarily
  advance the lowest colour of that spot.
* **Fix.** `GolemMarker.click` now reproduces the original selection
  (`GolemMarker.java:119-147`): the uncoloured marker only without Order, the lowest colour
  `-1..15` with Order.
* **Test.** `common/src/test/java/thaumcraft/golems/GolemOracleContractTest.java`
  (`withOrderTheBellCyclesTheLowestColourOfThatSpotFirst`,
  `withoutOrderTheBellNeverTouchesAColouredMarkerOfThatSpot`) and the corrected expectation in
  `common/src/test/java/thaumcraft/golems/GolemColorsTest.java:94-108`.
* **Note.** The port had deliberately normalised this corner case (the old test said “a leftover
  colour from a former Order golem is still the same spot and is removed, not duplicated”). The
  original's rule is now restored; the state is unreachable in normal play, because the Order
  upgrade always travels with the coloured markers.

### D2 — the Alchemy golem never found an alembic that stood above a gap

* **Original.** `oracle thaumcraft/common/entities/ai/fluid/AIEssentiaGather.java:51-67` scans the
  levels `cY+5 … cY+0` of the alchemy furnace column **without a break**: a level that is not an
  alembic is skipped and the search continues.
* **Port.** `common/src/main/java/thaumcraft/common/entities/golems/AlchemyGolemGoals.java:98`
  stopped the whole scan at the first non-alembic level (`break; // Contiguous real stack only`).
* **Effect.** An alembic that stood above a gap in the column was never used as the essentia
  source of the golem.
* **Fix.** The level is now skipped and the scan continues
  (`AlchemyGolemGoals.java:98`, `continue`). The selection rule is unchanged: the largest amount
  wins, and among equal amounts the **highest** alembic wins, exactly as the original's
  descending scan with a strictly greater comparison does.
* **Test.** Not unit-testable without a level; the rule is covered by the ledger evidence of
  `thaumcraft/common/entities/ai/fluid/AIEssentiaGather.java`.

---

## 3. Findings that were verified and left alone (with the reason)

### D3 — the Guard/Butcher target retention (deliberate deviation, not changed)

* **Original.** The jar of TC4 4.2.3.5 confirms the class layout:
  `thaumcraft/common/entities/ai/combat/AINearestAttackableTarget.class` and
  `…/AINearestButcherTarget.class` extend `net/minecraft/entity/ai/EntityAITarget` and declare their
  **own private** `float targetDistance` (access flag `ACC_PRIVATE`). The vanilla
  `continueExecuting()` reads the **parent** field, which the vanilla constructor
  `EntityAITarget(creature, 0.0F, checkSight, nearbyOnly)` leaves at **0.0F**; the subclass writes
  its private field instead (`oracle …/AINearestAttackableTarget.java:26,34`). The original
  therefore keeps its target only while `distanceSq <= 0`, i.e. it drops and re-acquires the target
  every other tick.
* **Port.** `common/src/main/java/thaumcraft/common/entities/golems/GuardGolemGoals.java:249-255`
  keeps the target while it stays a valid target, i.e. inside the golem's range of its home
  (`isGuardTarget`), with the golem's own range instead of the vanilla follow range.
* **Why not changed.** The choice of targets is identical (the nearest valid one inside the
  golem's range, `AINearestAttackableTarget.java:33-51`); only the retention of an already chosen
  target differs. Reproducing the original would make the Guard drop its target every other tick
  for no observable gain, and the audit decision of 2026-10-21 already chose the range. The
  deviation is now written down in the ledger rows of `AITarget` and `AINearestAttackableTarget`.
* **Note.** TC4's *own* `AITarget` class (the one `AIHurtByTarget` extends, with the distance
  16.0F) is a different class from the vanilla one — the earlier review conflated the two.

### D4 — the Fill/Empty/Sorting goal ladder (the open decision 1 of audit a7)

* **Original.** Five selector entries per core with the numbers 0..4 (Fill: `AIHomeReplace 0`,
  `AIHomePlace 1`, `AIHomeDrop 2`, `AIFillTake 3`, `AIFillGoto 4`;
  `oracle …/EntityGolemBase.java:238-257,282-286,295-300`), all with the mutex bits 3, so only one
  of them runs and the lowest number wins.
* **Port.** One route goal per core (`InventoryGolemGoals.java:33-37`) with the number of the goto
  plus a server tick that performs the other four actions.
* **What was checked.** The *observable* rules of all five goals were compared one by one with the
  original and are reproduced: the golem delay of five ticks on the replace/place/take/goto goals
  and **not** on the drop goal (`AIHomeDrop.java:23-35` has no delay gate), the squared distance of
  five to the home, the adjacency range of four, the range of the goto, the budget of 200 ticks and
  the random detour within 2/1, the “no home container → drop the stock at the home” rule with the
  original’s motion and pickup delay of ten, the first-marked-block fallback of the Empty route
  without a range and without a home exclusion, and the Empty preference of an adjacent container
  that accepts the stock over the drop at a marked block.
* **Why not changed.** Restoring five separate goals per core is a large rewrite of a working
  implementation, and this wave may not run Gradle, so the change could not be compiled or tested
  here. The structural difference stays documented in the twelve ledger rows.

### D5 — the door goal

* **Original.** `oracle …/ai/misc/AIDoorInteract.java:27-83` scans the **next two points of the
  current path** within a squared distance of 2.25 for a wooden door or a fence gate and keeps it
  open while the golem passes it; `AIOpenDoor.java:19-56` opens and closes it.
* **Port.** The vanilla `OpenDoorGoal` (`GatherGolemGoals.java:61-64`), which looks at the block in
  front of the golem. The twenty-tick open window and the fact that neither goal takes a mutex bit
  are the same.
* **Why not changed.** A native door goal that scans the path is not available; the deviation is
  written down in the two door rows.

### D6 — the narrowed Decanting and Alchemy destinations

* The original Decanting carries **any** registered fluid (lava included,
  `oracle …/ai/fluid/AILiquidGather.java:59-83`); the native accepts the water bucket alone
  (`GolemData.isSupportedWaterFilter`).
* The original Alchemy golem may empty itself into **any** marked essentia transport
  (`oracle …/ai/fluid/AIEssentiaEmpty.java:79-98`); the native accepts a marked jar or a marked
  reservoir (`AlchemyGolemGoals.java:161`).
* Both narrowings are documented in the six fluid rows; they are content decisions of the port,
  not defects of this wave.

### D7 — the Use core

* **Original.** `oracle …/ai/interact/AIUseItem.java:39-196` can use any item on any marked block.
  The port reproduces the original's *numbers* — the click delay is
  `ticksExisted + (5 - ticksExisted) * 3` (**not** an upgrade count), the unused
  `ceil(range / 3)` of the constructor, the squared distance of four, the budget of 200 ticks and
  the 10000 ms timeout — but only performs the original's default use (wheat seeds on farmland and
  a bare hand on a lever or a button). Documented in the `AIUseItem` row.

### D8 — the GUI button ids and the Fill double slot

* **Original.** `oracle …/golems/ContainerGolem.java:74-110`: the paging buttons are 66/67, the
  colours are cycled with the slot buttons `0 … 2*slots-1`, and the Fill core adds **two** ghost
  slots for every inventory index (lines 38-46: the `core == 0` branch and the `else` branch both
  add a slot for the same index, one with the stack limit 256 and one with 1).
* **Port.** The paging buttons are 60/61, the Order colours are 100+i, and one ghost per slot.
  The wanted quantity is a separate field (1..256) instead of the stack size in the ghost slot.
  Documented in the `ContainerGolem` row.

### What was checked and found correct (no change)

* the eight material rows and every derived number (armour cap 20, top hat +5 HP, carry
  `carry + min(16, max(4, carry))·Earth`, fluid `floor(sqrt(carry))·1000`, radius `16 + 4n` plus the
  spectacles and the advanced fifth, repair delay `0.66` with the fez, attack delay 20/18, heavy
  water speed ×2 for stone/iron/thaumium);
* the owner gate: the original cancels **every** interaction with an owned golem for a stranger
  (`oracle …/lib/events/EventHandlerEntity.java:317-326`, the message “You are not my Master!”);
  the port does the same in `EntityGolemBase.mobInteract` (`EntityGolemBase.java:2330-2337`) with
  the same text (`tc.golem.notowner`);
* the fetter: the original toggles the metadata 9 ↔ 10 on a neighbour change only
  (`oracle …/blocks/BlockCosmeticSolid.java:338-347`) and drops the active one as the inactive
  item (`damageDropped`, line 239-241); the port has the same `active` state property, the same
  neighbour rule and the same loot table;
* the AI is disabled while the golem is paused or fettered: the original’s `isAIEnabled()` is
  `!paused && !inactive` (`oracle …/EntityGolemBase.java:369-371`); the port gates every goal and
  the whole server tick with `canWork()` (`EntityGolemBase.java:784-792`), which contains
  `configurationOpeners.isEmpty()` and `!fettered`;
* the goal numbers and the registration order (`EntityGolemBase.GoalPriority`, the ladder table of
  `GolemGoalLadderTest`);
* the dart rules (`GolemDartRules`: the interval `30 - 8·Air`, the first delay `interval/2`, the
  minimum distance of three blocks, the reach `0.8·range`, the spread `7 - 1.75·Water`, the damage
  `0.4·attack`) — identical to `oracle …/ai/combat/AIDartAttack.java:15-63`;
* the death cargo: the original drops the carried stock (`dropStuff`,
  `oracle …/EntityGolemBase.java:929-938`) and nothing else; the port does the same
  (`EntityGolemBase.dropCustomDeathLoot`, `:2568-2602`);
* the bobber: the original’s two `EntityFishHook` counters are never incremented in
  `EntityGolemBobber`, so its gravity term and its extra splash particles never run — the native
  has neither of them either.

---

## 4. OpenTC4 is not the jar

OpenTC4 is a *continued* 1.7.10 tree, so it is not a faithful oracle in every place. The
difference was found in this domain and confirmed against the shipped jar:

* the jar’s `AILiquidGather.class` has the methods `validFluidBlock`, `rebuildQueue` and
  `getConnectedFluidBlocks`; OpenTC4 replaces the recursion with a breadth-first “tweak” and adds
  `ConfigurationHandler.INSTANCE.getDecantMaxBlocks()`. The port follows the **jar**
  (`GolemLiquidQueue` reproduces the original depth-first 26-neighbourhood walk and the
  farthest-first order, and the port documents its own `MAX_VISITED` bound).

The jar was also used to confirm that `thaumcraft/api/entities/IGolemInfo.java` and
`thaumcraft/common/integration/waila/GolemWailaPlugin.java` do **not** exist in TC4 4.2.3.5 (the
`api/entities` package holds only `IEldritchMob` and `ITaintedMob`), which is what the two
legacy-only rows of the ledger already state.

---

## 5. Ledger rows and their remaining difference

| unit | coverage | remaining difference |
|---|---|---|
| `client/fx/beams/FXBeamGolemBoss.java` | `parity_verified` | none (dead code in the original) |
| `common/integration/waila/GolemWailaPlugin.java` | `not_assessed` | the integration target does not exist in 1.21.1 |
| `client/gui/GuiGolem.java` | `partial` | needs a client inspection |
| `client/renderers/entity/RenderGolemBase.java` | `partial` | needs a client inspection |
| `client/renderers/entity/RenderGolemBobber.java` | `partial` | needs a client inspection |
| `client/renderers/models/ModelGolem.java` | `partial` | needs a client inspection |
| `client/renderers/models/ModelGolemAccessories.java` | `partial` | needs a client inspection |
| `client/renderers/models/entities/ModelGolem.java` | `partial` | needs a client inspection |
| `client/renderers/models/entities/ModelGolemAccessories.java` | `partial` | needs a client inspection |
| `common/entities/golems/EntityGolemBobber.java` | `parity_verified` | none |
| `api/entities/IGolemInfo.java` | `partial` | the public API type is not published (another lane owns `thaumcraft/api/**`) |
| `ai/combat/AINearestAttackableTarget.java` | `partial` | the target retention (D3) |
| `ai/combat/AITarget.java` | `partial` | the original has two different classes here (D3) |
| `ai/fluid/AIEssentiaEmpty.java` | `partial` | the narrowed destination set (D6) |
| `ai/fluid/AIEssentiaGather.java` | `partial` | the narrowed source set (D6); the column gap is **fixed** |
| `ai/fluid/AIEssentiaGoto.java` | `partial` | the narrowed destination set (D6) |
| `ai/fluid/AILiquidEmpty.java` | `partial` | water only (D6) |
| `ai/fluid/AILiquidGather.java` | `partial` | water only (D6) |
| `ai/fluid/AILiquidGoto.java` | `partial` | water only (D6) |
| `ai/interact/AIUseItem.java` | `partial` | the default use only (D7) |
| `ai/inventory/AIHomeReplace.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AIHomePlace.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AIHomeDrop.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AIFillTake.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AIFillGoto.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AIEmptyPlace.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AIEmptyDrop.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AIEmptyGoto.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AIHomeTake.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AIHomeTakeSorting.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AIItemPickup.java` | `parity_verified` | none |
| `ai/inventory/AISortingGoto.java` | `partial` | the merged ladder (D4) |
| `ai/inventory/AISortingPlace.java` | `partial` | the merged ladder (D4) |
| `ai/misc/AIDoorInteract.java` | `partial` | the path-point scan (D5) |
| `ai/misc/AIOpenDoor.java` | `partial` | the path-point scan (D5) |
| `ai/misc/AIReturnHome.java` | `parity_verified` | none |
| `common/entities/golems/ContainerGolem.java` | `partial` | the button ids and the Fill double slot (D8) |
| `common/entities/golems/EntityGolemBase.java` | `partial` | the merged ladder, the door goal, the target retention (D3-D5) |
| `common/entities/golems/GolemHelper.java` | `partial` | the helper is split into pure rule classes |
| `common/entities/golems/ItemGolemBell.java` | `parity_verified` | none |
| `common/entities/golems/ItemGolemPlacer.java` | `parity_verified` | none |

---

## 6. Tests authored (NOT executed)

No Gradle was run in this worktree, so every test below is **authored but not executed**; the
leader has to run `:common:test`.

* **new** `common/src/test/java/thaumcraft/golems/GolemOracleContractTest.java` — 10 tests that pin
  the original values: the eight material rows of `EnumGolemType`; the carry limit and the fluid
  limit; the work radius with the spectacles and the advanced fifth; the armour cap of twenty and
  the top hat’s five health; the attack delay 20/18; the repair delay with the fez; the bobber
  cast velocity (`sqrt(distance)`, not the distance and not the square root); the bell marker
  selection with and without Order.
* **changed** `common/src/test/java/thaumcraft/golems/GolemColorsTest.java:94-108` — the
  expectation of the bell’s marker selection now follows the original instead of the port’s
  normalisation.

Nothing else was changed in the test tree.

---

## 7. What could not be verified in this environment

* **No build, no test run, no GameTest, no client.** The wave was told not to run Gradle.
* **`update_scope_ledger.py --check` cannot run here**: it needs the private oracle
  (`work/thaumcraft-4.2.3.5-oracle`), which is not present in this sandbox
  (`Missing source root: /home/cyberfox-kali/wt/work/thaumcraft-4.2.3.5-oracle`). The script’s
  own `--self-test` passes, the JSON round trip is byte-identical, the review shape of all 41 rows
  was validated against the script’s policy, the 827 legacy fingerprints re-hash clean and the
  `counts` block is untouched. The leader has to run the real `--check` with the oracle present.
* **The 1.7.10 vanilla `EntityLivingBase.entityDropItem(ItemStack, float)` semantics.** The
  original drops the carried stock with `entityDropItem(this.itemCarried, 0.5F)`
  (`oracle …/EntityGolemBase.java:933-937`). The float is either a drop chance or a drop height:
  TC4 itself calls the same method with `1.0F`, `1.5F` and `this.height / 2.0F`
  (`oracle …/monster/EntityTaintSpore.java:229`, `…/monster/boss/EntityCultistLeader.java:125`),
  which reads like a height, but the vanilla 1.7.10 source is not available here to decide it. The
  port drops the cargo unconditionally; if the float is a chance, the original loses the cargo half
  of the time. **Left as is, flagged.**
* **The 1.7.10 vanilla mutex-bit semantics of `EntityAITasks`.** The mutual exclusion of the five
  goals of a core (all with the mutex bits 3) was taken from the original’s own `setMutexBits(3)`
  calls and from the 1.21.1 `GoalSelector` rules the port already documents; the vanilla 1.7.10
  `EntityAITasks.onUpdateTasks` itself could not be read here. This is part of D4.
* **The client rows.** The seven golem client classes are owned by another lane and were only read.
* **The eldritch golem beam.** The original never shows it (the packet is never sent), so the
  native orb projectile is the original’s observable behaviour; the *visual* comparison of the two
  was not possible and is not claimed.

---

## 8. Files changed by this wave

```
common/src/main/java/thaumcraft/common/items/golems/GolemMarker.java          (fix D1)
common/src/main/java/thaumcraft/common/entities/golems/AlchemyGolemGoals.java   (fix D2)
common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBobber.java   (pure rule for the test)
common/src/test/java/thaumcraft/golems/GolemOracleContractTest.java            (new, authored)
common/src/test/java/thaumcraft/golems/GolemColorsTest.java                    (expectation of D1)
docs/porting/scope-ledger.json                                                  (41 rows)
docs/analysis/2026-10-23-agent-golems.md                                        (this report)
```

Commits: `fix(golems)` ×2, `test(golems)` ×1, `docs(golems)` ×1.
