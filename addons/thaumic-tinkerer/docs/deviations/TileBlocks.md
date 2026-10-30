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
| `IWandable` clicks and `onBlockActivated` | In 1.21.1 `Block.useItemOn` runs before `Item.useOn`, and the wand item would run the block's `IWandable` after it. A plain wand click is therefore handled by the block (turns the Tablet, the Placer, the Dislocator); a sneaking wand click is not (vanilla skips the block when sneaking with an item) and reaches `IWandable.onWandRightClick`, which opens the Remote Placer's menu. For the Tablet and the Dislocator (not `IWandable`) this is what 1.7.10 did. **For the Remote Placer it is not**: see 2.2, "The wand turns the placer" |
| GUI ids and `IGuiHandler` | Architectury extended menus (`animation_tablet`, `remote_placer`), screens registered in `TileBlocksClient` |
| `PacketTabletButton`, `PacketPlacerButton` (SimpleNetworkWrapper) | `CustomPacketPayload` records `thaumictinkerer:tablet_button` and `thaumictinkerer:placer_button`, client to server |
| stack NBT | the custom-data component |
| `FakePlayer` of Forge | `TinkererFakePlayer extends ServerPlayer` (the add-on is loader neutral and cannot use the Fabric or NeoForge class); see 2.5 |
| `ThaumicTinkerer.registry.getFirstBlockFromClass` | `TileBlocks` accessors and the registry paths of `LibBlockNames` |
| `Item.itemShard:n`, `blockLifter`, `blockMagicalLog`, `blockCosmeticSolid:n`, `itemGolemBell`, `itemResource:10`, `blockStoneDevice` in recipes | `ItemApi.legacyItemIds`/`ItemApi.getItem/getBlock` through `TileBlocks.legacyItem/legacyBlock/anyOfLegacyBlocks`, so the 1.7.10 field and metadata pair is the single source. `blockStoneDevice` without a metadata is the wildcard, as the 1.7.10 crafting manager treated a bare `Block` |
| `itemGolemCore:100` (the Tablet recipe) | `thaumcraft:golem_core_blank` (`ItemsTC.stack("golem_core_blank")`) |
| a derived tool requirement: `Material.iron` and `Material.rock` needed a pickaxe for drops | `requiresCorrectToolForDrops()` on the Tablet, Remote Placer, Locomotive, Relay, Interface and Dislocator, and the six blocks are in `thaumictinkerer:mineable/tile_blocks/pickaxe` with no `needs/` tier (any pickaxe, as 1.7.10). The Golem Connector (`Material.wood`) needs nothing and is in the axe tag |
| hardness and blast resistance | hardness unchanged: Tablet 3 (metal sound), Interface and Dislocator 3; the Remote Placer, Locomotive, Relay and Golem Connector set none and so are instantly breakable. **Resistance is converted**: 1.7.10 `setResistance(r)` stored `r * 3` and `getExplosionResistance` read it back divided by 5, so the effective value is `0.6 * r`, which is what 1.21.1 `strength(hardness, resistance)` takes (the other groups convert the same way): Tablet `setResistance(50)` is 30, Interface and Dislocator `setResistance(10)` is 6. The default step sound of the 1.7.10 blocks that set none was stone, which is the 1.21.1 default |

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
  breakable block goes at the first hit. **The speed is a fifth of a player's on the ground**, as in the original: the
  1.7.10 `FakePlayer` was never ticked and so never landed (`onGround` stayed false), and `EntityPlayer.getBreakSpeed`
  divided by five when not on the ground (`Player.getDestroySpeed` of 1.21.1 has the same rule). An earlier revision of the
  port set `onGround` to true, which made the tablet five times faster than the original; that was reverted. A tablet
  with an iron pickaxe takes about 40 ticks for stone, with a stick about 200 for netherrack.
* **Attacks (decision).** 1.21.1 players have an attack cooldown and a held item's attribute modifiers only apply from the
  tick of a ticked player. The fake player is never ticked, so before each attack it applies the held item's main-hand
  modifiers itself (`TinkererFakePlayer.refreshHeldItemModifiers`) and sets the attack-strength ticker to "fully charged".
  A sword therefore hits with its full damage, as a 1.7.10 swing did, and the tablet's swing cadence (thirty ticks) is the
  only rate limit, as before. The fake player is not on the ground (see "Breaking"), which also keeps two 1.21.1 attack
  features away that 1.7.10 did not have: the *sweep* of a charged sword swing (it needs `onGround`; with it the tablet
  would also have hurt every mob next to its target) and the critical hit (it needs a fall, and the fall distance is reset
  on every tick). One swing hits one target, as before.
