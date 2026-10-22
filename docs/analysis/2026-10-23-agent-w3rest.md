# Wave 3 — the `w3rest` agent — the remaining server-side and shared `partial` units

Worktree: `/home/cyberfox-kali/wt/w3rest` (branch `agent/w3rest`).
Unit list: `/home/cyberfox-kali/units/w3rest.txt` (28 units).

The oracle is `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 859/859
fingerprints). `/home/cyberfox-kali/OpenTC4` and the 1.12 reference tree `src/main/java` were only
used as a second opinion — where they disagree with the 4.2.3.5 jar, the jar won (that is how the
warp reading of the sanity checker was removed).

## The compile proof

No Gradle was run. The proof is a direct `javac` of every source set of the port against the
remapped 1.21.1 jar of the Loom cache:

| source set | result |
|---|---|
| `common/src/main/java` (1361 files) | compiles |
| `fabric/src/main/java` | compiles |
| `neoforge/src/main/java` | compiles |
| `common/src/gameTest/java` | compiles |
| `fabric/src/clientTest/java` | compiles |
| `common/src/test/java` | compiles |

The classpath needed three corrections that the raw Loom cache does not carry:

* `BlockEntityType$BlockEntitySupplier` is `private` in the raw merged jar (both in its own
  access flags and in the `InnerClasses` attribute); Loom widens it in the dev environment, so
  the local jar was widened the same way (an ASM pass over the jar).
* the access wideners of the project and of the twenty Fabric API modules were applied to the
  local jar (they widen `SpawnPlacements.register`, `Projectile.<init>`, `ItemEntity.pickupDelay`
  and `LivingEntity.lastHurtByPlayerTime`).
* the Fabric API jars of the leader's `.gradle/loom-cache/remapped_mods` are used, because the
  plain `modules-2` jars are still in the intermediary names; the NeoForge source set uses the
  NeoForge-patched Minecraft jar.

**No unit test was executed.** The tests below are authored only.

## The units

### 1. `thaumcraft/common/tiles/TileResearchTable.java` — CLOSED

* **Oracle** `thaumcraft/common/tiles/TileResearchTable.java:398-422` — the copy of a solved note
  requires **one paper** (`Items.field_151121_aF`) and **one black dye** (`Items.field_151100_aR`
  with the metadata zero); both are consumed.
* **Port before** `common/src/main/java/thaumcraft/common/lib/network/ResearchTableCopyNetworking.java:47`
  looked for `Items.INK_SAC`, `common/src/main/java/thaumcraft/common/tiles/TileResearchTable.java:342`
  accepted an ink sac, and `common/src/main/java/thaumcraft/common/container/ContainerResearchTable.java:149`
  and `common/src/main/java/thaumcraft/client/gui/GuiResearchTable.java:104` showed the ink sac as
  the second material of the copy.
* **Fix** the copy costs `Items.BLACK_DYE` (the 1.21.1 name of the original `Items.dye` of the
  metadata zero) in the network receiver, in the commit of the table, in the admission of the
  menu, in the tooltip of the client and in the two probes of the game test harness.
* **Test** the existing `ResearchNoteCopyRuntimeChecks` of the harness now pays with the black dye.

### 2. `thaumcraft/common/lib/events/EventHandlerWorld.java` — the boss rule CLOSED, the chunk marker open

* **Oracle** `thaumcraft/common/lib/events/EventHandlerWorld.java:225-245` — the original cancelled
  the `BlockEvent.PlaceEvent` and the `BlockEvent.MultiPlaceEvent` of a placement that stood in
  the Outer Lands, in a labyrinth cell of a boss room (the feature two to five) and while a boss
  of the original `EntityThaumcraftBoss` family was alive within thirty-two blocks.
* **Port before** no such rule existed anywhere.
* **Fix** `common/src/main/java/thaumcraft/common/lib/events/BossPlacementRules.java` holds the
  rule (the pure decision `blocksPlacement(...)`, the labyrinth cell test and the boss search
  with the original radius of 32.0). The loader half is **one hook per platform**, as the
  Architectury common artifact and the NeoForge event do not share a signature:
  `fabric/src/main/java/thaumcraft/fabric/FabricBossPlacement.java` (the Architectury
  `BlockEvent.PLACE`, whose Fabric implementation cancels the placement with `InteractionResult.FAIL`)
  and `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeBossPlacement.java` (the native
  `BlockEvent.EntityPlaceEvent` and `BlockEvent.EntityMultiPlaceEvent`).
* **Cross cutting fix** the original praetor of the crimson cult was an `EntityThaumcraftBoss`
  (oracle `thaumcraft/common/entities/monster/boss/EntityCultistLeader.java:70-72`), so the
  native praetor now carries the same boss marker
  (`common/src/main/java/thaumcraft/common/entities/monster/EntityCultistLeader.java`) and names
  itself after its champion modifier exactly as the original did (oracle
  `EntityCultistLeader.generateName:105-110`). Without it the praetor was invisible to the boss
  search and to the champion subsystem.
* **Still open** the chunk save and chunk load marker of the original has no 1.21.1 counterpart
  (see the `ServerTickEventsFML` row).

### 3. `thaumcraft/api/nodes/INode.java` and `thaumcraft/common/tiles/TileNode.java` — the enum
   migration CLOSED, the identity stays open

* **Oracle** `thaumcraft/api/nodes/INode.java:12-29` — the published contract with the nine
  members; `thaumcraft/api/nodes/NodeType.java` and `NodeModifier.java` — the published enums with
  six and three constants; `thaumcraft/common/tiles/TileNode.java:75-77` — **the original node
  itself implements `INode`**; `:240-271` — the type, the modifier and the base are written
  through that contract.
* **Port before** the native node kept its own nested `TileNode.NodeType`/`TileNode.NodeModifier`
  enums, so the published enums and the native ones were two types, and the node could not
  implement the published interface at all. The nested enums were used in 166 places in 25 files
  (nine main source files, three unit test files, thirteen files of the client test harness and
  the game test harness).
* **Decision (the migration the brief asked for)** the **published enums are canonical**. The
  nested enums are gone; the node, the jar, the converter, the ecology, the generation, the scan
  aspects, the renderers and every harness use `thaumcraft.api.nodes.NodeType`/`NodeModifier`
  directly, and `TileNode` now `implements INode`. `NodeAdapter` stays as the published view and
  shares the state, the write path and the identity with the node. The migration was carried
  through the *shared* call sites as well (the two client renderers of
  `thaumcraft/client/renderers/tile/` and the thirteen files of `fabric/src/clientTest/`), so no
  half-migrated state is left behind.
* **The identity stays a UUID** and that is the remaining difference of the unit: the original
  derived it from the position (`dim:x:y:z`, oracle `TileNode:98-115`), the port keeps a stable
  random identity that is persisted with the node. The format is bound into the accepted
  evidence of the wave (`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/FocalManipulatorClientSequence.java:509`
  parses the identity with `UUID.fromString`, and the accepted saved worlds carry the UUIDs in
  their node NBT), so the port cannot change it without invalidating the accepted client run.
  The unit therefore stays `partial` with exactly this difference.
* **Test** `common/src/test/java/thaumcraft/api/NodeAdapterContractTest.java` gained
  `theNodeItselfImplementsThePublishedContract`; `common/src/test/java/thaumcraft/api/NodeTypeContractTest.java`
  lost the translation test, because there is no second set of enums any more.

### 4. `thaumcraft/common/CommonProxy.java` — CLOSED

* **Oracle** `thaumcraft/common/CommonProxy.java:63-96` — the proxy published the knowledge of a
  player, the research manager and the five global maps of the knowledge; `:103-156` — the numeric
  window routing of the server.
* **Port before** no proxy at all; the knowledge was reached through the per player
  `KnowledgeAccess` and the windows through the native menu types.
* **Decision** the native port has no static proxy and **no global knowledge registry** (the
  knowledge of a player lives in the data of that player), so the published surface asks for the
  *player* instead of a player name. The class
  `common/src/main/java/thaumcraft/common/CommonProxy.java` publishes the original members with
  that one change, and
  `common/src/main/java/thaumcraft/common/lib/research/ResearchManager.java` is the research
  manager of the original (the completion of a research, of an aspect and of the three kinds of
  scan) over the per player state.
* **Not claimed here** the forty client effect entry points of the original
  (`CommonProxy:158-327`) are the client proxy of the native port and belong to the client lane.
* **Test** `common/src/test/java/thaumcraft/common/CommonProxyApiTest.java` asserts the published
  members of the proxy and of the research manager.

### 5. `thaumcraft/api/IArchitect.java` — CLOSED

* **Oracle** `thaumcraft/api/IArchitect.java:17-28` — `getArchitectBlocks(ItemStack, World, int,
  int, int, int, EntityPlayer)`, `showAxis(ItemStack, World, EntityPlayer, int, EnumAxis)` and its
  own `EnumAxis`; the implementations are
  `thaumcraft/common/items/wands/foci/ItemFocusTrade.java:229-319` and
  `thaumcraft/common/items/wands/foci/ItemFocusWarding.java:196-309`.
* **Port before** the published interface only had `getMaxAreaSize` and `getAxisModes`, so an
  add-on that implemented the original contract could not compile against it.
* **Fix** the published interface carries the two original members and the original `EnumAxis`
  again, the two native foci implement the original rules (the block list of the area and the
  axes of the preview) and the shared rules live in
  `common/src/main/java/thaumcraft/common/items/wands/WandArchitect.java:105-160`.
* **Test** `common/src/test/java/thaumcraft/api/IArchitectAxisTest.java` asserts the axes of both
  foci and the face of the original direction ordinal.

### 6. `thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java` — CLOSED

* **Oracle** `thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:15-35` — the
  class extends `EntityDamageSourceIndirect`, names the direct attacker and the real cause, and
  carries the fire damage and the unblockable modifiers, the latter also zeroing the hunger damage.
* **Port before** the class was a factory without the two modifiers, so an add-on that used
  `setFireDamage()`/`setUnblockable()` had no counterpart at all.
* **Fix** the two modifiers are published with their original names
  (`setFireDamage`, `setUnblockable`); in 1.21.1 they are properties of the registered damage
  type (the fire tag, the armour bypass and an exhaustion of zero), so the factories select a type
  that carries the property and refuse a type that does not — a modifier is never lost silently.
* **Forced 1.21.1 change** the `DamageSource` of 1.21.1 is a record, so the class stays a factory.

### 7. `thaumcraft/common/items/relics/ItemSanityChecker.java` — CLOSED

* **Oracle** `thaumcraft/common/items/relics/ItemSanityChecker.java:27-55` — the item of the
  4.2.3.5 carries the stack limit of one, the uncommon rarity and **no use behaviour at all**.
* **Port before** the native item read the warp when it was used. That reading comes from the
  **1.12 reference of a later Thaumcraft** (`src/main/java/thaumcraft/common/items/relics/ItemSanityChecker.java`),
  not from the 4.2.3.5 jar.
* **Fix** the use behaviour was removed; the relic of the native port is as pointless as the relic
  of the original. The pure helper `EquipmentRules.sanity` and its test stay.

### 8. `thaumcraft/common/blocks/BlockCustomPlantItem.java` — the sound CLOSED, the Cave soil open

* **Oracle** `thaumcraft/common/blocks/BlockCustomPlantItem.java:69-89` — the placement plays the
  **break sound** of the plant block with the volume `(breakVolume + 1) / 2` and the pitch
  `breakPitch * 0.8`.
* **Port before** the plant played the *step* sound, and the javadoc claimed that 1.21.1 has no
  per block sound pitch.
* **Fix** that claim was wrong: the 1.21.1 `BlockItem.place` plays `getPlaceSound` with exactly
  `(soundType.getVolume() + 1) / 2` and `soundType.getPitch() * 0.8`. The plant now plays the
  break sound of its own sound type, so the original sound is reproduced.
* **Still open** the soil table of the Forge 1.7.10 `EnumPlantType.Cave` is not part of the
  decompiled original, so the two Cave plants keep the Plains soil of the original own block rule.

### 9. `thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java` — CLOSED

* **Oracle** `thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java:29-33,50-52` — the packet
  carries the position and the colour and asks the client for a sparkle burst;
  `thaumcraft/client/ClientProxy.java:632-644` — the client draws seven sparkles with the colour
  of the packet, the wand colour `-9999` asks for a random grey, and every component is spread by
  `0.2 + random * 0.4`; the radius of the `sendToAllAround` is thirty-two blocks.
* **Port before** the server half of the sparkle did not exist: the flux scrubber removed the flux
  block silently.
* **Fix** the sparkle is a kind of the block effect payload
  (`common/src/main/java/thaumcraft/common/lib/network/BlockFxPayload.java:30-31,79-82`), it
  travels to the players of the original radius
  (`common/src/main/java/thaumcraft/common/lib/network/BlockFxNetworking.java:76-86`), the flux
  scrubber sends it with the original colour `0xDD00FF`
  (`common/src/main/java/thaumcraft/common/tiles/TileFluxScrubber.java:136-141`, oracle
  `TileFluxScrubber:100`) and the client draws the seven sparkles with the colour of the packet
  and the random grey (`common/src/main/java/thaumcraft/client/fx/BlockFxClientEffects.java`,
  `common/src/main/java/thaumcraft/client/fx/particles/GrowthLampParticle.java`).

### 10. `thaumcraft/common/lib/world/dim/GenKeyRoom.java` — CLOSED

* **Oracle** `thaumcraft/common/lib/world/dim/GenKeyRoom.java:103-115` — the guardian count is two
  plus one for normal and one for hard, the offset is `(1..3) * (-1..1)`, the home of the
  guardian is the centre of the room with a radius of sixteen and the first guardian of a hard key
  room is a champion.
* **Port before** the spawn record of the world package carried no home and the champion flag of
  the spawn was never applied.
* **Fix** `DungeonRooms.Spawn` carries the home (`DungeonRooms.Home`) and
  `common/src/main/java/thaumcraft/common/config/ThaumcraftOuterLands.java:362-386` applies it
  and the forced champion.
* **Test** `common/src/test/java/thaumcraft/world/KeyRoomGuardianHomeTest.java` asserts the home
  of every guardian and the champion of the hard key room.

### 11. `thaumcraft/common/container/SlotGhostFluid.java` — CLOSED

* **Oracle** `thaumcraft/common/container/SlotGhostFluid.java:24-37` — the slot accepts what the
  Forge `FluidContainerRegistry` calls a container, its limit is one item and it never gives the
  container back.
* **Port before** only a `BucketItem` was accepted, so a fluid container of an add-on was
  refused.
* **Fix** the native fluid container is a bucket of the vanilla **or** the item that carries the
  bucket entity data, so an add-on is accepted as well.

### 12. The units that stay open, with the exact blocker

* `thaumcraft/api/aspects/AspectSourceHelper.java` — the drain, the search, the direction and the
  range are the original ones; the essence flow packet of the original (oracle
  `EssentiaHandler:54`, `PacketFXEssentiaSource:50-77`) and its client drawing (oracle
  `ClientTickEventsFML:250-269`) are missing, because the drawing needs the client particle
  effect `FXEssentiaTrail` of the client lane (`thaumcraft/client/fx/ParticleEngine.java`).
* `thaumcraft/common/blocks/BlockCosmeticStairs.java` — the icon and the texture of the eldritch
  stairs are the client side of the original (oracle `BlockCosmeticStairs:37-40` asks the
  cosmetic solid of the metadata eleven for the side plus hundred, and oracle
  `BlockCosmeticSolid:141-162` picks one of the four ancient stone textures by the hash of the
  position); 1.21.1 can only draw that with a custom renderer of the client lane.
* `thaumcraft/common/blocks/ItemJarNode.java` — the native node jar is a block, so the item form
  of the original does not exist; the four pieces of data survive the break and the placement in
  the block entity data of the drop.
* `thaumcraft/common/entities/EntityItemGrate.java` — the push out of the item is a native
  reimplementation of the vanilla `pushOutOfBlocks`, whose 1.7.10 arithmetic is not readable in
  this sandbox.
* `thaumcraft/common/items/ItemNugget.java` — the tin, the silver and the lead are the content of
  other mods; the original only offered them when the other mods were found (oracle
  `ItemNugget:72-85`, `Config.foundTinIngot` and the two next). The tag plumbing of the original
  ore recipes is already in the port.
* `thaumcraft/common/items/armor/ItemVoidRobeArmor.java` — the worn look is the client side of
  the original `ModelRobe`; the protection 3/7/6, the durability factor 10 and the
  enchantability 10 are the original ones.
* `thaumcraft/common/items/relics/ItemThaumometer.java` and
  `thaumcraft/common/lib/network/playerdata/PacketScannedToServer.java` — the rune particles of
  the original scan are a client side effect and belong to the client lane
  (`thaumcraft/client/fx/RuneEffects.java`); the use duration of 25, the completion after 20
  ticks, the camera tick sound and the server side completion of the scan are the original ones.
* `thaumcraft/common/lib/events/ServerTickEventsFML.java` — the chunk regeneration queue of the
  original (oracle `ServerTickEventsFML:83-104`) regenerates the chunks whose saved data carries
  no Thaumcraft marker, ten chunks per world tick, with the TC4 world generation of the original.
  **1.21.1 has no chunk data event and no per chunk NBT extension point**, so the marker of
  oracle `EventHandlerWorld:114-137` cannot be written or read, and the TC4 generation of the port
  runs through the datapack feature pipeline, which cannot be re-run for an existing chunk
  without regenerating its terrain (which would place the TC4 content at other places than the
  original put it, because the chunk generator draws with another random stream). The options of
  the original keep their default of `false`.
* `thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java` — the surface pass of the original
  delegates into the TC4 API jar (`thaumcraft.api.expands.worldgen.node.NodeGenerationManager`),
  and that API jar is not part of the 4.2.3.5 game jar in this sandbox.
* `thaumcraft/common/lib/world/WorldgenLootHelper.java` — the helper has no oracle counterpart;
  the original filled the chests with `ChestGenHooks.getInfo(dungeonChest)`, whose item list is a
  vanilla 1.7.10 table that is not part of this sandbox.
* `thaumcraft/common/tiles/TileCrucible.java` — the player crafting event of the original (oracle
  `EventHandlerWorld:149-168`) has no 1.21.1 counterpart on Fabric, because 1.21.1 has no item
  crafted event at all.
* `thaumcraft/common/tiles/TileJarBrain.java` and `thaumcraft/common/tiles/TileJarNode.java` —
  the native jars are block models without a block entity renderer, so the head turn of the brain
  jar and the growth of the node jar are computed with the original numbers but are not visible;
  the renderer belongs to the client lane.
* `thaumcraft/common/world/aura/AuraChunk.java` and `thaumcraft/common/world/aura/AuraHandler.java`
  — the TC6 per chunk aura state has no counterpart in the 4.2.3.5 oracle and the 1.12 handler
  refused it with an `UnsupportedOperationException` as well; the legacy `drainVis` drained the
  vis *network* at the position, the native projection drains the node *at* the position.

## The tests that were authored (and NOT executed)

* `common/src/test/java/thaumcraft/api/NodeAdapterContractTest.java` — the node itself
  implements the published contract and shares it with the published view.
* `common/src/test/java/thaumcraft/api/NodeTypeContractTest.java` — rewritten: the published
  enums are the only node type enums.
* `common/src/test/java/thaumcraft/api/IArchitectAxisTest.java` — the axes of the trade and of
  the warding preview and the face of the original direction ordinal.
* `common/src/test/java/thaumcraft/common/CommonProxyApiTest.java` — the published members of the
  proxy and of the research manager.
* `common/src/test/java/thaumcraft/world/KeyRoomGuardianHomeTest.java` — the home of the key room
  guardians and the champion of the hard key room.

None of them constructs a `Block`: every rule under test is a pure static function.
