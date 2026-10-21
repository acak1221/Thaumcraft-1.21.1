# agent/floatfix — the single precision of the ported numbers (2026-10-23)

Wave 2, agent `floatfix`. Worktree `/home/cyberfox-kali/wt/floatfix`, branch
`agent/floatfix`, base `de8049a1` (`work`).

The oracle is the byte exact decompilation of `Thaumcraft-1.7.10-4.2.3.5.jar`
at `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`; `/home/cyberfox-kali/OpenTC4`
was only read as a second opinion and was overruled twice (see *Divergences*).

---

## 1. The verdict for the sanity soap

**The production code was right, the test expectation was wrong.**

Oracle `thaumcraft/common/items/ItemSanitySoap.java:88-94` (the CFR line numbers of
the oracle file, the decompiled 4.2.3.5 jar):

```java
float chance = 0.33f;                                     // :88
if (player.func_82165_m(Config.potionWarpWardID)) {
    chance += 0.25f;                                      // :90
}
if (world.func_147439_a(...) == ConfigBlocks.blockFluidPure) {
    chance += 0.25f;                                      // :93
}
```

Java 1.7.10 evaluates this in **single precision**, because both operands of every
operation are `float`:

| expression | bits | value |
|---|---|---|
| `0.33F` | `0x3ea8f5c3` | 0.33000001311302185 |
| `0.33F + 0.25F` | `0x3f147ae2` | 0.5800000429153442 |
| `0.33F + 0.25F + 0.25F` | `0x3f547ae2` | 0.8300000429153442 |
| the literal `0.58F` | `0x3f147ae1` | 0.5799999833106995 |
| the literal `0.83F` | `0x3f547ae1` | 0.8299999833106995 |

The decimal literals are **one ulp below** the sum, so the old expectation
`assertEquals(0.58F, …)` could never hold.

The port reproduces the original exactly —
`common/src/main/java/thaumcraft/common/lib/warp/SanitySoapRules.java:12`:

```java
public static float chance(boolean ward,boolean pureFluid){float chance=.33F;if(ward)chance+=.25F;if(pureFluid)chance+=.25F;return chance;}
```

and the sibling test `common/src/test/java/thaumcraft/common/lib/warp/SanitySoapRulesTest.java:11-12`
already pinned the **raw bit patterns** `0x3f147ae2` and `0x3f547ae2` — the
independent proof that the production is the original's arithmetic.

**Fix** (`common/src/test/java/thaumcraft/common/blocks/BaseContentItemParityTest.java:234-250`):
the four expectations are now the original's own expressions
`0.33F`, `0.33F + 0.25F`, `0.33F + 0.25F`, `0.33F + 0.25F + 0.25F`, and the messages
cite the real oracle lines (`:53` for the two hundred ticks, `:88`, `:88-90`, `:88-93`)
instead of the wrong `:36`/`:76`.

---

## 2. The sweep

### 2.1 Method

1. The new test files of the wave were listed with
   `git log --since="2026-10-20" --diff-filter=A --name-only -- common/src/test/java`
   → **274 files**.
2. Every `assert*` call of those files was parsed (balanced parentheses, string aware)
   and every call with a `float`/`double` literal was extracted: **497 assertions**
   over **256 distinct decimal literals**, spread over **134 distinct production
   members** (constants and methods).
3. Every production member was matched to its declaration in
   `common/src/main/java` and to the oracle file the port's own javadoc cites.
4. Every value was recomputed in IEEE-754 single precision (numpy `float32`, which is
   round-to-nearest-even like Java) and compared bit by bit with the literal of the
   test.
5. Every rule whose oracle expression is a **sum or a product** was rewritten in the
   test as the original's expression, and every tolerance was removed.

The decisive filter: **122** of the 497 assertions carried a non-zero tolerance.
Those are the ones that can hide a wrong value; they are now **0**. All the other
assertions are exact (`JUnit5 assertEquals(float,float)` compares the raw bits,
`assertEquals(double,double)` the raw bits of the double) and were already
bit-exact, but the ones whose original is an arithmetic expression were rewritten so
that the test documents the original's arithmetic instead of a decimal that happens
to be one ulp away.

### 2.2 The table of the changed assertions

