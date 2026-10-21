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

**No Gradle was run in this worktree.** Every test named below is *authored but not executed*.

---

## 1. Result

| | before | after |
|---|---|---|
| `partial` | 34 | 26 |
| `parity_verified` | 0 | 8 |
| `missing` | 0 | 0 |

Units moved to `parity_verified` (8):

| unit | why |
|---|---|
| `thaumcraft/api/entities/IGolemInfo.java` | the file does not exist in 4.2.3.5; every value the 1.12 interface exposed is reachable on the native entity |
| `thaumcraft/common/entities/ai/combat/AINearestAttackableTarget.java` | the retention of the Guard's target now follows the original (D3) |
| `thaumcraft/common/entities/ai/combat/AITarget.java` | the base class is dead code for the golems; the native now reproduces the two effective rules (D3) |
| `thaumcraft/common/entities/golems/ContainerGolem.java` | the Order colour buttons and the page arrows use the original ids (D1) |
| `thaumcraft/client/renderers/models/ModelGolem.java` | the native model is the original's box model |
| `thaumcraft/client/renderers/models/entities/ModelGolem.java` | the same file covers both original paths (the two oracle files are byte-identical apart from the package) |
| `thaumcraft/client/renderers/models/ModelGolemAccessories.java` | the native model is the original's accessory model and is now rendered (D4) |
| `thaumcraft/client/renderers/models/entities/ModelGolemAccessories.java` | the same file covers both original paths |

The other 26 rows keep `partial`; every one of them now carries the **exact** remaining difference in its
`review.notes` (section 5). `docs/porting/scope-ledger.json` was rewritten with the same JSON shape
(`json.dumps(..., ensure_ascii=False, indent=2)`, a byte-identical round trip on the untouched file), `counts`
was not touched, and

```
python3 scripts/porting/update_scope_ledger.py --check --original-root /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle
… "stale_review_fingerprints": 0
```

---

## 2. Defects found and fixed

### D1 — the Order colour button of the golem menu could only raise the colour

* **Original.** `oracle thaumcraft/common/entities/golems/ContainerGolem.java:96-109` — a menu button whose id is
  **below** the golem's slot count *lowers* the colour of that slot (`colour - 1`, and `-1` wraps to `15`); a
  button whose id is **from the slot count up to twice the slot count** *raises* it (`colour + 1`, and `15` wraps
  to `-1`). The page arrows are the buttons 66 and 67 (`:84-90`).
  `oracle thaumcraft/client/gui/GuiGolem.java:130-145` draws the colour chip as the 6x6 square of the texture at
  `(0,176)`, tinted with the original palette and centred in a 24x12 bar at `(96 + a/2*28, 4 + a%2*31)`;
  `:328-342` sends the lowering id `a + currentScroll*6` and the raising id `a + slots + currentScroll*6`;
  `:102-114` writes the colour **name** above the panel while the mouse is over that bar (and only when the golem
  carries Entropy, `getUpgradeAmount(5) > 0`).
* **Port (before).** `common/src/main/java/thaumcraft/common/container/ContainerGolem.java` used one synthetic
  button id `100 + i` per ghost, which only ever **raised** the colour, and the page buttons 60/61.
  `common/src/main/java/thaumcraft/client/gui/GuiGolem.java:38-44` drew a flat 8x5 chip **below** the ghost slot
  with the vanilla dye colour, and the pager was a `<`/`>` text in the middle of the panel.
