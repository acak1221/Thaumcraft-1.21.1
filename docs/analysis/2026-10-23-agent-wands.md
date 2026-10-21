# 2026-10-23 — agent `wands`: wands, foci, the vis system and the node network

Wave: 16-agent parallel port wave, lane `wands_nodes_and_vis` (18 open ledger units).
Worktree: `/home/cyberfox-kali/wt/wands` (branch `agent/wands`), base commit `be3c816f`.
Oracle: `/home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/**` (readable 1.7.10 reference of
TC4 4.2.3.5) and the original jar `Thaumcraft-1.7.10-4.2.3.5.jar` (read only).
Previous waves read first and not redone: `docs/analysis/2026-10-21-audit-a4-foci.md`,
`docs/analysis/2026-10-21-openTC4-wands.md`.

Not touched (other lanes): `common/src/main/java/thaumcraft/api/**`, `.../client/**`,
`.../common/entities/**`, `.../common/blocks/**`.

---

## 1. Result

| | |
|---|---|
| Units reviewed | 18 (13 `partial`, 5 `missing`) |
| Units moved to `parity_verified` | 2 — `ItemWandCasting`, `WandManager` |
| Defects found | 6 (4 gameplay, 2 presentation) |
| Defects fixed | 6 |
| Files changed (production) | 5 (2 changed, 3 new) |
| Test classes authored | 4 (10 tests) |
| Blockers | 2 cross-lane items for the api lane, 6 client rows stay open |

The three defects that the previous waves had already fixed were re-checked and are still fixed:
the sceptre holds 1.5× its rod (`WandMaterials.capacity(rod, sceptre)` = `rod * 150 / 100`) and
spends 10 % less vis (`WandVisDiscount.SCEPTRE_DISCOUNT`), the Trade focus has its left click
(`ItemFocusTrade.leftClick` + the `TradeFocusServerPlayerGameModeMixin`), and the focus pouch and
the wand consumption API are in place.

---

## 2. Defects found and fixed

### D1 — the public block trigger registry of the wand was dead code

* **Original:** `thaumcraft/common/items/wands/ItemWandCasting.java:389-390` —
  `if (WandTriggerRegistry.hasTrigger(bi, md)) { return WandTriggerRegistry.performTrigger(world,
  itemstack, player, x, y, z, side, bi, md); }`. The registry is consulted right after the
  `IWandable` contract and *before* the warded stone branch, and the first successful trigger wins.
  `ConfigRecipes.java:56-74` registers the eight events of TC4 itself (bookshelf, cauldron,
  obsidian/nether brick/iron bars, `blockStoneDevice` md 2, glass, `blockMetalDevice` md 9,
  `blockEldritch` md 0, `blockMetalDevice` md 3 and md 9).
* **Port (before):** `grep -rn "WandTriggerRegistry" common/src/main/java` returned only the
  declaration of the class itself. `ItemWandCasting.useOn` answered the wand click with a hard
  coded list of `tryWandInteraction`/`tryCreate`/`tryForm` calls and never asked the registry, so
  the whole documented add-on extension point (`thaumcraft.api.wands.WandTriggerRegistry`) could
  never fire.
* **Fix:** `common/src/main/java/thaumcraft/common/items/wands/ItemWandCasting.java` —
  `dispatchBlockTrigger(UseOnContext)` asks `WandTriggerRegistry.hasTrigger(state)` and then
  `WandTriggerRegistry.performTrigger(...)` and returns `SUCCESS`/`FAIL`, exactly at the position
  of the original (after `dispatchWandable`, before the warded stone branch). The public helper
  `ItemWandCasting.hasBlockTrigger(BlockState)` asks for the exact state *and* for the whole
  block, because the native registry is keyed by a block or by one exact block state.
* **Evidence:** `common/src/test/java/thaumcraft/wands/WandBlockTriggerTest.java`.

### D2 — a wand with an installed focus could still make a Thaumonomicon

* **Original:** `thaumcraft/common/items/wands/WandManager.java:459-478` (`createThaumonomicon`) —
  `if (wand.getFocus(itemstack) != null) { return false; } else { ... }`. The bookshelf is left
  alone while a focus is installed; the click does nothing.
