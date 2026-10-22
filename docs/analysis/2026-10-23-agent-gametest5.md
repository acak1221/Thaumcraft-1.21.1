# Wave 2 — agent `gametest5`: the red GameTest scenarios of the shared suite

Date: 2026-10-23. Worktree: `/home/cyberfox-kali/wt/gametest5`, branch `agent/gametest5`.

The oracle of every verdict below is the byte-exact TC4 4.2.3.5 decompile
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (859/859 fingerprints). The
Forge 1.7.10 rules that the oracle itself does not contain are cited from the
Forge branch `MinecraftForge/MinecraftForge@1.7.10` (the patches of `Block`,
`BlockBush`, `BlockCrops`, `ItemHoe`, `BlockGrass`, `BlockDeadBush`), which is
the framework the oracle runs on.

## 1. The exact failures of the last full run

Source: the leader's `fabric/build/gametest-results.xml` of 2026-10-22 05:06
(318 scenarios, 6 failed) and the matching `fabric/run-gametest/logs/latest.log`.
The XML of the Fabric GameTest API carries an empty `<failure>` body, so the
messages were taken from the server log.

| scenario | message (verbatim) | where |
|---|---|---|
| `fluxOriginalExposureAndGooTail` | `original lower25 MISS falls through30 HIT then boolean; exact observed calls=[nextInt(25)]` | `FluxOverfullRuntimeChecks.gooTail(FluxOverfullRuntimeChecks.java:320)` |
| `fluxMovesAndExhaustsWithOriginalRules` | `pending real Thaumic Slime branch does not silently consume goo (M11 not complete)` | `FluxRuntimeChecks.decayChecks(FluxRuntimeChecks.java:216)` |
| `originalBiomeTreesAndDungeonsUseRealWorldgen` | (empty `<failure>`; the assertion ran inside a `runAfterDelay` callback, so the framework stored the `Throwable` and no text) | `BiomeRuntimeChecks.awaitDarkPlants` — the cinderpearl soil |
| `magicalFlowersGrowHarvestAndRefine` | `original Forge Bush/Plains/Desert soil rule: cinderpearl=true; soil=Block{minecraft:dirt}` | `PlantRuntimeChecks.run(PlantRuntimeChecks.java:53)` |
| `fullInventoryReusesFreedPaperSlotOrDrops` | `creative overflow spends one paper and the ink, and creates no note entity` | `ResearchNoteIssuanceRuntimeChecks.fullInventoryReusesFreedPaperSlotOrDrops(ResearchNoteIssuanceRuntimeChecks.java:186)` |
| `magicalBiomesGenerateNaturallyAndSpawnActualPech` | `region generation admits actual typed BE with original generation-plus-growth age3..7` | `NaturalBiomeRuntimeChecks.run(NaturalBiomeRuntimeChecks.java:87)` |

The NeoForge run of the same wave (`wt/neoforge/neoforge/run-gametest/logs/latest.log`)
adds one more real shared-code failure, `advancedFurnacePreservesCustodyAcrossRealChunkUnload`:
`own partial disassembly never force-loads saved remote shell terrain through
inner-parts' indirect shape updates; shell=33` at
`AdvancedFurnaceUnloadRuntimeChecks$Check.pollCoreReload(AdvancedFurnaceUnloadRuntimeChecks.java:94)`.
It is loader-specific (the same scenario is green on Fabric) — see §8.

## 2. `magicalFlowersGrowHarvestAndRefine` + `originalBiomeTreesAndDungeonsUseRealWorldgen` — the plant soil rule

**Verdict: the production code is wrong.**

* Oracle `thaumcraft/common/blocks/BlockCustomPlant.java:130-132` — the original
  block has **no** soil rule of its own (`canPlaceBlockAt` answers a plain
  `true`). The soil rule of the original is the Forge table of the plant type,
  because the item asks the soil through `IPlantable`
  (oracle `thaumcraft/common/blocks/BlockCustomPlantItem.java:69-83`).
