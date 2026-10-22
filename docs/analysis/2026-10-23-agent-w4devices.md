# Wave 4 — `w4devices`: the devices, the base content, the world generation and the shared infrastructure (2026-10-23)

Agent: `w4devices`. Worktree: `/home/cyberfox-kali/wt/w4devices`, branch `agent/w4devices`.
Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 859/859 fingerprints).

**No Gradle was run.** Every test below is **authored but NOT executed**; the leader runs the
build, the JUnit suite and the client.

---

## 1. Result

| unit | before | after |
|---|---|---|
| `thaumcraft/common/entities/EntityItemGrate.java` | partial | **parity_verified** |
| `thaumcraft/common/blocks/BlockCustomPlantItem.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemNugget.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/WorldgenLootHelper.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/events/EventHandlerWorld.java` | partial | partial (the chunk marker; the maze is closed) |
| `thaumcraft/common/lib/events/ServerTickEventsFML.java` | partial | partial (the chunk regeneration queue) |
| `thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java` | partial | partial (the scattered feature node, the `auraGen` flag) |
| `thaumcraft/client/fx/particles/FXSparkle.java` | partial | partial (the shock focus, the leaves, the lifter; the hole is closed) |
| `thaumcraft/client/renderers/tile/TileMirrorRenderer.java` | partial | partial (the fixed function `glTexGen`) |
| `thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java` | partial | partial (the OBJ, the cycling preview) |
| `thaumcraft/common/blocks/BlockCosmeticStairs.java` | partial | partial (the position hash of the texture) |

`python3 scripts/porting/update_scope_ledger.py --check --original-root
/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`;
`counts` untouched; the diff touches only the eleven units of this lane.

---

## 2. The two decisive sources that were not in the ledger before

### 2.1 The official 1.7.10 client jar and the MCP mapping

`/tmp/opencode/client1710.jar` (the official Mojang 1.7.10 client, sha1
`e80d9b3bf5085002218d4be59e668bac718abbc6`) together with the MCP mapping
(`mcp_stable-12-1.7.10.zip`, `methods.csv` and `fields.csv`) and the obf→srg table make the
**1.7.10 vanilla** readable. The port uses the same obf names, so a class is found with
`CL: <obf> net/minecraft/...` in the srg table and decompiled with CFR 0.152. That is how the
vanilla push out, the vanilla `BlockBush`, the vanilla `Material` and the vanilla dungeon chest
list were read for this wave.

### 2.2 The Forge 1.7.10 platform source

The original mod runs on **Forge 1.7.10**, and the soil table of the plant item is a *Forge*
method, not a mod method. The Forge 1.7.10 branch of the platform (the authoritative source of
the platform the original ran on) was read for the two methods this wave needed:

* `patches/minecraft/net/minecraft/block/Block.java.patch` — the added method
  `Block.canSustainPlant` and the added method `Block.isSideSolid`;
* `src/main/java/net/minecraftforge/common/ChestGenHooks.java` — the chest category
  `dungeonChest`.

---

## 3. Defects found and fixed

### 3.1 `EntityItemGrate` — the semantics of the original were **inverted**

**Original.** `oracle thaumcraft/common/entities/EntityItemGrate.java:26-36` overrides the
1.7.10 `Entity.func_145771_j` (`pushOutOfBlocks`):

```java
if (world.getBlock(i, j, k) == ConfigBlocks.blockMetalDevice
        && (world.getBlockMetadata(i, j, k) == 5 || world.getBlockMetadata(i, j, k) == 6)) {
    return true;
}
return super.func_145771_j(x, y, z);
```

The 1.7.10 vanilla `EntityItem.onUpdate` (the official client jar, the class `xk`) calls it
**every tick, before the move**:

```java
this.X = this.j(this.s, (this.C.b + this.C.e) / 2.0, this.u);
this.d(this.v, this.w, this.x);
```

