# Wave 4 — `w4alchemy`: the aspect / essentia / alchemy family (2026-10-23)

Worktree: `/home/cyberfox-kali/wt/w4alchemy` (branch `agent/w4alchemy`).
Unit list: `/home/cyberfox-kali/units/w4alchemy.txt` (7 rows).
Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 859/859 fingerprints).
`/home/cyberfox-kali/OpenTC4` was used only as a second opinion; the jar won everywhere.

**No Gradle was run.** Every test below is **authored but NOT executed**; the leader runs the build, the
JUnit suite, the GameTest suite and the client.

---

## 1. Result

| unit | before | after |
|---|---|---|
| `thaumcraft/api/aspects/AspectSourceHelper.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/tile/ItemJarNodeRenderer.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/tile/TileJarRenderer.java` | partial | **parity_verified** |
| `thaumcraft/common/blocks/ItemJarNode.java` | partial | **parity_verified** |
| `thaumcraft/common/tiles/TileCrucible.java` | partial | **parity_verified** |
| `thaumcraft/common/tiles/TileJarBrain.java` | partial | **parity_verified** |
| `thaumcraft/common/tiles/TileJarNode.java` | partial | **parity_verified** |

`python3 scripts/porting/update_scope_ledger.py --check --original-root
/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`; the diff touches
only the seven units of this lane and the mechanical `counts` block (the disposition of one unit changed
from `exclude` to `port`, so the tool regenerated the counts — it was not hand edited).

`python3 scripts/porting/extract_tc4_definitions.py --oracle
/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle --check` → `{"aspects": 48, …, "mode": "check"}`.

---

## 2. The defects that were found and fixed

### 2.1 The essence flow packet of the public essentia source API was missing

* **Oracle** `thaumcraft/common/lib/network/fx/PacketFXEssentiaSource.java:20-78` — the packet carries
  the position of the **consumer**, the three **byte deltas** to the source and the colour of the
  aspect; the client rebuilds the source with `tx = x - dx` (`:65-67`) and puts the flow into a map with
  the key `x:y:z:tx:ty:tz:color` and a lifetime of **fifteen** ticks (`:68-76`).
* **Oracle** `thaumcraft/common/lib/events/EssentiaHandler.java:54` — the only sender: the packet goes
  to the players within the **32** blocks of the consumer
  (`NetworkRegistry.TargetPoint(dim, x, y, z, 32.0)`).
* **Oracle** `thaumcraft/client/lib/ClientTickEventsFML.java:249-269` — the client tick draws the trail
  of every running flow; the trail starts at the **source** and flies to the **consumer**, the
  destination of an infusion matrix is **one block lower**, the trail keeps its full size while more
  than five ticks are left and shrinks with `ticks * ticks / 25` over the last five ticks, and the
  phase of the mote counts back by the missing ticks.
* **Oracle** `thaumcraft/client/fx/particles/FXEssentiaTrail.java:24-212` — the trail itself: the size
  `(sin(count/2) * 0.1 + 1) * scale`, the lifetime `base/2 + rand(base)` with `base = distance * 30`,
  the three speeds, the colour darkened by twenty per cent, the gravity `0.01 * 0.2`, the push out of
  the solid blocks, the `0.985` damping, the `0.05` clamp and the homing `0.01 / min(1, distance)`.
* **Port before** `common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java` drained the
  source and returned `true` — the packet of the original never left the server. The native had the
  effect of the **infusion matrix** only (`EssentiaHandler.drainEssentia(TileInfusionMatrix, Aspect)`,
  a different original packet, `PacketFXInfusionSource`).
* **Fix** `common/src/main/java/thaumcraft/common/lib/network/EssentiaFlowFxPayload.java` (new) keeps
  the byte deltas of the original and rebuilds the source the way the original client did;
  `common/src/main/java/thaumcraft/common/lib/network/EssentiaFlowFxNetworking.java` (new) is the
  sender with the original 32 block target point (measured from the raw block coordinate, like the
  1.7.10 target point);
  `common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:40-52` sends it after a
  successful take; `common/src/main/java/thaumcraft/client/fx/EssentiaFlowClientEffects.java` (new) is
  the client flow map with the original key, the fifteen ticks, the scale rule, the phase rule and the
  lower destination of an infusion matrix;
  `common/src/main/java/thaumcraft/client/fx/particles/EssentiaTrailParticle.java` (new) is the trail
  of the original with every number of the oracle. The payload is registered in
  `common/src/main/java/thaumcraft/common/Thaumcraft.java` and the receiver in
  `common/src/main/java/thaumcraft/client/ClientSetup.java`.

