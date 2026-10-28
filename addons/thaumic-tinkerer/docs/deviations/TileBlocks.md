# TileBlocks: deviations from Thaumic Tinkerer 1.7.10 (GTNH fork `28f4def9`)

Group TileBlocks: Animation Tablet (Tool Dynamism Tablet), Remote Placer, Levitational Locomotive and its Relay, Golem
Connector, Transvector Interface, Transvector Dislocator, and the Transvector Binder (`ItemConnector`).

Everything listed here is a place where the port does something other than the original, with the reason. A value that is
not listed (a research position, an aspect, a recipe, a hardness, a sound, a message key, a range, a tick interval) is the
original's. Each entry says whether it is **forced** (1.21.1 or the loader-neutral rule cannot do it), a **fix** (a
reproducible defect of the original, with the evidence), or a **decision** (the port chose between two readings and the lead
may overrule it). Nothing here was run in a game; see the end for what is unverified.

## 1. Mappings that every entry below relies on (forced)

| 1.7.10 | 1.21.1 |
|---|---|
| block metadata: facing (`orientation` field, or metadata 2..5) | block state property `facing` (`FACING`, six ways for the Remote Placer and the Dislocator, `HORIZONTAL_FACING` for the Tablet). `Direction.get3DDataValue()` equals `ForgeDirection.ordinal()`, so the saved `Direction` of the locomotive and the arithmetic of the placer are unchanged |
| metadata bit 8 of the Tablet, metadata 1 of the placer/dislocator ("powered") | block state property `powered` |
| `TileCamo` (block id + metadata of the camouflage) | `CamoDeviceBlockEntity`: the whole `BlockState`, saved as a block-state tag under `camo`; the block's own boolean property `camo` mirrors "has a camouflage" |
| "not bound" = `y == -1` of `TileTransvector` | a nullable `BlockPos` plus the saved flag `linked`. `y == -1` is a real height in 1.21.1 (the world starts at -64) |
| the Binder's "nothing remembered" = `posy == -1` | the absence of `posy` in the stack's custom data. The keys `posx`, `posy`, `posz`, `ConnectingGolem` are kept |
| `ForgeDirection`, `ChunkCoordinates`, `Vec3` | `Direction`, `BlockPos`, `Vec3` |
| `IInventory`/`ISidedInventory` | `Container`/`WorldlyContainer`; `getAccessibleSlotsFromSide` = `getSlotsForFace` |
| `IWandable` clicks and `onBlockActivated` | In 1.21.1 `Block.useItemOn` runs before `Item.useOn`, and the wand item would run the block's `IWandable` after it. A plain wand click is therefore handled by the block (turns the Tablet, the Placer, the Dislocator, as 1.7.10 `onBlockActivated` did first); a sneaking wand click is not (vanilla skips the block when sneaking with an item) and reaches `IWandable.onWandRightClick`, which opens the Remote Placer's menu. Same observable result as 1.7.10 |
| GUI ids and `IGuiHandler` | Architectury extended menus (`animation_tablet`, `remote_placer`), screens registered in `TileBlocksClient` |
| `PacketTabletButton`, `PacketPlacerButton` (SimpleNetworkWrapper) | `CustomPacketPayload` records `thaumictinkerer:tablet_button` and `thaumictinkerer:placer_button`, client to server |
| stack NBT | the custom-data component |
| `FakePlayer` of Forge | `TinkererFakePlayer extends ServerPlayer` (the add-on is loader neutral and cannot use the Fabric or NeoForge class); see 2.5 |
| `ThaumicTinkerer.registry.getFirstBlockFromClass` | `TileBlocks` accessors and the registry paths of `LibBlockNames` |
| `Item.itemShard:n`, `blockLifter`, `blockMagicalLog`, `blockCosmeticSolid:n`, `itemGolemBell`, `itemResource:10`, `blockStoneDevice` in recipes | `ItemApi.legacyItemIds`/`ItemApi.getItem/getBlock` through `TileBlocks.legacyItem/legacyBlock/anyOfLegacyBlocks`, so the 1.7.10 field and metadata pair is the single source. `blockStoneDevice` without a metadata is the wildcard, as the 1.7.10 crafting manager treated a bare `Block` |
| `itemGolemCore:100` (the Tablet recipe) | `thaumcraft:golem_core_blank` (`ItemsTC.stack("golem_core_blank")`) |
| a derived tool requirement: `Material.iron` and `Material.rock` needed a pickaxe for drops | `requiresCorrectToolForDrops()` on the Tablet, Remote Placer, Locomotive, Relay, Interface and Dislocator, and the six blocks are in `thaumictinkerer:mineable/tile_blocks/pickaxe` with no `needs/` tier (any pickaxe, as 1.7.10). The Golem Connector (`Material.wood`) needs nothing and is in the axe tag |
| hardness values | unchanged: Tablet 3/50 (metal sound), Interface and Dislocator 3/10, the Remote Placer, Locomotive, Relay and Golem Connector set none and so are instantly breakable. The default step sound of the 1.7.10 blocks that set none was stone, which is the 1.21.1 default |

