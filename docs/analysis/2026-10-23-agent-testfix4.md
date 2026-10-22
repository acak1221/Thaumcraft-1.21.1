# Agent `testfix4` — the four red JUnit tests of wave 2 (2026-10-23)

## Scope

Four JUnit tests of `:common:test` were red since the parallel wave wrote them; nobody had
executed them. This agent settles every one of them against the **byte-exact oracle** of the
original jar,

    /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle

and leaves the suite green. `/home/cyberfox-kali/OpenTC4` was only read as a second opinion; the
oracle wins everywhere.

| # | test | verdict |
|---|------|---------|
| 1 | `SpecialMiningSafetyTest.theRollBoundaryIsTheOriginalOne` | **test** bug |
| 2 | `SpecialMiningSafetyTest.anUnknownReplacementKeepsTheOriginalDrop` | **production** bug |
| 3 | `VillageComponentParityTest.thePieceWeightDrawIsTheVillageSizeOrOneMore` | **test** bug |
| 4 | `VillageComponentParityTest.theTradeTablesKeepTheOriginalRows` | **test** bug |

One production defect, three wrong expectations. No test was weakened: every assertion that
survived asserts the original value, and the two assertions that were wrong were replaced by the
original value, not deleted.

The GameTest suite was run as well (the second half of this report): it was red with three
pre-existing failures, all of them in other domains, and those are settled too.

---

## 1. `theRollBoundaryIsTheOriginalOne` — the test probed the wrong threshold

**Original.** `thaumcraft/common/lib/utils/Utils.java:174-183`:

```java
public static ItemStack findSpecialMiningResult(ItemStack is, float chance, Random rand) {
    ItemStack dropped = is.func_77946_l();
    float r = rand.nextFloat();
    List<Object> ik = Arrays.asList(is.func_77973_b(), is.func_77960_j());
    if (specialMiningResult.containsKey(ik) && r <= chance * specialMiningChance.get(ik).floatValue()) {
```

The roll that lands exactly on the threshold **succeeds** (`<=`, `Utils.java:178`). The threshold is
`chance * factor`, and the chance of the original is `0.2f + fortune * 0.075f`
(`thaumcraft/common/lib/events/EventHandlerWorld.java:182`, the call site is
`EventHandlerWorld.java:186`).

**Port.** `common/src/main/java/thaumcraft/common/lib/utils/SpecialMining.java:64-65`:

```java
/** Original {@code r <= chance * specialMiningChance}: a roll exactly on the threshold still succeeds. */
public static boolean succeeds(float roll, float chance, float factor) { return roll <= chance * factor; }
```

The production rule is exactly the original rule.

**The test.** `common/src/test/java/thaumcraft/crash/SpecialMiningSafetyTest.java:64` (before):

```java
assertTrue(SpecialMining.succeeds(0.2F, 1.0F, 1.0F));
assertFalse(SpecialMining.succeeds(0.20001F, 1.0F, 1.0F));
```

The second argument of `succeeds` is the **chance**, and the test passed `1.0` — a hundred per cent
chance, whose threshold is `1.0`, not `0.2`. Every roll below 1.0 succeeds, so the second
assertion could never hold. The test asserted a boundary the original never has.

**Verdict: the test was wrong, the production code is right.**

**Fix** (`SpecialMiningSafetyTest.java:64-73`): the test now probes the real threshold, the base
chance of the original:

```java
assertTrue(SpecialMining.succeeds(0.2F, SpecialMining.BASE_CHANCE, 1.0F));
assertFalse(SpecialMining.succeeds(0.20001F, SpecialMining.BASE_CHANCE, 1.0F));
assertTrue(SpecialMining.succeeds(0.17F, SpecialMining.BASE_CHANCE, SpecialMining.GOLD));
assertFalse(SpecialMining.succeeds(0.19F, SpecialMining.BASE_CHANCE, SpecialMining.GOLD));
```

The two added assertions cover the factor 0.9 of the gold and the cinnabar cluster
(`SpecialMining.java:41`), whose threshold is `0.2 * 0.9 = 0.18`.

---

