# Eldritch and Outer Lands client blocks — wave 2, agent `w2clientblocks` (2026-10-23)

**Scope.** The 37 units of `units/w2clientblocks.txt` — the eldritch and cultist renderers, the
eldritch models, the eldritch block models and block states, the eldritch tiles and the ten
Outer Lands units of the world package.

**Result.** 35 units moved `partial -> parity_verified`, 2 stay `partial` with a precise
blocker. Twelve defects were found and fixed (six in the first part of this session, six in
the second one, the second part closes the two open items of the cultist portal and of the
crystal). The client was **not** run (no Gradle in this wave): the acceptance criteria for
the frames are in §7.

---

## 1. The units

| canonical path | before | after |
|---|---|---|
| `thaumcraft/client/renderers/block/BlockEldritchRenderer.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/block/EldritchCrustBakedModel.java` | partial | partial (§6.1) |
| `thaumcraft/client/renderers/entity/RenderCultist.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/entity/RenderCultistPortal.java` | partial | **parity_verified** (§3.7, §3.8) |
| `thaumcraft/client/renderers/entity/RenderEldritchCrab.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/entity/RenderEldritchGolem.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/entity/RenderEldritchGuardian.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/entity/RenderEldritchOrb.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/entity/RenderEldritchWarden.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/models/ModelEldritchCap.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/models/ModelEldritchCrystal.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/models/entities/ModelEldritchCrab.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/models/entities/ModelEldritchGolem.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/models/entities/ModelEldritchGuardian.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/tile/TileEldritchCapRenderer.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java` | partial | **parity_verified** (§3.9, §3.10) |
| `thaumcraft/client/renderers/tile/TileEldritchLockRenderer.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/tile/TileEldritchObeliskRenderer.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/tile/TileEldritchPortalRenderer.java` | partial | **parity_verified** |
| `thaumcraft/common/entities/monster/boss/EntityCultistPortalGreater.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/WorldGenEldritchRing.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/biomes/BiomeEldritch.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/biomes/BiomeGenEldritch.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/ChunkProviderOuter.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/GenBossRoom.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/GenCommon.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/GenKeyRoom.java` | partial | partial (§6.4) |
| `thaumcraft/common/lib/world/dim/GenLibraryRoom.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/GenNestRoom.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/GenPassage.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/GenPortal.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/MazeGenerator.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/MazeHandler.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/WorldProviderOuter.java` | partial | **parity_verified** |
| `thaumcraft/common/tiles/TileEldritchObelisk.java` | partial | **parity_verified** |
| `thaumcraft/common/tiles/TileEldritchPortal.java` | partial | **parity_verified** |

---

## 2. The cosmetic / eldritch block family (the question of the wave)

The wave one agent `blockvis` had already fixed the **render shape** and the **block entity
contract** of the family: every class that inherits `BaseEntityBlock` now answers
`getRenderShape()` explicitly, and the block entity of a state is decided per state through
`PerStateBlockEntity`. I re-checked the whole family and found **no remaining gap**:

* every class of `common/src/main/java/thaumcraft/common/blocks/**` that inherits
  `BaseEntityBlock` or implements `EntityBlock` answers `getRenderShape()` (a sweep of the
  tree finds no other class without it);
* the piston rule is the one of the oracle: `hasTileEntity(int metadata)` of
  `BlockCosmeticSolid:278-286` (the warding stone and the node), of `BlockEldritch:151-153`
  (the altar, the obelisk, the capstone, the lock, the crab spawner and the runed trap) and
  of `BlockEldritchNothing:61-63` (the exposed filler) are reproduced by
  `EldritchBlockRules.cosmeticHasTile`, `eldritchHasTile` and the per state contract.

What the family was still missing was the **geometry** of the stones: see §3.1.

---

## 3. The defects

### 3.1 The eldritch altar, the obelisk and the capstone were drawn as cubes

