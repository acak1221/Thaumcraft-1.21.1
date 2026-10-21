# Wave 2 agent `w2clientfx` — client rendering, FX, input and GUI units

Worktree: `/home/cyberfox-kali/wt/w2clientfx`, branch `agent/w2clientfx`.
Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, TC4 4.2.3.5).
Unit list: `/home/cyberfox-kali/units/w2clientfx.txt` (37 units: 25 `client_rendering_fx_input_and_gui`,
7 `alchemy_and_essentia`, 5 `warp_creatures_and_ecology`).

**The tests written here are authored but NOT executed** — this agent may not run Gradle.

Eleven units moved `partial -> parity_verified`:
`thaumcraft/client/fx/WRMat4.java`, `thaumcraft/client/fx/WRVector3.java`,
`thaumcraft/client/fx/bolt/FXLightningBolt.java`, `thaumcraft/client/fx/bolt/FXLightningBoltCommon.java`,
`thaumcraft/client/fx/particles/FXBreaking.java`, `thaumcraft/client/fx/particles/FXEssentiaTrail.java`,
`thaumcraft/client/fx/particles/FXWisp.java`, `thaumcraft/client/fx/particles/FXWispArcing.java`,
`thaumcraft/client/fx/particles/FXWispEG.java`, `thaumcraft/client/gui/MappingThread.java`,
`thaumcraft/common/config/MaterialTaint.java`.

The other twenty-six stay `partial`; each one carries the exact remaining difference in the ledger.

---

## 1. The lightning bolt family — the parity gap the wave brief called out

The brief named one gap explicitly: *"the native bounded midpoint bridge, deliberately bounded rather than
importing the old WRVector3/bolt manager … This is a real parity gap."* It is now closed.

### 1.1 What the original does

| oracle file:line | original behaviour |
|---|---|
| `thaumcraft/client/fx/bolt/FXLightningBoltCommon.java:43-56` | the base constructor: `numsegments0 = 1`, `increment = 1`, `length = (end-start).length()`, `particleMaxAge = 3 + rand.nextInt(3) - 1`, `multiplier = 1.0f`, `particleAge = -(int)(length*3)`, one segment |
| `FXLightningBoltCommon.java:62-66` | the entity flavour: `increment = speed`, `multiplier = 0.4f` |
| `FXLightningBoltCommon.java:76-86` | the duration flavour: `particleMaxAge = duration + rand.nextInt(duration) - duration/2`, `multiplier = multi`; the speed flavour adds `increment = speed` |
| `FXLightningBoltCommon.java:92-136` | `fractal(splits, amount, splitchance, splitlength, splitangle)`: the perpendicular offset, the `WRMat4` rotation, the fork branch with `xCrossProduct`, the split parent map |
| `FXLightningBoltCommon.java:138-146` | `defaultFractal()`: the seven steps `2, L*mult/8, 0.7, 0.1, 45`, `/12, 0.5, 0.1, 50`, `/17, 0.5, 0.1, 55`, `/23, 0.5, 0.1, 60`, `/30, 0, 0, 0`, `/34, 0, 0, 0`, `/40, 0, 0, 0` |
| `FXLightningBoltCommon.java:148-176` | `calculateCollisionAndDiffs()`: the split parent map, the sort by split and segment number, the removal test, the joint sines `sin(acos(dot)/2)` |
| `FXLightningBoltCommon.java:178-184` | `finalizeBolt()`: the collision pass and the sort by descending light |
| `FXLightningBoltCommon.java:262-283` | `calcEndDiffs()`: the bisector directions and the joint sines |
| `thaumcraft/client/fx/WRVector3.java:16-131` | the float vector, `getPerpendicular` (`:120-124`), `xCrossProduct`, `zCrossProduct`, `rotate` |
| `thaumcraft/client/fx/WRMat4.java:20-53` | the Rodrigues rotation matrix and the `translate` |
| `thaumcraft/client/ClientProxy.java:783-788` | `bolt()`: the wisp zap, `FXLightningBolt(world, source, target, seed, 4)`, `setType(0)` |
| `thaumcraft/client/ClientProxy.java:790-796` | `nodeBolt(world, x, y, z, Entity)`: type 3, duration 10, multiplier 4, step 5 — **no caller in the oracle tree** |
| `thaumcraft/client/ClientProxy.java:798-803` | `nodeBolt(world, x, y, z, x2, y2, z2)`: type 0, duration 10, multiplier 4, step 5 |
| `thaumcraft/client/fx/bolt/FXLightningBolt.java:129-186` | `renderBolt`: the width `0.03f * (distance/5 + 1) * (1 + light) * 0.5f`, the ribbon with `width/sinprev`, the round end caps, the alpha `(1-boltage)*0.4` and `1-boltage*0.5` |
| `FXLightningBolt.java:203-247` | the type colours: type 0 large `0.6/0.3/0.6`, small `1.0/0.6/1.0`, the additive blend `(770,1)` |
| `thaumcraft/client/renderers/models/gear/ModelHoverHarness.java:106-107` | the harness bolt: `FXLightningBolt(..., 1, 2.0f, 3)` |
| `thaumcraft/common/tiles/TileInfusionMatrix.java:780`, `thaumcraft/common/tiles/TileNodeConverter.java:84,87,104,107` | the node bolts of the infusion matrix and of the node converter |