## 2. `anUnknownReplacementKeepsTheOriginalDrop` — the production guard never fired

**Original.** `Utils.java:174-183` again. The original table is filled at start up with the
**registered** clusters (`Utils.addSpecialMiningResult`, `Utils.java:169-172`, called from
`Thaumcraft.java:243` and `Config.java:611-631`), so the original never meets a missing cluster:
the replacement stack always exists.

**Port.** `common/src/main/java/thaumcraft/common/lib/utils/SpecialMining.java:91-100` (before the
fix):

```java
Item output = BuiltInRegistries.ITEM.get(result.output());
// A cluster that the registry does not know keeps the original drop.
if (output == null) return dropped.copy();
return new ItemStack(output, dropped.getCount());
```

The rule the comment states is never executed. `BuiltInRegistries.ITEM` is a
**`DefaultedRegistry<Item>`** (`javap net.minecraft.core.registries.BuiltInRegistries`:
`public static final net.minecraft.core.DefaultedRegistry<net.minecraft.world.item.Item> ITEM;`),
and a defaulted registry answers its **default value** — the air item — for an unknown id, never
`null`. So the guard was dead code and the drop was replaced by
`new ItemStack(Items.AIR, count)`, which `ItemStack.isEmpty()` reports as empty
(`javap -c net.minecraft.world.item.ItemStack` — `isEmpty()` is true when the item is
`Items.AIR`): **the drop of the block was destroyed**.

**Verdict: the production code was wrong, the test is the specification.**

**Fix** (`SpecialMining.java:91-100`): the lookup is now checked by key, so an unknown cluster
keeps the original drop:

```java
if (!BuiltInRegistries.ITEM.containsKey(result.output())) return dropped.copy();
Item output = BuiltInRegistries.ITEM.get(result.output());
// A cluster that the registry does not know keeps the original drop.
if (output == null) return dropped.copy();
```

`Registry.containsKey(ResourceLocation)` is a plain map lookup on the registered locations
(`javap -c net.minecraft.core.MappedRegistry`), so it is false for an id that was never
registered and true for every real cluster.

The registered path is unchanged: the GameTest `pickaxeOfTheCoreBurnsWearsAndFindsClusters`
(`fabric/src/gameTest/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java:926`,
`common/src/gameTest/java/thaumcraft/common/testing/ElementalPickaxeRuntimeChecks.java:154-176`)
still swaps raw iron, raw gold and the cinnabar ore into their clusters.

The test is the specification and stays — it was strengthened, not weakened: it now states its
premise (the unit test JVM knows the vanilla items only, so the cluster of the table is missing
there) and asserts the rule itself (the result is the original raw iron and not an empty stack),
not only the count (`SpecialMiningSafetyTest.java:48-69`).

---

## 3. `thePieceWeightDrawIsTheVillageSizeOrOneMore` — the test alternated by position, not by value

**Original.** `thaumcraft/common/lib/world/VillageBankerManager.java:58-60`:

```java
public StructureVillagePieces.PieceWeight getVillagePieceWeight(Random random, int i) {
    return new StructureVillagePieces.PieceWeight(ComponentBankerHome.class, 25, MathHelper.func_76136_a((Random)random, (int)(0 + i), (int)(1 + i)));
}
```

and `thaumcraft/common/lib/world/VillageWizardManager.java:55-57` with the weight 15 and the
wizard tower. `MathHelper.getRandomIntegerInRange(random, min, max)` is
`min + random.nextInt(max - min + 1)`, so the draw of the original is `i + nextInt(2)`: the two
values `i` and `i + 1`, and the draw itself is `0` or `1`. The helper survived into 1.21.1 as
`Mth.nextInt(random, min, max)` with the same body
(`javap -c net.minecraft.util.Mth`: `min + random.nextInt(max - min + 1)`).

**Port.** `common/src/main/java/thaumcraft/common/lib/world/VillageWorldGenRules.java:102`:

```java
public static int pieceWeightMinSize(int villageSize, int roll) {
    return villageSize + Math.floorMod(roll, 2);
}
```

The port models the two wide draw exactly and folds any draw outside the domain of the original
by its parity: an even draw is the village size, an odd draw the village size plus one.

