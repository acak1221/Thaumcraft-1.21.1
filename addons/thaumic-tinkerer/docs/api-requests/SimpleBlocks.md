# SimpleBlocks: requests to the Thaumcraft API and to the loader modules

Group SimpleBlocks: Smokey Quartz, the paving slabs, the Ethereal Platform, the gases and their items, the Forcefield, the
infused crops and potions, the elemental fires. The numbers `A4`, `A5`, `A7`, `A8` are those of `docs/API_GAPS.md` section 2.
Nothing below was done by reaching past `thaumcraft.api` into the mod's internals: every item is a named stub or a workaround with the loss
written down. Priorities are the group's own opinion (P1 changes what the player sees, P3 only hardens something that works
today).

## 1. Particles of the blocks (A8, P3, mostly done)

**Done by the API since the first version of this file.** `thaumcraft.api.client.ThaumcraftClientFX` (`sparkle`, `wisp`,
`blockRunes`, `burst`, `particleCount`) and the server twins in `ThaumcraftFX` (`sparkle`, `burst`, `blockRunes`,
`smokeSpiral`) exist and the group uses them: the Gaseous Illuminae and the Nitor Gas (sparkle), the Gaseous Tenebrae (wisp,
client), the Ward Slab (`blockRunes`), the Travel/Ward Slab and the Forcefield (sparkle, standing for `blockSparkle`), the
Air and Water potions (`burst` from the server). `SimpleBlockFx` is the one place that picks the client or the server call.

**Still missing, P3.**

* A server twin of the wisp: `ThaumcraftFX.wisp(ServerLevel, x, y, z, size, type, shrink, noClip, gravity)` (and the wisp
  that homes on a point). Used by the Gaseous Tenebrae that is placed or dissolved on the server (`placeParticle`, now a
  vanilla smoke puff) and by the Fire potion (30 wisps of type 4 around the victim every fifth tick, now one packet of
  30 vanilla flames).
* A client `blockSparkle(Level, BlockPos, int rgb, int count)` (the PacketFXBlockSparkle look: sparkles of size 1.75 and
  gravity 0.1 at random places in the block, in any rgb). The group draws plain sparkles of the nearest type (blue) at
  random places in the block, because the nearest sparkle types do not include the dark blue of `'?'`/`255`.

**Until then.** The look is Thaumcraft's, apart from those two; the counts and places are the original's.

## 2. Codecs of aspects (A4, P2)

**Need.** The infused seeds carry an aspect and a list of tendencies. A data component wants a `Codec` and a `StreamCodec`
for them; neither `Aspect` nor `AspectList` has one.

**Proposed signature.** `Aspect.CODEC` / `Aspect.STREAM_CODEC` (by tag, resolved against the live registry so add-on aspects
work) and `AspectList.CODEC` / `AspectList.STREAM_CODEC` (a list of tag and amount, order kept).

**Stub in the add-on.** `InfusedSeedComponents`: the aspect is a `String` component (the tag), the tendencies a
`Map<String, Integer>` component, converted by `toMap` / `toList` (order and zero amounts kept; tags of unknown aspects
are dropped on the way in, which is also what `AspectList.readFromNBT` does). With the codecs the two components would be an
`Aspect` and an `AspectList` and the conversion and the second registration of the types would go.

**Until then.** The stack data is correct; only the type of the components is not the natural one.

## 3. Flammability of other mods' blocks (loader modules, P3)

**Need.** The elemental fires ask how flammable a neighbouring block is. The vanilla fire of 1.7.10 answered
`Block.getFlammability` for the blocks of every mod; 1.21.1 has `FlammableBlockRegistry` on Fabric and
`BlockState.getFlammability` on NeoForge, and nothing in common code.

**Proposed signature.** A small platform hook in the loader modules, in the style of the other platform integrations:
`interface PlatformFire { int flammability(BlockState state, BlockGetter level, BlockPos pos); int spreadSpeed(BlockState state,
BlockGetter level, BlockPos pos); }` implemented once per loader.

**Stub in the add-on.** `FireRules.vanillaFlammability(BlockState)`: the 1.7.10 vanilla table (planks 20, slabs, stairs and
fences 20, logs 5, leaves 60, wool 60, plants 100, TNT 100, bookshelf 20, coal block 5, hay 20, carpet 20), by block tags. A
modded wooden block that is in no vanilla tag does not burn in an imbued fire.

