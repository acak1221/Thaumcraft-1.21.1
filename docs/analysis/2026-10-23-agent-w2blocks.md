# Wave 2 — the base content and the small interactions (`agent/w2blocks`)

**Date:** 2026-10-23
**Branch:** `agent/w2blocks` (worktree `/home/cyberfox-kali/wt/w2blocks`)
**Oracle:** `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte-exact, the authority).
`/home/cyberfox-kali/OpenTC4` was used only as the reading aid for the readable 1.7.10 names
(`Material.water` and the like).

**Units:** the eighteen units of `/home/cyberfox-kali/units/w2blocks.txt`
(`base_content_and_small_interactions`, milestone M02).

---

## 1. The result

| canonical path | before | after |
|---|---|---|
| `thaumcraft/common/blocks/BlockCosmeticStairs.java` | partial | **partial** (the per position icon, §4.1) |
| `thaumcraft/common/blocks/BlockCustomPlantItem.java` | partial | **partial** (the Cave soil, §4.2) |
| `thaumcraft/common/blocks/BlockFluidDeath.java` | partial | **parity_verified** |
| `thaumcraft/common/blocks/BlockFluidPure.java` | partial | **parity_verified** |
| `thaumcraft/common/blocks/CustomStepSound.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemBathSalts.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemBucketDeath.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemBucketPure.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemCompassStone.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemKey.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemManaBean.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemNugget.java` | partial | **partial** (the tin, silver and lead, §4.3) |
| `thaumcraft/common/items/ItemNuggetEdible.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemResource.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemSanitySoap.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemTripleMeatTreat.java` | partial | **parity_verified** |
| `thaumcraft/common/items/relics/ItemResonator.java` | partial | **parity_verified** (§3.2) |
| `thaumcraft/common/items/relics/ItemSanityChecker.java` | partial | **partial** (the added reading, §4.4) |

**Thirteen units moved `partial → parity_verified`, five stay `partial` with a precise blocker.**

The ledger was refreshed with
`python3 scripts/porting/update_scope_ledger.py --check --original-root /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`
and reports **`stale_review_fingerprints: 0`**. Only the eighteen units above were touched.

---

## 2. The defects that were found and fixed

### 2.1 The three custom block sounds that were still missing (`CustomStepSound`)

The wave of 2026-10-23 fixed the jar (`ThaumcraftContent.JAR_SOUND`) and listed three more
as open. The original has no closed sound enum: `thaumcraft.common.blocks.CustomStepSound` and
`thaumcraft.common.lib.CustomSoundType` are sound types whose name lives in the `thaumcraft`
namespace, so a block can carry its own sound with its own volume and its own pitch.

| block | original | port before | port after |
|---|---|---|---|
| the mirror | `thaumcraft:jar`, volume 0.5, pitch 2.0 (oracle `BlockMirror.java:68`) | `SoundType.GLASS` | `ThaumcraftEldritchBlocks.MIRROR_SOUND` (`:110-113`, used at `:224-230`) |
| the loot urn | `thaumcraft:urnbreak`, volume 1.0, pitch 0.7 (oracle `ConfigBlocks.java:302`) | `SoundType.STONE` | `ThaumcraftEldritchBlocks.LOOT_URN_SOUND` (`:117-120`, used at `:256-259`) |
| the flux goo | `thaumcraft:gore`, volume 1.0, pitch 1.0 (oracle `BlockFluxGoo.java:50`) | the vanilla default | `ThaumcraftFlux.FLUX_GOO_SOUND` (`:39-43`, used by `properties(true)`) |

The jar (oracle `BlockJar.java:88`), the crystal (oracle `BlockCrystal.java:62`) and the taint
and the taint fibres (oracle `BlockTaint.java:73`, `BlockTaintFibres.java:69`) already carried
their custom sound. The four sound events and their samples exist in the assets of the port
(`assets/thaumcraft/sounds.json`, `assets/thaumcraft/sounds/*.ogg`).

The sound types are now named constants, so the test can assert them without constructing a
block (see §5).

### 2.2 The death fluid was not a water (`BlockFluidDeath`)

* **original:** `BlockFluidDeath.java:39` — `super(ConfigBlocks.FLUIDDEATH, Material.field_151586_h)`.
  `Material.field_151586_h` is **`Material.water`**: the readable 1.7.10 reference names it in
  `RenderEventHandler.java:367` (`bi.getMaterial() == Material.water`), and the same field is
  used by the purifying fluid (`BlockFluidPure.java:43`).
  A 1.7.10 entity that stood in the death fluid was therefore an entity **in water**: it swam,
  it took no fall damage and it drowned once its air supply was gone.
* **port before:** the death fluid was a fluid of its own and was **not** in the vanilla water
  tag, so the 1.21.1 engine treated a cell as an ordinary block: the entities were not pushed,
  they did not swim and they did not drown. The wave of 2026-10-23 had written this down as an
  unfixable gap ("Closing that would need a water tag on the death fluid, which drowns the
  player in a fluid that is meant to kill by its own damage") — but the original *did* drown in
  the death fluid, because it was a water.
* **fix:** `common/src/main/resources/data/minecraft/tags/fluid/water.json` now carries
  `thaumcraft:death_fluid` and `thaumcraft:death_fluid_flowing` next to the two purifying fluid
  ids. The vanilla water tag is the 1.21.1 counterpart of the 1.7.10 `Material.water`.
  The drowning is unreachable in practice: the cell damages every living thing by its quanta
  (`BlockFluidDeath.java:55-59`, the damage is the metadata plus one), so a player dies in five
  ticks in the source cell — the original drowned in exactly the same way.
* **second effect:** the purifying fluid refuses to flow into a cell of another water material
  (the 1.7.10 lateral rule, `PureFlowRules.canFlowInto` and its `waterOrLava` argument). With
  the death fluid in the water tag, the purifying fluid no longer overwrites a death fluid cell,
  which is what the original did (both were `Material.water`).

### 2.3 The random display tick of the death fluid was missing (`BlockFluidDeath`)

* **original:** `BlockFluidDeath.java:65-80` — every cell draws a slimy bubble
  (`FXSlimyBubble`, white, alpha 0.8, the size `0.075 + rand*0.075`, at the height
  `y + 0.1 + 0.225 * metadata`) and in one of fifty rolls it plays the pop of the lava
  (`liquid.lavapop`, the volume `0.1 + rand*0.1`, the pitch `0.9 + rand*0.15`).
* **port before:** nothing; the death fluid had no `animateTick` and no random ticks at all.
* **fix:** `BlockFluidDeath.animateTick` (`:144-170`) with the client hook
  `BlockFluidDeath.setClientEffects` and the lava pop; the properties of the cell got
  `.randomTicks()` (`ThaumcraftLegacyTiles.fluid()`, `:83-88`); the client hook is wired in
  `ThaumcraftSpaClient.init()` (`:16-27`) to the white bubble of the port with the alpha 0.8.
  The sprite sheet of the original (the particle sheet index 144) has no counterpart in the
  port, so the bubble is the white bubble of the port (the same particle the purifying fluid
  uses, with the alpha of the original).

### 2.4 The zombie brain was in the creative tab (`ItemResource`)

* **original:** `ItemResource.java:132-137` — the creative listing of the resources skips the
  metadata five, the zombie brain.
* **port before:** every resource of `ThaumcraftContent.MATERIALS` was appended to the tab
  (`ThaumcraftContent.java:221`).
* **fix:** the tab now skips the `zombie_brain` (`ThaumcraftContent.java:221-224`).

### 2.5 Two stale ledger claims were corrected

* `ItemResonator`: the ledger said "the original's GUI was replaced by an inspection action".
  **The oracle has no GUI for the resonator at all** (`grep -rl Resonator` in the oracle finds
  no client class). The 4.2.3.5 original reads the transport of the clicked block, prints
  `tc.resonator1` and `tc.resonator2` and plays `thaumcraft:alembicknock` with the volume 0.5
  and the pitch `1.9 + rand*0.1` (oracle `ItemResonator.java:79-112`). The native inspection
  action does exactly that (`ItemResonator.java:53-102`), so the unit reproduces the original
  and moved to `parity_verified`.
* `ItemCompassStone`: the ledger said "the rarity of the original has no counterpart in
  1.21.1". 1.21.1 has `Rarity` and the port carries `Rarity.RARE`
  (`ItemCompassStone.java:28-45`); the unit moved to `parity_verified`.

---

## 3. What was audited and found to agree

### 3.1 The blast resistance of the whole cosmetic family of stairs

The 1.7.10 `BlockStairs` copies the hardness and the blast resistance of the block whose
texture it copies, and the 1.7.10 resistance setter stores the value three times while the
getter divides it by five, so the **effective** blast resistance is `0.6 * setterValue`.

| stair | reference | oracle | port |
|---|---|---|---|
| the eldritch stairs | the cosmetic solid, the metadata 11 | hardness 2.0, resistance 6.0 (`BlockCosmeticSolid.java:66-67`) | `ThaumcraftEldritchBlocks.java:459-465` (2.0, `COSMETIC_FAMILY_NATIVE_RESISTANCE` = 6.0) |
| the arcane stone stairs | the cosmetic solid, the metadata 7 | hardness 2.0, resistance 6.0 | `ThaumcraftMasonry.java:31-32` |
| the greatwood and the silverwood stairs | the wooden device, the metadata 6 and 7 | hardness 2.5, resistance 6.0 (`BlockWoodenDevice.java:82-83`) | `ThaumcraftCosmeticWood.java:41` (2.5, 6.0) |

The stair of the metadata seven does **not** take the hardness four and the resistance twenty
of that metadata, because the 1.7.10 stair copies the field of the block and not its per
metadata getter (`BlockCosmeticSolid.java:221-223` and `:249-251` are the per metadata getter).
The light opacity zero of the original (`BlockCosmeticStairs.java:32`) is in the port
(`BlockCosmeticStairs.lightOpacity`).

### 3.2 The items

Every remaining item of the list was checked line by line against the oracle and agrees with
the port; the numbers are now constants of the production code and are asserted by the test of
§5:

| unit | the original | the port |
|---|---|---|
| `ItemBathSalts` | the lifespan of 200 ticks (`ItemBathSalts.java:44-46`) | `SpaRules.BATH_SALT_LIFETIME` |
| `ItemBucketDeath` | the stack limit 1, the cell of the metadata three, the spot rules (`ItemBucketDeath.java:39-43,93-105`) | `EquipmentRules.BUCKET_DEATH_MAX_STACK_SIZE`, `bucketDeathCanPour`, `bucketDeathClearsSpot`, `BlockFluidDeath.BUCKET_CELL_METADATA` |
| `ItemBucketPure` | the capture of the source cell only (`ItemBucketPure.java:103`) | `PureBucketActions.isSourceLevel` |
| `ItemCompassStone` | the expiry of 10000 ms, the range 256, the field of view 0.66 (`ItemCompassStone.java:45-61`) | `EquipmentRules.COMPASS_STONE_*` |
| `ItemKey` | the access entry, the gold key of the owner, the iron key rule (`ItemKey.java:99-128`) | `EquipmentRules.key*` |
| `ItemManaBean` | the potion of 160 + rand(80) ticks, the quarter of the aspect pool (`ItemManaBean.java:60-77`) | `ItemManaBean.EFFECT_DURATION`, `EFFECT_DURATION_RANDOM`, `ASPECT_POOL_CHANCE` |
| `ItemNuggetEdible` | one hunger, three tenths of a point of saturation, ten ticks (`ItemNuggetEdible.java:28-37`) | `EquipmentRules.NUGGET_EDIBLE_*` |
| `ItemResource` | the nineteen metadata values, the tainted resources, the nitor, the alumentum, the knowledge fragment (`ItemResource.java:69-78,132-150,196-275`) | `EquipmentRules.RESOURCE_*`, the nineteen items of `ThaumcraftContent` |
| `ItemSanitySoap` | 200 ticks, the chances 0.33, 0.25, 0.25 (`ItemSanitySoap.java:35-39,76-95`) | `SanitySoapRules.DURATION`, `SanitySoapRules.chance` |
| `ItemTripleMeatTreat` | 6 hunger, 0.8 saturation, the regeneration of five seconds in two thirds of the cases (`ItemTripleMeatTreat.java:26-31`) | `EquipmentRules.TREAT_*` and the name in `en_us` and `ru_ru` |
| `ItemResonator` | the inspection of a transport (`ItemResonator.java:79-112`) | `ItemResonator.tryInspect` |
| `ItemFluidPure` | the Ward `min(32000, 200000/max(1,floor(sqrt(warp))))`, the source light eight of the luminosity ten (`BlockFluidPure.java:41-56`, `ConfigBlocks.java:419`) | `SpaRules.warpWardDuration`, `SpaRules.pureLight` |

---

## 4. What is still open, and why

### 4.1 The per position icon of the ancient stone (`BlockCosmeticStairs`)

* **the original:** `BlockCosmeticSolid.java:157-162` — for the metadata 11, 13 and for every
  call with `side >= 100` the block takes one of the four ancient stone textures
  (`es_1` … `es_4`) by the hash of the position:
  ```java
  String l = x + "" + y + "" + z;
  Random r1 = new Random(Math.abs(l.hashCode() * 100) + 1);
  int i = r1.nextInt(12345 + side) % 4;
  ```
  `BlockCosmeticStairs.java:38-40` reaches this rule with `side + 100`, so the stair shows the
  ancient stone texture of its own position. **New in this wave:** the same raw draw is taken
  modulo six different bounds (`12345 + 100` … `12345 + 105`), so all six faces of a stair
  agree in all but one of about twelve thousand positions — the original effectively shows
  **one** texture per stair, not a patchwork.
* **the port:** a 1.21.1 stair model is baked per block state and cannot randomise per
  position, so the native eldritch stairs always show `es_1`
  (`assets/thaumcraft/models/block/eldritch_stairs.json`).
* **can the port do it?** Yes: a block state property that holds the index of the texture
  (four models per stair shape, the blockstate document of the stair with the property in every
  one of its forty-eight variants) and a position hash that the block computes in `onPlace`.
  That is an asset and model change of the eldritch stairs plus a new state property, and this
  wave could neither compile nor run it, so the unit stays `partial`.
* **the same gap** holds for the ancient stone, the ancient rock and the pedestal of the
  cosmetic solid itself (`BlockCosmeticSolid.java:141-162`); that block is not a unit of this
  domain and is not touched here.

### 4.2 The Cave plant of the original (`BlockCustomPlantItem`)

* **the original:** the *item* (`BlockCustomPlantItem.java:85-101`) asks the soil for the plant
  type of its metadata: the metadata 3 is Desert, the metadata 4 and 5 are Cave, the rest is
  Plains. The *block* (`BlockCustomPlant.java:119-128`) answers a different table: the
  metadata 3 is Desert, the metadata 4 is Cave and the metadata 5 (the manashroom) is Plains.
  The two tables therefore differ for exactly one metadata, the manashroom.
* **the port:** 1.21.1 has no `IPlantable` and no `EnumPlantType`, so the port answers the
  plant type with the block itself. Both tables of the original are now carried as pure
  functions (`BlockCustomPlant.itemPlantType`, `BlockCustomPlant.blockPlantType`) and the soil
  rule is `BlockCustomPlant.soilSupports`.
* **the blocker:** the Forge 1.7.10 table of the Cave soils is not part of the decompiled
  original, so the port cannot prove which block the original's Cave plants needed. The port
  keeps the Plains soil of the original's own *block* rule, which is the soil the original's
  world generator puts the manashroom on (`BiomeGenMagicalForest.java:147`). The difference is
  confined to the two Cave plants of the original.
* **also open:** the pitch of the placement sound of the original (the break sound of the
  plant with the pitch of the sound type times 0.8, `BlockCustomPlantItem.java:76`) has no
  1.21.1 counterpart, because 1.21.1 has no per block sound pitch.

### 4.3 The tin, the silver and the lead (`ItemNugget`)

The original registers the nuggets of iron, copper, tin, silver, lead, quicksilver, thaumium
and void metal and the clusters of iron, copper, tin, silver, lead, cinnabar and gold
(`ItemNugget.java:39-52`); it offered the tin, the silver and the lead only when the other mods
were found (`ItemNugget.java:72-85`). The port carries the quicksilver drop, the copper nugget
and the iron, the gold, the cinnabar and the copper clusters
(`ThaumcraftContent.java:140-162`). The six missing items need the ores of the tin, the silver
and the lead, which are the content of other mods and not of 1.21.1 vanilla — a catalogue gap,
not a behaviour.

### 4.4 The reading of the sanity checker (`ItemSanityChecker`)

The 4.2.3.5 original carries only its identity — the stack limit of one and the uncommon
rarity (`ItemSanityChecker.java:24-30`) — and has **no** use behaviour at all; the reading of
the warp was added to the item in a later version of Thaumcraft. The native item keeps that
later reading (the deviation is written down in the javadoc of the native class), so the
identity of the relic is reproduced but the behaviour is a superset of the original.

---

## 5. The tests that were authored (**not executed**)

No test of this wave was executed: the agent may not run Gradle (the leader owns the build and
the shared Loom cache). **The tests are authored and unexecuted.**

All of them are in `common/src/test/java/thaumcraft/common/blocks/` and **none of them
constructs a `Block` or an `Item`** — a constructed block leaves an unregistered intrusive
holder behind and the frozen block registry then refuses to start, which breaks every test in
the JVM. Every rule is a pure function, a constant or a registered id.

| file | what it asserts |
|---|---|
| `CosmeticAndPlantParityTest.java` | the hardness two and the effective blast resistance six of the cosmetic family, the hardness 2.5 and the resistance 6.0 of the wooden device, the seamless rule of the stairs, the light opacity zero of the stairs, the two plant type tables of the original and the soil rule |
| `FluidFamilyParityTest.java` | the four quanta, the light eight, the damage of the quanta, the bucket cell of the original metadata three, the quanta rule of a cell, the water tag of the port, the scaled light of the purifying fluid, the Ward duration and the capture of the source cell only |
| `CustomStepSoundParityTest.java` | the names, the volumes and the pitches of the four custom sound types and the existence of their sound events and samples |
| `BaseContentItemParityTest.java` | the lifespan of the salts, the rules of the bucket of death, the expiry, the range and the field of view of the compass stone, the access rule of the keys, the duration of the potion of the mana bean, the metadata of the nuggets and of the clusters and their native ids, the food values of the edible nuggets, the nineteen resources and the taint roll, the duration and the chances of the soap and the food values and the two names of the treat |

The three production changes that the tests need are in the production code itself: the sound
types are named constants (`ThaumcraftEldritchBlocks.MIRROR_SOUND`, `LOOT_URN_SOUND`,
`ThaumcraftFlux.FLUX_GOO_SOUND`), the numbers of the mana bean are constants of the item and
the plant types are the pure functions of `BlockCustomPlant`.

---

## 6. What the leader has to check on the real client

1. **The death fluid** — a cell of the death fluid has to show a white bubble that rises out
   of it, and one cell in fifty has to pop like lava. The player that stands in the cell has
   to *swim* in it (the water tag) and has to die in about five ticks from the damage of the
   cell, not from drowning.
2. **The purifying fluid** — the bubbles of the original (white, alpha 0.25) and the lava pop
   in one of twenty five; the player that touches a source gets the Ward.
3. **The three custom sounds** — the mirror has to sound like a jar with a high pitch (0.5 /
   2.0), the loot urn has to crack with `urnbreak` (1.0 / 0.7) and the flux goo has to squelch
   with `gore` (1.0 / 1.0). The jar, the crystal and the taint keep their sounds.
4. **The creative tab of the mod** — the zombie brain must **not** be in it any more.
5. **The eldritch stairs** — they still show `es_1` on every face; the original shows one of
   the four ancient stone textures per position (§4.1). This is the known open gap.

## 7. The blockers, in one list

1. `BlockCosmeticStairs` — the per position icon of the ancient stone: needs a block state
   property, twelve models and the blockstate document of the eldritch stairs (§4.1).
2. `BlockCustomPlantItem` — the Cave soil of the two Cave plants of the original is not in the
   oracle, and the placement pitch of the original has no 1.21.1 counterpart (§4.2).
3. `ItemNugget` — the tin, the silver and the lead nuggets and their clusters need the content
   of other mods (§4.3).
4. `ItemSanityChecker` — the native item reads the warp, the 4.2.3.5 original does nothing
   when it is used (§4.4).
5. The entity current of the death fluid: the push of the 1.7.10 is the Forge
   `World.handleMaterialAcceleration`, which is not part of the oracle, so it cannot be
   reproduced from the oracle. It is only observable for an entity that outlives the five
   ticks the source cell gives it.
6. The sprite sheet of the slimy bubble of the death fluid (the original particle sheet index
   144) has no counterpart in the port; the port draws its own white bubble.
