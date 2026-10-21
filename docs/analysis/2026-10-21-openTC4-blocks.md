# TC4 4.2.3.5 block families vs the 1.21.1 port (audit 2026-10-21)

Task: check the original block families of TC4 4.2.3.5 against the 1.21.1 port of
this repository, with the decompiled oracle as the only authority and the readable
OpenTC4 checkout as the reading aid for the vanilla names.

| role | path |
|---|---|
| oracle (truth) | `/home/Acak/work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/blocks`, `.../common/config`, `.../common/tiles` |
| readable 1.7.10 | `/home/Acak/work/opentc4/src/main/java/thaumcraft/common/...` (commit 80aac29) |
| port | `/home/Acak/tcw/ocblocks/common/src/main/java/thaumcraft/common/...` (branch `port/ocblocks`) |
| name resolver | `python3 -B scripts/porting/resolve_srg.py field_76367_g func_149712_f` (MCP 1.7.10 in `/tmp/mcp-stable`) |

Scope of this audit: `BlockCosmeticSolid`, `BlockCosmeticOpaque`, `BlockCosmeticWoodSlab`,
`BlockCosmeticStoneSlab`, `BlockCosmeticDoubleSlab`, `BlockCosmeticStairs`, `BlockMetalDevice`,
`BlockWoodenDevice`, `BlockStoneDevice`, `BlockEldritch`, `BlockEldritchNothing`, `BlockAiry`,
`BlockArcaneDoor`, `BlockWarded`, `BlockMirror`, `BlockCustomOre`, `BlockLifter`, `BlockTube`,
`BlockCrystal` and the warded glass (the original metadata two of `BlockCosmeticOpaque`), plus
the stateful tiles `TileGrate` and `TileArcanePressurePlate`.

---

## 1. The 1.7.10 semantics that decide the numbers

The values of a 1.7.10 block are not the numbers a 1.21.1 block takes, so every rule
below was verified against the vanilla 1.7.10 client jar (`/tmp/opencode/client-1.7.10.jar`,
`net.minecraft.block.Block` = `aji`) and the Forge 1.7.10 patch of the same file
(`forgepatches.zip` -> `net/minecraft/block/Block.java.patch`).

| original call | 1.7.10 effect | 1.21.1 equivalent |
|---|---|---|
| `func_149752_b(x)` = `setResistance` | stores `x * 3.0F` in `field_149781_w` | - |
| `func_149638_a` = `getExplosionResistance` | returns `field_149781_w / 5.0F` | `Block.getExplosionResistance()` returns `Properties.explosionResistance` unchanged |
| `func_149711_c(h)` = `setHardness` | stores `h` and raises the resistance to at least `h * 5.0F` | `Properties.strength(h, r)` -> `destroyTime(h).explosionResistance(r)` |
| **effective blast resistance** | **`0.6 * setterValue`**, and `max(0.6 * setterValue, hardness)` when `setHardness` is the later call | the value that is put into `strength(...)` |
| `func_149715_a(f)` = `setLightLevel` | stores `(int)(15.0F * f)` in `field_149784_t` | `Properties.lightLevel(state -> n)` |
| `func_149722_s()` = `setBlockUnbreakable` | only `setHardness(-1.0F)`; it does **not** touch the resistance | `strength(-1.0F, r)` |
| `func_149713_g(n)` = `setLightOpacity` | writes `field_149786_r`, which the 1.7.10 `Block` constructor sets to `255` for an opaque cube and to `0` for a non opaque one | `getLightBlock` (0..15) |

The 1.7.10 light engine of Forge 1.7.10 uses the four argument
`getLightOpacity(IBlockAccess, int, int, int)`, and that Forge method forwards to the
no argument getter, i.e. to the `lightOpacity` field of the block
(`forgepatches.zip` -> `net/minecraft/world/World.java.patch:581` and `:593`). The
`lightOpacity` of a block that does not override `isOpaqueCube` (`func_149662_c`) is `0`,
so the warded glass of the original does **not** block light, while the amber block and
the amber brick block three of the fifteen light levels
(`BlockCosmeticOpaque:163-169`). The port reproduces that (`BlockAmber:14` and
`BlockCosmeticOpaque:61-63`).