* **Port (before):** `common/src/main/java/thaumcraft/common/items/wands/WandManager.java:26-42`
  (`canCreateThaumonomicon`) checked the player, the wand item, the bookshelf and the interaction
  permission, but not the focus. `createThaumonomicon` therefore consumed a bookshelf and spawned
  the Thaumonomicon with a focus installed.
* **Fix:** `WandManager.mayCreateThaumonomicon(boolean hasFocus)` and the focus gate in
  `canCreateThaumonomicon`. The refused click now falls through to the warded stone branch, which
  returns `PASS` for a bookshelf — the same "nothing happens" as the original.
* **Evidence:** `common/src/test/java/thaumcraft/wands/WandThaumonomiconTest.java`.

### D3 — the advanced node stabilizer was a dead feature

* **Original:** `thaumcraft/common/tiles/TileNode.java:879-897` (`checkLock`) — the block under the
  node gives the lock `1` for the node stabilizer (`blockStoneDevice` metadata nine) and the lock
  `2` for the advanced node stabilizer (`blockStoneDevice` metadata ten).
  `TileNode:644-650` — `if (this.getLock() == 1) { this.regeneration *= 2; } if (this.getLock() == 2)
  { this.regeneration *= 20; }`. `TileNode:775` — `nextInt(10000 / this.getLock()) == 42`;
  `TileNode:781` — `nextInt(12500 / this.getLock()) == 69`.
* **Port (before):** `common/src/main/java/thaumcraft/common/tiles/TileNode.java:513-526`
  (`checkLock`) only ever produced the lock one; `regenerationInterval()` (`:597-609`) multiplied
  only by two; `repairFading()` (`:674-680`) used the constant `nextInt(12500)`.
  The advanced stabilizer exists in the port as a real block
  (`ThaumcraftNodeDevices.ADVANCED_NODE_STABILIZER`, with the infusion recipe
  `data/thaumcraft/recipe/infusion/advanced_node_stabilizer.json`, research `NODESTABILIZERADV`)
  and `TileNodeStabilizer` even reports the lock two for it — but the node never saw it, so the
  advanced stabilizer changed nothing at all.
* **Fix:** the new `common/src/main/java/thaumcraft/common/lib/world/NodeStabilizerRules.java`
  (the lock, the interval multipliers and the two repair bounds) is used by `TileNode.checkLock`,
  `TileNode.regenerationInterval` and `TileNode.repairFading`. The advanced stabilizer now locks
  the node with the lock two: the regeneration interval is multiplied by **twenty** and the two
  repair rolls are halved (10000 → 5000 and 12500 → 6250).
* **Evidence:** `common/src/test/java/thaumcraft/wands/NodeStabilizerRulesTest.java`.

### D4 — every node host discharged, not only the invisible aura node

* **Original:** `thaumcraft/common/tiles/TileNode.java:559-560` —
  `if (this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord) == ConfigBlocks.blockAiry &&
  this.getLock() != 1)`, and `:577-579` — the target must be a `blockAiry` as well and must not be
  locked. The silverwood knot (`blockMagicalLog` metadata two) and the totem host
  (`blockCosmeticSolid` metadata eight) therefore never discharge and are never drained.
* **Port (before):** `common/src/main/java/thaumcraft/common/tiles/TileNode.java:554-591`
  (`handleDischarge`) checked only `nodeLock == 1` and the modifier, so a node in a silverwood
  knot or in the totem host discharged into its neighbours and was drained by them.
* **Fix:** the new `common/src/main/java/thaumcraft/common/lib/world/NodeDischargeRules.java` with
  `mayDischarge(auraNodeHost, lock, fading)` and `mayDischargeInto(auraNodeHost, otherLock)`,
  called by `TileNode.handleDischarge` for the node itself and for the target.
* **Evidence:** `common/src/test/java/thaumcraft/wands/NodeDischargeRulesTest.java`.

### D5 — the wand tooltip showed a consumption number that was not the real one