* **Original.** `thaumcraft/client/renderers/block/BlockEldritchRenderer.java:35-81` draws the
  metadata **four, five, six, seven, eight, nine and ten** and nothing else. The metadata
  zero, one, two and three — the altar, the obelisk, the obelisk middle and the capstone —
  are not drawn by the block renderer at all: a block with a custom render id is drawn by
  that renderer alone in 1.7.10 (the vanilla `RenderBlocks.renderBlockByRenderType` answers
  `false` for every render type it does not know). They are drawn by their own block entity
  renderers: `TileEldritchCapRenderer` (the altar and the capstone) and
  `TileEldritchObeliskRenderer` (the obelisk).
  The proof inside the game: the shaft of the obelisk
  (`TileEldritchObeliskRenderer.java:87-95`) is one block wide and covers exactly the
  metadata two blocks, so a cube of the obsidian tile there would hide the
  `obelisk_side.png` sheet of the obelisk.
* **Port.** `common/src/main/resources/assets/thaumcraft/models/block/eldritch.json`,
  `eldritch_1.json`, `eldritch_2.json` and `eldritch_3.json` were `minecraft:block/cube_all`
  with the `obsidiantile` sheet, so the port drew a full cube of the obsidian tile for the
  altar, the obelisk, the obelisk middle and the capstone.
* **Fix.** The four models have no element any more; the block entity renderers of the
  family draw the stones, like in 1.7.10. The **item** keeps the icon of the original
  (`BlockEldritch:79` and `:93`): `models/item/eldritch.json` is a self contained cube of the
  `obsidiantile` sheet.
* **Evidence.** `common/src/test/java/thaumcraft/eldritch/EldritchBlockAssetParityTest.java`
  (`theStonesThatTheOriginalNeverDrewHaveNoModel`, `theEldritchItemKeepsTheOriginalIcon`).

### 3.2 The crab and the golem swung with a sine instead of a cosine

* **Original.** `ModelEldritchCrab.java:163-164` and `:181-182` and
  `ModelEldritchGolem.java:259-260`, `:312-313` and `:323-324` use
  `MathHelper.func_76134_b`, that is **cos** (MCP: `func_76134_b = cos`), and only the claw
  idles of the crab (`ModelEldritchCrab.java:183-186`) use the sine (`func_76126_a`).
* **Port.** `ModelEldritchCrab.java` and `ModelEldritchGolem.java` used `Mth.sin` in all ten
  places, so the legs and the tail of the crab and the arms, the legs and the cloth of the
  golem swung with a phase of a quarter turn too early.
* **Fix.** The ten calls are `Mth.cos` again. The values are now reachable from a test:
  `ModelEldritchCrab.legSwing`, `legSwingHalfPhase`, `tailSway`, `tailSwayTilt` and
  `ModelEldritchGolem.armSwing`, `armSwingHalfPhase`, `legSwing`, `legSwingHalfPhase`,
  `clothSwing`, `clothSwingHalfPhase`.
* **Evidence.** `EldritchClientModelParityTest.theCrabSwingsItsLegsWithTheOriginalCosine`
  and `theGolemSwingsWithTheOriginalCosine` compare the helpers with `Math.cos` over five
  phases and check the rest angle of the phase zero.

### 3.3 Six boxes of the golem and one of the guardian were mirrored that the original never mirrored

* **Original.** The 1.7.10 `ModelRenderer.mirror` (MCP `field_78809_i`) is read **only** by
  the `ModelBox` constructor, that is while `addBox` runs
  (`net/minecraft/client/model/ModelBox.java`: the flag swaps the two x corners of the box
  and `flipFace()` restores the winding). A flag that is set **after** `addBox` therefore has
  no effect at all. In the oracle the flag of the torso, of the three left waist pieces, of
  the left back panel and of the left leg of the golem is set after the box
  (`ModelEldritchGolem.java:230-253`), and the flag of the left shoulder plate of the
  guardian is set **before** its box (`ModelEldritchGuardian.java:220-222`) and is real.
* **Port.** The golem mirrored all six dead flags and the guardian missed the one real flag.
* **Fix.** The six `mirror()` calls of the golem are gone, the guardian mirrors its left
  shoulder plate again.
