# Base content and small interactions (TC4 4.2.3.5 → Minecraft 1.21.1)

Date: 2026-10-23. Agent: `basecontent`. Worktree: `/home/cyberfox-kali/wt/basecontent`
(branch `agent/basecontent`). Domain: `base_content_and_small_interactions`, the 24 open
ledger units of `/home/cyberfox-kali/units/basecontent.txt`.

Oracle: the readable 1.7.10 tree `/home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/`
(cross-checked against the 1.12.2 legacy reference `src/main/java/thaumcraft/` of the
repository, which is a reference and not the truth).

**No Gradle was run.** The unit tests of this wave are *authored but not executed*.

---

## 1. The units and their verdicts

| unit | before | after | what changed |
|---|---|---|---|
| `thaumcraft/common/blocks/BlockFluxGas.java` | port / partial | port / **parity_verified** | the note of the previous wave was stale |
| `thaumcraft/common/blocks/BlockFluxGoo.java` | port / partial | port / **parity_verified** | the slime ecology of the original was missing, it is restored |
| `thaumcraft/common/blocks/BlockFluxGasItem.java` | replace / partial | replace / **parity_verified** | the item did not exist at all, it is added |
| `thaumcraft/common/blocks/BlockFluxGooItem.java` | replace / partial | replace / **parity_verified** | the item did not exist at all, it is added |
| `thaumcraft/common/blocks/BlockMagicalLeaves.java` | port / partial | port / **parity_verified** | the leaf cleared the original decay flag, it does not any more |
| `thaumcraft/common/blocks/BlockMagicalLeavesItem.java` | replace / partial | replace / **parity_verified** | the note of the previous wave was imprecise, the marker is the persistent state |
| `thaumcraft/common/blocks/BlockFluidPure.java` | port / partial | port / partial | the note of the previous wave was stale, the residual is written down |
| `thaumcraft/common/blocks/BlockFluidDeath.java` | port / partial | port / partial | the note of the previous wave was stale, the residual is written down |
| `thaumcraft/common/blocks/BlockCosmeticStairs.java` | port / partial | port / partial | the client visual gap is written down with the exact original lines |
| `thaumcraft/common/blocks/BlockCustomPlantItem.java` | replace / partial | replace / partial | the placement of the original is restored, the add-on hook stays open |
| `thaumcraft/common/blocks/CustomStepSound.java` | replace / partial | replace / partial | the exact list of the affected blocks is written down |
| the 13 units of `thaumcraft/common/items/**` | port / replace / partial | unchanged | audited line by line, the notes are corrected and the evidence is added |

The ledger keeps its shape: `docs/porting/scope-ledger.json` was rewritten with the same
`json.dumps(..., ensure_ascii=False, indent=2)` format, only the 24 rows of this domain
changed, `counts` is untouched and `stale_review_fingerprints` is **0**.

---

## 2. The defects that were found and fixed

### D1 — the two finite flux blocks had no item at all

* **Original**: `thaumcraft/common/config/ConfigBlocks.java:295-296` registers both blocks
  with an item class of their own:
  `GameRegistry.registerBlock(blockFluxGoo, BlockFluxGooItem.class, "blockFluxGoo")` and the
  same for `blockFluxGas`. `thaumcraft/common/blocks/BlockFluxGasItem.java:9,13-19` and
  `BlockFluxGooItem.java:9,13-19` give the item the stack limit of 64, the full cube in the
  inventory and the metadata **eight** of the placement.
* **Port before**: `common/src/main/java/thaumcraft/common/config/ThaumcraftFlux.java:30-31`
  registered the two blocks and nothing else — there was no `flux_gas` and no `flux_goo`
  item, so neither fluid could be obtained or placed in the game. The ledger note of the
  previous wave (“the native item places the block with its level state”) described an item
  that did not exist.
* **Fix**: `common/src/main/java/thaumcraft/common/blocks/BlockFluxGasItem.java` and
  `BlockFluxGooItem.java` (new), registered in `ThaumcraftFlux.java:41-47,60-67` with the creative
  tab entry of the original (`BlockFluxGas:29` and `BlockFluxGoo:34` put both blocks in the
  tab of the mod). The item places the cell with the level eight —
  `common/src/main/java/thaumcraft/common/blocks/FluxFiniteBlock.java:44-51,123-134` — which is the
  original metadata eight, the exceptional nine unit birth of a Forge `BlockFluidFinite`
  (a cell holds eight units). The item models
  `assets/thaumcraft/models/item/flux_gas.json` and `flux_goo.json` are the full cube of the
  fluid, the shape of the original `isFull3D`.