The 1.21.1 resistance of every family was therefore compared with `0.6 * setterValue`,
and the values that the original returns from its **own** getter (the "absolute" values
`999`, `30`, `100`, `50`, `200`, `Float.MAX_VALUE`) are used unscaled, because the
explosion reads them verbatim.

---

## 2. The values of every metadata, oracle against port

### 2.1 `BlockCosmeticSolid` (the sixteen decorative stones)

Oracle: `func_149711_c(2.0f)`, `func_149752_b(10.0f)` (lines 66-67), the per metadata
hardness in `func_149712_f` (213-225), the blast resistance in `getExplosionResistance`
(241-253) and the light in `getLightValue` (227-239).

| metadata | oracle hardness | oracle blast resistance | oracle light | port |
|---|---|---|---|---|
| 0, 1, 8 | 30 | 999 (absolute) | 0 | `cosmetic_solid_tough`, 30 / 999 / 0 |
| 2 (travel stone) | 2 | 6 | 9 | `cosmetic_solid`, 2 / 6 / 9 |
| 3 (warding stone) | 2 | 6 | 0 | `cosmetic_solid`, 2 / 6 / 0 |
| 4 (thaumium block) | 4 | 20 (absolute) | 0 | `cosmetic_solid_thaumium`, 4 / 20 / 0 |
| 5 (tallow block) | 2 | 6 | 0 | `cosmetic_solid`, 2 / 6 / 0 |
| 6 (pedestal top) | 4 | 20 | 0 | `cosmetic_solid_thaumium`, 4 / 20 / 0 |
| 7 (arcane stone) | 4 | 20 | 0 | `cosmetic_solid_thaumium`, 4 / 20 / 0 |
| 9, 10 (golem stone) | 2 | 6 | 0 | `cosmetic_solid`, 2 / 6 / 0 |
| 11..15 | 2 | 6 | 14 -> 4 for 14 | `cosmetic_solid`, 2 / 6 / 4 for 14 |

**Verdict: the hardness, the blast resistance and the light level of the cosmetic solid
family are correct in the port.** The group table is
`EldritchBlockRules.COSMETIC_GROUPS` (line 278) and the registration is
`ThaumcraftEldritchBlocks:138-145`.

### 2.2 `BlockCosmeticOpaque` (amber block, amber brick, warded glass)

Oracle: `func_149752_b(5.0f)`, `func_149711_c(1.5f)` (66-67) -> effective 3.0; the warded
glass has the hardness five (or none at all with the warded stone option) in
`func_149712_f` (253-259) and the absolute blast resistance 999 in
`getExplosionResistance` (261-266).

**Verdict: correct** (`COSMETIC_OPAQUE_HARDNESS` 1.5, `COSMETIC_OPAQUE_RESISTANCE` 3.0,
`COSMETIC_OPAQUE_WARDED_HARDNESS` 5.0, `COSMETIC_OPAQUE_WARDED_RESISTANCE` 999.0).
The amber block and the amber brick are separate native blocks in the port
(`ThaumcraftLamps:29-32`), the warded glass is the own block
`cosmetic_opaque_warded` with the `warded_glass` item.

### 2.3 The cosmetic slabs, the double slabs and the stairs

Oracle `ConfigBlocks:298-301`: `func_149711_c(2.0f).func_149752_b(5.0f)` for the wooden
slabs and the wooden double slabs (-> effective 3.0) and
`func_149711_c(2.0f).func_149752_b(10.0f)` for the stone slabs and the stone double slabs
 (-> effective 6.0). The double slab drops the single slab, two wooden ones and one stone
one (`BlockCosmeticWoodSlab:56-58`, `BlockCosmeticStoneSlab:56-58`).

The stairs copy the texture of another block, and the 1.7.10 `BlockStairs` constructor
takes the hardness and the blast resistance of **that** block
(`javap -c ans.class`, the obfuscated `net.minecraft.block.BlockStairs` of the 1.7.10
client jar: `setHardness(ref.blockHardness)` and
`setResistance(ref.blockResistance / 3.0F)`, so the effective resistance of the stair is
the effective resistance of the reference block). The four stairs of
`ConfigBlocks:294-297` therefore have:

| stair | reference | hardness | effective blast resistance |
|---|---|---|---|
| `blockStairsArcaneStone` | the cosmetic solid, the metadata seven | 2.0 | 6.0 |
| `blockStairsGreatwood` | the wooden device, the metadata six | 2.5 | 6.0 |
| `blockStairsSilverwood` | the wooden device, the metadata seven | 2.5 | 6.0 |
| `blockStairsEldritch` | the cosmetic solid, the metadata eleven | 2.0 | 6.0 |

**Verdict: the slabs and the double slabs are correct after the fix of the previous
commit** (the slabs carry 3.0 and 6.0, the four double slabs carry the same values and
their loot tables drop two and one). The stairs carried the raw setter value ten for the
eldritch stairs - fixed (defect 9).

### 2.4 `BlockEldritch`

Oracle: `func_149752_b(20000.0f)`, `func_149711_c(50.0f)` (68-69) -> effective 12000;
the per metadata hardness in `func_149712_f` (231-246), the blast resistance in
`getExplosionResistance` (248-260), the light in `getLightValue` (120-135).

| metadata | hardness | blast resistance | light | port group |
|---|---|---|---|---|
| 0, 1, 2, 3 | 50 | 12000 | 8 | 0 |
| 4 (glowing crust) | 2 | 30 | 12 | 1 |
| 5 (glyphed) | 2 | 30 | 12 | 1 |
| 6 (deco) | 4 | 100 | 5 | 2 |
| 7 (doorway), 8 (lock) | -1 | Float.MAX_VALUE | 12 / 5 | 3 |
| 9 (crab spawner) | 15 | 30 | 4 | 4 |
| 10 (runed) | 15 | 30 | 0 | 4 |
| 11 (eldritch crystal) | 50 | 12000 | 8 | 5 |

**Verdict: the values are correct** (`EldritchBlockRules.ELDRITCH_GROUP_HARDNESS` /
`ELDRITCH_GROUP_RESISTANCE`).

### 2.5 `BlockEldritchNothing`

Oracle: `func_149722_s()` (48, unbreakable), `func_149752_b(6000000.0f)` (49, effective
3600000), `func_149715_a(0.2f)` (51, **light level 3**).

**Verdict: the hardness and the blast resistance were correct, the light level was
missing - fixed (defect 1 below).**

### 2.6 `BlockAiry`

Oracle: the family hardness is the default (0), the per metadata hardness in
`func_149712_f` (115-127), the blast resistance in `getExplosionResistance` (129-141)
and the light in `getLightValue` (143-155).

| metadata | hardness | blast resistance | light | port |
|---|---|---|---|---|
| 0 (node), 5 (energised node) | 2 | 200 | 8 | `airy_node` |
| 10 (magic hole), 11 (void hole) | 100 | 50 | 8 | `airy_hole` |
| 12 (unbreakable) | -1 | Float.MAX_VALUE | 0 | `airy_unbreakable` |
| 1 (nitor), 2, 3 (leaves), 4 (fence) | 0 | 0 | 15 / 15 / 15 / 0 | `airy` and the own nitor block |

**Verdict: correct** (`EldritchBlockRules.airyGroup*`).

### 2.7 The device families

| family | oracle | effective | port |
|---|---|---|---|
| `BlockMetalDevice` | `func_149711_c(3.0f)`, `func_149752_b(17.0f)` (101-102) | 10.2 | 10.2 |
| `BlockWoodenDevice` | `func_149711_c(2.5f)`, `func_149752_b(10.0f)` (82-83) | 6.0 | 6.0 (hardness fixed by the previous commit) |
| `BlockWoodenDevice` metadata 2/3 (arcane pressure plate) | hardness 2 (or -1 with the warded stone option) in `func_149712_f` (196-200) and the absolute 999 in `getExplosionResistance` (202-211) | - | own block `arcane_pressure_plate`, 2.0 / 999.0 |
| `BlockStoneDevice` | `func_149711_c(3.0f)`, `func_149752_b(25.0f)` (93-94) | 15.0 | 15.0 |

**Verdict: the values are correct.**

### 2.8 The remaining families

