# SimpleBlocks: deviations from Thaumic Tinkerer 1.7.10 (GTNH fork `28f4def9`)

Group SimpleBlocks: Smokey (dark) Quartz with its chiseled block, pillar, slab and stairs; the Paving Slab and Stair of
Travel; the Paving Slab of Warding; the Ethereal Platform and the camouflage base; Gaseous Illuminae, Gaseous Tenebrae and
Nitor Gas with their items (the phials, the Fume Dissipator, the Hyperenergetic Nitor); the Forcefield; Infused Farmland,
Imbued Grain, Infused Seeds and the four Infused Potions with the four Imbued effects; the six elemental fires; the
loot table of the crops, the numbering of the soul aspects, and the bone meal rule.

Everything listed here is a place where the port does something other than the original, with the reason. A value that is
not listed (a research position, an aspect, a recipe, a hardness, a tick interval, a range, a probability, a loot weight)
is the original's. Each entry says whether it is **forced** (1.21.1 or the loader-neutral rule cannot do it), a **fix** (a
reproducible defect of the original, with the evidence) or a **decision** (the port chose between two readings and the
lead may overrule it). Nothing here was run in a game when this file was written; the client smoke and the GameTests came later, and the end of
the file says what they covered and what is unverified.

## 1. Mappings that every entry below relies on (forced)

### 1.1 Metadata and block ids

| 1.7.10 | 1.21.1 |
|---|---|
| `BlockDarkQuartz` metadata 0 / 1 / 2 | three blocks: `dark_quartz` (plain, with the darker top and bottom), `dark_quartz_chiseled`, `dark_quartz_pillar` (a `RotatedPillarBlock`, axis x/y/z; 1.7.10 metadata 2/3/4 were the axes) |
| `BlockDarkQuartzSlab` half / full (two blocks) | one `dark_quartz_slab` with the vanilla `type=bottom/top/double` property; the full block `dark_quartz_slab_full` does not exist (its lang key stays in `en_us.json` unused) |
| `BlockTravelSlab`, `BlockWardSlab` half / full | the same: one slab each, with the vanilla double state. The ward slab keeps its block entity in the double state, as both 1.7.10 blocks had one |
| `BlockDarkQuartzStairs`, `BlockTravelStairs` | `dark_quartz_stairs`, `travel_stairs` (the 8 orientations of the metadata are `facing` + `half` + `shape`) |
| `BlockGas` family: metadata 0..15 | block state property `level` (0..15) of `gaseous_light`, `gaseous_shadow`, `nitor_gas`. A freshly released phial is level 4. The Nitor Gas uses level 1 for the strong variant of the Kami leggings (light 15, range 6) and 0 for the item (light 12, range 1), as the metadata did |
| `ItemGas` metadata 0 / 1 | two items: `gaseous_shadow_item` (the default item of 1.7.10) and `gaseous_light_item` |
| `BlockPlatform` (a `BlockCamo`, camouflage id + metadata in `TileCamo`) | `platform` with the property `camouflaged`; the imitated `BlockState` is saved in `TileCamo` under `camo` (section 2.4) |
| `BlockInfusedGrain` metadata 0..7 (age) | `CropBlock.AGE` 0..7 |
| the crop's picture by the aspect in its tile | the property `texture` 0..6 (0 aer, 1 ignis, 2 aqua, 3 terra, 4 ordo, 5 perditio, 6 any other), set when the seeds are planted; the blockstate file maps `age` and `texture` to the model of the picture (age 0-1 -> picture 0, 2-3 -> 1, 4-6 -> 2, 7 -> 3, the `meta == 6 -> 5` rule of the original) |
| `BlockFireBase` metadata 0..15 | `FireBlock.AGE` 0..15 (0 is the source that never burns out) |
| `ItemInfusedGrain` metadata 0..3 (aer, ignis, terra, aqua) | four items `infused_grain_aer/ignis/terra/aqua` that share one name key `item.thaumictinkerer.infused_grain` (the 1.7.10 key `item.infusedGrain.name`) |
| `ItemInfusedPotion` metadata 0..3 | four items `infused_potion_aer/ignis/terra/aqua`; the display name keeps the key `item.infusedPotion.<Aspect>.name` |
| `ItemInfusedSeeds` with the aspect and the tendencies in the stack NBT (`mainAspect`, `aspectTendencies`) | one item `infused_seeds` with two data components: `thaumictinkerer:infused_aspect` (the tag of the aspect) and `thaumictinkerer:infused_tendencies` (map tag -> amount). The creative tab lists the seeds of the primal aspects as stacks (`TTRegistry.addTabStack`), in the order of `Aspect.getPrimalAspects()` (aer, terra, ignis, aqua, ordo, perditio) as 1.7.10 `getSubItems` did, not the bare item |
| `LibBlockNames.INFUSED_GRAIN_BLOCK` (`infused_grainBlock`) | the registry path `infused_grain_block`: a capital letter is no valid path in 1.21.1. The registrar uses its own constant `SimpleBlocks.INFUSED_GRAIN_BLOCK`; the constant of the lead is unused (request in the report) |
| the potion ids from the configuration and the reflective extension of `Potion.potionTypes` | four registered effects `thaumictinkerer:potion_air/fire/earth/water` (the four id options of the configuration are gone) |
| `NumericAspectHelper.init()` in `preInit` | filled when the class is first used; `init()` stays and is harmless to call twice |
| `Material.rock` / `Material.wood` and `setHardness(h)` / `setResistance(r)` | `strength(h, r * 0.6)`: the 1.7.10 setter multiplied by 3 and the explosion code divided by 5, 1.21.1 uses the number as it is (10 -> 6.0, 5 -> 3.0). Stone-like blocks have `requiresCorrectToolForDrops()` and are in `thaumictinkerer:mineable/simple_blocks/pickaxe` with no `needs/` tier (any pickaxe, as 1.7.10 rock); the platform is in the axe tag, the infused farmland in the shovel tag |
| `getDrops` of the blocks | loot tables (`data/thaumictinkerer/loot_table/blocks/*.json`) for the quartz family, the slabs (a double slab drops two), the stairs, the platform and the infused farmland (dirt); the gases, the forcefield, the fires and the crop use `noLootTable()` and code (`getDrops` override) |