* **Effect.** The player could not step the marker colour *down*; the chip had the wrong place, the wrong size and
  the wrong palette (the vanilla dye colours instead of the original's `Utils.colors`), and the colour name was
  never shown.
* **Fix.** `ContainerGolem.java:242-260` — `nextColour(int, int)` reproduces the original's cycle and
  `adjustColor(int)` accepts the original's id ranges (`[0, slotCount)` lowers, `[slotCount, 2*slotCount)` raises);
  the page arrows now use 66/67 (`:30`).
  `GuiGolem.java:39-52` — the original's 24x12 bar and the 6x6 tinted square (the tint uses the original's
  `Utils.colors`); `:63-68` the original's texture arrows; `:89-110` the original's half-size page counter and the
  colour name of the hovered bar; `:213-224` the two click regions of the bar with the original's ids.
  `assets/thaumcraft/lang/en_us.json` — the seventeen colour names (`tc.port.golem.color.any` and
  `tc.port.golem.color.0 … 15`).
* **Test.** `common/src/test/java/thaumcraft/golems/GolemMenuOrderTest.java` (authored, not executed).
  The GameTest `common/src/gameTest/java/thaumcraft/common/testing/GolemColorsRuntimeChecks.java:156-211` was
  updated to the new ids and now also checks the lowering button (the any colour lowered is fifteen).

### D2 — the golem preview stood in the wrong corner of the menu

* **Original.** `oracle thaumcraft/client/gui/GuiGolem.java:267-317` — the golem is drawn in the **320x240
  viewport of the screen centre**, which is the centre of the panel.
* **Port (before).** `common/src/main/java/thaumcraft/client/gui/GuiGolem.java` drew it in a 36x62 box in the
  **top left** corner of the panel.
* **Fix.** `GuiGolem.java:76-84` — the preview is centred on the panel (the box `leftPos+63, topPos+52` …
  `leftPos+113, topPos+114`).

### D3 — the Guard kept a target the original would have dropped

* **Original.** `oracle thaumcraft/common/entities/ai/combat/AINearestAttackableTarget.java` — the class
  **extends the vanilla `net.minecraft.entity.ai.EntityAITarget`** and declares its **own private**
  `float targetDistance` (`:29`). The jar confirms it:
  `javap -p thaumcraft/common/entities/ai/combat/AINearestAttackableTarget.class` →
  `extends net.minecraft.entity.ai.EntityAITarget` and `private float targetDistance;` (the parent's field is
  `protected float targetDistance`).
  The subclass **never overrides the continuation rule** — the oracle's class has only `func_75250_a`
  (`shouldExecute`, `:46-60`) and `func_75249_e` (`startExecuting`, `:62-65`). The inherited rule compares the
  squared distance with the **parent's** distance field, and the parent keeps its own distance at `0.0F` because
  the vanilla constructor is `EntityAITarget(EntityCreature, boolean, boolean)`. The goal therefore never
  continues: the Guard's attack target is the **nearest valid target at all times**
  (`oracle …/EntityGolemBase.java:302` registers it with a chance of zero, so there is no random gate).
  TC4's *own* `AITarget` (`oracle …/combat/AITarget.java:30-70`) has the distance check and the sixty tick
  unseen counter, but its only golem subclass `AIHurtByTarget` overrides the continuation rule
  (`oracle …/combat/AIHurtByTarget.java:30-33`: `getRevengeTarget() != null && getRevengeTarget() != entityPathNavigate`),
  so **neither rule ever applied to a golem**.
* **Port (before).** `common/src/main/java/thaumcraft/common/entities/golems/GuardGolemGoals.java:231-270` — the
  nearest goal kept the target while it was a valid target **and** within the golem's own range **and** unseen for
  at most sixty ticks. Two of those three rules belong to no original rule of this goal: the range is already
  inside `isValidTarget` (the original's `func_110176_b`, the distance **from the home**, which the native
  reproduces in `EntityGolemBase.isGuardTarget`), and the sixty tick counter is the rule of TC4's own `AITarget`,
  which this goal does not use.
* **Effect.** A Guard that had picked a target kept hitting it after a **closer** valid target had appeared, and
  it dropped a target it could not see for three seconds — both of which the original does not do.