### D2 — the flux goo lost the whole slime ecology of the original

* **Original**: `thaumcraft/common/blocks/BlockFluxGoo.java:52-63` — a real
  `EntityThaumicSlime` that walks into the goo grows by one when its size is below the
  metadata of the cell, and the cell loses one unit (or vanishes at the metadata one).
  `BlockFluxGoo.java:88-110` — a cell of the metadata two to five breeds a **size one**
  slime in one of twenty five rolls, a cell of the metadata six and above breeds a **size
  two** slime in one of twenty five or turns into the taint in one of fifty
  (`Config.taintFromFlux` and `Utils.setBiomeAt` with `blockTaintFibres`).
  The 1.12 reference `src/main/java/thaumcraft/common/blocks/BlockFluxGoo.java:57-124`
  confirms the same behaviour and the sound `thaumcraft:gore` of the born slime.
* **Port before**: `common/src/main/java/thaumcraft/common/blocks/BlockFluxGoo.java:23-36`
  and `:44-79` consumed the random rolls but performed **no outcome at all** (“M11 size1/gore
  deferred”, “real biome/fibres still M11”), and the entity collision never grew a slime.
  The port does have the real `EntityThaumicSlime`
  (`common/src/main/java/thaumcraft/common/entities/monster/EntityThaumicSlime.java:60,80`)
  and the native biome writer `common/src/main/java/thaumcraft/common/lib/world/BiomeColumns.java:56`,
  so nothing blocked the port.
* **Fix**: `common/src/main/java/thaumcraft/common/blocks/BlockFluxGoo.java:62-89` (the slime
  branch of the entity collision, the slime is not slowed and does not get the exhaustion) and
  `:97-148` (the breeding of the size one slime, of the size two slime and the taint of the
  cell with the biome of the column, the taint fibres and the block event of the original).
  The random order of the original is kept exactly: the roll of the thick cell is consumed
  before the roll of the taint, a failed roll of the thin cell falls through to the ordinary
  decay of one in thirty, and the gas above the cell keeps its own boolean roll.

### D3 — the magical leaves cleared the decay flag that the original keeps

* **Original**: `thaumcraft/common/blocks/BlockMagicalLeaves.java:213-219` — a leaf that
  still has its support is written back with **the same metadata**, and the line that would
  clear the bit is commented out at `:216`
  (`// par1World.setBlock(x, y, z, this, metadata & -9, 3);`). The leaf therefore keeps the
  bit eight for ever, is checked again on every later random tick and the sapling roll of
  the loot table keeps its condition. The 1.12 reference
  `src/main/java/thaumcraft/common/blocks/BlockMagicalLeaves.java:49-58` asks for
  `CHECK_DECAY && DECAYABLE`, the bit eight and the bit four of the original metadata.
* **Port before**: `common/src/main/java/thaumcraft/common/blocks/BlockMagicalLeaves.java:82`
  cleared the flag (`else level.setBlock(pos, state.setValue(CHECK_DECAY, false), ...)`), so
  a leaf that had been flagged stopped being checked and lost the sapling roll of the
  original.
* **Fix**: `common/src/main/java/thaumcraft/common/blocks/BlockMagicalLeaves.java:72-83` no
  longer clears the flag. The support test itself was already the original one (a log
  within four face steps through leaves, `BlockMagicalLeaves:141-212`, the native test at
  `BlockMagicalLeaves.java:95-111`).

### D4 — the item of a custom plant placed itself on every block

* **Original**: `thaumcraft/common/blocks/BlockCustomPlantItem.java:50-65` — the item
  places the plant only when the clicked face is the top one **and** the block below
  supports its plant type (`canSustainPlant`), it plays the step sound of its own block and
  it consumes one item. The plant type of the metadata is the class
  `BlockCustomPlantItem.java:67-89`: Desert for the metadata three, Cave for four and five,
  Plains otherwise.
* **Port before**: `common/src/main/java/thaumcraft/common/blocks/BlockCustomPlant.java:50-53`
  — `mustSurvive()` returned **false**, so the 1.21.1 item placed the plant on every block
  and the item was lost; the plant died on the next random tick
  (`BlockCustomPlant.java:41-43`). The item also placed the plant on the side faces.
* **Fix**: `common/src/main/java/thaumcraft/common/blocks/BlockCustomPlant.java:65-83` — the
  item refuses a placement whose face is not the top one, it requires the soil to support
  the plant (`mustSurvive()` is true) and it announces the plant with the step sound of the
  block. The soil rule of the port (`BlockCustomPlant.java:36-40`, grass, dirt, coarse dirt,
  podzol, farmland and the sand of the Desert type) is the port's own mapping of the
  original `canSustainPlant`.
  The sapling item of the magical trees (`BlockMagicalSapling.SaplingItem`) was **not**
  changed: its `canSurvive` also asks for light, and the original item of the sapling does
  not refuse a dark spot.

