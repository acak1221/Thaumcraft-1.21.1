# 2026-10-23 — devices wave: advanced devices and VisNet (`advanced_devices_and_visnet`)

Worktree `/home/cyberfox-kali/wt/devices`, branch `agent/devices`.
Oracle: the readable TC4 4.2.3.5 1.7.10 source `/home/cyberfox-kali/OpenTC4` (the private
decompiled oracle root is **not readable in this sandbox**, see §6).
Scope: the 39 open units of the lane, list `/home/cyberfox-kali/units/devices.txt`.

## 1. Result

| | before | after |
|---|---|---|
| `parity_verified` | 13 | **39 − 13 = 26** |
| `partial` | 26 | 13 |
| `not_assessed` | 13 | 0 |

26 units moved to `parity_verified`, 13 stay `partial` with a written reason.

* **7 defects found and fixed** (all in the common layer).
* **1 new test class** with 12 test methods, **authored but NOT executed** (no Gradle run is
  allowed in this wave).

| commit | content |
|---|---|
| `75b95db3` | `fix(devices)`: the seven defects of §2 and the new test class |
| `61e3163d` | `docs(devices)`: the scope ledger verdicts of the 39 units |

The previous wave had already closed the findings of `2026-10-21-audit-a3-devices.md` and
`2026-10-21-openTC4-tiles.md` (the tickers of the magic box and of the mirrors, the ORDER
vis payment of the mirror, the vis drain and the radius of the flux scrubber, the
`IAspectSource` surface of the essentia mirror, the grate test of the item entity, the
advanced node stabiliser block, the hand mirror bridge and view, the ticker of the
thaumatorium top). This wave re-read the oracle line by line for the units of the lane and
found the seven defects below, which no earlier report lists.

## 2. Defects: oracle → port → fix

### 2.1 The flux scrubber never removed a block of flux gas

* **Oracle** `thaumcraft/common/tiles/TileFluxScrubber.java:59-62` —
  `boolean isFlux(int x, int y, int z) { Material mat = this.worldObj.getBlock(x, y, z).getMaterial(); return mat == Config.fluxGoomaterial; }`
* **Oracle** `thaumcraft/common/config/Config.java:876` — `fluxGoomaterial = (new MaterialTaint(MapColor.grassColor)).setNoPushMobility();`
* **Oracle** `thaumcraft/common/blocks/BlockFluxGoo.java:32` and `thaumcraft/common/blocks/BlockFluxGas.java:28` —
  the flux goo **and** the flux gas are the only two blocks that hand that material to their
  constructor, so the original scrubbed both.
* **Port (before)** `common/src/main/java/thaumcraft/common/tiles/TileFluxScrubber.java:116-119` —
  `return level.getBlockState(pos).getBlock() instanceof BlockFluxGoo;` — the flux gas was
  invisible to the scrubber, so a gas cloud was never cleaned and the machine never earned
  its charges from it.
* **Fix** the material test became the whole flux family: the new rule
  `TileParityRules.fluxScrubberRemoves(BlockState)`
  (`common/src/main/java/thaumcraft/common/lib/tiles/TileParityRules.java`) accepts the two
  native flux blocks, which are exactly the two subclasses of `FluxFiniteBlock`, the native
  port of the finite fluid of the original; `TileFluxScrubber.isFlux` calls it.

### 2.2 The advanced node stabiliser never locked its node

* **Oracle** `thaumcraft/common/tiles/TileNode.java:885-889` —
  `if (metadata == 9) this.nodeLock = 1; else if (metadata == 10) this.nodeLock = 2;`
* **Oracle** `thaumcraft/common/tiles/TileNodeStabilizer.java:14` — `this.lock = metadata == 9 ? 1 : 2;`
* **Port (before)** `common/src/main/java/thaumcraft/common/tiles/TileNode.java:520` —
  `byte next = !powered && support.is(NODE_STABILIZER.get()) ? (byte) 1 : 0;` — only the basic
  stabiliser locked a node, the advanced one (`thaumcraft:advanced_node_stabilizer`, the stone
  device of the metadata ten) locked nothing and the lock value two was never produced.
* **Fix** `TileParityRules.nodeStabilizerLock(basic, advanced)` returns 1, 2 or 0, and
  `TileNode.checkLock` uses it.

### 2.3 A node that is locked with two regenerated with the factor of one

