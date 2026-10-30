# Foci: deviations from Thaumic Tinkerer 1.7.10

Group Foci: the seven ordinary wand foci, Wand Focus: Distortion (`focus_deflect`), Dislocation (`focus_dislocation`), Ender Rift
(`focus_ender_chest`), Uprising (`focus_flight`), Mending (`focus_heal`), Efreet's Flame (`focus_smelt`) and Telekinesis
(`focus_telekinesis`), the shared `ProjectileHelper`, and the readout of the Dislocation beside the wand's dial. The Kami foci
(Recall, Shadowbeam, Experience Drain) belong to KamiGear. The pinned 1.7.10 source is GTNewHorizons/ThaumicTinkerer at
`28f4def9...`. Only what differs from that source is listed; the vis costs, the cooldowns (none), the colours, the research keys,
positions, aspects and recipes, the upgrade ranks, the formulas of Mending, Efreet's Flame, Uprising, Telekinesis and Distortion,
the sounds and the config switches are ported as they are.

How this was checked (honestly): every focus compiles with `tools/compile.sh` (also with `-g` for the GameTests). 23 JUnit tests cover
the plain rules (costs, flags, the formulas of Mending, Efreet's Flame, Uprising and Distortion, the shield's box and shift, the
blacklist of the config, the cost of taking a block and what may never be taken, the installation of the flight-kick reset, the codecs of
the Dislocation's load) and the resources (models, ornaments, depth layers, language keys, the blacklist tag). Twenty-two of them,
all but the flight-kick installation test, were first **run outside Gradle** with a hand-built JUnit launcher (the
`junit-platform-launcher` jars of the Gradle cache, the Minecraft jar bootstrapped, no world) and passed, as did `TinkererApiOnlyTest`.
`FociLogicTest.theFlightKickResetIsWhatIsInstalledAndNullRemovesIt`, which replaced the test of the reflective accessors when the
Uprising moved to the loader hook, was written later and has only run under Gradle, with the whole add-on suite (`:fabric:tinkererTest`,
which includes `FociLogicTest` and `FociResourcesTest`). 35 GameTests (`FociRuntimeChecks`) cast the foci with a real wand and
mock players in a running server: they were **run outside Gradle** in a scratch Fabric GameTest server (Fabric Loader's `KnotServer`
with the classpath of the Gradle run configuration `runGameTest`, the Thaumcraft mod as built in `fabric/build`, and a copy of the
add-on whose `ContentGroups` lists only `Foci`) and all 35 passed in the last run (the first run of the review round had one failure,
a mistake of the new rose-bush test, which stood on stone). Deliberate changes in the copy made checks fail, so they bite: in the
first round the Mending threshold, the removal of a block entity before its block and the shield's shift (five checks); in the review
round the missing flight-kick reset, the missing two-part refusal and the missing flowing-water refusal (one check each). The
whole-add-on Gradle run of the lead (`runTinkererGameTest`, `fabric/build/tinkerer-gametest-results.xml`, 17:44) passed every Foci check
that existed then except the three that assert "no registration failures" (a KamiGear resource-location error, not a Foci one); the
checks added in the review round were not in it. After the Uprising was moved to the loader hook (section 2) the Foci GameTests were
run again through Gradle (`runTinkererGameTest`, filter `focigametests.*` together with the Machines group): 36 checks, all passed
(the 35 above, with the kick-counter check rewritten to read the counter through `FloatingTicks`, and one new check, section 2). The
interplay with the other groups (the Magnet that pulls with the Telekinesis focus) was not run. Nothing here has been seen in a running **client**: every particle (now Thaumcraft's own, drawn through
`ThaumcraftClientFX`, which a server test cannot exercise), sound, HUD pixel, the model of a focus on the wand and the feel of a held
cast is unverified.

## 1. Identity

| 1.7.10 | 1.21.1 |
|---|---|
| items `focusDeflect`, `focusDislocation`, `focusEnderChest`, `focusFlight`, `focusHeal`, `focusSmelt`, `focusTelekinesis` | `focus_deflect`, `focus_dislocation`, `focus_ender_chest`, `focus_flight`, `focus_heal`, `focus_smelt`, `focus_telekinesis` (`LibItemNames`), registered in the 1.7.10 order |
| `ItemModFocus extends ItemFocusBasic` | the same, over `thaumcraft.api.wands.ItemFocusBasic` (stack size one and rare rarity come from the API's constructor; `EnumRarity.rare` of `ItemModFocus.getRarity` is therefore kept) |
| `IIcon` ornament and depth layer, registered by `IconHelper.forItem(..., "Orn"/"Depth")` | `getOrnament`/`getFocusDepthLayerIcon` answer `thaumictinkerer:textures/item/<path>_orn.png` / `_depth.png` (the converted textures; the depth layer of Mending is the animated strip with its `.png.mcmeta`) |
| item icons | flat item models `models/item/<path>.json` (`minecraft:item/generated`, `layer0`); the 1.7.10 icons already carry the focus colour, so no tint |
| `focus.onFocusRightClick(stack, world, player, MovingObjectPosition)` | `onFocusRightClick(wandStack, level, player, hand)`: the API passes no hit, so a focus that needs a block asks `ItemModFocus.trace(player, range, fluids, collidable)` |
| `wand.consumeAllVis`, `getFocusPotency/Enlarge` on the wand item | `WandHelper` of the API; the cost of a focus is read from `getVisCost(WandHelper.getFocusItem(wand))` |
| `player.setItemInUse(stack, MAX_VALUE)` of the held foci | `player.startUsingItem(hand)` |
| `PlayerTracker` clearing `playerHealData`/`playerData` on logout | `PlayerEvent.PLAYER_QUIT` (and `CLIENT_PLAYER_QUIT` for the client's own maps), registered by `Foci.register()`/`FociClient` |
| `ItemFocusDeflect.DeflectBlacklist` (a list of classes) | a set of entity-type ids, `ProjectileHelper.DEFLECT_BLACKLIST`, and the entity-type tag `thaumictinkerer:deflect_blacklist` (data pack); both are checked |
| `ThaumicTinkerer.registry.getFirstItemFromClass(ItemFocusFlight.class)` | `TTRegistry.stack(LibItemNames.FOCUS_FLIGHT)` |

The wand's tooltip, the cost list, the upgrade lines and the vis payment (frugal, caps, gear) are the API's. `canApplyUpgrade`
is **not** overridden to `true` as `ItemModFocus` did (see `docs/API_GAPS.md` 3b): in 1.21.1 that would let an upgrade that
`getPossibleUpgradesByRank` does not offer make the focus unmountable. The focal manipulator offers exactly what each focus lists.

## 2. Uprising (`ItemFocusFlight`)

- **Potency.** 1.7.10 asked `getUpgradeLevel(itemstack, potency)` with the *wand's* stack, which carries no upgrade tag, so the
  answer was always zero (and the focus offers no potency upgrade). The port asks the mounted focus: also zero, so the
  launch is `1 / 1.5` blocks per tick, exactly what 1.7.10 did. The runic bonus of the primal staff, which `WandHelper.getFocusPotency`
  would add, is not used either (1.7.10 did not read it here).
- **Fly-kick.** 1.7.10 reset `playerNetServerHandler.floatingTickCount` on every cast. The 1.21.1 counter
  (`ServerGamePacketListenerImpl.aboveGroundTickCount`, an int; the server disconnects a floating player above 80) is private.
  The group's first version wrote it by reflection (two field names, a warning when neither was found); that stop-gap is
  **deleted**. The loader layer now owns the access: `thaumic.tinkerer.common.hooks.FloatingTicks` (`docs/HOOKS.md` section 9) reads
  and writes the field through an accessor mixin of the Fabric module, no reflection. `Foci.register()` installs
  `FociHooks.setFloatingTickReset(FloatingTicks.RESET)` once, and every cast of the focus on the server calls
  `FociHooks.resetFloatingTicks(player)`, which runs whatever is installed; nothing is installed by default, so the start-up
  call is what makes the cast reset the counter (a GameTest pins it). The rule is the 1.7.10 one: only a cast that does something
  (config on, vis paid) resets, only the caster's counter, and a player without a connection (a fake player) has nothing to reset.
  What the hook does not give: a platform whose module has no mixin (NeoForge today) gives `isAvailable == false`, and then the
  reset silently does nothing. **The group needs the NeoForge poster of `FloatingTicks`** (the same interface on the same class by a
  mixin, `docs/HOOKS.md` section 9): until the NeoForge module has it a player who keeps launching himself with the Uprising on a
  server without `allow-flight` can be kicked after four seconds of climbing. On a single-player world and on a server that allows
  flight there is no difference. Checked by GameTests on the real connection of a mock player: a cast through the wand's own
  `use` puts 79 back to 0 for the caster and leaves another player's counter, and a cast of another focus, alone; no vis, the
  config switch off, and no installed reset leave the counter as it was.
- The velocity is set on both sides (the client's own player does the flying; the server's copy is not sent as a velocity packet).
  `fallDistance = 0` is `resetFallDistance()`.
- The smoke spiral is Thaumcraft's own: `ThaumcraftClientFX.smokeSpiral(level, x, y - motionY, z, 2F, random 0..359, (int) y, 0x09E2FF)`
  five times per cast, on the client of the caster only, as in 1.7.10. The `y` is the player's **eye height** (`getEyeY()`): the
  1.7.10 client's `posY` of the local player is the eyes (`yOffset` 1.62), the server's the feet. That is from memory of 1.7.10, not
  checked against the original jar.

## 3. Mending (`ItemFocusHeal`)

- `p.shouldHeal()` is `Player.isHurt()`. `p.heal(1 + potency)` runs on the server only (1.7.10 ran it on both sides; the client's
  health follows the server's).
- The progress map `playerHealData`, keyed by the player's *name*, is a map by UUID **per side**. 1.7.10 shared one static map between
  the integrated server and the client of a single-player game (both counted into it); the dedicated-server behaviour, which a
  multiplayer player had, is the one kept.
- The sparkle is Thaumcraft's own, `ThaumcraftClientFX.sparkle(level, x, y, z, 0)` (violet) at `x + rand - 0.5`, `y + rand`, `z + rand - 0.5`
  of the caster's client, with `y` counted from the eyes (see Uprising).

## 4. Efreet's Flame (`ItemFocusSmelt`)

- **The furnace** is the level's recipe manager, `RecipeType.SMELTING` (`SingleRecipeInput` of the block's item); a result counts only
  when it is a `BlockItem`, as `result.getItem() instanceof ItemBlock`. Vanilla 1.21.1 turns cobblestone into stone, stone into smooth
  stone, the sands into glass (`#minecraft:smelts_to_glass`), clay into terracotta, every coloured terracotta into its glazed
  one, the stone, deepslate, nether and blackstone bricks into their cracked ones, sandstones and quartz blocks into smooth ones,
  basalt, wet sponges, and cobbled deepslate. As in 1.7.10 a block that melts into a block that melts again goes on while the
  player holds the beam (stone into smooth stone now): the block is forgotten and counted again from twenty in the same tick.
- **The target** is the first block with a collision box within ten blocks of the eye, looking through fluids and grass
  (`BlockUtils.getTargetBlock(world, p, false)`: a fixed ten-block ray, not the player's reach); it is `Level.clip` with
  `ClipContext.Block.COLLIDER`.
- **Per side.** The progress is per side (see Mending). The client also forgets the block at completion; 1.7.10's client kept
  counting below zero and repeated the sound and the burst on every tick until the server's block arrived, which a chained
  recipe would have made a second-long storm of ignition sounds.
- **New: protection.** A block inside spawn protection or the world border (`Level.mayInteract`), or in adventure mode
  (`Player.mayUseItemAt`), is not smelted. 1.7.10 had no check.
- Sounds: `fire.ignite` is `FLINTANDSTEEL_USE`, `fire.fire` is `FIRE_AMBIENT`. Particles are Thaumcraft's own, the calls of 1.7.10 with
  the same arguments: `ThaumcraftClientFX.wisp(level, x, y, z, rand / 2, 4, true, true, -rand / 10)` (25 at completion, 2 per tick)
  and `ThaumcraftClientFX.beam(level, player, centre of the block, 0xFF0000, 2, true, 0F, 1)` on every client tick (the beam lets go
  on its own when the calls stop).

## 5. Telekinesis (`ItemFocusTelekinesis`)

Ported as it is: items (`ItemEntity`) within six blocks of the point five blocks along the sight from the player's centre (on the
centre when sneaking), half a block up, are given the motion towards it at a third of a block per tick (`MiscHelper.setEntityMotionFromVector`),
paid per tick only when there is something to pull. The sparkle of every pulled item is `ThaumcraftClientFX.sparkle(level, x, y, z, 0)`
(client of the caster only, as in 1.7.10). The motion is set on both sides, as the client simulates dropped items between the
server's updates.

## 6. Distortion (`ItemFocusDeflect`, `ProjectileHelper`)

- **What is a projectile.** 1.7.10 shifted every `IProjectile`: arrows and `EntityThrowable` (snowballs, eggs, pearls, potions, and
  Thaumcraft's own frost shards, bolts and orbs). 1.21.1 has one base class, `Projectile` (which Thaumcraft's own bolts extend
  directly). `ProjectileHelper.isDeflectable` is `Projectile` **minus** `AbstractHurtingProjectile` (fireballs, wither skulls, wind and
  breeze charges), fireworks and fishing bobbers, which were never `IProjectile`. Llama spit and shulker bullets, which did not exist
  in 1.7.10, are shifted like arrows.
- **The owner** (`ProjectileHelper.getOwner`, 1.7.10 `ownerGetters` by class) is `Projectile.getOwner()`; the registry of owner
  getters remains for an entity that is not a `Projectile`. The comparison `getOwner(e) == p` is the same.
- **The blacklist** is a set of entity-type ids plus a tag. It holds the experience bottle (`setupBlackList`), as 1.7.10 did with the
  class. The Botania and Blood Magic entries are dropped (neither mod exists for 1.21.1); the interface rule (`isInterface()`)
  has no counterpart and is not needed.
- **The box.** `p.posX - (4 + range)` ... `p.posX + (3 + range)`, with `range` the enlarge upgrade, centred on the player's feet
  (`getY()`; the 1.7.10 `posY` of a player is the eye height on one side and the feet on the other, from memory, not checked against
  the original, and the KamiGear copy uses the feet too). Each projectile is moved along its flight by twice its distance from the
  player. Stuck arrows, which keep a tiny motion, are carried too, as the 1.7.10 code would carry them.
- It runs on both sides (1.7.10 did, with six sparkles on the client: `ThaumcraftClientFX.sparkle(level, x, y, z, 6)`, white, before
  the projectile is moved); the server's position is the one that stays.
- KamiGear keeps its own copy of this rule in `ItemGemChest.protectFromProjectiles` (arrows and `ThrowableProjectile` only, so
  Thaumcraft's frost shards and orbs are not shifted); it can call `ProjectileHelper.protectFromProjectiles(player, 0)` instead.

## 7. Ender Rift (`ItemFocusEnderChest`)

The window is the vanilla one: `ChestMenu.threeRows` over the player's ender chest (`getEnderChestInventory()`), opened on the server;
the sound `mob.endermen.portal` is `ENDERMAN_TELEPORT`. The EnderStorage branch of 1.7.10 (`EnderStorageFunctions`, the research page
`ES`) is not ported: that mod does not exist for 1.21.1; the lang key `FOCUS_ENDER_CHEST.ES` stays unused. Research and recipe
exist only while `ThaumcraftApi.isMirrorsAllowed()` (1.7.10 `Config.allowMirrors`), also for the Distortion, Dislocation and Mending
research, as in 1.7.10 (the recipes of those three stay).

## 8. Dislocation (`ItemFocusDislocation`)

| 1.7.10 | 1.21.1 |
|---|---|
| stack tag `available`, `blockName`, `blockMeta`, `tileCmp` (and the deprecated `blockID`) | data component `thaumictinkerer:dislocation_picked` `{state, tile}` on the focus stack inside the wand: the whole `BlockState` (metadata no longer exists), the block entity's `saveWithFullMetadata` tag; "available" is the presence of the component. Old tags are not read (fresh worlds only) |
| `world.removeTileEntity` before `setBlock(air)`, so that a container drops nothing | `Level.removeBlockEntity` first, then `setBlock(air, 3)`: in 1.21.1 `LevelChunk.setBlockState` calls `onRemove` with the section already holding air, so a container whose block entity is gone finds none and drops nothing (checked in the bytecode) |
| `TileEntity.createAndLoadEntity` + `setTileEntity` | the block entity the placed block made is loaded in place (`loadWithComponents`) when its type matches, otherwise `BlockEntity.loadStatic` + `setBlockEntity` |
| mop from the wand's ray | `trace(player, blockInteractionRange(), fluids = true, collidable = false)`: the ray includes fluid **sources** (`ClipContext.Fluid.SOURCE_ONLY`), as the wand's ray does in the repository's 1.12 reference (`ItemWandCasting.onItemRightClick` asks `rayTrace(world, player, true)`). 1.7.10's `rayTraceBlocks(..., true)` stopped only on a source block (`BlockLiquid.canCollideCheck` wants metadata 0, from memory of vanilla 1.7.10), never on flowing water or lava, which would otherwise be picked up as a flowing state and spread again when put down; `canTake` also refuses a flowing `LiquidBlock` for a cast with a hit made by hand. The 1.7.10 original of the wand could not be read in this session, so the range (the player's block interaction range) is the port's reading of it |
| placement guard: `block.canPlaceBlockOnSide` of the *clicked* block (in effect: the target is replaceable), then `onBlockPlacedBy` | the target is replaceable (`canBeReplaced`), the carried state can stand there (`canSurvive`), no entity blocks it (`isUnobstructed`), `Level.mayInteract`; then `Block.setPlacedBy` with a plain stack of the block. A block that cannot stand there is **kept in the focus**; 1.7.10 placed it anyway, so it could pop off as an item and its block entity data was lost |
| `canPlayerEdit(x, y, z, side, stack)` | `Player.mayUseItemAt(pos, face, wand)` |
| spawner (`TileEntityMobSpawner`) costs 100/100/50 | `SpawnerBlockEntity` and, new in 1.21, `TrialSpawnerBlockEntity` |
| blacklist: `Block.blockRegistry.getNameForObject(block)` equals an entry of `focusOfDislocationBlacklist` | the block's registry id equals an entry (an entry without a namespace is a `minecraft:` block, as the 1.7.10 names were); **new**: an entry `#namespace:tag` names a block tag. `ThaumcraftApi.portableHoleBlackList` is the block tag `#thaumcraft:portable_hole_blacklist` (beds, doors, pistons, flux) |
| 1.7.10 piston blacklist | `Blocks.MOVING_PISTON`, `Blocks.PISTON_HEAD`; air and anything with a destroy speed of -1 cannot be taken |
| `burst` ×8 at pickup (`blockX + rand, blockY + rand, blockZ + rand`) and placement (`+ 0.65` on `y`) on the caster's client | the same eight `burst(.., 0.2F)` at the same points, sent by the server (`ThaumcraftFX.burst`) to the players around, so that the server decides and the others see it too; nothing is sent when the cast fails |
| `getUniqueKey`: block name + hash of a fresh tag (always 0) | block state + hash of the carried block entity, so that two foci that carry different things are not merged by the focus selector |

New safeguards (not in 1.7.10, each from something that 1.21.1 makes possible):

- **Size cap.** A focus does not take a block whose saved block entity is larger than `ItemFocusDislocation.MAX_TILE_BYTES`
  (1,000,000 bytes of NBT); the player gets the action-bar message `ttmisc.focusDislocation.tooBig` and nothing is paid. The
  load travels with the wand to every client that sees it, and a client refuses a tag above two megabytes, which would
  disconnect the player at every login (a chest of nested shulker boxes can reach that).
- **Protection.** `Level.mayInteract` (spawn protection, world border) and `mayUseItemAt` are asked for a pickup too.
- **Double chests.** The half of a double chest that is taken is a *single* chest in the focus (1.21.1 chest halves carry their
  connection in the state).
- **Blocks of two parts are not taken** (`ItemFocusDislocation.isTwoPart`: any state with `DOUBLE_BLOCK_HALF`, which the tall
  flowers, tall grass and ferns, tall seagrass, the pitcher plant, the small dripleaf and the doors have, or `BED_PART`). In 1.21.1
  `DoublePlantBlock.setPlacedBy` and `BedBlock.setPlacedBy` set their other half over whatever stands above the target without any
  check (bedrock, a chest, a protected block), and the upper half of a plant can stand only on its own lower half, so a carried half
  could neither be placed safely nor, for the upper half, at all. 1.7.10 could not carry them either: the half dropped as an item at
  once, because it checks that its other half exists. Doors and beds were already on Thaumcraft's blacklist; the rule makes that
  independent of a data pack that edits the tag. A load of that kind that a command made is dropped by the next cast on a block
  instead of overwriting the block above or keeping the focus full for ever.
- The cost is whatever 1.7.10 asked, but a block with a block entity is judged by the 1.21.1 block entity (a trial spawner is a
  spawner); a fluid source can be taken (the ray includes fluid sources, not flowing fluid) and placed as a source, as the 1.7.10 ray allowed.

The readout (`DislocationHUDHandler`, 1.7.10 `HUDHandler.drawDislocationFocusHUD`): same box, same texts, in the unicode font as
1.7.10 drew them (`font.setUnicodeFlag(true)` is the style font `minecraft:uniform`), the carried block's item (a fluid shows its
bucket); the "Extra Contents" bar is as wide as the longer text (1.7.10 sized it by the first text). It sits at the position of
the default dial (1.7.10 moved it to the bottom with Thaumcraft's `dialBottom` setting, which the API does not expose). 1.7.10
looked at the wand in the main hand only; the wand in the off hand counts too, and the overlay is hidden with the rest of the
interface (the F1 key). It is the only overlay of the Dislocation: BasicItems once ported the same method as `HUDHandler` with a contract
(`IDislocationHudSource`) that no focus implemented; that copy and the contract were deleted by their group
(`docs/deviations/BasicItems.md` section 8), so nothing is drawn twice.

## 9. Visual and sound approximations

Thaumcraft's client particles are the twins of the proxy calls of 1.7.10 in `thaumcraft.api.client.ThaumcraftClientFX` (`sparkle`,
`smokeSpiral`, `wisp`, `beam`) and `thaumcraft.api.ThaumcraftFX.burst`, called with the arguments the original passed (listed above for
each focus); the group's first version used vanilla particles because the API had no facade yet. They are drawn on the client of
the caster only, as in 1.7.10, except the bursts of the Dislocation. The facade's own notes say its particles have not been looked at
on a rendered frame, and neither have these calls. The sounds are the original ones (`thaumcraft:wand`, `thaumcraft:wind` through
`ThaumcraftSoundIds`, the vanilla fire, enderman and block-break sounds), played with `Level.playSound(player, ...)` on both sides so
that the casting player hears his own copy once and the others hear the server's.

## 10. Not done

Nothing of the seven foci is left out. Dropped on purpose: the EnderStorage page and branch, the Botania and Blood Magic
blacklist hooks (`BotaniaFunctions`, `BloodMagic`, `IManaBurst`), and the `canApplyUpgrade` override.
