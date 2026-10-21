# OpenTC4 / TC4 4.2.3.5 block entities on 1.21.1 — tile parity audit

Date: 2026-10-21
Branch: `port/octiles`
Oracle (truth): `/home/Acak/work/thaumcraft-4.2.3.5-oracle` — the decompiled **TC4 4.2.3.5 for 1.7.10**.
Vanilla name reference: the 1.7.10 MCP mapping (`mcp_stable-12-1.7.10`), resolved with
`python3 -B scripts/porting/resolve_srg.py <name>` and with `srg1710.json` for the declaring class of a field.

## 0. Scope of this pass (honest statement)

The task named ~30 original block entities. A line-by-line audit of all of them does not fit in one
pass, so this report covers **the seven priority tiles of the task** (crucible, infusion matrix,
essentia reservoir, thaumatorium, arcane bore, jar, mirror) **plus the small tiles that were cheap to
read completely** (infusion pillar, alembic, node stabilizer, node energized, flux scrubber,
eldritch obelisk, eldritch nothing, banner, brainbox, arcane pressure plate, grate, deconstruction
table, arcane lamp).

* **Read completely, oracle + port:** TileCrucible, TileEssentiaReservoir, TileInfusionMatrix
  (server half, lines 86–684 of the oracle; 703–782 are client particles), TileThaumatorium,
  TileMirror, TileMirrorEssentia, TileJar + TileJarFillable + TileJarFillableVoid, TileInfusionPillar,
  TileAlembic, TileNodeStabilizer, TileNodeEnergized, TileFluxScrubber, TileEldritchObelisk,
  TileEldritchNothing, TileBanner, TileBrainbox, TileArcanePressurePlate, TileGrate,
  TileDeconstructionTable, TileArcaneLamp.
* **Sampled only (numbers grepped, logic not re-derived):** TileArcaneBore (the file is 40 kB and was
  already audited by `port/ocblocks` + `docs/analysis/2026-10-17-arcane-bore.md`),
  TileAlchemyFurnace, TileNodeConverter, TileArcaneWorkbench, TileResearchTable, TileEldritchPortal,
  TileAlchemyFurnaceAdvanced/Nozzle, TileArcaneFurnace/Nozzle, TileEldritchPortal.
* **Not touched in this pass:** TileArcaneDoor, TileFocus*, TileJarBrain, TileJarNode, TileArcaneBoreBase,
  TileAlchemyFurnaceAdvancedPart, TileVisRelay, TilePedestal, TileHole, TileTube*, TileSensor,
  TileManaPod, TileMagicBox, TileWandPedestal, TileResearchTable (logic), TileDeconstructionTable (GUI).

Every fix below is committed in `port/octiles` with the oracle file and line in the comment.

---

## 1. Defects found and fixed (oracle → port → fix)

### 1.1 The eldritch obelisk DEBUFFED the eldritch mobs instead of empowering them

* **Oracle** `thaumcraft/common/tiles/TileEldritchObelisk.java:47-55`:
  ```java
  if (!(e instanceof IEldritchMob) || !(e instanceof EntityLivingBase)
      || ((EntityLivingBase)e).func_82165_m(Potion.field_76428_l.field_76415_H)) continue;
  ((EntityLivingBase)e).func_70690_d(new PotionEffect(Potion.field_76420_g.func_76396_c(), 40, 0, true));
  ((EntityLivingBase)e).func_70690_d(new PotionEffect(Potion.field_76428_l.field_76415_H, 40, 0, true));
  ```
  With the 1.7.10 mapping `field_76420_g = damageBoost` and `field_76428_l = regeneration`
  (`resolve_srg.py field_76420_g field_76428_l`). The guard is `isPotionActive(regeneration)`.
  So the obelisk **gives damage boost + regeneration (40 ticks, ambient)** and skips a mob that
  already regenerates. It never slows anything down.
* **Port (before)** `common/src/main/java/thaumcraft/common/tiles/TileEldritchObelisk.java:35-41`:
  `MOVEMENT_SLOWDOWN` + `DIG_SLOWDOWN`, guarded on `DIG_SLOWDOWN`. That matches **neither** the 1.7.10
  oracle nor the 1.12 reference in `src/` (which uses `REGENERATION` + `RESISTANCE`), i.e. it was a
  pure misreading.
