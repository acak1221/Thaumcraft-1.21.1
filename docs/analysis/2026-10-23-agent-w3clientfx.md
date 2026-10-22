# Wave 3 — `w3clientfx`: the client FX, particles, GUI and HUD units (2026-10-23)

Agent: `w3clientfx`. Worktree: `/home/cyberfox-kali/wt/w3clientfx`, branch `agent/w3clientfx`.
Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 859/859 fingerprints).

**No Gradle was run.** Every test below is **authored but NOT executed**; the leader runs the
build, the JUnit suite and the client.

---

## 1. Result

| unit | before | after |
|---|---|---|
| `thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java` | partial | **parity_verified** |
| `thaumcraft/client/fx/particles/FXSpark.java` | partial | **parity_verified** |
| `thaumcraft/client/fx/ParticleEngine.java` | partial | partial (the 2000-particle eviction is a 1.21.1 limitation) |
| `thaumcraft/client/fx/beams/FXBeamWand.java` | partial | partial (the wand tip origin belongs to the wand renderer lane) |
| `thaumcraft/client/fx/particles/FXSparkle.java` | partial | partial (the portable hole and the shock focus sites) |
| `thaumcraft/client/gui/GuiGolem.java` | partial | partial (the Use core switches need the golems lane) |
| `thaumcraft/client/gui/GuiResearchBrowser.java` | partial | partial (the forbidden marker and the sibling connectors) |
| `thaumcraft/client/gui/GuiResearchRecipe.java` | partial | partial (five page renderers of the original book) |
| `thaumcraft/client/gui/GuiResearchTable.java` | partial | partial (the original rune/line drawing and the six parchment sheets) |
| `thaumcraft/client/lib/ClientTickEventsFML.java` | partial | partial (`renderAspectsInGui`, the OptiFine shader group) |
| `thaumcraft/client/lib/REHWandHandler.java` | partial | partial (the architect preview overlay) |
| `thaumcraft/client/lib/RenderEventHandler.java` | partial | partial (the scanned block overlay, the OptiFine shader group) |
| `thaumcraft/client/lib/UtilsFX.java` | partial | partial (the eased curve of the renderQuad helper) |
| `thaumcraft/common/entities/EntityItemGrate.java` | partial | partial (the 1.7.10 vanilla push-out arithmetic is not readable here) |
| `thaumcraft/client/renderers/tile/TileMirrorRenderer.java` | partial | partial (the fixed function texgen tunnel) |
| `thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java` | partial | partial (the retained OBJ is still not rendered) |
| `thaumcraft/client/renderers/tile/TileJarRenderer.java` | partial | partial (the item form of the node jar and of the filled jar) |

`python3 scripts/porting/update_scope_ledger.py --check --original-root
/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`;
`counts` untouched; the diff touches only the seventeen units of this lane.

---

## 2. Defects found and fixed

### 2.1 `PacketFXBlockSparkle` — the server half of the original packet did not exist

**Original.** `oracle thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java:36-52`: the packet
carries `x, y, z, colour` and the client half calls
`Thaumcraft.proxy.blockSparkle(world, x, y, z, colour, 7)`.
`oracle thaumcraft/client/ClientProxy.java:632-644` (`blockSparkle`): the three channels are the
bytes of the colour divided by 255, every channel is jittered by `-0.2 + random * 0.4`, the special
colour `-9999` is **not** a colour: it is replaced per particle by a random grey
`0.33 + random * 0.67`, the alpha is `0.9f`, the position is `x - 0.1 + random * 1.2`, the velocity is
`random * 0.02` upwards, the atlas frames are `112..120` of `textures/misc/particles.png`, the life is
`5 + random(8)` plus the delay `random(10)` and the scale is `0.7 + random * 0.4` of the tenth.
Every original sender wrote to the players within the thirty two blocks of the effect
(`NetworkRegistry.TargetPoint(..., 32.0)`).

**Port before.** The client half existed only for the growth lamp
(`common/src/main/java/thaumcraft/client/fx/particles/GrowthLampParticle.java`, green `0x40ff40`
only). The **server** half of the other six senders had no payload at all, so none of them reached the
client.

**Fix.**

* new `common/src/main/java/thaumcraft/common/lib/network/BlockSparkleFxPayload.java` — the S2C
  payload with the dimension, the block and the **raw** colour, including the special `-9999`
  (`BlockSparkleFxPayload.RANDOM_GREY`);