* **Fix.** `GuardGolemGoals.java:231-274` — the goal keeps the target while it is the golem's target and still a
  valid target, and `tick()` re-picks the nearest valid target every tick (`nearestGuardTarget`, `:266-274`).
  The observable rule of the original (the nearest valid target at all times) is reproduced without the
  selector churn of a goal that never continues.
* **Test.** No JUnit test: the rule needs a live level and a live entity scan. The GameTest
  `common/src/gameTest/java/thaumcraft/common/testing/GolemGuardTargetsRuntimeChecks.java` (the four real Guards
  that walk and strike) covers the behaviour and is executed by the leader's GameTest suite.

### D4 — the worn accessories of a golem were never drawn

* **Original.** `oracle thaumcraft/client/renderers/entity/RenderGolemBase.java:145-149` — the accessories are a
  **second pass over the golem model** with the texture `textures/models/golem_decoration.png`, drawn whenever
  the golem wears something or is advanced; `:98-152` of
  `oracle thaumcraft/client/renderers/models/ModelGolemAccessories.java` draws the fourteen worn pieces, the bow
  tie and the head jar with blending and every other piece cut out.
  `:116-141` of the same renderer draws the **installed upgrades** as small flat icons in a row on the golem's
  back (one per upgrade *slot*, so a free slot shows the blank `golem_upgrade_empty` icon).
* **Port (before).** `common/src/main/java/thaumcraft/client/renderers/models/entities/ModelGolemAccessories.java`
  was a complete and faithful port of the accessory model — **but no renderer ever used it** (the only reference
  in the whole tree was its own declaration). Every decoration of every golem (fez, top hat, glasses, bow tie,
  visor, plate, mace, dart gun) was invisible, and the upgrade icons were missing as well.
* **Fix.** `common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBase.java:122-152` — the new
  `UpgradesLayer` (the original's row of icons: `-0.05 - 0.08*(slots-1)/2 + 0.08*a`, `-1.106`, `0.099`, the scale
  `0.1`, the flat quad) and the new `AccessoriesLayer` (`:154-183`: the cut-out pass and the blended pass, exactly
  as the original blends the bow tie and the head jar).
  `common/src/main/java/thaumcraft/client/renderers/models/entities/ModelGolemAccessories.java:103-160` — the
  selection of the pieces is now a per-pass method (`setBlendPass`), so the two passes can be drawn with the
  right render type.