## 2. Per object

### 2.1 Animation Tablet

* **Facing at placement (decision).** The original `onBlockPlacedBy` maps the placer's yaw to metadata so that the tablet
  faces back toward the player. `getHorizontalDirection().getOpposite()` is the same rule.
* **The powered mark follows the redstone mode (fix).** In 1.7.10 `onNeighborBlockChange` returned early unless the tablet
  was in redstone mode. A tablet that was powered when the mode was switched off kept bit 8 forever, and the wand then
  refused to turn it ("not rotatable") until the mode was switched on and the power removed. The port computes
  `redstone && (powered here or above)` on every neighbour change and the packet that switches the mode re-runs the check.
  In redstone mode the behaviour is identical.
* **Failure report (decision).** The original sent two red chat lines to everybody within eight blocks on *every tick* of a
  failure. The port logs the exception every time (`ThaumicTinkerer.LOG`) and sends the two lines at most once per 100
  ticks. The text says "Check your game log." where the fork said "Check your FML log." (there is no FML log).
* **Breaking (decision).** The original called the 1.7.10 `ItemInWorldManager` (`onBlockClicked`, `updateBlockRemoving`,
  `tryHarvestBlock`), reading the private field `curblockDamage`/`durabilityRemainingOnBlock` through a mixin accessor.
  1.21.1 has no such state, so the tablet counts the ticks itself: the progress after `n` ticks is `getDestroyProgress *
  (n + 1)` (the 1.7.10 formula), the crack stage `(int) (progress * 10)` is shown every tick it changes, and the block is
  taken with `ServerPlayerGameMode.destroyBlock` at 100 % (drops, tool wear, events as for a player). An instantly
  breakable block goes at the first hit. The tablet's fake player stands on the ground (`setOnGround(true)`), so the
  1.21.1 airborne penalty of a fifth of the speed does not apply.
* **Attacks (decision).** 1.21.1 players have an attack cooldown and a held item's attribute modifiers only apply from the
  tick of a ticked player. The fake player is never ticked, so before each attack it applies the held item's main-hand
  modifiers itself (`TinkererFakePlayer.refreshHeldItemModifiers`) and sets the attack-strength ticker to "fully charged".
  A sword therefore hits with its full damage, as a 1.7.10 swing did, and the tablet's swing cadence (thirty ticks) is the
  only rate limit, as before.
* **Tool test for placing (decision).** `canUseItem` accepted `ItemBlock`, `ItemReed`, `ItemSeeds`, `ItemRedstone`; in
  1.21.1 all four are `BlockItem`s. The hoe rule (`ItemHoe` on dirt or grass) is kept literally: only `minecraft:dirt` and
  `minecraft:grass_block`, not the other tillable blocks that a 1.21.1 hoe also works on.
* **Right click on the block (decision).** The original asked Forge's `PlayerInteractEvent` and then called
  `onPlayerRightClick`; the port goes through `ServerPlayerGameMode.useItemOn`, which fires the loader's block-interaction
  event itself. The click on the block below an empty target is a `BlockHitResult.miss` on the top face, as the original
  `MovingObjectPosition(..., false)` was a MISS.
* **`isPickable` for the entity test (decision).** `canBeCollidedWith()` of 1.7.10 maps to `isPickable()`, and
  `getCollisionBorderSize()` to `getPickRadius()`.
* **Client swing (forced).** The GregTech World Accelerator hook (`MTEWorldAccelerator.getAccelerationForTEUnsafe`, the "xN"
  text it drew) is dropped: it is a GT5-Unofficial class that does not exist in 1.21.1. The client swings one step per tick.