and stores the answer in `Entity.field_70145_X`. The MCP mapping of that field is
**`noClip`** — "whether this entity won't clip with collision or not". The 1.7.10
`Entity.moveEntity` (the class `sa`) skips the whole collision handling of the move when the
flag is set.

⇒ The original does **not** push the item out of the grate: the item of the filter falls
through the plate of the filter with its motion untouched. The override also returns
**before** the vanilla push out, so the vanilla push out never runs for that item.

**Port before.** `common/src/main/java/thaumcraft/common/entities/EntityItemGrate.java:57-89`
pushed the item **out** of the grate with an impulse of `random * 0.2 + 0.1` in the direction
of the closest free side — the opposite of the original, and a visible bug: every item the
filter ejected was thrown out of the filter.

**Fix.** `EntityItemGrate` now
* computes the cell of the item from the **middle** of its bounding box (the rule of the
  1.7.10 `EntityItem.onUpdate`),
* sets the 1.21.1 `Entity.noPhysics` flag (the counterpart of the 1.7.10 `noClip`) while the
  item is inside the grate, in `move` (the counterpart of the 1.7.10 `moveEntity`),
* skips the 1.21.1 `moveTowardsClosestSpace` of the item entity while the item is inside the
  grate (the 1.7.10 original has no such step),
* never changes the motion of the item.

**The exact vanilla arithmetic** (for the record, read out of the client jar, the class `sa`,
the method `j(DDD)Z`): the cell is `floor(x), floor(y), floor(z)` with the middle of the
bounding box, the closest of the five sides wins with `9999.0` as the first distance, the
**down** side is never used and the speed is `random.nextFloat() * 0.2f + 0.1f`. The original
never lets that method run for the item of the filter, so the port does not need it.

### 3.2 `BlockCustomPlantItem` — the soil table of the Forge 1.7.10 was incomplete

**Original.** `oracle thaumcraft/common/blocks/BlockCustomPlantItem.java:69-83` asks the soil
for its own plant type (the metadata three is Desert, the metadata four and five are Cave and
every other metadata is Plains) and `:50-65` places the plant only on the top face of a
supporting soil.

The soil rule itself is the **Forge** table of `Block.canSustainPlant` (Forge 1.7.10,
`patches/minecraft/net/minecraft/block/Block.java.patch`):

```java
if (plantable instanceof BlockBush && ((BlockBush)plantable).func_149854_a(this)) return true;
switch (plantType) {
    case Desert: return this == Blocks.sand;
    case Cave:   return isSideSolid(world, x, y, z, UP);
    case Plains: return this == Blocks.grass || this == Blocks.dirt || this == Blocks.farmland;
}
```

The inherited soil is the 1.7.10 `BlockBush.func_149854_a` (the official client jar, the class
`ajr`): the grass, the dirt and the farmland — the MCP mapping of the method is
`canPlaceBlockOn`, "is the block grass, dirt or farmland".

**Port before.** The port kept the Plains soil for the two Cave plants (the row's open
difference) and it accepted the **podzol** and the **coarse dirt**, which the table of the
original does not carry.

**Fix.**
* `common/src/main/java/thaumcraft/common/blocks/BlockCustomPlant.java` carries the table
  exactly: the inherited soil for every plant type, the sand for the Desert type and
  `isSideSolid(world, x, y, z, UP)` for the Cave type.
* `isSideSolidUp` is the port of the Forge method, with its special cases (the upper half of a
  slab, the hopper, the flipped stairs, the full snow layer of eight) and its default (a full
  opaque cube, `isCollisionShapeFullBlock && canOcclude`; the glass and the leaves of 1.21.1
  carry `noOcclusion`, so they are not solid on the top side, exactly as the translucent
  material of the 1.7.10 glass and the leaves of the original were not opaque).
* The plant block answers `canSurvive` with the **true** of the original `canPlaceBlockAt`
  (`oracle BlockCustomPlant.java:130-132`), so the plant never dies when the block below it
  changes — the original never died.