* Forge 1.7.10 `Block.canSustainPlant`
  (`patches/minecraft/net/minecraft/block/Block.java.patch`, the added method):

  ```java
  Block plant = plantable.getPlant(world, x, y + 1, z);
  EnumPlantType plantType = plantable.getPlantType(world, x, y + 1, z);
  ...
  if (plantable instanceof BlockBush && ((BlockBush) plantable).func_149854_a(this)) return true;
  switch (plantType) {
      case Desert: return this == Blocks.field_150354_m;            // sand
      case Crop:   return this == Blocks.field_150458_ak;            // farmland
      case Cave:   return isSideSolid(world, x, y, z, UP);
      case Plains: return this == Blocks.field_150349_c || this == Blocks.field_150346_d || this == Blocks.field_150458_ak;
  }
  ```

  The inherited `BlockBush` soil is asked **first and for every plant type**, so
  the Desert plant (the cinderpearl, the metadata three) also stands on the
  inherited soil and only *adds* the sand to it. (`field_150354_m` is the sand,
  `field_150349_c` the grass, `field_150346_d` the dirt and `field_150458_ak` the
  farmland — proved by the Forge patches of `BlockCactus`, `BlockGrass`,
  `BlockDeadBush`, `BlockCrops` and `ItemHoe`.)