* **Update packet (forced).** A 1.21.1 client ignores a block-entity update with an empty tag, and the original update
  packet carried the item and the two modes only. The packet keeps carrying those; `loadAdditional` reads the swing counters
  only when they are present, so a sync cannot stop a swing that is running on the client.
* **Computer peripheral (forced).** `tt_animationTablet` (ComputerCraft/OpenComputers: `getRedstone`, `setRedstone`,
  `getLeftClick`, `setLeftClick`, `getRotation`, `setRotation`, `hasItem`, `trigger`) has no loader-neutral registration.
  The behaviour is plain methods on the block entity (`trigger()`, `getRotation()`, `setRotation(int)`, `setLeftClick`,
  `setRedstone`, the public fields and `isEmpty()`), ready for a loader module to bridge. `setRotation` also refuses a
  negative value (the original only refused values above 3 and wrote an invalid metadata for a negative one).
* **AE2 `IMovableTile` (forced).** Not applicable to 1.21.1; `setRemoved` stops a break in progress, which is what
  `prepareToMove` was for.
* **Item rendering on the tablet (unverified estimate).** The held item is drawn with `ItemDisplayContext.FIXED` at scale
  1.55 for blocks and 1.9 for other items. The 1.7.10 numbers belong to its own item renderer and cannot be reused; these
  two are a visual estimate and have not been looked at in a game.

### 2.2 Remote Placer

* **Registered although the fork disabled it (decision).** `BlockRPlacer.shouldRegister()` returned `false` in the pinned
  fork, so the block never existed there. The task is to port the block, so it is registered, with its research
  `REMOTE_PLACER` and recipe. If the lead wants the fork's behaviour, the one line that registers it in
  `TileBlocks.register()` (and the research and recipe that hang on the block) is what to remove.
* **New research text (decision).** The converted language base has no text for the two pages of `REMOTE_PLACER` (the entry
  was never shown). `ttresearch.page.REMOTE_PLACER.0` and `.1` were written for the port in `lang-extra/tile_blocks.*.json`
  (English and Russian), describing what the code does: one to four blocks, the redstone pulse, the empty target, the wand,
  sneaking with a wand opening the menu.
* **The east quirk is kept (decision).** In 1.7.10 `receiveRedstonePulse` had `case 5` (east) copied from `case 4` (west),
  so an east-facing placer aimed `x - blocks`. `TileRPlacer.EAST_AIMS_WEST_AS_IN_1_7_10 = true` keeps it; set it to `false`
  to aim east. The facing, the side of the click and the picture of the GUI are unaffected.
* **The fake player stands at the placer (decision).** The original used `FakePlayerFactory.getMinecraft`, a single shared
  player at the world spawn. The port gives the placer its own `TinkererFakePlayer` with the profile of that shared player
  (`[Minecraft]`, `41C82C87-7AfB-4024-BA57-13D2C99CAE77`), standing in the placer and looking along its facing, so items that
  depend on the player's position or look (stairs, logs, lecterns, buckets) place as expected and protection mods see the
  placer's chunk.
* **The click (decision).** `onItemUseFirst` and `onItemUse` of the original are `ServerPlayerGameMode.useItemOn` with side
  = the opposite of the facing and the hit at the lower corner of the target (the original passed 0, 0, 0). When nothing
  happens, the right click in the air follows, as `onItemRightClick` did. An item that was used up leaves the slot empty
  (the original left a stack of size zero in the slot).
* **Unloaded target (decision).** The target must be in a loaded chunk; the original loaded it by asking for the block.
* **Distance is clamped (fix).** The screen sends 1 to 4. The packet handler and `setBlocks` clamp to 1..4; the original
  accepted any integer a hacked client sent.
* **The packets are checked (fix).** `PacketPlacerButton` and `PacketTabletButton` act only when the sender has the
  matching menu open on that very block entity (`PacketTile.resolve`). The original applied the packet to whatever tile
  entity sat at the sent dimension and coordinates, for any player at any distance (a tile entity of the wrong type was a
  `ClassCastException`): a client could switch the modes of every Tablet and set the distance of every Placer in a loaded
  chunk. The packet's dimension field is gone (the open menu is in the sender's dimension).
* **Contents drop on break (fix).** The original `BlockRPlacer` had no `breakBlock` override, so the stack in the slot was
  destroyed with the block. The port drops it (as the Tablet already did).