| test file | assertion before | the original's expression | verdict |
|---|---|---|---|
| `common/blocks/BaseContentItemParityTest.java:241-244` | `0.58F`, `0.83F` | `0.33F + 0.25F`, `0.33F + 0.25F + 0.25F` | **wrong** (1 ulp), fixed |
| `crash/SpecialMiningSafetyTest.java:77-78` | `0.2F`, `0.2F + 0.075F` (tol. 1e-6) | `0.2f + (float)fortune * 0.075f` | right value, rewritten, tolerance dropped |
| `items/SpecialMiningRuleTest.java:13-16` | `0.275F`, `0.35F`, `0.425F` (tol. 1e-6) | `0.2f + (float)fortune * 0.075f` | **`0.35F` is 1 ulp off** (0.3500000238418579), rewritten, tolerance dropped |
| `equipment/LegacyItemsRulesTest.java:13-15` | `0.07F`, `0.14666667F`, `0.41666666F` (tol. 1e-6) | `f = (float)j / 10.0f; f = (f * f + f * 2.0f) / 3.0f` | **`0.14666667F` is 1 ulp off** (0.14666666090488434), rewritten |
| `equipment/LegacyItemsRulesTest.java:41-42` | `2.5F`, `1.25F` | `f * 2.5f` | right, rewritten |
| `equipment/EquipmentWiringRulesTest.java:24-27` | `3.75F`, `1.875F`, `3.0F` (tol. 1e-6) | `f1 * 1.5f` | right, rewritten, tolerance dropped |
| `items/LevitatorRulesTest.java:28-33` | `0.1 * 1.0F`, `0.4F`, `0.35F + 0.1F` | `e.x += (double)0.1f`, `e.x < (double)0.35f` | **`0.4F` and `0.35F + 0.1F` are off** (0.4000000014901161), rewritten |
| `items/LevitatorRulesTest.java:37` | `-0.5 * (double) 0.9F` (tol. 1e-12) | `e.x *= (double)0.9f` | right, tolerance dropped |
| `items/MagicWardTest.java:12-14` | `0.2` for `ratio(7)` | `(double)armor / 35.0` | right, rewritten as `7 / 35.0` |
| `items/MagicWardTest.java:19-20` | `20 * 19 / 35.0` (tol. 1e-5) | Forge's own `damage * 25`, share, `/ 25` | **the conceptual formula is not the Forge step** (10.857142448425293), rewritten as the Forge step |
| `items/FortressRulesTest.java:13-19` | `1.0`, `1.125`, `1.25`, `1.3`, `1.4` (tol. 1e-12) | `set = 0.875; set += 0.125; set += 0.05;` | right, rewritten as the sums, tolerance dropped |
| `items/FortressRulesTest.java:35-41` | `0.15`, `0.35`, `0.3` | `(double)armor / 20.0` | right, rewritten as `3 / 20.0` … |
| `items/ElementalSwordRulesTest.java:13-17` | `0.08F`, `-0.6D / 1.2 + 0.08F`, `0.2F`, `0.5F + 0.0F` | `x /= (double)1.2f; x += (double)0.08f; x = 0.2f` | **`-0.6D / 1.2 + 0.08F` and `0.5F + 0.0F` are off**, rewritten |
| `items/ElementalSwordRulesTest.java:21` | `5.0F` (tol. 1e-6) | `fallDistance /= 1.2f` (float division) | right, rewritten as `6.0F / 1.2F` |
| `items/ElementalSwordRulesTest.java:28-33` | `1.5 / 2.5 / 1.6` (tol. 1e-9) | `r.x / 2.5 / distance` with `distance = d + 0.1` | right, rewritten, tolerance dropped |
| `taint/TaintAnimalRulesTest.java:108-119` | `0.7F`, `0.2F`, `0.9F`, `0.45F`, `2.0F`, `-0.9F` | `EntityTaintChicken:103,113,115` (double arithmetic) | right, rewritten as the original's double expressions, tolerance dropped |
| `taint/TaintAnimalRulesTest.java:125-129` | `0.5F`, `0.62831855F`, `1.0F` | `EntityTaintSheep:146,151,154` | right, rewritten, tolerance dropped |
| `taint/TaintAnimalRulesTest.java:143` | `6.0F` | `(int)((float)villageSize * 0.6f)` | right, rewritten as `10 * 0.6F` |
| `taint/TaintAnimalRulesTest.java:166-169` | `4.0D`, `256.0D` (tol. 1e-9) | `area * 4.0d`, `area * 256.0d` | right, rewritten, tolerance dropped |
| `common/lib/taint/TaintCombatRulesTest.java:9-12` | `1.3D`, `2.6D`, `-.5D`, `-1.5D`, `19.1F`, `4.5F` | `1.3d * (amplifier + 1)`, `-.5f * (amplifier + 1)`, `EntityTaintacle:159-162` | right, rewritten |
| `common/lib/taint/TaintacleRulesTest.java:14,17-18` | `.90000004F`, `.1F` (tol. 1e-6) | `flailIntensity -= 0.2f`, the rise over ten ticks | **`.1F` is off** (0.10000000894069672), rewritten |
| `cult/CultistRulesTest.java:55-56` | `0.25F`, `0.5F` | `nextFloat() < 0.25f * f`, `< 0.5f * f` | right, rewritten |
| `cult/CultistRulesTest.java:73-74` | `0.5F`, `1.0F` | `MathHelper.sqrt_float((float)f) * 0.5f` | right, rewritten |
| `cult/CultistRulesTest.java:170` | `62.5F` | `int bh = (int)maxHealth / 2` | **wrong** (the original truncates), rewritten and the production fixed |
| `cult/CultistRulesTest.java:261` | `2.25D` (tol. 1e-9) | the square of the door distance | right, rewritten as `1.5 * 1.5`, tolerance dropped |
| `cult/CultistPortalRulesTest.java:18-44` | `500.0D`, `1.5F`, `3.0F`, `1.0D`, `9.0D`, `1.0F`, `0.0F` (tolerances) | the literals of the original | **`9.0D` is wrong** (the original asks for the squared distance and compares it with three), rewritten and the production fixed |
| `eldritch/EldritchMobRulesTest.java:98` | `7.5F` | `attack * 0.75f` | right, rewritten |
| `eldritch/EldritchMobRulesTest.java:113-118` | `1.75F`, `3.5F`, `3.33F` (tolerances) | `EntityEldritchGolem:92,134` | right, tolerance dropped |
| `eldritch/EldritchMobRulesTest.java:157` | `.5F` (tol. 1e-6) | `((float)ticks + partial) / (float)duration` | right, rewritten as `40 / (float)80` |
| `eldritch/EldritchMobRulesTest.java:169` | `.2F` (tol. 1e-6) | `0.1f * (float)Math.sqrt(size)` | right, rewritten |
| `eldritch/EldritchMobRulesTest.java:176-177` | `.75F`, `.5F` (tol. 1e-6) | `0.25f * (float)Math.sqrt(size) + 0.25f` | right, rewritten |
| `eldritch/EldritchBlockRulesTest.java:394-423` | `3.0F`, `10.2F`, `15.0F`, `6.0F`, `12000.0F` | the setter value times three over five | right, rewritten as `17.0F * 3 / 5` … |
| `eldritch/OuterLandsBossRoomRulesTest.java:41-120` | eleven literals (tolerances) | `TileEldritchLock:105,181-188,273-276,304,346` | right, tolerance dropped |
| `eldritch/EldritchTileRulesTest.java:32,38,77` | `3.0D`, `2.0F`, `16.0D` (tolerances) | the literals of the original | right, tolerance dropped |
| `entities/EntityParityRulesTest.java:25-37` | six literals (tolerances) | the literals of the original | right, tolerance dropped |
| `entities/ProjectileRulesTest.java:69-99` | `0.2D`, `0.4D`, `0.1D`, `0.025D`, `0.3D`, `0.25D`, `-0.25D`, `0.2D`, `3.15D`, `1.68D` (tolerances) | `EntityGolemOrb:95-105`, `EntityPrimalArrow:73,355-368` | **`3.15D` and `1.68D` are off** (2.1*1.5 = 3.1500000000000004, 2.1*0.8 = 1.6800000000000002), rewritten |
| `entities/ProjectileRulesTest.java:125-128` | `3F`, `3.75F`, `2F` | `par3 * 1.5f` | right, rewritten |
| `entities/ProjectileRulesTest.java:165-177` | `0.05D`, `1D`, `11D`, `0.04D` (tolerances) | `1D / slowdown`, `targetY + height / 2d` | right, rewritten, tolerance dropped |
| `equipment/TravelGearRulesTest.java:24-30` | `.055F`, `.05F`, `.03F`, `-.125F`, `.7F` | `ItemBootsTraveller:78-89`, `Hover:97-103` | right, rewritten |
| `golems/GolemDartRulesTest.java:43-53` | `7.0F`, `5.25F`, `3.5F`, `0.0F`, `-1.75F` | `7.0f - (float)upgrade * 1.75f` | right, rewritten |
| `items/SpecialMiningRuleTest.java:13-16` | see above | | |
| `client/fx/ParticleScaleParityTest.java:41-53` | `0.05F`, `0.2F`, `.4F`, `1.0F`, `0.25F` (tolerances) | `FXWisp:44` (`0.5f * scale`), `FXBurst:32,50`, `FXWispEG:25,53` | right, rewritten, tolerance dropped |
| `client/renderers/tile/LevitatorGlowParityTest.java:66-70` | `1.01F`, `-0.01F` (tolerances) | the port's own adaptation of the panel | right, rewritten, tolerance dropped |
| `common/blocks/CosmeticAndPlantParityTest.java:32,55,58` | `6.0F` | the setter value times three over five | right, rewritten as `10.0F * 3 / 5` |
| `common/blocks/BlockFamilyParityTest.java:38` | `3600000.0F` | `6000000f * 3 / 5` | right, rewritten |
| `taint/TaintMaterialRulesTest.java:58` | `3600000.0F` | `6000000f * 3 / 5` | right, rewritten |
| `tiles/LegacyTileRulesTest.java:231-236` | `20.0F`, `40.0F`, `360.0F`, `0.0F` | `TileTubeValve:51-54` | right, rewritten |
| `tiles/TileParityRulesTest.java:55-60` | `0.3F`, `0.8F`, `0.9999F`, `1.001F` (tolerances) | `TileCrucible:315-325` | right, rewritten as the original's own function, tolerance dropped |
| `common/lib/utils/EntityUtilsParityTest.java:42-45` | `0.8f` (tolerances) | `Math.max(0.8F, width * 0.5F)` | right, rewritten, tolerance dropped |
| `utils/LegacyUtilsTest.java:132-133` | `100.0`, `116.0` | `dx*dx + dz*dz`, `dx*dx + dy*dy + dz*dz` | right, rewritten |
| `utils/SharedInfrastructureParityTest.java:172-173` | `9.0D`, `25.0D` (tolerances) | the same | right, rewritten, tolerance dropped |
| `assets/TravelingTrunkClientAssetsTest.java:56-66` | `0.95238F`, `0.93333F`, `1.07411F`, `1.05263F`, `0F`, `-(1F - 0.125F) * (float) Math.PI / 2F` (tolerances) | `RenderTravelingTrunk:38-48` | **the pi constant is wrong** (`3.141593f`, not `(float) Math.PI`), the scale decimals are approximations, rewritten |
| `assets/WandModelAssetsTest.java:84-86` | `.25F`, `.5F`, `.75F` | `index / CAP_SCALE` | right, rewritten |
| `mods/ChampionModifierTest.java:105,108` | `7.6f`, `10.0f` (tolerances) | `ChampionModArmored:20-21` (`amount * 19.0f / 25.0f`) | right, rewritten, tolerance dropped |
| `wands/WandThaumonomiconTest.java:30-39` | `1.0F`, `1.1F`, `0.9F` (tolerances) | `ItemWandCasting:247-261` | right, rewritten, tolerance dropped |
| `integration/CrossSubsystemRulesTest.java:102-103` | `9.0D`, `49.0D` (tolerances) | the squares of the ranges | right, rewritten, tolerance dropped |
| `ward/WardRulesTest.java:23-30` | `132.0`, `10.0`, `62.5F`, `62.5` | `EntityEldritchWarden:164`, `EntityUtils.makeChampion:271-274` | **`62.5` is wrong**, rewritten and the production fixed |