* Port before the fix: `common/src/main/java/thaumcraft/common/blocks/BlockCustomPlant.java:88`
  — `if (type == PlantType.DESERT) return soil.is(Blocks.SAND) || soil.is(Blocks.RED_SAND);`
  i.e. the sand alone. The same file's own javadoc and the port's own analysis
  (`docs/analysis/2026-10-16-fire-focus-and-flowers.md:57-59`) said the opposite
  ("first the inherited Bush grass/dirt/farmland, then Desert additionally accepts
  both metadata variants of sand"). The regression was introduced by the *test*
  commit `559b3527` ("test(blocks): lock the original values …"), which changed
  the production rule and broke the two scenarios.
* Fix (`fix(plants)`, commit `0fcac27d`): `soilSupports(PlantType, BlockState)`
  now answers the inherited soil first and adds the sand for the Desert type.
* The JUnit assertion that contradicted it (`assertFalse(soilSupports(DESERT, grass))`
  in `common/src/test/java/thaumcraft/common/blocks/CosmeticAndPlantParityTest.java:125`)
  was corrected with the same evidence.
* The second scenario failed for the same reason: `BiomeRuntimeChecks.awaitDarkPlants`
  asserts `plant.defaultBlockState().canSurvive(level, origin)` for the vishroom,
  the shimmerleaf and the cinderpearl on grass; the cinderpearl was the one that
  the port refused. Its `<failure>` body is empty because the assertion runs in a
  `runAfterDelay` callback, which the framework reports as a `Throwable`.

## 3. `fluxOriginalExposureAndGooTail` — the goo tail of the original

**Verdict: the production code is wrong.**

* Oracle `thaumcraft/common/blocks/BlockFluxGoo.java:103-136` — the original
  `updateTick` is ONE `if / else if / else if` chain:

  ```java
  if (meta >= 2 && meta < 6 && world.isAirBlock(x, y+1, z) && rand.nextInt(25) == 0) { ...size one slime... }
  else if (meta >= 6 && world.isAirBlock(x, y+1, z)) {
      if (rand.nextInt(25) == 0) { ...size two slime... }
      else if (Config.taintFromFlux && rand.nextInt(50) == 0) { ...taint... }
  }
  else if (rand.nextInt(30) == 0) { ...ordinary decay... }
  ```

  A thin cell whose `nextInt(25)` **missed** therefore leaves the first branch
  and reaches the `nextInt(30)` decay of the original.
* Port before the fix: `common/src/main/java/thaumcraft/common/blocks/BlockFluxGoo.java:109-111`
  — `if (!step.coherent() || spawn != 0) return;` returned after the missed roll,
  so the decay roll and its boolean were never drawn. The port's own comment
  ("a failed lower-density25 falls THROUGH to30") already described the
  original; the code did the opposite.
* Fix (`fix(flux)`, commit `a9aab7fc`): the missed roll now leaves the branch
  and reaches the thirty roll; the thick cell still never reaches it, exactly as
  in the original chain. The port's own replacement fence survives
  (commit `604660f4`): a cell that was replaced during the roll ends the tail
  (the original never faces an external replacement inside its own tick, and the
  fence is the port's documented behaviour of `callbackFences`).

## 4. `fluxMovesAndExhaustsWithOriginalRules` and the M11 expectations of the goo tail

**Verdict: the scenarios encoded something the original never did** (they were
written for the port state *before* the thaumic slime and the taint were
ported, commit `c2e5ba16`).

* Oracle `BlockFluxGoo.java:106-112`: a thin cell (the metadata two to five)
  whose `nextInt(25) == 0` **empties the cell** (`setBlockToAir`) and **births a
  size one `EntityThaumicSlime`**.
* Oracle `BlockFluxGoo.java:113-125`: a thick cell (the metadata six and above)
  births a size two slime on the same roll and, when that roll missed, turns
  into `blockTaintFibres` and its column into the taint biome on the
  `nextInt(50) == 0` roll.
* The two scenarios asserted the opposite ("does not silently consume goo
  (M11 not complete)", "pending taint … leaves its resource intact").
* Fix (`test(flux)`, commit `2401c2a4`): both scenarios now assert the original
  — the cell is emptied, a **real** `EntityThaumicSlime` of the original size
  is born, and the thick cell becomes the taint fibres while its column becomes
  the taint biome. The column is restored to its original biome afterwards so
  the shared world of the suite is not changed by the scenario.

## 5. `fullInventoryReusesFreedPaperSlotOrDrops` — the creative ink

**Verdict: the scenario encoded a wrong expectation.**

* Oracle `thaumcraft/common/lib/research/ResearchManager.java:199-209` — the
  original spends the ink with `inv[a].damageItem(1, (EntityLivingBase) player)`.
* Minecraft 1.7.10 `ItemStack.damageItem(int, EntityLivingBase)` returns
  **before** the damageability, the enchantments and the RNG when the entity is
  a creative player, so the ink is never worn down in creative. The port
  already implements exactly that
  (`common/src/main/java/thaumcraft/common/lib/research/ScribingInk.java:57-60`,
  `planPlayer`), and the port's own spec says so
  (`docs/analysis/2026-10-15-scribing-ink-semantics.md:19` and `:50`: "a fully
  occupied creative inventory with a paper stack of 2 gives paper 1, an
  **unchanged usable inkwell**, zero entries in the inventory and zero dropped
  entries").
* The scenario asserted the ink to be worn down (`98 → 99`) in creative, which
  also contradicted its own sibling scenario `nativeInkPreservesCreativeAndUnbreakable`
  in the same file.
* Fix (`test(research)`, commit `957358b6`): the creative branch now expects the
  ink to stay at 98; the paper is still spent (the original's
  `consumeInventoryItem` has no creative branch) and the note is still silently
  discarded by the original `addItemStackToInventory` creative branch.

## 6. `magicalBiomesGenerateNaturallyAndSpawnActualPech` — the generated mana pod

**Verdict: the production code is wrong** (two defects), and the scenario also
needed a fixture of its own.

### 6.1 The age and the aspect of the generated pod

* Oracle `thaumcraft/common/lib/world/WorldGenManaPods.java:20-39`: the pod is
  placed with the metadata `2 + rand.nextInt(5)` (two to six) and
  `TileManaPod.checkGrowth()` is called immediately.
* Oracle `thaumcraft/common/tiles/TileManaPod.java:42-46`: `checkGrowth` raises
  the metadata once (below seven) and then, above the metadata two, writes the
  aspect into the tile. So the world carries the **age three to seven** with a
  real aspect — exactly what the scenario asserts.
* Port before the fix: `common/src/main/java/thaumcraft/common/tiles/TileManaPod.java:47-56`
  — `initializeGenerated` asked `region.getBlockEntity(pos)` for the tile. While
  a chunk is generated, `WorldGenRegion#setBlock` stores a **`DUMMY`** block
  entity marker (the bytecode of `WorldGenRegion.setBlock`:
  `ChunkAccess.setBlockEntityNbt` with the id `DUMMY`) and
  `LevelChunk.promotePendingBlockEntity` creates the real tile from that marker
  **without its data** (`BlockEntity.loadStatic` for every id but `DUMMY`). The
  region therefore has **no** tile at all, the method returned `false` at its
  first check, and the generated pod kept the placed age two to six and **never
  received an aspect**.
* Fix (`fix(pods)`, commit `07f28d4b`): the method now asks the chunk for the
  pending block entity data of the cell and writes the aspect into it (with the
  real tile id), so the promoted tile of the chunk carries the aspect exactly as
  the original's own tile does. The age is raised in the block state itself, so
  the world carries the age three to seven.

### 6.2 The bound of the search

* Oracle `WorldGenManaPods.java:23`: the search runs
  `while (y < Math.min(128, world.getHeight(x, z)))`. The 1.7.10 height map is
  **live**: the world generator keeps it up to date with every block it places,
  so the bound of the search includes the branches of the trees that the very
  same decoration pass has just written.
* Port before the fix: `common/src/main/java/thaumcraft/common/lib/world/WorldGenManaPods.java:30`
  — the bound was `level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)`. In
  1.21.1 the generation region freezes its height map before the features step
  (`ProtoChunk.setBlockState` only refreshes a height map once the persisted
  status passes the height map stage), so the height of the region is the
  **terrain** height and never the canopy. The search therefore stopped at the
  terrain (the diagnostic run: `PODSCAN break y=67 height=67`) and the magical
  forest grew **no mana pod at all**.
* Fix (`fix(pods)`, commit `936fa542`): the search asks the column itself for
  the live height (the block above the highest block that is not air), which is
  the answer the live height map of the original gives, and the drifted column
  is asked again exactly as the original does.

### 6.3 The fixture of the scenario

The scenario observes the generation of a region, so it needs a region that
*this* run generates. A world that already carries the region of an earlier run
answers with the pods of that run (the leader's `fabric/run-gametest` world and
this worktree's world both did). The scenario now removes the chunk data of the
region it is about to generate in the test-only natural dimension of the harness
(`test(pods)`, commit `ce819620`).

## 7. The flux scenario that the M11 expectations had hidden

`fluxOriginalExposureAndGooTail` also had to script the **champion join roll**
of the born slime: the native join event of the port rolls the original
one-in-hundred spawn roll of every mob that enters the world
(`ChampionRules.spawnRoll`, `ChampionHooks.onEntityAdded`), and the scenario
observes the level random. The scenario now scripts that draw
(`i(100, 1)`), the tier of the thaumic slime is no tier at all
(`ChampionRules.tierFor` → `TIER_NONE`), so no champion is born and the draw
stays the only one.

## 8. The advanced furnace custody (NeoForge only) — not settled here

`advancedFurnacePreservesCustodyAcrossRealChunkUnload` fails on the NeoForge
loader only (`shell=33`, the shell chunk became `FULL` after the core was
broken); the same scenario is green on the Fabric loader, so the shared code
does not reproduce it. The failing assertion is
`common/src/gameTest/java/thaumcraft/common/testing/AdvancedFurnaceUnloadRuntimeChecks.java:94`:
after `level.setBlock(center, AIR, QUIET)` the remote shell chunk must still be
unloaded. The port already keeps every own write of a partial retirement quiet
(`common/src/main/java/thaumcraft/common/lib/AdvancedFurnaceFormation.java:162-164`),
and the chunk that is loaded is the *neighbour* of the core, which the native
shape cascade of the removal reaches. Settling this needs a NeoForge run, and
the NeoForge module is explicitly deferred by `AGENTS.md` ("Fabric first …
NeoForge parity is deferred"). It is therefore **not settled** by this agent.

## 9. The final result

* `:fabric:runGameTest` — **318 scenarios, 0 failures** (the report of the run
  is `fabric/build/gametest-results.xml`, the server log is
  `fabric/run-gametest/logs/latest.log`).
* `:common:test` — **2203 tests, 0 failures, 0 errors, 0 skipped**.
* No scenario was added, removed or renamed, so
  `scripts/porting/required-gametests.json` is unchanged.

The commits of this agent (branch `agent/gametest5`):

| commit | kind |
|---|---|
| `0fcac27d` | `fix(plants)`: the Desert plant type keeps the inherited BlockBush soil |
| `a9aab7fc` | `fix(flux)`: a failed breed roll of the original falls through to the decay |
| `2401c2a4` | `test(flux)`: the original breeds the slime and turns the cell into the taint |
| `957358b6` | `test(research)`: the creative ink of the original is not worn down |
| `07f28d4b` | `fix(pods)`: the generated mana pod keeps its age and its aspect |
| `604660f4` | `fix(flux)`: the replacement fence of the port ends the goo tail |
| `936fa542` | `fix(pods)`: the mana pod search asks the live height of its column |
| `ce819620` | `test(pods)`: the natural forest scenario regenerates its own region |
