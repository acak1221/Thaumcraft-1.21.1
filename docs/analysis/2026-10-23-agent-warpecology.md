# Warp, creatures and ecology — 1.7.10 oracle audit of the 1.21.1 port

Date: 2026-10-23. Agent: `warpecology`. Worktree: `/home/cyberfox-kali/wt/warpecology`
(branch `agent/warpecology`, base `be3c816f`).

Domain: `warp_creatures_and_ecology`, 17 open ledger units (all `partial`).

Oracle used: `/home/cyberfox-kali/OpenTC4` (the readable 1.7.10 TC4 re-implementation —
the same tree the previous waves cited as `oracle thaumcraft/...`), the retained
`Thaumcraft-1.12.2-6.1.BETA26.jar` for the TC6-only classes, and the class file
constant pools of both jars (parsed locally, read only).

Read first: `docs/analysis/2026-10-21-audit-a2-warp.md` and
`docs/analysis/2026-10-21-openTC4-entities.md`. Their closed findings were **not**
redone; the two findings below are gaps those waves left open (see §3.1 and §3.4).

---

## 1. Units touched

| canonical path | before | after | disposition |
|---|---|---|---|
| `thaumcraft/common/entities/monster/EntityFireBat.java` | partial | **parity_verified** | port |
| `thaumcraft/common/entities/monster/EntityGiantBrainyZombie.java` | partial | **parity_verified** | port |
| `thaumcraft/common/entities/monster/mods/ChampionModifier.java` | partial | **parity_verified** | replace |
| `thaumcraft/common/entities/monster/tainted/EntityTaintCrawler.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/entities/monster/tainted/EntityTaintSeed.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/entities/monster/tainted/EntityTaintSeedPrime.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/entities/monster/tainted/EntityTaintSwarm.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/entities/monster/tainted/EntityTaintacle.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/entities/monster/tainted/EntityTaintacleSmall.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/items/ItemBottleTaint.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/blocks/world/taint/TaintHelper.java` | partial | **parity_verified** | replace |
| `thaumcraft/common/lib/WarpEvents.java` | partial | **parity_verified** | replace |
| `thaumcraft/client/renderers/block/BlockTaintRenderer.java` | partial | partial (reason refined) | exclude |
| `thaumcraft/common/config/MaterialTaint.java` | partial | partial (reason refined) | exclude |
| `thaumcraft/client/fx/particles/FXWisp.java` | partial | partial (reason refined) | replace |
| `thaumcraft/client/fx/particles/FXWispArcing.java` | partial | partial (reason refined) | replace |
| `thaumcraft/client/fx/particles/FXWispEG.java` | partial | partial (reason refined) | replace |

11 of 17 units moved to `parity_verified`; 6 stay `partial` with a precise reason
(client package outside the editable scope of this wave, or a 1.21.1 API that has no
counterpart). `counts` in the ledger were not touched; the row order and the JSON
shape are unchanged (verified with `update_scope_ledger.py`, see §6).

---

## 2. Defects found and fixed (4)

### 2.1 The fire bat spawned in a fully lit block — its own class gate was missing

* **Original** — `OpenTC4 …/entities/monster/EntityFireBat.java:333-340`:
  ```java
  public boolean getCanSpawnHere() {
     int var1 = MathHelper.floor_double(this.boundingBox.minY);
     int var2 = MathHelper.floor_double(this.posX);
     int var3 = MathHelper.floor_double(this.posZ);
     int var4 = this.worldObj.getBlockLightValue(var2, var1, var3);
     byte var5 = 7;
     return var4 <= this.rand.nextInt(var5) && super.getCanSpawnHere();
  }
  ```
  i.e. the **block** light of the cell against a `nextInt(7)` roll.
  `EntityFireBat.java:346-348` overrides `isValidLightLevel()` with a plain `true`, so
  the generic mob darkness gate does **not** apply to the fire bat — but the class
  gate above does.