* **Remaining difference.** The original draws a blank icon in every **free** upgrade slot; the native has no item
  for that blank icon and draws the installed upgrades only (at the original's positions).
* **Test.** `GolemMenuOrderTest.theAccessoryModelKeepsTheOriginalPartsAndHeadOffset` (authored, not executed).

### D5 — OpenTC4 and the 1.12 reference differ from the 4.2.3.5 oracle in the door goal

* **Oracle (4.2.3.5).** `oracle thaumcraft/common/entities/ai/misc/AIDoorInteract.java:27-83` — the scan is
  `Math.min(pathLength + 2, currentPathIndex)`, the door is the **oak** door or the fence gate, and there is no
  "one block above" test.
* **1.12 reference (not the authority).** `src/main/java/thaumcraft/common/entities/ai/misc/AIDoorInteract.java:38`
  — the scan is `Math.min(currentPathIndex + 2, pathLength)`, the door is *any* wooden door and the block above is
  tested as well.
* The ledger and the port follow the **oracle**. The door goal itself is not ported (D6 of section 3).

---

## 3. Findings that were verified and left alone (with the reason)

### D6 — the door goal of the original is not a native door goal

* **Original.** `oracle …/ai/misc/AIDoorInteract.java:27-83` scans the **next points of the current path** within
  a squared distance of 2.25 for an oak door or a fence gate, keeps it open while the golem passes it (the
  direction dot product, `:63-72`), and falls back to the golem's own block. `AIOpenDoor.java:16-58` opens the
  door and the fence gate and closes them again after a twenty tick window.
* **Port.** `common/src/main/java/thaumcraft/common/entities/golems/GatherGolemGoals.java:36-39` — the vanilla
  `OpenDoorGoal`, which opens the door **in front of** the golem and knows no fence gate.
* **Why not changed.** A native door goal would have to reproduce the 1.7.10 path API, the fence gate's own
  opening logic and the 1.7.10 sound. This wave may not run Gradle, so a new goal of that size could not be
  compiled or tested here; a broken goal would be worse than the documented difference. The difference is
  recorded in the two door rows.

### D7 — the Fill/Empty/Sorting goal ladder is one route goal per core, not five goals per core

* **Original.** `oracle thaumcraft/common/entities/golems/EntityGolemBase.java:269-341` — the Fill core
  registers `AIHomeReplace 0`, `AIHomePlace 1`, `AIHomeDrop 2`, `AIFillTake 3`, `AIFillGoto 4`; the Empty core
  `AIHomeReplace 0`, `AIEmptyPlace 1`, `AIEmptyDrop 2`, `AIEmptyGoto 3`, `AIHomeTake 4`; the Sorting core
  `AIHomeReplace 0`, `AISortingPlace 1`, `AISortingGoto 3`, `AIHomeTakeSorting 4`. All of them take the mutex
  bits three, so only one of them runs and the lowest number wins.
* **Port.** `common/src/main/java/thaumcraft/common/entities/golems/InventoryGolemGoals.java:33-37` — one route
  goal per core (Fill 4, Empty 3, Sorting 3) plus a server tick (`:65-315`) that performs the other four actions.
* **What was checked.** The *observable* rules of the five goals of a core were compared one by one with the
  oracle and are reproduced: the five-tick `golemDelay` cadence of the replace, place, take and goto goals and
  **not** of the drop goal (`oracle …/ai/inventory/AIHomeDrop.java:23-35` has no delay gate), the squared distance
  of five to the home, the adjacency range of four, the range of the goto, the budget of 200 ticks and the random
  detour within two and one (`oracle …/ai/inventory/AIFillGoto.java:96-108`), the "no home container → drop the
  stock at the home" rule with the original's motion and pickup delay of ten
  (`oracle …/ai/inventory/AIHomeDrop.java:60-77`), the first-marked-block fallback of the Empty route without a
  range and without a home exclusion, and the Empty preference of an adjacent container that accepts the stock
  over the drop at a marked block.
* **Why not changed.** Restoring fifteen separate goals is a large rewrite of a working implementation that this
  wave could not compile or test. The structural difference is written into the twelve rows.

### D8 — the Decanting and the Alchemy golem are water only

* **Original.** `oracle …/ai/fluid/AILiquidGather.java:59-83` — the marked **fluid handler** of *any* fluid and
  the marked **fluid block** of *any* fluid, with an explicit water and lava fall back;
  `oracle …/entities/golems/GolemHelper.java:225-266` — `findJarWithRoom` accepts a marked jar, a marked
  reservoir **and any other marked `IEssentiaTransport`**.
* **Port.** `common/src/main/java/thaumcraft/common/items/golems/GolemData.java:384-386` — only a pristine
  water bucket is a valid Decanting filter; `LiquidGolemGoals.java:60-140` gathers source water only;
  `AlchemyGolemGoals.java:118-140` accepts a marked jar or a marked reservoir.
* **Why not changed.** The native's fluid cargo is a single water volume in millibuckets
  (`GolemWaterCargo`), its own filter, its own bucket rendering and its own menu ghost. Widening it to every
  registered fluid is a content change across the whole Decanting subsystem (and the essentia side would need the
  same widening for every `IEssentiaTransport` block of the tree), not a defect fix. The exact set of blocks the
  native refuses is listed in the row.

### D9 — the Use core still performs the default use only

* **Original.** `oracle …/ai/interact/AIUseItem.java:39-196` can use **any** item on **any** marked block.
* **Port.** `common/src/main/java/thaumcraft/common/entities/golems/GolemUsePlatform.java` performs the original's
  default use (wheat seeds on farmland, a bare hand on a lever or a button) and reproduces the original's numbers
  (the click delay `ticksExisted + (5 - ticksExisted) * 3`, the squared distance of four, the budget of 200 ticks,
  the 10000 ms timeout).
* **Why not changed.** The Use core needs a whitelist of items and blocks that the original takes from the marked
  block and the carried item; that is a content decision, not a defect.

---

## 4. What the leader must verify on the real client

The client is runnable now, but only the leader can run it. For every client unit this wave touched, the
acceptance criteria are:

**A. The golem menu (`thaumcraft/client/gui/GuiGolem.java`, `…/container/ContainerGolem.java`)**

1. Open the menu of a Fill golem that carries the **Order** upgrade (an `golem_upgrade_order` item).
   Above each of the six ghost slots there must be a **24x12 bar** (transparent in the texture) with a **6x6
   square** in its middle; the square must show the **original's** colour of the slot (white `0xF0F0F0`, orange
   `0xEB8844`, …, black `0x1E1B1B`) and must be **absent** for a slot with the "any marker" colour.