**The test.** `common/src/test/java/thaumcraft/worldgen/VillageComponentParityTest.java:162`
(before):

```java
assertEquals(size,     ...pieceWeightMinSize(size, 0));
assertEquals(size + 1, ...pieceWeightMinSize(size, 1));
assertEquals(size,     ...pieceWeightMinSize(size, 2));
assertEquals(size + 1, ...pieceWeightMinSize(size, 3));
assertEquals(size,     ...pieceWeightMinSize(size, -1));   // <- odd draw, expected the size
assertEquals(size + 1, ...pieceWeightMinSize(size, -2));   // <- even draw, expected the size plus one
```

The six expectations alternate `size, size+1` **by their position in the list**. That is right for
the draws `0, 1, 2, 3` (even → size, odd → size + 1) and wrong for the two negative draws: `-1`
is odd and `-2` is even, so the original's two wide draw gives `size + 1` and `size`. No
implementation of a two wide draw can satisfy the old expectations: the parity of the *value* is
the only thing that can pick between the two values of the range.

**Verdict: the test was wrong, the production code is right.** The claim of the `worldgen` agent
(`docs/analysis/2026-10-23-agent-worldgen.md:99-113`, defect D3) — that the 1.7.10 helper is
`min + nextInt(max - min + 1)` and the draw is `i` or `i + 1` — was checked against the oracle and
is correct; the oracle expression `MathHelper.func_76136_a(random, 0 + i, 1 + i)` is exactly that
helper.

**Fix** (`VillageComponentParityTest.java:162-176`): the two wrong expectations are replaced by
the original values, and the comment states the rule and the oracle lines:

```java
assertEquals(size + 1, VillageWorldGenRules.pieceWeightMinSize(size, -1));
assertEquals(size,     VillageWorldGenRules.pieceWeightMinSize(size, -2));
```

The javadoc of the rule (`VillageWorldGenRules.java:84-101`) now states the fold of a draw that
is outside the domain of the original.

---

## 4. `theTradeTablesKeepTheOriginalRows` — a list compared against a single id

**Original.** The trade tables are `thaumcraft/common/lib/world/VillageBankerManager.java:41-56`
(twelve rows) and `thaumcraft/common/lib/world/VillageWizardManager.java:35-52` (fifteen rows).
Every row of the original is one pay stack and one receive stack, e.g.

```java
recipeList.add((Object)new MerchantRecipe(new ItemStack(ConfigItems.itemResource, 4 + random.nextInt(3), 3), new ItemStack(Items.field_151166_bC)));  // VillageWizardManager:39
```

`ConfigItems.itemResource` metadata 3 is the quicksilver and metadata 6 the amber
(`thaumcraft/common/config/ConfigItems.java:274-275`).

**Port.** `common/src/main/java/thaumcraft/common/lib/world/VillageWorldGenRules.java:632`:

```java
public record TradeSpec(String payItem, int payMin, int payMax, List<String> receiveItems, int receiveMin, int receiveMax)
```

`payItem` is a **single** id and `receiveItems` is the list of the metadata variants of the
receive side. The port table matches the oracle row for row:

| row | oracle | port |
|-----|--------|------|
| wizard 3 (index 2) | `VillageWizardManager.java:39` — resource metadata 3, the quicksilver, four to six | `thaumcraft:quick_silver`, 4..6 |
| wizard 5 (index 4) | `VillageWizardManager.java:41` — resource metadata 6, the amber, four to six | `thaumcraft:amber_gem`, 4..6 |
| wizard 7 (index 6) | `VillageWizardManager.java:43` — `itemNuggetChicken`, 24..31 | `thaumcraft:nugget_chicken`, 24..31 |
| wizard 9 (index 8) | `VillageWizardManager.java:45` — `itemNuggetBeef`, 24..31 | `thaumcraft:nugget_beef`, 24..31 |
| wizard 10 (index 9) | `VillageWizardManager.java:46` — the shard, two or three of the six metadata values | six shards, 2..3 |
| wizard 15 (index 14) | `VillageWizardManager.java:51` — the bauble blank, one of the six metadata values | six blanks, 1..1 |