### 1.2 Recipes

The crafting-table recipes are data recipes `data/thaumictinkerer/recipe/<name>.json`; the name is lower case because the
name is also the file name (`TTRecipes.dataRecipeId`). The 1.7.10 names were `DARK_QUARTZ+n`, `PAVETRAVEL`, `PAVEWARD`:
now `dark_quartz_0..5`, `pave_travel_slab`, `pave_travel_stairs`, `pave_ward_slab`.

* A `Block` ingredient in a 1.7.10 shaped recipe meant every metadata value. The family recipes (`dark_quartz_2/3/5`) take
  the item tag `thaumictinkerer:dark_quartz_blocks` (plain, chiseled, pillar); `dark_quartz_4` takes the slab.
* The two quartz recipes with coal (`Items.coal` damage 0) and charcoal (damage 1) are one recipe with a two-item
  ingredient.
* The stairs recipe came twice (a left and a right staircase). Shaped recipes of 1.21.1 are mirrored by default: one
  recipe (`dark_quartz_5`, and `pave_travel_stairs` for the travel stairs).
* Thaumcraft's paving stones are the items `thaumcraft:paving_stone_travel` and `thaumcraft:paving_stone_warding`
  (1.7.10 `blockCosmeticSolid:2` and `:3`).
* The arcane, crucible and infusion recipes are the originals through `ThaumicTinkerer*Recipe`; the ingredients that were
  `ConfigItems.*` are `ItemApi.getItem/getBlock(field, meta)`.

## 2. Per object

### 2.1 Smokey Quartz

* **Textures (decision).** The plain block is `cube_bottom_top`: the side `dark_quartz_0`, the top and bottom
  `dark_quartz_1` (1.7.10 `getIcon` chose by side). The chiseled block and the pillar are `cube_column` (end `_1`, side
  `_0`); the pillar lying down is `cube_column_horizontal` rotated by the blockstate.
* **The slab and stairs share the textures of the block (decision).** The stairs' base state is the plain block.
* **Research (decision).** `DARK_QUARTZ` is a stub, auto-unlocked, round entry at (-2, 2) with seven pages (text and the six
  recipe pages), as in 1.7.10; the original also called `registerResearchItem()` on it twice, which is dropped.

### 2.2 Paving Slab and Stair of Travel, Paving Slab of Warding

* **Walking (forced, decision).** 1.7.10 `onEntityWalking` ran once per step. `Block.stepOn` runs on every tick the
  entity moves on the block. The effects (speed II and jump boost I for 40 ticks, as before; the duration is renewed,
  never stacked) are therefore given again only when the entity has none, a weaker one, or no more than 20 ticks are
  left: adding the same effect on every tick would re-apply its attribute modifiers and, for a player, send two update
  packets and fire the effects-changed advancement trigger every tick. The five client sparkles per call (1.7.10
  `blockSparkle(world, x, y, z, '?', 5)`) are thinned to one call in eight and drawn with `ThaumcraftClientFX.sparkle`
  (blue, size 1.75, gravity 0.1, at random places in the block; the facade has no block sparkle for a client level, and
  `'?'` is a dark blue that the nearest sparkle type, blue, approximates). The effects are given on the server only.
* **Stairs base state (decision).** `travel_stairs` takes stone as the base state of `StairBlock`; the gap analysis
  proposed `blockCosmeticSolid:2`, which would be resolved while the registry is still being filled. The base state only
  delegates the no-op callbacks (`animateTick`, `stepOn`, `entityInside`...) of the base block, so nothing observable
  changes.
* **Ward Slab: `getBlocksMovement` (forced).** 1.7.10 answered "passable for the path finder" exactly while no redstone
  signal reached the slab, asking the tile entity. `isPathfindable` has no level to ask, so the answer is the block-state
  property `powered`, kept up to date by `neighborChanged` (and set at placement). The same rule: passable (LAND) when
  unpowered. The tile entity still asks the world itself (`level.hasNeighborSignal`, the 1.7.10
  `isBlockIndirectlyGettingPowered`) for the push and the aura.
* **Ward Slab: the aura (decision).** The tile entity puts Thaumcraft's `blockAiry:4` (the warding fence,
  `BlocksTC.state("blockAiry", 4)`) into the two cells above every 100 ticks unless the cell holds `blockAiry:3` or cannot
  be replaced. The original replaced a cell that already held `blockAiry:4` by the same block; the port leaves it alone.
  The fence destroys itself unless a warding stone is one or two blocks below (`docs/API_GAPS.md` 3b), so the aura
  flickers exactly as in 1.7.10; nothing is added.