* The soil rule of the **item** is asked by the item itself (`PlantItem.canPlace`), because
  the 1.21.1 `BlockItem.canPlace` asks the block whether it can survive, and the block of the
  original always answers yes.
* `common/src/main/java/thaumcraft/common/blocks/BlockVishroom.java` (the manashroom, the
  metadata five) asks the **Cave** case for its item, the metadata five of the original item
  table.

### 3.3 `ItemNugget` — two items that the port's own tags and recipes pointed at were never registered

**Original.** `oracle thaumcraft/common/items/ItemNugget.java:39-52` registers the icons of
the eight nuggets and of the seven clusters **unconditionally**; `:62-88` lists the iron, the
quicksilver, the cinnabar, the thaumium, the void, the iron cluster and the gold cluster in the
creative tab always, and the copper, the tin, the silver and the lead only when the ore mod
of the other mod is found (`Config.foundCopperIngot` and the three next).
`oracle thaumcraft/common/config/ConfigRecipes.java:399-406` adds the ore dictionary recipes:
nine iron nuggets into an iron ingot, nine thaumium nuggets into a thaumium resource, nine void
nuggets into a void resource, nine quicksilver nuggets into a quicksilver resource and the two
reverses.

**Port before.** The catalogue carried six of the fourteen items: the thaumium nugget and the
void metal nugget had their model, their texture, their language name and their ore dictionary
tag, but **they were never registered**, so
`data/thaumcraft/tags/item/original_ore/nugget_thaumium.json`,
`nugget_void.json`, `recipe/thaumium_nuggets.json`, `thaumium_ingot_from_nuggets.json`,
`void_nuggets.json` and `void_ingot_from_nuggets.json` and the object aspect list pointed at
items that did not exist. The six items of the tin, the silver and the lead were missing too.

**Fix.**
* the two missing items are registered (`thaumcraft:thaumium_nugget`,
  `thaumcraft:void_nugget`) and the six items of the tin, the silver and the lead are
  registered as well, with their models (the textures of the original were already in the
  assets) and their language names;
* the six ore dictionary tags of the original now carry the items of the port next to the
  optional common tags, like the copper one already did;
* the creative tab keeps the gate of the original: the six items of the tin, the silver and the
  lead are not listed (the 1.21.1 tree has no tin, no silver and no lead ore), the vanilla
  iron nugget is listed (the original always listed the iron nugget);
* the two ore dictionary recipes of the original that the port missed are added
  (`iron_ingot_from_nuggets.json`, `quick_silver_from_drops.json`).

### 3.4 `WorldgenLootHelper` — the chests of the structures carried the wrong loot

**Original.** `oracle thaumcraft/common/lib/world/WorldGenHilltopStones.java:86-93`,
`WorldGenMound.java:2518-2536` and `WorldGenGreatwoodTrees.java:369-372` fill the chests with
the Forge chest category `dungeonChest`, and the original rolls the chest of the hilltop stones
**twice** and the chest of the mound a second time in one roll out of five.

The category is the static list of the vanilla 1.7.10 `WorldGenDungeons` (the official client
jar, the class `asd`): the saddle, the iron ingot, the bread, the wheat, the gunpowder, the
string, the bucket, the golden apple, the redstone, the two music discs, the name tag and the
three horse armours, with their amounts and their weights — plus the enchanted book that Forge
adds to the category, and the count of the category is eight to eight (Forge 1.7.10
`ChestGenHooks.java`: `addInfo(DUNGEON_CHEST, WorldGenDungeons.field_111189_a, 8, 8)`).
The vanilla `WeightedRandomChestContent.fillChestWithLoot` (the class `qx`) draws the entry
**with its weight**, the amount between the minimum and the maximum of the entry and writes the
stack into a **random slot** of the chest.

**Port before.** The chest of the mound and the chest of the hilltop stones were filled with
the **1.21.1 vanilla `chests/simple_dungeon` loot table** — a different item list, a different
number of items and a different placement (the port filled the slots in order, the original
writes into random slots).