---

## 3. What was checked and found correct

* **The finite flow of the two flux blocks**: the note of the previous wave (“the native
  block is a placeable block with a level, so it does not flow”) is stale. The port owns the
  classic flow of the Forge `BlockFluidFinite`
  (`common/src/main/java/thaumcraft/common/blocks/FluxFiniteBlock.java:145-242`: vertical
  first, the strict lower than sharing and the remainder random).
* **The direction of the gas**: the original sets the density direction to one
  (`BlockFluxGas.java:30`) and its own renderer reads the direction one as *rising*
  (`client/renderers/block/BlockGasRenderer.java:116`, `boolean rises = theFluid.getDensityDir() == 1;`).
  The native block derives the direction from the density of the fluid (minus four for the
  gas, `ConfigBlocks:152`, plus eight for the goo), which gives the same result.
* **The effect of the flux gas** (`BlockFluxGas.java:60-77`): one in ten, not tainted, not
  undead, without the exhaustion and without the confusion, the exhaustion of 1200 ticks
  with the amplifier of the metadata divided by three, or the confusion of 80 + 20·metadata,
  and the loss of one unit — reproduced in
  `common/src/main/java/thaumcraft/common/blocks/BlockFluxGas.java:25-45`.
* **The death fluid** (`BlockFluidDeath.java:24-46`, `ConfigBlocks:420`): four quanta in a
  source cell, the light eight, the damage of the metadata plus one, the bucket cell of the
  metadata three — reproduced in
  `common/src/main/java/thaumcraft/common/blocks/BlockFluidDeath.java:73-135` and in
  `common/src/main/java/thaumcraft/common/lib/tiles/LegacyTileRules.java:194-261`.
* **The purifying fluid** (`BlockFluidPure.java:25-56`, `ConfigBlocks:419`): the classic
  eight quantum flow, the Ward of `min(32000, 200000/max(1,floor(sqrt(permanentWarp))))`
  ticks, the source light eight out of the configured luminosity ten — reproduced in
  `common/src/main/java/thaumcraft/common/blocks/BlockFluidPure.java:128-192` and in
  `common/src/main/java/thaumcraft/common/lib/SpaRules.java:7-15`.
* **The cosmetic stairs** (`BlockCosmeticStairs.java:16-27`): the light opacity zero, the
  hardness two and the effective blast resistance six of the block whose texture the stair
  copies — reproduced in `common/src/main/java/thaumcraft/common/blocks/BlockArcaneStoneStairs.java:20-36`,
  `BlockCosmeticWoodStairs.java` and `BlockStairsEldritch.java:26-38`.
* **The item damage bit four of the leaves** (`BlockMagicalLeavesItem.java:13-21`): 1.21.1
  has no item damage, the marker is the persistent state, and the 1.21.1 `LeavesBlock`
  sets the persistent state for every leaf a player places
  (`LeavesBlock.getStateForPlacement`), so a planted leaf never decays and never drops a
  sapling, exactly like the original.
* **The thirteen item units**: the numbers of the original and of the port agree —
  the bath salts live two hundred ticks (`ItemBathSalts:30`, `SpaRules.BATH_SALT_LIFETIME`),
  the bucket of death fills the cell of the metadata three (`ItemBucketDeath:88-121`),
  the bucket of the pure fluid only takes the source cell, the compass stone expires after
  ten seconds and reaches two hundred and fifty six blocks with a field of view of 0.66
  (`ItemCompassStone:53-72`, `EquipmentRules:504-519`), the key writes the kind of the key
  followed by the name of the player (`EquipmentRules:451-470`), the mana bean draws a
  random vanilla potion of 160 + rand(80) ticks and fills the aspect pool in a quarter of
  the cases (`ItemManaBean:52-63`), the edible nuggets have one hunger and three tenths of
  a point of saturation with a use duration of ten ticks (`ItemNuggetEdible:15-27`,
  `EquipmentRules:307-319`), the sanity soap is used for two hundred ticks with the chances
  of 0.33, 0.25 and 0.25 (`ItemSanitySoap:35-39,76-95`), the triple meat treat has six
  hunger, four fifths of a point of saturation and the regeneration of five seconds with a
  chance of two thirds (`ItemTripleMeatTreat:17-21`, `EquipmentRules:491-497`).
  The defect D1 of the audit of the 21.10 (the treat without a name) is **closed**: both
  languages carry `item.thaumcraft.triple_meat_treat`.

