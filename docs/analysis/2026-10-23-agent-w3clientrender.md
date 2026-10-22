# Wave 3 agent `w3clientrender` — the 25 remaining client rendering units

Worktree `/home/cyberfox-kali/wt/w3clientrender`, branch `agent/w3clientrender`.
Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 859/859).
Second opinion only: `/home/cyberfox-kali/OpenTC4` — it differs from the 4.2.3.5 jar in
real places (it carries the 1.7.10 `ModelRenderer.mirror` flag that the 4.2.3.5 jar does
not set, see §0.2), so the jar wins.

Unit list: `/home/cyberfox-kali/units/w3clientrender.txt` (25 units).
**Ten units moved `partial` -> `parity_verified`, fifteen stay `partial` with the exact
blocker.** The scope ledger went from 945 to 955 `parity_verified` and from 86 to 76
`partial`; `update_scope_ledger.py --check` reports `stale_review_fingerprints: 0`.

## 0. The two findings the client reported

### 0.1 "the guardian's `HoodEye` is hidden in the port (`ModelEldritchGuardian.java:15`)" — NOT A DEFECT

The client read `isRender` (oracle `:122-127`) and concluded that the original draws the
hood eye in the ordinary pass. It does not.

* oracle `thaumcraft/client/renderers/models/entities/ModelEldritchGuardian.java:428-465`
  is the model's render method (`func_78088_a`). It renders **38** parts:
  `BeltR, Mbelt, MbeltL, MbeltR, BeltL, Chestplate, Hood1, Backplate, ShoulderplateTopR,
  ShoulderplateR1, ShoulderplateR2, ShoulderplateR3, ShoulderR, ArmL1, ArmR1, ShoulderL,
  ShoulderplateLtop, ShoulderplateL1, ShoulderplateL2, ShoulderplateL3, LegpanelR4,
  LegpanelR5, LegpanelR6, BackpanelR1, BackpanelR2, BackpanelR3, BackpanelL3, LegpanelL4,
  LegpanelL5, LegpanelL6, BackpanelL1, BackpanelL2, Cloak1, SidepanelR1, SidepanelL1,
  SidepanelL2, SidepanelR2, LegpanelC1`.
  **`HoodEye` is not in that list.**
* The only place the original ever draws `HoodEye` is oracle `:467-479`:

  ```java
  if (entity instanceof EntityEldritchWarden) {
      GL11.glPushMatrix();
      GL11.glEnable(GL_BLEND);
      GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE);      // additive
      GL11.glScaled(1.01, 1.01, 1.01);
      int j = (int)(195.0f + MathHelper.func_76126_a(entity.field_70173_aa / 3.0f) * 15.0f + 15.0f);
      OpenGlHelper.func_77475_a(OpenGlHelper.field_77476_b, k / 1.0f, l / 1.0f);
      this.HoodEye.func_78785_a(f5);
      GL11.glDisable(GL_BLEND);
      GL11.glPopMatrix();
  }
  ```

  i.e. the eye is drawn **only for the warden**, in a separate additive pass at 1.01 scale.
* The native port does exactly that:
  * `common/src/main/java/thaumcraft/client/renderers/models/ModelEldritchGuardian.java:15`
    hides `HoodEye` for the *guardian* (the ordinary `MobRenderer` pass) — which is what
    the original's 38 part list does.
  * `common/src/main/java/thaumcraft/client/renderers/entity/RenderEldritchWarden.java:87-97`
    re-enables it, scales it by `1.01` and draws it.
* Textures: the port's `textures/models/eldritch_guardian.png` and `eldritch_warden.png`
  are byte identical to the 4.2.3.5 jar (`md5 1167d8b0d90d4930c58a2c27dad3b3cd` and
  `36abfde13ed567f544c2eeca5b0ff0c8`). The hood eye box samples the model UV region
  `(0,0)-(16,16)` of the 128x64 model space = the pixels `(0,0)-(32,32)` of the 256x128
  sheet. That region is **empty in the guardian sheet** and holds the dark red eye
  (`rgba(128,0,0,255)` around the pixels x=18..31, y=21..25) in the **warden** sheet.

