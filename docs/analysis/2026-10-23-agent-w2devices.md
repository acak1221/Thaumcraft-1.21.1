# 2026-10-23 — wave 2 devices lane (`w2devices`)

Worktree `/home/cyberfox-kali/wt/w2devices`, branch `agent/w2devices`.
Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 4.2.3.5).
Unit list: `/home/cyberfox-kali/units/w2devices.txt` (13 units of the
`advanced_devices_and_visnet` family).

## 1. Result

| unit | before | after | what changed |
|---|---|---|---|
| `thaumcraft/client/fx/particles/FXBoreSparkle.java` | partial | **parity_verified** | new native `BoreDigSparkleParticle` + the dig effect of the bore |
| `thaumcraft/client/fx/particles/FXSpark.java` | partial | partial | the native sparks cover the crystal and the shock trap, not every original site |
| `thaumcraft/client/fx/particles/FXSparkle.java` | partial | partial | the native sparkles cover four of the nine original proxy sites |
| `thaumcraft/client/fx/particles/FXSparkleTrail.java` | partial | **parity_verified** | the particle is dead code in the original |
| `thaumcraft/client/renderers/item/ItemTrunkSpawnerRenderer.java` | partial | **parity_verified** | the native item model draws the chest of the original with the original texture |
| `thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java` | partial | **parity_verified** | the turn of the cap and the phase of the bob fixed |
| `thaumcraft/client/renderers/tile/TileMirrorRenderer.java` | partial | partial | the view dependent texture generation of the original has no 1.21.1 equivalent |
| `thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java` | partial | partial | the retained OBJ is not rendered and the preview has no data |
| `thaumcraft/common/entities/EntityItemGrate.java` | partial | partial | the 1.7.10 vanilla `pushOutOfBlocks` is not readable here |
| `thaumcraft/common/entities/ItemSpawnerEgg.java` | partial | **parity_verified** | the creative egg is never spent any more |
| `thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java` | partial | partial | the transport needs a payload in the forbidden network library |
| `thaumcraft/common/lib/network/misc/PacketBoreDig.java` | partial | **parity_verified** | the dig event reaches the client and is used |
| `thaumcraft/common/tiles/TileArcaneBore.java` | partial | **parity_verified** | the range of the dig event and the dig effect fixed |

**7 units moved `partial -> parity_verified`, 6 stay `partial` with a written reason.**

* **4 defects found and fixed** (2 of them in the shared common layer).
* **3 new test classes** with 13 test methods, **authored but NOT executed** (no Gradle run is
  allowed in this wave; the leader has to run `:common:test`).

| commit | content |
|---|---|
| `15976653` | `fix(devices)`: the creative spawn egg of the original is never spent |
| `b1d9ca72` | `fix(bore)`: the dig event of the original reaches the client and digs with a flying sparkle |
| `2471daa7` | `fix(devices)`: the cap of the flux scrubber turns around the vertical axis and bobs with the original phase |
| `d8d6dc5a` | `docs(devices)`: this report |
| `0c81cea7` | `docs(devices)`: the scope ledger verdicts of the thirteen units || `14c423ec` | `fix(bore)`: the dig effect only draws when the dug block is still there |

## 2. Defects: oracle -> port -> fix

### 2.1 The creative spawn egg was spent in the creative mode

* **Oracle** `thaumcraft/common/entities/ItemSpawnerEgg.java:95-102` —
  `if ((entity = ItemSpawnerEgg.spawnCreature(...)) != null) { ... if (!player.field_71075_bZ.field_75098_d) { --stack.field_77994_a; } }`.
  The creative mode of the player is the **only** condition, so a creative player never
  loses the egg.
* **Port (before)** `common/src/main/java/thaumcraft/common/entities/ItemSpawnerEgg.java:100` and
  `:107` — `stack.shrink(1)` in both branches, always. The 1.21.1 `SpawnEggItem` does the same
  (its own `useOn` calls `itemstack.shrink(1)` in the spawner branch **and** in the mob branch,
  verified in the merged jar), so the subclass had to reproduce the original rule itself.
* **Fix** the new pure rule `ItemSpawnerEgg.spendsStack(boolean creative)` (`:110-113`) and the
  two guarded shrinks (`:100`, `:107`). The helper `creative(Player)` treats a player without
  abilities as never creative, because the original dereferences the player unconditionally.