* new `common/src/main/java/thaumcraft/common/lib/network/BlockSparkleFxNetworking.java` — the
  sender with the original thirty two block radius;
* new `common/src/main/java/thaumcraft/client/fx/particles/BlockSparkleParticle.java` — the original
  `FXGeneric` of the block sparkle, with the colour rule as two pure static functions
  (`baseColour`, `jitter`) so a test can assert them;
* `GrowthLampParticle` now extends it with the fixed green of the original, so the growth lamp keeps
  its exact old behaviour and both paths share one particle;
* new `common/src/main/java/thaumcraft/client/fx/BlockSparkleClientEffects.java` — the client half,
  `particleCount(7)` particles (none at the minimal particle setting, seven at the decreased one,
  fourteen at the full one, oracle `ClientProxy.java:899-905`);
* registered in `common/src/main/java/thaumcraft/common/Thaumcraft.java` (server) and
  `common/src/main/java/thaumcraft/client/ClientSetup.java` (client).

**The six senders, with the oracle line of each:**

| original sender | colour | native call site |
|---|---|---|
| `TileFluxScrubber.java:100` | `0xDD00FF` | `common/.../tiles/TileFluxScrubber.java` (the removed flux block) |
| `TileEldritchLock.java:130` | `0x400040` | `common/.../lib/world/OuterLandsBossRooms.java` (every removed field block of the 5×5×5 cube) |
| `ItemFocusWarding.java:137,151` | `16556032` (`0xFCA000`) | `common/.../lib/world/WardingCasts.java` (on the ward and on the restore) |
| `WandManager.java:244,245` | `-9999` | `common/.../lib/ThaumatoriumFormation.java` (both halves of the new thaumatorium) |
| `WandManager.java:420` | `-9999` | `common/.../items/wands/WandManager.java` (the thaumonomicon) |
| `WandManager.java:793-800` | `-9999` | `common/.../lib/AdvancedFurnaceFormation.java` (the whole 3×2×3 advanced furnace volume) |
| `EntityEldritchWarden.java:231-234` | `0x800080` | `common/.../entities/monster/EntityEldritchWarden.java` (the seven field blocks of ten that do not draw the arc) |
| `ServerTickEventsFML.java:154` | `0xC0C0FF` | `common/.../lib/world/TradeSwaps.java` (the swapped block) |

**A second defect fell out of the same table.** The native warden sent the arc lightning in the
30 % case and **nothing at all** in the other 70 %; the original sends the `0x800080` sparkle there.
Fixed in the same file.

### 2.2 `FXSpark` — three of the four missing call sites

**Original.** `oracle thaumcraft/client/fx/particles/FXSpark.java:22-37`: the spark lives
`5 + rand(5)` ticks, picks one of three 8×8 sprite sheets, has an optional horizontal flip, no
gravity and no movement; the caller sets the colour, the alpha and the size.

**Port before.** The native `ShockSparkParticle` and `CrystalSparkParticle` reproduce the particle
itself, but three original call sites had no native counterpart.

**Fix.**

* `common/src/main/java/thaumcraft/common/blocks/BlockEldritch.java` — the counting eldritch lock
  (metadata 8) now throws the original spark: size `0.5`, colour `0.65+random*0.1 / 1 / 1`, alpha
  `0.8` (oracle `BlockEldritch.java:289-302`). The condition of the original — the spark only while
  the lock counts down, `count >= 0` — is kept.
* `common/src/main/java/thaumcraft/common/blocks/BlockAiry.java` — a new `animateTick` reproduces
  oracle `BlockAiry.java:299-311`: the metadata ten is the white spark
  (`0.65+random*0.1 / 1 / 1`, alpha `0.8`), the metadata eleven is the blue spark
  (`0.3-random*0.1 / 0 / 0.5+random*0.2`), the size is `0.33+h` with `h = random*0.33` at the
  height `0.1515+h/2`, and one tick in fifty plays the original "jacobs" sound at `0.5` with the
  pitch `1+(random-random)*0.2`.
* `common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchGolem.java` — the headless
  golem throws the original spark of the size `0.3` in the direction of its head once in twenty
  ticks (oracle `EntityEldritchGolem.java:288-300`), with the original
  `0.65+random*0.1 / 1 / 1` and the alpha `0.8`.