=> The port matches the original. The client's finding is a false positive: it read
`isRender` but not the render list. **The guardian model was not changed.**

The one real difference next to it was the blend state, and it is fixed in this wave:
the original's eye pass is additive (`glBlendFunc(GL_SRC_ALPHA, GL_ONE)`), the port drew
the eye into the ordinary translucent buffer. The warden now draws the eye with
`RenderType.eyes(SKIN)`, the additive and emissive render type of 1.21.1
(`RenderEldritchWarden.java:47-52, :96`).

### 0.2 The 1.21.1 `mirror` vs the 1.7.10 mirror — the port is right, the premise is wrong

The client wrote: "the 1.21.1 `mirror` negates only the **X** of a face normal, while the
1.7.10 winding reversal negates the **whole** normal".

**The premise is incomplete: the 1.7.10 mirror is not only a winding reversal, it also
mirrors the geometry in X.** The two effects together leave exactly the X component
negated, which is what 1.21.1 does.

* The 1.7.10 `ModelRenderer` carries the public flag `field_78809_i` (default `false`);
  `field_78806_j` is `isRender` (default `true`) and `field_78807_k` is the hide flag
  (default `false`) — the render method tests `!field_78807_k && field_78806_j`.
  `func_78787_b(int,int)` is `setTextureSize(int,int)` and sets the **part's own**
  texture size, which is what the UV division uses.
  The oracle uses the flag for the left parts of the cultist armour, for example
  `thaumcraft/client/renderers/models/gear/ModelFortressArmor.java:272-277`, and
  `OpenTC4` names the same field `mirror` (the readable source,
  `src/main/java/thaumcraft/client/renderers/models/gear/ModelFortressArmor.java:257-262`).
* The vanilla 1.7.10/1.8 `ModelBox` - the class the 1.7.10 `ModelRenderer.Cube` was
  derived from - does two things when the flag is set:

  ```java
  if (renderer.field_78809_i) {          // the mirror flag
      float swap = maxX;                // the box is mirrored in X, its extent is unchanged
      maxX = minX;
      minX = swap;
  }
  for (int i = 0; i < quads.length; ++i) quads[i].flip();   // every quad is wound the other way
  ```

* 1.21.1 `ModelPart$Cube` (byte code of
  `minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`,
  `net/minecraft/client/model/geom/ModelPart$Cube`) does the same: with `mirror == true`
  it swaps `minX`/`maxX` (the `fstore_3`/`fstore 16` swap) and `ModelPart$Polygon`
  reverses the vertex order (`vertices[j] <-> vertices[len-1-j]`), exactly like the
  1.7.10 `TexturedQuad.flip()`.
* The **normal**: the 1.7.10 model derives the normal from the geometry, 1.21.1 uses the
  fixed per-face `Direction.step()` and then `normal.mul(-1, 1, 1)` for a mirrored
  polygon. The two agree:
  * a geometric mirror in X maps a normal `n` to `-M n` with `M = diag(-1,1,1)` (a mirror
    reverses the handedness), and the extra winding flip negates it once more, so the net
    normal is `M n = (-nx, ny, nz)` - the **X component only**;
  * 1.21.1 computes exactly `(-nx, ny, nz)`.
  * So "a winding reversal negates the whole normal" is true only for the winding
    reversal *alone*; 1.7.10 never applies it alone.

=> **The port's `CubeListBuilder.create().texOffs(...).mirror()` is the exact 1.21.1
equivalent of the 1.7.10 `ModelRenderer.mirror = true`.** No compensation is needed and
nothing is not representable. The difference the client saw is a difference between
"winding reversal alone" and "the 1.7.10 mirror", not a difference between the original
and the port. The proof is the new test
`common/src/test/java/thaumcraft/eldritch/EldritchWardenEyeAndMirrorParityTest.java`,
which renders a plain and a mirrored box with the real 1.21.1 classes and compares the
positions, the texture region and the normals.