### 2.2 The bore had no flying dig sparkle and no dig event

* **Oracle** `thaumcraft/client/fx/particles/FXBoreSparkle.java:30-64` — the sparkle: red
  `0.2`, green `0.6+rand*0.3`, blue `0.2`, the scale `rand*0.5+0.5`, the life
  `base/2+rand(base)` with `base = max(1, (int)(distance*3))`, the velocity `gauss*0.01`, the
  steering `0.3` (`0.6` below four blocks), the drag `0.985`, the clamp `+-0.35`, the size
  `0.1*scale*(sin(age/3)*0.5+1)`, the cell `age%4` of the second row of the sheet and the full
  bright light (`func_78380_c(240)`).
* **Oracle** `thaumcraft/client/ClientProxy.java:864-874` — `boreDigFx`: one dig sparkle in ten
  and the block particles of the dug block in the other nine, from the dug block towards the
  block the bore digs into.
* **Oracle** `thaumcraft/common/tiles/TileArcaneBore.java:496-501` — the client plays the break
  sound of the dug block once per dig event and then draws `particleCount(10)` dig effects.
* **Oracle** `thaumcraft/common/lib/network/misc/PacketBoreDig.java:39-58` — the dig event
  carries the dig target to the client (`(offset & 0xFF) - 64` per axis) and is sent to the
  players around the bore with a `TargetPoint` of `64.0` blocks
  (`thaumcraft/common/tiles/TileArcaneBore.java:707-713`).
* **Port (before)** `common/src/main/java/thaumcraft/client/BoreClientEffects.java:54` — the
  bore only had the vortex and the beam; there was no flying dig sparkle at all, and
  `common/src/main/java/thaumcraft/common/tiles/TileArcaneBore.java:789` accepted a client
  target that was up to **seventy** blocks away (`:796-799`), which the original never allows.
* **Fix**
  * the new native particle `common/src/main/java/thaumcraft/client/fx/particles/BoreDigSparkleParticle.java`
    with the numbers of the original (the colour, the scale, the life, the velocity, the
    steering, the drag, the clamp, the size, the cell and the full bright light);
  * `BoreClientEffects.dig(...)` (`:119-160`) — the break sound of the dug block with the
    original volume `(pitch+1)/2` and pitch `volume*0.8`, then `particleCount(10)` dig effects,
    the sparkle in one case out of ten;
  * `TileArcaneBore.DIG_EVENT_RANGE = 64` and `TileArcaneBore.inDigEventRange(int,int,int)`
    (`:69`, `:108-110`) replace the seventy blocks of the client target, and the beam uses the
    same range (`BoreClientEffects.java:89-90`);
  * the new client hook `TileArcaneBore.ClientEffects` (`:79-97`) is called by the client tick
    when the dig event arrives (`:673-676`), and `ThaumcraftBoreClient.init()` installs the
    real effect.

### 2.3 The cap of the flux scrubber turned around the wrong axis

* **Oracle** `thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java:45-61` —
  `translateFromOrientation`: the facing zero (down) and one (up) turn around the **X** axis
  by minus and plus ninety degrees, the facing three (south), four (west) and five (east) turn
  around the **Y** axis by one hundred and eighty, plus and minus ninety degrees, and the facing
  two (north) keeps the model as it is.
* **Port (before)** `common/src/main/java/thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java:56-58`
  — `case WEST -> Axis.ZP.rotationDegrees(90)` and `case EAST -> Axis.ZP.rotationDegrees(-90)`:
  the west and the east cap pointed out of the block instead of along the wall.
* **Fix** the new pure rule `TileFluxScrubberRenderer.orientationOf(Direction)`
  (`:73-118`) returns the axis and the angle of the original turn, and the drawing uses it.

### 2.4 The tip of the cap of the flux scrubber bobbed with the wrong phase

* **Oracle** `thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java:38-39` —
  `float q = player.ticksInWorld + f + tile.count; float bob = sin(q/8)*0.075+0.075;` where
  `count` is the counter of the scrubber (oracle
  `thaumcraft/common/tiles/TileFluxScrubber.java:37`, rolled once at `:44-46` **on the client as
  well as on the server** and never written to the NBT).