* **Ward Slab: the push (forced).** `addVelocity` is `LivingEntity.push(x, y, z)`; the yaw arithmetic is the original's.
* **Ward Slab: the runes.** The display tick calls `ThaumcraftClientFX.blockRunes` with the original colours, durations
  (20, 24, 20 ticks) and gravities (-0.02, -0.02, 0): red-orange when the aura is missing, violet when powered,
  magenta-red around a mob. The test "is there an aura above" compares the block of the aura, as the original
  compared blocks, and "passable" is `!blocksMotion()` (the old material flags: a carpet or a snow layer is passable;
  the collision shape would call them solid).

### 2.3 Gases, Fume Dissipator, Hyperenergetic Nitor

* **Not an air block (forced).** The 1.7.10 gases were `Material.air`; a light-emitting air block does not light a chunk
  section that holds nothing else, and 1.21.1 treats `air()` blocks as air everywhere (the light engine, `isAir()`,
  `canBeReplaced`). The gases are invisible, shapeless, replaceable, instantly breakable blocks with `noLootTable()`;
  `BlockGas.isAirLike(state)` stands in for `isAirBlock` wherever the add-on asks that question (the spreading, the
  phial, the Earth wall, the fires' fuel scan). Other groups must use it too for "free space" tests that should treat gas as
  air.
* **Light (forced).** `setLightOpacity(215)` of the Gaseous Tenebrae is `getLightBlock` 15 (the scale has 16 steps). The
  Nitor Gas: light 12, or 15 for level 1; the Gaseous Illuminae 12. `getLightValue()` of the base class (15) is not needed.
* **Spreading (forced).** `updateTick` is `tick` (scheduled), and `randomTick` (the 1.7.10 random update) calls `tick`, because the
  default `randomTick` of 1.21.1 does nothing (found by the first GameTest run of the fires; the gases had the same gap). The order is the original's: -x, +x, -y, +y,
  -z, +z; a cell that already holds the same gas is not overwritten, a cell with another gas is (every gas was an air block
  in 1.7.10, so one gas spread over another). The random tick of a settled gas does nothing;
  a scheduled tick (ten ticks) is queued for every cell a spreading creates, as in 1.7.10 (`scheduleBlockUpdate`).
* **Particles.** The display ticks call `ThaumcraftClientFX.sparkle` (yellow for the Gaseous Illuminae, red for the Nitor
  Gas, size 1, gravity `nextFloat() / 2`, as in 1.7.10) and `ThaumcraftClientFX.wisp` (the dark wisp of the Gaseous
  Tenebrae: size 0.125, type 5, shrinking, no clip, gravity -0.02). `placeParticle`, which runs on the server, sends the
  sparkle of the Gaseous Illuminae with `ThaumcraftFX.sparkle` (1.7.10 drew it only in the integrated client, so a
  dedicated server shows more). `ThaumcraftFX` has no wisp, so the Gaseous Tenebrae that is placed or dissolved on the
  server shows a vanilla smoke puff instead (request A8, the server twin of the wisp).
* **Gas item (decision).** `onItemRightClick` of the phial is `use`. The cell is `floor(position) + 1` (1.7.10 cut with
  `(int)`, which is the same above zero and wrong below it), the phial is used up even when there is no room and kept by a
  creative player (both as before), the pop sound is `ITEM_PICKUP`.
* **Fume Dissipator.** The sound is `ThaumcraftSoundIds.WAND` (skipped if it is not registered). The cube is `x-3 .. x+2` as the 1.7.10 loops were `< xs + 3`. The block position of the player is the floor,
  not a cut.
* **Hyperenergetic Nitor (decision).** `onUpdate` is `inventoryTick`; the static `meta` of the original (set by the Kami
  leggings for the duration of one call) is kept, and `ItemBrightNitor.placeNear/setBlock(level, pos, strength)` take the
  strength as an argument, which is the safer way in for other groups. The gas goes where there is air or the same gas
  (`isAir()`; not into another gas, as `Blocks.air` was tested). The cell is `floor(y) + 1`.
* **Nitor carrier (forced).** `inventory.hasItem` is `getInventory().contains(...)`; the Kami leggings are looked up by id
  (`ichorcloth_legs_gem`) while `enableKami`.

### 2.4 Ethereal Platform and the camouflage base

* **The collision (decision, forced).** 1.7.10 compared `entity.posY > y + (isPlayer ? 2 : 0)`. A player's `posY` was the
  eye height on the client (feet + 1.62) and the feet on the server, so the two sides disagreed; the client decides in
  1.7.10 movement, and a server that checks collisions in 1.21.1 would not accept a different rule. The port uses the
  client's rule on both sides: a player is held when `feet + 1.62 > y + 2` (the feet more than 0.38 above the platform's
  bottom, so standing on top) and is not sneaking; any other entity when its feet are above the platform's bottom;
  `CollisionContext.empty()` (and so the path finder, which asks `isPathfindable`) sees nothing, and the block is not
  pathfindable.