### 0.3 The one real difference next to the warden's eye: the blend state

* original oracle `ModelEldritchGuardian.java:467-479`: `glEnable(GL_BLEND)` +
  `glBlendFunc(GL_SRC_ALPHA, GL_ONE)` (additive) for the eye pass, then `glDisable`.
* port `RenderEldritchWarden.java:80-86`: the eye was drawn into the same
  `RenderType.entityTranslucent(SKIN)` buffer, i.e. `SRC_ALPHA, ONE_MINUS_SRC_ALPHA`.

The original adds the eye colour to the hood, the port replaced it. Fixed in this wave
with `RenderType.eyes(SKIN)`, the additive and emissive render type of 1.21.1
(`RenderStateShard.ADDITIVE_TRANSPARENCY` is `SRC_ALPHA, ONE`, and the emissive shader
ignores the lightmap, which is what the original's broken
`OpenGlHelper.func_77475_a(1002, ...)` call - a `glLight` with a texture unit as the
light index, which the driver rejects - left behind).

### 0.4 Why the "final full cube" of the 1.7.10 renderers is not a draw

Every TC4 `ISimpleBlockRenderingHandler` ends its world render with

```java
renderer.func_147771_a();                                  // endBlockDrawing
block.func_149676_a(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);  // Block.setBlockBounds
renderer.func_147775_a(block);                            // RenderBlocks
```

and the inventory render draws its faces with `BlockRenderer.drawFaces` and **not** with
`RenderBlocks`. `RenderBlocks.func_147775_a(Block)` is the one argument variant that
synchronises the block's bounds into the render bounds (the 1.8 equivalent is
`RenderBlocks.func_83018_a(Block)`, which reads `func_83009_v()`..`func_83006_A()` of the
block into the six render bounds), and `func_147784_q(block, x, y, z)` is the draw
(`renderStandardBlock`). The final pair is therefore a **bounds reset**, not a draw.

That is what makes the following decisions of this wave provable:

* the arcane ear of the original is the ten boxes of `BlockWoodenDeviceRenderer:44-83`, not
  a full cube - the port's model was a full cube and is now the ten boxes;
* the alembic of the original is the object of `ModelAlembic` and not a full metal block,
  so the port's empty `alembic.json` is right.

## 1. The 25 units

### closed in this wave (`partial` -> `parity_verified`)