### 1.2 What the port did (before this wave) — the defects

1. `client/fx/particles/InfusionArcParticle.java` built **its own** geometry: a five level midpoint bridge with
   a random branch every eighth segment. The original has **seven** fractal levels (the last three at `/30`,
   `/34`, `/40` still displace the middle point), the forks come from the fractal itself (the split chances
   `0.7/0.5/0.5/0.5`, the original split lengths and angles), a fork is half as bright, and the geometry is a
   pure function of the seed.
2. `client/fx/bolt/WispLightningShape.java` was a *partial* port of `FXLightningBoltCommon`: no `splitparents`
   map, no `calculateCollisionAndDiffs()` pass, and the joint sine was **clamped** (`jointSine()` returned `1`
   when the normalised dot product left the unit interval), so the port drew segments the original dropped.
3. The constructor flavour was wrong: `WispLightningShape(from, to, seed)` used the multiplier `0.4f`, but in
   the original `0.4f` belongs to the **entity** flavour `(detonator, target, seed, speed)`; the plain
   `(jammervec, targetvec, seed)` flavour uses the multiplier `1.0f`.
4. `ThaumcraftBoltParticle` aged the bolt by **4** per tick; the original `nodeBolt` uses the age step **5**.
5. `ThaumcraftBoltParticle` drew **both** passes in the large pass colour `0x994c99`; the original type 0 draws
   the small pass in `1.0/0.6/1.0` = `0xff99ff`.

### 1.3 The fix

`common/src/main/java/thaumcraft/client/fx/bolt/WispLightningShape.java` is now a **line by line port** of
`FXLightningBoltCommon` plus `WRVector3`/`WRMat4`:

* the four original constructor flavours with the original random draw order (`nextInt(3)` always, then
  `nextInt(duration)` for the duration flavour);
* `defaultFractal()` with the seven original steps and the public
  `fractal(splits, amount, splitchance, splitlength, splitangle)`;
* the `splitParents` map and the `calculateCollisionAndDiffs()` pass with the original removal test;
* the `WRMat4` Rodrigues matrix applied through the original `translate`, in `float`;
* the unnormalised `sin(acos(dot)/2)` joint sines, so a segment whose normalised dot product leaves the unit
  interval keeps the original `NaN` and is reported through `Segment.renderable()` as **not renderable**; the
  three bolt renderers skip exactly those segments, which is what the original rasteriser did with the
  `NaN`/`Inf` vertices.