* **Camouflage drawn as a block entity (forced).** 1.7.10 asked the camouflaged block for its icons per side. A dynamic
  model needs a loader API, so the block is invisible while `camouflaged` is true and `RenderTileCamo` draws the imitated
  state through `BlockRenderDispatcher.renderBatched`, with the lighting and the culling of the neighbourhood. It does not
  get ambient-occlusion shading from the chunk mesh and an animated block is not animated.
* **What can be imitated (decision).** Blocks whose render shape is `MODEL` and whose collision shape fills the cell, and
  that are no camouflage block themselves: the 1.7.10 test accepted the render types 0, 31 and 39 (plain cube, log, quartz
  block), which is this set without slabs and stairs. Items that are no `BlockItem`, or whose block is refused, are left
  alone.
* **Facing (decision).** A block with a horizontal facing imitates the face that was clicked (the 1.7.10 `BlockDirectional`
  rule); everything else its default state.
* **Hands (forced).** Only the main hand counts (1.7.10 had one hand). An empty hand clears the camouflage; a sneaking
  player with an item uses the item (vanilla), which places the item's block beside the platform.
* **Duplicate of the TileBlocks base (decision for the lead).** The Remote Placer, Dislocator and the other camouflageable
  devices of the TileBlocks group use `CamoDeviceBlock`/`CamoDeviceBlockEntity` with their own `camo` property. Both
  designs store a `BlockState` under `camo` and draw it with a block entity renderer; they could be merged later. Until then
  the platform's tile entity type `thaumictinkerer:camo` is valid for the platform only.

### 2.5 Forcefield

* The block is an invisible, replaceable, instantly breakable, full-collision block with no drops; its block entity counts
  60 ticks and removes the block on the 62nd call (`ticks < 0` is tested before the decrement, as in 1.7.10). The counter is
  not saved, so a wall that was saved lives three more seconds after a reload (as before). The sparkles on the client are
  `ThaumcraftClientFX.sparkle` (blue, two per tick at the "all" particle setting, 1.7.10 `blockSparkle(..., 255, 1)`).
* **Not a solid block for the game rules (forced).** 1.7.10 made the wall `Material.air`: no normal cube, no suffocation,
  no redstone conductor. The defaults of 1.21.1 would make the state suffocating (and a conductor), and the Earth potion
  raises the wall over the plane through the drinker's own cell when the victim is within about two blocks (the 1.7.10
  arithmetic, kept), so the drinker or the victim would take suffocation damage for the three seconds of its life. The
  block therefore has `isSuffocating`, `isViewBlocking` and `isRedstoneConductor` switched off and keeps the full
  collision (a GameTest checks the drinker takes no damage).
* It is not an air block here. The potion wall tests the cells with `isAirLike`, so a cell that already holds forcefield is
  left alone, which is also what the original's `setBlock` of the same block did.

### 2.6 Infused Farmland, Imbued Grain, Infused Seeds

* **The soil (decision).** `BlockInfusedFarmland` extends `FarmBlock` (so crops that accept farmland by class find it, and
  a solid block on top turns it into dirt, which is what `onNeighborBlockChange` of `BlockFarmland` did), is a full block
  (1.7.10 reset the bounds after the farmland constructor had shrunk them), never dries, never turns to dirt by itself
  (`randomTick` does nothing) and is not trampled (`fallOn` only does the fall damage). `canSustainPlant` (only
  `BlockInfusedGrain`) has no equivalent: vanilla crops accept only `Blocks.FARMLAND`, saplings and flowers accept dirt-like
  tags and farmland, so nothing else grows on the infused soil; a modded crop that accepts any `FarmBlock` would.