* `common/src/main/java/thaumcraft/client/fx/particles/ShockSparkParticle.java` — a new
  `spark(...)` factory that lets a caller set the colour and the alpha of the original
  `FXSpark`.

The fourth missing site — the champion — was **not** missing: the native
`common/.../common/lib/mods/ChampionModBold.java:11-19` already reproduces
`oracle ChampionModBold.java:30-40` (the fifty per cent, the position in the bounding box, the
colour `0.3-random*0.1 / 0 / 0.8+random*0.2` and the size `0.2`), and
`common/.../client/ThaumcraftChampionClient.java` draws it.

### 2.3 The node jar and the brain jar had no renderer at all

**Original.** `oracle thaumcraft/client/renderers/tile/TileJarRenderer.java:53-59`: a node jar is
drawn **by the node renderer itself**, one tenth of a block below the centre of the block, and
`oracle thaumcraft/client/renderers/tile/TileNodeRenderer.java:186-189` gives that node
`condition = true` (it is always visible), `size = 0.7f` and **no** depth ignore (only a revealed
node ignores the depth test), with the same sixty four block view distance.

`oracle thaumcraft/client/renderers/tile/TileJarRenderer.java:155-175` (`renderBrain`): the brain
bobs with `sin(ticksSinceLevel / 14) * 0.03 + 0.03`, it sits `0.8` below the content frame, it is
turned by the interpolated `rota`/`rotb` around the Z axis and by minus ninety degrees around the Y
axis, and it is drawn with the scale `0.4` on `textures/models/brain2.png`; the brine of the jar is
the `jarbrine.png` volume of the original `ModelJar`. The content frame itself
(`oracle TileJarRenderer.java:60-64`) is the block centre, one hundredth of a block above the block
floor, turned by one hundred and eighty degrees around the X axis.

**Port before.** The native tree had **no** renderer for the two jar blocks: the node jar was an
empty glass jar and the brain jar had no brain at all. The native `ModelBrain` and `ModelJar` were
**dead code** — nothing referenced them.

**Fix.**

* new `common/src/main/java/thaumcraft/client/renderers/tile/TileJarNodeRenderer.java` — the node
  of the jar, always visible, the size `0.7`, the depth test kept;
* new `common/src/main/java/thaumcraft/client/renderers/tile/TileJarBrainRenderer.java` — the bob,
  the turn and the scale of the brain, and the brine of the jar;
* `common/src/main/java/thaumcraft/client/renderers/tile/TileNodeRenderer.java` — the drawing body
  of the original `renderNode` was extracted into a shared `drawNode(...)` so the node of the jar and
  the revealed node use the same numbers; two new render types keep the depth test for the jar;
* `common/src/main/java/thaumcraft/client/ClientSetup.java` — the two renderers are registered and
  the two jar blocks now use the same translucent glass shell as the ordinary jar
  (oracle `BlockJarRenderer.java:64-77` draws every metadata value with the same faces).

**A dead-code proof, not a gap.** The ledger claimed that "the node jar grows to three times its
size for one second after a wand hit" (oracle `TileJarRenderer.java:92-100`). That is **dead code in
the original**: the `GL11.glScalef(size, size, size)` is applied *after* every draw call of that
renderer and the matrix is popped immediately (`oracle TileJarRenderer.java:107-117`), so the scale
never reaches a single vertex. The native tree therefore must **not** grow the node either, and
this wave did not add such an animation. The visible result of the original is the plain node.

### 2.4 The 1.21.1 block entity pose of the jar content

The native block entity renderer pose is at the **block corner**, not at its centre: the 1.21.1
`LevelRenderer` translates the pose by `pos.getX() - cameraX` (bytecode of
`net/minecraft/client/renderer/LevelRenderer.class`, the `BlockEntityRenderDispatcher.render`
call site). The native `TileJarRenderer` already relies on that (its liquid is drawn in block corner
coordinates). The brain jar therefore translates by `0.5, 0.01, 0.5` and not by a half block
offset.

### 2.5 The forbidden marker of a warped research was not drawn

**Original.** `oracle thaumcraft/client/gui/GuiResearchBrowser.java:366` draws `drawForbidden(x+11, y+11)`
for every research whose warp is above zero, and `drawForbidden` (`:789-802`) is the animated
quad strip of the **tainted** strip of the node sheet: thirty two frames, the strip 5, the frame
`31 - (player.tickCount % 32)`, the size eighty pixels, the colour `0x440055` with the alpha
`0.66`.

