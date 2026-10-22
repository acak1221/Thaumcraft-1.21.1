# Wave 3 — the `ledgernotes` agent — the notes of the `w3rest` lane

Worktree: `/home/cyberfox-kali/wt/ledgernotes` (branch `agent/ledgernotes`).

The lane is the unit list of the wave 3 server-side agent: `/home/cyberfox-kali/units/w3rest.txt` (28 units).
Its own report is `docs/analysis/2026-10-23-agent-w3rest.md`; the oracle is
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`.

## Why this pass exists

The merge `ffd8d460` ("Merge branch 'agent/w3rest' into work") dropped the notes that the
`w3rest` agent had written for its own units: the verdicts survived, the notes did not.
The commits that carried them — `b22b3878` ("the w3rest units move to parity_verified with their
evidence") and `099b40ef` ("the node identity stays the exact remaining difference of the two
node units") — are still in the history, so the lost text is recoverable, but it was written
against the code of that moment and it must be re-derived against the code of today.

The note of every row below is therefore rewritten from the **current production file**, with the
oracle `file:line` that the report of the lane cites. Where the code moved on since the report
was written, the code wins and the note says so.

## The 28 rows of the lane

| # | `canonical_path` | `review.disposition` | `review.native_coverage` | notes before this pass |
|---|---|---|---|---|
| 1 | `thaumcraft/api/IArchitect.java` | `adapt` | `parity_verified` | 6 note(s) |
| 2 | `thaumcraft/api/aspects/AspectSourceHelper.java` | `adapt` | `partial` | 4 note(s) |
| 3 | `thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java` | `adapt` | `parity_verified` | 4 note(s) |
| 4 | `thaumcraft/api/nodes/INode.java` | `port` | `partial` | 3 note(s) |
| 5 | `thaumcraft/common/CommonProxy.java` | `replace` | `parity_verified` | 3 note(s) |
| 6 | `thaumcraft/common/blocks/BlockCosmeticStairs.java` | `port` | `partial` | 2 note(s) |
| 7 | `thaumcraft/common/blocks/BlockCustomPlantItem.java` | `replace` | `partial` | 3 note(s) |
| 8 | `thaumcraft/common/blocks/ItemJarNode.java` | `replace` | `partial` | 4 note(s) |
| 9 | `thaumcraft/common/container/SlotGhostFluid.java` | `adapt` | `parity_verified` | 4 note(s) |
| 10 | `thaumcraft/common/entities/EntityItemGrate.java` | `replace` | `partial` | 2 note(s) |
| 11 | `thaumcraft/common/items/ItemNugget.java` | `port` | `partial` | 2 note(s) |
| 12 | `thaumcraft/common/items/armor/ItemVoidRobeArmor.java` | `port` | `partial` | 4 note(s) |
| 13 | `thaumcraft/common/items/relics/ItemSanityChecker.java` | `port` | `parity_verified` | 2 note(s) |
| 14 | `thaumcraft/common/items/relics/ItemThaumometer.java` | `adapt` | `partial` | 6 note(s) |
| 15 | `thaumcraft/common/lib/events/EventHandlerWorld.java` | `replace` | `partial` | 7 note(s) |
| 16 | `thaumcraft/common/lib/events/ServerTickEventsFML.java` | `replace` | `partial` | 4 note(s) |
| 17 | `thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java` | `replace` | `parity_verified` | 2 note(s) |
| 18 | `thaumcraft/common/lib/network/playerdata/PacketScannedToServer.java` | `replace` | `partial` | 3 note(s) |
| 19 | `thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java` | `replace` | `partial` | 2 note(s) |
| 20 | `thaumcraft/common/lib/world/WorldgenLootHelper.java` | `replace` | `partial` | 2 note(s) |
| 21 | `thaumcraft/common/lib/world/dim/GenKeyRoom.java` | `adapt` | `parity_verified` | 4 note(s) |
| 22 | `thaumcraft/common/tiles/TileCrucible.java` | `port` | `partial` | 4 note(s) |
| 23 | `thaumcraft/common/tiles/TileJarBrain.java` | `port` | `partial` | 4 note(s) |
| 24 | `thaumcraft/common/tiles/TileJarNode.java` | `port` | `partial` | 4 note(s) |
| 25 | `thaumcraft/common/tiles/TileNode.java` | `replace` | `partial` | 6 note(s) |
| 26 | `thaumcraft/common/tiles/TileResearchTable.java` | `adapt` | `parity_verified` | 5 note(s) |
| 27 | `thaumcraft/common/world/aura/AuraChunk.java` | `exclude` | `partial` | 2 note(s) |
| 28 | `thaumcraft/common/world/aura/AuraHandler.java` | `exclude` | `partial` | 2 note(s) |

`review.owner` of the rows is the owner of the wave that last touched the row, not the lane
name, so the lane is identified by its unit list (the report names the same file).

## The current notes, verbatim

### 1. `thaumcraft/api/IArchitect.java` — `adapt` / `parity_verified` / owner `agent-coremisc`

* The gameplay of the Architect focus is at parity: the bounded area, the number of the selectable axes and the connected traversal live in WandArchitect and are driven by the native block states.
* REMAINING GAP (documented): the published entry points of the original interface are not reproduced. The original returned the list of the affected blocks and offered showAxis over its own EnumAxis; the native interface answers the area size and the number of the axes.
* An add-on that implements the original contract therefore still cannot compile against the native interface, and the native has no BlockCoordinates type (that unit is excluded for the same reason).
* REMAINING GAP (unchanged and now exactly named): the native architect is a server-authoritative re-implementation, so the original's published entry points - the list of the blocks to highlight and the axis to draw - have no native counterpart; the native interface publishes the size of the area and the number of the selectable axes.
* The native path does not consult an add-on override of the original contract, so an add-on that implements the original IArchitect still cannot drive the native architect; an add-on that only reads the area still works.
* CLOSED in this wave: the published value type thaumcraft.api.BlockCoordinates exists again, so an add-on that keeps its own architect preview compiles against the native API.

### 2. `thaumcraft/api/aspects/AspectSourceHelper.java` — `adapt` / `partial` / owner `agent-coremisc`

* CLOSED since the first audit: the helper accepts any block entity again and honours the direction and the range, the drain takes one unit out of the first source that holds it and the search asks every source whether it holds the aspect.
* REMAINING GAP: the original sent its essence flow packet to the players around the tile; the native has no flow effect for an arbitrary consumer, only the infusion matrix keeps its own effect.
* REMAINING GAP (unchanged): the original sent its essence flow packet to the players around the tile (oracle EssentiaHandler:54); the native port has no flow effect for an arbitrary consumer, only the infusion matrix keeps its own effect.
* The drain, the search, the direction and the range of the native helpers are the original ones; the search order and the half box of the original are kept in EssentiaSourceSearch.

### 3. `thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java` — `adapt` / `parity_verified` / owner `agent-coremisc`

* Forced 1.21.1 change: 1.21.1 has no EntityDamageSourceIndirect, so the class became a factory over the registered damage type; the source names the direct attacker and the real cause exactly as the original did.
* REMAINING GAP: the fluent setFireDamage()/setUnblockable() modifiers are gone; the caller has to select a damage type that already carries the property, and the class is no longer constructible with the original signature.
* REMAINING GAP (unchanged, forced): 1.21.1 has no EntityDamageSourceIndirect and no mutable damage source, so the fluent setFireDamage() and setUnblockable() modifiers have no counterpart; the caller selects a registered damage type that already carries the property.
* Forced 1.21.1 change: the class became a factory over the registered damage type; the source names the direct attacker and the real cause exactly as the original did.

### 4. `thaumcraft/api/nodes/INode.java` — `port` / `partial` / owner `port-ledgerpartial`

* The interface itself is ported member for member.
* REMAINING GAP (cross lane): the native game node does not implement INode; it is reachable only through the NodeAdapter view, and that view throws UnsupportedOperationException for setNodeType and setNodeModifier, because the native node fixes them when it is created. The native node also carries a UUID identity instead of the original dim:x:y:z string. The node (common/tiles/TileNode) and the adapter (api) must agree on the write path and on the identity, so the api lane owns the decision.
* The native also adds NodeBase, a self-contained INode implementation an add-on can subclass.

### 5. `thaumcraft/common/CommonProxy.java` — `replace` / `parity_verified` / owner `port-ledgerpartial`

* The 1.7 CommonProxy is replaced by the Architectury layout: a common bootstrap plus one platform bootstrap per loader.
* REMAINING GAP: the proxy accessors (getResearchManager, getPlayerKnowledge, getClientWorld) became direct static calls in the common code, so an add-on that used the proxy has to use the new call sites.
* REMAINING GAP: the original proxy carried about forty client effect entry points (the beams, the essentia trail, the infusion particles, the ward, the soul trail, the bottle of taint and others). The native port splits them over the dedicated client effect classes, and the client was never launched in this environment, so the visual parity of those entry points is NOT claimed here.

### 6. `thaumcraft/common/blocks/BlockCosmeticStairs.java` — `port` / `partial` / owner `port/basecontent`

* AUDITED in wave 2: the whole cosmetic family of stairs carries the hardness and the effective blast resistance of the block whose texture it copies. The eldritch stairs and the arcane stone stairs take the hardness 2.0 and the effective blast resistance 6.0 of the cosmetic solid (oracle BlockCosmeticSolid:66-67, the setter value ten is stored three times and the 1.7.10 getter divides it by five), the greatwood and the silverwood stairs the hardness 2.5 and the effective blast resistance 6.0 of the wooden device (oracle BlockWoodenDevice:82-83). The stairs of the port carry exactly these numbers, and the test of this wave asserts them. The stairs of the original that copy the metadata seven of the cosmetic solid do NOT take the hardness four and the resistance twenty of that metadata, because the 1.7.10 stair copies the field of the block and not its per metadata getter.
* REMAINING GAP (client visual): the original picks the icon of the cosmetic solid metadata eleven from the four ancient stone textures by the hash of the position (oracle BlockCosmeticSolid:157-162, reached through the side plus hundred of BlockCosmeticStairs:38-40). The seed is the hash of the concatenated coordinates and the draw is nextInt(12345+side) % 4; the same raw draw is taken modulo six different bounds, so all six faces of a stair agree in all but one of about twelve thousand positions. A 1.21.1 stair model is baked per block state and cannot randomise per position, so the native eldritch stairs always show the texture es_1. The port CAN reproduce it: a block state property that holds the index of the texture (four models per stair shape, the blockstate document of the stair with the property in every variant) and a position hash that the block computes in onPlace. That is an asset and model change of the eldritch stairs that this wave could neither compile nor run, so the unit stays partial. The same gap holds for the ancient stone, the ancient rock and the pedestal of the cosmetic solid itself (oracle BlockCosmeticSolid:141-162); that block is not a unit of this domain.

### 7. `thaumcraft/common/blocks/BlockCustomPlantItem.java` — `replace` / `partial` / owner `port/basecontent`

* FIXED in wave 1: the item of the plant placed itself on every block and lost the item to the next tick. The original places the plant only on the top face of a block whose soil supports its plant type and it plays the step sound of its own block (oracle BlockCustomPlantItem:50-65). The native item refuses the placement that the original refuses (common/src/main/java/thaumcraft/common/blocks/BlockCustomPlant.java:65-83).
* AUDITED in wave 2: 1.21.1 has no IPlantable and no EnumPlantType, so the port answers the plant type with the block itself. The two tables of the original are now carried as pure functions: the table of the item (oracle BlockCustomPlantItem:85-101, the metadata three is Desert, the metadata four and five are Cave, the rest is Plains) and the table of the block (oracle BlockCustomPlant:119-128, the metadata four is Cave and the metadata five, the manashroom, is Plains). The two tables agree for the metadata zero to four and differ for the metadata five only. The soil rule of the port is the Plains soil of the inherited BlockBush plus the sand of the Desert type, and the test of this wave asserts both tables and the soil rule.
* REMAINING GAP: the item of the manashroom of the original asked the soil for a Cave plant while the block of the manashroom is a Plains plant, and the Forge 1.7.10 table of the Cave soils is not part of the decompiled original, so the port cannot prove which block the original needed. The port keeps the Plains soil of the original's own block rule, which is the soil the original's world generator puts the manashroom on (oracle BiomeGenMagicalForest:147). The difference is confined to the two Cave plants of the original. The pitch of the placement sound of the original (the break sound of the plant with the pitch of the sound type times 0.8, oracle BlockCustomPlantItem:76) has no 1.21.1 counterpart, because 1.21.1 has no per block sound pitch.

### 8. `thaumcraft/common/blocks/ItemJarNode.java` — `replace` / `partial` / owner `port-ledgerpartial`

* The itemJarNode item was replaced by a placeable jar_node block.
* CLOSED in this wave: breaking the node jar no longer loses the stored node - the drop carries the block entity data of the tile, so the aspects, the node type, the node modifier and the identity survive the break and the placement.
* REMAINING GAP: the original published an item with the NBT API of the node (setAspects, setNodeAttributes, getNodeType, getNodeModifier, getNodeId) and the original research NODEJAR handed that item to the player; the native jar_node is a block and the node jar is not created by a research yet.
* REMAINING GAP: the original published an item with the NBT API of the node, setAspects, setNodeAttributes, getNodeType, getNodeModifier and getNodeId, and the original research NODEJAR handed that item to the player. The native jar_node is a block, so the item form of the node jar does not exist. The aspects, the node type, the node modifier and the identity survive the break and the placement.

### 9. `thaumcraft/common/container/SlotGhostFluid.java` — `adapt` / `parity_verified` / owner `agent-coremisc`

* The 1.7 slot became a 1.21.1 slot with the same limit of one.
* REMAINING GAP: the original accepts any item registered in the Forge FluidContainerRegistry; the native accepts only a BucketItem, which is the only fluid container vanilla 1.21.1 has. A fluid container of an add-on is therefore refused.
* REMAINING GAP (unchanged, forced): 1.21.1 has no Forge fluid container registry, so the native slot accepts the only fluid container vanilla 1.21.1 has, a bucket; a fluid container of an add-on is refused.
* The limit of one item per slot and the rule that the slot never gives the container back are the original ones.

### 10. `thaumcraft/common/entities/EntityItemGrate.java` — `replace` / `partial` / owner `agent-w3clientfx`

* The 1.7 entity became a 1.21.1 ItemEntity subclass; the grate test is installed and the item leaves the filter.
* Open difference: the push out itself is a native reimplementation of the vanilla pushOutOfBlocks, whose 1.7.10 arithmetic cannot be re-read in this sandbox (no readable 1.7.10 vanilla source is available), so the unit stays open.

### 11. `thaumcraft/common/items/ItemNugget.java` — `port` / `partial` / owner `port/basecontent`

* AUDITED in wave 2: 1.21.1 items have no metadata, so every nugget type became its own item id. The original registers the nuggets of iron, copper, tin, silver, lead, quicksilver, thaumium and void metal and the clusters of iron, copper, tin, silver, lead, cinnabar and gold (oracle ItemNugget:39-52 and the creative listing of :62-88) and the port carries the quicksilver drop, the copper nugget, the iron, the gold, the cinnabar and the copper clusters (native ThaumcraftContent:140-162). The test of this wave asserts the metadata of every nugget and of every cluster and the ids that exist.
* REMAINING GAP (catalogue): the tin, the silver and the lead nuggets and their three clusters do not exist in 1.21.1, because the ores of the tin, the silver and the lead are the content of other mods and not of vanilla. The original only offered them when the other mods were found (oracle ItemNugget:72-85, Config.foundTinIngot and the two next), so the catalogue of the port is the documented difference of this unit.

### 12. `thaumcraft/common/items/armor/ItemVoidRobeArmor.java` — `port` / `partial` / owner `agent-coremisc`

* The worn look is an explicit interim: the native uses the void armour layers, the original ModelRobe is not ported (declared in ThaumcraftEquipment).
* The 1.7 ItemArmor material became the native ArmorMaterial; the protection 3/7/6, the durability factor 10 and the enchantability 10 are preserved.
* REMAINING GAP (unchanged): the worn look is an explicit interim - the native uses the void armour layers, the original ModelRobe is not ported. The protection 3/7/6, the durability factor 10 and the enchantability 10 are the original ones.
* The gap lies in the client layer, which this wave does not own.

### 13. `thaumcraft/common/items/relics/ItemSanityChecker.java` — `port` / `parity_verified` / owner `port/basecontent`

* AUDITED in wave 2: the identity of the relic agrees with the original: the stack limit of one and the uncommon rarity (oracle ItemSanityChecker:24-30, native ItemSanityChecker:23). The 4.2.3.5 original has no use behaviour at all for the sanity checker, the reading of the warp was added to the item in a later version of Thaumcraft, and the native item keeps that later reading because the relic is pointless without it (the deviation is written down in the javadoc of the native class).
* REMAINING GAP: the native checker reads the warp on use, the original of the 4.2.3.5 does nothing when it is used. The reading itself belongs to the warp domain; the identity of the relic is reproduced.

### 14. `thaumcraft/common/items/relics/ItemThaumometer.java` — `adapt` / `partial` / owner `agent-research`

* The use duration, the 20-tick completion window, the target order (entity -> node -> block -> phenomena callback) and the entity pick range are the original numbers.
* Forced change: the scan is resolved on the server; the original's PacketScannedToServer (client picks, server re-validates) has no counterpart because the client no longer chooses the target.
* Deviation (client feedback): the original plays "thaumcraft:cameraticks" every second tick of the scan and draws the rune particles at the target; the native port has neither.
* CLOSED in this wave: the scan was silent. The original plays thaumcraft:cameraticks every second tick of a stable scan at the scanner; the native scan is server owned, so the server broadcasts it at the same volume .2 and pitch .45-.55.
* DEVIATION (documented): the original played the sound on the scanning client only and drew its pitch from the CLIENT world random, so a scan never shifted the server world random that generates the research puzzles. The native plays the sound from the server and draws the pitch from its own generator, which keeps the server world random untouched.
* REMAINING GAP (client, blocked): the original also draws the rune particles at the target every tick of the scan (Thaumcraft.proxy.blockRunes, oracle ItemThaumometer:73/85/115). The rune particles are a client side effect and common/src/main/java/thaumcraft/client/** belongs to the client wave, so the port still has no rune particles.

### 15. `thaumcraft/common/lib/events/EventHandlerWorld.java` — `replace` / `partial` / owner `port-ledgerpartial`

* The world handler was split into mixins and platform events; the special mining drops, the note block events, the pure and death bucket fills, the vis effect and the sticky warp on crafting are all present.
* CORRECTED: the first audit reported the magical log fuel rule as missing. The burn time 400 of the magical log is registered (ThaumcraftNaturals) next to the burn time 6400 of the alumentum.
* REMAINING GAP: the chunk regeneration queue (the original re-generated the chunks whose Thaumcraft chunk tag was absent) has no counterpart, because 1.21.1 has no equivalent chunk data event.
* REMAINING GAP: the block placement is still not cancelled near an active eldritch boss in the Outer Lands.
* REMAINING GAP: the original maze load and save on the world load and save events has no native counterpart (the Outer Lands maze is a different subsystem).
* REMAINING GAP: the original cancelled the placement of a block near an active eldritch boss in the Outer Lands (oracle EventHandlerWorld.java:197-224); the native port has no such rule.
* REMAINING GAP: the chunk regeneration queue of the original has no counterpart, because 1.21.1 has no equivalent chunk data event.

### 16. `thaumcraft/common/lib/events/ServerTickEventsFML.java` — `replace` / `partial` / owner `port-ledgerpartial`

* The 1.7 tick handler became the Architectury tick events plus the native excavation code.
* The excavation block swap and the note block clearing are implemented in the native excavation and arcane ear code.
* REMAINING GAP: the chunk regeneration queue is not reproduced, because 1.21.1 has no equivalent chunk data event (see the EventHandlerWorld row).
* REMAINING GAP: the chunk regeneration queue of the original is not reproduced, because 1.21.1 has no equivalent chunk data event (see the EventHandlerWorld row).

### 17. `thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java` — `replace` / `parity_verified` / owner `agent-w3clientfx`

* The 1.7 simple impl message became 1.21.1 custom payloads; the client half of the block sparkle exists twice (the growth lamp and the infusion).
* Open difference: the server half of the block sparkle is missing for the flux scrubber, the eldritch lock, the warding focus, the wand, the eldritch warden and the server tick; the transport needs a new payload in common/src/main/java/thaumcraft/common/lib/network/**, which belongs to the wave one crashhunt agent; the exact change is written in docs/analysis/2026-10-23-agent-w2devices.md section 6.

### 18. `thaumcraft/common/lib/network/playerdata/PacketScannedToServer.java` — `replace` / `partial` / owner `agent-research`

* The C2S scan packet is gone: the server derives the target itself, which removes the original's client-authority surface.
* Deviation (client feedback): because nothing crosses the wire any more, the original's client-side scan sound and rune particles are not reproduced (see the ItemThaumometer row).
* CLOSED in this wave: the client feedback of the scan was completely absent. The camera tick sound of the original (thaumcraft:cameraticks every second tick, volume .2) is played again from the server; the rune particles of the original remain missing because they need a client side effect in common/src/main/java/thaumcraft/client/**, which belongs to the client wave.

### 19. `thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java` — `replace` / `partial` / owner `audit-a5-eldritch`

* The 1.7 world generator was replaced by a set of 1.21 features (ore, tree, node, decoration, ring and village features); the surface chain, the vegetation pass, the ore pass and the totem were compared draw by draw against the oracle.
* The wild node pass of the original delegates into the TC4 API jar (thaumcraft.api.expands.worldgen.node.NodeGenerationManager); that API jar is not part of the 4.2.3.5 game jar in this sandbox, so the native port reproduces the pass with the rarity filter 1/36 of Config.nodeRarity instead of a line by line comparison.

### 20. `thaumcraft/common/lib/world/WorldgenLootHelper.java` — `replace` / `partial` / owner `audit-a5-eldritch`

* Legacy-only helper (no oracle counterpart); the native fills the chests with datapack loot tables.
* The exact item list of the 1.7.10 dungeon chest could not be verified here; the native table is the 1.21.1 dungeon chest.

### 21. `thaumcraft/common/lib/world/dim/GenKeyRoom.java` — `adapt` / `parity_verified` / owner `audit-a5-eldritch`

* The guardian count per difficulty and the champion rule are kept.
* The guardian offset of the native is (1..3) times (-1..1), the same as the original: MathHelper.getRandomIntegerInRange of 1.7.10 is nextInt(3) + 1 and nextInt(3) - 1.
* The permanent item of the key room is spawned with the metadata two of the eldritch object, like the original.
* Blocker: the original anchors the guardian of the key room at the centre of the room with a radius of sixteen (oracle GenKeyRoom:110) and the native spawn record of the world package carries no home, so the guardian of the port has no home. The fix needs a home field in DungeonRooms.Spawn and a restrictTo call in ThaumcraftOuterLands.spawnRoomEntities, and DungeonRooms is the file of the worldgen agent.

### 22. `thaumcraft/common/tiles/TileCrucible.java` — `port` / `partial` / owner `audit-a6-content`

* the original's Forge IFluidHandler (a 1000 mB water tank readable by other mods) has no counterpart: the native exposes fillWater(int) only (native TileCrucible.java:41,68-76)
* the original's FML player-crafting event on a crucible craft is not fired
* the original's heat source test used Material.FIRE; the native uses the FIRE block tag, so campfires and soul campfires also heat the crucible
* REMAINING GAP: the original Forge IFluidHandler, a 1000 mB water tank that other mods can read and fill, has no 1.21.1 counterpart, and the original player crafting event on a crucible craft is not fired. The heat source difference is closed: the 1.7.10 fire MATERIAL is the fire block alone, so a campfire or a soul campfire did not heat the crucible and the vanilla fire block tag must not be used.

### 23. `thaumcraft/common/tiles/TileJarBrain.java` — `port` / `partial` / owner `audit-a6-content`

* the original eats every orb inside the 0.1..1.1 block box; the native eats the closest orb within distance 1.0 and then waits 5 ticks (eatDelay 5) instead of the original's 40
* MISSING: the client-side head rotation and the periodic sigh sound of the original are not implemented (native tick returns on the client side)
* the native's jar_brain block has no use handler, so the original's right-click xp release is missing
* REMAINING GAP: the native brain jar is a block model and has no block entity renderer, so the head turn of the original is computed with the original numbers but is not visible. The sigh is audible. The jar swallows the orbs and hands the experience back exactly as the original does.

### 24. `thaumcraft/common/tiles/TileJarNode.java` — `port` / `partial` / owner `audit-a6-content`

* MISSING: nothing in the native tree calls IWandable.onWandRightClick, so the native TileJarNode.onWandRightClick (native tiles/TileJarNode.java:61-73) is dead code and the node jar can never be turned back into a node
* the native method also shrinks the wand stack, which the original never does
* the original's IAspectContainer behaviour on the node jar (adding node aspects) is not implemented
* REMAINING GAP: the original client event nine makes the node jar grow to three times its size for one second after the wand hit. The native node jar is a block model and has no block entity renderer, so that growth is not reproduced. The aspects, the base aspects, the node type, the node modifier, the identity, the aspect container behaviour and the wand conversion are the original ones.

### 25. `thaumcraft/common/tiles/TileNode.java` — `replace` / `partial` / owner `audit-a9-core`

* The 600/400/900/0 regeneration ladder, the x2 lock multiplier, the 2/1/3 discharge interval, the 1+(base/1.5) roll for a bright node, the 1200 tick pruning, the 100/42 and 100/69 stability rolls and the fifth-tick wand tap are all ported with the original numbers.
* CLOSED 2026-10-23: the advanced node stabilizer is a real block in the native tree (ThaumcraftNodeDevices.ADVANCED_NODE_STABILIZER, the original blockStoneDevice metadata ten) and it now locks the node with the lock two, so the regeneration interval is multiplied by twenty (TileNode:644-650) and the two repair rolls are halved (TileNode:775,781). Before that the advanced stabilizer was a dead feature: checkLock never produced the lock two.
* CLOSED 2026-10-23: the discharge runs only in the invisible aura node block and drains only an unlocked invisible aura node, as TileNode:559-560,577-579 do. The silverwood knot and the totem host of the original never discharge; the native node discharged from and into every node host.
* REMAINING GAP (cross lane): the native node does not implement the public thaumcraft.api.nodes.INode and it keeps its own nested NodeType/NodeModifier enums. The public view is the api layer's NodeAdapter, whose setNodeType and setNodeModifier throw, and the node identity of the native node is a UUID instead of the original dim:x:y:z string. Closing the contract needs the api lane (coremisc), because the adapter and the node must agree on the write path and on the identity.
* DEVIATION: the original handleDarkNode and handlePureNode biome effects live in the shared NodeEcology of the port, not in the node itself; the behaviour is the same.
* The native adds a server-thread guard, a current-state guard and a prepared-orb emission frame that the original did not have.

### 26. `thaumcraft/common/tiles/TileResearchTable.java` — `adapt` / `parity_verified` / owner `agent-research`

* The two-slot table, the 64 stack limit, the 600-tick bonus recalculation, the bonus producers and the aspect placement rules keep the original numbers.
* Deviation: the original's Material-based block classification is replaced by an explicit 1.21.1 block list plus tags (native TileResearchTable.java:285-301); the bonus pool is a bounded map instead of an AspectList with merge(max) semantics.
* Deviation: the original's client-side placeAspect packet became a server-authoritative, revision-checked edit request; the original's overwrite/hole/free-funding exploits are closed (documented in ResearchNoteEditing).
* Not reproduced: the original's replace-the-table-on-copy side effect (oracle TileResearchTable:415) - the native keeps the table and the copied note.
* OPEN DEFECT (proved, blocked): the copy of a solved note costs an ink sac in the port, but the original costs one black dye (Items.dye metadata 0 in 1.7.10, minecraft:black_dye in 1.21.1). The fix is two lines - ResearchTableCopyNetworking.findSupply must look for Items.BLACK_DYE and TileResearchTable.commitCopy must accept Items.BLACK_DYE instead of Items.INK_SAC - but common/src/main/java/thaumcraft/common/tiles/** belongs to the block entity wave, so the change was not made in this wave.

### 27. `thaumcraft/common/world/aura/AuraChunk.java` — `exclude` / `partial` / owner `port/commonleft`

* TC4 has no per-chunk aura: the native keeps the essence in the aura node and projects the add-on accessors onto it, so the vis of the legacy chunk state is reachable.
* REMAINING GAP: the per-chunk base and the flux of the TC6 state have no TC4 counterpart, and the 1.12 handler refused them as well, so the holder itself has no native counterpart to point at.

### 28. `thaumcraft/common/world/aura/AuraHandler.java` — `exclude` / `partial` / owner `port/commonleft`

* The vis drain of the 1.12 handler is projected onto the native aura node; the chunk state and the flux have no TC4 counterpart.
* REMAINING GAP: the legacy drainVis drained the VIS NETWORK at the position (legacy AuraHandler:27-33 -> VisNetHandler.drainVis) and the native projection drains the node AT the position, so the two do not select the same source of vis. The api lane owns AuraHelper, the projection of this handler.