* **Port (before)** `common/src/main/java/thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java:33`
  — `+ tile.getCharges()`: the phase used the number of the removed flux blocks instead of the
  counter of the original, and the native scrubber had no counter at all.
* **Fix** the new field `count` and the roll `TileFluxScrubber.rollCount(Level)`
  (`common/src/main/java/thaumcraft/common/tiles/TileFluxScrubber.java:37-48`, `:88-93`), the
  client ticker of the stone device (`common/src/main/java/thaumcraft/common/blocks/BlockStoneDevice.java:74-86`)
  and the renderer uses `tile.getCount()`.

## 3. Units closed without a code change

### 3.1 `FXSparkleTrail` is dead code in the original

The only construction site of the original particle is
`thaumcraft/client/ClientProxy.java:887`, inside `ClientProxy.soulTrail`
(`:883-897`). A tree wide scan of the oracle finds **no caller** of `soulTrail` at all: only the
empty declaration of the common proxy (`thaumcraft/common/CommonProxy.java:234`) and the
implementation. The particle of the original is therefore never created in TC4 4.2.3.5, and the
native tree not having it is the exact behaviour. (The same scan is the reason the ledger
already closed `FXSmokeTrail`, the other particle of that method.)

### 3.2 The traveling trunk is drawn in the hand

The original `thaumcraft/client/renderers/item/ItemTrunkSpawnerRenderer.java:34-51` binds
`textures/models/trunk.png` and draws the vanilla chest model
(`ModelChest`, a 14x10x14 body, a 14x5x14 lid and a 2x4x1 latch). The native item model
`common/src/main/resources/assets/thaumcraft/models/item/trunk_spawner.json:1` draws exactly
that geometry (the body from 1,0,1 to 15,10,15, the lid from 1,9,1 to 15,14,15 and the latch
from 7,7,0 to 9,11,1) with the retained `thaumcraft:models/trunk` texture
(`common/src/main/resources/assets/thaumcraft/textures/models/trunk.png`), and the item is the
registered `thaumcraft:trunk_spawner`
(`common/src/main/java/thaumcraft/common/config/ThaumcraftGolems.java:109`). The native item
is therefore the traveling trunk in the hand, as in the original.

## 4. Units that stay open, and why

### 4.1 `FXSpark` — the native sparks do not cover every original site

* The original class is used at `thaumcraft/client/ClientProxy.java:669` (the generic proxy
  helper), `thaumcraft/common/entities/monster/mods/ChampionModBold.java:36`,
  `thaumcraft/common/blocks/BlockCrystal.java:86`, `thaumcraft/common/blocks/BlockAiry.java:299`
  and `thaumcraft/common/blocks/BlockEldritch.java:299`.
* The native tree reproduces the crystal exactly
  (`common/src/main/java/thaumcraft/client/fx/particles/CrystalSparkParticle.java:22-39`, the
  5..9 ticks, the 8x8 sheet, the alpha .8, the flip, the no movement) and the shock trap
  (`common/src/main/java/thaumcraft/client/ThaumcraftShockOrbsClient.java:24-32`, the size
  `0.33+h` and the colour `0.65+rand*0.1/1/1` of the original `BlockAiry:299-302`).
* **Not covered:** the spark of the eldritch lock (oracle `BlockEldritch.java:299-302`, the size
  `0.5`, the colour `0.65+rand*0.1/1/1` and the alpha `0.8`; the native
  `common/src/main/java/thaumcraft/common/blocks/BlockEldritch.java` has no `animateTick` at
  all), the second variant of the shock focus (oracle `BlockAiry:303-304`, the colour
  `0.3-rand*0.1/0/0.5+rand*0.2`), the champion (the native tree has no `ChampionModBold`) and
  the eldritch golem boss (the native tree has no `EntityEldritchGolem`; both are units of
  other families).

### 4.2 `FXSparkle` — the native sparkles do not cover every original site

* The original class is used by the proxy helpers (`ClientProxy.java:650` and `:660`), by
  `ClientProxy.arcLightning` (`:1153`), by `UtilsFX.java:124` and by the blocks and the
  entities that call the proxy: `TileHole` (24 calls, `TileHole.java:142-211`),
  `ItemFocusShock` (two calls, `:166` and `:174`), `EntityFollowingItem:99`,
  `EntityAlumentum:45`, `EntityFrostShard`, `EntityPechBlast:68`, `BlockLifter` and
  `BlockMagicalLeaves`.
