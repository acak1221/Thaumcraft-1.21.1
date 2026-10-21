# Wave 2 agent `w2clientfx` — client rendering, FX, input and GUI units

Worktree: `/home/cyberfox-kali/wt/w2clientfx`, branch `agent/w2clientfx`.
Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, TC4 4.2.3.5).
Unit list: `/home/cyberfox-kali/units/w2clientfx.txt` (37 units).

The tests written here are **authored but not executed**: this agent may not run Gradle.

---

## 1. The lightning bolt family (the parity gap the wave brief called out)

The brief named one gap explicitly: *"the native bounded midpoint bridge, deliberately bounded rather than
importing the old WRVector3/bolt manager … This is a real parity gap."*

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
| `thaumcraft/client/fx/WRVector3.java:16-131` | the float vector, `getPerpendicular`, `xCrossProduct`, `zCrossProduct`, `rotate` |
| `thaumcraft/client/fx/WRMat4.java:1-56` | the Rodrigues rotation matrix and the `translate` |
| `thaumcraft/client/ClientProxy.java:783-788` | `bolt()`: the wisp zap, `FXLightningBolt(world, source, target, seed, 4)`, `setType(0)` |
| `thaumcraft/client/ClientProxy.java:790-796` | `nodeBolt(world, x, y, z, Entity)`: **type 3**, duration 10, multiplier 4, step 5 |
| `thaumcraft/client/ClientProxy.java:798-803` | `nodeBolt(world, x, y, z, x2, y2, z2)`: **type 0**, duration 10, multiplier 4, step 5 |
| `thaumcraft/client/fx/bolt/FXLightningBolt.java:129-186` | `renderBolt`: the width `0.03f * (distance/5 + 1) * (1 + light) * 0.5f`, the ribbon with `width/sinprev`, the round end caps, the alpha `(1-boltage)*0.4` and `1-boltage*0.5` |
| `FXLightningBolt.java:188-250` | the type colours: type 0 large `0.6/0.3/0.6`, small `1.0/0.6/1.0`, blend `(770,1)` |
| `thaumcraft/client/renderers/models/gear/ModelHoverHarness.java:106-107` | the harness bolt: `FXLightningBolt(..., 1, 2.0f, 3)` |
| `thaumcraft/common/tiles/TileInfusionMatrix.java:780`, `thaumcraft/common/tiles/TileNodeConverter.java:84,87,104,107` | the node bolts of the infusion matrix and of the node converter: the point to point `nodeBolt` |

### 1.2 What the port did (before this wave)

* `common/src/main/java/thaumcraft/client/fx/particles/InfusionArcParticle.java` built **its own** geometry: a
  five level midpoint bridge with a random branch every eighth segment. That is *not* the original fractal: the
  original has **seven** fractal levels (the last three at `/30`, `/34`, `/40` still displace the middle point),
  the original forks come from the fractal itself (a fork per split chance, `0.7/0.5/0.5/0.5` and the original
  split lengths and angles), the original fork is half as bright, and the original geometry is a pure function of
  the seed.
* `common/src/main/java/thaumcraft/client/fx/bolt/WispLightningShape.java` was a *partial* port of
  `FXLightningBoltCommon`: it hard coded the seven steps, it had **no** `splitparents` map, it had **no**
  `calculateCollisionAndDiffs()` pass, and its joint sine was clamped
  (`jointSine()` returned `1` when the normalised dot product left the unit interval), so it drew segments the
  original dropped.
* The constructor flavour was wrong: `WispLightningShape(from, to, seed)` used the multiplier `0.4f`, but in the
  original `0.4f` belongs to the **entity** flavour `(detonator, target, seed, speed)`; the plain
  `(jammervec, targetvec, seed)` flavour uses the multiplier `1.0f`.
* `ThaumcraftBoltParticle` aged the bolt by **4** per tick; the original `nodeBolt` uses the age step **5**
  (`FXLightningBoltCommon.increment = speed = 5`).
* `ThaumcraftBoltParticle` drew **both** passes in the large pass colour `0x994c99`; the original type 0 draws the
  small pass in `1.0/0.6/1.0` = `0xff99ff`.
* The original `nodeBolt(world, x, y, z, Entity)` is the **type 3** bolt (green, `0.1/1.0/0.1` and `0.1/0.6/0.1`).
  The port only has the type 0 variant.

### 1.3 The fix

`common/src/main/java/thaumcraft/client/fx/bolt/WispLightningShape.java` is now a **line by line port** of
`FXLightningBoltCommon` plus `WRVector3`/`WRMat4`:

* the four original constructor flavours, with the original random draw order
  (`nextInt(3)` always, then `nextInt(duration)` for the duration flavour);
* `defaultFractal()` with the seven original steps, and the public
  `fractal(splits, amount, splitchance, splitlength, splitangle)`;
* the `splitParents` map and the `calculateCollisionAndDiffs()` pass with the original removal test;
* the `WRMat4` Rodrigues matrix applied through the original `translate`, in `float`;
* the unnormalised `sin(acos(dot)/2)` joint sines, so a segment whose normalised dot product leaves the unit
  interval keeps the original `NaN` and is reported through `Segment.renderable()` as **not renderable**; the
  three bolt renderers skip exactly those segments, which is what the original rasteriser did with the
  `NaN`/`Inf` vertices.

**Proof that the original removal test never fires.** `calculateCollisionAndDiffs()` fills
`lastactivesegment[split]` with the *largest* `segmentno` of that split, and then removes a segment when
`segment.segmentno > lastactiveseg` with `lastactiveseg = lastactivesegment[segment.splitno]`. The comparison is
therefore always false, and the pass only computes the joint directions. The port keeps the test, so the two
implementations cannot drift apart. `BoltShapeParityTest.theCollisionPassNeverRemovesASegment` asserts it.

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
`0.03f` width, the original `p_large`/`p_small` passes, the original additive blend and the original ribbon with
the round end caps.

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

`common/src/test/java/thaumcraft/client/fx/BoltShapeParityTest.java` (authored, **not executed**):
the segment counts, the maximum ages, the main chain length, the age step, the multiplier, the number of
segments the original rasteriser dropped, and the first and last main chain end points, for the node bolt and
for the wisp bolt. `common/src/test/java/thaumcraft/warp/WispLightningShapeTest.java` was updated: the old
version asserted that every joint sine is finite and positive, which is no longer true because the port now
keeps the original `NaN` instead of clamping it.
