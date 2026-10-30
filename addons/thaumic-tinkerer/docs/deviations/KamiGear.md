# KamiGear: deviations from Thaumic Tinkerer 1.7.10

Group KamiGear: Ichor resources, the Ichorcloth wand rod and cap, the eight Ichorium tools, the eight Ichorcloth armour pieces,
the Feline Amulet, the Bottomless Pouch, the Black Hole Ring, the Worldshaper's Looking Glass (Placement Mirror), the Protoclay,
the Celestial Pearl and the Foci of Shadowbeam, Experience Drain and Celestial Recall. The pinned 1.7.10 source is
GTNewHorizons/ThaumicTinkerer at `28f4def9...`. Only what differs from that source is listed; everything else (numbers, costs,
recipes, research keys and positions, aspects, tooltips, sounds, config use) is ported as it is.

How this was checked (honestly): the research entries (key, aspects, column, row, complexity, warp, parents) and all 30
recipes (instability, aspects) were compared with the 1.7.10 source by script, the ingredient lists of a dozen recipes by hand;
the add-on compiles with `tools/compile.sh` (also with `-g`, which adds the GameTest sources); the pure logic and the data files are
covered by 31 JUnit tests (KamiGear's 30 and `TinkererApiOnlyTest`) that were run with the real JUnit Jupiter launcher from the
Gradle cache, without Gradle. The 30 GameTests (`KamiGearRuntimeChecks`) were first run on a real Fabric dedicated server in the
integration round (`-PtinkererGameTestFilter='kamigeargametests.*'`): 23 passed, 7 failed, and all 7 were wrong expectations or
set-ups of the checks, not defects of the implementation (the mock player of `GameTestHelper.makeMockServerPlayerInLevel` is in
Creative mode, so the checks now set Survival; `GameTestHelper.relativePos` is not the inverse of `absolutePos` for an unrotated
test; a pouch menu is opened for the *selected* hotbar slot; the target of a piglin is its `ATTACK_TARGET` brain memory, which
`AbstractPiglin.getTarget` reads, not the field that `Mob.setTarget` writes). After the corrections all 30 pass. Nothing
here has been seen in a running client.

## 1. Identity: registry paths, components, models

| 1.7.10 | 1.21.1 |
|---|---|
| `kamiResource` metadata 0..7 | eight items, in the original order: `ichor`, `ichor_cloth`, `ichorium`, `ichor_nugget`, `ichor_cap`, `ichorcloth_rod`, `nether_shard`, `ender_shard` (`LibItemNames.KAMI_RESOURCE_NAMES`) |
| armour `ichorcloth{Helm,Chest,Legs,Boots}`, `...Gem` | `ichorcloth_helm`, `_chest`, `_legs`, `_boots` and the same with `_gem` |
| tools `ichorPick/Shovel/Axe/Sword` and `...Gem` | `ichor_pick`, `ichor_shovel`, `ichor_axe`, `ichor_sword` and the same with `_gem` |
| `catAmulet`, `ichorPouch`, `blockTalisman`, `placementMirror`, `protoclay`, `skyPearl` | `cat_amulet`, `ichor_pouch`, `block_talisman`, `placement_mirror`, `protoclay`, `sky_pearl` |
| `focusShadowbeam`, `focusXPDrain`, `focusRecall` | `focus_shadowbeam`, `focus_xp_drain`, `focus_recall` |
| damage value 0/1/2 of an awakened tool (mode) | component `thaumictinkerer:kami_tool_mode` (0..2). The item model predicate is `thaumictinkerer:kami_mode` = mode / 2 (0, 0.5, 1) because predicates are clamped to 0..1 |
| damage value 1 of an awakened armour piece (switched off) | component `kami_piece_disabled` (boolean) |
| ring: `blockName`, `blockMeta`, `blockCount`; damage value 1 (active) | components `kami_talisman_store` `{block, count}` and `kami_talisman_active`; model predicate `thaumictinkerer:kami_active` |
| mirror: `blockName`, `blockMeta`, `size` | components `kami_mirror_state` (the whole block state: metadata no longer exists) and `kami_mirror_size` |
| pearl: `x`, `y`, `z`, `dim` | component `kami_pearl_target` `{dimension, pos}` |
| pouch: tag `Inventory` | component `kami_pouch_contents` (`ItemContainerContents`, 117 cells) |

The wand parts keep their 1.7.10 tags `ICHOR` (cap) and `ICHORCLOTH` (rod), so the research keys `CAP_ICHOR` and `ROD_ICHORCLOTH` and
the language keys `item.Wand.ICHOR.cap` / `item.Wand.ICHORCLOTH.rod` are those of the original. The 1.7.10 constructors
`CapIchor(String)` and `RodIchorcloth(String)` (a "patch for changing the name" that registered a second cap and rod from the
metadata 10 and 9, which do not exist) are not ported.

The creative-tab wand with the Ichor rod and cap and 100000 centivis of every primal is registered with `TTRegistry.addTabStack`.

### Armour textures (copied, recorded)

`scripts/porting/tt_assets.py` converts the 1.7.10 armour sheets to `textures/models/ichor_1.png`, `ichor_2.png`, `ichor_gem_1.png`
and `ichor_gem_2.png`. 1.21.1 names the worn layers of an armour material by path
(`textures/models/armor/<material>_layer_1.png` for the helmet, chestplate and boots, `_layer_2` for the leggings), so the four
sheets were **copied by file copy** (bytes identical, checked by `KamiGearResourcesTest`) to:

| Source (converted) | Copy |
|---|---|
| `assets/thaumictinkerer/textures/models/ichor_1.png` | `.../textures/models/armor/ichor_layer_1.png` |
| `.../models/ichor_2.png` | `.../models/armor/ichor_layer_2.png` |
| `.../models/ichor_gem_1.png` | `.../models/armor/ichor_gem_layer_1.png` |
| `.../models/ichor_gem_2.png` | `.../models/armor/ichor_gem_layer_2.png` |

The converter was **not** changed: its table covers one directory level and a generated subdirectory would need changes in its
map, its check and its documentation. The copies are hand-written files, which the converter never touches. The sheet
`ichor_gem_layer_1.png` is also the texture of the wings (two regions of it, as in 1.7.10), drawn by `WingsLayer`.

## 2. Rarity

`TTCommonProxy.kamiRarity` was an `EnumRarity` added by `EnumHelper` (light purple, "Kami"). `Rarity` is a fixed enum in
1.21.1, so every Kami item has `Rarity.EPIC`, which is light purple as well; the two shards stay common as in 1.7.10. The
Shadowbeam and the Experience Drain keep the Kami name colour through `ItemModKamiFocus.getName`.

## 3. Armour

- **No `ISpecialArmor`.** The pieces absorbed 0.0425 x their armour points of every blow, 85% for the set, and never wore out.
  The materials `ichor` and `ichor_gem` have the original 3/8/6/3 armour points and enchantability 20 and a toughness of 5 per piece; the
  pieces have no durability. The 1.21.1 formula does not reach 85% (about 74% against a blow of 10, 57% against a blow of 40).
  Request K1 in `docs/api-requests/KamiGear.md`.
- **Gregtech hazard protection and the Thaumic Boots mod** (`IHazardProtector`, `IBoots`, optional in the pinned fork) are not ported:
  neither mod exists for 1.21.1.
- **The toggle key** "Toggle Awakened Armor" (category "Thaumic Tinkerer") is bound to **no key**, as `Keyboard.KEY_NONE` in the
  pinned source, although the Thaumonomicon pages still say "Pressing U". The player binds it in the controls screen. It switches the
  master flag of the player: the client sets its own copy and tells the server (`PacketToggleArmor`), the server saves it with
  the world (`SavedData`) and tells the client on login (`PacketArmorStatus`); the piece-wise switch is the sneak-click.
- **Cowl of the Abyssal Depths.** Unchanged, including the 1.7.10 quirks: lava gives unlimited breath and Blindness (the code
  names `Potion.blindness`); the food regeneration ignores both switches.
- **Boots of the Horizontal Shield.** Step height and jump height are attribute modifiers that the server sets
  (`STEP_HEIGHT` +0.4, -0.1 while sneaking; `JUMP_STRENGTH` +0.3); the walking boost (speed 0.075, a quarter in water, half
  while sneaking) is added to the **local player's** motion on the client, because a 1.21.1 server cannot push a client-driven
  player. As in the original, the speed boost and the step height while moving ignore the disable switches, and the fall distance
  is zeroed for every wearer. Dirt turns to grass under the wearer only while the piece is switched on and
  `shouldKamiBootsDoGrass` is on.
- **Leggings of the Burning Mantle.** Unchanged; the light feature (config `shouldKamiLegsDoLightFeature`, off by default) places
  `nitor_gas` with the integer state property `level` = 1 on the player and level 0 on the five cells in front (the 1.7.10 code
  had put the metadata back to 0 before the loop). The gas block removes itself when no carrier is near (SimpleBlocks group).
- **Robes of the Stratosphere.** Flight is `Abilities.mayfly`, granted and taken away by the server every tick for players the
  armour has granted it (and only for them: a creative player keeps theirs); the projectile deflection is the Kami copy of the
  Distortion rule (the Foci group owns the focus). Projectiles that did not move during their last tick (an arrow in a block
  keeps its hit vector in 1.21.1, where 1.7.10's had no motion left) are not pushed; a projectile that spawned this tick counts as
  not having moved and is looked at one tick later. The six white sparkles per deflected projectile of the original are sent by
  `ThaumcraftFX.sparkle` (the server twin of the client sparkle: request A8, now in the API). The wings are a render layer
  (`WingsLayer`, the model `ModelWings`), added to the player renderers by the loader glue (`docs/api-requests/KamiGear.md`
  section 4). The flap animation resets the wings to the rest pose when the wearer stops flying (1.7.10 left the last angle).

## 4. Tools

- **No durability.** `ToolMaterial ICHOR` (harvest level 4, durability -1, efficiency 10, damage 5, enchantability 25) is the tier
  `IchorTier`: nothing is incorrect for drops (the netherite rule), and the tools carry the `UNBREAKABLE` component with the
  tooltip hidden. `Item.isEnchantable` is overridden to true because an unbreakable item would otherwise refuse enchantments (1.7.10
  allowed them). The tools are in the vanilla item tags `pickaxes`/`shovels`/`axes`/`swords` through the add-on's own copy of those
  tag files (optional entries, so the tags still load with Kami off).
- **Attack damage** is the 1.7.10 one: the weapon modifier is the tool's base (pickaxe 2, shovel 1, axe 3, sword 4, passed to
  `createAttributes`) plus the tier's 5, so a hit with the player's own 1 deals 8, 7, 9 and 10 (the vanilla 1.21.1 bases 1, 1.5, 5, 3
  are not used). The attack speeds are the vanilla ones, because 1.7.10 had no attack cooldown (see the sword below).
- **`Material` lists become predicates over block tags** (`ToolHandler.MATERIALS_SHOVEL/PICK/AXE`): the shovel list is
  `#minecraft:mineable/shovel`; the pickaxe list is `#minecraft:mineable/pickaxe`, bedrock, glass-sounding blocks and the shovel list
  (the 1.7.10 list is the concatenation); the axe list is `#minecraft:mineable/axe`, leaves, plants (bush blocks), sugar cane and
  cocoa. The Protoclay keeps the original order, so dirt counts as `pick`.
- **`onBlockStartBreak` is `BlockEvent.BREAK`** (Architectury). The extra blocks of the square, line and column modes are broken
  with `ServerPlayerGameMode.destroyBlock`, which gives them the player's normal break (drops, experience, claims and other
  mods' break events) instead of the hand-made copy of `ItemInWorldManager.tryHarvestBlock`; a thread-local flag keeps those inner
  breaks from starting the mode again. `destroyBlock` asks only for the game mode's restrictions, so every extra block is first
  checked with `Level.mayInteract` (spawn protection, world border), which vanilla asks only for the block the player hits; a
  block it refuses is left standing (1.7.10 had no such protection). The items on the ground in the box are moved to the player
  as before, and once more at the end of the server tick (`TickEvent.SERVER_POST`), because the clicked block is part of the
  box and the break in progress drops its items only after `BlockEvent.BREAK` returns (1.7.10 broke it inside the loop).
- **Blocks that cannot be broken at all** (hardness -1: end portal frames, barriers...) are left alone by the extra breaks; the pinned
  fork would have destroyed them (it only filtered by material). Bedrock is broken only in the Bedrock dimension and not in the top two layers, as before.
- **Column mode** (Shovel): the pinned fork lost the "same block" filter (`removeBlocksInIteration(..., block, ...)` is called with the
  parameter and ignores it in the loop; the neighbouring method `removeBlockWithDrops` still has it). The filter is restored: the
  column takes only blocks of the kind that was broken, 8 below to 7 above.
- **Bedrock (left click).** 1.7.10 has two paths: `onBlockStartBreak` (which survival never reaches for a block that cannot be
  broken) and `KamiDimensionHandler.onInteract`, which calls `onBlockStartBreak` of the awakened pickaxe on every left click
  on bedrock. The second one is what makes it work in survival, and it is ported **here**, not in the KamiWorld group:
  `ItemIchorPickAdv.onLeftClickBlock` (`InteractionEvent.LEFT_CLICK_BLOCK`, registered by `KamiGear`) does the same call. KamiWorld
  must not port `KamiDimensionHandler` again. Sneaking: the clicked bedrock becomes the portal block (Overworld, lowest five layers;
  top two layers of the Bedrock dimension) or air (rest of the Bedrock dimension). Not sneaking, in square or line mode: the blocks
  around break, and the bedrock itself too where the rules allow (the Bedrock dimension below its top two layers; 1.7.10 included
  the clicked block in the loops). The click is cancelled afterwards (the bedrock has nothing left to break; in creative the
  normal break would repeat the mode). Differences: the portal is made in the Overworld only (1.7.10: in any "surface world"),
  and the bedrock dimension counts as available when the portal block is registered (1.7.10: `bedrockDimensionID != 0`).
  Mining speed on bedrock is the maximum, as in 1.7.10.
- **Sneaking and the vanilla abilities.** The awakened pickaxe, shovel, axe and sword cycle the mode on a sneak-click (`use`, as 1.7.10
  `onItemRightClick`). In 1.21.1 the shovel (flattening grass to a path) and the axe (stripping logs, scraping copper) have a vanilla
  `useOn` that runs before `use` and would swallow a sneak-click on a block, so the awakened shovel and axe pass `useOn` while
  sneaking. Those two vanilla abilities themselves are new (1.7.10 had neither) and are inherited by all four shovels and axes.
- **Tree mode** is the 1.7.10 loop over Thaumcraft's own two helpers (now API, request A6): while the clicked cell is not air, break
  the furthest block of the walk that starts there. Their rules apply: oak logs are not logs for them (the original's rule), no tool
  wear, no protection check (spawn protection included) and no break event for the felled logs, a walk limited to 24 blocks sideways and 48 up or down. Two
  small differences: the loop stops after 1024 blocks or when the helper breaks nothing (1.7.10 would loop forever if a break was
  vetoed), and the gathered items keep their normal pickup delay (1.7.10 added 20 ticks to their age, which has no counterpart).
- **Sword, area mode.** 1.7.10 had no attack cooldown; without a fast attack speed every hit after the first would deal a fifth
  of its damage, so the attack speed gets a temporary huge modifier while the area strike runs. The mode is started from
  `PlayerEvent.ATTACK_ENTITY`, before the hit, like `onLeftClickEntity`. Soul mode gives the target Resistance II for one tick and
  asks the Soul Heart hook (`KamiHooks.soulHeartSink`, see `docs/api-requests/KamiGear.md` section 5).

## 5. Black Hole Ring

- **No loss when the container refuses.** 1.7.10 took the blocks out of the ring before asking the slot whether it accepts them and lost them when
  it did not; here nothing leaves the ring unless the slot accepts the stack (`canPlaceItem`, and `canPlaceItemThroughFace` for a sided
  container). A failed placement keeps the block in the ring (1.7.10 had taken it).
- **Stack size.** The stack the worn ring hands out is `min(64, the item's own maximum)`; 1.7.10 hard-coded 64, which gave a
  slot of 64 ender pearls or signs. A container still gets the item's own maximum, as before.
- **Binding.** An empty ring binds to the block it is clicked on only if that block has an item (not to water, fire, a portal...); 1.7.10
  also bound to blocks without one, which left a ring that could neither fill a container nor place anything.
- **The cursor condition of the worn refill** keeps its 1.7.10 shape: the ring hands out a stack when the cursor is empty, holds the
  bound item, or holds an item whose damage differs from the ring's (`getDamageValue() != 0`, the block metadata being 0 now).
- **Overflow.** The count saturates at `Integer.MAX_VALUE` ("over 2 billion"); 1.7.10 let the int overflow.
- **Worn merge.** `onWornTick` is the 1.7.10 algorithm (every stack of the bound block in the 36 main slots is merged into the
  first one found, which a later, larger stack replaces only while the first is outside the hotbar; the target is topped up from the
  ring; with none in the inventory and a slot free, the ring hands out a stack), with `isSameItemSameComponents` instead of
  metadata. It runs for the ring in a ring slot of the Baubles menu, on the server.
- **Equip on use.** `equipsOnUse` is false: a sneak-click toggles the ring and the ring is put on in the Baubles menu (request A1,
  now API).

## 6. Worldshaper's Looking Glass

- The square, the cost and the range (5 blocks) are those of 1.7.10 (`squareOffsets` is the original arithmetic as a pure method, tested for every side, pitch, rotation and size 3 to 11).
- **Protection.** Every placement asks `Level.mayInteract` and `Player.mayBuild` (spawn protection, world border and adventure mode);
  1.7.10 placed wherever the square reached. Blocks outside the build height are skipped.
- **Payment.** The blocks must be in the inventory or in Black Hole Rings in it; a creative player always has them and the
  placing takes nothing from them (1.7.10 `hasBlocks`: `if (player.capabilities.isCreativeMode) return true`), the ghost blocks included.
  The inventory is paid first, then the rings; the first ring that really gives a block pays (1.7.10 stopped at the first ring bound
  to the block, empty or not, which made the blocks free when a bound empty ring came first).
- **Sound.** The placing sound is played by the server for everyone near, the player included (`KamiHooks.broadcastThaumcraftSound`);
  `Level.playSound(player, ...)` on a server level leaves that player out.
- **Reach.** The server aims with the reach of the player (`blockInteractionRange`, 1.7.10: the game mode's reach), the client with
  5 blocks, so the preview can show a square at the very edge that the server does not place, as before.
- **Choosing the block.** 1.7.10 took a block with render type 0: cubes, slabs, glass, leaves, furnaces, snow layers, and also
  carpets, pressure plates, buttons, trapdoors, cake; not stairs, fences, walls, doors, beds, torches, panes, ladders. 1.21.1 has no
  render type and nearly every block is `RenderShape.MODEL`, so `ItemPlacementMirror.canRemember` takes a block *that has an
  item* whose shape is the full cube, or a slab or a layer of snow. That keeps out everything the original kept out, but
  also the flat render-type-0 blocks that are not slabs or snow (carpets, plates, buttons, trapdoors, cake), which 1.7.10 took.
- **Ghost blocks.** The translucent blocks are drawn from the loader's after-world hook (`PlacementMirrorPredictionRenderer.render`, alpha 0.6, only
  while the player holds the mirror and has the blocks; config `showPlacementMirrorBlocks`). Not seen in a running client.

## 7. Bottomless Pouch

- 13 x 9 cells; opens on a right click in either hand (`MenuRegistry.ofExtended`; the opening packet carries the hand); the
  cells are saved into the pouch whenever one changes, and again when the menu closes. The menu holds the stack it was opened for
  (the 1.7.10 `pouch` field) and never looks at the hand again, so selecting another hotbar slot while it is open (a client may send
  that) cannot redirect the writes into a second pouch. The pouch's own slot cannot be clicked, moved with its hotbar key (or the
  off hand key) or shift-clicked, and the menu closes when that stack leaves its inventory slot (1.7.10's duplication hole) or when
  something else writes the pouch's contents behind the open menu (the wand's focus selector does; the menu then does not write
  its stale cells over it). Pouches (Thaumcraft's and this one) cannot be put in. The dimensions of `ConfigHandler.forbiddenDimensions` keep it shut.
- It is an `IFocusPouch` (request A2, now API) and a belt `IBauble`, so the wand's focus selector offers the foci in it. The selector
  lists at most the first 128 cells (the pouch has 117).
- The screen title key `container.thaumictinkerer.ichor_pouch` is new (the screen draws no title, as in 1.7.10).

## 8. Feline Amulet

1.7.10 rewrote the goals of every mob around the wearer once and for good, which made a creeper's "avoid ocelots" goal avoid players
from then on and every "nearest attackable player" goal target an ender crystal instead; the effect survived the amulet being taken off
and was lost on a reload. 1.21.1 goals are final fields, so the amulet applies the effect again **every tick while worn** and the effect
ends with it. No permanent change to any mob. Scope, narrowed to what the 1.7.10 goals did:

- **Creepers** within 10 x 4 blocks lose their target and their fuse (a creeper that was already swelling stops: 1.7.10 set
  `timeSinceIgnited = 2`) and walk away from the wearer (6 blocks, speed 1.0, 1.2 when closer than 7), whoever they were after.
- **Other mobs** within the same box lose the wearer as their target only if they are after the wearer *without being provoked*: a mob
  that the wearer hurt in the last 100 ticks keeps its revenge (1.7.10's goals only stopped mobs from *finding* a player; a mob that
  already had a target, or struck back, went on). Left alone: neutral mobs (`NeutralMob`: wolves, bees, polar bears, zombified piglins...),
  the Wither and Endermen (their targeting was not a "nearest attackable player" goal), and every mob that thinks with a brain (Warden,
  piglins, hoglins, villagers...: they did not exist, and the amulet must not read a brain memory a mob does not have, which throws).
  A goal swap cannot be limited to one player either, so 1.7.10 made those mobs ignore every player; only the wearer is spared here.

## 9. Foci

- **Shadowbeam.** The cost per tick (`visShadowbeam*`), the damage (`baseDamageShadowbeam`, +20% of the base per potency level, truncated),
  the 300 steps of 0.15 blocks, the entity margin 0.3 and the mirror rule are those of 1.7.10; an entity on the way takes the hit and
  the beam goes on, a block reflects it (a Nether portal does not: an `EntityThrowable` only "enters" it). The beam is simulated by `ShadowBeam` (a pure
  function of the world, no entity); the **visuals** are a packet with the path to the players within 64 blocks and black dust particles
  along it, where 1.7.10 spawned a custom black `FXSparkle` subclass per step (request K3). The hum (`thaumcraft:brain`) is played by
  the server for everyone near, the shooter included.
- **Experience Drain.** 5 vis (+20% per potency) per tick for 15 experience points (-1 per frugal level, scaled to what fits);
  `getExperienceForLevel` and `getLevelForExperience` use the **1.21.1 experience curve** (7 + 2 per level up to 15, and so on), not the
  1.7.10 one of the original helper (17 per level up to 15, `1.5 L^2 - 29.5 L + 360` up to 30...), because the experience bar of
  a 1.21.1 player follows the new curve; the cost stays 15 experience *points*.
  The focus colour runs through the rainbow on the client: the icon is a grey texture, tinted by an item colour handler
  (`KamiGearClient.registerItemColors`, 1.7.10 `getColorFromItemStack`). The tooltips reuse Thaumcraft's own keys `item.Focus.cost2` (foci)
  and `tc.visdiscount` (armour).
- **Celestial Recall.** Held for three seconds, with an attuned pearl in the hotbar, teleports to that pearl's gateway of the
  same dimension for 40 vis each of Aer, Terra and Ordo, through `KamiHooks.warpGateTeleporter` (the gateway belongs to the KamiWorld
  group). The research is registered only while Thaumcraft's mirrors are allowed (`ThaumcraftApi.isMirrorsAllowed()`), as in 1.7.10.
  The two jump sounds and the 20 white sparkles before and after the jump of `TileWarpGate.teleportPlayer` belong to the gateway code
  that the hook calls: the default hook (`KamiHooks.teleportToWarpGate`, used only while KamiWorld has not replaced it) plays both
  and sends the sparkles (`ThaumcraftFX.sparkle`); the KamiWorld group's `TileWarpGate.teleportPlayer` plays the sounds and **sends no
  sparkles yet** (see the report: a request to that group).
- **`canApplyUpgrade`.** The original Kami foci answered true for every upgrade; the port keeps the base rule (an upgrade must be
  one the focus lists in `getPossibleUpgradesByRank`, see `docs/API_GAPS.md` 3b). The Shadowbeam and the Experience Drain list
  frugal and potency on every rank and the Recall frugal only, which is what the focal manipulator offered before, so the only
  difference is that an API caller cannot force another upgrade.

## 10. Client

- **HUD message.** The mode of a tool, the size of the mirror and the state of an armour piece are drawn above the hotbar by
  `ToolModeHUDHandler` (`ClientGuiEvent.RENDER_HUD`): 400 ticks, fading out, in the 1.7.10 rainbow colour (`cos(ticks / 250)`).
- **Wings and ghost blocks** need the two loader hooks (`docs/api-requests/KamiGear.md` section 4); Fabric has them
  (`fabric/.../client/KamiGearFabricClient.java`), which `KamiGearClient.register()` starts by name on Fabric (`Platform.isFabric()`),
  so `fabric.mod.json` needs no entry (listing the class as a `client` entrypoint as well is harmless: it registers once). NeoForge has none yet.
- **Network.** The two S2C payload types (`armor_status`, `shadow_beam`) are registered by their client receivers
  (`KamiGearClient`); `KamiGear.registerNetwork()` registers them itself only on a dedicated server (`Platform.getEnvironment() ==
  Env.SERVER`), because Fabric's payload registry throws when a client registers a type twice. `PacketToggleArmor` is C2S and is
  registered on both sides.
- **Models.** The 33 items have JSON item models (`assets/thaumictinkerer/models/item/<path>.json`); the awakened tools and the ring have the
  override models of their modes/state. Nothing was looked at in a running client.

## 11. Deliberately not carried over

- `GameRegistry.addShapelessRecipe(9 x nugget <- ichorium)` is a data recipe: `data/thaumictinkerer/recipe/ichor_nuggets.json`. It is
  not conditional on `enableKami`: with the option off `ichorium` does not exist and the game logs **one recipe parse error** at data
  load (the tag files are optional entries and cause none). A load condition would be loader-specific
  (`fabric:load_conditions` / `neoforge:conditions`) and could not be tried here.
- The item class `ItemKamiBase` is `KamiItemBase`; `ItemKamiResource.getRarity` per damage value is the per-item rarity above.
- The creative tab entry of the 1.7.10 mod's `ichor wand` is the tab stack above.