* The native tree has a client hook for the following item
  (`common/src/main/java/thaumcraft/common/entities/EntityFollowingItem.java:155`), for the
  alumentum (`EntityAlumentum.java:34`), for the frost shard
  (`EntityFrostShard.java:254`, the `FocusFrostSparkleParticle` reproduces the type six colour
  and the 18 ticks of the original) and for the pech blast (`EntityPechBlast.java:107`).
* **Not covered:** the sparkles around the portable hole (the native
  `common/src/main/java/thaumcraft/common/tiles/TileHole.java` has no client tick and no
  `surroundwithsparkles`), the sparkles of the shock focus
  (`common/src/main/java/thaumcraft/common/items/wands/foci/ItemFocusShock.java` has no client
  hook), the sparkles of the magical leaves and of the lifter (the native
  `common/src/main/java/thaumcraft/common/blocks/BlockMagicalLeaves.java` has no hook and the
  native tree has no `BlockLifter`).

### 4.3 `TileMirrorRenderer` — the fixed function tunnel of the original

The original draws the linked view with `GL11.glTexGen` texture coordinate generation and a
hand written projection matrix (`TileMirrorRenderer.java:52-554`). The native replacement
`common/src/main/java/thaumcraft/client/renderers/models/ModelTunnelField.java:1-46` keeps the
sixteen layers, the rotation `(i*i*4321+i*9)*2`, the fade `1/(16-i+1)`, the two original
textures, the original `Random(31100L)` colour sequence and the original alpha of the outer
layer, but the view dependent texture generation of the original has **no** equivalent in the
1.21.1 shader pipeline, and the additive blend of the inner layers is replaced by the
translucent pass. Everything else of the renderer (the pane of the unlinked and of the linked
mirror, the offset of the unstable mirror, the brightness and the turn of the six facings) is
the original.

### 4.4 `TileThaumatoriumRenderer` — the retained OBJ is not rendered

The original draws `textures/models/thaumatorium.obj` with `textures/models/thaumatorium.png`
(`TileThaumatoriumRenderer.java:44-73`) and the recipe output that cycles every forty ticks
above the machine (`:74-100`). The native tree draws a plain orientable block model
(`common/src/main/resources/assets/thaumcraft/models/block/thaumatorium.json:1`) and its OBJ
loader `common/src/main/java/thaumcraft/client/renderers/models/ModelThaumatorium.java:23-61`
is never referenced. The OBJ is one block wide and two blocks tall (`x` and `y` from -0.5 to
0.5, `z` from 0 to 2), so the native machine (a lower and an upper block,
`common/src/main/java/thaumcraft/common/blocks/BlockThaumatorium.java`) could take it. The
exact pose of the original is:

```java
pose.translate(.5F, 0.0F, .5F);                    // the original glTranslatef(x+0.5, y, z+0.5)
pose.mulPose(Axis.XP.rotationDegrees(-90.0F));     // the original glRotatef(90, -1, 0, 0)
// the original glRotatef of the facing, in the same frame:
//   NORTH 270, SOUTH 90, EAST 180, WEST/DOWN/UP 0 degrees around the Z axis
pose.mulPose(Axis.ZP.rotationDegrees(facingAngle));
```

and the two block models of the machine would have to become `RenderShape.INVISIBLE`, because
the OBJ already covers both blocks. **This was not done in this wave on purpose:** the pose
above was derived by hand, the client cannot be run here, and a machine that is drawn with the
wrong pose is worse than the box that is drawn today. The leader has to check the frames
before the change is accepted.

The cycling preview cannot be reproduced at all: the original shows the output of one of the
*possible* recipes of the machine (`tile.recipeHash`) and the native machine is programmed
with a job list (`TileThaumatorium.java:322-324`) that holds no such set.

### 4.5 `EntityItemGrate` — the 1.7.10 vanilla push out is not readable here