### 2.3 The count

* 497 float/double assertions checked in the 274 new test files.
* 122 of them carried a tolerance → **0** now.
* **14** assertions asserted a value that is *not* the original's value
  (the sanity soap × 2, `SpecialMining.chance(2)`, `boneBowPower(2)`,
  `LevitatorRules.lifted(0.3)`, `ElementalSwordRules.rise(-0.6)` and `rise(0.42)`,
  `TaintacleRules.ariseHeight(29,3)`, `primalArrowDamage` heavy/order/wither,
  `championAbsorption`, `PORTAL_COLLISION_DISTANCE_SQ`) — the first ten were
  masked by a tolerance, the last two were masked by nothing at all.
* **~90** further assertions were the original's own arithmetic written as a
  decimal literal; they are now the original's expression.
* The rest are literal against literal and were already bit exact.

---

## 3. The production changes

| file | the original | the port before | the port now |
|---|---|---|---|
| `common/lib/taint/TaintAnimalRules.java:141-165` | `EntityTaintChicken:103` `this.destPos = (float)((double)this.destPos + (double)(this.field_70122_E ? -1 : 4) * 0.3);` — the step is computed in **double** | `speed+(onGround ? -1.0F : 4.0F)*0.3F` (float) | `(float)((double)speed+(double)(onGround?-1:4)*0.3)` |
| `common/lib/taint/TaintAnimalRules.java:146-155` | `EntityTaintChicken:113` `this.field_755_h = (float)((double)this.field_755_h * 0.9);` — the damping is computed in **double** | `next*0.9F` (float) | `(float)((double)next*0.9)` |
| `common/lib/taint/TaintAnimalRules.java:157-165` | `EntityTaintChicken:115` `this.field_70181_x *= 0.9;` — the double motion times the double 0.9 | `motionY*0.9F` (float) | `(float)((double)motionY*0.9)` |
| `client/renderers/entity/RenderTravelingTrunk.java:39-52` | `RenderTravelingTrunk:48` `-(f1 * 3.141593f / 2.0f)` — the float literal **3.141593f** | `* (float) Math.PI` (0x40490fdb) | `* 3.141593F` (0x40490fdc) |
| `common/lib/cult/CultistRules.java:288-296` | `EntityUtils.makeChampion:271-274` `int bh = (int)maxHealth / 2;` — an **integer** division | `maxHealth / 2.0F` (62.5 for 125) | `(float)((int)maxHealth/2)` (62 for 125) |
| `common/lib/cult/CultistRules.java:323-330` | `EntityCultistPortal:239` `this.func_70068_e((Entity)p) < 3.0` — `func_70068_e` is `getDistanceSqToEntity`, so the **squared** distance is compared with three | `PORTAL_COLLISION_DISTANCE_SQ = 9.0` (the square of three blocks) | `PORTAL_COLLISION_DISTANCE_SQ = 3.0` |
| `common/entities/monster/EntityCultistPortal.java:202-208` | the same | the comment claimed the original asked for the plain distance | the comment and the constant corrected |
| `common/lib/entities/ProjectileRules.java:109-116` | `EntityGolemOrb:103-105` `MathHelper.clamp_float((float)this.field_70159_w, -0.25f, 0.25f)` — the motion is narrowed to a **float** before the clamp | the double was clamped | the float is clamped |
| `common/lib/tiles/TileParityRules.java:61-70` | `TileCrucible:316` `0.3f + 0.5f * ((float)amount / (float)capacity)` — the division comes **first** | `0.3F + 0.5F * water / CRUCIBLE_CAPACITY` | `0.3F + 0.5F * (water / (float) CRUCIBLE_CAPACITY)` |
| `common/tiles/TileCrucible.java:180-188` | the same | the same wrong order | the same corrected order |

