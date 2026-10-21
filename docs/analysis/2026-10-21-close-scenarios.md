# Closing the red GameTest scenarios of the TC4 1.21.1 port (2026-10-21)

Branch: `port/fixscenarios` (based on `work` @ `5084a65`).
Scope: the four red scenarios plus the unstable one, the one port deviation they exposed and the
determinism of the whole GameTest suite.

Everything below was checked against the decompiled 1.7.10 original
(`/home/Acak/work/thaumcraft-4.2.3.5-oracle`), the readable OpenTC4 sources and the readable
vanilla 1.7.10 sources. The SRG names of the oracle were resolved with
`scripts/porting/resolve_srg.py`.

---

## 1. The real cause of the four red scenarios

The four scenarios did **not** fail because of a wrong expectation about the original. They failed
because the vanilla GameTest framework packs the scenarios of a batch a few blocks apart while the
port's fixtures reach far over that distance, so a scenario of the same batch reached into the room
of another one.

Evidence (full suite, `./gradlew :fabric:runGameTest`, 312 tests):

* The vanilla runner places the scenarios of a batch on a grid of about twenty five blocks
  (`net/minecraft/gametest/framework/StructureGridSpawner.java:26-27`: `SPACE_BETWEEN_COLUMNS = 5`,
  `SPACE_BETWEEN_ROWS = 6`, plus the size of the `minecraft:empty` template).
* `net/minecraft/gametest/framework/GameTestInfo.java:259-266` — when a scenario **succeeds**, the
  framework discards *every* non player entity inside the structure bounds of the finished
  scenario: `entities.forEach(e -> e.remove(RemovalReason.DISCARDED))`.
* The diagnostics of the failing runs showed exactly that removal reason:

```
DIAG bookshelf alive=false removed=true reason=DISCARDED ... age=0 tick=8
DIAG fetter    lane=held golem (active fetter) fettered=false alive=false removed=true reason=DISCARDED
DIAG decorations lane=VISOR guard alive=false removed=true reason=DISCARDED hp=35.0 maxhp=35.0
```

  The book never ticked (`age=0`) and the golem never computed `isFettered`; both were discarded by
  the cleanup of a **neighbouring** scenario while they were still the subject of their own
  scenario.
* The construct scenario failed because a foreign actor occupied the block the fixture wanted to
  build on: `use=FAIL hand=1 thaumcraft:alchemical_construct ... near=[entity.minecraft.player@...]`.
  The native placement refuses a block that an actor occupies, so the scenario could not observe
  the original behaviour at all.

So the fixtures could not let their object behave as in the original. The scenarios are therefore
fixed in two ways: the fixture now owns its room (own batch, own spot, own chunk) and the
expectations are the ones of the original.

---

## 2. The scenarios

### 2.1 `bookshelfwandcreatesonefloatingbook`

**What it checked wrongly.** The scenario waited eight world ticks (`test.runAfterDelay(8, ...)`)
and then asserted that the result of the wand still floats. That assertion is right, but the
fixture is not: the result is a world entity, and any scenario of the same batch may discard it
while the eight ticks pass. The scenario also asserted that the special item "resists fire", which
the original does **not** do.

**What it checks now.**

* The original creates the result at `x+0.5 / y+0.3 / z+0.5` with a zero motion
  (oracle `thaumcraft/common/items/wands/WandManager.java:408-421`) — asserted now.
* The hover of the original: `EntitySpecialItem.onUpdate` damps a rising motion by `0.9` and adds
  `0.04` before the vanilla item gravity takes `0.04` again
  (oracle `thaumcraft/common/entities/EntitySpecialItem.java:37-43` with the readable 1.7.10
  `net/minecraft/entity/item/EntityItem.java:82`), so the item stays exactly where it was
  created. The fixture now drives the eight ticks of the entity itself, so the result cannot be
  taken away by a neighbour while the scenario waits for it.
* The result is not picked up: the fixture's actor is not in the world, and the scenario asserts
  that the book is still there with its single book.
* The damage rule of the original: the special item refuses an **explosion** and takes every other
  damage like a vanilla item entity (oracle `thaumcraft/common/entities/EntitySpecialItem.java:45-49`,
  `if (source.isExplosion()) return false;`).

**Port deviation found and fixed.** `common/src/main/java/thaumcraft/common/entities/EntitySpecialItem.java`
refused **fire** (`DamageTypeTags.IS_FIRE`) instead of explosions. The 1.7.10 original
(`EntitySpecialItem.java:45-49`) refuses `DamageSource.isExplosion()`, the 1.12 reference of this
repository (`src/main/java/thaumcraft/common/entities/EntitySpecialItem.java`) does the same, and
the audit of the port itself states that the Primordial Pearl is an "explosion-immune special
drop" (`docs/goals/2026-08-03-eldritch-full-parity-audit-fixes/reports/A-017-outer-entities-drops.md:442`).
The port now refuses explosions. In 1.21.1 the vanilla item entity ignores fire damage on its own,
so the item no longer dies in fire in the port either.