* **Oracle** `thaumcraft/common/tiles/TileNode.java:644-650` —
  `if (this.getLock() == 1) { this.regeneration *= 2; } if (this.getLock() == 2) { this.regeneration *= 20; }`
* **Port (before)** `common/src/main/java/thaumcraft/common/tiles/TileNode.java:599` —
  `return nodeLock == 1 ? interval * 2 : interval;`
* **Fix** `TileParityRules.nodeRegenerationInterval(interval, lock)` multiplies with 2 for the
  lock one and with 20 for the lock two.

### 2.4 The fading repair of a locked node ignored the lock

* **Oracle** `thaumcraft/common/tiles/TileNode.java:781` —
  `this.worldObj.rand.nextInt(12500 / this.getLock()) == 69`
* **Port (before)** `common/src/main/java/thaumcraft/common/tiles/TileNode.java:648` —
  `level.random.nextInt(12500) != 69` — the divisor of the advanced stabiliser (6250) was
  missing, so its fading nodes repaired themselves half as often as in the original.
* **Fix** `TileParityRules.nodeFadingRepairBound(lock)` returns `12500 / lock`
  (12500 for the lock one, 6250 for the lock two, zero for an unlocked node whose repair the
  original never attempts).

### 2.5 The item mirror lost every buffered item on the next save

* **Oracle** `thaumcraft/common/tiles/TileMirror.java:330-344` —
  `nbtCompound1.setByte("Slot", (byte)i); ((ItemStack)this.outputStacks.get(i)).writeToNBT(nbtCompound1);`
  — the slot byte and the stack share **one** compound, so the fields of the stack sit at the
  top of the entry; `readFromNBT` (`:317-328`) reads that entry as an item stack.
* **Port (before)** `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:286-292`
  wrote the stack into a nested `"stack"` compound (`:292`), while `loadAdditional`
  (`:271-273`) parsed the **entry** itself (`:273`). `ItemStack.parse` needs the `id` field at
  the top level, so every entry failed to parse, every buffered item was dropped and the
  mirror silently lost the items it had not ejected yet.
* **Fix** `TileMirror.saveOutputStack(stack, slot, registries)` writes the shape of the
  original (the stack at the top of the entry plus the slot byte) and
  `TileMirror.loadOutputStack(entry, registries)` reads it. The same shape is already used by
  the native `TileSpa:229` and `TileSpa:244`, so the round trip is proven inside the port.

### 2.6 The item of a mirror got a pick up delay instead of a portal cooldown

* **Oracle** `thaumcraft/common/items/relics/ItemHandMirror.java:147` — `ie2.timeUntilPortal = 20;`
* **Oracle** `thaumcraft/common/tiles/TileMirror.java:207` — `ie2.timeUntilPortal = 20;`
  (`timeUntilPortal` is the 1.7.10 name of `Entity.field_71088_bW`; the migration status and
  `2026-10-21-openTC4-tiles.md` §1.6 prove that this field is **not** the pick up delay).
* **Port (before)** `common/src/main/java/thaumcraft/common/items/relics/ItemHandMirror.java:183`
  — `item.setPickUpDelay(EquipmentRules.MIRROR_ITEM_LIFETIME);` — the transported item was
  kept out of the player's reach for twenty ticks, which the original never did; the mirror
  itself never set the portal cooldown at all.
* **Fix** `TileParityRules.MIRROR_PORTAL_COOLDOWN = 20` and
  `item.setPortalCooldown(...)` in `ItemHandMirror.transport` (`:184`) and in
  `TileMirror.spawnItem` (`:181`). The vanilla pick up delay is left alone.

### 2.7 The magic box kept the number of linked boxes on the disk

* **Oracle** `thaumcraft/common/tiles/TileMagicBox.java:124-145` — `writeToNBT` writes the
  items and `writeCustomNBT` writes the sorting and the master, **never** the number of
  linked boxes; `updateEntity` (`:155-161`) refreshes the links while `linkedBoxes < 0`, so
  after every load the chain is scanned again.
* **Port (before)** `common/src/main/java/thaumcraft/common/tiles/TileMagicBox.java:252` read
  the persisted `linked` short and `:260` wrote it. A chain that changed while the chunk was
  unloaded kept its old size forever, and the box never scanned its neighbours again.