2. Clicking the **left third** of that bar must lower the colour (any → black → dark grey → …) and the **right
   third** must raise it (any → white → orange → …); the square must follow the click after the server round trip.
3. Moving the mouse over the bar must show the **name** of the colour **above** the panel ("Any color", "White",
   "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink", "Gray", "Light Gray", "Cyan", "Purple", "Blue",
   "Brown", "Green", "Red", "Black"). The name appears only when the golem carries **Entropy**
   (`golem_upgrade_entropy`), exactly as in the original.
4. A golem with more than six slots (a Fill golem with a **Fire** upgrade) must show the **texture arrows** at
   the bottom right of the panel (at x = 111 and x = 135 of the panel) and the half-size page counter
   "1/2" at the right of them. The arrow of the end that cannot be used must be the dimmed sprite.
5. The **golem preview** must stand in the **centre** of the panel, not in its top left corner.
6. The Guard switches (Monsters/Animals/Players/Creepers), the Entropy switches and the "Precise amount" box
   must keep their original places; they were not touched.

**B. The golem renderer (`thaumcraft/client/renderers/entity/RenderGolemBase.java`)**

7. A golem with a **top hat** (or any other decoration) must show it on its head, a golem with the **bow tie**
   must show it at the neck, a golem with the **plate** must show the armour on its body, the **glasses** and the
   **visor** on the face, the **fez** on the head, the **mace** in the right and the **dart gun** in the left
   hand. Before this wave none of them was drawn at all.
8. An **advanced** golem must show the brain and the jar on its head and the darker "evil head" over its face.
9. The bow tie and the head jar must be **semi transparent**; every other piece must be solid.
10. The installed **upgrades** must show as a row of small icons on the golem's **back** (the original draws one
    per slot, the native only the installed ones — the free slots stay empty).
11. The golem's body texture, the sway while it walks, the 0.4 model scale, the head angle while it boots up
    and the red/green healing tint must be unchanged.

---

## 5. The ledger rows and their exact remaining difference