---

## 4. What could not be fixed and why

| unit | the blocker |
|---|---|
| `BlockCosmeticStairs.java` | The original picks the icon of the cosmetic solid metadata eleven from the four ancient stone textures by the hash of the position (`BlockCosmeticSolid:158-161`, reached through the side plus hundred of `BlockCosmeticStairs:26`). A 1.21.1 model cannot randomise per position; it needs a client renderer, and `common/src/main/java/thaumcraft/client/**` is outside this domain. **The same gap holds for the ancient stone, the ancient rock and the pedestal of the cosmetic solid itself** (`BlockCosmeticSolid:141-162`) — a block that is *not* a unit of this domain, see §6. |
| `BlockFluidDeath.java` | The original death fluid is a `BlockFluidFinite` over `Material.water`, so the 1.7.10 engine pushed the entities that stand in it and let them drown. The native death fluid is a fluid of its own and is not in the water tag, so the entities are not pushed and do not drown. A water tag would drown the player in a fluid that is meant to kill by its own damage. |
| `BlockFluidPure.java` | The movement of an entity in the cell is the 1.21.1 engine and not the transplanted 1.7.10 loop; the native water tag owns the swimming and the drowning. The flow vector itself is the original one (`BlockFluidPure.flowVector`). |
| `BlockCustomPlantItem.java` | The Forge `IPlantable` hook has no 1.21.1 counterpart: an add-on plant that relied on it cannot place itself on the terms of the original. The player facing placement is now the original one. |
| `CustomStepSound.java` | 1.21.1 has no per block sound name: `SoundType` is a closed enum and a block can only answer `getSoundType` with one of its values. A custom step and break sound needs a client renderer or a mixin. The affected blocks of the original are the mirror (`thaumcraft:jar`, volume 0.5, pitch 2.0, `BlockMirror:39`), the jar (`BlockJar:57`), the crystal (`BlockCrystal:40`) and the loot urn (`thaumcraft:urnbreak`, `ConfigBlocks:290`); the sound events are in the port (`assets/thaumcraft/sounds.json`). |
| the 13 units of `common/items/**` | The files of this domain live in `common/src/main/java/thaumcraft/common/items/**`, which the task forbids to touch. The audit of their numbers found no defect, so nothing had to be changed there; the notes of the ledger are corrected and the evidence is added. |

---

## 5. The tests that were authored (**not executed**)

`common/src/test/java/thaumcraft/common/blocks/BaseContentParityTest.java` — nine JUnit 5
methods in the package of the port classes, each one naming the file and the line of the
1.7.10 original:

1. `theFiniteFluxItemsCarryTheOriginalIdsAndPlaceTheOriginalMetadataEight` — the ids
   `thaumcraft:flux_gas` / `thaumcraft:flux_goo`, the metadata eight of the placement
   (`BlockFluxGasItem:9,17-19`).
2. `theFiniteFluxLevelCarriesTheOriginalMetadataRange` — the level property of the finite
   flux runs from zero to eight, the capacity of a cell is eight.
3. `theFluxGooKeepsTheOriginalBreedingNumbers` — the metadata 2..6, the rolls 25, 50 and 30
   and the sizes 1 and 2 (`BlockFluxGoo:88-122`).
4. `thePlantItemRefusesThePlacementTheOriginalRefuses` — the item of the plant asks for a
   supporting soil and the sand carries the Desert type only
   (`BlockCustomPlantItem:50-65`).
5. `theMagicalLeavesSaplingRollsAreTheOriginalOnes` — 200 and 250
   (`BlockMagicalLeaves:246-255`) against the loot tables of the port.
6. `theMagicalLeavesSaplingNeedsTheOriginalDecayFlagAndNoPlayerFlag` — the condition of the
   sapling entry is `check_decay` true and `persistent` false, the bit eight and the bit
   four of the original metadata.
7. `theDeathFluidKeepsTheOriginalQuantaLightAndDamage` — four quanta, the light eight, the
   bucket cell of the metadata three and the damage of the metadata plus one
   (`BlockFluidDeath:24-46`).
8. `thePurifyingFluidKeepsTheOriginalScaledLight` — the light of the source eight and the
   flowing cells 7, 6, 5, 3, 2, 1, 0 (`ConfigBlocks:419`).
9. `theCosmeticStairsKeepTheOriginalLightOpacity` — the dry stair blocks no light
   (`BlockCosmeticStairs:20`).

**These tests were authored and never executed.** No Gradle run was made in this wave (the
task forbids it and a concurrent daemon would corrupt the loom cache), so neither the
compilation nor the result of the nine methods is claimed.