* **Fix** the counter is transient again: it starts at minus one after a load and the first
  tick re-scans the neighbours.

## 3. Findings that are not defects (documented, not changed)

1. **The mod sort of the magic box (deliberate deviation).** The original
   `thaumcraft/common/tiles/TileMagicBox.java:220-255` builds an **inconsistent** key for the
   sort by mod: the mod identifier of the *second* slot is appended to the key of the *first*
   one (`s1 = s1 + GameRegistry.findUniqueIdentifierFor(item(j)).modId;`), so the comparison is
   not a strict order. The bubble sort of the original (`sort()`, `:190-218`, a
   `while (!done)` loop) then swaps the same pair back and forth and can run forever inside
   a single tick. The native tree compares the mod identifier and the name of each slot
   separately (`TileMagicBox.swopSlots`), so the sort always terminates. The original
   comparator is **not** copied; the deviation is written into the ledger note of the unit.
   The second, smaller difference of the same method: the original appends the hash code of
   the tag compound of a stack to the sort key of the sort by name, so two stacks with the
   same display name are ordered by their tags. The 1.21.1 hash of a tag is not the 1.7.10
   hash, so the native tree leaves the tie to the order of the list instead of pretending a
   value it cannot reproduce.
2. **`TileArcaneLampLight` is dead code in the original.** The oracle
   `thaumcraft/common/tiles/TileArcaneLampLight.java` is not registered as a block entity
   (`ConfigBlocks.java:343-415` registers seventy three tiles, this one is not among them), no
   block of the oracle creates it and a scan of the whole oracle tree finds no other
   reference. The native tree therefore carries no such tile; the rule of the dead class
   survives as `LegacyTileRules.lampLightExpires`.
3. **The item spawn egg spends the egg in the creative mode.** The original
   `thaumcraft/common/entities/ItemSpawnerEgg.java:73` only spends the stack outside the
   creative mode; the native `ItemSpawnerEgg` (a 1.21.1 `SpawnEggItem` subclass) always
   spends it, like the vanilla spawn egg. The class is in `common/entities`, which this wave
   is not allowed to touch, so the unit stays `partial`.
4. **The item entity of the grate.** The original
   `thaumcraft/common/entities/EntityItemGrate.java:18-23` overrides a 1.7.10 vanilla method
   so that the metal device of the metadata five and six counts as a block of its own. The
   native entity reimplements the push out arithmetically. The 1.7.10 vanilla source is not
   readable in this sandbox, so the exact vanilla method could not be re-read; the class is in
   `common/entities` and the unit stays `partial`.
5. **The client surface of the lane.** The sparkle of the original
   `thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java` (used by the flux scrubber at
   `TileFluxScrubber.java:98`, the growth lamp at `TileArcaneLampGrowth.java:96`, the eldritch
   lock, the warding focus, the wand manager and the eldritch warden) has no single native
   payload: the growth lamp has its own (`GrowthLampFxPayload`), the other effects have their
   own families, and the flux scrubber has none. The dig event of the bore
   (`thaumcraft/common/lib/network/misc/PacketBoreDig.java`, `TileArcaneBore.java:775-793`)
   became the client state of the block entity. All of these are client pictures and live in
   `common/client`, which this wave is not allowed to touch.

## 4. Units

### moved to `parity_verified` (26)

