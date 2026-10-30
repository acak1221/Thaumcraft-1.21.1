# KamiWorld: deviations from Thaumic Tinkerer 1.7.10

Group KamiWorld: the Bedrock dimension (terrain, ore clusters, light, sky), the Bedrock portal, the Celestial Gateway (warp gate)
with its two screens and packets, the Soul Hearts (server pool, packet, HUD), the dimensional shard drops and
`/iskamiunlocked`. The pinned 1.7.10 source is GTNewHorizons/ThaumicTinkerer at `28f4def9...`. Only what differs from that
source is listed; everything else (numbers, ore weights, research keys and positions, aspects, recipe, tooltips, sounds,
messages, config use) is ported as it is.

How this was checked (honestly): the add-on compiles with `tools/compile.sh` (whole tree and with `-g`). The pure logic and the
data files are covered by JUnit tests (`common/src/test/java/thaumic/tinkerer/kamiworld`, 38 checks) that were first **run with a
hand-written stand-in for JUnit Jupiter** (the real one was not available offline then) and now run under the real one (below): they pass. The
dimension type and biome JSON were parsed with the game's own codecs (`DimensionType.DIRECT_CODEC`, `Biome.DIRECT_CODEC`,
`ChunkGeneratorBedrock.CODEC` over the dimension file's generator object) in a throw-away program on the game classes, and the
chunk fill, the ore selection with bound item tags and the ore clusters through `applyBiomeDecoration` (with a proxy world) were
run there as well. The GameTests (`KamiWorldRuntimeChecks`, 20 checks) run on a real Fabric dedicated server
(`:fabric:runTinkererGameTest`, filter `kamiworldgametests.*`) and **pass there**: 17 of 17 in the integration round (section 10 says what
that round changed in the tests and why), 19 of 19 after the hooks round (the groups moved from their workarounds to the real events of
`docs/HOOKS.md`: the hooks-round bullet of section 10; run with the 12 `hooksgametests.*` of that day, 31 of 31) and 20 of 20 after the review round
(the review-round bullet of section 10; run with the 13 `hooksgametests.*` that exist now, 33 of 33). The JUnit tests of the group
(`KamiWorldLogicTest` 24, `KamiWorldResourcesTest` 14) pass under the real JUnit Jupiter (`:fabric:tinkererTest --tests 'thaumic.tinkerer.kamiworld.*'`). Nothing here has been seen in a running client:
the screens, the HUD, the model of the gateway and the renderer are untested visually, and a hit taken in the survival of a real
client was never seen either.

## 1. Identity: registry paths, data