* **Evidence.** `EldritchClientModelParityTest.theGuardianMirrorsOnlyTheLeftShoulderPlate`
  bakes the guardian and compares the texture coordinates of the two shoulder plates: the
  mirrored plate keeps the same coordinates but in the opposite order.

### 3.4 The warden never drew its hood eye

* **Original.** `ModelEldritchGuardian.java:467-479`: the warden alone draws the hood eye a
  hundredth larger than the hood and with the pulsing light of the original.
* **Port.** `RenderEldritchWarden` hid the part and never drew it again.
* **Fix.** the warden draws the eye again, with the full bright light of the 1.21.1 lightmap.

> **A claim of an earlier draft of this report was wrong and is not a defect of the
> port.** The offset of the original renderers of the golem and of the guardian
> (`RenderEldritchGolem.java:57` and `RenderEldritchGuardian.java:68`) is
> `par4 - entity.field_70129_M`, and `field_70129_M` is **`yOffset`** (MCP
> `fields.csv`: `field_70129_m,yOffset`), which is zero for a mob — **not** the height
> (`field_70131_O` is the height). The three mobs are therefore drawn at the position of
> the entity, and the port already did that. The warden alone sinks, by its height times
> the share of its spawn timer (`RenderEldritchGuardian.java:69-71`), which the port
> reproduces.
* **Evidence.** the sizes of the entities are the original ones
  (`EntityEldritchGuardian:111` `setSize(0.8, 2.25)`, `EntityEldritchWarden:110`
  `setSize(1.5, 3.5)`, `EntityEldritchGolem:92` `setSize(1.75, 3.5)`) and the port
  registers the same sizes.

### 3.5 The link line of the ritualist cleric was a coloured line instead of a ribbon

* **Original.** `RenderCultist.java:83-163` draws the link as a scrolling ribbon of
  `textures/misc/wispy.png`, twice: once widened in y and once widened in x. The wave uses
  the world lattice, the scroll speed is `-0.03`, the half width `0.25`, the colour
  `0x110011` with the alpha `0.8`, the length is the rounded distance times
  `Config.golemLinkQuality / 2` (the default of `Config.java:881` is 16) and the number of
  segments is the length times the share of the ten ticks of the ritual.
  The random phase of the float of the cleric is seeded from the **entity id**
  (`RenderCultist.java:69`, MCP `func_145782_y = getEntityId`) and the line starts at the
  **eye height** of the cleric (`RenderCultist.java:77`).
* **Port.** `RenderCultist` drew a plain `RenderType.lineStrip` without the sheet, seeded
  the phase from the uuid of the entity and started the line at the bounding box height.
* **Fix.** the two ribbons are drawn again with the original wave, speed, width, colour and
  segment count, the phase is seeded from the entity id and the line starts at the eye
  height.

### 3.6 The obelisk and the gate were culled by the frustum

* **Original.** `TileEldritchObelisk.java:40-43`: the render bounding box of the obelisk is
  five blocks high. `TileEldritchPortal.java:34-37`: the gate is drawn in a cube of three
  blocks around itself.
* **Port.** neither of the two renderers asked for the off screen drawing, so the tall
  obelisk and the wide gate were cut off when the block itself left the view. Minecraft
  1.21.1 has no render bounding box for a block entity any more
  (`net/minecraft/world/level/block/entity/BlockEntity` has no `getRenderBoundingBox`); the
  1.21.1 way to say the same is `BlockEntityRenderer.shouldRenderOffScreen`.
  The original also draws the two tiles up to ninety six blocks away
  (`TileEldritchObelisk.java:37-39` and `TileEldritchPortal.java:31-33`,
  `getMaxRenderDistanceSquared` of 9216), while the 1.21.1 default of
  `BlockEntityRenderer.getViewDistance` is sixty four.
* **Fix.** `TileEldritchObeliskRenderer.shouldRenderOffScreen` and
  `TileEldritchPortalRenderer.shouldRenderOffScreen` answer `true` and their
  `getViewDistance` answers 96.

---

### 3.7 The eldritch void drew its tunnel on the wrong faces