* **Growth is the tile entity's (forced).** `TileInfusedGrain.serverTick` is `updateEntity` on the server: the chance
  `1 / ((2510 - water^2) * 6)` per tick when the cell above has light 9 (`getRawBrightness(pos, level.getSkyDarken())`:
  as `getBlockLightValue` did, the sky light is reduced by the darkening of the time of day, so a crop under open sky
  does not grow at night, which vanilla crops of 1.21.1 do), and the aspect exchange `1 / ((2550 - air^2) * 10)` per tick for a crop with a compound
  aspect. `randomTick` only checks that the crop may stay (the original's `updateTick` called `checkAndDropBlock`).
  The divisor is clamped to at least one (it cannot reach zero with the cap of fifty points, so this only guards).
* **The exchange runs on the server only (forced).** The original ran it on both sides with separate dice and drew the
  trail on the client's own roll; the port runs it once and sends `ThaumcraftFX.essentiaTrail`.
* **A crop without an aspect (fix).** The original crashed with a `NullPointerException` in the exchange when a neighbouring
  crop had not had its first tick (`tileInfusedGrain.aspect.isPrimal()` with a null aspect). The port treats an unset
  aspect as `aer`, the value the tile gives itself on its first tick.
* **Loaded neighbours (decision).** The exchange skips neighbours in unloaded chunks (`level.isLoaded`).
* **Saved data (decision).** The tile saves `mainAspect` and `aspectTendencies` as 1.7.10 did (an aspect list each), as
  custom data of `TileThaumcraft`, which is also the update packet (the goggles read the aspect of the crop and the
  soil from `IAspectContainer`).
* **Harvest (forced).** `getDrops` is the override of the block with the loot parameters; the tile comes from
  `BLOCK_ENTITY` (or the level at the origin). The quirks are kept: the seed count grows while
  `nextInt(10000) < entropy^2` (up to the stack size, which the original did not limit), the bounty at age 7 repeats while
  `nextInt(75) < order`, the soil is fertilised while `nextInt(55) < earth`, and the breeding roll is
  `nextInt(10) < (fire + 1) * amount^2` over the soil's aspects.
* **Seeds (decision).** `useOn` plants on the top of any `FarmBlock` (the original tested `instanceof BlockFarmland`), turns
  the soil into infused farmland unless it is one already and plants the crop with the aspect (aer when the stack has none)
  and the tendencies of the stack. As before the player must be allowed to build at both cells. The differences: the cell
  above must be air or replaceable (the original replaced whatever stood there, even a solid block), and the stack is not
  used up for a creative player (the original always took one). The seeds are not an `ItemSeeds`, so nothing else plants
  them.
* **Pick block (decision).** The crop's pick-block is the seeds of its aspect.
* **Bone meal (forced).** `BonemealEvent` does not exist. `isValidBonemealTarget` is `cropsAllowBonemeal && !isMaxAge`
  and `growCrops` adds the vanilla two to five stages without touching the state's picture. The Hoe of Growth (the item
  `elemental_hoe` of Thaumcraft, looked up by id) is handled by the click handler of 1.7.10 (`BonemealEventHandler`), but
  only while `cropsAllowBonemeal` is false: otherwise Thaumcraft's own hoe grows the crop through the bone meal path and the
  handler would grow it twice. The sparkle over the crop is `ThaumcraftFX.blockSparkle` in white and the sound is
  `ThaumcraftSoundIds.WAND` (decision: 1.7.10 passed the colour `0`, which the 4.2 proxy reads as black before the jitter
  of the channels; white is what a growth sparkle is meant to look like). The hoe loses 25 uses (`hurtAndBreak`). The electric hoe of Electro-Magic Tools has no
  counterpart.
* **The picture of the crop (forced).** `RenderInfusedCrops` (an ISBRH) is a blockstate file (56 variants: 7 texture groups
  by 8 ages), the cutout render layer and a block colour handler that tints the generic group with the colour of the aspect
  (`SimpleBlocksClient`). The seeds' icon (`RenderGenericSeeds`) is the item property `thaumictinkerer:aspect_texture` with
  seven overrides. Its value is the texture group divided by eight (0, 0.125, 0.25 ... 0.75; `SimpleBlocksClient.aspectTextureValue`),
  not the group number: Architectury registers the function as a vanilla `ClampedItemPropertyFunction`, whose `call` clamps
  every value to 0..1, so in the first version the groups 2 to 6 all arrived as 1 and the seeds of aqua, terra, ordo, perditio and of
  every compound aspect were drawn as ignis seeds (seen in the client smoke picture of 2026-10-30, fixed afterwards). Eight is a power of two: the
  values and the thresholds in `models/item/infused_seeds.json` are exact floats, which a division by six would not give.
  The crop model is the vanilla `crop` (four crossed planes), not the 1.7.10 cross with the same
  textures.

### 2.7 Aspect crop loot

* The table is the original's entry for entry (the aspect, the stack, the count and the weight). A stack built from a
  Thaumcraft item uses `ItemApi.getItem(field, meta)` or `ItemsTC.stack(id)`; an item that does not exist is skipped.
* **Ore dictionary (forced).** The substring scans of the ore dictionary (`logWood`, `treeSapling`, `ingotCopper`,
  `slimeball`, `plankWood`, ...) are item tags (`minecraft:logs`, `minecraft:saplings`, `c:ingots/copper`, `c:slime_balls`,
  ...), one entry of weight one for every member of the tag, resolved when the loot is rolled (the tags are not bound while
  the table is filled in the ready phase). Names without a counterpart in 1.21.1 or its common tags (`bucketEnder`,
  `rodBlizz`, `cloud`) add nothing, as they added nothing in 1.7.10 without the mod that registered them. "Grass" is read as
  the grass items. The ore names `bucket`, `bowl` and `ingotVoid` are read as the vanilla buckets (empty, water, lava, milk),
  the bowl (count 4) and Thaumcraft's void metal ingot (count 2): the first two are what Forge registered in 1.7.10, the
  last one what Thaumcraft registered.
* **Dyes and wool (decision).** The 16 dyes of the Senses loot were `Items.dye` damage 0..15 (ink sac, cocoa beans, lapis
  lazuli and bone meal among them); they are the 16 coloured dyes of 1.21.1 (`DyeItem.byColor`). The 16 wools, stained glass
  blocks and stained glass panes are by `DyeColor`; the healing potion (`potionitem:8261`) is a potion of healing.
* **Recipe viewers.** `getAllLoot()` returns the whole table with the tags resolved, aspect to weighted stacks; the NEI
  handler that listed it is dropped and an EMI/JEI category should be written later (the lang key `tt.nei.category.infusedseeds`
  is kept).

### 2.8 Infused potions and the Imbued effects

* **Effects (forced, decision).** `DummyPotions` are `ImbuedEffect` instances of the `HARMFUL` category (the constructor
  argument `true` was "bad effect") with colour 0 that do nothing by themselves. The names are the effect keys
  `effect.thaumictinkerer.potion_air/fire/earth/water` (English "Air Imbued" and so on, as `setPotionName`); the icons are
  copies of the potion item pictures (16 by 16, the effect atlas scales them).
