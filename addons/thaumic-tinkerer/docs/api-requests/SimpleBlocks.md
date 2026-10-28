# SimpleBlocks: requests to the Thaumcraft API and to the loader modules

Group SimpleBlocks: Smokey Quartz, the paving slabs, the Ethereal Platform, the gases and their items, the Forcefield, the
infused crops and potions, the elemental fires. The numbers `A4`, `A5`, `A7`, `A8` are those of `docs/API_GAPS.md` section 2.
Nothing below was done by reaching past `thaumcraft.api` into the mod's internals: every item is a named stub or a workaround with the loss
written down. Priorities are the group's own opinion (P1 changes what the player sees, P3 only hardens something that works
today).

## 1. Particles of the blocks (A8, P3)

**Need.** 1.7.10 drew its effects through `Thaumcraft.proxy.sparkle`, `wispFX2`, `blockRunes`, `burst`. They are client-only
particles of Thaumcraft with no public facade.

**Used by.** The sparkle of the Gaseous Illuminae, the Nitor Gas and the Forcefield (display ticks, the
`placeParticle` of the Fume Dissipator and of a spreading), the wisp of the Gaseous Tenebrae, the runes of the Ward Slab
(`blockRunes`: three colours), the sparkles of the Travel and Ward Slabs when walked on, the bursts of the Air and Fire
potions, the trail between crops (this one exists: `ThaumcraftFX.essentiaTrail`) and the Earth wall
(`ThaumcraftFX.blockSparkle`, exists).

**Proposed signature.** As in `API_GAPS.md` A8: a client-only `thaumcraft.api.client.ThaumcraftClientFX` with `sparkle(Level,
x, y, z, float size, int colour, int ticks, float gravity)`, `wisp(Level, x, y, z, ..., int type)`, `blockRunes(Level, x, y, z,
float r, float g, float b, int lifetime, float gravity)` and `burst(Level, x, y, z, float size)`, plus the server-side senders
in `ThaumcraftFX` for the effects that 1.7.10 triggered on the server.

**Stub in the add-on.** `SimpleBlockFx`: `END_ROD` for the sparkle and `SMOKE` for the shadow wisp; the Ward Slab draws dust
particles for the runes, the potion handler `POOF` and `FLAME`; sent by the server (`ServerLevel.sendParticles`) where
1.7.10 ran on the server or on both sides, and added with `Level.addParticle` in the display ticks. When the facade exists
`SimpleBlockFx` and these three call sites are the places to change.

**Until then.** The look is vanilla; the counts and places are the original's.

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

* The sound `thaumcraft:wand` (the Fume Dissipator, the hoe handler): looked up by id; skipped if it is not registered.
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