* **Original.** `TileEldritchNothingRenderer.java:39-59` draws the tunnel on the face whose
  neighbour is **not an opaque cube**:
  `if (!te.getWorld().getBlock(x, y + 1, z).isOpaqueCube()) { this.drawPlaneYNeg(x, y, z, f); }`
  The tunnel is therefore visible on the border between the filler and the **air** of the
  room, and the filler itself stays invisible where it touches stone. The original
  `BlockEldritchNothing.java:52-60` is an opaque cube, so two fillers do not show the
  tunnel between them.
* **Port.** `TileEldritchNothingRenderer.java:36-47` skipped the faces whose neighbour is
  **air** and drew the tunnel on the faces that touch the stone: the exact opposite.
* **Fix.** `TileEldritchNothingRenderer.java:44-58` asks the neighbour for its opaque cube
  and `TileEldritchNothingRenderer.java:70-88` reproduces the 1.7.10 `isOpaqueCube` for the
  three blocks of the family that do not agree with the 1.21.1 `isSolidRender`: the
  eldritch filler is opaque, the cosmetic solid is opaque, the eldritch stone is not
  (`BlockEldritch.java:171-173`).

### 3.8 The two portal quads did not use the camera basis of the original

* **Original.** the basis is the one of the 1.7.10 `ActiveRenderInfo.updateRenderInfo`:
  the left vector is `(cos(yaw), 0, sin(yaw))` and the up vector is
  `(-sin(yaw) sin(pitch), cos(pitch), cos(yaw) sin(pitch))`; both are built from the look
  angles of the **player** and not from the camera, and their horizontal part is negated in
  the third person view (the `1 - 2i` of the original, `i` is one in the third person).
  `RenderCultistPortal.java:62-70` and `:92-108` and `TileEldritchPortalRenderer.java:44-52`
  and `:74-81` build the four corners of the quad from that basis and multiply the
  horizontal part of a corner with the scale and the vertical part with the scaley.
* **Port.** both renderers used the camera orientation, so the quad of the original was
  mirrored in the third person view and it always faced the camera. The four texture
  coordinates of the cultist portal were twisted as well: the two opposite corners carried
  the same coordinate, so the sheet of the sixteen frames was mirrored along the diagonal.
  The gate was drawn at **half** the size of the original (the port used half of the scale
  and of the scaley as the half extents) and at **twice the position** of its block (the
  1.21.1 block entity renderer is placed on the corner of the block,
  `LevelRenderer:1232-1275`, and the port added the absolute centre of the block on top of
  it).
* **Fix.** the new `common/src/main/java/thaumcraft/client/renderers/CameraBasis.java` with
  the pure helpers `leftFor(yaw, thirdPerson)` and `upFor(yaw, pitch, thirdPerson)`;
  `RenderCultistPortal.java:74-92` and `:99-111` and
  `TileEldritchPortalRenderer.java:78-92` and `:130-142` use them. A fully opened gate is
  two blocks wide and two blocks high again, and the frame of the sheet of the gate is
  turned by ninety degrees like the original.
* **Evidence.** `EldritchPortalParityTest` checks the basis, its mirror, the half extents of
  the gate and the four texture coordinates of the two sheets.

### 3.9 The crystal of the Outer Lands was drawn upside down

* **Original.** `TileEldritchCrystalRenderer.java:31` loads `vcrystal.obj` with
  `AdvancedModelLoader`. The file and `obelisk_cap.obj` come from the same exporter
  ("3ds Max Wavefront OBJ Exporter v0.97b") and the original loads both with the same
  loader, so both use the same convention: the `v` of the wavefront file runs from the
  **bottom** of the image and the `v` of the model is its complement.
* **Port.** `ModelEldritchCrystal.java:557` used the `v` of the file unchanged, while
  `ModelEldritchCap.java:144` (the cap of the obelisk) already flipped it. One of the two
  was wrong: the cap was right, so the crystal was drawn upside down.

### 3.10 The crystal followed the light of its block