* **Drinking (decision).** `ItemInfusedPotion` is drunk like a vanilla potion (use animation `DRINK`, 32 ticks), gives the
  effect for 3600 ticks, leaves a glass bottle and counts as a used item; the effect only on the server. It glints
  (`isFoil`), as `ItemPotion` did while `getEffects` was not empty. Its description id is the key of its name
  (`item.infusedPotion.<Aspect>.name`), so the statistics screen shows the name too.
* **The hit (forced).** `LivingAttackEvent` is Architectury `EntityEvent.LIVING_HURT`, the closest event; whether it fires
  before the invulnerability checks, as the attack event did, depends on the loader and is unverified. The attacker is the
  direct source of the damage if it is a player.
* **The tables (decision).** The victims are remembered by UUID with the dimension and the game time of the hit, not as
  entities (1.7.10 held the entity objects in a static map and so kept a dead entity alive until the timer ran out). The
  tables are dropped when the server stops. A victim is processed once per tick of the server, not twice (1.7.10 handled
  both phases of the tick).
* **Air.** Every fifth tick for one second a random velocity (`x, z` in -0.5..0.5, `y` in 0..1) is set on the victim and sent
  to the clients (`hurtMarked`; `setVelocity` of 1.7.10 did not mark players). The burst of the client twin (size 0.5) is
  sent by the server with `ThaumcraftFX.burst`.
* **Fire.** Every fifth tick for five minutes the victim burns for six seconds. The 30 wisps of the client twin
  (`PotionEffectHandlerClient`, `wispFX2` type 4) have no server twin in the API, so the server sends one packet of 30
  vanilla flames in a Gaussian cloud around the victim's chest (standard deviation 1.5, the original sat on a sphere of
  radius 2.5), one packet per victim and action (30 packets would be thirty times the traffic for every victim, for five
  minutes). The table of victims has no size limit, as in 1.7.10: it holds what the player hit in the last five minutes.
* **Earth.** The wall of 5 by 5 forcefield cells half way between the player and the victim, in the plane that cuts the
  longer axis; floor (not a cut) of the coordinates; only cells that are air or gas; sparkles by `ThaumcraftFX.blockSparkle`
  (decision: one call, seven particles of the random grey, over every cell that received a wall; the client twin of 1.7.10
  called `blockSparkle(..., 100, 100)` over all 25 cells, which is a dark blue and a hundred counts each). The wall does not
  suffocate (section 2.5).
* **Water.** Every lava block within two blocks of the player becomes obsidian each tick (all heights of lava; floor of the
  coordinates, 1.7.10 cut them); the burst of the client twin (size 1.2) by `ThaumcraftFX.burst`.

### 2.9 The elemental fires

* **Class (forced).** The fires extend `FireBlock`: they are in the tag `minecraft:fire`, can be punched out, set entities on
  fire, climb walls (the vanilla fire state, drawn by the vanilla fire models with the six textures) and keep the age
  property 0..15. The model files are the vanilla fire's, renamed. Being in `minecraft:fire` has more effects than
  those: a nether portal frame treats the fire as an empty cell (`PortalShape`), the path finder rates a fire cell as
  dangerous and mobs avoid it, and the extinguishing of fire by punching applies. The file is
  `data/minecraft/tags/block/fire.json`.
* **Light, sound, random ticks.** The fires have light 0 and the default step sound, because the 1.7.10 fires never set
  either: the light level 1 and the wood sound belonged to `Blocks.fire` (set when vanilla registered it), not to the
  constructor of `BlockFire`. They are ticked at random (`randomTicks()`), as `BlockFire` called `setTickRandomly(true)`, and
  `randomTick` calls `tick` (the default `randomTick` of 1.21.1 is empty and the vanilla fire is never ticked at random):
  a fire of age 15 schedules nothing but is still ticked at random, so it burns out when no fuel is left and rain puts it out.
* **The tables (forced).** A rule is a predicate over the block state and a result block (`FireRules`), because one 1.7.10 id
  stands for a family of 1.21.1 blocks. The families are block tags: logs are `minecraft:overworld_natural_logs` (the
  stripped logs and wood are not natural logs and so not transmuted; decision), leaves `minecraft:leaves`, planks
  `minecraft:planks`, the ores of Ignis `coal_ores`, `iron_ores`, `diamond_ores`, `emerald_ores`, `lapis_ores`,
  `redstone_ores` (deepslate ores included, so more than the original). Single blocks: `Blocks.dirt` is dirt, coarse dirt and
  podzol; `Blocks.sand` is sand and red sand; `Blocks.lava`/`flowing_lava` is every level of lava; `Blocks.water` (still
  water) is a source block (`level` 0); `red_flower` is the nine small flowers; `yellow_flower` is the dandelion;
  `snow_layer` is a snow layer; `tallgrass` of metadata 0 is the dead bush; the nether brick fence is turned into an oak
  fence. The result is the default state of the target block, as the 1.7.10 result had metadata 0, and the properties the old
  and the new block share (stairs facing, fence connections) are carried over (not `level` and `age`).