| family | oracle | effective | port |
|---|---|---|---|
| `BlockMirror` | `func_149711_c(1.0f)`, `func_149752_b(10.0f)` (66-67) | 6.0 | 6.0 |
| `BlockCustomOre` | `func_149752_b(5.0f)`, `func_149711_c(1.5f)` (59-60) | 3.0 | 3.0 |
| `BlockLifter` | `func_149711_c(2.5f)`, `func_149752_b(15.0f)` (45-46) | 9.0 | 9.0 |
| `BlockTube` | `func_149711_c(0.5f)`, `func_149752_b(5.0f)` (88-89) | 3.0 | 3.0 |
| `BlockCrystal` | `func_149711_c(0.7f)`, `func_149752_b(1.0f)` (59-60), `func_149715_a(0.5f)` (61) | 0.6, light 7 | 0.6, light 7 |
| `BlockWarded` | `func_149722_s()` (73), `func_149752_b(999.0f)` (72) | 599.4 | 599.4 |
| `BlockArcaneDoor` | `func_149752_b(999.0f)` (67), `func_149711_c(wardedStone ? -1 : 15)` (68) | 599.4 | 599.4, hardness 15 |
| `BlockCosmeticOpaque` (warded glass) | see 2.2 | 999 | 999 |

**Verdict: the values are correct.** The `setBlockUnbreakable` of the warded block does
not raise its resistance, because the 1.7.10 setter only calls `setHardness(-1)` and
`setHardness` never lowers the resistance - so 599.4 and not 3600000 is the right number
for the warded block.

---

## 3. The defects that were found and fixed

### Defect 1 - the blank eldritch filler emitted no light

* oracle: `BlockEldritchNothing.java:51` - `this.func_149715_a(0.2f)`, the 1.7.10
  `setLightLevel` that stores `(int)(15 * 0.2)` = 3.
* port before: `ThaumcraftEldritchBlocks.java:123-127` registered the block with
  `strength(-1.0F, 3600000.0F)` and **no** `lightLevel`, so the filler was pitch black.
* fix: `EldritchBlockRules.ELDRITCH_NOTHING_LIGHT = 3` and
  `ThaumcraftEldritchBlocks.java:123-131` now use it.

### Defect 2 - the eldritch crystal dropped nothing

* oracle: `BlockCrystal.java:149-151` - the metadata seven (the eldritch crystal) drops
  `new ItemStack(ConfigItems.itemShard, 1, 6)`, the balanced shard.
* port before: `BlockEldritch.getDrops` returned an empty list for the crystal, and the
  loot table `eldritch_crystal.json` has no pool, so the crystal was worthless.
* fix: `EldritchBlockRules.eldritchDropsBalancedShard` (line 169) and
  `BlockEldritch.java:89-105` now drop `thaumcraft:balance_shard`.

### Defect 3 - the arcane pressure plate had the wrong box and a collision box

* oracle: `BlockWoodenDevice.java:249-256` - the selection box of the metadata two is
  `(0.0625, 0, 0.0625)` to `(0.9375, 0.0625, 0.9375)` and the pressed metadata three is
  half as high (`0.03125`); `BlockWoodenDevice.java:305-306` adds **no** collision box at
  all for the metadata two and three.
* port before: `BlockArcanePressurePlate` used `Block.box(0, 0, 0, 16, 1, 16)` for both
  variants (no inset, no half height) and had no `getCollisionShape` at all, so
  everything stood on the plate.
* fix: `BlockFamilyShapes.arcanePressurePlate`, `EldritchBlockRules.PLATE_BOX_*` and
  `BlockArcanePressurePlate.java:46-84`.

### Defect 4 - the item filter of the metal device lost its plate and its labels were swapped

* oracle: `BlockMetalDevice.java:264-268` - the metadata five **and** six share the
  selection box, the metal plate across the top three sixteenths;
  `BlockMetalDevice.java:330-337` - the metadata five collides with everything that is
  not a dropped item, the metadata six always;
  `BlockMetalDevice.java:677-686` - a right click on five sets six and the other way
  round, `:711-720` - a powered filter becomes six and an unpowered one five;
  `TileGrate.java:65-67` - the tile takes an item only while the metadata is five.