**The test.** `VillageComponentParityTest.java:191` (before):

```java
assertEquals(List.of("thaumcraft:quick_silver"), wizard.get(2).payItem());
```

A `List` was compared against a `String`; the four assertions of the pay item could never hold.
The three other pay item assertions of the same test had the same defect and were never reached
because the first one aborted the test.

**Verdict: the test was wrong, the production table is right** (it is the same table the other
test `thaumcraft/world/VillageWorldGenRulesTest.java:224-258` already asserts as a string).

**Fix** (`VillageComponentParityTest.java:191-217`): the pay item is compared as the single id it
is, and every row got its oracle line:

```java
assertEquals("thaumcraft:quick_silver", wizard.get(2).payItem());
assertEquals("thaumcraft:amber_gem", wizard.get(4).payItem());
assertEquals("thaumcraft:nugget_chicken", wizard.get(6).payItem());
assertEquals("thaumcraft:nugget_beef", wizard.get(8).payItem());
```

---

# The GameTest suite

`:fabric:runGameTest` was red with **three** failures that had nothing to do with the four JUnit
tests. All three are proved below to be older than this branch (the same three fail at the
branch point `55458da2`), and all three are settled now.

| case | verdict | what it was |
|------|---------|-------------|
| `fluxOriginalExposureAndGooTail` | **production** bug | the goo tail of the original is one else-if chain, the port ended the tick after a failed breed roll |
| `fluxMovesAndExhaustsWithOriginalRules` | **stale test** | the case still expected the M11 placeholder, the port implements the real slime and the taint branch now |
| `fullInventoryReusesFreedPaperSlotOrDrops` | **stale test** | the case expected the creative note issuance to spend an ink point, the original does not |

Two more cases of the suite turned out to be **stateful** (they poison the GameTest world for the
next run) and are repaired as well: `magicalBiomesGenerateNaturallyAndSpawnActualPech` and
`surfaceStructuresReachTheOverworld`.

## 5. `fluxOriginalExposureAndGooTail` — the goo tail was a nested condition, the original is a chain

**Original.** `thaumcraft/common/blocks/BlockFluxGoo.java:103-136` (the update tick):

```java
int meta = world.func_72805_g(x, y, z);
if (meta >= 2 && meta < 6 && world.func_147437_c(x, y + 1, z) && rand.nextInt(25) == 0) {
    // the cell is emptied, a size one slime is born
} else if (meta >= 6 && world.func_147437_c(x, y + 1, z)) {
    if (rand.nextInt(25) == 0) { /* the cell is emptied, a size two slime is born */ }
    else if (Config.taintFromFlux && rand.nextInt(50) == 0) { /* the taint takes the cell */ }
} else if (rand.nextInt(30) == 0) {
    /* the decay: the cell loses one quantum, then the boolean and the gas above */
}
```

One **else-if chain**: the breed roll is part of the *condition* of the first branch, so a failed
roll falls through to the thirty roll of the decay branch. `Config.taintFromFlux` is true by
default (`Config.java:838`, `:496`).

**Port.** `common/src/main/java/thaumcraft/common/blocks/BlockFluxGoo.java:109-121` (before the
fix): the breed branch ended the tick after a failed roll, so the cell never decayed and never
turned into the flux gas of the original. The port's own comment stated the fall-through, the
code did not do it.

**Verdict: the production code was wrong.**

**Fix** (`BlockFluxGoo.java:109-120`): the breed branch only ends the tick when the roll breeds
the slime, a missed roll continues with the decay branch of the original (its own thirty roll
and boolean).

## 6. `fluxMovesAndExhaustsWithOriginalRules` — the case still expected the M11 placeholder

The case asserted *"real M11 slime/taint still explicitly DEFERRED: no vanilla slime, fake entity
debit or invented ninth-unit spill"* and that the high goo stays untouched. The port implements
the branch of the original now (`BlockFluxGoo.java:112-137`, the commit of the `basecontent`
agent *grow and breed the thaumic slime in the flux goo*): the breed roll empties the cell and
births a real thaumic slime, the fifty roll turns the cell into the taint fibres.