| unit | why |
|---|---|
| `client/renderers/block/AlchemyFurnaceBakedModel.java` | not in the oracle; the native furnace uses the 1.21.1 block model |
| `client/renderers/block/ArcaneFurnaceBakedModel.java` | not in the oracle; the native class carries the geometry of the original renderer |
| `client/renderers/block/BlockWardedRenderer.java` | not in the oracle; the native ward is drawn by `TileWardedRenderer` |
| `client/renderers/block/WardedGlassBakedModel.java` | not in the oracle; the native class carries the neighbour mask and the rune textures |
| `client/renderers/item/TrunkSpawnerPerspectiveModel.java` | not in the oracle; the traveling trunk uses the 1.21.1 item display |
| `client/renderers/models/ModelBoreEmit.java` | not in the oracle; the native vortex lives in `BoreClientEffects` |
| `client/renderers/models/ModelFluxScrubber.java` | not in the oracle; the scrubber uses the 1.21.1 block model |
| `client/renderers/models/ModelNodeStabilizer.java` | not in the oracle; the native class of the same name carries the geometry |
| `client/renderers/models/ModelThaumatorium.java` | not in the oracle; the native class is the OBJ loader of the retained asset |
| `client/renderers/tile/TileArcanePressurePlateRenderer.java` | not in the oracle; the native plate is a 1.21.1 block with two variants |
| `client/renderers/tile/TileSensorRenderer.java` | not in the oracle; the native arcane ear has no tile renderer |
| `common/blocks/ItemBlocks/BlockArcaneFurnaceItem.java` | not in the oracle; the native furnace uses the plain `BlockItem` |
| `common/blocks/ItemBlocks/BlockMirrorItem.java` | not in the oracle; the native mirror drops a `BlockItem` and keeps the link in `CustomData` |
| `common/items/relics/ItemHandMirror.java` | the link, the view (gui sixteen), the transport and the portal cooldown are the original ones (§2.6) |
| `common/tiles/TileArcaneLampFertility.java` | the charges, the draw, the three hundredth tick, the density cap of seven and the two charges are the original ones |
| `common/tiles/TileArcaneLampGrowth.java` | the hundred charges, the reserve, the column scan and the suction are the original ones |
| `common/tiles/TileArcanePressurePlate.java` | the setting byte, the owner rule and the detection box are the original ones |
| `common/tiles/TileFluxScrubber.java` | §2.1, the charges, the essentia, the vis, the radius and the quantum rules are the original ones |
| `common/tiles/TileGrate.java` | the grate aware drop, the height, the fall and the open filter rule are the original ones |
| `common/tiles/TileMagicBox.java` | §2.7, the twenty seven slots per box, the link scan and the sort are the original ones (the mod sort deviation is documented) |
| `common/tiles/TileMirror.java` | §2.5 and §2.6, the link, the instability, the ORDER vis, the ejection rate and the inventory are the original ones |
| `common/tiles/TileMirrorEssentia.java` | the aspect source, the drain of eight, the one unit per request and the link retry are the original ones |
| `common/tiles/TileNodeStabilizer.java` | §2.2, the lock one and two and the client counter of thirty seven are the original ones |
| `common/tiles/NodeStabilizerHelper.java` | legacy only helper; the native block state test carries both locks (§2.2) |
| `common/tiles/TileArcaneLampLight.java` | dead code in the oracle (§3.2) |
| `common/tiles/TileThaumatoriumTop.java` | the orphaned half becomes the construct of the metal device metadata nine, the ticker watches the lower half |

### stay `partial` (13)

`client/fx/particles/FXBoreSparkle.java`, `client/fx/particles/FXSpark.java`,
`client/fx/particles/FXSparkle.java`, `client/fx/particles/FXSparkleTrail.java`,
`client/renderers/item/ItemTrunkSpawnerRenderer.java`,
`client/renderers/tile/TileFluxScrubberRenderer.java`,
`client/renderers/tile/TileMirrorRenderer.java`,
`client/renderers/tile/TileThaumatoriumRenderer.java` — the client pictures of the lane; the
wave is not allowed to touch `common/client`.

`common/entities/ItemSpawnerEgg.java`, `common/entities/EntityItemGrate.java` — the two
entities of the wave; the wave is not allowed to touch `common/entities` (§3.3, §3.4).

`common/lib/network/fx/PacketFXBlockSparkle.java`,
`common/lib/network/misc/PacketBoreDig.java` — the two packets of the client feedback; the
client half of a sparkle and of the dig event lives in `common/client` (§3.5).

`common/tiles/TileArcaneBore.java` — the server behaviour is the original one (the 806 line
1.7 tile became a scheduler plus `BoreWork`), but the dig event and the dig sparkle of the
original have no native counterpart, so the bore keeps its open client feedback.

## 5. Tests

**`common/src/test/java/thaumcraft/tiles/AdvancedDeviceParityTest.java`** — new, twelve
methods, **authored but NOT EXECUTED** (this wave may not run Gradle):

* `theFluxScrubberRemovesTheFluxGooAndTheFluxGas` — the rule of §2.1 against a real goo and a
  real gas block state.
* `theFluxScrubberKeepsItsOriginalNumbers` — charges 4, essentia 4, chance 4, power 5, vis 10,
  radius 16, batch 16, power per block 5.
* `theFluxScrubberOnlyReachesTheRadiusOfSixteen` — the squared distance test of the oracle
  `TileFluxScrubber.java:89`.