**Proof that the original removal test never fires.** `calculateCollisionAndDiffs()` fills
`lastactivesegment[split]` with the *largest* `segmentno` of that split and then removes a segment when
`segment.segmentno > lastactiveseg` with `lastactiveseg = lastactivesegment[segment.splitno]`. The comparison
is therefore always false and the pass only computes the joint directions. The port keeps the test, so the two
implementations cannot drift apart (`BoltShapeParityTest.theCollisionPassNeverRemovesASegment`).

The call sites now use the original flavours:

| port file | original call it reproduces |
|---|---|
| `client/fx/particles/WispBoltParticle.java:23` | `ClientProxy.bolt` → `FXLightningBoltCommon(world, det, target, seed, 4)` (multiplier `0.4f`, step 4) |
| `client/fx/particles/HarnessBoltParticle.java:22` | `ModelHoverHarness` → `FXLightningBoltCommon(..., 1, 2.0f, 3)` |
| `client/fx/particles/ThaumcraftBoltParticle.java:48` | `ClientProxy.nodeBolt(world,x,y,z,x2,y2,z2)` → `FXLightningBoltCommon(..., 10, 4.0f, 5)` |
| `client/fx/particles/InfusionArcParticle.java:76` | the same `nodeBolt` of `TileInfusionMatrix:780` and `TileNodeConverter:84,87,104,107` |

and the two remaining numbers of the original were restored: the age step **5** and the small pass colour
**`0xff99ff`**.

`InfusionArcParticle` no longer builds its own geometry: it is now the original node bolt, with the original
`0.03f` width, the original `p_large`/`p_small` passes, the original additive blend and the original ribbon
with the round end caps.

### 1.4 The reference values

The expected values of the test were produced by a **float32 transcription of the original algorithm**, taken
line by line from the oracle file, with a bit exact `java.util.Random` (verified against the published
`new Random(0).nextInt()` = -1155484576, `new Random(0).nextInt(100)` = 60, 48, 29, … and
`new Random(123456789).nextInt(100)` = 65, 0, 83, …).

| seed | node bolt segments | max age | not renderable | wisp bolt segments | max age | not renderable |
|---|---|---|---|---|---|---|
| 0 | 424 | 13 | 0 | 384 | 2 | 0 |
| 1 | 288 | 13 | 0 | 384 | 2 | 0 |
| 2 | 400 | 7 | 0 | 336 | 3 | 1 |
| 3 | 392 | 5 | 0 | 240 | 4 | 0 |
| 4 | 232 | 7 | 0 | 208 | 4 | 0 |
| 5 | 392 | 7 | 0 | 456 | 4 | 2 |
| 6 | 256 | 11 | 0 | 456 | 3 | 8 |
| 7 | 304 | 9 | 0 | 256 | 3 | 0 |

(segment counts include the forks; the main chain always has `2^7 = 128` segments.)

### 1.5 Test

`common/src/test/java/thaumcraft/client/fx/BoltShapeParityTest.java` (authored, **not executed**): the segment
counts, the maximum ages, the main chain length, the age step, the multiplier, the number of segments the
original rasteriser dropped, and the first and last main chain end points, for the node bolt and for the wisp
bolt. `common/src/test/java/thaumcraft/warp/WispLightningShapeTest.java` was updated: the old version asserted
that every joint sine is finite and positive, which is no longer true because the port now keeps the original
`NaN` instead of clamping it.

---

## 2. The other FX particles

### 2.1 `FXWisp` — the mote size, the push out of blocks, the tinkle sound

* original `thaumcraft/client/fx/particles/FXWisp.java:34-140`: the six types, the lifetime
  `36 / (rand * 0.3 + 0.7)`, the mote physics, the `0.985` target damping and the `0.2` steering;
  `:44` the particle scale, `:180` the half size `0.5f * scale * ageScale`;
  `:231-283` the push out of blocks; `:188-190` the tinkle orb sound.