* **Tool test for placing (decision).** `canUseItem` accepted `ItemBlock`, `ItemReed`, `ItemSeeds`, `ItemRedstone`; in
  1.21.1 all four are `BlockItem`s, and **so are items that 1.7.10 did not accept**: doors, beds, signs, skulls, carrots,
  potatoes, cocoa beans (`ItemDoor`, `ItemBed`, `ItemSign`, `ItemSkull`, `ItemSeedFood`, `ItemDye` there). A tablet in
  right-click mode with nothing in front of it now also places those onto the block below its target, where it used to
  stay idle. The widening is accepted: it follows from the item classes of 1.21.1, and restricting it would mean a list
  of block classes to maintain. The hoe rule (`ItemHoe` on `Blocks.dirt` or `Blocks.grass`) is kept literally: dirt and
  grass, plus coarse dirt, which was a metadata variant of `Blocks.dirt` in 1.7.10 (podzol was one too, but a 1.21.1 hoe
  does nothing to it, so a tablet would swing at it for nothing), and not the other tillable blocks that a 1.21.1 hoe also
  works on (dirt path, rooted dirt).
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
* **Slot (kept).** `isItemValidForSlot` is "a `BlockItem`" (an `ItemBlock` in 1.7.10) and restricts what *automation*
  puts in (`Container.canPlaceItem`: hoppers). The screen's slot is a plain `Slot`, as in 1.7.10, where `Slot.isItemValid`
  returned true whatever the inventory said: **any item can be put in by hand and by shift-click**, and the placer then
  uses it as it uses a block (a bucket, seeds, bone meal). 1.21.1 `Slot.mayPlace` does not ask the container either.
  An earlier revision of this file claimed the screen slot was restricted to blocks; it never was, here or in 1.7.10.
* **The wand turns the placer (decision, new behaviour).** The original `BlockRPlacer.onBlockActivated` turned the placer
  to the clicked side for a wand, but the block also implements `IWandable` and returns 0 from `onWandRightClick` for every
  click, and `ItemWandCasting.onItemUseFirst` treats any result of zero or more as handled (it runs before the block's
  `onBlockActivated` in Forge's `ItemInWorldManager`). So in 1.7.10 the wand branch was unreachable: a plain wand click did
  nothing, and only the sneaking wand click did something, by opening the screen. (The fork never registered the block, so
  nobody could have seen it either way.) The port keeps the author's evident intent: a plain wand click turns the placer
  with the wand sound (like the Dislocator and the Tablet), a sneaking one opens the screen. The 1.7.10 engine is not on
  disk here; this rests on the port's own `IWandable` contract (checked against the `ItemWandCasting` bytecode by the API
  documentation) and on Forge's call order. If the lead wants the literal behaviour, `PulseDeviceBlock.useSpecialItem`
  has to return a "consumed, nothing done" result for `BlockRPlacer` and the second research page loses its sentence
  about the plain click.
* **GUI picture (forced).** The screen uses `remote_placer.png`, the texture the converted base has for it.

### 2.3 Levitational Locomotive and Relay

* **Chunk handling (decision).** 1.7.10 asked `getTileEntity`/`getBlock`, which loaded chunks. The port consults only loaded
  chunks (`Level.isLoaded`) for the relay search, the relay check, the bounce and the move, and does nothing when the chunk is
  not loaded. A track across a chunk border that is not loaded stops until it is.
* **A partner out of reach ends the link (fix).** `TileEntityRelay.verifyPartner` ends the link of a relay whose partner is
  outside what `checkForPartner` can find (offsets -32 to +31 on each axis; `TileEntityRelay.partnerInRange`). 1.7.10 loaded
  the partner's chunk, found no relay there and ended the link; the port does not ask an unloaded chunk (see above), so a
  relay that carries the coordinates of a far-away partner (copied with `/clone` or a structure, edited data) kept its link
  forever, and the beam loops of `tick()` walk the whole rectangle between the two relays: for a partner thousands of
  blocks away that never ends and the server thread hangs. Seen in a real GameTest run: the made-up partner (77, -88) of the
  persistence check, in a run where the test area lay at x = -944,791, z = -6,350,655, stopped the server thread in
  `TileEntityRelay.tick` for good. Every partner a search can set is within the range, so real links are unaffected.
  `DeviceGeometryTest` pins the range, the GameTest `relayDropsAStalePartner` the tick.