**Verdict: the test was stale, the production code is right** (the oracle
`BlockFluxGoo.java:106-124`).

**Fix**: the two flux cases read the implemented and original behaviour:

- the high goo births a real size two slime and empties the cell — and the champion hook of the
  original draws from the world random when the mob is born
  (`thaumcraft/common/lib/events/EventHandlerEntity.java:391`), so the case declares that draw;
- the taint roll turns the cell into the taint fibres;
- a missed breed roll and a missed taint roll leave the high goo untouched.

## 7. `fullInventoryReusesFreedPaperSlotOrDrops` — the creative ink of the original

**Original.** `thaumcraft/common/lib/research/ResearchManager.java:199-209` spends the ink with
the vanilla `ItemStack.damageItem(1, player)`. The audited bytecode of that vanilla method
(`docs/analysis/2026-10-15-scribing-ink-semantics.md:19`, the SRG `func_77972_a`, offsets 0–20)
returns for a creative player **before** the damageability, the enchantments and the RNG: no
consumption, no random call. The same document, line 50, states the outcome for the full
creative inventory: *"a fully occupied creative inventory with a paper stack of two gives
paper1, an **unchanged** usable inkwell, zero notes in the inventory and zero dropped notes"*.

**Verdict: the test was wrong** (it expected the ink to be spent in creative), the production
code of the port (`common/src/main/java/thaumcraft/common/lib/research/ScribingInk.java`,
`planPlayer`: `if (creative || !state.damageable()) return new Plan(true, damage, damage);`) is
right.

**Fix** (`ResearchNoteIssuanceRuntimeChecks.java`): the creative case expects the unchanged ink
(`getDamageValue() == 98`) and says why.

## 8. `magicalBiomesGenerateNaturallyAndSpawnActualPech` — the case poisoned its own world

The case harvests the first pod of the generated region and replants the bean there. The original
plants the age zero (`thaumcraft/common/items/ItemManaBean.java:192-193`), and the GameTest world
keeps that planted pod, so the **next** run of the case read the planted pod instead of the
generated one and the "generation-plus-growth age 3..7" check failed (the pod of the run had the
age 0).

**Fix** (`NaturalBiomeRuntimeChecks.java`): the case grows a stale planted pod back with the
original growth rule (`TileManaPod.checkGrowth`) before it reads it, and restores the generated
age and the aspect of the cell after the replant. The case is repeatable (verified with two
consecutive runs).

## 9. `surfaceStructuresReachTheOverworld` — the candidate selection of the mound case

The case picked the chunks of the exclusive chain by the terrain alone and then looked for the
mound in them. The mound of the original grows nine blocks below the **live** surface of its
column and is refused when that base reaches the sea level
(`thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java:424-425`), and the live surface
carries the decoration of the chunk, so a chunk whose trees raise the surface carries no mound.
The terrain alone therefore does not say which chunks can: for a world seed whose forest stands
on the chain's chunks, no candidate carried the mound and the case failed.

**Fix** (`SurfaceStructurePlacementRuntimeChecks.java`): the case asks the live surface of the
generated chunk, exactly the rule the feature applies, and the content assertions of the mound
(the chest, the two spawners, the loot container, the shell, the aura node) stay as they are.

## 10. The taint of the flux cases reached the other cases of the level

After the fixes of 5 and 6, the full suite reported
`unstableNodesEmitPaidAspectOrbsAndCanStabilise` as failed: the taint fibres of the original
spread over the level (`thaumcraft/common/blocks/BlockTaintFibres.java:84-100` — the fibres grow
into the neighbours and the taint biome follows them), so the taint of the flux case reached the
structure of the node case and the natural unstable node was gone before its thaumometer could
observe it.

**Fix** (both flux cases): the cell and the biome of the column are restored after the taint
check, so the taint never spreads into the other cases of the level.

---

# Files changed