* **Original:** `thaumcraft/common/items/wands/ItemWandCasting.java:132-183` — for every stored
  aspect the tooltip shows `mod = getConsumptionModifier(stack, player, aspect, false)` and
  `consumption = mod * 100`, and the capacity line carries the average `tot /= num` of those
  modifiers (`:168-172`). `getConsumptionModifier` (`:232-256`) is the **aspect dependent** cap
  modifier (`:236-241`), the gear discount, the focus discount (only when not crafting) and the
  sceptre discount.
* **Port (before):** `common/src/main/java/thaumcraft/common/items/wands/ItemWandCasting.java:283-297`
  showed one line `tc.port.wand.base_cost` with `WandMaterials.capModifier(state.cap())`, the
  **base** modifier of the cap. That number was wrong for the copper cap (1.0 for order and
  entropy, 1.1 for everything else) and for every sceptre (the −0.1 was missing), and the average
  of the original was not shown at all.
* **Fix:** the tooltip now computes the real modifier per aspect with
  `WandVisDiscount.focusModifier(cap.modifierFor(tag), 0, frugal, sceptre)` and shows it, plus the
  average in the capacity line with the original key `tc.vis.costavg` and the per aspect key
  `tc.vis.cost`.
* **Documented deviation:** the 1.21.1 tooltip API has no player, so the equipment discount of the
  original (`:244-245`) cannot be shown; the per aspect listing of the original shift mode is
  always shown instead of the compact line.

### D6 — the "Base vis cost" tooltip line was a port-only invention

The same change as D5 removed the port-only `tc.port.wand.base_cost` line, because the number it
showed was wrong (D5). The language key stays in the file (the language parity test only compares
the two language files with each other).

---

## 3. Units touched

| Unit | Before | After | Why |
|---|---|---|---|
| `thaumcraft/common/items/wands/ItemWandCasting.java` | `partial` | `parity_verified` | D1, D2, D5, D6; the stale sceptre deviation of the old note is gone |
| `thaumcraft/common/items/wands/WandManager.java` | `partial` | `parity_verified` | D2; the architect area of the original `toggleMisc` is `WandArchitect.Area` |
| `thaumcraft/common/tiles/TileNode.java` | `partial` | `partial` | D3, D4 closed; the public `INode` contract stays open (api lane) |
| `thaumcraft/api/wands/WandTriggerRegistry.java` | `partial` | `partial` | D1 closed (the dispatch is live); the `hasTrigger(BlockState)` asymmetry stays open (api lane) |
| `thaumcraft/api/nodes/INode.java` | `partial` | `partial` | the game node still does not implement the interface (api lane) |
| `thaumcraft/api/aura/AuraHelper.java` | `partial` | `partial` | the TC6 per-chunk aura and the flux have no TC4 counterpart; the drain semantics differ from the 1.12 shim (api lane) |
| `thaumcraft/common/world/aura/AuraChunk.java` | `partial` | `partial` | TC4 has no per-chunk aura; the 1.12 handler refused the chunk state itself |
| `thaumcraft/common/world/aura/AuraHandler.java` | `partial` | `partial` | the legacy drain hit the vis network, the native projection drains the node at the position |
| `thaumcraft/client/fx/beams/FXBeamWand.java` | `partial` | `partial` | the original beam starts at the wand tip, the native per-focus particles at the player (client lane) |
| `thaumcraft/client/lib/REHWandHandler.java` | `partial` | `partial` | the radial and the area are ported; the selection overlay is a client duty |
| `thaumcraft/client/renderers/item/ItemWandRenderer.java` | `partial` | `partial` | one wand item renderer and a model catalogue instead of the general renderer; the use pose is missing (client lane) |
| `thaumcraft/client/renderers/item/WandPerspectiveModel.java` | `partial` | `partial` | the flat item model instead of the three dimensional wand (client lane) |
| `thaumcraft/client/renderers/tile/ItemNodeRenderer.java` | `partial` | `partial` | the native node has no item form; the harvest drops the wisp essence of the original `BlockAiry:265-279` (`NodeHarvest`) |
| `thaumcraft/client/fx/beams/WandEffectOrigin.java` | `missing` | `missing` | 1.12 helper without a TC4 counterpart; the beam-from-the-tip gap is the `FXBeamWand` row (client lane) |
| `thaumcraft/client/renderers/item/FirstPersonWandTipOrigin.java` | `missing` | `missing` | idem (client lane) |
| `thaumcraft/client/renderers/item/WandPoseMath.java` | `missing` | `missing` | idem (client lane) |
| `thaumcraft/client/renderers/item/WandUsePose.java` | `missing` | `missing` | idem (client lane) |
| `thaumcraft/client/renderers/item/WandUsePoseSampler.java` | `missing` | `missing` | idem (client lane) |