* **The partner search keeps the last relay it finds (kept).** `checkForPartner` has no `break`: along the z line and the x
  line, offsets -32 to +31, every relay found becomes the partner and replaces the previous one, so with three relays in a
  line the farthest wins and the nearer one keeps a stale partner that does not point back, which its next check ends. The
  GameTest `relayKeepsTheLastRelayItFinds` pins it. Consequence for tests: the areas of a GameTest run lie side by side
  13 blocks apart, so relays of different checks at the same height find each other; every check of the group that
  places relays uses a height of its own.
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
  piston head, as in 1.7.10: `Block.getBlockFromName("piston_head")` is the arm block, `Blocks.PISTON_HEAD`, so a piston
  head on top of the locomotive is copied and left where it was. (An earlier revision tested `Blocks.MOVING_PISTON`, the
  1.7.10 `piston_extension`, which is a different block.)
* **No hang beyond 16,777,216 blocks (fix).** The loops over the beam and over the locomotives between two relays
  advance with `i += Math.copySign(1, difference)`, which adds a float to an int and stops advancing once the coordinate
  reaches 2^24 in absolute value; `while (i < partnerX)` then never ends, the relay ticks on both sides every tick, and a
  linked pair out there (the world border is at 30,000,000) hangs the server thread until the watchdog. The port steps by
  the integer `-1` or `+1` (`TileEntityRelay.step`, `+1` for a difference of zero as `copySign(1, 0)` is); everything below
  2^24 behaves as before, including the original's walk that visits only the first coordinate when the partner lies in the
  negative direction. `DeviceGeometryTest` pins both.
* **The locomotive is a new object after a move (forced).** 1.7.10 re-pointed the same tile entity at the new coordinates.
  The port places a new block entity at the target and copies the link; the old one is marked dead, which its ghost check
  already did.