**Until then.** The imbued fires burn the vanilla blocks as 1.7.10 did and ignore the flammability that other mods registered.

## 4. Magic hoes (P3)

**Need.** The Hoe of Growth of Thaumcraft (`elemental_hoe`) and, in 1.7.10, the electric hoe of Electro-Magic Tools grow an
infused crop when it is clicked with them while the configuration forbids bone meal (`BonemealEventHandler`).

**Proposed signature.** An item tag `thaumcraft:growth_hoes` (or `IGrowthTool` on the item) that add-ons can extend instead
of the add-on looking the hoe up by id.

**Stub in the add-on.** `BonemealEventHandler.isMagicHoe` compares the stack with `ItemsTC.get("elemental_hoe")`.

## 5. Identifiers the add-on needs by name (A7, P3)

* The sound `thaumcraft:wand` (the Fume Dissipator, the hoe handler): done, `ThaumcraftSoundIds.WAND` (skipped if it is
  not registered).
* The Kami leggings `ichorcloth_legs_gem` (the strong Nitor Gas) are the add-on's own item.
* The warding fence is read through `BlocksTC.state("blockAiry", 4)` (the grid of `legacy_ids.json`); a registry key
  constant would harden it.

## 6. The warding fence flickers (no request; recorded)

`blockAiry:4` destroys itself unless a warding stone is one or two blocks below (`docs/API_GAPS.md` 3b). The Ward Slab
re-places it every 100 ticks exactly as 1.7.10 did, so the aura flickers as before. If this is unacceptable, an API that
creates a fence which does not look for a stone (or a documented way to mark the slab as a warding stone) would fix it; the
add-on does not need anything else.

## 7. Shared files: what the group changed or asks the lead to change

* `LibBlockNames.INFUSED_GRAIN_BLOCK` (`INFUSED_GRAIN_BASE + "Block"`, that is `infused_grainBlock`) is no valid registry
  path in 1.21.1 (no capitals). The registrar uses its own constant `SimpleBlocks.INFUSED_GRAIN_BLOCK` =
  `infused_grain_block`. The lead should change the constant to the same string (nothing else reads it) or delete it.
* New resource files outside the group's own folders: `data/minecraft/tags/block/fire.json`, `block/slabs.json`,
  `block/stairs.json`, `item/slabs.json`, `item/stairs.json` (vanilla tags that list the fires, slabs and stairs of the
  group). The mineable tags `thaumictinkerer:mineable/simple_blocks/{pickaxe,axe,shovel}` are filled; the vanilla ones that
  point to them were already there.
* `assets/thaumictinkerer/textures/mob_effect/potion_{air,fire,earth,water}.png` are plain copies of the item textures
  `potion_aer/ignis/terra/aqua.png`. `scripts/porting/tt_assets.py` does not produce them (the 1.7.10 potions had no icons of
  their own: `setIconIndex(0, 0)` pointed at the first icon of the vanilla effect sheet). The lead may add them to the
  converter for provenance.
* The language fragments `lang-extra/simple_blocks.{en_us,ru_ru}.json` (effect names, and Russian names of the six blocks the
  original Russian file never named: Ward Slab, Gaseous Illuminae, Gaseous Tenebrae, Nitor Gas, Infused Farmland, Imbued
  Grain). `python3 -I scripts/porting/tt_assets.py` and `--check` were run.
* The camouflage base of the platform (`BlockCamo`, `TileCamo`) and the base of the TileBlocks devices (`CamoDeviceBlock`)
  are two designs for one 1.7.10 class; they could be merged (`docs/deviations/SimpleBlocks.md` 2.4).
* Other groups can use: `ItemBrightNitor.setBlock(level, pos, strength)` / `placeNear(level, entity, strength)` (the Kami
  leggings), `NumericAspectHelper` (the soul aspects), `BlockGas.isAirLike(state)` (1.7.10 `isAirBlock` for the gases),
  `AspectCropLootManager.getAllLoot()` (recipe viewers).
* The GameTest file `fabric/src/gameTest/.../SimpleBlocksGameTests.java` was filled with one wrapper per check of
  `SimpleBlocksRuntimeChecks`. The two checks that count the victims of the potion handler have batches of their own.