* **Fix** `TileEldritchObelisk.java` + `EldritchBlockRules.java`:
  damage boost and regeneration (ambient, 40 ticks, amplifier 0), guarded on regeneration.
  The rule `obeliskAffects(eldritch, living, alreadyRegenerating)` was renamed accordingly and the
  guard test moved from the dig slowdown to the regeneration.
* **Also fixed:** the original never raises its counter, so the offer happens on **every** server tick
  (`OBELISK_TICK_PERIOD` 20 → 1); the regeneration guard is what limits it to one pair every 40 ticks.
  The port also saved a `counter` in NBT although the original obelisk has **no** NBT at all — the
  counter was removed from the disk save.

### 1.2 The arcane lamp placed its light into cells that were lit by torches

* **Oracle** `thaumcraft/common/tiles/TileArcaneLamp.java:35`:
  `world.func_72957_l(x, y, z) < 9` — `World.getLightValue` is the **larger** of the block light and
  the sky light.
* **Port (before)** `TileArcaneLamp.java`: `server.getRawBrightness(candidate, 0)` — the **sky light
  only** (`LevelReader.getRawBrightness` is the sky light minus the ambient darkness).
  A cell lit by a torch (block light 15, sky light 0) counted as dark and received a lamp light.
* **Fix** `ArcaneLampRules.darkEnough(blockLight, skyLight)` → `Math.max(block, sky) < 9`, and the
  tile now asks for both layers (`LightLayer.BLOCK` / `LightLayer.SKY`).

### 1.3 The thaumatorium threw away formulas of a fully upgraded machine

* **Oracle** `thaumcraft/common/tiles/TileThaumatorium.java:494-516` (`getUpgrades`): `mr = 1` and
  `mr += 2` for every mnemonic matrix that stands on one of the scanned cells — two levels
  (`yy = 0..1`) times every direction but down and the front (`ForgeDirection.VALID_DIRECTIONS` minus
  `DOWN` minus `facing`). With the machine facing down that is ten cells, so the machine can hold
  **1 + 2 × 10 = 21** formulas.
* **Port (before)** `TileThaumatorium.java`: `Math.clamp(tag.getInt("MaxRecipes"), 1, 19)` and
  `list.size() > 19` → a machine with 20 or 21 slots lost one or two formulas on every reload
  (`refreshUpgrades()` then trimmed the job list to the clamped capacity).
* **Fix** `ThaumatoriumRules.MAX_RECIPES = 21` (`BASE_RECIPES = 1`, `RECIPES_PER_MATRIX = 2`,
  `MAX_MATRIX_SLOTS = 10`) used by the NBT clamp, by the recipe-list bound and by `refreshUpgrades()`
  through the new `recipeCapacity(matrices)`.

### 1.4 The mirrors ran their three schedules one tick late

* **Oracle** `thaumcraft/common/tiles/TileMirror.java:287-306`: the counter is **read before it is
  raised** — `if (this.count++ % this.inc == 0)`. `checkInstability()` (line 310) and `eject()`
  (line 199, `this.count > 20`) read the same, still unraised counter.
  `TileMirrorEssentia.java:224` does the same with `this.count++ % this.inc == 0`.
* **Port (before)** `TileMirror.java` / `TileMirrorEssentia.java`: `mirror.count++` came **first**,
  so the ejection rate, the instability decay (`count % 20`) and the link retry (`count % inc`) were
  all shifted by one tick, and the first link check happened on the fortieth tick instead of the
  first.
* **Fix** both tiles now read the counter first and raise it at the end of the tick.

### 1.5 The crucible counted its bellows again on every tick

* **Oracle** `thaumcraft/common/tiles/TileCrucible.java:69, 98-100, 360-371`: `int bellows = -1;`
  and `if (this.bellows < 0) { this.getBellows(); }` — the count is found **once**, on the first
  server tick, and is never invalidated (grep over the oracle shows no other write to the field).
  The arcane furnace, in contrast, counts its bellows on every tick (`TileArcaneFurnace.java:227`).
* **Port (before)** `TileCrucible.java`: `bellows()` re-counted the four horizontal bellows on
  every tick, so a bellows that was placed or removed later changed the heat rate immediately.
* **Fix** the count is cached in a field that starts at `-1` and is filled on the first server tick.
  Consequence (same as the original): the heat rate of a crucible is decided when it is first ticked
  and only changes again when the chunk is read again.