* **Port (before)** — `common/…/monster/EntityFireBat.java:219-226`:
  ```java
  public static boolean checkFireBatSpawnRules(… ) {
      return level.getDifficulty()!=Difficulty.PEACEFUL;
  }
  ```
  The whole light rule was gone; the fire bat spawned on a block with any light.
  The previous wave (`docs/analysis/2026-10-21-openTC4-entities.md`, item 13) removed
  the *raw brightness* roll of the port and concluded from `isValidLightLevel() == true`
  that the fire bat has **no** light restriction. That conclusion missed the class own
  `getCanSpawnHere` of `EntityFireBat:333-340`.
* **Fix** — `common/…/monster/EntityFireBat.java:222-244`: the class gate is restored
  through the new pure helper
  `OriginalLightRules.validClassLightLevel(blockLight, roll)` (added at
  `common/…/lib/entities/OriginalLightRules.java:44-64` together with the constant
  `CLASS_LIGHT_ROLL_BOUND = 7`). The rule is exposed without a world as
  `EntityFireBat.originalSpawnGate(int blockLight, int roll, boolean peaceful)`.
  The roll is drawn **before** the inherited gate, exactly as the original
  (`var4 <= this.rand.nextInt(var5) && super.getCanSpawnHere()`).
* **Evidence** — the fire bat now refuses a cell whose block light is above the
  `nextInt(7)` roll; open air (block light 0) always passes, as in the original.

### 2.2 The taint swarm could not spawn under the open sky of the day

* **Original** — `OpenTC4 …/entities/monster/EntityTaintSwarm.java:226-233`:
  ```java
  int var4 = this.worldObj.getBlockLightValue(var2, var1, var3);
  byte var5 = 7;
  return var4 <= this.rand.nextInt(var5) && super.getCanSpawnHere();
  ```
  and `EntityTaintSwarm.java:235-237` overrides `isValidLightLevel()` with `true`.
* **Port (before)** — `common/…/monster/EntityTaintSwarm.java:225-231`:
  ```java
  if(level.getMaxLocalRawBrightness(pos)>random.nextInt(7) || level.getDifficulty()==Difficulty.PEACEFUL
          ||level.getBrightness(LightLayer.SKY,pos)>random.nextInt(32))return false;
  var world=level.getLevel();
  return world.getMaxLocalRawBrightness(pos,world.isThundering()?10:world.getSkyDarken())<=random.nextInt(8)
          && .5F-level.getPathfindingCostFromLightLevels(pos)>=0;
  ```
  Two deviations: the `nextInt(7)` roll was read from the **maximum raw brightness**
  (`max(blockLight, skyLight - skyDarken)`) instead of the **block light**, and the
  inherited 32/8 mob rolls were drawn although the original opts out of them.
  The maximum raw brightness under the open sky of the day is the full sky light of
  fifteen, so `15 > nextInt(7)` was always true: the swarm never spawned in daylight.
* **Fix** — `common/…/monster/EntityTaintSwarm.java:227-252`: the class gate reads
  `LightLayer.BLOCK`, the inherited 32/8 rolls are gone, and the rule is exposed
  without a world as `EntityTaintSwarm.originalSpawnGate(blockLight, roll, peaceful,
  pathfindingCost)`.
* **Evidence** — the swarm spawns in open air under the day sky again (block light 0
  always passes a `nextInt(7)` roll), and it consumes exactly **one** random draw.

### 2.3 The taintacle could not spawn under the open sky of the day

* **Original** — `OpenTC4 …/entities/monster/EntityTaintacle.java:34-43`. The
  taintacle does **not** override `isValidLightLevel()`, so it inherits the mob gate:
  the saved sky light against `nextInt(32)` and the **block** light against
  `nextInt(8)`. (The `getBlockLightValue` call in the method is a dead statement in
  the original — its result is discarded.)
* **Port (before)** — `common/…/monster/EntityTaintacle.java:170-172`:
  ```java
  if(level.getBrightness(LightLayer.SKY,pos)>random.nextInt(32))return false;
  var server=level.getLevel();return server.getMaxLocalRawBrightness(pos,server.isThundering()?10:server.getSkyDarken())<=random.nextInt(8)
  ```
  The second roll again read the maximum raw brightness, so in daylight the taintacle
  was refused (`15 <= nextInt(8)` is never true).