---

## 6. The blocks of the domain that suffer from the same root cause as the known
`BaseEntityBlock` defect (**reported, not fixed** — the `blockvis` agent owns them)

`BaseEntityBlock.getRenderShape` returns `RenderShape.INVISIBLE`
(`net/minecraft/world/level/block/BaseEntityBlock.class`, the method returns
`RenderShape.INVISIBLE`), and `EntityBlock` does not override it. Every block of the port
that extends one of the two and does not override `getRenderShape` is therefore invisible
in the client, and `BlockState.hasBlockEntity()` is `block instanceof EntityBlock`, which
`PistonBaseBlock.isPushable` and `PistonBaseBlock.moveBlocks` consult — so the piston does
not move them.

The classes of the port with the same root cause, with the icon of the 1.7.10 original:

| class | the icon of the original | visible in the original |
|---|---|---|
| `BlockCosmeticSolid` | many (`BlockCosmeticSolid:66-92`) | yes (the known defect) |
| `BlockCosmeticOpaque` | amber (`BlockCosmeticOpaque`) | **yes — a new instance of the defect** |
| `BlockWoodenDevice` | real icons | **yes — a new instance** |
| `BlockStoneDevice` | real icons | **yes — a new instance** |
| `BlockMetalDevice` | real icons | **yes — a new instance** |
| `BlockArcaneDoor` | `adoorbot` / `adoortop` (`BlockArcaneDoor:45-46`) | **yes — a new instance** |
| `BlockMirror` | real icons | **yes — a new instance** |
| `BlockMagicBox` | real icons | **yes — a new instance** |
| `BlockArcanePressurePlate` | the plate of the wooden device | **yes — a new instance** |
| `BlockEldritch`, `BlockEldritchNothing`, `BlockEldritchPortal` | the last two use the blank icon (`BlockEldritchNothing:38`, `BlockEldritchPortal:31`) | the known defect covers the family |
| `BlockAiry` | the blank icon (`BlockAiry:43-54`) | no, the airy block is invisible in the original too — the render shape of this class is correct |

The classes of the port that report a block entity for the whole block although only some
of their variants have one (the marker `common/src/main/java/thaumcraft/common/blocks/StateDependentBlockEntity.java`):
`BlockAiry`, `BlockArcaneFurnace`, `BlockCosmeticOpaque`, `BlockCosmeticSolid`,
`BlockEldritch`, `BlockEldritchNothing`, `BlockInfusionPillar`, `BlockMetalDevice`,
`BlockStoneDevice`, `BlockWandPedestal`, `BlockWoodenDevice`.

---

## 7. The files that were changed

```
common/src/main/java/thaumcraft/common/blocks/BlockCustomPlant.java        (D4)
common/src/main/java/thaumcraft/common/blocks/BlockFluxGoo.java            (D2)
common/src/main/java/thaumcraft/common/blocks/BlockMagicalLeaves.java      (D3)
common/src/main/java/thaumcraft/common/blocks/FluxFiniteBlock.java         (D1)
common/src/main/java/thaumcraft/common/blocks/BlockFluxGasItem.java        (D1, new)
common/src/main/java/thaumcraft/common/blocks/BlockFluxGooItem.java        (D1, new)
common/src/main/java/thaumcraft/common/config/ThaumcraftFlux.java           (D1)
common/src/main/resources/assets/thaumcraft/models/item/flux_gas.json      (D1, new)
common/src/main/resources/assets/thaumcraft/models/item/flux_goo.json      (D1, new)
common/src/test/java/thaumcraft/common/blocks/BaseContentParityTest.java  (new)
docs/porting/scope-ledger.json                                            (the 24 units)
```

`python3 scripts/porting/convert_assets.py --verify` → `missing_references: 0`
(2905 local model references, the two new item models are included).

`python3 scripts/porting/update_scope_ledger.py --check` **cannot run in this checkout**:
the private decompilation of the original jar (`work/thaumcraft-4.2.3.5-oracle`) is not
part of the tree, so the script stops with `Missing source root` — the same before and
after the edit of the ledger. The reconstruction of the script
(`refresh()` with the stored original side and the real legacy side, the real class
inventory and the real review copies) reproduces the edited file byte for byte and reports
`stale_review_fingerprints: 0`, so the leader can run the check unchanged.

## 8. The commits

* `c2e5ba16` `fix(basecontent): grow and breed the thaumic slime in the flux goo, add the flux items`
* `42b9a9d9` `docs(basecontent): review the base content units against the 1.7.10 oracle`
* `c79cc71e` `docs(basecontent): the report of the base content wave`