* port before: `client/fx/particles/PrimalWispParticle.java` used
  `initialSize = .5F * size * (rand.nextFloat()*.5F+.5F) * 2F` — a random factor of one to two that **does not
  exist in the original**, and the 1.21.1 particle engine halves the quad size, so the mote was drawn at half
  the original size as well. `client/fx/particles/MonsterSpriteParticle.java` had the same random factor.
* fix: the random factor is gone. `MonsterSpriteParticle.moteHalfSize(scale)` is `0.5f * scale` (the original
  `FXWisp:180`), `MonsterSpriteParticle.burstHalfSize(scale)` is `scale` (the original `FXBurst:32`), and
  `PrimalWispParticle.moteQuadSize(scale)` is `scale` (the 1.21.1 engine halves it, so the half size is
  `0.5f * scale`).
* the push out of blocks of the original is now implemented in
  `client/fx/particles/ParticleDeflection.java:33-79` and used by the wisp mote, by the guardian haze and by
  the essentia trail. The original implements it three times with the same body
  (`FXWisp:231-283`, `FXWispEG:87-139`, `FXEssentiaTrail:145-197`).
* the tinkle orb sound is **not** a gap: the oracle tree never sets `FXWisp.tinkle` to true, so the original
  never plays it. The ledger note of wave 1 was wrong on that point.

### 2.2 `FXWispEG` — the guardian haze

* original `thaumcraft/client/fx/particles/FXWispEG.java:20-40`: the gaussian drift `nextGaussian() * 0.03`,
  `motionY = -0.05`, the particle scale `*= 0.4f`, the lifetime `40 / (rand * 0.3 + 0.7)`, the colour
  `nextFloat() * 0.05f`; `:64-80` the render, the age fade `1 - age/maxAge` and the distance fade
  `1 - min(1024, distance)/1024`.
* port before: `client/fx/particles/WarpGuardianParticle.java` used
  `quadSize = .4F * (random.nextFloat()*.5F+.5F)` — again a random factor that the original does not have.
* fix: the size is the constant `WarpGuardianParticle.QUAD_SIZE = .4F` and the push out of blocks was added.

### 2.3 `FXBreaking` — the thaumic slime jump burst

* original `thaumcraft/client/ClientProxy.java:1024-1035` `slimeJumpFX`: the slime ball icon, the colour
  `0.7f/0.0f/1.0f`, the particle scale `0.4f`, the lifetime `(int)(66.0f / (rand.nextFloat() * 0.9f + 0.1f))`,
  on the ring of the radius `size * 0.5f * radius`;
  `thaumcraft/common/entities/monster/EntityThaumicSlime.java:143-160`: the launch burst `i * (launched+1)`
  and the landing burst `i * 8`, with `i = (int) sqrt(size)`.
* port before: the native slime had no jump particles at all; the ledger recorded the variant as missing.
* fix: `client/fx/particles/SlimeJumpParticle.java:12-40` reproduces the original particle, and
  `common/entities/monster/EntityThaumicSlime.java:112-124` reproduces the two bursts through the
  `ClientEffects` hook that `client/ClientSetup.java:80-82` installs.

### 2.4 `FXEssentiaTrail`

* original `thaumcraft/client/fx/particles/FXEssentiaTrail.java:44-60` (size, lifetime, colour),
  `:120-133` (the homing with the acceleration `0.01 / min(1, distance)`, the clamp `0.05` and the shrink
  below the distance two), `:145-197` (the push out of blocks).
* port before: the native clamped the lifetime to six hundred, which the original does not, and it had no push
  out of blocks.
* fix: the clamp is gone and the push out of blocks was added. The rest of the native essentia trail was
  already a faithful port (the size, the lifetime, the colour, the homing and the UV cell `0.5625`).

### 2.5 `FXWispArcing` — dead code in the original

`FXWispArcing` is instantiated only by `ClientProxy.sourceStreamFX` (oracle
`thaumcraft/client/ClientProxy.java:774-778`), and the oracle tree has **no caller** of `sourceStreamFX`. The
original therefore never shows an arcing bolt, and the port needs none. The wisp zap of the original is
`ClientProxy.bolt` → `FXLightningBolt`, and the port reproduces that (section 1).