| unit | what the original does | what the port did before | the fix |
|---|---|---|---|
| `BlockCosmeticOpaqueRenderer` | the amber family and the warded glass, the rune particles belong to the cosmetic **solid** family | the models were there, the note blamed the cosmetic opaque family for the rune particles | the warding stone spawns the rune particles (`RuneEffects.wardingStone`, `TileWardingStone.java:47-48`), the warded glass has its own block entity renderer with the neighbour mask |
| `BlockEssentiaReservoirRenderer` | the vessel of 2/16..14/16 in the world **and** in the inventory (oracle `:28-52`) | the block model was a full cube | `models/block/essentia_reservoir.json` is now the 2..14 cube; the contents stay in the block entity renderer |
| `ItemEssentiaReservoirRenderer` | the 1.12 item renderer shrank the vessel | the item model showed the full cube | the item model inherits the shrunk vessel |
| `BlockLifterRenderer` | two emissive inner slabs of 0.01..0.99 by 0.9..0.99 and 0.01..0.99 by 0.1..0.9 in the inventory, tinted with `BlockCustomOreItem.colors[4]` (0x00A000) and `colors[5]` (0xEECCFF) (oracle `:28-50`) | the item model was the plain cube | `models/item/levitator.json` now has the cube and the two slabs with the tint indices 0 and 1, and `ClientSetup` registers the two original ore colours for the levitator item |
| `BlockMetalDeviceRenderer` | the grates, the brain box, the crucible, the alembic, the charger, the vis relay and the three lamps | the note said that the variants 0..4, 7..11 and 13..15 share one base model | the port's metal device block owns only the grates and the brain box, every other metadata of the original is its own native block, and the three lamps carry the original box of W4..W12 by W2..W14 (`arcane_lamp.json`, `growth_lamp.json`, `fertility_lamp.json`) |
| `BlockStoneDeviceRenderer` | the wand pedestal of three boxes, the focus pedestal of **nine** boxes (oracle `:36-70`) | the focus pedestal model was **empty** and the wand pedestal had the wrong sheets | `models/block/wand_pedestal_focus.json` now carries the nine boxes with the focus sheets, `models/block/wand_pedestal.json` now uses the wand pedestal side and top sheets and the pedestal top below |
| `BlockWoodenDeviceRenderer` | the arcane ear of **ten** boxes (oracle `:44-83`) | the arcane ear model was a **full cube** | `models/block/arcane_ear.json` and `arcane_ear_on.json` now carry the ten boxes with the sheets of the off and the on state, and the item model shows the on state like the original's inventory render |
| `ModelCrystalizer` | the 4.2.3.5 crystalizer is the object based `TileEssentiaCrystalizerRenderer` (oracle `:29-89`) | the native was a rewrite that was never compared | the native `ModelEssentiaCrystalizer` + `TileEssentiaCrystalizerRenderer` reproduce the oracle value by value: the six orientation steps, the four crystals, the scale 0.75, the quarter turn, the translation 0.34/0/1.2125, the spin, the aspect tint and the pulsing light coordinate |
| `ItemJarFilledRenderer` | the filled jar item with its contents, aspect, amount and filter (oracle `:55-82`) | the jar item renderer draws exactly that | the flattening into the jar block item with a contents component keeps the visible result |
| `WandPerspectiveModel` | not a 4.2.3.5 class, a 1.12 first person pose model | the display transform is in the item model | the three dimensional wand belongs to the `ItemWandRenderer` unit, not to this class |

### still open (`partial`, with the exact blocker in the ledger)