**Port before.** The native node loop had no forbidden marker at all.

**Fix.** `common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java` — a new `forbidden(...)`
draws the marker from `textures/misc/nodes.png` (2048×2048, so the cell is 64×64) with the original
colour and the original alpha, before the frame of the cell, exactly as the original does.

**Still open in that unit:** the sibling connectors (`oracle GuiResearchBrowser.java:327-348`) and the
three pixel wide gradient connector line with the sine wobble of the original
(`:732-787`), which the native approximates with a one pixel line in its own colours.


### 3.1 `PacketFXBlockSparkle` — **settled**, see §2.1.

### 3.2 `EntityItemGrate` — **documented, not a defect of the port**

The semantics match: the original overrides the vanilla check of the item entity
(`oracle thaumcraft/common/entities/EntityItemGrate.java:28-36`); the overridden method is
`func_145771_j(double,double,double)`, which the 1.12.2 MCP names call `Entity.pushOutOfBlocks`, so
the semantics of the override are the original ones. The native installs the grate predicate
(`common/.../common/entities/EntityItemGrate.java:49`) and pushes the item out of the grate with its
own arithmetic (`:57-89`).
**The blocker is the environment, not the port:** there is no readable 1.7.10 vanilla source here
(the oracle holds the mod only), so the *exact* vanilla push-out arithmetic cannot be compared
byte for byte. Everything that the mod itself specifies is reproduced.

### 3.3 `TileMirrorRenderer` — **a genuine 1.21.1 limitation**

The original draws the linked view with `GL11.glTexGen` texture coordinate generation and a hand
written projection matrix (`oracle thaumcraft/client/renderers/tile/TileMirrorRenderer.java:52-554`).
The fixed function texture coordinate generation has no equivalent in the 1.21.1 shader pipeline
(`RenderType`/shader based rendering only), and the additive blend of the inner layers has to be
approximated by the translucent pass.
Everything else of the renderer **is** the original and is already reproduced: the pane of the
unlinked and of the linked mirror, the offset of the unstable mirror, the brightness and the turn of
the six facings, the sixteen layers, the rotation `(i*i*4321+i*9)*2`, the fade `1/(16-i+1)`, the two
original textures and the original `Random(31100L)` colour sequence
(`common/.../client/renderers/models/ModelTunnelField.java:24,36`).
**Verdict: the approximation can not be made exact in 1.21.1.** The unit stays `partial` with that
blocker.

### 3.4 `TileThaumatoriumRenderer` — **the pose is now derived exactly; the change was not applied here**

The retained asset is in the tree
(`common/src/main/resources/assets/thaumcraft/textures/models/thaumatorium.obj`) and its vertex
range was measured: **x −0.5…0.5, y −0.5…0.5, z 0.0…2.0** (114 vertices, 92 texture coordinates).
The OBJ therefore stands two blocks tall with its **z axis up**, which is exactly what the original
compensates with `glRotatef(90, -1, 0, 0)`.

The exact pose, derived from the oracle and the asset:

```java
// oracle TileThaumatoriumRenderer.java:44-73
pose.translate(0.5, 0.0, 0.5);                 // the original glTranslatef(x+0.5, y, z+0.5)
pose.mulPose(Axis.XP.rotationDegrees(-90));   // the original glRotatef(90, -1, 0, 0)
switch (facing) {                             // the original facing rotation around Z
    case NORTH -> pose.mulPose(Axis.ZP.rotationDegrees(270));
    case SOUTH -> pose.mulPose(Axis.ZP.rotationDegrees(90));
    case EAST  -> pose.mulPose(Axis.ZP.rotationDegrees(180));
    default    -> pose.mulPose(Axis.ZP.rotationDegrees(0));   // WEST/DOWN/UP
}
```

(the 1.21.1 block entity pose is at the block corner, so the original `x+0.5, y, z+0.5` becomes
`0.5, 0, 0.5` inside the renderer).