### 2.2 The head turn of the brain jar was rotated around the wrong axis

* **Oracle** `thaumcraft/client/renderers/tile/TileJarRenderer.java:166-167` —
  `glRotatef(f3 * 180 / pi, 0, 1, 0)` and `glRotatef(-90, 0, 1, 0)`. The signature of `glRotatef` is
  `(angle, x, y, z)`: the axis of **both** rotations is the **Y** axis, so the brain turns around the
  vertical axis and is then laid on its side.
* **Port before** `common/src/main/java/thaumcraft/client/renderers/tile/TileJarBrainRenderer.java`
  (written by the `w3clientfx` wave) rotated the head around the **Z** axis first and then around the
  Y axis — the head was tilted out of the horizontal plane. The javadoc of the wave repeated the
  misreading.
* **Fix** the turn is now `Axis.YP` twice, the body was extracted into the testable
  `TileJarBrainRenderer.applyTurn(PoseStack, float)`, and the stale javadoc of the wave (and of
  `common/src/test/java/thaumcraft/client/renderers/tile/JarRendererParityTest.java`) was corrected.

### 2.3 The client event nine of the node jar was missing

* **Oracle** `thaumcraft/common/tiles/TileJarNode.java:204-219` — `receiveClientEvent(9, …)` answers
  on the client with a burst of block sparkles over the whole jar: three blocks in X, **four** in Y and
  three in Z, so **thirty six** sparkles, each with **five** particles and the special random grey
  colour `-9999`, and it sets `animate = System.currentTimeMillis() + 1000L`.
* **Oracle** `thaumcraft/common/items/wands/WandManager.java:322` — the server sends that block event
  when the wand ritual traps a node in a jar.
* **Port before** `common/src/main/java/thaumcraft/common/tiles/TileJarNode.java` had no
  `triggerEvent` at all.
* **Fix** the tile answers the event nine (the timer of the original), and
  `common/src/main/java/thaumcraft/client/ThaumcraftEldritchClient.java` draws the burst of thirty six
  sparkles through the shared `BlockSparkleClientEffects.burst(...)`.
* **The growth itself is dead code in the original** (verified, not assumed): oracle
  `TileJarRenderer.java:107-115` applies `GL11.glScalef(size, size, size)` **after** every draw call
  of that renderer and pops the matrix immediately, so the scale never reaches a single vertex. The
  native tree therefore does **not** grow the node — exactly like the original. The ledger note of the
  `w3rest` wave ("the node jar grows to three times its size for one second after the wand hit") was
  wrong on that point and is corrected in the ledger.

### 2.4 The node jar emitted the wrong light level

* **Oracle** `thaumcraft/common/blocks/BlockJar.java:87-90` sets the light value `0.66`, which the
  1.7.10 setter stores as `(int)(15 * 0.66) = 9`; `:314-320` overrides it with **eleven** for the node
  jar (metadata 2).
* **Port before** the native `jar_node` block emitted the nine of the ordinary jar.
* **Fix** `ThaumcraftEldritchBlocks.JAR_NODE` now emits eleven.

### 2.5 The brain jar had no comparator output and no green particle

* **Oracle** `thaumcraft/common/blocks/BlockJar.java:340-351` — the comparator reads the share of the
  stored experience (`floor(xp / xpMax * 14)`) plus one when the jar holds anything at all.
* **Oracle** `thaumcraft/common/blocks/BlockJar.java:322-334` — a **full** brain jar throws the green
  spell particle at `(x + 0.3 + rand * 0.4, y + 0.9, z + 0.3 + rand * 0.4)` with the colour
  `(0, 0.4 + rand * 0.1, 0.3 + rand * 0.2)` and the alpha `0.5`.
* **Port before** the native `JarBlock` of the eldritch family had neither.
* **Fix** both are in `common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java`.
* **Forced 1.21.1 removal** the enchantment power bonus of the original (`BlockJar.java:306-312`, the
  value 2.0 for a brain jar) has **no** 1.21.1 counterpart: the enchanting table lost its power bonus
  and the method does not exist in 1.21.1.

### 2.6 The item form of the node jar did not exist at all

* **Oracle** `thaumcraft/common/blocks/ItemJarNode.java:56-214` — the item of the original carries the
  aspects, the node type, the node modifier and the identity of the stored node and publishes them
  with `setAspects`, `setNodeAttributes`, `getNodeType`, `getNodeModifier` and `getNodeId`; it is an
  `IEssentiaContainerItem` and its stack limit is **one** (`:63-64`).