| 1.7.10 | 1.21.1 |
|---|---|
| block `warpGate` (+ `ItemBlockWarpGate`, a plain item block that was never registered on its own) | block and item `thaumictinkerer:warp_gate` (`LibBlockNames.WARP_GATE`), block entity type and menu type of the same path |
| block `portal` (`BlockBedrockPortal`; `getItemBlock()` returned null, so `GameRegistry` registered the default item block, and the block was in the creative tab) | block `thaumictinkerer:bedrock_portal` (`LibBlockNames.PORTAL`) with the default item, a creative tab entry and an item model, no block entity (section 3) |
| dimension id -19 (`bedrockDimensionID`, config), `WorldProviderBedrock`, `ChunkProviderBedrock` | the dimension `thaumictinkerer:bedrock` (`WorldProviderBedrock.KEY`): `data/thaumictinkerer/dimension/bedrock.json`, `dimension_type/bedrock.json`, `worldgen/biome/bedrock.json`, generator type `thaumictinkerer:bedrock` (`ChunkGeneratorBedrock`, a `DeferredRegister` of `Registries.CHUNK_GENERATOR` in `KamiWorld`) |
| ore dictionary names of `EnumOreFrequency` | the same names, resolved to item tags by `OreDictionaryNames`; "first ore" is the member with the lowest registry id |
| `PacketSoulHearts`, `PacketWarpGateButton`, `PacketWarpGateTeleport` (`SimpleNetworkWrapper`, ids 142+) | `CustomPacketPayload` records `soul_hearts` (S2C), `warp_gate_button`, `warp_gate_teleport` (C2S) |
| `LibGuiIDs.GUI_ID_WARP_GATE`, `GUI_ID_WARP_GATE_DESTINATIONS` | menu type `warp_gate` (`ContainerWarpGate`); the destinations screen has no container and is opened by the block entity on the client (`TileWarpGate.setDestinationsOpener`) |
| `GuiWarpGate`, `GuiButtonWG`, `GuiWarpGateDestinations`, `SoulHeartClientHandler`, `RenderTileWarpGate`, `RenderWarpGate` (ISBRH) | the same names under `thaumic.tinkerer.client` (section 8); the ISBRH is a JSON model plus the block entity renderer |
| `KamiUnlockedCommand` (`CommandBase`) | `thaumic.tinkerer.common.core.commands.KamiUnlockedCommand`, a Brigadier literal registered with `CommandRegistrationEvent` |
| `DimensionalShardDropHandler`, `SoulHeartHandler` (Forge event handlers) | `core.handler.kami.DimensionalShardDropHandler` (a listener of `TinkererEvents.LIVING_DROPS`) and `SoulHeartHandler` (a listener of `TinkererEvents.LIVING_DAMAGE`, Architectury events for the player's login, respawn and logout), registered by `KamiWorld.register()` |

Everything of the group is registered only when `ConfigHandler.enableKami` is on (1.7.10: `shouldRegister() == enableKami` and
the handlers registered in `if (enableKami)`), with **one exception**: the chunk generator type `thaumictinkerer:bedrock`. The
dimension is a file of the data pack that names that type, and a data pack that cannot decode a dimension stops the world from
loading; with Kami off the dimension exists but nothing leads there and nothing in the add-on touches it. The
`ThaumicTinkerer.preInit` registration of the provider type and the dimension (`DimensionManager`) and the config option
`bedrockDimensionID` (0 disabled the dimension) are gone; `enableKami` is the switch.

Classes of the original not ported as classes: `TileBedrockPortal` (section 3: it never ran in 1.7.10), `KamiDimensionHandler` (already ported by the
KamiGear group as `ItemIchorPickAdv.onLeftClickBlock`, which the KamiGear docs say KamiWorld must not register again),
`WorldProviderBedrock`'s methods (section 2: the class remains as the home of the dimension key and two predicates),
`BlockBedrockPortal.travelToDimension` (a public copy of `Entity.travelToDimension` that nothing called),
`PacketTile` (the shared `PacketTile` helper resolves the block entity for the button packet).

## 2. The Bedrock dimension

- **Shape.** 1.7.10's world height is 256, so the dimension type is `min_y 0`, `height 256`, `logical_height 256`: the top two
  layers (y above 253, `WorldProviderBedrock.PORTAL_LAYER_ABOVE`) are where the Awakened Pickaxe makes the exit portal, as in the
  KamiGear group's `convertBedrock`. Sky light on, no ceiling, `ultrawarm` false, `natural` false (`isSurfaceWorld()` was false),
  `bed_works` and `respawn_anchor_works` false (`canRespawnHere()` was false), `has_raids` false.
- **Light.** `generateLightBrightnessTable` made every light level full brightness (the table values are above 1): `ambient_light`
  is 1.0, which does the same in 1.21.1 (no darkness at night or in a tunnel). Light levels still exist for what depends on them
  (monster spawning: `monster_spawn_light_level` 0 to 7 as in the overworld).
- **Sky and fog.** The original used the beach biome, the sun, moon and day cycle of a normal world (its `calculateCelestialAngle`
  and `calcSunriseSunsetColors` are the vanilla formulas) and the End's fog colour dimmed to 15 percent: `getFogColor` returned
  `0xA080A0 * 0.15` = `0x181318` at all times. The dimension type uses the `minecraft:overworld` effects (a mod cannot register
  its own `DimensionSpecialEffects` in common code), and the biome carries `fog_color 0x181318`. Difference: the overworld
  effects scale the fog colour with the time of day (night fog is darker than the constant colour of 1.7.10), and the sky colour
  is that of the beach (`7907327`).
- **Biome.** A data biome `thaumictinkerer:bedrock`: the beach's temperature and rain, no features, no carvers, the monsters of
  the 1.7.10 default list (spider, zombie, skeleton, creeper, slime weight 100 in groups of 4; enderman 10, 1 to 4; witch 5), no
  animals (the beach cleared them), bats and squid. 1.7.10's zombie villagers and 1.21's other new mobs are not added.
- **Terrain.** `ChunkGeneratorBedrock` fills every block of every chunk with bedrock (`fillFromNoise`) and primes the generation
  heightmaps; `applyCarvers`, `buildSurface`, `spawnOriginalMobs` do nothing; `getBaseHeight` is the top and `getBaseColumn` is
  all bedrock; the sea level answers 1 (`getAverageGroundLevel`). No structures: the fixed biome source has no structure set.
- **Ore clusters.** `OreClusterGenerator.populate` is `ChunkGeneratorBedrock.applyBiomeDecoration` (1.7.10: `populate`): the random is
  seeded exactly like the original (`chunkRandom`: two odd multipliers drawn from the world seed, mixed with the chunk
  coordinates, then the three unused draws of the lake generator), `ConfigHandler.bedrockOreDensity` clusters per chunk (a column,
  one ore picked by weight), 200 veins of 0 to 19 blocks each at heights 6 to 250 that replace **bedrock only**
  (`isReplaceableOreGen(bedrock)`). The vein is the 1.7.10 `WorldGenMinable.generate` loop line by line (the table `sin`, the
  order of the draws), now `OreClusterGenerator.generateVein`. A vein reaches at most the chunk and the one east and south of it
  (it starts 8 to 23 blocks into the chunk's column and swings at most four blocks), which the generation region of the features
  step allows; the written area is checked (`reaches`). The outer layers (y 0 and 1, 252 to 255) never hold ore: a vein starts at
  y 250 at most and its ellipsoids reach 251 at most. **The portal cavity is not safe from ore** (this document said so before
  and was wrong): the layers 250 and 251 of a column can hold an ore block (measured with the real `generateVein`, 2,000,000
  veins started at y = 250: 36.3 percent placed a block at y 250, 0.35 percent at y 251, none above 251). The entrance only clears
  stone and bedrock (section 3), so an ore block of a vein stays in the cavity, as in 1.7.10 (the original cleared the same two
  blocks); the traveller who arrives at 251 then stands in it for the rare vein that reaches 251 (and on it for a vein at 250).
  Kept as it was: it needs a vein start at the very top of the range to cross a given column. Blocks are placed with the flag
  "update clients" (1.7.10: flag 2).
  Differences: (a) the veins are generated per chunk when it reaches the features step, in the order the server generates
  chunks, so a vein in a neighbour is shaped by what that neighbour already took (1.7.10 had the same order dependence through
  `populate`); (b) with no valid ore at all the cluster is skipped (1.7.10 threw on `nextInt(0)`); (c) the iron-ore fallback of
  `getRandomOre` is not needed (the weight walk cannot fall through).
- **Ore names to blocks.** `OreDictionary.getOres(name).get(0)` is the member of the name's item tag with the lowest registry id
  (the vanilla ore before its deepslate variant; the infused Thaumcraft ores through `thaumcraft:original_ore/*`). A name is
  valid when its tag has members and that first member is a block item (the 1.7.10 `isValid` conditions), so ores that no mod of
  the instance adds (aluminium, desh, uranium, "cheese"...) are skipped like an empty ore dictionary entry. `oreNetherQuartz`
  falls back to `c:ores/quartz` (the shared convention name; the API's rule would say `c:ores/nether_quartz`, which nobody fills).
  The 1.7.10 default blacklist entry `oreFirestone` is not an ore of the table and was never needed.
- **Config.** `bedrockOreBlacklist` entries are 1.7.10 ore names (`oreCoal`), item tag ids (`c:ores/coal`, a leading `#` is
  allowed) or block ids (`minecraft:coal_ore`); `bedrockOreDensity` 0 to 1023 as before.
- **Thaumcraft.** `ThaumcraftApi.addDimBlacklist(thaumictinkerer:bedrock, 2)` keeps Thaumcraft's ores, aura nodes and vegetation out.
- **Not ported (no effect in 1.7.10 either):** `renderStars`, `getStarBrightness`, `renderClouds`, `renderVoidFog`, `renderEndSky`,
  `setSunSize`, `setMoonSize` (no `@Override`, nothing called them), `getEntrancePortalLocation`, `getWelcomeMessage`,
  `canCoordinateBeSpawn`, `doesXZShowFog`, `getCloudHeight`.

## 3. The Bedrock portal

The block keeps its 1.7.10 properties: hardness -1 and resistance 6000000 (unbreakable, no drops: no loot table), no collision
box, not opaque, stone sound, `canEntityDestroy == false` (the block is in `minecraft:wither_immune` and `dragon_immune`, two
files in the `minecraft` namespace that are new in this group), `canHarvestBlock == false`. Right-clicking it turns it back into
bedrock and the click goes on (a held block is placed against it), as in `onBlockActivated`.

- **Travelling.** Players only, in both directions. 1.7.10 sent a player from `onEntityCollidedWithBlock` (`entity instanceof
  EntityPlayer`, on the way in and on the way out) with a `TeleporterBedrock` that placed nothing. The block also named a
  `TileBedrockPortal` whose `updateEntity` would have sent *every* entity in the block to the dimension, but that tile entity
  never existed in a world: `BlockBedrockPortal extends BlockMod` (a plain `Block`), is no `ITileEntityProvider` and does not
  override `hasTileEntity`, which then answers false (the default of Forge 1.7.10's `Block.hasTileEntity(int)` is `this instanceof ITileEntityProvider`), so `createTileEntity` was never called and `updateEntity` never ran (the
  class was only registered by name). Items, mobs, arrows and boats therefore always passed through the portal untouched, and
  they still do. An earlier version of this port assumed the tile worked and sent every entity (and built a Nether portal in the
  Bedrock dimension for each); that was wrong and is removed: `entityInside` and `getPortalDestination` accept `Player` only.
  In 1.21.1 the block is a vanilla `Portal`: `entityInside` hands the player to the game's portal processing, which changes its
  dimension at the end of its tick with no waiting time and no screen effect, to the `DimensionTransition` of
  `getPortalDestination`. The tile entity is not needed and is not ported. Differences: (a) the 1.7.10 portal checked
  `isSurfaceWorld()`; the 1.21.1 predicate is "has sky light, no ceiling and is not the Bedrock dimension"
  (`WorldProviderBedrock.isSurfaceWorld`), which is the Overworld and similar mod dimensions, not the Nether or the End; (b) the
  game's portal cooldown (10 ticks for a player) applies.
- **In** (`TeleporterBedrock.toBedrock`): the Bedrock column with the portal's x and z is loaded, layer 249 becomes stone if it
  is air, layers 250 to 253 are cleared when they are stone or bedrock (other blocks stay), the bedrock at 254 becomes the
  portal, and the traveller arrives at the middle of the block at y = 251 with yaw and pitch 0, no momentum.
- **Out** (`TeleporterBedrock.toSurface`): the Overworld (dimension 0 whatever the player came from), a random column
  `(int) x + nextInt(100) - 50` (and z; the original read `entity.posX` after `transferPlayerToDimension`, which in the Forge
  1.7.10 of the fork scales the position by `oldProvider.getMovementFactor() / newProvider.getMovementFactor()`, 1 / 1 for the
  Bedrock dimension and the Overworld since neither overrides it, so it is the portal's own x and z, as here; the 8-fold
  scaling is the Nether's. This rests on a recollection of that Forge patch: no 1.7.10 server source is on disk to check it), scanned down from y = 120 for the first block that is not air (leaves and water count),
  three blocks above it; a column that is air all the way down gets a stone at 119 first (1.7.10: at `y <= 1`; the floor is the
  world's bottom + 1). **Deviation:** the 1.7.10 scan started at 120 even inside rock, so a landing under a mountain put the
  player inside it; here a start cell that is not air sends the scan to the top of the column (`WORLD_SURFACE` heightmap), and
  the player lands three blocks above that.

## 4. Celestial Gateway

- **Block.** Hardness 5, resistance 2000, stone, needs a pickaxe (any: `mineable/pickaxe`, `requiresCorrectToolForDrops`), not
  opaque, creative tab entry in the Kami rarity (`Rarity.EPIC`, the stand-in the Kami items use). Drops itself and the pearls.
  Right-click opens the menu with any hand (also with a pearl in it; sneaking with the pearl attunes it, the pearl's code).
- **Model.** 1.7.10's `RenderWarpGate` (ISBRH): top face icon 0, other faces icon 1, plus a layer with icon 2 on the top face at full
  brightness. `models/block/warp_gate.json` is a cube with those textures (the top texture has transparent pixels where the
  frame is; the block uses the cut-out render type), and `RenderTileWarpGate` draws the layer (`warp_gate_2`, only white pixels,
  exactly the holes) at full brightness over it, as a block entity renderer, because plain 1.21.1 JSON models have no emissive
  faces on Fabric. The item model (`models/item/warp_gate.json`) has the layer as a second element (not glowing).
  The twelve spinning cubes (five rings) above the gate, half a turn about the (1, 0, 1) axis, are `ModelSpinningCubes` (ported
  by the KamiGear group for this) at the same place, driven by the client player's tick count plus the partial tick.
- **Block entity and menu.** Ten slots, stack limit 1, only Celestial Pearls (`SlotSkyPearl`), the flag `locked`, saved under
  the keys `locked` and `Items` (the key `locked` is the one `KamiHooks`' default teleporter reads). The menu layout is the
  original's (two rows of five at `(30 + 25 x, 27 + 25 y)`, the inventory at (8, 84)); shift-click moves pearls only, from the
  gate into the first 26 slots of the inventory (the original merged `10..36` exclusive, so the last slot of the inventory and
  the hot bar are never filled) and from the inventory into the gate. The flag is a data slot of the menu. The screen
  (`GuiWarpGate`) draws the 13 by 13 button at (5, 5), the sprite at (176, 13) when off and (176, 0) when on, and the label
  "Allow incoming teleports" in 0x999999 with a shadow; a click flips it and sends `PacketWarpGateButton`.
- **Teleport.** The same checks and messages: the destination must be a gateway that does not refuse (`ttmisc.noDest`,
  `ttmisc.noTeleport`), the player is taken off a mount and put at `(x + 0.5, y + 1.6, z + 0.5)` of the destination block with the
  sound `thaumcraft:wand` before (pitch 1) and after (pitch 0.1) at volume 1, once per gateway and tick (`teleportedThisTick`,
  cleared by the block entity tick). The sparkles of the original (20 before the jump, 20 after) are written inside
  `if (player.worldObj.isRemote)` in `teleportPlayer(EntityPlayer, ChunkCoordinates)`: they showed on the client of a player
  who used the Focus of Celestial Recall (its `onUsingFocusTick` runs on both sides), and never on the gateway's own menu and map
  path (the packet handler `teleportPlayer(player, slot)` runs on the server only). Ported for the Recall path: the server sends
  them to the clients (`KamiHooks.sparklesAround`, `teleportPlayer(ServerPlayer, BlockPos)`, which the registrar gives to
  `KamiHooks.warpGateTeleporter`); the menu and map path has none, as before. The sound is played for everyone nearby **including**
  the teleported player (`level.playSound(null, ...)`; 1.7.10's `playSoundAtEntity` reached every player).
- **Cross-dimension (requested, an extension).** 1.7.10 looked for a gateway at the pearl's coordinates in the *player's own*
  dimension, so its map skipped the pearls of other dimensions and the pearl's `dim` only decided a red "Different Dimension" line.
  Here a pearl attuned to another dimension takes the player to the gateway at those coordinates in that dimension
  (`changeDimension`, loading the chunk), with the same checks and messages; a dimension that does not exist is "no
  destination". The map still shows only the pearls of the player's dimension; the others are listed on the left with the red
  "Different Dimension" note and the id of their dimension in the tooltip, and shift and left click on such a row teleports.
  The Focus of Celestial Recall stays same-dimension (it asks `KamiHooks.warpGateTeleporter`, which the registrar points at
  `TileWarpGate.teleportPlayer(player, pos)`).
- **Destinations screen** (`GuiWarpGateDestinations`). Opened by the *client* player sneaking in the space above the gate
  (`updateEntity` on the client; here the block entity's client tick, through a hook set by the client registration, only while no
  other screen is open). Same layout and texts: the End-portal texture tinted with a drifting hue, a map of x across and z down
  dragged with the mouse, a line from the gateway to each destination (white, width 2, pulsing alpha; drawn as quads), the pearl
  icon at half size at every dot, the list on the left (underlined title, `n: name` at `(5, 54 + 11 n)`), the two centred help
  lines, tooltips with the name, "(Destination n)", X, Z, the distance without the height, and "Shift + Left Click to Teleport";
  the number keys 1 to 9 and 0 centre the map on the pearl of that slot, space on the gateway. Shift and left *click* replaces
  the polled "button down and shift while over a dot". Names come from the pearl's custom name (an anvil). Differences: the
  1.7.10 map skipped a pearl whose attuned y was -1 (the old "not attuned" marker); the pearl is attuned by a component now and
  y = -1 is a real height, so no pearl is skipped for its height. The screen closes when its gateway is removed.
- **Packet hardening.** `PacketWarpGateTeleport` accepted any gateway of the world from any player; it now needs the gateway in
  the sender's own loaded level and the sender within 8 blocks of it (the reach of the menu). `PacketWarpGateButton` needs the
  gateway's menu open on the sender (the shared `PacketTile.resolve`).

## 5. Soul Hearts

- **The pool.** Up to 20 points (ten hearts). `addHearts` adds 1 point (the hook of the Awakened Sword's Soul Mode,
  `KamiHooks.soulHeartSink`), `addHP` clamps to 20 and refuses a full pool, `removeHP` returns the overflow, `setHP`/`getHP`.
  1.7.10 stored the number in the player's entity data, which a respawn does not copy; here it is saved with the world by player
  UUID (`SoulHeartData`, the server's overworld data storage) and **cleared** when a player is re-created by a respawn
  (`PLAYER_CLONE`, death or the end poem), so the hearts are still lost on death. It survives logout and dimension changes as before.
- **The client.** `PacketSoulHearts` after every change, and **on login and respawn** (1.7.10 sent nothing on login, so the bar
  was empty until the next change; a client also kept the number of the last world, which is cleared on leaving).
- **The HUD.** `SoulHeartClientHandler`: hearts 9 pixels (half hearts 7) wide, 8 apart, from `width / 2 + 10`, at
  `height - ConfigHandler.soulHeartHeight` (49: the row above the food bar), 10 higher while the air bubbles are shown, from
  `textures/gui/soul_hearts.png` (16 by 16: the full heart on the left, the half heart at u 9). Drawn from
  `ClientGuiEvent.RENDER_HUD` after the vanilla HUD, only where the health bar is drawn (not in creative or spectator, not with the
  HUD hidden). Since the draw is after the vanilla HUD, it lies over the air bubbles when the air row and the soul-heart row
  coincide at the default height; the 10-pixel shift avoids that as in 1.7.10.
- **Spending.** 1.7.10 spent the pool in `LivingHurtEvent`, before armour, with
  `event.ammount = removeHP(player, (int) event.ammount)`: **every** hit of every player is truncated to its whole points
  (a hit under 1 point is swallowed, the fraction of the others is lost) and what the pool covers is taken off, whether the
  player has hearts or not, and the player's client is sent the pool after every hit. This is now ported **exactly**, through
  the real event: `SoulHeartHandler.onLivingDamage`, a listener of `TinkererEvents.LIVING_DAMAGE` (`docs/HOOKS.md` section 3,
  the position of `LivingHurtEvent`), registered once by `SoulHeartHandler.register()`, answers for every `ServerPlayer`
  victim (nothing else: a pig or a zombie gets its amount back untouched) and calls
  `modifyIncomingDamage` (the rule above). The Architectury `LIVING_HURT` fallback of the first version (cancel the hit, deal
  the rest again as a new hit with the difficulty scaling undone; players without hearts left alone; `exactDamageHook`,
  `beforeDifficultyScaling`) is **deleted**: there is no second path. The scaling by difficulty happens before the event in
  both games (`Player.hurt` / `EntityPlayer.attackEntityFrom`), so the amount the pool sees is the scaled one as in 1.7.10.
  **The group needs the poster of `LIVING_DAMAGE` on every loader: on Fabric it is `LivingEntityHurtMixin`; on NeoForge (the
  `LivingIncomingDamageEvent` handler of `HOOKS.md` section 3, with `TinkererEvents.framesRefuse`) it does not exist yet, and
  until it does the Soul Hearts do not act there at all** (the fallback ran on NeoForge before; it is gone).
- **The invulnerability frames (what the event contract changes).** In 1.7.10 `EntityLivingBase.attackEntityFrom` set
  `hurtResistantTime = 20` **before** `damageEntity` posted the event. So a hit that the pool swallowed completely still began
  the frames: while `hurtResistantTime > 10` (nine more ticks) a hit that was not stronger than `lastDamage` (the *raw*
  amount of the first) was refused before any event, a stronger one reached the event as `amount - lastDamage`, and the hit ten
  ticks after the first was a new one. A source that hits every tick (lava 4, a fire block or a cactus 1) therefore cost the pool
  once per ten ticks. In 1.21.1 a cancel (the listener returns 0) is a refusal of the whole hit and begins **no** frames
  (`HOOKS.md` section 3), and the frames that do run compare the next hit with what the listener *returned*, not with the raw
  amount; left alone, lava would be posted every tick and the pool of 20 would be gone in five ticks, about ten times faster
  than in 1.7.10. Two ways were open (both are in `HOOKS.md`): return the uncovered rest and let the game's frames count, or keep
  a tick stamp per player. **The first does not work alone**: a fully covered hit has no rest (it must be cancelled), and even a
  partly covered one comes back on the next tick because the game's frames compare it with the *rest* (lava 4 against a rest of
  1 is "stronger" and is posted again). So the handler **keeps the frames of the original itself**, per player
  (`SoulHeartHandler.Frames`, a map by UUID in memory, dropped on logout, respawn and server stop; nothing is saved): the game
  tick of the hit that began them, the strongest raw amount so far (`lastDamage`) and what the game's own `lastHurt` holds because of
  the amounts handed back to it (`handed`, which outlives the pool's frames, see the third item). For every hit the event posts for a
  server player:
  - outside the frames (none yet, ten ticks over, or a source of the damage-type tag `bypasses_cooldown`, which the game does not
    hold to the frames either; the tag is empty in vanilla 1.21.1): the whole hit is the event's amount, the pool is charged,
    new frames begin at this tick;
  - inside them: a hit not stronger than the strongest so far is cancelled and **does not charge the pool** (the original refused
    it before the event); a stronger one is charged for the difference (`amount - lastDamage`) only;
  - what is left after the pool (`rest`) is what the game is to deal, exactly, and the game turns the amount it is handed into
    damage by its **own** rule: while its frames run (`invulnerableTime > 10`, the source not in `bypasses_cooldown`) it deals the
    amount minus its `lastHurt` and sets `lastHurt` to the amount; otherwise it deals the amount and starts its frames with
    `lastHurt` = the amount. So the pool hands back `handed + rest` when the game's frames run and `rest` alone when they do not,
    and it asks the **game's** frames (`invulnerableTime`), not its own: they begin at different times (a hit the pool covers begins
    the pool's frames and not the game's) and either can end first. A hit with no rest is cancelled.

  The two cases that tell the pool's frames from the game's are in the GameTest
  `soulHeartsAgreeWithTheOriginalWhenTheGamesFramesBeginLater` (pool 5, generic damage, hits at `b`, `b+3`, `b+10`, `b+14` of 3, 6, 3, 5):
  `b` is covered (the pool's frames begin), at `b+3` the event gets 3, 1 goes on and the game's frames begin three ticks late (health 19);
  at `b+10` the pool's frames are over and the game's run until `b+13` with `lastHurt` 1, so the game must be handed 1 + 3 to deal the 3
  (health 16); at `b+14` the pool's frames (from `b+10`) run and the game's are over, so the game must be handed 2 and not 3 + 2 (health
  14). Those are the numbers of 1.7.10. (A first version kept the sum of the rests per pool frames and handed it back whenever the pool's
  frames ran: it dealt 17 at `b+10`, one too little, and 12 at `b+14`, three too much, because the game's frames were over and it dealt
  the whole 3 + 2.)

  The GameTest `soulHeartsAreChargedOncePerTenTicksByLava` lets two mock players stand in real lava (4 points a tick): the one
  with a full pool pays 4 points every ten ticks, exactly, and loses no health, the one whose pool of 3 does not cover the first
  hit loses 1, then 4 every ten ticks (the numbers 1.7.10 gives). `soulHeartsKeepTheFramesOfTheOriginal` pins each case of the list
  above by tick, including the tick-ten boundary and the partly covered, stronger hit, and what the player's client is sent: the pool after
  every charged hit (also an empty pool, also a hit below one point) and nothing for a refused one.
- **Consequences of the choice (differences from 1.7.10, all forced by the event contract).**
  - A hit that the pool **covers entirely is refused** (`hurt` returns `false`, as for an invulnerable player): no knockback, no
    hurt animation or sound **from the game**, no `lastHurtByMob` (the attacker is not remembered), an arrow that is stopped bounces
    off as against an invulnerable player and a melee attacker sees a refused hit (no Fire Aspect or Knockback of its own on that
    blow, no thorns). Two more things of the refusal: **a sleeping player is not woken** by a hit the pool covers (in 1.21.1 the wake-up is
    in `LivingEntity.hurt`, after the point where the event is posted and a cancel returns; 1.7.10's `EntityPlayer.attackEntityFrom` woke the
    sleeper before `super`, so a covered hit woke him: recalled, the vanilla 1.7.10 source is not on disk) and **a raised shield is not used**
    for it (no durability lost, the attacker is not pushed back or disabled by it: `hurtCurrentlyUsedShield` and `blockUsingShield` come after
    the event too). For a hit the pool only partly covers the shield works as usual, on the rest, but the pool has been charged with the
    whole pre-block amount (`HOOKS.md` section 3: the event comes before the shield), so a blocking player pays the pool for damage the
    shield then takes. 1.7.10's cancel only skipped `damageEntity`, so the covered hit still knocked back, showed the red flash and
    played the hurt sound. To keep what can be kept, the handler sends the **damage event** (`ServerLevel.broadcastDamageEvent`)
    for a covered hit that *begins* the frames, so the client plays the hurt animation and sound; a refused repeat and a stronger hit
    inside the frames are silent (1.7.10 gave neither there either). The knockback of a covered hit is lost. (What the refusal does to
    the attacker was read from the 1.21.1 code of `Mob.doHurtTarget`, `Player.attack`, `AbstractArrow.onHitEntity` and the end of
    `LivingEntity.hurt`, not run; the GameTests prove the refusal itself, the pool and the damage event.)
  - The game's invulnerability frames do not begin for a covered hit (`invulnerableTime` stays 0 and a mob's follow-up hit is posted
    again at once); the pool's own frames stand in for them, which is what the numbers above rely on. Other mods that read
    `invulnerableTime` (a thorns-like effect, a `hurtTime` renderer) see a player who was not hurt.
  - A hit that the pool only **partly** covers is dealt and begins the game's frames as in 1.7.10, with the *rest* as `lastHurt`.
  - **Where the game's `lastHurt` is not what the pool handed.** The pool knows the game's `lastHurt` only as the last amount it handed
    back: the field is protected, common code cannot read it (the poster of each loader does, for `framesRefuse`), and a request for a
    public reader was not made. Three things change it behind the pool's back: a raised shield (the game sets `lastHurt` to the amount
    after the block, 0), a helmet against a falling block (the amount is scaled by 0.75 before the game compares it), and a listener
    registered **after** the pool that changes or cancels the amount it handed back (the pool has already been charged; no listener of this
    group does that, whether another group's does is for that group and the order of the groups in `ThaumicTinkerer`). Then a stronger
    hit inside the game's frames (at most ten ticks) is handed `handed + rest` against a `lastHurt` that is not `handed`, and the game
    deals another amount than `rest`: more after a shield (up to the damage of the hit before), more or less, even none, when a helmet
    scaled a falling block, or whatever the later listener changed. Not exercised by a check; the pool itself is charged right in every case.
  - `bypasses_cooldown` sources start new frames every time (the pool's too); empty in vanilla, so **not exercised** by a check.
  - 1.7.10 reduced a falling anvil/block by a quarter for a helmet *before* the frames; here that happens after the event (`HOOKS.md`
    section 3, item 7), so such a hit is charged at its full amount.
  - The pool is charged on the server only; the client learns the number from `PacketSoulHearts`, sent after every hit that is
    charged (and not for a hit the frames refuse), as 1.7.10 sent it after every event.
  - **Other listeners of `LIVING_DAMAGE` and the pool's frames.** `ContentGroups.ALL` runs the Enchantments group before this one, so its
    listener (`ModEnchantmentHandler.onEntityDamaged`) is earlier in the chain, as `ModEnchantments.initEnchantments()` was before the
    registration of `SoulHeartHandler` in 1.7.10's `TTCommonProxy` (the order is the same). Two consequences of the pool keeping its own
    frames. (a) The poster filters only the *game's* refusals (`TinkererEvents.framesRefuse`); a hit that the **pool's** frames refuse
    (a repeat inside ten ticks after a covered hit, which started no game frames, or a weaker one after a partly covered hit, whose
    `lastHurt` is the truncated rest) is still posted, so the listener of the Enchantments group runs for it first and its side effects
    happen (Vampirism heals the attacker and plays the zap, the counter of Focused and Dispersed Strikes on the weapon advances), and then
    the pool refuses the hit. In 1.7.10 that hit was refused before any event and had none of them. Example: player A with a Vampirism
    sword hits player B for 7, the pool covers it; three ticks later A hits for 6: A is healed again and the counter advances, though
    the hit does nothing. It concerns every player victim (the pool and its frames apply to all of them) and an attacker with those
    enchantments. (b) The amount the pool sees, and the `lastDamage` it compares, is the one the earlier listeners left (Pounce, Final
    Strike, Valiance and the rest multiply it), where 1.7.10 compared the raw amount of `attackEntityFrom`; the pool was after the
    enchantments in 1.7.10 too, and for the same factor on both hits the amount it charges is the same, but the *comparison* of a repeat
    uses the multiplied amounts (Final Strike rolls a new factor for every hit, so a repeat can count as stronger or weaker than it did). Neither can be put right inside this group: a hook that asks the pool's frames **before**
    any listener runs (a registered refusal test consulted by `TinkererEvents.framesRefuse`, with the raw amount) would give both back; it
    is a request to the lead (`docs/api-requests/KamiWorld.md` section 1), the Enchantments group could also guard its side effects.
- **Dead code of the original** (`applyArmorCalculations` and `applyPotionDamageCalculations`, copies of `EntityLivingBase`
  methods nothing called) is not ported.

## 6. Dimensional shards

`DimensionalShardDropHandler`: an Enderman killed by a player in the End drops an Ender Shard with chance 1/32, a Zombified
Piglin killed by a player in the Nether a Nether Shard with chance 1/16 (1.7.10 metadata 7 and 6 of `ItemKamiResource`, here the
items `ender_shard` and `nether_shard`). Ported through the real event: `onLivingDrops`, a listener of
`TinkererEvents.LIVING_DROPS` (`docs/HOOKS.md` section 8, the `LivingDropsEvent` of the original), registered once by
`DimensionalShardDropHandler.register()`. It is the 1.7.10 handler line by line: the blow came from a player
(`source.getEntity() instanceof Player`: the attacker of the killing blow, the shooter of an arrow or the thrower of a potion
counts, a wolf does not; the Animation Tablet's fake player is a player, as a `FakePlayer` was an `EntityPlayer`), the victim is an
`EnderMan` in the End or a `ZombifiedPiglin` in the Nether (`instanceof`, so a subclass counts again; the dimension is
`Level.END` / `Level.NETHER` where 1.7.10 compared the ids from the config) and `Math.random() <= 1D / 32D` (`1D / 16D`), the
roll made only when the first two hold. The shard is a new `ItemEntity` at the victim's position added to the list, as the
original added an `EntityItem` (the `ItemEntity` constructor gives it the same small random motion, 0.2 up, and, like the `EntityItem`
constructor (recalled: the vanilla 1.7.10 source is not on disk), no pickup delay: the game's own drops get the delay of 10 only through
`spawnAtLocation`, which neither handler uses);
what the loot table, the equipment and the other listeners put in the list is left alone, Looting does not change the chance (it
did not), and the shard is dropped whether or not the kill was "recent" and whatever `doMobLoot` says (the handler of the original
did not ask either; the Fabric poster wraps the whole of `dropAllDeathLoot`, the rule check included, so it posts whatever the rule says: read from the
mixin, not run). The loot-table pools that the first version added to
`minecraft:entities/enderman` and `minecraft:entities/zombified_piglin` (`LootEvent.MODIFY_LOOT_TABLE`, the conditions as predicates,
a data pack's replacement of the tables modified as well) are **deleted**, and with them their differences (a modded subclass that
uses another loot table dropping nothing; the shard rolled by the loot system, where a data pack could change it). If the shard item
is not registered (the Kami gear group failed to load) the listener logs one warning and drops nothing: an exception in a listener
would be thrown into the mob's death. Differences that remain: none known in behaviour. **The group needs the poster of
`LIVING_DROPS` on every loader**: on Fabric it is `LivingEntityDropsMixin` with the capture mixins; on NeoForge (`LivingDropsEvent`,
`HOOKS.md` section 8) it does not exist yet, and until it does no shard drops there (the loot-table pools worked on NeoForge before; they are gone).

The chance is rolled with `DimensionalShardDropHandler.dice` (`Math::random`), a public field that only the GameTests replace to make a roll
certain; the real dice are checked on 12,800 real deaths (`shardDropsComeFromRealDeathsWithTheChanceOfTheOriginal`).

The research `DIMENSION_SHARDS` belongs to the KamiGear group's `ItemKamiResource`; its page text embedded
`<IMG>ttinkerer:textures/items/netherShard.png...`, which no longer exists: `lang-extra/kami_world.<lang>.json` (six languages)
overrides `ttresearch.page.DIMENSION_SHARDS.0` with `thaumictinkerer:textures/item/nether_shard.png` and `ender_shard.png`.

## 7. `/iskamiunlocked`

Permission level 0 (everyone). Sends the player "Yes", or "No" and "Remaining: key,key" (the hidden parents of the `ICHOR`
research the player has not completed; 1.7.10 read `ResearchHelper.kamiResearch.parentsHidden`, here
`ResearchCategories.getResearch("ICHOR")`). The original crashed for a console sender (a cast); the command now answers "A player
is required" for a non-player. The three words are literal English texts, as in the original.

## 8. Client and resources

- Screens `GuiWarpGate`, `GuiWarpGateDestinations`, button `GuiButtonWG`, HUD `SoulHeartClientHandler`, renderer
  `RenderTileWarpGate`, registration `KamiWorldClient` (called from `KamiWorld.registerClient()`; the S2C receiver of
  `PacketSoulHearts`, the cut-out render type of the gate, the opener of the destinations screen).
- `models/block/warp_gate.json`, `models/item/warp_gate.json`, `models/block/bedrock_portal.json` (all six faces; the top and
  the bottom are always drawn and a side is culled by an opaque neighbour, which is what 1.7.10's `shouldSideBeRendered` came to;
  the portal texture is animated, 3 ticks per frame), blockstates, the gate's loot table, the pickaxe tag (an entry with
  `required: false` so that a world with Kami off still loads the tag file), `data/minecraft/tags/block/wither_immune.json` and
  `dragon_immune.json`.
- Language: no new key is needed (the texts of the screens, the messages, the research text and the block names are in the
  generated base); `lang-extra/kami_world.<lang>.json` only carries the shard page fix. German and Korean have the literal
  "None" as the name of the portal in the converted base (the 1.7.10 files say so).

## 9. Deliberately not carried over

- `y == -1` hiding a destination on the map (section 4).
- The configurable dimension id and the option to disable the dimension (section 1).

## 10. Open items and cross-group references

- **Gateway tooltip (Jade).** The original `TTinkererProvider` (Waila, lines 94-98) showed one line over the Celestial Gateway:
  `ttwaila.allowIncoming` when the block entity's `locked` flag is **true** and `ttwaila.disallowIncoming` when it is false. That is
  the inverse of what the flag does (`teleportPlayer` refuses a gateway whose `locked` is true), so the original tooltip lies; the
  lead decides whether the Jade port copies it or fixes it. The keys are in the generated base language files; nothing reads them
  yet because the Waila/Jade providers are not a class of this group. They belong to the lead's Waila-to-Jade port
  (`docs/API_GAPS.md` item 10). **Not done.**
- **Classes of the KamiGear group that this group uses** (PORTING_RULES 1.3 says groups reach each other by registry path):
  `ItemSkyPearl` (the static `isAttuned`, `getPos`, `getDim`, `getX`, `getZ`, `addInfo` and the `instanceof` test in
  `TileWarpGate`, `ContainerWarpGate` and `GuiWarpGateDestinations`), `SlotSkyPearl` (`ContainerWarpGate`), `ModelSpinningCubes`
  (`RenderTileWarpGate`) and `KamiHooks` (`KamiWorld`, `KamiWorldClient`, `TileWarpGate`). They are the contract that the KamiGear
  group documents for this group (`docs/api-requests/KamiGear.md` section 5; `SlotSkyPearl` and `ModelSpinningCubes` were ported
  by that group for the gateway). The attunement of a pearl is a data component with its own static accessors, and the gateway's
  screens read and write it all the time, so there is no registry-path equivalent that does not copy the component's code.
  Behaviour is unaffected and it compiles; the lead decides whether PORTING_RULES gets an explicit exception for these four
  helpers or the helpers move to a neutral package (a request to the lead, not a change of this group).
- **The GameTest run (integration round).** The 17 checks of the group pass in a real run of the Fabric GameTest server. What the
  first run showed, and what was done about it (no check was weakened or removed):
  - *Five checks failed with "the Bedrock dimension exists"* (`bedrockDimensionIsSolidBedrockWithOreVeins`,
    `bedrockOreConfigDecidesDensityAndBlacklist`, `portalDestinationsFollowTheRules`, `registrationIsComplete`,
    `warpGateTakesAPearlOfAnotherDimension`): the level `thaumictinkerer:bedrock` was not there. **Cause: the test world, not the
    dimension.** Vanilla's `GameTestServer.create` builds its dimensions from the *flat world preset* only
    (`WorldPresets.FLAT.createWorldDimensions().bake(registry)` with `registry = new MappedRegistry<>(Registries.LEVEL_STEM, ...)
    .freeze()`, an empty registry), whereas a normal server bakes the preset (or the `level.dat` list) against the level stems of the
    data packs, which is how `data/thaumictinkerer/dimension/bedrock.json` becomes a level in a real world. So no data-pack dimension has
    a level in the vanilla GameTest world (Thaumcraft's own `thaumcraft:outer_lands` was missing from the add-on's run for the same
    reason, hence its "TC4 Outer Lands dimension is NOT registered" line; the mod's own GameTest run avoids it with an override of the flat preset in
    `common/src/gameTest/resources/data/minecraft/worldgen/world_preset/flat.json`, and the add-on's preset has the same entry for
    `thaumcraft:outer_lands` since the hooks round: see below). The data pack was not at fault: the dimension
    type, the biome and the dimension file decode without a message in the server log. **Fix (test set-up only):**
    `fabric/src/gameTest/resources/data/minecraft/worldgen/world_preset/flat.json`, the vanilla preset (the same three dimensions,
    the same flat layers) plus a copy of the dimension file under `thaumictinkerer:bedrock`. A new check,
    `bedrockDimensionFileDecodesAndIsTheLevelOfTheTestWorld`, decodes the real file with the server's registries (the path of the data
    pack loader, so what a normal world gets), compares it to the copy in the preset and to the running level; a JUnit test
    (`theFlatPresetOfTheGameTestWorldHoldsTheDimensionFileAsItIs`) pins the copy to the file. The leftover world of
    `fabric/run-tinkerer-gametest` did not hide the dimension (the list of levels is rebuilt from the preset at every start and was
    never read from the saved `level.dat`; only saved chunks persist between runs); the saved chunks of the Bedrock dimension were
    deleted once (`world/dimensions/thaumictinkerer`) before the run that proved it from a cold start.
  - *`soulHeartsAbsorbDamageBeforeHealth` failed with "it costs 3 soul hearts: 5"*: a wrong set-up. A new `ServerPlayer` refuses every
    hit for 60 ticks (`spawnInvulnerableTime`, counted down by `ServerPlayer.tick`) before any event is posted, so the hit of the check
    changed neither health nor the pool. The hits now come after 65 ticks (`runAfterDelay`, as the Hooks checks do). The handler was right.
    That check ran under the Architectury fallback and asserted only what the fallback and the exact rule share; since the hooks round it
    runs the real event and asserts the exact rule (see the hooks-round bullet below). The exact rule also has its own check on its
    seam, `soulHeartsExactRuleCutsTheFractionOfEveryHit` (`modifyIncomingDamage`: 3.9 against 5 hearts leaves nothing and costs 3, 4.9
    against 2 leaves 2, 1.5 with no hearts leaves 1, 0.5 is gone, a hit that is not positive is returned as it is, 100 against 20 leaves 80).
  - *`groupHadNoRegistrationFailures`* failed in the first round only as a cascade of another group's research clash and passes now.
- **The hooks round (the groups moved from their workarounds to the real events of `docs/HOOKS.md`).**
  - *Wired:* `LIVING_DAMAGE` (`SoulHeartHandler.onLivingDamage`, all server players) and `LIVING_DROPS`
    (`DimensionalShardDropHandler.onLivingDrops`), both registered where the group registers its other handlers. *Deleted:* the
    Architectury `LIVING_HURT` fallback with `exactDamageHook`, `reissuing` and `beforeDifficultyScaling` (and the JUnit check of that
    inverse), and the loot-table pools with `ENDERMAN`, `ZOMBIFIED_PIGLIN` and `pool(...)` (and their two checks, replaced by the two
    below). Sections 5 and 6 say what the events give and what they do not.
  - *What the Soul Hearts check now runs:* `soulHeartsAbsorbDamageBeforeHealth` hits a survival mock player with `player.hurt` and
    reads the result of the real event (covered: refused, no health lost, no game frames; partly covered: dealt; no hearts: the fraction cut,
    17 and not 16.5); its hits are eleven ticks apart because the pool keeps the frames of the original (it used to set
    `player.invulnerableTime = 0` between hits, which does not reset the pool's frames; waiting is also closer to the game).
    `soulHeartsKeepTheFramesOfTheOriginal` (new) pins the frames tick by tick, the damage event sent for the covered hit that begins them (read
    from the mock player's connection, which is an `EmbeddedChannel` that keeps what it is sent), the pool the client is sent (one
    `PacketSoulHearts` after every charged hit, none for a refused one; Architectury sends the payload as its wrapper of the encoded
    bytes, which the helper decodes with the codec of the packet) and that another player's pool and a pig are not touched.
    `soulHeartsAgreeWithTheOriginalWhenTheGamesFramesBeginLater` (new, review round) is the sequence of section 5 in which the game's
    frames begin after the pool's and end before or after them. `soulHeartsAreChargedOncePerTenTicksByLava` (new) is the lava case, real lava and real `Entity.baseTick`.
  - *What the shard checks now run:* the two loot-table checks (`shardDropPoolHasItsThreeConditions`, `shardDropsAreAddedToTheEntityLootTables`)
    tested the pools, which no longer exist, so they were **replaced** by equivalents against the real thing:
    `shardDropsFollowTheConditionsOfTheOriginal` (real deaths by `LivingEntity.die` of an Enderman and a Piglin in the End and the
    Nether with certain dice: the player, a player behind the blow, the fake player, a subclass, the roll at the edge of the chance;
    and the negatives: the other dimension, the other mob, a blow that is not a player's; the level takes the shard when the drops are kept
    and never sees it when a later listener cancels them) and `shardDropsComeFromRealDeathsWithTheChanceOfTheOriginal` (12,800 deaths with the real dice: about 1/32 and 1/16, bounds
    five standard deviations wide, never the other shard, none for plain damage, the Piglin's own rotten flesh still in the list). The old
    checks' "vanilla drops are still there" is kept as that last assertion. "The level takes the shard" is asked of the level's entity
    manager (`PersistentEntitySectionManager.isLoaded(uuid)`, through its private field): the usual `ServerLevel.getEntity(uuid)` finds only
    entities of visible chunks, and an attempt with a forced Nether chunk found the item invisible for 300 ticks (the chunk was loaded,
    `isPositionEntityTicking` false: no player is near), so that route was dropped. The mock player's connection keeps the packets it is sent
    only once the channel is flushed (the server thread writes without flushing), which the helper does before it reads.
  - *The log line.* The add-on's GameTest preset (`fabric/src/gameTest/resources/data/minecraft/worldgen/world_preset/flat.json`) got
    `thaumcraft:outer_lands` the way the mod's own preset has it (the same entry: the flat generator, the biome
    `thaumcraft:eldritch_lands`, one layer of air). Result, from the server log of the run: the line
    `TC4 Outer Lands dimension is NOT registered: thaumcraft:outer_lands` (an `ERROR` line, harmless: Thaumcraft's own checks were not
    affected) is gone, and `TC4 Outer Lands dimension registered: thaumcraft:outer_lands (FlatLevelSource generator, ...)` takes its place.
    Nothing in the tests was changed because of it (the JUnit check of the preset only asks for the Bedrock copy and the three vanilla
    dimensions).
  - *Mutation checks* (done by hand, the sources changed, the checks run, the sources put back byte for byte and compared with `cmp`; each
    mutation failed the checks named): removing the `LIVING_DAMAGE` registration failed `soulHeartsAbsorbDamageBeforeHealth`,
    `soulHeartsKeepTheFramesOfTheOriginal` and `soulHeartsAreChargedOncePerTenTicksByLava` (and left the seam check and the persistence check
    green); removing the `LIVING_DROPS` registration failed both shard checks; making the frames never run failed the frames check ("b+1: the
    repeat ... no pool") and the lava check (the pool is charged at ticks `[60, 61, 62, 63, 64]`, the ten-times-faster drain of
    `HOOKS.md` section 3); handing the game only `rest` instead of the sum (then called `dealt + rest`, now `handed + rest`) failed the frames check at "b+22: a stronger hit is dealt";
    taking the damage event out failed "b: and the player is told of it"; a frame length of 11 failed "b+10: a new hit, charged whole"; one
    frames record for every player failed the absorb, frames and lava checks; answering for non-players too failed "a pig is not the pool's";
    removing the player condition of the shard handler failed "damage that is not a player's gives nothing" (and the statistical check:
    205 shards from 3200 plain-damage kills); removing the Nether condition failed "another dimension or another mob drops no shard".
    Not mutation-tested: the exact edges of the `<=` in the chance (the check pins them, it was only shown to pass), the fake-player and
    subclass cases, and everything about the client.
  - *The review round* (an independent review of the work above; five minor findings, all real). (1) The pool handed the game
    `dealt + rest` by its own frames, but the game's frames can end earlier or begin later: fixed as described in section 5 (the pool
    asks `invulnerableTime`, keeps `handed` for as long as the player is on the server) and pinned by the new check; the old text of
    section 5 mentioned only the under-comparison. (2) The interplay with the Enchantments listener (side effects for a hit the pool's frames
    refuse) was not written down: it is now, in section 5, and asked of the lead in `docs/api-requests/KamiWorld.md` section 1. (3) No check
    asserted the pool sent to the client: `soulHeartsKeepTheFramesOfTheOriginal` does now; with `updateClient` taken out of
    `modifyIncomingDamage` it fails at "b: and of the pool". (4) `shardDropsFollowTheConditionsOfTheOriginal` left its mock player in the
    world when it passed (it was discarded only on failure): the success path runs the same cleanup now, and the check asserts that both
    mock players are removed; with the discard taken out of the cleanup it fails at "the mock player of the check does not stay in the world".
    (5) Section 5 lacked the sleeping player and the shield, and this header counted 37 JUnit checks where there are 38 (24 + 14): both fixed.
    Mutations of the review round (sources restored byte for byte, compared with `cmp`): handing `handed + rest` always failed
    "b+14: the difference of 2 went on, not all 5" (health 10) and three older checks; handing `rest` always failed "b+10: all of its 3 went
    on" (health 17) and the frames check at "b+22"; `>=` instead of `>` in the test of the game's frames failed "b+24: a new hit, dealt whole"
    (health 8) and two older checks.
  - *Not machine-checked:* that `bypasses_cooldown` sources start new frames (the tag is empty in vanilla; the branch is one condition),
    the client side of the damage event (what the player sees and hears: no client was run), what the client does with `PacketSoulHearts` (only that
    the server sends it, with the right number), the shield, helmet and sleeping cases of section 5 (read from the bytecode of 1.21.1, not run),
    and NeoForge (no poster).