> Not changed on purpose: `EntityPermanentItem` and `EntityFollowingItem` still use the fire rule.
> They belong to the eldritch/axe-stream domain, they inherit no behaviour from
> `EntitySpecialItem` and they are not part of these scenarios. The deviation is recorded here for
> the owner of that domain.

### 2.2 `constructandcentrifugerecipesandplainnativeblocks`

**What it checked wrongly.** The fixture built the construct on the first free spot of its room and
the native placement of a `BlockItem` refuses a block that an actor occupies. A player of a
neighbouring scenario could stand on that spot, so the scenario failed with
`InteractionResult.FAIL` without ever testing the original behaviour.

**What it checks now.** The fixture builds the floor first and then takes the first spot of its own
room that **no actor occupies**; it never touches an actor of another scenario. The original
behaviour that is checked is unchanged and stays the one of the oracle: the Alchemical Construct is
`blockMetalDevice` metadata 9 (oracle `ConfigRecipes.java:290`,
`new ItemStack(ConfigBlocks.blockMetalDevice, 1, 9)`), it is an ordinary placeable block
(oracle `BlockMetalDeviceItem.java` places metadata 9 through the normal path), it has no tile
entity (`BlockMetalDevice.createTileEntity` has no case for 9), it is a full opaque cube with
hardness 3 (`setHardness(3.0f)`), and it is placed without any crucible or assembly prerequisite.

### 2.3 `golemfetterholdsgolemswhileitsredstonesignallasts`

**What it checked wrongly.** Nothing about the expectation was wrong; the fixture could not keep the
lane's golem alive: the golem was discarded by the cleanup of a neighbouring scenario (removal
reason `DISCARDED`) before it had ticked once, so `isFettered` was still its initial value.

**What it checks now.** The original is unchanged and is the oracle's rule: the golem is held while
the block under its feet is the active fetter — the original computes
`this.inactive = yy > 0 && world.getBlock(xx, yy - 1, zz) == ConfigBlocks.blockCosmeticSolid &&
world.getBlockMetadata(xx, yy - 1, zz) == 10` on every living update
(oracle `thaumcraft/common/entities/golems/EntityGolemBase.java:425`, with the metadata 9/10 pair of
the cosmetic solid, `BlockCosmeticSolid.java:360-370`), and `isAIEnabled` is
`!paused && !inactive` (`EntityGolemBase.java:404-406`). The fixture now arms a lane again when a
neighbouring scenario discarded its golem, so the scenario keeps checking the original rule instead
of a missing entity.

### 2.4 `golemaccessoriesinstallchangestatsandsurvivethebell`

**What it checked wrongly.** The same interference: the Guard of the VISOR lane was discarded by a
neighbouring scenario (removal reason `DISCARDED`, full health), so "the Guard survived" was read
from an entity that no longer existed. The scenario additionally swept the entities of a wide lane
and thereby discarded the golems of the **other** golem scenarios.

**What it checks now.**

* The cleanup only discards the golems of this scenario, never the golems of a neighbouring one.
* A lane whose Guard or spider was discarded by a neighbouring scenario is armed again, so the
  scenario keeps checking the original rule.
* The checked behaviour is the original one and unchanged: the visor decoration gives the original
  `+1` armour (`EntityGolemBase.java:1088-1099`) and marks the victim as recently hit
  (`EntityGolemBase.java:1102-1116`), which is what the experience of the lane reads.

### 2.5 `essentiareservoirholdsmixedessentiapullsandbursts`

**What it checked wrongly.** The fixture put its rooms into the band a hundred blocks above the
GameTest structure — the same band as the Portable Hole, Deconstructor and Thaumatorium scenarios —
and spread them over ninety blocks in z. The GameTest grid places a neighbouring scenario about
twenty five blocks away, so the rows of this scenario landed on the rooms of the neighbours, which
wiped the reservoirs: the direct reservoir was empty after twelve ticks and the whole buffer was
still full (`after twelve ticks one to three units came over and none was lost: {} {...}`).

**What it checks now.** The scenario has a band of its own (a hundred and forty blocks above the
structure) and it forces the chunks of its own rows, so the reservoirs really tick. The original
rule that is checked is unchanged: a reservoir pulls a unit every fifth tick
(`ReservoirRules.PULL_INTERVAL == 5`, `SUCTION == 24`) from the buffer next to it, an opening that
faces away draws nothing, a closed buffer side gives nothing and a full reservoir draws nothing.