**Fix.** The new `common/src/main/java/thaumcraft/common/lib/world/WorldgenLootHelper.java`
(the name of the legacy helper of the 1.12 reference) rolls the category of the original: the
entries of the port (the vanilla list plus the enchanted book of Forge plus the TC4 additions),
the count of the category, the weighted draw, the amount and the random slot. The entries are
generated again for every roll, exactly as the original does (the enchanted book is rolled
again).

### 3.5 `FXSparkle` — the portable hole had **no** sparkles at all

**Original.** `oracle thaumcraft/common/tiles/TileHole.java:51-56` — the update tick of the
hole runs on the client as well, and `:127-211` (`surroundwithsparkles`) draws **twenty four**
sparkles of the type two around the hole: one for every edge of the hole block where a
neighbour is an opaque cube and the neighbour on the other side of the edge is not the hole
block. The sparkle itself is the configurable `FXSparkle` of the original
(`oracle thaumcraft/client/fx/particles/FXSparkle.java:22-110`): the life of `3 * multiplier`
ticks, the frames `16 + age / multiplier` of the sheet `textures/misc/particles.png`, the shrink
over the life, the alpha `0.75f` and the colour of the type — the type two is the blue
`0.2f, 0.2f, 0.7f + rand * 0.3f`. The proxy of the original draws it with the size `1.5f` and
with the roll `rand.nextInt(6) < particleCount(2)`
(`oracle thaumcraft/client/ClientProxy.java:658-664`).

**Port before.** The native tree had **no** configurable sparkle particle and the native hole
block had **no** client ticker, so the twenty four sparkles of the original had no call site at
all.

**Fix.**
* `common/src/main/java/thaumcraft/client/fx/particles/TypeSparkleParticle.java` — the
  configurable sparkle of the original: the life, the frames, the shrink, the alpha, the size
  (`0.1f * field_70544_f` with the random one to two of the 1.7.10 `EntityFX`) and the colours
  of the eight types as a pure function.
* `common/src/main/java/thaumcraft/common/lib/world/HoleSparkleRules.java` — the twenty four
  calls of the original as a pure function of the six neighbours.
* `common/src/main/java/thaumcraft/client/ThaumcraftHolesClient.java` — the client effect: the
  opaque test of the original (`isSolidRender && canOcclude`, the 1.21.1 counterpart of the
  1.7.10 `isOpaqueCube`, the glass and the leaves are not opaque in either), the hole test, the
  random fraction on the axis of the edge and the roll of the particle count.
* `common/src/main/java/thaumcraft/common/blocks/BlockHole.java` and `TileHole.java` — the
  client ticker of the hole.

### 3.6 `EventHandlerWorld` — the second gap of the row was wrong: the maze **is** ported

The row said that the maze load and save of the original has no native counterpart. It has
one: `common/src/main/java/thaumcraft/common/lib/world/OuterLandsProvider.java:67-140` keeps
the cells of the labyrinth in a `SavedData` of the dimension (`MazeSavedData`), the 1.21.1
counterpart of the file `labyrinth.dat` of the original
(`oracle thaumcraft/common/lib/world/dim/MazeHandler.java:89-131`), and a `SavedData` of a
dimension is written and read with the world. The gap of the row is corrected, only the chunk
marker is left.

---

## 4. What is still open, with the precise blocker