### 2.6 `FXBlockRunes` — still partial

The rune particle itself is faithfully ported (`client/fx/particles/InfusionParticle.java:110-119` and
`:296-311`: the UV row `.375`, the ninety degree rotation, the `0.15` half extent, the `3 * duration`
lifetime, the rise and fade profile and the `0.04 * gravity` drift). The remaining difference: the original
also draws the runes on the eldritch blocks (`BlockEldritch:304`) and on the thaumometer
(`ItemThaumometer:92,103,128`); the native rune particle is only used by the infusion matrix.

---

## 3. The tiles, the blocks and the configuration

### 3.1 `TileJarBrain` — the jar never ticked

* original `thaumcraft/common/tiles/TileJarBrain.java:48-50` `canUpdate()` is true,
  `:53-113` the tick: the pull of the closest orb with `(1 - distance/7)^2` and the factors `0.15/0.33/0.15`,
  the swallowing of **every** orb inside the `0.1..1.1` box, `:64-97` the client side head turn and the
  periodic sigh, `:114-130` `getClosestXPOrb()` over the block box expanded by six.
* port before: `common/src/main/java/thaumcraft/common/tiles/TileJarBrain.java` had all the numbers, but
  **the tile had no ticker at all** — `ThaumcraftEldritchBlocks.JarBlock` had no `getTicker` — so the jar
  never pulled an orb, never swallowed one and never sighed. The client tick returned immediately.
* fix: `common/config/ThaumcraftEldritchBlocks.java:495-502` registers the ticker of the original, and
  `common/tiles/TileJarBrain.java:80-121` adds the client side head turn (`atan2`, the `0.04` smoothing, the
  `0.1` counter and the pitch jitter) and the sigh (`thaumcraft:brain`, volume `0.15`, pitch
  `0.8 + rand * 0.4`, the next sigh after `5000 + rand(25000)` milliseconds).
* the right click release of the original (`thaumcraft/common/blocks/BlockJar.java:239-250`, the eat delay of
  forty and the random share of at most sixty four) and the break drop
  (`BlockJar.java:197-207`) were already in the native `JarBlock` — the wave 1 ledger note was outdated.
* **remaining**: the native brain jar is a block model and has no block entity renderer, so the head turn is
  computed with the original numbers but is not visible. The sigh is audible.

### 3.2 `TileCrucible` — the heat source

* original `thaumcraft/common/tiles/TileCrucible.java:99-101`: the heat sources are the blocks with the
  material fire, the blocks with the material lava and the nitor (`blockAiry` metadata one).
* port before: `common/tiles/TileCrucible.java:155-158` used the vanilla **fire block tag**, so a campfire or a
  soul campfire heated the crucible, which the original never did. The 1.7.10 fire *material* is the fire block
  alone.
* fix: `common/tiles/TileCrucible.java:155-161` now tests `Blocks.FIRE`; the nitor and the lava are unchanged.
* **remaining**: the original Forge `IFluidHandler` (a 1000 mB water tank that other mods can read and fill)
  has no 1.21.1 counterpart, and the original player crafting event on a crucible craft is not fired.

### 3.3 `MaterialTaint` — the mobility flag

* original `thaumcraft/common/config/MaterialTaint.java:24-38`: the taint material is not solid, is not
  replaceable, does not block the grass and carries the mobility flag one; `Config.java:917-918` gives the
  flux goo and the taint material the same class.
* port before: `common/config/ThaumcraftTaint.java:72` and `common/config/ThaumcraftFlux.java:53-56` used
  `PushReaction.DESTROY`, which **destroys** the block when a piston pushes it. The original cannot push it at
  all, and the block survives.