### 2.6 `champion_modifiers_answer_their_hit_and_wound` and the champion join roll of the suite

The scenario let the world random decide whether the venomous champion poisoned its victim: the
original poisons on four of every ten hits (oracle `ChampionModPoison`, the port
`ChampionModPoison.java:11`), the scenario looked at the effect only after twenty hits and two
hundred and forty ticks, and the poison of the original lasts a hundred ticks — so the poison of
the last hit had already expired in about one run of twenty. The scenario now declares that
original roll with the existing helper (`ChampionJoinRoll.declaredEffect`), exactly like the
existing `join`/`declared` declarations of the original join roll, and the real world stream still
advances by the original single draw.

The same run showed the root of that class of flake: **the original rolls the champion promotion of
every hostile mob that joins the world** (oracle `EventHandlerEntity.entitySpawns:391-419`), so a
scenario that adds a hostile mob and reads its own numbers depends on the world stream unless it
declares that draw. The run failed with
`the watcher keeps the original 30 health, 6 damage, 0.50 speed and 16 follow range: 60.0 12.0 0.5`
— the watcher had been promoted to a champion (thirty health and double damage more). Every
scenario that adds a hostile mob now declares that original roll with
`ChampionJoinRoll.join(level, mob)` (the declared value keeps the mob an ordinary mob of the
original and the world stream still advances by the original single draw): the eldritch bestiary
(crab, watcher, slime, inhabited zombie), the projectiles (thrower, shooter), the travelling trunk
(attacker), the taint block (zombie), the fortress (zombies) and the four golem scenarios that use
a spider as their target.

### 2.7 The remaining fragile rooms of the suite

Two scenarios of the suite lost their object because a neighbouring scenario reached into their
room; they are not part of the five scenarios of this task, but they broke the green runs and are
fixed here as well:

* `swordofthezephyrliftsblowsandsweeps` — the sweep happens thirty blocks further along z than the
  room the scenario loads the chunks of, so the room of the sweep could fall outside the loaded
  chunks; the scenario now loads the chunks of its sweep room as well.
* `unknownknowledgerevealshiddennotesandretriesfragments` — every fragment drops where its actor
  stands, so the two fragments of the scenario landed on the same spot and merged into one stack
  (the item entity of 1.21.1 merges two stacks of the same item); the scenario now uses the second
  note three blocks away, so every fragment keeps its own original amount.
* `tableFirstNativeTickRepairsReloadedOrphans` and `tableBreakEvacuatesEachHalfOnce` — the drops of
  the broken table are posted into the world and were taken by the cleanup of a neighbouring
  scenario; both scenarios now run in a batch of their own, like the scenarios of this task.
* `advancedFurnacePreservesCustodyAcrossRealChunkUnload` — the scenario waits for the native
  **asynchronous** reload of the saved shell terrain (the radius zero ticket of the original) and
  gave that reload two hundred ticks; on a loaded server the reload needs more, so the poll limit
  is now a thousand ticks, well inside the timeout of the scenario.
* `mixedCrystalOrientationSurvivesActualChunkUnload` — the same race with the reload of the crystal
  chunk and the return of its entity section: the scenario waited five hundred and forty ticks and
  the timeout of the scenario was six hundred; both are raised now.

---

## 3. The changes