* **Oracle** `thaumcraft/common/blocks/ItemJarNode.java:133-145` — the placement hands the aspects and
  the node attributes to the placed tile; the base of the stored node is the aspects themselves.
* **Oracle** `thaumcraft/common/blocks/ItemJarNode.java:81-98` — the tooltip names the node type, the
  node modifier and the aspects (an undiscovered aspect is the unknown marker).
* **Oracle** `thaumcraft/common/blocks/BlockJar.java:184-193` — the broken node jar drops the item with
  those four pieces of data.
* **Oracle** `thaumcraft/common/config/ConfigResearch.java:379-382` — the research **NODEJAR** shows
  that item with the four primal aspects of forty units and a normal node type.
* **Port before** the native had no item form: the node jar was a plain block item and the drop
  carried the raw block entity data.
* **Fix** `common/src/main/java/thaumcraft/common/blocks/ItemJarNode.java` (new) is the item of the
  `jar_node` block with the published contract of the original, the stack limit one and the tooltip of
  the original; the drop of the node jar now writes the data with that API
  (`ThaumcraftEldritchBlocks.JarBlock.getDrops`); the icon of the NODEJAR research resolves to the
  item with the four primal aspects (`ResearchIconDisplay`).
* **Forced 1.21.1 change** the data of the item lives in the **block entity data component** and the
  key names of the node attributes are the key names of the **tile** (`type`, `modifier`, `nodeId`),
  because the 1.21.1 placement reads the block entity data component back into the tile
  (`BlockItem.updateCustomBlockEntityTag` → `BlockItem.updateBlockEntityComponents`). The aspects keep
  the key `Aspects` of the original. The published methods, their names and their values are the
  original ones.

### 2.7 The item renderer of the node jar did not exist

* **Oracle** `thaumcraft/client/renderers/tile/ItemJarNodeRenderer.java:36-87` — the item draws the
  node of the jar in **three crossed planes** at `(0.5, 0.4, 0.5)`
  (`glRotatef(90, 0, 1, 0)` and `glRotatef(90, 1, 0, 0)`, so around Z and around X) and then the glass
  shell of the jar with the alpha blend of the original.
* **Port before** there was no `ItemJarNodeRenderer` in the native tree; the item was the ordinary jar
  model.
* **Fix** `common/src/main/java/thaumcraft/client/renderers/tile/ItemJarNodeRenderer.java` (new) draws
  the three crossed planes with the shared node body of `TileNodeRenderer` (the new
  `drawItemNode(...)` is the body without the camera rotation of the world) and the shell of the
  block model. The renderer is registered on both loaders:
  `fabric/…/ThaumcraftFabricClient.java` (`BuiltinItemRendererRegistry`) and
  `neoforge/…/ThaumcraftNeoForgeClient.java` (`RegisterClientItemRenderersEvent`).

### 2.8 The crucible had no fluid tank and no player crafting event

* **Oracle** `thaumcraft/common/tiles/TileCrucible.java:61-71,373-410` — the crucible is a Forge
  `IFluidHandler` with a tank of **one thousand millibuckets** that accepts **water only**; the fill
  marks the tile dirty **even when the tank refuses it**, and the drain of a named fluid refuses a
  stack that is not exactly the content of the tank (the Forge comparison of 1.7.10 compares the
  amounts as well).
* **Oracle** `thaumcraft/common/tiles/TileCrucible.java:253-259` — the craft fires the player crafting
  event of the original for the player who threw the item, with the fake inventory of the input item
  as the matrix; `thaumcraft/common/lib/events/EventHandlerWorld.java:149-168` answers it with the
  sticky warp of the crafted output.
* **Port before** the native tile had a private `water` counter and nothing else: no other mod could
  read or fill the tank, and the craft never notified the player, so the sticky warp of a crafted
  output never reached the player who threw the item.
* **Fix** the rules live in the shared tile
  (`common/src/main/java/thaumcraft/common/tiles/TileCrucible.java`, the pure helpers
  `acceptedFill(current, millibuckets)` and `drained(current, maxDrain)`); Fabric publishes the tank
  through the transfer API of Fabric
  (`fabric/src/main/java/thaumcraft/fabric/fluid/FabricCrucibleStorage.java`, eighty one droplets for
  one millibucket) and NeoForge through the `Capabilities.FluidHandler.BLOCK` capability
  (`neoforge/src/main/java/thaumcraft/neoforge/NeoForgeCrucibleFluidHandler.java`, where the unit of
  the original and the unit of NeoForge are the same). The craft now fires the shared
  `PlayerEvent.CRAFT_ITEM` of the port with a one slot `SimpleContainer` that holds the input item,
  exactly like the infusion matrix does — the sticky warp of the output reaches the player again.