* **Quirks kept (as in 1.7.10, on purpose).** The Ignis fire turns the dandelion into a dandelion and the gold block (not the
  ore) into quartz ore. The rain test of the spread uses `z` instead of `zt` in one of its five checks. A fire of age 0 is a
  source and never burns out.
* **Order (as in the original).** The Order fire lists stone, glass, sand and gravel with no result and overrides
  `isTransmutationTarget` to ignore them: it has no rules, is no target or result of anything, and so only burns what burns
  in vanilla. Its research text says it "is currently being reworked and does nothing".
* **Chaos.** Tick rate 1; turns the other five fires into plain fire (the rule list is built on first use because the
  blocks are registered by then and the tags are bound).
* **Flammability (forced).** The vanilla fire of 1.7.10 answered `getFlammability` for the blocks of every mod from its own
  table (planks 20, logs 5, leaves 60, wool 60, plants 100, TNT 100, ...). 1.21.1 has no such lookup that works in common
  code on both loaders, so `FireRules.vanillaFlammability` is the table of 1.7.10 vanilla, by block tags. A transmutation
  target burns surely (100), a result never (0).
* **Tick (forced).** The `doFireTick` gamerule, `enableFire` and `enableFireMechanics` are tested as before; the rain test
  (`canLightningStrikeAt` of the cell and its four neighbours) is `isRainingAt` for the same five cells; `isBlockHighHumidity`
  is the biome tag `increased_fire_burnout`; TNT is lit by `TntBlock.explode`. The rule scan treats unloaded chunks as the
  original's `blockExists`.
* **Placing (decision).** `ItemBlockFire.mustSurvive()` is false, so the item places the fire anywhere (`canPlaceBlockAt` was
  always true); `updateShape` and `canSurvive` then remove it unless it has a solid top face below or a transmutation target
  beside it (`onNeighborBlockChange`). A fire placed in a nether-portal frame lights the portal as in 1.7.10 (Overworld and
  Nether only).
* **Config switches.** The research and the recipe of a fire exist only while `enableFire` (as before); the cake rule of the
  Aer fire is read when its table is first built, so changing `enableCake` needs a restart.

### 2.10 Numbering of the soul aspects, bone meal handler

* `NumericAspectHelper` is the original's list of 17 aspects (water, man, air, flight, fire, magic, undead, flesh, beast,
  poison, earth, eldritch, travel, metal, slime, cloth, crop), filled on first use (section 1.1).
* `BonemealEventHandler` is section 2.6.

### 2.11 The debug command

* `/setCropTendency <Aspect> <Count>` (`SetTendencyCommand`) is registered through `CommandRegistrationEvent` and exists
  only while `debugCommands.enabled` is on, for everybody (1.7.10 `canCommandSenderUseCommand` returned the switch). It
  finds the crop with `player.pick(10, 1, true)` (1.7.10 `BlockUtils.getTargetBlock`), merges the count into the
  primal tendencies (the larger of the old and the new amount) and trims the sum to fifty points; a count that is no
  number answers "Invalid number"; an aspect that is not primal does nothing. **Fix:** the count is limited to 10000
  before it is merged (trimming removes one point at a time, so 1.7.10 spent two billion iterations on the server
  thread for the largest int). The crop is sent to the clients afterwards, which 1.7.10 did not.

## 3. Dropped (forced)

* **Forge Multipart parts** (`multipart/*`): the candle and Nitor parts that put a Nitor on a multipart block. Forge
  Multipart has no 1.21.1 counterpart; the Hyperenergetic Nitor and the Gaseous blocks are not affected.
* **NEI handler** (`client/nei/*`) for the seed drops: replaced by `AspectCropLootManager.getAllLoot()` for a future EMI/JEI
  plugin.
* **EnderIO planter** (`InfusedSeedsPlanter`), **Electro-Magic Tools hoe** (`EMTCompat`), **ComputerCraft turtle tool**
  (`FumeTool`, `FumePeripheral`; optional later as a data-driven turtle upgrade).
* **The `ItemBlock` helper classes** of the slabs and quartz blocks (`ItemTravelSlab`, `ItemWardSlab`, `ItemDarkQuartzBlock`)
  and `BlockMod`/`BlockModContainer`: block items are made by the registrar; the base classes are `BlockCamo` and plain
  blocks.

## 4. Not verified

Written without being able to run Gradle or the game; the GameTests were run later (see the last bullet). What was done (second pass, after the review):

* `tools/compile.sh` and `tools/compile.sh -g` (javac with the Gradle-exported class path): 0 errors for the whole add-on,
  the GameTest sources and the Fabric wrappers included. With `-Xlint:deprecation,removal,cast,unchecked,rawtypes` the
  group's own files have no warning (the deprecated `LevelReader.hasChunkAt` is called once, in `BlockFireBase.isLoaded`,
  and `blocksMotion`, `makeMockServerPlayerInLevel` carry a `@SuppressWarnings` with the reason).
* `SimpleBlocksLogicTest` (17 tests) and `SimpleBlocksResourcesTest` (17 tests), and the existing `TinkererApiOnlyTest`,
  were run with the **real JUnit engine** (Jupiter 5.11.4 and the platform launcher 1.11.4 from the Gradle cache, driven by a
  small `Launcher` main outside Gradle, the main resources on the class path, working directory
  `addons/thaumic-tinkerer`): 35 tests, 35 passed. That is still not Gradle's own test task.