The original overrides the vanilla check of the item entity
(`thaumcraft/common/entities/EntityItemGrate.java:28-36`): the metal device of the fifth and
the sixth metadata counts as a solid block, so the item is pushed out of the grate. The
overridden method is `func_145771_j(double,double,double)`, which the 1.12.2 MCP names call
`Entity.pushOutOfBlocks` (proved by the 1.12.2 `Entity` javadoc, where the same SRG name
`func_145771_j` carries the name `pushOutOfBlocks`), so the semantics of the override are the
original ones. The native tree installs the grate test
(`common/src/main/java/thaumcraft/common/entities/EntityItemGrate.java:49-55`) and pushes the
item out of the grate with its own arithmetic (`:57-89`). The **exact** vanilla arithmetic of
1.7.10 cannot be compared in this environment: there is no readable 1.7.10 vanilla source
here (the oracle only holds the mod), and the ledger already records that.

### 4.6 `PacketFXBlockSparkle` — the transport of the block sparkle

The original packet (`thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java:36-52`) shows
a sparkle burst at a block with a colour. The native tree has the **client** half of that
effect twice: `common/src/main/java/thaumcraft/client/fx/particles/GrowthLampParticle.java:21-33`
reproduces `ClientProxy.blockSparkle` and `FXGeneric` for the growth lamp (the green
`0x40ff40`, the cells 112..120, the delay, the size, the drag and the half alpha of the first
and the last frame), and `InfusionParticle.Kind.BURST` (`:104-113`) does the same for the
infusion. The **server** half of the other senders of the original packet is missing:

| original sender | colour | native |
|---|---|---|
| `TileFluxScrubber.java:100` | `0xDD00FF` | missing |
| `TileArcaneLampGrowth.java:106` | `0x40FF40` | `GrowthLampFxNetworking` |
| `TileEldritchLock.java:130` | `0x400040` | missing |
| `ItemFocusWarding.java:137,151` | `16556032` | missing |
| `WandManager.java:244,245,420,795` | `-9999` | missing |
| `EntityEldritchWarden.java:234` | `0x800080` | missing |
| `ServerTickEventsFML.java:154` | `0xC0C0FF` | missing |

The transport needs a payload in
`common/src/main/java/thaumcraft/common/lib/network/**`, which **this wave must not touch**
(it belongs to the wave one `crashhunt` agent). The exact change is written in §6.

## 5. Tests (authored, NOT executed)

| file | methods | what it protects |
|---|---|---|
| `common/src/test/java/thaumcraft/entities/SpawnerEggParityTest.java` | 3 | the creative egg is never spent, the survival egg is |
| `common/src/test/java/thaumcraft/bore/BoreDigEventParityTest.java` | 5 | the sixty four blocks of the dig event, the life and the size of the dig sparkle, the render distance |
| `common/src/test/java/thaumcraft/client/renderers/tile/FluxScrubberRendererParityTest.java` | 4 | the six turns of the cap of the scrubber, the amplitude of its bob |

No test constructs a `Block`: every rule under test is a pure static function
(`ItemSpawnerEgg.spendsStack(boolean)`, `TileArcaneBore.inDigEventRange(int,int,int)`,
`BoreDigSparkleParticle.lifetimeFor(double,int)`,
`BoreDigSparkleParticle.sizeFor(float,int)`,
`TileFluxScrubberRenderer.orientationOf(Direction)`,
`TileFluxScrubberRenderer.bobFor(float)`).

## 6. What the crashhunt agent (or the leader) has to change for `PacketFXBlockSparkle`

A new payload in `common/src/main/java/thaumcraft/common/lib/network/`:

```java
public record BlockSparklePayload(ResourceLocation dimension, BlockPos target, int rgb)
        implements CustomPacketPayload {
    public static final Type<BlockSparklePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "block_sparkle_fx"));
    // the stream codec writes the dimension, the target and the rgb
}
```

and a sender in the same package that writes it to the players within the thirty two blocks
of the effect (the `TargetPoint` of the original):

```java
public static void sendBlockSparkle(ServerLevel level, BlockPos target, int rgb) {
    var payload = new BlockSparklePayload(level.dimension().location(), target, rgb);
    for (ServerPlayer player : level.players())
        if (player.distanceToSqr(Vec3.atCenterOf(target)) <= 32 * 32)
            NetworkManager.sendToPlayer(player, payload);
}
```