**Why it was not applied in this wave.** The unit is also on the `w3clientrender` list, and the
native tree has **no** `TileThaumatoriumRenderer.java` at all: applying the pose means creating
that file, registering it, and turning the two machine blocks (`thaumatorium` and
`thaumatorium_top`) into `RenderShape.INVISIBLE`, because the OBJ already covers both blocks. Two
agents writing the same new file and the same registration is an add/add merge conflict, and a
machine drawn with a wrong pose is worse than the box that is drawn today. The exact change is
written above and in the ledger evidence so that it can be applied in one step by the owner of the
unit or by the leader.

The **cycling output preview** cannot be reproduced at all: the original shows the output of one of
the *possible* recipes of the machine (`tile.recipeHash`, one recipe every forty ticks) and the
native machine is programmed with a job list (`common/.../common/tiles/TileThaumatorium.java:322`)
that holds no such set.

### 3.5 `ItemJarNodeRenderer` / `ItemJarFilledRenderer` — **the block entity side is done, the item side is not**

The original registers an item renderer for the node jar item
(`oracle thaumcraft/client/ClientProxy.java:455`) that draws the jar shell **and** the node in three
rotations, and one for the filled jar item (`:454`) that draws the jar with its liquid and its label.
The native tree renders the jar item through
`common/.../client/renderers/item/JarItemRenderer.java` and has **no** `ItemJarNodeRenderer`; the
node jar item is the ordinary jar model. The item form of the two jars therefore still differs, and
the unit stays `partial`. The *block* form of the node jar is now reproduced (§2.3).

### 3.6 The rune blocks still have no rune particles — **described, not changed**

