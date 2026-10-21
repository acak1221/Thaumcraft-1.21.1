# Agent `w2clientblocks` — the eldritch client renderers of TC4 4.2.3.5 → 1.21.1

**Date:** 2026-10-23 · **Branch:** `agent/w2clientblocks` · **Worktree:** `/home/cyberfox-kali/wt/w2clientblocks`
**Oracle (authority):** `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, CFR 0.152)
**Second opinion only:** `/home/cyberfox-kali/OpenTC4`
**Unit list:** `/home/cyberfox-kali/units/w2clientblocks.txt` (37 units of the `eldritch_and_outer_lands` family)

---

## 1. Result

The unit list was generated before the wave-2 merges. After merging `work` into this
branch (which brought the `worldgen`, `crashhunt`, `w2api`, `w2blocks`, `w2devices`,
`w2golems` and `w2clientfx` branches) **33 of the 37 units were already
`parity_verified`** and only four were still open:

| Unit | Before | After |
|---|---|---|
| `thaumcraft/client/renderers/entity/RenderCultistPortal.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/block/EldritchCrustBakedModel.java` | partial | partial (exact blocker, §6) |
| `thaumcraft/common/lib/world/dim/GenKeyRoom.java` | partial | partial (exact blocker, §6) |

Five units that were already `parity_verified` carried **real defects** that the
wave-1 code review had missed; they are fixed and their evidence is extended:
`BlockEldritchRenderer`/`EldritchCrustBakedModel` (the neighbour dependent box,
still open), `RenderCultistPortal`, `TileEldritchNothingRenderer`,
`TileEldritchPortalRenderer`, `ModelEldritchCrystal`/`TileEldritchCrystalRenderer`.

**Defects found: 9 · fixed: 7 · documented as open: 2.**
**Files changed: 9** (7 in `common/src/main`, 1 test, 1 ledger).
**Tests authored: 3 files, 12 test methods — authored, NOT executed** (no Gradle in this
session, see §7).

---

## 2. Defects found and fixed

### D1. The eldritch void drew its tunnel on the wrong faces — **fixed**

* **Original** `thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java:39-59`:
  ```java
  if (!te.getWorld().getBlock(x, y + 1, z).isOpaqueCube()) { this.drawPlaneYNeg(x, y, z, f); }
  ```
  The tunnel is drawn on the face whose neighbour is **not an opaque cube**, i.e. on the
  border between the filler and the **air** of the room. The original
  `BlockEldritchNothing.java:52-60` is an opaque cube itself, so two fillers do not
  show the tunnel between them.
* **Port (before)** `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java:36-47`:
  ```java
  if (level.getBlockState(neighbour).isAir()) continue;   // skip the air
  ```
  The exact opposite: the tunnel was drawn between the filler and the **stone** and
  never on the border with the air.
* **Fix** `TileEldritchNothingRenderer.java:44-58` asks the neighbour for its opaque
  cube; `TileEldritchNothingRenderer.java:70-88` reproduces the 1.7.10 `isOpaqueCube`
  for the three blocks of the family that do not agree with the 1.21.1
  `isSolidRender`: the eldritch filler is opaque, the cosmetic solid is opaque, the
  eldritch stone is not (`BlockEldritch.java:171-173`).
* **Evidence** the ledger unit `thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java`.

### D2. The eldritch gate was half the size of the original — **fixed**

* **Original** `TileEldritchPortalRenderer.java:44-52`: the four corners of the quad are
  the four corners of a **unit square** of the camera basis and the horizontal part of a
  corner is multiplied by `scale` and the vertical part by `scaley`. The **half width**
  of the gate is therefore `scale = min(openCount, 5) / 5` and the **half height** is
  `scaley = min(openCount, 30) / 30`: a fully opened gate is two blocks wide and two
  blocks high.
* **Port (before)** `TileEldritchPortalRenderer.java:47-48`: `halfWidth = width * .5F`,
  `halfHeight = height * .5F` — the gate was one block wide and one block high.
* **Fix** `TileEldritchPortalRenderer.java:78-92` and the two pure helpers
  `halfWidth`/`halfHeight` (`:108-127`).
* **Evidence** the ledger unit `thaumcraft/client/renderers/tile/TileEldritchPortalRenderer.java`.

### D3. The eldritch gate was drawn at twice the position of its block — **fixed**

* **Original** `TileEldritchPortalRenderer.java:57-59`: the gate is drawn in the middle
  of its own block (`px = x + 0.5`).
* **Port (before)** `TileEldritchPortalRenderer.java:44-70`: the vertices were the
  **absolute** world position of the block centre, and the 1.21.1 block entity renderer
  is already placed on the corner of the block (`LevelRenderer:1232-1275`,
  `pose.translate(pos.getX(), pos.getY(), pos.getZ())`), so the gate was drawn at
  **twice** the world position of its block.
* **Fix** `TileEldritchPortalRenderer.java:88-91`: the pose is moved by half a block.

### D4. The frame of the eldritch gate was not turned by ninety degrees — **fixed**

* **Original** `TileEldritchPortalRenderer.java:74-81`: the four texture coordinates
  are `(u, 1)`, `(uMax, 1)`, `(uMax, 0)`, `(u, 0)` for the corners
  `-left - up`, `-left + up`, `left + up` and `left - up`. The `u` of the frame runs
  from the bottom to the top of the gate and its `v` from the left to the right: the
  frame of the sheet is turned by ninety degrees.
* **Port (before)**: the `u` ran horizontally and the `v` vertically, that is the frame
  was drawn upright.
* **Fix** `TileEldritchPortalRenderer.java:130-142` (`frameUv`).

### D5. The sheet of the cultist portal was drawn twisted — **fixed**

* **Original** `RenderCultistPortal.java:92-108`: the four corners are the bottom
  right, the top right, the top left and the bottom left of the basis and they carry
  the texture coordinates `(uMax, 0)`, `(uMax, 1)`, `(u, 1)` and `(u, 0)`. The frame of
  the sheet is upright, its `u` runs from the left to the right and its `v` from the
  bottom to the top, and the frame is counted backwards (`frame = 15 - time % 16`).
* **Port (before)** `RenderCultistPortal.java:61-65`: the two opposite corners carried
  the same coordinate (`(u1, 1)`, `(u1, 0)`, `(u0, 0)`, `(u0, 1)`), so the sheet was
  mirrored along the diagonal.
* **Fix** `RenderCultistPortal.java:74-92` and the pure helper `frameUv` (`:99-111`).

### D6. The two portal quads did not use the camera basis of the original — **fixed**

* **Original** the basis is the one of the 1.7.10 `ActiveRenderInfo.updateRenderInfo`:
  the left vector is `(cos(yaw), 0, sin(yaw))` and the up vector is
  `(-sin(yaw) sin(pitch), cos(pitch), cos(yaw) sin(pitch))`; both are built from the
  look angles of the **player** and not from the camera, and their horizontal part is
  negated in the third person view (`1 - 2i`).
* **Port (before)** both renderers used the camera orientation
  (`entityRenderDispatcher.cameraOrientation()`), so the quad of the original was
  mirrored in the third person view and it always faced the camera.
* **Fix** the new `common/src/main/java/thaumcraft/client/renderers/CameraBasis.java`
  with the pure helpers `leftFor(yaw, thirdPerson)` and `upFor(yaw, pitch, thirdPerson)`,
  used by `RenderCultistPortal.java:80-84` and `TileEldritchPortalRenderer.java:80-81`.

### D7. The crystal of the Outer Lands was drawn upside down — **fixed**

* **Original** `TileEldritchCrystalRenderer.java:31` loads `vcrystal.obj` with
  `AdvancedModelLoader`. The file and `obelisk_cap.obj` come from the same exporter
  ("3ds Max Wavefront OBJ Exporter v0.97b") and the original loads both with the same
  loader, so both use the same convention: the `v` of the wavefront file runs from the
  **bottom** of the image and the `v` of the model is its complement.
* **Port (before)** `ModelEldritchCrystal.java:557` used the `v` of the file unchanged,
  while `ModelEldritchCap.java:144` (the cap of the obelisk) already flipped it. One
  of the two was wrong; the cap was right, so the crystal was drawn upside down.
* **Fix** `ModelEldritchCrystal.java:557-560`.

### D8. The crystal followed the light of its block — **fixed**

* **Original** `TileEldritchCrystalRenderer.java:45-49` overwrites the coordinates of
  the light map with `(OpenGlHelper.field_77476_b, (int) (210 * shade))`, that is with
  `(5898, 178)` up to `(5898, 210)`. The light map clamps the coordinates it is given
  to the range of the texture, so the crystal of the original is **always** drawn with
  the brightest texel of the light map and the pulse of the original never reaches the
  screen.
* **Port (before)** the crystal was drawn with the light of its block, so it was black
  in an unlit room.
* **Fix** `TileEldritchCrystalRenderer.java:67-78`: the crystal is drawn with
  `LightTexture.FULL_BRIGHT`.
* **Note** the wave-1 ledger had recorded this as an unfixable blocker ("the light map
  of 1.21.1 holds sixteen steps, so the pulse of the crystal cannot be reproduced").
  The clamp of the light map is the real reason, and it makes the pulse irrelevant.

### D9. The original of the previous session — see §5

The previous session of this agent had already fixed the ribbon of the ritualist
cleric, the cubes of the altar, the obelisk and the capstone, the ground of the eldritch
mobs, the shoulder plate of the guardian and the cosine of the swing of the crab and
of the golem, and it had added the render bounding box of the obelisk and of the gate.
Those five commits are in the branch and are verified against the oracle in §5.

---

## 3. The cosmetic/eldritch block family (the question of the brief)

The `blockvis` agent had already fixed the family: 14 explicit `getRenderShape()`
overrides, the per state block entity contract
(`common/src/main/java/thaumcraft/common/blocks/PerStateBlockEntity.java` and
`common/src/main/java/thaumcraft/common/mixin/PerStateBlockEntityMixin.java`).

**Verification** (read only, no change of that work):

* `BlockEldritch.java:141` — `getRenderShape` returns `RenderShape.MODEL` for the whole
  eldritch family, so the stones are visible again.
* `BlockEldritch.java:208-210` + `EldritchBlockRules.eldritchHasTile` — the tile is
  asked per state, and the metadata 2 (the middle of the obelisk), 4, 5, 6 and 7 have
  no tile, so those stones are movable by a piston like in the original.
* The known gap of the brief (a variant that genuinely carries a tile still cannot be
  pushed) is unchanged and belongs to the `piston` agent; nothing of it was touched
  here.

**One new finding for that family** (not fixed here, see §6): the 1.7.10
`Block.isOpaqueCube` and the 1.21.1 `isSolidRender` do not agree for the TC4 blocks,
because the port registers the eldritch stones, the cosmetic solids and the eldritch
filler with `noOcclusion()`. The original says:

| block | 1.7.10 `isOpaqueCube` | port `isSolidRender` |
|---|---|---|
| `BlockEldritch` (the stones) | **false** (`BlockEldritch.java:171-173`) | false ✓ |
| `BlockCosmeticSolid` | **true** (not overridden) | false ✗ |
| `BlockEldritchNothing` (the filler) | **true** (not overridden) | false ✗ |

`TileEldritchNothingRenderer.isOpaqueCube` (D1) now compensates for the two wrong
rows. Any other place that asks "is this block opaque" (face culling, the light, the
tunnel) still sees the 1.21.1 answer.

---

## 4. The assets

* `python3 scripts/porting/convert_assets.py --verify` → `missing_references: 0`
  (checked at the end of the session, no asset was changed by this agent).
* `python3 scripts/porting/update_scope_ledger.py --check --original-root
  /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`.

---

## 5. The work of the previous session (verified here)

| commit | what | oracle |
|---|---|---|
| `6408b5b3` | the link line of the ritualist cleric is the original ribbon | `RenderCultist.java` the ribbon of the cleric |
| `671d294c` | the altar, the obelisk and the capstone are not drawn as cubes | `BlockEldritchRenderer.java:35-81` draws only the metadata 4..10 |
| `3685d648` | the golem, the guardian and the warden stand on the ground | the original models of the three mobs |
| `79cb1161` | the left shoulder plate of the guardian is mirrored | `ModelEldritchGuardian.java:220-222` the mirror flag before the box |
| `75b4f028` | the crab and the golem swing with the original cosine | `ModelEldritchCrab.java:163-164`, `ModelEldritchGolem.java:259-260` |
| `b101389e` | the render bounding box of the obelisk and of the gate | `TileEldritchObelisk.java:40-43`, `TileEldritchPortal.java:34-37` |

---

## 6. What is still open, and why

### O1. `thaumcraft/client/renderers/block/EldritchCrustBakedModel.java` — the crust is a fixed cube

* **Original** `BlockEldritchRenderer.java:35-63`: the crust (the metadata 4), the
  glyphed stone (5) and the decorated stone (6) are drawn with a box that is inset by
  **two sixteenths on every side whose neighbour is not a solid material**
  (`Block.isBlockSolid` asks `world.getBlock(x, y, z).getMaterial().isSolid()`; the rock
  material of the eldritch stone is solid). Two crust blocks therefore join seamlessly
  in the original and only the border of a field of crust is inset.
* **Port** `assets/thaumcraft/models/block/eldritch_crust.json:1-30` (and
  `eldritch_crust_5.json`, `eldritch_deco.json`) is the **fixed** box of two to fourteen
  sixteenths, so the port draws a groove of two sixteenths between every block of a
  field of crust. The helper `EldritchCrustBakedModel.java:18-61` already computes the
  bounds of the joined crust, but **no model of the port uses it**.
* **Why it is not closed here** the fix is implementable in 1.21.1 in two ways: (a) six
  boolean state properties on the crust group with a neighbour update and one model per
  combination (64 models per sheet and a block state of 128 entries), or (b) a custom
  `BakedModel` that reads the neighbours in `getQuads` and rebuilds the box. Both touch
  the shared `BlockEldritch` of the six eldritch groups and the per state block entity
  contract of the `blockvis` agent, and neither can be compile checked in this session
  (no Gradle). A half done change of the state space of the eldritch family is worse
  than a precise description, so the unit stays `partial` with the exact difference.
* **Blocker for the leader** the crust of the Outer Lands shows a grid of two
  sixteenth grooves in the port and a seamless slab in the original.

### O2. `thaumcraft/common/lib/world/dim/GenKeyRoom.java` — the guardians have no home

* **Original** `GenKeyRoom.java:110`: `eg.setHome(x + 8, y + 2, z + 8, 16)` — the
  guardian of the key room is anchored at the centre of the room with a radius of
  sixteen.
* **Port** `DungeonRooms.java:826-832` carries the count, the offset and the champion
  (all of which now match the original, the offset of the original is
  `MathHelper.getRandomIntegerInRange(random, 1, 3) * getRandomIntegerInRange(random, -1, 1)`
  = `(1..3) * (-1..1)`, which is what the port does), but the spawn record has no home
  and `ThaumcraftOuterLands.spawnRoomEntities` never calls `restrictTo`.
* **Why it is not closed here** the fix needs a home field in `DungeonRooms.Spawn` and a
  `restrictTo` call in `ThaumcraftOuterLands.spawnRoomEntities`; `DungeonRooms` is in
  `common/src/main/java/thaumcraft/common/lib/world/**`, the file of the `worldgen`
  agent, and the brief forbids touching it.
* **Exact change for the owner of that file** in
  `DungeonRooms.generateKeyRoom` the guardian spawn must carry the home
  `(x + 8, y + 2, z + 8)` and the radius `16`, and
  `ThaumcraftOuterLands.spawnRoomEntities` must call
  `entity.restrictTo(home, 16)` for it. (The original only stores the home, it does
  not use it for the artificial intelligence, so the port should store it without
  restricting the mob if the artificial intelligence of the guardian is to stay the
  same.)

### O3. Risk for the entity models (not a unit of my list, reported)

While proving the wavefront convention (D7) the layout of the model boxes was compared
and a difference was found that is **not** part of my unit list but touches three
units of it (`ModelEldritchCrab`, `ModelEldritchGolem`, `ModelEldritchGuardian`, all
`parity_verified` after the wave-1 audit):

* 1.7.10 `ModelBox` (the readable 1.7.10 vanilla source) gives the box the regions
  north `(u, v+d)`, south `(u+d, v+d)`, east `(u+d+w, v+d)`, west `(u+d+w+d, v+d)`,
  top `(u+d+w, v)`, bottom `(u+d, v)` and the local `v` of the box grows upwards, so
  the `v` of the model space of 1.7.10 runs from the **bottom** of the image.
* 1.21.1 `ModelPart$Cube` (the bytecode of the merged jar) gives the same box the
  regions north `(u+d, v+d)`, west `(u, v+d)`, east `(u+d+w, v)`, up
  `(u+d+w+d, v)`, down `(u+d, v)`, south `(u+d+w+d, v+d)`, so the `v` of the model
  space of 1.21.1 runs from the **top** of the image.
* The port copies the 1.7.10 offsets verbatim (for example `texOffs(40, 12)` for the
  hood of the guardian against the original `new ModelRenderer(this, 40, 12)`), so the
  texture of every face of the three eldritch entity models may be mirrored
  vertically, and the region of the east and the top faces is exchanged.

**The leader should look at the guardian, the crab and the golem on a frame** (see §8).
If the models are upside down, the conversion of the three models has to flip the `v`
of every box (`texOffs(u, textureHeight - v - depth - height)`) and to exchange the
east and the top faces, which is a change of the three model files and not of the
renderers.

---

## 7. The tests (authored, NOT executed)

| file | what it locks |
|---|---|
| `common/src/test/java/thaumcraft/eldritch/EldritchPortalParityTest.java` | **new** — the basis of `ActiveRenderInfo` and its mirror of the third person view, the half extents of the eldritch gate, the four texture coordinates of the two sheets, the light of the crystal |
| `common/src/test/java/thaumcraft/eldritch/EldritchClientModelParityTest.java` | the parts, the rotation points and the cosine of the crab, of the golem and of the guardian |
| `common/src/test/java/thaumcraft/eldritch/EldritchBlockAssetParityTest.java` | the models and the block states of the eldritch stones: the two to fourteen cube of the crust, the sheets of the stones, the item and the block state of the group |

**They are authored but not executed**: this agent has no Gradle, the leader runs every
build and every test suite sequentially with the shared Loom cache. They must be
executed by the leader before the branch is merged.

---

## 8. The acceptance criteria of the client run (for the leader)

1. **The eldritch void** — in the Outer Lands, stand in a room whose walls are the
   eldritch filler and look at the border between the wall and the air of the room:
   the **tunnel must swirl on the border with the air** and the wall must be invisible
   **inside** the stone. (The port drew it on the other faces; D1.)
2. **The eldritch gate** — walk into a gate of the Outer Lands: the magenta sheet must
   be **in the middle of its own block** (it was drawn at twice the position of the
   block) and it must open to **two blocks wide and two blocks high** (it was one by
   one). The frame of the sheet is turned by ninety degrees in the original: the
   animation of the original runs **vertically**. (D2, D3, D4.)
3. **The cultist portal** — the boss portal of the Inner Sanctum: the sheet of the
   sixteen frames must be **upright and not twisted along the diagonal** (D5), it must
   be 2.6 blocks wide and 3.0 blocks high when it is fully open, and it must be
   **mirrored in the third person view** like the original (D6).
4. **The eldritch crystal** — the crystal of the Outer Lands must stand **upright**
   (the tip of the crystal at the top, D7) and it must **glow in the dark** instead of
   following the light of its block (D8).
5. **The crust of the Outer Lands** — a field of the glowing crust must be a
   **seamless slab** in the original and shows a **grid of grooves** in the port (O1).
   This one is **expected to differ** until the crust is closed.
6. **The guardian, the crab and the golem** — check that the texture of the faces is
   **not upside down** and that the plates of the guardian are on the right side
   (O3). The geometry, the parts and the animation of the three models were checked
   number by number, the texture mapping of the three models was not.

---

## 9. The files of this agent

```
common/src/main/java/thaumcraft/client/renderers/CameraBasis.java                    (new)
common/src/main/java/thaumcraft/client/renderers/entity/RenderCultistPortal.java
common/src/main/java/thaumcraft/client/renderers/models/ModelEldritchCrystal.java
common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java
common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java
common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchPortalRenderer.java
common/src/main/java/thaumcraft/common/tiles/TileEldritchObelisk.java                  (previous session)
common/src/main/java/thaumcraft/common/tiles/TileEldritchPortal.java                   (previous session)
common/src/test/java/thaumcraft/eldritch/EldritchPortalParityTest.java                (new)
common/src/test/java/thaumcraft/eldritch/EldritchClientModelParityTest.java           (previous session)
common/src/test/java/thaumcraft/eldritch/EldritchBlockAssetParityTest.java            (previous session)
docs/porting/scope-ledger.json
docs/analysis/2026-10-23-agent-w2clientblocks.md
```

The previous session of this agent also changed, in the five commits of §5:
`RenderCultist.java`, `RenderEldritchGolem.java`, `RenderEldritchGuardian.java`,
`RenderEldritchWarden.java`, `ModelEldritchGuardian.java`,
`ModelEldritchCrab.java`, `ModelEldritchGolem.java`,
`TileEldritchRenderer.java` and the models and the item of the eldritch stone.