### 1.6 The item mirror waited for the pickup delay before it took an item

* **Oracle** `thaumcraft/common/blocks/BlockMirror.java:163`:
  `((EntityItem)entity).field_71088_bW == 0`. The field `field_71088_bW` is declared on
  **`net/minecraft/entity/Entity`** (obf `am`) and is **not** a field of `EntityItem`
  (`srg1710.json`); in the 1.7.10 mapping it is `Entity.timeUntilPortal`
  (`resolve_srg.py field_71088_bW`). It is therefore *not* the pickup delay: the original takes the
  item as soon as it enters the mirror. The 1.12 reference (`src/…/BlockMirror.java:168`) uses
  `cannotPickup()`, i.e. the *1.12* mapping of the same SRG name, and the port had inherited that
  1.12 behaviour.
* **Port (before)** `BlockMirror.java:119`: `item.hasPickUpDelay()` → the item waited the vanilla
  pickup delay (10 ticks) and a player could pick it up in the meantime.
* **Fix** the pickup-delay gate was removed. The spawned item keeps the vanilla default delay
  (`ItemEntity.setDefaultPickUpDelay()` = 10 ticks in 1.21.1), which is exactly the 1.7.10 default
  of `EntityItem`; the original's `ie2.field_71088_bW = 20` (`TileMirror.java:221`) only set the
  *portal* cooldown and therefore never delayed the pickup.

### 1.7 The brain jar pulled every orb and swallowed them too late

* **Oracle** `thaumcraft/common/tiles/TileJarBrain.java:53-113`:
  * the pull is applied to the **closest** orb of the six blocks around the jar
    (`getClosestXPOrb()`, lines 114-130) with the strength `(1 − distance/7)²`
    (lines 58-63);
  * the jar swallows **every** orb that lies inside its own block
    (`AxisAlignedBB(x−0.1 … x+1.1)`, line 108) and the sound is `random.eat` with the volume 0.1;
  * the eat delay is only ever set to forty by the block that empties the jar
    (`BlockJar.java:241`); the tile itself never invents a delay.
* **Port (before)** `TileJarBrain.java`:
  * every orb of a ±7 box around the centre was pulled at once (the original pulls only the closest);
  * an orb was swallowed when it came within one block of the centre (the original swallows the orbs
    that are inside the block);
  * the jar invented an `eatDelay = 5` after every meal (the original has no such delay) and it
    clamped the stored experience while it ate (the original caps it at the start of the next tick).
* **Fix** `TileJarBrain.java`: the closest orb is found in the block box inflated by six and pulled
  with the original formula, the orbs inside the block are swallowed with the original sound, the
  eat delay is only the one the block sets, and the cap of two thousand is applied where the original
  applies it.

---