| unit | the exact remaining difference | why it cannot be closed here |
|---|---|---|
| `EventHandlerWorld` | the chunk save and chunk load marker of the original (`oracle EventHandlerWorld:114-137`, the `Thaumcraft` compound with the key of `Config.regenKey`) | the vanilla 1.21.1 writes the chunk NBT as a fixed set of keys (`net/minecraft/world/level/chunk/storage/ChunkSerializer.write`, the remapped jar) and the Fabric API has no chunk data event, so the marker cannot be written or read on both loaders |
| `ServerTickEventsFML` | the chunk regeneration queue of the original (`oracle ServerTickEventsFML:83-104`), ten chunks per world tick, with the TC4 world generation | the marker of the queue does not exist (see above) and the generation of the port runs through the datapack feature pipeline, which cannot be re-run for an existing chunk without regenerating its terrain. The options of the original (`Config.regenAura` and the five next) keep their default of `false` |
| `ThaumcraftWorldGenerator` | the node of the vanilla scattered features (`oracle ThaumcraftWorldGenerator:413-419`) and the `auraGen` flag that makes the wild aura node and the aura totem of a chunk exclusive (`:419` and `:455`) | the vanilla scattered feature subsystem does not exist in 1.21.1, and the two native features (the placed feature of the node and the totem feature) do not share the per chunk flag of the original. **The note of the row was wrong**: the wild node pass does *not* delegate into the TC4 API jar, the whole plan is in the oracle (`ThaumcraftWorldGenerator.createRandomNodeAt:218-399`) and the native `NodeGeneration.plan` reproduces it line by line |
| `FXSparkle` | the sparkles of the shock focus (`oracle ItemFocusShock:166,174`: five at the hit point and five at the pointed entity, the size `2.0+rand`, the type two and the gravity `0.05+rand*0.05`) and the sparkles of the magical leaves and of the lifter | the native shock focus payload carries one endpoint and not the hit point **and** the pointed entity, so the ten sparkles of the original have no call site. The magical leaves and the lifter have no native block at all: a content gap of the block lane. **The sparkles around the portable hole are closed** |
| `TileMirrorRenderer` | the view dependent texture generation of the linked view (`oracle TileMirrorRenderer:52-554`, the fixed function `GL11.glTexGen` and the hand written projection matrix) and the additive blend of the inner layers | the 1.21.1 renderer draws every quad with a shader of the render pipeline; the fixed function texture coordinate generation has no counterpart. Everything else of the renderer is the original (the pane, the offset of the unstable mirror, the brightness, the six turns, the sixteen layers, the rotation `(i*i*4321+i*9)*2`, the fade `1/(16-i+1)`, the two textures and the `Random(31100L)` colour sequence) |
| `TileThaumatoriumRenderer` | the retained OBJ is still not rendered and the cycling output preview cannot be reproduced | the native draws the plain orientable block model and the OBJ loader (`ModelThaumatorium:23`) stays unreferenced. The exact pose is derived and unchanged: `pose.translate(0.5,0,0.5)`, `Axis.XP.rotationDegrees(-90)` and the facing rotation around Z (NORTH 270, SOUTH 90, EAST 180, WEST 0); the two machine blocks would have to become `RenderShape.INVISIBLE`. The cycling preview needs the set of the possible recipes of the machine, and the native machine is programmed with a job list (`TileThaumatorium:322`) |
| `BlockCosmeticStairs` | the texture of the eldritch stairs is the one of the position hash of the original (`oracle BlockCosmeticSolid:141-162`) | a 1.21.1 stair model is baked per block state and cannot randomise per position. The port **can** reproduce it with a block state property that holds the index of the texture (four models per stair shape) and a position hash in `onPlace`; that is an asset and model change of the eldritch stairs that this wave could neither compile nor run |

---

## 5. The tests (authored, NOT executed)

| file | methods | what it protects |
|---|---|---|
| `common/src/test/java/thaumcraft/common/entities/EntityItemGrateParityTest.java` | 3 | the cell of the item is the cell of the middle of its bounding box, the item does not clip while it is inside the grate, the vanilla push out never runs for it |
| `common/src/test/java/thaumcraft/common/blocks/PlantSoilRuleParityTest.java` | 4 | the inherited soil is the grass, the dirt and the farmland, the Cave case is the solid top side with its special cases, the table of every plant type |
| `common/src/test/java/thaumcraft/common/lib/world/DungeonChestLootParityTest.java` | 2 | the fifteen entries of the vanilla dungeon chest with their amounts and their weights, the count of the category is eight |
| `common/src/test/java/thaumcraft/common/lib/world/HoleSparkleRulesParityTest.java` | 3 | the eight edges of the hole in the ground, the hole without a solid neighbour, the suppression of the sparkles by a hole neighbour |
| `common/src/test/java/thaumcraft/common/blocks/BaseContentItemParityTest.java` (extended) | 1 | the ids of the fourteen items of the catalogue of the nuggets and of the clusters |