The last two are numerically identical for the crucible's fixed capacity of a
thousand (proved over every water amount from 0 to 1000), but they now carry the
original's order of operations.

### 3.1 The tainted chicken: how big is the difference?

The float step of the port differed from the original's double step for
**7 549 747** of the 16 777 216 float inputs of `flapSpeed` over `[0,1]` (both
branches) — for example the original gives `1.1920929132713809E-08` for the ground
case of a speed of `0.30000001192092896` and the port gave `0.0`. The amplitude
damping differs for every float that is not a multiple of the rounding step. The
fall damping of the range `[-1,0]` is identical, but the arithmetic is now the
original's.

---

## 4. Divergences between the oracle and OpenTC4

Two places where the second opinion and the oracle disagree; **the oracle wins**,
as `BRIEF2.md` demands.

1. `EntityGolemOrb:95` — the oracle divides the delta by `func_70068_e`, which is
   `getDistanceSqToEntity` (proved by `AITarget:59`, which compares it with
   `targetDistance * targetDistance`, and by `EntityWisp:202`, which compares it
   with `attackrange * attackrange`). OpenTC4 writes `getDistanceSqToEntity`
   explicitly. **The port's squared distance is right**; the wave-1 commit
   `d499b335` that introduced it was correct and I did not touch it.