| unit | coverage | remaining difference |
|---|---|---|
| `api/entities/IGolemInfo.java` | `parity_verified` | none (the file is not in 4.2.3.5; the values are on the native entity) |
| `client/gui/GuiGolem.java` | `partial` | the preview camera: the original's fixed 320x240 viewport with a yaw of −20 and a pitch of −5, the native uses the vanilla inventory preview helper |
| `client/renderers/entity/RenderGolemBase.java` | `partial` | the blank "empty upgrade" icon of the free upgrade slots |
| `client/renderers/entity/RenderGolemBobber.java` | `partial` | the texture of the quad: the 1.7.10 particle atlas does not exist in 1.21.1, the native uses the fishing hook texture |
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
| `ai/inventory/AIEmptyDrop.java` … `ai/inventory/AISortingPlace.java` (12 rows) | `partial` | the structure: five selector entries per core with the numbers 0..4, the native has one route goal per core |
| `ai/misc/AIDoorInteract.java` | `partial` | the path point scan, the oak door and the fence gate; the native uses the vanilla `OpenDoorGoal` |
| `ai/misc/AIOpenDoor.java` | `partial` | the fence gate; the vanilla goal knows only the door in front of the golem |
| `common/entities/golems/ContainerGolem.java` | `parity_verified` | none |
| `common/entities/golems/EntityGolemBase.java` | `partial` | the inventory goal structure, the door goal and the fluid set |
| `common/entities/golems/GolemHelper.java` | `partial` | the original's one helper class is split into pure rule classes of the native tree |

---

## 6. Tests authored (NOT executed)

No Gradle was run in this worktree; the leader has to run `:common:test` and the GameTest suite.

* **new** `common/src/test/java/thaumcraft/golems/GolemMenuOrderTest.java` — five tests: the original's Order
  colour cycle (the lowering button turns "any" into fifteen, the raising button turns fifteen into "any"), the
  original's menu button ids (66/67, 51..54, 55..57), the original's sixteen marker colours and their names, the
  seventeen language keys of the colour names, and the fourteen pieces of the accessory model with the head
  offset of thirty.
* **changed** `common/src/gameTest/java/thaumcraft/common/testing/GolemColorsRuntimeChecks.java:156-211` — the
  colour buttons of the menu use the original's ids, and the lowering button is now covered.

---

## 7. What could not be verified in this environment

* **No build, no test run, no GameTest, no client.** The wave was told not to run Gradle.
* **The 1.7.10 vanilla `EntityAITarget`.** The vanilla classes are not part of the oracle (the oracle holds TC4's
  own sources only). D3 therefore rests on (a) the **bytecode** of the shipped jar, which proves that the class
  extends the vanilla `EntityAITarget` and declares its own private `targetDistance`, (b) the oracle's own copy of
  that class (`oracle …/combat/AITarget.java:30-70`), which shows the distance rule and the sixty tick counter
  that the vanilla base carries, and (c) the fact that TC4's only golem subclass of that copy overrides the
  continuation rule. The conclusion — that the nearest goal of the original never continues and the golem's
  target is the nearest valid target at all times — is written down in the two combat rows with its evidence.
* **The 1.7.10 `GuiContainer` hit test.** The claim that the second (limit 1) ghost slot of the Fill core of the
  original is a no-op duplicate rests on the 1.7.10 `GuiContainer` taking the **first** slot under the mouse. That
  vanilla class is not in the oracle either; the consequence (the effective limit of the Fill ghost is 256) is
  the same in both readings, because the two slots share one inventory slot.
* **The client frames.** Every acceptance criterion of section 4 is unverified: the leader has to run the client
  and look at the frames.

---

## 8. Files changed by this wave

```
common/src/main/java/thaumcraft/client/gui/GuiGolem.java                          (D1, D2)
common/src/main/java/thaumcraft/common/container/ContainerGolem.java               (D1)
common/src/main/java/thaumcraft/common/entities/golems/GuardGolemGoals.java        (D3)
common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBase.java       (D4)
common/src/main/java/thaumcraft/client/renderers/models/entities/ModelGolemAccessories.java (D4)
common/src/main/resources/assets/thaumcraft/lang/en_us.json                        (D1)
common/src/test/java/thaumcraft/golems/GolemMenuOrderTest.java                      (new, authored)
common/src/gameTest/java/thaumcraft/common/testing/GolemColorsRuntimeChecks.java   (D1)
docs/porting/scope-ledger.json                                                      (34 rows)
docs/analysis/2026-10-23-agent-w2golems.md                                          (this report)
```

Commits: `fix(golems)` ×3, `test(golems)` ×1, `docs(golems)` ×1.