* **Slot (decision).** `isItemValidForSlot` is "a `BlockItem`" (an `ItemBlock` in 1.7.10). Shift-click uses it too.
* **GUI picture (forced).** The screen uses `remote_placer.png`, the texture the converted base has for it.

### 2.3 Levitational Locomotive and Relay

* **Chunk handling (decision).** 1.7.10 asked `getTileEntity`/`getBlock`, which loaded chunks. The port consults only loaded
  chunks (`hasChunkAt`) for the relay search, the relay check, the bounce and the move, and does nothing when the chunk is
  not loaded. A track across a chunk border that is not loaded stops until it is.
* **The move condition (decision).** The original condition `isAirBlock(target) || target.isAir && (...)` reduces to "the
  target is air" by operator precedence; the port tests exactly that. The passenger rules are kept: moved when the space
  above the locomotive is empty or the passenger can be placed above the target; bedrock stays; a piston head is not
  erased.
* **The passenger's block entity (forced).** The original moved only vanilla tile entities (class name starting with
  `net.minecraft.tileentity`) or, with AE2, the ones AE2 allowed, by invalidating the object and re-adding it. The port
  moves the block entities whose class is in `net.minecraft.` through their saved data (`saveWithFullMetadata`, then
  `Clearable.clearContent` on the old one so removing it spills nothing, then `loadWithComponents` on the new one). Other
  block entities are left behind, as in the original; the AE2 branch has no equivalent.
* **The "erase the passenger" quirk (kept).** The moved block is replaced by air with flag 2 only when it is not air or a
  piston head, as in 1.7.10; a moving-piston block is treated as the piston head.
* **The locomotive is a new object after a move (forced).** 1.7.10 re-pointed the same tile entity at the new coordinates.
  The port places a new block entity at the target and copies the link; the old one is marked dead, which its ghost check
  already did.
* **Sparkles (forced).** `ThaumicTinkerer.tcProxy.sparkle` (type 2 blue, otherwise white; the original drew in four of
  six calls) has no API (A8 of `docs/API_GAPS.md`). The beam between two relays is drawn with the vanilla dust particle in
  the same two colours, on the same loops and the same four-in-six chance. The comparison `zCoord > partnerX` of the original
  (an obvious typo that picks the colour) is kept.
* **The relay renderer is not registered (kept).** `RenderTileRelay` and `ModelRelay` are ported, but the original also
  never registered them in its client proxy, and `relay.png` is not among the original's textures. The relay is a block
  with a model.
* **Research (kept).** The Relay has no research of its own: its recipe is the second page of the
  locomotive's entry, under the key `LEVITATOR_RELAY`, as in 1.7.10. The multiblock diagram of the original is not drawn
  (N1 of `docs/API_GAPS.md`); the pages are text and the two recipe pages.

### 2.4 Golem Connector

* **Registered without ComputerCraft (decision).** The fork registered the block only when ComputerCraft was loaded
  (`shouldRegister() == LoadedMods.computerCraftLoaded`), because its only use is as a computer peripheral. The port
  registers it always, with its research, recipe and the Binder's golem binding, so that the group is complete and a loader
  module can bridge it later. Without that bridge the block does nothing a player can see except remember its golem and
  stay camouflageable.
* **Parent research (decision).** The parent `PERIPHERALS` ("Peripherals", documentation) was also registered only with
  ComputerCraft. The Golem Connector registers it the same way the Machines group does for the Aspectalyzer (only when the
  key is not there yet; a second registration of a key is ignored by Thaumcraft). It belongs in the shared
  `ResearchHelper`: see the shared-file requests of the report.