* fix: both families now use `PushReaction.BLOCK`, exposed as `ThaumcraftTaint.PUSH_REACTION` and
  `ThaumcraftFlux.PUSH_REACTION`. 1.21.1 has no `Material` class, so the class itself is not carried over; all
  of its observable traits are now expressed with the 1.21.1 block properties.

### 3.4 `TileJarNode` and `ItemJarNode`

The wave 1 notes were outdated: the wand dispatch of the native
(`common/items/wands/ItemWandCasting.java:160-165`) **does** reach the jar, the native
`common/tiles/TileJarNode.java:128-146` reproduces the original conversion of
`thaumcraft/common/tiles/TileJarNode.java:222-241` (the aspects, the node type, the node modifier, the
identity, the level event 2001, the glass break sound, the swing, and the original does **not** shrink the
wand), and the `IAspectContainer` behaviour of `TileJarNode:110-144` is implemented.
**Remaining**: the original client event nine makes the node jar grow to three times its size for one second
after the wand hit (`TileJarNode:205-221` and `TileJarRenderer:92-100`). The native node jar is a block model
and has no block entity renderer, so that growth is not reproduced.

### 3.5 The remaining renderer units

| unit | oracle | port | remaining difference |
|---|---|---|---|
| `BlockCandleRenderer` | `BlockCandleRenderer.java:33-58` the deterministic random wax drips, one to five per block, seeded by `x + y*z` | `models/block/tallow_candle.json` and `colored_tallow_candle.json` the original geometry and the tint | the random drips cannot be expressed by a 1.21.1 JSON model — a 1.21.1 limitation |
| `BlockCosmeticOpaqueRenderer` | `BlockCosmeticSolid.java:325-355` the rune particles on the rune blocks and on the mobs inside them | `models/block/cosmetic_opaque.json`, `ObsidianTotemBakedModel.java:30-48`, `WardedGlassBakedModel.java` | the rune blocks have no rune effect (the native rune particle is only used by the infusion matrix) |
| `BlockCustomOreRenderer` | `BlockCustomOreRenderer.java:40-63` the minimum brightness 160 and the fast graphics second pass | `models/item/cinnabar_ore.json` the tint index | the emissive overlay is not reproduced |
| `BlockGasRenderer` | `BlockGasRenderer.java:38-48` the per corner height of the gas surface | `ThaumcraftFluxClient.java:9-13` a plain translucent block | the 1.21.1 fluid renderer has no per block corner height hook |
| `BlockLifterRenderer` | `BlockLifterRenderer.java:18-38` the two emissive inner slabs of the inventory render, `:40-80` the glow faces | `client/renderers/tile/TileLevitatorRenderer.java:80-108` the glow faces, the lightmap 180, the colours `0x00A000` and `0xDD11FF` | the two emissive inner slabs of the inventory render are not in the model |
| `BlockMetalDeviceRenderer` | `BlockMetalDeviceRenderer.java:18-40` the metadata 5 lid, the 7, 8, 13 boxes `W4..W12 x W2..W14`, the 12 box `W3..W13` | `blockstates/metal_device.json`, `models/block/metal_device.json` | the variants 0 to 4, 7 to 11 and 13 to 15 share one base model |
| `BlockStoneDeviceRenderer` | `BlockStoneDeviceRenderer.java:1-178` the per metadata geometry | `models/block/stone_device*.json` | the remaining variants share the wand pedestal parent |
| `BlockTaintRenderer` | `BlockTaintRenderer.java:19-63` the fibre overlay on the taint crust and soil | `fabric/.../FabricTaintFibresModel.java:52-80` the overlay on the fibres | the overlay is on the wrong block, and it exists in the fabric module only, so the neo forge client has none |
| `BlockWoodenDeviceRenderer` | `BlockWoodenDeviceRenderer.java:1-161` the per metadata geometry, the nine boxes of the arcane ear | `models/block/wooden_device*.json` | the per metadata geometry is only partly reproduced |
| `RenderWatcher` | `RenderWatcher.java:1-153`; the oracle tree never registers it | `ThaumcraftEldritchClient.java:37` the native registers it | the original never showed a watcher; the 1.21.1 client shows one |
| `ItemBowBoneRenderer` | `ItemBowBoneRenderer.java:22-44` the held bow transform `2.6666667` and `-20/90/-60`, `:71-84` the item quad | `models/item/bone_bow*.json` the vanilla bow pose | a 1.21.1 item model cannot carry that transform |
| `ModelCrystalizer` | the 1.12 model only; the oracle has no such class | `client/renderers/models/ModelEssentiaCrystalizer.java` | the rewrite was not compared cube by cube |
| `ItemJarFilledRenderer` / `ItemJarNodeRenderer` | `ClientProxy.java:454-455` registers both item renderers | `client/renderers/item/JarItemRenderer.java:14-40` and `models/block/jar_node.json` | the original had two separate items; the native has the jar block item and the jar node block |
| `TileEtherealBloomRenderer` | `TileEtherealBloomRenderer.java:38-70` the growing and scrolling light strip | `TileEtherealBloomRenderer.java:49-54` the static strip | the scroll and the growth are not reproduced |
| `TileJarRenderer` | `TileJarRenderer.java:76-90` the crooked label, `:92-100` the node jar growth | `TileJarRenderer.java:36-44` the crooked label with the original seed | the node jar growth needs a block entity renderer |