**No test constructs a `Block`.** Every rule under test is a pure function
(`EntityItemGrate.cellHeight`/`grateCell`/`noClipInsideGrate`/`appliesVanillaPushOut`,
`BlockCustomPlant.bushSoil`/`soilSupports`/`isSideSolidUp`,
`ThaumcraftLoot.addVanillaDungeonEntries`, `WorldgenLootHelper.DUNGEON_CHEST_COUNT`).

---

## 6. The `javac` proof

No Gradle was run. The whole `common` main tree (1378 files) and the whole `common` test tree
(354 files) were type-checked with `javac` against

* the remapped 1.21.1 jar of the Loom cache
  (`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2/minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`),
  **widened by the three entries of `common/src/main/resources/thaumcraft.accesswidener`**
  (a small ASM tool that flips the access flags of `Projectile.<init>`,
  `ItemEntity.pickupDelay` and `LivingEntity.lastHurtByPlayerTime`, because Loom applies the
  access widener to the Minecraft jar at build time),
* the exact library versions of Minecraft 1.21.1 (the version json of the Loom cache) and the
  loader, the Architectury API (the remapped jar of the project cache), the mixin and the
  annotation jars of the module cache.

```
sources: 1378  classpath entries: 57
javac exit: 0
test sources: 354
javac(test) exit: 0
```

---

## 7. The files changed

* `common/src/main/java/thaumcraft/common/entities/EntityItemGrate.java` — the noClip of the original.
* `common/src/main/java/thaumcraft/common/blocks/BlockCustomPlant.java` — the soil table, the survival rule, the item rule.
* `common/src/main/java/thaumcraft/common/blocks/BlockVishroom.java` — the Cave soil of the item, the survival rule.
* `common/src/main/java/thaumcraft/common/config/ThaumcraftPlants.java`, `ThaumcraftBiomes.java` — the plant type of the items.
* `common/src/main/java/thaumcraft/common/config/ThaumcraftContent.java` — the catalogue of the nuggets and of the clusters, the gate of the creative tab.
* `common/src/main/java/thaumcraft/common/config/ThaumcraftLoot.java` — the vanilla dungeon list as a pure function.
* `common/src/main/java/thaumcraft/common/lib/world/HoleSparkleRules.java` — **new**, the twenty four sparkles of the hole.
* `common/src/main/java/thaumcraft/client/fx/particles/TypeSparkleParticle.java` — **new**, the configurable sparkle of the original.
* `common/src/main/java/thaumcraft/client/ThaumcraftHolesClient.java` — **new**, the client effect of the hole.
* `common/src/main/java/thaumcraft/common/blocks/BlockHole.java`, `common/src/main/java/thaumcraft/common/tiles/TileHole.java`, `common/src/main/java/thaumcraft/client/ClientSetup.java` — the client tick of the hole.
* `common/src/main/java/thaumcraft/common/lib/world/WorldgenLootHelper.java` — **new**, the chest of the original.
* `common/src/main/java/thaumcraft/common/lib/world/WorldGenMound.java`, `WorldGenHilltopStones.java` — the chest of the original.
* `common/src/main/resources/assets/thaumcraft/models/item/{tin,silver,lead}_nugget.json`, `cluster_{tin,silver,lead}.json` — **new**.
* `common/src/main/resources/assets/thaumcraft/lang/{en_us,ru_ru}.json` — the names of the six new items.
* `common/src/main/resources/data/thaumcraft/tags/item/original_ore/{nugget,cluster}_{tin,silver,lead}.json` — the items of the port.
* `common/src/main/resources/data/thaumcraft/recipe/iron_ingot_from_nuggets.json`, `quick_silver_from_drops.json` — **new**.
* `common/src/test/java/thaumcraft/common/entities/EntityItemGrateParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/common/blocks/PlantSoilRuleParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/common/lib/world/DungeonChestLootParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/common/lib/world/HoleSparkleRulesParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/common/blocks/BaseContentItemParityTest.java` — the catalogue.
* `docs/porting/scope-ledger.json` — the verdicts and the evidence of the eleven units.
* `docs/analysis/2026-10-23-agent-w4devices.md` — this report.