The eldritch rune (oracle `BlockEldritch.java:302-304`, the runed block's random update) and the
thaumometer rune (`ItemThaumometer`) call sites belong to the block and the API lanes.
The native already has `common/.../client/fx/RuneEffects.java` with the original rune particle and
the original rate; the missing part is only the **call site** of the eldritch rune at the exact
metadata and the scan-only rate of the thaumometer. The native `BlockEldritch.animateTick` does
call `RuneEffects.eldritchRune(client, pos)` for the runed variant, so the eldritch rune is in
place; the **thaumometer** rune (the original draws the rune particles of the scanned block over
the block, `oracle ItemThaumometer`) is still missing and belongs to the research lane.

---

## 4. What is still open, with the precise blocker

| unit | the exact remaining difference | the lane that owns it |
|---|---|---|
| `ParticleEngine` | the 2000-particle eviction per layer and dimension bucket (oracle `ParticleEngine.java:57,143`); the vanilla engine owns the budget now | a genuine 1.21.1 limitation |
| `FXBeamWand` | the beam starts at the wand tip, the native focus particles start at the player | the wand renderer lane (`ItemWandRenderer`, `WandPerspectiveModel`, `FirstPersonWandTipOrigin`) |
| `FXSparkle` | the portable hole sparkles (oracle `TileHole.java:142-211`, 24 calls) need a client tick of the native `TileHole`; the shock focus sparkles (oracle `ItemFocusShock.java:166,174`) need the five sparkles at the hit point and at the pointed entity, which the native `ShockFxPayload` does not carry; the magical leaves and the lifter have no native block | the tiles and the wands lanes |
| `GuiGolem` | the three switches of the Use core; `EntityGolemBase.setUseOptions` accepts only the default baseline (native `EntityGolemBase.java:657-662`) and the original consumes the toggles in `AIUseItem.java:135,155,190` and `AIFillTake.java:55` | the golems lane (`w3golems`) |
| `GuiResearchBrowser` | the sibling connectors (oracle `GuiResearchBrowser.java:327-348`) and the three pixel gradient connector line of the original (`:732-787`) | this lane, not started |
| `GuiResearchRecipe` | the JARLABEL, ORE, PRIMALARROW, SCEPTRE and CRUCIBLE smelting pages (oracle `GuiResearchRecipe.java:25-1313`) | this lane, not started |
| `GuiResearchTable` | the original rune and line drawing and the six parchment sheets | this lane, not started |
| `ClientTickEventsFML` | `renderAspectsInGui`, the aspect row on the hovered container slot (oracle `ClientTickEventsFML.java:660-729`); the OptiFine shader group has no 1.21.1 counterpart | this lane, not started |
| `REHWandHandler` | the architect preview overlay (oracle `REHWandHandler.java:79,311,319`) | this lane, not started |
| `RenderEventHandler` | the scanned block overlay (oracle `RenderEventHandler.java:252-268`) and the OptiFine shader group driver | this lane, not started |
| `UtilsFX` | the curve of the `renderQuad` helper of the original is the linear one of the native port | this lane, not started |
| `EntityItemGrate` | the exact 1.7.10 vanilla push-out arithmetic (no readable 1.7.10 vanilla in this environment) | the environment |
| `TileMirrorRenderer` | the fixed function `glTexGen` tunnel | a genuine 1.21.1 limitation |
| `TileThaumatoriumRenderer` | the retained OBJ is still not rendered (the exact pose is derived above) | the `w3clientrender` unit, one step away |
| `TileJarRenderer` | the item form of the node jar and of the filled jar | the `w3clientrender` unit |

---

## 5. Tests (authored, NOT executed)

| file | methods | what it protects |
|---|---|---|
| `common/src/test/java/thaumcraft/client/fx/BlockSparkleParityTest.java` | 5 | the original colour rule (the three bytes), the random grey range `0.33…1.0`, the jitter `-0.2 + random*0.4`, the frames `112`/`9`, the alpha `0.9`, the quarter `0.1`, the `-9999` marker and the thirty two block radius |
| `common/src/test/java/thaumcraft/client/renderers/tile/JarRendererParityTest.java` | 4 | the brain bob `sin(tick/14)*0.03+0.03`, the wrapped turn, the content offset `0.01`, the brain scale `0.4`, the brain height `-0.8`, the jar node size `0.7` and its sixty four block distance |

No test constructs a `Block`: every rule under test is a pure static function
(`BlockSparkleParticle.baseColour`, `BlockSparkleParticle.jitter`,
`BlockSparkleParticle.colour`, `TileJarBrainRenderer.bob`, `TileJarBrainRenderer.turn`).

---

## 6. Acceptance criteria for the leader's client run

1. **Block sparkle, flux scrubber** — power a flux scrubber next to a flux goo/gas block. The moment
   the scrubber removes one flux block, the client must show a burst of **magenta** (`0xDD00FF`)
   sparkles at that block (seven at the "decreased" particle setting, fourteen at the full one).
2. **Block sparkle, eldritch lock** — trigger an eldritch lock in the Outer Lands. Every eldritch
   field block that the opening removes must flash with a **dark violet** (`0x400040`) burst.
3. **Block sparkle, warding focus** — ward a block with the warding focus. The warded block must
   flash with an **orange** (`0xFCA000`) burst, and so must the block when the focus un-wards it.
4. **Block sparkle, the wand** — build a thaumonomicon from a bookshelf with a focusless wand: the
   bookshelf must flash with a burst of **random grey** sparkles (not a fixed colour). The same
   grey burst must appear on both halves of a thaumatorium and over the whole volume of an advanced
   alchemical furnace.
5. **Block sparkle, the eldritch warden** — make the warden frenzy. Seven field blocks out of ten
   must flash with a **violet** (`0x800080`) burst and three out of ten must draw the arc lightning
   instead.
6. **Block sparkle, the trade focus** — swap a block with the trade focus. The swapped block must
   flash with a **pale blue** (`0xC0C0FF`) burst.
7. **Node jar** — put a node into a node jar. The jar must show the **glowing node inside it**,
   at 70 % of the size of a free node, always visible (no thaumometer or goggles needed) and
   **not** visible through the terrain (the depth test is kept). The node must **not** grow or
   shrink: the one second growth of the original is dead code.
8. **Brain jar** — place a brain jar. A **brain** must float in the brine of the jar, it must bob
   gently, and it must **turn to look at the player** when no experience orb is near. When an
   experience orb is thrown in the jar, the brain must turn towards the orb.
9. **Eldritch lock spark** — activate an eldritch lock. While it counts down, a **white** spark must
   appear at the block every tick, and it must stop when the lock is no longer counting.
10. **Airy spark** — place the magic/void hole airy blocks (metadata ten and eleven). The metadata
    ten must throw a **white** spark and the metadata eleven a **blue** one, and the "jacobs" sound
    must be audible once in a while.
11. **Headless eldritch golem** — behead the eldritch golem boss. A small **white** spark must fly
    out of its head once in a while.
12. **The jar glass** — the ordinary jar, the void jar, the node jar and the brain jar must all use
    the same translucent glass. **If the brain or the node is not visible through the glass, the
    shell writes its depth and the content is hidden** — that is the one open risk of this wave
    (the 1.7.10 alpha test of the original discards the fully transparent pixels, the 1.21.1
    translucent pass does not). In that case the jar shell needs a no-depth-write render type.
13. **The forbidden marker** — open the research browser and look at a research with warp (the
    original marks `FOCUSHELLBAT`, `SINSTONE`, `INFERNALFURNACE`, `JARBRAIN`, `LIQUIDDEATH`, … with
    a warp of one to three). The cell must carry an **eighty pixel wide dark violet** (`0x440055`)
    animated square with the alpha 0.66, and it must animate backwards over the thirty two frames
    of the tainted strip of the node sheet.

---

## 7. Files changed

* `common/src/main/java/thaumcraft/common/lib/network/BlockSparkleFxPayload.java` — **new**.
* `common/src/main/java/thaumcraft/common/lib/network/BlockSparkleFxNetworking.java` — **new**.
* `common/src/main/java/thaumcraft/client/fx/BlockSparkleClientEffects.java` — **new**.
* `common/src/main/java/thaumcraft/client/fx/particles/BlockSparkleParticle.java` — **new**.
* `common/src/main/java/thaumcraft/client/fx/particles/GrowthLampParticle.java` — now the green
  specialisation of the shared particle.
* `common/src/main/java/thaumcraft/client/fx/particles/ShockSparkParticle.java` — the `spark(...)`
  factory.
* `common/src/main/java/thaumcraft/client/renderers/tile/TileJarNodeRenderer.java` — **new**.
* `common/src/main/java/thaumcraft/client/renderers/tile/TileJarBrainRenderer.java` — **new**.
* `common/src/main/java/thaumcraft/client/renderers/tile/TileNodeRenderer.java` — the shared
  `drawNode(...)` and the two depth-keeping render types.
* `common/src/main/java/thaumcraft/client/ClientSetup.java` — the two new renderers, the two jar
  block render types, the block sparkle client receiver.
* `common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java` — the forbidden marker of a
  warped research.
* `common/src/main/java/thaumcraft/common/Thaumcraft.java` — the block sparkle server payload.
* `common/src/main/java/thaumcraft/common/tiles/TileFluxScrubber.java` — the `0xDD00FF` sender.
* `common/src/main/java/thaumcraft/common/lib/world/OuterLandsBossRooms.java` — the `0x400040`
  sender.
* `common/src/main/java/thaumcraft/common/lib/world/WardingCasts.java` — the `16556032` sender.
* `common/src/main/java/thaumcraft/common/lib/world/TradeSwaps.java` — the `0xC0C0FF` sender.
* `common/src/main/java/thaumcraft/common/lib/ThaumatoriumFormation.java` — the `-9999` sender.
* `common/src/main/java/thaumcraft/common/lib/AdvancedFurnaceFormation.java` — the `-9999` sender.
* `common/src/main/java/thaumcraft/common/items/wands/WandManager.java` — the `-9999` sender.
* `common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchWarden.java` — the
  `0x800080` sender and the arc/sparkle split.
* `common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchGolem.java` — the headless
  spark.
* `common/src/main/java/thaumcraft/common/blocks/BlockEldritch.java` — the lock spark.
* `common/src/main/java/thaumcraft/common/blocks/BlockAiry.java` — the airy family spark.
* `common/src/test/java/thaumcraft/client/fx/BlockSparkleParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/client/renderers/tile/JarRendererParityTest.java` — **new**.
* `docs/porting/scope-ledger.json` — the verdicts and the evidence of the seventeen units.
* `docs/analysis/2026-10-23-agent-w3clientfx.md` — this report.

---

## 8. What is verified here and what is not

**Verified here (no Gradle needed):**

* `python3 scripts/porting/update_scope_ledger.py --check --original-root
  /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`; the
  ledger diff touches only the seventeen units of this lane and leaves `counts` untouched.
* The JSON of the ledger parses and every edited unit carries its new coverage and its new
  evidence.
* The numbers of the block sparkle, of the brain bob, of the brain turn and of the jar node were
  read out of the oracle and of the retained assets, not out of the port's own comments.

**NOT verified here (the leader must run it):**

* `:common:compileJava` and `:common:compileTestJava` — **not run**.
* `:common:test` — **not run**; the two new test files are authored only.
* The client frames — **not seen**; the acceptance criteria of §6 are written for the leader.
* The jar glass depth question of §6.12 is an open risk that only a real frame can answer.
