# KamiWorld: deviations from Thaumic Tinkerer 1.7.10

Group KamiWorld: the Bedrock dimension (terrain, ore clusters, light, sky), the Bedrock portal, the Celestial Gateway (warp gate)
with its two screens and packets, the Soul Hearts (server pool, packet, HUD), the dimensional shard drops and
`/iskamiunlocked`. The pinned 1.7.10 source is GTNewHorizons/ThaumicTinkerer at `28f4def9...`. Only what differs from that
source is listed; everything else (numbers, ore weights, research keys and positions, aspects, recipe, tooltips, sounds,
messages, config use) is ported as it is.

How this was checked (honestly): the add-on compiles with `tools/compile.sh` (whole tree and with `-g`). The pure logic and the
data files are covered by JUnit tests (`common/src/test/java/thaumic/tinkerer/kamiworld`, 37 checks) that were **run with a
hand-written stand-in for JUnit Jupiter** (the real one is not available offline; Gradle was not run): they pass. The
dimension type and biome JSON were parsed with the game's own codecs (`DimensionType.DIRECT_CODEC`, `Biome.DIRECT_CODEC`,
`ChunkGeneratorBedrock.CODEC` over the dimension file's generator object) in a throw-away program on the game classes, and the
chunk fill, the ore selection with bound item tags and the ore clusters through `applyBiomeDecoration` (with a proxy world) were
run there as well. The GameTests (`KamiWorldRuntimeChecks`, 15 checks) **compile but were not run** (no Gradle and no game in
the porting session). Nothing here has been seen in a running client: the screens, the HUD, the model of the gateway and the
renderer are untested visually.

## 1. Identity: registry paths, data

| 1.7.10 | 1.21.1 |
|---|---|
| block `warpGate` (+ `ItemBlockWarpGate`, a plain item block that was never registered on its own) | block and item `thaumictinkerer:warp_gate` (`LibBlockNames.WARP_GATE`), block entity type and menu type of the same path |
| block `portal` (`BlockBedrockPortal`, no item) | block `thaumictinkerer:bedrock_portal` (`LibBlockNames.PORTAL`), no item, no block entity (section 3) |
| dimension id -19 (`bedrockDimensionID`, config), `WorldProviderBedrock`, `ChunkProviderBedrock` | the dimension `thaumictinkerer:bedrock` (`WorldProviderBedrock.KEY`): `data/thaumictinkerer/dimension/bedrock.json`, `dimension_type/bedrock.json`, `worldgen/biome/bedrock.json`, generator type `thaumictinkerer:bedrock` (`ChunkGeneratorBedrock`, a `DeferredRegister` of `Registries.CHUNK_GENERATOR` in `KamiWorld`) |
| ore dictionary names of `EnumOreFrequency` | the same names, resolved to item tags by `OreDictionaryNames`; "first ore" is the member with the lowest registry id |
| `PacketSoulHearts`, `PacketWarpGateButton`, `PacketWarpGateTeleport` (`SimpleNetworkWrapper`, ids 142+) | `CustomPacketPayload` records `soul_hearts` (S2C), `warp_gate_button`, `warp_gate_teleport` (C2S) |
| `LibGuiIDs.GUI_ID_WARP_GATE`, `GUI_ID_WARP_GATE_DESTINATIONS` | menu type `warp_gate` (`ContainerWarpGate`); the destinations screen has no container and is opened by the block entity on the client (`TileWarpGate.setDestinationsOpener`) |
| `GuiWarpGate`, `GuiButtonWG`, `GuiWarpGateDestinations`, `SoulHeartClientHandler`, `RenderTileWarpGate`, `RenderWarpGate` (ISBRH) | the same names under `thaumic.tinkerer.client` (section 8); the ISBRH is a JSON model plus the block entity renderer |
| `KamiUnlockedCommand` (`CommandBase`) | `thaumic.tinkerer.common.core.commands.KamiUnlockedCommand`, a Brigadier literal registered with `CommandRegistrationEvent` |
| `DimensionalShardDropHandler`, `SoulHeartHandler` (Forge event handlers) | `core.handler.kami.DimensionalShardDropHandler` (a loot-table hook) and `SoulHeartHandler` (Architectury events), registered by `KamiWorld.register()` |

Everything of the group is registered only when `ConfigHandler.enableKami` is on (1.7.10: `shouldRegister() == enableKami` and
the handlers registered in `if (enableKami)`), with **one exception**: the chunk generator type `thaumictinkerer:bedrock`. The
dimension is a file of the data pack that names that type, and a data pack that cannot decode a dimension stops the world from
loading; with Kami off the dimension exists but nothing leads there and nothing in the add-on touches it. The
`ThaumicTinkerer.preInit` registration of the provider type and the dimension (`DimensionManager`) and the config option
`bedrockDimensionID` (0 disabled the dimension) are gone; `enableKami` is the switch.