## 2. Numbers that were verified against the oracle and found correct
| Tile | Oracle | Value | Port |
|---|---|---|---|
| Crucible | `TileCrucible.java:71` | 1000 mB water | `TileCrucible.CAPACITY` |
| | `:68` | 100 aspects | `MAX_TAGS` |
| | `:108` / `:153` | boils at 151 | `CRUCIBLE_BOIL_HEAT` |
| | `:106` | heat stops at 200 | `CRUCIBLE_MAX_HEAT` |
| | `:107` | heat step 1 + 2 per bellows | `crucibleHeatStep` |
| | `:114` | cool step 1 | `CRUCIBLE_COOL_STEP` |
| | `:123` | overflow every 5th tick | `CRUCIBLE_OVERFLOW_INTERVAL` |
| | `:127` | decay at counter > 100 and heat > 150 | `crucibleDecays` |
| | `:135` | decay spends 2 mB | `CRUCIBLE_DECAY_WATER` |
| | `:264` | smelt spends 50 mB | `CRUCIBLE_SMELT_WATER` |
| | `:315-325` | fluid height 0.3 + 0.5·water/cap + tags/100·(1−base), clamped to 1.001/0.9999 | `crucibleFluidHeight` |
| | `:250-285` | ceil(stack/2) items per collision | port comment + loop shape |
| | `:132-145` | the primal aspect is redrawn **once**, not searched | port comment + code |
| Essentia reservoir | `TileEssentiaReservoir.java:35` | 256 units in total | `ReservoirRules.CAPACITY` |
| | `:164` | minimum suction 24 | `ReservoirRules.SUCTION` |
| | `:201` | one unit every 5th tick | `ReservoirRules.PULL_INTERVAL` |
| | `:205` | creak in 1/(500−units) | `ReservoirRules.creaks` |
| | `:208-217` | display aspect cycles every 20 ticks, colour walks 1/20 | `ReservoirColour` |
| | `:56-58` | more than the capacity → an **empty** list, not a truncated one | `TileEssentiaReservoir.loadAdditional` |
| | `:89-92` | the byte `face` is the ForgeDirection ordinal (= the 1.21 `get3DDataValue`) | `TileEssentiaReservoir` NBT |
| Infusion matrix | `TileInfusionMatrix.java:110` | craft cycle 10 ticks | `INFUSION_CYCLE_DELAY` |
| | `:431` | 20 ticks while an XP level is paid | `INFUSION_XP_CYCLE_DELAY` |
| | `:217` | structure check every 100 (20 while crafting) | `infusionStructurePeriod` |
| | `:349` | instability rolls against 500 | `INFUSION_INSTABILITY_ROLL` |
| | `:350` | 21 outcomes | `INFUSION_OUTCOMES` |
| | `:441-443` | instability cap 25 | `INFUSION_INSTABILITY_CAP` |
| | `:480` | ingredient countdown 5 | `INFUSION_INGREDIENT_COUNTDOWN` |
| | `:614-617` | surroundings cube −12..12 / −5..10 | `INFUSION_SCAN_*` |
| | `:623` | pedestals only inside the square of 17 | `INFUSION_PEDESTAL_RADIUS` |
| | `:634-668` | symmetry: +2/+1 per pedestal, −2/−1 for the mirrored one, ±0.1/0.2 per stabiliser | `InfusionStructure.scan` |
| | `:456` | essentia source range 12 | `INFUSION_SOURCE_RANGE` |
| Thaumatorium | `TileThaumatorium.java:183` | work every 5th tick | `THAUMATORIUM_WORK_INTERVAL` |
| | `:178` | heat + upgrades every 40th tick | `THAUMATORIUM_HEAT_INTERVAL` |
| | `:360` | suction 128 | `THAUMATORIUM_SUCTION` |
| | `:461` | catalyst slot holds 64 | `THAUMATORIUM_STACK_LIMIT` |
| | `:465` | usable from 64 blocks | `THAUMATORIUM_USE_RANGE` |
| | `:494-516` | 1 formula + 2 per matrix, max 21 | `ThaumatoriumRules` (**fixed**) |
| Jar | `TileJarFillable.java:28` | 64 units | `JarContents.CAPACITY` |
| | `:200` | one unit every 5th tick | `JAR_PULL_INTERVAL` |
| | `:158, 167-175` | suction 32 / 64 (labelled) | `jarSuction` |
| | `TileJarFillableVoid.java:41, 46` | 48 / 32 for the void jar | `jarSuction` |
| | `TileJarFillableVoid.java:24-31` | the void jar consumes the whole offer but stores at most 64 | `JarContents.add(..., voidOverflow)` |
| | `TileJarFillable.java:116-120` | the deprecated `doesContainerContain` is an intersection test | port |
| Mirror | `TileMirror.java:64, 297-300` | link retry 40, +20 up to 600 | `EldritchBlockRules` |
| | `:310` | instability −1 every 20 ticks | `MIRROR_INSTABILITY_DECAY` |
| | `:290` | ejection rate = (instability/50)² | `mirrorEjectRate` |
| | `:199` | nothing leaves in the first 20 ticks | `MIRROR_EJECT_DELAY` |
| | `:346-348, 387-389` | inventory 1 slot, stack limit 64 | `MIRROR_INVENTORY_SLOTS` |
| | `TileMirrorEssentia.java:186-190` | at most one unit per request | `MIRROR_ESSENTIA_MAX_TAKE` |
| | `:194` | the twin is looked at 8 blocks deep | `MIRROR_ESSENTIA_RANGE` |
| Alembic | `TileAlembic.java:44` | 32 units | `ALEMBIC_CAPACITY` |
| | `:194-205` | connected on every side but down and the front | `alembicConnectable` |
| Node stabilizer | `TileNodeStabilizer.java:22` | lock 1 (9) / 2 | `TileNodeStabilizer.lock` |
| | `:34-45` | the client animation counts to 37 | `animationCount` |
| Node energized | `TileNodeEnergized.java:28` | aura 6 × 20 | port |
| | `:60-72` | BRIGHT 1.2, PALE 0.8, FADING 0.5, then the float sqrt | `TileNodeEnergized.baseRate` |
| | `:73-75` | unstable: +nextInt(5)−2 | `calculateCapacity` |
| | `:40` | unstable reset on nextInt(500) == 1 | `serverTick` |
| Flux scrubber | `TileFluxScrubber.java:36-53` | 4 charges per essentia roll of 1/4, at most 4 essentia | `FLUX_*` |
| | `:58-60` | power < 5 → draw 10 AIR vis | `FLUX_POWER_DRAIN` |
| | `:62-104` | cube of 33³, 16 cells a pass, 5 power a block | `FLUX_SCAN_*` |
| Deconstruction table | `TileDeconstructionTable.java:151, 186` | 40 ticks, 1/80 chance | `DeconstructionRules` |
| Grate | `TileGrate.java:41-47` | the item leaves at 0.6 with a fall of 0.1 | `GrateRules` |
| Brain jar | `TileJarBrain.java:24-25` | at most 2000 experience | `TileJarBrain.xpMax` |
| | `:58-63` | the pull is `(1 − distance/7)²` on 0.15/0.33/0.15 | `TileJarBrain.tick` (**fixed**) |
| | `:114-130` | the closest orb of the six blocks around the jar | `TileJarBrain.closestOrb` (**fixed**) |
| Banner / brainbox / pressure plate / eldritch nothing | `TileBanner.java:63-77`, `TileBrainbox.java:23-30`, `TileArcanePressurePlate.java:26-33` | the NBT fields `facing`, `color`, `aspect`, `wall` / `facing` / `setting` | port |