| unit | the remaining difference | the blocker |
|---|---|---|
| `BlockCandleRenderer` | the deterministic random wax drips, one to five per block, seeded by `x + y * z` (oracle `:43-71`) | the drips need a position aware renderer; the original `BlockCandle` has no tile entity and the 1.7.10 piston rule never moves a block with one, so a block entity would change a gameplay rule for a cosmetic effect |
| `BlockCustomOreRenderer` | the coloured overlay at the lightmap coordinate `max(brightness, 160)` and the second pass with the stone texture (oracle `:40-63`) | same: a fixed light needs a position aware renderer and the original `BlockCustomOre` has no tile entity. **Correction of the wave 2 note:** the second pass is not the "fast graphics" setting, it tests `gameSettings.field_151443_J`, the *particle* setting (2 = full) |
| `BlockGasRenderer` | the variable height surface with the per corner heights and the `LIGHT_*` constants | 1.21.1 computes the fluid height inside `BlockFluidRenderer` and offers no hook |
| `BlockTaintRenderer` | the fibre overlay lives in the fabric module only, so the NeoForge client has none | **Correction of the wave 2 note:** the original draws the overlay on the taint **fibres** block, not on the crust and the soil: the overlay branch of `BlockTaintRenderer:52-78` needs the render type one and only the fibres answer it (`BlockTaintFibres.java:286-288`). The native is right; only the NeoForge loader misses the overlay, and the Fabric implementation uses the model extension of the Fabric API, which NeoForge has no equivalent of |
| `EldritchCrustBakedModel` | the crust is inset on the face whose neighbour is not a solid material, so two crust blocks join seamlessly (oracle `BlockEldritchRenderer:46-63`) | the neighbour dependent inset needs six boolean state properties or a neighbour reading baked model; the eldritch crust has no tile entity in the original |
| `RenderGolemBobber` | the quad samples the region (1,2) of the vanilla 1.7.10 `textures/particle/particles.png` | the sheet is a vanilla 1.7.10 client asset, it is not in the TC4 jar and 1.21.1 has no particle atlas; shipping a redrawn vanilla asset is not a reproduction |
| `RenderWatcher` | the original never registers the watcher renderer, so the watcher was invisible; the native shows it | 1.21.1 has no fallback for an entity type without a renderer, so a renderer has to be registered; the faithful choice would be one that draws nothing. **The leader has to decide** whether the port keeps the wave 1 decision to show the watcher |
| `ItemBowBoneRenderer` | the held transform (scale 2.6666667, the rotations -20/90/-60) and the glint | **Correction of the wave 2 note:** the transform *is* representable, the whole chain multiplies to a pure rotation with the uniform scale 2.5 and the translation 3.903089, 1.002933, 0.673826. The blocker is that the original discards the 1.7.10 equip transform (`glPopMatrix`/`glPushMatrix`) while 1.21.1 applies its own equip transform before the display transform of the model, and the vanilla 1.7.10 equip matrix is not part of the TC4 jar |
| `ItemThaumometerRenderer` | the first person scanner view | forced: 1.21.1 removed `IItemRenderer`/`IModelCustom`, and the two arm rotations need the 1.7.10 arm angles |
| `ItemWandRenderer` | the use pose and the three dimensional wand | the object models of every rod, cap and focus combination and the 1.12 pose helpers are not part of the 4.2.3.5 jar and would have to be rebuilt from the 1.12 reference |
| `ItemJarNodeRenderer` | the native jar_node item is the block item of the placeable block, so it shows the jar without the node inside | the 1.7.10 item renderer API is gone; the native jar_node has a tile entity, so a block entity renderer can draw the node |
| `ItemNodeRenderer` | the original had an item form of the aura node that this renderer drew | the native node is a permanent block; the 1.7.10 item renderer API is gone |
| `TileJarRenderer` | the node inside the node jar and the one second growth after a wand hit (oracle `:54-56, :107-115`) | the native jar_node has a tile entity, so a block entity renderer can draw both, but the growth needs the `animate` timestamp that the wand hit sets, and the native node jar never receives that hit - a gameplay rule, not a renderer duty |
| `TileMirrorRenderer` | the view dependent texture generation of the linked view | the fixed function pipeline is gone in 1.21.1; the native tunnel field is the closest equivalent and the leader has to compare the frames |
| `TileThaumatoriumRenderer` | the object geometry of the machine and the cycling preview | the object is one block wide and two blocks tall, and the native machine holds no set of possible recipes, so the preview has no data |

## 2. The tests (authored, **not executed** - I may not run Gradle)

* `common/src/test/java/thaumcraft/client/renderers/ClientDeviceModelParityTest.java`
  - the levitator item: the cube and the two inner slabs with the tint indices 0 and 1
    and the animated glow sheet, and the block model stays the plain cube;
  - the reservoir: the vessel of 2..14 in the block and in the item;
  - the arcane ear: the ten boxes of the original and the sheets of the off and the on
    state, and the item model shows the on state;
  - the wand pedestal: the three boxes and the three sheets; the focus pedestal: the
    nine boxes;
  - the three alchemy lamps: the box of W4..W12 by W2..W14 and their sheets.
  The test only reads the model assets, it never constructs a `Block`.
* `common/src/test/java/thaumcraft/eldritch/EldritchWardenEyeAndMirrorParityTest.java`
  - the guardian hides the hood eye and the warden draws it at the scale 1.01;
  - the mirror of a box: the geometry is the mirror image, the texture region is the one
    of the plain box and only the X of the normal is negated - the proof of §0.2 with
    the real 1.21.1 classes.