Classes of the original not ported as classes: `TileBedrockPortal` (section 3), `KamiDimensionHandler` (already ported by the
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
  step allows; the written area is checked (`reaches`) and the outer layers (y 0 and 1, 252 to 255) never hold ore, so the entrance
  cavity of the portal (section 3) is never cut by a vein. Blocks are placed with the flag "update clients" (1.7.10: flag 2).
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

- **Travelling.** 1.7.10 sent players from `onEntityCollidedWithBlock` (with a `TeleporterBedrock` that placed nothing) and
  everything else from `TileBedrockPortal.updateEntity` with the vanilla teleporter, which built a Nether portal in the
  Bedrock dimension for every item, mob or arrow that touched the block. In 1.21.1 the block is a vanilla `Portal`:
  `entityInside` hands an entity to the game's portal processing, which changes its dimension at the end of its tick with no
  waiting time and no screen effect, to the `DimensionTransition` of `getPortalDestination`. The tile entity is not needed
  and is not ported. Differences: (a) non-player entities no longer cause a stray Nether portal; in a surface world they arrive
  at the cavity like players; in the Bedrock dimension only players leave (the tile entity "travelled" them to the dimension
  they were already in); (b) the 1.7.10 portal checked `isSurfaceWorld()`; the 1.21.1 predicate is "has sky light, no ceiling and
  is not the Bedrock dimension" (`WorldProviderBedrock.isSurfaceWorld`), which is the Overworld and similar mod dimensions, not
  the Nether or the End; (c) the game's portal cooldown (10 ticks for players, 15 seconds for others) applies.
- **In** (`TeleporterBedrock.toBedrock`): the Bedrock column with the portal's x and z is loaded, layer 249 becomes stone if it
  is air, layers 250 to 253 are cleared when they are stone or bedrock (other blocks stay), the bedrock at 254 becomes the
  portal, and the traveller arrives at the middle of the block at y = 251 with yaw and pitch 0, no momentum.
- **Out** (`TeleporterBedrock.toSurface`): the Overworld (dimension 0 whatever the player came from), a random column
  `(int) x + nextInt(100) - 50` (and z), scanned down from y = 120 for the first block that is not air (leaves and water count),
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
  cleared by the block entity tick). The sparkles of the original are written inside `if (worldObj.isRemote)` in a method that
  only ever runs on the server, so they never showed; they are not ported. The sound is played for everyone nearby **including**
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
- **Spending (the deviation that matters).** 1.7.10 spent the pool in `LivingHurtEvent`, before armour, with
  `event.ammount = removeHP(player, (int) event.ammount)`: **every** hit of every player is truncated to its whole points
  (a hit under 1 point is swallowed, the fraction of the others is lost) and what the pool covers is taken off, whether the
  player has hearts or not. Architectury's `EntityEvent.LIVING_HURT` can cancel a hit but not change its amount, so
  `SoulHeartHandler.onLivingHurt` does this instead, **only for a player that has soul hearts** and not during the
  invulnerability frames of a hit: a hit the pool covers is cancelled (the damage event, so the hurt animation and sound, is still
  sent; knockback and invulnerability frames of that hit are not), and a hit it does not cover is cancelled and dealt again
  as a new hit of the same source with what is left (the Easy/Hard scaling of the player's own `hurt` is undone first, so the
  second scaling gives the same number). The original's loss of the fraction for players *without* hearts is not reproduced.
  `SoulHeartHandler.modifyIncomingDamage` is the exact 1.7.10 rule for a loader hook that can set the amount
  (`exactDamageHook` then silences the fallback); see `docs/api-requests/KamiWorld.md` section 1.
- **Dead code of the original** (`applyArmorCalculations` and `applyPotionDamageCalculations`, copies of `EntityLivingBase`
  methods nothing called) is not ported.

## 6. Dimensional shards

`DimensionalShardDropHandler`: an Enderman killed by a player in the End drops an Ender Shard with chance 1/32, a Zombified
Piglin killed by a player in the Nether a Nether Shard with chance 1/16 (1.7.10 metadata 7 and 6 of `ItemKamiResource`, here the
items `ender_shard` and `nether_shard`). Architectury has no event for living drops, so the drop is a loot pool added to
`minecraft:entities/enderman` and `minecraft:entities/zombified_piglin` by `LootEvent.MODIFY_LOOT_TABLE` (also to a data pack's
replacement of those tables) with the conditions "damage source entity is a player" (`event.source.getEntity()`: the shooter of an
arrow counts, a wolf does not, which is *not* the same as vanilla's "killed by player"), "location in the End/Nether" and the
random chance. The shard is a separate item entity, as it was a separate `EntityItem` added to the drops; Looting does not change
the chance (it did not). Differences: modded subclasses of the two mobs that use another loot table drop nothing; the item is
spawned by the loot system at the mob's position (1.7.10: the same position, without the death motion).

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

- The Nether portal that the vanilla teleporter built in the Bedrock dimension for non-player entities (section 3).
- The truncation of damage for players without soul hearts (section 5).
- Sparkles of the gateway teleport (dead code in the original, section 4).
- `y == -1` hiding a destination on the map (section 4).
- The configurable dimension id and the option to disable the dimension (section 1).