---

## 3. NBT: what the original stores and what the port stores

| Tile | Oracle NBT | Port NBT | Note |
|---|---|---|---|
| Crucible | `Heat`, the tank (`Empty` + `Fluid`), `Aspects` (`TileCrucible.java:78-92`) | `Heat`, `Water`, `Aspects` | the tank became a plain integer; the values are the same |
| Essentia reservoir | `Aspects`, `face` (`:54-66`) | `Aspects` list, `face` byte | the same byte value (ForgeDirection ordinal == `get3DDataValue`) |
| Jar | `Aspect`, `AspectFilter`, `Amount` (short), `facing` (byte) (`TileJarFillable.java:40-57`) | the same names; `Amount` is an int | the port clamps the amount to 64 |
| Alembic | `facing`, `AspectFilter`, `aspect`, `amount` (short) (`TileAlembic.java:65-86`) | the same; the facing also lives in the block state | the port keeps the facing in the native state, the NBT copy is written for the original shape |
| Infusion matrix | `active`, `crafting`, `instability`, `Aspects` + `recipein`, `rotype`, `recipeout`, `recipeinput`, `recipeinst`, `recipetype`, `recipexp`, `recipeplayer` (`TileInfusionMatrix.java:119-201`) | the same names; the components are stored as native predicates | the port additionally stores `RecipeCost` and `RecipeEnchantment` |
| Thaumatorium | `facing`, `maxrec`, `Aspects`, `recipes` (int hashes), `Items`, `OutputPlayer` (`:86-154`) | `MaxRecipes`, `Recipes` (recipe ids), `Essentia`, `Input`, `PendingOutput`, `Recipe` | the int recipe hashes cannot survive 1.21; the recipe ids replace them |
| Mirror | `linked`, `linkX/Y/Z`, `linkDim`, `instability`, `Items` (`TileMirror.java:242-262, 320-344`) | the same names; `linkDim` is a dimension key string instead of an int | the int dimension id has no 1.21 equivalent |
| Mirror (essentia) | `linked`, `linkX/Y/Z`, `linkDim` | the same | |
| Flux scrubber | `facing`, `charges`, `power`, `essentia` (`:92-117`) | the same | |
| Eldritch obelisk | **nothing** | nothing (the `counter` was removed) | fixed |
| Eldritch portal | **nothing** (`opencount`/`count` are transient) | `opencount`, `count` | minor deviation, client animation only |
| Infusion pillar | `orientation` (byte) (`TileInfusionPillar.java:31-39`) | the orientation lives in the block state | |
| Deconstruction table | `Aspect`, `Items`, `CustomName` (`:97-138`) | the same names | |
| Grate, banner, brainbox, pressure plate, eldritch nothing | the original fields | the same names | |