---

## 3. The public contracts

* `thaumcraft/api/aspects/AspectSourceHelper` — the published `drainEssentia` and `findEssentia` were
  already the original ones (oracle `AspectSourceHelper.java:21-47`, the reflective call of the
  handler); the native calls the handler directly because it owns the search. **Unchanged.**
* `thaumcraft/common/blocks/ItemJarNode` — **reproduced** with the original names
  (`setAspects`, `setNodeAttributes`, `getNodeType`, `getNodeModifier`, `getNodeId`) and the original
  `IEssentiaContainerItem` contract.
* `thaumcraft/api/aspects/IEssentiaTransport`, `IEssentiaContainerItem`, `Aspect`, `AspectList` — the
  prompt asked for the contracts of the public API of this family: they are already `parity_verified`
  in the ledger and were **not** touched by this lane. The original never published a generic
  essentia transport contract beyond `IEssentiaTransport`; the native publishes the same six tiles and
  no more, so nothing was invented.
* Nothing was invented as a public contract in this wave: the only new published surface is the item
  form of the node jar, and it carries the **original** method names.

---

## 4. What is still open, with the precise blocker

| item | the exact remaining difference | the lane that owns it |
|---|---|---|
| the server half of the client event nine of the node jar | the original sends the block event when the wand ritual traps a node in a jar (oracle `WandManager.java:322`); the native tree has **no** node jar ritual at all, so the event never fires | the wands lane (`w4wands`) |
| the item form of the node jar in the **creative tab** | the original offers `itemJarNode` and the ordinary jar; the native offers the item of the `jar_node` block, which is the same thing | closed here |
| the offsets of the original item renderers | the original shifts the dropped item by `(-0.5, -0.25, -0.5)` and the held item by `(0, 0, -0.5)`; the 1.21.1 item pipeline applies its own transform, and the native jar item renderer (already `parity_verified`) does not repeat them | the client lane, one line per renderer |
| the enchanting power bonus of the brain jar | the method does not exist in 1.21.1 | a genuine 1.21.1 removal |

---

## 5. The tests (authored, **NOT executed**)

| file | what it protects |
|---|---|
| `common/src/test/java/thaumcraft/essentia/EssentiaFlowPacketParityTest.java` | the key of the flow map, the fifteen ticks, the scale rule, the phase rule, the destination offset, the rebuild of the source from the byte deltas, the wrap of a delta beyond 127 and the 32 block target point |
| `common/src/test/java/thaumcraft/tiles/JarNodeClientEventParityTest.java` | the event id nine, the sparkle count five, the one second timer and the 3×4×3 volume of the burst |
| `common/src/test/java/thaumcraft/client/renderers/tile/JarBrainTurnAxisParityTest.java` | the turn of the brain happens in the horizontal plane (the Y axis) and the second rotation lays the brain on its side |
| `common/src/test/java/thaumcraft/tiles/CrucibleFluidTankParityTest.java` | the capacity of one thousand millibuckets, the fill never taking more than the free space, the drain never giving away more than it holds, water only and any fluid may drain |
| `common/src/test/java/thaumcraft/blocks/ItemJarNodeContractTest.java` | the node type, the node modifier and the identity travel with the item, an item without data carries no node, and the aspects use the key `Aspects` of the original |

No test constructs a `Block`: every rule under test is a pure static function
(`EssentiaFlowClientEffects.key/scale/phase/destinationOffset`, `EssentiaFlowFxPayload.source`,
`TileJarNode.ANIMATE_EVENT/SPARKLE_COUNT/SPARKLE_X/Y/Z`, `TileJarBrainRenderer.applyTurn`,
`TileCrucible.acceptedFill/drained/canFillWater/canDrainWater`,
`ItemJarNode.writeNodeAttributes/nodeTypeOf/nodeModifierOf`).

---

## 6. The compile proof (no Gradle)

A direct `javac` of every source set against the remapped 1.21.1 jar of the Loom cache:

| source set | files | result |
|---|---|---|
| `common/src/main/java` | 1380 | compiles, 0 errors |
| `fabric/src/main/java` | 17 | compiles, 0 errors |
| `neoforge/src/main/java` | 27 | compiles, 0 errors |
| `common/src/test/java` | 355 | compiles, 0 errors |