2. `RenderTravelingTrunk:48` — the oracle uses the float literal `3.141593f`,
   which is one ulp above `(float) Math.PI`. The port had used `(float) Math.PI`.

---

## 5. A second failing test that was not in the report

The baseline of this worktree (commit `de8049a1`) had **two** failures, not one:

```
WandBlockTriggerTest > aRegisteredTriggerIsFoundForEveryStateOfItsBlock() FAILED
BaseContentItemParityTest > theSanitySoapKeepsTheOriginalDurationAndChances() FAILED
2137 tests completed, 2 failed
```

`WandTriggerRegistry` is a **global static** and neither `ItemWandCasting.hasBlockTrigger`
nor `WandTriggerRegistry.performTrigger` is mod scoped.
`WandTriggerRegistryApiTest:67` registers a trigger for `Blocks.CLAY` and
`WandBlockTriggerTest` used `Blocks.COBBLESTONE` (registered by
`WandTriggerRegistryApiTest:42` as well) and asserted that CLAY has no trigger
and that the first successful trigger is the one of event 4. Both assertions
depend on which other tests ran before. The test now uses `Blocks.SAND` (the
registration) and `Blocks.GRAVEL` (the negative case), two blocks that no other
test of the suite registers.

---

## 6. The result

```
:common:test  ->  2137 tests, 0 failures, 0 errors, 0 skipped   (318 test classes)
```