**Network note for the packet agent** (not fixed here, they are client-side surfaces):

* The original `TileAlembic.onDataPacket` calls `getAppearance()`, which sets `aboveAlembic` /
  `aboveFurnace`; the port has no such flags, so the alembic cannot tell whether it stands on
  another alembic or on a furnace.
* The original `TileThaumatorium.func_145842_c(i, j)` with `i >= 0` sets `venting = 7`, which
  draws the vent particles on the client; the port's `TileThaumatorium` has no `triggerEvent`
  override, so the block event is swallowed.
* The original `TileMirror.transport` fires the block event `blockMirror, 1, 0`, which the client
  turns into the spell particles; the port plays a beacon sound instead and the tile has no
  `triggerEvent` override.
* The original `TileArcanePressurePlate.onDataPacket` marks the block for an update on the client.

---

## 4. Gaps that are still open

1. **Crucible client effects.** `TileCrucible.setClientEffects(...)` is never called anywhere in the
   repository, so the client side of the crucible is missing entirely: the froth and the bubbles of
   `drawEffects` (`TileCrucible.java:158-184`), the boil sound of `func_145842_c(2, …)` and the
   client heat quirk of `TileCrucible.java:153-155`
   (`if (world.isRemote && prevheat < 151 && heat >= 151) heat++;`).
2. **The item mirror has no inventory surface.** The original `TileMirror` implements `IInventory`
   with **one** slot and a stack limit of 64 (`TileMirror.java:346-348, 387-389`);
   `func_70299_a` forwards an inserted stack to the linked twin (or ejects it). The port's
   `TileMirror` has no `Container`/`WorldlyContainer` surface, so a hopper cannot push an item into
   a mirror.
3. **Client particles of the infusion matrix** (`TileInfusionMatrix.doEffects`, oracle lines
   703-782) are not reproduced in the port; the port sends FX through `InfusionFxNetworking`
   (owned by the root/network agent).
4. **Not audited line by line in this pass:** TileArcaneBore (40 kB oracle file, already covered by
   `port/ocblocks`), TileResearchTable, TileNodeConverter, TileArcaneWorkbench, TileEldritchPortal,
   TileAlchemyFurnaceAdvanced, TileArcaneDoor, TileFocus*.
5. **The node jar has no `IAspectContainer` surface.** The original `TileJarNode` implements it
   (`TileJarNode.java:110-131`: the stored aspects may not exceed the stored base), the port's
   `TileJarNode` does not. Nothing in the original flow calls it (the jar is only filled through
   `setAspects`), but the API is missing.
6. **The eldritch portal keeps two transient counters in NBT.** The original `TileEldritchPortal`
   stores nothing; the port saves `opencount` and `count`. Only the client animation and the
   250-tick ambient sound read them.
7. **No client acceptance.** Nothing in this pass was verified in a running client; only the server
   unit suite and the compilation of the game-test harness were executed.

---

## 5. Verification

Command (as required by the task):

```
export JAVA_HOME=/home/Acak/.jdk21
export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/octiles
./gradlew --no-daemon --console=plain :common:test
```

Result: **BUILD SUCCESSFUL**, `common/build/test-results/test` contains **241 test classes,
1688 tests, 0 failures, 0 errors, 0 skipped**.

The game-test harness was compiled with `:common:compileGameTestJava` → **BUILD SUCCESSFUL**
(the eldritch obelisk check was adapted to the corrected effects).

New test coverage added by this pass:

* `common/src/test/java/thaumcraft/tiles/TileParityRulesTest.java` — the numbers of the crucible,
  the jars, the alembic, the infusion matrix, the thaumatorium and the mirrors against the oracle.
* `common/src/main/java/thaumcraft/common/lib/tiles/TileParityRules.java` — the same numbers in
  production code, each with the oracle file and line.
* Updated: `EldritchObeliskRulesTest`, `EldritchBlockRulesTest` (obelisk), `ArcaneLampRulesTest`
  (the lamp).

---

## 6. Summary of the commits

| Commit | Content |
|---|---|
| `7804b2a` | the six fixes of section 1.1–1.6, the new `TileParityRules` and its test |
| follow-up commit | the brain jar fix of section 1.7 and the report update |