* **Golem data is not readable (forced, API request A11).** Thaumcraft's golems have no public API in 1.21.1. The golem is
  recognised by its entity type id `thaumcraft:golem` (the entity class is not reachable), found within thirty blocks every
  100 ticks (`time % 100 == 1`, as before) by its UUID, and the 15 methods of the peripheral
  (`GolemConnectorMethods`: `getDecorations`, `getPosition`, `getType`, `getHealth`, `getCore`, `getHome`, `setHome`,
  `getMarkers`, `setMarkers`, `newMarker`, `addMarker`, `saveMarker`, `deleteMarker`, `getMarker`, `getMarkerCount`, with
  the original's results for "no golem" and its error messages) run on a `GolemAccess`. The only implementation today is
  `UnavailableGolemAccess` ("no golem"), installed by `GolemAccessProvider`; when the API has a golem view, the provider is
  the one place to change. The methods are plain Java and are covered by a JUnit test against a fake golem.
* **Computer peripheral (forced).** As for the Tablet: `tt_golemconnector` is not registered with ComputerCraft or
  OpenComputers.
* **A stale id is cleared (fix).** The original `readCustomNBT` kept the previous UUID when the tag had none; the port sets
  it to none, so an unbound connector stays unbound after a sync.

### 2.5 Transvector Interface

* **Items, aspects, essentia (kept).** `getSizeInventory`, `getStackInSlot`, ... forward to the bound block entity when it
  is a `Container`; `getAspects`, `addToContainer`, ... when it is an `IAspectContainer`; the transport methods when it is
  an `IEssentiaTransport` (both are the API's interfaces, which the tubes and jars ask of any block entity). The face rules
  of a non-sided inventory (all slots, insert and extract allowed) are kept.
* **Fluids and energy are not forwarded (forced).** The original implemented `IFluidHandler`, CoFH RF
  (`IEnergyHandler`, `IEnergyReceiver`, `IEnergyProvider`) and IC2 EU (`IEnergySink`, `IEnergySource`,
  `IEnergyConductor`) and forwarded them. 1.21.1 has no interface for these that common code can implement: fluid and
  energy storage are loader capabilities (Fabric Transfer API and Team Reborn Energy, NeoForge capabilities).
  `TileTransvector.getTile()` is the one method a loader module needs to build the forwarding capabilities on; the common
  module does not provide them. Players lose pipes and cables into a bound tank or machine until the loader modules do.
* **An unbound interface does not swallow essentia (fix).** The original `addToContainer` returned `0` (nothing left over)
  when the interface was not bound to an aspect container, so a tube or a phial that poured into an unbound interface lost
  the essentia. The port returns the whole amount (nothing was taken), `doesContainerAccept` is `false`, and `getAspects`
  is an empty list instead of `null`.
* **Binding that is gone (decision).** `getTile()` ends a binding on the server when the bound block entity is gone (for the
  Interface; the Dislocator needs none) or the target is beyond the range, as the original did by setting `y = -1`; a
  client that has not received the target yet keeps it, and a target in an unloaded chunk answers "no block entity" and keeps
  the binding.
* **Notification (kept).** Every 100 ticks the interface calls `updateNeighborsAt`, as `notifyBlockChange` did, so pipes
  look again.
* **Computer peripheral (forced).** The Interface answered `getType` and the methods of the bound peripheral; not bridged.

### 2.6 Transvector Dislocator

* **`checkBlock` is kept as written (kept, decision).** The original expression is
  `(!node && !blacklisted && hardness != -1 && block != null) || block != air`. Every non-air block passes through the
  second operand and air passes the first, so the check is true for every block: nodes, bedrock and the portable hole's
  blacklist are swapped too. The port keeps the expression (the blacklist is `ThaumcraftApi.portableHoleBlackList`, which
  mirrors the block tag `thaumcraft:portable_hole_blacklist`). If the lead wants the evident intention (refuse nodes,
  bedrock and the blacklist), delete the final `|| !state.isAir()` of `TileTransvectorDislocator.checkBlock`. A GameTest
  (`dislocatorChecksLetEveryBlockThrough`) pins the present behaviour for bedrock and would have to change with it.
* **Block entity swap (forced).** The original swapped metadata and the tile NBT (`writeToNBT`, `createAndLoadEntity`). The
  port swaps the block state and the full block-entity data (`saveWithFullMetadata`, `BlockEntity.loadStatic`), after
  replacing the old block entity by a fresh one so that removing the block does not spill the old one's contents.
* **Unloaded end (decision).** The swap is aborted when the end block's chunk is not loaded (the original tested
  `blockExists`, which is false for an unloaded chunk too, and still moved the entities and started the cooldown).
* **Entities (kept).** The entities inside the two blocks change places by the vector between the blocks; players are
  teleported through their connection.
* **Neighbour update after the swap (forced).** `block.onNeighborBlockChange` is `BlockState.handleNeighborChanged`.

### 2.7 Transvector Binder

* **Messages and sound (kept).** The six `ttmisc.connector.*`/`ttmisc.golemconnector.*` keys and the orb-pickup sound at
  volume 0.8 are the original's. The sound plays only on the server, as before.
* **The golem test (forced).** `instanceof EntityGolemBase` is the entity type id `thaumcraft:golem` (A7 of
  `docs/API_GAPS.md`).
* **The device is also updated (decision).** The original marked the *new target* for an update. The port marks both the
  target and the device, so a client sees the binding of an Interface at once.
* **Interface to interface (kept).** Two Interfaces cannot be bound to each other ("interffail"); an Interface can still
  be bound to a Dislocator, as in the original.
* **A remembered Golem Connector clicked on a block (kept).** The original forgets it with "notpresent", because it is
  not a `TileTransvector`: the connector is bound by sneaking and clicking the golem. The port does the same.

### 2.8 Camouflage (the shared base of the Interface, Dislocator, Remote Placer and Golem Connector)

* **Rendering (forced).** 1.7.10 asked the camouflaged block for the icon of each side from inside the device block. In
  1.21.1 a block cannot answer for another block's model, so while a camouflage is set the device block is
  `RenderShape.INVISIBLE` (`camo=true`) and a block entity renderer (`RenderTileCamoDevice`) draws the imitated state with
  `renderSingleBlock`. Consequences: no ambient occlusion on the camouflage, the default (untinted) colour of the imitated
  block (the original asked its `colorMultiplier`), no culling of the faces between a camouflaged device and a neighbour,
  and chunk-rendering features of other mods do not see the device as the imitated block. Not looked at in a game.
* **What can be imitated (forced).** The original accepted render types 0 (plain cube), 31 (log) and 39 (quartz pillar).
  The port accepts any block whose `RenderShape` is `MODEL`, so fluids, invisible blocks and blocks drawn by a special
  renderer (chests, beds, skulls, banners, signs) are refused, and a furnace or dispenser, which were plain cubes, is
  accepted. The camouflageable devices of this group (`CamoDeviceBlock`) and the Camouflage block of the SimpleBlocks group (by its
  registry path `thaumictinkerer:camo`) are refused, as `block instanceof BlockCamo` refused them.
* **Variants (forced).** 1.7.10 copied the item's damage as the camouflage's metadata. A 1.21.1 block item is one block
  with its default state, with the horizontal facing set to the clicked side if it has one (`BlockDirectional`, with the
  same side-to-facing table).
* **Both hands (decision).** Only the main hand clicks count, as 1.7.10 had one hand. An empty main hand clears the
  camouflage, as an empty hand did.

### 2.9 Fake player

* **A new class, no 1.7.10 counterpart.** `TinkererFakePlayer` is a `ServerPlayer` that is never added to the level and
  never ticked: no connection (a silent listener drops every packet), no menus, no chat, invulnerable, no collision box
  (dimensions 0 by 0), no game mode change, no spawn-height lookup (so creating one does not load chunks). The Tablet's
  `TabletFakePlayer` positions it at the tablet's centre with the tablet's yaw and a pitch of -15 and keeps only slot 0.
  The 1.7.10 mixin `AccessorItemInWorldManager` is not needed (see 2.1).

## 3. Not done (kept visible)

* Fluid and energy forwarding of the Interface (2.5): needs the loader modules.
* ComputerCraft/OpenComputers peripherals of the Tablet, the Interface and the Golem Connector (2.1, 2.4, 2.5): the method
  tables exist as plain Java, nothing registers them.
* Golem data of the Golem Connector (2.4): API request A11.
* The multiblock diagram page of the Locomotive research (N1).
* The sparkle of the Relay beam and the Tablet's "xN" text are approximations or absent (2.1, 2.3).
* AE2 handling of the Locomotive's passenger (2.3).
* No look at any of the rendering (the Tablet's item and overlays, the camouflage, the screens) in a game.

## 4. Unverified

The group compiles (`tools/compile.sh`, and an isolated javac build of the group). The JUnit tests were run with a local
runner against stubbed Jupiter annotations (32 tests, all passed), not with Gradle. The GameTests
(`TileBlocksRuntimeChecks`, 28 checks) compile and have **not been run**. No dedicated-server smoke, no client, no
multiplayer, no survival play. Every behaviour above that says "as the original" is by reading the original code, not by
running both.