* **Sparkles (API).** `ThaumicTinkerer.tcProxy.sparkle` is `ThaumcraftClientFX.sparkle(level, x, y, z, type)` (A8 of
  `docs/API_GAPS.md`, which the API has now), called on the same loops from the client branch of the relay's tick. The API
  knows the colours 0 to 7 and rolls the original's four-in-six chance itself: the original type 2 (blue) is 2, and the
  original type 14 (white) is 6. The comparison `zCoord > partnerX` of the original (an obvious typo that picks the colour)
  is kept.
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
  recognised by its entity type, `ThaumcraftEntities.GOLEM` of the API (the entity class is not reachable), found within thirty blocks every
  100 ticks (`time % 100 == 1`, as before) by its UUID, and the 15 methods of the peripheral
  (`GolemConnectorMethods`: `getDecorations`, `getPosition`, `getType`, `getHealth`, `getCore`, `getHome`, `setHome`,
  `getMarkers`, `setMarkers`, `newMarker`, `addMarker`, `saveMarker`, `deleteMarker`, `getMarker`, `getMarkerCount`, with
  the original's results for "no golem" and its error messages) run on a `GolemAccess`. The only implementation today is
  `UnavailableGolemAccess` ("no golem"), installed by `GolemAccessProvider`; when the API has a golem view, the provider is
  the one place to change. The methods are plain Java and are covered by a JUnit test against a fake golem.
* **Computer peripheral (forced).** As for the Tablet: `tt_golemconnector` is not registered with ComputerCraft or
  OpenComputers.
* **The block says that it is inert (decision).** Its item has a grey tooltip, `tooltip.thaumictinkerer.golem_connector`
  ("The computer link and the golem data are not part of this version yet."), and the research pages `GOLEM_CONNECTOR.0`
  and `.3` are rewritten (`lang-extra/tile_blocks.*.json`, all six languages) so that they do not promise a computer
  that reads golems or send the reader to the 1.7.10 documentation web page (`GuiResearchPeripheral`, ENTER, is not
  ported: the page lived in a GUI that the add-on does not have). Page 1 (binding, thirty blocks) is unchanged and true.
* **A stale id is cleared (fix).** The original `readCustomNBT` kept the previous UUID when the tag had none; the port sets
  it to none, so an unbound connector stays unbound after a sync.

### 2.5 Transvector Interface

* **Items, aspects, essentia (kept).** `getSizeInventory`, `getStackInSlot`, ... forward to the bound block entity when it
  is a `Container`; `getAspects`, `addToContainer`, ... when it is an `IAspectContainer`; the transport methods when it is
  an `IEssentiaTransport` (both are the API's interfaces, which the tubes and jars ask of any block entity). The face rules
  of a non-sided inventory (all slots, insert and extract allowed) are kept.
* **Fluids are forwarded on Fabric only; energy is not forwarded (forced).** The original implemented `IFluidHandler`, CoFH RF
  (`IEnergyHandler`, `IEnergyReceiver`, `IEnergyProvider`) and IC2 EU (`IEnergySink`, `IEnergySource`,
  `IEnergyConductor`) and forwarded them. 1.21.1 has no interface for these that common code can implement: fluid and
  energy storage are loader capabilities (Fabric Transfer API and Team Reborn Energy, NeoForge capabilities).
  `TileTransvector.getTile()` is the one method a loader module needs to build the forwarding capabilities on.
  *Fluids on Fabric:* `thaumic.tinkerer.fabric.TileBlocksFabric` (new, in the Fabric module; reached by name from
  `TileBlocks.ready()` as the Kami gear does for its client glue) registers `FluidStorage.SIDED` for the Interface's block
  entity type and answers with the fluid storage of the bound block for the same side, so tanks, pipes and buckets work
  through it as `fill`/`drain`/`getTankInfo` did. The Fabric GameTest `interfaceForwardsFluids` covers it (not run). An
  Interface bound to another Interface is refused there (no endless chain). *Not done:* energy (Team Reborn Energy is not
  a dependency, and it is outside the import list of PORTING_RULES), and everything on NeoForge (no glue yet).
  Items go through the Interface's `WorldlyContainer`, which the Transfer API wraps itself, and essentia through the
  Thaumcraft interfaces.
* **The research text of the Interface says what it does (decision).** `INTERFACE.0` promised BuildCraft, TE3 and IC2
  power; `lang-extra/tile_blocks.*.json` (all six languages) now says essentia, items and liquids and that power does not
  pass through it in this version. `INTERFACE.1` still carries the 1.7.10 note about BuildCraft pipes, which is harmless.
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
* **The golem test (API).** `instanceof EntityGolemBase` is `ThaumcraftEntities.is(entity, ThaumcraftEntities.GOLEM)`
  (A7 of `docs/API_GAPS.md`, which the API has now).
* **A remembered device in an unloaded chunk (kept).** The original asked `getTileEntity` for the remembered position,
  which loads its chunk, so a player who walked away from a remembered device found it ("too far"), not forgotten. The
  port does the same (`Level.getBlockEntity` on the server loads the chunk); an earlier revision looked only at loaded
  chunks and so cleared the memory with "not present". The golem branch is the same.
* **The device is also updated (decision).** The original marked the *new target* for an update. The port marks both the
  target and the device, so a client sees the binding of an Interface at once.
* **Interface to interface (kept).** Two Interfaces cannot be bound to each other ("interffail"); an Interface can still
  be bound to a Dislocator, as in the original.
* **A remembered Golem Connector clicked on a block (kept).** The original forgets it with "notpresent", because it is
  not a `TileTransvector`: the connector is bound by sneaking and clicking the golem. The port does the same.

### 2.8 Camouflage (the shared base of the Interface, Dislocator, Remote Placer and Golem Connector)

* **Rendering (forced).** 1.7.10 asked the camouflaged block for the icon of each side from inside the device block. In
  1.21.1 a block cannot answer for another block's model, so while a camouflage is set the device block is
  `RenderShape.INVISIBLE` (`camo=true`) and a block entity renderer (`RenderTileCamoDevice`) draws the imitated state at the device's position **in the world**
  (`BlockRenderDispatcher.renderBatched`, as the piston renderer draws a moving block, and as the SimpleBlocks camouflage
  does). That lights the camouflage from its neighbours (an earlier revision used `renderSingleBlock` with the packed light
  of the device's own cell, which is dark for the opaque Golem Connector), with ambient occlusion, the tint of the position
  and the culling of faces against opaque neighbours. Still different from 1.7.10: the chunk mesh's batching, and the
  chunk-rendering features of other mods do not see the device as the imitated block. **Seen once**, in the client smoke
  harness (`docs/CLIENT_VERIFICATION.md`, run of 2026-10-30, Mesa software renderer, one frame): with oak planks set as the
  camouflage, the close-ups `renderer-interface.png`, `renderer-dislocator.png`, `renderer-remote_placer.png` and
  `renderer-golem_connector.png` each show an oak-planks cube at the device's place, shaded like the floor around it; a
  one-off variant with glass on the Golem Connector shows a glass cube with the floor visible through it. **Not seen:** on a
  GPU, in play, other camouflage blocks, the camouflage next to other camouflaged devices or lit by torches. (The first
  harness run drew nothing for these four close-ups, because the harness set the camouflage only on the Ethereal Platform's
  block entity: that was a harness fault, fixed in `RendererPhase`, not a defect of the renderer.)
* **A camouflaged device occludes nothing (fix, found by the first client run).** While `camo=true` the device block is
  `RenderShape.INVISIBLE`: its own model is not drawn and the camouflage is drawn by the block entity renderer. The Golem
  Connector is opaque (`isOpaqueCube() == true`, kept), and its full-cube occlusion shape made the chunk renderer cull the
  face of the floor under it, so a connector in the state `camo=true` was a hole to the sky (`blocks-tileblocks-03.png`,
  first cell; the hole had exactly the colour of the sky, 179/207/255). The Interface, Dislocator and Remote Placer have
  `noOcclusion()` and never did. `CamoDeviceBlock.getOcclusionShape` now returns the empty shape while `camo=true` (the game
  caches the occlusion shape per block state, so it may depend on the state), so no device culls the faces of its neighbours
  while it is drawn by the renderer, and a camouflage that is not a solid cube (glass, ice, leaves) shows what is behind it.
  In its own look the Golem Connector still hides them. It also keeps **blocking light in both states**
  (`BlockGolemConnector.getLightBlock` is the maximum, the one opacity that `isOpaqueCube` gave it in 1.7.10): the default
  would have been derived from the (now empty) occlusion shape and let light through a camouflaged connector. After the fix
  the same picture shows the floor where the connector stands, as for the three other devices. In 1.7.10 the opaque
  connector also culled its neighbours' faces whatever it imitated, so a connector imitating glass probably showed the same
  hole there; that was not checked (no 1.7.10 run is possible here). The state `camo=true` with no camouflage block is not
  reachable by a click (`setCamo` sets both); a saved camouflage whose block no longer exists reads as air, which leaves the
  property true and the device drawn as nothing until an empty hand clears it (read from `readCustomData`, not tried). The
  GameTest `camouflagedDevicesLeaveTheFacesOfTheirNeighbours` pins the rule with the game's own `Block.shouldRenderFace`.
* **What can be imitated (forced).** The original accepted render types 0 (plain block), 31 (log) and 39 (quartz pillar),
  which the device drew as a cube with the imitated block's textures. The port accepts a block whose `RenderShape` is
  `MODEL` **and** whose collision shape is the full cube: stairs, slabs, fences, panes, doors, torches, plants, carpets,
  ladders, rails and so on are refused (as they were), fluids, invisible blocks and blocks drawn by a special renderer
  (chests, beds, skulls, banners, signs) too, and glass, leaves, ice, a furnace or a dispenser are accepted. **Narrower
  than 1.7.10 in one place:** render type 0 also covered blocks that were not cubes (slabs, farmland, soul sand, snow
  layers, an enchanting table), which the device showed as a cube with their textures; the port draws the model, which
  would be smaller than the device, so these are refused. The camouflageable devices of this group (`CamoDeviceBlock`) and the Camouflage block of the SimpleBlocks group (by its
  registry path `thaumictinkerer:camo`) are refused, as `block instanceof BlockCamo` refused them.
* **Variants (forced).** 1.7.10 copied the item's damage as the camouflage's metadata. A 1.21.1 block item is one block
  with its default state, with the horizontal facing set to the clicked side for the pumpkins (`BlockDirectional` was the
  one directional class among the accepted cube-like blocks; a furnace or dispenser, which was not directional, keeps its
  default facing), with the same side-to-facing rule.
* **Both hands (decision).** Only the main hand clicks count, as 1.7.10 had one hand. An empty main hand clears the
  camouflage, as an empty hand did.

### 2.9 Fake player

* **A new class, no 1.7.10 counterpart.** `TinkererFakePlayer` is a `ServerPlayer` that is never added to the level and
  never ticked: no connection (a silent listener drops every packet), no menus, no chat, invulnerable, no collision box
  (dimensions 0 by 0), no game mode change, no spawn-height lookup (so creating one does not load chunks). The Tablet's
  `TabletFakePlayer` positions it at the tablet's centre with the tablet's yaw and a pitch of -15 and keeps only slot 0.
  The 1.7.10 mixin `AccessorItemInWorldManager` is not needed (see 2.1). It is not on the ground (see 2.1).
* **No advancements (decision).** The `ServerPlayer` constructor registers the criterion listeners of the profile, so a kill
  of the tablet or a block of the placer could complete an advancement of `[ThaumcraftTablet]` or `[Minecraft]`, give its
  rewards and announce it in the chat. `TinkererFakePlayer.getAdvancements()` takes the listeners off whenever the
  advancements are asked for (every trigger asks, a data pack reload re-registers them), so nothing fires. The 1.7.10
  `FakePlayer` had no advancements. The GameTest `fakePlayerEarnsNoAdvancements` checks it with "Monster Hunter".

## 3. Not done (kept visible)

* Energy forwarding of the Interface (2.5), and fluid forwarding on NeoForge: need the loader modules (Fabric has the
  fluids).
* ComputerCraft/OpenComputers peripherals of the Tablet, the Interface and the Golem Connector (2.1, 2.4, 2.5): the method
  tables exist as plain Java, nothing registers them.
* Golem data of the Golem Connector (2.4): API request A11.
* The multiblock diagram page of the Locomotive research (N1).
* The Tablet's "xN" text of the GregTech accelerator is absent (2.1).
* AE2 handling of the Locomotive's passenger (2.3).
* The rendering of the group was seen only in the client smoke harness (software renderer, one frame, no input; 2.8 for the
  camouflage): not on a GPU, not in play, no click on a screen. The Tablet's item and overlays and the screens are not
  described in this document from those pictures.

## 4. Unverified

Round of 2026-10-30 (the first client run, 2.8): the add-on's JUnit suite (`:fabric:tinkererTest`, 364 tests of all groups, 37 of
them in the `tileblocks` package) and the 35 GameTests of the group (`-PtinkererGameTestFilter=tileblocksgametests.*`: the 34
below and the new `camouflagedDevicesLeaveTheFacesOfTheirNeighbours`) ran with no failure, error or skipped entry. The new
check was also run once with the occlusion override of `CamoDeviceBlock` taken out: it failed at its first assertion
("interface camouflaged occludes nothing"), so it does see the missing override; the assertions about the Golem Connector's
floor face were not reached in that run.

An earlier round, before the camouflage check above existed: on a Fabric dedicated server (`:fabric:runTinkererGameTest`, filter
`tileblocksgametests.*`) the 34 GameTests of the group (the 33 checks of `TileBlocksRuntimeChecks` and `interfaceForwardsFluids`
of the Fabric wrapper) passed, in two runs whose test areas lay at different random positions. The add-on's JUnit suite of that
round (`:fabric:tinkererTest`, 296 tests then, 37 of them in the group's `tileblocks` package) passed. What the first real run showed, and was
fixed: the relay fix above (a stale partner hung the server thread), and six checks whose set-up was wrong, not the code
(the Interface was bound to a stone block, which has no block entity to stand in for and so ends the binding at once;
`GameTestHelper.relativePos` of 1.21.1 is not the inverse of `absolutePos`; `GameTestHelper.destroyBlock` drops nothing;
the relays of neighbouring checks found each other, see "keeps the last relay it finds"). **Still not run:** the Fabric glue
`TileBlocksFabric` is only exercised through `interfaceForwardsFluids`; the client ran only in the smoke harness (2.8: the
camouflage renderer, `renderBatched`, drew in it on a software renderer); multiplayer, survival play and any run on a GPU
have never happened. Every behaviour above that says "as the
original" is by reading the original code, not by running both; the claims about 1.7.10 engine code (the break-speed rule,
Forge's call order of `onItemUseFirst`) are from memory of that code, whose engine source is not on disk here.


## Research cell of the Remote Placer (lead decision)

The Remote Placer is registered although the pinned fork disabled it (`shouldRegister() == false`). Its research sat at
(-6, 3) in 1.7.10, the cell of Magnets, which only worked because the block was never registered. It is at (-8, 3) now, below
its parent the Animation Tablet. The registration reports a research that Thaumcraft drops because its cell is taken
(`ThaumicTinkerer.failures()`), so such a clash can not go unnoticed again.