| File | Change |
| --- | --- |
| `common/src/main/java/thaumcraft/common/entities/EntitySpecialItem.java` | the original explosion immunity instead of the fire rule |
| `common/src/gameTest/java/thaumcraft/common/testing/RuntimeChecks.java` | the bookshelf scenario: the original spawn point, the original hover (driven by the entity itself), the original explosion immunity, no early pickup |
| `common/src/gameTest/java/thaumcraft/common/testing/CentrifugeRuntimeChecks.java` | the construct scenario: the fixture builds on a spot that no actor occupies |
| `common/src/gameTest/java/thaumcraft/common/testing/GolemFetterRuntimeChecks.java` | the fetter scenario: a lane whose golem was discarded is armed again |
| `common/src/gameTest/java/thaumcraft/common/testing/GolemDecorationsRuntimeChecks.java` | the decoration scenario: the cleanup only discards the golems of the scenario, a discarded lane is armed again |
| `common/src/gameTest/java/thaumcraft/common/testing/ReservoirRuntimeChecks.java` | the reservoir scenario: a band of its own and forced chunks for its rows |
| `common/src/gameTest/java/thaumcraft/common/testing/ChampionJoinRoll.java` | `declaredEffect` declares the original roll of a champion effect |
| `common/src/gameTest/java/thaumcraft/common/testing/ChampionRuntimeChecks.java` | the champion scenario declares the original effect roll |
| `common/src/gameTest/java/thaumcraft/common/testing/EldritchBestiaryRuntimeChecks.java`, `ProjectileRuntimeChecks.java`, `TravelingTrunkRuntimeChecks.java`, `TaintBlockRuntimeChecks.java`, `FortressRuntimeChecks.java`, `GolemFireOrderRuntimeChecks.java`, `GolemDartRuntimeChecks.java`, `GolemUpgradeRuntimeChecks.java`, `GolemAdvancedRuntimeChecks.java` | every hostile mob that joins the world declares the original champion join roll |
| `common/src/gameTest/java/thaumcraft/common/testing/ElementalSwordRuntimeChecks.java` | the scenario loads the chunks of its sweep room |
| `common/src/gameTest/java/thaumcraft/common/testing/UnknownKnowledgeRuntimeChecks.java` | the two fragments of the scenario land apart, so they cannot merge into one stack |
| `common/src/gameTest/java/thaumcraft/common/testing/AdvancedFurnaceUnloadRuntimeChecks.java` | the asynchronous reload of the saved shell gets the time of the original |
| `common/src/gameTest/java/thaumcraft/common/testing/CrystalClustersRuntimeChecks.java` | the asynchronous reload of the crystal chunk gets the time of the original |
| `fabric/src/gameTest/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java` | the scenarios with a room of their own run in a batch of their own, so no other scenario runs beside them |
| `neoforge/src/gameTest/java/thaumcraft/neoforge/testing/ThaumcraftGameTests.java` | the same batch for the one scenario of that registry |

The neoforge registry knows only the bookshelf scenario of the five, so only that entry got the
batch; the neoforge module itself was not built or run (the port works on Fabric first).

---

## 4. The runs

`./gradlew --no-daemon --console=plain :common:test` — BUILD SUCCESSFUL, 0 failures.

`./gradlew --no-daemon --console=plain :fabric:runGameTest` with
`rm -rf fabric/run-gametest/world` before every run (312 scenarios, the world of the run is
created from scratch every time):

| run | result |
| --- | --- |
| 1 (the state of `work`, before the fixes) | 312 tests, **5 failed**: construct, bookshelf, fetter, decorations, sword of the zephyr |
| 2 (after the first fixes) | 312 tests, 0 failed |
| 3 (after the first fixes) | 312 tests, 1 failed: unknown knowledge |
| 4 (after the first fixes) | 312 tests, 3 failed: unknown knowledge, sword of the zephyr, table orphan repair |
| 5 (after the champion declarations) | 312 tests, 1 failed: eldritch bestiary (a promoted watcher) |
| 6 (after the champion declarations) | 312 tests, 0 failed |
| 7 (after the champion declarations) | 312 tests, 1 failed: travelling trunk |
| 8 (after the trunk isolation) | 312 tests, 1 failed: advanced furnace (a real chunk unload) |
| 9 (after the trunk isolation) | 312 tests, 0 failed |
| 10 (after the trunk isolation) | 312 tests, 1 failed: crystal cluster (a real chunk unload) |
| 11 (after the chunk waits) | 312 tests, **0 failed** |
| 12 (after the chunk waits) | 312 tests, **0 failed** |
| 13 (after the chunk waits) | 312 tests, **0 failed** |

The three runs 11, 12 and 13 are the runs of the final state of the branch: they are consecutive and
green (`========= 312 GAME TESTS COMPLETE IN ...` with no `required tests failed` line and the exit
code of the task `0`). The runs before them show every failure of the way and what removed it.

The unit suite of the common module (`./gradlew :common:test`) is green as well.

---

## 5. What is still open for the owner of the game test suite

The interference described in section 1 is a property of the whole suite, not of the five
scenarios: several scenarios place their rooms over more than a hundred blocks
(`GolemDartRuntimeChecks` uses `z+240` to `z+440`, `GolemFireOrderRuntimeChecks` uses `z+0` to
`z+160`, `GolemDecorationsRuntimeChecks` uses `z+40` to `z+120` and `x+22`), while the GameTest
grid places the scenarios of a batch about twenty five blocks apart. Every scenario that reaches
over that distance can wipe the room of a neighbour (the cleanup of a scenario puts the blocks of
its room back to the state of the empty structure) and can discard the entities of a neighbour.
The scenarios of this task are now immune (own batch, own band, own chunks, re-arming lanes), but
the remaining wide scenarios of the suite still disturb their neighbours — the sweep of the zephyr
sword that failed in the first run above is one of those symptoms. The durable fix for the suite
is to give every scenario a room of its own (a structure template that reserves the space, or a
private height band like the reservoir scenario now uses) and to keep every cleanup inside the
room of its own scenario.