---

## 4. The library units

* `ParticleEngine` — the original `ParticleEngine.java:57` keeps four layers x dimension buckets and
  `:143` evicts at 2000 particles per bucket. The port keeps both atlases, the per effect blend split and the
  1/255 alpha threshold, but the 1.21.1 particle engine owns the budget now, so that exact eviction is not
  reproduced.
* `ClientTickEventsFML` — the original `:660-729` `renderAspectsInGui` (the aspect row on the hovered container
  slot) has no native equivalent, and the OptiFine shader group switching has no 1.21.1 counterpart. The
  native class does not exist; the HUD effects were re-derived in the split clients.
* `RenderEventHandler` — the scanned block 17x17x17 overlay, the OptiFine shader group driver and the wand
  radial overlay are not reproduced; the wand radial was replaced by `GuiFocusSelect` on purpose. The truetyper
  font load of the original `renderOverlay` is dead code (the field is never used).
* `TCFontRenderer` — the research book uses the 1.21.1 vanilla font, so the 1.7.10 glyph shapes are not
  reproduced. The extra galactic atlas was never selectable either (`FONT_GALACTIC` is defined but never used).
* `UtilsFX` — the animated texture helpers `getTextureAnimationSize`/`getTextureSize` and the
  `getEquippedProgress`/`getTimer` helpers have no native counterpart. `UtilsFX.shootFire` and `FXScorch` are
  **dead code in the original** (the oracle tree never calls `shootFire`), so the fire focus scorch is not a
  gap.
* `MappingThread` — **closed**. The id to `ItemStack` cache is gone; the 1.21.1 research book resolves the live
  native recipe holders, which is the result the original cache produced.

---

## 5. Tests authored (NOT executed)

| test | what it pins |
|---|---|
| `common/src/test/java/thaumcraft/client/fx/BoltShapeParityTest.java` | the segment counts, the maximum ages, the age steps, the multipliers, the dropped segments and the end points of the original bolt, for the node bolt, the wisp bolt and the harness bolt |
| `common/src/test/java/thaumcraft/client/fx/ParticleScaleParityTest.java` | the original half sizes of the wisp mote, the burst and the guardian haze; the original crucible heat source; the original unpushable taint and flux |
| `common/src/test/java/thaumcraft/warp/WispLightningShapeTest.java` | updated: the structural contract of the bolt shape, with the original `NaN` joint sine allowed |

---

## 6. Acceptance criteria for the leader on the real client

Each item is a frame the leader must inspect; none of them can be proven by a compile.