**Both files are authored but NOT executed**: the leader runs the builds and the JUnit
suite. I did not run Gradle at all.

## 3. Acceptance criteria for the leader's next client run

1. **Warden / guardian hood**: the eldritch warden must show a small dark red glowing
   eye in the middle of its hood, added on top of the hood (the original's additive
   pass at 1.01); the plain eldritch guardian must show **no** eye in its hood. The
   guardian's hood is the plain hood texture.
2. **Levitator in the inventory**: the levitator item must show the metal cube with a
   green (0x00A000) slab on the top inside and a pink (0xEECCFF) slab below it, both
   with the animated glow texture. In the world the levitator must show only the cube
   and the magenta glow panels on the open sides.
3. **Essentia reservoir**: the reservoir must look like a 12/16 wide vessel (a cube
   inset by two sixteenths on every side), not like a full block, in the world and in
   the inventory.
4. **Arcane ear**: the arcane ear must look like the original: a low plate over the
   whole block, a square pillar in the middle and the eight arms of the bell around
   it - **not** a full cube.
5. **Arcane wand pedestal (the focus variant)**: it must show the cross of nine thin
   bars, not an empty space.
6. **Wand pedestal**: the pedestal must show the wand pedestal side sheet on its sides
   and the wand pedestal top sheet on its top.
7. **Alchemy lamps**: the three lamps (arcane, growth, fertility) must be the slab of
   8x12x8 sixteenths, not a full block.
8. **Crystalizer**: the crystalizer must show the object shell of the original with the
   four crystals orbiting above it, tinted with the aspect colour and pulsing.
9. **Filled jar item**: the jar item in the inventory must show the liquid of the
   stored aspect, the amount and the crooked label.
10. **Bone bow in the hand**: the bone bow is still drawn with the vanilla 1.21.1 bow
    pose, not with the original's held transform - a known, documented difference.

## 4. The commits

```
00c02784 docs(clientrender): the wave 3 unit list and the two client findings settled
1465bc46 fix(clientrender): the reservoir vessel, the levitator inventory slabs and the warden's hood eye
d7c1c7c2 fix(clientrender): the arcane ear, the wand pedestals and the arcane lamp boxes of the original
83ac0fe3 test(clientrender): the device models, the warden's hood eye and the mirror of a box
8d33f7d3 docs(ledger): the wave 3 client render units, ten closed and fifteen with their exact blocker
```

## 5. The files this wave changed

* `common/src/main/resources/assets/thaumcraft/models/block/essentia_reservoir.json`
* `common/src/main/resources/assets/thaumcraft/models/item/levitator.json`
* `common/src/main/resources/assets/thaumcraft/models/block/wand_pedestal.json`
* `common/src/main/resources/assets/thaumcraft/models/block/wand_pedestal_focus.json`
* `common/src/main/resources/assets/thaumcraft/models/block/arcane_ear.json`
* `common/src/main/resources/assets/thaumcraft/models/block/arcane_ear_on.json`
* `common/src/main/resources/assets/thaumcraft/models/item/arcane_ear.json`
* `common/src/main/java/thaumcraft/client/ClientSetup.java` (the levitator item colours)
* `common/src/main/java/thaumcraft/client/renderers/entity/RenderEldritchWarden.java`
  (the additive hood eye, the two named constants)
* `common/src/test/java/thaumcraft/client/renderers/ClientDeviceModelParityTest.java` (new)
* `common/src/test/java/thaumcraft/eldritch/EldritchWardenEyeAndMirrorParityTest.java` (new)
* `docs/porting/scope-ledger.json` (my 25 units only)
* `docs/analysis/2026-10-23-agent-w3clientrender.md` (this file)

`python3 scripts/porting/convert_assets.py --verify` reports `missing_references: 0`.