* **Original.** `TileEldritchCrystalRenderer.java:45-49` overwrites the coordinates of the
  light map with `(OpenGlHelper.field_77476_b, (int) (210 * shade))`, that is with
  `(5898, 178)` up to `(5898, 210)`. The light map clamps the coordinates it is given to the
  range of the texture, so the crystal of the original is **always** drawn with the
  brightest texel of the light map and the pulse of the original never reaches the screen.
* **Port.** the crystal was drawn with the light of its block, so it was black in an unlit
  room. The earlier draft of this report called the pulse an unfixable blocker ("the light
  map of 1.21.1 holds sixteen steps"); the clamp of the light map is the real reason and it
  makes the pulse irrelevant.
* **Fix.** `TileEldritchCrystalRenderer.java:67-78` draws the crystal with
  `LightTexture.FULL_BRIGHT`.

## 4. What was verified and found correct

* the icon of every metadata of the eldritch stone (`BlockEldritch:92-94`): `es_i_1`,
  `es_i_2`, `deco_1`, `deco_3`, `deco_2`, the crust of the cosmetic solid and `es_5` — the
  port uses the same sheets;
* the inset cube of the crust: `BlockEldritchRenderer:22-32` and `:46-63` draw it from the
  second to the fourteenth sixteenth, the port uses the same box
  (`models/block/eldritch_crust.json:1-30`);
* the twenty-one parts of the crab, the thirty-six of the golem and the fifty-four of the
  guardian: texture offset, box, rotation point and rest rotation are identical
  (compared part by part against the oracle);
* the wavefront models: the tables of `ModelEldritchCap` and `ModelEldritchCrystal` are the
  tables of `obelisk_cap.obj` and `vcrystal.obj` of the original jar, vertex for vertex;
* `RenderEldritchCrab`: the second pass over `craboverlay.png` with the alpha blending;
* `RenderEldritchOrb`: the twelve seeded fans, the growth of the age and the colour
  `0xEECCFF` of `BlockCustomOreItem:17`;
* `RenderEldritchGuardian`: the fade with the distance to the player and the difficulty;
* `TileEldritchCapRenderer`, `TileEldritchLockRenderer`, `TileEldritchNothingRenderer`,
  `TileEldritchObeliskRenderer`, `TileEldritchPortalRenderer`: the geometry, the sheets, the
  rotations, the counters and the colours of the original;
* `TileEldritchObelisk`: the range of six blocks, the damage boost and the regeneration of
  forty ticks and the guard on a mob that still carries a regeneration;
* `TileEldritchPortal`: the scan every five ticks, the refusals, the cooldown of a hundred
  ticks, the research `ENTEROUTER` with the warp of `ResearchManager:510-528` and the gate
  search;
* the ten Outer Lands units: the difference that the ledger still named was **already
  fixed** by the wave one agent of the worldgen in the commit `14116360` ("Fix the Outer
  Lands labyrinth generator and the eldritch rooms against the oracle"), the notes of the
  ledger were simply stale. The labyrinth generator has the oracle condition
  (`MazeGenerator.java:334-336`) and `MazeFidelityTest` compares the two generators over
  159821 cells; the slab of the library room uses the half property
  (`ThaumcraftOuterLands.java:270-275`); the nest room rolls its own rarity
  (`DungeonRooms.java:920-929`); the passage draws the trapped stone twice
  (`DungeonRooms.java:425-429`); the portal room is cleared in the four corners
  (`DungeonRooms.java:700-712`); the lock of the boss room takes the direction of its side
  (`DungeonRooms.java:1082-1088`); the ring carries the aura node and draws the labyrinth
  seed after it (`EldritchRingFeature.java:78-88`).

---

## 5. The files

**Changed**

* `common/src/main/java/thaumcraft/client/renderers/entity/RenderCultist.java`
* `common/src/main/java/thaumcraft/client/renderers/entity/RenderEldritchGolem.java`
* `common/src/main/java/thaumcraft/client/renderers/entity/RenderEldritchGuardian.java`
* `common/src/main/java/thaumcraft/client/renderers/entity/RenderEldritchWarden.java`
* `common/src/main/java/thaumcraft/client/renderers/models/ModelEldritchGuardian.java`
* `common/src/main/java/thaumcraft/client/renderers/models/entities/ModelEldritchCrab.java`
* `common/src/main/java/thaumcraft/client/renderers/models/entities/ModelEldritchGolem.java`
* `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchObeliskRenderer.java`
* `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchPortalRenderer.java`
* `common/src/main/java/thaumcraft/client/renderers/entity/RenderCultistPortal.java`
* `common/src/main/resources/assets/thaumcraft/models/block/eldritch.json`,
  `eldritch_1.json`, `eldritch_2.json`, `eldritch_3.json`
* `common/src/main/resources/assets/thaumcraft/models/item/eldritch.json`

* `common/src/main/java/thaumcraft/client/renderers/CameraBasis.java` (the basis of the
  1.7.10 `ActiveRenderInfo`)
* `common/src/main/java/thaumcraft/client/renderers/models/ModelEldritchCrystal.java`
* `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java`
* `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java`
* `common/src/test/java/thaumcraft/eldritch/EldritchPortalParityTest.java`

**Not touched** (the rules of the wave): `common/src/main/java/thaumcraft/common/lib/world/**`
and `common/src/main/java/thaumcraft/common/lib/{utils,network,enchantment}/**`. The four
units that are still open are described in §6 with the exact change that is needed.

---

## 6. What is still open

### 6.1 `EldritchCrustBakedModel` — the crust cannot merge with a solid neighbour

The original insets the crust by two sixteenths **on every side without an eldritch
neighbour** (`BlockEldritchRenderer:46-63`), so two crust blocks that touch form one box.
A 1.21.1 baked model has no access to the world, so the model of the port is the isolated
cube (2..14) and the helper `EldritchCrustBakedModel` computes the merged box and is
unit tested. **Blocker: a static model cannot express the neighbour dependent bounds.**
The leader can close it with a custom `BakedModel` that is re-baked per chunk, or accept the
isolated cube.

### 6.2 `RenderCultistPortal` — **closed** (§3.8)

The basis of the 1.7.10 `ActiveRenderInfo` is reproducible in 1.21.1: it is built from the
look angles of the player, and the third person view is read from the type of the camera of
the options. The quad of the original is now drawn with the four corners and the four
texture coordinates of the oracle.

### 6.3 `TileEldritchCrystalRenderer` — **closed** (§3.9, §3.10)

The wavefront file of the crystal is loaded with the same convention as the cap of the
obelisk, and the crystal is drawn with the full light like the original, whose light map
coordinate is clamped to the brightest texel.

### 6.4 `GenKeyRoom` — the guardian of the key room has no AI home

The original anchors the guardian at the centre of the room with a radius of sixteen
(`GenKeyRoom.java:110`, `setHomePos(x + 8, y + 2, z + 8, 16)`). The native spawn record of
the world package (`DungeonRooms.Spawn`) carries no home and
`ThaumcraftOuterLands.spawnRoomEntities` only creates the entity, so the guardian of the
port wanders. **The exact change:** add a home field to `DungeonRooms.Spawn` (the world
package, which this wave may not touch), fill it with `x + 8, y + 2, z + 8` and a radius of
sixteen in `DungeonRooms.generateKeyRoom` (`:826-832`) and call
`restrictTo(home, 16)` on the guardian in `ThaumcraftOuterLands.spawnRoomEntities`
(`:349-360`) — the guardian already has the restriction of the original
(`EntityEldritchGuardian.java:141` and `:145`).

---

## 7. The acceptance criteria for the client

The leader has to check these on a frame of the real client; none of them could be checked
in this wave (no Gradle, no client).

1. **The eldritch altar, the obelisk and the capstone are not cubes.** Place the
   `thaumcraft:eldritch` block (the variant 0, the altar) and the variant 1 (the obelisk):
   no block of the `obsidiantile` texture may appear. Only the small cap model of the
   original may be visible on the altar and on the capstone, and only the thin one block
   wide obelisk shaft with the `obelisk_side.png` sheet may stand on the obelisk base.
2. **The eldritch item keeps its icon.** The item `thaumcraft:eldritch` in the inventory
   still shows the cube of the `obsidiantile` sheet.
3. **The glowing crust is an inset cube.** The `thaumcraft:eldritch_crust` block shows the
   `es_i_1` sheet on a cube that is inset by an eighth of a block on every side, not a full
   cube.
4. **The crab walks with the original gait.** The legs of the eldritch crab have to swing
   from their rest angle when the phase is zero (the legs are at their rest angle when the
   crab starts to walk), and the tail has to sway with the same phase as the legs.
5. **The golem walks with the original gait.** The arms of the eldritch golem start at their
   rest angle (the arm raised forward) when the phase is zero, and the cloth of the golem
   swings with the same phase as the legs.
6. **The three eldritch mobs stand on the floor.** The eldritch golem, the eldritch guardian
   and the eldritch warden have to stand on the ground with their feet, not float above it
   and not sink into it. The warden has to rise out of the floor while it spawns and sink
   back.
7. **The warden shows its glowing eye.** The hood of the warden shows a bright eye that the
   guardian does not have.
8. **The eldritch void is only on the border with the air.** In the Outer Lands, look at a
   wall of the eldritch filler from inside a room: the tunnel effect must swirl on the
   border between the wall and the air of the room and must **not** appear between the
   filler and the stone of the floor. (Before the fix of §3.7 it was the other way round.)
9. **The eldritch gate is in its own block and it is two blocks wide.** Walk into a gate of
   the Outer Lands: the magenta sheet must be in the middle of the block of the gate (it
   was drawn at twice the position of the block) and it must open to two blocks wide and
   two blocks high (it was one by one). The animation of the sheet runs **vertically**,
   because the frame of the original is turned by ninety degrees.
10. **The sheet of the cultist portal is upright.** The boss portal of the Inner Sanctum
    must show the sixteen frames of the sheet upright and not twisted along a diagonal
    (§3.8), and it must be mirrored in the third person view like the original.
11. **The eldritch crystal stands upright and glows.** The crystal of the Outer Lands must
    stand with its tip at the top (§3.9) and it must keep its own light in an unlit room
    (§3.10).

12. **The link of the ritualist cleric is a ribbon.** A ritualist cleric that channels shows
    a dark purple ribbon of the `wispy.png` sheet between its eyes and the ritual target,
    and the ribbon keeps a constant width on the screen while the cleric and the target
    move.

---

## 7.1 The tests

| file | what it locks |
|---|---|
| `common/src/test/java/thaumcraft/eldritch/EldritchPortalParityTest.java` | **new in the second part** — the basis of `ActiveRenderInfo` and its mirror of the third person view, the half extents of the eldritch gate, the four texture coordinates of the two sheets, the light of the crystal |
| `common/src/test/java/thaumcraft/eldritch/EldritchClientModelParityTest.java` | the parts, the rotation points and the cosine of the crab, of the golem and of the guardian |
| `common/src/test/java/thaumcraft/eldritch/EldritchBlockAssetParityTest.java` | the models and the block states of the eldritch stones |

**All of them are authored but NOT executed**: this wave has no Gradle, the leader runs every
build and every test suite sequentially with the shared Loom cache.

## 8. The checks that were run

| check | result |
|---|---|
| `python3 scripts/porting/convert_assets.py --verify` | `missing_references: 0` (3877 texture references, 2904 model references) |
| `python3 scripts/porting/update_scope_ledger.py --check --original-root /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` | `stale_review_fingerprints: 0` |
| `javac` of the whole `common` tree (1350 sources) and of the three tests against the real `minecraft-merged-1.21.1` of the Loom cache | **no error in a changed file and in the three tests**; the only errors of the run are the known ones of the plain jar (the 65 `BlockEntitySupplier` accesses, the four `Projectile` constructors, the four `lastHurtByPlayerTime` and the one `pickupDelay`, that is the access widener `thaumcraft.accesswidener` and nothing else), all of them in files this wave did not touch |
| the JUnit suites of this wave | **authored, NOT executed** — the leader has to run them with Gradle |
| the client | **not run** — the acceptance criteria of §7 are open |