* port before: `EldritchBlockRules.DEVICE_METAL_GRATE_CLOSED` was 5 and
  `DEVICE_METAL_GRATE_OPEN` was 6, i.e. the labels were swapped; the open variant had
  no selection box at all (`Shapes.empty()`), so it could not be clicked; the tile read
  the `closed` property of the dedicated grate block, which the metal device state does
  not have, so a hopper that asked the metal device grate threw an
  `IllegalArgumentException`.
* fix: `EldritchBlockRules.java:498-508` (the labels), `BlockMetalDevice.java:70-108`
  (the selection and the collision box), `TileGrate.java:29-42` (the state read) and
  the test `DeviceWiringRulesTest:59-70` that carried the wrong comment.

### Defect 5 - the mirror was never oriented, and its box and its wall were on the wrong side

* oracle: `BlockMirror.java:168-175` - `func_149660_a` returns `metadata + side`, so the
  side the player clicked becomes the direction the mirror looks at;
  `BlockMirror.java:268-295` - the plate is the thin square on the **opposite** side,
  the side of the wall; `BlockMirror.java:177-203` - the wall behind the mirror is the
  opposite side as well; `BlockMirror.java:252-254` - no collision box at all.
* port before: `BlockMirror` had no `getStateForPlacement`, so every mirror was placed
  looking north; the table of the plates put the plate on the side of the look
  direction and the entries for west and east were swapped with the axis convention of
  Minecraft; the wall was checked on the side the mirror looks at, so the mirror
  needed a wall in front of it and the items were thrown into that wall.
* fix: `BlockMirror.java:53-95` and `:111-125`, `BlockFamilyShapes.mirrorPlate` and
  `EldritchBlockRules.mirrorWallSide`. The renderer of the port already drew the pane
  on the opposite side of the look direction, exactly like the original
  (`TileMirrorRenderer` of the oracle, `translateFromOrientation`), so it needed no
  change.

### Defect 6 - the wand pedestal of the stone device had the wrong box

* oracle: `BlockStoneDevice.java:464-480` - the selection box of the metadata five is
  the half wide column `(0.25, 0, 0.25)` to `(0.75, 1, 0.75)`; `:449-461` - the
  collision box is the base, the focus and the wide step between them.
* port before: `BlockStoneDevice` used `Block.box(2, 0, 2, 14, 10, 14)` for both the
  selection and the collision box.
* fix: `BlockFamilyShapes.wandPedestal` / `wandPedestalCollision` and
  `BlockStoneDevice.java:36-38` and `:69-85`.

### Defect 7 - the travel stone of the cosmetic solid had no effect

* oracle: `BlockCosmeticSolid.java:263-276` - a living entity that walks over the
  metadata two gets the speed effect (amplifier one, forty ticks) and the jump effect
  (forty ticks).
* port before: no effect at all.
* fix: `BlockCosmeticSolid.java:103-116` (`stepOn` is the 1.21.1 equivalent of the
  1.7.10 `onEntityWalk`).

### Defect 8 - the cosmetic solid was no beacon base

* oracle: `BlockCosmeticSolid.java:356-358` - `isBeaconBase` returns true for the whole
  block, so every metadata of the cosmetic solid is a beacon base.
* port before: the tag `thaumcraft:arcane_stone_beacon_bases` only held the two
  duplicated masonry blocks, the three native blocks that host the metadata of the
  cosmetic solid were missing.
* fix: `data/thaumcraft/tags/block/arcane_stone_beacon_bases.json` now holds the three
  native blocks of the family as well.

### Defect 9 - the eldritch stairs carried the raw setter value of the resistance

* oracle: the 1.7.10 `BlockStairs` copies the hardness and the blast resistance of the
  block whose texture it copies (`javap -c ans.class`: `setHardness(ref.blockHardness)`
  and `setResistance(ref.blockResistance / 3.0F)`), so the eldritch stairs of
  `ConfigBlocks:297` have the hardness two and the effective resistance six of the
  cosmetic solid, not the raw setter value ten.
* port before: `ThaumcraftEldritchBlocks.java:446-449` used `.strength(2.0F, 10.0F)`, and
  `BlockStairsEldritch.java:32-35` returned 10.0F from `getExplosionResistance()`.