The client half is `GrowthLampParticle` with the colour of the payload instead of the green
`0x40ff40` (the rule of the original `ClientProxy.blockSparkle` is already implemented there:
the colour `c` is read as three bytes, jittered by `-0.2+rand*0.4`, and `c == -9999` is the
random grey `0.33+rand*0.67`). The four senders above then call
`BlockFxNetworking.sendBlockSparkle(level, pos, colour)`.

## 7. Acceptance criteria the leader has to check on the real client

1. **Bore dig sparkle** — build a bore with an excavation focus and a pickaxe, power it and
   let it dig into stone. On every dig the client must show, at the dug block, a burst of
   block particles and, in one case out of ten, a small green spark (red `0.2`, green
   `0.6..0.9`, blue `0.2`) that flies from the dug block towards the block the bore digs into
   and disappears there. The block break sound of the dug block must be heard once per dig.
2. **Bore dig range** — the dig effect must not appear for a bore that digs further than
   sixty four blocks away; a bore that digs at sixty four blocks must still show it.
3. **Flux scrubber cap** — a flux scrubber that faces west or east must show its cap pointing
   along the wall (the same direction as the north facing), not out of the block; the tip of
   the cap must bob gently (at most a tenth and a half of a block) and the bob must not jump
   when the scrubber removes a flux block.
4. **Creative spawn egg** — in the creative mode, right clicking with a spawn egg must spawn
   the mob and must **not** consume the egg; in survival the egg must be consumed.
5. **Traveling trunk in the hand** — the `trunk_spawner` item must show the three dimensional
   trunk (the chest shape with the trunk texture) in the hand, in the inventory and in the gui.
6. **Mirror** — a linked mirror must still show the receding tunnel of sixteen layers; the
   difference to the original is the view dependent texture, which the port cannot reproduce.

## 8. Files changed

* `common/src/main/java/thaumcraft/client/BoreClientEffects.java` — the dig effect, the range.
* `common/src/main/java/thaumcraft/client/fx/particles/BoreDigSparkleParticle.java` — **new**.
* `common/src/main/java/thaumcraft/client/ThaumcraftBoreClient.java` — the client hook.
* `common/src/main/java/thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java` — the
  turn and the bob.
* `common/src/main/java/thaumcraft/common/blocks/BlockStoneDevice.java` — the client ticker of
  the scrubber.
* `common/src/main/java/thaumcraft/common/entities/ItemSpawnerEgg.java` — the creative rule.
* `common/src/main/java/thaumcraft/common/tiles/TileArcaneBore.java` — the range, the client
  effects.
* `common/src/main/java/thaumcraft/common/tiles/TileFluxScrubber.java` — the counter.
* `common/src/test/java/thaumcraft/entities/SpawnerEggParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/bore/BoreDigEventParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/client/renderers/tile/FluxScrubberRendererParityTest.java` —
  **new**.
* `docs/porting/scope-ledger.json` — the verdicts of the thirteen units.

## 9. What was verified in this wave and what was not

* **Verified here (no Gradle needed):**
  * `python3 scripts/porting/update_scope_ledger.py --check --original-root /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`
    → `{"mode": "check", ..., "stale_review_fingerprints": 0}` (the ledger is fresh and the
    fingerprints of the thirteen units still match the oracle).
  * the ledger diff touches **only** the thirteen units of this lane and leaves `counts`
    untouched (`git diff` of the JSON: 13 changed rows, `counts` identical).
  * `javac -proc:none` over the eleven changed and new files reports **no syntax error**
    (only the expected "cannot find symbol" of a classpath free parse).
  * the 1.21.1 API of every new call was read out of the merged jar
    (`~/.gradle/caches/fabric-loom/.../minecraft-merged-1.21.1...jar`): `EntityBlock.getTicker`
    is the `(Level, BlockState, BlockEntityType)` method the native blocks override, and
    `ClientLevel.tickEntities` calls `Level.tickBlockEntities`, so the client ticker of the
    flux scrubber really runs on the client.
* **NOT verified here (the wave forbids Gradle):** the build, the JUnit suite, the game test
  suite and the client run. The three test classes are **authored but not executed**; the
  client acceptance criteria of §7 are **not inspected**.