The classpath needed the access wideners of the project (`Projectile.<init>`, `ItemEntity.pickupDelay`,
`LivingEntity.lastHurtByPlayerTime`, `BlockEntityType.BlockEntitySupplier` — the nested type is
private in the raw merged jar, in its own flags **and** in its `InnerClasses` attribute) and the
`SpawnPlacements.register` widening of the Fabric API. A local ASM pass widens a **copy** of the
merged jar; the Loom cache itself is never touched.

---

## 7. Files changed

* `common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java` — the essence flow packet.
* `common/src/main/java/thaumcraft/common/lib/network/EssentiaFlowFxPayload.java` — **new**.
* `common/src/main/java/thaumcraft/common/lib/network/EssentiaFlowFxNetworking.java` — **new**.
* `common/src/main/java/thaumcraft/client/fx/EssentiaFlowClientEffects.java` — **new**.
* `common/src/main/java/thaumcraft/client/fx/particles/EssentiaTrailParticle.java` — **new**.
* `common/src/main/java/thaumcraft/client/fx/BlockSparkleClientEffects.java` — the client side burst.
* `common/src/main/java/thaumcraft/client/renderers/tile/TileJarBrainRenderer.java` — the Y axis.
* `common/src/main/java/thaumcraft/client/renderers/tile/TileNodeRenderer.java` — the shared node body
  and the item variant.
* `common/src/main/java/thaumcraft/client/renderers/tile/ItemJarNodeRenderer.java` — **new**.
* `common/src/main/java/thaumcraft/common/blocks/ItemJarNode.java` — **new**.
* `common/src/main/java/thaumcraft/common/tiles/TileJarNode.java` — the client event nine.
* `common/src/main/java/thaumcraft/common/tiles/TileCrucible.java` — the fluid tank and the crafting
  event.
* `common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java` — the item of the node
  jar, the light level eleven, the drop, the comparator output and the green particle.
* `common/src/main/java/thaumcraft/client/ThaumcraftEldritchClient.java` — the burst of the node jar.
* `common/src/main/java/thaumcraft/client/ClientSetup.java` — the receiver and the tooltip viewer.
* `common/src/main/java/thaumcraft/client/gui/ResearchIconDisplay.java` — the icon of the NODEJAR.
* `common/src/main/java/thaumcraft/common/Thaumcraft.java` — the payload registration.
* `fabric/src/main/java/thaumcraft/fabric/fluid/FabricCrucibleStorage.java` — **new**.
* `fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabric.java` — the tank registration.
* `fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java` — the item renderer.
* `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeCrucibleFluidHandler.java` — **new**.
* `neoforge/src/main/java/thaumcraft/neoforge/ThaumcraftNeoForge.java` — the capability registration.
* `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` — the item renderer.
* `common/src/test/java/thaumcraft/essentia/EssentiaFlowPacketParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/tiles/JarNodeClientEventParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/tiles/CrucibleFluidTankParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/blocks/ItemJarNodeContractTest.java` — **new**.
* `common/src/test/java/thaumcraft/client/renderers/tile/JarBrainTurnAxisParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/client/renderers/tile/JarRendererParityTest.java` — the stale javadoc.
* `docs/porting/scope-ledger.json` — the seven units and the mechanical counts.
* `docs/analysis/2026-10-23-agent-w4alchemy.md` — this report.

---

## 8. Acceptance criteria for the leader's client run

1. **Essentia flow** — let an add-on drain an essentia source through the public API: a coloured trail
   of motes must fly from the **source** into the **consumer** for about fifteen ticks, the trail must
   shrink over its last five ticks, and the consumer must be one block lower when it is an infusion
   matrix.
2. **Brain jar** — place a brain jar: the brain must float in the brine, bob gently and **turn around
   the vertical axis** towards the closest player (never tilt sideways). A full jar must answer a
   comparator with the share of its experience and throw **green** sparks above the jar.
3. **Node jar** — break a node jar: the dropped item must name the node type, the node modifier and
   the aspects, and the item must show the **node in three crossed planes** inside the glass shell.
   The jar must emit the light level **eleven**.
4. **Crucible** — fill the crucible with a bucket of water from another mod: the tank must accept it
   (one thousand millibuckets, water only) and the crucible must cook with it. Throw an item with warp
   into the crucible and craft it: the **sticky warp** of the output must reach the player who threw
   the item.
5. **The research browser** — the NODEJAR research must show the item of the node jar with the four
   primal aspects.