* fix: `ThaumcraftEldritchBlocks.java:446-452` now uses
  `EldritchBlockRules.COSMETIC_FAMILY_NATIVE_RESISTANCE` and `BlockStairsEldritch.java:32-37`
  returns 6.0F, exactly like `BlockArcaneStoneStairs:29-36` already did.

---

## 4. The drops that were checked

| family | oracle | port |
|---|---|---|
| `BlockCosmeticSolid` | `func_149692_a` (259-261): the metadata eight drops as one and the metadata ten as nine | the metadata is a block state, so the two native items of the port carry the value |
| `BlockCosmeticOpaque` | `func_149692_a` (210-212): the metadata is kept | the warded glass has its own item, the amber blocks their own |
| `BlockEldritch` | `func_149650_a` (189-191) and `getDrops` (207-214): the metadata four drops itself, the metadata five a knowledge fragment, every other metadata nothing | the same, plus the balanced shard of the crystal (defect 2) |
| `BlockAiry` | `func_149650_a` (253-255) and `func_149694_d` (257-263): only the metadata one (the nitor) drops the resource, everything else nothing | the nitor has its own block and its own loot table |
| `BlockWoodenDevice` | `getDrops` (416-435): the metadata eight drops the banner with its colour and its aspect, `func_149650_a` (181-189): the metadata eight and, with the warded stone option, the metadata two and three drop nothing | the banner drops `thaumcraft:banner` with the `banner_data` component |
| `BlockStoneDevice` | `func_149692_a` (249-251) and `func_149650_a` (253-255): the metadata three drops as seven, the metadata four as six and both drop the cosmetic solid | the infusion pillar and the infusion matrix are own native blocks |
| `BlockMetalDevice` | `func_149692_a` (388-396): the metadata six drops as five, the metadata ten and eleven as nine | the same |
| `BlockMirror` | `getDrops` (123-158): the item mirror keeps the link of its partner, the essentia mirror as well | the same, the link in the custom data |
| `BlockCrystal` | `getDrops` (135-154): the metadata below six drops six shards of its own kind, the metadata six six shards of every kind, the metadata seven one balanced shard | the six crystal loot tables drop six shards, the mixed one one of every kind |
| `BlockCustomOre` | `getDrops` (132-145): the metadata zero drops itself, the metadata seven amber, every other metadata one to three shards | the same |

---

## 5. The shapes that were checked

| family | oracle | port |
|---|---|---|
| `BlockCosmeticSolid` | `func_149719_a` (182-184): the full block | the full block |
| `BlockCosmeticOpaque` | `func_149676_a` (70): the full block, `isSideSolid` (190-192) always true | the full block |
| `BlockWoodenDevice` metadata 0 | `func_149719_a` (249-250): `(0.1, 0, 0.1)` to `(0.9, 1, 0.9)` | the bellows block of the port, own native block |
| `BlockWoodenDevice` metadata 2/3 | the plate of defect 3 | fixed |
| `BlockWoodenDevice` metadata 8 (banner) | `func_149719_a` (264-290): a wall banner is two blocks high and a quarter thick, a standing banner is two blocks high and a third wide | **gap, see section 6** |
| `BlockMetalDevice` metadata 5/6 | the plate of defect 4 | fixed |
| `BlockMetalDevice` metadata 12 | `func_149719_a` (275-277): the cube inset by three sixteenths | unchanged, correct |
| `BlockStoneDevice` metadata 5 | the pedestal of defect 6 | fixed |
| `BlockAiry` | `func_149719_a` (157-165): the metadata three, four, ten, eleven and twelve have no box, the rest a 0.3 to 0.7 cube | the same |
| `BlockEldritchNothing` | `func_149633_g` (69-71): no selection box, `func_149668_a` (108-111): the collision box inset by an eighth | unchanged, correct |
| `BlockMirror` | `func_149719_a` (261-263) and `setBlockBoundsForBlockRender` (268-295), `func_149668_a` (252-254): no collision box | fixed (defect 5) |
| `BlockLifter` | `isSideSolid` (85-87): solid on the four sides, not on the top and the bottom | the port block, own native block |
| `BlockCustomOre` | `func_149719_a` (122-125): the full block, `isSideSolid` (157-159) always true | the full block |
| `BlockTube` | `func_149719_a` (190-197): the metadata two a half wide column, the metadata seven the full block | the port uses the tube geometry of the oracle |
| `BlockArcaneDoor` | `func_149719_a` (154-213): the closed door a thin plate, the open one a plate on the hinge side | the port reproduces the six shapes |
| `BlockWarded` | `func_149719_a` (216-218) forwards to the remembered block | the port forwards to the remembered block as well |