* `aScrubbedFluxBlockLosesOneQuantum` — the metadata rule of the oracle `:91-96`.
* `theAdvancedNodeStabilizerLocksItsNodeWithTwo` — §2.2.
* `aLockedNodeRegeneratesFasterTheStrongerTheLockIs` — §2.3.
* `theFadingRepairOfALockedNodeUsesTheDivisorOfItsLock` — §2.4.
* `theItemOfAMirrorOnlyWaitsForThePortalCooldown` — §2.6.
* `theBufferedItemsOfTheMirrorSurviveASaveAndLoad` — §2.5, a real `ItemStack` through
  `TileMirror.saveOutputStack` and `TileMirror.loadOutputStack`.
* `theMirrorEjectsAndDecaysLikeTheOriginal` — the ejection rate, the eject delay, the decay
  period and the one ORDER vis per call.
* `theMagicBoxGrowsByTwentySevenSlotsPerBox` and `theMagicBoxSearchStopsAfterAThousandAndTwentyFourBoxes`.

The port already has `thaumcraft/tiles/TileParityRulesTest.java`; the new class only adds the
numbers this wave changed.

## 6. Blockers and honest limits

* **The private oracle root is not readable in this sandbox.** The ledger tool expects
  `../work/thaumcraft-4.2.3.5-oracle` next to the worktree; that directory does not exist
  here, and `OpenTC4` is not a drop in replacement (it lacks
  `thaumcraft/common/lib/HexUtils.java`, `thaumcraft/common/lib/UUIDHelper.java` and
  `thaumcraft/common/lib/utils/UUIDHelper.java`, so the official run stops with
  *"Scope path loss"*). The oracle was therefore read through the readable 1.7.10 source
  `/home/cyberfox-kali/OpenTC4`, which is the reference the briefing names. The self test of
  the tool (`python3 scripts/porting/update_scope_ledger.py --self-test`) passes.
* **The ledger check was emulated.** `.tmp/check_ledger_without_oracle.py` verifies what the
  official run verifies without the oracle: the file is byte identical to the JSON of the
  ledger writer, the fingerprint of every row still matches the fingerprint recorded in the
  row (this is exactly what `stale_review_fingerprints` counts) and the fingerprint of every
  retained 1.12 reference file and of the class inventory still matches the file on disk.
  Result: `stale_review_fingerprints: 0`, `legacy_fingerprint_mismatches: 0`,
  `class_inventory_legacy_mismatches: 0` — the same numbers as before the change of this
  wave. **The leader has to run the official `update_scope_ledger.py --check` with the
  oracle.**
* **No build, no test run, no game run.** The tests of §5 are authored only. The whole tree
  was checked for syntax errors with `javac -proc:none` (no error that is not a missing
  symbol of the absent class path), but nothing was compiled against Minecraft.
* **The client acceptance of the lane is untouched.** The client renderer and particle units
  stay `partial`, and no client visual was inspected in a running client.

## 7. Security and crash review of the lane

* **Machines that a client can drive: none.** Every machine of the lane keeps its state on
  the server: the magic box (`TileMagicBox.tick` and `BlockMagicBox.getTicker` run on the
  server, the menu size is written by the server in `BlockMagicBox.useWithoutItem`), the
  mirrors (`BlockMirror.getTicker` runs on the server, `TileMirror.setItem` refuses the
  client, `TileMirrorEssentia.takeFromContainer` is a server side aspect source), the flux
  scrubber (`TileFluxScrubber.tick` is server only, the vis is drawn by the server), the
  grate (`TileGrate.setItem` requires a `ServerLevel`), the lamps and the pressure plate
  (server ticks). The two containers of the lane check the viewer and the server flag
  (`ContainerMagicBox.active`, `ContainerHandMirror.onInputChanged`). The bore carries an
  explicit `clientOnlyPayload` flag and `isCurrent()` demands the server owner on the server.
* **Devices that can crash the server: none found.** The one hazard of the lane is the sort
  loop of the magic box: the original comparator of the sort by mod is inconsistent and can
  spin the loop of the original forever (§3.1); the native comparator always terminates, so
  the wave *removes* that hazard instead of copying it. The recursion of the link search of
  the magic box is bounded by the thousand and twenty four boxes of the original, exactly as
  in the original.
