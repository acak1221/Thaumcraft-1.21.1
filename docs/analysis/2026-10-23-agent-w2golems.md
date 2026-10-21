# Agent wave 2 `w2golems` — the golemmancy surface of TC4 4.2.3.5 (2026-10-23)

Branch: `agent/w2golems` (worktree `/home/cyberfox-kali/wt/w2golems`).
Domain: `golems_and_tasks` — the 34 units of `/home/cyberfox-kali/units/w2golems.txt` that were still `partial`.

Sources used:

* the **oracle** `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (859 files, byte-exact, the authority);
* the shipped jar `Thaumcraft-1.7.10-4.2.3.5.jar` for the bytecode of the classes whose behaviour the
  decompiled source cannot show (the shadowed field of `AINearestAttackableTarget`);
* the 1.12.2 reference `src/main/java/thaumcraft/common/entities/ai/**` of this repository, used **only** to
  read the *names* of the vanilla 1.7.10 methods (it is not the 4.2.3.5 authority — where it differs from the
  oracle, the oracle wins; see D5);
* the 1.21.1 port `common/src/main/java/thaumcraft/**`.

**No Gradle was run in this worktree.** Every test named below is *authored but not executed*. The changed
sources were **type-checked** with `javac` against the 1.21.1 merged jar of the leader's Gradle cache (the
access widener of the project is not applied by that check, so the five access errors of `EntityGolemBase`,
`EntityTaintSwarm`, `EntityFireBat` and `EntityFollowingItem` are pre-existing and expected; **no new error
came out of the changed files**).

---

## 1. Result

| | before the wave | after the first session of this wave | after this session |
|---|---|---|---|
| `partial` | 34 | 26 | 23 |
| `parity_verified` | 0 | 8 | 11 |
| `missing` | 0 | 0 | 0 |

Units moved to `parity_verified` by **this** session (3):

| unit | why |
|---|---|
| `thaumcraft/common/entities/ai/misc/AIDoorInteract.java` | the native door goal reproduces the original's path scan, its reach and its two doors (D10) |
| `thaumcraft/common/entities/ai/misc/AIOpenDoor.java` | the native door goal opens the plain wooden door and the fence gate for twenty ticks and closes them again (D10) |
| `thaumcraft/client/renderers/entity/RenderGolemBase.java` | the free upgrade slots show the original's blank icon again (D12) |

Units moved to `parity_verified` by the **first session of this wave** (8):

| unit | why |
|---|---|
| `thaumcraft/api/entities/IGolemInfo.java` | the file does not exist in 4.2.3.5; every value the 1.12 interface exposed is reachable on the native entity |
| `thaumcraft/common/entities/ai/combat/AINearestAttackableTarget.java` | the retention of the Guard's target now follows the original (D3) |
| `thaumcraft/common/entities/ai/combat/AITarget.java` | the base class is dead code for the golems; the native now reproduces the two effective rules (D3) |
| `thaumcraft/common/entities/golems/ContainerGolem.java` | the Order colour buttons and the page arrows use the original ids (D1) and the menu click is the original's (D11) |
| `thaumcraft/client/renderers/models/ModelGolem.java` | the native model is the original's box model |
| `thaumcraft/client/renderers/models/entities/ModelGolem.java` | the same file covers both original paths (the two oracle files are byte-identical apart from the package) |
| `thaumcraft/client/renderers/models/ModelGolemAccessories.java` | the native model is the original's accessory model and is now rendered (D4) |
| `thaumcraft/client/renderers/models/entities/ModelGolemAccessories.java` | the same file covers both original paths |

The other 23 rows keep `partial`; every one of them now carries the **exact** remaining difference in its
`review.notes`. `docs/porting/scope-ledger.json` was rewritten with the same JSON shape
(`json.dumps(..., ensure_ascii=False, indent=2)`), `counts` was not touched (the script compares it and it is
byte-identical), and

```
python3 scripts/porting/update_scope_ledger.py --check --original-root /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle
… "stale_review_fingerprints": 0
```

---

## 2. What the first session of this wave had already done

* **D1** — the Order colour button of the golem menu could only raise the colour; the button ids are now the
  original's (`ContainerGolem.nextColour`, `ContainerGolem.java:254-257`).
* **D2** — the golem preview stood in the wrong corner of the menu.
* **D3** — the Guard kept a target the original would have dropped (the retention rule of the original's
  nearest goal).
* **D4** — the worn accessories of a golem were never drawn and the upgrade icons were missing.
* **D5** — the 1.12.2 reference and the 4.2.3.5 oracle differ in the door goal; the oracle wins.
* the ledger of the 34 rows and the report itself.

---

## 3. The defects found and fixed in this session

### D10 — the golem opened every door it walked into, and never a fence gate

* **Original.** `oracle thaumcraft/common/entities/ai/misc/AIDoorInteract.java:24-95` and
  `…/AIOpenDoor.java:117-171`:
  * the goal runs only while the golem **collided horizontally** (`AIDoorInteract.func_75250_a`:40-42; the
    1.7.10 field `collidedHorizontally`, the 1.21.1 field `horizontalCollision`) **and** while it has an
    **unfinished path** (`:44-46`);
  * it scans the **next points of the current path** — the loop bound is
    `Math.min(currentPathIndex + 2, pathLength)` (`:47`) — and takes the first point within a **squared
    distance of 2.25** of the golem (`:52`) that holds a door;
  * the only doors are the **plain wooden door** and the **fence gate** (`findUsableDoor`:91-94);
  * the golem's **own block** is the fallback (`:58-65`);
  * the door is opened for **twenty ticks** (`AIOpenDoor.func_75249_e`:135) and closed again when the goal
    stops (`func_75251_c`:152-164);
  * the goal took **no mutex bits**, so it ran beside every other goal of the golem.
* **Port (before).** `common/src/main/java/thaumcraft/common/entities/golems/GatherGolemGoals.java:36-39` — the
  **vanilla** `OpenDoorGoal`, which looks at the block in front of the golem, opens it whenever the golem
  walks, knows no fence gate and never closes a door again. A native goal with no flags can only start when
  no other goal runs in the 1.21.1 selector, so the goal was practically dead.
* **Effect.** A walking golem opened the doors in front of it; a riding golem (the original's case) never
  opened a door, and a fence gate was never opened at all.
* **Fix.** The new `common/src/main/java/thaumcraft/common/entities/golems/GolemDoorGoal.java` reproduces the
  original: the blocked-golem rule (`:70-75`), the unfinished path (`:76`), the scan of the path points and
  the fallback (`:77-89`), the two doors (`:117-123`), the twenty tick window (`:96-100`) and the closing
  (`:108-113, :146-160`). The pure numbers live in
  `common/src/main/java/thaumcraft/common/lib/golems/GolemDoorRules.java`.
* **Quirk reproduced on purpose.** `AIOpenDoor.func_75249_e` of the 4.2.3.5 jar does **not** call the base
  `startExecuting`: the counter stays at the 200 of `shouldExecute` and the two direction vectors of the base
  class stay zero, so the dot product of `AIDoorInteract.func_75246_d`:82-89 is always zero and
  `hasStoppedDoorInteraction` is never set. The door therefore stays open for the whole twenty ticks. The
  1.12.2 reference does call the base method and ends the interaction as soon as the golem passes the door —
  the 4.2.3.5 jar wins.
* **The 1.7.10 mutex.** The original's goal took no mutex bits. The 1.21.1 selector has no such goal, so the
  native takes the `JUMP` flag (which no other goal of the golem uses) and is not interruptable: it never
  blocks the other goals and is never preempted by them.
* **Test.** `common/src/test/java/thaumcraft/golems/GolemDoorRulesTest.java` (authored, not executed).

### D11 — the golem menu played no sound at all

* **Original.** `oracle thaumcraft/common/entities/golems/ContainerGolem.java:110` — **every** menu button
  plays `random.click` with the volume **0.2** and the pitch **0.8** at the golem, also the buttons that
  change nothing (the page arrow at the first or at the last page). The method returns `true` in every case
  (`:111`).
* **Port (before).** `common/src/main/java/thaumcraft/common/container/ContainerGolem.java` — the button
  handling returned `false` for an unhandled button and played no sound at all.
* **Fix.** `ContainerGolem.java:200-211` — the handling moved into `applyButton` (`:213-251`) and the click is
  now played at the golem for every button that reaches the menu, with the volume 0.2 and the pitch 0.8.

### D12 — the free upgrade slots of a golem showed nothing

* **Original.** `oracle thaumcraft/client/renderers/entity/RenderGolemBase.java:116-141` draws **one icon per
  upgrade slot**; the icon of a negative damage value is the blank one
  (`oracle thaumcraft/common/entities/golems/ItemGolemUpgrade.java:33,45-57`).
* **Port (before).** `common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBase.java` — only the
  **installed** upgrades were drawn; a free slot was empty.
* **Fix.** `RenderGolemBase.java:135-190` — the free slot now draws the flat quad of the original with the
  texture `textures/items/golem_upgrade_empty.png`, which is in the native assets, at the original's place and
  scale.

### D13 — the golem menu never showed the threat line of an advanced golem

* **Original.** `oracle thaumcraft/client/gui/GuiGolem.java:72-78` — an advanced golem shows a random threat
  line in **one case of four** (`:83-87`); the blurb of the core is drawn for **every** core (the harvest, the
  alchemy and the lumber core have the text `??`).
* **Port (before).** `common/src/main/java/thaumcraft/client/gui/GuiGolem.java` — the blurb was drawn for
  seven of the twelve cores and the threat line never.
* **Fix.** `GuiGolem.java:21-45, :128-140` — the threat line is decided once, when the menu opens, and the
  blurb is drawn for every core.

### D14 — the marked containers of the golem were bound to the distance of the **home**

* **Original.** `oracle thaumcraft/common/entities/golems/GolemHelper.java:54-64` — `getMarkedContainers`
  knows **every** marked container of the dimension; it has **no range at all**. The range of the original is
  the **golem's own** (`AIFillGoto:100-104`, `AIEmptyGoto:56`, `AISortingGoto:58-59`); the adjacent goals are
  bound by the **squared distance of four** (`GolemHelper.getMarkedContainersAdjacentToGolem`:70).
* **Port (before).** `common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java:2103-2117`
  — a marked container had to be inside the range of the **home** *and* inside the range of the golem, which
  is a bound no original rule has.
* **Fix.** `EntityGolemBase.java:2103-2125` — only the range of the golem is a bound now, exactly like the
  Sorting lookup that already had it.
* **Also fixed.** `EntityGolemBase.java:2130-2136` — the first adjacent marked block of the Empty golem is
  never the **home** of the golem any more (`oracle …/ai/inventory/AIEmptyDrop.java:49`).

---

## 4. The two questions the wave had to answer

### Q1 — the Fill/Empty/Sorting goal ladder: what is actually missing?

* **Original.** `oracle thaumcraft/common/entities/golems/EntityGolemBase.java:269-341` — the Fill core
  registers `AIHomeReplace 0`, `AIHomePlace 1`, `AIHomeDrop 2`, `AIFillTake 3`, `AIFillGoto 4`; the Empty core
  `AIHomeReplace 0`, `AIEmptyPlace 1`, `AIEmptyDrop 2`, `AIEmptyGoto 3`, `AIHomeTake 4`; the Sorting core
  `AIHomeReplace 0`, `AISortingPlace 1`, `AISortingGoto 3`, `AIHomeTakeSorting 4`. All of them take the mutex
  bits three, so only one of them runs and the lowest number wins.
* **Port.** `common/src/main/java/thaumcraft/common/entities/golems/InventoryGolemGoals.java:33-37` — one route
  goal per core (Fill 4, Empty 3, Sorting 3) plus a server tick (`:65-77`) that performs the other four
  actions.
* **What was checked, goal by goal, in this wave.** The observable rules of all fifteen goals were compared
  with the oracle one by one:
  * the **five tick cadence** (`Config.golemDelay`:879) of the replace, the place, the take and the goto goals
    and **not** of the drop goal (`AIHomeDrop.func_75250_a`:37-39 has no delay gate) — the native reproduces
    this (`GolemRouteRules.decisionTick`);
  * the **squared distance of five** to the home (`AIHomePlace.func_75250_a`:37) — `GolemRouteRules.atHome`;
  * the **squared distance of four** to a marked block (`GolemHelper.ADJACENT_RANGE`:49) —
    `GolemRouteRules.adjacent`;
  * the **budget of 200** ticks of a route and the random detour of **2 and 1** with a budget of **20**
    (`AIFillGoto:128,140`) — `GolemRouteRules.ROUTE_BUDGET`, `DETOUR_XZ`, `DETOUR_Y`, `DETOUR_BUDGET`;
  * the **five tick chest window** of a transfer (`countChest` of the place and the take goals) and the
    **10000 ms** ignore delay of an item without a destination (`Config.golemIgnoreDelay`:880) —
    `GolemRouteRules.CHEST_WINDOW`, `IGNORE_DELAY_MS`;
  * the **home range of a marked container** — removed in D14, the original has no such bound;
  * the **home of the golem is never a drop target** of the Empty golem (D14, `AIEmptyDrop`:49);
  * the drop of the Fill golem at its home with the **motion of the original** and the **pickup delay of ten**
    (`AIHomeDrop:78-88`, `DeferredItemDrops.enqueueGolemThrow`:423-452) — reproduced;
  * the rule that the **Fill core never replaces** its cargo: the original's `getItemsNeeded` returns `null`
    for the Fill core (`GolemHelper:448-471`), so `AIHomeReplace` never fires for it — the native reproduces
    this as well.
* **What is still missing (and why the twelve rows stay `partial`).**
  1. **The structure.** The original registers five selector entries per core that exclude each other with
     the mutex bits three; the native has one route goal per core and performs the other four actions in its
     server tick, in the order of the original. The observable outcome is the same for a complete transfer,
     but the goals of the original hold the mutex of the ladder for **up to 200 ticks** when a transfer only
     placed **part** of the cargo (`AIEmptyPlace.func_75253_b`:69-71), so the ladder of the original is
     blocked for ten seconds in that case; the native retries at the next decision tick.
  2. **The chest interaction.** The original opens the chest of a transfer for five ticks
     (`Config.golemChestInteract`, `AIHomePlace.func_75251_c`:69-78); 1.21.1 has no equivalent for a mob.
- **Verdict.** The rules are reproduced and now testable as pure functions, the structure is not. The twelve
  rows keep `partial` with that exact difference.

### Q2 — the Guard/Butcher target retention: is the deviation observable?

* **Original.** `oracle thaumcraft/common/entities/ai/combat/AINearestAttackableTarget.java` — the class
  **extends the vanilla `net.minecraft.entity.ai.EntityAITarget`** and declares its **own private**
  `float targetDistance` (`:29`). The jar confirms it:
  `javap -p thaumcraft/common/entities/ai/combat/AINearestAttackableTarget.class` →
  `extends net.minecraft.entity.ai.EntityAITarget` and `private float targetDistance;` (the parent's field is
  `protected float targetDistance`). The subclass **never overrides the continuation rule** — the oracle's
  class has only `func_75250_a` (`shouldExecute`, `:46-60`) and `func_75249_e` (`startExecuting`, `:62-65`).
  The inherited rule compares the squared distance with the **parent's** distance field, and the parent keeps
  its own distance at `0.0F` because the vanilla constructor is `EntityAITarget(EntityCreature, boolean,
  boolean)`. The goal therefore never continues.
* **But the goal is re-evaluated on every tick.** The subclass **overrides** `shouldExecute` completely, so the
  vanilla guard "`theCreature.getAttackTarget() != null`" never runs. `func_75250_a` searches the box
  `inflate(range, 4, range)` (`:51`), sorts it by distance (`:52`) and takes the first entity the golem may
  attack (`:53-58`); `func_75249_e` writes it into the golem's attack target (`:63`). The registration
  (`oracle …/EntityGolemBase.java:302`) uses a chance of zero, so there is no random gate.
* **Verdict.** The *retention* of the original is "the goal never continues", but the *effect* is **not** that
  the golem loses its target: the goal starts again on the very next tick and writes the **nearest valid
  target** into the golem's attack target again. The observable rule of the original is therefore
  **"the golem's attack target is the nearest valid target at all times"** — exactly what the native does now
  (`GuardGolemGoals.java:231-277`: the goal re-picks the nearest valid target in `tick()`, `:255-260`).
  The wave-1 claim ("it drops and re-acquires the target every other tick") is therefore **wrong in its
  wording but right in its effect**: the target is not dropped, it is re-picked.
* The same holds for the Butcher: `oracle …/combat/AINearestButcherTarget.java` has the same shadowed field
  and the same override, and the native re-picks the oldest animal in `tick()` as well
  (`GuardGolemGoals.java:279-316`).
* TC4's *own* `AITarget` (`oracle …/combat/AITarget.java:30-70`) has the distance check and the sixty tick
  unseen counter, but its only golem subclass `AIHurtByTarget` overrides the continuation rule
  (`oracle …/combat/AIHurtByTarget.java:30-33`), so **neither rule ever applied to a golem**.

---

## 5. Findings that were verified and left alone (with the reason)

### D6 — the Use core still performs the default use only

* **Original.** `oracle …/ai/interact/AIUseItem.java:39-196` can use **any** item on **any** marked block.
* **Port.** `common/src/main/java/thaumcraft/common/entities/golems/GolemUsePlatform.java` performs the
  original's default use (wheat seeds on farmland, a bare hand on a lever or a button) and reproduces the
  original's numbers (the click delay `ticksExisted + (5 - ticksExisted) * 3`, the squared distance of four,
  the budget of 200 ticks, the 10000 ms timeout).
* **Why not changed.** The Use core needs a whitelist of items and blocks that the original takes from the
  marked block and the carried item; that is a content decision, not a defect. The three switches of the
  Use core of the original menu belong to the same content and are not drawn either.

### D7 — the Decanting and the Alchemy golem are water only

* **Original.** `oracle …/ai/fluid/AILiquidGather.java:59-83` — the marked **fluid handler** of *any* fluid
  and the marked **fluid block** of *any* fluid, with an explicit water and lava fall back;
  `oracle …/entities/golems/GolemHelper.java:225-266` — `findJarWithRoom` accepts a marked jar, a marked
  reservoir **and any other marked `IEssentiaTransport`**.
* **Port.** `common/src/main/java/thaumcraft/common/items/golems/GolemData.java:384-386` — only a pristine
  water bucket is a valid Decanting filter; `LiquidGolemGoals.java:74-103` gathers source water only;
  `AlchemyGolemGoals.java` accepts a marked jar or a marked reservoir.
* **Why not changed.** The native's fluid cargo is a single water volume in millibuckets
  (`GolemWaterCargo`), its own filter, its own bucket rendering and its own menu ghost, and the 1.21.1
  **common** module of this port has no loader independent fluid handler or essentia transport API. Widening
  it is a content change over the whole Decanting and essentia transport subsystems, not a defect fix.

### D8 — the travelling trunk, the bell and the placer

* They were closed by the first session of wave 1 (`ItemGolemBell`, `ItemGolemPlacer`) and are outside the
  unit list of this wave.

---

## 6. What the leader must verify on the real client

The client is runnable now, but only the leader can run it. The acceptance criteria of every client unit this
wave touched:

**A. The golem menu (`thaumcraft/client/gui/GuiGolem.java`, `…/container/ContainerGolem.java`)**

1. Open the menu of a Fill golem that carries the **Order** upgrade. Above each of the six ghost slots there
   must be a **24x12 bar** with a **6x6 square** in its middle; the square must show the **original's** colour
   of the slot (white `0xF0F0F0`, orange `0xEB8844`, …, black `0x1E1B1B`) and must be **absent** for a slot
   with the "any marker" colour.
2. Clicking the **left third** of that bar must lower the colour (any → black → dark grey → …) and the **right
   third** must raise it (any → white → orange → …); the square must follow the click after the server round
   trip.
3. Moving the mouse over the bar must show the **name** of the colour **above** the panel. The name appears
   only when the golem carries **Entropy**, exactly as in the original.
4. A golem with more than six slots (a Fill golem with a **Fire** upgrade) must show the **texture arrows** at
   the bottom right of the panel and the half-size page counter "1/2". The arrow of the end that cannot be
   used must be the dimmed sprite.
5. The **golem preview** must stand in the **centre** of the panel.
6. **Every** button of the menu must play the **click** (the vanilla button click) **at the golem** — also the
   page arrow at the first or at the last page, which changes nothing. (New in this session.)
7. The blurb of the core must be drawn for **every** core, and an **advanced** golem must show a **threat
   line** instead of the blurb in **one case of four** — the line is decided once, when the menu opens, so
   reopening the menu may show another line. (New in this session.)

**B. The golem renderer (`thaumcraft/client/renderers/entity/RenderGolemBase.java`)**

8. A golem with a **top hat** (or any other decoration) must show it on its head, a golem with the **bow tie**
   must show it at the neck, a golem with the **plate** must show the armour on its body, the **glasses** and
   the **visor** on the face, the **fez** on the head, the **mace** in the right and the **dart gun** in the
   left hand.
9. An **advanced** golem must show the brain and the jar on its head and the darker "evil head" over its face.
10. The bow tie and the head jar must be **semi transparent**; every other piece must be solid.
11. The installed **upgrades** must show as a row of small icons on the golem's **back**, and a **free** slot
    must show the **blank** icon of the original (a small empty square), not nothing. (New in this session.)
12. The golem's body texture, the sway while it walks, the 0.4 model scale, the head angle while it boots up
    and the red/green healing tint must be unchanged.

**C. The door goal (server side, visible in the world)**

13. A golem that walks into a **closed oak door** must open it and close it again about a second later; a
    golem that walks into a **fence gate** must open the gate and close it again. A golem that is **not**
    blocked horizontally must not open anything. (New in this session.)

---

## 7. The ledger rows and their exact remaining difference

| unit | coverage | remaining difference |
|---|---|---|
| `api/entities/IGolemInfo.java` | `parity_verified` | none (the file is not in 4.2.3.5; the values are on the native entity) |
| `client/gui/GuiGolem.java` | `partial` | the preview camera (the original's fixed 320x240 viewport) and the three switches of the Use core |
| `client/renderers/entity/RenderGolemBase.java` | `parity_verified` | none (the blank icon of a free upgrade slot is drawn again) |
| `client/renderers/entity/RenderGolemBobber.java` | `partial` | the texture of the quad: the 1.7.10 particle atlas does not exist in 1.21.1 |
| `client/renderers/models/ModelGolem.java` | `parity_verified` | none |
| `client/renderers/models/ModelGolemAccessories.java` | `parity_verified` | none |
| `client/renderers/models/entities/ModelGolem.java` | `parity_verified` | none |
| `client/renderers/models/entities/ModelGolemAccessories.java` | `parity_verified` | none |
| `ai/combat/AINearestAttackableTarget.java` | `parity_verified` | none |
| `ai/combat/AITarget.java` | `parity_verified` | none |
| `ai/fluid/AIEssentiaEmpty.java` | `partial` | the marked essentia transports that are neither a jar nor a reservoir |
| `ai/fluid/AIEssentiaGather.java` | `partial` | the same narrowing on the source side |
| `ai/fluid/AIEssentiaGoto.java` | `partial` | the same narrowing on the route side |
| `ai/fluid/AILiquidEmpty.java` | `partial` | the marked fluid handler of any fluid; the native empties into the attached crucible |
| `ai/fluid/AILiquidGather.java` | `partial` | any registered fluid (lava included); the native gathers source water only |
| `ai/fluid/AILiquidGoto.java` | `partial` | the route of the native is a water route |
| `ai/interact/AIUseItem.java` | `partial` | any item on any marked block; the native performs the default use only |
| `ai/inventory/AIEmptyDrop.java` … `ai/inventory/AISortingPlace.java` (12 rows) | `partial` | the structure: five selector entries per core with the numbers 0..4 and the mutex of the ladder, the native has one route goal per core and a server tick; the chest interaction of the original; the 200 tick mutex hold of a partial transfer |
| `ai/misc/AIDoorInteract.java` | `parity_verified` | none |
| `ai/misc/AIOpenDoor.java` | `parity_verified` | none |
| `common/entities/golems/ContainerGolem.java` | `parity_verified` | none |
| `common/entities/golems/EntityGolemBase.java` | `partial` | the inventory goal structure, the fluid set and the Use core |
| `common/entities/golems/GolemHelper.java` | `partial` | the original's one helper class is split into pure rule classes of the native tree |

---

## 8. Tests authored (NOT executed)

No Gradle was run in this worktree; the leader has to run `:common:test` and the GameTest suite. The two test
classes of this session were **type-checked** with `javac` against the 1.21.1 merged jar (they compile), but
they were **never executed**.

* **new** `common/src/test/java/thaumcraft/golems/GolemRouteRulesTest.java` — six tests: the decision cadence
  of five ticks, the squared distance of five to the home (the original's test is "not greater than five"),
  the squared distance of four to a marked block (the original's test is "less than four"), the budgets of 200
  and 20 with the detour of 2 and 1, the chest window of five and the ignore delay of ten seconds.
* **new** `common/src/test/java/thaumcraft/golems/GolemDoorRulesTest.java` — four tests: the scan bound
  `min(currentPathIndex + 2, pathLength)`, the squared distance of 2.25, the counter of 200 and the open
  window of 20, the direction order of the 1.7.10 fence gate.
* **from the first session** `common/src/test/java/thaumcraft/golems/GolemMenuOrderTest.java` — the Order
  colour cycle, the menu button ids, the sixteen marker colours, the seventeen language keys and the fourteen
  pieces of the accessory model.
* **changed** `common/src/gameTest/java/thaumcraft/common/testing/GolemColorsRuntimeChecks.java:156-211` — the
  colour buttons of the menu use the original's ids, and the lowering button is now covered.

**No test constructs a `Block`.** Every rule that is asserted is a pure static function of a world-free class
(`GolemRouteRules`, `GolemDoorRules`, `ContainerGolem.nextColour`), so the intrusive holder of the block
registry is never touched.

---

## 9. What could not be verified in this environment

* **No build, no test run, no GameTest, no client.** The wave was told not to run Gradle.
* **The type check is not a build.** The changed files were compiled with `javac` against the 1.21.1 merged
  jar of the leader's Gradle cache, but **without** the access widener of the project, so five pre-existing
  access errors of other files remain in that check. The changed files themselves produced no error. The
  resources, the mixins and the runtime behaviour were not checked at all.
* **The 1.7.10 vanilla `EntityAITarget` and `EntityAITasks`.** The vanilla classes are not part of the oracle
  (the oracle holds TC4's own sources only). Q2 therefore rests on (a) the **bytecode** of the shipped jar,
  which proves that the class extends the vanilla `EntityAITarget` and declares its own private
  `targetDistance`, (b) the oracle's own copy of that class, and (c) the fact that the subclass **overrides**
  `shouldExecute` completely, so the vanilla guard on the attack target never runs.
* **The 1.7.10 `GuiContainer` hit test.** The claim that the second (limit 1) ghost slot of the Fill core of
  the original is a no-op duplicate rests on the 1.7.10 `GuiContainer` taking the **first** slot under the
  mouse. That vanilla class is not in the oracle either; the consequence (the effective limit of the Fill
  ghost is 256) is the same in both readings, because the two slots share one inventory slot.
* **The client frames.** Every acceptance criterion of section 6 is unverified: the leader has to run the
  client and look at the frames.

---

## 10. Files changed by this wave

```
common/src/main/java/thaumcraft/common/entities/golems/GolemDoorGoal.java        (new, D10)
common/src/main/java/thaumcraft/common/lib/golems/GolemDoorRules.java            (new, D10)
common/src/main/java/thaumcraft/common/lib/golems/GolemRouteRules.java           (new, D4 of the ladder)
common/src/main/java/thaumcraft/common/entities/golems/GatherGolemGoals.java      (D10)
common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java       (D14 and the route rules)
common/src/main/java/thaumcraft/common/entities/golems/InventoryGolemGoals.java   (the route rules)
common/src/main/java/thaumcraft/common/container/ContainerGolem.java               (D11)
common/src/main/java/thaumcraft/client/gui/GuiGolem.java                          (D13)
common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBase.java      (D12)
common/src/test/java/thaumcraft/golems/GolemRouteRulesTest.java                   (new, authored)
common/src/test/java/thaumcraft/golems/GolemDoorRulesTest.java                    (new, authored)
docs/porting/scope-ledger.json                                                      (27 rows)
docs/analysis/2026-10-23-agent-w2golems.md                                          (this report)
```

Commits: `fix(golems)` ×3, `test(golems)` ×2, `docs(golems)` ×2.