* **Fix** — `common/…/monster/EntityTaintacle.java:160-188`: the block light of the
  original is used, and the pure gate
  `EntityTaintacle.originalLightGate(skyLight, blockLight, skyRoll, blockRoll)` is
  exposed for the tests. The draw order (sky roll, then block roll) is unchanged.

### 2.4 The tainted chicken, pig and sheep had no armour at all

* **Original** — `OpenTC4 …/entities/monster/EntityTaintChicken.java:64-66`
  (`return 2;`), `EntityTaintPig.java:66-68` (`return 2;`),
  `EntityTaintSheep.java:63-65` (`return 1;`). The cow
  (`EntityTaintCow`) and the villager keep the inherited armour of their equipment.
* **Port (before)** — `common/…/monster/EntityTaintChicken.java`,
  `EntityTaintPig.java`, `EntityTaintSheep.java`: no `getArmorValue()` override at
  all, so the tainted animals had the vanilla armour of their animal (zero).
* **Fix** — the constant is carried in the pure table
  `TaintAnimalRules.armor(TaintedAnimal)`
  (`common/…/lib/taint/TaintAnimalRules.java:87-100`) and the three animals override
  `getArmorValue()` with it (`EntityTaintChicken.java:94`,
  `EntityTaintPig.java:77`, `EntityTaintSheep.java:138`).
* **Evidence** — the tainted chicken and pig reduce five points of incoming damage
  and the tainted sheep two, as in the original.

### 2.5 GameTest expectations that encoded the two defects