---

## 8. Acceptance criteria for the leader

1. **The item filter.** Push an item into an open item filter: the item must **fall through**
   the filter and continue downwards. It must not be thrown out of the filter sideways. The
   same holds for the chest of the greatwood dungeon and for the chest of the loot room of a
   mound: they must carry the items of the original list (the saddle, the iron ingot, the
   bread, the wheat, the gunpowder, the string, the bucket, the golden apple, the redstone,
   the two music discs, the name tag, the horse armours, an enchanted book, the loot bags, the
   thaumium ingot, the amber gem, the mundane jewellery, the knowledge fragments, the
   thaumonomicon, the thaumium tools, the discount rings and the vis amulet) and **eight**
   items per roll.
2. **The plants.** A shimmerleaf (the metadata two) is placeable on the grass, the dirt and
   the farmland only. A cinderpearl (the metadata three) is placeable on the same three and
   on the sand. The manashroom (the metadata five) is placeable on every soil with a **solid
   top face** (the stone, the sand, the cobblestone) and on the grass, the dirt and the
   farmland. A plant that stands on a soil it does not like must **not die** (the original
   answers `canPlaceBlockAt` with a plain `true`).
3. **The nuggets.** The creative tab of Thaumcraft must show the iron nugget, the copper
   nugget, the quicksilver drop, the thaumium nugget, the void metal nugget, the iron, the
   copper, the cinnabar and the gold cluster — and **not** the tin, the silver and the lead
   nuggets and clusters (the original lists them only when the ore mod of the other mod is
   found). Nine iron nuggets must craft an iron ingot and nine quicksilver drops a quicksilver
   ingot; nine thaumium nuggets must craft a thaumium ingot and nine void metal nuggets a void
   ingot.
4. **The portable hole.** Open a portable hole next to a solid block: the hole must be
   surrounded by **blue** sparkles on the edges where it touches the solid block, they must
   shrink over their life and they must not fall down. The sparkles must not appear on an edge
   where the neighbour is another hole block.
5. **The tags.** The tag `thaumcraft:original_ore/nugget_thaumium` and the tag
   `thaumcraft:original_ore/nugget_void` must resolve (they pointed at nothing before this
   wave), and so must the four recipes of the thaumium and the void nuggets.

---

## 9. What is verified here and what is not

**Verified here (no Gradle needed):**

* `python3 scripts/porting/update_scope_ledger.py --check --original-root
  /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`; the
  ledger diff touches only the eleven units of this lane and leaves `counts` untouched.
* The `javac` type check of the whole `common` main tree and of the whole `common` test tree
  (§6).
* The numbers of the vanilla push out, of the vanilla `BlockBush`, of the vanilla `Material`,
  of the vanilla dungeon chest list, of the Forge soil table and of the Forge chest category
  were read out of the official 1.7.10 client jar, of the MCP mapping and of the Forge 1.7.10
  source, not out of the comments of the port.

**NOT verified here (the leader must run it):**

* `:common:compileJava` and `:common:compileTestJava` — **not run** (the `javac` proof above
  is the substitute).
* `:common:test` — **not run**; the four test files are authored only.
* The client frames — **not seen**; the acceptance criteria of §8 are written for the leader.