`counts` was not touched. The ledger was validated with the rules of
`scripts/porting/update_scope_ledger.py` (the script's own `validate_review` over all 1044 rows)
and the script's `--self-test` passes.

---

## 4. Files changed

Production:

* `common/src/main/java/thaumcraft/common/items/wands/ItemWandCasting.java` — the trigger dispatch,
  the public `hasBlockTrigger`, the tooltip.
* `common/src/main/java/thaumcraft/common/items/wands/WandManager.java` — the focus gate of the
  Thaumonomicon.
* `common/src/main/java/thaumcraft/common/tiles/TileNode.java` — the lock two, the regeneration
  multiplier, the fading repair bound, the discharge host.
* `common/src/main/java/thaumcraft/common/lib/world/NodeStabilizerRules.java` — **new**.
* `common/src/main/java/thaumcraft/common/lib/world/NodeDischargeRules.java` — **new**.

Tests:

* `common/src/test/java/thaumcraft/wands/NodeStabilizerRulesTest.java` — **new**.
* `common/src/test/java/thaumcraft/wands/NodeDischargeRulesTest.java` — **new**.
* `common/src/test/java/thaumcraft/wands/WandBlockTriggerTest.java` — **new**.
* `common/src/test/java/thaumcraft/wands/WandThaumonomiconTest.java` — **new**.

Ledger: `docs/porting/scope-ledger.json` (18 rows, `review` only).

---

## 5. Tests authored

All four classes are pure JUnit 5 (`org.junit.jupiter`), no Gradle, no Minecraft world; the two
that touch the vanilla registry use the `SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();`
pattern of the existing `thaumcraft/api/WandApiTest`.

| Class | Tests | What they assert |
|---|---|---|
| `NodeStabilizerRulesTest` | 4 | the lock of the ordinary and of the advanced stabilizer (1 and 2), the ×2 and ×20 regeneration multipliers over the 600/400/900/0 ladder, the 10000/lock and 12500/lock repair bounds, and that the advanced lock halves both rolls |
| `NodeDischargeRulesTest` | 3 | only the invisible aura node discharges, a stabilized or FADING node does not, and only an unlocked invisible aura node is drained |
| `WandBlockTriggerTest` | 1 | a registered trigger is found for the block of the click and the first successful trigger wins |
| `WandThaumonomiconTest` | 2 | a wand with a focus cannot make a Thaumonomicon; the tooltip consumption is the aspect dependent cap modifier with the sceptre and the frugal discounts |

**Execution status.** The leader runs Gradle; this wave did not. The four classes were however
compiled and executed with a hand-built classpath (`javac` + the JUnit 5 platform launcher from the
Gradle cache, against the pre-built `common/build/classes/java/main` of the base commit and the
mapped 1.21.1 jar):

* the four new classes: **10 tests, 10 successful, 0 failed**;
* the whole `thaumcraft.wands` and `thaumcraft.foci` test packages: **82 tests, 81 successful**;
  the single failure is `WandCraftCostTest.theCraftCostComponentHoldsEveryOriginalCraftCost`, which
  dies in `ThaumcraftWands.<clinit>` with `NoSuchMethodError: DeferredRegister.create(String,
  ResourceKey)` — the hand-built classpath carries the *intermediary* architectury jar, and the
  class is untouched by this wave;
* `thaumcraft.api.WandApiTest` and `thaumcraft.api.NodesApiTest`: **11 tests, 11 successful**.

The Gradle suite itself was **not** run; the numbers above come from the ad-hoc launcher and are
reported as such.

**Compile check.** The five changed production files and the four new test classes were compiled
with `javac` (Java 21) against the mapped 1.21.1 jar, the pre-built `common/build/classes/java/main`
of the base commit and the Gradle cache dependencies: **no error**. As a control, the whole
non-client tree of the base commit and of this branch were compiled with the same hand-built
classpath: **640 errors in both**, all of them pre-existing artifacts of that classpath (the
*intermediary* architectury jar and the missing access widener — `DeferredRegister.create`,
`class_3222`, `BlockEntitySupplier has private access`, `lastHurtByPlayerTime has protected
access`), and **none of them in a file of this wave**. The client tree could not be compiled at
all with that classpath, for the same reason.

---

## 6. What could not be fixed here

1. **`thaumcraft.api.wands.WandTriggerRegistry` (api lane).** `hasTrigger(BlockState)` only finds a
   trigger registered for that exact state, while `performTrigger` also accepts a trigger
   registered for the whole block. The original always agreed, because both were keyed by block
   and metadata. The wand works around it (`ItemWandCasting.hasBlockTrigger` asks for both), but
   the registry itself is owned by the api lane (`coremisc`).
2. **`thaumcraft.api.nodes.INode` (api lane).** The native game node does not implement `INode`: it
   keeps its own nested `NodeType`/`NodeModifier` enums and a UUID identity instead of the original
   `dim:x:y:z` string, and the `NodeAdapter` view throws for `setNodeType`/`setNodeModifier`.
   Closing it needs the node and the adapter to agree on the write path and on the identity, which
   is an api decision.
3. **`thaumcraft.api.aura.AuraHelper` (api lane).** The 1.12 `AuraHandler.drainVis` drained the
   vis **network** at the position; the native projection drains the node **at** the position.
4. **Six client rows.** The wand beam from the wand tip, the wand use pose, the three dimensional
   wand and the architect selection overlay are client renderer duties; `client/**` was outside
   this lane.

## 7. Server-crash class: dangling node links

Checked, because the brief asks for it explicitly. **No node or tile in this domain can be left
with a dangling link when its block is removed.**

* The vis network keeps its edges in `WeakReference`s
  (`thaumcraft/api/visnet/TileVisNode.java:21-22`) and detaches on retirement:
  `TileVisNode.setRemoved` (`:94-102`) calls `VisNetHandler.removeNode(this, true)`, and
  `TileVisNode.setLevel` (`:104-112`) removes the node from the network before it moves.
  `VisNetHandler.removeNode` (`:478-492`) detaches the raw parent/child edges of the node and of
  its whole descendant set (`detachRaw`, `:336-351`) and invalidates the route cache, so a removed
  node can never be reached again.
* `VisNetHandler.path` (`:274-294`) walks the parent chain with an identity set and refuses a
  cycle, a missing parent or a second edge, so a broken edge cannot loop.
* The ordinary node (`TileNode`) is not part of the vis graph: it holds no neighbour reference, it
  only *looks* at a neighbour block entity inside `handleDischarge`, and the lookup is guarded by
  `level.hasChunkAt`, `instanceof TileNode` and `other.current()`. A removed neighbour is simply
  not found.
* The node devices (`TileNodeConverter`, `TileNodeEnergized`, `TileNodeStabilizer`,
  `TileVisRelay`) hold their own state and no reference to another tile; the converter swaps the
  node for an energized node through the world, not through a stored pointer.

No crash-class defect was found in this domain.

## 8. Commits

* `2e8c9932` `fix(nodes): give the advanced node stabilizer its lock two and keep the discharge to the aura node`
* `6fbe6976` `fix(wands): dispatch the public block triggers, refuse the Thaumonomicon with a focus and show the real consumption`
* `0c5135cb` `test(wands): assert the original node lock, the discharge, the trigger dispatch and the Thaumonomicon rule`
* `f1133818` `docs(ledger): refresh the wands, nodes and vis verdicts of the 2026-10-23 wave`