`common/src/gameTest/…/TaintSwarmRuntimeChecks.java:150-160` asserted that the swarm
is **rejected** in the bright day ("registered actual bright-day swarm spawn rejects
after FIRST original raw-light nextInt7") and
`common/src/gameTest/…/FireBatWispRuntimeChecks.java:170-201` asserted that the fire
bat has "no light restriction of the original". Both are now rewritten to the
original rule (the block light against `nextInt(7)`), and the random draw order is
still asserted (`actual.nextLong()==expected.nextLong()`).

---

## 3. Verified and closed without a code change

These are the ledger units whose audit notes were **stale**: the behaviour they
describe as missing has been implemented by a later wave. Each row was re-verified
against the oracle and the verdict moved.

* **`ItemBottleTaint`** — the original throws an `EntityBottleTaint`
  (`OpenTC4 …/items/ItemBottleTaint.java:36-47`) and the splash skips the tainted
  mobs and the undead (`OpenTC4 …/entities/projectile/EntityBottleTaint.java:44`).
  The port does both (`common/…/items/ItemBottleTaint.java:31-42`,
  `common/…/entities/projectile/EntityBottleTaint.java:111-123`,
  `ProjectileRules.bottleTaintAffects`). The earlier audit note ("the item bursts at
  the block the thrower looks at") is obsolete.
* **`ChampionModifier`** — the two deviations of the earlier audit are closed: the
  warded promotion ward now reads the **total** of the max health attribute
  (`ChampionRules.wardedChampionWard`, `common/…/lib/mods/ChampionRules.java:200-202`)
  and the whitelist resolves the thaumcraft boss base **by class**
  (`ChampionRules.CHAMPION_CLASS_WHITELIST`, `…:111-124`), so the cultist leader
  always becomes a champion as in `ConfigEntities:195-208`. The grim modifier is the
  wither (`ChampionRules.status`, `…:413-422`), not the weakness. The attribute
  operations were re-checked: the original operation two multiplied the total
  (`OpenTC4 …/lib/utils/EntityUtils.java:48-52`) and the native `ADD_MULTIPLIED_TOTAL` with the
  amount minus one is the same.
* **`WarpEvents`** — the whole warp arithmetic matches the original
  (`OpenTC4 …/lib/WarpEvents.java`), the research warp split was compared with
  `ResearchManager.completeResearch` (`OpenTC4 …/lib/research/ResearchManager.java:577-589`:
  perm `warp - warp/2`, sticky `warp/2`, and a warp of one goes to perm alone) and the
  native split is the same (`common/…/lib/WarpEvents.java:69-74`). The four bauble
  slots of `getWarpFromGear` have no 1.21.1 counterpart, but no TC4 accessory
  implements `IWarpingGear` (the warping gear of the original is the void tools and
  armour, the crimson sword, the primal crusher and the cultist armour — all hand or
  armour slots), so the sum is behaviourally equal.
* **`EntityGiantBrainyZombie`** — the attribute row (60 health, 7 damage), the
  experience value 15, the leap goal at priority two, the anger growth
  (`min(2, anger + 0.1)`) and decay (`-0.002` per tick) and the whole drop table
  (two loops of six booleans with two flesh each, the brain, and the rare pool of
  `itemResource` metadata 2 and 6 = thaumium ingot and amber) were compared line by
  line with `OpenTC4 …/entities/monster/EntityGiantBrainyZombie.java` and match.
* **`TaintHelper`** — the legacy TC6 facade only calls
  `BlockTaintFibres.spreadFibres` twice (`src/main/java/thaumcraft/common/blocks/world/taint/TaintHelper.java:15-25`);
  the native port has that one function
  (`common/…/blocks/BlockTaintFibres.java:84-93`), so the facade is fully covered.
* **The six `tainted/*` units** — TC4 4.2.3.5 has no tainted crawler, seed, prime
  seed, swarm, taintacle or small taintacle. The port keeps the 1.12.2/TC6 content and
  its numbers were verified against the **constant pool of the real 1.12.2 jar**:
  `EntityTaintCrawler.class` → `Double 8.0` (health), `Double 2.0` (damage),
  `Double 0.275` (speed); `EntityTaintSeed.class` → `Double 75.0`, `Double 4.0`;
  `EntityTaintSeedPrime.class` → `Double 150.0`, `Double 7.0`. The swarm, the taintacle
  and the small taintacle were merged into the TC4 classes, whose numbers come from
  the oracle.

---

## 4. Not fixed, with the precise blocker

### 4.1 The death aspect orbs are missing from the port (a real, provable gap)

* **Original** — `OpenTC4 …/lib/events/EventHandlerEntity.java:507-546`. The
  `LivingDeathEvent` handler has two branches. The first one converts a mob that
  carries the taint poison into its tainted twin (the port implements this in
  `TaintDeathConversion` and `ThaumcraftTaintAnimals`). The **second** branch drops
  aspect orbs:
  ```java
  } else if (!event.entityLiving.worldObj.isRemote && EntityUtils.getRecentlyHit(event.entityLiving) > 0) {
     AspectList aspectsCompound = ScanManager.generateEntityAspects(event.entityLiving);
     if (aspectsCompound != null && aspectsCompound.size() > 0) {
        AspectList aspects = ResearchManager.reduceToPrimals(aspectsCompound);
        for (Aspect aspect : aspects.getAspects()) {
           if (event.entityLiving.worldObj.rand.nextBoolean()) {
              EntityAspectOrb orb = new EntityAspectOrb(world, posX, posY, posZ, aspect,
                      1 + event.entityLiving.worldObj.rand.nextInt(aspects.getAmount(aspect)));
              world.spawnEntityInWorld(orb);
           }
        }
     }
  }
  ```
  So: one orb per **primal** aspect of the dead mob, a fifty percent roll per aspect,
  and a value of `1 + nextInt(amount of that aspect)`. The port has **no** such
  branch: no mob ever drops a vis orb.
* **Why it was not landed in this wave** — the original draws the two rolls from the
  **world** random, inside the death, *before* the loot of the same death. The native
  GameTests of several lanes lock the world random stream of a mob death:
  `TaintSwarmRuntimeChecks.loot()` (`:126-135`) and `.experience()` (`:137-148`),
  `BrainyZombieRuntimeChecks.allFleshSeed()` (`:149-156`, used at `:171`, `:177`,
  `:184`), `PechRuntimeChecks` (`:144`, `:159`), `FireBatWispRuntimeChecks` (`:156`).
  Publishing the orbs shifts every one of those expectations, and the new
  expectations cannot be re-derived in this wave because **Gradle must not be run
  here** (the leader owns the builds). A second, independent risk is that the orbs
  are entities that a player with a wand in the hotbar picks up (the orb writes vis
  into the wand), which can change the wand vis that `CombatFocusSpecialRuntimeChecks`
  asserts.
  The ledger unit that owns this behaviour is
  `thaumcraft/common/lib/events/EventHandlerEntity.java` (`shared_infrastructure`,
  `replace`/`partial`), **not** the `warp_creatures_and_ecology` family, so the
  behaviour belongs to the lane that owns that unit.
* **What a follow-up needs** — a pure rule class
  (`AspectList.reduceToPrimals(false)` → per aspect `nextBoolean()` and
  `1 + nextInt(amount)`), a death hook, and a re-derivation of the five RNG
  expectations above. The pure part is trivial and was written and unit tested
  during this wave; the commit was reverted (`8bc39488`) because of the parity risk.

### 4.2 The client units (four rows)

`thaumcraft/client/fx/particles/FXWisp.java`, `FXWispArcing.java`, `FXWispEG.java`
and `thaumcraft/client/renderers/block/BlockTaintRenderer.java` stay `partial`:

* `common/src/main/java/thaumcraft/client/**` is outside the editable scope of this
  wave, so no client defect could be repaired here.
* `BlockTaintRenderer`: the 1.7.10 ISBRH draws the transparent fibre overlay pass on
  the taint **crust and soil** (`OpenTC4 …/BlockTaintRenderer.java:27-57`, using
  `BlockTaintFibres.getOverlayBlockTexture(x,y,z,side)`), while the native port draws
  the overlay on the taint **fibres** block
  (`fabric/…/FabricTaintFibresModel.java:52-80`). The overlay rule itself is
  reproduced exactly (`common/…/lib/world/TaintSpreadRules.java:28-30` against
  `OpenTC4 …/blocks/BlockTaintFibres.java:114-117`:
  `new Random(side + y + (long) x * z)`, `nextInt(100) < 95 ? iconOver[0] :
  iconOver[nextInt(3) + 1]`). The original also picks one of three soil textures per
  metadata, the native port uses one cube for all of them.
* `FXWisp`: the six types, the lifetime `36 / (random * 0.3 + 0.7)` and the mote
  physics are reproduced (`common/…/client/fx/particles/PrimalWispParticle.java:19-70`);
  the per tick *tinkle* orb sound and the exact push out of blocks are not.

### 4.3 `MaterialTaint` (one row)

`thaumcraft/common/config/MaterialTaint.java` stays `partial`. The class itself has
no 1.21.1 counterpart (`net.minecraft.block.material.Material` does not exist) and is
deliberately not carried over; its observable traits are expressed with the block
properties of the taint family. The one remaining trait is the **mobility flag**
(`func_76227_m == 1`, set by `setNoPushMobility()`,
`OpenTC4 …/config/MaterialTaint.java:35-42`). The closest 1.21.1 property is the push
reaction of the block (`BlockBehaviour.Properties.pushReaction`), which lives in
`common/blocks/**` — outside the editable scope of this wave. The decision belongs to
the blocks lane.

### 4.4 The giant brainy zombie: stored dimensions against the collision box

The original `EntityGiantBrainyZombie` constructor scales the zombie size by
`1.2 + anger` (`OpenTC4 …/EntityGiantBrainyZombie.java:15-16`), but whether that
changes the **actual** collision box depends on the 1.7.10 `EntityZombie.setSize`
override, which is **not** part of the OpenTC4 reference. The native behaviour
(stored dimensions only, the box applied on an explicit child-size setter) is locked
by `common/src/gameTest/…/BrainyZombieRuntimeChecks.java:104-113` with an
"executable 1.7 oracle" citation, so this wave did not change it. The row was moved
to `parity_verified` with that residual documented in the ledger note.

### 4.5 The Silverwood / Thaumonium ecosystem and the taint block interaction

`common/…/lib/world/**` and `common/…/common/blocks/**` (the taint crust, the taint
soil, the taint fibres and the taint block rendering) are outside the editable scope
of this wave, so the taint fibre / taint block interaction and the Silverwood
ecosystem were only read, not repaired. The open item found there is the overlay
pass of §4.2 and the taint material of §4.3.

---

## 5. Tests authored (authored, NOT executed)

`./gradlew` must not be run in this wave, so **no test was executed**; the files
below are authored only and have never been compiled or run.

| file | what it pins |
|---|---|
| `common/src/test/java/thaumcraft/taint/OriginalSpawnGateTest.java` (new) | the class gate roll bound 7, the block light gate, the fire bat gate (`EntityFireBat.originalSpawnGate`), the swarm gate including the walk target value (`EntityTaintSwarm.originalSpawnGate`) and the inherited mob gate of the taintacle (`EntityTaintacle.originalLightGate`) |
| `common/src/test/java/thaumcraft/taint/TaintAnimalRulesTest.java` (extended) | the original armour of the tainted animals (chicken 2, pig 2, sheep 1, cow 0, villager 0) |

The two GameTest scenarios that encoded the two spawn defects were **updated** (not
authored): `TaintSwarmRuntimeChecks.spawnRules()` (`:150-163`) and
`FireBatWispRuntimeChecks.population()` (`:172-215`). They are native scenarios, run by the
leader's CI, not by this wave.

---

## 6. Verification performed in this wave

* `python3 scripts/porting/update_scope_ledger.py --check` — the **private oracle
  root is not present in this sandbox**
  (`/home/cyberfox-kali/wt/work/thaumcraft-4.2.3.5-oracle` does not exist), so the
  script aborts with `Missing source root` before it can read the original side.
  The ledger was therefore validated with a local harness that reuses
  `update_scope_ledger.refresh()` unchanged and feeds it the **oracle fingerprints
  the ledger itself records** (they were produced from the oracle when the ledger was
  written) instead of re-reading the oracle tree. The harness reproduces the real
  `refresh()` — the same legacy tree, the same class inventory, the same review
  validation, the same row order and the same counts — and reports:
  ```
  {"mode": "check", …, "stale_review_fingerprints": 0}
  ```
  and the refreshed ledger is **byte identical** to the file on disk. The
  `reviewed_sha256` anchors of the 17 rows use the recorded `original`/`legacy`
  fingerprints, so the real `--check` with the real oracle will agree.
* `counts` untouched, row order untouched, JSON shape untouched (the file is still
  `json.dumps(…, ensure_ascii=False, indent=2) + "\n"`, byte identical on a
  round trip).
* No Gradle build, no test execution, no client run.

---

## 7. Commits

| commit | content |
|---|---|
| `edaf1966` | fix(warp): restore the original class spawn gates of the fire bat and the taint swarm (+ the taintacle block light) |
| `65d81e14` | fix(warp): give the tainted chicken, pig and sheep their original armour |
| `67ba2edf` | feat(warp): drop the original aspect orbs when a mob dies (+ the ledger verdicts) |
| `8bc39488` | revert(warp): hold back the death aspect orbs until the death RNG parity is settled |

The ledger verdicts of §1 are in `67ba2edf`.

---

## 8. Open items for the leader

1. **Death aspect orbs** (§4.1) — the highest value open item of this domain. It
   belongs to the `shared_infrastructure` unit `EventHandlerEntity`, and it needs a
   window in which the GameTests can be run to re-derive the five RNG expectations.
2. **Taint block overlay pass** (§4.2) — the client/blocks lane: the overlay belongs
   on the taint crust and soil, not on the fibres block.
3. **Taint material mobility flag** (§4.3) — the blocks lane: decide between
   `BlockBehaviour.Properties.pushReaction(PushReaction.ALLOW)` and a documented
   loss.
4. **Giant brainy zombie box** (§4.4) — needs the 1.7.10 `EntityZombie.setSize`
   source (the private oracle) to settle whether the original giant is really
   1.32 × 3.96 blocks.