* `python3 -I scripts/porting/tt_assets.py --check` was run (`check: ok (329 files)`), and every JSON file of the add-on
  parses.
* A plain JVM with the vanilla bootstrap (`Bootstrap.bootStrap()`, the block registry unfrozen by reflection so that blocks can
  be built, the state caches initialised) built the Forcefield and an Aer fire: the wall is not suffocating, not view
  blocking, not a redstone conductor, does not occlude and is replaceable; the fire has light 0, the stone sound, takes
  random ticks and is replaceable; the four potions glint and have the description ids of their names; the command
  node `setCropTendency <aspect> <count>` exists. That checks the block properties only, not the behaviour in a level.
* The 48 GameTest checks (`SimpleBlocksRuntimeChecks`, wrapped by `SimpleBlocksGameTests`) were first run on a Fabric
  dedicated server (`:fabric:runTinkererGameTest`, filter `simpleblocksgametests.*`) after the lead's integration. Three
  failed. One was a defect of the implementation: the imbued fires and the gases are `randomTicks()` blocks but 1.21.1's
  default `randomTick` is empty (1.7.10 `updateTick` served both kinds of tick), so a fire of age 15 never burned out;
  `BlockFireBase` and `BlockGas` now override `randomTick` to call `tick` (section 2.3 and 2.9). The other two were wrong
  checks: the water-imbued check waited for a mock player that nobody ticks (the framework's mock `ServerPlayer` is in
  the player list but its connection is not in the server's connection list, and `Player.tick` is only run by
  `ServerPlayer.doTick` from the packet listener), so the check calls `doTick` itself; and the gas-cloud check swept the
  area in coordinate order, which ticks a cell created a moment ago in the same sweep and makes the levels depend on the
  order, whereas scheduled ticks run wave by wave (a Python model of both orders gave 117 cells for the sweep and 125 for
  the waves), and it assumed room for a radius of four in the framework's 8-cell cage (the barrier stands at x = -1 and
  z = -1 and the floor at y = 0, the top is open): it now ticks wave by wave and compares the cloud with a flood fill of
  the free cells. A fourth check (`aCropGrowsInItsTileAndNotByRandomTicks`) failed in the first whole-group run because
  the crop's real ticker grew it during the ten ticks of waiting for the light (chance 1 in 6): it compares the age before
  and after the 2000 random ticks inside one server tick.
  Result of the last run of the whole group: 48 of 48 passed (`:fabric:runTinkererGameTest`,
  `-PtinkererGameTestFilter='simpleblocksgametests.*'`).

* **Client smoke, 2026-10-30** (`docs/CLIENT_VERIFICATION.md`: a real 1.21.1 client under Xvfb and Mesa software rendering, one flat
  world, English and then Russian). Its report had no failing check for the group's blocks and items. The engineer looked at one
  picture of the group, the item grid page `items-simpleblocks-01.png` and its `-ru_ru` twin, because of a defect found there: the
  six creative-tab stacks of the Infused Seeds drew one aer icon and five identical ignis icons (the item property was clamped to
  0..1, section 2.6). After the fix (`SimpleBlocksClient.aspectTextureValue`, thresholds in `models/item/infused_seeds.json`) the
  same page, drawn again by a new run of the harness, shows six different icons in both languages (aer yellow, terra green, ignis
  orange, aqua blue, ordo pink, perditio purple: the creative-tab order of `Aspect.getPrimalAspects()`); a colour measure of the
  six cells matches the mean colours of `seed_*.png`. The new JUnit check `everyTextureGroupOfTheSeedsSurvivesTheClampOfTheItemPropertyAndPicksItsOwnModel`
  feeds the value of each group through vanilla's `ClampedItemPropertyFunction.call` and the override rule of `ItemOverrides`; with the
  old function and the old model file it fails at group 2. `SimpleBlocksResourcesTest` has 18 tests now. Runs of 2026-10-30: add-on JUnit
  (`:fabric:tinkererTest --rerun`) 364 tests, 0 failures; the 48 GameTests of the group (`simpleblocksgametests.*`) all passed. These
  pictures show what a software renderer drew; they are not an acceptance on a GPU, and the other pictures of the group
  (block rows, the crop at every age, the tint of the generic crop, the camouflage) were not judged in that pass.

Not seen in a running game by the engineer of the group (apart from the seed icons above): the look of every model, the crop colours (the tint of the generic crop reads the block entity,
which must have reached the client before the chunk section is meshed), the camouflage renderer, the particles (the
Thaumcraft sparkles, wisps, runes and bursts of the facade have not been looked at on a frame, nor have the vanilla
stand-ins), the spreading of the gases against the light engine, the Nether tick rates, the speed of the crops, the
behaviour of the fires over time (the checks drive the tick by hand, so the real rate of the random ticks is unseen), the
flung victims of the Air potion, the hoe handler, the unloaded-chunk guards, the two sides of the platform's collision on
a dedicated server, a player who is ticked by a real connection (the mock player of the GameTests is not ticked by anybody; the water check
calls `doTick` itself), the day and night rule of the crop growth, and the first creation of the group on NeoForge
(not attempted: Fabric first). The `config` switches `enableCake`, `enableFire` and `enableKami` were read from the defaults
only.