```
common/src/main/java/thaumcraft/common/lib/utils/SpecialMining.java              (+6)   production fix, failure 2
common/src/main/java/thaumcraft/common/blocks/BlockFluxGoo.java                  (~)    production fix, the goo tail
common/src/main/java/thaumcraft/common/lib/world/VillageWorldGenRules.java       (+4)   javadoc of the fold, failure 3
common/src/test/java/thaumcraft/crash/SpecialMiningSafetyTest.java               (~)    failures 1 and 2
common/src/test/java/thaumcraft/worldgen/VillageComponentParityTest.java         (~)    failures 3 and 4
common/src/gameTest/java/thaumcraft/common/testing/FluxOverfullRuntimeChecks.java (~)   the goo tail and the taint cleanup
common/src/gameTest/java/thaumcraft/common/testing/FluxRuntimeChecks.java        (~)    the slime and the taint cleanup
common/src/gameTest/java/thaumcraft/common/testing/ResearchNoteIssuanceRuntimeChecks.java (~) the creative ink
common/src/gameTest/java/thaumcraft/common/testing/NaturalBiomeRuntimeChecks.java (~)   the mana pod cell
common/src/gameTest/java/thaumcraft/common/testing/SurfaceStructurePlacementRuntimeChecks.java (~) the mound candidates
```

No unit test constructs a `Block`; the special mining rule is a pure static function
(`SpecialMining.succeeds`, `SpecialMining.find`) and the village rule is a pure static function
(`VillageWorldGenRules.pieceWeightMinSize`), both called by the production code.

# Verification

- `:common:test` — **2059 tests, 0 failures, 0 errors, 0 skipped** (the four tests of this report
  are green: `SpecialMiningSafetyTest` 6/6, `VillageComponentParityTest` 9/9, and the village
  table of `thaumcraft.world.VillageWorldGenRulesTest` 39/39 is still green).
- `:fabric:runGameTest` — **318 tests, all passed**, twice in a row in the same world (the first
  run after a clean `fabric/run-gametest/world`, the second on the world the first one left). The
  three failures of the suite before this agent are gone, and the two stateful cases of the
  section above are repaired.
- Both runs used a private Gradle home
  (`GRADLE_USER_HOME=<worktree>/.gradle-fix4`, which is ignored by the repository), so the shared
  Loom cache of the leader was not touched. The machine was under the memory pressure of the
  other agents, so the daemon heap was lowered to 1 GiB for the GameTest runs.

# Commits

| commit | what |
|--------|------|
| `chore(build): ignore the private gradle home of the test agent` | `.gradle-fix4/` is never committed |
| `fix(utils): an unknown cluster keeps the original drop` | the production defect of failure 2 and the boundary test of failure 1 |
| `fix(worldgen): the piece weight draw and the trade rows of the village` | failures 3 and 4 |
| `test(utils): the unknown replacement test states its premise and the drop it keeps` | the strengthened case of failure 2 |
| `fix(flux): a failed breed roll falls through to the decay of the original` | the goo tail of the original |
| `test(flux): the goo tail cases assert the original slime and taint branches` | the stale M11 expectations |
| `test(research): the creative note issuance keeps the ink of the original` | the creative ink of the original |
| `test(worldgen): the mana pod case restores the cell it harvests` | the stateful mana pod case |
| `test(worldgen): the mound case selects the chunks the original rule accepts` | the mound candidates |
| `test(flux): the taint cases restore their cell, the taint would spread` | the taint spread into the other cases |
| `docs(testfix4): the four red tests against the oracle` | this report |

# Open points

- The scope ledger was not touched: this agent had no unit list
  (`/home/cyberfox-kali/units/testfix4.txt` does not exist), and the units of the touched files
  (`thaumcraft/common/lib/utils/Utils.java`, `thaumcraft/common/lib/world/VillageBankerManager.java`,
  `thaumcraft/common/lib/world/VillageWizardManager.java`) are already `parity_verified` in
  `docs/porting/scope-ledger.json`. The leader regenerates the counts.
- The GameTest world of a worktree is a real world: a case that changes it (the mana pod case,
  the taint of the flux cases) has to restore what it changed, and that is now done. A world that
  was saved by an older build can still hold a planted pod; the mana pod case repairs that pod
  itself on its next run.