---

## 6. The gaps that are documented and not fixed

1. **The banner of the wooden device is one block high in the port and two in the
   original.** The oracle `BlockWoodenDevice.java:264-290` gives the wall banner the
   box `(0, 0, 0)` to `(1, 2, 0.25)` and the standing banner `(0.33, 0, 0.33)` to
   `(0.66, 2, 0.66)`, that is two blocks high, and `TileBanner.java:31-33` gives it a
   render box of two blocks. The port uses a single block plate
   (`BlockWoodenDevice.java:45`). A two block high banner needs a second block state
   (like a door) and a new model, so it is not changed here.
2. **The cosmetic solid never forbids the spawning of a mob.** The oracle
   `BlockCosmeticSolid.java:190-193` returns false for the metadata two, three and
   thirteen. The 1.21.1 default (`isValidSpawn`) is close but not the same, so the
   difference is left open instead of reimplementing the vanilla rule.
3. **The warding stone of the cosmetic solid shows no runes.** The oracle
   `BlockCosmeticSolid.java:325-354` draws the runes of the ward, and the port has the
   tile (`TileWardingStone`) but no particle effect. The particle is a client effect
   and the client acceptance of this audit was not run.
4. **The client rendering of the mirror was not checked.** The port draws the pane on
   the opposite side of the look direction, exactly like the oracle
   `TileMirrorRenderer.translateFromOrientation`, but no client run was made.
5. **The eldritch family keeps the crystal in its own group.** The oracle has no
   metadata eleven of `BlockEldritch`; the eldritch crystal is the metadata seven of
   `BlockCrystal`. The port hosts it as the metadata eleven of the eldritch family
   with the hardness 50 and the resistance 12000 of the family, which is the value the
   crystal block would have had.
6. **The essentia mirror has no item and no recipe.** The oracle has the second infusion
   recipe `MirrorEssentia` (`ConfigRecipes.java:362`), whose result is
   `new ItemStack(ConfigBlocks.blockMirror, 1, 6)`, the essentia mirror. The port keeps
   the essentia mirror as the `essentia` block state of the mirror block
   (`BlockMirror.ESSENTIA`) with its own tile (`TileMirrorEssentia`), but there is no
   item that places that state and no research page, so the essentia mirror cannot be
   built. The gap is already tracked by the port in
   `ResearchRecipeCoverage.java:528` ("MirrorEssentia"); the item, the model, the name
   and the recipe of the essentia mirror belong to the research and recipe area and were
   not added by this audit.
7. **The arcane pressure plate keeps the three sensitivity modes of the original**
   (`BlockWoodenDevice.java:371-393`); the port reproduces the messages and the
   filter, but the plate does not schedule a tick of its own after the state change
   (the port uses the scheduled tick of the block).

---

## 7. The verification that was run

```
export JAVA_HOME=/home/Acak/.jdk21
export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/ocblocks
./gradlew --no-daemon --console=plain :common:test --rerun-tasks
```

```
> Task :common:compileJava
> Task :common:processResources
> Task :common:classes
> Task :common:compileTestJava
> Task :common:processResources
> Task :common:testClasses
> Task :common:test

BUILD SUCCESSFUL in 35s
5 actionable tasks: 5 executed
```

The result files of that run (`common/build/test-results/test`) hold 241 test suites
with **1672 tests, 0 failures, 0 errors, 0 skipped**.

The new test `common/src/test/java/thaumcraft/common/blocks/BlockFamilyParityTest.java`
locks the values, the boxes and the drops of this audit, and the corrected comment of
`DeviceWiringRulesTest` locks the open and the closed metadata of the item filter.

No client run, no dedicated server run and no multiplayer acceptance was made for this
audit.