1. **Wisp zap** (`WispBoltParticle`): strike a wisp with a wand. The bolt must be a **branched, forked
   purple** arc with several small side branches, it must **grow from the wisp to the target over about a
   quarter of a second**, and it must fade out. It must **not** be a plain two point line.
2. **Infusion matrix arc** (`InfusionArcParticle`): while the matrix is unstable, a **branched purple arc**
   must run from the matrix centre to a point up to two blocks away. It must show the same forks and the same
   width as the wisp zap, and it must disappear in well under a second.
3. **Node converter zap** (`ThaumcraftBoltParticle`): the bolt of the node converter must be **branched and
   purple**, must reach the target in about a fifth of a second and must fade. The second (small) pass must be
   **brighter** than the first: the large pass is `0x994C99` and the small pass is `0xFF99FF`.
4. **Hover harness** (`HarnessBoltParticle`): the bolt of the harness must be the **pale blue white** of the
   original (`0.75/1.0/1.0`), about half as wide as the wisp bolt (the original width `0.015f` against
   `0.03f`) and it must last a single tick.
5. **Wisp motes** (`PrimalWispParticle`, `MonsterSpriteParticle`): the motes of a primal orb must be **small**
   — the original half size is `0.5 * 0.4 = 0.2` blocks for the trail and `0.5 * 0.1 = 0.05` for the orb mote —
   and they must **grow and shrink** over their life, not shrink from the start. The motes of a wisp entity
   must be the original size, not one to two times bigger.
6. **Eldritch guardian haze** (`WarpGuardianParticle`): the haze around the guardian must be **small and
   constant in size** (the original scale `0.4`), tinted almost black (`nextFloat * 0.05` per channel), drifting
   downwards and fading with the distance (nothing beyond 1024 blocks of distance).
7. **Thaumic slime** (`SlimeJumpParticle`): when a thaumic slime leaps and lands, **purple slime ball
   fragments** must fly off it: the colour `0.7/0.0/1.0`, the scale `0.4`, and eight of them on the landing
   burst.
8. **Brain jar** (`TileJarBrain`): an experience orb that is dropped next to a brain jar must **be pulled
   towards the jar** and **disappear** when it reaches it; the jar must swallow every orb inside its own
   block. Standing next to the jar, the **brain sigh** must be audible every five to thirty seconds, and the
   jar must be **visibly empty of any head movement** (the native has no model animation for the head).
9. **Crucible heat** (`TileCrucible`): a **fire block** under the crucible must heat it (the water boils and
   the aspects decay); a **campfire** or a **soul campfire** under it must **not** heat it.
10. **Taint and flux** (`ThaumcraftTaint`, `ThaumcraftFlux`): a piston pushing a taint crust, a taint fibre,
    the flux gas or the flux goo must **not move the block and must not destroy it**; the piston must simply
    fail to extend.
11. **Essentia trail** (`InfusionParticle`): the essentia motes between a node and a source must **stream
    towards the source with the original speed**, must not disappear after ten seconds, and must be pushed
    out of a solid block they end up inside.

---

## 7. Blockers

* The **2000 particles per layer and per dimension** eviction of the original `ParticleEngine` has no 1.21.1
  counterpart: the vanilla engine owns the particle budget.
* The **OptiFine/Iris shader group** driver of the original has no 1.21.1 counterpart at all.
* The **deterministic random wax drips** of the candle and the **per corner height** of the flux gas need
  renderers that a 1.21.1 JSON model or block cannot express.
* The **held bow transform** of the original needs an item model that a 1.21.1 item model cannot carry.
* The **node jar growth** and the **brain jar head turn** need a block entity renderer for the two jar
  blocks, which the native renders as block models; the port has no `BlockEntityRenderer` for them yet.
* The **fibre overlay of the taint** exists in the fabric module only, so the neo forge client has none.
* The **scanned block overlay** of the original `RenderEventHandler` needs the scan data of the original
  client, which the 1.21.1 port does not keep.