run with

```
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
GRADLE_USER_HOME=/home/cyberfox-kali/wt/floatfix/.gradle-float
./gradlew --console=plain --offline --no-daemon :common:test
```

The Gradle home of this worktree is a **copy** of the one of the `testfix4`
agent; the `daemon` directory of the copy was deleted so that this worktree
never touches the daemon registry of another agent.

Commits on `agent/floatfix`:

* `test(warp): the sanity soap chance is the original float sum, not a decimal literal`
* `fix(taint): the tainted chicken damps and steps in double like the original`
* `fix(cult): the cultist portal asks for the squared distance like the original`
* `test(sweep): the last approximate float assertions become the original's own expressions`

---

## 7. What could not be settled

* **Forge's `ApplyArmor`** (the magic ward of the void robes and the Thaumium
  Fortress) is not part of the 4.2.3.5 jar: it lives in the Forge runtime. The
  TC4 half of it is in the oracle (`ItemVoidRobeArmor:229-240`,
  `ItemFortressArmor:144-168`: the ratios `armor / 25.0`, `armor / 35.0`,
  `armor / 20.0` and the set bonus `0.875 + 0.125 (+ 0.05)`), and those the port
  has. The *step* in which the shares come off the damage
  (`damage * 25`, minus the share, divided by 25) is the port's documented
  reconstruction of the Forge handler and could not be checked against the jar.
  `MagicWardTest` now pins that reconstruction exactly instead of the
  conceptual `20 * 19 / 35`, which is a different number.
* **The 1.12 era rules** (the hungry node, the aspect orb, the taint seed, the
  taint creeper swell, the ethereal bloom, the taint combat modifiers) are not in
  the 4.2.3.5 oracle at all. Their expectations were tightened to the port's own
  documented expressions, but they are not oracle-verified.
* **The client model geometry** (`ClientModelGeometryTest`,
  `CrystalClusterGeometryTest`, `EldritchBlockModelTest`) asserts rotation and
  scale decimals such as `.0523599F`; these are the port's own model numbers,
  not numbers of the original, and they were left as they are.
* `MagicWard.wearByRatio` and the ordinary armour of the original are Forge
  code as well; the ward point counts the port computes
  (`(int)(damage * 25 * ratio / 25)`) follow the TC4 `damageArmor` hook and were
  not changed.
